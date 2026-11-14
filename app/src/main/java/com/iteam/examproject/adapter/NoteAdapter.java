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

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.ViewHolder> {
    private List<Notes> notes=new ArrayList<>();
    private NoteClickListener noteClickListener;
    private NoteLongClickListener noteLongClickListener;

    public NoteAdapter() {
    }

    public NoteAdapter(List<Notes> notes) {
        this.notes = notes;
    }

    public void updateNotes(List<Notes> notes) {
        this.notes.clear();
        this.notes.addAll(notes);
        notifyDataSetChanged();
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
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int currentPosition = holder.getBindingAdapterPosition();
                    if (currentPosition != RecyclerView.NO_POSITION) {
                        noteClickListener.onClick(currentPosition);
                    }
                }
            });
        }

        //长按
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                int currentPosition = holder.getBindingAdapterPosition();
                if (noteLongClickListener != null && currentPosition != RecyclerView.NO_POSITION) {
                    noteLongClickListener.onLongClick(currentPosition);
                    return true;
                }
                return false;
            }
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
