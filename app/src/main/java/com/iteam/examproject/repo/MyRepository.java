package com.iteam.examproject.repo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.iteam.examproject.db.MyDatabaseHelper;
import com.iteam.examproject.entity.NoteGroup;
import com.iteam.examproject.entity.Notes;
import com.iteam.examproject.entity.User;
import com.iteam.examproject.utils.PasswordUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 所有数据库操作的入口。
 * 每个方法各自创建 ContentValues，不共享可变状态，避免上一次调用失败后脏值残留到下一次。
 */
public class MyRepository {
    private static final String DATABASE_NAME = "database.db";
    private static final int DATABASE_VERSION = 2;
    private MyDatabaseHelper dbHelper;
    private SQLiteDatabase database;
    public static final String SORT_ASC = "asc";
    public static final String SORT_DESC = "desc";

    public MyRepository(Context context) {
        dbHelper = new MyDatabaseHelper(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void open() {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        database.close();
    }

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
        //select account from table_users where account=account
        boolean result = isExisted(MyDatabaseHelper.TABLE_USERS,
                MyDatabaseHelper.ACCOUNT + "=?", new String[]{account});
        if (result) {//账号存在，返回null
            return null;
        }
        //账号不存在，存入数据库并返回user信息
        ContentValues values = new ContentValues();
        values.put(MyDatabaseHelper.ACCOUNT, account);
        //只存摘要，不存明文
        values.put(MyDatabaseHelper.PASSWORD, PasswordUtil.hash(password));
        long userId = database.insert(MyDatabaseHelper.TABLE_USERS, null, values);
        //密码不再回传给上层，内存里也不保留明文
        return new User(userId, account);
    }

    //登录
    public User login(String account, String password) {
        Cursor cursor = null;
        try {
            //select * from table_users where account=account
            //密码是加盐摘要，不能再用 password=? 直接比对，取出来自己校验
            cursor = database.query(MyDatabaseHelper.TABLE_USERS,
                    null,
                    MyDatabaseHelper.ACCOUNT + "=?",
                    new String[]{account},
                    null, null, null);
            if (!cursor.moveToFirst()) {
                return null;
            }
            int index = cursor.getColumnIndex(MyDatabaseHelper.USER_ID);
            long userId = cursor.getLong(index);
            index = cursor.getColumnIndex(MyDatabaseHelper.ACCOUNT);
            String acc = cursor.getString(index);
            index = cursor.getColumnIndex(MyDatabaseHelper.PASSWORD);
            String stored = cursor.getString(index);

            if (!PasswordUtil.verify(password, stored)) {
                return null;
            }
            if (!PasswordUtil.isHashed(stored)) {
                //旧数据是明文，登录成功后顺手升级成摘要
                updatePassword(userId, password);
            }
            return new User(userId, acc);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    //修改账号密码
    public void updatePassword(long userId, String changedPassword) {
        ContentValues values = new ContentValues();
        values.put(MyDatabaseHelper.PASSWORD, PasswordUtil.hash(changedPassword));
        //update table_users set password=changedPassword where userId=userId
        database.update(MyDatabaseHelper.TABLE_USERS,
                values,
                MyDatabaseHelper.USER_ID + "=?",
                new String[]{String.valueOf(userId)});
    }

    //注销账号
    public void cancelUser(long userId) {
        //删除用户表中的
        database.delete(MyDatabaseHelper.TABLE_USERS,
                MyDatabaseHelper.USER_ID + "=?",
                new String[]{String.valueOf(userId)});
        //删除分组表中的
        database.delete(MyDatabaseHelper.TABLE_GROUPS,
                MyDatabaseHelper.USER_ID + "=?",
                new String[]{String.valueOf(userId)});
        //删除笔记表中的
        database.delete(MyDatabaseHelper.TABLE_NOTES,
                MyDatabaseHelper.USER_ID + "=?",
                new String[]{String.valueOf(userId)});
    }


    //创建笔记
    //1.从创建笔记处添加(全部笔记中添加还是某个分组中添加)
    //1.1全部笔记处的groupId手动写死为0
    //2.从某个分组中添加到某个分组
    public long createNote(long userId, long groupId, String title, String content, long editedTime) {
        //insert into table_notes (userId,groupId,title,content,editedTime) values (...)
        long notesId = database.insert(MyDatabaseHelper.TABLE_NOTES,
                null, noteValues(userId, groupId, title, content, editedTime));
        if (groupId != 0) {
            //在别的分组中添加笔记，则给全部笔记中备份groupId=0
            database.insert(MyDatabaseHelper.TABLE_NOTES,
                    null, noteValues(userId, 0, title, content, editedTime));
        }
        //把插入得到的行号回填成 notesId，让同一笔记的多行记录共用一个 id
        ContentValues update = new ContentValues();
        update.put(MyDatabaseHelper.NOTES_ID, notesId);
        database.update(MyDatabaseHelper.TABLE_NOTES,
                update,
                MyDatabaseHelper.USER_ID + "=? and (" + MyDatabaseHelper.GROUP_ID + "=? or " + MyDatabaseHelper.GROUP_ID + "=?) and " + MyDatabaseHelper.EDIT_TIME + "=?",
                new String[]{String.valueOf(userId), String.valueOf(groupId), String.valueOf(0), String.valueOf(editedTime)});
        return notesId;
    }

    /**
     * 构造一条笔记记录的列值。每次返回新对象，避免多个方法共用同一个 ContentValues。
     */
    private ContentValues noteValues(long userId, long groupId, String title, String content, long editedTime) {
        ContentValues values = new ContentValues();
        values.put(MyDatabaseHelper.USER_ID, userId);
        values.put(MyDatabaseHelper.GROUP_ID, groupId);
        values.put(MyDatabaseHelper.TITLE, title);
        values.put(MyDatabaseHelper.CONTENT, content);
        values.put(MyDatabaseHelper.EDIT_TIME, editedTime);
        return values;
    }

    //将笔记添加到别的组
    public boolean addNotesToGroup(Notes notes) {
        //主键唯一
        //select count(*) as count from table_notes where userId=? and groupId=? and notesId=?
        boolean result = isExisted(MyDatabaseHelper.TABLE_NOTES,
                MyDatabaseHelper.USER_ID + "=? and " + MyDatabaseHelper.GROUP_ID + "=? and " + MyDatabaseHelper.NOTES_ID + "=?",
                new String[]{String.valueOf(notes.getUserId()), String.valueOf(notes.getGroupId()), String.valueOf(notes.getNotesId())});
        if (result) {//该分组下已有这条笔记
            return false;
        }
        ContentValues values = new ContentValues();
        values.put(MyDatabaseHelper.USER_ID, notes.getUserId());
        values.put(MyDatabaseHelper.GROUP_ID, notes.getGroupId());
        values.put(MyDatabaseHelper.NOTES_ID, notes.getNotesId());
        values.put(MyDatabaseHelper.TITLE, notes.getTitle());
        values.put(MyDatabaseHelper.CONTENT, notes.getContent());
        values.put(MyDatabaseHelper.EDIT_TIME, notes.getEditTime());
        //insert into table_notes values userId,groupId,notesId,title,content,editedTime
        database.insert(MyDatabaseHelper.TABLE_NOTES, null, values);
        return true;
    }


    //修改笔记（标题，内容，编辑时间）
    public void updateNotes(Notes notes) {
        ContentValues values = new ContentValues();
        values.put(MyDatabaseHelper.TITLE, notes.getTitle());
        values.put(MyDatabaseHelper.CONTENT, notes.getContent());
        values.put(MyDatabaseHelper.EDIT_TIME, notes.getEditTime());
        //update table_notes set (title,content,editedTime) where userId=userId and notesId=notesId
        database.update(MyDatabaseHelper.TABLE_NOTES,
                values,
                MyDatabaseHelper.USER_ID + "=? and " + MyDatabaseHelper.NOTES_ID + "=?",
                new String[]{String.valueOf(notes.getUserId()), String.valueOf(notes.getNotesId())});
    }


    //删除笔记
    public List<Notes> deleteNotes(long userId, long groupId, long notesId) {
        if (groupId == 0) {
            //全部笔记处：所有分组下的该笔记都要删除
            database.delete(MyDatabaseHelper.TABLE_NOTES,
                    MyDatabaseHelper.USER_ID + "=? and " + MyDatabaseHelper.NOTES_ID + "=?",
                    new String[]{String.valueOf(userId), String.valueOf(notesId)});
        } else {
            //删除该分组下的该笔记
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
            //select * from table_notes where userId=? and groupId=? order by editedTime desc
            //不加排序时 SQLite 返回顺序不受控，这里默认按编辑时间倒序（最新在前）
            cursor = database.query(MyDatabaseHelper.TABLE_NOTES,
                    null,
                    MyDatabaseHelper.USER_ID + "=? and " + MyDatabaseHelper.GROUP_ID + "=?",
                    new String[]{String.valueOf(userId), String.valueOf(groupId)},
                    null, null, MyDatabaseHelper.EDIT_TIME + " " + SORT_DESC, null);
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
        }
        ContentValues values = new ContentValues();
        values.put(MyDatabaseHelper.USER_ID, userId);
        values.put(MyDatabaseHelper.GROUP_NAME, groupName);
        //insert into table_groups (userId,groupName) values (userId,groupName)
        long groupId = database.insert(MyDatabaseHelper.TABLE_GROUPS, null, values);
        //返回完整的分组信息
        return new NoteGroup(userId, groupId, groupName);
    }


    //删除分组
    //同时删除该分组下的笔记
    public void deleteGroup(NoteGroup noteGroup) {
        //删除该分组
        database.delete(MyDatabaseHelper.TABLE_GROUPS,
                MyDatabaseHelper.USER_ID + "=? and " + MyDatabaseHelper.GROUP_ID + "=?",
                new String[]{String.valueOf(noteGroup.getUserId()),
                        String.valueOf(noteGroup.getGroupId())});
        //删除该分组下的笔记
        database.delete(MyDatabaseHelper.TABLE_NOTES,
                MyDatabaseHelper.USER_ID + "=? and " + MyDatabaseHelper.GROUP_ID + "=?",
                new String[]{String.valueOf(noteGroup.getUserId()),
                        String.valueOf(noteGroup.getGroupId())});
    }


    //修改分组名
    //返回值：true 表示重名（修改失败），false 表示修改成功
    public boolean updateGroupName(NoteGroup noteGroup) {
        //查重时要排除自己，否则名字没改也会被判成重名
        boolean existed = isExisted(MyDatabaseHelper.TABLE_GROUPS,
                MyDatabaseHelper.USER_ID + "=? and " + MyDatabaseHelper.GROUP_NAME + "=? and " + MyDatabaseHelper.GROUP_ID + "<>?",
                new String[]{String.valueOf(noteGroup.getUserId()), noteGroup.getGroupName(), String.valueOf(noteGroup.getGroupId())});
        if (existed) {
            return true;
        }
        ContentValues values = new ContentValues();
        values.put(MyDatabaseHelper.GROUP_NAME, noteGroup.getGroupName());
        //update table_groups set groupName=? where userId=? and groupId=?
        database.update(MyDatabaseHelper.TABLE_GROUPS,
                values,
                MyDatabaseHelper.USER_ID + "=? and " + MyDatabaseHelper.GROUP_ID + "=?",
                new String[]{String.valueOf(noteGroup.getUserId()),
                        String.valueOf(noteGroup.getGroupId())});
        return false;
    }

    //获取分组
    public List<NoteGroup> getAllGroups(long userId) {
        //select * from table_groups where userId=?
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
                noteGroup.setUserId(cursor.getLong(index));

                index = cursor.getColumnIndex(MyDatabaseHelper.GROUP_ID);
                noteGroup.setGroupId(cursor.getLong(index));

                index = cursor.getColumnIndex(MyDatabaseHelper.GROUP_NAME);
                noteGroup.setGroupName(cursor.getString(index));
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
            //select * from table_notes where userId=? and groupId=? order by editedTime asc/desc
            cursor = database.query(MyDatabaseHelper.TABLE_NOTES,
                    null,
                    MyDatabaseHelper.USER_ID + "=? and " + MyDatabaseHelper.GROUP_ID + "=?",
                    new String[]{String.valueOf(userId), String.valueOf(groupId)},
                    null, null, MyDatabaseHelper.EDIT_TIME + " " + sortWay, null);
            return gainNotesList(cursor);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public List<Notes> searchResult(long userId, String keyWord) {
        Cursor cursor = null;
        try {
            //select * from table_notes where userId=? and groupId=0 and title like ?
            cursor = database.query(MyDatabaseHelper.TABLE_NOTES,
                    null,
                    MyDatabaseHelper.USER_ID + "=? and " + MyDatabaseHelper.GROUP_ID + "=? and " + MyDatabaseHelper.TITLE + " like ?",
                    new String[]{String.valueOf(userId), String.valueOf(0), "%" + keyWord + "%"},
                    null, null, MyDatabaseHelper.EDIT_TIME + " " + SORT_DESC, null);
            return gainNotesList(cursor);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }


}
