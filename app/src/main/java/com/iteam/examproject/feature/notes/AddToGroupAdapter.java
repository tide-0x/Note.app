package com.iteam.examproject.feature.notes;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.iteam.examproject.R;
import com.iteam.examproject.data.model.NoteGroup;

import java.util.ArrayList;
import java.util.List;

public class AddToGroupAdapter extends RecyclerView.Adapter<AddToGroupAdapter.ViewHolder> {
    private final List<NoteGroup> noteGroups = new ArrayList<>();
    private OnItemClickListener onItemClickListener;

    public AddToGroupAdapter() {
    }

    public AddToGroupAdapter(List<NoteGroup> noteGroups) {
        if (noteGroups != null) {
            this.noteGroups.addAll(noteGroups);
        }
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public AddToGroupAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.bottom_dialog_rv_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AddToGroupAdapter.ViewHolder holder, int position) {
        holder.textView.setText(noteGroups.get(position).getGroupName());
        holder.itemView.setOnClickListener(v -> {
            int currentPosition = holder.getBindingAdapterPosition();
            if (onItemClickListener != null && currentPosition != RecyclerView.NO_POSITION) {
                onItemClickListener.onClick(currentPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return noteGroups.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.rv_item_text);
        }
    }

    public interface OnItemClickListener {
        void onClick(int position);
    }
}
