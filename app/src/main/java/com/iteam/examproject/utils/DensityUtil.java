package com.iteam.examproject.utils;

import android.content.Context;

public class DensityUtil {
    public static int dp2px(Context context, float dp) {
        float density = context.getResources().getDisplayMetrics().density;//屏幕矩阵的密度（分辨率）
        return (int) (dp * density + 0.5f);
    }
    //sp2px
}
