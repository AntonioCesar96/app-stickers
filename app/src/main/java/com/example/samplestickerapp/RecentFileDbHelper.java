package com.example.samplestickerapp;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class RecentFileDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "recent_files.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_NAME = "arquivo_recente";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_PATH = "caminho";

    private static final String SQL_CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_PATH + " TEXT NOT NULL" +
                    ");";

    private static final String SQL_DROP_TABLE =
            "DROP TABLE IF EXISTS " + TABLE_NAME;

    public RecentFileDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL(SQL_DROP_TABLE);
        onCreate(db);
    }
}
