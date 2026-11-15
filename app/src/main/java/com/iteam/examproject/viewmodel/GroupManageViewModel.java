package com.iteam.examproject.viewmodel;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.iteam.examproject.R;
import com.iteam.examproject.entity.NoteGroup;
import com.iteam.examproject.entity.User;
import com.iteam.examproject.repo.MyRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * 分组管理页的 ViewModel：分组列表状态 + 新建/删除/重命名的数据库编排。
 * 只在 GroupFragment 的作用域内（ViewModelProvider(this)），页面销毁状态跟着走。
 */
public class GroupManageViewModel extends AndroidViewModel {

    /** 分组操作结果，View 观察后提示、更新列表、处理「删的是当前分组」 */
    public static final class GroupOutcome {
        public enum Kind { ADDED, DUPLICATE_ADD, DELETED, RENAME_OK, DUPLICATE_RENAME, FAILED }

        public final Kind kind;
        /** ADDED / DELETED 时携带该分组，其余为 null */
        public final NoteGroup group;

        GroupOutcome(Kind kind, NoteGroup group) {
            this.kind = kind;
            this.group = group;
        }
    }

    /** 重命名对话框需要同步的结果（true = 改名失败，对话框保持编辑态） */
    public interface RenameCallback {
        void onResult(boolean failed);
    }

    private final MutableLiveData<List<NoteGroup>> groups = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Event<GroupOutcome>> outcomes = new MutableLiveData<>();

    private MyRepository repo;
    private long userId = -1;

    public GroupManageViewModel(Application application) {
        super(application);
        repo = MyRepository.getInstance(application);
    }

    /** 由 View 把当前登录用户递进来（用户信息在 MainViewModel 里） */
    public void setUser(User user) {
        userId = user == null ? -1 : user.getUserId();
    }

    public LiveData<List<NoteGroup>> getGroups() {
        return groups;
    }

    public LiveData<Event<GroupOutcome>> getOutcomes() {
        return outcomes;
    }

    /** 查当前账号的全部分组，结果写进 groups */
    public void load() {
        if (userId <= 0) {
            return;
        }
        repo.runAsync(() -> repo.getAllGroups(userId), result -> {
            if (!result.isSuccess() || result.getData() == null) {
                return;
            }
            groups.setValue(result.getData());
        });
    }

    /** 新建分组；组名重复时 outcome 是 DUPLICATE_ADD，不改列表 */
    public void addGroup(String groupName) {
        if (userId <= 0) {
            return;
        }
        repo.runAsync(() -> repo.createGroup(userId, groupName), result -> {
            if (!result.isSuccess()) {
                outcomes.setValue(new Event<>(new GroupOutcome(GroupOutcome.Kind.FAILED, null)));
                return;
            }
            NoteGroup created = result.getData();
            if (created == null) {
                //成功但没返回分组 = 组名重复
                outcomes.setValue(new Event<>(new GroupOutcome(GroupOutcome.Kind.DUPLICATE_ADD, null)));
                return;
            }
            List<NoteGroup> next = new ArrayList<>(groups.getValue() == null ? new ArrayList<>() : groups.getValue());
            next.add(created);
            groups.setValue(next);
            outcomes.setValue(new Event<>(new GroupOutcome(GroupOutcome.Kind.ADDED, created)));
        });
    }

    /** 删除分组（连带删组内笔记，库里的事）；结果用事件通知 */
    public void deleteGroup(NoteGroup noteGroup) {
        if (noteGroup == null || userId <= 0) {
            return;
        }
        repo.runAsync(() -> {
            repo.deleteGroup(noteGroup);
            return noteGroup;
        }, result -> {
            if (!result.isSuccess()) {
                outcomes.setValue(new Event<>(new GroupOutcome(GroupOutcome.Kind.FAILED, null)));
                return;
            }
            List<NoteGroup> current = groups.getValue() == null ? new ArrayList<>() : groups.getValue();
            List<NoteGroup> next = new ArrayList<>(current);
            int index = indexOf(next, noteGroup);
            if (index >= 0) {
                next.remove(index);
            }
            groups.setValue(next);
            outcomes.setValue(new Event<>(new GroupOutcome(GroupOutcome.Kind.DELETED, result.getData())));
        });
    }

    /**
     * 重命名分组。对话框需要「失败/重名就保持编辑态」的同步结果，所以单独给回调；
     * 同时发事件让 View 提示、刷新列表。
     */
    public void renameGroup(NoteGroup target, RenameCallback callback) {
        if (target == null || userId <= 0) {
            if (callback != null) {
                callback.onResult(true);
            }
            return;
        }
        repo.runAsync(() -> repo.updateGroupName(target), result -> {
            if (!result.isSuccess()) {
                outcomes.setValue(new Event<>(new GroupOutcome(GroupOutcome.Kind.FAILED, null)));
                if (callback != null) {
                    callback.onResult(true);
                }
                return;
            }
            //结果用装箱类型接，避免 null 自动拆箱崩溃
            boolean duplicate = Boolean.TRUE.equals(result.getData());
            if (duplicate) {
                outcomes.setValue(new Event<>(new GroupOutcome(GroupOutcome.Kind.DUPLICATE_RENAME, null)));
            } else {
                List<NoteGroup> current = groups.getValue() == null ? new ArrayList<>() : groups.getValue();
                List<NoteGroup> next = new ArrayList<>(current);
                int index = indexOf(next, target);
                if (index >= 0) {
                    NoteGroup old = next.get(index);
                    next.set(index, new NoteGroup(old.getUserId(), old.getGroupId(), target.getGroupName()));
                }
                groups.setValue(next);
                outcomes.setValue(new Event<>(new GroupOutcome(GroupOutcome.Kind.RENAME_OK, null)));
            }
            if (callback != null) {
                callback.onResult(duplicate);
            }
        });
    }

    private static int indexOf(List<NoteGroup> list, NoteGroup target) {
        for (int i = 0; i < list.size(); i++) {
            NoteGroup item = list.get(i);
            if (item.getGroupId() == target.getGroupId() && item.getUserId() == target.getUserId()) {
                return i;
            }
        }
        return -1;
    }
}
