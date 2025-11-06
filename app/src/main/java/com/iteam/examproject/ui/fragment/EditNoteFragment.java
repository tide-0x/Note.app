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
        contentEdit.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });

        viewModel.getNotesLiveData().observe(getViewLifecycleOwner(), new Observer<Notes>() {
            @Override
            public void onChanged(Notes notes) {
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
                    //没有编辑，直接返回
                    startActivity(new Intent(requireActivity(), ContentActivity.class));
                    return;
                }
                //其余情况
                long editedTime = System.currentTimeMillis();
                //在全部笔记中还是在其他分组中
                //NoteFragment中viewmodel数据进行了更新
                Notes notes1 = viewModel.getNotesLiveData().getValue();
                if (notes1 != null) {
                    notes1.setEditTime(editedTime);
                    if (notes1.getNotesId() == -1) {//从创建笔记的按钮来到这个fragment
                        repo.createNote(notes1.getUserId(), notes1.getGroupId(), title, content, editedTime);
                    } else {//从某个分组中来到这个fragment
                        //有无编辑都会覆盖
                        repo.updateNotes(notes1);
                    }
                    startActivity(new Intent(requireContext(), ContentActivity.class));
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
                    if (title.isEmpty() && content.isEmpty()) {
                        startActivity(new Intent(requireContext(), ContentActivity.class));
                    } else {//标题和内容至少有一个不为空
                        deleteNotesAlertDialog();
                    }

                } else if (itemId == R.id.save) {
                    //保存笔记
                    title = binding.noteTitleEdit.getText().toString();
                    content = binding.noteContentEdit.getText().toString();
                    if (title.isEmpty() && content.isEmpty()) {
                        startActivity(new Intent(requireContext(), ContentActivity.class));
                        //todo:是否需要手动消费事件
                        return true;
                    } else if (!content.isEmpty() && title.isEmpty()) {
                        //有内容但没标题
                        //ToastUtil.showToast(requireContext(),"标题不能为空");
                        Toast.makeText(requireContext(), "标题不能为空", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    Notes notes = viewModel.getNotesLiveData().getValue();
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
                            viewModel.setNotesLiveData(notes);
                        }
                        //ToastUtil.showToast(requireContext(),"保存成功");
                        Toast.makeText(requireContext(), "保存成功", Toast.LENGTH_SHORT).show();
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
                Notes notes = viewModel.getNotesLiveData().getValue();
                if (notes != null) {
                    List<Notes> notesList = repo.deleteNotes(notes.getUserId(), notes.getGroupId(), notes.getNotesId());
                    viewModel.setNotesListLiveData(notesList);
                    startActivity(new Intent(requireContext(), ContentActivity.class));
                }

                alertDialog.dismiss();
            }
        });
        alertDialog.show();
    }

    private void init() {
        binding = FragmentEditNoteBinding.inflate(getLayoutInflater());
        viewModel = new ViewModelProvider(this).get(MyViewModel.class);
        repo = new MyRepository(requireContext());
        repo.open();


    }
}