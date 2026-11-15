package com.iteam.examproject.feature.editor;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.iteam.examproject.R;
import com.iteam.examproject.data.model.Notes;
import com.iteam.examproject.databinding.ActivityEditNoteBinding;

/**
 * 编辑笔记页容器。
 *
 * <p>要编辑哪篇笔记由 Intent 传入（原来是靠进程级单例「先写后读」，现在显式传参）；
 * 保存/删除的结果通过 Activity Result 回传给启动它的列表页。
 */
public class EditNoteActivity extends AppCompatActivity {
    public static final String EDITED_FRAGMENT = "editedFragment";

    public static final String EXTRA_NOTE = "extra_note";
    public static final String EXTRA_OUTCOME = "extra_outcome";
    public static final String OUTCOME_INSERTED = "inserted";
    public static final String OUTCOME_UPDATED = "updated";
    public static final String OUTCOME_DELETED = "deleted";

    private ActivityEditNoteBinding binding;

    /** 列表页启动编辑页的统一入口 */
    public static Intent createIntent(Context context, Notes note) {
        Intent intent = new Intent(context, EditNoteActivity.class);
        intent.putExtra(EXTRA_NOTE, note);
        return intent;
    }

    /** 编辑页保存/删除成功后给列表页的回传结果 */
    public static Intent buildResult(String outcome, Notes note) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_OUTCOME, outcome);
        intent.putExtra(EXTRA_NOTE, note);
        return intent;
    }

    @SuppressWarnings("deprecation")
    public static Notes readNote(Intent intent) {
        return intent == null ? null : (Notes) intent.getSerializableExtra(EXTRA_NOTE);
    }

    public static String readOutcome(Intent intent) {
        return intent == null ? null : intent.getStringExtra(EXTRA_OUTCOME);
    }

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
