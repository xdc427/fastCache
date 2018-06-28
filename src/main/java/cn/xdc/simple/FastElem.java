package cn.xdc.simple;

/**
 * Created by xdc on 18-6-27.
 */
class FastElem<T> {
    private T data;
    private volatile FastElem<T> next,prev;
    private FastElemForMap<?,T> forMap;
    private long timestamp;

    FastElem() {
        timestamp = System.currentTimeMillis();
    }

    FastElem(T data, FastElemForMap<?,T> forMap) {
        this.data = data;
        this.forMap = forMap;
        this.timestamp = System.currentTimeMillis();
    }

    protected T getData() {
        return data;
    }

    protected FastElemForMap<?,T> getForMap() {
        return forMap;
    }

    protected FastElem<T> getNext() {
        return next;
    }

    protected void setNext(FastElem<T> next) {
        this.next = next;
    }

    protected FastElem<T> getPrev() {
        return prev;
    }

    protected void setPrev(FastElem<T> prev) {
        this.prev = prev;
    }

    protected void flushTime() {
        this.timestamp = System.currentTimeMillis();
    }

    protected long getTimestamp() {
        return timestamp;
    }
}