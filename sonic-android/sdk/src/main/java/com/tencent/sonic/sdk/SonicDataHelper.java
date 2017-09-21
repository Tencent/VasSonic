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
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

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
     * Sonic data structure
     */
    static class SessionData {

        String sessionId;

        /**
         * The eTag of html
         */
        String eTag;

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
            eTag = "";
            templateTag = "";
            htmlSha1 = "";
            htmlSize = 0;
            templateUpdateTime = 0;
            expiredTime = 0;
            cacheHitCount = 0;
            unAvailableTime = 0;
        }
    }
    
    /**
     * Get sonic sessionData by unique session id
     *
     * @param sessionId a unique session id
     * @return SessionData
     */
    @NonNull static SessionData getSessionData(String sessionId) {
        SQLiteDatabase db = SonicDBHelper.getInstance().getWritableDatabase();
        SessionData sessionData = getSessionData(db, sessionId);
        db.close();
        if (null == sessionData) {
            sessionData = new SessionData();
        }
        return sessionData;
    }

    /**
     * Get sonic sessionData by unique session id
     *
     * @param db The database.
     * @param sessionId a unique session id
     * @return SessionData
     */
    private static SessionData getSessionData(SQLiteDatabase db, String sessionId) {
        Cursor cursor = db.query(SonicDBHelper.Sonic_SESSION_TABLE_NAME,
                SonicDBHelper.getAllSessionDataColumn(),
                SESSION_DATA_COLUMN_SESSION_ID + "=?",
                new String[] {sessionId},
                null, null, null);

        SessionData sessionData = null;
        if (cursor != null && cursor.moveToFirst()) {
            sessionData = querySessionData(cursor);
        }
        if(cursor != null){
            cursor.close();
        }
        return sessionData;
    }

    /**
     * translate cursor to session data.
     * @param cursor db cursor
     */
    private static SessionData querySessionData(Cursor cursor) {
        SessionData sessionData = new SessionData();
        sessionData.sessionId = cursor.getString(cursor.getColumnIndex(SESSION_DATA_COLUMN_SESSION_ID));
        sessionData.eTag = cursor.getString(cursor.getColumnIndex(SESSION_DATA_COLUMN_ETAG));
        sessionData.htmlSha1 = cursor.getString(cursor.getColumnIndex(SESSION_DATA_COLUMN_HTML_SHA1));
        sessionData.htmlSize = cursor.getLong(cursor.getColumnIndex(SESSION_DATA_COLUMN_HTML_SIZE));
        sessionData.templateTag = cursor.getString(cursor.getColumnIndex(SESSION_DATA_COLUMN_TEMPLATE_EAG));
        sessionData.templateUpdateTime = cursor.getLong(cursor.getColumnIndex(SESSION_DATA_COLUMN_TEMPLATE_UPDATE_TIME));
        sessionData.expiredTime = cursor.getLong(cursor.getColumnIndex(SESSION_DATA_COLUMN_CACHE_EXPIRED_TIME));
        sessionData.unAvailableTime = cursor.getLong(cursor.getColumnIndex(SESSION_DATA_COLUMN_UNAVAILABLE_TIME));
        sessionData.cacheHitCount = cursor.getInt(cursor.getColumnIndex(SESSION_DATA_COLUMN_CACHE_HIT_COUNT));
        return sessionData;
    }

    /**
     *
     * @return all of the session data order by HitCount decrease.
     */
    static List<SessionData> getAllSessionByHitCount() {
        List<SessionData> sessionDatas = new ArrayList<SessionData>();
        SQLiteDatabase db = SonicDBHelper.getInstance().getWritableDatabase();
        Cursor cursor = db.query(SonicDBHelper.Sonic_SESSION_TABLE_NAME,
                SonicDBHelper.getAllSessionDataColumn(),
                null,null,null, null, SESSION_DATA_COLUMN_CACHE_HIT_COUNT + " ASC");
        while(cursor != null && cursor.moveToNext()) {
            sessionDatas.add(querySessionData(cursor));
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
        SQLiteDatabase db = SonicDBHelper.getInstance().getWritableDatabase();
        saveSessionData(db, sessionId, sessionData);
        db.close();
    }

    /**
     * Save or update sonic sessionData with a unique session id
     *
     * @param db The database.
     * @param sessionId   a unique session id
     * @param sessionData SessionData
     */
    private static void saveSessionData(SQLiteDatabase db, String sessionId, SessionData sessionData) {
        sessionData.sessionId = sessionId;
        SessionData storedSessionData = getSessionData(db, sessionId);
        if (storedSessionData != null) {
            sessionData.cacheHitCount = storedSessionData.cacheHitCount;
            updateSessionData(db, sessionId, sessionData);
        } else {
            insertSessionData(db, sessionId, sessionData);
        }
    }

    private static void insertSessionData(SQLiteDatabase db, String sessionId, SessionData sessionData) {
        ContentValues contentValues = getContentValues(sessionId, sessionData);
        db.insert(SonicDBHelper.Sonic_SESSION_TABLE_NAME, null, contentValues);
    }

    private static void updateSessionData(SQLiteDatabase db, String sessionId, SessionData sessionData) {
        ContentValues contentValues = getContentValues(sessionId, sessionData);
        db.update(SonicDBHelper.Sonic_SESSION_TABLE_NAME, contentValues, SESSION_DATA_COLUMN_SESSION_ID + "=?",
                new String[] {sessionId});
    }

    @NonNull
    private static ContentValues getContentValues(String sessionId, SessionData sessionData) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(SESSION_DATA_COLUMN_SESSION_ID, sessionId);
        contentValues.put(SESSION_DATA_COLUMN_ETAG, sessionData.eTag);
        contentValues.put(SESSION_DATA_COLUMN_HTML_SHA1, sessionData.htmlSha1);
        contentValues.put(SESSION_DATA_COLUMN_HTML_SIZE, sessionData.htmlSize);
        contentValues.put(SESSION_DATA_COLUMN_TEMPLATE_EAG, sessionData.templateTag);
        contentValues.put(SESSION_DATA_COLUMN_TEMPLATE_UPDATE_TIME, sessionData.templateUpdateTime);
        contentValues.put(SESSION_DATA_COLUMN_CACHE_EXPIRED_TIME, sessionData.expiredTime);
        contentValues.put(SESSION_DATA_COLUMN_UNAVAILABLE_TIME, sessionData.unAvailableTime);
        contentValues.put(SESSION_DATA_COLUMN_CACHE_HIT_COUNT, sessionData.cacheHitCount);
        return contentValues;
    }


    /**
     * Remove a unique session data
     *
     * @param sessionId A unique session id
     */
    static void removeSessionData(String sessionId) {
        SQLiteDatabase db = SonicDBHelper.getInstance().getWritableDatabase();
        db.delete(SonicDBHelper.Sonic_SESSION_TABLE_NAME, SESSION_DATA_COLUMN_SESSION_ID + "=?",
                new String[] {sessionId});
        db.close();
    }

    /**
     * Set sonic unavailable time, sonic will not execute its logic before this time.
     *
     * @param sessionId       A unique session id.
     * @param unavailableTime Unavailable time.
     * @return The result of save unavailable time
     */
    static boolean setSonicUnavailableTime(String sessionId, long unavailableTime) {
        SQLiteDatabase db = SonicDBHelper.getInstance().getWritableDatabase();
        SessionData sessionData = getSessionData(db, sessionId);
        if (sessionData != null) {
            sessionData.unAvailableTime = unavailableTime;
            updateSessionData(db, sessionId, sessionData);
            db.close();
            return true;
        }
        db.close();
        return false;
    }

    /**
     * Get the sonic unavailable time
     *
     * @param sessionId A unique session id
     * @return The sonic unavailable time
     */
    static long getLastSonicUnavailableTime(String sessionId) {
        SessionData sessionData = getSessionData(sessionId);
        return sessionData.unAvailableTime;
    }

    /**
     * It will increase HitCount when local session cache is used.
     * @param sessionId session id
     */
    static void updateSonicCacheHitCount(String sessionId) {
        SQLiteDatabase db = SonicDBHelper.getInstance().getWritableDatabase();
        updateSonicCacheHitCount(db, sessionId);
        db.close();
    }

    /**
     * It will increase HitCount when local session cache is used.
     *
     * @param sessionId session id
     * @param db The database.
     */
    private static void updateSonicCacheHitCount(SQLiteDatabase db, String sessionId) {
        SessionData sessionData = getSessionData(db, sessionId);
        if (sessionData != null) {
            sessionData.cacheHitCount += 1;
            updateSessionData(db, sessionId, sessionData);
        }
    }

    /**
     * Remove all sonic data
     */
    static synchronized void clear() {
        SQLiteDatabase db = SonicDBHelper.getInstance().getWritableDatabase();
        db.delete(SonicDBHelper.Sonic_SESSION_TABLE_NAME, null, null);
        db.close();
    }
}
