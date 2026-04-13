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

import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.orm.dao.FlowUserDao;
import org.dromara.warm.flow.core.utils.CollUtil;
import org.dromara.warm.flow.core.utils.StringUtils;
import org.dromara.warm.flow.core.utils.page.Page;
import org.dromara.warm.flow.orm.entity.FlowUser;
import org.dromara.warm.flow.orm.utils.TenantDeleteUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 流程用户Dao接口
 *
 * @author warm
 * @since 2023-03-29
 */
public class FlowUserDaoImpl extends WarmDaoImpl<FlowUser> implements FlowUserDao<FlowUser> {

    @Override
    public FlowUser newEntity() {
        return new FlowUser();
    }

    @Override
    public Page<FlowUser> selectPage(FlowUser entity, Page<FlowUser> page) {
        return null;
    }

    @Override
    protected String getTableName() {
        return "flow_user";
    }

    @Override
    public int deleteByTaskIds(List<Long> taskIdList) {
        FlowUser entity = TenantDeleteUtil.getEntity(newEntity());
        if (StringUtils.isNotEmpty(entity.getDelFlag())) {
            return executeLogicDeleteByColumn("associated", taskIdList, entity);
        }
        return deleteByColumn("associated", taskIdList, entity);
    }

    @Override
    public List<FlowUser> listByAssociatedAndTypes(List<Long> associateds, String[] types) {
        String dataSourceType = FlowEngine.dataSourceType();
        FlowUser entity = TenantDeleteUtil.getEntity(newEntity());
        StringBuilder sql = new StringBuilder("select * from ").append(getTableName());
        sql.append(" where 1=1");
        List<Object> params = new ArrayList<>();

        if (CollUtil.isNotEmpty(associateds) && associateds.size() == 1) {
            sql.append(" and associated = ?");
            params.add(associateds.get(0));
        } else if (CollUtil.isNotEmpty(associateds)) {
            sql.append(" and associated in (").append(buildInPlaceholders(associateds.size())).append(")");
            params.addAll(associateds);
        }

        if (types != null && types.length > 0) {
            String typeColumn = wrapKeyword("type", dataSourceType);
            sql.append(" and ").append(typeColumn).append(" in (").append(buildInPlaceholders(types.length)).append(")");
            for (String type : types) {
                params.add(type);
            }
        }

        if (StringUtils.isNotEmpty(entity.getDelFlag())) {
            sql.append(" and del_flag = ?");
            params.add(entity.getDelFlag());
        }
        if (StringUtils.isNotEmpty(entity.getTenantId())) {
            sql.append(" and tenant_id = ?");
            params.add(entity.getTenantId());
        }
        return findBySql(sql.toString(), params.toArray());
    }

    @Override
    public List<FlowUser> listByProcessedBys(Long associated, List<String> processedBys, String[] types) {
        String dataSourceType = FlowEngine.dataSourceType();
        FlowUser entity = TenantDeleteUtil.getEntity(newEntity());
        entity.setAssociated(associated);
        StringBuilder sql = new StringBuilder("select * from ").append(getTableName());
        sql.append(" where associated = ?");
        List<Object> params = new ArrayList<>();
        params.add(associated);

        if (CollUtil.isNotEmpty(processedBys) && processedBys.size() == 1) {
            sql.append(" and processed_by = ?");
            params.add(processedBys.get(0));
        } else if (CollUtil.isNotEmpty(processedBys)) {
            sql.append(" and processed_by in (").append(buildInPlaceholders(processedBys.size())).append(")");
            params.addAll(processedBys);
        }

        if (types != null && types.length > 0) {
            String typeColumn = wrapKeyword("type", dataSourceType);
            sql.append(" and ").append(typeColumn).append(" in (").append(buildInPlaceholders(types.length)).append(")");
            for (String type : types) {
                params.add(type);
            }
        }

        if (StringUtils.isNotEmpty(entity.getDelFlag())) {
            sql.append(" and del_flag = ?");
            params.add(entity.getDelFlag());
        }
        if (StringUtils.isNotEmpty(entity.getTenantId())) {
            sql.append(" and tenant_id = ?");
            params.add(entity.getTenantId());
        }
        return findBySql(sql.toString(), params.toArray());
    }

    private String wrapKeyword(String column, String dataSourceType) {
        if ("postgresql".equals(dataSourceType) || "oracle".equals(dataSourceType)) {
            return "\"" + column + "\"";
        }
        return "`" + column + "`";
    }
}
