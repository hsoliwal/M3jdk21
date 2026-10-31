// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
package com.m3.collections;

import java.lang.reflect.Field;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

/** Independent deterministic oracles, compiled against exact target source. */
public final class M3ZeroSlotCapacityContract {
    private static long checks;
    private M3ZeroSlotCapacityContract() { }
    private static void check(boolean condition, String context) {
        checks++;
        if (!condition) throw new AssertionError(context);
    }
    private static void eq(long a, long b, String context) { check(a == b, context + ": " + a + " != " + b); }
    private static void eq(Object a, Object b, String context) {
        check(Objects.equals(a,b), context + ": " + a + " != " + b);
    }
    private static void ids(int[] ids, Set<Integer> expected, String context) {
        HashSet<Integer> found = new HashSet<>();
        for (int v : ids) check(found.add(v), context + " duplicate " + v);
        eq(expected, found, context + " members");
    }
    private static int key(Random random, int step) {
        return switch (step % 13) {
            case 0 -> 0;
            case 1 -> Integer.MAX_VALUE;
            case 2 -> Integer.MIN_VALUE;
            case 3 -> -1;
            case 4 -> 1;
            default -> random.nextInt(513) - 256;
        };
    }
    private static void ensureNoNodeFields(Class<?> c) {
        for (Field field : c.getDeclaredFields()) {
            if (field.getType().isArray()) {
                check(field.getType().getComponentType().isPrimitive(), c + " object array " + field);
            }
            check(!Map.Entry.class.isAssignableFrom(field.getType()), c + " entry owner " + field);
            check(!field.getType().getSimpleName().endsWith("Node"), c + " node owner " + field);
        }
    }
    private static void capacity(boolean required) {
        M3IntHashSet a = new M3IntHashSet();
        M3IntIntHashMap b = new M3IntIntHashMap();
        M3IntLongHashMap c = new M3IntLongHashMap();
        for (int i = 1; i <= 5; i++) {
            check(a.add(i), "set initial"); check(b.put(i, i), "int map initial");
            check(c.put(i, (long) i), "long map initial");
        }
        if (required) {
            eq(8, a.capacity(), "set before zero capacity");
            eq(8, b.capacity(), "int map before zero capacity");
            eq(8, c.capacity(), "long map before zero capacity");
        }
        check(a.add(0), "set zero"); check(b.put(0, 0), "int map zero"); check(c.put(0, 0L), "long map zero");
        if (required) {
            eq(8, a.capacity(), "set zero must not grow");
            eq(8, b.capacity(), "int map zero must not grow");
            eq(8, c.capacity(), "long map zero must not grow");
            eq(32L, a.payloadBytes(), "set off-lane payload");
            eq(64L, b.payloadBytes(), "int map off-lane payload");
            eq(96L, c.payloadBytes(), "long map off-lane payload");
        }
        eq(6, a.size(), "set zero increments logical size");
        eq(6, b.size(), "int map zero increments logical size");
        eq(6, c.size(), "long map zero increments logical size");
        check(!a.add(0), "set repeat zero no-op");
        check(!b.put(0, 100), "int map repeat zero replace");
        check(!c.put(0, 100L), "long map repeat zero replace");
        eq(100, b.getOrDefault(0, -1), "int map zero value");
        eq(100L, c.getOrDefault(0, -1), "long map zero value");
        check(a.add(6), "set sixth nonzero");
        check(b.put(6, 6), "int map sixth nonzero");
        check(c.put(6, 6L), "long map sixth nonzero");
        eq(16, a.capacity(), "set needed growth");
        eq(16, b.capacity(), "int map needed growth");
        eq(16, c.capacity(), "long map needed growth");
        for (int i = 0; i <= 6; i++) {
            check(a.contains(i), "set survives growth");
            check(b.containsKey(i), "int map survives growth");
            check(c.containsKey(i), "long map survives growth");
        }
        M3IntHashSet first = new M3IntHashSet();
        M3IntIntHashMap second = new M3IntIntHashMap();
        M3IntLongHashMap third = new M3IntLongHashMap();
        first.add(0); second.put(0, 10); third.put(0, 20);
        for (int i = 1; i <= 5; i++) {
            first.add(i); second.put(i, i); third.put(i, i);
        }
        if (required) {
            eq(8, first.capacity(), "set zero before nonzero");
            eq(8, second.capacity(), "int map zero before nonzero");
            eq(8, third.capacity(), "long map zero before nonzero");
        }
        first.remove(0); second.remove(0); third.remove(0);
        eq(5, first.size(), "set zero removal");
        eq(5, second.size(), "int map zero removal");
        eq(5, third.size(), "long map zero removal");
    }
    private static void set(int loops) {
        M3IntHashSet actual = new M3IntHashSet(0);
        Set<Integer> expected = new HashSet<>();
        Random random = new Random(0x334134);
        for (int step = 0; step < loops; step++) {
            int k=key(random, step);
            switch (random.nextInt(7)) {
                case 0, 1 -> check(actual.add(k) == expected.add(k), "set add " + step);
                case 2 -> check(actual.remove(k) == expected.remove(k), "set remove " + step);
                case 3 -> check(actual.contains(k) == expected.contains(k), "set contains " + step);
                case 4 -> { if ((step & 1023) == 0) { actual.clear(); expected.clear(); } }
                case 5 -> { // Repeated zero toggling must not reorder/lose other keys.
                    check(actual.add(0) == expected.add(0), "set zero add " + step);
                }
                default -> check(actual.contains(Integer.MAX_VALUE) == expected.contains(Integer.MAX_VALUE), "set max");
            }
            eq(expected.size(), actual.size(), "set size " + step);
            if ((step & 255) == 0) {
                ids(actual.toArray(),expected,"set snapshot " + step);
                Set<Integer> iterated = new HashSet<>();
                M3IntIterator itr = actual.iterator();
                while (itr.hasNext()) check(iterated.add(itr.nextInt()), "set iterator duplicate");
                eq(expected,iterated,"set iterated");
            }
        }
    }
    private static void intMap(int loops) {
        M3IntIntHashMap actual=new M3IntIntHashMap(0);
        Map<Integer,Integer> expected=new HashMap<>();
        Random random=new Random(0x776633);
        for (int step=0;step<loops;step++) {
            int k=key(random,step), v=random.nextInt(1001)-500;
            switch(random.nextInt(10)) {
                case 0,1 -> {
                    boolean inserted=!expected.containsKey(k);
                    expected.put(k,v);
                    check(actual.put(k,v)==inserted,"int map put");
                }
                case 2 -> check(actual.remove(k)==(expected.remove(k)!=null),"int map remove");
                case 3 -> eq(expected.getOrDefault(k,-10001).longValue(),actual.getOrDefault(k,-10001),"int map get");
                case 4 -> check(expected.containsKey(k)==actual.containsKey(k),"int map contains");
                case 5 -> {
                    int prev=expected.getOrDefault(k,0);
                    int next=prev+v;
                    expected.put(k,next);
                    eq(next,actual.addTo(k,v),"int map addTo");
                }
                case 6 -> {
                    int old=expected.getOrDefault(k,0);
                    boolean changed=expected.containsKey(k)&&old==v;
                    if(changed)expected.put(k,v+1);
                    check(actual.replace(k,v,v+1)==changed,"int map replace");
                }
                case 7 -> check(actual.remove(k,v)==expected.remove(k,v),"int map conditional remove");
                case 8 -> {
                    if((step&1023)==0){expected.clear();actual.clear();}
                }
                default -> check(actual.containsValue(v)==expected.containsValue(v),"int map containsValue");
            }
            eq(expected.size(),actual.size(),"int map size");
            if((step&255)==0){
                Map<Integer,Integer> got=new HashMap<>();actual.forEach((x,y)->{
                    if(got.put(x,y)!=null)throw new AssertionError("int map duplicate");
                });
                eq(expected,got,"int map traversal");
                Set<Integer> keys=new HashSet<>();for(int x:actual.keysToArray())check(keys.add(x),"int map duplicate keys");
                eq(expected.keySet(),keys,"int map key snapshot");
            }
        }
    }
    private static void longMap(int loops) {
        M3IntLongHashMap actual=new M3IntLongHashMap(0);
        Map<Integer,Long> expected=new HashMap<>();
        Random random=new Random(0x226644);
        for(int step=0;step<loops;step++){
            int k=key(random,step),small=random.nextInt(1001)-500;
            long v=(step%11==0?Long.MIN_VALUE:step%13==0?Long.MAX_VALUE:(long)small*7L);
            switch(random.nextInt(10)){
                case 0,1 -> {boolean inserted=!expected.containsKey(k);expected.put(k,v);check(actual.put(k,v)==inserted,"long map put");}
                case 2 -> check(actual.remove(k)==(expected.remove(k)!=null),"long map remove");
                case 3 -> eq(expected.getOrDefault(k,Long.MIN_VALUE+1).longValue(),actual.getOrDefault(k,Long.MIN_VALUE+1),"long map get");
                case 4 -> check(expected.containsKey(k)==actual.containsKey(k),"long map contains");
                case 5 -> {long next=expected.getOrDefault(k,0L)+v;expected.put(k,next);eq(next,actual.addTo(k,v),"long map addTo");}
                case 6 -> {long old=expected.getOrDefault(k,0L);boolean changed=expected.containsKey(k)&&old==v;if(changed)expected.put(k,v+1);check(actual.replace(k,v,v+1)==changed,"long map replace");}
                case 7 -> check(actual.remove(k,v)==expected.remove(k,v),"long map conditional remove");
                case 8 -> {if((step&1023)==0){expected.clear();actual.clear();}}
                default -> check(actual.containsValue(v)==expected.containsValue(v),"long map containsValue");
            }
            eq(expected.size(),actual.size(),"long map size");
            if((step&255)==0){
                Map<Integer,Long> got=new HashMap<>();actual.forEach((x,y)->{
                    if(got.put(x,y)!=null)throw new AssertionError("long map duplicate");
                });
                eq(expected,got,"long map traversal");
                Set<Integer> keys=new HashSet<>();for(int x:actual.keysToArray())check(keys.add(x),"long map duplicate keys");
                eq(expected.keySet(),keys,"long map keys");
            }
        }
    }
    private static void iterators() {
        M3IntHashSet s=new M3IntHashSet();s.add(0);s.add(1);
        M3IntIterator a=s.iterator();eq(0,a.nextInt(),"set zero first");check(!s.add(0),"set duplicate zero");eq(1,a.nextInt(),"set duplicate does not invalidate");
        a=s.iterator();s.add(2);
        try{a.hasNext();throw new AssertionError("set new key should invalidate iterator");}
        catch(ConcurrentModificationException okay){checks++;}
        M3IntIntHashMap m=new M3IntIntHashMap();m.put(0,10);m.put(1,11);
        M3IntIterator b=m.keyIterator();eq(0,b.nextInt(),"int map zero first");check(!m.put(0,99),"int map existing zero");eq(1,b.nextInt(),"int map nonstructural write keeps iterator");
        b=m.keyIterator();m.put(2,2);
        try{b.hasNext();throw new AssertionError("int map new key must invalidate");}
        catch(ConcurrentModificationException okay){checks++;}
        M3IntLongHashMap l=new M3IntLongHashMap();l.put(0,10L);l.put(1,11L);
        M3IntIterator c=l.keyIterator();eq(0,c.nextInt(),"long map zero first");check(!l.put(0,99L),"long map existing zero");eq(1,c.nextInt(),"long map nonstructural write keeps iterator");
        c=l.keyIterator();l.put(2,2L);
        try{c.hasNext();throw new AssertionError("long map new key must invalidate");}
        catch(ConcurrentModificationException okay){checks++;}
    }
    public static void main(String[] args) {
        boolean physical = args.length == 0 || !args[0].equals("--values-only");
        ensureNoNodeFields(M3IntHashSet.class);
        ensureNoNodeFields(M3IntIntHashMap.class);
        ensureNoNodeFields(M3IntLongHashMap.class);
        capacity(physical);
        set(65_537);
        intMap(65_537);
        longMap(65_537);
        iterators();
        System.out.println("PASS M3ZeroSlotCapacityContract checks="+checks+" operations=196611 physical="+physical);
    }
}
