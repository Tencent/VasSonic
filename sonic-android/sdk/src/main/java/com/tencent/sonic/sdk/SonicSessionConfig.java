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

import java.util.Map;

/**
 *
 * The sonicSession configurations. A sonicSession configuration describes
 * the http(s) connection time out, the way sonicSession's id be generated and
 * so on
 *
 */
public class SonicSessionConfig {

    /**
     * Http(s) connection time out , default 5s
     */
    int CONNECT_TIMEOUT_MILLIS = 5000;

    /**
     * Http(s) read time out, default 15s
     */
    int READ_TIMEOUT_MILLIS = 15000;

    /**
     * Buffer size when read data from network, default 10KB
     */
    int READ_BUF_SIZE = 1024 * 10;

    /**
     * Preloaded session expiration time, default 3 minute
     */
    long PRELOAD_SESSION_EXPIRED_TIME = 3 * 60 * 1000;

    /**
     * Accept diff data or not, if accept diff data server will return
     * only update data when local exists html data.
     */
    boolean ACCEPT_DIFF_DATA = true;

    /**
     * Local data is related to user id or nor, if true local data is used only by this user.
     */
    boolean IS_ACCOUNT_RELATED = true;

    /**
     * Need reload url in bad network or not.
     */
    boolean RELOAD_IN_BAD_NETWORK = false;

    /**
     * SonicSession start flow when created or not, if true, SonicSession will load local data
     * and initiate an http request when created, or these will be do when client ready
     */
    boolean AUTO_START_WHEN_CREATE = true;

    /**
     * Need to check the Cache-Control response header or not.
     */
    boolean SUPPORT_CACHE_CONTROL = false;

    /**
     * Use local Sonic Server or not. If SUPPORT_LOCAL_SERVER is true, Sonic will treat normal request as sonic request
     * to separate html into template and data file.
     */
    boolean SUPPORT_LOCAL_SERVER = false;

    /**
     * The toast when network unavailable
     */
    String USE_SONIC_CACHE_IN_BAD_NETWORK_TOAST = "Bad Network!";

    /**
     * The mode of SonicSession, include{@link QuickSonicSession} and {@link StandardSonicSession}
     */
    int sessionMode = SonicConstants.SESSION_MODE_QUICK;

    /**
     * {@link SonicCacheInterceptor} object, provider local data
     */
    SonicCacheInterceptor cacheInterceptor = null;

    /**
     *{@link SonicSessionConnectionInterceptor} object, provider SonicSessionConnection
     */
    SonicSessionConnectionInterceptor connectionInterceptor = null;

    /**
     * The custom request headers which will be sent to web server
     */
    Map<String, String> customRequestHeaders = null;

    /**
     * The custom response headers which will be sent to webView for intercept WebResourceResponse
     */
    Map<String, String> customResponseHeaders = null;

    @Override
    public boolean equals(Object other) {
        if (other instanceof SonicSessionConfig) {
            SonicSessionConfig config = (SonicSessionConfig)other;
            return sessionMode == config.sessionMode && SUPPORT_LOCAL_SERVER == config.SUPPORT_LOCAL_SERVER;
        }

        return false;

    }

    private SonicSessionConfig() {

    }

    /**
     * Builder for SonicSessionConfig
     */
    public static class Builder {

        private final SonicSessionConfig target;

        public Builder() {
            target = new SonicSessionConfig();
        }

        public Builder setConnectTimeoutMillis(int connectTimeoutMillis) {
            target.CONNECT_TIMEOUT_MILLIS = connectTimeoutMillis;
            return this;
        }

        public Builder setReadTimeoutMillis(int readTimeoutMillis) {
            target.READ_TIMEOUT_MILLIS = readTimeoutMillis;
            return this;
        }

        public Builder setReadBufferSize(int readBufferSize) {
            target.READ_BUF_SIZE = readBufferSize;
            return this;
        }

        public Builder setPreloadSessionExpiredTimeMillis(long preloadSessionExpiredTimeMillis) {
            target.PRELOAD_SESSION_EXPIRED_TIME = preloadSessionExpiredTimeMillis;
            return this;
        }

        public Builder setAcceptDiff(boolean enable) {
            target.ACCEPT_DIFF_DATA = enable;
            return this;
        }

        public Builder setIsAccountRelated(boolean value) {
            target.IS_ACCOUNT_RELATED = value;
            return this;
        }

        public Builder setReloadInBadNetwork(boolean reloadInBadNetwork) {
            target.RELOAD_IN_BAD_NETWORK = reloadInBadNetwork;
            return this;
        }

        public Builder setAutoStartWhenCreate(boolean autoStartWhenCreate) {
            target.AUTO_START_WHEN_CREATE = autoStartWhenCreate;
            return this;
        }

        public Builder setUseSonicCacheInBadNetworkToastMessage(String toastMessage) {
            target.USE_SONIC_CACHE_IN_BAD_NETWORK_TOAST = toastMessage;
            return this;
        }

        public Builder setSessionMode(int sessionMode) {
            target.sessionMode = sessionMode;
            return this;
        }

        public Builder setCacheInterceptor(SonicCacheInterceptor interceptor) {
            target.cacheInterceptor = interceptor;
            return this;
        }

        public Builder setConnectionInterceptor(SonicSessionConnectionInterceptor interceptor) {
            target.connectionInterceptor = interceptor;
            return this;
        }

        public Builder setCustomRequestHeaders(Map<String, String> customRequestHeaders) {
            target.customRequestHeaders = customRequestHeaders;
            return this;
        }

        public Builder setCustomResponseHeaders(Map<String, String> customResponseHeaders) {
            target.customResponseHeaders = customResponseHeaders;
            return this;
        }

        public Builder setSupportCacheControl(boolean supportCacheControl) {
            target.SUPPORT_CACHE_CONTROL = supportCacheControl;
            return this;
        }

        public Builder setSupportLocalServer(boolean enable) {
            target.SUPPORT_LOCAL_SERVER = enable;
            return this;
        }


        public SonicSessionConfig build() {
            return target;
        }
    }
}
