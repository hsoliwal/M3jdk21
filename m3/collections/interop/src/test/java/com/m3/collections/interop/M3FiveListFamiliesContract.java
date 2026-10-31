/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections.interop;

import com.google.common.collect.ListMultimap;
import com.m3.collections.M3IndexPathMasks;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.collections4.ListValuedMap;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.springframework.util.MultiValueMap;

/** Cross-provider family behavior with distinct key-count/pair-count contracts. */
public final class M3FiveListFamiliesContract {
    private static long checks;
    private M3FiveListFamiliesContract() { }
    private static void equal(Object want, Object actual, String label) {
        checks++;
        if (!Objects.equals(want, actual))
            throw new AssertionError(label + ": want=" + want + " actual=" + actual);
    }
    private static void yes(boolean ok, String label) {
        checks++;
        if (!ok) throw new AssertionError(label);
    }
    private static <K,V> void equivalentValues(K key,List<V> values,Map<K,List<V>> jdk,
            ListMultimap<K,V> guava, MutableListMultimap<K,V> eclipse,
            MultiValueMap<K,V> spring, ListValuedMap<K,V> apache) {
        equal(values,jdk.get(key),"jdk values");
        equal(values,guava.get(key),"guava values");
        equal(values,eclipse.get(key),"eclipse values");
        equal(values,spring.get(key),"spring values");
        equal(values,apache.get(key),"apache values");
    }
    public static void run() {
        checks=0;
        // Selected state ordinals are independent of the provider's internal
        // bucket iteration; all projections are built in one indexed encounter order.
        M3GuavaListMultimap<String,Integer> source=new M3GuavaListMultimap<>();
        for(int i=0;i<150;i++)source.put(i%17==0?null:"k"+(i%7),i%5==0?null:i%11);
        int[] flags=new int[source.size()];
        List<Map.Entry<String,Integer>> original=new ArrayList<>(source.entries());
        for(int i=0;i<flags.length;i++) flags[i]=(i%2==0?Integer.MIN_VALUE:0)|(i%3==0?4:0);
        var selected=M3FiveListFamilies.snapshot(source,flags,Integer.MIN_VALUE,4,M3IndexPathMasks.Backend.JAVA);
        List<Map.Entry<String,Integer>> kept=new ArrayList<>();
        for(int i=0;i<flags.length;i++) if((flags[i]&Integer.MIN_VALUE)!=0 && (flags[i]&4)==0)
            kept.add(original.get(i));
        equal(kept.size(),selected.occurrences(),"snapshot total selected pairs");
        ListMultimap<String,Integer> guava=selected.guava();
        Map<String,List<Integer>> jdk=selected.jdk();
        MutableListMultimap<String,Integer> eclipse=selected.eclipse();
        MultiValueMap<String,Integer> spring=selected.spring();
        ListValuedMap<String,Integer> apache=selected.apache();
        equal(kept.size(),guava.size(),"Guava counts pairs");
        equal(kept.size(),apache.size(),"Apache counts pairs");
        equal(kept.size(),eclipse.size(),"Eclipse counts pairs");
        equal(jdk.size(),spring.size(),"Spring/JDK count distinct keys");
        equal(jdk.size(),selected.distinctKeys(),"selected distinct key count");
        for(String key: jdk.keySet())equivalentValues(key,jdk.get(key),jdk,guava,eclipse,spring,apache);
        source.put("extra",9);
        yes(!jdk.containsKey("extra"),"selected detached from source");
        guava.put("detached",777);
        yes(!spring.containsKey("detached"),"Spring owner independent of Guava");
        yes(!jdk.containsKey("detached"),"JDK owner independent of Guava");
        yes(!source.containsKey("detached"),"original owner independent");
        List<Integer> jdkList= jdk.get("k1");
        if(jdkList!=null){
            int count=guava.get("k1").size();
            jdkList.add(808);
            equal(count,guava.get("k1").size(),"JDK changes independent of Guava");
        }
        equal(Collections.emptyList(),M3FiveListFamilies.<String,Integer>guava().get("absent"),"Guava absent-key list");
        equal(null,M3FiveListFamilies.<String,Integer>spring().get("absent"),"Spring absent-key null");
        equal(List.of(),M3FiveListFamilies.<String,Integer>eclipse().get("absent"),"Eclipse absent-key empty");
        equal(List.of(),M3FiveListFamilies.<String,Integer>apache().get("absent"),"Apache absent-key empty");
        // These factories expose real, unrelated interfaces; no generic type
        // erasure or universal Map alias is used to implement their behavior.
        yes(M3FiveListFamilies.<String,Integer>guava() instanceof M3GuavaListMultimap<?,?>,
                "direct Guava concrete M3 owner");
        System.out.println("PASS M3FiveListFamiliesContract checks="+checks);
    }
    public static void main(String[] args) {run();}
}
