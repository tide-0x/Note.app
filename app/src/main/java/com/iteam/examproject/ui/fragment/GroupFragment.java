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
import com.iteam.examproject.entity.Notes;
import com.iteam.examproject.entity.User;
import com.iteam.examproject.repo.MyRepository;
import com.iteam.examproject.utils.RvDecorationUtil;
import com.iteam.examproject.viewmodel.MyViewModel;

import java.util.List;
import java.util.Objects;

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
                        NoteGroup noteGroup = repo.createGroup(user.getUserId(), groupName);
                        if (noteGroup != null) {
                            adapter.addNoteGroup(noteGroup);
                            Toast.makeText(requireContext(), getString(R.string.add_success), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(), getString(R.string.group_name_exists), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                binding.addGroupEdit.setText("");
            }
        });

        //删除
        adapter.setGarbageCanIvClickListener(new GroupManagementAdapter.GarbageCanIvClickListener() {
            @Override
            public void onClick(int position) {
                NoteGroup noteGroup = noteGroups.get(position);
                repo.deleteGroup(noteGroup);
                NoteGroup viewModelNoteGroup = viewModel.getNoteGroupLiveData().getValue();
                if (viewModelNoteGroup != null) {
                    if (noteGroup.getUserId() == viewModelNoteGroup.getUserId() &&
                            noteGroup.getGroupId() == viewModelNoteGroup.getGroupId()) {
                        viewModel.setNoteGroupLiveData(new NoteGroup(noteGroup.getUserId(), 0, getString(R.string.all_notes)));
                        List<Notes> notesList = repo.getNotesList(viewModelNoteGroup.getUserId(), 0);
                        viewModel.setNotesListLiveData(notesList);
                    }
                }
                adapter.removeNoteGroup(position);
            }
        });

        //确认
        adapter.setConfirmIvClickListener(new GroupManagementAdapter.ConfirmIvClickListener() {
            @Override
            public boolean onClick(int position, String editedGroupName) {
                String groupName = editedGroupName.trim();
                if (groupName.isEmpty()) {
                    Toast.makeText(requireContext(), getString(R.string.group_name_empty), Toast.LENGTH_SHORT).show();
                    noteGroups = repo.getAllGroups(Objects.requireNonNull(viewModel.getUserLiveData().getValue()).getUserId());
                    adapter.updateNoteGroupList(noteGroups);
                    return true;
                }

                NoteGroup noteGroup = noteGroups.get(position);
                //名字没变，直接当成功处理，不必再去数据库查重
                if (groupName.equals(noteGroup.getGroupName())) {
                    return false;
                }

                noteGroup.setGroupName(groupName);
                boolean existed = repo.updateGroupName(noteGroup);
                if (existed) {
                    Toast.makeText(requireContext(), getString(R.string.group_name_exists), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), getString(R.string.update_success), Toast.LENGTH_SHORT).show();
                }
                noteGroups = repo.getAllGroups(Objects.requireNonNull(viewModel.getUserLiveData().getValue()).getUserId());
                adapter.updateNoteGroupList(noteGroups);
                return existed;
            }
        });


        return binding.getRoot();
    }

    private void init() {
        binding = FragmentGroupBinding.inflate(getLayoutInflater());
        viewModel = new ViewModelProvider(this).get(MyViewModel.class);
        repo = new MyRepository(requireContext());
        repo.open();
        noteGroups = repo.getAllGroups(Objects.requireNonNull(viewModel.getUserLiveData().getValue()).getUserId());
        adapter = new GroupManagementAdapter(noteGroups);
        binding.groupFragmentRv.setAdapter(adapter);
        binding.groupFragmentRv.addItemDecoration(new RvDecorationUtil(8F));
        linearLayoutManager = new LinearLayoutManager(requireContext());
        binding.groupFragmentRv.setLayoutManager(linearLayoutManager);
        // 使软键盘不调整窗口（键盘会覆盖底部导航）[实际发现，写这里没用，注册文件有用]
        //requireActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
    }


}