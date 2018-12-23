/*
 *
 *  * Tencent is pleased to support the open source community by making VasSonic available.
 *  *
 *  * Copyright (C) 2017 THL A29 Limited, a Tencent company. All rights reserved.
 *  * Licensed under the BSD 3-Clause License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *  *
 *  * https://opensource.org/licenses/BSD-3-Clause
 *  *
 *  * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 *  *
 *  *
 *
 */

package com.tencent.sonic.sdk.download;

import java.util.List;
import java.util.Map;

/**
 * download callback.
 */

public interface SonicDownloadCallback {
    /**
     * notify the download start.
     */
    void onStart();

    /**
     * notify the download progress.
     *
     * @param pro downloaded size
     * @param total total size
     */
    void onProgress(int pro, int total);

    /**
     * notify download success.
     *
     * @param content downloaded content bytes
     * @param rspHeaders http response headers
     */
    void onSuccess(byte[] content, Map<String, List<String>> rspHeaders);

    /**
     * notify download failed.
     *
     * @param errorCode error code
     */
    void onError(int errorCode);

    /**
     * notify download finish. <code>onSuccess</code> or <code>onError</code>
     */
    void onFinish();

    /**
     * an empty implementation of {@link SonicDownloadCallback}
     */
    class SimpleDownloadCallback implements SonicDownloadCallback {

        @Override
        public void onStart() { }

        @Override
        public void onProgress(int pro, int total) { }

        @Override
        public void onSuccess(byte[] content, Map<String, List<String>> rspHeaders) { }

        @Override
        public void onError(int errorCode) { }

        @Override
        public void onFinish() { }
    }
}
