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
package org.dromara.warm.flow.orm.utils;

import org.dromara.warm.flow.core.config.WarmFlow;
import org.dromara.warm.flow.core.utils.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

public class CommonUtil {

    private CommonUtil() {
    }

    public static void setDataSourceType(WarmFlow flowConfig, DataSource dataSource) {
        String dataSourceType = flowConfig.getDataSourceType();
        // 未配置时尝试获取可用数据库类型
        if (StringUtils.isEmpty(dataSourceType)) {
            DatabaseMetaData metaData;
            Connection connection = null;
            try {
                connection = dataSource.getConnection();
                metaData = connection.getMetaData();
                dataSourceType = metaData.getDatabaseProductName().toLowerCase();
            } catch (Exception e) {
                // 不能因为一个字段的取值, 影响到框架自身运行环境
            } finally {
                try {
                    if (connection != null) {
                        connection.close();
                    }
                } catch (Exception e) {
                    // 不能因为一个字段的取值, 影响到框架自身运行环境
                }
            }
        }

        // 兜底数据库类型
        if (StringUtils.isEmpty(dataSourceType)) {
            dataSourceType = "mysql";
        }
        flowConfig.setDataSourceType(dataSourceType);
    }
}
