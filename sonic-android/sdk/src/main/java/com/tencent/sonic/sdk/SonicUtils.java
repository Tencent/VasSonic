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

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sonic Utils
 */
public class SonicUtils {

    /**
     * the default charset is UTF-8.
     */
    public static final String DEFAULT_CHARSET = Charset.defaultCharset().name();

    /**
     * Log filter
     */
    private static final String TAG = SonicConstants.SONIC_SDK_LOG_PREFIX + "SonicUtils";

    /**
     * Sonic template tag :the beginning of title label
     * title is considered part of the data in sonic
     */
    private static final String SONIC_TAG_TITLE_OPEN = "<title>";

    /**
     * Sonic template tag :the end of title label
     */
    private static final String SONIC_TAG_TITLE_CLOSE = "</title>";

    /**
     * Sonic template tag:
     */
    private static final String SONIC_TAG_TITLE_KEY = "{title}";

    /**
     * Sonic pattern : through the pattern sonic splits the html to template and data
     */
    private static final String SONIC_TAG_PATTERN = "<!--sonicdiff-?(\\w*)-->([\\s\\S]+?)<!--sonicdiff-?(\\w*)-end-->";

    /**
     * The beginning of sonic pattern
     */
    private static final String SONIC_TAG_DIFF_BEGIN = "<!--sonicdiff-";

    /**
     * the end of sonic pattern
     */
    private static final String SONIC_TAG_DIFF_END = "-->";

    /**
     * The beginning of data key
     */
    private static final String SONIC_TAG_KEY_BEGIN = "{";

    /**
     * the end of data key
     */
    private static final String SONIC_TAG_KEY_END = "}";

    /**
     * the key of last cache check and clear time saved in SharedPreference
     */
    private static final String SONIC_CLEAR_CACHE_TIME = "check_and_clear_cache_time";

    /**
     * Logger function
     *
     * @param level Level of this log，such like Log.DEBUG.
     * @return Should log or not
     */
    public static boolean shouldLog(int level) {
        return SonicEngine.getInstance().getRuntime().shouldLog(level);
    }

    /**
     * Logger function
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param level Level of this log，such like Log.DEBUG.
     * @param message The message you would like logged.
     */
    public static void log(String tag, int level, String message) {
        SonicEngine.getInstance().getRuntime().log(tag, level, message);
    }

    /**
     * Save sonic data to SharedPreferences, such as the eTag, template tag and so on
     *
     * @param sessionId   A unique session id
     * @param eTag        Html eTag
     * @param templateTag Template tag
     * @param htmlSha1    Html sha1
     * @param htmlSize    Html size
     */
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    static void saveSonicData(String sessionId, String eTag, String templateTag, String htmlSha1,
                              long htmlSize, Map<String, List<String>> headers) {
        if (SonicUtils.shouldLog(Log.INFO)) {
            SonicUtils.log(TAG, Log.INFO, "saveSonicData sessionId = " + sessionId + ", eTag = " + eTag + ", templateTag = " + templateTag + ",htmlSha1 = " + htmlSha1 + ", htmlSize = " + htmlSize );
        }
        SonicDataHelper.SessionData sessionData = new SonicDataHelper.SessionData();
        sessionData.sessionId = sessionId;
        handleCacheControl(headers, sessionData);
        sessionData.eTag = eTag;
        sessionData.templateTag = templateTag;
        sessionData.htmlSha1 = htmlSha1;
        sessionData.htmlSize = htmlSize;
        sessionData.templateUpdateTime = System.currentTimeMillis();
        SonicDataHelper.saveSessionData(sessionId, sessionData);
    }

    /**
     * Calculate the expired time of session cache depends on the response header Cache-Control.
     * @param headers response headers
     * @param sessionData session data
     */
    private static void handleCacheControl(Map<String, List<String>> headers, SonicDataHelper.SessionData sessionData) {
        List<String> responseHeaderValues;
        if (headers.containsKey(SonicSessionConnection.HTTP_HEAD_FIELD_CACHE_CONTROL.toLowerCase())) {
            responseHeaderValues = headers.get(SonicSessionConnection.HTTP_HEAD_FIELD_CACHE_CONTROL.toLowerCase());
            if (responseHeaderValues != null && responseHeaderValues.size() > 0) {
                String header = responseHeaderValues.get(0).toLowerCase();
                if (header.contains("max-age")) {
                    int index = header.indexOf("max-age");
                    String temp = header.substring(index);
                    int endIndex = temp.indexOf(",");
                    endIndex = endIndex == -1 ? temp.length() : endIndex;
                    String maxAgeStr = temp.substring(8, endIndex);
                    try {
                        long maxAgeTime = Long.parseLong(maxAgeStr) * 1000;
                        if (maxAgeTime != 0) {
                            sessionData.expiredTime = maxAgeTime + System.currentTimeMillis();
                        }
                    } catch (Exception e) {
                        log(TAG, Log.ERROR, "handleCacheControl:sessionId(" + sessionData.sessionId + ") error:" + e.getMessage());
                    }
                } else if (header.contains("private") || header.contains("public")) {
                    //max 5min
                    sessionData.expiredTime = System.currentTimeMillis() + SonicEngine.getInstance().getConfig().SONIC_CACHE_MAX_AGE;
                }
            } else if (headers.containsKey(SonicSessionConnection.HTTP_HEAD_FIELD_EXPIRES)) {
                responseHeaderValues = headers.get(SonicSessionConnection.HTTP_HEAD_FIELD_EXPIRES);
                if (responseHeaderValues != null && responseHeaderValues.size() > 0) {
                    String header = responseHeaderValues.get(0);
                    DateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss z", Locale.US);
                    df.setTimeZone(TimeZone.getTimeZone("GMT"));
                    try {
                        Date date = df.parse(header);
                        sessionData.expiredTime = date.getTime() + 8 * 60 * 60 * 1000;
                    } catch (Exception e) {
                        log(TAG, Log.ERROR, "handleCacheControl:sessionId(" + sessionData.sessionId + ") error:" + e.getMessage());
                    }
                }
            }

            //The max age of sonic cache is {@link com.tencent.sonic.sdk.SonicConfig.SONIC_CACHE_MAX_AGE}
            long maxAge = System.currentTimeMillis() + SonicEngine.getInstance().getConfig().SONIC_CACHE_MAX_AGE;
            if (sessionData.expiredTime > maxAge) {
                sessionData.expiredTime = maxAge;
            }
        }
    }

    /**
     * save resource data to database, such as resource sha1, resource size etc.
     *
     * @param resourceUrl the resource url
     * @param resourceSha1 the resource sha1
     * @param resourceSize the resource size
     */
    public static void saveSonicResourceData(String resourceUrl, String resourceSha1, long resourceSize) {
        if (SonicUtils.shouldLog(Log.INFO)) {
            SonicUtils.log(TAG, Log.INFO, "saveSonicResourceData resourceUrl = " + resourceUrl + ", resourceSha1 = " + resourceSha1 + ", resourceSize = " + resourceSize);
        }
        SonicResourceDataHelper.ResourceData resourceData = new SonicResourceDataHelper.ResourceData();
        resourceData.resourceId = getMD5(resourceUrl);
        resourceData.resourceSha1 = resourceSha1;
        resourceData.resourceSize = resourceSize;
        handleResourceExpireTime(resourceUrl, resourceData);
        resourceData.lastUpdateTime = System.currentTimeMillis();
        SonicResourceDataHelper.saveResourceData(resourceData.resourceId, resourceData);
    }

    /**
     * parse the cache expired time from resource url parameters.
     *
     * @param resourceUrl the resource url
     * @param resourceData the resource data
     */
    private static void handleResourceExpireTime(String resourceUrl, SonicResourceDataHelper.ResourceData resourceData) {
        Uri uri = Uri.parse(resourceUrl);
        String maxAgeStr = uri.getQueryParameter("max-age");
        if (TextUtils.isEmpty(maxAgeStr)) {
            resourceData.expiredTime = Long.MAX_VALUE;
        } else {
            try {
                long maxAgeTime = Long.parseLong(maxAgeStr) * 1000;
                if (maxAgeTime != 0) {
                    resourceData.expiredTime = maxAgeTime + System.currentTimeMillis();
                }
            } catch (Exception e) {
                log(TAG, Log.ERROR, "handleResourceExpireTime:resourceUrl(" + resourceUrl + ") error:" + e.getMessage());
            }
        }
    }

    /**
     * Obtain the difference data between the server and the local data
     *
     * @param sessionId      A unique session id
     * @param serverDataJson Server data
     * @return Difference data between the server and the local data
     */
    static JSONObject getDiffData(String sessionId, JSONObject serverDataJson) {
        JSONObject diffData;
        try {
            String localDataString = SonicFileUtils.readFile(new File(SonicFileUtils.getSonicDataPath(sessionId)));
            if (!TextUtils.isEmpty(localDataString)) {
                JSONObject localDataJson = new JSONObject(localDataString);
                diffData = getDiffData(localDataJson, serverDataJson);
            } else {
                diffData = serverDataJson;
            }
            if (diffData != null && diffData.length() > 0) {
                diffData.put("local_refresh_time", System.currentTimeMillis());
            }
        } catch (Throwable e) {
            diffData = null;
            log(TAG, Log.ERROR, "getDiffData error1:" + e.getMessage());
        }
        return diffData;
    }

    static JSONObject getDiffData(JSONObject localDataJson, JSONObject serverDataJson){
        if(localDataJson == null || serverDataJson == null){
            return null;
        }

        JSONObject diffData = new JSONObject();
        try {
            Iterator<?> iterator = serverDataJson.keys();
            String key;
            String serverData;
            String localData;
            while (iterator.hasNext()) {
                key = iterator.next().toString();
                serverData = serverDataJson.optString(key);
                localData = localDataJson.optString(key);
                if (!serverData.equals(localData)) {
                    diffData.put(key, serverData);
                    if (shouldLog(Log.DEBUG)) {
                        log(TAG, Log.DEBUG, "getDiffData:find diff data, key ->" + key + ", length=" + serverData.length() + ".");
                    }
                }
            }
        }catch (Throwable e){
            diffData = null;
            log(TAG, Log.ERROR, "getDiffData error2:" + e.getMessage());
        }

        return diffData;
    }

    static String buildHtml(final String sessionId, JSONObject dataJson, String sha1, int dataMaxSize) {
        File templateFile = new File(SonicFileUtils.getSonicTemplatePath(sessionId));
        if (templateFile.exists()) {
            String templateString = SonicFileUtils.readFile(templateFile);
            if (!TextUtils.isEmpty(templateString)) {

                final String htmlString = buildHtml(templateString, dataJson, dataMaxSize);

                if (TextUtils.isEmpty(sha1) || sha1.equalsIgnoreCase(SonicUtils.getSHA1(htmlString))) {
                    return htmlString;
                }

                SonicEngine.getInstance().getRuntime().postTaskToThread(new Runnable() {
                    @Override
                    public void run() {
                        String path = SonicFileUtils.getSonicHtmlPath(sessionId) + ".tmp";
                        SonicFileUtils.writeFile(htmlString, path);
                    }
                }, 0);

                log(TAG, Log.ERROR, "buildHtml error: verify sha1 error.");
                return null;
            } else {
                log(TAG, Log.ERROR, "buildHtml error: template string is empty.");
            }
        } else {
            log(TAG, Log.ERROR, "buildHtml error: template file is not exists.");
        }
        return null;
    }

    /**
     * Build the template and data into html
     * @param templateString The contents of the template
     * @param dataJson       The contents of the data
     * @param dataMaxSize    the length of data.Through it to determine StringBuilder's original length
     * @return final html string
     */
    static String buildHtml(String templateString, JSONObject dataJson, int dataMaxSize) {
        if (TextUtils.isEmpty(templateString) || dataJson == null) {
            return null;
        }
        StringBuilder htmlStringBuilder = new StringBuilder(templateString.length() + dataMaxSize);
        htmlStringBuilder.append(templateString);
        String key;
        String data;
        int index;
        Iterator<?> iterator = dataJson.keys();
        while (iterator.hasNext()) {
            key = iterator.next().toString();
            data = dataJson.optString(key);
            index = htmlStringBuilder.indexOf(key);
            if (-1 != index) {
                htmlStringBuilder.replace(index, index + key.length(), data);
            }
        }
        return htmlStringBuilder.toString();
    }

    /**
     * Save sonic files, such as html, template and data
     *
     * @param sessionId      A unique session id
     * @param htmlString     Html content
     * @param templateString Template content
     * @param dataString     Data content
     * @return The result of save files.true if all data is saved successfully
     */
    static boolean saveSessionFiles(String sessionId, String htmlString, String templateString, String dataString, Map<String, List<String>> headers) {
        if (!TextUtils.isEmpty(htmlString) && !SonicFileUtils.writeFile(htmlString, SonicFileUtils.getSonicHtmlPath(sessionId))) {
            log(TAG, Log.ERROR, "saveSessionData error: write html file fail.");
            return false;
        }

        if (!TextUtils.isEmpty(templateString) && !SonicFileUtils.writeFile(templateString, SonicFileUtils.getSonicTemplatePath(sessionId))) {
            log(TAG, Log.ERROR, "saveSessionData error: write template file fail.");
            return false;
        }

        if (!TextUtils.isEmpty(dataString) && !SonicFileUtils.writeFile(dataString, SonicFileUtils.getSonicDataPath(sessionId))) {
            log(TAG, Log.ERROR, "saveSessionData error: write data file fail.");
            return false;
        }

        if (headers != null && headers.size() > 0
                &&!SonicFileUtils.writeFile(SonicFileUtils.convertHeadersToString(headers), SonicFileUtils.getSonicHeaderPath(sessionId))) {
            log(TAG, Log.ERROR, "saveSessionData error: write header file fail.");
            return false;
        }
        return true;
    }

    /**
     * save resource files, including resource and headers.
     *
     * @param resourceName resource file name
     * @param resourceBytes resource bytes content
     * @param headers resource headers
     * @return The result of save files. true if all data is saved successfully.
     */
    public static boolean saveResourceFiles(String resourceName, byte[] resourceBytes, Map<String, List<String>> headers) {
        if (resourceBytes != null && !SonicFileUtils.writeFile(resourceBytes, SonicFileUtils.getSonicResourcePath(resourceName))) {
            log(TAG, Log.ERROR, "saveResourceFiles error: write resource data fail.");
            return false;
        }

        if (headers != null && headers.size() > 0
                &&!SonicFileUtils.writeFile(SonicFileUtils.convertHeadersToString(headers), SonicFileUtils.getSonicResourceHeaderPath(resourceName))) {
            log(TAG, Log.ERROR, "saveResourceFiles error: write header file fail.");
            return false;
        }
        return true;
    }

    /**
     * Get filtered headers by session id, this method will return a map of header(k-v) which
     * will not contains "Set-Cookie", "Cache-Control", "Expires".
     *
     * @param srcHeaders      The source headers
     * @return The headers of sessionId
     */
    public static HashMap<String, String> getFilteredHeaders(Map<String, List<String>> srcHeaders) {
        HashMap<String, String> headers = new HashMap<String, String>();
        if (null != srcHeaders) {
            List<String> headerValues;
            try {
                for (Map.Entry<String, List<String>> entry : srcHeaders.entrySet()) {
                    if ("Set-Cookie".equalsIgnoreCase(entry.getKey()) || "Cache-Control".equalsIgnoreCase(entry.getKey()) ||
                            "Expires".equalsIgnoreCase(entry.getKey()) || "Etag".equalsIgnoreCase(entry.getKey())) {
                        // forbid webview kernel to run cache related logic
                        continue;
                    }
                    headerValues = entry.getValue();
                    if (null != headerValues && 1 == headerValues.size()) {
                        headers.put(entry.getKey(), headerValues.get(0));
                    }
                }
            } catch (Throwable e) {
                SonicUtils.log(TAG, Log.ERROR, "getFilteredHeaders error! " + e.getMessage());
            }
        }
        return headers;
    }

    /**
     * Find and get the charset from the Content-Type value.
     * @param headerValue The value corresponding to the HTTP Header Content-Type
     * @return The charset.
     */
    static String getCharset(String headerValue) {
        String charset = DEFAULT_CHARSET;
        if (!TextUtils.isEmpty(headerValue) ) {
            headerValue = headerValue.toLowerCase();
            int index = headerValue.indexOf("charset");
            if (index != -1) {
                String temp = headerValue.substring(index);
                temp = temp.replace(" ", "");
                int endIndex = temp.indexOf(";");
                endIndex = endIndex == -1 ? temp.length() : endIndex;
                charset = temp.substring(8, endIndex);

                charset = TextUtils.isEmpty(charset) ? DEFAULT_CHARSET : charset;
            }
        }
        return charset;
    }

    /**
     * Separate html into templates and data
     *
     * @param sessionId             A unique session id
     * @param htmlString            Html content
     * @param templateStringBuilder StringBuilder to save template content
     * @param dataStringBuilder     StringBuilder to save data content
     * @return The result of separate
     */
    static boolean separateTemplateAndData(String sessionId, String htmlString, StringBuilder templateStringBuilder, StringBuilder dataStringBuilder) {
        long startTime = System.currentTimeMillis();
        log(TAG, Log.INFO, "separateTemplateAndData:sessionId(" + sessionId + ") start, htmlString = " + (htmlString.length() > 128 ? htmlString.substring(0, 128) : htmlString));

        JSONObject info = new JSONObject();
        int lastEnd = 0;
        try {
            Pattern pattern = Pattern.compile(SONIC_TAG_PATTERN, Pattern.MULTILINE);
            Matcher matcher = pattern.matcher(htmlString);
            while (matcher.find()) {
                String dataInfo = matcher.group();
                String dataMark = SONIC_TAG_DIFF_BEGIN;
                int markLen = dataMark.length();
                int keyStart = dataInfo.indexOf(dataMark);
                int keyEnd = dataInfo.indexOf(SONIC_TAG_DIFF_END);
                String key = null;
                if (keyStart != -1 && keyStart + markLen < keyEnd) {
                    key = dataInfo.substring(keyStart + markLen, keyEnd);
                }

                key = SONIC_TAG_KEY_BEGIN + key + SONIC_TAG_KEY_END;
                if (SonicUtils.shouldLog(Log.DEBUG)) {
                    SonicUtils.log(TAG, Log.DEBUG, "separateTemplateAndData:sessionId(" + sessionId + "), key = " + key);
                }

                if (!TextUtils.isEmpty(key) && !TextUtils.isEmpty(dataInfo)) {
                    info.put(key, dataInfo);
                    int start = matcher.start();
                    templateStringBuilder.append(htmlString.substring(lastEnd, start));
                    templateStringBuilder.append(key);
                    lastEnd = matcher.end();
                }
            }

            if (lastEnd < htmlString.length() && templateStringBuilder.length() > 0) {
                templateStringBuilder.append(htmlString.substring(lastEnd, htmlString.length()));
            }

            int titleStart = templateStringBuilder.indexOf(SONIC_TAG_TITLE_OPEN);
            int titleEnd = templateStringBuilder.indexOf(SONIC_TAG_TITLE_CLOSE, titleStart + SONIC_TAG_TITLE_OPEN.length()) + SONIC_TAG_TITLE_CLOSE.length();
            if (titleStart != -1 && titleStart < titleEnd) {
                String key = SONIC_TAG_TITLE_KEY;
                info.put(key, templateStringBuilder.substring(titleStart, titleEnd));
                templateStringBuilder.replace(titleStart, titleEnd, key);
            }

            dataStringBuilder.append(info.toString());

        } catch (Exception e) {
            log(TAG, Log.ERROR, "separateTemplateAndData:sessionId(" + sessionId + ") error:" + e.getMessage());
            return false;
        }
        log(TAG, Log.INFO, "separateTemplateAndData:sessionId(" + sessionId + ") end, cost " + (System.currentTimeMillis() - startTime) + "ms.");
        return true;
    }

    /**
     * Remove a unique session cache, include memory cache and disk cache
     *
     * @param sessionId A unique session id
     */
    static void removeSessionCache(String sessionId) {
        SonicDataHelper.removeSessionData(sessionId);
        SonicFileUtils.deleteSonicFiles(sessionId);
    }

    /**
     * Remove a unique resource cache.
     *
     * @param resourceId a unique resource id
     */
    public static void removeResourceCache(String resourceId) {
        SonicResourceDataHelper.removeResourceData(resourceId);
        SonicFileUtils.deleteResourceFiles(resourceId);
    }

    /**
     * Remove all session cache, include memory cache and disk cache
     *
     */
    static boolean removeAllSessionCache() {
        File cacheRootDir = new File(SonicFileUtils.getSonicCacheDirPath());
        if (cacheRootDir.exists()) {
            SonicDataHelper.clear();
            return SonicFileUtils.deleteAllChildFiles(cacheRootDir);
        }
        return false;
    }

    /**
     * According to cache-offline head and Cache-Control header to decide whether to save data
     *
     * @param isSupportCacheControl Indicates whether to check Cache-Control header or not
     * @param cacheOffline Cache-offline head
     * @param headers Response http headers
     *
     * @return need or not
     */
    static boolean needSaveData(boolean isSupportCacheControl, String cacheOffline, Map<String, List<String>> headers) {
        boolean needSaveData =  !TextUtils.isEmpty(cacheOffline) && (SonicSession.OFFLINE_MODE_STORE.equals(cacheOffline) || SonicSession.OFFLINE_MODE_TRUE.equals(cacheOffline));
        if (needSaveData && isSupportCacheControl) {
            //check http header Cache-Control
            List<String> responseHeaderValues = headers.get(SonicSessionConnection.HTTP_HEAD_FIELD_CACHE_CONTROL.toLowerCase());
            if (headers.containsKey(SonicSessionConnection.HTTP_HEAD_FIELD_CACHE_CONTROL)) {
                if (responseHeaderValues != null && responseHeaderValues.size() > 0) {
                    String header = responseHeaderValues.get(0).toLowerCase();
                    if (header.contains("no-cache") || header.contains("no-store") || header.contains("must-revalidate")) {
                        needSaveData = false;
                    }
                }
            } else if (headers.containsKey(SonicSessionConnection.HTTP_HEAD_FIELD_PRAGMA)) {
                needSaveData = false;
            }
        }

        return needSaveData;
    }

    /**
     * According to cache-offline head decide whether to refresh page
     *
     * @param cacheOffline Cache-offline head
     * @return need refresh or not
     */
    static boolean needRefreshPage(String cacheOffline) {
        return !TextUtils.isEmpty(cacheOffline) && (SonicSession.OFFLINE_MODE_FALSE.equals(cacheOffline) || SonicSession.OFFLINE_MODE_TRUE.equals(cacheOffline));
    }


    static String addSonicUrlParam(String url, String paramKey, String paramValue) {
        if (!TextUtils.isEmpty(url)) {
            StringBuilder stringBuilder = new StringBuilder(url);

            int paramKeyIndex;
            int nextParamStartIndex;
            int paramStartIndex = stringBuilder.lastIndexOf("/");
            if (paramStartIndex < 0) paramStartIndex = 0;
            String paramKeyPattern1 = "&" + paramKey + "=";
            String paramKeyPattern2 = "?" + paramKey + "=";
            int paramKeyPattern = paramKeyPattern1.length();
            try {
                do {
                    paramKeyIndex = stringBuilder.indexOf(paramKeyPattern1, paramStartIndex);
                    if (-1 == paramKeyIndex) {
                        paramKeyIndex = stringBuilder.indexOf(paramKeyPattern2, paramStartIndex);
                    }
                    if (paramKeyIndex > 0) {
                        nextParamStartIndex = stringBuilder.indexOf("&", paramKeyIndex + paramKeyPattern);
                        if (nextParamStartIndex > 0) {
                            stringBuilder.replace(paramKeyIndex + 1, nextParamStartIndex + 1, "");
                        } else {
                            stringBuilder.replace(paramKeyIndex, stringBuilder.length(), "");
                        }
                    } else {
                        break;
                    }
                } while (true);
            } catch (Throwable e) {
                log(TAG, Log.ERROR, "addSonicUrlParam error:" + e.getMessage());
                return url;
            }
            if (-1 != stringBuilder.indexOf("?")) {
                stringBuilder.append("&").append(paramKey).append("=").append(paramValue);
            } else {
                stringBuilder.append("?").append(paramKey).append("=").append(paramValue);
            }
            return stringBuilder.toString();
        }
        return url;
    }


    /**
     * Get mime type for url simply.
     * (Maybe {@code android.webkit.MimeTypeMap.getMimeTypeFromExtension} is better.)
     * @param url target url
     * @return mime type
     */
    public static String getMime(String url) {
        String mime = "text/html";
        Uri currentUri = Uri.parse(url);
        String path = currentUri.getPath();
        if (path.endsWith(".css")) {
            mime = "text/css";
        } else if (path.endsWith(".js")) {
            mime = "application/x-javascript";
        } else if (path.endsWith(".jpg") || path.endsWith(".gif") ||
                path.endsWith(".png") || path.endsWith(".jpeg") ||
                path.endsWith(".webp") || path.endsWith(".bmp")) {
            mime = "image/*";
        }
        return mime;
    }


    private static final char[] hexChar = {'0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};


    static String getSHA1(String content) {
        if (TextUtils.isEmpty(content)) {
            return "";
        }
        return getSHA1(content.getBytes());
    }

    public static String getSHA1(byte[] contentBytes) {
        if (contentBytes == null || contentBytes.length <= 0) {
            return "";
        }
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA1");
            sha1.update(contentBytes, 0, contentBytes.length);
            return toHexString(sha1.digest());
        } catch (Exception e) {
            return "";
        }
    }


    public static String getMD5(String content) {
        if (TextUtils.isEmpty(content))
            return "";
        try {
            MessageDigest sha1 = MessageDigest.getInstance("MD5");
            sha1.update(content.getBytes(), 0, content.getBytes().length);
            return toHexString(sha1.digest());
        } catch (Exception e) {
            return "";
        }
    }

    private static String toHexString(byte b[]) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte aB : b) {
            sb.append(hexChar[(aB & 0xf0) >>> 4]);
            sb.append(hexChar[aB & 0xf]);
        }
        return sb.toString();
    }

    /**
     *
     * @param timeInterval The time interval between check and clear sonic cache
     * @return true if we should clear sonic cache.
     */
    static boolean shouldClearCache(long timeInterval) {
        SharedPreferences sp = SonicEngine.getInstance().getRuntime().getSonicSharedPreferences();
        long lastCheckTime = sp.getLong(SONIC_CLEAR_CACHE_TIME, 0L);
        return System.currentTimeMillis() - lastCheckTime > timeInterval;
    }

    /**
     * save the clear cache time.
     *
     * @param timestamp time when clear the cache
     */
    static void saveClearCacheTime(long timestamp) {
        SharedPreferences sp = SonicEngine.getInstance().getRuntime().getSonicSharedPreferences();
        sp.edit().putLong(SONIC_CLEAR_CACHE_TIME, timestamp).apply();
    }
}
