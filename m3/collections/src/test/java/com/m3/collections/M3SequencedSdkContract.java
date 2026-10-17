/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.*;

/** Actual Java 21 JDK oracle over present M3 slot/link owners. */
public final class M3SequencedSdkContract {
    private static long checks;
    private static void check(boolean ok) { checks++; if (!ok) throw new AssertionError("check="+checks); }
    private static void eq(Object expected,Object actual) {
        checks++; if (!Objects.equals(expected,actual))throw new AssertionError("check="+checks+" expected="+expected+" actual="+actual);
    }
    private static void mapOracle() {
        Random rng=new Random(20261017L);
        for(int seed=0;seed<64;seed++) {
            SequencedMap<Integer,Integer> a=M3CollectionSdk.jdkSequencedMap();
            SequencedMap<Integer,Integer> b=new LinkedHashMap<>();
            for(int step=0;step<768;step++) {
                Integer key=rng.nextInt(7)==0?null:rng.nextInt(50), val=rng.nextInt(13)==0?null:rng.nextInt(100);
                switch(rng.nextInt(11)) {
                    case 0,1 -> eq(b.put(key,val),a.put(key,val));
                    case 2 -> eq(b.putFirst(key,val),a.putFirst(key,val));
                    case 3 -> eq(b.putLast(key,val),a.putLast(key,val));
                    case 4 -> eq(b.remove(key),a.remove(key));
                    case 5 -> eq(b.reversed().putFirst(key,val),a.reversed().putFirst(key,val));
                    case 6 -> eq(b.reversed().putLast(key,val),a.reversed().putLast(key,val));
                    case 7 -> eq(b.pollFirstEntry(),a.pollFirstEntry());
                    case 8 -> eq(b.pollLastEntry(),a.pollLastEntry());
                    case 9 -> {eq(b.firstEntry(),a.firstEntry());eq(b.lastEntry(),a.lastEntry());}
                    default -> {eq(b.get(key),a.get(key));eq(b.containsKey(key),a.containsKey(key));}
                }
                eq(new ArrayList<>(b.sequencedKeySet()),new ArrayList<>(a.sequencedKeySet()));
                eq(new ArrayList<>(b.reversed().sequencedKeySet()),new ArrayList<>(a.reversed().sequencedKeySet()));
                eq(b,a);
                ((M3LinkedHashMap<Integer,Integer>) a).verifyInvariants();
            }
        }
    }
    private static void setOracle() {
        Random rng=new Random(29999L);
        for(int seed=0;seed<64;seed++) {
            SequencedSet<Integer> a=M3CollectionSdk.jdkSequencedSet();
            SequencedSet<Integer> b=new LinkedHashSet<>();
            for(int step=0;step<768;step++) {
                Integer value=rng.nextInt(8)==0?null:rng.nextInt(50);
                switch(rng.nextInt(10)) {
                    case 0,1 -> eq(b.add(value),a.add(value));
                    case 2 -> {a.addFirst(value);b.addFirst(value);}
                    case 3 -> {a.addLast(value);b.addLast(value);}
                    case 4 -> eq(b.remove(value),a.remove(value));
                    case 5 -> {a.reversed().addFirst(value);b.reversed().addFirst(value);}
                    case 6 -> {a.reversed().addLast(value);b.reversed().addLast(value);}
                    case 7 -> {if(!a.isEmpty())eq(b.removeFirst(),a.removeFirst());}
                    case 8 -> {if(!a.isEmpty())eq(b.removeLast(),a.removeLast());}
                    default -> eq(b.contains(value),a.contains(value));
                }
                eq(new ArrayList<>(b),new ArrayList<>(a));
                eq(new ArrayList<>(b.reversed()),new ArrayList<>(a.reversed()));
                eq(b,a);
                ((M3LinkedHashSet<Integer>)a).verifyInvariants();
            }
        }
    }
    private static void listOracle() {
        Random rng=new Random(9843L);
        for(int seed=0;seed<64;seed++) {
            SequencedCollection<Integer> a=M3CollectionSdk.jdkSequencedCollection();
            SequencedCollection<Integer> b=new LinkedList<>();
            for(int step=0;step<768;step++) {
                Integer value=rng.nextInt(9)==0?null:rng.nextInt(50);
                switch(rng.nextInt(7)) {
                    case 0,1 -> {a.addFirst(value);b.addFirst(value);}
                    case 2 -> {a.addLast(value);b.addLast(value);}
                    case 3 -> {a.reversed().addFirst(value);b.reversed().addFirst(value);}
                    case 4 -> {a.reversed().addLast(value);b.reversed().addLast(value);}
                    case 5 -> {if(!a.isEmpty())eq(b.removeFirst(),a.removeFirst());}
                    default -> {if(!a.isEmpty())eq(b.removeLast(),a.removeLast());}
                }
                eq(new ArrayList<>(b),new ArrayList<>(a));
                eq(new ArrayList<>(b.reversed()),new ArrayList<>(a.reversed()));
                if(!a.isEmpty()) {eq(b.getFirst(),a.getFirst());eq(b.getLast(),a.getLast());}
            }
        }
    }
    public static void main(String[] args) {
        mapOracle();setOracle();listOracle();
        System.out.println("PASS M3_SEQUENCED_SDK checks="+checks+" mutations="+(64*768*3));
    }
}
