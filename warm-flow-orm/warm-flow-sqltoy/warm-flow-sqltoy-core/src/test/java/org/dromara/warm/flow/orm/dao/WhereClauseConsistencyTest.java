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

import org.dromara.warm.flow.orm.entity.FlowDefinition;
import org.dromara.warm.flow.orm.entity.FlowHisTask;
import org.dromara.warm.flow.orm.entity.FlowTask;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * MyBatis select_parameter 与 SqlToy buildWhereClause 一致性测试
 * <p>
 * 核心发现: MyBatis XML 对 String 类型字段额外检查 != ''（空字符串不作为查询条件），
 * 而 SqlToy 的 buildWhereClause 仅检查 != null（空字符串会被作为查询条件）。
 * 这导致两者在 String 字段为空字符串时行为不一致。
 * <p>
 * 测试策略:
 * - 场景1: 所有字段为 null → 两者行为一致
 * - 场景2: 仅设置数值/日期字段 → 两者行为一致
 * - 场景3: 仅设置有效 String 字段 → 两者行为一致
 * - 场景4: String 字段为空字符串 "" → 行为不一致（关键差异）
 * - 场景5: 混合场景 → 部分一致
 * - 场景6: 三态测试 null/""/valid → 精确确认差异范围
 */
public class WhereClauseConsistencyTest {

    // ========== FlowHisTask 一致性测试 ==========

    @Test
    public void testFlowHisTask_allNull_consistent() {
        FlowHisTask entity = new FlowHisTask();

        List<String> mybatisCols = MyBatisSelectParameterSimulator.simulateConditions(entity);
        List<String> sqltoyCols = MyBatisSelectParameterSimulator.simulateSqlToyConditions(entity);

        assertTrue("MyBatis: all null should produce no conditions", mybatisCols.isEmpty());
        assertTrue("SqlToy: all null should produce no conditions", sqltoyCols.isEmpty());
    }

    @Test
    public void testFlowHisTask_numericOnly_consistent() {
        FlowHisTask entity = new FlowHisTask()
            .setId(1L)
            .setDefinitionId(100L)
            .setInstanceId(200L)
            .setTaskId(300L)
            .setCooperateType(1)
            .setNodeType(0);

        List<String> mybatisCols = MyBatisSelectParameterSimulator.simulateConditions(entity);
        List<String> sqltoyCols = MyBatisSelectParameterSimulator.simulateSqlToyConditions(entity);

        assertEquals("Numeric-only: both should produce same columns",
            sorted(mybatisCols), sorted(sqltoyCols));
        // 预期: id, definition_id, instance_id, task_id, cooperate_type, node_type
        assertEquals(6, mybatisCols.size());
    }

    @Test
    public void testFlowHisTask_validStrings_consistent() {
        FlowHisTask entity = new FlowHisTask()
            .setNodeCode("node1")
            .setNodeName("审批节点")
            .setApprover("admin")
            .setFlowStatus("1");

        List<String> mybatisCols = MyBatisSelectParameterSimulator.simulateConditions(entity);
        List<String> sqltoyCols = MyBatisSelectParameterSimulator.simulateSqlToyConditions(entity);

        assertEquals("Valid strings: both should produce same columns",
            sorted(mybatisCols), sorted(sqltoyCols));
        assertEquals(4, mybatisCols.size());
    }

    @Test
    public void testFlowHisTask_emptyStrings_inconsistent() {
        FlowHisTask entity = new FlowHisTask()
            .setNodeCode("")
            .setNodeName("")
            .setApprover("")
            .setFlowStatus("");

        List<String> mybatisCols = MyBatisSelectParameterSimulator.simulateConditions(entity);
        List<String> sqltoyCols = MyBatisSelectParameterSimulator.simulateSqlToyConditions(entity);

        // MyBatis: 空字符串被过滤 → 无条件
        assertTrue("MyBatis: empty strings should produce no conditions", mybatisCols.isEmpty());
        // SqlToy: 空字符串 != null → 生成条件
        assertEquals("SqlToy: empty strings should produce conditions", 4, sqltoyCols.size());

        // 明确验证不一致
        assertNotEquals("CRITICAL: empty string handling differs between MyBatis and SqlToy",
            sorted(mybatisCols), sorted(sqltoyCols));
    }

    @Test
    public void testFlowHisTask_mixedScenario() {
        FlowHisTask entity = new FlowHisTask()
            .setId(1L)
            .setNodeCode("node1")        // 有效String
            .setNodeName("")             // 空String
            .setApprover(null)           // null
            .setCooperateType(1)         // 数值
            .setCreateTime(new Date());  // 日期

        List<String> mybatisCols = MyBatisSelectParameterSimulator.simulateConditions(entity);
        List<String> sqltoyCols = MyBatisSelectParameterSimulator.simulateSqlToyConditions(entity);

        // MyBatis: id, node_code, cooperate_type, create_time (nodeName=""被过滤)
        assertEquals("MyBatis mixed: should filter empty strings", 4, mybatisCols.size());
        assertFalse("MyBatis should not include empty string field",
            mybatisCols.contains("node_name"));

        // SqlToy: id, node_code, node_name, cooperate_type, create_time (nodeName=""不被过滤)
        assertEquals("SqlToy mixed: should include empty strings", 5, sqltoyCols.size());
        assertTrue("SqlToy should include empty string field",
            sqltoyCols.contains("node_name"));
    }

    @Test
    public void testFlowHisTask_stringTriState() {
        // 三态测试: null / "" / "valid"
        // 使用三个不同的实体分别测试
        FlowHisTask nullEntity = new FlowHisTask().setNodeCode(null);
        FlowHisTask emptyEntity = new FlowHisTask().setNodeCode("");
        FlowHisTask validEntity = new FlowHisTask().setNodeCode("valid");

        // MyBatis: null → 无条件, "" → 无条件, "valid" → 有条件
        assertTrue("MyBatis: null string → no condition",
            MyBatisSelectParameterSimulator.simulateConditions(nullEntity).isEmpty());
        assertTrue("MyBatis: empty string → no condition",
            MyBatisSelectParameterSimulator.simulateConditions(emptyEntity).isEmpty());
        assertEquals("MyBatis: valid string → has condition", 1,
            MyBatisSelectParameterSimulator.simulateConditions(validEntity).size());

        // SqlToy: null → 无条件, "" → 有条件, "valid" → 有条件
        assertTrue("SqlToy: null string → no condition",
            MyBatisSelectParameterSimulator.simulateSqlToyConditions(nullEntity).isEmpty());
        assertEquals("SqlToy: empty string → has condition", 1,
            MyBatisSelectParameterSimulator.simulateSqlToyConditions(emptyEntity).size());
        assertEquals("SqlToy: valid string → has condition", 1,
            MyBatisSelectParameterSimulator.simulateSqlToyConditions(validEntity).size());
    }

    // ========== FlowDefinition 一致性测试 ==========

    @Test
    public void testFlowDefinition_emptyStrings_inconsistent() {
        FlowDefinition entity = new FlowDefinition()
            .setFlowCode("")
            .setFlowName("")
            .setVersion("")
            .setDelFlag("")
            .setTenantId("");

        List<String> mybatisCols = MyBatisSelectParameterSimulator.simulateConditions(entity);
        List<String> sqltoyCols = MyBatisSelectParameterSimulator.simulateSqlToyConditions(entity);

        assertTrue("MyBatis: empty strings filtered", mybatisCols.isEmpty());
        assertEquals("SqlToy: empty strings NOT filtered", 5, sqltoyCols.size());

        assertNotEquals("FlowDefinition: empty string handling differs",
            sorted(mybatisCols), sorted(sqltoyCols));
    }

    @Test
    public void testFlowDefinition_listFields_excluded() {
        FlowDefinition entity = new FlowDefinition();
        // nodeList 和 userList 默认初始化为 new ArrayList<>()，非 null
        // 但它们在 EXCLUDED_FIELDS 中

        List<String> mybatisCols = MyBatisSelectParameterSimulator.simulateConditions(entity);
        List<String> sqltoyCols = MyBatisSelectParameterSimulator.simulateSqlToyConditions(entity);

        // nodeList/userList 被排除，不应出现在条件中
        assertFalse("Should not contain node_list", mybatisCols.contains("node_list"));
        assertFalse("Should not contain user_list", mybatisCols.contains("user_list"));
        assertFalse("Should not contain node_list", sqltoyCols.contains("node_list"));
        assertFalse("Should not contain user_list", sqltoyCols.contains("user_list"));
    }

    // ========== FlowTask 一致性测试 ==========

    @Test
    public void testFlowTask_emptyStrings_inconsistent() {
        FlowTask entity = new FlowTask()
            .setNodeCode("")
            .setNodeName("")
            .setFlowStatus("")
            .setCreateBy("")
            .setTenantId("");

        List<String> mybatisCols = MyBatisSelectParameterSimulator.simulateConditions(entity);
        List<String> sqltoyCols = MyBatisSelectParameterSimulator.simulateSqlToyConditions(entity);

        assertTrue("MyBatis: empty strings filtered", mybatisCols.isEmpty());
        assertEquals("SqlToy: empty strings NOT filtered", 5, sqltoyCols.size());

        assertNotEquals("FlowTask: empty string handling differs",
            sorted(mybatisCols), sorted(sqltoyCols));
    }

    // ========== 全字段 String 空值一致性对比 ==========

    @Test
    public void testFlowHisTask_allStringFieldsEmpty_identifyDifference() {
        FlowHisTask entity = new FlowHisTask()
            .setTenantId("")
            .setDelFlag("")
            .setFlowName("")
            .setBusinessId("")
            .setNodeCode("")
            .setNodeName("")
            .setTargetNodeCode("")
            .setTargetNodeName("")
            .setApprover("")
            .setCollaborator("")
            .setSkipType("")
            .setFlowStatus("")
            .setMessage("")
            .setVariable("")
            .setExt("")
            .setFormCustom("")
            .setFormPath("");

        List<String> mybatisCols = MyBatisSelectParameterSimulator.simulateConditions(entity);
        List<String> sqltoyCols = MyBatisSelectParameterSimulator.simulateSqlToyConditions(entity);

        // 所有 String 字段设置为空字符串
        assertTrue("MyBatis: all empty strings → 0 conditions", mybatisCols.isEmpty());
        assertEquals("SqlToy: all empty strings → 17 conditions (all String fields)",
            17, sqltoyCols.size());

        System.out.println("=== FlowHisTask 全字段空字符串差异 ===");
        System.out.println("MyBatis 条件列: " + mybatisCols);
        System.out.println("SqlToy 条件列: " + sqltoyCols);
        System.out.println("差异列: " + new HashSet<>(sqltoyCols).stream()
            .filter(c -> !mybatisCols.contains(c))
            .collect(Collectors.joining(", ")));
    }

    @Test
    public void testFlowDefinition_allStringFieldsEmpty_identifyDifference() {
        FlowDefinition entity = new FlowDefinition()
            .setCreateBy("")
            .setUpdateBy("")
            .setTenantId("")
            .setDelFlag("")
            .setFlowCode("")
            .setFlowName("")
            .setModelValue("")
            .setCategory("")
            .setVersion("")
            .setFormCustom("")
            .setFormPath("")
            .setListenerType("")
            .setListenerPath("")
            .setExt("");

        List<String> mybatisCols = MyBatisSelectParameterSimulator.simulateConditions(entity);
        List<String> sqltoyCols = MyBatisSelectParameterSimulator.simulateSqlToyConditions(entity);

        assertTrue("MyBatis: all empty strings → 0 conditions", mybatisCols.isEmpty());
        assertEquals("SqlToy: all empty strings → 14 conditions", 14, sqltoyCols.size());
    }

    @Test
    public void testFlowTask_allStringFieldsEmpty_identifyDifference() {
        FlowTask entity = new FlowTask()
            .setCreateBy("")
            .setUpdateBy("")
            .setTenantId("")
            .setDelFlag("")
            .setFlowName("")
            .setBusinessId("")
            .setNodeCode("")
            .setNodeName("")
            .setFlowStatus("")
            .setFormCustom("")
            .setFormPath("");

        List<String> mybatisCols = MyBatisSelectParameterSimulator.simulateConditions(entity);
        List<String> sqltoyCols = MyBatisSelectParameterSimulator.simulateSqlToyConditions(entity);

        assertTrue("MyBatis: all empty strings → 0 conditions", mybatisCols.isEmpty());
        assertEquals("SqlToy: all empty strings → 11 conditions", 11, sqltoyCols.size());
    }

    // ========== 辅助方法 ==========

    private Set<String> sorted(List<String> list) {
        return new HashSet<>(list);
    }
}
