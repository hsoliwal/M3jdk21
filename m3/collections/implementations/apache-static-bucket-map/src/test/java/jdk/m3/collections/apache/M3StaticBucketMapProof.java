// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
package jdk.m3.collections.apache;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Standalone compiler + differential oracle. No copied vendor test logic. */
public final class M3StaticBucketMapProof {
    private static long checks;
    private M3StaticBucketMapProof() { }
    private static void check(final boolean b) {
        checks++;
        if (!b) throw new AssertionError("M3_ARRAY_MAP_DIFF:" + checks);
    }
    private static void same(final Object a, final Object b) { check(Objects.equals(a, b)); }
    private static void throwsCme(final Runnable run) {
        checks++;
        try { run.run(); }
        catch (ConcurrentModificationException correct) { return; }
        throw new AssertionError("missing CME " + checks);
    }
    private record Collision(int value) {
        @Override public int hashCode() { return 7; }
    }
    private static Object randomKey(Random rng) {
        int v = rng.nextInt(95);
        return v == 0 ? null : v < 30 ? new Collision(v) : "k" + v;
    }
    private static Integer randomValue(Random rng) {
        return rng.nextInt(11) == 0 ? null : rng.nextInt(800) - 400;
    }
    private static void compare(Map<Object,Integer> actual, Map<Object,Integer> expected, Random rng) {
        same(actual.size(), expected.size());
        same(actual.isEmpty(), expected.isEmpty());
        check(actual.equals(expected) && expected.equals(actual));
        same(actual.hashCode(), expected.hashCode());
        check(new HashSet<>(actual.keySet()).equals(expected.keySet()));
        check(new HashMap<>(actual).equals(expected));
        for (int i = 0; i < 20; i++) {
            Object key = randomKey(rng);
            same(actual.get(key), expected.get(key));
            same(actual.containsKey(key), expected.containsKey(key));
            same(actual.getOrDefault(key, -999), expected.getOrDefault(key, -999));
            check(actual.entrySet().contains(Map.entry(key == null ? "missing-null" : key, -999))
                    == expected.entrySet().contains(Map.entry(key == null ? "missing-null" : key, -999)));
        }
    }
    private static void seededDifferential() {
        for (int seed = 0; seed < 12; seed++) {
            Random rng = new Random(seed * 10009L + 0x41ddcb);
            M3StaticBucketMap<Object,Integer> actual = new M3StaticBucketMap<>(seed == 0 ? 0 : seed * 3);
            HashMap<Object,Integer> expected = new HashMap<>();
            for (int step = 0; step < 8200; step++) {
                final Object k = randomKey(rng);
                final Integer v = randomValue(rng);
                int op = rng.nextInt(14);
                switch(op) {
                    case 0 -> same(actual.put(k,v), expected.put(k,v));
                    case 1 -> same(actual.remove(k), expected.remove(k));
                    case 2 -> same(actual.putIfAbsent(k,v), expected.putIfAbsent(k,v));
                    case 3 -> same(actual.replace(k,v), expected.replace(k,v));
                    case 4 -> {Integer old = randomValue(rng);same(actual.replace(k,old,v),expected.replace(k,old,v));}
                    case 5 -> {Integer old = randomValue(rng);same(actual.remove(k,old),expected.remove(k,old));}
                    case 6 -> same(actual.computeIfAbsent(k,x->v),expected.computeIfAbsent(k,x->v));
                    case 7 -> same(actual.computeIfPresent(k,(x,y)->v),expected.computeIfPresent(k,(x,y)->v));
                    case 8 -> same(actual.compute(k,(x,y)->v),expected.compute(k,(x,y)->v));
                    case 9 -> {Integer nonnull = Objects.requireNonNullElse(v,17);same(actual.merge(k,nonnull,(a,b)->a+b),expected.merge(k,nonnull,(a,b)->a+b));}
                    case 10 -> same(actual.containsValue(v), expected.containsValue(v));
                    case 11 -> {if(rng.nextInt(30)==0) {actual.clear();expected.clear();}}
                    case 12 -> {actual.atomic(()->actual.put(k,v));expected.put(k,v);}
                    case 13 -> {actual.replaceAll((x,y)->y==null?null:y+1);expected.replaceAll((x,y)->y==null?null:y+1);}
                    default -> throw new AssertionError();
                }
                check(actual.size()==expected.size());
                if ((step & 31)==0) compare(actual, expected, rng);
            }
            compare(actual,expected,rng);
        }
    }
    private static void iteratorAndViews() {
        M3StaticBucketMap<String,Integer> map = new M3StaticBucketMap<>(4);
        map.put(null, null);
        map.put("a",11);
        map.put("b",22);
        Iterator<Map.Entry<String,Integer>> it = map.entrySet().iterator();
        Map.Entry<String,Integer> first = it.next();
        check(first!=null);
        final String firstKey = first.getKey();
        it.remove();
        check(!map.containsKey(firstKey));
        map.put("different",33);
        same(first.getKey(),firstKey);
        same(first.getValue(),null);
        Iterator<String> k = map.keySet().iterator();
        k.next();
        map.put("fresh",44);
        throwsCme(k::hasNext);
        var mapIt = map.mapIterator();
        check(mapIt.hasNext());
        String mkey = mapIt.next();
        same(mapIt.getKey(),mkey);
        same(mapIt.getValue(),map.get(mkey));
        Integer previous = map.get(mkey);
        same(mapIt.setValue(987),previous);
        same(map.get(mkey),987);
        mapIt.remove();
        check(!map.containsKey(mkey));
        boolean illegal = false;
        try {mapIt.remove();}catch(IllegalStateException ok){illegal=true;}
        check(illegal);
        check(map.keySet().remove("not-present")==false);
        check(map.entrySet().contains(Map.entry("fresh",44)) == map.containsKey("fresh"));
        check(map.values().contains(44) == map.containsValue(44));
        map.values().clear();
        check(map.isEmpty());
    }
    private static void occupancyAndRecycle() throws Exception {
        M3StaticBucketMap<Integer,Integer> map = new M3StaticBucketMap<>(0);
        for(int i=0;i<5000;i++) map.put(i,i);
        for(int i=0;i<5000;i+=2) same(map.remove(i),i);
        for(int i=0;i<5000;i+=2) map.put(i,i+10000);
        check(map.size()==5000);
        for(int i=0;i<5000;i++) same(map.get(i), i%2==0?i+10000:i);
        Field keyLane=M3StaticBucketMap.class.getDeclaredField("keys");
        keyLane.setAccessible(true);
        Field highWater=M3StaticBucketMap.class.getDeclaredField("highWater");
        highWater.setAccessible(true);
        check((Integer)highWater.get(map)==5000);
        check(((Object[])keyLane.get(map)).length < 20000);
        for(int i=0;i<5000;i++) map.remove(i);
        for(Object key:(Object[])keyLane.get(map))check(key==null);
        map.clear();
        check(map.isEmpty());
    }
    private static final class ReentrantKey {
        private M3StaticBucketMap<Object, Integer> owner;
        @Override public int hashCode() { return 7; }
        @Override public boolean equals(final Object other) {
            owner.put("during-equality", 77);
            return this == other;
        }
    }
    private static final class ReentrantValue {
        private M3StaticBucketMap<Object, Object> owner;
        @Override public int hashCode() { return 1; }
        @Override public boolean equals(final Object other) {
            owner.put("during-value-equality", 88);
            return this == other;
        }
    }
    private static void reentrantMutationRefused() {
        M3StaticBucketMap<Object, Integer> map = new M3StaticBucketMap<>(2);
        ReentrantKey key = new ReentrantKey();
        key.owner = map;
        map.put(key, 1);
        boolean refused = false;
        try { map.containsKey(new Collision(2)); }
        catch (ConcurrentModificationException okay) { refused = true; }
        check(refused);
        same(map.get("during-equality"), 77);
        M3StaticBucketMap<Object,Object> other = new M3StaticBucketMap<>(2);
        ReentrantValue value = new ReentrantValue();
        value.owner = other;
        other.put("thing", value);
        refused = false;
        try { other.containsValue("other"); }
        catch (ConcurrentModificationException okay) { refused = true; }
        check(refused);
        same(other.get("during-value-equality"), 88);
    }
    private static void declaredPublicApiUnchanged() {
        final java.util.Set<String> expected = java.util.Set.of(
            "atomic/1", "clear/0", "compute/2", "computeIfAbsent/2",
            "computeIfPresent/2", "containsKey/1", "containsValue/1", "entrySet/0",
            "equals/1", "forEach/1", "get/1", "getOrDefault/2", "hashCode/0",
            "isEmpty/0", "keySet/0", "mapIterator/0", "merge/3", "put/2",
            "putAll/1", "putIfAbsent/2", "remove/1", "remove/2", "replace/2",
            "replace/3", "replaceAll/1", "size/0", "values/0");
        final java.util.Set<String> actual = new java.util.HashSet<>();
        for (final Method method : M3StaticBucketMap.class.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers()) && !method.isSynthetic()) {
                actual.add(method.getName() + "/" + method.getParameterCount());
                check(!Modifier.isSynchronized(method.getModifiers()));
                check(!Modifier.isStatic(method.getModifiers()));
            }
        }
        check(actual.equals(expected));
        check(M3StaticBucketMap.class.getConstructors().length == 2);
        check(Modifier.isFinal(M3StaticBucketMap.class.getModifiers()));
    }
    private static void atomicParallel() throws Exception {
        M3StaticBucketMap<Integer,Integer> map = new M3StaticBucketMap<>(4);
        map.put(1,0);
        int workers=8,loops=1800;
        try(var pool=Executors.newVirtualThreadPerTaskExecutor()){
            List<Future<?>> tasks=new ArrayList<>();
            for(int i=0;i<workers;i++){
                tasks.add(pool.submit(()->{
                    for(int j=0;j<loops;j++){
                        map.atomic(()->map.put(1,map.get(1)+1));
                    }
                }));
            }
            for(Future<?> task:tasks)task.get();
        }
        same(map.get(1),workers*loops);
        same(map.size(),1);
    }
    public static void main(String[] args)throws Exception{
        if(args.length!=0)throw new IllegalArgumentException("args");
        declaredPublicApiUnchanged();
        seededDifferential();
        iteratorAndViews();
        occupancyAndRecycle();
        reentrantMutationRefused();
        atomicParallel();
        System.out.println("M3_STATIC_BUCKET_SOA_PROOF_PASS checks="+checks+" noMappingNodes=true");
    }
}
