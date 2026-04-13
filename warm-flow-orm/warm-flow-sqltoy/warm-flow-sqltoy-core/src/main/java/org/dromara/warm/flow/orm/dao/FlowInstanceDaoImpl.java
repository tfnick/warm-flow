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

import org.dromara.warm.flow.core.orm.dao.FlowInstanceDao;
import org.dromara.warm.flow.core.utils.StringUtils;
import org.dromara.warm.flow.core.utils.page.Page;
import org.dromara.warm.flow.orm.entity.FlowInstance;
import org.dromara.warm.flow.orm.utils.TenantDeleteUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 流程实例Dao接口
 *
 * @author warm
 * @since 2023-03-29
 */
public class FlowInstanceDaoImpl extends WarmDaoImpl<FlowInstance> implements FlowInstanceDao<FlowInstance> {

    @Override
    public FlowInstance newEntity() {
        return new FlowInstance();
    }

    @Override
    public Page<FlowInstance> selectPage(FlowInstance entity, Page<FlowInstance> page) {
        return null;
    }

    @Override
    protected String getTableName() {
        return "flow_instance";
    }

    @Override
    public List<FlowInstance> getByDefIds(List<Long> defIds) {
        FlowInstance entity = TenantDeleteUtil.getEntity(newEntity());
        StringBuilder sql = new StringBuilder("select * from ").append(getTableName());
        sql.append(" where definition_id in (").append(buildInPlaceholders(defIds.size())).append(")");
        List<Object> params = new ArrayList<>(defIds);
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
}
