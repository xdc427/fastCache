package noneblock;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by xdc on 18-6-17.
 */
public class FastList<T> {
    private static int NONE = 0;
    private static int ADD = 1;
    private static int DELETE = 2;

    private final FastElem<T> head,tail;
    private final AtomicReference<Internal> internal;

    class Internal{
        private final long num;
        private final FastElem<T> elem;
        private final int opt;

        private Internal( long num, FastElem<T> elem, int opt){
            this.num = num;
            this.elem = elem;
            this.opt = opt;
        }
    }

    public FastList(){
        List<FastElem<T>> elems= Arrays.asList(new FastElem(),new FastElem()
                ,new FastElem(),new FastElem());
        for( int i=0; i < elems.size()-1; i++){
            FastElem<T> first = elems.get(i);
            FastElem<T> second = elems.get(i+1);
            first.setNext(second);
            second.setPrev(second);
        }
        head = elems.get(0);
        tail = elems.get(elems.size()-1);
        internal = new AtomicReference<>();
    }

    public void delete( FastElem<T> elem){

        AtomicReference<FastElem<T>> prev = elem.getPrev();
        AtomicReference<FastElem<T>> next = elem.getNext();

        //prev.compareAndSet();
    }

    private void addAfter(FastElem<T> before, FastElem<T> node){

    }

}

