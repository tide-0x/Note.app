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

public class BottomDialogSearchAdapter extends RecyclerView.Adapter<BottomDialogSearchAdapter.ViewHolder> {
    private final List<Notes> notes = new ArrayList<>();
    public BottomDialogSearchAdapter(){}
    public BottomDialogSearchAdapter(List<Notes> notes){
        if (notes != null) {
            this.notes.addAll(notes);
        }
    }

    private OnBottomDialogSearchClickListener listener;
    public void setListener(OnBottomDialogSearchClickListener listener){
        this.listener=listener;
    }

    /**
     * 搜索结果同样走 DiffUtil，避免整表重绘。
     */
    public void updateSearchResults(List<Notes> newNotes){
        if (newNotes == null || newNotes == notes) {
            return;
        }
        final List<Notes> oldNotes = new ArrayList<>(notes);
        final List<Notes> target = new ArrayList<>(newNotes);
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
                long oldId = oldNotes.get(oldItemPosition).getNotesId();
                long newId = target.get(newItemPosition).getNotesId();
                if (oldId > 0 && newId > 0) {
                    return oldId == newId;
                }
                return TextUtils.equals(oldNotes.get(oldItemPosition).getTitle(),
                        target.get(newItemPosition).getTitle());
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Notes oldItem = oldNotes.get(oldItemPosition);
                Notes newItem = target.get(newItemPosition);
                return oldItem.getNotesId() == newItem.getNotesId()
                        && TextUtils.equals(oldItem.getTitle(), newItem.getTitle());
            }
        });
        notes.clear();
        notes.addAll(target);
        result.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public BottomDialogSearchAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(parent.getContext()).inflate(R.layout.bottom_dialog_rv_item,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BottomDialogSearchAdapter.ViewHolder holder, int position) {
        holder.textView.setText(notes.get(position).getTitle());
        if(listener!=null){
            holder.itemView.setOnClickListener(v -> {
                int currentPosition = holder.getBindingAdapterPosition();
                if (currentPosition != RecyclerView.NO_POSITION) {
                    listener.onClick(currentPosition);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        if(notes!=null){
            return notes.size();
        }
        return 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        TextView textView;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView=itemView.findViewById(R.id.rv_item_text);
        }
    }

    public interface OnBottomDialogSearchClickListener{
        void onClick(int position);
    }
}
