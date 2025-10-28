package com.iteam.examproject.ui.fragment;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
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
import com.iteam.examproject.viewmodel.MyViewModel;
import com.iteam.examproject.repo.MyRepository;
import com.iteam.examproject.ui.activity.MainActivity;

public class LoginAndRegisterFragment extends Fragment {
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

        //从第二次打开都是上一次的账户，但如果是切换账户则不行SharedPreferences
        long userId = spf.getLong("userId", -1);
        boolean changedUser = spf.getBoolean("changedUser", false);
        if (userId != -1 && !changedUser) {//之间登录过且没有切换账号
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
                    Toast.makeText(getContext(), "账号或密码不能为空", Toast.LENGTH_SHORT).show();
                    return;
                }
                User user = repo.login(account, password);
                if (user == null) {//登录失败
                    Toast.makeText(requireActivity(), "账号或密码错误", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireActivity(), "登录成功", Toast.LENGTH_SHORT).show();
                    viewModel.setUserLiveData(user);
                    edit.putLong("userId", user.getUserId());
                    edit.putString("account", user.getAccount());
                    edit.putString("password", user.getPassword());
                    edit.putBoolean("changedUser", false);
                    edit.apply();
                    toNoteFragment();
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
                    Toast.makeText(requireActivity(), "账号或密码不能为空", Toast.LENGTH_SHORT).show();
                    return;
                }
                User user = repo.register(account, password);
                if (user == null) {
                    Toast.makeText(requireContext(), "该账号已存在", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "注册成功", Toast.LENGTH_SHORT).show();
                    viewModel.setUserLiveData(user);
                    edit.putLong("userId", user.getUserId());
                    edit.putString("account", user.getAccount());
                    edit.putString("password", user.getPassword());
                    edit.putBoolean("changedUser", false);
                    edit.apply();
                    toNoteFragment();
                }
            }
        });


        return binding.getRoot();
    }

    private void init() {
        MainActivity.activityMainBinding.bottomNavigation.setVisibility(View.GONE);
        binding = FragmentLoginAndRegisterBinding.inflate(getLayoutInflater());
        viewModel = new ViewModelProvider(this).get(MyViewModel.class);
        repo = new MyRepository(requireContext());
        repo.open();
        spf = requireActivity().getSharedPreferences("currentUser", MODE_PRIVATE);
        edit = spf.edit();
    }

    private void toNoteFragment() {
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new NoteFragment())
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .commit();
    }
}