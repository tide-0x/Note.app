package com.iteam.examproject.ui.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.iteam.examproject.R;
import com.iteam.examproject.databinding.BackAlertDialogBinding;
import com.iteam.examproject.databinding.DeleteAlertDialogBinding;
import com.iteam.examproject.databinding.FragmentEditNoteBinding;
import com.iteam.examproject.entity.Notes;
import com.iteam.examproject.repo.MyRepository;
import com.iteam.examproject.repo.SharedDataRepository;
import com.iteam.examproject.utils.ToastUtil;
import com.iteam.examproject.viewmodel.MyViewModel;

import java.util.List;

public class EditNoteFragment extends Fragment {
    private FragmentEditNoteBinding binding;
    private MyViewModel viewModel;
    private MyRepository repo;
    private String title = "";
    private String content = "";
    private SharedDataRepository sharedRepo;
    private String originalTitle = "";
    private String originalContent = "";
    private boolean titleIsChanged;
    private boolean contentIsChanged;
    private Notes viewModelNotes;
    private AlertDialog activeDialog;

    public EditNoteFragment() {
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //初始化
        init();
        //把第二个 EditText 设为多行可滚动、启用纵向滚动条并保证父窗口在软键盘弹出时调整大小
        EditText contentEdit = binding.noteContentEdit;
        contentEdit.setVerticalScrollBarEnabled(true);

        contentEdit.setMovementMethod(ScrollingMovementMethod.getInstance());
        //让 EditText 支持内部文本滚动
        //当文本内容超过控件高度时，用户可以上下滚动查看

        contentEdit.setOnTouchListener((v, event) -> {
            //当 EditText 嵌套在可滚动容器中时会发生滚动冲突：用户想在内部滚动文本，
            //却触发了外层容器滚动。这里告诉父容器不要拦截触摸事件，交给 EditText 自己处理。
            v.getParent().requestDisallowInterceptTouchEvent(true);
            //返回值 false 表示触摸事件继续传给 EditText 默认处理
            return false;
        });

        viewModel.getNotesLiveData().observe(getViewLifecycleOwner(), notes -> {
            if (notes == null) {
                return;
            }
            if (!notes.getTitle().isEmpty()) {
                binding.noteTitleEdit.setText(notes.getTitle());
            }
            if (!notes.getContent().isEmpty()) {
                binding.noteContentEdit.setText(notes.getContent());
            }
            //originalTitle是否为空也可以判断是来自新建笔记还是已存在的笔记
            originalTitle = notes.getTitle();
            originalContent = notes.getContent();

            viewModelNotes = notes;
        });


        //返回键（标题栏箭头）
        binding.editNoteToolbar.setNavigationOnClickListener(v -> handleBack());

        //系统返回键（手势/硬件键）走同一套逻辑：
        //否则按返回会直接关掉页面，没保存的修改就静默丢了
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        handleBack();
                    }
                });


        binding.editNoteToolbar.setOnMenuItemClickListener((MenuItem item) -> {
            int itemId = item.getItemId();
            if (itemId == R.id.delete) {//删除笔记
                //删除：要么有，要么无
                title = binding.noteTitleEdit.getText().toString();
                content = binding.noteContentEdit.getText().toString();
                //新建笔记处点开，没有进行保存
                if (originalTitle.isEmpty() && originalContent.isEmpty()) {
                    requireActivity().finish();
                } else {//标题和内容至少有一个不为空
                    deleteNotesAlertDialog();
                }

            } else if (itemId == R.id.save) {
                //保存笔记：创建或更新后刷新列表，清空临时notes，finish
                title = binding.noteTitleEdit.getText().toString();
                content = binding.noteContentEdit.getText().toString();
                //保存时判断和一开始进入该篇笔记时是否发生变化
                titleIsChanged = !title.equals(originalTitle);
                contentIsChanged = !content.equals(originalContent);
                if (titleIsChanged || contentIsChanged) {//标题和内容至少有一个发生改变
                    //标题内容都为空->变相删除笔记
                    if (title.isEmpty() && content.isEmpty()) {
                        //删库也是 IO，删完再关页面
                        long notesUserId = viewModelNotes.getUserId();
                        long notesGroupId = viewModelNotes.getGroupId();
                        long notesId = viewModelNotes.getNotesId();
                        repo.runAsync(() -> repo.deleteNotes(notesUserId, notesGroupId, notesId), result -> {
                            if (!isAdded()) {
                                return;
                            }
                            if (!result.isSuccess()) {
                                ToastUtil.showOperationFailed(requireContext());
                                return;
                            }
                            sharedRepo.setNotes(new Notes(-1, -1, -1, "", "", -1));
                            sharedRepo.setNotesList(result.getData());
                            requireActivity().finish();
                        });
                        return true;
                    } else if (!content.isEmpty() && title.isEmpty()) {
                        //有内容但没标题
                        Toast.makeText(requireContext(), getString(R.string.title_empty), Toast.LENGTH_SHORT).show();
                        return true;
                    } else {//一般情况
                        //后续用户又点击保存后判断是否有变化
                        originalTitle = title;
                        originalContent = content;
                        SaveNotesData(() -> {
                            if (isAdded()) {
                                Toast.makeText(requireContext(), getString(R.string.save_success), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } else {//什么都没有改变
                    if (title.isEmpty() && content.isEmpty()) {//没有编辑（新建笔记处来，什么都没有就点了保存）
                        Toast.makeText(requireContext(), getString(R.string.nothing_to_save), Toast.LENGTH_SHORT).show();
                    } else {//已保存过又点击了保存
                        Toast.makeText(requireContext(), getString(R.string.content_saved), Toast.LENGTH_SHORT).show();
                    }
                }
            }
            return true;
        });

        return binding.getRoot();
    }

    /**
     * 返回前的统一处理：有未保存的修改就弹对话框确认，没有就直接退出。
     */
    private void handleBack() {
        title = binding.noteTitleEdit.getText().toString();
        content = binding.noteContentEdit.getText().toString();

        //标题内容是否更改
        titleIsChanged = !title.equals(originalTitle);
        contentIsChanged = !content.equals(originalContent);

        if (titleIsChanged || contentIsChanged) {//标题内容至少有一个发生了改变
            if (title.isEmpty() && !content.isEmpty()) {//有内容但标题为空
                Toast.makeText(requireContext(), getString(R.string.title_empty), Toast.LENGTH_SHORT).show();
            } else {
                //1.都为空
                //2.都不为空
                //3.有标题没内容
                backAlertDialog();
                //弹出对话框处理保存/取消，注意这里直接返回，不在此处 finish
            }
            return;
        }
        //有编辑但保存好了/没有发生变化
        //清空临时notes防止下次新建保留
        sharedRepo.setNotes(new Notes(-1, -1, -1, "", "", -1));
        requireActivity().finish();
    }

    /**
     * 保存当前笔记：新建走 createNote，已存在的走 updateNotes，然后刷新当前分组的列表。
     * 写库在 IO 线程，onSaved 在主线程回调；需要界面跳转或提示的，都放进 onSaved。
     */
    private void SaveNotesData(Runnable onSaved) {
        long editedTime = System.currentTimeMillis();
        //notesLiveData 里存的正是列表点击时传进来的那个对象，NoteAdapter 的列表同样持有它。
        //这里必须复制一份再改：直接在原对象上 set 会「悄悄改掉适配器的数据」，
        //保存后 DiffUtil 对比新旧列表看到内容相同，就不会重绑那一行，
        //结果数据库和内存都是新标题，界面上却一直显示旧标题。
        Notes source = sharedRepo.getNotesLiveData().getValue();
        if (source == null) {
            if (onSaved != null) {
                onSaved.run();
            }
            return;
        }
        final Notes notes = new Notes(source.getUserId(), source.getGroupId(), source.getNotesId(),
                title, content, editedTime);
        final boolean isNew = notes.getNotesId() == -1;
        final long userId = notes.getUserId();
        final long groupId = notes.getGroupId();
        final String noteTitle = notes.getTitle();
        final String noteContent = notes.getContent();
        repo.runAsync(() -> {
            long newNotesId = -1;
            if (isNew) {//从创建笔记的按钮来到这个fragment
                newNotesId = repo.createNote(userId, groupId, noteTitle, noteContent, editedTime);
            } else {//从某个分组中来到这个fragment
                repo.updateNotes(notes);
            }
            //写完顺手把当前分组的列表读回来，避免调两次 runAsync
            return new SaveResult(newNotesId, repo.getNotesList(userId, groupId));
        }, result -> {
            if (!isAdded()) {
                return;
            }
            if (!result.isSuccess()) {
                //写库失败就停在本页，别让用户以为已经保存
                ToastUtil.showOperationFailed(requireContext());
                return;
            }
            SaveResult saved = result.getData();
            if (saved == null) {
                return;
            }
            if (saved.notesId > 0) {
                //新建成功：行号记在这份副本上，本页再点保存就走更新，不会重复建一条
                notes.setNotesId(saved.notesId);
            }
            //用这份独立副本替换 notesLiveData 的引用，之后的保存/返回对话框读到的都不再是列表里的对象
            sharedRepo.setNotes(notes);
            //刷新当前分组的笔记列表并通知仓库观察者
            sharedRepo.setNotesList(saved.notes);
            if (onSaved != null) {
                onSaved.run();
            }
        });
    }

    /** runAsync 的返回包：新建出来的 notesId + 刷新后的列表 */
    private static class SaveResult {
        final long notesId;
        final List<Notes> notes;

        SaveResult(long notesId, List<Notes> notes) {
            this.notesId = notesId;
            this.notes = notes;
        }
    }

    private void backAlertDialog() {
        BackAlertDialogBinding backAlertDialogBinding = BackAlertDialogBinding.inflate(getLayoutInflater());
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        AlertDialog alertDialog = builder.setView(backAlertDialogBinding.getRoot()).create();
        activeDialog = alertDialog;
        //取消-->直接返回，数据不发生更改
        backAlertDialogBinding.backAlertDialogCancel.setOnClickListener(v -> {
            alertDialog.dismiss();
            activeDialog = null;
            requireActivity().finish();
        });
        //保存-->返回，数据发生更改
        backAlertDialogBinding.backAlertDialogSave.setOnClickListener(v -> {
            //1.都为空
            if (title.isEmpty() && content.isEmpty()) {//返回选择保存时标题内容都为空
                //等效删除：删掉数据库里的记录
                Notes notes = sharedRepo.getNotesLiveData().getValue();
                if (notes != null) {
                    long notesUserId = notes.getUserId();
                    long notesGroupId = notes.getGroupId();
                    long notesId = notes.getNotesId();
                    alertDialog.dismiss();
                    activeDialog = null;
                    repo.runAsync(() -> repo.deleteNotes(notesUserId, notesGroupId, notesId), result -> {
                        if (!isAdded()) {
                            return;
                        }
                        if (!result.isSuccess()) {
                            ToastUtil.showOperationFailed(requireContext());
                            return;
                        }
                        sharedRepo.setNotesList(result.getData());
                        sharedRepo.setNotes(new Notes(-1, -1, -1, "", "", -1));
                        requireActivity().finish();
                    });
                    return;
                }
            }
            //2.都不为空
            //3.有标题没内容
            alertDialog.dismiss();
            activeDialog = null;
            //保存完再退出，避免页面先关、写入还没落盘
            SaveNotesData(() -> {
                if (isAdded()) {
                    requireActivity().finish();
                }
            });
        });
        alertDialog.show();
    }

    private void deleteNotesAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        DeleteAlertDialogBinding deleteAlertDialog = DeleteAlertDialogBinding.inflate(getLayoutInflater());
        AlertDialog alertDialog = builder.setView(deleteAlertDialog.getRoot()).create();
        deleteAlertDialog.customizeAlertDialogTitle.setText(R.string.tip);
        deleteAlertDialog.customizeAlertDialogMessage.setText(R.string.delete_note_message);
        activeDialog = alertDialog;
        //取消
        deleteAlertDialog.customizeAlertDialogCancel.setOnClickListener(v -> {
            alertDialog.dismiss();
            activeDialog = null;
        });
        //确定删除
        deleteAlertDialog.customizeAlertDialogConfirm.setOnClickListener(v -> {
            //删除在数据库中的记录
            //创建处删除
            //某个分组中的笔记删除
            Notes notes = sharedRepo.getNotesLiveData().getValue();
            alertDialog.dismiss();
            activeDialog = null;
            if (notes == null) {
                requireActivity().finish();
                return;
            }
            long notesUserId = notes.getUserId();
            long notesGroupId = notes.getGroupId();
            long notesId = notes.getNotesId();
            repo.runAsync(() -> repo.deleteNotes(notesUserId, notesGroupId, notesId), result -> {
                if (!isAdded()) {
                    return;
                }
                if (!result.isSuccess()) {
                    ToastUtil.showOperationFailed(requireContext());
                    return;
                }
                sharedRepo.setNotesList(result.getData());
                sharedRepo.setNotes(new Notes(-1, -1, -1, "", "", -1));
                requireActivity().finish();
            });
        });
        alertDialog.show();
    }


    private void init() {
        binding = FragmentEditNoteBinding.inflate(getLayoutInflater());
        sharedRepo = SharedDataRepository.getInstance();
        viewModel = new ViewModelProvider(this).get(MyViewModel.class);
        //共享仓储：全进程只有一条数据库连接，用完不需要关闭
        repo = MyRepository.getInstance(requireContext());
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        //确保对话框被dismiss，防止leaked window
        if (activeDialog != null && activeDialog.isShowing()) {
            activeDialog.dismiss();
            activeDialog = null;
        }
        //连接由 MyRepository 单例统一持有，这里不关闭、也不置空：
        //同一条连接可能正被别的页面使用，而且后台线程可能还在用它
        binding = null;
    }
}
