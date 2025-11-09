package com.iteam.examproject.ui.fragment;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.EditText;
import android.widget.Toast;

import com.iteam.examproject.R;
import com.iteam.examproject.databinding.CustomizeAlertDialogBinding;
import com.iteam.examproject.databinding.FragmentEditNoteBinding;
import com.iteam.examproject.entity.NoteGroup;
import com.iteam.examproject.entity.Notes;
import com.iteam.examproject.repo.MyRepository;
import com.iteam.examproject.repo.SharedDataRepository;
import com.iteam.examproject.ui.activity.ContentActivity;
import com.iteam.examproject.ui.activity.LoginAndRegisterActivity;
import com.iteam.examproject.utils.ToastUtil;
import com.iteam.examproject.viewmodel.MyViewModel;

import java.util.List;
import java.util.Objects;

public class EditNoteFragment extends Fragment {
    private FragmentEditNoteBinding binding;
    private MyViewModel viewModel;
    private MyRepository repo;
    private String title;
    private String content;
    private SharedDataRepository sharedRepo;

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
                if(notes==null){
                    return;
                }
                if (!notes.getTitle().isEmpty()) {
                    binding.noteTitleEdit.setText(notes.getTitle());
                }
                if (!notes.getContent().isEmpty()) {
                    binding.noteContentEdit.setText(notes.getContent());
                }

            }
        });


        //返回键
        binding.editNoteToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                title = binding.noteTitleEdit.getText().toString();
                content = binding.noteContentEdit.getText().toString();
                if (title.isEmpty() && !content.isEmpty()) {//有内容但标题为空
                    //ToastUtil.showToast(requireContext(),"标题不能为空");
                    Toast.makeText(requireContext(), "标题不能为空", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (content.isEmpty() && title.isEmpty()) {
                    //没有编辑，清空临时notes并finish
                    sharedRepo.setNotes(new Notes(-1,-1,-1,"","",-1));
                    requireActivity().finish();
                    return;
                }
                //其余情况
                long editedTime = System.currentTimeMillis();
                //在全部笔记中还是在其他分组中
                //NoteFragment中viewmodel数据进行了更新
                Notes notes1 =sharedRepo.getNotesLiveData().getValue();
                if (notes1 != null) {
                    notes1.setEditTime(editedTime);
                    notes1.setTitle(title);
                    notes1.setContent(content);
                    if (notes1.getNotesId() == -1) {//从创建笔记的按钮来到这个fragment
                        long notesId=repo.createNote(notes1.getUserId(), notes1.getGroupId(), notes1.getTitle(), notes1.getContent(), editedTime);
                        notes1.setNotesId(notesId);
                    } else {//从某个分组中来到这个fragment
                        //有无编辑都会覆盖
                        repo.updateNotes(notes1);
                    }
                    List<Notes> refreshed=repo.getNotesList(notes1.getUserId(), notes1.getGroupId());
                    sharedRepo.setNotesList(refreshed);
                    //清空临时notes防止下次新建保留
                    sharedRepo.setNotes(new Notes(-1,-1,-1,"","",-1));
                    requireActivity().finish();
                }
            }
        });


        binding.editNoteToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.delete) {//删除笔记
                    title = binding.noteTitleEdit.getText().toString();
                    content = binding.noteContentEdit.getText().toString();
                    //点开了但标题和内容为空
                    //todo:全删干净了再按删除键
                    if (title.isEmpty() && content.isEmpty()) {
                        requireActivity().finish();
                    } else {//标题和内容至少有一个不为空
                        deleteNotesAlertDialog();
                    }

                } else if (itemId == R.id.save) {
                    //保存笔记：创建或更新后刷新列表，清空临时notes，finish
                    title = binding.noteTitleEdit.getText().toString();
                    content = binding.noteContentEdit.getText().toString();
                    if (title.isEmpty() && content.isEmpty()) {
                        sharedRepo.setNotes(new Notes(-1,-1,-1,"","",-1));
                        requireActivity().finish();
                        //todo:是否需要手动消费事件
                        return true;
                    } else if (!content.isEmpty() && title.isEmpty()) {
                        //有内容但没标题
                        //ToastUtil.showToast(requireContext(),"标题不能为空");
                        Toast.makeText(requireContext(), "标题不能为空", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    Notes notes = sharedRepo.getNotesLiveData().getValue();
                    long editedTime = System.currentTimeMillis();
                    if (notes != null) {
                        notes.setEditTime(editedTime);
                        notes.setTitle(title);
                        notes.setContent(content);
                        if (notes.getNotesId() == -1) {//从创建笔记处来到这个fragment
                            long notesId = repo.createNote(notes.getUserId(), notes.getGroupId(), notes.getTitle(), notes.getContent(), editedTime);
                            notes.setNotesId(notesId);
                        } else {//从某个分组中来到这个fragment
                            repo.updateNotes(notes);
                        }
                        //ToastUtil.showToast(requireContext(),"保存成功");
                        //刷新当前分组的笔记列表并通知仓库观察者
                        List<Notes> refreshed=repo.getNotesList(notes.getUserId(), notes.getGroupId());
                        sharedRepo.setNotesList(refreshed);
                        //清空临时notes防止下次新建保存旧内容
                        Toast.makeText(requireContext(), "保存成功", Toast.LENGTH_SHORT).show();
                        //requireActivity().finish();
                    }
                }
                return true;
            }
        });

        return binding.getRoot();
    }

    private void deleteNotesAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        CustomizeAlertDialogBinding customizeAlertDialogBinding = CustomizeAlertDialogBinding.inflate(getLayoutInflater());
        AlertDialog alertDialog = builder.setView(customizeAlertDialogBinding.getRoot()).create();
        customizeAlertDialogBinding.customizeAlertDialogTitle.setText("提示");
        customizeAlertDialogBinding.customizeAlertDialogMessage.setText("是否要删除该笔记");
        //取消
        customizeAlertDialogBinding.customizeAlertDialogCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });
        //确定删除
        customizeAlertDialogBinding.customizeAlertDialogConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //删除在数据库中的记录
                //创建处删除
                //某个分组中的笔记删除
                Notes notes = sharedRepo.getNotesLiveData().getValue();
                if (notes != null) {
                    List<Notes> notesList = repo.deleteNotes(notes.getUserId(), notes.getGroupId(), notes.getNotesId());
                    sharedRepo.setNotesList(notesList);
                    sharedRepo.setNotes(new Notes(-1,-1,-1,"","",-1));
                    requireActivity().finish();
                }

                alertDialog.dismiss();
            }
        });
        alertDialog.show();
    }

    private void init() {
        binding = FragmentEditNoteBinding.inflate(getLayoutInflater());
        sharedRepo=SharedDataRepository.getInstance();
        viewModel = new ViewModelProvider(this).get(MyViewModel.class);
        repo = new MyRepository(requireContext());
        repo.open();


    }
}