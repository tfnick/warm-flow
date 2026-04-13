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

import org.dromara.warm.flow.core.orm.dao.FlowFormDao;
import org.dromara.warm.flow.core.utils.page.Page;
import org.dromara.warm.flow.orm.entity.FlowForm;
import org.dromara.warm.flow.orm.utils.TenantDeleteUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author vanlin
 * @className FlowFormDaoImpl
 * @description
 * @since 2024/8/19 14:29
 */
public class FlowFormDaoImpl extends WarmDaoImpl<FlowForm> implements FlowFormDao<FlowForm> {

    @Override
    public FlowForm newEntity() {
        return new FlowForm();
    }

    @Override
    public Page<FlowForm> selectPage(FlowForm entity, Page<FlowForm> page) {
        return null;
    }

    @Override
    protected String getTableName() {
        return "flow_form";
    }

    @Override
    public List<FlowForm> queryByCodeList(List<String> formCodeList) {
        FlowForm entity = TenantDeleteUtil.getEntity(newEntity());
        StringBuilder sql = new StringBuilder("select * from ").append(getTableName());
        sql.append(" where form_code in (").append(buildInPlaceholders(formCodeList.size())).append(")");
        List<Object> params = new ArrayList<>(formCodeList);
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
}
