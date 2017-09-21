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
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;


public class SonicDBHelper extends SQLiteOpenHelper {

    /**
     * log filter
     */
    private static final String TAG = SonicConstants.SONIC_SDK_LOG_PREFIX + "SonicDBHelper";

    /**
     * name of the database file
     */
    private static final String SONIC_DATABASE_NAME = "sonic.db";

    /**
     * number of the database (starting at 1)
     */
    private static final int SONIC_DATABASE_VERSION = 1;

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

    private static SonicDBHelper sInstance = null;

    private static AtomicBoolean isDBUpgrading = new AtomicBoolean(false);

    private SonicDBHelper(Context context) {
        super(context, SONIC_DATABASE_NAME, null, SONIC_DATABASE_VERSION);
    }

    static synchronized SonicDBHelper createInstance(Context context) {
        if (null == sInstance) {
            sInstance = new SonicDBHelper(context);
        }
        return sInstance;
    }

    public static synchronized SonicDBHelper getInstance() {
        if (null == sInstance) {
            throw new IllegalStateException("SonicDBHelper::createInstance() needs to be called before SonicDBHelper::getInstance()!");
        }
        return sInstance;
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
     * Called when the database is created for the first time. This is where the
     * creation of tables and the initial population of the tables should happen.
     *
     * @param db The database.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        // create sessionData table
        String createTableSql = "CREATE TABLE IF NOT EXISTS " + Sonic_SESSION_TABLE_NAME + " ( " +
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
        db.execSQL(createTableSql);

        // upgrade SP if need(session data save in SP on sdk 1.0)
        onUpgrade(db, -1, SONIC_DATABASE_VERSION);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (isDBUpgrading.compareAndSet(false, true)) {
            long startTime = System.currentTimeMillis();
            SonicUtils.log(TAG, Log.INFO, "onUpgrade start, from " + oldVersion + " to " + newVersion + ".");
            if (-1 == oldVersion) {
                SonicEngine.getInstance().getRuntime().postTaskToThread(new Runnable() {
                    @Override
                    public void run() {
                        SonicUtils.removeAllSessionCache();
                        isDBUpgrading.set(false);
                    }
                }, 0L);
            } else {
                doUpgrade(db, oldVersion, newVersion);
                isDBUpgrading.set(false);
            }
            SonicUtils.log(TAG, Log.INFO, "onUpgrade finish, cost " + (System.currentTimeMillis() - startTime) + "ms.");
        }
    }

    /**
     * Upgrade old session data from SP into Database.
     */
    private void doUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    /**
     * Indicates whether is upgrading or not. If return true, It will fail to create session.
     * @return is Upgrading or not
     */
    public boolean isUpgrading() {
        return isDBUpgrading.get();
    }
}
