package com.iteam.examproject.utils;

import android.content.Context;
import android.widget.Toast;

import com.iteam.examproject.R;

public class ToastUtil {
    private static Toast toast;

    private ToastUtil() {
    }

    /**
     * 复用同一个 Toast 实例，连续提示时不会排队等待。
     * 统一取 applicationContext：静态字段持有 Activity 会泄漏。
     */
    public static void showToast(Context context, String message) {
        Context appContext = context.getApplicationContext();
        if (toast == null) {
            toast = Toast.makeText(appContext, message, Toast.LENGTH_SHORT);
        } else {
            toast.setText(message);
        }
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.show();
    }

    /** 统一的「操作失败」提示，避免界面层各自写一份 */
    public static void showOperationFailed(Context context) {
        showToast(context, context.getString(R.string.operation_failed));
    }
}
