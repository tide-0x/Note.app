package com.iteam.examproject.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.iteam.examproject.R;
import com.iteam.examproject.entity.Notes;

import java.util.ArrayList;
import java.util.List;

public class BottomDialogSearchAdapter extends RecyclerView.Adapter<BottomDialogSearchAdapter.ViewHolder> {
    private List<Notes> notes=new ArrayList<>();
    public BottomDialogSearchAdapter(){}
    public BottomDialogSearchAdapter(List<Notes> notes){
        this.notes=notes;
    }

    private OnBottomDialogSearchClickListener listener;
    public void setListener(OnBottomDialogSearchClickListener listener){
        this.listener=listener;
    }
    public void updateSearchResults(List<Notes> notes){
        this.notes.clear();
        this.notes.addAll(notes);
        notifyDataSetChanged();
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
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onClick(holder.getAdapterPosition());
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
