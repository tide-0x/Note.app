package com.iteam.examproject.ui.activity;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.iteam.examproject.R;
import com.iteam.examproject.databinding.ActivityContentBinding;
import com.iteam.examproject.ui.fragment.GroupFragment;
import com.iteam.examproject.ui.fragment.MineFragment;
import com.iteam.examproject.ui.fragment.NoteFragment;
import com.iteam.examproject.viewmodel.Event;
import com.iteam.examproject.viewmodel.MainViewModel;

/**
 * 底部导航容器。
 *
 * <p>三个 Fragment 用 add + show/hide 复用实例，不销毁重建（切 tab 不丢列表滚动位置和输入内容）。
 * 关键约定：{@link #currentTag} 必须和「屏幕上真正可见的那个」保持一致——
 * 切换、重建两条路径都要显式对齐，不能指望底部导航的回调顺手完成
 * （同 tag 时回调会直接 return，什么都不做）。
 *
 * <p>页面级状态（当前用户、当前分组、笔记列表）由 {@link MainViewModel} 持有，
 * 作用域是本 Activity：登录页把 userId/account 通过 Intent 传进来，退出登录时状态随页面销毁。
 */
public class ContentActivity extends AppCompatActivity {
    public static final String NOTE_FRAGMENT = "noteFragment";
    public static final String GROUP_FRAGMENT = "groupFragment";
    public static final String MINE_FRAGMENT = "mineFragment";
    public static final String EXTRA_USER_ID = "extra_user_id";
    public static final String EXTRA_ACCOUNT = "extra_account";
    private static final String[] ALL_TAGS = {NOTE_FRAGMENT, GROUP_FRAGMENT, MINE_FRAGMENT};
    private static final String KEY_CURRENT_TAG = "key_current_tag";

    private ActivityContentBinding binding;
    private MainViewModel mainViewModel;
    private String currentTag = NOTE_FRAGMENT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityContentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        //登录态：登录页通过 Intent 传进来；VM 内部会做本地兜底和失效检查
        long userId = getIntent().getLongExtra(EXTRA_USER_ID, -1);
        String account = getIntent().getStringExtra(EXTRA_ACCOUNT);
        mainViewModel.start(userId, account == null ? "" : account);
        mainViewModel.getSessionExpired().observe(this, event -> {
            if (event.getContentIfNotHandled() == null) {
                return;
            }
            Intent intent = new Intent(this, LoginAndRegisterActivity.class);
            //清空返回栈，防止注销后按返回键回到已失效的页面
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        FragmentManager fm = getSupportFragmentManager();
        if (savedInstanceState == null) {
            //首次创建：把首页加进来
            switchTo(NOTE_FRAGMENT);
        } else {
            //重建：已 add 的实例由 FragmentManager 自动恢复，但「谁该显示」不会自动校准，
            //这里显式 show 当前 tag、hide 其余，避免多个 Fragment 叠在一起
            currentTag = savedInstanceState.getString(KEY_CURRENT_TAG, NOTE_FRAGMENT);
            if (fm.findFragmentByTag(currentTag) == null) {
                //极端情况：当前 tag 的实例没被恢复回来，退回首屏重建
                currentTag = NOTE_FRAGMENT;
                switchTo(currentTag);
            } else {
                showOnly(currentTag);
            }
        }

        bottomNav();
        //让底部导航的选中项与当前显示的页面一致
        binding.bottomNavigation.setSelectedItemId(tagToMenuId(currentTag));
    }

    private void bottomNav() {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            String targetTag = menuIdToTag(item.getItemId());
            if (targetTag.equals(currentTag)) {
                return true;//已是当前页，不做任何操作
            }
            switchTo(targetTag);
            return true;
        });
    }

    /**
     * 切到指定页面：当前页 hide，目标页 show；目标页从未创建过才 add。
     *
     * <p>不做切换动画。原来进出场都挂了 alpha 动画，两个全屏页面同时半透明，
     * 中途透出的是窗口底色，屏幕会整体闪一下；而三个 tab 之间本来没有空间上的
     * 先后关系，快速连切时任何过渡都是噪音，直接换页最干净。
     */
    private void switchTo(String targetTag) {
        FragmentManager fm = getSupportFragmentManager();
        Fragment current = fm.findFragmentByTag(currentTag);
        Fragment target = fm.findFragmentByTag(targetTag);

        FragmentTransaction ft = fm.beginTransaction();
        if (current != null && current != target && current.isAdded()) {
            ft.hide(current);
        }
        if (target == null) {
            ft.add(binding.contentFragmentContainerView.getId(), createFragment(targetTag), targetTag);
        } else if (target.isHidden()) {
            ft.show(target);
        }
        ft.commit();
        currentTag = targetTag;
    }

    /**
     * 只保留一个可见：用于进程重建后的状态校准。
     * 用 commitNow，确保进入下一帧前就绪，避免先闪一下多个页面。
     */
    private void showOnly(String visibleTag) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        for (String tag : ALL_TAGS) {
            Fragment fragment = fm.findFragmentByTag(tag);
            if (fragment == null) {
                continue;
            }
            if (tag.equals(visibleTag)) {
                ft.show(fragment);
            } else {
                ft.hide(fragment);
            }
        }
        ft.commitNow();
    }

    private Fragment createFragment(String tag) {
        if (GROUP_FRAGMENT.equals(tag)) {
            return new GroupFragment();
        }
        if (MINE_FRAGMENT.equals(tag)) {
            return new MineFragment();
        }
        return new NoteFragment();
    }

    private String menuIdToTag(int menuId) {
        if (menuId == R.id.group) {
            return GROUP_FRAGMENT;
        }
        if (menuId == R.id.mine) {
            return MINE_FRAGMENT;
        }
        return NOTE_FRAGMENT;
    }

    private int tagToMenuId(String tag) {
        if (GROUP_FRAGMENT.equals(tag)) {
            return R.id.group;
        }
        if (MINE_FRAGMENT.equals(tag)) {
            return R.id.mine;
        }
        return R.id.note;
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
}
