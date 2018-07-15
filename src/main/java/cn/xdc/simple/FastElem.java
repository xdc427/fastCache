package cn.xdc.simple;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by xdc on 18-6-27.
 */
class FastElem<T> {
    private T data;
    private volatile FastElem<T> next,prev;
    private FastElemForMap<?,T> forMap;
    private final AtomicReference<TimeTrack> timeTrack;

    private class TimeTrack{
        private final long timestamp;
        private final boolean isTimeout;

        TimeTrack(long timestamp, boolean isTimeout){
            this.timestamp = timestamp;
            this.isTimeout = isTimeout;
        }

        long getTimestamp() {
            return timestamp;
        }

        boolean isTimeout() {
            return isTimeout;
        }
    }

    FastElem() {
        timeTrack = new AtomicReference<>();
    }

    FastElem(FastElem<T> old){
        this.data = old.getData();
        this.forMap = old.getForMap();
        this.timeTrack = old.getTimeTrack();
    }

    FastElem(T data, FastElemForMap<?,T> forMap) {
        this.data = data;
        this.forMap = forMap;
        this.timeTrack = new AtomicReference<>(
                new TimeTrack(System.currentTimeMillis(),false));
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

    protected AtomicReference<TimeTrack> getTimeTrack() {
        return timeTrack;
    }

    protected long getTimestamp(){
        return timeTrack.get().getTimestamp();
    }

    //确保只要返回过超时，那么这个cache将永远不会再被使用
    protected boolean flushTime() {
        do {
            TimeTrack old = timeTrack.get();
            if (old.isTimeout()) {
                return false;
            }
            TimeTrack replace = new TimeTrack(System.currentTimeMillis(), false);
            if (timeTrack.compareAndSet(old, replace)) {
                return true;
            }
        }while (true);
    }

    protected boolean isTimeout(long timeoutMs){
        do {
            TimeTrack old = timeTrack.get();
            if (old.isTimeout()) {
                return true;
            }
            if (old.getTimestamp() + timeoutMs <= System.currentTimeMillis()) {
                TimeTrack replace = new TimeTrack(old.getTimestamp(), true);
                if(timeTrack.compareAndSet(old, replace)){
                    return true;
                }
            }else{
                return false;
            }
        }while (true);
    }
}
