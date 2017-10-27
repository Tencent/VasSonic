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

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.tencent.sonic.sdk.SonicSession.OFFLINE_MODE_HTTP;
import static com.tencent.sonic.sdk.SonicSession.OFFLINE_MODE_TRUE;
import static com.tencent.sonic.sdk.SonicSessionConnection.CUSTOM_HEAD_FILED_HTML_SHA1;
import static com.tencent.sonic.sdk.SonicSessionConnection.CUSTOM_HEAD_FILED_TEMPLATE_TAG;

/**
 * Instances of this class can be used to read server response from SonicSessionConnection.
 * If this request support Local Sonic Server, it will separate html into template and data file.
 */
public class SonicServer {
    public static final String TAG = "SonicServer";

    /**
     * A session connection implement.
     */
    protected final SonicSessionConnection connectionImpl;

    protected String serverRsp;

    protected String templateString;

    protected String dataString;

    protected int responseCode;

    final protected SonicSession session;

    final protected Intent intent;

    public SonicServer(SonicSession session, SonicDataHelper.SessionData sessionData) {
        this.session = session;
        this.intent = session.createConnectionIntent(sessionData);
        connectionImpl = SonicSessionConnectionInterceptor.getSonicSessionConnection(session, intent);
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
        String eTag = connectionImpl.getResponseHeaderField(SonicSessionConnection.CUSTOM_HEAD_FILED_ETAG);
        if (!TextUtils.isEmpty(eTag) && eTag.toLowerCase().startsWith("w/")) {
            eTag = eTag.toLowerCase().replace("w/", "");
            eTag = eTag.replace("\"", "");
            addResponseHeaderFields(SonicSessionConnection.CUSTOM_HEAD_FILED_ETAG, eTag);
        }

        if (responseCode == SonicConstants.ERROR_CODE_SUCCESS && session.config.SUPPORT_SONIC_SERVER) {
            String cacheOffline = connectionImpl.getResponseHeaderField(SonicSessionConnection.CUSTOM_HEAD_FILED_CACHE_OFFLINE);
            if (TextUtils.isEmpty(cacheOffline)) {
                cacheOffline = OFFLINE_MODE_TRUE;
                addResponseHeaderFields(SonicSessionConnection.CUSTOM_HEAD_FILED_CACHE_OFFLINE, cacheOffline);
            } else if (OFFLINE_MODE_HTTP.equalsIgnoreCase(cacheOffline)) {
                // When cache-offline is "http": which means sonic server is in bad condition, need feed back to run standard http request.
                return SonicConstants.ERROR_CODE_SUCCESS;
            }

            String requestETag = intent.getStringExtra(SonicSessionConnection.CUSTOM_HEAD_FILED_ETAG);
            if (!TextUtils.isEmpty(requestETag)) {
                if (HttpURLConnection.HTTP_OK == getResponseCode()) {
                    String templateChange = connectionImpl.getResponseHeaderField(SonicSessionConnection.CUSTOM_HEAD_FILED_TEMPLATE_CHANGE);

                    //standard sonic response.
                    if (!TextUtils.isEmpty(eTag) && !TextUtils.isEmpty(templateChange)) {
                        return SonicConstants.ERROR_CODE_SUCCESS;
                    }

                    SonicDataHelper.SessionData sessionData = SonicDataHelper.getSessionData(session.id);
                    String templateTag = connectionImpl.getResponseHeaderField(CUSTOM_HEAD_FILED_TEMPLATE_TAG);

                    // When eTag is empty, run fix logic
                    if (TextUtils.isEmpty(eTag)) {
                        // Try fix eTag
                        readServerResponse();

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
                            readServerResponse();
                        }

                        if (!TextUtils.isEmpty(serverRsp)) {
                            separateTemplateAndData();
                            templateTag = connectionImpl.getResponseHeaderField(CUSTOM_HEAD_FILED_TEMPLATE_TAG);
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

    public  Map<String, List<String>> getResponseHeaderFields() {
        return connectionImpl.getResponseHeaderFields();
    }

    /**
     *
     * @param key  the name of a header field.
     * @return Returns the value of the named header field from SonicSessionConnection.
     */
    public  String getResponseHeaderField(String key) {
        return connectionImpl.getResponseHeaderField(key);
    }

    /**
     * Translate current cached ServerRsp into ByteArrayOutputStream if the ServerRsp is not empty.
     * Or reads all of data from {@link SonicSessionConnection#getResponseStream()} into byte array output stream. <br>
     * <p><b>Note: This method blocks until the end of the input stream has been reached</b></p>
     *
     * @return Returns a ByteArrayOutputStream that holds all of server response data.
     */
    public synchronized ByteArrayOutputStream getResponseData() {
        String serverRsp = getServerRsp();
        if (!TextUtils.isEmpty(serverRsp)) {
            try {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                byte[] bytes = serverRsp.getBytes(session.getCharsetFromHeaders());
                outputStream.write(bytes, 0, bytes.length);
                return outputStream;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return connectionImpl.getResponseData();
    }

    /**
     * This method returns {@link SonicSessionConnection.ResponseDataTuple} object which holds the current cached ServerRsp if it is not empty. <br>
     *
     * Or try call {@link SonicSessionConnection#getResponseData()} to return {@link SonicSessionConnection.ResponseDataTuple} object  which holds the input and output stream. <<br>
     * <p><b>Note: This method blocks until the end of the input stream has been reached or {@code breakCondition} has been reset to true.</b></p>
     *
     * @param breakCondition This method won't read any data from {@link SonicSessionConnection#getResponseStream()} if {@code breakCondition} is false when ServerRsp is empty..
     * @param outputStream   This method will reuse this byte array output stream instead of creating new output stream.
     * @return
     *      Returns {@link SonicSessionConnection.ResponseDataTuple} caches information about the all of the stream and the state which indicates there is no more data.
     */
    public synchronized SonicSessionConnection.ResponseDataTuple getResponseData(AtomicBoolean breakCondition, ByteArrayOutputStream outputStream) {
        if (!TextUtils.isEmpty(getServerRsp())) {
            SonicSessionConnection.ResponseDataTuple responseDataTuple = new SonicSessionConnection.ResponseDataTuple();
            responseDataTuple.htmlString = getServerRsp();
            responseDataTuple.isComplete = true;
        }

        return connectionImpl.getResponseData(breakCondition, outputStream);
    }

    /**
     * Put key and value into http header.
     * @param key the name of a header field.
     * @param args the value which need to put into http header.
     */
    private void addResponseHeaderFields(String key, String... args) {
        ArrayList<String> field = new ArrayList<>(args.length);
        Collections.addAll(field, args);
        connectionImpl.getResponseHeaderFields().put(key.toLowerCase(), field);
    }

    public synchronized String getServerRsp() {
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
     * Set current ServerRsp and calculate the SHA1 of ServerRsp and put into http header.
     * @param response server response data.
     */
    protected void setServerRsp(String response) {
        serverRsp = response;

        String eTag = connectionImpl.getResponseHeaderField(SonicSessionConnection.CUSTOM_HEAD_FILED_ETAG);
        if (!TextUtils.isEmpty(serverRsp) && TextUtils.isEmpty(eTag)) {
            eTag = SonicUtils.getSHA1(serverRsp);
            addResponseHeaderFields(CUSTOM_HEAD_FILED_HTML_SHA1, eTag);
            addResponseHeaderFields(SonicSessionConnection.CUSTOM_HEAD_FILED_ETAG, eTag);
        }
    }

    private void readServerResponse() {
        if (TextUtils.isEmpty(serverRsp)) {
            ByteArrayOutputStream byteArrayOutputStream = connectionImpl.getResponseData();
            if (null == byteArrayOutputStream) {
                SonicUtils.log(TAG, Log.ERROR, "session(" + session.sId + ") readServerResponse error: getResponseData is null!");
                return;
            }

            try {
                serverRsp = byteArrayOutputStream.toString(session.getCharsetFromHeaders());
            } catch (UnsupportedEncodingException e) {
                SonicUtils.log(TAG, Log.ERROR, "session(" + session.sId + ") readServerResponse error:" + e.getMessage() + ".");
            }
        }
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
                templateTag = connectionImpl.getResponseHeaderField(CUSTOM_HEAD_FILED_HTML_SHA1);
            } else {
                templateTag = SonicUtils.getSHA1(templateString);
            }
            addResponseHeaderFields(CUSTOM_HEAD_FILED_TEMPLATE_TAG, templateTag);


            if (!TextUtils.isEmpty(data)) {
                try {
                    JSONObject object = new JSONObject();
                    object.put("data", new JSONObject(data));
                    object.put("html-sha1", connectionImpl.getResponseHeaderField(CUSTOM_HEAD_FILED_HTML_SHA1));
                    object.put("template-tag", templateTag);

                    dataString = object.toString();
                } catch (Exception e) {
                    SonicUtils.log(TAG, Log.ERROR, "session(" + session.sId + ") parse server response data error:" + e.getMessage() + ".");
                }
            }
        }
    }
}
