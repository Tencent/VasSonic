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

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


/**
 * <code>SonicRuntime</code> is a class which interacts with the overall running information in the system,
 * including Context, UA, ID (which is the unique identification for the saved data) and other information.
 */
public abstract class SonicRuntime {

    /**
     * Log filter
     */
    private final static String TAG = SonicConstants.SONIC_SDK_LOG_PREFIX + "SonicRuntime";

    /**
     * A context for this runtime, it's expected to be ApplicationContext
     */
    protected final Context context;

    /**
     * This handle thread use to save sonic cache.
     */
    protected volatile static HandlerThread fileHandlerThread;

    public SonicRuntime(Context context) {
        if (null == context) {
            throw new NullPointerException("SonicRuntime context con not be null!");
        }
        this.context = context;
    }

    public Context getContext() {
        return context;
    }


    /**
     * Make a unique session id for the url, it can be account related.
     * @param url Url which need to make session id
     * @param isAccountRelated Is account related or not
     * @return A unique session id
     */
    public String makeSessionId(String url, boolean isAccountRelated) {
        if (isSonicUrl(url)) {
            StringBuilder sessionIdBuilder = new StringBuilder();
            try {
                Uri uri = Uri.parse(url);
                sessionIdBuilder.append(uri.getAuthority()).append(uri.getPath());
                if (uri.isHierarchical()) {
                    String sonicRemainParams = uri.getQueryParameter(SonicConstants.SONIC_REMAIN_PARAMETER_NAMES);
                    TreeSet<String> remainParamTreeSet = new TreeSet<String>();
                    if (!TextUtils.isEmpty(sonicRemainParams)) {
                        Collections.addAll(remainParamTreeSet, sonicRemainParams.split(SonicConstants.SONIC_REMAIN_PARAMETER_SPLIT_CHAR));
                    }

                    TreeSet<String> parameterNamesTreeSet = new TreeSet<String>(getQueryParameterNames(uri));
                    if (!remainParamTreeSet.isEmpty()) {
                        parameterNamesTreeSet.remove(SonicConstants.SONIC_REMAIN_PARAMETER_NAMES);
                    }

                    for (String parameterName : parameterNamesTreeSet) {
                        if (!TextUtils.isEmpty(parameterName) && (parameterName.startsWith(SonicConstants.SONIC_PARAMETER_NAME_PREFIX) || remainParamTreeSet.contains(parameterName))) {
                            sessionIdBuilder.append(parameterName).append(uri.getQueryParameter(parameterName));
                        }
                    }
                }
            } catch (Throwable e) {
                log(TAG, Log.ERROR, "makeSessionId error:" + e.getMessage() + ", url=" + url);
                sessionIdBuilder.setLength(0);
                sessionIdBuilder.append(url);
            }
            String sessionId;
            if (isAccountRelated) {
                sessionId = getCurrentUserAccount() + "_" + SonicUtils.getMD5(sessionIdBuilder.toString());
            } else {
                sessionId = SonicUtils.getMD5(sessionIdBuilder.toString());
            }
            return sessionId;
        }
        return null;
    }

    /**
     * Returns a set of the unique names of all query parameters. Iterating
     * over the set will return the names in order of their first occurrence.
     *
     * @throws UnsupportedOperationException if this isn't a hierarchical URI
     *
     * @param uri The uri
     * @return A set of decoded names
     */
    public Set<String> getQueryParameterNames(Uri uri) {
        if (uri == null) {
            return Collections.emptySet();
        }

        if (uri.isOpaque()) {
            throw new UnsupportedOperationException("This isn't a hierarchical URI.");
        }

        String query = uri.getEncodedQuery();
        if (query == null) {
            return Collections.emptySet();
        }

        Set<String> names = new LinkedHashSet<String>();
        int start = 0;
        do {
            int next = query.indexOf('&', start);
            int end = (next == -1) ? query.length() : next;

            int separator = query.indexOf('=', start);
            if (separator > end || separator == -1) {
                separator = end;
            }

            String name = query.substring(start, separator);
            names.add(Uri.decode(name));

            // Move start to end of name.
            start = end + 1;
        } while (start < query.length());

        return Collections.unmodifiableSet(names);
    }

    /**
     * Logger function
     *
     * @param level Level of this log，such like Log.DEBUG.
     * @return Should log or not
     */
    public boolean shouldLog(int level) {
        return true;
    }

    public abstract void log(String tag, int level, String message);

    /**
     * Get cookies of the input url, this method will be called before sonic session make a
     * session connection to request data.
     *
     * @param url The url which need to get cookies
     * @return The cookies for current input url
     */
    public abstract String getCookie(String url);

    /**
     * Set cookies to webview after session connection response with cookies in it's headers.
     *
     * @param url The url which need to set cookies
     * @param cookies The cookies for current input url
     * @return Set cookie success or not
     */
    public abstract boolean setCookie(String url, List<String> cookies);

    /**
     * Get user agent of current runtime, this method will be called before sonic session make a
     * session connection to request data.(sonic sdk info such like "sonic-sdk-version/2.0.0" will
     * be added to this user agent.)
     * @return The user agent
     */
    public abstract String getUserAgent();

    /**
     * The sonic cache root dir which sonic cache such like .html/.template/.data will be storage.
     * it's expected to be a dir in /data dir for security.
     *
     * @return The root cache dir.
     */
    public File getSonicCacheDir() {
        String path = context.getFilesDir() + "/Sonic/";
        File file = new File(path.trim());
        if (!file.exists() && !file.mkdir()) {
            log(TAG, Log.ERROR, "getSonicCacheDir error:make dir(" + file.getAbsolutePath() + ") fail!");
            notifyError(null, path, SonicConstants.ERROR_CODE_MAKE_DIR_ERROR);
        }
        return file;
    }

    /**
     * The resource cache root dir which resource cache will be storage.
     * it's expected to be a dir in /sdcard dir for security.
     *
     * @return The root cache dir.
     */
    public File getSonicResourceCacheDir() {
        File file = new File(Environment.getExternalStorageDirectory(), "/SonicResource/");
        if (!file.exists() && !file.mkdir()) {
            log(TAG, Log.ERROR, "getSonicResourceCacheDir error:make dir(" + file.getAbsolutePath() + ") fail!");
            notifyError(null, file.getAbsolutePath(), SonicConstants.ERROR_CODE_MAKE_DIR_ERROR);
        }
        return file;
    }

    /**
     * get SharedPreferences of sonic.
     *
     * @return the sp
     */
    public SharedPreferences getSonicSharedPreferences() {
        return context.getSharedPreferences("sonic", Context.MODE_PRIVATE);
    }

    /**
     * Get the current user account, this method will be called when makeSessionId's params is
     * account related.
     *
     * @return Current user account
     */
    public abstract String getCurrentUserAccount();

    /**
     * This method is used to judge the input url is support sonic or not, when this method return
     * true, it means it's allow to create a sonic session for this url.
     * e.g. In mobile QQ, it will judge url params contain sonic=1 or not, if contains it will return
     * true, others return false.
     *
     * @param url The url which need to judge
     * @return Return is sonic url or not
     */
    public abstract boolean isSonicUrl(String url);

    /**
     * We add this method to decoupling webview since some application may use x5 webview or others.
     *
     * e.g. If u use a system webview, just call new android.webkit.WebResourceResponse
     *
     * Constructs a resource response with the given MIME type, encoding, and
     * input stream. Callers must implement
     * {@link InputStream#read(byte[]) InputStream.read(byte[])} for the input
     * stream.
     *
     * @param mimeType The resource response's MIME type, for example text/html
     * @param encoding The resource response's encoding
     * @param data     The input stream that provides the resource response's data. Must not be a
     *                 StringBufferInputStream.
     * @param headers  The headers
     *
     * @return The response to kernel
     */
    public abstract Object createWebResourceResponse(String mimeType, String encoding, InputStream data, Map<String, String> headers);

    /**
     * This method is used to judge is network valid or not
     *
     * @return Network valid or not
     */
    public abstract boolean isNetworkValid();

    /**
     * Get the direct address of a url(host)，format as[ip:port]，the default http port is 80 and
     * 443 for https.
     *
     * @param url The input url which need to get direct address
     * @return Return a valid direct address or null.
     */
    public String getHostDirectAddress(String url) {
        return null;
    }

    /**
     * Show toast
     *
     * @param text     Content
     * @param duration See Toast.LENGTH_SHORT/Toast.LENGTH_LONG
     */
    public abstract void showToast(CharSequence text, int duration);

    /**
     * Post a task to the thread(a io thread is better) which used to separate template and data.
     *
     * @param task A runnable task
     * @param delayMillis The delay (in milliseconds) until the Runnable
     *        will be executed.
     */
    public abstract void postTaskToThread(Runnable task, long delayMillis);

    /**
     * Post a task to session thread(a high priority thread is better)
     *
     * @param task A runnable task
     */
    public void postTaskToSessionThread(Runnable task) {
        SonicSessionThreadPool.postTask(task);
    }

    /**
     * Post a task in main thread
     *
     * @param task A runnable task
     * @param delayMillis Delay millis
     */
    public void postTaskToMainThread(Runnable task, long delayMillis) {
        new Handler(Looper.getMainLooper()).postDelayed(task, delayMillis);
    }

    /**
     * Return the looper of HandleThread which use to save sonic cache.
     *
     * @return The looper of HandleThread which use to save sonic cache.
     */
    public Looper getFileThreadLooper() {
        if (fileHandlerThread == null) {
            fileHandlerThread = new HandlerThread("SonicSdk_FileThread");
            fileHandlerThread.start();
        }

        return fileHandlerThread.getLooper();
    }

    /**
     * Notify error for host application to do report or statics
     *
     * @param client    The error client
     * @param url      The error url
     * @param errorCode Error code
     */
    public abstract void notifyError(SonicSessionClient client, String url, int errorCode);
}
