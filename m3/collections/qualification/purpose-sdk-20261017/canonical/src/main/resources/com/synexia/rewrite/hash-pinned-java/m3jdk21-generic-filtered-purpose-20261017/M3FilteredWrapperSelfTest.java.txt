/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import com.m3.collections.M3LinkedHashMap;
import com.m3.collections.M3TreeMap;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;

/** Standalone and recipe-executed integration proof; no substituted M3 owner classes. */
public final class M3FilteredWrapperSelfTest {
    private static long checks;
    private static final BiPredicate<Integer, Integer> ACCEPT =
            (k, v) -> v != null && v >= 0 && (k == null || (k & 1) == 0);
    private M3FilteredWrapperSelfTest() { }
    private static void check(boolean ok) { checks++; if (!ok) throw new AssertionError("check " + checks); }
    private static void eq(Object a, Object b) {
        checks++; if (!Objects.equals(a, b)) throw new AssertionError(a + " != " + b + " at " + checks);
    }
    private static void fails(Class<? extends Throwable> expected, Runnable action) {
        checks++;
        try { action.run(); } catch (Throwable t) {
            if (expected.isInstance(t)) return;
            throw new AssertionError("expected " + expected, t);
        }
        throw new AssertionError("missing " + expected);
    }
    /** Deliberately exposes SortedMap only, so dispatch cannot use NavigableMap. */
    private static final class SortedOnly<K, V> extends AbstractMap<K, V> implements SortedMap<K, V> {
        private final SortedMap<K, V> owner;
        SortedOnly(SortedMap<K, V> owner) { this.owner = owner; }
        @Override public Set<Entry<K, V>> entrySet() { return owner.entrySet(); }
        @Override public Comparator<? super K> comparator() { return owner.comparator(); }
        @Override public boolean containsKey(Object k) { return owner.containsKey(k); }
        @Override public V get(Object k) { return owner.get(k); }
        @Override public V put(K k, V v) { return owner.put(k, v); }
        @Override public V remove(Object k) { return owner.remove(k); }
        @Override public K firstKey() { return owner.firstKey(); }
        @Override public K lastKey() { return owner.lastKey(); }
        @Override public SortedMap<K, V> subMap(K a, K b) { return new SortedOnly<>(owner.subMap(a, b)); }
        @Override public SortedMap<K, V> headMap(K b) { return new SortedOnly<>(owner.headMap(b)); }
        @Override public SortedMap<K, V> tailMap(K a) { return new SortedOnly<>(owner.tailMap(a)); }
    }
    private static Map<Integer, Integer> owner(int kind) {
        Comparator<Integer> c = Comparator.nullsFirst(Comparator.naturalOrder());
        return switch (kind) {
            case 0 -> new LinkedHashMap<>();
            case 1 -> new M3LinkedHashMap<>();
            case 2 -> new TreeMap<>(c);
            case 3 -> new M3TreeMap<>(c);
            case 4 -> new SortedOnly<>(new TreeMap<>(c));
            case 5 -> new SortedOnly<>(new M3TreeMap<>(c));
            default -> throw new IllegalArgumentException();
        };
    }
    private static void verifyOwner(Map<?, ?> map) {
        if (map instanceof M3LinkedHashMap<?, ?> m) m.verifyInvariants();
        if (map instanceof M3TreeMap<?, ?> m) m.verifyInvariants();
        if (map instanceof SortedOnly<?, ?> m) verifyOwner(m.owner);
    }
    private static void seed(Map<Integer, Integer> owner) {
        for (int k = -8; k <= 32; k++) owner.put(k, k % 5 == 0 ? -k - 10 : k + 12);
        owner.put(null, 7); owner.put(40, null);
    }
    private static Map<Integer, Integer> expected(Map<Integer, Integer> owner) {
        Map<Integer, Integer> copy = new LinkedHashMap<>();
        owner.forEach((k, v) -> { if (ACCEPT.test(k, v)) copy.put(k, v); });
        return copy;
    }
    private static void shape(Map<?, ?> source, Map<?, ?> wrapper) {
        eq(source instanceof SortedMap<?, ?>, wrapper instanceof SortedMap<?, ?>);
        eq(source instanceof NavigableMap<?, ?>, wrapper instanceof NavigableMap<?, ?>);
        if (source instanceof SortedMap<?, ?> sorted) {
            check(sorted.comparator() == ((SortedMap<?, ?>) wrapper).comparator());
        }
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void purpose(int kind) {
        Map<Integer, Integer> source = owner(kind); seed(source);
        Map<Integer, Integer> filtered = M3FilteredMaps.filtered(source, ACCEPT);
        Map<Integer, Integer> checked = M3FilteredMaps.checkedMap(filtered, Integer.class, Integer.class);
        Map<Integer, Integer> readOnly = M3FilteredMaps.unmodifiableMap(checked);
        shape(source, checked); shape(source, readOnly);
        eq(expected(source), readOnly); eq(readOnly, expected(source));
        Map<Integer, Integer> before = new LinkedHashMap<>(source);
        fails(ClassCastException.class, () -> ((Map) checked).put("bad-key", 1));
        fails(ClassCastException.class, () -> ((Map) checked).put(2, "bad-value"));
        fails(IllegalArgumentException.class, () -> checked.put(2, -1));
        eq(source, before);
        Map.Entry<Integer, Integer> e = checked.entrySet().iterator().next();
        fails(ClassCastException.class, () -> ((Map.Entry) e).setValue("bad-value"));
        fails(IllegalArgumentException.class, () -> e.setValue(-1));
        e.setValue(123); eq(source.get(e.getKey()), 123);
        // No write-through Entry leaks through any public traversal route.
        ArrayList<Map.Entry<Integer, Integer>> entries = new ArrayList<>(readOnly.entrySet());
        entries.addAll(readOnly.entrySet().stream().toList());
        entries.addAll(readOnly.entrySet().parallelStream().toList());
        readOnly.entrySet().spliterator().forEachRemaining(entries::add);
        for (Object item : readOnly.entrySet().toArray()) entries.add((Map.Entry<Integer, Integer>) item);
        for (Map.Entry<Integer, Integer> item : entries) fails(UnsupportedOperationException.class, () -> item.setValue(999));
        fails(UnsupportedOperationException.class, () -> readOnly.put(2, 1));
        fails(UnsupportedOperationException.class, () -> readOnly.compute(2, (k, v) -> 1));
        fails(UnsupportedOperationException.class, () -> readOnly.merge(2, 1, Integer::sum));
        fails(UnsupportedOperationException.class, readOnly::clear);
        fails(UnsupportedOperationException.class, () -> readOnly.keySet().remove(2));
        fails(UnsupportedOperationException.class, () -> readOnly.values().remove(123));
        // Live read-only means changes through the authoritative source remain visible.
        source.put(2, 24); eq(readOnly.get(2), 24);
        source.put(2, -24); check(!readOnly.containsKey(2));
        Map<Integer, Integer> hidden = new LinkedHashMap<>(source); hidden.entrySet().removeIf(x -> ACCEPT.test(x.getKey(), x.getValue()));
        checked.clear(); eq(source, hidden); verifyOwner(source);
        fails(UnsupportedOperationException.class, readOnly::clear); // including empty view
        seed(source); shape(source, M3FilteredMaps.synchronizedMap(filtered));
    }
    private static void sorted() {
        for (int kind = 2; kind <= 5; kind++) {
            Map<Integer, Integer> source = owner(kind); seed(source);
            SortedMap<Integer, Integer> f = M3FilteredMaps.filtered((SortedMap<Integer, Integer>) source, ACCEPT);
            SortedMap<Integer, Integer> c = M3FilteredMaps.checkedMap(f, Integer.class, Integer.class);
            SortedMap<Integer, Integer> r = M3FilteredMaps.unmodifiableMap(c);
            check(c.comparator() == f.comparator()); shape(f, c); shape(f, r);
            SortedMap<Integer, Integer> bounded = c.subMap(0, 30);
            Map<Integer, Integer> expected = new LinkedHashMap<>();
            source.forEach((k,v) -> { if (k != null && k >= 0 && k < 30 && ACCEPT.test(k,v)) expected.put(k,v); });
            eq(expected, bounded);
            fails(IllegalArgumentException.class, () -> bounded.put(32, 3));
            SortedMap<Integer, Integer> readonlyRange = r.subMap(0, 30);
            fails(UnsupportedOperationException.class, readonlyRange::clear);
            eq(expected.keySet(), readonlyRange.keySet());
            Map<Integer, Integer> hidden = new LinkedHashMap<>(); source.forEach((k,v) -> {
                if (k == null || k < 0 || k >= 30 || !ACCEPT.test(k,v)) hidden.put(k,v);
            });
            bounded.clear(); eq(source, hidden); verifyOwner(source);
        }
        for (int kind : new int[]{2, 3}) for (boolean reversed : new boolean[]{false, true}) {
            NavigableMap<Integer, Integer> raw = (NavigableMap<Integer, Integer>) owner(kind); seed(raw);
            NavigableMap<Integer, Integer> range = raw.subMap(-4, true, 28, false);
            if (reversed) range = range.descendingMap();
            NavigableMap<Integer, Integer> f = M3FilteredMaps.filtered(range, ACCEPT);
            NavigableMap<Integer, Integer> c = M3FilteredMaps.checkedMap(f, Integer.class, Integer.class);
            NavigableMap<Integer, Integer> r = M3FilteredMaps.unmodifiableMap(c);
            TreeMap<Integer, Integer> expected = new TreeMap<>(range.comparator()); expected.putAll(expected(range));
            for (int q=-10; q<40; q++) {
                eq(expected.lowerEntry(q), r.lowerEntry(q)); eq(expected.floorEntry(q), r.floorEntry(q));
                eq(expected.ceilingEntry(q), r.ceilingEntry(q)); eq(expected.higherEntry(q), r.higherEntry(q));
                eq(expected.lowerKey(q), c.lowerKey(q)); eq(expected.floorKey(q), c.floorKey(q));
                eq(expected.ceilingKey(q), c.ceilingKey(q)); eq(expected.higherKey(q), c.higherKey(q));
            }
            eq(r.descendingMap().keySet(), expected.descendingKeySet());
            eq(new ArrayList<>(r.reversed().keySet()), new ArrayList<>(expected.descendingKeySet()));
            fails(UnsupportedOperationException.class, () -> r.firstEntry().setValue(777));
            fails(UnsupportedOperationException.class, r::pollFirstEntry);
            fails(UnsupportedOperationException.class, () -> r.descendingKeySet().pollFirst());
            eq(c.pollFirstEntry(), expected.pollFirstEntry()); eq(c, expected); verifyOwner(raw);
        }
        // Comparator-equivalent, non-equal keys: predicate sees stored representative.
        for (boolean m3 : new boolean[]{false, true}) {
            NavigableMap<String, Integer> owner = m3 ? new M3TreeMap<>(String.CASE_INSENSITIVE_ORDER) : new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            owner.put("ALPHA", 1); owner.put("BETA", -2);
            NavigableMap<String, Integer> f = M3FilteredMaps.filtered(owner, (k,v) -> k.equals(k.toUpperCase(Locale.ROOT)) && v > 0);
            var checked = M3FilteredMaps.checkedMap(f, String.class, Integer.class);
            var readonly = M3FilteredMaps.unmodifiableMap(checked);
            eq(readonly.get("alpha"), 1); check(readonly.keySet().contains("alpha"));
            eq(checked.put("alpha", 3), 1); eq(owner.firstKey(), "ALPHA");
            checked.keySet().remove("alpha"); eq(owner, Map.of("BETA", -2)); verifyOwner(owner);
        }
    }
    private static void lockComposition() throws Exception {
        for (int kind=0;kind<6;kind++) {
            Map<Integer, Integer> owner=owner(kind);owner.put(2, 0);
            AtomicReference<Object> monitor=new AtomicReference<>();
            AtomicInteger held=new AtomicInteger();
            Map<Integer, Integer> f=M3FilteredMaps.filtered(owner,(k,v)->{
                if (!Thread.holdsLock(monitor.get())) throw new AssertionError("root monitor not held");
                held.incrementAndGet();return v>=0;
            });
            Map<Integer, Integer> sync=M3FilteredMaps.synchronizedMap(f);monitor.set(sync);
            Map<Integer, Integer> other=sync;
            if(sync instanceof NavigableMap<Integer, Integer> nav) other=nav.descendingMap().subMap(4,true,0,true);
            else if(sync instanceof SortedMap<Integer, Integer> sorted) other=sorted.subMap(0,4);
            final Map<Integer, Integer> range=other;
            try(var pool=Executors.newFixedThreadPool(2)) {
                Future<?> a=pool.submit(()->{for(int i=0;i<150;i++)sync.compute(2,(k,v)->v+1);});
                Future<?> b=pool.submit(()->{for(int i=0;i<150;i++)range.compute(2,(k,v)->v+1);});
                a.get(20,TimeUnit.SECONDS);b.get(20,TimeUnit.SECONDS);
            }
            eq(sync.get(2),300); check(held.get()>600);
            sync.forEach((k,v)->check(Thread.holdsLock(sync)));
            synchronized(sync) { eq(sync.entrySet().stream().count(),1L); }
            if(sync instanceof SortedMap<Integer,Integer> sorted) {
                shape(sorted,M3FilteredMaps.synchronizedMap(sorted));
            }
            verifyOwner(owner);
        }
    }
    private static void collections() {
        ArrayList<Integer> source=new ArrayList<>(List.of(1,2,3,4));AtomicInteger calls=new AtomicInteger();
        Collection<Integer> f=M3CollectionViews.filtered(source,x->{calls.incrementAndGet();return (x&1)==0;});
        var sp=f.spliterator();check((sp.characteristics()&Spliterator.SIZED)==0);eq(calls.get(),0);
        eq(f.stream().findFirst().orElseThrow(),2);eq(calls.get(),2);
        source.add(6);eq(new ArrayList<>(f),List.of(2,4,6));
        fails(IllegalArgumentException.class,()->f.addAll(List.of(8,9)));eq(source,List.of(1,2,3,4,6));
        f.clear();eq(source,List.of(1,3));
        NavigableSet<String> set=new TreeSet<>(String.CASE_INSENSITIVE_ORDER);set.addAll(List.of("ALPHA","BETA","GAMMA"));
        var filtered=M3CollectionViews.filtered(set,k->k.startsWith("A")||k.startsWith("G"));
        var ro=Collections.unmodifiableNavigableSet(filtered);
        check(ro.contains("alpha"));eq(ro.tailSet("beta",true).floor("ZZZ"),"GAMMA");
        fails(UnsupportedOperationException.class,()->ro.descendingSet().pollFirst());
        filtered.subSet("a",true,"z",false).clear();eq(set,Set.of("BETA"));
    }
    private static void nullArguments() {
        fails(NullPointerException.class,()->M3FilteredMaps.unmodifiableMap((Map<Object,Object>)null));
        fails(NullPointerException.class,()->M3FilteredMaps.unmodifiableMap((SortedMap<Object,Object>)null));
        fails(NullPointerException.class,()->M3FilteredMaps.unmodifiableMap((NavigableMap<Object,Object>)null));
        fails(NullPointerException.class,()->M3FilteredMaps.checkedMap(new HashMap<>(),null,Object.class));
        fails(NullPointerException.class,()->M3FilteredMaps.checkedMap(new TreeMap<>(),Object.class,null));
        fails(NullPointerException.class,()->M3FilteredMaps.synchronizedMap((Map<Object,Object>)null));
        fails(NullPointerException.class,()->M3FilteredMaps.synchronizedMap((SortedMap<Object,Object>)null));
        fails(NullPointerException.class,()->M3FilteredMaps.synchronizedMap((NavigableMap<Object,Object>)null));
    }
    private static void nativeBoundaries(String library) throws Exception {
        System.load(library);Method m=Class.forName("RunSearchJni").getDeclaredMethod("first",int[].class,int.class,int.class);m.setAccessible(true);
        for(int n:new int[]{0,1,2,511,512,513,1023,1024,1025,32768}) {
            int[] a=new int[2*n+3];for(int i=0;i<n;i++){a[2*i]=2*i;a[2*i+1]=2*i+1;}
            a[2*n]=-1;int[] before=a.clone();
            for(int value:new int[]{Integer.MIN_VALUE,-1,0,1,511,512,1023,1024,1025,2047,32767,65534,65535,65536,Integer.MAX_VALUE}) {
                int expected=0;while(expected<n&&a[2*expected+1]<=value)expected++;
                eq(m.invoke(null,a,n,value),expected);check(Arrays.equals(a,before));
            }
            if(n>0) {
                int[] broken=a.clone();broken[2*n-1]=65537;
                try { m.invoke(null,broken,n,-1);throw new AssertionError("late invalid endpoint admitted"); }
                catch(InvocationTargetException e){check(e.getCause() instanceof IllegalArgumentException);}
                if(n>512){broken=a.clone();broken[1024]=broken[1023];
                    try {m.invoke(null,broken,n,-1);throw new AssertionError("cross-chunk overlap");}
                    catch(InvocationTargetException e){check(e.getCause() instanceof IllegalArgumentException);}}
            }
        }
    }
    public static void main(String[] args) throws Exception {
        if(args.length==1) {nativeBoundaries(args[0]);System.out.println("WRAPPER_NATIVE_BOUNDARIES_PASS checks="+checks);return;}
        for(int kind=0;kind<6;kind++)purpose(kind);
        sorted();lockComposition();collections();nullArguments();
        System.out.println("FILTERED_WRAPPER_COMPOSITION_PASS checks="+checks+" owners=6");
    }
}
