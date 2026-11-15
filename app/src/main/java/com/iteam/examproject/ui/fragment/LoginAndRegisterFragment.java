package com.iteam.examproject.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import com.iteam.examproject.R;
import com.iteam.examproject.databinding.FragmentLoginAndRegisterBinding;
import com.iteam.examproject.entity.User;
import com.iteam.examproject.ui.activity.ContentActivity;
import com.iteam.examproject.ui.activity.LoginAndRegisterActivity;
import com.iteam.examproject.utils.ToastUtil;
import com.iteam.examproject.viewmodel.AuthViewModel;
import com.iteam.examproject.viewmodel.AuthViewModel.AuthOutcome;

public class LoginAndRegisterFragment extends Fragment {
    private static final String TAG = "LoginAndRegisterFragment";
    private FragmentLoginAndRegisterBinding binding;
    private AuthViewModel viewModel;

    public LoginAndRegisterFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //初始化
        init();

        //切换账号场景下关掉自动登录，按钮文字换成「切换账号」
        boolean switchMode = requireActivity().getIntent()
                .getBooleanExtra(LoginAndRegisterActivity.EXTRA_SWITCH_ACCOUNT, false);
        viewModel.setSwitchMode(switchMode);
        if (switchMode) {
            enterSwitchMode();
        }

        //登录/注册/自动登录的结果统一在这里收尾
        viewModel.getOutcomes().observe(getViewLifecycleOwner(), event -> {
            AuthOutcome outcome = event.getContentIfNotHandled();
            if (outcome == null) {
                return;
            }
            switch (outcome.kind) {
                case AUTO_LOGIN:
                    //本地记住过登录态，直接进主界面
                    navigateToContent(outcome.user);
                    break;
                case LOGGED_IN:
                    ToastUtil.showToast(requireContext(), R.string.login_success);
                    navigateToContent(outcome.user);
                    break;
                case REGISTERED:
                    ToastUtil.showToast(requireContext(), R.string.register_success);
                    navigateToContent(outcome.user);
                    break;
                case WRONG_PASSWORD:
                    //成功但没查到人 = 账号或密码错误（业务上的空结果，不是出错）
                    ToastUtil.showToast(requireContext(), R.string.account_or_password_error);
                    break;
                case ALREADY_CURRENT:
                    ToastUtil.showToast(requireContext(), R.string.account_is_current);
                    break;
                case ACCOUNT_EXISTS:
                    ToastUtil.showToast(requireContext(), R.string.account_exists);
                    break;
                case FAILED:
                default:
                    ToastUtil.showOperationFailed(requireContext());
                    break;
            }
        });

        //本地有数据且不是切换账号场景，直接进主界面（密码不再落盘，记住的只有 userId 和 account）
        viewModel.autoLogin();

        //登录
        binding.loginBtn.setOnClickListener(v -> {
            String account = binding.accountEdit.getText().toString().trim();
            String password = binding.passwordEdit.getText().toString().trim();
            if (account.isEmpty() || password.isEmpty()) {
                ToastUtil.showToast(requireContext(), R.string.account_or_password_empty);
                return;
            }
            //校验密码要做 10000 次 SHA-256，VM 内部走 IO 线程
            viewModel.login(account, password);
        });

        //注册：先存进数据库，成功即登录
        binding.registerBtn.setOnClickListener(v -> {
            String account = binding.accountEdit.getText().toString().trim();
            String password = binding.passwordEdit.getText().toString().trim();
            if (account.isEmpty() || password.isEmpty()) {
                ToastUtil.showToast(requireContext(), R.string.account_or_password_empty);
                return;
            }
            //注册同样要算摘要，VM 内部走 IO 线程
            viewModel.register(account, password);
        });


        return binding.getRoot();
    }

    /**
     * 切换账号模式的界面差异，全部是「同一张表单的另一种状态」，不是另一个页面：
     * <ul>
     *   <li>顶部露出返回箭头：点它、手势返回、系统返回键效果一致，都是关掉本页回到「我的」，
     *       此时登录态一点没动（切换模式不写 SP），等于"反悔"；</li>
     *   <li>隐藏注册按钮：在切换页点注册会新建一个账号并登录，和"切到另一个已有账号"是两件事；</li>
     *   <li>回填当前账号、焦点落在密码框：用户只需要重输密码。</li>
     * </ul>
     */
    private void enterSwitchMode() {
        binding.loginBtn.setText(R.string.exchange_account);

        binding.loginToolbar.setVisibility(View.VISIBLE);
        binding.loginToolbar.setTitle(R.string.exchange_account);
        binding.loginToolbar.setNavigationContentDescription(R.string.back);
        binding.loginToolbar.setNavigationOnClickListener(v -> requireActivity().finish());

        binding.registerBtn.setVisibility(View.GONE);

        binding.accountEdit.setText(viewModel.currentAccount());
        //等这一帧铺完再要焦点、弹键盘：此时 View 才挂到窗口上，否则焦点会被丢掉
        binding.passwordEdit.post(() -> {
            if (!isAdded()) {
                return;
            }
            binding.passwordEdit.requestFocus();
            InputMethodManager imm = (InputMethodManager) requireContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(binding.passwordEdit, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    /** 登录成功：userId/account 通过 Intent 传给 ContentActivity，不再依赖全局单例 */
    private void navigateToContent(User user) {
        Intent intent = new Intent(requireActivity(), ContentActivity.class);
        intent.putExtra(ContentActivity.EXTRA_USER_ID, user.getUserId());
        intent.putExtra(ContentActivity.EXTRA_ACCOUNT, user.getAccount());
        //切换账号时栈里还压着旧的 ContentActivity。CLEAR_TOP 会把旧实例结束掉、
        //用新的登录态重建一份，避免用户按返回键又回到上一个账号的界面。
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        requireActivity().finish();
    }

    private void init() {
        binding = FragmentLoginAndRegisterBinding.inflate(getLayoutInflater());
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);
    }
}
