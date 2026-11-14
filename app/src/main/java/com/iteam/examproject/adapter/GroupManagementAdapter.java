package com.iteam.examproject.adapter;

import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.iteam.examproject.R;
import com.iteam.examproject.entity.NoteGroup;
import com.iteam.examproject.entity.Notes;

import java.util.ArrayList;
import java.util.List;

public class GroupManagementAdapter extends RecyclerView.Adapter<GroupManagementAdapter.ViewHolder> {
    private static final String TAG = "GroupManagementAdapter";
    private final List<NoteGroup> noteGroups = new ArrayList<>();
    private GarbageCanIvClickListener garbageCanIvClickListener;
    private ConfirmIvClickListener confirmIvClickListener;

    public GroupManagementAdapter() {
    }

    public GroupManagementAdapter(List<NoteGroup> noteGroups) {
        if (noteGroups != null) {
            this.noteGroups.addAll(noteGroups);
        }
    }


    public void setGarbageCanIvClickListener(GarbageCanIvClickListener garbageCanIvClickListener) {
        this.garbageCanIvClickListener = garbageCanIvClickListener;
    }

    public void setConfirmIvClickListener(ConfirmIvClickListener confirmIvClickListener) {
        this.confirmIvClickListener = confirmIvClickListener;
    }

    /**
     * 用 DiffUtil 只刷新变化的行，避免整表重绘把正在编辑的输入框清掉。
     */
    public void updateNoteGroupList(List<NoteGroup> newGroups) {
        if (newGroups == null || newGroups == noteGroups) {
            return;
        }
        final List<NoteGroup> oldGroups = new ArrayList<>(noteGroups);
        final List<NoteGroup> target = new ArrayList<>(newGroups);
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return oldGroups.size();
            }

            @Override
            public int getNewListSize() {
                return target.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return oldGroups.get(oldItemPosition).getGroupId()
                        == target.get(newItemPosition).getGroupId();
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                NoteGroup oldItem = oldGroups.get(oldItemPosition);
                NoteGroup newItem = target.get(newItemPosition);
                return oldItem.getGroupId() == newItem.getGroupId()
                        && TextUtils.equals(oldItem.getGroupName(), newItem.getGroupName());
            }
        });
        noteGroups.clear();
        noteGroups.addAll(target);
        result.dispatchUpdatesTo(this);
    }

    public void addNoteGroup(NoteGroup noteGroup){
        noteGroups.add(noteGroup);
        notifyItemInserted(noteGroups.size() - 1);
    }

    public void removeNoteGroup(int position){
        noteGroups.remove(position);
        notifyItemRemoved(position);
    }

    /**
     * 按位置取分组。
     * 列表数据由适配器自己持有，外部不要拿自己的 List 去按 position 索引：
     * 增删之后两边长度会不一致，容易越界或取错行。
     */
    public NoteGroup getItem(int position) {
        if (position < 0 || position >= noteGroups.size()) {
            return null;
        }
        return noteGroups.get(position);
    }

    /** 按 groupId 找当前下标，找不到返回 -1 */
    public int indexOf(NoteGroup noteGroup) {
        if (noteGroup == null) {
            return -1;
        }
        for (int i = 0; i < noteGroups.size(); i++) {
            if (noteGroups.get(i).getGroupId() == noteGroup.getGroupId()) {
                return i;
            }
        }
        return -1;
    }

    @NonNull
    @Override
    public GroupManagementAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_group_edit, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupManagementAdapter.ViewHolder holder, int position) {
        String originalGroupName = noteGroups.get(position).getGroupName();
        //holder除了一开始的初始化，这个容器中的东西发生了什么都可以从holder获取
        //内容有变要传一个新的集合要更新
        Log.d(TAG, "onBindViewHolder: "+originalGroupName);
        holder.editGroupName.setText(originalGroupName);
        editTextEnable(false, holder.editGroupName);

        //编辑按钮
        holder.editIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editTextEnable(true, holder.editGroupName);
                holder.confirmIv.setVisibility(View.VISIBLE);
            }
        });

        //垃圾桶
        if (garbageCanIvClickListener != null) {
            holder.garbageCanIv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int currentPosition = holder.getBindingAdapterPosition();
                    if (currentPosition != RecyclerView.NO_POSITION) {
                        garbageCanIvClickListener.onClick(currentPosition);
                    }
                }
            });
        }

        //确认
        if (confirmIvClickListener != null) {
            holder.confirmIv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String editedGroupName = holder.editGroupName.getText().toString().trim();
                    int currentPosition = holder.getBindingAdapterPosition();
                    if (currentPosition == RecyclerView.NO_POSITION) {
                        return;
                    }
                    //改名要走数据库，结果异步回传：failed=true 表示重名，需要还原成原来的名字
                    confirmIvClickListener.onConfirm(currentPosition, editedGroupName, failed -> {
                        //等待数据库返回期间列表可能已经变了，这一行被复用就不要再改它
                        if (holder.getBindingAdapterPosition() != currentPosition) {
                            return;
                        }
                        holder.editGroupName.setText(failed ? originalGroupName : editedGroupName);
                        holder.confirmIv.setVisibility(View.GONE);
                        editTextEnable(false, holder.editGroupName);
                    });
                }
            });

        }
    }

    @Override
    public int getItemCount() {
        if(noteGroups!=null){
            return noteGroups.size();
        }
        return 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView editIv;
        EditText editGroupName;
        ImageView garbageCanIv;
        ImageView confirmIv;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            editIv = itemView.findViewById(R.id.edit_group_iv);
            editGroupName = itemView.findViewById(R.id.group_name_edit);
            garbageCanIv = itemView.findViewById(R.id.garbage_can_iv);
            confirmIv = itemView.findViewById(R.id.confirm_iv);
        }
    }

    private void editTextEnable(boolean enable, EditText editText) {
        editText.setFocusable(enable);
        editText.setFocusableInTouchMode(enable);
        editText.setLongClickable(false);
        editText.setInputType(enable ? InputType.TYPE_CLASS_TEXT : InputType.TYPE_NULL);
        if (enable) {
            editText.setSelection(editText.getText().length());
        }
    }

    public interface GarbageCanIvClickListener {
        void onClick(int position);
    }

    public interface ConfirmIvClickListener {
        /**
         * 确认改名。数据库操作可能是异步的，处理完再调用 done 回传结果。
         *
         * @param done failed = true 表示重名或失败，需要把输入框还原成原名
         */
        void onConfirm(int position, String editedGroupName, ResultCallback done);
    }

    public interface ResultCallback {
        void onResult(boolean failed);
    }
}
