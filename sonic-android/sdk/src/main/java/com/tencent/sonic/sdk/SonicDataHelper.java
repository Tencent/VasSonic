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

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.tencent.sonic.sdk.SonicDBHelper.SESSION_DATA_COLUMN_CACHE_EXPIRED_TIME;
import static com.tencent.sonic.sdk.SonicDBHelper.SESSION_DATA_COLUMN_CACHE_HIT_COUNT;
import static com.tencent.sonic.sdk.SonicDBHelper.SESSION_DATA_COLUMN_ETAG;
import static com.tencent.sonic.sdk.SonicDBHelper.SESSION_DATA_COLUMN_HTML_SHA1;
import static com.tencent.sonic.sdk.SonicDBHelper.SESSION_DATA_COLUMN_HTML_SIZE;
import static com.tencent.sonic.sdk.SonicDBHelper.SESSION_DATA_COLUMN_SESSION_ID;
import static com.tencent.sonic.sdk.SonicDBHelper.SESSION_DATA_COLUMN_TEMPLATE_EAG;
import static com.tencent.sonic.sdk.SonicDBHelper.SESSION_DATA_COLUMN_TEMPLATE_UPDATE_TIME;
import static com.tencent.sonic.sdk.SonicDBHelper.SESSION_DATA_COLUMN_UNAVAILABLE_TIME;

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
     * The key of Content-Security-Policy
     */
    private static final String SP_KEY_CSP = "csp_";

    /**
     * The key of Content-Security-Policy-Report-Only
     */
    private static final String SP_KEY_CSP_REPORT_ONLY = "cspReportOnly_";

    /**
     * The key of last clear cache time.
     */
    private static final String SP_KEY_LAST_CLEAR_CACHE_TIME = "last_clear_cache_time";

    private static SharedPreferences sSharedPreferences;

    /**
     * Sonic data structure
     */
    static class SessionData {

        String sessionId;

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
         * Indicates when local sonic cache is expired.
         * If It is expired, the record of database and file on SDCard will be removed.
         */
        long expiredTime;

        /**
         * Indicates when sonic session is unavailable.
         */
        long unAvailableTime;

        /**
         * Indicates this cache  how many times to be used.
         */
        int cacheHitCount;

        /**
         * Reset data
         */
        public void reset() {
            etag = "";
            templateTag = "";
            htmlSha1 = "";
            htmlSize = 0;
            templateUpdateTime = 0;
            expiredTime = 0;
            cacheHitCount = 0;
            unAvailableTime = 0;
        }
    }

    private static synchronized SharedPreferences getSonicSharedPref() {
        if (null == sSharedPreferences) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
                sSharedPreferences = SonicEngine.getInstance().getRuntime().getContext().getSharedPreferences(SP_FILE_SONIC, Context.MODE_MULTI_PROCESS);
            } else {
                sSharedPreferences = SonicEngine.getInstance().getRuntime().getContext().getSharedPreferences(SP_FILE_SONIC, Context.MODE_PRIVATE);
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
        SessionData sessionData = new SessionData();

        SQLiteDatabase db = SonicDBHelper.getInstance(SonicEngine.getInstance().getRuntime().getContext()).getWritableDatabase();
        Cursor cursor = db.query(SonicDBHelper.Sonic_SESSION_TABLE_NAME,
                SonicDBHelper.getAllSessionDataColumn(),
                SESSION_DATA_COLUMN_SESSION_ID + "=?",
                new String[] {sessionId},
                null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            cursorToSessionData(sessionData, cursor);
        }

        if(cursor != null){
            cursor.close();
        }
        db.close();

        return sessionData;
    }

    /**
     * translate cursor to session data.
     * @param sessionData
     * @param cursor
     */
    private static void cursorToSessionData(SessionData sessionData, Cursor cursor) {
        sessionData.sessionId = cursor.getString(cursor.getColumnIndex(SESSION_DATA_COLUMN_SESSION_ID));
        sessionData.etag = cursor.getString(cursor.getColumnIndex(SESSION_DATA_COLUMN_ETAG));
        sessionData.htmlSha1 = cursor.getString(cursor.getColumnIndex(SESSION_DATA_COLUMN_HTML_SHA1));
        sessionData.htmlSize = cursor.getLong(cursor.getColumnIndex(SESSION_DATA_COLUMN_HTML_SIZE));
        sessionData.templateTag = cursor.getString(cursor.getColumnIndex(SESSION_DATA_COLUMN_TEMPLATE_EAG));
        sessionData.templateUpdateTime = cursor.getLong(cursor.getColumnIndex(SESSION_DATA_COLUMN_TEMPLATE_UPDATE_TIME));
        sessionData.expiredTime = cursor.getLong(cursor.getColumnIndex(SESSION_DATA_COLUMN_CACHE_EXPIRED_TIME));
        sessionData.unAvailableTime = cursor.getLong(cursor.getColumnIndex(SESSION_DATA_COLUMN_UNAVAILABLE_TIME));
        sessionData.cacheHitCount = cursor.getInt(cursor.getColumnIndex(SESSION_DATA_COLUMN_CACHE_HIT_COUNT));
    }

    /**
     *
     * @return all of the session data order by HitCount decrease.
     */
    static List<SessionData> getAllSessionByHitCount() {
        List<SessionData> sessionDatas = new ArrayList<SessionData>();
        SQLiteDatabase db = SonicDBHelper.getInstance(SonicEngine.getInstance().getRuntime().getContext()).getWritableDatabase();
        Cursor cursor = db.query(SonicDBHelper.Sonic_SESSION_TABLE_NAME,
                SonicDBHelper.getAllSessionDataColumn(),
                null,null,null, null, SESSION_DATA_COLUMN_CACHE_HIT_COUNT + " ASC");
        while(cursor != null && cursor.moveToNext()) {
            SessionData sessionData = new SessionData();
            cursorToSessionData(sessionData, cursor);
            sessionDatas.add(sessionData);
        }

        return sessionDatas;
    }

    /**
     * Save or update sonic sessionData with a unique session id
     *
     * @param sessionId   a unique session id
     * @param sessionData SessionData
     */
    static void saveSessionData(String sessionId, SessionData sessionData) {
        if (sessionData != null && sessionId != null) {
            sessionData.sessionId = sessionId;
            SQLiteDatabase db = SonicDBHelper.getInstance(SonicEngine.getInstance().getRuntime().getContext()).getWritableDatabase();
            SessionData storedSessionData = getSessionData(sessionId);

            if (storedSessionData != null) {
                sessionData.cacheHitCount = storedSessionData.cacheHitCount;
                updateSessionData(sessionId, sessionData);
            } else {
                insertSessionData(sessionId, sessionData);
            }

            db.close();
        }
    }

    static void insertSessionData(String sessionId, SessionData sessionData) {
        ContentValues contentValues = getContentValues(sessionId, sessionData);
        SQLiteDatabase db = SonicDBHelper.getInstance(SonicEngine.getInstance().getRuntime().getContext()).getWritableDatabase();
        db.insert(SonicDBHelper.Sonic_SESSION_TABLE_NAME, null, contentValues);
        db.close();
    }

    @NonNull
    private static ContentValues getContentValues(String sessionId, SessionData sessionData) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(SESSION_DATA_COLUMN_SESSION_ID, sessionId);
        contentValues.put(SESSION_DATA_COLUMN_ETAG, sessionData.etag);
        contentValues.put(SESSION_DATA_COLUMN_HTML_SHA1, sessionData.htmlSha1);
        contentValues.put(SESSION_DATA_COLUMN_HTML_SIZE, sessionData.htmlSize);
        contentValues.put(SESSION_DATA_COLUMN_TEMPLATE_EAG, sessionData.templateTag);
        contentValues.put(SESSION_DATA_COLUMN_TEMPLATE_UPDATE_TIME, sessionData.templateUpdateTime);
        contentValues.put(SESSION_DATA_COLUMN_CACHE_EXPIRED_TIME, sessionData.expiredTime);
        contentValues.put(SESSION_DATA_COLUMN_UNAVAILABLE_TIME, sessionData.unAvailableTime);
        contentValues.put(SESSION_DATA_COLUMN_CACHE_HIT_COUNT, sessionData.cacheHitCount);
        return contentValues;
    }

    static void updateSessionData(String sessionId, SessionData sessionData) {
        ContentValues contentValues = getContentValues(sessionId, sessionData);
        SQLiteDatabase db = SonicDBHelper.getInstance(SonicEngine.getInstance().getRuntime().getContext()).getWritableDatabase();

        db.update(SonicDBHelper.Sonic_SESSION_TABLE_NAME, contentValues, SESSION_DATA_COLUMN_SESSION_ID + "=?",
                new String[] {sessionId});
        db.close();
    }

    /**
     * Remove a unique session data
     *
     * @param sessionId A unique session id
     */
    static void removeSessionData(String sessionId) {
        SQLiteDatabase db = SonicDBHelper.getInstance(SonicEngine.getInstance().getRuntime().getContext()).getWritableDatabase();
        db.delete(SonicDBHelper.Sonic_SESSION_TABLE_NAME, SESSION_DATA_COLUMN_SESSION_ID + "=?",
                new String[] {sessionId});
        db.close();
    }

    /**
     * remove the old session data of SharedPreferences when upgrade is done.
     * @param sessionId
     */
    static void removeSessionDataFromOldSp(String sessionId) {
        SharedPreferences sp = getSonicSharedPref();
        SharedPreferences.Editor editor = sp.edit();
        editor.remove(SP_KEY_ETAG + sessionId);
         editor.remove(SP_KEY_TEMPLATE_TAG + sessionId);
         editor.remove(SP_KEY_HTML_SHA1 + sessionId);
         editor.remove(SP_KEY_TEMPLATE_UPDATE_TIME + sessionId);
         editor.remove(SP_KEY_HTML_SIZE + sessionId);
         editor.remove(SP_KEY_CSP + sessionId);
         editor.remove(SP_KEY_CSP_REPORT_ONLY + sessionId);
        editor.commit();
    }

    /**
     * Set sonic unavailable time, sonic will not execute its logic before this time.
     *
     * @param sessionId       A unique session id.
     * @param unavailableTime Unavailable time.
     * @return The result of save unavailable time
     */
    static boolean setSonicUnavailableTime(String sessionId, long unavailableTime) {
        SessionData sessionData = getSessionData(sessionId);
        if (sessionData != null) {
            sessionData.unAvailableTime = unavailableTime;
            updateSessionData(sessionId, sessionData);
            return true;
        }
        return false;
    }

    /**
     * Set the latest time when check and clear the sonic cache.
     * @param time The time when check and clear the sonic cache.
     */
    static void setLastClearCacheTime(long time) {
        SharedPreferences sharedPreferences = getSonicSharedPref();
        sharedPreferences.edit().putLong(SP_KEY_LAST_CLEAR_CACHE_TIME, time).apply();
    }

    /**
     * Get the sonic unavailable time
     *
     * @param sessionId A unique session id
     * @return The sonic unavailable time
     */
    static long getLastSonicUnavailableTime(String sessionId) {
        SessionData sessionData = getSessionData(sessionId);
        if (sessionData != null) {
            return sessionData.unAvailableTime;
        }
        return 0;
    }

    /**
     * It will increase HitCount when local session cache is used.
     * @param sessionId
     */
    static void updateSonicCacheHitCount(String sessionId) {
        SessionData sessionData = getSessionData(sessionId);
        if (sessionData != null) {
            sessionData.cacheHitCount += 1;
            updateSessionData(sessionId, sessionData);
        }
    }

    /**
     * Remove all sonic data
     */
    static synchronized void clear() {
        if (null != sSharedPreferences) {
            sSharedPreferences.edit().clear().apply();
            sSharedPreferences = null;
        }

        SQLiteDatabase db = SonicDBHelper.getInstance(SonicEngine.getInstance().getRuntime().getContext()).getWritableDatabase();
        db.delete(SonicDBHelper.Sonic_SESSION_TABLE_NAME, null, null);
        db.close();
    }

    /**
     *
     * @return the last time when check or clear the sonic cache.
     */
    static long getLastClearCacheTime() {
        SharedPreferences sharedPreferences = getSonicSharedPref();
        return sharedPreferences.getLong(SP_KEY_LAST_CLEAR_CACHE_TIME, 0L);
    }

    /**
     * It will upgrade old session data of SharedPreferences to new session data of database when
     * 2.0 version first start.
     */
    static void upgradeOldDataFromSp() {
        SharedPreferences sSharedPreferences = getSonicSharedPref();

        String oldVersionNum = sSharedPreferences.getString(SP_KEY_VERSION_NUM, "");
        if (!TextUtils.isEmpty(oldVersionNum) && oldVersionNum.startsWith("1.")) {
            saveOldSessionIntoDB();

            String newVersionNum = SonicConstants.SONIC_VERSION_NUM;
            if (!newVersionNum.equals(oldVersionNum)) {
                sSharedPreferences.edit().putString(SP_KEY_VERSION_NUM, newVersionNum).apply();
            }
        }
    }

    /**
     * insert old session data of SharedPreferences into database.
     */
    private static void saveOldSessionIntoDB() {
        SharedPreferences sp = getSonicSharedPref();
        Map<String, ?> allEntries = sp.getAll();

        List<String> oldSessionDatas = null;
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String key = entry.getKey();
            if (!TextUtils.isEmpty(key) && key.startsWith(SP_KEY_HTML_SHA1)) {
                String[] array = key.split("_");
                if (oldSessionDatas == null) {
                    oldSessionDatas = new ArrayList<String>();
                }
                oldSessionDatas.add(array[1].trim());
            }
        }


        if (oldSessionDatas != null && oldSessionDatas.size() > 0) {
            SessionData sessionData;
            for (String sessionId : oldSessionDatas) {
                sessionData = new SessionData();
                sessionData.etag = sp.getString(SP_KEY_ETAG + sessionId, "");
                sessionData.templateTag = sp.getString(SP_KEY_TEMPLATE_TAG, "");
                sessionData.htmlSha1 = sp.getString(SP_KEY_HTML_SHA1, "");
                sessionData.templateUpdateTime = sp.getLong(SP_KEY_TEMPLATE_UPDATE_TIME, 0);
                sessionData.htmlSize = sp.getLong(SP_KEY_HTML_SIZE, 0);
                sessionData.sessionId = sessionId;
                saveSessionData(sessionId, sessionData);

                removeSessionDataFromOldSp(sessionId);

                String cspContent = sp.getString(SP_KEY_CSP, "");
                String cspReportOnlyContent = sp.getString(SP_KEY_CSP_REPORT_ONLY, "");
                if (!TextUtils.isEmpty(cspContent) || !TextUtils.isEmpty(cspReportOnlyContent)) {
                    Map<String, List<String>> headers = SonicFileUtils.getHeaderFromLocalCache(sessionId);
                    if (!TextUtils.isEmpty(cspContent)) {
                        List<String> values = new ArrayList<String>();
                        values.add(cspContent);
                        headers.put(SonicSessionConnection.HTTP_HEAD_CSP, values);
                    }

                    if (!TextUtils.isEmpty(cspReportOnlyContent)) {
                        List<String> values = new ArrayList<String>();
                        values.add(cspReportOnlyContent);
                        headers.put(SonicSessionConnection.HTTP_HEAD_CSP_REPORT_ONLY, values);
                    }

                    if (headers != null && headers.size() > 0) {
                        SonicUtils.saveSessionFiles(sessionId, "", "", "", headers);
                    }
                }
            }
        }
    }
}
