package com.iteam.examproject.ui.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.iteam.examproject.R;
import com.iteam.examproject.databinding.ActivityMainBinding;
import com.iteam.examproject.ui.fragment.LoginAndRegisterFragment;

public class MainActivity extends AppCompatActivity {
    public static ActivityMainBinding activityMainBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityMainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(activityMainBinding.getRoot());

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new LoginAndRegisterFragment())
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .commit();
    }
}