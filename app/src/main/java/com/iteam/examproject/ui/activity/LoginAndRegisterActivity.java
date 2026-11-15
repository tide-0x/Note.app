package com.iteam.examproject.ui.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.iteam.examproject.R;
import com.iteam.examproject.databinding.ActivityLoginAndRegisterBinding;
import com.iteam.examproject.ui.fragment.LoginAndRegisterFragment;

public class LoginAndRegisterActivity extends AppCompatActivity {
    /** 从「我的」页切账号进来时带上，登录页据此关掉自动登录 */
    public static final String EXTRA_SWITCH_ACCOUNT = "extra_switch_account";

    private ActivityLoginAndRegisterBinding binding;
    /** 本次是来登录，还是来切换账号（决定离开时要不要把自己关掉） */
    private boolean switchMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginAndRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        switchMode = getIntent().getBooleanExtra(EXTRA_SWITCH_ACCOUNT, false);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.login_and_register_fragment_container, new LoginAndRegisterFragment())
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .commit();
    }

    /**
     * 切换账号只是"在内容界面上面临时叠一层"，用户离开（按 Home、手势回桌面、从最近任务切走）
     * 就等同于放弃切换，直接把自己关掉：否则任务栈里会留着一个已经离开过的切换页，
     * 下次点图标进来看到的还是它。
     *
     * <p>用 {@link #onUserLeaveHint()} 而不是 {@code onStop()}：它只在「用户主动离开」时回调，
     * 旋转屏幕不会触发（否则一转屏页面就被关掉），灭屏、下拉通知栏也不会触发。
     */
    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (switchMode && !isFinishing()) {
            finish();
        }
    }
}