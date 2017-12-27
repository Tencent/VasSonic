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
     * table name of the SessionData
     */
    protected static final String Sonic_SESSION_TABLE_NAME = "SessionData";

    /**
     * SessionData's id
     */
    protected static final String SESSION_DATA_COLUMN_SESSION_ID = "sessionID";

    /**
     * The key of eTag
     */
    protected static final String SESSION_DATA_COLUMN_ETAG = "eTag";

    /**
     * The key of templateTag
     */
    protected static final String SESSION_DATA_COLUMN_TEMPLATE_EAG = "templateTag";

    /**
     * The key of html sha1
     */
    protected static final String SESSION_DATA_COLUMN_HTML_SHA1 = "htmlSha1";

    /**
     * The key of html size
     */
    protected static final String SESSION_DATA_COLUMN_HTML_SIZE = "htmlSize";

    /**
     * The key of template update time
     */
    protected static final String SESSION_DATA_COLUMN_TEMPLATE_UPDATE_TIME = "templateUpdateTime";

    /**
     * The key of Unavailable Time
     */
    protected static final String SESSION_DATA_COLUMN_UNAVAILABLE_TIME = "UnavailableTime";

    /**
     * The key of cache expired Time
     */
    protected static final String SESSION_DATA_COLUMN_CACHE_EXPIRED_TIME = "cacheExpiredTime";

    /**
     * The key of cache hit count
     */
    protected static final String SESSION_DATA_COLUMN_CACHE_HIT_COUNT = "cacheHitCount";

    /**
     * The create table sql
     */
    public static final String CREATE_TABLE_SQL = "CREATE TABLE IF NOT EXISTS " + Sonic_SESSION_TABLE_NAME + " ( " +
            "id  integer PRIMARY KEY autoincrement" +
            " , " + SESSION_DATA_COLUMN_SESSION_ID + " text not null" +
            " , " + SESSION_DATA_COLUMN_ETAG + " text not null" +
            " , " + SESSION_DATA_COLUMN_TEMPLATE_EAG + " text" +
            " , " + SESSION_DATA_COLUMN_HTML_SHA1 + " text not null" +
            " , " + SESSION_DATA_COLUMN_UNAVAILABLE_TIME + " integer default 0" +
            " , " + SESSION_DATA_COLUMN_HTML_SIZE + " integer default 0" +
            " , " + SESSION_DATA_COLUMN_TEMPLATE_UPDATE_TIME + " integer default 0" +
            " , " + SESSION_DATA_COLUMN_CACHE_EXPIRED_TIME + " integer default 0" +
            " , " + SESSION_DATA_COLUMN_CACHE_HIT_COUNT + " integer default 0" +
            " ); ";

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
     *
     * @return all of the column in {@code Sonic_SESSION_TABLE_NAME}
     */
    static String[] getAllSessionDataColumn() {
        return new String[] {SESSION_DATA_COLUMN_SESSION_ID, SESSION_DATA_COLUMN_ETAG,
                SESSION_DATA_COLUMN_TEMPLATE_EAG, SESSION_DATA_COLUMN_HTML_SHA1,
                SESSION_DATA_COLUMN_UNAVAILABLE_TIME, SESSION_DATA_COLUMN_HTML_SIZE,
                SESSION_DATA_COLUMN_TEMPLATE_UPDATE_TIME, SESSION_DATA_COLUMN_CACHE_EXPIRED_TIME,
                SESSION_DATA_COLUMN_CACHE_HIT_COUNT};
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
        Cursor cursor = db.query(Sonic_SESSION_TABLE_NAME,
                getAllSessionDataColumn(),
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
        Cursor cursor = db.query(Sonic_SESSION_TABLE_NAME,
                getAllSessionDataColumn(),
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
        db.insert(Sonic_SESSION_TABLE_NAME, null, contentValues);
    }

    private static void updateSessionData(SQLiteDatabase db, String sessionId, SessionData sessionData) {
        ContentValues contentValues = getContentValues(sessionId, sessionData);
        db.update(Sonic_SESSION_TABLE_NAME, contentValues, SESSION_DATA_COLUMN_SESSION_ID + "=?",
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
        db.delete(Sonic_SESSION_TABLE_NAME, SESSION_DATA_COLUMN_SESSION_ID + "=?",
                new String[] {sessionId});
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
            return true;
        } else {
            sessionData = new SessionData();
            sessionData.sessionId = sessionId;
            sessionData.eTag = "Unknown";
            sessionData.htmlSha1 = "Unknown";
            sessionData.unAvailableTime = unavailableTime;
            insertSessionData(db, sessionId, sessionData);
            return true;
        }
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
        db.delete(Sonic_SESSION_TABLE_NAME, null, null);
    }
}
