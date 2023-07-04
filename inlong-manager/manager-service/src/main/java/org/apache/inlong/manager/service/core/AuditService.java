/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.service.core;

import org.apache.inlong.manager.dao.entity.AuditQuerySourceConfigEntity;
import org.apache.inlong.manager.pojo.audit.AuditRequest;
import org.apache.inlong.manager.pojo.audit.AuditVO;

import java.util.List;

/**
 * The service interface for audit.
 */
public interface AuditService {

    /**
     * Query audit data for list by condition
     *
     * @param request The audit request of query condition
     * @return The result of query
     */
    List<AuditVO> listByCondition(AuditRequest request) throws Exception;

    /**
     * Get audit id by type and isSent.
     *
     * @param type audit type.
     * @param isSent Whether to receive or send
     * @return Audit id.
     */
    String getAuditId(String type, boolean isSent);

    /**
     * Refresh the base item of audit cache.
     *
     * @return true if not exception, or false if it has exception
     */
    Boolean refreshBaseItemCache();

    /**
     * Insert a new source and make the source in use.
     * @param auditQuerySource MYSQL, CLICKHOUSE, ELASTICSEARCH
     * @param hosts
     * @param userName
     * @param password
     * @param esAuthEnable a param related to Elasticsearch which can be written as any integer when writing the ClickHouse source
     * @return
     */
    Boolean updateAuditQuerySource(String auditQuerySource, String hosts, String userName, String password,
            Integer esAuthEnable);

    /**
     * Insert a new source.
     * @param auditQuerySource MYSQL, CLICKHOUSE, ELASTICSEARCH
     * @param hosts
     * @param userName
     * @param password
     * @param esAuthEnable a param related to Elasticsearch which can be written as any integer when writing the ClickHouse source
     * @return
     */
    Boolean insertAuditSource(String auditQuerySource, String hosts, String userName, String password,
            Integer esAuthEnable);

    /**
     * Make a source in use according its hosts, only one source can be in use in one time.
     * @param hosts
     * @return
     */
    Boolean updateSourceByHosts(String hosts);

    /**
     * Query which source is in use.
     * @return
     */
    AuditQuerySourceConfigEntity queryInUse();
}