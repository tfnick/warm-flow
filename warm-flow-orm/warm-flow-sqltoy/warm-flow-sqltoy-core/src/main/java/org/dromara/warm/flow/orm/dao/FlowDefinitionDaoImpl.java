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

import org.dromara.warm.flow.core.orm.dao.FlowDefinitionDao;
import org.dromara.warm.flow.orm.entity.FlowDefinition;
import org.dromara.warm.flow.orm.utils.TenantDeleteUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 流程定义Dao接口
 *
 * @author warm
 * @since 2023-03-29
 */
public class FlowDefinitionDaoImpl extends WarmDaoImpl<FlowDefinition> implements FlowDefinitionDao<FlowDefinition> {

    @Override
    public FlowDefinition newEntity() {
        return new FlowDefinition();
    }

    @Override
    protected String getTableName() {
        return "flow_definition";
    }

    @Override
    public List<FlowDefinition> queryByCodeList(List<String> flowCodeList) {
        FlowDefinition entity = TenantDeleteUtil.getEntity(newEntity());
        StringBuilder sql = new StringBuilder("select * from ").append(getTableName());
        sql.append(" where flow_code in (").append(buildInPlaceholders(flowCodeList.size())).append(")");
        List<Object> params = new ArrayList<>(flowCodeList);
        if (entity.getDelFlag() != null && !entity.getDelFlag().isEmpty()) {
            sql.append(" and del_flag = ?");
            params.add(entity.getDelFlag());
        }
        if (entity.getTenantId() != null && !entity.getTenantId().isEmpty()) {
            sql.append(" and tenant_id = ?");
            params.add(entity.getTenantId());
        }
        return findBySql(sql.toString(), params.toArray());
    }

    @Override
    public void updatePublishStatus(List<Long> ids, Integer publishStatus) {
        FlowDefinition entity = TenantDeleteUtil.getEntity(newEntity());
        StringBuilder sql = new StringBuilder("update ").append(getTableName());
        sql.append(" set is_publish = ? where id in (").append(buildInPlaceholders(ids.size())).append(")");
        List<Object> params = new ArrayList<>();
        params.add(publishStatus);
        params.addAll(ids);
        if (entity.getDelFlag() != null && !entity.getDelFlag().isEmpty()) {
            sql.append(" and del_flag = ?");
            params.add(entity.getDelFlag());
        }
        if (entity.getTenantId() != null && !entity.getTenantId().isEmpty()) {
            sql.append(" and tenant_id = ?");
            params.add(entity.getTenantId());
        }
        executeSql(sql.toString(), params.toArray());
    }
}
