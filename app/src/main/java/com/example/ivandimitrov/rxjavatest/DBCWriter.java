package com.example.ivandimitrov.rxjavatest;

import android.content.ContentValues;
import android.content.Context;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.os.Looper;

/**
 * Created by Ivan Dimitrov on 1/26/2017.
 */

public class DBCWriter extends Thread {
    public static final int    DATABASE_MAX_LENGTH = 100;
    public static final String SQL_CREATE_ENTRIES  =
            "CREATE TABLE " + FeedReaderContract.FeedEntry.TABLE_NAME + " (" +
                    FeedReaderContract.FeedEntry._ID + " INTEGER PRIMARY KEY," +
                    FeedReaderContract.FeedEntry.COLUMN_JOKE + " TEXT);";

    public static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + FeedReaderContract.FeedEntry.TABLE_NAME;

    private Context            mContext;
    private Looper             mLooper;
    //SQL
    private SQLiteDatabase     mDataBase;
    private FeedReaderDbHelper mDbHelper;
    private ContentValues      mContentValues;
    private boolean isDatabaseFull = false;
    private boolean isRunning      = false;

    DBCWriter(Context context) {
        mContext = context;
    }

    @Override
    public void run() {
        Looper.prepare();
        mLooper = Looper.myLooper();
        isRunning = true;
        initDB();
        Looper.loop();
    }

    public Looper getLooper() {
        return mLooper;
    }

    public void stopWriter() {
        Looper.myLooper().quit();
    }

    public long getDBJokeCount() {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        long cnt = DatabaseUtils.queryNumEntries(db, FeedReaderContract.FeedEntry.TABLE_NAME);
        return cnt;
    }

    private void initDB() {
        mDbHelper = new FeedReaderDbHelper(mContext);
        mContentValues = new ContentValues();
        mDataBase = mDbHelper.getWritableDatabase();
    }

    public boolean writeToDB(String joke) {
        if (getDBJokeCount() > DATABASE_MAX_LENGTH) {
            return false;
        }
        mContentValues.put(FeedReaderContract.FeedEntry.COLUMN_JOKE, joke);
        long newRowId = mDataBase.insert(FeedReaderContract.FeedEntry.TABLE_NAME, null, mContentValues);
        return true;
    }
}
