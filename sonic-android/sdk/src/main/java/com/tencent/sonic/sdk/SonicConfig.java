/*
 * Tencent is pleased to support the open source community by making VasSonic available.
 *
 * Copyright (C) 2017 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the BSD 3-Clause License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 *
 *
 */

package com.tencent.sonic.sdk;

/**
 * Sonic global config
 */
public class SonicConfig {

    /**
     * The max number of preload session , default is 5
     */
    int MAX_PRELOAD_SESSION_COUNT = 5;

    /**
     * When sonic server unavailable, sonic will not execute its flow and will execute
     * webview normal loading process. This time control sonic how log will not execute its flow.
     */
    long SONIC_UNAVAILABLE_TIME = 6 * 60 * 60 * 1000;

    /**
     * The max size of sonic cache, default is 30M.
     */
    long SONIC_CACHE_MAX_SIZE = 30 * 1024 * 1024;

    /**
     * The time interval between check sonic cache, default is 24 hours.
     */
    long SONIC_CACHE_CHECK_TIME_INTERVAL = 24 * 60 * 60 * 1000L;

    /**
     * Whether verify file by compare SHA1. If this value is false, sonic will verify file by file's size.
     * Verify the file size is less time consuming than checking SHA1.
     */
    boolean VERIFY_CACHE_FILE_WITH_SHA1 = true;

    private SonicConfig() {

    }

    /**
     * Builder for SonicConfig
     */
    public static class Builder {

        private final SonicConfig target;

        public Builder() {
            target = new SonicConfig();
        }

        public Builder setMaxPreloadSessionCount(int maxPreloadSessionCount) {
            target.MAX_PRELOAD_SESSION_COUNT = maxPreloadSessionCount;
            return this;
        }

        public Builder setUnavailableTime(long unavailableTime) {
            target.SONIC_UNAVAILABLE_TIME = unavailableTime;
            return this;
        }

        public Builder setCacheVerifyWithSha1(boolean enable) {
            target.VERIFY_CACHE_FILE_WITH_SHA1 = enable;
            return this;
        }

        public Builder setCacheMaxSize(long maxSize) {
            target.SONIC_CACHE_MAX_SIZE = maxSize;
            return this;
        }

        public Builder setCacheCheckTimeInterval(long time) {
            target.SONIC_CACHE_CHECK_TIME_INTERVAL = time;
            return this;
        }

        public SonicConfig build() {
            return target;
        }
    }
}
