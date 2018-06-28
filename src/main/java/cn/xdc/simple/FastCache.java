package cn.xdc.simple;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Created by xdc on 18-6-27.
 */
public class FastCache<K,V> {
    private final FastList<V> list;
    private final ConcurrentHashMap<K, FastElemForMap<K,V>> map;
    private final ScheduledExecutorService singleExecutor;
    private final ScheduledExecutorService poolExecutor;

    public FastCache( long maxNum, long timeoutMs) {
        if(timeoutMs <= 0) {
            timeoutMs = Long.MAX_VALUE;
        }
        if(maxNum <= 0) {
            maxNum = Long.MAX_VALUE;
        }
        list = new FastList<V>(maxNum, timeoutMs);
        map = new ConcurrentHashMap<>();
        singleExecutor = Executors.newSingleThreadScheduledExecutor();
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
        executor.setMaximumPoolSize(Runtime.getRuntime().availableProcessors());
        poolExecutor = executor;
        singleExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                final ArrayList<FastElem<V>> elems = new ArrayList<>(128);
                long delay = list.timeout(elems);
                singleExecutor.schedule(this, delay, TimeUnit.MILLISECONDS);
                if(!elems.isEmpty()) {
                    elems.forEach((elem)->elem.getForMap().setDeleted());
                    executor.execute(()-> elems.forEach((elem)->map.remove(elem.getForMap().getKey(), elem.getForMap())));
                }
            }
        }, timeoutMs, TimeUnit.MILLISECONDS);
    }

    public V Put(K key, V value) {
        final FastElemForMap<K,V> newElem = new FastElemForMap<K,V>(key,value);
        final FastElemForMap<K,V> oldElem = map.put(key, newElem);

        singleExecutor.execute(()->{
            if(oldElem != null && !oldElem.isDeleted()) {
                list.remove(oldElem.getElem());
                oldElem.setDeleted();
            }
            if(!newElem.isDeleted()) {
                newElem.getElem().flushTime();
                final FastElem<V> removed = list.insertHead(newElem.getElem());
                if (removed != null) {
                    removed.getForMap().setDeleted();
                    poolExecutor.execute(() -> {
                        map.remove(removed.getForMap().getKey(), removed.getForMap());
                    });
                }
            }
        });

        if(oldElem != null && !list.isTimeout(oldElem.getElem())) {
            return oldElem.getElem().getData();
        }
        return null;
    }

    public V get(K key) {
        FastElemForMap<K,V> elem = map.get(key);
        if(elem != null && !list.isTimeout(elem.getElem())) {
            return elem.getElem().getData();
        }
        return null;
    }

    public V getAndFlush(K key) {
        final FastElemForMap<K,V> elem = map.get(key);

        if(elem != null && !list.isTimeout(elem.getElem())) {
            singleExecutor.execute(() -> {
                if (!elem.isDeleted()) {
                    list.remove(elem.getElem());
                    final FastElem<V> newElem = new FastElem<>(elem.getElem().getData(), elem);
                    elem.setElem(newElem);
                    final FastElem<V> removed = list.insertHead(elem.getElem());
                    if (removed != null) {
                        removed.getForMap().setDeleted();
                        poolExecutor.execute(() -> {
                            map.remove(removed.getForMap().getKey(), removed.getForMap());
                        });
                    }
                }
            });
            return elem.getElem().getData();
        }
        return null;
    }

    public V remove(K key) {
        final FastElemForMap<K, V> elem = map.remove(key);

        if(elem != null && !list.isTimeout(elem.getElem())) {
            singleExecutor.execute(()->{
                if(!elem.isDeleted()) {
                    list.remove(elem.getElem());
                    elem.setDeleted();
                }
            });
            return elem.getElem().getData();
        }
        return null;
    }

}