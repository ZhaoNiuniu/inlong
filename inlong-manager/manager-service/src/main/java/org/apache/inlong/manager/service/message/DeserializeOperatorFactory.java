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

package org.apache.inlong.manager.service.message;

import org.apache.inlong.common.enums.DataProxyMsgEncType;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Factory for {@link DeserializeOperator}.
 */
@Service
public class DeserializeOperatorFactory {

    @Autowired
    private List<DeserializeOperator> operatorList;

    /**
     * Get a message queue resource operator instance via the given mqType
     */
    public DeserializeOperator getInstance(DataProxyMsgEncType type) {
        return operatorList.stream()
                .filter(inst -> inst.accept(type))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCodeEnum.MQ_TYPE_NOT_SUPPORT,
                        String.format(ErrorCodeEnum.MQ_TYPE_NOT_SUPPORT.getMessage(), type)));
    }

}
