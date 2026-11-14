package com.iteam.examproject.entity;

/**
 * 登录用户信息。
 * 注意：这里不再保存密码。密码只以加盐摘要的形式存在数据库里，见 PasswordUtil。
 */
public class User {
    private long userId;
    private String account;

    public User() {
    }

    public User(long userId, String account) {
        this.userId = userId;
        this.account = account;
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
}
