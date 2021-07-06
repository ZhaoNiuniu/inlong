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

package org.apache.inlong.sort.util;

/**
 * The Count based sampler.
 */
public class CountBasedSampler implements Sampler {

    private static final long serialVersionUID = 7829692577372380499L;

    private final int samplingInTimes;

    private final boolean samplingDisabled;

    private int counter = 0;

    public CountBasedSampler(int samplingInTimes) {
        this.samplingInTimes = samplingInTimes;
        samplingDisabled = samplingInTimes < 2;
    }

    @Override
    public boolean collect() {
        if (samplingDisabled) {
            return true;
        }

        if (++counter == samplingInTimes) {
            counter = 0;
            return true;
        } else {
            return false;
        }
    }
}
