package com.iteam.examproject.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.navigation.NavigationBarView;
import com.iteam.examproject.R;
import com.iteam.examproject.databinding.ActivityContentBinding;
import com.iteam.examproject.ui.fragment.GroupFragment;
import com.iteam.examproject.ui.fragment.MineFragment;
import com.iteam.examproject.ui.fragment.NoteFragment;

public class ContentActivity extends AppCompatActivity {
    public static final String NOTE_FRAGMENT = "noteFragment";
    public static final String GROUP_FRAGMENT = "groupFragment";
    public static final String MINE_FRAGMENT = "mineFragment";
    private ActivityContentBinding binding;
    private String currentTag = NOTE_FRAGMENT;
    private static final String KEY_CURRENT_TAG = "key_current_tag";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityContentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (savedInstanceState != null) {
            currentTag = savedInstanceState.getString(KEY_CURRENT_TAG, NOTE_FRAGMENT);
        }

        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentByTag(NOTE_FRAGMENT);
        if (fragment == null) {
            fm
                    .beginTransaction()
                    .add(binding.contentFragmentContainerView.getId(), new NoteFragment(), NOTE_FRAGMENT)
                    .commit();
        }
        bottomNav();

        // 如果是重建，确保底部导航与 currentTag 同步（触发 listener 时会检测 currentTag）
        if (savedInstanceState != null) {
            binding.bottomNavigation.setSelectedItemId(tagToMenuId(currentTag));
        }
    }

    private int tagToMenuId(String tag) {
        if (NOTE_FRAGMENT.equals(tag)) return R.id.note;
        if (GROUP_FRAGMENT.equals(tag)) return R.id.group;
        return R.id.mine;
    }

    private void bottomNav() {
        //跳转到对应Fragment
        binding.bottomNavigation.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int viewId = item.getItemId();
                String targetTag = NOTE_FRAGMENT;
                FragmentManager fm = getSupportFragmentManager();
                if (viewId == R.id.note) {
                    targetTag = NOTE_FRAGMENT;
                } else if (viewId == R.id.group) {
                    targetTag = GROUP_FRAGMENT;
                } else if (viewId == R.id.mine) {
                    targetTag = MINE_FRAGMENT;
                }
                if (currentTag.equals(targetTag)) {
                    return true;//已是当前，不做任何操作
                }

                FragmentTransaction ft = fm.beginTransaction();
                Fragment current = fm.findFragmentByTag(currentTag);
                if (current != null && current.isAdded()) {
                    ft.hide(current);
                }

                Fragment target = fm.findFragmentByTag(targetTag);
                if (target == null) {
                    //只在不存在时创建并add(避免replace导致重复)
                    if (targetTag.equals(NOTE_FRAGMENT)) {
                        target = new NoteFragment();
                    } else if (targetTag.equals(GROUP_FRAGMENT)) {
                        target = new GroupFragment();
                    } else {
                        target = new MineFragment();
                    }
                    ft.add(binding.contentFragmentContainerView.getId(), target, targetTag);
                } else {
                    ft.show(target);
                }

                ft.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out);
                ft.commit();
                currentTag = targetTag;
                return true;
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString(KEY_CURRENT_TAG, currentTag);
        super.onSaveInstanceState(outState);
    }
//    Activity 的 onSaveInstanceState 会在系统需要保存临时状态以防 Activity 被销毁时被调用，常见场景：
//    配置变更（如屏幕旋转）前由系统调用以保存 UI 状态；
//    当用户把应用切到后台且系统可能在内存紧张时回收进程前；
//    从后台恢复时，保存的 Bundle 会在 onCreate 或 onRestoreInstanceState 中恢复。
//    注意：当用户主动退出（按返回键或调用 finish()）通常不会调用 onSaveInstanceState
}