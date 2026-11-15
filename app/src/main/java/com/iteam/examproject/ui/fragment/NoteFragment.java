package com.iteam.examproject.ui.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.iteam.examproject.R;
import com.iteam.examproject.adapter.AddToGroupAdapter;
import com.iteam.examproject.adapter.BottomDialogGroupAdapter;
import com.iteam.examproject.adapter.BottomDialogSearchAdapter;
import com.iteam.examproject.adapter.NoteAdapter;
import com.iteam.examproject.databinding.BottomDialogGroupBinding;
import com.iteam.examproject.databinding.BottomDialogSearchNotesBinding;
import com.iteam.examproject.databinding.FragmentNoteBinding;
import com.iteam.examproject.databinding.SortBottomDialogBinding;
import com.iteam.examproject.entity.NoteGroup;
import com.iteam.examproject.entity.Notes;
import com.iteam.examproject.repo.MyRepository;
import com.iteam.examproject.repo.SharedDataRepository;
import com.iteam.examproject.ui.activity.EditNoteActivity;
import com.iteam.examproject.utils.RvDecorationUtil;
import com.iteam.examproject.utils.ToastUtil;
import com.iteam.examproject.viewmodel.MyViewModel;

import java.util.List;
import java.util.Objects;


public class NoteFragment extends Fragment {
    private FragmentNoteBinding binding;
    private MyViewModel viewModel;
    private MyRepository repo;
    private NoteAdapter noteAdapter;
    private LinearLayoutManager layoutManager;
    private SharedDataRepository sharedRepo;
    private BottomDialogGroupBinding bottomDialogGroupBinding;

    public NoteFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        init();

        //观察当前分组
        sharedRepo.getNoteGroupLiveData().observe(getViewLifecycleOwner(), noteGroup -> {
            String groupName = noteGroup.getGroupName();
            //分组名可能是登录前的占位空值，兜底显示「全部笔记」
            binding.noteFragmentToolbar.setTitle(
                    groupName == null || groupName.isEmpty() ? getString(R.string.all_notes) : groupName);
            //给创建笔记的按钮传递空的临时 Notes（避免残留）
            sharedRepo.setNotes(new Notes(noteGroup.getUserId(), noteGroup.getGroupId(), -1, "", "", -1));
        });

        //一开始进入时展示当前分组的笔记（查库放 IO 线程，回来后写进 LiveData 驱动列表）
        NoteGroup noteGroup = sharedRepo.getNoteGroupLiveData().getValue();
        if (noteGroup != null) {
            long userId = noteGroup.getUserId();
            long groupId = noteGroup.getGroupId();
            loadNotes(userId, groupId);
        }


        //观察笔记集合
        sharedRepo.getNotesListLiveData().observe(getViewLifecycleOwner(), notesList -> {
            if (notesList == null) {
                return;
            }
            noteAdapter.updateNotes(notesList);

            //点击笔记
            noteAdapter.setNoteClickListener(position -> {
                sharedRepo.setNotes(notesList.get(position));
                startActivity(new Intent(requireContext(), EditNoteActivity.class));
            });

            //长按笔记选择添加至哪个分组
            noteAdapter.setNoteLongClickListener(position -> notesLongClickListener(position, notesList));
        });


        //跳转至EditNoteActivity，先清空临时Notes再跳转
        binding.createNoteFloatActionBtn.setOnClickListener(v -> {
            NoteGroup currentGroup = sharedRepo.getNoteGroupLiveData().getValue();
            long userId = currentGroup != null ? currentGroup.getUserId() : -1;
            long groupId = currentGroup != null ? currentGroup.getGroupId() : 0;
            sharedRepo.setNotes(new Notes(userId, groupId, -1, "", "", -1));
            startActivity(new Intent(requireActivity(), EditNoteActivity.class));
        });

        binding.noteFragmentToolbar.setOnMenuItemClickListener((MenuItem item) -> {
            int itemId = item.getItemId();
            if (itemId == R.id.choose_group) {
                //选择分组
                chooseGroup();
            } else if (itemId == R.id.search) {
                //搜索
                searchNotes();
            } else if (itemId == R.id.sort) {
                //排序
                sortNotes();
            }
            return true;
        });


        return binding.getRoot();
    }

    /**
     * 查某个分组的笔记并写进 LiveData。
     * 失败时提示一句、保留原列表，不要把空列表当成「没有笔记」刷出去。
     */
    private void loadNotes(long userId, long groupId) {
        repo.runAsync(() -> repo.getNotesList(userId, groupId), result -> {
            if (!isAdded()) {
                return;
            }
            if (!result.isSuccess()) {
                ToastUtil.showOperationFailed(requireContext());
                return;
            }
            viewModel.setNotesListLiveData(result.getData());
        });
    }

    private void sortNotes() {
        SortBottomDialogBinding sortBottomDialogBinding = SortBottomDialogBinding.inflate(getLayoutInflater());
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(sortBottomDialogBinding.getRoot());
        dialog.show();
        //升序
        sortBottomDialogBinding.sortByAsc.setOnClickListener(v -> {
            dialog.dismiss();
            sortBy(MyRepository.SORT_ASC);
        });
        //降序
        sortBottomDialogBinding.sortByDes.setOnClickListener(v -> {
            dialog.dismiss();
            sortBy(MyRepository.SORT_DESC);
        });
        //取消
        sortBottomDialogBinding.cancelTv.setOnClickListener(v -> dialog.dismiss());
    }

    /**
     * 按编辑时间重排当前列表。
     * 列表里每条笔记都带着自己的编辑时间，直接按它排序就行，不用再回数据库查一遍。
     * 排序方式记在 SharedDataRepository 里，之后新建/编辑笔记插到哪一行也按它算。
     */
    private void sortBy(String sortWay) {
        sharedRepo.setSortWay(sortWay);
    }

    private void searchNotes() {
        EditText editText = new EditText(requireContext());
        editText.setHint(R.string.input_note_title_hint);
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder
                .setTitle(R.string.search)
                .setView(editText)
                .setNegativeButton(getString(R.string.cancel), (d, which) -> d.dismiss())
                .setPositiveButton(getString(R.string.confirm), (d, which) -> {
                    String keyWord = editText.getText().toString();
                    if (keyWord.isEmpty()) {
                        Toast.makeText(requireContext(), getString(R.string.input_note_title), Toast.LENGTH_SHORT).show();
                    } else {
                        long userId = Objects.requireNonNull(viewModel.getUserLiveData().getValue()).getUserId();
                        //搜索要查库，结果回来再弹列表
                        repo.runAsync(() -> repo.searchResult(userId, keyWord), result -> {
                            if (!isAdded()) {
                                return;
                            }
                            if (!result.isSuccess() || result.getData() == null) {
                                ToastUtil.showOperationFailed(requireContext());
                                return;
                            }
                            List<Notes> notesList = result.getData();
                            BottomDialogSearchAdapter searchAdapter = new BottomDialogSearchAdapter();
                            searchAdapter.updateSearchResults(notesList);
                            layoutManager = new LinearLayoutManager(requireContext());
                            BottomDialogSearchNotesBinding searchNotesBinding = BottomDialogSearchNotesBinding.inflate(getLayoutInflater());
                            searchNotesBinding.bottomDialogNotesRv.setAdapter(searchAdapter);
                            searchNotesBinding.bottomDialogNotesRv.setLayoutManager(layoutManager);
                            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
                            bottomSheetDialog.setContentView(searchNotesBinding.getRoot());
                            bottomSheetDialog.show();
                            searchNotesBinding.bottomDialogGroupCancel.setOnClickListener(v -> bottomSheetDialog.dismiss());
                            searchAdapter.setListener(position -> {
                                if (position < 0 || position >= notesList.size()) {
                                    return;
                                }
                                viewModel.setNotesLiveData(notesList.get(position));
                                bottomSheetDialog.dismiss();
                                startActivity(new Intent(requireContext(), EditNoteActivity.class));
                            });
                        });
                    }
                })
                .show();
    }

    private void chooseGroup() {
        bottomDialogGroupBinding = BottomDialogGroupBinding.inflate(getLayoutInflater());
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(bottomDialogGroupBinding.getRoot());
        //全部笔记
        bottomDialogGroupBinding.bottomDialogGroupAllNotes.setOnClickListener(v -> {
            //groupId 固定为 0：不这样做，全部笔记和别的分组里的同一篇笔记会被重复获取
            long allNotesUserId = Objects.requireNonNull(viewModel.getUserLiveData().getValue()).getUserId();
            viewModel.setNoteGroupLiveData(new NoteGroup(allNotesUserId, 0, getString(R.string.all_notes)));
            dialog.dismiss();
            loadNotes(allNotesUserId, 0);
        });

        //获取当前账号下的分组，结果回来再塞进对话框
        long userId = Objects.requireNonNull(viewModel.getUserLiveData().getValue()).getUserId();
        repo.runAsync(() -> repo.getAllGroups(userId), result -> {
            if (!isAdded()) {
                return;
            }
            if (!result.isSuccess() || result.getData() == null) {
                ToastUtil.showOperationFailed(requireContext());
                return;
            }
            List<NoteGroup> noteGroups = result.getData();
            BottomDialogGroupAdapter adapter = new BottomDialogGroupAdapter(noteGroups);
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(requireContext());
            bottomDialogGroupBinding.bottomDialogGroupRv.setAdapter(adapter);
            bottomDialogGroupBinding.bottomDialogGroupRv.setLayoutManager(linearLayoutManager);
            adapter.setListener(position -> {
                if (position < 0 || position >= noteGroups.size()) {
                    return;
                }
                NoteGroup noteGroup = noteGroups.get(position);
                viewModel.setNoteGroupLiveData(noteGroup);
                dialog.dismiss();
                loadNotes(noteGroup.getUserId(), noteGroup.getGroupId());
            });
        });

        //取消
        bottomDialogGroupBinding.bottomDialogGroupCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void notesLongClickListener(int position, List<Notes> notes) {
        if (position < 0 || position >= notes.size()) {
            return;
        }
        //复制一份再改 groupId：这个对象同时被 NoteAdapter 的列表持有，
        //直接 set 会污染列表数据，让 DiffUtil 之后比不出差异
        Notes source = notes.get(position);
        final Notes note = new Notes(source.getUserId(), source.getGroupId(), source.getNotesId(),
                source.getTitle(), source.getContent(), source.getEditTime());
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        bottomDialogGroupBinding = BottomDialogGroupBinding.inflate(getLayoutInflater());
        bottomSheetDialog.setContentView(bottomDialogGroupBinding.getRoot());
        bottomSheetDialog.show();
        bottomDialogGroupBinding.bottomDialogGroupTitle.setText(R.string.add_to_group);
        bottomDialogGroupBinding.bottomDialogGroupAllNotes.setVisibility(View.GONE);
        //取消
        bottomDialogGroupBinding.bottomDialogGroupCancel.setOnClickListener(v -> bottomSheetDialog.dismiss());

        long userId = Objects.requireNonNull(viewModel.getUserLiveData().getValue()).getUserId();
        repo.runAsync(() -> repo.getAllGroups(userId), result -> {
            if (!isAdded()) {
                return;
            }
            if (!result.isSuccess() || result.getData() == null) {
                ToastUtil.showOperationFailed(requireContext());
                return;
            }
            List<NoteGroup> noteGroups = result.getData();
            AddToGroupAdapter adapter = new AddToGroupAdapter(noteGroups);
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(requireContext());
            bottomDialogGroupBinding.bottomDialogGroupRv.setAdapter(adapter);
            bottomDialogGroupBinding.bottomDialogGroupRv.setLayoutManager(linearLayoutManager);
            adapter.setOnItemClickListener(itemPosition -> {
                if (itemPosition < 0 || itemPosition >= noteGroups.size()) {
                    return;
                }
                note.setGroupId(noteGroups.get(itemPosition).getGroupId());
                bottomSheetDialog.dismiss();
                repo.runAsync(() -> repo.addNotesToGroup(note), addResult -> {
                    if (!isAdded()) {
                        return;
                    }
                    if (!addResult.isSuccess()) {
                        ToastUtil.showOperationFailed(requireContext());
                        return;
                    }
                    //返回包是装箱 Boolean，用 TRUE.equals 判断，避免 null 拆箱崩溃
                    boolean added = Boolean.TRUE.equals(addResult.getData());
                    Toast.makeText(requireContext(),
                            getString(added ? R.string.add_success : R.string.group_already_has_note),
                            Toast.LENGTH_SHORT).show();
                });
            });
        });
    }

    private void init() {
        binding = FragmentNoteBinding.inflate(getLayoutInflater());
        sharedRepo = SharedDataRepository.getInstance();//使用单例仓库
        viewModel = new ViewModelProvider(this).get(MyViewModel.class);
        noteAdapter = new NoteAdapter();
        binding.noteFragmentNotesRv.setAdapter(noteAdapter);
        layoutManager = new LinearLayoutManager(requireContext());
        binding.noteFragmentNotesRv.setLayoutManager(layoutManager);
        binding.noteFragmentNotesRv.addItemDecoration(new RvDecorationUtil(10F));
        //共享仓储：全进程只有一条数据库连接，用完不需要关闭
        repo = MyRepository.getInstance(requireContext());
    }
}
