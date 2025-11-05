package com.iteam.examproject.ui.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationBarView;
import com.iteam.examproject.R;
import com.iteam.examproject.databinding.ActivityContentBinding;
import com.iteam.examproject.ui.fragment.GroupFragment;
import com.iteam.examproject.ui.fragment.MineFragment;
import com.iteam.examproject.ui.fragment.NoteFragment;

public class ContentActivity extends AppCompatActivity {
    private ActivityContentBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityContentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getSupportFragmentManager()
                .beginTransaction()
                .replace(binding.contentFragmentContainerView.getId(), new NoteFragment())
                .commit();
        bottomNav();
    }

    private void bottomNav() {
        //跳转到对应Fragment
        binding.bottomNavigation.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int viewId = item.getItemId();
                Fragment fragment = null;
                if (viewId == R.id.note) {
                    fragment = new NoteFragment();
                } else if (viewId == R.id.group) {
                    fragment = new GroupFragment();
                } else if (viewId == R.id.mine) {
                    fragment = new MineFragment();
                }
                if (fragment != null) {
                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.content_fragment_container_view, fragment)
                            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                            .commit();
                }
                return true;
            }
        });
    }
}