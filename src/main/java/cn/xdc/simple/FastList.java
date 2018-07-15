package cn.xdc.simple;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xdc on 18-6-27.
 */
class FastList<T> {
    protected static final int DELETE_MAXNUM_ONETIME = 1000;
    private final long maxNum;
    private final long timeoutMs;
    private volatile long num;
    private final FastElem<T> head, tail;

    FastList(long maxNum, long timeoutMs) {
        this.maxNum = maxNum;
        this.timeoutMs = timeoutMs;
        head = new FastElem<T>();
        tail = new FastElem<T>();
        tail.setPrev(head);
        head.setNext(tail);
    }

    protected void remove(FastElem<T> elem) {
        if(elem.getNext() != null && elem.getPrev() != null
                && elem.getNext().getPrev() == elem && elem.getPrev().getNext() == elem) {
            elem.getPrev().setNext(elem.getNext());
            elem.getNext().setPrev(elem.getPrev());
            num--;
        }
    }

    protected FastElem<T> insertHead(FastElem<T> elem){
        if(elem.getPrev() == null && elem.getNext() == null) {
            elem.setPrev(head);
            elem.setNext(head.getNext());
            elem.getPrev().setNext(elem);
            elem.getNext().setPrev(elem);
            num++;
            if(num > maxNum) {
                FastElem<T> removed = tail.getPrev();
                if(removed != head) {
                    removed.getPrev().setNext(removed.getNext());
                    removed.getNext().setPrev(removed.getPrev());
                    num--;
                    return removed;
                }
            }
        }
        return null;
    }

    protected long timeout(List<FastElem<T>> elems){
        FastElem<T> elem = tail.getPrev();
        long curTime = System.currentTimeMillis();

        while (elem != head && elems.size() < DELETE_MAXNUM_ONETIME ) {
            if(elem.getTimestamp() + timeoutMs <= curTime) {
                elems.add(elem);
                elem.getPrev().setNext(elem.getNext());
                elem.getNext().setPrev(elem.getPrev());
                num--;
            }else {
                curTime = System.currentTimeMillis();
                if(elem.getTimestamp() + timeoutMs <= curTime) {
                    elems.add(elem);
                    elem.getPrev().setNext(elem.getNext());
                    elem.getNext().setPrev(elem.getPrev());
                    num--;
                }else {
                    return elem.getTimestamp() - curTime + timeoutMs;
                }
            }
            elem = elem.getPrev();
        }
        return elems.size() == DELETE_MAXNUM_ONETIME ? 1 : timeoutMs;
    }

    protected boolean isTimeout(FastElem<T> elem){
        return elem.isTimeout(timeoutMs);
    }

    protected long size(){
        return num;
    }

    protected List<FastElem<T>> getAll(){
        ArrayList<FastElem<T>> list = new ArrayList<>((int) num);

        for( FastElem<T> elem = head.getNext(); elem != tail; elem = elem.getNext()){
            list.add(elem);
        }
        return list;
    }
}
