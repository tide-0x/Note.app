package com.iteam.examproject.feature.mine;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.iteam.examproject.R;
import com.iteam.examproject.core.ToastUtil;
import com.iteam.examproject.databinding.FragmentMineBinding;
import com.iteam.examproject.feature.auth.LoginAndRegisterActivity;
import com.iteam.examproject.feature.main.MainViewModel;

public class MineFragment extends Fragment {
    private FragmentMineBinding binding;
    private MainViewModel viewModel;
    /** 防止连点「切换账号」叠出多个登录页：本页重新回到前台时复位 */
    private boolean switchLaunched;

    public MineFragment() {
    }

    @Override
    public void onResume() {
        super.onResume();
        switchLaunched = false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //初始化
        init();

        viewModel.getUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                binding.userNameTv.setText(user.getAccount());
            }
        });

        //VM 里的操作结果（改密码成功、操作失败）统一在这里弹
        viewModel.getMessages().observe(getViewLifecycleOwner(), event -> {
            Integer resId = event.getContentIfNotHandled();
            if (resId != null) {
                ToastUtil.showToast(requireContext(), resId);
            }
        });
        //会话失效（注销成功）由 ContentActivity 统一处理跳转

        //更改账号密码
        binding.updateAccountPasswordBtn.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            EditText editText = new EditText(requireContext());
            builder.setView(editText);
            builder
                    .setTitle(R.string.update_password_title)
                    .setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.dismiss())
                    .setPositiveButton(getString(R.string.confirm), (dialog, which) -> {
                        String changedPassword = editText.getText().toString();
                        if (changedPassword.isEmpty()) {
                            ToastUtil.showToast(requireContext(), R.string.password_empty);
                            return;
                        }
                        dialog.dismiss();
                        //改密码同样要算 10000 次摘要，VM 内部走后台线程
                        viewModel.updatePassword(changedPassword);
                    })
                    .show();
        });

        //切换账号：登录页叠在内容界面上面，不清任务栈、也不结束本页。
        //这样手势返回 / 返回箭头就是"反悔"，回到「我的」，登录态全程没被动过；
        //清栈的写法会让返回栈里只剩登录页，返回只能退到桌面，从最近任务回来还是登录页。
        binding.exchangeAccountBtn.setOnClickListener(v -> {
            if (switchLaunched) {
                return;
            }
            switchLaunched = true;
            Intent intent = new Intent(requireContext(), LoginAndRegisterActivity.class);
            intent.putExtra(LoginAndRegisterActivity.EXTRA_SWITCH_ACCOUNT, true);
            startActivity(intent);
        });

        //注销账号
        binding.cancelAccountBtn.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle(R.string.warning)
                    .setMessage(R.string.cancel_account_message)
                    .setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.dismiss())
                    .setPositiveButton(getString(R.string.confirm), (dialog, which) -> {
                        dialog.dismiss();
                        //删库成功后 VM 会清本地登录态并发 sessionExpired，跳转由 ContentActivity 统一处理；
                        //注销失败就别清本地登录态，否则账号还留着却登不回来
                        viewModel.cancelAccount();
                    })
                    .show();
        });

        //退出账号
        binding.exitAccountBtn.setOnClickListener(v -> viewModel.logout());

        return binding.getRoot();
    }

    private void init() {
        binding = FragmentMineBinding.inflate(getLayoutInflater());
        //登录用户、账号操作都在 ContentActivity 级的 MainViewModel 里
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
    }

}
