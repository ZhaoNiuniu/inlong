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

package org.apache.inlong.dataproxy.config.holder;

import org.apache.inlong.common.constant.Constants;
import org.apache.inlong.common.pojo.dataproxy.CacheClusterObject;
import org.apache.inlong.common.pojo.dataproxy.CacheClusterSetObject;
import org.apache.inlong.common.pojo.dataproxy.DataProxyCluster;
import org.apache.inlong.common.pojo.dataproxy.DataProxyConfigResponse;
import org.apache.inlong.common.pojo.dataproxy.InLongIdObject;
import org.apache.inlong.common.pojo.dataproxy.ProxyClusterObject;
import org.apache.inlong.dataproxy.config.CommonConfigHolder;
import org.apache.inlong.dataproxy.config.ConfigHolder;
import org.apache.inlong.dataproxy.config.ConfigManager;
import org.apache.inlong.dataproxy.config.pojo.CacheClusterConfig;
import org.apache.inlong.dataproxy.config.pojo.CacheType;
import org.apache.inlong.dataproxy.config.pojo.DataType;
import org.apache.inlong.dataproxy.config.pojo.IdTopicConfig;
import org.apache.inlong.dataproxy.consts.ConfigConstants;
import org.apache.inlong.sdk.commons.protocol.InlongId;

import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Json to object
 */
public class MetaConfigHolder extends ConfigHolder {

    private static final String metaConfigFileName = "metadata.json";
    private static final int MAX_ALLOWED_JSON_FILE_SIZE = 300 * 1024 * 1024;
    private static final Logger LOG = LoggerFactory.getLogger(MetaConfigHolder.class);
    private static final Gson GSON = new Gson();
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    // meta data
    private String dataMd5 = "";
    private String dataStr = "";
    private final AtomicLong lastUpdVersion = new AtomicLong(0);
    private String tmpDataMd5 = "";
    private final AtomicLong lastSyncVersion = new AtomicLong(0);
    // cached data
    private final List<String> defTopics = new ArrayList<>();
    private final AtomicInteger clusterType = new AtomicInteger(CacheType.N.getId());
    private final ConcurrentHashMap<String, CacheClusterConfig> mqClusterMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, IdTopicConfig> id2TopicSrcMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, IdTopicConfig> id2TopicSinkMap = new ConcurrentHashMap<>();

    public MetaConfigHolder() {
        super(metaConfigFileName);
    }

    public void addDefTopic(String defTopic) {
        if (StringUtils.isBlank(defTopic)) {
            return;
        }
        defTopics.add(defTopic);
    }

    /**
     * get source topic by groupId and streamId
     */
    public String getSrcBaseTopicName(String groupId, String streamId) {
        IdTopicConfig idTopicConfig = getSrcIdTopicConfig(groupId, streamId);
        if (idTopicConfig == null) {
            return null;
        }
        return idTopicConfig.getTopicName();
    }

    public IdTopicConfig getSrcIdTopicConfig(String groupId, String streamId) {
        IdTopicConfig idTopicConfig = null;
        if (StringUtils.isNotEmpty(groupId) && !id2TopicSrcMap.isEmpty()) {
            idTopicConfig = id2TopicSrcMap.get(InlongId.generateUid(groupId, streamId));
            if (idTopicConfig == null) {
                idTopicConfig = id2TopicSrcMap.get(groupId);
            }
        }
        return idTopicConfig;
    }

    /**
     * get topic by groupId and streamId
     */
    public String getSourceTopicName(String groupId, String streamId) {
        String topic = null;
        if (StringUtils.isNotEmpty(groupId) && !id2TopicSrcMap.isEmpty()) {
            IdTopicConfig idTopicConfig = id2TopicSrcMap.get(InlongId.generateUid(groupId, streamId));
            if (idTopicConfig == null) {
                idTopicConfig = id2TopicSrcMap.get(groupId);
            }
            if (idTopicConfig != null) {
                topic = idTopicConfig.getTopicName();
            }
        }
        return topic;
    }

    public IdTopicConfig getSinkIdTopicConfig(String groupId, String streamId) {
        IdTopicConfig idTopicConfig = null;
        if (StringUtils.isNotEmpty(groupId) && !id2TopicSinkMap.isEmpty()) {
            idTopicConfig = id2TopicSinkMap.get(InlongId.generateUid(groupId, streamId));
            if (idTopicConfig == null) {
                idTopicConfig = id2TopicSinkMap.get(groupId);
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Get Sink Topic Config by groupId = {}, streamId = {}, IdTopicConfig = {}",
                    groupId, streamId, idTopicConfig);
        }
        return idTopicConfig;
    }

    public String getConfigMd5() {
        if (this.lastSyncVersion.get() > this.lastUpdVersion.get()) {
            return tmpDataMd5;
        } else {
            return dataMd5;
        }
    }

    public boolean updateConfigMap(String inDataMd5, String inDataJsonStr) {
        if (StringUtils.isBlank(inDataMd5)
                || StringUtils.isBlank(inDataJsonStr)) {
            return false;
        }
        synchronized (this.lastSyncVersion) {
            if (this.lastSyncVersion.get() > this.lastUpdVersion.get()) {
                if (inDataJsonStr.equals(tmpDataMd5)) {
                    return false;
                }
                LOG.info("Load changed metadata {} , but reloading content, over {} ms",
                        getFileName(), System.currentTimeMillis() - this.lastSyncVersion.get());
                return false;
            } else {
                if (inDataMd5.equals(dataMd5)) {
                    return false;
                }
            }
            return storeConfigToFile(inDataMd5, inDataJsonStr);
        }
    }

    public List<CacheClusterConfig> forkCachedCLusterConfig() {
        List<CacheClusterConfig> result = new ArrayList<>();
        if (mqClusterMap.isEmpty()) {
            return result;
        }
        for (Map.Entry<String, CacheClusterConfig> entry : mqClusterMap.entrySet()) {
            if (entry == null || entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            CacheClusterConfig config = new CacheClusterConfig();
            config.setClusterName(entry.getValue().getClusterName());
            config.setToken(entry.getValue().getToken());
            config.getParams().putAll(entry.getValue().getParams());
            result.add(config);
        }
        return result;
    }

    public Set<String> getAllTopicName() {
        Set<String> result = new HashSet<>();
        // add default topics first
        if (CommonConfigHolder.getInstance().isEnableUnConfigTopicAccept()) {
            result.addAll(CommonConfigHolder.getInstance().getDefTopics());
        }
        // add configured topics
        for (IdTopicConfig topicConfig : id2TopicSrcMap.values()) {
            if (topicConfig == null) {
                continue;
            }
            result.add(topicConfig.getTopicName());
        }
        return result;
    }

    @Override
    protected boolean loadFromFileToHolder() {
        // check meta update setting
        if (!CommonConfigHolder.getInstance().isEnableStartupUsingLocalMetaFile()
                && !ConfigManager.handshakeManagerOk.get()) {
            LOG.warn("Failed to load json config from {}, don't obtain metadata from the Manager,"
                    + " and the startup via the cache file is false", getFileName());
            return false;
        }
        String jsonString = "";
        readWriteLock.writeLock().lock();
        try {
            jsonString = loadConfigFromFile();
            if (StringUtils.isBlank(jsonString)) {
                LOG.warn("Load changed json {}, but no records configured", getFileName());
                return true;
            }
            DataProxyConfigResponse metaConfig =
                    GSON.fromJson(jsonString, DataProxyConfigResponse.class);
            // check result tag
            if (!metaConfig.isResult() || metaConfig.getErrCode() != DataProxyConfigResponse.SUCC) {
                LOG.warn("Load failed json config from {}, error code is {}",
                        getFileName(), metaConfig.getErrCode());
                return true;
            }
            // check cluster data
            DataProxyCluster clusterObj = metaConfig.getData();
            if (clusterObj == null) {
                LOG.warn("Load failed json config from {}, malformed content, data is null", getFileName());
                return true;
            }
            // update cache data
            if (updateCacheData(jsonString, metaConfig)) {
                LOG.info("Load changed {} file success!", getFileName());
            }
            return true;
        } catch (Throwable e) {
            LOG.warn("Process json {} changed data {} failure", getFileName(), jsonString, e);
            return false;
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    private boolean updateCacheData(String jsonString, DataProxyConfigResponse metaConfig) {
        // get and valid inlongIds configure
        ProxyClusterObject proxyClusterObject = metaConfig.getData().getProxyCluster();
        if (proxyClusterObject == null) {
            LOG.warn("Load failed json config from {}, malformed content, proxyCluster field is null",
                    getFileName());
            return false;
        }
        CacheClusterSetObject clusterSetObject = metaConfig.getData().getCacheClusterSet();
        if (clusterSetObject == null) {
            LOG.warn("Load failed json config from {}, malformed content, cacheClusterSet field is null",
                    getFileName());
            return false;
        }
        List<InLongIdObject> inLongIds = proxyClusterObject.getInlongIds();
        if (inLongIds == null) {
            LOG.warn("Load failed json config from {}, malformed content, inlongIds field is null",
                    getFileName());
            return false;
        }
        // get mq type
        CacheType mqType = CacheType.convert(clusterSetObject.getType());
        if (mqType == CacheType.N) {
            LOG.warn("Load failed json config from {}, unsupported mq type {}",
                    getFileName(), clusterSetObject.getType());
            return false;
        }
        // get mq cluster info
        Map<String, CacheClusterConfig> tmpClusterConfigMap = new HashMap<>();
        for (CacheClusterObject clusterObject : clusterSetObject.getCacheClusters()) {
            if (clusterObject == null || StringUtils.isBlank(clusterObject.getName())) {
                continue;
            }
            CacheClusterConfig config = new CacheClusterConfig();
            config.setClusterName(clusterObject.getName());
            config.setToken(clusterObject.getToken());
            config.getParams().putAll(clusterObject.getParams());
            tmpClusterConfigMap.put(config.getClusterName(), config);
        }
        if (tmpClusterConfigMap.isEmpty()) {
            LOG.warn("Load failed json config from {}, no valid {} mq cluster",
                    getFileName(), clusterSetObject.getType());
            return false;
        }
        // get topic config info
        Map<String, IdTopicConfig> tmpTopicConfigMap = buildCacheTopicConfig(mqType, inLongIds);
        replaceCacheConfig(mqType, tmpClusterConfigMap, tmpTopicConfigMap);
        // update cached data
        this.dataMd5 = metaConfig.getMd5();
        this.dataStr = jsonString;
        if (this.lastSyncVersion.get() == 0) {
            this.lastUpdVersion.set(System.currentTimeMillis());
            this.lastSyncVersion.compareAndSet(0, this.lastUpdVersion.get());
        } else {
            this.lastUpdVersion.set(this.lastSyncVersion.get());
        }
        return true;
    }

    private void replaceCacheConfig(CacheType cacheType,
            Map<String, CacheClusterConfig> clusterConfigMap,
            Map<String, IdTopicConfig> topicConfigMap) {
        this.clusterType.getAndSet(cacheType.getId());
        // remove deleted id2topic config
        Set<String> tmpKeys = new HashSet<>();
        for (Map.Entry<String, IdTopicConfig> entry : id2TopicSrcMap.entrySet()) {
            if (entry == null || entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            if (!topicConfigMap.containsKey(entry.getKey())) {
                tmpKeys.add(entry.getKey());
            }
        }
        for (String key : tmpKeys) {
            id2TopicSrcMap.remove(key);
        }
        // add new id2topic source config
        id2TopicSrcMap.putAll(topicConfigMap);
        // add new id2topic sink config
        id2TopicSinkMap.putAll(topicConfigMap);
        // remove deleted cluster config
        tmpKeys.clear();
        for (Map.Entry<String, CacheClusterConfig> entry : mqClusterMap.entrySet()) {
            if (entry == null || entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            if (!clusterConfigMap.containsKey(entry.getKey())) {
                tmpKeys.add(entry.getKey());
            }
        }
        for (String key : tmpKeys) {
            mqClusterMap.remove(key);
        }
        // add new mq cluster config
        mqClusterMap.putAll(clusterConfigMap);
    }

    private Map<String, IdTopicConfig> buildCacheTopicConfig(
            CacheType mqType, List<InLongIdObject> inLongIds) {
        Map<String, IdTopicConfig> tmpTopicConfigMap = new HashMap<>();
        if (inLongIds.isEmpty()) {
            return tmpTopicConfigMap;
        }
        int index;
        String[] idItems;
        String groupId;
        String streamId;
        String topicName;
        String tenant;
        String nameSpace;
        for (InLongIdObject idObject : inLongIds) {
            if (idObject == null
                    || StringUtils.isBlank(idObject.getInlongId())
                    || StringUtils.isBlank(idObject.getTopic())) {
                continue;
            }
            // parse inlong id
            idItems = idObject.getInlongId().split("\\.");
            if (idItems.length == 2) {
                if (StringUtils.isBlank(idItems[0])) {
                    continue;
                }
                groupId = idItems[0].trim();
                streamId = idItems[1].trim();
            } else {
                groupId = idObject.getInlongId().trim();
                streamId = "";
            }
            topicName = idObject.getTopic().trim();
            // change full topic name "pulsar-xxx/test/base_topic_name" to
            // base topic name "base_topic_name"
            index = topicName.lastIndexOf('/');
            if (index >= 0) {
                topicName = topicName.substring(index + 1).trim();
            }
            tenant = idObject.getParams().getOrDefault(ConfigConstants.KEY_TENANT, "");
            nameSpace = idObject.getParams().getOrDefault(ConfigConstants.KEY_NAMESPACE, "");
            if (StringUtils.isBlank(idObject.getTopic())) {
                // namespace field must exist and value not be empty,
                // otherwise it is an illegal configuration item.
                continue;
            }
            if (mqType.equals(CacheType.TUBE)) {
                topicName = nameSpace;
            } else if (mqType.equals(CacheType.KAFKA)) {
                if (topicName.equals(streamId)) {
                    topicName = String.format(Constants.DEFAULT_KAFKA_TOPIC_FORMAT, nameSpace, topicName);
                }
            }
            IdTopicConfig tmpConfig = new IdTopicConfig();
            tmpConfig.setInlongGroupIdAndStreamId(groupId, streamId);
            tmpConfig.setTenantAndNameSpace(tenant, nameSpace);
            tmpConfig.setTopicName(topicName);
            tmpConfig.setParams(idObject.getParams());
            tmpConfig.setDataType(DataType.convert(
                    idObject.getParams().getOrDefault("dataType", DataType.TEXT.value())));
            tmpConfig.setFieldDelimiter(idObject.getParams().getOrDefault("fieldDelimiter", "|"));
            tmpConfig.setFileDelimiter(idObject.getParams().getOrDefault("fileDelimiter", "\n"));
            tmpTopicConfigMap.put(tmpConfig.getUid(), tmpConfig);
            if (mqType.equals(CacheType.TUBE)
                    && !tmpConfig.getUid().equals(tmpConfig.getInlongGroupId())
                    && tmpTopicConfigMap.get(tmpConfig.getInlongGroupId()) == null) {
                IdTopicConfig tmpConfig2 = new IdTopicConfig();
                tmpConfig2.setInlongGroupIdAndStreamId(groupId, "");
                tmpConfig.setTenantAndNameSpace(tenant, nameSpace);
                tmpConfig2.setTopicName(topicName);
                tmpConfig2.setDataType(tmpConfig.getDataType());
                tmpConfig2.setFieldDelimiter(tmpConfig.getFieldDelimiter());
                tmpConfig2.setFileDelimiter(tmpConfig.getFileDelimiter());
                tmpConfig2.setParams(tmpConfig.getParams());
                tmpTopicConfigMap.put(tmpConfig.getUid(), tmpConfig2);
            }
        }
        return tmpTopicConfigMap;
    }

    /**
     * store meta config to file
     */
    private boolean storeConfigToFile(String inDataMd5, String metaJsonStr) {
        boolean isSuccess = false;
        String filePath = getFilePath();
        if (StringUtils.isBlank(filePath)) {
            LOG.error("Error in writing file {} as the file path is null.", getFileName());
            return isSuccess;
        }
        readWriteLock.writeLock().lock();
        try {
            File sourceFile = new File(filePath);
            File targetFile = new File(getNextBackupFileName());
            File tmpNewFile = new File(getFileName() + ".tmp");

            if (sourceFile.exists()) {
                FileUtils.copyFile(sourceFile, targetFile);
            }
            FileUtils.writeStringToFile(tmpNewFile, metaJsonStr, StandardCharsets.UTF_8);
            FileUtils.copyFile(tmpNewFile, sourceFile);
            tmpNewFile.delete();
            tmpDataMd5 = inDataMd5;
            lastSyncVersion.set(System.currentTimeMillis());
            isSuccess = true;
            setFileChanged();
        } catch (Throwable ex) {
            LOG.error("Error in writing file {}", getFileName(), ex);
        } finally {
            readWriteLock.writeLock().unlock();
        }
        return isSuccess;
    }

    /**
     * load from holder
     */
    private String loadConfigFromFile() {
        String result = "";
        if (StringUtils.isBlank(getFileName())) {
            LOG.error("Fail to load json {} as the file name is null.", getFileName());
            return result;
        }
        InputStream inStream = null;
        try {
            URL url = getClass().getClassLoader().getResource(getFileName());
            inStream = url != null ? url.openStream() : null;
            if (inStream == null) {
                LOG.error("Fail to load json {} as the input stream is null", getFileName());
                return result;
            }
            int size = inStream.available();
            if (size > MAX_ALLOWED_JSON_FILE_SIZE) {
                LOG.error("Fail to load json {} as the content size({}) over max allowed size({})",
                        getFileName(), size, MAX_ALLOWED_JSON_FILE_SIZE);
                return result;
            }
            byte[] buffer = new byte[size];
            inStream.read(buffer);
            result = new String(buffer, StandardCharsets.UTF_8);
        } catch (Throwable e) {
            LOG.error("Fail to load json {}", getFileName(), e);
        } finally {
            if (null != inStream) {
                try {
                    inStream.close();
                } catch (IOException e) {
                    LOG.error("Fail in inStream.close for file {}", getFileName(), e);
                }
            }
        }
        return result;
    }
}
