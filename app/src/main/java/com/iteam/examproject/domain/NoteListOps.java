package com.iteam.examproject.domain;

import com.iteam.examproject.data.model.Notes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 笔记列表的纯内存操作：排序、插入、替换、删除。
 * 每个方法都返回<b>新的</b>列表，不改动传进来的那一份。
 *
 * <p>存在的意义：写库成功之后不必再把整张表查回来。写操作本来就知道自己动了哪一条，
 * 直接在内存列表上做增量修改就够了，省掉一次全表 select + 逐行构造对象。
 *
 * <p>这里不依赖 Android、也不碰数据库，可以用普通 JVM 单元测试覆盖
 * （见 {@code src/test/java/.../domain/NoteListOpsTest.java}）。
 */
public final class NoteListOps {

    private NoteListOps() {
    }

    /**
     * 升序基准：按编辑时间从小到大；时间相同的（同一毫秒保存的）再按 notesId 排。
     * 加上 notesId 这个次级条件是为了让排序结果稳定——顺序不确定的话，
     * DiffUtil 会把「本来没动、只是位置换了」的行也判成变化。
     */
    private static final Comparator<Notes> ASC_ORDER = (a, b) -> {
        int byTime = Long.compare(a.getEditTime(), b.getEditTime());
        if (byTime != 0) {
            return byTime;
        }
        return Long.compare(a.getNotesId(), b.getNotesId());
    };

    /** 按方向取比较器：降序就是升序反过来 */
    public static Comparator<Notes> order(boolean ascending) {
        return ascending ? ASC_ORDER : Collections.reverseOrder(ASC_ORDER);
    }

    /** 排序，返回新列表 */
    public static List<Notes> sorted(List<Notes> source, boolean ascending) {
        List<Notes> copy = copyOf(source);
        Collections.sort(copy, order(ascending));
        return copy;
    }

    /** 插入一条后重新排序，返回新列表 */
    public static List<Notes> inserted(List<Notes> source, Notes note, boolean ascending) {
        List<Notes> copy = copyOf(source);
        // 存副本：传进来的那条同时被编辑页拿着，共用同一个对象迟早会互相改到
        copy.add(copyOf(note));
        Collections.sort(copy, order(ascending));
        return copy;
    }

    /**
     * 按 notesId 替换掉列表里的那一条，并按新的编辑时间重新排位。
     * 列表里没有这条时返回 {@code null}，表示不用动界面（例如这条属于别的分组）。
     */
    public static List<Notes> replaced(List<Notes> source, Notes note, boolean ascending) {
        int index = indexOf(source, note);
        if (index < 0) {
            return null;
        }
        List<Notes> copy = copyOf(source);
        Notes old = copy.get(index);
        // 列表里这条记录的身份（userId / groupId）由列表自己决定，只接受来自编辑页的
        // 标题、内容、编辑时间。编辑页那条可能是从搜索结果点进来的，groupId 固定是 0，
        // 直接整条覆盖会把分组视图里的一行改成「全部笔记」的记录。
        copy.set(index, new Notes(old.getUserId(), old.getGroupId(), note.getNotesId(),
                note.getTitle(), note.getContent(), note.getEditTime()));
        Collections.sort(copy, order(ascending));
        return copy;
    }

    /** 删掉列表里的这一条，返回新列表；列表里没有这条时返回 {@code null} */
    public static List<Notes> removed(List<Notes> source, Notes note) {
        int index = indexOf(source, note);
        if (index < 0) {
            return null;
        }
        List<Notes> copy = copyOf(source);
        copy.remove(index);
        return copy;
    }

    /**
     * 找这条笔记在列表里的下标，找不到返回 -1。
     * 判定规则与 {@link com.iteam.examproject.adapter.NoteAdapter} 的 DiffUtil 回调对齐：
     * 有 notesId 就用 notesId，没有才退化成「编辑时间 + 标题」。两边不一致的话，
     * 会出现「适配器认为换了、列表认为没换」的错位。
     */
    public static int indexOf(List<Notes> source, Notes note) {
        if (source == null || note == null) {
            return -1;
        }
        long notesId = note.getNotesId();
        if (notesId > 0) {
            for (int i = 0; i < source.size(); i++) {
                if (source.get(i).getNotesId() == notesId) {
                    return i;
                }
            }
            return -1;
        }
        //notesId 为 0 的历史数据没有可用的主键，退化成「编辑时间 + 标题」认一条。
        //这种匹配只在「标题和时间都没变」时成立（例如直接删除），编辑过之后就认不出来，
        //调用方会收到 null 并退回整表重查
        for (int i = 0; i < source.size(); i++) {
            Notes item = source.get(i);
            if (item.getNotesId() <= 0
                    && item.getEditTime() == note.getEditTime()
                    && Objects.equals(item.getTitle(), note.getTitle())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 逐字段复制一份列表。
     * 列表对象会和适配器、界面层共享，复制是为了让它们各持一份，谁改都不会波及别人。
     */
    private static List<Notes> copyOf(List<Notes> source) {
        if (source == null) {
            return new ArrayList<>();
        }
        ArrayList<Notes> copy = new ArrayList<>(source.size() + 1);
        for (Notes item : source) {
            copy.add(copyOf(item));
        }
        return copy;
    }

    /** 逐字段复制一条笔记 */
    private static Notes copyOf(Notes item) {
        return new Notes(item.getUserId(), item.getGroupId(), item.getNotesId(),
                item.getTitle(), item.getContent(), item.getEditTime());
    }
}
