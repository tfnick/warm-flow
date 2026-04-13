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
import org.dromara.warm.flow.core.orm.dao.FlowNodeDao;
import org.dromara.warm.flow.core.utils.StringUtils;
import org.dromara.warm.flow.core.utils.page.Page;
import org.dromara.warm.flow.orm.entity.FlowNode;
import org.dromara.warm.flow.orm.utils.TenantDeleteUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 流程节点Dao接口
 *
 * @author warm
 * @since 2023-03-29
 */
public class FlowNodeDaoImpl extends WarmDaoImpl<FlowNode> implements FlowNodeDao<FlowNode> {

    @Override
    public FlowNode newEntity() {
        return new FlowNode();
    }

    @Override
    public Page<FlowNode> selectPage(FlowNode entity, Page<FlowNode> page) {
        return null;
    }

    @Override
    protected String getTableName() {
        return "flow_node";
    }

    @Override
    public List<FlowNode> getByNodeCodes(List<String> nodeCodes, Long definitionId) {
        FlowNode entity = TenantDeleteUtil.getEntity(newEntity());
        StringBuilder sql = new StringBuilder("select * from ").append(getTableName());
        sql.append(" where definition_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(definitionId);
        if (nodeCodes != null && !nodeCodes.isEmpty()) {
            sql.append(" and node_code in (").append(buildInPlaceholders(nodeCodes.size())).append(")");
            params.addAll(nodeCodes);
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
    public int deleteNodeByDefIds(Collection<? extends Serializable> defIds) {
        FlowNode entity = TenantDeleteUtil.getEntity(newEntity());
        if (StringUtils.isNotEmpty(entity.getDelFlag())) {
            return executeLogicDeleteByColumn("definition_id", defIds, entity);
        }
        return deleteByColumn("definition_id", defIds, entity);
    }
}
