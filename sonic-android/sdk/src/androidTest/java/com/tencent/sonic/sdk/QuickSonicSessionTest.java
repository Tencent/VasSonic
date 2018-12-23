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

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.nio.charset.Charset;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class QuickSonicSessionTest extends BaseSonicTest{

    @Test
    public void handleFlow_NotModified() throws Exception {
        handleFlow_FirstLoad();
        desroySonicSession();

        final String etag = SonicUtils.getSHA1(SonicTestData.htmlString);
        final String templateTag = SonicUtils.getSHA1(SonicTestData.templateString);

        server.enqueue(mock304Response());
        initSonicSession();

        sonicSession.addSessionCallback(new SonicSessionCallback.SimpleCallbackImpl() {
            @Override
            public void onSessionLoadLocalCache(String cacheHtml) {
                assertTrue(!TextUtils.isEmpty(cacheHtml));
                assertEquals(cacheHtml, SonicTestData.htmlString);
            }

            @Override
            public void onSessionHitCache() {
                if (finishLock.compareAndSet(false, true)) {
                    synchronized (finishLock) {
                        finishLock.notify();
                    }
                }
            }

            @Override
            public void onSessionDestroy() {
                if (destroyLock.compareAndSet(false, true)) {
                    synchronized (destroyLock) {
                        destroyLock.notify();
                    }
                }
            }
        });

        mockLoadUrl();

        if (!finishLock.get()) {
            synchronized (finishLock) {
                finishLock.wait();
            }
        }

        RecordedRequest request = server.takeRequest();
        assertEquals("true", request.getHeader(SonicSessionConnection.CUSTOM_HEAD_FILED_ACCEPT_DIFF));
        assertEquals(etag, request.getHeader(SonicSessionConnection.HTTP_HEAD_FILED_IF_NOT_MATCH));
    }


    @Test
    public void handleFlow_DataUpdate() throws Exception {
        handleFlow_FirstLoad();
        desroySonicSession();

        final MockResponse dataUpdatedResponse = mockDataUpdatedResponse();
        server.enqueue(dataUpdatedResponse);

        final String newTag = dataUpdatedResponse.getHeaders().get(SonicSessionConnection.CUSTOM_HEAD_FILED_ETAG);
        final String etag = SonicUtils.getSHA1(SonicTestData.htmlString);
        final String templateTag = SonicUtils.getSHA1(SonicTestData.templateString);

        initSonicSession();

        sonicSessionClient.getDiffData(new SonicDiffDataCallback() {
            @Override
            public void callback(String resultData) {
                try {
                    JSONObject callbackJson = new JSONObject(resultData);
                    assertTrue(callbackJson.optInt(SonicSession.WEB_RESPONSE_SRC_CODE) == SonicSession.SONIC_RESULT_CODE_DATA_UPDATE);
                } catch (Exception e) {
                    e.printStackTrace();
                    assertFalse(e != null);
                }
            }
        });

        sonicSession.addSessionCallback(new SonicSessionCallback.SimpleCallbackImpl() {
            @Override
            public void onSessionLoadLocalCache(String cacheHtml) {
                assertTrue(!TextUtils.isEmpty(cacheHtml));
                assertEquals(cacheHtml, SonicTestData.htmlString);
            }

            public void onSessionDataUpdated(String serverRsp) {
                assertEquals(serverRsp, dataUpdatedResponse.getBody().clone().readString(Charset.forName("UTF-8")));
            }

            public void onSessionSaveCache(String htmlString, String templateString, String dataString) {
                assertEquals(SonicUtils.getSHA1(htmlString), newTag);

                try {
                    JSONObject serverRsp = new JSONObject(dataUpdatedResponse.getBody().clone().readString(Charset.forName("UTF-8")));
                    assertEquals(dataString, serverRsp.optString("data"));
                } catch (Exception e) {
                    assertFalse(e != null);
                }

                if (finishLock.compareAndSet(false, true)) {
                    synchronized (finishLock) {
                        finishLock.notify();
                    }
                }

            }

            @Override
            public void onSessionDestroy() {
                if (destroyLock.compareAndSet(false, true)) {
                    synchronized (destroyLock) {
                        destroyLock.notify();
                    }
                }
            }
        });

        mockLoadUrl();

        if (!finishLock.get()) {
            synchronized (finishLock) {
                finishLock.wait();
            }
        }

        RecordedRequest request = server.takeRequest();
        assertEquals("true", request.getHeader(SonicSessionConnection.CUSTOM_HEAD_FILED_ACCEPT_DIFF));
        assertEquals(etag, request.getHeader(SonicSessionConnection.HTTP_HEAD_FILED_IF_NOT_MATCH));
        assertEquals(templateTag, request.getHeader(SonicSessionConnection.CUSTOM_HEAD_FILED_TEMPLATE_TAG));
    }

    @Test
    public void handleFlow_TemplateChange() throws Exception {
        handleFlow_FirstLoad();
        desroySonicSession();

        final String etag = SonicUtils.getSHA1(SonicTestData.htmlString);
        final String templateTag = SonicUtils.getSHA1(SonicTestData.templateString);

        MockResponse templateChangedResponse = mockTemplateChangedResponse();
        final String newTag = templateChangedResponse.getHeaders().get(SonicSessionConnection.CUSTOM_HEAD_FILED_ETAG);
        final String newTemplateTag = templateChangedResponse.getHeaders().get(SonicSessionConnection.CUSTOM_HEAD_FILED_TEMPLATE_TAG);

        server.enqueue(templateChangedResponse);

        initSonicSession();

        sonicSessionClient.getDiffData(new SonicDiffDataCallback() {
            @Override
            public void callback(String resultData) {
                try {
                    JSONObject callbackJson = new JSONObject(resultData);
                    assertTrue(callbackJson.optInt(SonicSession.WEB_RESPONSE_SRC_CODE) == SonicSession.SONIC_RESULT_CODE_TEMPLATE_CHANGE);
                } catch (Exception e) {
                    assertFalse(e != null);
                }
            }
        });

        sonicSession.addSessionCallback(new SonicSessionCallback.SimpleCallbackImpl() {
            @Override
            public void onSessionLoadLocalCache(String cacheHtml) {
                assertTrue(!TextUtils.isEmpty(cacheHtml));
                assertEquals(cacheHtml, SonicTestData.htmlString);
            }

            public void onSessionTemplateChanged(String newHtml) {
                assertEquals(SonicUtils.getSHA1(newHtml), newTag);
            }

            public void onSessionSaveCache(String htmlString, String templateString, String dataString) {
                assertEquals(SonicUtils.getSHA1(htmlString), newTag);
                assertEquals(SonicUtils.getSHA1(templateString), newTemplateTag);

                if (finishLock.compareAndSet(false, true)) {
                    synchronized (finishLock) {
                        finishLock.notify();
                    }
                }

            }

            @Override
            public void onSessionDestroy() {
                if (destroyLock.compareAndSet(false, true)) {
                    synchronized (destroyLock) {
                        destroyLock.notify();
                    }
                }
            }
        });

        mockLoadUrl();

        if (!finishLock.get()) {
            synchronized (finishLock) {
                finishLock.wait();
            }
        }

        RecordedRequest request = server.takeRequest();
        assertEquals("true", request.getHeader(SonicSessionConnection.CUSTOM_HEAD_FILED_ACCEPT_DIFF));
        assertEquals(etag, request.getHeader(SonicSessionConnection.HTTP_HEAD_FILED_IF_NOT_MATCH));
        assertEquals(templateTag, request.getHeader(SonicSessionConnection.CUSTOM_HEAD_FILED_TEMPLATE_TAG));
    }

    @Test
    public void handleFlow_HttpError() throws Exception {
    }

    @Test
    public void handleFlow_ServiceUnavailable() throws Exception {
    }

    @Test
    public void handleFlow_FirstLoad() throws Exception {
        initSonicSession();

        final String etag = SonicUtils.getSHA1(SonicTestData.htmlString);
        final String templateTag = SonicUtils.getSHA1(SonicTestData.templateString);

        server.enqueue(mockFirstLoadResponse());

        sonicSessionClient.getDiffData(new SonicDiffDataCallback() {
            @Override
            public void callback(final String resultData) {
                try {
                    JSONObject callbackJson = new JSONObject(resultData);
                    assertTrue(callbackJson.optInt(SonicSession.WEB_RESPONSE_SRC_CODE) == SonicSession.SONIC_RESULT_CODE_FIRST_LOAD);
                } catch (Exception e) {
                    assertFalse(e != null);
                }
            }
        });

        sonicSession.addSessionCallback(new SonicSessionCallback.SimpleCallbackImpl() {
            @Override
            public void onSessionLoadLocalCache(String cacheHtml) {
                assertTrue(TextUtils.isEmpty(cacheHtml));
            }

            public void onSessionSaveCache(String htmlString, String templateString, String dataString) {
                assertEquals(SonicTestData.htmlString, htmlString);
                assertEquals(SonicTestData.templateString, templateString);

                JSONObject serverRspJson = null;
                JSONObject testServerRspJson = null;
                try {
                    serverRspJson = new JSONObject(dataString);
                    testServerRspJson = new JSONObject(SonicTestData.dataString);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                String htmlSha1 = serverRspJson.optString("html-sha1");
                String templateTag1 = serverRspJson.optString("template-tag");
                assertEquals(etag, htmlSha1);
                assertEquals(templateTag, templateTag1);
                assertEquals(testServerRspJson.optString("data").toString(), serverRspJson.optJSONObject("data").toString());

                if (finishLock.compareAndSet(false, true)) {
                    synchronized (finishLock) {
                        finishLock.notify();
                    }
                }
            }

            @Override
            public void onSessionDestroy() {
                if (destroyLock.compareAndSet(false, true)) {
                    synchronized (destroyLock) {
                        destroyLock.notify();
                    }
                }
            }
        });

        mockLoadUrl();

        mockInterceptRequest(SonicTestData.htmlString);

        if (!finishLock.get()) {
            synchronized (finishLock) {
                finishLock.wait();
            }
        }

        RecordedRequest request = server.takeRequest();
        assertEquals("true", request.getHeader("accept-diff"));
        assertTrue(TextUtils.isEmpty(request.getHeader(SonicSessionConnection.HTTP_HEAD_FILED_IF_NOT_MATCH)));
        assertTrue(TextUtils.isEmpty(request.getHeader(SonicSessionConnection.CUSTOM_HEAD_FILED_TEMPLATE_TAG)));
    }

}