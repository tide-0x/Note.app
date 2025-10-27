package com.iteam.examproject.ui.fragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.iteam.examproject.R;
import com.iteam.examproject.databinding.ActivityMainBinding;
import com.iteam.examproject.databinding.FragmentLoginAndRegisterBinding;

import com.iteam.examproject.model.MyViewModel;
import com.iteam.examproject.ui.activity.MainActivity;

public class LoginAndRegisterFragment extends Fragment {
    private FragmentLoginAndRegisterBinding binding;
    private MyViewModel myViewModel;
    private String account;
    private String password;

    public LoginAndRegisterFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //隐藏底部导航栏
        MainActivity.activityMainBinding.bottomNavigation.setVisibility(View.GONE);

        binding = FragmentLoginAndRegisterBinding.inflate(getLayoutInflater());
        myViewModel = new ViewModelProvider(this).get(MyViewModel.class);
        //登录
        //通过数据库获取该user的userId
        binding.loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                account = binding.accountEdit.getText().toString().trim();
                password = binding.passwordEdit.getText().toString().trim();
                /*if (account.isEmpty() || password.isEmpty()) {
                    Toast.makeText(getContext(), "账号或密码不能为空", Toast.LENGTH_SHORT).show();
                    return;
                }*/

                toNoteFragment();

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
                    Toast.makeText(getContext(), "账号或密码不能为空", Toast.LENGTH_SHORT).show();
                    return;
                }

                toNoteFragment();

            }
        });


        return binding.getRoot();
    }

    private void toNoteFragment() {
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new NoteFragment())
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .commit();
    }
}