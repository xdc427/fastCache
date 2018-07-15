import cn.xdc.simple.FastCache;

/**
 * Created by xdc on 18-7-15.
 */
public class FastCacheTest {
    public static void main(String[] args){
        FastCache<Integer,Integer> fastCache = new FastCache<>(1,1000000);

        for(int i = 0; i < 1000000; i++){
            fastCache.put(i,i);
        }
        try {
            Thread.sleep(5000);
            fastCache.close(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(fastCache.status());
        System.out.println("Success");
    }
}
