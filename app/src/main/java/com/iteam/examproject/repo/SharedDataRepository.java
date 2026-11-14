package com.iteam.examproject.repo;

import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.iteam.examproject.entity.NoteGroup;
import com.iteam.examproject.entity.Notes;
import com.iteam.examproject.entity.User;

import java.util.ArrayList;
import java.util.List;

/**
 * 单例数据仓库，持有应用级的 LiveData，供不同 Activity/Fragment 订阅。
 *
 * <p>写入统一走 {@link #setSafely(MutableLiveData, Object)}：
 * 主线程调用时仍是同步的 setValue，页面之间「先写后读」的约定完全不变
 * （例如 NoteFragment 先 setNotes 再启动 EditNoteActivity，后者靠 getValue 取值）；
 * 子线程调用时自动降级为 postValue，不会抛 IllegalStateException。
 *
 * <p>注意：从子线程写入后 {@code getValue()} 不会立刻变，要等主线程派发完。
 * 跨线程写之后不能紧接着同步读，读要靠 observe 回调。
 */
public class SharedDataRepository {
    private static final SharedDataRepository INSTANCE = new SharedDataRepository();

    private final MutableLiveData<User> userLiveData = new MutableLiveData<>(new User(-1, ""));
    //登录前的占位分组，名称为空，界面显示时会兜底成「全部笔记」
    private final MutableLiveData<NoteGroup> noteGroupLiveData = new MutableLiveData<>(new NoteGroup(-1, 0, ""));
    private final MutableLiveData<List<Notes>> notesListLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Notes> notesLiveData = new MutableLiveData<>(new Notes(-1, -1, -1, "", "", -1));
    private final MutableLiveData<Boolean> changedUserLiveData=new MutableLiveData<>(false);

    private SharedDataRepository() {}

    public static SharedDataRepository getInstance() {
        return INSTANCE;
    }

    /**
     * 线程安全的 LiveData 写入：主线程 setValue 同步生效，子线程 postValue 切回主线程派发。
     */
    private static <T> void setSafely(MutableLiveData<T> liveData, T value) {
        if (Looper.myLooper() != null && Looper.myLooper() == Looper.getMainLooper()) {
            liveData.setValue(value);
        } else {
            liveData.postValue(value);
        }
    }

    // User
    public LiveData<User> getUserLiveData() {
        return userLiveData;
    }
    public void setUser(User user) {
        setSafely(userLiveData, user);
    }

    // NoteGroup
    public LiveData<NoteGroup> getNoteGroupLiveData() {
        return noteGroupLiveData;
    }
    public void setNoteGroup(NoteGroup noteGroup) {
        setSafely(noteGroupLiveData, noteGroup);
    }

    // Notes list
    public LiveData<List<Notes>> getNotesListLiveData() {
        return notesListLiveData;
    }
    public void setNotesList(List<Notes> notes) {
        setSafely(notesListLiveData, notes);
    }

    // Single Notes
    public LiveData<Notes> getNotesLiveData() {
        return notesLiveData;
    }
    public void setNotes(Notes notes) {
        setSafely(notesLiveData, notes);
    }

    // ChangedUser
    public void setChangedUser(boolean changedUser){
        setSafely(changedUserLiveData, changedUser);
    }
    public LiveData<Boolean> getChangedUserLiveData(){
        return changedUserLiveData;
    }
}
