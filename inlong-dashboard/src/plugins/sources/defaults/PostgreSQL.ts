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

import { DataWithBackend } from '@/plugins/DataWithBackend';
import { RenderRow } from '@/plugins/RenderRow';
import { RenderList } from '@/plugins/RenderList';
import { SourceInfo } from '../common/SourceInfo';
import i18n from '@/i18n';

const { I18n } = DataWithBackend;
const { FieldDecorator, SyncField } = RenderRow;
const { ColumnDecorator } = RenderList;

export default class PostgreSQLSource
  extends SourceInfo
  implements DataWithBackend, RenderRow, RenderList
{
  @FieldDecorator({
    type: 'input',
    rules: [{ required: true }],
    props: values => ({
      disabled: values?.status === 101,
    }),
  })
  @ColumnDecorator()
  @SyncField()
  @I18n('meta.Sources.PostgreSQL.Hostname')
  hostname: string;

  @FieldDecorator({
    type: 'inputnumber',
    rules: [{ required: true }],
    initialValue: 5432,
    props: values => ({
      min: 1,
      max: 65535,
      disabled: values?.status === 101,
    }),
  })
  @ColumnDecorator()
  @SyncField()
  @I18n('meta.Sources.PostgreSQL.Port')
  port: number;

  @FieldDecorator({
    type: 'input',
    rules: [{ required: true }],
    props: values => ({
      disabled: values?.status === 101,
    }),
  })
  @SyncField()
  @I18n('meta.Sources.PostgreSQL.Database')
  database: string;

  @FieldDecorator({
    type: 'input',
    rules: [{ required: true }],
    props: values => ({
      disabled: values?.status === 101,
    }),
  })
  @SyncField()
  @I18n('meta.Sources.PostgreSQL.SchemaName')
  schema: string;

  @FieldDecorator({
    type: 'input',
    rules: [{ required: true }],
    props: values => ({
      disabled: values?.status === 101,
    }),
  })
  @SyncField()
  @ColumnDecorator()
  @I18n('meta.Sources.PostgreSQL.Username')
  username: string;

  @FieldDecorator({
    type: 'password',
    rules: [{ required: true }],
    props: values => ({
      disabled: values?.status === 101,
    }),
  })
  @SyncField()
  @I18n('meta.Sources.PostgreSQL.Password')
  password: string;

  @FieldDecorator({
    type: 'input',
    rules: [{ required: true }],
    tooltip: i18n.t('meta.Sources.PostgreSQL.TableNameHelp'),
    props: values => ({
      disabled: values?.status === 101,
    }),
  })
  @SyncField()
  @I18n('meta.Sources.PostgreSQL.TableName')
  tableNameList: string;

  @FieldDecorator({
    type: 'input',
    props: values => ({
      disabled: values?.status === 101,
    }),
  })
  @SyncField()
  @I18n('meta.Sources.PostgreSQL.PrimaryKey')
  primaryKey: string;

  @FieldDecorator({
    type: 'select',
    initialValue: 'decoderbufs',
    props: values => ({
      disabled: values?.status === 101,
      options: [
        {
          label: 'decoderbufs',
          value: 'decoderbufs',
        },
        {
          label: 'wal2json',
          value: 'wal2json',
        },
        {
          label: 'wal2json_rds',
          value: 'wal2json_rds',
        },
        {
          label: 'wal2json_streaming',
          value: 'wal2json_streaming',
        },
        {
          label: 'wal2json_rds_streaming',
          value: 'wal2json_rds_streaming',
        },
        {
          label: 'pgoutput',
          value: 'pgoutput',
        },
      ],
    }),
  })
  @SyncField()
  @I18n('meta.Sources.PostgreSQL.decodingPluginName')
  decodingPluginName: string;

  parse(data) {
    let obj = { ...data };
    obj.tableNameList = obj.tableNameList.join(',');
    return obj;
  }

  stringify(data) {
    let obj = { ...data };
    if (typeof obj.tableNameList === 'string') {
      obj.tableNameList = obj.tableNameList.split(',');
    }
    return obj;
  }
}
