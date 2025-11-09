package com.iteam.examproject.ui.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationBarView;
import com.iteam.examproject.R;
import com.iteam.examproject.adapter.GroupManagementAdapter;
import com.iteam.examproject.databinding.FragmentGroupBinding;
import com.iteam.examproject.entity.NoteGroup;
import com.iteam.examproject.entity.User;
import com.iteam.examproject.repo.MyRepository;
import com.iteam.examproject.ui.activity.LoginAndRegisterActivity;
import com.iteam.examproject.utils.RvDecorationUtil;
import com.iteam.examproject.utils.ToastUtil;
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
                    //.showToast(requireContext(),"组别名不能为空");
                    Toast.makeText(requireContext(), "组别名不能为空", Toast.LENGTH_SHORT).show();
                } else {
                    User user = viewModel.getUserLiveData().getValue();
                    if (user != null) {
                        NoteGroup noteGroup = repo.createGroup(user.getUserId(), groupName);
                        if (noteGroup != null) {
                            adapter.addNoteGroup(noteGroup);
                            //ToastUtil.showToast(requireContext(),"添加成功");
                            Toast.makeText(requireContext(), "添加成功", Toast.LENGTH_SHORT).show();
                        } else {
                            //ToastUtil.showToast(requireContext(),"该组名已存在");
                            Toast.makeText(requireContext(), "该组名已存在", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        });

        //删除
        adapter.setGarbageCanIvClickListener(new GroupManagementAdapter.GarbageCanIvClickListener() {
            @Override
            public void onClick(int position) {
                repo.deleteGroup(noteGroups.get(position));
                adapter.removeNoteGroup(position);
            }
        });

        //确认
        adapter.setConfirmIvClickListener(new GroupManagementAdapter.ConfirmIvClickListener() {
            @Override
            public boolean onClick(int position, String editedGroupName) {
                String groupName = noteGroups.get(position).getGroupName();
                if (groupName.equals(editedGroupName)) {
                    //ToastUtil.showToast(requireContext(),"该分组名已存在");
                    Toast.makeText(requireContext(), "该分组名已存在", Toast.LENGTH_SHORT).show();
                    return false;
                }
                if(editedGroupName.isEmpty()){
                    //ToastUtil.showToast(requireContext(),"分组名不能为空");
                    Toast.makeText(requireContext(), "分组名不能为空", Toast.LENGTH_SHORT).show();
                    return false;
                }
                noteGroups.get(position).setGroupName(editedGroupName);
                repo.updateGroupName(noteGroups.get(position));
                noteGroups = repo.getAllGroups(Objects.requireNonNull(viewModel.getUserLiveData().getValue()).getUserId());
                //ToastUtil.showToast(requireContext(),"修改成功");
                Toast.makeText(requireContext(), "修改成功", Toast.LENGTH_SHORT).show();
                adapter.updateNoteGroupList(noteGroups);
                return true;
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