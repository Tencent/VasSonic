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

/**
 *
 * SonicDataHelper provides sonic data such as eTag, templateTag, etc.
 *
 */
class SonicDataHelper {

    /**
     * Log filter
     */
    private static final String TAG = SonicConstants.SONIC_SDK_LOG_PREFIX + "SonicDataHelper";

    /**
     * The SharedPreferences file name
     */

    private static final String SP_FILE_SONIC = "sp_sonic_db";

    /**
     * The SharedPreferences file version
     */
    private static final String SP_KEY_VERSION_NUM = "versionNum";

    /**
     * The key of eTag
     */
    private static final String SP_KEY_ETAG = "etag_";

    /**
     * The key of templateTag
     */
    private static final String SP_KEY_TEMPLATE_TAG = "templateTag_";

    /**
     * The key of html sha1
     */
    private static final String SP_KEY_HTML_SHA1 = "htmlSha1_";

    /**
     * The key of html size
     */
    private static final String SP_KEY_HTML_SIZE = "htmlSize_";

    /**
     * The key of template update time
     */
    private static final String SP_KEY_TEMPLATE_UPDATE_TIME = "templateUpdateTime_";

    /**
     * The key of Unavailable Time
     */
    private static final String SP_KEY_UNAVAILABLE_TIME = "UnavailableTime_";

    /**
     * The key of Content-Security-Policy
     */
    private static final String SP_KEY_CSP = "csp_";

    /**
     * The key of Content-Security-Policy-Report-Only
     */
    private static final String SP_KEY_CSP_REPORT_ONLY = "cspReportOnly_";

    private static SharedPreferences sSharedPreferences;

    /**
     * Sonic data structure
     */
    static class SessionData {

        /**
         * The etag of html
         */
        String etag;

        /**
         * Template tag
         */
        String templateTag;

        /**
         * The sha1 of html
         */
        String htmlSha1;

        /**
         * The size of html
         */
        long htmlSize;

        /**
         * The latest time of template update
         */
        long templateUpdateTime;

        /**
         * The content of Content-Security-Policy
         */
        String cspContent;

        /**
         * The content of Content-Security-Policy-Report-Only
         */
        String cspReportOnlyContent;

        /**
         * Reset data
         */
        public void reset() {
            etag = "";
            templateTag = "";
            htmlSha1 = "";
            htmlSize = 0;
            templateUpdateTime = 0;
            cspContent = "";
            cspReportOnlyContent = "";
        }
    }

    private static synchronized SharedPreferences getSonicSharedPref() {
        if (null == sSharedPreferences) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
                sSharedPreferences = SonicEngine.getInstance().getRuntime().getContext().getSharedPreferences(SP_FILE_SONIC, Context.MODE_MULTI_PROCESS);
            } else {
                sSharedPreferences = SonicEngine.getInstance().getRuntime().getContext().getSharedPreferences(SP_FILE_SONIC, Context.MODE_PRIVATE);
            }
            String oldVersionNum = sSharedPreferences.getString(SP_KEY_VERSION_NUM, "");
            String newVersionNum = SonicConstants.SONIC_VERSION_NUM;
            if (!newVersionNum.equals(oldVersionNum)) {
                sSharedPreferences.edit().putString(SP_KEY_VERSION_NUM, newVersionNum).apply();
            }
        }
        return sSharedPreferences;
    }
    
    /**
     * Get sonic sessionData by unique session id
     *
     * @param sessionId a unique session id
     * @return SessionData
     */
    static SessionData getSessionData(String sessionId) {
        SharedPreferences sharedPreferences = getSonicSharedPref();
        SessionData sessionData = new SessionData();
        sessionData.etag = sharedPreferences.getString(SP_KEY_ETAG + sessionId, "");
        sessionData.templateTag = sharedPreferences.getString(SP_KEY_TEMPLATE_TAG + sessionId, "");
        sessionData.htmlSha1 = sharedPreferences.getString(SP_KEY_HTML_SHA1 + sessionId, "");
        sessionData.templateUpdateTime = sharedPreferences.getLong(SP_KEY_TEMPLATE_UPDATE_TIME + sessionId, 0L);
        sessionData.htmlSize = sharedPreferences.getLong(SP_KEY_HTML_SIZE + sessionId, 0L);
        sessionData.cspContent = sharedPreferences.getString(SP_KEY_CSP + sessionId, "");
        sessionData.cspReportOnlyContent = sharedPreferences.getString(SP_KEY_CSP_REPORT_ONLY + sessionId, "");
        return sessionData;
    }

    /**
     * Save sonic sessionData with a unique session id
     *
     * @param sessionId   a unique session id
     * @param sessionData SessionData
     */
    static void saveSessionData(String sessionId, SessionData sessionData) {
        if (sessionData != null && sessionId != null) {
            SharedPreferences sharedPreferences = getSonicSharedPref();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(SP_KEY_ETAG + sessionId, sessionData.etag);
            editor.putString(SP_KEY_TEMPLATE_TAG + sessionId, sessionData.templateTag);
            editor.putString(SP_KEY_HTML_SHA1 + sessionId, sessionData.htmlSha1);
            editor.putLong(SP_KEY_TEMPLATE_UPDATE_TIME + sessionId, sessionData.templateUpdateTime);
            editor.putLong(SP_KEY_HTML_SIZE + sessionId, sessionData.htmlSize);
            editor.putString(SP_KEY_CSP + sessionId, sessionData.cspContent);
            editor.putString(SP_KEY_CSP_REPORT_ONLY + sessionId, sessionData.cspReportOnlyContent);
            editor.apply();
        }
    }

    /**
     * Remove a unique session data
     *
     * @param sessionId A unique session id
     */
    static void removeSessionData(String sessionId) {
        SharedPreferences.Editor editor = getSonicSharedPref().edit();
        editor.remove(SP_KEY_ETAG + sessionId).remove(SP_KEY_TEMPLATE_TAG + sessionId);
        editor.remove(SP_KEY_HTML_SHA1 + sessionId).remove(SP_KEY_TEMPLATE_UPDATE_TIME + sessionId);
        editor.remove(SP_KEY_HTML_SIZE + sessionId).apply();
    }

    /**
     * Set sonic unavailable time, sonic will not execute its logic before this time.
     *
     * @param sessionId       A unique session id.
     * @param unavailableTime Unavailable time.
     * @return The result of save unavailable time
     */
    static boolean setSonicUnavailableTime(String sessionId, long unavailableTime) {
        SharedPreferences sharedPreferences = getSonicSharedPref();
        return sharedPreferences.edit().putLong(SP_KEY_UNAVAILABLE_TIME + sessionId, unavailableTime).commit();
    }

    /**
     * Get the sonic unavailable time
     *
     * @param sessionId A unique session id
     * @return The sonic unavailable time
     */
    static long getLastSonicUnavailableTime(String sessionId) {
        SharedPreferences sharedPreferences = getSonicSharedPref();
        return sharedPreferences.getLong(SP_KEY_UNAVAILABLE_TIME + sessionId, 0);
    }

    /**
     * Remove all sonic data
     */
    static synchronized void clear() {
        if (null != sSharedPreferences) {
            sSharedPreferences.edit().clear().apply();
            sSharedPreferences = null;
        }
    }

    /**
     * Get the content of Content-Security-Policy
     *
     * @param sessionId A unique session id
     * @return The content of Content-Security-Policy
     */
    static String getCSPContent(String sessionId) {
        SharedPreferences sharedPreferences = getSonicSharedPref();
        return sharedPreferences.getString(SP_KEY_CSP + sessionId, "");
    }

    /**
     * Get the content of Content-Security-Policy-Report-Only
     *
     * @param sessionId A unique session id
     * @return The content of Content-Security-Policy-Report-Only
     */
    static String getCSPReportOnlyContent(String sessionId) {
        SharedPreferences sharedPreferences = getSonicSharedPref();
        return sharedPreferences.getString(SP_KEY_CSP_REPORT_ONLY + sessionId, "");
    }
}
