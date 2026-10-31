/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0. */
package com.m3.collections.interop;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.List;
import java.util.Objects;
import org.springframework.util.LinkedCaseInsensitiveMap;

/** Differential against real Spring Framework 6.2.0, not a mock or donor copy. */
public final class M3SpringCaseInsensitiveContract {
    private static long checks;
    private M3SpringCaseInsensitiveContract() { }
    private static void eq(Object expected, Object actual) {
        checks++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("check="+checks+" expected="+expected+" actual="+actual);
        }
    }
    private static void check(boolean value) {
        checks++;if(!value)throw new AssertionError("check="+checks);
    }
    private static String variant(Random r) {
        String key = switch(r.nextInt(7)) {
            case 0 -> "name"; case 1 -> "Content-Type"; case 2 -> "X-Foo";
            case 3 -> "ABC"; case 4 -> "Istanbul"; case 5 -> "KEY";
            default -> "İstanbul";
        };
        if(r.nextBoolean())key=key.toLowerCase(Locale.ROOT);
        else if(r.nextBoolean())key=key.toUpperCase(Locale.ROOT);
        return key;
    }
    private static void compare(Map<String,Integer> want, Map<String,Integer> got, String label) {
        eq(want.size(),got.size());
        eq(want,got);
        eq(got,want);
        eq(want.hashCode(),got.hashCode());
        eq(List.copyOf(want.keySet()),List.copyOf(got.keySet()));
        eq(want.entrySet(),got.entrySet());
        eq(want.values().size(),got.values().size());
        eq(want.toString(),got.toString());
        check(want.keySet().containsAll(got.keySet()));
        for(String key: new String[]{"NAME","content-type","x-Foo","Abc","iStAnBuL","KEY","İSTANBUL","missing"}) {
            eq(want.get(key),got.get(key));
            eq(want.containsKey(key),got.containsKey(key));
            eq(want.getOrDefault(key,99),got.getOrDefault(key,99));
            if (want.keySet().contains(key) != got.keySet().contains(key))
                throw new AssertionError(label+" key="+key+" want.contains="+want.keySet().contains(key)
                    +" got.contains="+got.keySet().contains(key)+" want="+want+" got="+got);
            checks++;
        }
    }
    private static void randomized(Locale locale, long seed) {
        var want=new LinkedCaseInsensitiveMap<Integer>(locale);
        var got=new M3SpringCaseInsensitiveMap<Integer>(locale);
        eq(want.getLocale(),got.getLocale());
        var r=new Random(seed);
        for(int op=0;op<7_500;op++) {
            String k=variant(r);
            Integer value=r.nextInt(6)==0?null:r.nextInt(31);
            switch(r.nextInt(11)) {
                case 0,1 -> eq(want.put(k,value),got.put(k,value));
                case 2 -> eq(want.remove(k),got.remove(k));
                case 3 -> eq(want.putIfAbsent(k,value),got.putIfAbsent(k,value));
                case 4 -> eq(want.computeIfAbsent(k,key-> value),got.computeIfAbsent(k,key->value));
                case 5 -> eq(want.get(k),got.get(k));
                case 6 -> eq(want.computeIfPresent(k,(key,old)->value),got.computeIfPresent(k,(key,old)->value));
                case 7 -> { if(value!=null)eq(want.merge(k,value,(a,b)->a+b),got.merge(k,value,(a,b)->a+b)); }
                case 8 -> eq(want.keySet().remove(k),got.keySet().remove(k));
                case 9 -> { if(!want.isEmpty()) {
                    String first=want.keySet().iterator().next();
                    var w=want.entrySet().iterator();var g=got.entrySet().iterator();
                    eq(w.next().getKey(),g.next().getKey());w.remove();g.remove();
                    check(!got.containsKey(first));
                } }
                default -> eq(want.containsValue(value),got.containsValue(value));
            }
            compare(want,got,"op="+op);
        }
        want.clear();got.clear();
        compare(want,got,"cleared");
        // Null values preserve an existing representative for putIfAbsent.
        want.put("Header",null);got.put("Header",null);
        eq(want.putIfAbsent("hEaDeR",4),got.putIfAbsent("hEaDeR",4));
        compare(want,got,"null putIfAbsent");
        want.put("Header",null);got.put("Header",null);
        eq(want.computeIfAbsent("hEaDeR",key->key.length()),got.computeIfAbsent("hEaDeR",key->key.length()));
        compare(want,got,"null computeIfAbsent");
        // Canonical spelling changes on explicit put, not on putIfAbsent.
        want.put("hEaDeR",8);got.put("hEaDeR",8);
        compare(want,got,"case-replacing put");
        // Iterator setValue and removals retain normalized index synchronization.
        want.entrySet().iterator().next().setValue(11);
        got.entrySet().iterator().next().setValue(11);
        compare(want,got,"entry.setValue");
        eq(want.remove("HEADER"),got.remove("HEADER"));
        compare(want,got,"alias removal");
    }
    public static void main(String[] args) {
        if(args.length!=0)throw new IllegalArgumentException("no args");
        randomized(Locale.ROOT,0xAE4412);
        randomized(Locale.forLanguageTag("tr-TR"),0xAE4414);
        randomized(Locale.US,0xAE4416);
        System.out.println("PASS M3_SPRING_CASE_INSENSITIVE oracle_checks="+checks+" randomized_operations=22500");
    }
}
