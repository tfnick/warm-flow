/*
 *    Copyright 2024-2025, Warm-Flow (290631660@qq.com).
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.dromara.warm.flow.orm.dao;

import com.sagframe.sagacity.sqltoy.plus.dao.SqlToyHelperDao;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.entity.RootEntity;
import org.dromara.warm.flow.core.invoker.FrameInvoker;
import org.dromara.warm.flow.core.orm.agent.WarmQuery;
import org.dromara.warm.flow.core.orm.dao.WarmDao;
import org.dromara.warm.flow.core.utils.ObjectUtil;
import org.dromara.warm.flow.core.utils.StringUtils;
import org.dromara.warm.flow.core.utils.page.Page;
import org.dromara.warm.flow.orm.utils.TenantDeleteUtil;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.*;

/**
 * SqlToy BaseDao 实现
 *
 * @author warm
 * @since 2023-03-17
 */
public abstract class WarmDaoImpl<T extends RootEntity> implements WarmDao<T> {

    private static final Set<String> EXCLUDED_FIELDS = new HashSet<>(Arrays.asList(
        "skipList", "nodeList", "userList", "permissionList"
    ));

    protected SqlToyHelperDao getSqlToyHelperDao() {
        return FrameInvoker.getBean(SqlToyHelperDao.class);
    }

    public abstract T newEntity();

    protected abstract String getTableName();

    @Override
    public T selectById(Serializable id) {
        T entity = TenantDeleteUtil.getEntity(newEntity());
        StringBuilder sql = new StringBuilder("select * from ").append(getTableName());
        sql.append(" where id = ?");
        List<Object> params = new ArrayList<>();
        params.add(id);
        if (StringUtils.isNotEmpty(entity.getDelFlag())) {
            sql.append(" and del_flag = ?");
            params.add(entity.getDelFlag());
        }
        if (StringUtils.isNotEmpty(entity.getTenantId())) {
            sql.append(" and tenant_id = ?");
            params.add(entity.getTenantId());
        }
        List<T> list = getSqlToyHelperDao().findBySql(sql.toString(), null, params.toArray(), (Class<T>) newEntity().getClass());
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public List<T> selectByIds(Collection<? extends Serializable> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        T entity = TenantDeleteUtil.getEntity(newEntity());
        StringBuilder sql = new StringBuilder("select * from ").append(getTableName());
        sql.append(" where id in (").append(buildInPlaceholders(ids.size())).append(")");
        List<Object> params = new ArrayList<>(ids);
        if (StringUtils.isNotEmpty(entity.getDelFlag())) {
            sql.append(" and del_flag = ?");
            params.add(entity.getDelFlag());
        }
        if (StringUtils.isNotEmpty(entity.getTenantId())) {
            sql.append(" and tenant_id = ?");
            params.add(entity.getTenantId());
        }
        return getSqlToyHelperDao().findBySql(sql.toString(), null, params.toArray(), (Class<T>) newEntity().getClass());
    }

    @Override
    public Page<T> selectPage(T entity, Page<T> page) {
        TenantDeleteUtil.getEntity(entity);
        String dataSourceType = FlowEngine.dataSourceType();
        long offset = (page.getPageNum() - 1) * page.getPageSize();
        long limit = page.getPageSize();
        if ("oracle".equals(dataSourceType)) {
            limit = limit + offset;
        }

        String whereClause = buildWhereClause(entity);
        String order = page.getOrderBy() + " " + page.getIsAsc();
        String countSql = "select count(*) from " + getTableName() + whereClause;
        long total = getSqlToyHelperDao().getCount(countSql, null, getWhereParams(entity).toArray());

        if (total > 0) {
            String selectSql = "select * from " + getTableName() + whereClause;
            selectSql = wrapWithPageSql(selectSql, order, offset, limit, dataSourceType);
            List<T> list = getSqlToyHelperDao().findBySql(selectSql, null, getWhereParams(entity).toArray(), (Class<T>) newEntity().getClass());
            return new Page<>(list, total);
        }
        return Page.empty();
    }

    @Override
    public List<T> selectList(T entity, WarmQuery<T> query) {
        TenantDeleteUtil.getEntity(entity);
        String order = null;
        if (ObjectUtil.isNotNull(query)) {
            order = query.getOrderBy() + " " + query.getIsAsc();
        }
        String selectSql = "select * from " + getTableName() + buildWhereClause(entity);
        if (StringUtils.isNotEmpty(order)) {
            selectSql += " order by " + order;
        }
        return getSqlToyHelperDao().findBySql(selectSql, null, getWhereParams(entity).toArray(), (Class<T>) newEntity().getClass());
    }

    @Override
    public long selectCount(T entity) {
        TenantDeleteUtil.getEntity(entity);
        String countSql = "select count(*) from " + getTableName() + buildWhereClause(entity);
        return getSqlToyHelperDao().getCount(countSql, null, getWhereParams(entity).toArray());
    }

    @Override
    public int save(T entity) {
        TenantDeleteUtil.getEntity(entity);
        StringBuilder columns = new StringBuilder();
        StringBuilder placeholders = new StringBuilder();
        List<Object> params = new ArrayList<>();

        Field[] fields = entity.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            String fieldName = field.getName();
            if (EXCLUDED_FIELDS.contains(fieldName)) {
                continue;
            }
            try {
                Object value = field.get(entity);
                if (value != null) {
                    String columnName = camelToSnake(fieldName);
                    if (columns.length() > 0) {
                        columns.append(", ");
                        placeholders.append(", ");
                    }
                    columns.append(columnName);
                    placeholders.append("?");
                    params.add(value);
                }
            } catch (IllegalAccessException ignored) {
            }
        }

        String sql = "insert into " + getTableName() + " (" + columns + ") values (" + placeholders + ")";
        return getSqlToyHelperDao().executeSql(sql, null, params.toArray()).intValue();
    }

    @Override
    public int updateById(T entity) {
        TenantDeleteUtil.getEntity(entity);
        Set<String> excludedSetFields = new HashSet<>(Arrays.asList("id", "delFlag", "tenantId"));
        excludedSetFields.addAll(EXCLUDED_FIELDS);

        StringBuilder setClause = new StringBuilder();
        List<Object> params = new ArrayList<>();

        Field[] fields = entity.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            String fieldName = field.getName();
            if (excludedSetFields.contains(fieldName)) {
                continue;
            }
            try {
                Object value = field.get(entity);
                if (value != null) {
                    String columnName = camelToSnake(fieldName);
                    if (setClause.length() > 0) {
                        setClause.append(", ");
                    }
                    setClause.append(columnName).append(" = ?");
                    params.add(value);
                }
            } catch (IllegalAccessException ignored) {
            }
        }

        if (setClause.length() == 0) {
            return 0;
        }

        String sql = "update " + getTableName() + " set " + setClause + " where id = ?";
        params.add(entity.getId());
        if (StringUtils.isNotEmpty(entity.getDelFlag())) {
            sql += " and del_flag = ?";
            params.add(entity.getDelFlag());
        }
        if (StringUtils.isNotEmpty(entity.getTenantId())) {
            sql += " and tenant_id = ?";
            params.add(entity.getTenantId());
        }
        return getSqlToyHelperDao().executeSql(sql, null, params.toArray()).intValue();
    }

    @Override
    public int delete(T entity) {
        TenantDeleteUtil.getEntity(entity);
        if (StringUtils.isNotEmpty(entity.getDelFlag())) {
            return executeLogicDelete(buildWhereClause(entity), getWhereParams(entity), entity.getDelFlag());
        }
        String sql = "delete from " + getTableName() + buildWhereClause(entity);
        return getSqlToyHelperDao().executeSql(sql, null, getWhereParams(entity).toArray()).intValue();
    }

    @Override
    public int deleteById(Serializable id) {
        T entity = TenantDeleteUtil.getEntity(newEntity());
        if (StringUtils.isNotEmpty(entity.getDelFlag())) {
            String where = " where id = ?";
            if (StringUtils.isNotEmpty(entity.getDelFlag())) {
                where += " and del_flag = ?";
            }
            if (StringUtils.isNotEmpty(entity.getTenantId())) {
                where += " and tenant_id = ?";
            }
            return executeLogicDelete(where, buildDeleteByIdParams(id, entity), entity.getDelFlag());
        }
        String sql = "delete from " + getTableName() + " where id = ?";
        if (StringUtils.isNotEmpty(entity.getTenantId())) {
            sql += " and tenant_id = ?";
        }
        List<Object> params = new ArrayList<>();
        params.add(id);
        if (StringUtils.isNotEmpty(entity.getTenantId())) {
            params.add(entity.getTenantId());
        }
        return getSqlToyHelperDao().executeSql(sql, null, params.toArray()).intValue();
    }

    @Override
    public int deleteByIds(Collection<? extends Serializable> ids) {
        T entity = TenantDeleteUtil.getEntity(newEntity());
        if (StringUtils.isNotEmpty(entity.getDelFlag())) {
            String where = " where id in (" + buildInPlaceholders(ids.size()) + ")";
            if (StringUtils.isNotEmpty(entity.getDelFlag())) {
                where += " and del_flag = ?";
            }
            if (StringUtils.isNotEmpty(entity.getTenantId())) {
                where += " and tenant_id = ?";
            }
            return executeLogicDelete(where, buildDeleteByIdsParams(ids, entity), entity.getDelFlag());
        }
        String sql = "delete from " + getTableName() + " where id in (" + buildInPlaceholders(ids.size()) + ")";
        List<Object> params = new ArrayList<>(ids);
        if (StringUtils.isNotEmpty(entity.getTenantId())) {
            sql += " and tenant_id = ?";
            params.add(entity.getTenantId());
        }
        return getSqlToyHelperDao().executeSql(sql, null, params.toArray()).intValue();
    }

    @Override
    public void saveBatch(List<T> list) {
        for (T record : list) {
            TenantDeleteUtil.getEntity(record);
        }
        String dataSourceType = FlowEngine.dataSourceType();
        if ("oracle".equals(dataSourceType)) {
            saveBatchOracle(list);
        } else {
            saveBatchMysql(list);
        }
    }

    @Override
    public void updateBatch(List<T> list) {
        for (T record : list) {
            updateById(record);
        }
    }

    // ========== 子类可用的辅助方法 ==========

    protected int executeLogicDelete(String whereClause, List<Object> whereParams, String currentDelFlag) {
        String logicDeleteValue = FlowEngine.getFlowConfig().getLogicDeleteValue();
        String sql = "update " + getTableName() + " set del_flag = ?" + whereClause + " and del_flag = ?";
        List<Object> params = new ArrayList<>();
        params.add(logicDeleteValue);
        params.addAll(whereParams);
        params.add(currentDelFlag);
        return getSqlToyHelperDao().executeSql(sql, null, params.toArray()).intValue();
    }

    protected int executeLogicDeleteByColumn(String column, Collection<?> values, T entity) {
        String logicDeleteValue = FlowEngine.getFlowConfig().getLogicDeleteValue();
        String sql = "update " + getTableName() + " set del_flag = ? where " + column + " in ("
            + buildInPlaceholders(values.size()) + ")";
        List<Object> params = new ArrayList<>();
        params.add(logicDeleteValue);
        params.addAll(values);
        if (StringUtils.isNotEmpty(entity.getTenantId())) {
            sql += " and tenant_id = ?";
            params.add(entity.getTenantId());
        }
        sql += " and del_flag = ?";
        params.add(entity.getDelFlag());
        return getSqlToyHelperDao().executeSql(sql, null, params.toArray()).intValue();
    }

    protected int deleteByColumn(String column, Collection<?> values, T entity) {
        String sql = "delete from " + getTableName() + " where " + column + " in ("
            + buildInPlaceholders(values.size()) + ")";
        List<Object> params = new ArrayList<>(values);
        if (StringUtils.isNotEmpty(entity.getTenantId())) {
            sql += " and tenant_id = ?";
            params.add(entity.getTenantId());
        }
        return getSqlToyHelperDao().executeSql(sql, null, params.toArray()).intValue();
    }

    protected List<T> findBySql(String sql, Object... params) {
        return getSqlToyHelperDao().findBySql(sql, null, params, (Class<T>) newEntity().getClass());
    }

    protected int executeSql(String sql, Object... params) {
        return getSqlToyHelperDao().executeSql(sql, null, params).intValue();
    }

    protected long countBySql(String sql, Object... params) {
        return getSqlToyHelperDao().getCount(sql, null, params);
    }

    // ========== 私有辅助方法 ==========

    private String buildWhereClause(T entity) {
        StringBuilder where = new StringBuilder();
        Field[] fields = entity.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            String fieldName = field.getName();
            if (EXCLUDED_FIELDS.contains(fieldName)) {
                continue;
            }
            try {
                Object value = field.get(entity);
                if (value != null) {
                    String columnName = camelToSnake(fieldName);
                    where.append(" and ").append(columnName).append(" = ?");
                }
            } catch (IllegalAccessException ignored) {
            }
        }
        return where.length() > 0 ? " where " + where.substring(5) : "";
    }

    private List<Object> getWhereParams(T entity) {
        List<Object> params = new ArrayList<>();
        Field[] fields = entity.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            String fieldName = field.getName();
            if (EXCLUDED_FIELDS.contains(fieldName)) {
                continue;
            }
            try {
                Object value = field.get(entity);
                if (value != null) {
                    params.add(value);
                }
            } catch (IllegalAccessException ignored) {
            }
        }
        return params;
    }

    private List<Object> buildDeleteByIdParams(Serializable id, T entity) {
        List<Object> params = new ArrayList<>();
        params.add(id);
        if (StringUtils.isNotEmpty(entity.getDelFlag())) {
            params.add(entity.getDelFlag());
        }
        if (StringUtils.isNotEmpty(entity.getTenantId())) {
            params.add(entity.getTenantId());
        }
        return params;
    }

    private List<Object> buildDeleteByIdsParams(Collection<? extends Serializable> ids, T entity) {
        List<Object> params = new ArrayList<>(ids);
        if (StringUtils.isNotEmpty(entity.getDelFlag())) {
            params.add(entity.getDelFlag());
        }
        if (StringUtils.isNotEmpty(entity.getTenantId())) {
            params.add(entity.getTenantId());
        }
        return params;
    }

    protected String buildInPlaceholders(int size) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("?");
        }
        return sb.toString();
    }

    private String wrapWithPageSql(String sql, String order, long offset, long limit, String dataSourceType) {
        if (StringUtils.isNotEmpty(order)) {
            sql += " order by " + order;
        }
        if ("oracle".equals(dataSourceType)) {
            return "SELECT * FROM (SELECT TMP.*, ROWNUM ROW_ID FROM (" + sql
                + ") TMP WHERE ROWNUM <= " + limit + ") WHERE ROW_ID > " + offset;
        } else {
            return sql + " LIMIT " + limit + " OFFSET " + offset;
        }
    }

    private void saveBatchMysql(List<T> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        // 获取字段信息（基于第一条记录）
        T first = list.get(0);
        Set<String> excludedFields = new HashSet<>(EXCLUDED_FIELDS);
        List<String> columnNames = new ArrayList<>();
        List<Field> activeFields = new ArrayList<>();

        Field[] fields = first.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            String fieldName = field.getName();
            if (excludedFields.contains(fieldName)) {
                continue;
            }
            try {
                if (field.get(first) != null) {
                    columnNames.add(camelToSnake(fieldName));
                    activeFields.add(field);
                }
            } catch (IllegalAccessException ignored) {
            }
        }

        if (columnNames.isEmpty()) {
            return;
        }

        StringBuilder sql = new StringBuilder("insert into ").append(getTableName()).append(" (");
        sql.append(String.join(", ", columnNames)).append(") values ");

        List<Object> params = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append("(");
            for (int j = 0; j < activeFields.size(); j++) {
                if (j > 0) {
                    sql.append(", ");
                }
                sql.append("?");
                try {
                    params.add(activeFields.get(j).get(list.get(i)));
                } catch (IllegalAccessException ignored) {
                    params.add(null);
                }
            }
            sql.append(")");
        }

        getSqlToyHelperDao().executeSql(sql.toString(), null, params.toArray());
    }

    private void saveBatchOracle(List<T> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        T first = list.get(0);
        Set<String> excludedFields = new HashSet<>(EXCLUDED_FIELDS);
        List<String> columnNames = new ArrayList<>();
        List<Field> activeFields = new ArrayList<>();

        Field[] fields = first.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            String fieldName = field.getName();
            if (excludedFields.contains(fieldName)) {
                continue;
            }
            try {
                if (field.get(first) != null) {
                    columnNames.add(camelToSnake(fieldName));
                    activeFields.add(field);
                }
            } catch (IllegalAccessException ignored) {
            }
        }

        if (columnNames.isEmpty()) {
            return;
        }

        StringBuilder sql = new StringBuilder("insert into ").append(getTableName()).append(" (");
        sql.append(String.join(", ", columnNames)).append(") ");

        List<Object> params = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sql.append(" union all ");
            }
            sql.append("select ");
            for (int j = 0; j < activeFields.size(); j++) {
                if (j > 0) {
                    sql.append(", ");
                }
                sql.append("?");
                try {
                    params.add(activeFields.get(j).get(list.get(i)));
                } catch (IllegalAccessException ignored) {
                    params.add(null);
                }
            }
            sql.append(" from dual");
        }

        getSqlToyHelperDao().executeSql(sql.toString(), null, params.toArray());
    }

    private static String camelToSnake(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
}
