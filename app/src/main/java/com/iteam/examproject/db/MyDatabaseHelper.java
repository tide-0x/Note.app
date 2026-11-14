package com.iteam.examproject.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class MyDatabaseHelper extends SQLiteOpenHelper {
    //用户表
    public static final String TABLE_USERS = "table_users";
    public static final String USER_ID = "userId";
    public static final String ACCOUNT = "account";
    public static final String PASSWORD = "password";

    private static final String CREATE_TABLE_USERS = "create table " + TABLE_USERS + "(" +
            USER_ID + " integer primary key," +
            ACCOUNT + " text," +
            PASSWORD + " text )";

    //分组表
    public static final String TABLE_GROUPS = "table_groups";
    public static final String GROUP_ID = "groupId";
    public static final String GROUP_NAME = "groupName";

    private static final String CREATE_TABLE_GROUPS = "create table " + TABLE_GROUPS + "(" +
            USER_ID + " integer, " +
            GROUP_ID + " integer primary key," +
            GROUP_NAME + " text)";

    //内容表
    public static final String TABLE_NOTES = "table_notes";
    public static final String NOTES_ID="notesId";
    public static final String TITLE = "title";
    public static final String CONTENT = "content";
    public static final String EDIT_TIME = "editedTime";

    private static final String CREATE_TABLE_NOTES="create table "+TABLE_NOTES+"(" +
            USER_ID+" integer," +
            GROUP_ID+" integer," +
            NOTES_ID+" integer,"+
            TITLE +" text," +
            CONTENT+" text," +
            EDIT_TIME +" integer)";

    //索引：笔记/分组都按 (userId, groupId) 查，之前全表扫描
    private static final String CREATE_INDEX_GROUPS_USER =
            "create index if not exists idx_groups_user on " + TABLE_GROUPS + "(" + USER_ID + ")";

    private static final String CREATE_INDEX_NOTES_USER_GROUP =
            "create index if not exists idx_notes_user_group on " + TABLE_NOTES + "(" + USER_ID + ", " + GROUP_ID + ")";

    private static final String CREATE_INDEX_NOTES_USER_NOTES =
            "create index if not exists idx_notes_user_notes on " + TABLE_NOTES + "(" + USER_ID + ", " + NOTES_ID + ")";

    private static final String CREATE_INDEX_USERS_ACCOUNT =
            "create index if not exists idx_users_account on " + TABLE_USERS + "(" + ACCOUNT + ")";


    public MyDatabaseHelper(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_USERS);
        db.execSQL(CREATE_TABLE_GROUPS);
        db.execSQL(CREATE_TABLE_NOTES);
        createIndexes(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //v1 -> v2：只补索引，表结构不变，老数据不受影响
        if (oldVersion < 2) {
            createIndexes(db);
        }
    }

    private void createIndexes(SQLiteDatabase db) {
        db.execSQL(CREATE_INDEX_USERS_ACCOUNT);
        db.execSQL(CREATE_INDEX_GROUPS_USER);
        db.execSQL(CREATE_INDEX_NOTES_USER_GROUP);
        db.execSQL(CREATE_INDEX_NOTES_USER_NOTES);
    }
}
