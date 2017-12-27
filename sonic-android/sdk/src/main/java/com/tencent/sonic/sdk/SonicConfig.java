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
     * The max size of sonic resource cache, default is 60M.
     */
    long SONIC_RESOURCE_CACHE_MAX_SIZE = 60 * 1024 * 1024;

    /**
     * The time interval between check sonic cache, default is 24 hours.
     */
    long SONIC_CACHE_CHECK_TIME_INTERVAL = 24 * 60 * 60 * 1000L;

    /**
     * The max number of tasks which is downloading in the same time.
     */
    public int SONIC_MAX_NUM_OF_DOWNLOADING_TASK = 3;

    /**
     * The max age of sonic cache before expired.
     */
    int SONIC_CACHE_MAX_AGE = 5 * 60 * 1000;

    /**
     * Whether verify file by compare SHA1. If this value is false, sonic will verify file by file's size.
     * Verify the file size is less time consuming than checking SHA1.
     */
    public boolean VERIFY_CACHE_FILE_WITH_SHA1 = true;

    /**
     * Whether auto call init db when create sonicEngine or not, default is true.
     */
    boolean AUTO_INIT_DB_WHEN_CREATE = true;

    /**
     * There will be a deadlock when ShouldInterceptRequest and getCookie are running at the same thread.
     * This bug was found on Android ( < 5.0) system. @see <a href="https://github.com/Tencent/VasSonic/issues/90">Issue 90</a> <br>
     * So Sonic will call getCookie before sending Sonic request If GET_COOKIE_WHEN_SESSION_CREATE is true.<br>
     * The value of this property should be true unless your app uses <a href="https://x5.tencent.com/tbs">X5 kernel</a>.
     */
    boolean GET_COOKIE_WHEN_SESSION_CREATE = true;

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

        public Builder setResourceCacheMaxSize(long maxSize) {
            target.SONIC_RESOURCE_CACHE_MAX_SIZE = maxSize;
            return this;
        }

        public Builder setCacheCheckTimeInterval(long time) {
            target.SONIC_CACHE_CHECK_TIME_INTERVAL = time;
            return this;
        }

        public Builder setMaxNumOfDownloadingTasks(int num) {
            target.SONIC_MAX_NUM_OF_DOWNLOADING_TASK = num;
            return this;
        }

        public Builder setAutoInitDBWhenCreate(boolean autoInitDBWhenCreate) {
            target.AUTO_INIT_DB_WHEN_CREATE = autoInitDBWhenCreate;
            return this;
        }

        public Builder setGetCookieWhenSessionCreate(boolean value) {
            target.GET_COOKIE_WHEN_SESSION_CREATE = value;
            return this;
        }

        public Builder setSonicCacheMaxAge(int maxAge) {
            target.SONIC_CACHE_MAX_AGE = maxAge;
            return this;
        }

        public SonicConfig build() {
            return target;
        }
    }
}
