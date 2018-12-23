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
 * SonicResourceDataHelper manages the resource database.
 */
public class SonicResourceDataHelper {

    /**
     * Log filter
     */
    private static final String TAG = SonicConstants.SONIC_SDK_LOG_PREFIX + "SonicResourceDataHelper";

    /**
     * table name of resource data.
     */
    private static final String Sonic_RESOURCE_TABLE_NAME = "ResourceData";

    /**
     * resource data's id.
     */
    private static final String RESOURCE_DATA_COLUMN_RESOURCE_ID = "resourceID";

    /**
     * key of resource sha1.
     */
    private static final String RESOURCE_DATA_COLUMN_RESOURCE_SHA1 = "resourceSha1";

    /**
     * key of resource size.
     */
    private static final String RESOURCE_DATA_COLUMN_RESOURCE_SIZE = "resourceSize";

    /**
     * key of last update time.
     */
    private static final String RESOURCE_DATA_COLUMN_LAST_UPDATE_TIME = "resourceUpdateTime";

    /**
     * key of cache expired time.
     */
    private static final String RESOURCE_DATA_COLUMN_CACHE_EXPIRED_TIME = "cacheExpiredTime";

    /**
     * The create table sql
     */
    public static final String CREATE_TABLE_SQL = "CREATE TABLE IF NOT EXISTS " + Sonic_RESOURCE_TABLE_NAME + " ( " +
            "id  integer PRIMARY KEY autoincrement" +
            " , " + RESOURCE_DATA_COLUMN_RESOURCE_ID + " text not null" +
            " , " + RESOURCE_DATA_COLUMN_RESOURCE_SHA1 + " text not null" +
            " , " + RESOURCE_DATA_COLUMN_RESOURCE_SIZE + " integer default 0" +
            " , " + RESOURCE_DATA_COLUMN_LAST_UPDATE_TIME + " integer default 0" +
            " , " + RESOURCE_DATA_COLUMN_CACHE_EXPIRED_TIME + " integer default 0" +
            " ); ";

    /**
     * resource data structure
     */
    public static class ResourceData {

        String resourceId;

        /**
         * The sha1 of resource
         */
        public String resourceSha1;

        /**
         * The size of resource
         */
        public long resourceSize;

        /**
         * The latest time of resource update
         */
        long lastUpdateTime;

        /**
         * Indicates when local resource cache is expired.
         * If It is expired, the record of database and file on SDCard will be removed.
         */
        public long expiredTime;

        /**
         * Reset data
         */
        public void reset() {
            resourceSha1 = "";
            resourceSize = 0;
            lastUpdateTime = 0;
            expiredTime = 0;
        }
    }
    
    /**
     * Get sonic ResourceData by unique resource id
     *
     * @param resourceId a unique resource id
     * @return ResourceData
     */
    @NonNull
    public static ResourceData getResourceData(String resourceId) {
        SQLiteDatabase db = SonicDBHelper.getInstance().getWritableDatabase();
        ResourceData resourceData = getResourceData(db, resourceId);
        if (null == resourceData) {
            resourceData = new ResourceData();
        }
        return resourceData;
    }

    /**
     * Get sonic resourceData by unique resource id
     *
     * @param db The database.
     * @param resourceId a unique resource id
     * @return ResourceData
     */
    private static ResourceData getResourceData(SQLiteDatabase db, String resourceId) {
        Cursor cursor = db.query(Sonic_RESOURCE_TABLE_NAME,
                getAllResourceDataColumn(),
                RESOURCE_DATA_COLUMN_RESOURCE_ID + "=?",
                new String[] {resourceId},
                null, null, null);

        ResourceData resourceData = null;
        if (cursor != null && cursor.moveToFirst()) {
            resourceData = queryResourceData(cursor);
        }
        if(cursor != null){
            cursor.close();
        }
        return resourceData;
    }

    /**
     *
     * @return all of the column in {@code Sonic_RESOURCE_TABLE_NAME}
     */
    public static String[] getAllResourceDataColumn() {
        return new String[]{
                RESOURCE_DATA_COLUMN_RESOURCE_ID,
                RESOURCE_DATA_COLUMN_RESOURCE_SHA1,
                RESOURCE_DATA_COLUMN_RESOURCE_SIZE,
                RESOURCE_DATA_COLUMN_LAST_UPDATE_TIME,
                RESOURCE_DATA_COLUMN_CACHE_EXPIRED_TIME
        };
    }

    /**
     * translate cursor to resource data.
     * @param cursor db cursor
     */
    private static ResourceData queryResourceData(Cursor cursor) {
        ResourceData resourceData = new ResourceData();
        resourceData.resourceId = cursor.getString(cursor.getColumnIndex(RESOURCE_DATA_COLUMN_RESOURCE_ID));
        resourceData.resourceSha1 = cursor.getString(cursor.getColumnIndex(RESOURCE_DATA_COLUMN_RESOURCE_SHA1));
        resourceData.resourceSize = cursor.getLong(cursor.getColumnIndex(RESOURCE_DATA_COLUMN_RESOURCE_SIZE));
        resourceData.lastUpdateTime = cursor.getLong(cursor.getColumnIndex(RESOURCE_DATA_COLUMN_LAST_UPDATE_TIME));
        resourceData.expiredTime = cursor.getLong(cursor.getColumnIndex(RESOURCE_DATA_COLUMN_CACHE_EXPIRED_TIME));
        return resourceData;
    }

    /**
     * Save or update sonic resourceData with a unique resource id
     *
     * @param resourceId   a unique resource id
     * @param resourceData ResourceData
     */
    static void saveResourceData(String resourceId, ResourceData resourceData) {
        SQLiteDatabase db = SonicDBHelper.getInstance().getWritableDatabase();
        saveResourceData(db, resourceId, resourceData);
    }

    /**
     * Save or update sonic resourceData with a unique resource id
     *
     * @param db The database.
     * @param resourceId   a unique resource id
     * @param resourceData ResourceData
     */
    private static void saveResourceData(SQLiteDatabase db, String resourceId, ResourceData resourceData) {
        resourceData.resourceId = resourceId;
        ResourceData storedResourceData = getResourceData(db, resourceId);
        if (storedResourceData != null) {
            updateResourceData(db, resourceId, resourceData);
        } else {
            insertResourceData(db, resourceId, resourceData);
        }
    }

    private static void insertResourceData(SQLiteDatabase db, String resourceId, ResourceData resourceData) {
        ContentValues contentValues = getContentValues(resourceId, resourceData);
        db.insert(Sonic_RESOURCE_TABLE_NAME, null, contentValues);
    }

    private static void updateResourceData(SQLiteDatabase db, String resourceId, ResourceData resourceData) {
        ContentValues contentValues = getContentValues(resourceId, resourceData);
        db.update(Sonic_RESOURCE_TABLE_NAME, contentValues, RESOURCE_DATA_COLUMN_RESOURCE_ID + "=?",
                new String[] {resourceId});
    }

    static List<ResourceData> getAllResourceData() {
        List<ResourceData> resourceDataList = new ArrayList<ResourceData>();
        SQLiteDatabase db = SonicDBHelper.getInstance().getWritableDatabase();
        Cursor cursor = db.query(Sonic_RESOURCE_TABLE_NAME, getAllResourceDataColumn(),
                null,null,null, null, "");
        while(cursor != null && cursor.moveToNext()) {
            resourceDataList.add(queryResourceData(cursor));
        }
        return resourceDataList;
    }

    @NonNull
    private static ContentValues getContentValues(String resourceId, ResourceData resourceData) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(RESOURCE_DATA_COLUMN_RESOURCE_ID, resourceId);
        contentValues.put(RESOURCE_DATA_COLUMN_RESOURCE_SHA1, resourceData.resourceSha1);
        contentValues.put(RESOURCE_DATA_COLUMN_RESOURCE_SIZE, resourceData.resourceSize);
        contentValues.put(RESOURCE_DATA_COLUMN_LAST_UPDATE_TIME, resourceData.lastUpdateTime);
        contentValues.put(RESOURCE_DATA_COLUMN_CACHE_EXPIRED_TIME, resourceData.expiredTime);
        return contentValues;
    }


    /**
     * Remove a unique resource data
     *
     * @param resourceId A unique resource id
     */
    static void removeResourceData(String resourceId) {
        SQLiteDatabase db = SonicDBHelper.getInstance().getWritableDatabase();
        db.delete(Sonic_RESOURCE_TABLE_NAME, RESOURCE_DATA_COLUMN_RESOURCE_ID + "=?",
                new String[] {resourceId});
    }

    /**
     * Remove all sonic data
     */
    static synchronized void clear() {
        SQLiteDatabase db = SonicDBHelper.getInstance().getWritableDatabase();
        db.delete(Sonic_RESOURCE_TABLE_NAME, null, null);
    }
}
