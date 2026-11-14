package com.iteam.examproject.repo;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.iteam.examproject.entity.NoteGroup;
import com.iteam.examproject.entity.Notes;
import com.iteam.examproject.entity.User;

import java.util.ArrayList;
import java.util.List;

/**
 * 单例数据仓库，持有应用级的 LiveData，供不同 Activity/Fragment 订阅。
 */
public class SharedDataRepository {
    private static final SharedDataRepository INSTANCE = new SharedDataRepository();

    private final MutableLiveData<User> userLiveData = new MutableLiveData<>(new User(-1, ""));
    //登录前的占位分组，名称为空，界面显示时会兜底成「全部笔记」
    private final MutableLiveData<NoteGroup> noteGroupLiveData = new MutableLiveData<>(new NoteGroup(-1, 0, ""));
    private final MutableLiveData<List<Notes>> notesListLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Notes> notesLiveData = new MutableLiveData<>(new Notes(-1, -1, -1, "", "", -1));
    private final MutableLiveData<Boolean> changedUserLiveData=new MutableLiveData<>(false);

    private SharedDataRepository() {}

    public static SharedDataRepository getInstance() {
        return INSTANCE;
    }

    // User
    public LiveData<User> getUserLiveData() {
        return userLiveData;
    }
    public void setUser(User user) {
        userLiveData.setValue(user);
    }

    // NoteGroup
    public LiveData<NoteGroup> getNoteGroupLiveData() {
        return noteGroupLiveData;
    }
    public void setNoteGroup(NoteGroup noteGroup) {
        noteGroupLiveData.setValue(noteGroup);
    }

    // Notes list
    public LiveData<List<Notes>> getNotesListLiveData() {
        return notesListLiveData;
    }
    public void setNotesList(List<Notes> notes) {
        notesListLiveData.setValue(notes);
    }

    // Single Notes
    public LiveData<Notes> getNotesLiveData() {
        return notesLiveData;
    }
    public void setNotes(Notes notes) {
        notesLiveData.setValue(notes);
    }

    // ChangedUser
    public void setChangedUser(boolean changedUser){
        changedUserLiveData.setValue(changedUser);
    }
    public LiveData<Boolean> getChangedUserLiveData(){
        return changedUserLiveData;
    }
}

