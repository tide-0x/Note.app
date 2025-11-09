package com.iteam.examproject.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.iteam.examproject.R;
import com.iteam.examproject.databinding.ActivityEditNoteBinding;
import com.iteam.examproject.ui.fragment.EditNoteFragment;

public class EditNoteActivity extends AppCompatActivity {
    public static final String EDITED_FRAGMENT = "editedFragment";
    private ActivityEditNoteBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditNoteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentByTag(EDITED_FRAGMENT);
        if (fragment == null) {
            fm
                    .beginTransaction()
                    .add(R.id.edit_note_fragment_container_view, new EditNoteFragment(), EDITED_FRAGMENT)
                    .commit();
        }

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }
    //Activity 使用 launchMode（如 singleTop/singleTask）或通过带相应 flag 的启动方式（通知、深链接等）复用已存在实例时，需要在这里处理或保存新的启动参数。
}