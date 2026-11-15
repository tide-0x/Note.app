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
import com.iteam.examproject.repo.MyRepository;
import com.iteam.examproject.utils.RvDecorationUtil;
import com.iteam.examproject.utils.ToastUtil;
import com.iteam.examproject.viewmodel.MyViewModel;

import java.util.ArrayList;
import java.util.List;

public class GroupFragment extends Fragment {
    private static final String TAG = "GroupFragment";
    private FragmentGroupBinding binding;
    private GroupManagementAdapter adapter;
    private MyViewModel viewModel;
    private MyRepository repo;
    private List<NoteGroup> noteGroups;
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
            } else {
                User user = viewModel.getUserLiveData().getValue();
                if (user != null) {
                    repo.runAsync(() -> repo.createGroup(user.getUserId(), groupName), result -> {
                        if (!isAdded()) {
                            return;
                        }
                        if (!result.isSuccess()) {
                            ToastUtil.showOperationFailed(requireContext());
                        } else if (result.getData() != null) {
                            adapter.addNoteGroup(result.getData());
                            Toast.makeText(requireContext(), getString(R.string.add_success), Toast.LENGTH_SHORT).show();
                        } else {
                            //成功但没返回分组 = 组名重复
                            Toast.makeText(requireContext(), getString(R.string.group_name_exists), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
            binding.addGroupEdit.setText("");
        });

        //删除
        adapter.setGarbageCanIvClickListener(position -> {
            //从适配器取：新增分组后外部的 noteGroups 可能还是旧的，按 position 索引会越界
            NoteGroup noteGroup = adapter.getItem(position);
            if (noteGroup == null) {
                return;
            }
            repo.runAsync(() -> repo.deleteGroup(noteGroup), () -> {
                if (!isAdded()) {
                    return;
                }
                NoteGroup viewModelNoteGroup = viewModel.getNoteGroupLiveData().getValue();
                boolean deletingCurrent = viewModelNoteGroup != null
                        && noteGroup.getUserId() == viewModelNoteGroup.getUserId()
                        && noteGroup.getGroupId() == viewModelNoteGroup.getGroupId();
                //按对象重新定位：删除是异步的，回来时列表可能已经变了，用旧下标会删错行
                int index = adapter.indexOf(noteGroup);
                if (index >= 0) {
                    adapter.removeNoteGroup(index);
                }
                if (deletingCurrent) {
                    //删掉的是当前正在看的分组，退回「全部笔记」
                    viewModel.setNoteGroupLiveData(new NoteGroup(noteGroup.getUserId(), 0, getString(R.string.all_notes)));
                    repo.runAsync(() -> repo.getNotesList(noteGroup.getUserId(), 0), result -> {
                        if (result.isSuccess()) {
                            viewModel.setNotesListLiveData(result.getData());
                        }
                    });
                }
            }, () -> {
                if (isAdded()) {
                    ToastUtil.showOperationFailed(requireContext());
                }
            });
        });

        //确认
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

            long userId = noteGroup.getUserId();
            //构造新对象去改名，不直接改列表里的那个：改名失败时它还得保持原名
            NoteGroup target = new NoteGroup(noteGroup.getUserId(), noteGroup.getGroupId(), groupName);
            repo.runAsync(() -> repo.updateGroupName(target), result -> {
                if (!isAdded()) {
                    return;
                }
                if (!result.isSuccess()) {
                    ToastUtil.showOperationFailed(requireContext());
                    done.onResult(true);
                    return;
                }
                //结果用装箱类型接，避免 null 自动拆箱崩溃
                boolean existed = Boolean.TRUE.equals(result.getData());
                Toast.makeText(requireContext(),
                        getString(existed ? R.string.group_name_exists : R.string.update_success),
                        Toast.LENGTH_SHORT).show();
                done.onResult(existed);
                //不管成功失败都重新拉一次，保证界面和库里一致
                repo.runAsync(() -> repo.getAllGroups(userId), refreshed -> {
                    if (!isAdded() || !refreshed.isSuccess() || refreshed.getData() == null) {
                        return;
                    }
                    noteGroups = refreshed.getData();
                    adapter.updateNoteGroupList(noteGroups);
                });
            });
        });


        return binding.getRoot();
    }

    private void init() {
        binding = FragmentGroupBinding.inflate(getLayoutInflater());
        viewModel = new ViewModelProvider(this).get(MyViewModel.class);
        repo = MyRepository.getInstance(requireContext());
        //先把空列表交给适配器，查库结果回来再填，主线程不阻塞
        noteGroups = new ArrayList<>();
        adapter = new GroupManagementAdapter(noteGroups);
        binding.groupFragmentRv.setAdapter(adapter);
        binding.groupFragmentRv.addItemDecoration(new RvDecorationUtil(8F));
        linearLayoutManager = new LinearLayoutManager(requireContext());
        binding.groupFragmentRv.setLayoutManager(linearLayoutManager);
        // 使软键盘不调整窗口（键盘会覆盖底部导航）[实际发现，写这里没用，注册文件有用]
        //requireActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);

        User user = viewModel.getUserLiveData().getValue();
        if (user != null) {
            repo.runAsync(() -> repo.getAllGroups(user.getUserId()), result -> {
                if (!isAdded() || !result.isSuccess() || result.getData() == null) {
                    return;
                }
                noteGroups = result.getData();
                adapter.updateNoteGroupList(noteGroups);
            });
        }
    }


}
