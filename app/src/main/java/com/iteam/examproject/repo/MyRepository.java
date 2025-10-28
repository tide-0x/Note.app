package com.iteam.examproject.repo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.iteam.examproject.db.MyDatabaseHelper;
import com.iteam.examproject.entity.User;

public class MyRepository {
    private static final String DATABASE_NAME = "database.db";
    private static final int DATABASE_VERSION = 1;
    private MyDatabaseHelper dbHelper;
    private SQLiteDatabase database;
    ContentValues values=new ContentValues();

    public MyRepository(Context context) {
        dbHelper = new MyDatabaseHelper(context, DATABASE_NAME, null, 1);
    }

    public void open() {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        database.close();
    }

    //表名
    //返回查询的列

    //where
    //查哪一列
    //列的值

    //分组
    //分组下的查询

    //排序


    //判断是否存在
    public boolean isExisted(String tableName, String selection, String[] selectionArgs) {
        //select count(*) as count from tableName where selection
        Cursor cursor = null;
        try {
            cursor = database.query(tableName,
                    new String[]{"count(*) as count"},
                    selection,
                    selectionArgs,
                    null, null, null);
            //存在返回true，不存在返回false
            //cursor指向表头，调用moveToFirst()移到数据行
            cursor.moveToFirst();
            int count=cursor.getInt(0);
            return count>0;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    //注册
    //返回完整的用户信息，失败返回null
    public User register(String account, String password) {
        //select account from table_users where account=account and password=password
        boolean result = isExisted(MyDatabaseHelper.TABLE_USERS,
                MyDatabaseHelper.ACCOUNT + "=?", new String[]{account});
        if (result) {//账号存在，返回null
            return null;
        } else {//账号不存在，存入数据库并返回user信息
            values.put(MyDatabaseHelper.ACCOUNT, account);
            values.put(MyDatabaseHelper.PASSWORD, password);
            long userId = database.insert(MyDatabaseHelper.TABLE_USERS,
                    null,
                    values);
            values.clear();
            return new User(userId, account, password);
        }
    }

    //登录
    public User login(String account,String password){
        Cursor cursor=null;
        try {
            cursor=database.query(MyDatabaseHelper.TABLE_USERS,
                    null,
                    MyDatabaseHelper.ACCOUNT+"=? and "+ MyDatabaseHelper.PASSWORD+"=?",
                    new String[]{account,password},
                    null,null,null);
            if(cursor.moveToFirst()){
                User user=new User();
                int index=cursor.getColumnIndex(MyDatabaseHelper.USER_ID);
                long userId=cursor.getLong(index);
                user.setUserId(userId);
                index=cursor.getColumnIndex(MyDatabaseHelper.ACCOUNT);
                String acc=cursor.getString(index);
                user.setAccount(acc);
                index=cursor.getColumnIndex(MyDatabaseHelper.PASSWORD);
                String pwd=cursor.getString(index);
                user.setPassword(pwd);
                return user;
            }
            return null;
        } finally {
            if(cursor!=null){
                cursor.close();
            }
        }
    }


    //创建笔记
    //修改笔记（内容，组别）
    //删除笔记
    //查看笔记

    //创建分组
    //删除分组
    //修改分组
    //查看分组

}
