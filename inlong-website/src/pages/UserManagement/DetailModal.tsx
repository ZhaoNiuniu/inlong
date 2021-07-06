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
import { Modal, message } from 'antd';
import { ModalProps } from 'antd/es/modal';
import FormGenerator, { useForm } from '@/components/FormGenerator';
import { useUpdateEffect, useRequest } from '@/hooks';
import request from '@/utils/request';

export interface Props extends ModalProps {
  id?: number;
}

const content = [
  {
    type: 'radio',
    label: '帐号类型',
    name: 'type',
    initialValue: 1,
    rules: [{ required: true }],
    props: {
      options: [
        {
          label: '普通用户',
          value: 1,
        },
        {
          label: '系统管理员',
          value: 0,
        },
      ],
    },
  },
  {
    type: 'input',
    label: '用户名称',
    name: 'username',
    rules: [{ required: true }],
  },
  {
    type: 'password',
    label: '用户密码',
    name: 'password',
    rules: [{ required: true }],
  },
  {
    type: 'inputnumber',
    label: '有效时长',
    name: 'validDays',
    suffix: '天',
    rules: [{ required: true }],
    props: {
      min: 1,
    },
  },
];

const Comp: React.FC<Props> = ({ id, ...modalProps }) => {
  const [form] = useForm();

  const { run: getData } = useRequest(
    id => ({
      url: `/user/get/${id}`,
    }),
    {
      manual: true,
      onSuccess: result => {
        form.setFieldsValue(result);
      },
    },
  );

  const onOk = async () => {
    const values = await form.validateFields();
    const isUpdate = id;
    if (isUpdate) {
      values.id = id;
    }
    await request({
      url: isUpdate ? '/user/update' : '/user/register',
      method: 'POST',
      data: values,
    });
    await modalProps?.onOk(values);
    message.success('保存成功');
  };

  useUpdateEffect(() => {
    if (modalProps.visible) {
      // open
      id ? getData(id) : form.resetFields();
    }
  }, [modalProps.visible]);

  return (
    <Modal {...modalProps} title={`${id ? '编辑' : '新建'}用户`} onOk={onOk}>
      <FormGenerator
        content={id ? content.filter(item => item.name !== 'password') : content}
        form={form}
        useMaxWidth
      />
    </Modal>
  );
};

export default Comp;
