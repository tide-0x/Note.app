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

import com.iteam.examproject.databinding.FragmentMineBinding;
import com.iteam.examproject.entity.User;
import com.iteam.examproject.repo.MyRepository;
import com.iteam.examproject.ui.activity.LoginAndRegisterActivity;
import com.iteam.examproject.utils.ToastUtil;
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
                editText.setHint("请输入新密码");
                builder.setView(editText);
                builder
                        .setTitle("请输入新的密码")
                        .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setPositiveButton("确认", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String changedPassword = editText.getText().toString();
                                repo.updatePassword(Objects.requireNonNull(viewModel.getUserLiveData().getValue()).getUserId(),
                                        changedPassword);
                                User user = viewModel.getUserLiveData().getValue();
                                user.setPassword(changedPassword);
                                viewModel.setUserLiveData(user);
                                //ToastUtil.showToast(requireContext(),"密码修改成功");
                                Toast.makeText(requireContext(), "密码修改成功", Toast.LENGTH_SHORT).show();
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
                edit.putBoolean("changedUser", true);
                edit.apply();
                startActivity(new Intent(requireContext(), LoginAndRegisterActivity.class));
            }
        });

        //注销账号
        binding.cancelAccountBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                builder.setTitle("警告")
                        .setMessage("是否要注销当前账号")
                        .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setPositiveButton("确认", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                repo.cancelUser(Objects.requireNonNull(viewModel.getUserLiveData().getValue()).getUserId());
                                updateViewModel();
                                edit.apply();
                                Intent intent = new Intent(requireContext(), LoginAndRegisterActivity.class);
                                startActivity(intent);
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
                updateViewModel();
                edit.apply();
                Intent intent = new Intent(requireContext(), LoginAndRegisterActivity.class);
                startActivity(intent);
            }
        });


        return binding.getRoot();
    }

    private void updateViewModel() {
        edit.putLong("userId", -1);
        edit.putString("account", "");
        edit.putString("password", "");
        edit.putBoolean("changedUser", false);
    }

    private void init() {
        binding = FragmentMineBinding.inflate(getLayoutInflater());
        viewModel = new ViewModelProvider(this).get(MyViewModel.class);
        repo = new MyRepository(requireContext());
        repo.open();
        spf = requireActivity().getSharedPreferences("currentUser", MODE_PRIVATE);
        edit = spf.edit();
    }

}