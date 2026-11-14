package com.iteam.examproject.ui.fragment;

import static android.content.Context.MODE_PRIVATE;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.iteam.examproject.R;
import com.iteam.examproject.databinding.FragmentMineBinding;
import com.iteam.examproject.entity.User;
import com.iteam.examproject.repo.MyRepository;
import com.iteam.examproject.ui.activity.LoginAndRegisterActivity;
import com.iteam.examproject.viewmodel.MyViewModel;

import java.util.Objects;


public class MineFragment extends Fragment {
    private FragmentMineBinding binding;
    private MyViewModel viewModel;
    private MyRepository repo;
    private SharedPreferences spf;
    private SharedPreferences.Editor edit;

    public MineFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //初始化
        init();

        viewModel.getUserLiveData().observe(getViewLifecycleOwner(), new Observer<User>() {
            @Override
            public void onChanged(User user) {
                binding.userNameTv.setText(user.getAccount());
            }
        });

        //更改账号密码
        binding.updateAccountPasswordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                EditText editText = new EditText(requireContext());
                builder.setView(editText);
                builder
                        .setTitle(R.string.update_password_title)
                        .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String changedPassword = editText.getText().toString();
                                if(changedPassword.isEmpty()){
                                    Toast.makeText(requireContext(), getString(R.string.password_empty), Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                repo.updatePassword(Objects.requireNonNull(viewModel.getUserLiveData().getValue()).getUserId(),
                                        changedPassword);
                                Toast.makeText(requireContext(), getString(R.string.password_updated), Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            }
                        })
                        .show();
            }
        });

        //切换账号
        binding.exchangeAccountBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewModel.setChangedUserLiveData(true);
                Intent intent = new Intent(new Intent(requireContext(), LoginAndRegisterActivity.class));
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                requireActivity().finish();
            }
        });

        //注销账号
        binding.cancelAccountBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                builder.setTitle(R.string.warning)
                        .setMessage(R.string.cancel_account_message)
                        .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                repo.cancelUser(Objects.requireNonNull(viewModel.getUserLiveData().getValue()).getUserId());
                                viewModel.setChangedUserLiveData(false);
                                updateSPF();
                                edit.apply();
                                Intent intent = new Intent(requireContext(), LoginAndRegisterActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                requireActivity().finish();
                                dialog.dismiss();
                            }
                        })
                        .show();
            }
        });

        //退出账号
        binding.exitAccountBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewModel.setChangedUserLiveData(false);
                updateSPF();
                edit.apply();
                Intent intent = new Intent(requireContext(), LoginAndRegisterActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                //FLAG_ACTIVITY_NEW_TASK：在新的任务（task）中启动目标 Activity（或复用已有任务）。
                //FLAG_ACTIVITY_CLEAR_TASK：如果要启动的任务已存在，先清空该任务中的所有 Activity。
                //两者合用的效果：清空当前任务的返回栈，并把要启动的 Activity 作为新的根 Activity 启动。常用于注销/切换账号场景，防止用户按返回键回到之前的界面。
                //注意：FLAG_ACTIVITY_CLEAR_TASK 通常需要和 FLAG_ACTIVITY_NEW_TASK 一起使用，否则会被忽略
                startActivity(intent);
                requireActivity().finish();
            }
        });


        return binding.getRoot();
    }

    private void updateSPF() {
        edit.putLong("userId", -1);
        edit.putString("account", "");
        //退出/注销时把旧版本残留的明文密码一并清掉
        edit.remove("password");
    }

    private void init() {
        binding = FragmentMineBinding.inflate(getLayoutInflater());
        viewModel = new ViewModelProvider(this).get(MyViewModel.class);
        //共享仓储：全进程只有一条数据库连接，用完不需要关闭
        repo = MyRepository.getInstance(requireContext());
        spf = requireActivity().getSharedPreferences("currentUser", MODE_PRIVATE);
        edit = spf.edit();
    }

}