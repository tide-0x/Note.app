package com.iteam.examproject.ui.fragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

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
            binding.loginBtn.setText(R.string.exchange_account);
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
                    Toast.makeText(requireContext(), getString(R.string.login_success), Toast.LENGTH_SHORT).show();
                    navigateToContent(outcome.user);
                    break;
                case REGISTERED:
                    Toast.makeText(requireContext(), getString(R.string.register_success), Toast.LENGTH_SHORT).show();
                    navigateToContent(outcome.user);
                    break;
                case WRONG_PASSWORD:
                    //成功但没查到人 = 账号或密码错误（业务上的空结果，不是出错）
                    Toast.makeText(requireContext(), getString(R.string.account_or_password_error), Toast.LENGTH_SHORT).show();
                    break;
                case ALREADY_CURRENT:
                    Toast.makeText(requireContext(), getString(R.string.account_is_current), Toast.LENGTH_SHORT).show();
                    break;
                case ACCOUNT_EXISTS:
                    Toast.makeText(requireContext(), getString(R.string.account_exists), Toast.LENGTH_SHORT).show();
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
                Toast.makeText(requireContext(), getString(R.string.account_or_password_empty), Toast.LENGTH_SHORT).show();
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
                Toast.makeText(requireContext(), getString(R.string.account_or_password_empty), Toast.LENGTH_SHORT).show();
                return;
            }
            //注册同样要算摘要，VM 内部走 IO 线程
            viewModel.register(account, password);
        });


        return binding.getRoot();
    }

    /** 登录成功：userId/account 通过 Intent 传给 ContentActivity，不再依赖全局单例 */
    private void navigateToContent(User user) {
        Intent intent = new Intent(requireActivity(), ContentActivity.class);
        intent.putExtra(ContentActivity.EXTRA_USER_ID, user.getUserId());
        intent.putExtra(ContentActivity.EXTRA_ACCOUNT, user.getAccount());
        startActivity(intent);
        requireActivity().finish();
    }

    private void init() {
        binding = FragmentLoginAndRegisterBinding.inflate(getLayoutInflater());
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);
    }
}
