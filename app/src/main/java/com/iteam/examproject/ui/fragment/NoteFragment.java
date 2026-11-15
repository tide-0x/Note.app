package com.iteam.examproject.ui.fragment;

import android.app.AlertDialog;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.iteam.examproject.entity.User;
import com.iteam.examproject.repo.MyRepository;
import com.iteam.examproject.ui.activity.EditNoteActivity;
import com.iteam.examproject.utils.RvDecorationUtil;
import com.iteam.examproject.utils.ToastUtil;
import com.iteam.examproject.viewmodel.MainViewModel;

import java.util.List;


public class NoteFragment extends Fragment {
    private FragmentNoteBinding binding;
    private MainViewModel viewModel;
    private NoteAdapter noteAdapter;
    private LinearLayoutManager layoutManager;
    private BottomDialogGroupBinding bottomDialogGroupBinding;

    /**
     * 编辑页的结果回传：保存/删除成功后按 notesId 在内存列表上做增量修改，
     * 认不出那一条时 VM 内部会自动退回整表重查。
     * launcher 必须在字段初始化时注册（Fragment 进入 STARTED 之前）。
     */
    private final ActivityResultLauncher<android.content.Intent> editNoteLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), this::onEditNoteResult);

    public NoteFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        init();

        //观察当前分组：改标题栏，分组名可能为空（登录前的占位值）就兜底显示「全部笔记」
        viewModel.getCurrentGroup().observe(getViewLifecycleOwner(), noteGroup -> {
            String groupName = noteGroup.getGroupName();
            binding.noteFragmentToolbar.setTitle(
                    groupName == null || groupName.isEmpty() ? getString(R.string.all_notes) : groupName);
        });

        //观察提示（VM 里操作失败等一次性消息）
        viewModel.getMessages().observe(getViewLifecycleOwner(), event -> {
            Integer resId = event.getContentIfNotHandled();
            if (resId != null) {
                Toast.makeText(requireContext(), getString(resId), Toast.LENGTH_SHORT).show();
            }
        });

        //观察笔记集合，DiffUtil 只刷新真正变化的条目
        viewModel.getNotesList().observe(getViewLifecycleOwner(), notesList -> {
            if (notesList == null) {
                return;
            }
            noteAdapter.updateNotes(notesList);
        });

        //点击笔记：带上笔记内容跳编辑页（Intent 传参，不再依赖全局单例）
        noteAdapter.setNoteClickListener(position -> {
            Notes note = noteAt(position);
            if (note != null) {
                editNoteLauncher.launch(EditNoteActivity.createIntent(requireContext(), note));
            }
        });

        //长按笔记选择添加至哪个分组
        noteAdapter.setNoteLongClickListener(position -> {
            Notes note = noteAt(position);
            if (note != null) {
                notesLongClickListener(note);
            }
        });

        //跳转至EditNoteActivity新建笔记
        binding.createNoteFloatActionBtn.setOnClickListener(v -> {
            NoteGroup currentGroup = viewModel.getCurrentGroup().getValue();
            User user = viewModel.getUser().getValue();
            long userId = user == null ? -1 : user.getUserId();
            long groupId = currentGroup == null ? 0 : currentGroup.getGroupId();
            //新建的笔记还没有 id，用 -1 占位
            editNoteLauncher.launch(EditNoteActivity.createIntent(requireContext(),
                    new Notes(userId, groupId, -1, "", "", -1)));
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

    /** 从当前列表里按位置取笔记，越界防御 */
    private Notes noteAt(int position) {
        List<Notes> notes = viewModel.getNotesList().getValue();
        if (notes == null || position < 0 || position >= notes.size()) {
            return null;
        }
        return notes.get(position);
    }

    /** 编辑页回传的结果：写库已在编辑页完成，这里只改内存列表 */
    private void onEditNoteResult(ActivityResult result) {
        if (result.getResultCode() != android.app.Activity.RESULT_OK || result.getData() == null) {
            return;
        }
        String outcome = EditNoteActivity.readOutcome(result.getData());
        Notes note = EditNoteActivity.readNote(result.getData());
        if (outcome == null || note == null) {
            return;
        }
        switch (outcome) {
            case EditNoteActivity.OUTCOME_INSERTED:
                viewModel.insertNote(note);
                break;
            case EditNoteActivity.OUTCOME_UPDATED:
                viewModel.updateNote(note);
                break;
            case EditNoteActivity.OUTCOME_DELETED:
                viewModel.deleteNote(note);
                break;
            default:
                break;
        }
    }

    /**
     * 按编辑时间重排当前列表。
     * 列表里每条笔记都带着自己的编辑时间，VM 直接按它排序，不用再回数据库查一遍。
     */
    private void sortNotes() {
        SortBottomDialogBinding sortBottomDialogBinding = SortBottomDialogBinding.inflate(getLayoutInflater());
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(sortBottomDialogBinding.getRoot());
        dialog.show();
        //升序
        sortBottomDialogBinding.sortByAsc.setOnClickListener(v -> {
            dialog.dismiss();
            viewModel.sortBy(MyRepository.SORT_ASC);
        });
        //降序
        sortBottomDialogBinding.sortByDes.setOnClickListener(v -> {
            dialog.dismiss();
            viewModel.sortBy(MyRepository.SORT_DESC);
        });
        //取消
        sortBottomDialogBinding.cancelTv.setOnClickListener(v -> dialog.dismiss());
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
                        return;
                    }
                    //搜索要查库，结果回来再弹列表
                    viewModel.searchNotes(keyWord, this::showSearchResult,
                            () -> {
                                if (isAdded()) {
                                    ToastUtil.showOperationFailed(requireContext());
                                }
                            });
                })
                .show();
    }

    /** 弹出搜索结果列表，点某一条跳编辑页 */
    private void showSearchResult(List<Notes> notesList) {
        if (!isAdded()) {
            return;
        }
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
            bottomSheetDialog.dismiss();
            editNoteLauncher.launch(EditNoteActivity.createIntent(requireContext(), notesList.get(position)));
        });
    }

    private void chooseGroup() {
        bottomDialogGroupBinding = BottomDialogGroupBinding.inflate(getLayoutInflater());
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(bottomDialogGroupBinding.getRoot());
        //全部笔记
        bottomDialogGroupBinding.bottomDialogGroupAllNotes.setOnClickListener(v -> {
            User user = viewModel.getUser().getValue();
            if (user == null) {
                return;
            }
            //groupId 固定为 0：不这样做，全部笔记和别的分组里的同一篇笔记会被重复获取
            dialog.dismiss();
            viewModel.switchGroup(new NoteGroup(user.getUserId(), 0, getString(R.string.all_notes)));
        });

        //获取当前账号下的分组，结果回来再塞进对话框
        viewModel.loadGroups(noteGroups -> {
            if (!isAdded()) {
                return;
            }
            BottomDialogGroupAdapter adapter = new BottomDialogGroupAdapter(noteGroups);
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(requireContext());
            bottomDialogGroupBinding.bottomDialogGroupRv.setAdapter(adapter);
            bottomDialogGroupBinding.bottomDialogGroupRv.setLayoutManager(linearLayoutManager);
            adapter.setListener(position -> {
                if (position < 0 || position >= noteGroups.size()) {
                    return;
                }
                dialog.dismiss();
                viewModel.switchGroup(noteGroups.get(position));
            });
        }, () -> {
            if (isAdded()) {
                ToastUtil.showOperationFailed(requireContext());
            }
        });

        //取消
        bottomDialogGroupBinding.bottomDialogGroupCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    /** 长按笔记：弹分组列表，选一个把笔记加进去 */
    private void notesLongClickListener(Notes source) {
        //复制一份再改 groupId：这个对象同时被 NoteAdapter 的列表持有，
        //直接 set 会污染列表数据，让 DiffUtil 之后比不出差异
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

        viewModel.loadGroups(noteGroups -> {
            if (!isAdded()) {
                return;
            }
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
                viewModel.addToGroup(note, added -> {
                    if (!isAdded()) {
                        return;
                    }
                    Toast.makeText(requireContext(),
                            getString(added ? R.string.add_success : R.string.group_already_has_note),
                            Toast.LENGTH_SHORT).show();
                }, () -> {
                    if (isAdded()) {
                        ToastUtil.showOperationFailed(requireContext());
                    }
                });
            });
        }, () -> {
            if (isAdded()) {
                ToastUtil.showOperationFailed(requireContext());
            }
        });
    }

    private void init() {
        binding = FragmentNoteBinding.inflate(getLayoutInflater());
        //页面状态在 ContentActivity 级的 MainViewModel 里，和分组页共享同一份
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        noteAdapter = new NoteAdapter();
        binding.noteFragmentNotesRv.setAdapter(noteAdapter);
        layoutManager = new LinearLayoutManager(requireContext());
        binding.noteFragmentNotesRv.setLayoutManager(layoutManager);
        binding.noteFragmentNotesRv.addItemDecoration(new RvDecorationUtil(10F));
    }
}
