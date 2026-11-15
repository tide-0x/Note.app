package com.iteam.examproject.domain;

import com.iteam.examproject.data.model.Notes;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * NoteListOps 的单元测试：只跑在 JVM 上，不需要模拟器。
 * 覆盖「写库后只改一条，不整表重查」这条路径依赖的排序/插入/替换/删除行为。
 */
public class NoteListOpsTest {

    /** 编辑时间 100 < 200 < 300，notesId 与之一一对应 */
    private static Notes note(long notesId, long editTime) {
        return new Notes(1, 0, notesId, "标题" + notesId, "内容" + notesId, editTime);
    }

    private static Notes note(long groupId, long notesId, long editTime) {
        return new Notes(1, groupId, notesId, "标题" + notesId, "内容" + notesId, editTime);
    }

    private static List<Notes> list(Notes... items) {
        return new ArrayList<>(Arrays.asList(items));
    }

    private static List<Long> ids(List<Notes> notes) {
        List<Long> ids = new ArrayList<>();
        for (Notes item : notes) {
            ids.add(item.getNotesId());
        }
        return ids;
    }

    @Test
    public void sorted_desc_putsNewestFirst() {
        List<Notes> result = NoteListOps.sorted(list(note(1, 100), note(3, 300), note(2, 200)), false);
        assertEquals(Arrays.asList(3L, 2L, 1L), ids(result));
    }

    @Test
    public void sorted_asc_putsOldestFirst() {
        List<Notes> result = NoteListOps.sorted(list(note(3, 300), note(1, 100), note(2, 200)), true);
        assertEquals(Arrays.asList(1L, 2L, 3L), ids(result));
    }

    @Test
    public void sorted_sameEditTime_isStableByNotesId() {
        // 同一毫秒保存的笔记：升序按 notesId 从小到大，降序反过来，两次结果必须一致
        List<Notes> asc = NoteListOps.sorted(list(note(9, 500), note(4, 500)), true);
        List<Notes> desc = NoteListOps.sorted(list(note(9, 500), note(4, 500)), false);
        assertEquals(Arrays.asList(4L, 9L), ids(asc));
        assertEquals(Arrays.asList(9L, 4L), ids(desc));
    }

    @Test
    public void inserted_desc_putsTheNewestOnTop() {
        List<Notes> current = list(note(1, 100), note(2, 200));
        List<Notes> result = NoteListOps.inserted(current, note(3, 300), false);
        assertEquals(Arrays.asList(3L, 2L, 1L), ids(result));
        // 不改动传进来的列表
        assertEquals(Arrays.asList(1L, 2L), ids(current));
    }

    @Test
    public void inserted_asc_appendsTheNewestAtTheEnd() {
        List<Notes> current = list(note(1, 100), note(2, 200));
        List<Notes> result = NoteListOps.inserted(current, note(3, 300), true);
        assertEquals(Arrays.asList(1L, 2L, 3L), ids(result));
    }

    @Test
    public void inserted_intoMiddle_ofOlderNotes() {
        // 补录一条比较旧的笔记：降序下应该落在 300 和 100 之间
        List<Notes> current = list(note(1, 100), note(3, 300));
        List<Notes> result = NoteListOps.inserted(current, note(2, 200), false);
        assertEquals(Arrays.asList(3L, 2L, 1L), ids(result));
    }

    @Test
    public void replaced_updatesFieldsAndMovesToTop() {
        Notes old = new Notes(1, 7, 2, "旧标题", "旧内容", 200);
        List<Notes> current = list(note(3, 300), old, note(1, 100));
        Notes edited = new Notes(1, 7, 2, "新标题", "新内容", 400);

        List<Notes> result = NoteListOps.replaced(current, edited, false);

        assertNotNull(result);
        assertEquals(Arrays.asList(2L, 3L, 1L), ids(result));
        assertEquals("新标题", result.get(0).getTitle());
        assertEquals("新内容", result.get(0).getContent());
        assertEquals(400, result.get(0).getEditTime());
        // 原列表里那条还是旧值，说明改的是副本
        assertEquals("旧标题", old.getTitle());
    }

    @Test
    public void replaced_keepsTheGroupOfTheListEntry() {
        // 从搜索结果点进编辑页时，那条笔记的 groupId 固定是 0；
        // 当前列表是某个分组（groupId=7）的，替换后分组不能被改掉
        Notes inGroupList = new Notes(1, 7, 2, "旧标题", "旧内容", 200);
        List<Notes> current = list(inGroupList);
        Notes fromSearch = new Notes(1, 0, 2, "新标题", "新内容", 400);

        List<Notes> result = NoteListOps.replaced(current, fromSearch, false);

        assertNotNull(result);
        assertEquals(7, result.get(0).getGroupId());
        assertEquals(1, result.get(0).getUserId());
    }

    @Test
    public void replaced_returnsNull_whenNotInList() {
        List<Notes> current = list(note(1, 100));
        assertNull(NoteListOps.replaced(current, note(99, 900), false));
    }

    @Test
    public void removed_takesOutOnlyTheTarget() {
        // 列表本来就是排好序的（降序），删掉一条不用重新排序
        List<Notes> current = list(note(3, 300), note(2, 200), note(1, 100));
        List<Notes> result = NoteListOps.removed(current, note(2, 200));
        assertNotNull(result);
        assertEquals(Arrays.asList(3L, 1L), ids(result));
        assertEquals(3, current.size());
    }

    @Test
    public void removed_returnsNull_whenNotInList() {
        assertNull(NoteListOps.removed(list(note(1, 100)), note(42, 400)));
    }

    @Test
    public void emptyAndNullInputs_areSafe() {
        assertEquals(0, NoteListOps.sorted(null, false).size());
        assertEquals(0, NoteListOps.sorted(new ArrayList<>(), false).size());
        assertEquals(-1, NoteListOps.indexOf(null, note(1, 100)));
        assertNull(NoteListOps.removed(null, note(1, 100)));
        // 空列表插入一条也能正常排
        assertEquals(1, NoteListOps.inserted(null, note(1, 100), false).size());
    }

    @Test
    public void legacyNotesWithoutId_areMatchedForDelete() {
        // 早期版本写入的数据 notesId 是 0，只能按「编辑时间 + 标题」认。
        // 删除时传的是没改过的那条，两个条件都还成立，所以能认出来
        Notes legacyA = new Notes(1, 0, 0, "A", "内容A", 100);
        Notes legacyB = new Notes(1, 0, 0, "B", "内容B", 200);
        List<Notes> current = list(legacyB, legacyA);

        List<Notes> result = NoteListOps.removed(current, new Notes(1, 0, 0, "B", "内容B", 200));

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("A", result.get(0).getTitle());
    }

    @Test
    public void legacyNoteEdited_failsToMatch_soCallerReloads() {
        // 保存会顺带改掉编辑时间，历史数据就对不上了：这里必须返回 null，
        // 由调用方退回「整表重查」，不能悄悄不动界面
        Notes legacy = new Notes(1, 0, 0, "B", "内容B", 200);
        Notes edited = new Notes(1, 0, 0, "B", "改过的内容", 300);
        assertNull(NoteListOps.replaced(list(legacy), edited, false));
    }

    @Test
    public void inserted_keepsGroupIdOfTheNote() {
        // 新建笔记时 groupId 由列表所属分组给出，插入后要原样保留
        List<Notes> result = NoteListOps.inserted(new ArrayList<>(), note(7, 5, 100), false);
        assertEquals(7, result.get(0).getGroupId());
    }
}
