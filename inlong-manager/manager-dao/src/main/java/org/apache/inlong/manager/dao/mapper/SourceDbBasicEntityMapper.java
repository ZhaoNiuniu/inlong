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

package org.apache.inlong.manager.dao.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.inlong.manager.dao.entity.SourceDbBasicEntity;
import org.springframework.stereotype.Repository;

@Repository
public interface SourceDbBasicEntityMapper {

    int deleteByPrimaryKey(Integer id);

    int insert(SourceDbBasicEntity record);

    int insertSelective(SourceDbBasicEntity record);

    SourceDbBasicEntity selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(SourceDbBasicEntity record);

    int updateByPrimaryKey(SourceDbBasicEntity record);

    SourceDbBasicEntity selectByIdentifier(@Param("bid") String bid, @Param("dsid") String dsid);

    /**
     * According to the business identifier and data stream identifier,
     * physically delete the basic information of the DB data source
     *
     * @return rows deleted
     */
    int deleteByIdentifier(@Param("bid") String bid, @Param("dsid") String dsid);

    /**
     * According to the business identifier and data stream identifier,
     * logically delete the basic information of the DB data source
     *
     * @return rows deleted
     */
    int logicDeleteByIdentifier(@Param("bid") String bid, @Param("dsid") String dsid,
            @Param("operator") String operator);

}