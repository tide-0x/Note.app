package com.iteam.examproject.ui.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
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

        contentEdit.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.getParent().requestDisallowInterceptTouchEvent(true);
                //当 EditText 嵌套在另一个可滚动容器中时（如 ScrollView、RecyclerView），会发生滚动冲突：
                //用户想在 EditText 内部滚动文本时，可能意外触发外部容器的滚动
                //或者反过来，导致滚动体验不流畅
                //解决方案：
                //告诉父容器："不要拦截触摸事件，让EditText自己处理"
                //当用户触摸 EditText 时，确保所有的滚动操作都由 EditText 自己处理
                //防止父容器"抢走"触摸事件
                return false;//返回值 false 表示触摸事件会继续传递给 EditText 的默认处理
            }
        });

        viewModel.getNotesLiveData().observe(getViewLifecycleOwner(), new Observer<Notes>() {
            @Override
            public void onChanged(Notes notes) {
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
            }
        });


        //返回键
        binding.editNoteToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
        });


        binding.editNoteToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
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
                            List<Notes> refreshed = repo.deleteNotes(viewModelNotes.getUserId(), viewModelNotes.getGroupId(), viewModelNotes.getNotesId());
                            sharedRepo.setNotes(new Notes(-1, -1, -1, "", "", -1));
                            viewModel.setNotesListLiveData(refreshed);
                            requireActivity().finish();
                            return true;
                        } else if (!content.isEmpty() && title.isEmpty()) {
                            //有内容但没标题
                            Toast.makeText(requireContext(), getString(R.string.title_empty), Toast.LENGTH_SHORT).show();
                            return true;
                        } else {//一般情况
                            //后续用户又点击保存后判断是否有变化
                            originalTitle = title;
                            originalContent = content;
                            SaveNotesData();
                            Toast.makeText(requireContext(), getString(R.string.save_success), Toast.LENGTH_SHORT).show();
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
            }
        });

        return binding.getRoot();
    }

    private void SaveNotesData() {
        long editedTime = System.currentTimeMillis();
        //在全部笔记中还是在其他分组中
        //NoteFragment中viewmodel数据进行了更新
        Notes notes = sharedRepo.getNotesLiveData().getValue();
        if (notes != null) {
            notes.setEditTime(editedTime);
            notes.setTitle(title);
            notes.setContent(content);
            if (notes.getNotesId() == -1) {//从创建笔记的按钮来到这个fragment
                long notesId = repo.createNote(notes.getUserId(), notes.getGroupId(), notes.getTitle(), notes.getContent(), editedTime);
                notes.setNotesId(notesId);
            } else {//从某个分组中来到这个fragment
                repo.updateNotes(notes);
            }
            //刷新当前分组的笔记列表并通知仓库观察者
            List<Notes> refreshed = repo.getNotesList(notes.getUserId(), notes.getGroupId());
            sharedRepo.setNotesList(refreshed);
        }
    }

    private void backAlertDialog() {
        BackAlertDialogBinding backAlertDialogBinding = BackAlertDialogBinding.inflate(getLayoutInflater());
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        AlertDialog alertDialog = builder.setView(backAlertDialogBinding.getRoot()).create();
        activeDialog = alertDialog;
        //取消-->直接返回，数据不发生更改
        backAlertDialogBinding.backAlertDialogCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
                activeDialog = null;
                requireActivity().finish();
            }
        });
        //保存-->返回，数据发生更改
        backAlertDialogBinding.backAlertDialogSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //1.都为空
                if (title.isEmpty() && content.isEmpty()) {//返回选择保存时标题内容都为空
                    //等效删除
                    //删除在数据库中的记录
                    //创建处删除
                    //某个分组中的笔记删除
                    Notes notes = sharedRepo.getNotesLiveData().getValue();
                    if (notes != null) {
                        List<Notes> notesList = repo.deleteNotes(notes.getUserId(), notes.getGroupId(), notes.getNotesId());
                        sharedRepo.setNotesList(notesList);
                        sharedRepo.setNotes(new Notes(-1, -1, -1, "", "", -1));
                        alertDialog.dismiss();
                        activeDialog = null;
                        requireActivity().finish();
                        return;
                    }
                }
                //2.都不为空
                //3.有标题没内容
                alertDialog.dismiss();
                activeDialog = null;
                SaveNotesData();
                requireActivity().finish();
            }
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
        deleteAlertDialog.customizeAlertDialogCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
                activeDialog = null;
            }
        });
        //确定删除
        deleteAlertDialog.customizeAlertDialogConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //删除在数据库中的记录
                //创建处删除
                //某个分组中的笔记删除
                Notes notes = sharedRepo.getNotesLiveData().getValue();
                if (notes != null) {
                    List<Notes> notesList = repo.deleteNotes(notes.getUserId(), notes.getGroupId(), notes.getNotesId());
                    sharedRepo.setNotesList(notesList);
                    sharedRepo.setNotes(new Notes(-1, -1, -1, "", "", -1));
                }
                alertDialog.dismiss();
                activeDialog = null;
                requireActivity().finish();
            }
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
        //连接由 MyRepository 单例统一持有，这里不再关闭：
        //同一条连接可能正被别的页面使用，关掉会让它们下一次查询直接抛 already-closed
        repo = null;
        binding = null;
    }
}