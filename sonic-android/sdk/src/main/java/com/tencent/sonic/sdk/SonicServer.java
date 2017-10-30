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
import java.util.concurrent.atomic.AtomicBoolean;

import static com.tencent.sonic.sdk.SonicSession.OFFLINE_MODE_HTTP;
import static com.tencent.sonic.sdk.SonicSession.OFFLINE_MODE_TRUE;
import static com.tencent.sonic.sdk.SonicSessionConnection.CUSTOM_HEAD_FILED_HTML_SHA1;
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

    public SonicServer(SonicSession session, SonicDataHelper.SessionData sessionData) {
        this.session = session;
        this.requestIntent = session.createConnectionIntent(sessionData);
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
        responseCode = connectionImpl.connect();

        //Handle weak ETag
        String eTag = getResponseHeaderField(SonicSessionConnection.CUSTOM_HEAD_FILED_ETAG);
        if (!TextUtils.isEmpty(eTag) && eTag.toLowerCase().startsWith("w/")) {
            eTag = eTag.toLowerCase().replace("w/", "");
            eTag = eTag.replace("\"", "");
            addResponseHeaderFields(SonicSessionConnection.CUSTOM_HEAD_FILED_ETAG, eTag);
        }

        if (responseCode == SonicConstants.ERROR_CODE_SUCCESS && session.config.SUPPORT_SONIC_SERVER) {
            String cacheOffline = getResponseHeaderField(SonicSessionConnection.CUSTOM_HEAD_FILED_CACHE_OFFLINE);
            if (TextUtils.isEmpty(cacheOffline)) {
                cacheOffline = OFFLINE_MODE_TRUE;
                addResponseHeaderFields(SonicSessionConnection.CUSTOM_HEAD_FILED_CACHE_OFFLINE, cacheOffline);
            } else if (OFFLINE_MODE_HTTP.equalsIgnoreCase(cacheOffline)) {
                // When cache-offline is "http": which means sonic server is in bad condition, need feed back to run standard http request.
                return SonicConstants.ERROR_CODE_SUCCESS;
            }

            String requestETag = requestIntent.getStringExtra(SonicSessionConnection.CUSTOM_HEAD_FILED_ETAG);
            if (!TextUtils.isEmpty(requestETag)) {
                if (HttpURLConnection.HTTP_OK == getResponseCode()) {
                    String templateChange = getResponseHeaderField(SonicSessionConnection.CUSTOM_HEAD_FILED_TEMPLATE_CHANGE);

                    //standard sonic response.
                    if (!TextUtils.isEmpty(eTag) && !TextUtils.isEmpty(templateChange)) {
                        return SonicConstants.ERROR_CODE_SUCCESS;
                    }

                    SonicDataHelper.SessionData sessionData = SonicDataHelper.getSessionData(session.id);
                    String templateTag = getResponseHeaderField(CUSTOM_HEAD_FILED_TEMPLATE_TAG);

                    // When eTag is empty, run fix logic
                    if (TextUtils.isEmpty(eTag)) {
                        // Try fix eTag
                        readServerResponse(null);

                        if (!TextUtils.isEmpty(serverRsp)) {
                            eTag = SonicUtils.getSHA1(serverRsp);
                            addResponseHeaderFields(SonicSessionConnection.CUSTOM_HEAD_FILED_ETAG, eTag);
                            addResponseHeaderFields(CUSTOM_HEAD_FILED_HTML_SHA1, eTag);
                        } else {
                            return SonicConstants.ERROR_CODE_CONNECT_IOE;
                        }
                    }

                    // After fix eTag, which may hit 304
                    if (!TextUtils.isEmpty(eTag) && eTag.equals(sessionData.eTag)) {
                        responseCode = HttpURLConnection.HTTP_NOT_MODIFIED;
                        return SonicConstants.ERROR_CODE_SUCCESS;
                    }

                    //fix templateTag
                    if (TextUtils.isEmpty(templateChange) && TextUtils.isEmpty(templateTag)) {
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
                    if (!TextUtils.isEmpty(templateTag) && templateTag.equals(sessionData.templateTag)) {
                        addResponseHeaderFields(SonicSessionConnection.CUSTOM_HEAD_FILED_TEMPLATE_CHANGE, "false");
                    } else {
                        addResponseHeaderFields(SonicSessionConnection.CUSTOM_HEAD_FILED_TEMPLATE_CHANGE, "true");
                    }
                }
            }
        }

        return SonicConstants.ERROR_CODE_SUCCESS;
    }

    public boolean isHttpNotModified() {
        if (HttpURLConnection.HTTP_NOT_MODIFIED == responseCode) {
            return true;
        } else {
            String requestETag = requestIntent.getStringExtra(SonicSessionConnection.CUSTOM_HEAD_FILED_ETAG);
            String responseETag = getResponseHeaderField(SonicSessionConnection.CUSTOM_HEAD_FILED_ETAG);
            if (!TextUtils.isEmpty(requestETag) && requestETag.equalsIgnoreCase(responseETag)) {
                return true;
            }
        }

        return false;
    }

    public int getResponseCode() {
        if (responseCode != 0) {
            return responseCode;
        } else {
            return connectionImpl.getResponseCode();
        }
    }

    /**
     * Disconnect the communications link to the resource referenced by Sonic session
     */
    public  void disconnect() {
        connectionImpl.disconnect();
    }


    /**
     *  return response headers which contains response headers from server and custom response headers from
     *  {@code com.tencent.sonic.sdk.SonicSessionConfig}
     *  note: server response headers have high priority than custom headers!
     */
    public  Map<String, List<String>> getResponseHeaderFields() {
        if (null == cachedResponseHeaders) {
            // new cachedResponseHeaders
            cachedResponseHeaders = new HashMap<String, List<String>>();
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
            Set<Map.Entry<String, List<String>>> entrySet = headersFromServer.entrySet();
            for (Map.Entry<String, List<String>> entry : entrySet) {
                String key = entry.getKey();
                if (!TextUtils.isEmpty(key)) {
                    cachedResponseHeaders.put(key.toLowerCase(), entry.getValue());
                } else {
                    cachedResponseHeaders.put(key, entry.getValue());
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

    private void separateTemplateAndData() {
        if (!TextUtils.isEmpty(serverRsp)) {
            StringBuilder templateStringBuilder = new StringBuilder();
            StringBuilder dataStringBuilder = new StringBuilder();
            String data = null;
            String templateTag ;
            if (SonicUtils.separateTemplateAndData(session.id, serverRsp, templateStringBuilder, dataStringBuilder)) {
                templateString = templateStringBuilder.toString();
                data = dataStringBuilder.toString();
            }

            if (TextUtils.isEmpty(templateString)) {
                templateString = serverRsp;
                templateTag = getResponseHeaderField(CUSTOM_HEAD_FILED_HTML_SHA1);
            } else {
                templateTag = SonicUtils.getSHA1(templateString);
            }
            addResponseHeaderFields(CUSTOM_HEAD_FILED_TEMPLATE_TAG, templateTag);


            if (!TextUtils.isEmpty(data)) {
                try {
                    JSONObject object = new JSONObject();
                    object.put("data", new JSONObject(data));
                    object.put("html-sha1", getResponseHeaderField(CUSTOM_HEAD_FILED_HTML_SHA1));
                    object.put("template-tag", templateTag);

                    dataString = object.toString();
                } catch (Exception e) {
                    SonicUtils.log(TAG, Log.ERROR, "session(" + session.sId + ") parse server response data error:" + e.getMessage() + ".");
                }
            }
        }
    }

    @Override
    public void onClose(boolean readComplete, ByteArrayOutputStream outputStream) {
        if (TextUtils.isEmpty(serverRsp) && readComplete) {
            try {
                serverRsp = outputStream.toString(session.getCharsetFromHeaders());
                String eTag = getResponseHeaderField(SonicSessionConnection.CUSTOM_HEAD_FILED_ETAG);
                if (!TextUtils.isEmpty(serverRsp) && TextUtils.isEmpty(eTag)) {
                    eTag = SonicUtils.getSHA1(serverRsp);
                    addResponseHeaderFields(CUSTOM_HEAD_FILED_HTML_SHA1, eTag);
                    addResponseHeaderFields(SonicSessionConnection.CUSTOM_HEAD_FILED_ETAG, eTag);
                }
            } catch (Exception e) {
                SonicUtils.log(TAG, Log.ERROR, "session(" + session.sId + "), onClose error:" + e.getMessage() + ".");
            }
        }
        session.onServerClosed(readComplete);
    }

}
