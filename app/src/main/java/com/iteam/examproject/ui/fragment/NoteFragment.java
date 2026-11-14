package com.iteam.examproject.ui.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationBarView;
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
import com.iteam.examproject.ui.activity.LoginAndRegisterActivity;
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
    private com.iteam.examproject.databinding.BottomDialogGroupBinding bottomDialogGroupBinding;

    public NoteFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        init();

        //观察当前分组
        sharedRepo.getNoteGroupLiveData().observe(getViewLifecycleOwner(), new Observer<NoteGroup>() {
            @Override
            public void onChanged(NoteGroup noteGroup) {
                String groupName = noteGroup.getGroupName();
                //分组名可能是登录前的占位空值，兜底显示「全部笔记」
                binding.noteFragmentToolbar.setTitle(
                        groupName == null || groupName.isEmpty() ? getString(R.string.all_notes) : groupName);
                //给创建笔记的按钮floatActionBtn的fragment传递数据//
                //给创建笔记的按钮传递空的临时 Notes（避免残留）
                sharedRepo.setNotes(new Notes(noteGroup.getUserId(), noteGroup.getGroupId(), -1, "", "", -1));
            }
        });

        //一开始进入时展示全部笔记
        NoteGroup noteGroup = sharedRepo.getNoteGroupLiveData().getValue();
        List<Notes> notes = null;
        if (noteGroup != null) {
            notes = repo.getNotesList(noteGroup.getUserId(), noteGroup.getGroupId());
        }
        sharedRepo.setNotesList(notes);


        //观察笔记集合
        sharedRepo.getNotesListLiveData().observe(getViewLifecycleOwner(), new Observer<List<Notes>>() {
            @Override
            public void onChanged(List<Notes> notesList) {
                noteAdapter.updateNotes(notesList);

                //点击笔记
                noteAdapter.setNoteClickListener(new NoteAdapter.NoteClickListener() {
                    @Override
                    public void onClick(int position) {
                        sharedRepo.setNotes(notesList.get(position));
                        startActivity(new Intent(requireContext(), EditNoteActivity.class));
                    }
                });

                //长按笔记选择添加至哪个分组
                noteAdapter.setNoteLongClickListener(new NoteAdapter.NoteLongClickListener() {
                    @Override
                    public void onLongClick(int position) {
                        notesLongClickListener(position, notesList);
                    }
                });
            }
        });


        //跳转至EditNoteActivity，先清空临时Notes再跳转
        binding.createNoteFloatActionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NoteGroup currentGroup = sharedRepo.getNoteGroupLiveData().getValue();
                long userId = currentGroup != null ? currentGroup.getUserId() : -1;
                long groupId = currentGroup != null ? currentGroup.getGroupId() : 0;
                sharedRepo.setNotes(new Notes(userId, groupId, -1, "", "", -1));
                startActivity(new Intent(requireActivity(), EditNoteActivity.class));
            }
        });

        binding.noteFragmentToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
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
            }
        });


        return binding.getRoot();
    }

    private void sortNotes() {
        SortBottomDialogBinding sortBottomDialogBinding = SortBottomDialogBinding.inflate(getLayoutInflater());
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(sortBottomDialogBinding.getRoot());
        dialog.show();
        //升序
        sortBottomDialogBinding.sortByAsc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<Notes> notes = repo.notesSort(
                        Objects.requireNonNull(viewModel.getUserLiveData().getValue()).getUserId(),
                        Objects.requireNonNull(viewModel.getNoteGroupLiveData().getValue()).getGroupId(),
                        MyRepository.SORT_ASC);
                viewModel.setNotesListLiveData(notes);
                dialog.dismiss();
            }
        });
        //降序
        sortBottomDialogBinding.sortByDes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<Notes> notes = repo.notesSort(
                        Objects.requireNonNull(viewModel.getUserLiveData().getValue()).getUserId(),
                        Objects.requireNonNull(viewModel.getNoteGroupLiveData().getValue()).getGroupId(),
                        MyRepository.SORT_DESC);
                viewModel.setNotesListLiveData(notes);
                dialog.dismiss();
            }
        });
        //取消
        sortBottomDialogBinding.cancelTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }

    private void searchNotes() {
        EditText editText = new EditText(requireContext());
        editText.setHint(R.string.input_note_title_hint);
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder
                .setTitle(R.string.search)
                .setView(editText)
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String keyWord = editText.getText().toString();
                        if (keyWord.isEmpty()) {
                            Toast.makeText(requireContext(), getString(R.string.input_note_title), Toast.LENGTH_SHORT).show();
                        } else {
                            List<Notes> notesList = repo.searchResult(Objects.requireNonNull(viewModel.getUserLiveData().getValue()).getUserId(), keyWord);
                            BottomDialogSearchAdapter searchAdapter = new BottomDialogSearchAdapter();
                            searchAdapter.updateSearchResults(notesList);
                            layoutManager = new LinearLayoutManager(requireContext());
                            BottomDialogSearchNotesBinding searchNotesBinding = BottomDialogSearchNotesBinding.inflate(getLayoutInflater());
                            searchNotesBinding.bottomDialogNotesRv.setAdapter(searchAdapter);
                            searchNotesBinding.bottomDialogNotesRv.setLayoutManager(layoutManager);
                            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
                            bottomSheetDialog.setContentView(searchNotesBinding.getRoot());
                            bottomSheetDialog.show();
                            searchNotesBinding.bottomDialogGroupCancel.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    bottomSheetDialog.dismiss();

                                }
                            });
                            searchAdapter.setListener(new BottomDialogSearchAdapter.OnBottomDialogSearchClickListener() {
                                @Override
                                public void onClick(int position) {
                                    viewModel.setNotesLiveData(notesList.get(position));
                                    dialog.dismiss();
                                    bottomSheetDialog.dismiss();
                                    startActivity(new Intent(requireContext(), EditNoteActivity.class));
                                }
                            });
                        }
                    }
                })
                .show();
    }

    private void chooseGroup() {
        bottomDialogGroupBinding = BottomDialogGroupBinding.inflate(getLayoutInflater());
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(bottomDialogGroupBinding.getRoot());
        //全部笔记
        bottomDialogGroupBinding.bottomDialogGroupAllNotes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //获取notes需要groupId为0：不这样做，会导致全部笔记以及在别的分组中的笔记会被重复获取
                List<Notes> notes = repo.getNotesList(Objects.requireNonNull(viewModel.getUserLiveData().getValue()).getUserId(), 0);
                viewModel.setNoteGroupLiveData(new NoteGroup(viewModel.getUserLiveData().getValue().getUserId(), 0, getString(R.string.all_notes)));
                viewModel.setNotesListLiveData(notes);
                dialog.dismiss();
            }
        });

        //获取当前账号下的分组
        List<NoteGroup> noteGroups = repo.getAllGroups(Objects.requireNonNull(viewModel.getUserLiveData().getValue()).getUserId());
        BottomDialogGroupAdapter adapter = new BottomDialogGroupAdapter(noteGroups);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(requireContext());
        bottomDialogGroupBinding.bottomDialogGroupRv.setAdapter(adapter);
        bottomDialogGroupBinding.bottomDialogGroupRv.setLayoutManager(linearLayoutManager);
        adapter.setListener(new BottomDialogGroupAdapter.OnBottomDialogGroupClickListener() {
            @Override
            public void onClick(int position) {
                NoteGroup noteGroup = noteGroups.get(position);
                //获取当前账号该分组中的笔记
                List<Notes> notes = repo.getNotesList(noteGroup.getUserId(), noteGroup.getGroupId());
                viewModel.setNoteGroupLiveData(noteGroup);
                viewModel.setNotesListLiveData(notes);
                dialog.dismiss();
            }
        });

        //取消
        bottomDialogGroupBinding.bottomDialogGroupCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private void notesLongClickListener(int position, List<Notes> notes) {
        Notes notes1 = notes.get(position);
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        bottomDialogGroupBinding = BottomDialogGroupBinding.inflate(getLayoutInflater());
        bottomSheetDialog.setContentView(bottomDialogGroupBinding.getRoot());
        bottomSheetDialog.show();
        List<NoteGroup> noteGroups = repo.getAllGroups(Objects.requireNonNull(viewModel.getUserLiveData().getValue()).getUserId());
        AddToGroupAdapter adapter = new AddToGroupAdapter(noteGroups);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(requireContext());
        bottomDialogGroupBinding.bottomDialogGroupRv.setAdapter(adapter);
        bottomDialogGroupBinding.bottomDialogGroupRv.setLayoutManager(linearLayoutManager);
        bottomDialogGroupBinding.bottomDialogGroupTitle.setText(R.string.add_to_group);
        bottomDialogGroupBinding.bottomDialogGroupAllNotes.setVisibility(View.GONE);
        //取消
        bottomDialogGroupBinding.bottomDialogGroupCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetDialog.dismiss();
            }
        });

        adapter.setOnItemClickListener(new AddToGroupAdapter.OnItemClickListener() {
            @Override
            public void onClick(int position) {
                NoteGroup noteGroup1 = noteGroups.get(position);
                notes1.setGroupId(noteGroup1.getGroupId());
                boolean result = repo.addNotesToGroup(notes1);
                if (result) {//true
                    Toast.makeText(requireContext(), getString(R.string.add_success), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), getString(R.string.group_already_has_note), Toast.LENGTH_SHORT).show();
                }
                bottomSheetDialog.dismiss();
            }
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
        repo = new MyRepository(requireContext());
        repo.open();
    }
}