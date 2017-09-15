package com.tencent.sonic.sdk;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by marlonlu on 2017/9/14.
 */

public class SonicDBHelper extends SQLiteOpenHelper {

    private static final String SONIC_DATABASE_NAME = "sonic.db";

    private static final int SONIC_DATABASE_VERSION = 1;

    protected static final String Sonic_SESSION_TABLE_NAME = "SessionData";

    /**
     * SessionData's id
     */
    protected static final String SESSION_DATA_COLUMN_SESSION_ID = "sessionID";

    /**
     * The key of eTag
     */
    protected static final String SESSION_DATA_COLUMN_ETAG = "etag";

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

    private static AtomicBoolean upgradOldData = new AtomicBoolean(false);

    public SonicDBHelper(Context context) {
        super(context, SONIC_DATABASE_NAME, null, SONIC_DATABASE_VERSION);
    }

    public static synchronized SonicDBHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new SonicDBHelper(context);
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

    @Override
    public void onCreate(SQLiteDatabase db) {
        StringBuilder createTableSql = new StringBuilder("CREATE TABLE IF NOT EXISTS ");

        createTableSql.append(Sonic_SESSION_TABLE_NAME).append(" ( ");

        //column
        createTableSql.append("id  integer PRIMARY KEY autoincrement");
        createTableSql.append(" , ").append(SESSION_DATA_COLUMN_SESSION_ID).append(" text not null");
        createTableSql.append(" , ").append(SESSION_DATA_COLUMN_ETAG).append(" text not null");
        createTableSql.append(" , ").append(SESSION_DATA_COLUMN_TEMPLATE_EAG).append(" text");
        createTableSql.append(" , ").append(SESSION_DATA_COLUMN_HTML_SHA1).append(" text not null");
        createTableSql.append(" , ").append(SESSION_DATA_COLUMN_UNAVAILABLE_TIME).append(" integer default 0");
        createTableSql.append(" , ").append(SESSION_DATA_COLUMN_HTML_SIZE).append(" integer default 0");
        createTableSql.append(" , ").append(SESSION_DATA_COLUMN_TEMPLATE_UPDATE_TIME).append(" integer default 0");
        createTableSql.append(" , ").append(SESSION_DATA_COLUMN_CACHE_EXPIRED_TIME).append(" integer default 0");
        createTableSql.append(" , ").append(SESSION_DATA_COLUMN_CACHE_HIT_COUNT).append(" integer default 0");

        createTableSql.append(" ); ");

        db.execSQL(createTableSql.toString());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        upgrade();
    }

    /**
     * Upgrade old session data from SP into Database.
     */
    public static void upgrade() {
        upgradOldData.set(true);
        SonicDataHelper.upgradeOldDataFromSp();
        upgradOldData.set(false);

    }

    /**
     * Indicates whether is upgrading or not. If return true, It will fail to make any session request.
     * @return
     */
    public static boolean isUpgrading() {
        return upgradOldData.get();
    }
}
