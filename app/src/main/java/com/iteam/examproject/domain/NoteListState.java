package com.iteam.examproject.domain;

import com.iteam.examproject.data.model.NoteGroup;
import com.iteam.examproject.data.model.Notes;

import java.util.ArrayList;
import java.util.List;

/**
 * 笔记列表页的状态机：列表 + 排序方式，纯 Java、不碰 Android。
 *
 * <p>所有变更都返回新的 {@link NoteListState}（不可变），绝不原地改。
 * MainViewModel 持有当前状态，变更后把新状态交给 LiveData；
 * 排序、插入、替换、删除的细活委托给 {@link NoteListOps}。
 *
 * <p>做成纯类而不是直接写在 ViewModel 里，是为了能在普通 JVM 上做单元测试。
 */
public final class NoteListState {

    /**
     * 排序方向。字面量和 SQL 的 asc / desc 一致，但定义在这里：排序是列表自己的事，
     * 数据层不该替界面决定「升序叫什么」，界面层也不该为了一个常量去 import 数据层。
     */
    public static final String SORT_ASC = "asc";
    public static final String SORT_DESC = "desc";

    private final List<Notes> notes;
    private final String sortWay;

    public NoteListState() {
        this(new ArrayList<>(), SORT_DESC);
    }

    private NoteListState(List<Notes> notes, String sortWay) {
        this.notes = notes;
        this.sortWay = sortWay;
    }

    public String getSortWay() {
        return sortWay;
    }

    /** 只读视图；要往 LiveData 里放请用副本，别让外面改到内部状态 */
    public List<Notes> getNotes() {
        return new ArrayList<>(notes);
    }

    /**
     * 一次变更的结果：新状态 + 是否真的改了。
     * applied 为 false 表示这条变更与本列表无关（认不出、不属于这个分组），界面不用动。
     */
    public static final class Mutation {
        public final NoteListState state;
        public final boolean applied;

        private Mutation(NoteListState state, boolean applied) {
            this.state = state;
            this.applied = applied;
        }

        static Mutation changed(NoteListState state) {
            return new Mutation(state, true);
        }

        static Mutation unchanged(NoteListState state) {
            return new Mutation(state, false);
        }
    }

    /** 整份替换（登录、切换分组后重新查库），并按当前排序方式排好 */
    public Mutation replaceAll(List<Notes> fresh) {
        List<Notes> sorted = NoteListOps.sorted(fresh, isAscending());
        return Mutation.changed(new NoteListState(sorted, sortWay));
    }

    /** 切换排序方式：只重排内存里已有的列表 */
    public Mutation sortBy(String newSortWay) {
        if (newSortWay == null || newSortWay.equals(sortWay)) {
            return Mutation.unchanged(this);
        }
        List<Notes> sorted = NoteListOps.sorted(notes, SORT_ASC.equals(newSortWay));
        return Mutation.changed(new NoteListState(sorted, newSortWay));
    }

    /**
     * 新建笔记写库成功：插到正确位置。
     * 笔记不属于当前显示的分组（分组信息已知时）就不动——
     * 比如在分组 A 的列表里，插进来的却是刚在「全部笔记」建的。
     */
    public Mutation insert(Notes note, NoteGroup currentGroup) {
        if (note == null || !belongsToCurrentGroup(note, currentGroup)) {
            return Mutation.unchanged(this);
        }
        List<Notes> next = NoteListOps.inserted(notes, note, isAscending());
        return Mutation.changed(new NoteListState(next, sortWay));
    }

    /** 编辑笔记写库成功：按 notesId 原位替换并重排；认不出这条时不变更 */
    public Mutation update(Notes note) {
        List<Notes> next = note == null ? null : NoteListOps.replaced(notes, note, isAscending());
        if (next == null) {
            return Mutation.unchanged(this);
        }
        return Mutation.changed(new NoteListState(next, sortWay));
    }

    /** 删除笔记写库成功：摘掉这一条；认不出这条时不变更 */
    public Mutation delete(Notes note) {
        List<Notes> next = note == null ? null : NoteListOps.removed(notes, note);
        if (next == null) {
            return Mutation.unchanged(this);
        }
        return Mutation.changed(new NoteListState(next, sortWay));
    }

    /** 分组还是登录前的占位值（userId 为 -1）时无从判断，按「属于」处理 */
    private static boolean belongsToCurrentGroup(Notes note, NoteGroup currentGroup) {
        if (currentGroup == null || currentGroup.getUserId() <= 0) {
            return true;
        }
        return currentGroup.getUserId() == note.getUserId() && currentGroup.getGroupId() == note.getGroupId();
    }

    private boolean isAscending() {
        return SORT_ASC.equals(sortWay);
    }
}
