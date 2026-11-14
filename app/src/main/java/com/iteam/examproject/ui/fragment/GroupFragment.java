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
        binding.addGroupIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String groupName = binding.addGroupEdit.getText().toString().trim();
                if (groupName.isEmpty()) {
                    Toast.makeText(requireContext(), getString(R.string.group_name_empty), Toast.LENGTH_SHORT).show();
                } else {
                    User user = viewModel.getUserLiveData().getValue();
                    if (user != null) {
                        repo.runAsync(() -> repo.createGroup(user.getUserId(), groupName), noteGroup -> {
                            if (!isAdded()) {
                                return;
                            }
                            if (noteGroup != null) {
                                adapter.addNoteGroup(noteGroup);
                                Toast.makeText(requireContext(), getString(R.string.add_success), Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(requireContext(), getString(R.string.group_name_exists), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
                binding.addGroupEdit.setText("");
            }
        });

        //删除
        adapter.setGarbageCanIvClickListener(new GroupManagementAdapter.GarbageCanIvClickListener() {
            @Override
            public void onClick(int position) {
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
                        repo.runAsync(() -> repo.getNotesList(noteGroup.getUserId(), 0),
                                viewModel::setNotesListLiveData);
                    }
                });
            }
        });

        //确认
        adapter.setConfirmIvClickListener(new GroupManagementAdapter.ConfirmIvClickListener() {
            @Override
            public void onConfirm(int position, String editedGroupName, GroupManagementAdapter.ResultCallback done) {
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
                repo.runAsync(() -> repo.updateGroupName(target), existed -> {
                    if (!isAdded()) {
                        return;
                    }
                    Toast.makeText(requireContext(),
                            getString(existed ? R.string.group_name_exists : R.string.update_success),
                            Toast.LENGTH_SHORT).show();
                    done.onResult(existed);
                    //不管成功失败都重新拉一次，保证界面和库里一致
                    repo.runAsync(() -> repo.getAllGroups(userId), groups -> {
                        if (!isAdded() || groups == null) {
                            return;
                        }
                        noteGroups = groups;
                        adapter.updateNoteGroupList(groups);
                    });
                });
            }
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
            repo.runAsync(() -> repo.getAllGroups(user.getUserId()), groups -> {
                if (!isAdded() || groups == null) {
                    return;
                }
                noteGroups = groups;
                adapter.updateNoteGroupList(groups);
            });
        }
    }


}
