package com.iteam.examproject.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.iteam.examproject.R;
import com.iteam.examproject.adapter.GroupManagementAdapter;
import com.iteam.examproject.databinding.FragmentGroupBinding;
import com.iteam.examproject.entity.NoteGroup;
import com.iteam.examproject.entity.User;
import com.iteam.examproject.utils.RvDecorationUtil;
import com.iteam.examproject.utils.ToastUtil;
import com.iteam.examproject.viewmodel.GroupManageViewModel;
import com.iteam.examproject.viewmodel.MainViewModel;

public class GroupFragment extends Fragment {
    private static final String TAG = "GroupFragment";
    private FragmentGroupBinding binding;
    private GroupManagementAdapter adapter;
    private GroupManageViewModel viewModel;
    private MainViewModel mainViewModel;
    private RecyclerView.LayoutManager linearLayoutManager;

    public GroupFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //初始化
        init();

        //点击添加分组按钮后的逻辑代码
        binding.addGroupIv.setOnClickListener(v -> {
            String groupName = binding.addGroupEdit.getText().toString().trim();
            if (groupName.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.group_name_empty), Toast.LENGTH_SHORT).show();
                return;
            }
            viewModel.addGroup(groupName);
            binding.addGroupEdit.setText("");
        });

        //删除
        adapter.setGarbageCanIvClickListener(position -> {
            //从适配器取：新增分组后按旧下标索引会越界
            NoteGroup noteGroup = adapter.getItem(position);
            if (noteGroup == null) {
                return;
            }
            viewModel.deleteGroup(noteGroup);
        });

        //确认（重命名）
        adapter.setConfirmIvClickListener((position, editedGroupName, done) -> {
            String groupName = editedGroupName.trim();
            NoteGroup noteGroup = adapter.getItem(position);
            if (noteGroup == null) {
                done.onResult(true);
                return;
            }
            if (groupName.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.group_name_empty), Toast.LENGTH_SHORT).show();
                done.onResult(true);
                return;
            }
            //名字没变，直接当成功处理，不必再去数据库查重
            if (groupName.equals(noteGroup.getGroupName())) {
                done.onResult(false);
                return;
            }
            //构造新对象去改名，不直接改列表里的那个：改名失败时它还得保持原名
            NoteGroup target = new NoteGroup(noteGroup.getUserId(), noteGroup.getGroupId(), groupName);
            viewModel.renameGroup(target, done::onResult);
        });

        //分组列表：VM 的状态变化直接刷进适配器（DiffUtil 只更新变化条目）
        viewModel.getGroups().observe(getViewLifecycleOwner(), groups -> {
            if (groups != null) {
                adapter.updateNoteGroupList(groups);
            }
        });

        //分组操作结果：提示 + 适配器增删 + 处理「删掉的是当前分组」
        viewModel.getOutcomes().observe(getViewLifecycleOwner(), event -> {
            GroupManageViewModel.GroupOutcome outcome = event.getContentIfNotHandled();
            if (outcome == null || !isAdded()) {
                return;
            }
            switch (outcome.kind) {
                case ADDED:
                    //列表由 groups LiveData 的观察者统一刷新，这里只负责提示
                    Toast.makeText(requireContext(), getString(R.string.add_success), Toast.LENGTH_SHORT).show();
                    break;
                case DUPLICATE_ADD:
                    Toast.makeText(requireContext(), getString(R.string.group_name_exists), Toast.LENGTH_SHORT).show();
                    break;
                case DELETED:
                    onGroupDeleted(outcome.group);
                    break;
                case RENAME_OK:
                    Toast.makeText(requireContext(), getString(R.string.update_success), Toast.LENGTH_SHORT).show();
                    break;
                case DUPLICATE_RENAME:
                    Toast.makeText(requireContext(), getString(R.string.group_name_exists), Toast.LENGTH_SHORT).show();
                    break;
                case FAILED:
                default:
                    ToastUtil.showOperationFailed(requireContext());
                    break;
            }
        });

        return binding.getRoot();
    }

    /**
     * 删掉的是当前正在看的分组时，退回「全部笔记」（列表页会跟着重新查库）；
     * 分组列表行由 groups LiveData 的观察者统一摘除。
     */
    private void onGroupDeleted(NoteGroup deleted) {
        NoteGroup current = mainViewModel.getCurrentGroup().getValue();
        if (current != null && current.getGroupId() == deleted.getGroupId()
                && current.getUserId() == deleted.getUserId()) {
            //groupId 固定为 0：全部笔记是内置的聚合视图
            User user = mainViewModel.getUser().getValue();
            long userId = user == null ? deleted.getUserId() : user.getUserId();
            mainViewModel.switchGroup(new NoteGroup(userId, 0, getString(R.string.all_notes)));
        }
    }

    private void init() {
        binding = FragmentGroupBinding.inflate(getLayoutInflater());
        //分组管理的数据与操作在自己页面的 VM 里；登录用户、当前分组在 MainViewModel 里
        viewModel = new ViewModelProvider(this).get(GroupManageViewModel.class);
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        viewModel.setUser(mainViewModel.getUser().getValue());
        mainViewModel.getUser().observe(getViewLifecycleOwner(), viewModel::setUser);
        //先把空列表交给适配器，查库结果回来再填，主线程不阻塞
        adapter = new GroupManagementAdapter();
        binding.groupFragmentRv.setAdapter(adapter);
        binding.groupFragmentRv.addItemDecoration(new RvDecorationUtil(8F));
        linearLayoutManager = new LinearLayoutManager(requireContext());
        binding.groupFragmentRv.setLayoutManager(linearLayoutManager);
        // 使软键盘不调整窗口（键盘会覆盖底部导航）[实际发现，写这里没用，注册文件有用]
        //requireActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);

        viewModel.load();
    }


}
