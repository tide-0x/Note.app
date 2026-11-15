package com.iteam.examproject.feature.editor;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
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
import android.widget.EditText;

import com.iteam.examproject.R;
import com.iteam.examproject.core.ToastUtil;
import com.iteam.examproject.data.model.Notes;
import com.iteam.examproject.databinding.BackAlertDialogBinding;
import com.iteam.examproject.databinding.DeleteAlertDialogBinding;
import com.iteam.examproject.databinding.FragmentEditNoteBinding;
import com.iteam.examproject.feature.editor.EditNoteViewModel.BackDecision;
import com.iteam.examproject.feature.editor.EditNoteViewModel.SaveDecision;

public class EditNoteFragment extends Fragment {
    private FragmentEditNoteBinding binding;
    private EditNoteViewModel viewModel;
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

        //用初始内容铺输入框（旋转屏幕重建时 ViewModel 还在，铺的是同一份）
        Notes source = viewModel.getSource();
        if (source.getTitle() != null && !source.getTitle().isEmpty()) {
            binding.noteTitleEdit.setText(source.getTitle());
        }
        if (source.getContent() != null && !source.getContent().isEmpty()) {
            binding.noteContentEdit.setText(source.getContent());
        }

        //写库结果统一在这里收尾：回传给列表页、提示、关页面
        viewModel.getOutcomes().observe(getViewLifecycleOwner(), event -> {
            EditNoteViewModel.EditOutcome outcome = event.getContentIfNotHandled();
            if (outcome == null) {
                return;
            }
            switch (outcome.kind) {
                case SAVED_NEW:
                case SAVED_UPDATED:
                    if (isAdded()) {
                        ToastUtil.showToast(requireContext(), R.string.save_success);
                    }
                    finishWithResult(outcome.kind == EditNoteViewModel.EditOutcome.Kind.SAVED_NEW
                            ? EditNoteActivity.OUTCOME_INSERTED : EditNoteActivity.OUTCOME_UPDATED, outcome.note);
                    break;
                case DELETED:
                    finishWithResult(EditNoteActivity.OUTCOME_DELETED, outcome.note);
                    break;
                case FAILED:
                default:
                    if (isAdded()) {
                        ToastUtil.showOperationFailed(requireContext());
                    }
                    break;
            }
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
                //新建笔记处点开，没有进行保存
                if (viewModel.isBrandNew()) {
                    requireActivity().finish();
                } else {//标题和内容至少有一个不为空
                    deleteNotesAlertDialog();
                }

            } else if (itemId == R.id.save) {
                //保存笔记：怎么处理由 VM 的状态说了算
                String title = binding.noteTitleEdit.getText().toString();
                String content = binding.noteContentEdit.getText().toString();
                SaveDecision decision = viewModel.decideSave(title, content);
                switch (decision) {
                    case DELETE_INTENT:
                        //标题内容都为空->变相删除笔记
                        viewModel.delete();
                        break;
                    case TITLE_EMPTY:
                        //有内容但没标题
                        ToastUtil.showToast(requireContext(), R.string.title_empty);
                        break;
                    case PROCEED:
                        viewModel.save(title, content);
                        break;
                    case NOTHING_CHANGED_EMPTY:
                        ToastUtil.showToast(requireContext(), R.string.nothing_to_save);
                        break;
                    case NOTHING_CHANGED:
                    default:
                        ToastUtil.showToast(requireContext(), R.string.content_saved);
                        break;
                }
            }
            return true;
        });

        return binding.getRoot();
    }

    /** 把结果回传给启动编辑页的列表页，然后关页面 */
    private void finishWithResult(String outcome, Notes note) {
        Intent result = EditNoteActivity.buildResult(outcome, note);
        requireActivity().setResult(android.app.Activity.RESULT_OK, result);
        requireActivity().finish();
    }

    /**
     * 返回前的统一处理：怎么处理由 VM 的状态说了算，View 照着做。
     */
    private void handleBack() {
        String title = binding.noteTitleEdit.getText().toString();
        String content = binding.noteContentEdit.getText().toString();

        BackDecision decision = viewModel.decideBack(title, content);
        switch (decision) {
            case STAY_TITLE_EMPTY:
                //有内容但标题为空
                ToastUtil.showToast(requireContext(), R.string.title_empty);
                break;
            case ASK_SAVE:
                //弹出对话框处理保存/取消，注意这里直接返回，不在此处 finish
                backAlertDialog();
                break;
            case EXIT:
            default:
                requireActivity().finish();
                break;
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
            String title = binding.noteTitleEdit.getText().toString();
            String content = binding.noteContentEdit.getText().toString();
            alertDialog.dismiss();
            activeDialog = null;
            //1.都为空：返回选择保存时标题内容都为空，等效删除
            //2.都不为空
            //3.有标题没内容
            SaveDecision decision = viewModel.decideSave(title, content);
            if (decision == SaveDecision.DELETE_INTENT) {
                viewModel.delete();
            } else {
                //保存完再退出，避免页面先关、写入还没落盘（关页面在 outcome 回调里做）
                viewModel.save(title, content);
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
        deleteAlertDialog.customizeAlertDialogCancel.setOnClickListener(v -> {
            alertDialog.dismiss();
            activeDialog = null;
        });
        //确定删除
        deleteAlertDialog.customizeAlertDialogConfirm.setOnClickListener(v -> {
            alertDialog.dismiss();
            activeDialog = null;
            viewModel.delete();
        });
        alertDialog.show();
    }


    private void init() {
        binding = FragmentEditNoteBinding.inflate(getLayoutInflater());
        //状态在 EditNoteActivity 级的 ViewModel 里，旋转屏幕不丢
        viewModel = new ViewModelProvider(requireActivity()).get(EditNoteViewModel.class);
        //要编辑哪篇笔记由启动方的 Intent 传入
        viewModel.start(EditNoteActivity.readNote(requireActivity().getIntent()));
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        //确保对话框被dismiss，防止leaked window
        if (activeDialog != null && activeDialog.isShowing()) {
            activeDialog.dismiss();
            activeDialog = null;
        }
        binding = null;
    }
}
