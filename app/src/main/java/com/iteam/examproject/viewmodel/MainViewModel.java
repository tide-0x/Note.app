package com.iteam.examproject.viewmodel;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.iteam.examproject.R;
import com.iteam.examproject.entity.NoteGroup;
import com.iteam.examproject.entity.Notes;
import com.iteam.examproject.entity.User;
import com.iteam.examproject.repo.MyRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * ContentActivity 级别的 ViewModel：会话（当前用户）、当前分组、笔记列表、排序方式。
 * NoteFragment（列表）和 GroupFragment（分组管理）都从 {@code ViewModelProvider(requireActivity())}
 * 拿同一个实例，替代原来进程级 SharedDataRepository 单例——状态跟着 Activity 走，
 * Activity 销毁（退出登录）状态就没了，不再有跨任务的残留。
 *
 * <p>职责边界：数据库访问、线程调度、列表状态机都在这里；View 只做渲染、输入校验、
 * 对话框和 Toast，不允许直接拿 {@link MyRepository}。
 *
 * <p>一次性动作（提示、跳登录页）用 {@link Event} 包装，避免旋转屏幕后重放。
 */
public class MainViewModel extends AndroidViewModel {

    /** 一次性提示（string 资源 id），View 观察后弹 Toast */
    private final MutableLiveData<Event<Integer>> messages = new MutableLiveData<>();
    /** 会话失效（注销/退出/无登录态），View 观察后回登录页 */
    private final MutableLiveData<Event<Boolean>> sessionExpired = new MutableLiveData<>();

    private final MutableLiveData<User> user = new MutableLiveData<>();
    // 登录前的占位分组，名称为空，View 显示时会兜底成「全部笔记」
    private final MutableLiveData<NoteGroup> currentGroup = new MutableLiveData<>(new NoteGroup(-1, 0, ""));
    private final MutableLiveData<List<Notes>> notesList = new MutableLiveData<>(new ArrayList<>());

    private MyRepository repo;
    private NoteListState state = new NoteListState();

    public MainViewModel(Application application) {
        super(application);
        repo = MyRepository.getInstance(application);
    }

    // ---- 会话 ----

    /**
     * 登录成功后由 ContentActivity 调用。userId 无效时尝试读本地记住的登录态，
     * 再不行就发 sessionExpired 让 View 回登录页。
     */
    public void start(long userId, String account) {
        User current = user.getValue();
        if (current != null && current.getUserId() == userId && userId > 0) {
            // 配置变更（旋转屏幕）会重新走 onCreate，别重复初始化、重复查库
            return;
        }
        if (userId <= 0) {
            //Intent 里没有有效的登录态，退回本地记住的那份
            android.content.SharedPreferences spf =
                    getApplication().getSharedPreferences("currentUser", android.content.Context.MODE_PRIVATE);
            userId = spf.getLong("userId", -1);
            account = userId > 0 ? spf.getString("account", "") : "";
        }
        if (userId <= 0) {
            sessionExpired.setValue(new Event<>(true));
            return;
        }
        User session = new User(userId, account);
        user.setValue(session);
        switchGroup(new NoteGroup(userId, 0, getApplication().getString(R.string.all_notes)));
    }

    /** 退出/注销账号：清掉本地登录态并要求回登录页 */
    public void logout() {
        clearSession();
        sessionExpired.setValue(new Event<>(true));
    }

    /** 注销账号：删库成功才清登录态，失败让用户再试 */
    public void cancelAccount() {
        User current = user.getValue();
        if (current == null) {
            return;
        }
        long userId = current.getUserId();
        repo.runAsync(() -> {
                    repo.cancelUser(userId);
                    return null;
                }, result -> {
                    if (!result.isSuccess()) {
                        messages.setValue(new Event<>(R.string.operation_failed));
                        return;
                    }
                    // 注销失败就别清本地登录态，否则账号还留着却登不回来
                    clearSession();
                    sessionExpired.setValue(new Event<>(true));
                });
    }

    /** 修改密码：成功/失败都只发一条提示 */
    public void updatePassword(String changedPassword) {
        User current = user.getValue();
        if (current == null) {
            return;
        }
        repo.runAsync(() -> {
                    repo.updatePassword(current.getUserId(), changedPassword);
                    return null;
                }, result -> {
                    if (result.isSuccess()) {
                        messages.setValue(new Event<>(R.string.password_updated));
                    } else {
                        messages.setValue(new Event<>(R.string.operation_failed));
                    }
                });
    }

    private void clearSession() {
        getApplication().getSharedPreferences("currentUser", android.content.Context.MODE_PRIVATE)
                .edit()
                .putLong("userId", -1)
                .putString("account", "")
                //把旧版本残留的明文密码一并清掉
                .remove("password")
                .apply();
        user.setValue(null);
    }

    // ---- 当前分组与笔记列表 ----

    public LiveData<User> getUser() {
        return user;
    }

    public LiveData<NoteGroup> getCurrentGroup() {
        return currentGroup;
    }

    public LiveData<List<Notes>> getNotesList() {
        return notesList;
    }

    public LiveData<Event<Integer>> getMessages() {
        return messages;
    }

    public LiveData<Event<Boolean>> getSessionExpired() {
        return sessionExpired;
    }

    public String getSortWay() {
        return state.getSortWay();
    }

    /** 切换分组：换掉当前分组并重新查库 */
    public void switchGroup(NoteGroup group) {
        if (group == null) {
            return;
        }
        currentGroup.setValue(group);
        loadNotes();
    }

    /** 按编辑时间重排当前列表。列表里每条笔记都带着编辑时间，直接在内存里排 */
    public void sortBy(String sortWay) {
        NoteListState.Mutation mutation = state.sortBy(sortWay);
        if (mutation.applied) {
            applyState(mutation.state);
        }
    }

    /** 整份重新查库（写操作后的兜底路径） */
    public void reloadNotes() {
        loadNotes();
    }

    /**
     * 新建笔记写库成功（编辑页回传结果）：只把这一条插进内存列表。
     * 笔记数量越大，省下的这次全表重查越明显。
     */
    public void insertNote(Notes note) {
        NoteListState.Mutation mutation = state.insert(note, currentGroup.getValue());
        if (mutation.applied) {
            applyState(mutation.state);
        }
    }

    /** 编辑笔记写库成功（编辑页回传结果）：按 notesId 原位替换；认不出就退回整表重查 */
    public void updateNote(Notes note) {
        NoteListState.Mutation mutation = state.update(note);
        if (mutation.applied) {
            applyState(mutation.state);
        } else {
            loadNotes();
        }
    }

    /** 删除笔记写库成功（编辑页回传结果）：摘掉这一条；认不出就退回整表重查 */
    public void deleteNote(Notes note) {
        NoteListState.Mutation mutation = state.delete(note);
        if (mutation.applied) {
            applyState(mutation.state);
        } else {
            loadNotes();
        }
    }

    // ---- 一次性命令（对话框用的数据，结果走回调，不走 LiveData 避免旋转后重放） ----

    /** 查当前账号的全部分组，给「选择分组」「添加到分组」两个对话框用 */
    public void loadGroups(ValueCallback<List<NoteGroup>> onLoaded, Runnable onError) {
        long userId = currentUserId();
        if (userId <= 0) {
            return;
        }
        repo.runAsync(() -> repo.getAllGroups(userId), result -> {
            if (!result.isSuccess() || result.getData() == null) {
                if (onError != null) {
                    onError.run();
                }
                return;
            }
            if (onLoaded != null) {
                onLoaded.onValue(result.getData());
            }
        });
    }

    /** 把笔记加入某个分组；onDone 的 Boolean 为 true 表示添加成功，false 表示分组里已有 */
    public void addToGroup(Notes note, ValueCallback<Boolean> onDone, Runnable onError) {
        repo.runAsync(() -> repo.addNotesToGroup(note), result -> {
            if (!result.isSuccess() || result.getData() == null) {
                if (onError != null) {
                    onError.run();
                }
                return;
            }
            if (onDone != null) {
                onDone.onValue(Boolean.TRUE.equals(result.getData()));
            }
        });
    }

    /** 按标题模糊搜索（只搜 groupId=0 的「全部笔记」记录） */
    public void searchNotes(String keyWord, ValueCallback<List<Notes>> onLoaded, Runnable onError) {
        User current = user.getValue();
        if (current == null) {
            return;
        }
        repo.runAsync(() -> repo.searchResult(current.getUserId(), keyWord), result -> {
            if (!result.isSuccess() || result.getData() == null) {
                if (onError != null) {
                    onError.run();
                }
                return;
            }
            if (onLoaded != null) {
                onLoaded.onValue(result.getData());
            }
        });
    }

    // ---- 内部 ----

    private void loadNotes() {
        NoteGroup group = currentGroup.getValue();
        if (group == null || group.getUserId() <= 0) {
            return;
        }
        repo.runAsync(() -> repo.getNotesList(group.getUserId(), group.getGroupId()), result -> {
            if (!result.isSuccess()) {
                // 失败时保留原列表，不要把空列表当成「没有笔记」刷出去
                messages.setValue(new Event<>(R.string.operation_failed));
                return;
            }
            applyState(state.replaceAll(result.getData()).state);
        });
    }

    private void applyState(NoteListState newState) {
        state = newState;
        notesList.setValue(state.getNotes());
    }

    private long currentUserId() {
        User current = user.getValue();
        return current == null ? -1 : current.getUserId();
    }

    /** 命令式回调：结果保证在主线程 */
    public interface ValueCallback<T> {
        void onValue(T value);
    }
}
