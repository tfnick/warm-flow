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
import org.dromara.warm.flow.core.orm.dao.FlowHisTaskDao;
import org.dromara.warm.flow.core.utils.StringUtils;
import org.dromara.warm.flow.core.utils.page.Page;
import org.dromara.warm.flow.orm.entity.FlowHisTask;
import org.dromara.warm.flow.orm.utils.TenantDeleteUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 历史任务记录Dao接口
 *
 * @author warm
 * @since 2023-03-29
 */
public class FlowHisTaskDaoImpl extends WarmDaoImpl<FlowHisTask> implements FlowHisTaskDao<FlowHisTask> {

    @Override
    public FlowHisTask newEntity() {
        return new FlowHisTask();
    }

    @Override
    public Page<FlowHisTask> selectPage(FlowHisTask entity, Page<FlowHisTask> page) {
        return null;
    }

    @Override
    protected String getTableName() {
        return "flow_his_task";
    }

    @Override
    public List<FlowHisTask> getNoReject(Long instanceId) {
        FlowHisTask entity = TenantDeleteUtil.getEntity(newEntity());
        StringBuilder sql = new StringBuilder("select * from ").append(getTableName());
        sql.append(" where instance_id = ? and skip_type = 'PASS'");
        List<Object> params = new ArrayList<>();
        params.add(instanceId);
        if (StringUtils.isNotEmpty(entity.getDelFlag())) {
            sql.append(" and del_flag = ?");
            params.add(entity.getDelFlag());
        }
        if (StringUtils.isNotEmpty(entity.getTenantId())) {
            sql.append(" and tenant_id = ?");
            params.add(entity.getTenantId());
        }
        sql.append(" order by create_time desc");
        return findBySql(sql.toString(), params.toArray());
    }

    @Override
    public List<FlowHisTask> getByInsAndNodeCodes(Long instanceId, List<String> nodeCodes) {
        FlowHisTask entity = TenantDeleteUtil.getEntity(newEntity());
        StringBuilder sql = new StringBuilder("select * from ").append(getTableName());
        sql.append(" where instance_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(instanceId);
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
        sql.append(" order by create_time desc");
        return findBySql(sql.toString(), params.toArray());
    }

    @Override
    public int deleteByInsIds(List<Long> instanceIds) {
        FlowHisTask entity = TenantDeleteUtil.getEntity(newEntity());
        if (StringUtils.isNotEmpty(entity.getDelFlag())) {
            return executeLogicDeleteByColumn("instance_id", instanceIds, entity);
        }
        return deleteByColumn("instance_id", instanceIds, entity);
    }

    @Override
    public List<FlowHisTask> listByTaskIdAndCooperateTypes(Long taskId, Integer[] cooperateTypes) {
        FlowHisTask entity = TenantDeleteUtil.getEntity(newEntity());
        entity.setTaskId(taskId);
        StringBuilder sql = new StringBuilder("select * from ").append(getTableName());
        sql.append(" where task_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(taskId);
        if (cooperateTypes != null && cooperateTypes.length > 0) {
            sql.append(" and cooperate_type in (").append(buildInPlaceholders(cooperateTypes.length)).append(")");
            params.addAll(Arrays.asList(cooperateTypes));
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
}
