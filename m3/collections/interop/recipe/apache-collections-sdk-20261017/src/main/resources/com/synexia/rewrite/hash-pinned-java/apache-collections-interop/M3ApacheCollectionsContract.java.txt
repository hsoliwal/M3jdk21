/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections.interop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
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
        for(int k=-1;k<24;k++) {
            Integer key=k<0?null:k;
            eq(want.containsKey(key),got.containsKey(key));
            eq(want.get(key),got.get(key));
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
                case 4 -> {b.get(k).add(v);a.get(k).add(v);}
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
                case 4 -> {b.get(k).add(v);a.get(k).add(v);}
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
    public static void main(String[] args) {
        if(args.length!=0)throw new IllegalArgumentException("no args");
        list();set();
        System.out.println("PASS M3_APACHE_MULTIVALUED_MAP contracts="+checks+" operations=4600");
    }
}
