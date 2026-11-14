package com.iteam.examproject.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.iteam.examproject.R;
import com.iteam.examproject.entity.NoteGroup;

import java.util.ArrayList;
import java.util.List;

public class BottomDialogGroupAdapter extends RecyclerView.Adapter<BottomDialogGroupAdapter.ViewHolder> {
    private final List<NoteGroup> noteGroups = new ArrayList<>();
    private OnBottomDialogGroupClickListener listener;
    public BottomDialogGroupAdapter(){}
    public BottomDialogGroupAdapter(List<NoteGroup> noteGroups){
        if (noteGroups != null) {
            this.noteGroups.addAll(noteGroups);
        }
    }
    public void setListener(OnBottomDialogGroupClickListener listener){
        this.listener=listener;
    }
    @NonNull
    @Override
    public BottomDialogGroupAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(parent.getContext()).inflate(R.layout.bottom_dialog_rv_item,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BottomDialogGroupAdapter.ViewHolder holder, int position) {
        holder.textView.setText(noteGroups.get(position).getGroupName());
        if(listener!=null){
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int currentPosition = holder.getBindingAdapterPosition();
                    if (currentPosition != RecyclerView.NO_POSITION) {
                        listener.onClick(currentPosition);
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return noteGroups.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        TextView textView;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView=itemView.findViewById(R.id.rv_item_text);
        }
    }

    public interface OnBottomDialogGroupClickListener {
        void onClick(int position);
    }
}
