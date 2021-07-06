/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.dataproxy.demo;

import org.apache.inlong.dataproxy.DefaultMessageSender;
import org.apache.inlong.dataproxy.FileCallback;
import org.apache.inlong.dataproxy.SendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyMessageCallBack extends FileCallback {
    private static final Logger logger = LoggerFactory
            .getLogger(MyMessageCallBack.class);
    private DefaultMessageSender messageSender = null;
    private Event event = null;

    public MyMessageCallBack() {

    }

    public MyMessageCallBack(DefaultMessageSender messageSender, Event event) {
        super();
        this.messageSender = messageSender;
        this.event = event;
    }

    public void onMessageAck(String result) {
        logger.info("onMessageAck return result = {}", result);
    }


    public void onMessageAck(SendResult result) {
        if (result == SendResult.OK) {
            logger.info("onMessageAck return Ok");
        } else {
            logger.info("onMessageAck return failure = {}", result);
        }
    }

    public void onException(Throwable e) {
        logger.error("Send message failure, error {}", e.getMessage());
        e.printStackTrace();
    }

}
