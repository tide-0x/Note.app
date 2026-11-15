package com.iteam.examproject.entity;

import java.io.Serializable;

public class Notes implements Serializable {
    private static final long serialVersionUID = 1L;

    private long userId;
    private long groupId;
    private long notesId;
    private String title;
    private String content;
    private long editTime;

    public Notes() {
    }

    public Notes(long userId, long groupId,long notesId,String title, String content, long editTime) {
        this.userId = userId;
        this.groupId = groupId;
        this.notesId=notesId;
        this.title = title;
        this.content = content;
        this.editTime = editTime;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getGroupId() {
        return groupId;
    }

    public void setGroupId(long groupId) {
        this.groupId = groupId;
    }

    public long getNotesId() {
        return notesId;
    }

    public void setNotesId(long notesId) {
        this.notesId = notesId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getEditTime() {
        return editTime;
    }

    public void setEditTime(long editTime) {
        this.editTime = editTime;
    }
}
