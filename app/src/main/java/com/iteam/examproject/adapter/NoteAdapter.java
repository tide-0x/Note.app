package com.iteam.examproject.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.iteam.examproject.R;
import com.iteam.examproject.entity.Notes;

import java.util.ArrayList;
import java.util.List;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.ViewHolder> {
    private final List<Notes> notes = new ArrayList<>();
    private NoteClickListener noteClickListener;
    private NoteLongClickListener noteLongClickListener;

    public NoteAdapter() {
    }

    public NoteAdapter(List<Notes> notes) {
        if (notes != null) {
            for (Notes item : notes) {
                this.notes.add(copyOf(item));
            }
        }
    }

    /**
     * 逐字段复制。适配器持有的必须是自己的独立数据：
     * 外部（比如编辑页）直接改列表传出去的对象时，这里不会被波及，
     * DiffUtil 才能正常比出新旧差异、刷新那一行。
     */
    private static Notes copyOf(Notes item) {
        return new Notes(item.getUserId(), item.getGroupId(), item.getNotesId(),
                item.getTitle(), item.getContent(), item.getEditTime());
    }

    /**
     * 用 DiffUtil 计算差异，只刷新真正变化的条目。
     * 直接 notifyDataSetChanged() 会让所有条目重新绑定，失去增删动画、正在编辑的输入框也会被打断。
     */
    public void updateNotes(List<Notes> newNotes) {
        if (newNotes == null || newNotes == notes) {
            return;
        }
        final List<Notes> oldNotes = new ArrayList<>(notes);
        final List<Notes> target = new ArrayList<>();
        for (Notes item : newNotes) {
            target.add(copyOf(item));
        }
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return oldNotes.size();
            }

            @Override
            public int getNewListSize() {
                return target.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                Notes oldItem = oldNotes.get(oldItemPosition);
                Notes newItem = target.get(newItemPosition);
                long oldId = oldItem.getNotesId();
                long newId = newItem.getNotesId();
                if (oldId > 0 && newId > 0) {
                    return oldId == newId;
                }
                //历史数据里 notesId 可能是 0，退化成按标题+编辑时间判断
                return oldItem.getEditTime() == newItem.getEditTime()
                        && TextUtils.equals(oldItem.getTitle(), newItem.getTitle());
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Notes oldItem = oldNotes.get(oldItemPosition);
                Notes newItem = target.get(newItemPosition);
                return oldItem.getNotesId() == newItem.getNotesId()
                        && oldItem.getEditTime() == newItem.getEditTime()
                        && TextUtils.equals(oldItem.getTitle(), newItem.getTitle())
                        && TextUtils.equals(oldItem.getContent(), newItem.getContent());
            }
        });
        notes.clear();
        notes.addAll(target);
        result.dispatchUpdatesTo(this);
    }

    public void setNoteClickListener(NoteClickListener noteClickListener) {
        this.noteClickListener = noteClickListener;
    }

    public void setNoteLongClickListener(NoteLongClickListener noteLongClickListener) {
        this.noteLongClickListener = noteLongClickListener;
    }

    @NonNull
    @Override
    public NoteAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.notes_title_rv_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteAdapter.ViewHolder holder, int position) {
        holder.textView.setText(notes.get(position).getTitle());
        //点击
        if (noteClickListener != null) {
            holder.itemView.setOnClickListener(v -> {
                int currentPosition = holder.getBindingAdapterPosition();
                if (currentPosition != RecyclerView.NO_POSITION) {
                    noteClickListener.onClick(currentPosition);
                }
            });
        }

        //长按
        holder.itemView.setOnLongClickListener(v -> {
            int currentPosition = holder.getBindingAdapterPosition();
            if (noteLongClickListener != null && currentPosition != RecyclerView.NO_POSITION) {
                noteLongClickListener.onLongClick(currentPosition);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        if(notes!=null){
            return notes.size();
        }
        return  0 ;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.notes_title_rv_item_text);
        }
    }


    public interface NoteClickListener {
        void onClick(int position);
    }

    public interface NoteLongClickListener {
        void onLongClick(int position);
    }
}
