package com.iteam.examproject.core;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class RvDecorationUtil extends RecyclerView.ItemDecoration {
    private final float space;

    public RvDecorationUtil(float space) {
        this.space = space;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        int margin = DensityUtil.dp2px(parent.getContext(), space);
        outRect.right = margin;
        outRect.left = margin;
        outRect.top = margin;
        //最后一个item不加底部间距
        if (parent.getChildAdapterPosition(view) != parent.getAdapter().getItemCount() - 1) {
            outRect.bottom = margin;
        }
    }
}
