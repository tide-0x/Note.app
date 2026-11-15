package com.iteam.examproject.feature.auth;

import android.app.Application;
import android.content.Context;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.iteam.examproject.core.Event;
import com.iteam.examproject.data.MyRepository;
import com.iteam.examproject.data.model.User;

/**
 * 登录/注册页的 ViewModel：登录态的判定（自动登录、切换账号）、登录注册流程、
 * 本地登录信息（SharedPreferences）的读写都在这里。
 *
 * <p>登录结果用 {@link Event} 通知 View；View 拿到成功事件后带着 userId/account
 * 跳 ContentActivity（Intent 传参，取代原来跨 Activity 共享单例的写法）。
 */
public class AuthViewModel extends AndroidViewModel {

    /** 登录/注册流程的结果，View 按它提示和跳转 */
    public static final class AuthOutcome {
        public enum Kind { AUTO_LOGIN, LOGGED_IN, REGISTERED, WRONG_PASSWORD, ALREADY_CURRENT, ACCOUNT_EXISTS, FAILED }

        public final Kind kind;
        public final User user;

        AuthOutcome(Kind kind, User user) {
            this.kind = kind;
            this.user = user;
        }
    }

    private static final String SP_NAME = "currentUser";

    private final MutableLiveData<Event<AuthOutcome>> outcomes = new MutableLiveData<>();

    private MyRepository repo;
    private boolean switchMode = false;

    public AuthViewModel(Application application) {
        super(application);
        repo = MyRepository.getInstance(application);
    }

    /** 由 View 把 Intent 里的「切换账号」标记递进来 */
    public void setSwitchMode(boolean switchMode) {
        this.switchMode = switchMode;
    }

    /** 本地记住的账号：切换账号页用它回填输入框，用户只需要重输密码 */
    public String currentAccount() {
        return spf().getString("account", "");
    }

    /**
     * 自动登录：本地记住了 userId 且不是切换账号场景，直接当登录成功处理。
     * 密码不再落盘，所以记住的只有 userId 和 account。
     */
    public void autoLogin() {
        if (switchMode) {
            return;
        }
        long userId = spf().getLong("userId", -1);
        if (userId == -1) {
            return;
        }
        User user = new User(userId, spf().getString("account", ""));
        outcomes.setValue(new Event<>(new AuthOutcome(AuthOutcome.Kind.AUTO_LOGIN, user)));
    }

    public LiveData<Event<AuthOutcome>> getOutcomes() {
        return outcomes;
    }

    /** 登录：密码校验要做 10000 次 SHA-256，VM 内部走 IO 线程 */
    public void login(String account, String password) {
        repo.runAsync(() -> repo.login(account, password), result -> {
            if (!result.isSuccess()) {
                emit(AuthOutcome.Kind.FAILED, null);
                return;
            }
            // 成功但没查到人 = 账号或密码错误（业务上的空结果，不是出错）
            User user = result.getData();
            if (user == null) {
                emit(AuthOutcome.Kind.WRONG_PASSWORD, null);
                return;
            }
            if (switchMode && spf().getLong("userId", -1) == user.getUserId()) {
                // 切换账号但输的还是当前账号
                emit(AuthOutcome.Kind.ALREADY_CURRENT, null);
                return;
            }
            saveSession(user);
            emit(AuthOutcome.Kind.LOGGED_IN, user);
        });
    }

    /** 注册：成功即登录 */
    public void register(String account, String password) {
        repo.runAsync(() -> repo.register(account, password), result -> {
            if (!result.isSuccess()) {
                emit(AuthOutcome.Kind.FAILED, null);
                return;
            }
            User user = result.getData();
            if (user == null) {
                emit(AuthOutcome.Kind.ACCOUNT_EXISTS, null);
                return;
            }
            saveSession(user);
            emit(AuthOutcome.Kind.REGISTERED, user);
        });
    }

    private void saveSession(User user) {
        spf().edit()
                .putLong("userId", user.getUserId())
                .putString("account", user.getAccount())
                //清掉旧版本曾经写进 SharedPreferences 的明文密码
                .remove("password")
                .apply();
    }

    private android.content.SharedPreferences spf() {
        return getApplication().getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
    }

    private void emit(AuthOutcome.Kind kind, User user) {
        outcomes.setValue(new Event<>(new AuthOutcome(kind, user)));
    }
}
