package com.iteam.examproject.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.iteam.examproject.entity.NoteGroup;
import com.iteam.examproject.entity.Notes;
import com.iteam.examproject.entity.User;
import com.iteam.examproject.repo.SharedDataRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MyViewModel extends ViewModel {
    private final SharedDataRepository repository=SharedDataRepository.getInstance();
    /*private static final MutableLiveData<User> userLiveData = new MutableLiveData<>(new User(-1, "", ""));
    private static final MutableLiveData<NoteGroup> noteGroupLiveData = new MutableLiveData<>(new NoteGroup(Objects.requireNonNull(userLiveData.getValue()).getUserId(), 0, "全部笔记"));
    private static final MutableLiveData<List<Notes>> notesListLiveData=new MutableLiveData<>(new ArrayList<>());
    private static final MutableLiveData<Notes> notesLiveData=new MutableLiveData<>(new Notes(-1,-1,-1,"","",-1));*/
    //private static final MutableLiveData<List<NoteGroup>> noteGroupListLiveData=new MutableLiveData<>(new ArrayList<>());

    public void setUserLiveData(User user) {
        repository.setUser(user);
    }

    public LiveData<User> getUserLiveData() {
        return repository.getUserLiveData();
    }

    public void setNoteGroupLiveData(NoteGroup noteGroup) {
        repository.setNoteGroup(noteGroup);
    }

    public LiveData<NoteGroup> getNoteGroupLiveData() {
        return repository.getNoteGroupLiveData();
    }

    public void setNotesListLiveData(List<Notes> notesList){
        repository.setNotesList(notesList);
    }

    public LiveData<List<Notes>> getNotesListLiveData(){
        return repository.getNotesListLiveData();
    }

    /*public void setNoteGroupListLiveData(List<NoteGroup> noteGroupList){
        noteGroupListLiveData.setValue(noteGroupList);
    }*/

    /*public LiveData<List<NoteGroup>> getNoteGroupListLiveData(){
        return noteGroupListLiveData;
    }*/

    public void setNotesLiveData(Notes notes){
        repository.setNotes(notes);
    }

    public LiveData<Notes> getNotesLiveData(){
        return repository.getNotesLiveData();
    }

}
