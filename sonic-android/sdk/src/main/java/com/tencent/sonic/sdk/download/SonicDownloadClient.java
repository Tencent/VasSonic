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

import android.text.TextUtils;
import android.util.Log;

import com.tencent.sonic.sdk.SonicConstants;
import com.tencent.sonic.sdk.SonicSessionStream;
import com.tencent.sonic.sdk.SonicUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;

import static com.tencent.sonic.sdk.SonicSessionConnection.HTTP_HEAD_FIELD_COOKIE;
import static java.net.HttpURLConnection.HTTP_OK;

/**
 * Handles a single HTTP resource download
 *
 */
public class SonicDownloadClient implements SonicSessionStream.Callback {

    /**
     * log filter
     */
    public static final String TAG = SonicConstants.SONIC_SDK_LOG_PREFIX + "SonicDownloadClient";

    /**
     * download buffer size
     */
    private static final int READ_BUFFER_SIZE = 2048;

    /**
     * Task which record the download info
     */
    public static class DownloadTask {

        /**
         * download in initiate state.
         */
        public static final int STATE_INITIATE = 0;

        /**
         * download in queueing state.
         */
        public static final int STATE_QUEUEING = 1;

        /**
         * the task is in downloading state.
         */
        public static final int STATE_DOWNLOADING = 2;

        /**
         * the task is in download complete state.
         */
        public static final int STATE_DOWNLOADED = 3;

        /**
         * the task is load from cache, not from network.
         */
        public static final int STATE_LOAD_FROM_CACHE = 4;

        /**
         * url of the resource to be download
         */
        public String mResourceUrl;

        /**
         * ip address instead of host to launch a http request
         */
        public String mIpAddress;

        /**
         * cookie to be set in the http download request
         */
        public String mCookie;

        /**
         * the download request's response headers
         */
        public Map<String, List<String>> mRspHeaders;

        /**
         * the network stream or memory stream or the bridge stream
         */
        public InputStream mInputStream;

        /**
         * the task's download state
         */
        public AtomicInteger mState = new AtomicInteger(STATE_INITIATE);

        /**
         * whether the task's responding resource was intercepted by kernel
         */
        public final AtomicBoolean mWasInterceptInvoked = new AtomicBoolean(false);

        /**
         * list of download callback
         */
        public List<SonicDownloadCallback> mCallbacks = new ArrayList<SonicDownloadCallback>();
    }

    /**
     * A download connection implement.
     */
    private final SonicDownloadConnection mConn;

    /**
     * the responding download task
     */
    private DownloadTask mTask;

    private ByteArrayOutputStream mOutputStream;

    /**
     * whether the download task is finished or is a bridge stream
     */
    private boolean mDownloadFinished = false;

    public SonicDownloadClient(DownloadTask task) {
        mTask = task;
        mConn = new SonicDownloadConnection(task.mResourceUrl);
        mOutputStream = new ByteArrayOutputStream();
    }

    /**
     * download the resource and notify download progress
     *
     * @return response code
     */
    public int download() {
        onStart();

        int resultCode = mConn.connect();

        if (SonicConstants.ERROR_CODE_SUCCESS != resultCode) {
            onError(resultCode);
            return resultCode; // error case
        }

        int responseCode = mConn.getResponseCode();
        if (responseCode != HTTP_OK) {
            onError(responseCode);
            return responseCode;
        }

        mTask.mRspHeaders = mConn.getResponseHeaderFields();
        if (getResponseStream(mTask.mWasInterceptInvoked)) {
            return SonicConstants.ERROR_CODE_SUCCESS;
        }
        return SonicConstants.ERROR_CODE_UNKNOWN;
    }

    private boolean readServerResponse(AtomicBoolean breakCondition) {
        BufferedInputStream bufferedInputStream = mConn.getResponseStream();
        if (null == bufferedInputStream) {
            SonicUtils.log(TAG, Log.ERROR, "readServerResponse error: bufferedInputStream is null!");
            return false;
        }

        try {
            byte[] buffer = new byte[READ_BUFFER_SIZE];

            int total = mConn.connectionImpl.getContentLength();
            int n = 0, sum = 0;
            while (((breakCondition == null) || !breakCondition.get()) && -1 != (n = bufferedInputStream.read(buffer))) {
                mOutputStream.write(buffer, 0, n);
                sum += n;
                if (total > 0) {
                    onProgress(sum, total);
                }
            }

            if (n == -1) {
                mDownloadFinished = true;
                onSuccess(mOutputStream.toByteArray(), mConn.getResponseHeaderFields());
            }
        } catch (Exception e) {
            SonicUtils.log(TAG, Log.ERROR, "readServerResponse error:" + e.getMessage() + ".");
            return false;
        }

        return true;
    }

    private synchronized boolean getResponseStream(AtomicBoolean breakConditions) {
        if (readServerResponse(breakConditions)) {
            BufferedInputStream netStream = mDownloadFinished ? null : mConn.getResponseStream();
            mTask.mInputStream = new SonicSessionStream(this, mOutputStream, netStream);
            synchronized (mTask.mWasInterceptInvoked) {
                mTask.mWasInterceptInvoked.notify();
            }
            if (mDownloadFinished) {
                SonicUtils.log(TAG, Log.INFO, "sub resource compose a memory stream (" + mTask.mResourceUrl + ").");
            } else {
                SonicUtils.log(TAG, Log.INFO, "sub resource compose a bridge stream (" + mTask.mResourceUrl + ").");
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onClose(boolean readComplete, ByteArrayOutputStream outputStream) {
        SonicUtils.log(TAG, Log.INFO, "sub resource bridge stream on close(" + mTask.mResourceUrl + ").");
        if (!mDownloadFinished) {
            onSuccess(outputStream.toByteArray(), mConn.getResponseHeaderFields());
        }
    }

    private void onStart() {
        for (SonicDownloadCallback callback : mTask.mCallbacks) {
            if (callback != null) {
                callback.onStart();
            }
        }
    }

    private void onProgress(int pro, int total) {
        for (SonicDownloadCallback callback : mTask.mCallbacks) {
            if (callback != null) {
                callback.onProgress(pro, total);
            }
        }
    }

    private void onSuccess(byte[] content, Map<String, List<String>> rspHeaders) {
        for (SonicDownloadCallback callback : mTask.mCallbacks) {
            if (callback != null) {
                callback.onSuccess(content, rspHeaders);
            }
        }
        onFinish();
    }

    private void onError(int errCode) {
        for (SonicDownloadCallback callback : mTask.mCallbacks) {
            if (callback != null) {
                callback.onError(errCode);
            }
        }
        onFinish();
    }

    private void onFinish() {
        for (SonicDownloadCallback callback : mTask.mCallbacks) {
            if (callback != null) {
                callback.onFinish();
            }
        }
        mConn.disconnect();
    }

    public class SonicDownloadConnection {
        final URLConnection connectionImpl;

        private String url;

        private BufferedInputStream responseStream;

        public SonicDownloadConnection(String url) {
            this.url = url;
            connectionImpl = createConnection();
            initConnection(connectionImpl);
        }

        URLConnection createConnection() {
            String currentUrl = url;
            if (TextUtils.isEmpty(currentUrl)) {
                return null;
            }

            URLConnection connection = null;
            try {
                URL url = new URL(currentUrl);
                String originHost = null;

                if (!TextUtils.isEmpty(mTask.mIpAddress)) {
                    originHost = url.getHost();
                    url = new URL(currentUrl.replace(originHost, mTask.mIpAddress));
                    SonicUtils.log(TAG, Log.INFO, "create UrlConnection with DNS-Prefetch(" + originHost + " -> " + mTask.mIpAddress + ").");
                }
                connection = url.openConnection();
                if (connection != null) {
                    if (!TextUtils.isEmpty(originHost)) {
                        connection.setRequestProperty("Host", originHost);
                    }
                }
            } catch (Throwable e) {
                if (connection != null) {
                    connection = null;
                }
                SonicUtils.log(TAG, Log.ERROR, "create UrlConnection fail, error:" + e.getMessage() + ".");
            }
            return connection;
        }

        boolean initConnection(URLConnection connection) {
            if (null != connection) {
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(15000);

                connection.setRequestProperty("method", "GET");
                connection.setRequestProperty("Accept-Encoding", "gzip");
                connection.setRequestProperty("Accept-Language", "zh-CN,zh;");

                if (!TextUtils.isEmpty(mTask.mCookie)) {
                    connection.setRequestProperty(HTTP_HEAD_FIELD_COOKIE, mTask.mCookie);
                }
                return true;
            }
            return false;
        }

        synchronized int connect() {
            if (connectionImpl instanceof HttpURLConnection) {
                HttpURLConnection httpURLConnection = (HttpURLConnection) connectionImpl;
                try {
                    httpURLConnection.connect();
                    return SonicConstants.ERROR_CODE_SUCCESS;
                } catch (IOException e) {
                    return SonicConstants.ERROR_CODE_CONNECT_IOE;
                }
            }
            return SonicConstants.ERROR_CODE_UNKNOWN;
        }

        public void disconnect() {
            if (connectionImpl instanceof HttpURLConnection) {
                final HttpURLConnection httpURLConnection = (HttpURLConnection) connectionImpl;
                try {
                    httpURLConnection.disconnect();
                } catch (Exception e) {
                    SonicUtils.log(TAG, Log.ERROR, "disconnect error:" + e.getMessage());
                }
            }
        }

        BufferedInputStream getResponseStream() {
            if (null == responseStream && null != connectionImpl) {
                try {
                    InputStream inputStream = connectionImpl.getInputStream();
                    if ("gzip".equalsIgnoreCase(connectionImpl.getContentEncoding())) {
                        responseStream = new BufferedInputStream(new GZIPInputStream(inputStream));
                    } else {
                        responseStream = new BufferedInputStream(inputStream);
                    }
                } catch (Throwable e) {
                    SonicUtils.log(TAG, Log.ERROR, "getResponseStream error:" + e.getMessage() + ".");
                }
            }
            return responseStream;
        }

        int getResponseCode() {
            if (connectionImpl instanceof HttpURLConnection) {
                try {
                    return ((HttpURLConnection) connectionImpl).getResponseCode();
                } catch (IOException e) {
                    String errMsg = e.getMessage();
                    SonicUtils.log(TAG, Log.ERROR, "getResponseCode error:" + errMsg);
                    return SonicConstants.ERROR_CODE_CONNECT_IOE;
                }
            }
            return SonicConstants.ERROR_CODE_UNKNOWN;
        }

        Map<String, List<String>> getResponseHeaderFields() {
            if (null == connectionImpl) {
                return null;
            }
            return connectionImpl.getHeaderFields();
        }
    }

    /**
     * sub resource download callback.
     */
    public static class SubResourceDownloadCallback extends SonicDownloadCallback.SimpleDownloadCallback {

        private String resourceUrl;

        public SubResourceDownloadCallback(String url) {
            this.resourceUrl = url;
        }

        @Override
        public void onStart() {
            if (SonicUtils.shouldLog(Log.INFO)) {
                SonicUtils.log(TAG, Log.INFO, "session start download sub resource, url=" + resourceUrl);
            }
        }

        @Override
        public void onSuccess(byte[] content, Map<String, List<String>> rspHeaders) {
            // save cache files
            String fileName = SonicUtils.getMD5(resourceUrl);
            SonicUtils.saveResourceFiles(fileName, content, rspHeaders);
            // save resource data to db
            SonicUtils.saveSonicResourceData(resourceUrl, SonicUtils.getSHA1(content), content.length);

        }

        @Override
        public void onError(int errorCode) {
            if (SonicUtils.shouldLog(Log.INFO)) {
                SonicUtils.log(TAG, Log.INFO, "session download sub resource error: code = " + errorCode + ", url=" + resourceUrl);
            }
        }
    }
}
