package com.iteam.examproject.utils;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.iteam.examproject.R;

/**
 * 全局统一的轻提示。
 *
 * <p>为什么不让界面直接写 {@code Toast.makeText(...).show()}：
 * 每调一次都会新建一个 Toast 交给系统排队，用户快速点几下（比如连点保存、连点删除）
 * 就会看到提示一条接一条地弹，界面已经跳走了，旧提示还在后面排队。
 * 这里始终复用同一个实例，新提示会把正在显示的顶掉，不会排队，也不会越积越多。
 *
 * <p>实例持有的是 applicationContext：静态字段如果握着 Activity，页面关了它还会被这个
 * 单例引用着，属于典型的内存泄漏。
 */
public class ToastUtil {
    private static Toast toast;

    private ToastUtil() {
    }

    /** 按字符串提示 */
    public static void showToast(Context context, String message) {
        if (context == null || message == null) {
            return;
        }
        //统一取 applicationContext：静态字段持有 Activity 会泄漏
        Context appContext = context.getApplicationContext();
        if (toast == null) {
            toast = Toast.makeText(appContext, message, Toast.LENGTH_SHORT);
        } else {
            //复用同一个实例：show() 之前改文案会把当前这条替换掉，而不是排队
            toast.setText(message);
        }
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.show();
    }

    /** 按字符串资源提示，省掉调用方到处写 {@code getString(...)} */
    public static void showToast(Context context, @StringRes int resId) {
        if (context == null) {
            return;
        }
        showToast(context, context.getString(resId));
    }

    /** 统一的「操作失败」提示，避免界面层各自写一份 */
    public static void showOperationFailed(@NonNull Context context) {
        showToast(context, context.getString(R.string.operation_failed));
    }
}
