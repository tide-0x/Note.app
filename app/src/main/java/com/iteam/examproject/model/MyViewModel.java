package com.iteam.examproject.model;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.iteam.examproject.entity.User;

public class MyViewModel extends ViewModel {
    private MutableLiveData<User> userLiveData;
    public void setUserLiveData(User user){
        if(userLiveData==null){
            userLiveData=new MutableLiveData<>();
        }
        userLiveData.setValue(user);
    }

    public LiveData<User> getUserLiveData(){
        if(userLiveData==null){
            userLiveData=new MutableLiveData<>();
        }
        return userLiveData;
    }
}
