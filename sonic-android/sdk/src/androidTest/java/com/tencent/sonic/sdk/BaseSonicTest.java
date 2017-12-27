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

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.test.InstrumentationRegistry;
import android.util.Log;
import android.webkit.WebResourceResponse;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static org.junit.Assert.assertEquals;


@RunWith(JUnit4.class)
public class BaseSonicTest implements Handler.Callback {
    private static final String TAG = "SonicSdk_BaseSonicTest";

    protected SonicSession sonicSession;
    protected SonicSessionClientImplTest sonicSessionClient;
    protected final Handler mainHandler = new Handler(Looper.getMainLooper(), this);
    protected String url;

    final AtomicBoolean destroyLock = new AtomicBoolean(false);
    final AtomicBoolean finishLock = new AtomicBoolean(false);

    @Rule
    public final MockWebServer server = new MockWebServer();

    protected void initSonicSession()  {
        // init sonic engine if necessary, or maybe u can do this when application created
        // step 1: Initialize sonic engine if necessary, or maybe u can do this when application created

        sonicSessionClient = null;

        // step 2: Create SonicSession
        url = server.url("/").toString();
        sonicSession = SonicEngine.getInstance().createSession(url,  new SonicSessionConfig.Builder().build());
        if (null != sonicSession) {
            sonicSession.bindClient(sonicSessionClient = new SonicSessionClientImplTest());
        } else {
            // this only happen when a same sonic session is already running,
            // u can comment following codes to feedback as a default mode.
            throw new UnknownError("create session fail!");
        }

    }

    @Before
    public void setUp() throws Exception {
        Log.d(TAG, "setUp() clear all sonic cache.");
        if (!SonicEngine.isGetInstanceAllowed()) {
            SonicEngine.createInstance(new SonicRuntimeImplTest(InstrumentationRegistry.getContext()), new SonicConfig.Builder().build());
        }
        SonicEngine.getInstance().cleanCache();
    }

    @After
    public void tearDown() throws Exception {
        desroySonicSession();
    }

    protected void desroySonicSession() throws InterruptedException {
        if (sonicSession != null) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    sonicSession.destroy();
                }
            });

            if (!destroyLock.get()) {
                synchronized (destroyLock) {
                    destroyLock.wait();
                }
            }
            sonicSession = null;
        }
        destroyLock.set(false);
        finishLock.set(false);
    }

    protected MockResponse mockFirstLoadResponse() {
        final String etag = SonicUtils.getSHA1(SonicTestData.htmlString);
        final String templateTag = SonicUtils.getSHA1(SonicTestData.templateString);

        Map<String, String> headers = new HashMap<>();
        headers.put(SonicSessionConnection.CUSTOM_HEAD_FILED_ETAG, etag);
        headers.put(SonicSessionConnection.CUSTOM_HEAD_FILED_TEMPLATE_TAG, templateTag);
        headers.put(SonicSessionConnection.CUSTOM_HEAD_FILED_CACHE_OFFLINE, "true");
        headers.put(SonicSessionConnection.CUSTOM_HEAD_FILED_TEMPLATE_CHANGE, "true");

        MockResponse response = new MockResponse();
        Set<Map.Entry<String, String >> entrySet = headers.entrySet();
        for (Map.Entry<String, String> entry : entrySet) {
            response.addHeader(entry.getKey(), entry.getValue());
        }
        response.setBody(SonicTestData.htmlString);
        return response;
    }

    protected MockResponse mock304Response() {
        final String etag = SonicUtils.getSHA1(SonicTestData.htmlString);
        final String templateTag = SonicUtils.getSHA1(SonicTestData.templateString);

        Map<String, String> headers = new HashMap<>();
        headers.put(SonicSessionConnection.CUSTOM_HEAD_FILED_ETAG, etag);
        headers.put(SonicSessionConnection.CUSTOM_HEAD_FILED_TEMPLATE_TAG, templateTag);
        headers.put(SonicSessionConnection.CUSTOM_HEAD_FILED_CACHE_OFFLINE, "true");
        headers.put(SonicSessionConnection.CUSTOM_HEAD_FILED_TEMPLATE_CHANGE, "false");

        MockResponse response = new MockResponse();
        Set<Map.Entry<String, String >> entrySet = headers.entrySet();
        for (Map.Entry<String, String> entry : entrySet) {
            response.addHeader(entry.getKey(), entry.getValue());
        }
        response.setBody("");
        response.setResponseCode(304);
        return response;
    }

    protected MockResponse mockTemplateChangedResponse() {
        String newHtml = SonicTestData.htmlString.replace("maxAge: 2592000", "maxAge: 3592000");
        String newTemplateHtml = SonicTestData.templateString.replace("maxAge: 2592000", "maxAge: 3592000");

        final String etag = SonicUtils.getSHA1(newHtml);
        final String templateTag = SonicUtils.getSHA1(newTemplateHtml);

        Map<String, String> headers = new HashMap<>();
        headers.put(SonicSessionConnection.CUSTOM_HEAD_FILED_ETAG, etag);
        headers.put(SonicSessionConnection.CUSTOM_HEAD_FILED_TEMPLATE_TAG, templateTag);
        headers.put(SonicSessionConnection.CUSTOM_HEAD_FILED_CACHE_OFFLINE, "true");
        headers.put(SonicSessionConnection.CUSTOM_HEAD_FILED_TEMPLATE_CHANGE, "true");

        MockResponse response = new MockResponse();
        Set<Map.Entry<String, String >> entrySet = headers.entrySet();
        for (Map.Entry<String, String> entry : entrySet) {
            response.addHeader(entry.getKey(), entry.getValue());
        }
        response.setBody(newHtml);
        return response;
    }

    protected MockResponse mockDataUpdatedResponse() {
        String newHtml = SonicTestData.htmlString.replace("img-1.png?max_age=2592000", "img-2.png?max_age=2592000");
        String newDataString = SonicTestData.dataString.replace("img-1.png?max_age=2592000", "img-2.png?max_age=2592000");

        final String templateTag = SonicUtils.getSHA1(SonicTestData.templateString);
        final String newHtmlSha1 = SonicUtils.getSHA1(newHtml);

        JSONObject testServerRspJson = null;
        try {
            testServerRspJson = new JSONObject(newDataString);
            testServerRspJson.put("template-tag", templateTag);
            testServerRspJson.put("html-sha1", newHtmlSha1);
        } catch (Exception e) {

        }

        Map<String, String> headers = new HashMap<>();
        headers.put(SonicSessionConnection.CUSTOM_HEAD_FILED_ETAG, newHtmlSha1);
        headers.put(SonicSessionConnection.CUSTOM_HEAD_FILED_TEMPLATE_TAG, templateTag);
        headers.put(SonicSessionConnection.CUSTOM_HEAD_FILED_CACHE_OFFLINE, "true");
        headers.put(SonicSessionConnection.CUSTOM_HEAD_FILED_TEMPLATE_CHANGE, "false");

        MockResponse response = new MockResponse();
        Set<Map.Entry<String, String >> entrySet = headers.entrySet();
        for (Map.Entry<String, String> entry : entrySet) {
            response.addHeader(entry.getKey(), entry.getValue());
        }
        response.setBody(testServerRspJson.toString());

        return response;

    }

    protected void mockLoadUrl() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                sonicSessionClient.clientReady();
            }
        });
    }

    protected void mockInterceptRequest(String html) throws IOException {
        WebResourceResponse resourceResponse = (WebResourceResponse) sonicSession.getSessionClient().requestResource(url);
        if (resourceResponse != null) {
            BufferedInputStream bufferedInputStream = new BufferedInputStream(resourceResponse.getData());
            byte[] buffer = new byte[1024];
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            String data = null;
            int n = 0;
            while (-1 != (n = bufferedInputStream.read(buffer))) {
                outputStream.write(buffer, 0, n);
            }

            if (n == -1) {
                data = outputStream.toString("UTF-8");
            }
            assertEquals(data, html);
            bufferedInputStream.close();
        }
    }


    @Override
    public boolean handleMessage(Message msg) {
        return false;
    }
}
