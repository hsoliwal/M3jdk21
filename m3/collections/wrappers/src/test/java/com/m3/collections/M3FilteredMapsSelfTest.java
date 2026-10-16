/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.Spliterator;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;

/** Finite contract corpus; neither a provider-union nor a concurrency proof. */
public final class M3FilteredMapsSelfTest {
    private static long checks;
    private M3FilteredMapsSelfTest() { }
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    private static void eq(Object actual, Object expected) {
        check(Objects.equals(actual, expected), "actual=" + actual + " expected=" + expected);
    }
    private static void raises(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try { action.run(); } catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("wanted " + type + " got " + failure, failure);
        }
        throw new AssertionError("missing " + type);
    }
    private static <K,V> Map.Entry<K,V> entry(K key,V value) {
        return new AbstractMap.SimpleImmutableEntry<>(key,value);
    }
    private record Key(int id, String label) { }

    private static void basicAndDefaults() {
        Map<Integer,Integer> owner = new LinkedHashMap<>();
        owner.put(0,-1); owner.put(1,10); owner.put(2,null); owner.put(null,30);
        BiPredicate<Integer,Integer> admit = (k,v) -> v == null || v >= 0;
        Map<Integer,Integer> view = M3FilteredMaps.filtered(owner,admit);
        Collection<Integer> values = view.values(); Set<Integer> keys = view.keySet();
        Set<Map.Entry<Integer,Integer>> entries = view.entrySet();
        eq(view.size(),3); eq(view.get(0),null); check(!view.containsKey(0),"hidden");
        eq(view.remove(0),null); eq(owner.get(0),-1);
        eq(view.compute(0,(k,v)->null),null); eq(owner.get(0),-1);
        eq(view.computeIfPresent(0,(k,v)->99),null); eq(owner.get(0),-1);
        check(!view.remove(0,-1),"hidden conditional remove");
        eq(view.replace(0,100),null); eq(owner.get(0),-1);
        check(!view.replace(0,-1,100),"hidden conditional replace");
        eq(view.put(0,40),null); eq(owner.get(0),40);
        eq(view.put(0,50),40); eq(view.putIfAbsent(0,70),50);
        eq(view.computeIfAbsent(2,k->22),22); eq(owner.get(2),22);
        eq(view.merge(1,2,Integer::sum),12); eq(owner.get(1),12);
        check(view.replace(1,12,14),"visible replace");
        eq(view.compute(1,(k,v)->v+1),15);
        raises(IllegalArgumentException.class,()->view.compute(1,(k,v)->-10));
        raises(IllegalArgumentException.class,()->view.merge(1,1,(a,b)->-20)); eq(owner.get(1),15);
        owner.put(8,-8); view.replaceAll((k,v)->v+1); eq(owner.get(8),-8);
        check(values.contains(23)&&keys.contains(2)&&entries.contains(entry(2,23)),"retained live views");
        owner.put(9,90); check(keys.contains(9),"external addition");
        Map<Integer,Integer> nested=M3FilteredMaps.filtered(view,(k,v)->k!=null&&k%2==0);
        raises(IllegalArgumentException.class,()->nested.put(3,30));
        nested.clear(); check(owner.containsKey(8)&&owner.containsKey(1)&&owner.containsKey(null),"nested hidden survivors");
        eq(M3CollectionViews.sortedSnapshot(view.values(),Comparator.naturalOrder()),List.of(16,31,90));
        view.clear(); eq(owner,Map.of(8,-8));
        eq(values.size(),0); eq(keys.size(),0); eq(entries.size(),0);
        owner.put(4,null); check(keys.remove(4),"visible null removal returns true");
        owner.put(4,null); check(entries.remove(entry(4,null)),"entry null removal");
        Map<Integer,Integer> bulk=new LinkedHashMap<>(); bulk.put(10,10);bulk.put(11,-11);
        raises(IllegalArgumentException.class,()->view.putAll(bulk));
        eq(owner.get(10),10); check(!owner.containsKey(11),"documented prefix update");
        raises(NullPointerException.class,()->M3FilteredMaps.filtered((Map<Integer,Integer>)null,admit));
        raises(NullPointerException.class,()->M3FilteredMaps.filtered(owner,null));
        raises(NullPointerException.class,()->view.values().removeIf(null));
        raises(NullPointerException.class,()->view.keySet().removeAll(null));
        raises(NullPointerException.class,()->view.entrySet().retainAll(null));
    }

    /** Exposes a SortedMap whose keySet is deliberately not a SortedSet. */
    private static final class SortedOnly<K,V> extends AbstractMap<K,V> implements SortedMap<K,V> {
        private final SortedMap<K,V> source;
        private SortedOnly(SortedMap<K,V> source) { this.source=source; }
        @Override public Set<Entry<K,V>> entrySet(){return source.entrySet();}
        @Override public boolean containsKey(Object key){return source.containsKey(key);}
        @Override public V get(Object key){return source.get(key);}
        @Override public V put(K key,V value){return source.put(key,value);}
        @Override public V remove(Object key){return source.remove(key);}
        @Override public Comparator<? super K> comparator(){return source.comparator();}
        @Override public K firstKey(){return source.firstKey();}
        @Override public K lastKey(){return source.lastKey();}
        @Override public SortedMap<K,V> subMap(K from,K to){return new SortedOnly<>(source.subMap(from,to));}
        @Override public SortedMap<K,V> headMap(K to){return new SortedOnly<>(source.headMap(to));}
        @Override public SortedMap<K,V> tailMap(K from){return new SortedOnly<>(source.tailMap(from));}
    }
    private static void storedRepresentatives() {
        for(boolean onlySorted:new boolean[]{false,true}) {
            Key keep=new Key(1,"keep"),hide=new Key(2,"hide");
            TreeMap<Key,Integer> owner=new TreeMap<>(Comparator.comparingInt(Key::id));
            owner.put(keep,1);owner.put(hide,2);
            SortedMap<Key,Integer> source=onlySorted?new SortedOnly<>(owner):owner;
            SortedMap<Key,Integer> view=M3FilteredMaps.filtered(source,(k,v)->k.label().equals("keep")&&v>=0);
            Key probe=new Key(1,"different");
            eq(view.get(probe),1);check(view.keySet().contains(probe),"comparator keySet");
            check(view.entrySet().contains(entry(probe,1)),"comparator entrySet");
            eq(view.put(probe,3),1);check(owner.firstKey()==keep,"stored key identity");
            raises(IllegalArgumentException.class,()->view.put(new Key(2,"keep"),3));
            eq(owner.get(hide),2);check(!view.containsKey(new Key(2,"keep")),"stored predicate not probe");
            check(view.comparator()==source.comparator(),"same comparator");
            eq(view.headMap(new Key(2,"x")).size(),1);eq(view.tailMap(probe).size(),1);
            eq(view.firstKey(),keep);eq(view.lastKey(),keep);
            check(view.keySet().remove(probe),"comparator remove");check(owner.containsKey(hide),"hidden key retained");
            raises(NoSuchElementException.class,view::firstKey);raises(NoSuchElementException.class,view::lastKey);
        }
    }

    private static void ranges() {
        for(int mask=0;mask<32;mask++) for(int lo=0;lo<5;lo++) for(int hi=lo;hi<5;hi++) {
            for(int flags=0;flags<8;flags++) {
                TreeMap<Integer,Integer> owner=new TreeMap<>();
                for(int i=0;i<5;i++)owner.put(i*4,((mask>>>i)&1)==0?-i-1:i+1);
                NavigableMap<Integer,Integer> range=owner.subMap(lo*4,(flags&1)!=0,hi*4,(flags&2)!=0);
                if((flags&4)!=0)range=range.descendingMap();
                NavigableMap<Integer,Integer> view=M3FilteredMaps.filtered(range,(k,v)->v>0);
                TreeMap<Integer,Integer> expected=new TreeMap<>(view.comparator());
                for(var e:range.entrySet())if(e.getValue()>0)expected.put(e.getKey(),e.getValue());
                eq(view,expected);eq(view.hashCode(),expected.hashCode());
                eq(new ArrayList<>(view.keySet()),new ArrayList<>(expected.keySet()));
                eq(new ArrayList<>(view.descendingKeySet()),new ArrayList<>(expected.descendingKeySet()));
                eq(view.firstEntry(),expected.firstEntry());eq(view.lastEntry(),expected.lastEntry());
                for(int q=-2;q<=18;q++) {
                    eq(view.lowerEntry(q),expected.lowerEntry(q));eq(view.floorEntry(q),expected.floorEntry(q));
                    eq(view.ceilingEntry(q),expected.ceilingEntry(q));eq(view.higherEntry(q),expected.higherEntry(q));
                    eq(view.lowerKey(q),expected.lowerKey(q));eq(view.floorKey(q),expected.floorKey(q));
                    eq(view.ceilingKey(q),expected.ceilingKey(q));eq(view.higherKey(q),expected.higherKey(q));
                    eq(view.navigableKeySet().ceiling(q),expected.ceilingKey(q));
                    eq(view.navigableKeySet().floor(q),expected.floorKey(q));
                }
                NavigableMap<Integer,Integer> twice=view.descendingMap().descendingMap();eq(twice,expected);
                eq(view.pollFirstEntry(),expected.pollFirstEntry());
                while(!expected.isEmpty())eq(view.pollLastEntry(),expected.pollLastEntry());
                eq(view.pollLastEntry(),null);eq(view.pollFirstEntry(),null);
                for(var e:owner.entrySet())check(e.getValue()<0||e.getKey()<lo*4||e.getKey()>hi*4
                    ||e.getKey()==lo*4&&(flags&1)==0||e.getKey()==hi*4&&(flags&2)==0,"poll preserves hidden/outside");
            }
        }
        TreeMap<Integer,Integer> owner=new TreeMap<>();owner.put(1,1);owner.put(3,3);
        NavigableMap<Integer,Integer> view=M3FilteredMaps.filtered(owner.subMap(1,true,3,true),(k,v)->true);
        raises(IllegalArgumentException.class,()->view.headMap(0,true));
        raises(IllegalArgumentException.class,()->view.tailMap(4,true));
        eq(view.ceilingKey(-100),1);eq(view.floorKey(100),3);
        var snapshot=view.firstEntry();owner.put(1,99);eq(snapshot,entry(1,1));
        raises(UnsupportedOperationException.class,()->snapshot.setValue(4));
        Map<Integer,Integer> erased=owner;check(M3FilteredMaps.filtered(erased,(k,v)->true) instanceof NavigableMap<?,?>,"capability dispatch");
    }

    private static void nulls() {
        for(boolean descending:new boolean[]{false,true}) {
            TreeMap<Integer,Integer> owner=new TreeMap<>(Comparator.nullsFirst(Comparator.naturalOrder()));
            owner.put(null,null);owner.put(0,-1);owner.put(2,20);owner.put(4,null);
            NavigableMap<Integer,Integer> view=M3FilteredMaps.filtered(descending?owner.descendingMap():owner,(k,v)->v==null||v>=0);
            TreeMap<Integer,Integer> expected=new TreeMap<>(view.comparator());expected.put(null,null);expected.put(2,20);expected.put(4,null);
            for(Integer key:Arrays.asList(null,-1,0,1,2,4,5)) {
                eq(view.lowerEntry(key),expected.lowerEntry(key));eq(view.floorEntry(key),expected.floorEntry(key));
                eq(view.ceilingEntry(key),expected.ceilingEntry(key));eq(view.higherEntry(key),expected.higherEntry(key));
            }
            check(view.containsKey(null),"present null key/value");eq(view.getOrDefault(null,100),null);
            while(!expected.isEmpty())eq(view.pollFirstEntry(),expected.pollFirstEntry());
            eq(owner,Map.of(0,-1));
        }
    }

    @SuppressWarnings("unchecked")
    private static void mutationBoundaries() {
        for(int route=0;route<7;route++) {
            Map<Integer,Integer> owner=new LinkedHashMap<>();owner.put(1,1);owner.put(2,-2);
            Map<Integer,Integer> view=M3FilteredMaps.filtered(owner,(k,v)->v>0);
            AtomicReference<Map.Entry<Integer,Integer>> ref=new AtomicReference<>();
            var entries=view.entrySet();
            switch(route) {
                case 0 -> ref.set(entries.iterator().next());
                case 1 -> ref.set((Map.Entry<Integer,Integer>)entries.toArray()[0]);
                case 2 -> ref.set(entries.stream().findFirst().orElseThrow());
                case 3 -> ref.set(entries.parallelStream().findFirst().orElseThrow());
                case 4 -> entries.spliterator().tryAdvance(ref::set);
                case 5 -> entries.forEach(ref::set);
                default -> ref.set((Map.Entry<Integer,Integer>)entries.toArray(Map.Entry[]::new)[0]);
            }
            raises(IllegalArgumentException.class,()->ref.get().setValue(-9));eq(owner.get(1),1);
            eq(ref.get().setValue(3),1);eq(owner.get(1),3);
            raises(IllegalArgumentException.class,()->entries.removeIf(e->{e.setValue(-7);return false;}));
            eq(owner.get(1),3);eq(owner.get(2),-2);
            Iterator<?> iterator=entries.iterator();iterator.next();raises(UnsupportedOperationException.class,iterator::remove);
        }
        for(int route=0;route<9;route++) {
            TreeMap<Integer,Integer> owner=new TreeMap<>();for(int i=0;i<8;i++)owner.put(i,i);
            Map<Integer,Integer> view=M3FilteredMaps.filtered(owner,(k,v)->k%2==0);
            switch(route) {
                case 0 -> view.entrySet().removeIf(e->e.getValue()%4==0);
                case 1 -> view.entrySet().removeAll(Set.of(entry(0,0),entry(4,4)));
                case 2 -> view.entrySet().retainAll(Set.of(entry(2,2),entry(6,6)));
                case 3 -> view.values().removeIf(v->v%4==0);
                case 4 -> view.values().removeAll(Set.of(0,4));
                case 5 -> view.values().retainAll(Set.of(2,6));
                case 6 -> view.keySet().removeIf(k->k%4==0);
                case 7 -> view.keySet().removeAll(Set.of(0,4));
                default -> view.keySet().retainAll(Set.of(2,6));
            }
            eq(owner.keySet(),Set.of(1,2,3,5,6,7));
        }
        Map<Integer,Integer> raw=new LinkedHashMap<>();raw.put(1,1);
        var readOnly=M3FilteredMaps.filtered(Collections.unmodifiableMap(raw),(k,v)->true);
        raises(UnsupportedOperationException.class,()->readOnly.put(1,2));
        raises(UnsupportedOperationException.class,()->readOnly.entrySet().iterator().next().setValue(2));
        eq(raw,Map.of(1,1));
    }

    private static void sequenced() {
        for (boolean reverse : new boolean[]{false,true}) {
            TreeMap<Integer,Integer> owner=new TreeMap<>();
            owner.put(1,10);owner.put(2,-20);owner.put(3,30);owner.put(4,40);
            SortedMap<Integer,Integer> sorted=M3FilteredMaps.filtered(owner,(k,v)->v>0);
            java.util.SequencedMap<Integer,Integer> view=reverse?sorted.reversed():sorted;
            eq(new ArrayList<>(view.sequencedKeySet()),reverse?List.of(4,3,1):List.of(1,3,4));
            eq(new ArrayList<>(view.sequencedValues()),reverse?List.of(40,30,10):List.of(10,30,40));
            var entries=view.sequencedEntrySet();
            raises(IllegalArgumentException.class,()->entries.getFirst().setValue(-9));
            raises(IllegalArgumentException.class,()->entries.getLast().setValue(-9));
            raises(IllegalArgumentException.class,()->entries.reversed().iterator().next().setValue(-9));
            raises(UnsupportedOperationException.class,()->view.putFirst(0,10));
            raises(UnsupportedOperationException.class,()->view.putLast(5,50));
            eq(view.pollFirstEntry(),reverse?entry(4,40):entry(1,10));
            view.sequencedValues().remove(30);
            eq(view.pollLastEntry(),reverse?entry(1,10):entry(4,40));
            eq(owner,Map.of(2,-20));
        }
    }

    private static void lazy() {
        TreeMap<Integer,Integer> owner=new TreeMap<>();for(int i=0;i<20_000;i++)owner.put(i,i);
        AtomicInteger calls=new AtomicInteger();
        var view=M3FilteredMaps.filtered(owner,(k,v)->{calls.incrementAndGet();return k%2==0;});
        for(Collection<?> projection:List.of(view.entrySet(),view.values(),view.keySet())) {
            calls.set(0);var s=projection.spliterator();s.estimateSize();s.characteristics();
            eq(s.getExactSizeIfKnown(),-1L);eq(calls.get(),0);
            check(projection.stream().findFirst().isPresent(),"lazy first");eq(calls.get(),1);
        }
        calls.set(0);eq(view.values().parallelStream().mapToLong(Integer::longValue).sum(),99_990_000L);eq(calls.get(),20_000);
        eq(view.values().stream().toList(),view.values().parallelStream().toList());

    }

    public static void main(String[] args) {
        basicAndDefaults();storedRepresentatives();ranges();nulls();mutationBoundaries();sequenced();lazy();
        System.out.println("M3_FILTERED_MAPS_PASS checks="+checks);
    }
}
