package com.iteam.examproject.ui.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.iteam.examproject.R;
import com.iteam.examproject.databinding.ActivityLoginAndRegisterBinding;
import com.iteam.examproject.ui.fragment.LoginAndRegisterFragment;

public class LoginAndRegisterActivity extends AppCompatActivity {
    private ActivityLoginAndRegisterBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginAndRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.login_and_register_fragment_container, new LoginAndRegisterFragment())
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .commit();
    }
}