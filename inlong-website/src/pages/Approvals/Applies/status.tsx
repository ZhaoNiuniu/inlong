/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import React from 'react';
import StatusTag, { StatusTagProps } from '@/components/StatusTag';
import { CloseCircleFilled } from '@/components/Icons';

type StatusProp = {
  label: string;
  value: string | number;
  type: StatusTagProps['type'];
  icon?: StatusTagProps['icon'];
};

export const statusList: StatusProp[] = [
  {
    label: '已完成',
    value: 'COMPLETED',
    type: 'success',
  },
  {
    label: '待审批',
    value: 'PROCESSING',
    type: 'warning',
  },
  {
    label: '已驳回',
    value: 'REJECTED',
    type: 'error',
  },
  {
    label: '已取消',
    value: 'CANCELED',
    type: 'default',
    icon: <CloseCircleFilled />,
  },
  {
    label: '已终止',
    value: 'TERMINATED',
    type: 'error',
  },
];

export const statusMap = statusList.reduce(
  (acc, cur) => ({
    ...acc,
    [cur.value]: cur,
  }),
  {},
);

export const genStatusTag = (value: StatusProp['value']) => {
  const item = statusMap[value] || {};

  return <StatusTag type={item.type || 'default'} title={item.label || value} icon={item.icon} />;
};
