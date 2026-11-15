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
 *
 * <p>笔记列表（{@link #notesListLiveData}）有一条额外约定：它<b>永远</b>按 {@link #sortWay}
 * 排好序。整份替换（登录、切换分组）和增量修改（新建、编辑、删除笔记）都走这个约定，
 * 所以界面层看到的就是最终顺序，不需要再查一次库。
 */
public class SharedDataRepository {
    private static final SharedDataRepository INSTANCE = new SharedDataRepository();

    private final MutableLiveData<User> userLiveData = new MutableLiveData<>(new User(-1, ""));
    //登录前的占位分组，名称为空，界面显示时会兜底成「全部笔记」
    private final MutableLiveData<NoteGroup> noteGroupLiveData = new MutableLiveData<>(new NoteGroup(-1, 0, ""));
    private final MutableLiveData<List<Notes>> notesListLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Notes> notesLiveData = new MutableLiveData<>(new Notes(-1, -1, -1, "", "", -1));
    private final MutableLiveData<Boolean> changedUserLiveData=new MutableLiveData<>(false);
    //当前列表的排序方式，与 notesListLiveData 的顺序始终保持一致
    private String sortWay = MyRepository.SORT_DESC;

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

    /**
     * 整份替换列表（登录、切换分组等场景）。
     * 写进去之前统一按当前排序方式排一遍，保证「列表顺序」和 {@link #sortWay} 不会各排各的。
     */
    public void setNotesList(List<Notes> notes) {
        setSafely(notesListLiveData, NoteListOps.sorted(notes, isAscending()));
    }

    /** 当前的排序方式，取值是 {@link MyRepository#SORT_ASC} / {@link MyRepository#SORT_DESC} */
    public String getSortWay() {
        return sortWay;
    }

    /**
     * 切换排序方式：只重排内存里已有的列表，不再回数据库重查。
     * 每条笔记的编辑时间都在列表里，排序用不到数据库里的其他信息。
     */
    public void setSortWay(String sortWay) {
        if (sortWay == null || sortWay.equals(this.sortWay)) {
            return;
        }
        this.sortWay = sortWay;
        setNotesList(notesListLiveData.getValue());
    }

    private boolean isAscending() {
        return MyRepository.SORT_ASC.equals(sortWay);
    }

    /**
     * 新建笔记写库成功后调用：只把这一条插到内存列表的正确位置。
     *
     * <p>不再调 {@code getNotesList} 把整张表查回来——表里其他笔记一条都没变。
     * 笔记数量越多，省下的这一趟越明显。
     */
    public void applyNoteInserted(Notes note) {
        if (note == null || !isInCurrentGroup(note)) {
            return;
        }
        setSafely(notesListLiveData, NoteListOps.inserted(notesListLiveData.getValue(), note, isAscending()));
    }

    /**
     * 修改笔记写库成功后调用：按 notesId 就地替换那一条，并按新的编辑时间重新排位。
     *
     * @return {@code false} 表示列表里认不出这一条（早期数据没有 notesId，标题和编辑时间也变了），
     * 界面这时没被改到，调用方应退回 {@code getNotesList} 整表重查
     */
    public boolean applyNoteUpdated(Notes note) {
        if (note == null) {
            return false;
        }
        List<Notes> next = NoteListOps.replaced(notesListLiveData.getValue(), note, isAscending());
        if (next == null) {
            return false;
        }
        setSafely(notesListLiveData, next);
        return true;
    }

    /**
     * 删除笔记写库成功后调用：把这一条从内存列表里摘掉。
     *
     * @return {@code false} 表示列表里认不出这一条，调用方应退回整表重查
     */
    public boolean applyNoteDeleted(Notes note) {
        if (note == null) {
            return false;
        }
        List<Notes> next = NoteListOps.removed(notesListLiveData.getValue(), note);
        if (next == null) {
            return false;
        }
        setSafely(notesListLiveData, next);
        return true;
    }

    /**
     * 新建的这条笔记是不是属于当前显示的分组。
     * 分组还是登录前的占位值（userId 为 -1）时无从判断，按「是」处理，不拦。
     */
    private boolean isInCurrentGroup(Notes note) {
        NoteGroup current = noteGroupLiveData.getValue();
        if (current == null || current.getUserId() <= 0) {
            return true;
        }
        return current.getUserId() == note.getUserId() && current.getGroupId() == note.getGroupId();
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
