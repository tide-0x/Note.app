package com.iteam.examproject.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.iteam.examproject.entity.User;

public class MyViewModel extends ViewModel {
    private static final MutableLiveData<User> userLiveData =new MutableLiveData<>(new User(-1,"",""));

    public void setUserLiveData(User user) {
        userLiveData.setValue(user);
    }

    public LiveData<User> getUserLiveData() {
        return userLiveData;
    }
}
