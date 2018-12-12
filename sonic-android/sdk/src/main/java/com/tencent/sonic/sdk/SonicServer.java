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
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.tencent.sonic.sdk.SonicSession.OFFLINE_MODE_HTTP;
import static com.tencent.sonic.sdk.SonicSession.OFFLINE_MODE_TRUE;
import static com.tencent.sonic.sdk.SonicSessionConnection.CUSTOM_HEAD_FILED_CACHE_OFFLINE;
import static com.tencent.sonic.sdk.SonicSessionConnection.CUSTOM_HEAD_FILED_HTML_SHA1;
import static com.tencent.sonic.sdk.SonicSessionConnection.CUSTOM_HEAD_FILED_TEMPLATE_CHANGE;
import static com.tencent.sonic.sdk.SonicSessionConnection.CUSTOM_HEAD_FILED_TEMPLATE_TAG;

/**
 * Instances of this class can be used to read server response from SonicSessionConnection.
 * If this request support Local Sonic Server, it will separate html into template and data file.
 */
public class SonicServer implements SonicSessionStream.Callback {
    public static final String TAG = SonicConstants.SONIC_SDK_LOG_PREFIX + "SonicServer";

    /**
     * A session connection implement.
     */
    protected final SonicSessionConnection connectionImpl;

    protected String serverRsp;

    protected String templateString;

    protected String dataString;

    protected int responseCode;

    final protected SonicSession session;

    final protected Intent requestIntent;

    /**
     *  Cached response headers which contains response headers from server and custom response headers from
     *  {@code com.tencent.sonic.sdk.SonicSessionConfig}
     */
    protected Map<String, List<String>> cachedResponseHeaders;
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    public SonicServer(SonicSession session, Intent requestIntent) {
        this.session = session;
        this.requestIntent = requestIntent;
        connectionImpl = SonicSessionConnectionInterceptor.getSonicSessionConnection(session, requestIntent);
    }

    /**
     *
     * Opens a communications link to the resource referenced by Sonic session.
     * If this request support Local Sonic Server, it will separate html into template and data file.
     *
     * @return Returns the response code of connection
     */
    protected int connect() {
        long startTime = System.currentTimeMillis();

        int resultCode = connectionImpl.connect();
        session.statistics.connectionConnectTime = System.currentTimeMillis();
        if (SonicUtils.shouldLog(Log.DEBUG)) {
            SonicUtils.log(TAG, Log.DEBUG, "session(" + session.id + ") server connect cost = " + (System.currentTimeMillis() - startTime) + " ms.");
        }

        if (SonicConstants.ERROR_CODE_SUCCESS != resultCode) {
            return resultCode; // error case
        }

        startTime = System.currentTimeMillis();
        responseCode = connectionImpl.getResponseCode(); // update response code
        session.statistics.connectionRespondTime = System.currentTimeMillis();
        if (SonicUtils.shouldLog(Log.DEBUG)) {
            SonicUtils.log(TAG, Log.DEBUG, "session(" + session.id + ") server response cost = " + (System.currentTimeMillis() - startTime) + " ms.");
        }

        if (HttpURLConnection.HTTP_NOT_MODIFIED == responseCode) { // nothing needs to do
            return SonicConstants.ERROR_CODE_SUCCESS;
        }

        if (HttpURLConnection.HTTP_OK != responseCode) { // error case
            return SonicConstants.ERROR_CODE_SUCCESS;
        }

        // fix issue for Weak ETag case [https://github.com/Tencent/VasSonic/issues/128]
        String eTag = getResponseHeaderField(getCustomHeadFieldEtagKey());
        if (!TextUtils.isEmpty(eTag) && eTag.toLowerCase().startsWith("w/")) {
            eTag = eTag.toLowerCase().replace("w/", "");
            eTag = eTag.replace("\"", "");
            addResponseHeaderFields(getCustomHeadFieldEtagKey(), eTag);
        }

        String requestETag = requestIntent.getStringExtra(getCustomHeadFieldEtagKey());
        String responseETag = getResponseHeaderField(getCustomHeadFieldEtagKey());
        if (!TextUtils.isEmpty(requestETag) && requestETag.equals(responseETag)) {
            responseCode = HttpURLConnection.HTTP_NOT_MODIFIED; // fix 304 case
            return SonicConstants.ERROR_CODE_SUCCESS;
        }

        if (isSonicResponse() || !session.config.SUPPORT_LOCAL_SERVER) {
            return SonicConstants.ERROR_CODE_SUCCESS; // real sonic response or not support local server
        }

        String cacheOffline = getResponseHeaderField(SonicSessionConnection.CUSTOM_HEAD_FILED_CACHE_OFFLINE);
        if (OFFLINE_MODE_HTTP.equalsIgnoreCase(cacheOffline)) {
            // When cache-offline is "http": which means sonic server is in bad condition, need feed back to run standard http request.
            return SonicConstants.ERROR_CODE_SUCCESS;
        }

        if (TextUtils.isEmpty(cacheOffline)) {
            addResponseHeaderFields(SonicSessionConnection.CUSTOM_HEAD_FILED_CACHE_OFFLINE, OFFLINE_MODE_TRUE);
        }

        if (isFirstLoadRequest()) { // first load case
            return SonicConstants.ERROR_CODE_SUCCESS;
        }

        // When eTag is empty
        if (TextUtils.isEmpty(eTag)) {
            readServerResponse(null);
            if (!TextUtils.isEmpty(serverRsp)) {
                eTag = SonicUtils.getSHA1(serverRsp);
                addResponseHeaderFields(getCustomHeadFieldEtagKey(), eTag);
                addResponseHeaderFields(CUSTOM_HEAD_FILED_HTML_SHA1, eTag);
            } else {
                return SonicConstants.ERROR_CODE_CONNECT_IOE;
            }

            if (requestETag.equals(eTag)) { // 304 case
                responseCode = HttpURLConnection.HTTP_NOT_MODIFIED;
                return SonicConstants.ERROR_CODE_SUCCESS;
            }
        }

        // When templateTag is empty
        String templateTag = getResponseHeaderField(CUSTOM_HEAD_FILED_TEMPLATE_TAG);
        if (TextUtils.isEmpty(templateTag)) {
            if (TextUtils.isEmpty(serverRsp)) {
                readServerResponse(null);
            }
            if (!TextUtils.isEmpty(serverRsp)) {
                separateTemplateAndData();
                templateTag = getResponseHeaderField(CUSTOM_HEAD_FILED_TEMPLATE_TAG);
            } else {
                return SonicConstants.ERROR_CODE_CONNECT_IOE;
            }
        }

        //check If it changes template or update data.
        String requestTemplateTag = requestIntent.getStringExtra(SonicSessionConnection.CUSTOM_HEAD_FILED_TEMPLATE_TAG);
        if (requestTemplateTag.equals(templateTag)) {
            addResponseHeaderFields(SonicSessionConnection.CUSTOM_HEAD_FILED_TEMPLATE_CHANGE, "false");
        } else {
            addResponseHeaderFields(SonicSessionConnection.CUSTOM_HEAD_FILED_TEMPLATE_CHANGE, "true");
        }

        return SonicConstants.ERROR_CODE_SUCCESS;
    }

    private boolean isSonicResponse() {
        Map<String, List<String>> headersFromServer = connectionImpl.getResponseHeaderFields();
        if (null != headersFromServer && !headersFromServer.isEmpty()) {
            Set<Map.Entry<String, List<String>>> entrySet = headersFromServer.entrySet();
            String KeyInLowercase;
            for (Map.Entry<String, List<String>> entry : entrySet) {
                if (!TextUtils.isEmpty(entry.getKey())) {
                    KeyInLowercase = entry.getKey().toLowerCase();
                    if (KeyInLowercase.equals(CUSTOM_HEAD_FILED_CACHE_OFFLINE) ||
                            KeyInLowercase.equals(CUSTOM_HEAD_FILED_TEMPLATE_CHANGE) ||
                            KeyInLowercase.equals(CUSTOM_HEAD_FILED_TEMPLATE_TAG)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isFirstLoadRequest() {
        return TextUtils.isEmpty(requestIntent.getStringExtra(getCustomHeadFieldEtagKey())) ||
                TextUtils.isEmpty(requestIntent.getStringExtra(SonicSessionConnection.CUSTOM_HEAD_FILED_TEMPLATE_TAG));
    }

    public int getResponseCode() {
        return responseCode;
    }

    /**
     * Disconnect the communications link to the resource referenced by Sonic session
     */
    public void disconnect() {
        // We need to close connectionImpl.getResponseStream() manually.
        // ConnectionImpl.disconnect() doesn't close the stream because doing so would require all stream
        // access to be synchronized. It's expected that the thread using the
        // connection will close its streams directly. If it doesn't, the worst
        // case is that the GzipSource's Inflater won't be released until it's
        // finalized. (This logs a warning on Android.)
        try {
            BufferedInputStream bufferedInputStream = connectionImpl.getResponseStream();
            if (bufferedInputStream != null) {
                bufferedInputStream.close();
            }
        } catch (Throwable e) {
            SonicUtils.log(TAG, Log.ERROR, "session(" + session.sId + ") server disconnect error:" + e.getMessage() + ".");
        }

        connectionImpl.disconnect();
    }


    /**
     *  return response headers which contains response headers from server and custom response headers from
     *  {@code com.tencent.sonic.sdk.SonicSessionConfig}
     *  note: server response headers have high priority than custom headers!
     *  @return a Map of header fields
     */
    public  Map<String, List<String>> getResponseHeaderFields() {
        if (null == cachedResponseHeaders) {
            // new cachedResponseHeaders
            cachedResponseHeaders = new ConcurrentHashMap<String, List<String>>();
            // fill custom headers
            List<String> tmpHeaderList;
            if (session.config.customResponseHeaders != null && session.config.customResponseHeaders.size() > 0) {
                for (Map.Entry<String, String> entry : session.config.customResponseHeaders.entrySet()) {
                    String key = entry.getKey();
                    if (!TextUtils.isEmpty(key)) {
                        tmpHeaderList = cachedResponseHeaders.get(key.toLowerCase());
                        if (null == tmpHeaderList) {
                            tmpHeaderList = new ArrayList<String>(1);
                            cachedResponseHeaders.put(key.toLowerCase(), tmpHeaderList);
                        }
                        tmpHeaderList.add(entry.getValue());
                    }
                }
            }

            // fill real response headers
            Map<String, List<String>> headersFromServer = connectionImpl.getResponseHeaderFields();
            if (null != headersFromServer && !headersFromServer.isEmpty()) {
                Set<Map.Entry<String, List<String>>> entrySet = headersFromServer.entrySet();
                for (Map.Entry<String, List<String>> entry : entrySet) {
                    String key = entry.getKey();
                    if (!TextUtils.isEmpty(key)) {
                        cachedResponseHeaders.put(key.toLowerCase(), entry.getValue());
                    }
                }
            }

        }

        return cachedResponseHeaders;
    }

    /**
     *
     * @param key  the name of a header field.
     * @return Returns the value of the named header field from SonicSessionConnection.
     */
    public String getResponseHeaderField(String key) {
        Map<String, List<String>> responseHeaderFields = getResponseHeaderFields();
        if (null != responseHeaderFields && 0 != responseHeaderFields.size()) {
            List<String> responseHeaderValues = responseHeaderFields.get(key.toLowerCase());
            if (null != responseHeaderValues && 0 != responseHeaderValues.size()) {
                StringBuilder stringBuilder = new StringBuilder(responseHeaderValues.get(0));
                for (int index = 1, size = responseHeaderValues.size(); index < size; ++index) {
                    stringBuilder.append(',');
                    stringBuilder.append(responseHeaderValues.get(index));
                }
                return stringBuilder.toString();
            }
        }
        return null;
    }

    /**
     * Read all of data from {@link SonicSessionConnection#getResponseStream()} into byte array output stream {@code outputStream} until
     * {@code breakCondition} is true when {@code breakCondition} is not null.
     * Then return a {@code SonicSessionStream} obtains input bytes
     * from  {@code outputStream} and a {@code netStream} when there is unread data from network.
     *
     * @param breakConditions This method won't read any data from {@link SonicSessionConnection#getResponseStream()} if {@code breakCondition} is true.
     * @return Returns a {@code SonicSessionStream} obtains input bytes
     * from  {@code outputStream} and a {@code netStream} when there is unread data from network.
     */
    public synchronized InputStream getResponseStream(AtomicBoolean breakConditions) {
        if (readServerResponse(breakConditions)) {
            BufferedInputStream netStream = !TextUtils.isEmpty(serverRsp) ? null : connectionImpl.getResponseStream();
            return new SonicSessionStream(this, outputStream, netStream);
        } else {
            return null;
        }
    }

    /**
     * Put key and value into http header.
     * @param key the name of a header field.
     * @param args the value which need to put into http header.
     */
    private void addResponseHeaderFields(String key, String... args) {
        ArrayList<String> field = new ArrayList<>(args.length);
        Collections.addAll(field, args);
        getResponseHeaderFields().put(key.toLowerCase(), field);
    }

    /**
     *  Return current cached server response data.
     *  If @{code readUntilEnd} is true and current cached response data is empty, read all of data from {@link SonicSessionConnection#getResponseStream()} into byte array output stream {@code outputStream}.
     *  And then this method convert outputStream into response string {@code serverRsp}. <br>
     * <p><b>Note: This method blocks until the end of the input stream has been reached or {@code breakCondition} has been reset to true.</b></p>
     *
     * @param readUntilEnd This method won't read any data from {@link SonicSessionConnection#getResponseStream()} if {@code readUntilEnd} is false.
     *
     * @return
     *      Returns {@code serverRsp} current cached server response data.
     */
    public synchronized String getResponseData(boolean readUntilEnd) {
        if (readUntilEnd && TextUtils.isEmpty(serverRsp)) {
            readServerResponse(null);
        }
        return serverRsp;
    }

    /**
     * If the serverRsp is not empty, It will separate serverRsp into template and data file and return template as string.
     * @return The template.
     */
    public synchronized String getTemplate() {
        if (TextUtils.isEmpty(templateString) && !TextUtils.isEmpty(serverRsp)) {
            separateTemplateAndData();
        }
        return templateString;
    }

    /**
     * If the serverRsp is not empty, It will separate serverRsp into template and data file and return data as JSONObject String.
     * @return the JSONObject String which represent data.
     */
    public synchronized String getUpdatedData() {
        if (TextUtils.isEmpty(dataString) && !TextUtils.isEmpty(serverRsp)) {
            separateTemplateAndData();
        }
        return dataString;
    }

    /**
     * Read all of data from {@link SonicSessionConnection#getResponseStream()} into byte array output stream {@code outputStream} until
     * {@code breakCondition} is true if {@code breakCondition} is not null.
     *  And then this method convert outputStream into response string {@code serverRsp} at the end of response stream.
     *
     * @param breakCondition This method won't read any data from {@link SonicSessionConnection#getResponseStream()} if {@code breakCondition} is true.
     * @return True when read any of data from {@link SonicSessionConnection#getResponseStream()} and write into {@code outputStream}
     */
    private boolean readServerResponse(AtomicBoolean breakCondition) {
        if (TextUtils.isEmpty(serverRsp)) {
            BufferedInputStream bufferedInputStream = connectionImpl.getResponseStream();
            if (null == bufferedInputStream) {
                SonicUtils.log(TAG, Log.ERROR, "session(" + session.sId + ") readServerResponse error: bufferedInputStream is null!");
                return false;
            }

            try {
                byte[] buffer = new byte[session.config.READ_BUF_SIZE];

                int n = 0;
                while (((breakCondition == null) || !breakCondition.get()) && -1 != (n = bufferedInputStream.read(buffer))) {
                    outputStream.write(buffer, 0, n);
                }

                if (n == -1) {
                    serverRsp = outputStream.toString(session.getCharsetFromHeaders());
                }
            } catch (Exception e) {
                SonicUtils.log(TAG, Log.ERROR, "session(" + session.sId + ") readServerResponse error:" + e.getMessage() + ".");
                return false;
            }
        }

        return true;
    }

    protected void separateTemplateAndData() {
        if (!TextUtils.isEmpty(serverRsp)) {
            StringBuilder templateStringBuilder = new StringBuilder();
            StringBuilder dataStringBuilder = new StringBuilder();
            String data = null;
            if (SonicUtils.separateTemplateAndData(session.id, serverRsp, templateStringBuilder, dataStringBuilder)) {
                templateString = templateStringBuilder.toString();
                data = dataStringBuilder.toString();
            }

            String eTag = getResponseHeaderField(getCustomHeadFieldEtagKey());
            String templateTag = getResponseHeaderField(CUSTOM_HEAD_FILED_TEMPLATE_TAG);
            String newHtmlSha1 = null;
            if (TextUtils.isEmpty(eTag)) { // When eTag is empty, fill eTag with Sha1
                newHtmlSha1 = eTag = SonicUtils.getSHA1(serverRsp);
                addResponseHeaderFields(getCustomHeadFieldEtagKey(), eTag);
                addResponseHeaderFields(CUSTOM_HEAD_FILED_HTML_SHA1, newHtmlSha1);
            }

            if (TextUtils.isEmpty(templateString)) { // The same with htmlString
                templateString = serverRsp;
                addResponseHeaderFields(CUSTOM_HEAD_FILED_TEMPLATE_TAG, eTag);
            } else if (TextUtils.isEmpty(templateTag)){ // When eTag is empty, fill templateTag with Sha1 of templateString
                addResponseHeaderFields(CUSTOM_HEAD_FILED_TEMPLATE_TAG, SonicUtils.getSHA1(templateString));
            }

            if (!TextUtils.isEmpty(data)) {
                try {
                    JSONObject object = new JSONObject();
                    object.put("data", new JSONObject(data));
                    if (TextUtils.isEmpty(newHtmlSha1)) {
                        newHtmlSha1 = SonicUtils.getSHA1(serverRsp);
                        addResponseHeaderFields(CUSTOM_HEAD_FILED_HTML_SHA1, newHtmlSha1);
                    }
                    object.put("html-sha1", getResponseHeaderField(CUSTOM_HEAD_FILED_HTML_SHA1));
                    object.put("template-tag", getResponseHeaderField(CUSTOM_HEAD_FILED_TEMPLATE_TAG));
                    dataString = object.toString();
                } catch (Exception e) {
                    SonicUtils.log(TAG, Log.ERROR, "session(" + session.sId + ") parse server response data error:" + e.getMessage() + ".");
                }
            }
        }
    }

    public String getCustomHeadFieldEtagKey() {
        return connectionImpl != null ? connectionImpl.getCustomHeadFieldEtagKey() : SonicSessionConnection.CUSTOM_HEAD_FILED_ETAG;
    }

    @Override
    public void onClose(boolean readComplete, ByteArrayOutputStream outputStream) {
        if (TextUtils.isEmpty(serverRsp) && readComplete && outputStream != null) {
            try {
                serverRsp = outputStream.toString(session.getCharsetFromHeaders());
                outputStream.close();
            } catch (Throwable e) {
                SonicUtils.log(TAG, Log.ERROR, "session(" + session.sId + "), onClose error:" + e.getMessage() + ".");
            }
        }
        session.onServerClosed(this, readComplete);
    }

}
