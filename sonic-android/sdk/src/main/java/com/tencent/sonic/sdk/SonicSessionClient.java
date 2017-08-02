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

import android.os.Bundle;

import java.util.HashMap;

/**
 * <code>SonicSessionClient</code> is a thin API class that delegates its public API to
 * a backend WebView class instance, such as loadUrl and loadDataWithBaseUrl.
 */

public abstract class SonicSessionClient {

    /**
     * Sonic session of current(this) client
     */
    private SonicSession session;

    /**
     * Notify client is ready to accept data
     */
    public void clientReady() {
        if (session != null) {
            session.onClientReady();
        }
    }

    /**
     * Webview ask the host client to intercept request, this method should be called when webview
     * call shouldInterceptRequest.
     *
     * @param url The target url which need to request web response
     *
     * @return The data to kernel.
     */
    public Object requestResource(String url) {
        if (session != null) {
            return session.onClientRequestResource(url);
        }
        return null;
    }

    /**
     * The page execute a java script function to invoke a native method by javascript interface,
     * this callback will be called when sonic has finished diff data.
     * @param callback A callback of web page
     */
    public void getDiffData(SonicDiffDataCallback callback) {
        if (session != null) {
            session.onWebReady(callback);
        }
    }

    /**
     * We need to tell the session when onPageFinished is called by WebViewClient since to make a
     * better reload when current hit template-changed case.
     *
     * @param url The target url which is page finished
     */
    public void pageFinish(String url) {
        if (session != null) {
            session.onClientPageFinished(url);
        }
    }

    /**
     * Bind a sonic session to current client
     *
     * @param session A sonic session
     */
    public void bindSession(SonicSession session) {
        this.session = session;
    }

    /**
     * We add this method to decoupling webview since some application may use x5 webview or others.
     *
     * @param url   Url which need to load
     * @param extraData Extra data
     */
    public abstract void loadUrl(String url, Bundle extraData);


    /**
     * We add this method to decoupling webview since some application may use x5 webview or others.
     *
     * @param baseUrl    The URL to use as the page's base URL. If null defaults to
     *                   'about:blank'.
     * @param data       A String of data in the given encoding
     * @param mimeType   the MIMEType of the data, e.g. 'text/html'. If null,
     *                   defaults to 'text/html'.
     * @param encoding   The encoding of the data
     * @param historyUrl The URL to use as the history entry. If null defaults
     *                   to 'about:blank'. If non-null, this must be a valid URL.
     */
    public abstract void loadDataWithBaseUrl(String baseUrl, String data, String mimeType, String encoding, String historyUrl);

    /**
     * We add this method to decoupling webview since some application may use x5 webview or others.
     *
     * @param baseUrl    The URL to use as the page's base URL. If null defaults to
     *                   'about:blank'.
     * @param data       A String of data in the given encoding
     * @param mimeType   The MIMEType of the data, e.g. 'text/html'. If null,
     *                   defaults to 'text/html'.
     * @param encoding   The encoding of the data
     * @param historyUrl The URL to use as the history entry. If null defaults
     *                   to 'about:blank'. If non-null, this must be a valid URL.
     * @param headers    The headers
     */
    public abstract void loadDataWithBaseUrlAndHeader(String baseUrl, String data, String mimeType, String encoding, String historyUrl, HashMap<String, String> headers);

    /**
     * We add this method to decoupling webview since some application may use x5 webview or others.
     * When we hit template-change case, webview may load twice(first load:local cache second load:new page)
     * when user press back button, if we do not clear history,it will return to the first load case
     * which will make user feel unsure about the action. So we need to clear history if need.
     */
    public void clearHistory() {

    }

}
