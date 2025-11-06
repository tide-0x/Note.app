package com.iteam.examproject.utils;

import android.content.Context;
import android.widget.Toast;

public class ToastUtil {
    private static Toast toast;
    private ToastUtil(){}
    private static Toast getToast(Context context){
        if(toast==null){
            toast=new Toast(context);
        }
        return toast;
    }
    public static void showToast(Context context,String message){
        Toast t=getToast(context);
        t.setText(message);
        t.setDuration(Toast.LENGTH_SHORT);
        t.show();
    }
}
