package com.iteam.examproject.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.iteam.examproject.entity.NoteGroup;
import com.iteam.examproject.entity.Notes;
import com.iteam.examproject.entity.User;
import com.iteam.examproject.repo.SharedDataRepository;

import java.util.List;

public class MyViewModel extends ViewModel {
    private final SharedDataRepository repository=SharedDataRepository.getInstance();

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

    public void setNotesLiveData(Notes notes){
        repository.setNotes(notes);
    }

    public LiveData<Notes> getNotesLiveData(){
        return repository.getNotesLiveData();
    }

    public void setChangedUserLiveData(boolean changedUser){
        repository.setChangedUser(changedUser);
    }

    public LiveData<Boolean> getChangedUserLiveData(){
        return repository.getChangedUserLiveData();
    }

}
