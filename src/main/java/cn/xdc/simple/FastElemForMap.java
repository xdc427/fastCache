package cn.xdc.simple;

/**
 * Created by xdc on 18-6-27.
 */
class FastElemForMap<K,V> {
    private final K key;
    private FastElem<V> elem;
    private boolean isDeleted = false;

    FastElemForMap(K key, V data) {
        this.key = key;
        this.elem = new FastElem<V>(data, this);
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

}