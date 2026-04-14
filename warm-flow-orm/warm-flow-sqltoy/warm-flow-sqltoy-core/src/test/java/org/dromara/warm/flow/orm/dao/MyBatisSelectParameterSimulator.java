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

import java.lang.reflect.Field;
import java.util.*;

/**
 * 模拟 MyBatis XML select_parameter 的条件判断逻辑
 * <p>
 * MyBatis XML 中 String 类型字段使用: field != null and field != ''
 * 非 String 类型字段使用: field != null
 * <p>
 * 此类通过反射读取实体字段，复现 MyBatis Mapper XML 中的条件判断行为，
 * 用于与 SqlToy 的 buildWhereClause 进行一致性对比。
 */
public class MyBatisSelectParameterSimulator {

    /**
     * 排除的字段列表（与 WarmDaoImpl.EXCLUDED_FIELDS 保持一致）
     */
    private static final Set<String> EXCLUDED_FIELDS = new HashSet<>(Arrays.asList(
        "skipList", "nodeList", "userList", "permissionList"
    ));

    /**
     * 模拟 MyBatis select_parameter 逻辑，返回 WHERE 子句中的条件列名集合
     *
     * @param entity 实体对象
     * @return 会被纳入 WHERE 条件的列名列表
     */
    public static List<String> simulateConditions(Object entity) {
        List<String> columns = new ArrayList<>();
        Field[] fields = entity.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            String fieldName = field.getName();
            if (EXCLUDED_FIELDS.contains(fieldName)) {
                continue;
            }
            try {
                Object value = field.get(entity);
                if (shouldIncludeCondition(value)) {
                    columns.add(camelToSnake(fieldName));
                }
            } catch (IllegalAccessException ignored) {
            }
        }
        return columns;
    }

    /**
     * 复现 MyBatis XML 中的条件判断逻辑:
     * - String 类型: value != null && !value.isEmpty()
     * - 其他类型:   value != null
     */
    private static boolean shouldIncludeCondition(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof String) {
            return !((String) value).isEmpty();
        }
        return true;
    }

    /**
     * 模拟 SqlToy buildWhereClause 逻辑（仅检查 != null，不检查空字符串）
     *
     * @param entity 实体对象
     * @return 会被纳入 WHERE 条件的列名列表
     */
    public static List<String> simulateSqlToyConditions(Object entity) {
        List<String> columns = new ArrayList<>();
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
                    columns.add(camelToSnake(fieldName));
                }
            } catch (IllegalAccessException ignored) {
            }
        }
        return columns;
    }

    private static String camelToSnake(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
}
