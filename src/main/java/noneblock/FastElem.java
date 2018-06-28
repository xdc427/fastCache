package noneblock;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by xdc on 18-6-17.
 */
public class FastElem<T> {
    private T data;
    private final AtomicReference<FastElem<T>> next,prev;

    public  FastElem(){
        next = new AtomicReference<>();
        prev = new AtomicReference<>();
    }

    public FastElem(T t){
        next = new AtomicReference<>();
        prev = new AtomicReference<>();
        data = t;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    protected AtomicReference<FastElem<T>> getNext() {
        return next;
    }

    protected AtomicReference<FastElem<T>> getPrev() {
        return prev;
    }

    protected void setNext( FastElem<T> elem){
        next.set(elem);
    }

    protected void setPrev( FastElem<T> elem){
        prev.set(elem);
    }
}
