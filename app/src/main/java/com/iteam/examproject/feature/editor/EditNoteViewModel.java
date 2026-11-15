package com.iteam.examproject.feature.editor;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.iteam.examproject.core.Event;
import com.iteam.examproject.data.MyRepository;
import com.iteam.examproject.data.model.Notes;

/**
 * 编辑笔记页的 ViewModel：持有表单状态（原标题/内容、有没有改过）和「这个动作该怎么处理」的判断，
 * 写库也在这里编排。View 只负责把输入框内容递进来、按决策弹对话框和提示。
 *
 * <p>状态只在这里，不放进任何全局单例：编辑页要编辑哪篇笔记由 Intent 传入（见
 * {@link EditNoteActivity}），结果通过 Activity Result 回传给列表页。
 */
public class EditNoteViewModel extends AndroidViewModel {

    /** 写库操作的结果，View 观察后决定提示、回传结果、关页面 */
    public static final class EditOutcome {
        public enum Kind { SAVED_NEW, SAVED_UPDATED, DELETED, FAILED }

        public final Kind kind;
        public final Notes note;

        EditOutcome(Kind kind, Notes note) {
            this.kind = kind;
            this.note = note;
        }
    }

    /** 对「点了保存」的裁决，View 按它决定弹什么、提示什么 */
    public enum SaveDecision {
        /** 标题内容都被清空 = 变相删除 */
        DELETE_INTENT,
        /** 有内容但没标题 */
        TITLE_EMPTY,
        /** 正常保存 */
        PROCEED,
        /** 新建页什么都没写就点了保存 */
        NOTHING_CHANGED_EMPTY,
        /** 内容和上次保存时一致 */
        NOTHING_CHANGED
    }

    /** 对「按了返回」的裁决 */
    public enum BackDecision {
        /** 有内容但标题为空，提示后留在本页 */
        STAY_TITLE_EMPTY,
        /** 有未保存的修改，弹对话框问用户 */
        ASK_SAVE,
        /** 没有未保存的修改，直接退出 */
        EXIT
    }

    private final MutableLiveData<Event<EditOutcome>> outcomes = new MutableLiveData<>();

    private MyRepository repo;
    // 进入编辑页时那篇笔记的副本；与列表页不共享对象，怎么改都不会互相波及
    private Notes source = new Notes(-1, -1, -1, "", "", -1);
    private String originalTitle = "";
    private String originalContent = "";
    private boolean initialized = false;

    public EditNoteViewModel(Application application) {
        super(application);
        repo = MyRepository.getInstance(application);
    }

    /**
     * 用 Intent 传进来的笔记初始化。只在第一次调用时生效：
     * 旋转屏幕会重新走 onCreateView，但 ViewModel 还活着，不能把用户已保存的进度冲掉。
     */
    public void start(Notes fromIntent) {
        if (initialized) {
            return;
        }
        initialized = true;
        source = fromIntent == null ? new Notes(-1, -1, -1, "", "", -1) : fromIntent;
        originalTitle = source.getTitle() == null ? "" : source.getTitle();
        originalContent = source.getContent() == null ? "" : source.getContent();
    }

    /** 传入的笔记本身（初始标题/内容用它铺输入框） */
    public Notes getSource() {
        return source;
    }

    /** 标题内容都是空的 = 从「新建」入口进来还没保存过，点删除不用弹对话框 */
    public boolean isBrandNew() {
        return originalTitle.isEmpty() && originalContent.isEmpty();
    }

    public LiveData<Event<EditOutcome>> getOutcomes() {
        return outcomes;
    }

    /**
     * 裁决「点了保存」：怎么处理由状态说了算，View 照着做。
     * 这里全是纯判断，可以直接单元测试。
     */
    public SaveDecision decideSave(String title, String content) {
        boolean changed = !title.equals(originalTitle) || !content.equals(originalContent);
        if (!changed) {
            return title.isEmpty() && content.isEmpty() ? SaveDecision.NOTHING_CHANGED_EMPTY : SaveDecision.NOTHING_CHANGED;
        }
        if (title.isEmpty() && content.isEmpty()) {
            return SaveDecision.DELETE_INTENT;
        }
        if (title.isEmpty()) {
            return SaveDecision.TITLE_EMPTY;
        }
        return SaveDecision.PROCEED;
    }

    /** 裁决「按了返回」：留、问、走 */
    public BackDecision decideBack(String title, String content) {
        boolean changed = !title.equals(originalTitle) || !content.equals(originalContent);
        if (!changed) {
            return BackDecision.EXIT;
        }
        if (title.isEmpty() && !content.isEmpty()) {
            return BackDecision.STAY_TITLE_EMPTY;
        }
        return BackDecision.ASK_SAVE;
    }

    /**
     * 保存：新建走 createNote，已存在的走 updateNotes。
     * 只有写库<b>成功</b>才更新「上次保存的内容」——失败的话界面仍处于「有改动」状态，用户可以直接再存一次。
     */
    public void save(String title, String content) {
        long editedTime = System.currentTimeMillis();
        Notes notes = new Notes(source.getUserId(), source.getGroupId(), source.getNotesId(),
                title, content, editedTime);
        final boolean isNew = notes.getNotesId() == -1;
        repo.runAsync(() -> {
            if (isNew) {
                // 新建只需要插入行号，别的笔记都没动，不用把列表查回来
                long notesId = repo.createNote(notes.getUserId(), notes.getGroupId(),
                        notes.getTitle(), notes.getContent(), notes.getEditTime());
                notes.setNotesId(notesId);
            } else {
                repo.updateNotes(notes);
            }
            return notes;
        }, result -> {
            if (!result.isSuccess()) {
                // 写库失败就停在本页，别让用户以为已经保存
                outcomes.setValue(new Event<>(new EditOutcome(EditOutcome.Kind.FAILED, null)));
                return;
            }
            Notes saved = result.getData();
            originalTitle = saved.getTitle();
            originalContent = saved.getContent();
            outcomes.setValue(new Event<>(new EditOutcome(
                    isNew ? EditOutcome.Kind.SAVED_NEW : EditOutcome.Kind.SAVED_UPDATED, saved)));
        });
    }

    /** 删除当前这篇（含「标题内容清空」的变相删除） */
    public void delete() {
        repo.runAsync(() -> {
            repo.deleteNotes(source.getUserId(), source.getGroupId(), source.getNotesId());
            return source;
        }, result -> {
            if (!result.isSuccess()) {
                outcomes.setValue(new Event<>(new EditOutcome(EditOutcome.Kind.FAILED, null)));
                return;
            }
            outcomes.setValue(new Event<>(new EditOutcome(EditOutcome.Kind.DELETED, result.getData())));
        });
    }
}
