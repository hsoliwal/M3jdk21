/* SPDX-License-Identifier: Apache-2.0; Copyright 2026 Hitesh Soliwal and contributors. */
package com.m3.collections.interop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.impl.multimap.list.FastListMultimap;

/** Actual Eclipse Collections 11.1.0 binary differential oracle; no API stubs. */
public final class M3EclipseListMultimapSelfTest {
    private static long assertions;
    private M3EclipseListMultimapSelfTest() { }
    private static void eq(Object expected, Object actual, String context) {
        assertions++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(context+": expected="+expected+", actual="+actual+", check="+assertions);
        }
    }
    private static void state(MutableListMultimap<Integer, Integer> expected,
            MutableListMultimap<Integer, Integer> actual, String context) {
        eq(expected.size(), actual.size(), context+" size");
        eq(expected.keySet(), actual.keySet(), context+" keys");
        eq(expected.keysView().toBag(), actual.keysView().toBag(), context+" key counts");
        for (int i=-1;i<24;i++) {
            Integer key=i<0?null:i;
            eq(expected.containsKey(key), actual.containsKey(key), context+" contains key "+key);
            eq(expected.get(key).toList(), actual.get(key).toList(), context+" key "+key);
        }
    }
    public static void main(String[] args) {
        if(args.length!=0)throw new IllegalArgumentException("no arguments");
        var expected=new FastListMultimap<Integer,Integer>();
        var actual=new M3EclipseListMultimap<Integer,Integer>();
        Random r=new Random(0x4d3345434c495053L);
        for(int step=0;step<3000;step++) {
            Integer key=r.nextInt(13)==0?null:r.nextInt(24);
            Integer value=r.nextInt(9)==0?null:r.nextInt(50);
            switch(r.nextInt(8)) {
                case 0,1 -> {
                    eq(expected.put(key,value),actual.put(key,value),"put return " + step);
                }
                case 2 -> {
                    eq(expected.remove(key,value),actual.remove(key,value),"remove return " + step);
                }
                case 3 -> {
                    eq(expected.removeAll(key).toList(),actual.removeAll(key).toList(),"removeAll return " + step);
                }
                case 4 -> {
                    eq(expected.replaceValues(key,Arrays.asList(value,value)).toList(),
                       actual.replaceValues(key,Arrays.asList(value,value)).toList(),"replace return " + step);
                }
                case 5 -> {
                    expected.putAll(key,Arrays.asList(value,value));
                    actual.putAll(key,Arrays.asList(value,value));
                }
                case 6 -> {
                    eq(expected.containsKey(key),actual.containsKey(key),"contains " + step);
                }
                default -> {
                    eq(expected.containsKeyAndValue(key,value),actual.containsKeyAndValue(key,value),"pair " + step);
                }
            }
            state(expected,actual,"step "+step);
        }
        var selected=actual.selectKeysValues((k,v)->v!=null&&v%2==0);
        for(int i=-1;i<24;i++) {
            Integer key=i<0?null:i;
            eq(expected.selectKeysValues((k,v)->v!=null&&v%2==0).get(key).toList(),
                    selected.get(key).toList(),"select " +key);
        }
        var rejected=actual.rejectKeysValues((k,v)->v!=null&&v%2==0);
        eq(expected.rejectKeysValues((k,v)->v!=null&&v%2==0).size(),rejected.size(),"reject count");
        eq(expected.flip().size(),actual.flip().size(),"flip size");
        eq(0,actual.newEmpty().size(),"newEmpty size");
        actual.clear();expected.clear();
        var vs=actual.get(4);
        eq(0,vs.size(),"absent list");
        boolean refuses=false;
        try { vs.add(17); } catch (UnsupportedOperationException expectedFailure) { refuses=true; }
        eq(true, refuses, "Eclipse absent-value list is immutable");
        eq(expected.get(4).size(),actual.get(4).size(),"absent mutation semantics");
        System.out.println("PASS M3_ECLIPSE_LIST_MULTIMAP checks="+assertions+" provider=Eclipse-Collections-11.1.0");
    }
}
