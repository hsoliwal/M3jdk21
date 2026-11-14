// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Random;
import java.util.function.Function;

/** Original LFU semantics oracle: identity keys, frequency, ties, null and overflow. */
public final class LFUCacheContract {
    private static long checks;
    private LFUCacheContract() { }
    private static void check(boolean ok, String m) {
        if (!ok) throw new AssertionError(m);
        checks++;
    }
    private static Object field(Object instance, String name) throws Exception {
        Field f = instance.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(instance);
    }
    private static void setLong(Object instance, String name, long value) throws Exception {
        Field f = instance.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.setLong(instance, value);
    }
    private record Box(int id) { }
    private static final class Model {
        final Object[] keys;
        final Box[] values;
        final int[] freq;
        final long[] stamp;
        final boolean[] used;
        long clock;
        int size;
        Model(int capacity) {
            keys=new Object[capacity]; values=new Box[capacity]; freq=new int[capacity];
            stamp=new long[capacity];used=new boolean[capacity];
        }
        Box get(Object key, Box value) {
            for(int i=0;i<used.length;i++) if(used[i]&&keys[i]==key) {
                freq[i]=Math.min(Integer.MAX_VALUE,freq[i]+1);
                stamp[i]=++clock;
                return values[i];
            }
            if(used.length==0) return value;
            int victim=-1;
            for(int i=0;i<used.length;i++) if(!used[i]) {victim=i;break;}
            if(victim<0) for(int i=0;i<used.length;i++) {
                if(victim<0||freq[i]<freq[victim]||freq[i]==freq[victim]&&stamp[i]<stamp[victim])victim=i;
            }
            if(!used[victim])size++;
            used[victim]=true;keys[victim]=key;values[victim]=value;
            freq[victim]=1;stamp[victim]=++clock;
            return value;
        }
        void remove(Object key) {
            for(int i=0;i<used.length;i++) if(used[i]&&keys[i]==key) {
                used[i]=false;keys[i]=null;values[i]=null;freq[i]=0;stamp[i]=0;size--;return;
            }
        }
        void clear() {
            Arrays.fill(used,false);Arrays.fill(keys,null);Arrays.fill(values,null);
            Arrays.fill(freq,0);Arrays.fill(stamp,0);clock=0;size=0;
        }
        void verify(LFUCache<Object,Box> cache) throws Exception {
            check(cache.size()==size,"size");
            check(cache.capacity()==keys.length,"capacity");
            Object[] gotKeys=(Object[])field(cache,"keys");
            Object[] gotValues=(Object[])field(cache,"values");
            int[] gotFreq=(int[])field(cache,"frequencies");
            long[] gotStamp=(long[])field(cache,"accessStamps");
            byte[] gotState=(byte[])field(cache,"states");
            for(int i=0;i<keys.length;i++) {
                check((gotState[i]!=0)==used[i],"state "+i);
                check(gotKeys[i]==keys[i],"identity "+i);
                check(gotValues[i]==values[i],"value "+i);
                check(gotFreq[i]==freq[i],"frequency "+i);
                check(gotStamp[i]==stamp[i],"recency "+i);
            }
            check((long)field(cache,"clock")==clock,"clock");
        }
    }
    private static void randomized() throws Exception {
        Random r=new Random(0x4c46554152524159L);
        Object[] keys=new Object[37];Box[] boxes=new Box[keys.length];
        for(int i=0;i<keys.length;i++){keys[i]=new Object();boxes[i]=new Box(i);}
        keys[0]=null;
        IdentityHashMap<Object,Box> lookup=new IdentityHashMap<>();
        for(int i=0;i<keys.length;i++)lookup.put(keys[i],boxes[i]);
        for(int capacity:new int[] {0,1,2,3,7,16}) {
            LFUCache<Object,Box> actual=new LFUCache<>(capacity);
            Model model=new Model(capacity);
            Function<Object,Box> factory=lookup::get;
            for(int step=0;step<10000;step++) {
                Object key=keys[r.nextInt(keys.length)];
                int action=r.nextInt(100);
                if(action<76) {
                    Box expected=model.get(key,factory.apply(key));
                    Box got=actual.getOrCreate(key,factory);
                    check(got==expected,"getOrCreate value");
                } else if(action<95) {actual.removeIdentity(key);model.remove(key);}
                else {actual.clear();model.clear();}
                model.verify(actual);
            }
            check(model.clock == (long)field(actual,"clock"),"end clock");
        }
    }
    private static void overflowTies() throws Exception {
        LFUCache<Object,Box> c=new LFUCache<>(4);
        Object[] keys={new Object(),new Object(),new Object(),new Object(),new Object(),new Object()};
        for(int i=0;i<4;i++){int n=i;c.getOrCreate(keys[i],key->new Box(n));}
        long[] stamp=(long[])field(c,"accessStamps");
        int[] freq=(int[])field(c,"frequencies");
        System.arraycopy(new long[]{0,5,5,Long.MAX_VALUE-1},0,stamp,0,4);
        Arrays.fill(freq,1);
        setLong(c,"clock",Long.MAX_VALUE);
        c.getOrCreate(keys[4],key->new Box(4));
        Object[] stored=(Object[])field(c,"keys");
        check(stored[0]==keys[4]&&stored[1]==keys[1]&&stored[2]==keys[2]&&stored[3]==keys[3],
                "least-recent zero-stamp key must be evicted");
        check(stamp[1]==stamp[2],"equal stamp ties must survive rebasing");
        check(stamp[1]<stamp[3]&&stamp[3]<stamp[0],"total recency preserved");
        c.getOrCreate(keys[5],key->new Box(5));
        check(stored[1]==keys[5],"lowest-slot old tied stamp evicted");
        setLong(c,"clock",Long.MAX_VALUE);
        stamp[0]=0;
        int before=freq[3];
        c.getOrCreate(keys[3],key->{throw new AssertionError("factory for live key");});
        check(freq[3]==Math.min(Integer.MAX_VALUE,before+1),"touch at saturated clock");
        check(stamp[3]>stamp[0],"touched key receives newest stamp");
        freq[3]=Integer.MAX_VALUE;
        c.getOrCreate(keys[3],key->{throw new AssertionError("factory for live key");});
        check(freq[3]==Integer.MAX_VALUE,"saturated frequency stays bounded");
    }
    private static void missingFactoryFailure() throws Exception {
        LFUCache<Object,Box> c=new LFUCache<>(2);
        Object a=new Object(), b=new Object(), d=new Object();
        c.getOrCreate(a,key->new Box(1));c.getOrCreate(b,key->new Box(2));
        Object[] keys=(Object[])field(c,"keys");int[] freq=(int[])field(c,"frequencies");
        long[] stamps=(long[])field(c,"accessStamps");
        Object[] keyBefore=keys.clone();int[] freqBefore=freq.clone();long[] stampBefore=stamps.clone();
        setLong(c,"clock",Long.MAX_VALUE);
        try{c.getOrCreate(d,key->{throw new IllegalStateException("factory failed");});
            throw new AssertionError("factory failure swallowed");}
        catch(IllegalStateException expected){checks++;}
        check(Arrays.equals(keys,keyBefore)&&Arrays.equals(freq,freqBefore)&&Arrays.equals(stamps,stampBefore),
                "failing factory must not evict or change recency");
        try{c.getOrCreate(d,key->null);throw new AssertionError("null factory accepted");}
        catch(NullPointerException expected){checks++;}
        check(Arrays.equals(keys,keyBefore),"null factory retains live entries");
    }
    public static void main(String[] args) throws Exception {
        randomized();overflowTies();missingFactoryFailure();
        System.out.println("PASS LFUCache reference oracle "+checks+" assertions");
    }
}
