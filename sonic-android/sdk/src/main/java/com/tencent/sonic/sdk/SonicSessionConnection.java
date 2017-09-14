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

import android.content.Intent;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

/**
 *
 * The abstract class <code>SonicSessionConnection</code> is the superclass
 * of all classes that represent a communications link between the
 * application and a URL. Instances of this class can be used both to
 * read from and to write to the resource referenced by the URL
 */
public abstract class SonicSessionConnection {

    private static final String TAG = SonicConstants.SONIC_SDK_LOG_PREFIX + "SonicSessionConnection";

    /**
     * HTTP header:eTag. <br>
     * This header represents SHA1 value of the whole website, including template and data.
     */
    public final static String CUSTOM_HEAD_FILED_ETAG = "etag";

    /**
     * HTTP header:accept-diff. <br>
     * This header represents that client accepts data incremental scene updates or not.
     */
    private final static String CUSTOM_HEAD_FILED_ACCEPT_DIFF = "accept-diff";

    /**
     * HTTP header:template_tag. <br>
     * This header represents SHA1 value of the template file.
     */
    public final static String CUSTOM_HEAD_FILED_TEMPLATE_TAG = "template-tag";

    /**
     * HTTP header:template_change. <br>
     * This header indicates whether the template file has changed or not.
     */
    public final static String CUSTOM_HEAD_FILED_TEMPLATE_CHANGE = "template-change";

    /**
     * HTTP header:cache-offline. <br>
     * This header indicates whether the website needs to be refreshed or not.
     */
    public final static String CUSTOM_HEAD_FILED_CACHE_OFFLINE = "cache-offline";

    /**
     * HTTP header:dns-prefetch-address <br>
     * This header represents the ip address of the server. <br>
     * Sonic Connection will use this ip to connect to server to avoid the cost time of DNS resolution.
     */
    public final static String DNS_PREFETCH_ADDRESS = "dns-prefetch-address";

    /**
     * HTTP Header:sdk_version. <br>
     * This header represents the version of SDK.
     */
    private final static String CUSTOM_HEAD_FILED_SDK_VERSION = "sonic-sdk-version";

    /**
     * HTTP Header:dns-prefetch. <br>
     * This header indicates that Sonic connection has used the ip represented by {@link #DNS_PREFETCH_ADDRESS}
     */
    public final static String CUSTOM_HEAD_FILED_DNS_PREFETCH = "sonic-dns-prefetch";

    /**
     * HTTP Header：Content-Security-Policy. <br>
     * This header represents the HTML CSP.
     */
    public final static String HTTP_HEAD_CSP = "Content-Security-Policy";

    /**
     * HTTP Header：Content-Security-Policy-Report-Only. <br>
     * This header represents the HTML Content-Security-Policy-Report-Only.
     */
    public final static String HTTP_HEAD_CSP_REPORT_ONLY = "Content-Security-Policy-Report-Only";


    /**
     * HTTP Header：Set-Cookie. <br>
     * This header represents the HTML Set-Cookie.
     */
    public final static String HTTP_HEAD_FILED_SET_COOKIE = "Set-Cookie";

    /**
     * SonicSession Object used by SonicSessionConnection.
     */
    protected final SonicSession session;

    /**
     * This intent saves all of the initialization param.
     */
    protected final Intent intent;

    /**
     * The input stream that reads from this open connection.
     */
    protected BufferedInputStream responseStream;

    /**
     * Constructor
     * @param session The SonicSession instance
     * @param intent The intent
     */
    public SonicSessionConnection(SonicSession session, Intent intent) {
        this.session = session;
        this.intent = intent != null ? intent : new Intent();
    }

    /**
     *
     * Opens a communications link to the resource referenced by Sonic session
     *
     * @return Returns the response code of connection
     */
    public synchronized int connect() {
        return internalConnect();
    }

    /**
     * Disconnect the communications link to the resource referenced by Sonic session
     */
    public abstract void disconnect();

    public abstract int getResponseCode();

    public abstract Map<String, List<String>> getResponseHeaderFields();

    /**
     *
     * @param key  the name of a header field.
     * @return Returns the value of the named header field.
     */
    public abstract String getResponseHeaderField(String key);

    /**
     *
     * @return Returns an input stream that reads from this open connection.
     */
    public synchronized BufferedInputStream getResponseStream() {
        if (responseStream == null) {
            responseStream = internalGetResponseStream();
        }
        return responseStream;
    }

    /**
     * Reads all of data from {@link #getResponseStream()} into byte array output stream. <br>
     * <p><b>Note: This method blocks until the end of the input stream has been reached</b></p>
     *
     * @return Returns a ByteArrayOutputStream that reads from {@link #getResponseStream()}
     */
    public synchronized ByteArrayOutputStream getResponseData() {
        BufferedInputStream responseStream = getResponseStream();
        if (null != responseStream) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[session.config.READ_BUF_SIZE];
            try {
                int n;
                while (-1 != (n = responseStream.read(buffer))) {
                    outputStream.write(buffer, 0, n);
                }
                return outputStream;
            } catch (Throwable e) {
                SonicUtils.log(TAG, Log.ERROR, "getResponseData error:" + e.getMessage() + ".");
            }
        }
        return null;
    }

    /**
     * A holder caches information about the input and output stream . Meanwhile this holder indicates the end of
     *   the input stream has been reached or not.
     */
    public class ResponseDataTuple {

        boolean isComplete;

        BufferedInputStream responseStream;

        ByteArrayOutputStream outputStream;
    }

    /**
     *  Reads all of data from {@link #getResponseStream()} into byte array output stream {@code outputStream}.
     *  And then this method returns {@link ResponseDataTuple} object which holds the input and output stream. <br>
     * <p><b>Note: This method blocks until the end of the input stream has been reached or {@code breakCondition} has been reset to true.</b></p>
     * 
	 * @param breakCondition This method won't read any data from {@link #getResponseStream()} if {@code breakCondition} is false.
     * @param outputStream   This method will reuse this byte array output stream instead of creating new output stream.
     * @return
     *      Returns {@link ResponseDataTuple} caches information about the all of the stream and the state which indicates there is no more data.
     */
    public synchronized ResponseDataTuple getResponseData(AtomicBoolean breakCondition, ByteArrayOutputStream outputStream) {
        BufferedInputStream responseStream = getResponseStream();
        if (null != responseStream) {
            if (null == outputStream) {
                outputStream = new ByteArrayOutputStream();
            }
            byte[] buffer = new byte[session.config.READ_BUF_SIZE];
            try {
                int n = 0;
                while (!breakCondition.get() && -1 != (n = responseStream.read(buffer))) {
                    outputStream.write(buffer, 0, n);
                }
                ResponseDataTuple responseDataTuple = new ResponseDataTuple();
                responseDataTuple.responseStream = responseStream;
                responseDataTuple.outputStream = outputStream;
                responseDataTuple.isComplete = -1 == n;
                return responseDataTuple;
            } catch (Throwable e) {
                SonicUtils.log(TAG, Log.ERROR, "getResponseData error:" + e.getMessage() + ".");
            }
        }
        return null;
    }

    protected abstract int internalConnect();

    protected abstract BufferedInputStream internalGetResponseStream();


    public static class SessionConnectionDefaultImpl extends SonicSessionConnection {

        /**
         *  A default http connection  referred to by the {@code com.tencent.sonic.sdk.SonicSession#currUrl}.
         */
        private URLConnection connectionImpl;

        public SessionConnectionDefaultImpl(SonicSession session, Intent intent) {
            super(session, intent);
        }

        private URLConnection createConnection() {

            String currentUrl = session.currUrl;

            if (TextUtils.isEmpty(currentUrl)) {
                return null;
            }

            URLConnection connection = null;
            try {
                URL url = new URL(currentUrl);
                String dnsPrefetchAddress = intent.getStringExtra(SonicSessionConnection.DNS_PREFETCH_ADDRESS);
                String originHost = null;
                /**
                 * Use the ip value mapped by {@code SonicSessionConnection.DNS_PREFETCH_ADDRESS} to avoid the cost time of DNS resolution.
                 * Meanwhile it can reduce the risk from hijacking http session.
                 */
                if (!TextUtils.isEmpty(dnsPrefetchAddress)) {
                    originHost = url.getHost();
                    url = new URL(currentUrl.replace(originHost, dnsPrefetchAddress));
                    SonicUtils.log(TAG, Log.INFO, "create UrlConnection with DNS-Prefetch(" + originHost + " -> " + dnsPrefetchAddress + ").");
                }
                connection = url.openConnection();
                if (connection != null) {
                    if (connection instanceof HttpURLConnection) {
                        ((HttpURLConnection) connection).setInstanceFollowRedirects(false);
                    }

                    if (!TextUtils.isEmpty(originHost)) {
                        /**
                         * If originHost is not empty, that means connection uses the ip value instead of http host.
                         * So http header need to set the Host and {@link com.tencent.sonic.sdk.SonicSessionConnection.CUSTOM_HEAD_FILED_DNS_PREFETCH} request property.
                         */
                        connection.setRequestProperty("Host", originHost);

                        connection.setRequestProperty(SonicSessionConnection.CUSTOM_HEAD_FILED_DNS_PREFETCH, url.getHost());
                        if (connection instanceof HttpsURLConnection) { // 如果属于https，需要特殊处理，比如支持sni
                            /**
                             * If the scheme of url is https, then it needs extra processing, such as the sni support.
                             */
                            final String finalOriginHost = originHost;
                            final URL finalUrl = url;
                            HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;
                            httpsConnection.setSSLSocketFactory(new SonicSniSSLSocketFactory(SonicEngine.getInstance().getRuntime().getContext(), originHost));
                            httpsConnection.setHostnameVerifier(new HostnameVerifier() {
                                @Override
                                public boolean verify(String hostname, SSLSession session) {
                                    boolean verifySuccess = false;
                                    long startTime = System.currentTimeMillis();
                                    if (finalUrl.getHost().equals(hostname)) {
                                        verifySuccess = HttpsURLConnection.getDefaultHostnameVerifier().verify(finalOriginHost, session);
                                        SonicUtils.log(TAG, Log.DEBUG, "verify hostname cost " + (System.currentTimeMillis() - startTime) + " ms.");
                                    }
                                    return verifySuccess;
                                }
                            });
                        }
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

        private URLConnection getConnection() {
            if (null == connectionImpl) {
                connectionImpl = createConnection();
                if (null != connectionImpl) {
                    String currentUrl = session.srcUrl;
                    SonicSessionConfig config = session.config;
                    connectionImpl.setConnectTimeout(config.CONNECT_TIMEOUT_MILLIS);
                    connectionImpl.setReadTimeout(config.READ_TIMEOUT_MILLIS);
                    /**
                     *  {@link SonicSessionConnection#CUSTOM_HEAD_FILED_ACCEPT_DIFF} is need to be set If client accepts incrementally updates. <br>
                     *  <p><b>Note: It doesn't support incrementally updated for template file.</b><p/>
                     */
                    connectionImpl.setRequestProperty(CUSTOM_HEAD_FILED_ACCEPT_DIFF, config.ACCEPT_DIFF_DATA ? "true" : "false");

                    String eTag = intent.getStringExtra(CUSTOM_HEAD_FILED_ETAG);
                    if (null == eTag) eTag = "";
                    connectionImpl.setRequestProperty("If-None-Match", eTag);

                    String templateTag = intent.getStringExtra(CUSTOM_HEAD_FILED_TEMPLATE_TAG);
                    if (null == templateTag) templateTag = "";
                    connectionImpl.setRequestProperty(CUSTOM_HEAD_FILED_TEMPLATE_TAG, templateTag);

                    connectionImpl.setRequestProperty("method", "GET");
                    connectionImpl.setRequestProperty("accept-Charset", "utf-8");
                    connectionImpl.setRequestProperty("accept-Encoding", "gzip");
                    connectionImpl.setRequestProperty("accept-Language", "zh-CN,zh;");
                    connectionImpl.setRequestProperty(CUSTOM_HEAD_FILED_SDK_VERSION, "Sonic/" + SonicConstants.SONIC_VERSION_NUM);

                    SonicRuntime runtime = SonicEngine.getInstance().getRuntime();
                    String cookie = runtime.getCookie(currentUrl);
                    if (!TextUtils.isEmpty(cookie)) {
                        connectionImpl.setRequestProperty("cookie", cookie);
                    } else {
                        SonicUtils.log(TAG, Log.ERROR, "create UrlConnection cookie is empty");
                    }
                    String userAgent = runtime.getUserAgent();
                    if (!TextUtils.isEmpty(userAgent)) {
                        userAgent += " Sonic/" + SonicConstants.SONIC_VERSION_NUM;
                    } else {
                        userAgent = "Sonic/" + SonicConstants.SONIC_VERSION_NUM;
                    }
                    connectionImpl.setRequestProperty("User-Agent", userAgent);
                }
            }
            return connectionImpl;
        }

        @Override
        protected synchronized int internalConnect() {
            URLConnection urlConnection = getConnection();
            if (urlConnection instanceof HttpURLConnection) {
                HttpURLConnection httpURLConnection = (HttpURLConnection) urlConnection;
                try {
                    httpURLConnection.connect();
                    return SonicConstants.ERROR_CODE_SUCCESS;
                } catch (Throwable e) {
                    String errMsg = e.getMessage();
                    SonicUtils.log(TAG, Log.ERROR, "connect error:" + errMsg);

                    if (e instanceof IOException) {
                        if (e instanceof SocketTimeoutException) {
                            return SonicConstants.ERROR_CODE_CONNECT_TOE;
                        }

                        if (!TextUtils.isEmpty(errMsg) && errMsg.contains("timeoutexception")) {
                            return SonicConstants.ERROR_CODE_CONNECT_TOE;
                        }
                        return SonicConstants.ERROR_CODE_CONNECT_IOE;
                    }

                    if (e instanceof NullPointerException) {
                        return SonicConstants.ERROR_CODE_CONNECT_NPE;
                    }
                }
            }
            return SonicConstants.ERROR_CODE_UNKNOWN;
        }

        @Override
        public void disconnect() {
            if (connectionImpl instanceof HttpURLConnection) {
                final HttpURLConnection httpURLConnection = (HttpURLConnection) connectionImpl;
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    SonicEngine.getInstance().getRuntime().postTaskToThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                httpURLConnection.disconnect();
                            } catch (Throwable e) {
                                SonicUtils.log(TAG, Log.ERROR, "disconnect error:" + e.getMessage());
                            }
                        }
                    }, 0);
                } else {
                    try {
                        httpURLConnection.disconnect();
                    } catch (Exception e) {
                        SonicUtils.log(TAG, Log.ERROR, "disconnect error:" + e.getMessage());
                    }
                }
            }
        }

        @Override
        protected BufferedInputStream internalGetResponseStream() {
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

        @Override
        public int getResponseCode() {
            if (connectionImpl instanceof HttpURLConnection) {
                try {
                    return ((HttpURLConnection) connectionImpl).getResponseCode();
                } catch (Throwable e) {
                    String errMsg = e.getMessage();
                    SonicUtils.log(TAG, Log.ERROR, "getResponseCode error:" + errMsg);

                    if (e instanceof IOException) {
                        if (e instanceof SocketTimeoutException) {
                            return SonicConstants.ERROR_CODE_CONNECT_TOE;
                        }


                        if (!TextUtils.isEmpty(errMsg) && errMsg.contains("timeoutexception")) {
                            return SonicConstants.ERROR_CODE_CONNECT_TOE;
                        }

                        return SonicConstants.ERROR_CODE_CONNECT_IOE;
                    }

                    if (e instanceof NullPointerException) {
                        return SonicConstants.ERROR_CODE_CONNECT_NPE;
                    }
                }
            }
            return SonicConstants.ERROR_CODE_UNKNOWN;
        }

        @Override
        public Map<String, List<String>> getResponseHeaderFields() {
            if (null != connectionImpl) {
                return connectionImpl.getHeaderFields();
            }
            return null;
        }

        @Override
        public String getResponseHeaderField(String key) {
            if (null != connectionImpl) {
                return connectionImpl.getHeaderField(key);
            }
            return null;
        }
    }
}
