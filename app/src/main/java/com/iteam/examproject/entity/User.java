package com.iteam.examproject.entity;

public class User {
    private long userId;
    private String account;
    private String password;
    //用来判断是否切换账户
    //private boolean currentAccount=true;

    public User(){}

    public User(long userId, String account, String password) {
        this.userId = userId;
        this.account = account;
        this.password = password;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /*public boolean isCurrentAccount() {
        return currentAccount;
    }

    public void setCurrentAccount(boolean currentAccount) {
        this.currentAccount = currentAccount;
    }*/
}
