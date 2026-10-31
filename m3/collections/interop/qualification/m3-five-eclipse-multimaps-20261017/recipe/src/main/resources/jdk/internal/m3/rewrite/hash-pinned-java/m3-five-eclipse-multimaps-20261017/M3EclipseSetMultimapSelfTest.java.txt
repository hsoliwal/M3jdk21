/* SPDX-License-Identifier: Apache-2.0; Copyright 2026 Hitesh Soliwal and contributors. */
package com.m3.collections.interop;

import java.util.Arrays;
import java.util.Objects;
import java.util.Random;
import org.eclipse.collections.api.multimap.set.MutableSetMultimap;
import org.eclipse.collections.impl.multimap.set.UnifiedSetMultimap;

/** Seeded real Eclipse 11.1.0 binary differential, including high-collision M3 keys. */
public final class M3EclipseSetMultimapSelfTest {
    private static long checks;
    private M3EclipseSetMultimapSelfTest() { }
    private static void eq(Object want,Object got,String context) {
        checks++;
        if(!Objects.equals(want,got)) throw new AssertionError(context+" want="+want+" got="+got+" #"+checks);
    }
    private static void state(MutableSetMultimap<Integer,Integer> ref,
            MutableSetMultimap<Integer,Integer> actual,String note) {
        eq(ref.size(),actual.size(),note+" pair cardinality");
        eq(ref.keySet(),actual.keySet(),note+" keys");
        eq(ref.keyBag(),actual.keyBag(),note+" keyBag multiplicity");
        for(int k=-1;k<24;k++){
            Integer key=k<0?null:k;
            eq(ref.containsKey(key),actual.containsKey(key),note+" containsKey");
            eq(ref.get(key).toSet(),actual.get(key).toSet(),note+" value set");
        }
    }
    public static void main(String[] arguments) {
        if(arguments.length!=0)throw new IllegalArgumentException("no arguments");
        var ref=new UnifiedSetMultimap<Integer,Integer>();
        var actual=new M3EclipseSetMultimap<Integer,Integer>();
        Random rng=new Random(0x4d334543534554L);
        for(int step=0;step<3000;step++){
            Integer key=rng.nextInt(11)==0?null:rng.nextInt(24);
            Integer value=rng.nextInt(9)==0?null:rng.nextInt(50);
            switch(rng.nextInt(8)){
                case 0,1 -> eq(ref.put(key,value),actual.put(key,value),"put");
                case 2 -> eq(ref.remove(key,value),actual.remove(key,value),"remove pair");
                case 3 -> eq(ref.removeAll(key).toSet(),actual.removeAll(key).toSet(),"removeAll");
                case 4 -> eq(ref.replaceValues(key,Arrays.asList(value,value)).toSet(),
                        actual.replaceValues(key,Arrays.asList(value,value)).toSet(),"replaceValues");
                case 5 -> {ref.putAll(key,Arrays.asList(value,value));actual.putAll(key,Arrays.asList(value,value));}
                case 6 -> eq(ref.containsKeyAndValue(key,value),actual.containsKeyAndValue(key,value),"pair existence");
                default -> eq(ref.containsValue(value),actual.containsValue(value),"value existence");
            }
            state(ref,actual,"step "+step);
        }
        eq(ref.flip().size(),actual.flip().size(),"flipped pair count");
        for(int k=-1;k<24;k++){
            Integer key=k<0?null:k;
            eq(ref.selectKeysValues((x,v)->v!=null&&v%2==0).get(key).toSet(),
                    actual.selectKeysValues((x,v)->v!=null&&v%2==0).get(key).toSet(),"select "+key);
            eq(ref.rejectKeysValues((x,v)->v!=null&&v%2==0).get(key).toSet(),
                    actual.rejectKeysValues((x,v)->v!=null&&v%2==0).get(key).toSet(),"reject "+key);
        }
        eq(0,actual.newEmpty().size(),"newEmpty");
        System.out.println("PASS M3_ECLIPSE_SET_MULTIMAP checks="+checks+" provider=Eclipse-Collections-11.1.0");
    }
}
