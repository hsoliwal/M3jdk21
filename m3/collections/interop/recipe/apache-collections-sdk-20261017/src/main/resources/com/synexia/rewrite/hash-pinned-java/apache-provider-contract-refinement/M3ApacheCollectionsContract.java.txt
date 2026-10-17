/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections.interop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;
import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.SetValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedLinkedHashMap;
import org.apache.commons.collections4.multimap.LinkedHashSetValuedLinkedHashMap;

/** Actual 4.5.0 provider differential oracle (test dependency; no stub allowed). */
public final class M3ApacheCollectionsContract {
    private static long checks;
    private M3ApacheCollectionsContract() { }
    private static void yes(boolean good) {
        checks++;
        if (!good) throw new AssertionError("contract check=" + checks);
    }
    private static void eq(Object want, Object got) {
        checks++;
        if (!Objects.equals(want,got)) throw new AssertionError("expected="+want+", actual="+got+", check="+checks);
    }
    private static void same(MultiValuedMap<Integer,Integer> want,
            MultiValuedMap<Integer,Integer> got) {
        eq(want.size(),got.size());
        eq(want.isEmpty(),got.isEmpty());
        eq(want.keySet(),got.keySet());
        eq(new ArrayList<>(want.keySet()),new ArrayList<>(got.keySet()));
        eq(want.asMap(),got.asMap());
        eq(want.keys(),got.keys());
        eq(new ArrayList<>(want.entries()),new ArrayList<>(got.entries()));
        eq(new ArrayList<>(want.values()),new ArrayList<>(got.values()));
        for(int k=-1;k<24;k++) {
            Integer key=k<0?null:k;
            eq(want.containsKey(key),got.containsKey(key));
            eq(want.get(key),got.get(key));
            eq(new ArrayList<>(want.get(key)),new ArrayList<>(got.get(key)));
        }
    }
    private static void list() {
        ListValuedMap<Integer,Integer> a=M3ApacheCollections.listValuedMap();
        ListValuedMap<Integer,Integer> b=new ArrayListValuedLinkedHashMap<>();
        Random r=new Random(0x3333_1122);
        for(int i=0;i<2400;i++) {
            Integer k=r.nextInt(9)==0?null:r.nextInt(24), v=r.nextInt(8)==0?null:r.nextInt(20);
            switch(r.nextInt(8)) {
                case 0,1 -> eq(b.put(k,v),a.put(k,v));
                case 2 -> eq(b.removeMapping(k,v),a.removeMapping(k,v));
                case 3 -> eq(b.remove(k),a.remove(k));
                case 4 -> eq(b.get(k).add(v),a.get(k).add(v));
                case 5 -> eq(b.putAll(k,Arrays.asList(v,v)),a.putAll(k,Arrays.asList(v,v)));
                case 6 -> {
                    if(!b.get(k).isEmpty()) {
                        int at=r.nextInt(b.get(k).size());
                        eq(b.get(k).remove(at),a.get(k).remove(at));
                    }
                }
                default -> {eq(b.containsMapping(k,v),a.containsMapping(k,v));}
            }
            same(b,a);
        }
        a.clear();b.clear();
        List<Integer> view=a.get(7);
        yes(!a.containsKey(7));view.addAll(List.of(1,1,2));
        eq(3,a.size());eq(List.of(1,1,2),a.get(7));
        eq(List.of(1,1,2),a.remove(7));yes(!a.containsKey(7));
        view.add(4);eq(List.of(4),a.get(7));
        var it=a.get(7).listIterator();eq(4,it.next());it.set(5);it.add(6);
        eq(List.of(5,6),a.get(7));
        eq(a.get(7),a.asMap().get(7));
        eq(2,a.keys().getCount(7));
        a.asMap().get(7).clear();yes(!a.containsKey(7));
    }
    private static void set() {
        SetValuedMap<Integer,Integer> a=M3ApacheCollections.setValuedMap();
        SetValuedMap<Integer,Integer> b=new LinkedHashSetValuedLinkedHashMap<>();
        Random r=new Random(0x7777_3344);
        for(int i=0;i<2200;i++) {
            Integer k=r.nextInt(9)==0?null:r.nextInt(24),v=r.nextInt(8)==0?null:r.nextInt(20);
            switch(r.nextInt(7)) {
                case 0,1 -> eq(b.put(k,v),a.put(k,v));
                case 2 -> eq(b.removeMapping(k,v),a.removeMapping(k,v));
                case 3 -> eq(b.remove(k),a.remove(k));
                case 4 -> eq(b.get(k).add(v),a.get(k).add(v));
                case 5 -> eq(b.putAll(k,Arrays.asList(v,v)),a.putAll(k,Arrays.asList(v,v)));
                default -> eq(b.containsMapping(k,v),a.containsMapping(k,v));
            }
            same(b,a);
        }
        a.clear();b.clear();
        yes(a.put(3,4));yes(!a.put(3,4));eq(1,a.size());
        Set<Integer> values=a.get(3);values.add(5);eq(Set.of(4,5),a.get(3));
        var entry=a.entries().iterator();yes(entry.hasNext());entry.next();entry.remove();
        eq(1,a.size());eq(a.get(3).iterator().next(),a.values().iterator().next());
        a.get(3).clear();yes(!a.containsKey(3));
    }
    private static Object outcome(Supplier<?> operation) {
        try { return operation.get(); }
        catch (RuntimeException failure) { return failure.getClass(); }
    }

    private static void orderedSetTrace() {
        SetValuedMap<Integer,Integer> a=M3ApacheCollections.setValuedMap();
        SetValuedMap<Integer,Integer> b=new LinkedHashSetValuedLinkedHashMap<>();
        Integer[][] pairs={{3,4},{7,8},{3,null},{3,5},{null,null},{3,4},{3,null}};
        for(Integer[] pair:pairs) {eq(b.put(pair[0],pair[1]),a.put(pair[0],pair[1]));same(b,a);}
        eq(b.removeMapping(3,4),a.removeMapping(3,4));same(b,a);
        eq(b.put(3,4),a.put(3,4));same(b,a);
        eq(b.remove(3),a.remove(3));same(b,a);
        eq(b.put(3,9),a.put(3,9));same(b,a);
        eq(b.remove(null),a.remove(null));same(b,a);
        eq(b.put(null,10),a.put(null,10));same(b,a);
    }

    private static void iteratorTrace(MultiValuedMap<Integer,Integer> b,
            MultiValuedMap<Integer,Integer> a) {
        for(Integer[] pair:new Integer[][]{{null,null},{3,4},{3,5},{7,8},{3,4}})
            eq(b.put(pair[0],pair[1]),a.put(pair[0],pair[1]));
        var expected=b.mapIterator();var actual=a.mapIterator();
        eq(outcome(expected::getKey),outcome(actual::getKey));
        eq(outcome(expected::getValue),outcome(actual::getValue));
        eq(outcome(()->expected.setValue(19)),outcome(()->actual.setValue(19)));
        eq(outcome(()->{expected.remove();return null;}),outcome(()->{actual.remove();return null;}));
        while(expected.hasNext()) {
            eq(expected.hasNext(),actual.hasNext());
            eq(expected.next(),actual.next());
            eq(expected.getKey(),actual.getKey());eq(expected.getValue(),actual.getValue());
            eq(outcome(()->expected.setValue(19)),outcome(()->actual.setValue(19)));
            expected.remove();actual.remove();same(b,a);
            eq(outcome(()->{expected.remove();return null;}),outcome(()->{actual.remove();return null;}));
            // Apache keeps its current API entry after remove; compare retained observations.
            eq(outcome(expected::getKey),outcome(actual::getKey));
            eq(outcome(expected::getValue),outcome(actual::getValue));
        }
        eq(expected.hasNext(),actual.hasNext());
        eq(outcome(expected::next),outcome(actual::next));
        var emptyExpected=b.mapIterator();var emptyActual=a.mapIterator();
        eq(outcome(emptyExpected::getKey),outcome(emptyActual::getKey));
        eq(outcome(emptyExpected::getValue),outcome(emptyActual::getValue));
        eq(outcome(emptyExpected::next),outcome(emptyActual::next));
        eq(outcome(()->{emptyExpected.remove();return null;}),
                outcome(()->{emptyActual.remove();return null;}));
    }

    private static void heldViewTrace(MultiValuedMap<Integer,Integer> b,
            MultiValuedMap<Integer,Integer> a) {
        for(Integer[] pair:new Integer[][]{{3,4},{3,5},{7,8},{null,null}})
            eq(b.put(pair[0],pair[1]),a.put(pair[0],pair[1]));
        Collection<Integer> bg=b.get(3),ag=a.get(3),missingB=b.get(9),missingA=a.get(9);
        Map<Integer,Collection<Integer>> bm=b.asMap(),am=a.asMap();
        Collection<Integer> mappedB=bm.get(3),mappedA=am.get(3);
        var keysB=b.keySet();var keysA=a.keySet();
        var countsB=b.keys();var countsA=a.keys();
        var entriesB=b.entries();var entriesA=a.entries();
        var valuesB=b.values();var valuesA=a.values();
        b.clear();a.clear();
        eq(bg.isEmpty(),ag.isEmpty());eq(mappedB.isEmpty(),mappedA.isEmpty());
        eq(bm.get(3),am.get(3));eq(countsB,countsA);
        eq(new ArrayList<>(keysB),new ArrayList<>(keysA));
        eq(new ArrayList<>(entriesB),new ArrayList<>(entriesA));
        eq(new ArrayList<>(valuesB),new ArrayList<>(valuesA));same(b,a);
        for(Integer[] pair:new Integer[][]{{7,10},{3,11},{null,12}})
            eq(b.put(pair[0],pair[1]),a.put(pair[0],pair[1]));
        eq(bg.add(13),ag.add(13));same(b,a);
        eq(mappedB.remove(11),mappedA.remove(11));same(b,a);
        eq(missingB.add(null),missingA.add(null));same(b,a);
        bg.clear();ag.clear();same(b,a);
        eq(outcome(()->mappedB.add(14)),outcome(()->mappedA.add(14)));same(b,a);
        eq(outcome(()->countsB.remove(7)),outcome(()->countsA.remove(7)));same(b,a);
        eq(new ArrayList<>(keysB),new ArrayList<>(keysA));
        eq(new ArrayList<>(entriesB),new ArrayList<>(entriesA));
        eq(new ArrayList<>(valuesB),new ArrayList<>(valuesA));
        var bi=entriesB.iterator();var ai=entriesA.iterator();
        eq(bi.next(),ai.next());bi.remove();ai.remove();same(b,a);
        eq(bm.remove(null),am.remove(null));same(b,a);
    }

    public static void main(String[] args) {
        if(args.length!=0)throw new IllegalArgumentException("no args");
        list();set();orderedSetTrace();
        iteratorTrace(new ArrayListValuedLinkedHashMap<>(),M3ApacheCollections.listValuedMap());
        iteratorTrace(new LinkedHashSetValuedLinkedHashMap<>(),M3ApacheCollections.setValuedMap());
        heldViewTrace(new ArrayListValuedLinkedHashMap<>(),M3ApacheCollections.listValuedMap());
        heldViewTrace(new LinkedHashSetValuedLinkedHashMap<>(),M3ApacheCollections.setValuedMap());
        System.out.println("PASS M3_APACHE_MULTIVALUED_MAP contracts="+checks+" operations=4600");
    }
}
