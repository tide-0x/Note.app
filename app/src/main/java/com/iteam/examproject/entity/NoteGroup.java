package com.iteam.examproject.entity;

public class NoteGroup {
    long userId;
    long groupId;
    String groupName;

    public NoteGroup() {
    }

    public NoteGroup(long userId, long groupId, String groupName) {
        this.userId = userId;
        this.groupId = groupId;
        this.groupName = groupName;
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

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
}
