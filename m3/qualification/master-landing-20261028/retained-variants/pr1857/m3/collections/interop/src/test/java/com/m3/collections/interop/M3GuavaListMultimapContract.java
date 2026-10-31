/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections.interop;

import com.google.common.collect.Multiset;
import com.m3.collections.M3IndexPathMasks;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

/** Standalone deterministic JDK oracle; authentic Guava-JAR JUnit runs separately. */
public final class M3GuavaListMultimapContract {
    private static long checks;
    private M3GuavaListMultimapContract() { }
    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
    private static void equal(Object expected, Object actual, String label) {
        checks++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(label + " expected=" + expected + " actual=" + actual);
        }
    }
    private static int pairs(LinkedHashMap<Integer,List<Integer>> oracle) {
        return oracle.values().stream().mapToInt(List::size).sum();
    }
    private static List<Map.Entry<Integer,Integer>> flatten(Map<Integer,List<Integer>> source) {
        List<Map.Entry<Integer,Integer>> output = new ArrayList<>();
        source.forEach((key,values) -> values.forEach(v -> output.add(new AbstractMap.SimpleImmutableEntry<>(key,v))));
        return output;
    }
    private static void checkState(M3GuavaListMultimap<Integer,Integer> actual,
            LinkedHashMap<Integer,List<Integer>> expected, String label) {
        equal(pairs(expected),actual.size(),label+" pair count");
        equal(expected.isEmpty(),actual.isEmpty(),label+" empty");
        equal(expected,actual.asMap(),label+" asMap");
        equal(expected.keySet(),actual.keySet(),label+" keySet");
        equal(flatten(expected),new ArrayList<>(actual.entries()),label+" entries");
        equal(flatten(expected).stream().map(Map.Entry::getValue).toList(),
                new ArrayList<>(actual.values()),label+" values");
        equal(flatten(expected).stream().map(Map.Entry::getKey).toList(),
                new ArrayList<>(actual.keys()),label+" keys");
        for(int key=-8;key<=8;key++){
            int n=expected.getOrDefault(key, List.of()).size();
            equal(n,actual.keys().count(key),label+" key count="+key);
            equal(expected.containsKey(key),actual.containsKey(key),label+" contains="+key);
            equal(expected.getOrDefault(key,List.of()),actual.get(key),label+" get="+key);
        }
        for(Map.Entry<Integer,List<Integer>> e:expected.entrySet()){
            for(Integer v:e.getValue())check(actual.containsEntry(e.getKey(),v),label+" containsEntry");
        }
    }
    private static void append(Map<Integer,List<Integer>> oracle,Integer key,Integer value){
        oracle.computeIfAbsent(key,ignore->new ArrayList<>()).add(value);
    }
    private static void replace(Map<Integer,List<Integer>> oracle,Integer key,List<Integer> values){
        if(values.isEmpty())oracle.remove(key);
        else if(oracle.containsKey(key)){List<Integer> data=oracle.get(key);data.clear();data.addAll(values);}
        else oracle.put(key,new ArrayList<>(values));
    }
    private static void removePair(Map<Integer,List<Integer>> oracle,Integer key,Integer value){
        List<Integer> data=oracle.get(key);
        if(data!=null && data.remove(value)&&data.isEmpty())oracle.remove(key);
    }
    public static void run(){
        checks=0;
        Random rnd=new Random(0x5EC7ED77L);
        M3GuavaListMultimap<Integer,Integer> actual=new M3GuavaListMultimap<>();
        LinkedHashMap<Integer,List<Integer>> oracle=new LinkedHashMap<>();
        for(int step=0;step<6400;step++){
            Integer key=rnd.nextInt(9)==0?null:rnd.nextInt(17)-8;
            Integer value=rnd.nextInt(13)==0?null:rnd.nextInt(19)-9;
            int op=rnd.nextInt(15);
            switch(op){
                case 0,1,2 ->{check(actual.put(key,value),"put");append(oracle,key,value);}
                case 3 ->{boolean was=oracle.containsKey(key) && oracle.get(key).contains(value);
                    equal(was,actual.remove(key,value),"remove pair");removePair(oracle,key,value);}
                case 4 ->{List<Integer> expected=oracle.remove(key);
                    equal(expected==null?List.of():expected,actual.removeAll(key),"removeAll");}
                case 5 ->{List<Integer> values=Arrays.asList(value,value,null);
                    List<Integer> prior=oracle.get(key);
                    equal(prior==null?List.of():new ArrayList<>(prior),actual.replaceValues(key,values),"replace");
                    replace(oracle,key,values);}
                case 6 ->{List<Integer> valueList=actual.get(key);int idx=valueList.size();
                    valueList.add(idx,value);append(oracle,key,value);}
                case 7 ->{List<Integer> valueList=actual.get(key);if(!valueList.isEmpty()){
                    int idx=rnd.nextInt(valueList.size());
                    equal(oracle.get(key).set(idx,value),valueList.set(idx,value),"list set");}}
                case 8 ->{List<Integer> valueList=actual.get(key);if(!valueList.isEmpty()){
                    int idx=rnd.nextInt(valueList.size());
                    equal(oracle.get(key).remove(idx),valueList.remove(idx),"list remove");
                    if(oracle.get(key).isEmpty())oracle.remove(key);}}
                case 9 ->{int was=oracle.getOrDefault(key,List.of()).size();
                    int n=rnd.nextInt(was+3);
                    equal(was,actual.keys().remove(key,n),"keys remove previous");
                    if(was>0&&n>0){List<Integer> list=oracle.get(key);
                        list.subList(0,Math.min(was,n)).clear();if(list.isEmpty())oracle.remove(key);}}
                case 10 ->{List<Integer> v=actual.get(key);List<Integer> data=oracle.get(key);
                    if(data!=null){v.clear();oracle.remove(key);}else v.clear();}
                case 11 ->{List<Integer> data=oracle.remove(key);
                    equal(data,actual.asMap().remove(key),"asMap remove");}
                case 12 -> {List<Integer> p=Arrays.asList(value,value);
                    check(actual.putAll(key,p),"putAll");append(oracle,key,value);append(oracle,key,value);}
                case 13 ->{if(!oracle.isEmpty()){
                    Integer keyToRemove=oracle.keySet().iterator().next();
                    var iter=actual.keySet().iterator();equal(keyToRemove,iter.next(),"keys iterator");
                    iter.remove();oracle.remove(keyToRemove);}}
                case 14 ->{if(!oracle.isEmpty()){
                    var iter=actual.entries().iterator();var first=iter.next();
                    Map.Entry<Integer,Integer> ref=flatten(oracle).get(0);
                    equal(ref,first,"entry iterator");iter.remove();removePair(oracle,ref.getKey(),ref.getValue());}}
                default ->throw new AssertionError();
            }
            if(step%23==0)checkState(actual,oracle,"step="+step);
        }
        checkState(actual,oracle,"final");
        // Freeze one full-32-bit physical state word per pair; the native path only
        // reads primitive ordinals/masks and never sees key or value references.
        int count=actual.size();
        int[] states=new int[count];
        List<Map.Entry<Integer,Integer>> entries=new ArrayList<>(actual.entries());
        for(int i=0;i<count;i++)states[i]=(i%3==0?Integer.MIN_VALUE:0)|(i%2==0?4:0);
        var selected=actual.selectedByStates(states,Integer.MIN_VALUE,4,M3IndexPathMasks.Backend.JAVA);
        List<Map.Entry<Integer,Integer>> expected=new ArrayList<>();
        for(int i=0;i<count;i++)if((states[i]&Integer.MIN_VALUE)!=0 && (states[i]&4)==0)expected.add(entries.get(i));
        equal(expected,new ArrayList<>(selected.entries()),"indexed full 32-bit selection");
        selected.put(-100,123);
        check(!actual.containsKey(-100),"selection detached from source");
        boolean failed=false;try{actual.selectedByStates(new int[count+1],0,0,M3IndexPathMasks.Backend.JAVA);}catch(IllegalArgumentException ex){failed=true;}
        check(failed,"unaligned full-width state refusal");
        if (M3IndexPathMasks.nativeAvailable()) {
            var nativeSelected=actual.selectedByStates(
                    states,Integer.MIN_VALUE,4,M3IndexPathMasks.Backend.NATIVE_REQUIRED);
            equal(expected,new ArrayList<>(nativeSelected.entries()),
                    "native indexed full 32-bit selection");
        } else {
            failed=false;
            try{actual.selectedByStates(states,0,0,M3IndexPathMasks.Backend.NATIVE_REQUIRED);}
            catch(UnsatisfiedLinkError ex){failed=true;}
            check(failed,"native required fail closed when absent");
        }
        equal(Collections.emptyList(),new M3GuavaListMultimap<>().removeAll("absent"),"absent removal");
        System.out.println("PASS M3GuavaListMultimapContract checks="+checks+" steps=6400");
    }
    public static void main(String[] args){run();}
}
