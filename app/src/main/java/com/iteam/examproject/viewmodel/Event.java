package com.iteam.examproject.viewmodel;

/**
 * 一次性事件包装。
 *
 * <p>问题背景：LiveData 是「状态」，观察者注册时会立刻收到当前值。Toast、页面跳转这类
 * <b>动作</b>如果也塞进 LiveData，旋转屏幕重建后会被重放一遍——Toast 再弹一次、页面再跳一次。
 *
 * <p>用法：VM 往 {@code MutableLiveData<Event<T>>} 里 setValue；View 在观察者里用
 * {@link #getContentIfNotHandled()} 取值，取过一次就不再给。只想偷看不想消费用 {@link #peekContent()}。
 *
 * @param <T> 事件携带的数据
 */
public class Event<T> {

    private final T content;
    private boolean handled = false;

    public Event(T content) {
        this.content = content;
    }

    /** 只在第一次观察时返回内容，之后返回 null */
    public T getContentIfNotHandled() {
        if (handled) {
            return null;
        }
        handled = true;
        return content;
    }

    /** 不消费，只偷看（比如日志） */
    public T peekContent() {
        return content;
    }
}
