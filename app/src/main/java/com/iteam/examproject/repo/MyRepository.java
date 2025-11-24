package com.iteam.examproject.repo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.iteam.examproject.db.MyDatabaseHelper;
import com.iteam.examproject.entity.NoteGroup;
import com.iteam.examproject.entity.Notes;
import com.iteam.examproject.entity.User;

import java.util.ArrayList;
import java.util.List;

public class MyRepository {
    private static final String DATABASE_NAME = "database.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TAG = "MyRepository";
    private MyDatabaseHelper dbHelper;
    private SQLiteDatabase database;
    public static final String SORT_ASC = "asc";
    public static final String SORT_DESC = "desc";
    ContentValues values = new ContentValues();

    public MyRepository(Context context) {
        dbHelper = new MyDatabaseHelper(context, DATABASE_NAME, null, DATABASE_VERSION);
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
            int count = cursor.getInt(0);
            return count > 0;
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
            //insert into table_users (account password) values (account,password)
            long userId = database.insert(MyDatabaseHelper.TABLE_USERS,
                    null,
                    values);
            values.clear();
            return new User(userId, account, password);
        }
    }

    //登录
    public User login(String account, String password) {
        Cursor cursor = null;
        try {
            //select * form table_users where account=account and password=password
            cursor = database.query(MyDatabaseHelper.TABLE_USERS,
                    null,
                    MyDatabaseHelper.ACCOUNT + "=? and " + MyDatabaseHelper.PASSWORD + "=?",
                    new String[]{account, password},
                    null, null, null);
            if (cursor.moveToFirst()) {
                User user = new User();
                int index = cursor.getColumnIndex(MyDatabaseHelper.USER_ID);
                long userId = cursor.getLong(index);
                user.setUserId(userId);
                index = cursor.getColumnIndex(MyDatabaseHelper.ACCOUNT);
                String acc = cursor.getString(index);
                user.setAccount(acc);
                index = cursor.getColumnIndex(MyDatabaseHelper.PASSWORD);
                String pwd = cursor.getString(index);
                user.setPassword(pwd);
                return user;
            }
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    //修改账号密码
    public void updatePassword(long userId, String changedPassword) {
        values.put(MyDatabaseHelper.PASSWORD, changedPassword);
        //update table_users
        //set password values changedPassword
        //where userId=userId
        database.update(MyDatabaseHelper.TABLE_USERS,
                values,
                MyDatabaseHelper.USER_ID + "=?",
                new String[]{String.valueOf(userId)});
        values.clear();
    }

    //注销账号
    public void cancelUser(long userId) {
        //删除用户表中的
        //delete from table_users where userId=userId
        database.delete(MyDatabaseHelper.TABLE_USERS,
                MyDatabaseHelper.USER_ID + "=?",
                new String[]{String.valueOf(userId)});
        //删除分组表中的
        //delete from table_groups where userId=userId
        database.delete(MyDatabaseHelper.TABLE_GROUPS,
                MyDatabaseHelper.USER_ID + "=?",
                new String[]{String.valueOf(userId)});
        //删除笔记表中的
        //delete from table_notes where userId=userId
        database.delete(MyDatabaseHelper.TABLE_NOTES,
                MyDatabaseHelper.USER_ID + "=?",
                new String[]{String.valueOf(userId)});
    }


    //创建笔记
    //1.从创建笔记处添加(全部笔记中添加还是某个分组中添加)
    //1.1全部笔记处的groupId手动写死为0
    //2.从某个分组中添加到某个分组
    public long createNote(long userId, long groupId, String title, String content, long editedTime) {
        createNotesCodeBlock(userId, groupId, title, content, editedTime);
        //insert into table_notes
        //(userId,groupId,title,content,editedTime) values (userId,groupId,title,content,editedTime)
        long notesId=database.insert(MyDatabaseHelper.TABLE_NOTES, null, values);
        values.clear();
        if (groupId != 0) {
            //在别的分组中添加笔记，则给全部笔记中备份groupId=0
            createNotesCodeBlock(userId, 0, title, content, editedTime);
            database.insert(MyDatabaseHelper.TABLE_NOTES, null, values);
            values.clear();
        }
        values.put(MyDatabaseHelper.NOTES_ID,notesId);
        //update table_notes
        //set notesId=notesId
        //where userId=userId and (groupId=groupId or groupId=0) and editedTime =editedTime
        database.update(MyDatabaseHelper.TABLE_NOTES,
                values,
                MyDatabaseHelper.USER_ID+"=? and ("+MyDatabaseHelper.GROUP_ID+"=? or "+MyDatabaseHelper.GROUP_ID+"=?) and "+MyDatabaseHelper.EDIT_TIME+"=?",
                new String[]{String.valueOf(userId),String.valueOf(groupId),String.valueOf(0),String.valueOf(editedTime)});
        values.clear();
        return notesId;
    }

    private void createNotesCodeBlock(long userId, long groupId, String title, String content, long editedTime) {
        values.put(MyDatabaseHelper.USER_ID, userId);
        values.put(MyDatabaseHelper.GROUP_ID, groupId);
        values.put(MyDatabaseHelper.TITLE, title);
        values.put(MyDatabaseHelper.CONTENT, content);
        values.put(MyDatabaseHelper.EDIT_TIME, editedTime);
    }

    //将笔记添加到别的组
    public boolean addNotesToGroup(Notes notes) {
        //主键唯一
        //select count(*) as count from table_notes where groupId=groupId and groupId=groupId and notesId=notesId
        boolean result = isExisted(MyDatabaseHelper.TABLE_NOTES,
                MyDatabaseHelper.USER_ID + "=? and " + MyDatabaseHelper.GROUP_ID + "=? and " + MyDatabaseHelper.NOTES_ID + "=?",
                new String[]{String.valueOf(notes.getUserId()), String.valueOf(notes.getGroupId()), String.valueOf(notes.getNotesId())});
        if (!result) {//不存在，可以添加
            values.put(MyDatabaseHelper.USER_ID, notes.getUserId());
            values.put(MyDatabaseHelper.GROUP_ID, notes.getGroupId());
            values.put(MyDatabaseHelper.NOTES_ID, notes.getNotesId());
            values.put(MyDatabaseHelper.TITLE, notes.getTitle());
            values.put(MyDatabaseHelper.CONTENT, notes.getContent());
            values.put(MyDatabaseHelper.EDIT_TIME, notes.getEditTime());
            //insert into table_notes
            //values userId,groupId,notesId,title,content,editedTime
            database.insert(MyDatabaseHelper.TABLE_NOTES, null, values);
            values.clear();
        }
        return !result;//false
    }



    //修改笔记（标题，内容，编辑时间）
    public void updateNotes(Notes notes) {
        values.put(MyDatabaseHelper.TITLE, notes.getTitle());
        values.put(MyDatabaseHelper.CONTENT, notes.getContent());
        values.put(MyDatabaseHelper.EDIT_TIME, notes.getEditTime());
        //update table_notes
        //set (title,content,editedTime) values (title,content,editedTime)
        //where userId=userId and notesId=notesId
        database.update(MyDatabaseHelper.TABLE_NOTES,
                values,
                MyDatabaseHelper.USER_ID + "=? and " + MyDatabaseHelper.NOTES_ID + "=?",
                new String[]{String.valueOf(notes.getUserId()), String.valueOf(notes.getNotesId())});
        values.clear();
    }


    //删除笔记
    public List<Notes> deleteNotes(long userId, long groupId, long notesId) {
        if (groupId == 0) {
            //全部笔记处：所有分组下的该笔记都要删除
            //delete from table_notes where userId=userId and notesId=notesId
            database.delete(MyDatabaseHelper.TABLE_NOTES,
                    MyDatabaseHelper.USER_ID + "=? and " + MyDatabaseHelper.NOTES_ID + "=?",
                    new String[]{String.valueOf(userId), String.valueOf(notesId)});
        } else {
            //删除该分组下的该笔记
            //delete from table_notes where userId=userId and groupId=groupId and notesId=notesId
            database.delete(MyDatabaseHelper.TABLE_NOTES,
                    MyDatabaseHelper.USER_ID + "=? and " + MyDatabaseHelper.GROUP_ID + "=? and " + MyDatabaseHelper.NOTES_ID + "=?",
                    new String[]{String.valueOf(userId), String.valueOf(groupId), String.valueOf(notesId)});
        }

        return getNotesList(userId,groupId);
    }

    //获取笔记(外界)
    //全部笔记的groupId为0
    public List<Notes> getNotesList(long userId, long groupId) {
        Cursor cursor = null;
        try {
            //select * from table_notes where userId=userId and groupId=groupId
            cursor = database.query(MyDatabaseHelper.TABLE_NOTES,
                    null,
                    MyDatabaseHelper.USER_ID + "=? and " + MyDatabaseHelper.GROUP_ID + "=?",
                    new String[]{String.valueOf(userId), String.valueOf(groupId)},
                    null, null, null, null);
            return gainNotesList(cursor);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    //获取笔记(代码块抽取)
    private List<Notes> gainNotesList(Cursor cursor) {
        List<Notes> notesList = new ArrayList<>();
        while (cursor.moveToNext()) {
            Notes notes = new Notes();
            int index = cursor.getColumnIndex(MyDatabaseHelper.USER_ID);
            notes.setUserId(cursor.getLong(index));

            index = cursor.getColumnIndex(MyDatabaseHelper.GROUP_ID);
            notes.setGroupId(cursor.getLong(index));

            index = cursor.getColumnIndex(MyDatabaseHelper.NOTES_ID);
            notes.setNotesId(cursor.getLong(index));

            index = cursor.getColumnIndex(MyDatabaseHelper.TITLE);
            notes.setTitle(cursor.getString(index));

            index = cursor.getColumnIndex(MyDatabaseHelper.CONTENT);
            notes.setContent(cursor.getString(index));

            index = cursor.getColumnIndex(MyDatabaseHelper.EDIT_TIME);
            notes.setEditTime(cursor.getLong(index));

            notesList.add(notes);
        }
        return notesList;
    }

    //创建分组
    public NoteGroup createGroup(long userId, String groupName) {
        boolean result = isExisted(MyDatabaseHelper.TABLE_GROUPS,
                MyDatabaseHelper.USER_ID + "=? and " + MyDatabaseHelper.GROUP_NAME + "=?",
                new String[]{String.valueOf(userId), groupName});
        if (result) {//true，分组已存在，返回null
            return null;
        } else {
            values.put(MyDatabaseHelper.USER_ID, userId);
            values.put(MyDatabaseHelper.GROUP_NAME, groupName);
            //insert into table_groups
            //(userId,groupName) values (userId,groupName)
            long groupId = database.insert(MyDatabaseHelper.TABLE_GROUPS, null, values);
            values.clear();
            //返回完整的分组信息
            return new NoteGroup(userId, groupId, groupName);
        }
    }


    //删除分组
    //同时删除该分组下的笔记
    public void deleteGroup(NoteGroup noteGroup) {
        //删除该分组
        //delete form table_groups where userId=userId and groupId=groupId
        database.delete(MyDatabaseHelper.TABLE_GROUPS,
                MyDatabaseHelper.USER_ID + "=? and " + MyDatabaseHelper.GROUP_ID + "=?",
                new String[]{String.valueOf(noteGroup.getUserId()),
                        String.valueOf(noteGroup.getGroupId())});
        //删除该分组下的笔记
        //delete from table_notes where userId=userId and groupId=groupId
        database.delete(MyDatabaseHelper.TABLE_NOTES,
                MyDatabaseHelper.USER_ID + "=? and " + MyDatabaseHelper.GROUP_ID + "=?",
                new String[]{String.valueOf(noteGroup.getUserId()),
                        String.valueOf(noteGroup.getGroupId())});
    }


    //修改分组名
    public boolean updateGroupName(NoteGroup noteGroup) {
        boolean existed=isExisted(MyDatabaseHelper.TABLE_GROUPS,
                MyDatabaseHelper.USER_ID+"=? and "+MyDatabaseHelper.GROUP_NAME+"=?",
                new String[]{String.valueOf(noteGroup.getUserId()), noteGroup.getGroupName()});
        if (!existed) {
            values.put(MyDatabaseHelper.GROUP_NAME, noteGroup.getGroupName());
            //分组表
            //update table_groups
            //set groupName values groupName
            //where userId=userId and groupId=groupId
            database.update(MyDatabaseHelper.TABLE_GROUPS,
                    values,
                    MyDatabaseHelper.USER_ID + "=? and " + MyDatabaseHelper.GROUP_ID + "=?",
                    new String[]{String.valueOf(noteGroup.getUserId()),
                            String.valueOf(noteGroup.getGroupId())});
        }
        return existed;
    }

    //获取分组
    public List<NoteGroup> getAllGroups(long userId) {
        //select * form table_groups where userId=userId
        Cursor cursor = null;
        List<NoteGroup> groups = new ArrayList<>();
        try {
            cursor = database.query(MyDatabaseHelper.TABLE_GROUPS,
                    null,
                    MyDatabaseHelper.USER_ID + "=?",
                    new String[]{String.valueOf(userId)},
                    null, null, null);
            while (cursor.moveToNext()) {
                NoteGroup noteGroup = new NoteGroup();

                int index = cursor.getColumnIndex(MyDatabaseHelper.USER_ID);
                long usId = cursor.getLong(index);
                noteGroup.setUserId(usId);

                index = cursor.getColumnIndex(MyDatabaseHelper.GROUP_ID);
                long groupId = cursor.getLong(index);
                noteGroup.setGroupId(groupId);

                index = cursor.getColumnIndex(MyDatabaseHelper.GROUP_NAME);
                String groupName = cursor.getString(index);
                noteGroup.setGroupName(groupName);
                groups.add(noteGroup);
            }
            return groups;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    //排序
    public List<Notes> notesSort(long userId, long groupId, String sortWay) {
        Cursor cursor = null;
        try {
            //select * from table_notes where userId=userId and groupId=groupId orderBy editedTime asc/desc
            cursor = database.query(MyDatabaseHelper.TABLE_NOTES,
                    null,
                    MyDatabaseHelper.USER_ID + "=? and " + MyDatabaseHelper.GROUP_ID + "=?",
                    new String[]{String.valueOf(userId), String.valueOf(groupId)},
                    null, null, MyDatabaseHelper.EDIT_TIME+" "+sortWay, null);

            return gainNotesList(cursor);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public List<Notes> searchResult(long userId,String keyWord){
        Cursor cursor=null;
        //select * from table_notes where userId=userId and groupId=0 and title like keyWord
        cursor=database.query(MyDatabaseHelper.TABLE_NOTES,
                null,
                MyDatabaseHelper.USER_ID+"=? and "+MyDatabaseHelper.GROUP_ID+"=? and "+MyDatabaseHelper.TITLE+" like ?",
                new String[]{String.valueOf(userId),String.valueOf(0),"%"+keyWord+"%"},
                null,null,null,null);
        return gainNotesList(cursor);
    }


}
