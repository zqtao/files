package com.zqtao.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

import com.zqtao.files.FilesActivity;

public class FilesData extends SQLiteOpenHelper {
    public static final String DB_NAME = ".zqtao_media.db";
    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS MEDIA(ID INTEGER PRIMARY KEY AUTOINCREMENT,"+
            "PATH TEXT, FILE_TYPE INTEGER, THUMBNAIL_PATH TEXT);";
    private static final String TABLE_NAME = "MEDIA";
    private static final String PATH = "PATH";
    private static final String FILE_TYPE = "FILE_TYPE";
    private static final String THUMBNAIL_PATH = "THUMBNAIL_PATH";

    private static final int version = 1;

    public FilesData(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    public FilesData(Context context) {
        this(context, DB_NAME, null, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL(CREATE_TABLE);
        } catch (SQLException e) {
            Log.e(FilesActivity.logTag, "Create table failed!");
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(FilesActivity.logTag, "Old version is"+String.valueOf(oldVersion));
        Log.i(FilesActivity.logTag, "New version is"+String.valueOf(newVersion));
    }

    public long updateThumbnail(String filePath, String thumbnailPath) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(THUMBNAIL_PATH, thumbnailPath);
        String[] paths = {filePath};
        try {
            return db.update(TABLE_NAME, values, PATH+"=?", paths);
        } catch (SQLException e) {
            Log.e(FilesActivity.logTag, "Insert thumbnail failed!");
            return -1;
        }
    }

    public long insertFile(String filePath, int fileType) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(PATH, filePath);
        values.put(FILE_TYPE, fileType);
        try {
            return db.insert(TABLE_NAME, null, values);
        } catch (SQLException e) {
            Log.e(FilesActivity.logTag, "Insert thumbnail failed!");
            return -1;
        }
    }

    public String getThumbnail(String filePath) {
        SQLiteDatabase db = getReadableDatabase();
        String[] columns = {THUMBNAIL_PATH};
        String[] parms = {filePath};
        try {
            Cursor cursor = db.query(TABLE_NAME, columns, PATH+"=?", parms, null, null, null);
            cursor.moveToFirst();
            String thumbnailPath = "";
            while(!cursor.isAfterLast()) {
                thumbnailPath = cursor.getString(0);
                cursor.moveToNext();
            }
            cursor.close();
            return thumbnailPath;
        } catch (SQLException e) {
            Log.e(FilesActivity.logTag, "Get thumbnail failed!");
            return null;
        }
    }
}
