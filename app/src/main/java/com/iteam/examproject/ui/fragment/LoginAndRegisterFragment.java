package com.iteam.examproject.ui.fragment;

import static android.content.Context.MODE_PRIVATE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.iteam.examproject.R;
import com.iteam.examproject.databinding.FragmentLoginAndRegisterBinding;

import com.iteam.examproject.entity.NoteGroup;
import com.iteam.examproject.entity.User;
import com.iteam.examproject.ui.activity.ContentActivity;
import com.iteam.examproject.utils.ToastUtil;
import com.iteam.examproject.viewmodel.MyViewModel;
import com.iteam.examproject.repo.MyRepository;
import com.iteam.examproject.ui.activity.LoginAndRegisterActivity;

public class LoginAndRegisterFragment extends Fragment {
    private static final String TAG = "LoginAndRegisterFragment";
    private FragmentLoginAndRegisterBinding binding;
    private MyViewModel viewModel;
    private String account;
    private String password;
    private MyRepository repo;
    private SharedPreferences spf;
    private SharedPreferences.Editor edit;

    public LoginAndRegisterFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //初始化
        init();

        //从第二次打开都是上一次的账户，但如果是切换账户则不行，用SharedPreferences
        long userId = spf.getLong("userId", -1);
        boolean changedUser= Boolean.TRUE.equals(viewModel.getChangedUserLiveData().getValue());
        if(changedUser){
            binding.loginBtn.setText("切换账号");
        }
        if (userId != -1 && !changedUser) {//本地有数据且没有切换账号
            String account = spf.getString("account", "");
            String password = spf.getString("password", "");
            viewModel.setUserLiveData(new User(userId, account, password));
            viewModel.setNoteGroupLiveData(new NoteGroup(userId,0,"全部笔记"));
            viewModel.setChangedUserLiveData(false);
            toNoteFragment();
        }

        //登录
        //通过数据库获取该user的userId
        binding.loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                account = binding.accountEdit.getText().toString().trim();
                password = binding.passwordEdit.getText().toString().trim();
                if (account.isEmpty() || password.isEmpty()) {
                    //ToastUtil.showToast(requireContext(),"账号或密码不能为空");
                    Toast.makeText(getContext(), "账号或密码不能为空", Toast.LENGTH_SHORT).show();
                    return;
                }
                User user = repo.login(account, password);
                if (user == null) {//登录失败
                    //ToastUtil.showToast(requireContext(),"账号或密码错误");
                    Toast.makeText(requireActivity(), "账号或密码错误", Toast.LENGTH_SHORT).show();
                } else {
                    User liveDataUser = viewModel.getUserLiveData().getValue();
                    if (liveDataUser != null &&
                            changedUser &&
                            liveDataUser.getUserId() == user.getUserId()) {//判断是否切换账号 true
                        //ToastUtil.showToast(requireContext(),"该账号为当前登录账号");
                        Toast.makeText(requireContext(), "该账号为当前登录账号", Toast.LENGTH_SHORT).show();
                    } else {
                        //ToastUtil.showToast(requireContext(),"登录成功");
                        Toast.makeText(requireActivity(), "登录成功", Toast.LENGTH_SHORT).show();
                        saveData(user);
                        toNoteFragment();
                    }

                }
            }
        });

        //注册
        //先存进数据库再将user信息给viewmodel
        binding.registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                account = binding.accountEdit.getText().toString().trim();
                password = binding.passwordEdit.getText().toString().trim();
                if (account.isEmpty() || password.isEmpty()) {
                    //ToastUtil.showToast(requireContext(),"账号或密码不能为空");
                    Toast.makeText(requireActivity(), "账号或密码不能为空", Toast.LENGTH_SHORT).show();
                    return;
                }
                User user = repo.register(account, password);
                if (user == null) {
                    //ToastUtil.showToast(requireContext(),"该账号已存在");
                    Toast.makeText(requireContext(), "该账号已存在", Toast.LENGTH_SHORT).show();
                } else {
                    //ToastUtil.showToast(requireContext(),"注册成功");
                    Toast.makeText(requireContext(), "注册成功", Toast.LENGTH_SHORT).show();
                    saveData(user);
                    toNoteFragment();
                }
            }
        });


        return binding.getRoot();
    }

    private void saveData(User user) {
        viewModel.setUserLiveData(user);
        viewModel.setNoteGroupLiveData(new NoteGroup(user.getUserId(),0,"全部笔记"));
        edit.putLong("userId", user.getUserId());
        edit.putString("account", user.getAccount());
        edit.putString("password", user.getPassword());
        viewModel.setChangedUserLiveData(false);
        edit.apply();
    }

    private void init() {
        binding = FragmentLoginAndRegisterBinding.inflate(getLayoutInflater());
        viewModel = new ViewModelProvider(this).get(MyViewModel.class);
        repo = new MyRepository(requireContext());
        repo.open();
        spf = requireActivity().getSharedPreferences("currentUser", MODE_PRIVATE);
        edit = spf.edit();
    }

    private void toNoteFragment() {
        Intent intent = new Intent(requireActivity(), ContentActivity.class);
        startActivity(intent);
        requireActivity().finish();
    }
}