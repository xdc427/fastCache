package cn.xdc.simple;

import javafx.util.Pair;

import java.util.concurrent.CompletableFuture;

/**
 * Created by xdc on 18-6-27.
 */
class FastElemForMap<K,V> {
    private final K key;
    private FastElem<V> elem;
    private boolean isDeleted = false;
    private final CompletableFuture<Pair<K,V>> future;

    FastElemForMap(K key, V data) {
        this.key = key;
        this.elem = new FastElem<V>(data, this);
        this.future = new CompletableFuture<>();
    }

    protected K getKey() {
        return key;
    }

    protected FastElem<V> getElem() {
        return elem;
    }

    protected void setElem(FastElem<V> elem) {
        this.elem = elem;
    }

    protected boolean isDeleted() {
        return isDeleted;
    }

    protected void setDeleted() {
        this.isDeleted = true;
    }

    protected CompletableFuture<Pair<K, V>> getFuture() {
        return future;
    }

    protected void completeFurure(){
        future.complete(new Pair<K, V>(key, elem.getData()));
    }
}
