package cn.xdc.simple;

import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Created by xdc on 18-6-27.
 */

/*
* 1.map层次的生命周期：加入->删除,所以唯一需要处理的循环问题就是删除时已加入了新的，ConcurrentHashMap有相应的接口
*                     ^ _  _|
* 2.刷新放在FastElemForMap这一层来处理，替换其中的引用，避免操作map。
* 3.FastElem的TimeTrack确保只要返回过超时，那么这个cache将永远不会再被使用
* 4.将队列的操作都调度在singleExecutor中执行，这些操作及其简单快速同时避免了多线程问题。复杂的map操作和其他耗时操作在poolExecutor中执行
* */
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
                    executor.execute(()-> elems.forEach((elem)->{
                        map.remove(elem.getForMap().getKey(), elem.getForMap());
                        elem.getForMap().completeFurure();
                    }));
                }
            }
        }, timeoutMs, TimeUnit.MILLISECONDS);
    }

    public CompletableFuture<Pair<K,V>> put(K key, V value) {
        final FastElemForMap<K,V> newElem = new FastElemForMap<K,V>(key,value);
        final FastElemForMap<K,V> oldElem = map.put(key, newElem);

        singleExecutor.execute(()->{
            if(oldElem != null && !oldElem.isDeleted()) {
                list.remove(oldElem.getElem());
                oldElem.setDeleted();
                poolExecutor.execute(oldElem::completeFurure);
            }
            if(!newElem.isDeleted() && newElem.getElem().flushTime()) {
                final FastElem<V> removed = list.insertHead(newElem.getElem());
                if (removed != null) {
                    removed.getForMap().setDeleted();
                    poolExecutor.execute(() -> {
                        K key1= (K) removed.getForMap().getKey();
                        boolean is = map.remove(removed.getForMap().getKey(), removed.getForMap());
                        //System.out.println(key1+" "+is);
                        removed.getForMap().completeFurure();
                    });
                }
            }
        });
        return newElem.getFuture();
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
                if (!elem.isDeleted() && elem.getElem().flushTime()) {
                    list.remove(elem.getElem());
                    final FastElem<V> newElem = new FastElem<>(elem.getElem());
                    elem.setElem(newElem);
                    final FastElem<V> removed = list.insertHead(elem.getElem());
                    if (removed != null) {
                        removed.getForMap().setDeleted();
                        poolExecutor.execute(() -> {
                            map.remove(removed.getForMap().getKey(), removed.getForMap());
                            removed.getForMap().completeFurure();
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
                    poolExecutor.execute(elem::completeFurure);
                }
            });
            return elem.getElem().getData();
        }
        return null;
    }

    public void close(long timeoutMs) throws InterruptedException {
        if (timeoutMs <= 0){
            singleExecutor.shutdownNow();
            poolExecutor.shutdownNow();
        }else{
            long begin = System.currentTimeMillis();
            singleExecutor.shutdown(); // Disable new tasks from being submitted
            poolExecutor.shutdown();
            try {
                // Wait a while for existing tasks to terminate
                singleExecutor.awaitTermination(timeoutMs, TimeUnit.MILLISECONDS);
                long left = timeoutMs - (System.currentTimeMillis() - begin);
                if(left > 0){
                    poolExecutor.awaitTermination(left, TimeUnit.MILLISECONDS);
                }
                singleExecutor.shutdownNow(); // Cancel currently executing tasks
                poolExecutor.shutdownNow();
            } catch (InterruptedException ie) {
                // (Re-)Cancel if current thread also interrupted
                singleExecutor.shutdownNow();
                poolExecutor.shutdownNow();
                throw ie;
            }
        }
    }

    public long size(){
        return list.size();
    }

    public String status(){
        StringBuilder builder = new StringBuilder(128);

        builder.append("list size:").append(size())
                .append("    map size:").append(map.size()).append('\n');
        List<FastElem<V>> all = list.getAll();
        all.forEach( (elem)->{
            FastElemForMap<K,V> mapElem = map.get(elem.getForMap().getKey());
            if(elem.getForMap() != mapElem){
                builder.append("list- ").append(elem.getForMap().getKey()).append(":")
                        .append(elem.getData()).append("    map- ");
                if(mapElem != null){
                    builder.append(mapElem.getKey()).append(":")
                            .append(mapElem.getElem().getData());
                }else {
                    builder.append(" null:null");
                }
                builder.append('\n');
            }
        });
        return builder.toString();
    }
}
