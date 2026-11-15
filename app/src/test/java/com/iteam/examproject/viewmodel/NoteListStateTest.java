package com.iteam.examproject.viewmodel;

import com.iteam.examproject.entity.NoteGroup;
import com.iteam.examproject.entity.Notes;
import com.iteam.examproject.repo.MyRepository;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * NoteListState 的单元测试：只跑在 JVM 上，不需要模拟器。
 * 覆盖列表页状态机的排序切换、整份替换、增量插入/替换/删除。
 */
public class NoteListStateTest {

    private static Notes note(long groupId, long notesId, long editTime) {
        return new Notes(1, groupId, notesId, "标题" + notesId, "内容" + notesId, editTime);
    }

    private static List<Long> ids(List<Notes> notes) {
        return notes.stream().map(Notes::getNotesId).collect(Collectors.toList());
    }

    private static NoteGroup group(long userId, long groupId) {
        return new NoteGroup(userId, groupId, "组" + groupId);
    }

    @Test
    public void replaceAll_keepsCurrentSortWay() {
        NoteListState state = new NoteListState().sortBy(MyRepository.SORT_ASC).state;
        NoteListState.Mutation mutation = state.replaceAll(Arrays.asList(
                note(0, 3, 300), note(0, 1, 100)));
        assertTrue(mutation.applied);
        assertEquals(MyRepository.SORT_ASC, mutation.state.getSortWay());
        assertEquals(Arrays.asList(1L, 3L), ids(mutation.state.getNotes()));
    }

    @Test
    public void sortBy_switchesOrder_andPersists() {
        NoteListState initial = new NoteListState().replaceAll(Arrays.asList(
                note(0, 1, 100), note(0, 2, 200))).state;
        NoteListState.Mutation mutation = initial.sortBy(MyRepository.SORT_ASC);
        assertTrue(mutation.applied);
        assertEquals(Arrays.asList(1L, 2L), ids(mutation.state.getNotes()));
        assertEquals(MyRepository.SORT_ASC, mutation.state.getSortWay());
        // 再切回降序：顺序反转，设置也跟着回去
        NoteListState.Mutation back = mutation.state.sortBy(MyRepository.SORT_DESC);
        assertEquals(Arrays.asList(2L, 1L), ids(back.state.getNotes()));
        assertEquals(MyRepository.SORT_DESC, back.state.getSortWay());
    }

    @Test
    public void sortBy_sameOrNullOrNoChange_reportsUnapplied() {
        NoteListState initial = new NoteListState();
        assertFalse(initial.sortBy(MyRepository.SORT_DESC).applied);
        assertFalse(initial.sortBy(null).applied);
    }

    @Test
    public void insert_putsNewestOnTop_inDescOrder() {
        NoteListState state = new NoteListState().replaceAll(Arrays.asList(
                note(0, 1, 100), note(0, 2, 200))).state;
        NoteListState.Mutation mutation = state.insert(note(0, 3, 300), group(1, 0));
        assertTrue(mutation.applied);
        assertEquals(Arrays.asList(3L, 2L, 1L), ids(mutation.state.getNotes()));
    }

    @Test
    public void insert_skipsNoteFromAnotherGroup() {
        NoteListState state = new NoteListState().replaceAll(new ArrayList<>()).state;
        // 当前分组是 (1, 0)，插进来的笔记却属于分组 7 → 不动
        NoteListState.Mutation mutation = state.insert(note(7, 3, 300), group(1, 0));
        assertFalse(mutation.applied);
        assertTrue(mutation.state.getNotes().isEmpty());
    }

    @Test
    public void insert_allowsWhenGroupUnknown() {
        // 分组还是登录前的占位值（userId=-1）时不做判断
        NoteListState state = new NoteListState();
        assertTrue(state.insert(note(7, 3, 300), group(-1, 0)).applied);
    }

    @Test
    public void update_replacesAndMovesToTop() {
        NoteListState state = new NoteListState().replaceAll(Arrays.asList(
                note(0, 1, 100), note(0, 2, 200))).state;
        Notes edited = new Notes(1, 0, 1, "新标题", "新内容", 400);
        NoteListState.Mutation mutation = state.update(edited);
        assertTrue(mutation.applied);
        assertEquals(Arrays.asList(1L, 2L), ids(mutation.state.getNotes()));
        assertEquals("新标题", mutation.state.getNotes().get(0).getTitle());
    }

    @Test
    public void update_unknownNote_reportsUnapplied() {
        NoteListState state = new NoteListState().replaceAll(Arrays.asList(note(0, 1, 100))).state;
        assertFalse(state.update(note(0, 42, 900)).applied);
    }

    @Test
    public void delete_removesTheNote() {
        NoteListState state = new NoteListState().replaceAll(Arrays.asList(
                note(0, 1, 100), note(0, 2, 200))).state;
        NoteListState.Mutation mutation = state.delete(note(0, 2, 200));
        assertTrue(mutation.applied);
        assertEquals(Arrays.asList(1L), ids(mutation.state.getNotes()));
    }

    @Test
    public void delete_unknownNote_reportsUnapplied() {
        NoteListState state = new NoteListState().replaceAll(new ArrayList<>()).state;
        assertFalse(state.delete(note(0, 9, 900)).applied);
    }

    @Test
    public void mutationDoesNotTouchOriginalState() {
        NoteListState initial = new NoteListState().replaceAll(Arrays.asList(note(0, 1, 100))).state;
        NoteListState.Mutation mutation = initial.insert(note(0, 2, 200), group(1, 0));
        assertEquals(1, initial.getNotes().size());
        assertEquals(2, mutation.state.getNotes().size());
    }
}
