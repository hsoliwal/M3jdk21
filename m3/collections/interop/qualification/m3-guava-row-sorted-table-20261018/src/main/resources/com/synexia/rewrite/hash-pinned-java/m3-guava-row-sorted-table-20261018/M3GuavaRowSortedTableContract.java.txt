/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections.interop;

import com.google.common.collect.RowSortedTable;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.TreeMap;

/** Deterministic real Guava 33.4.8-jre TreeBasedTable contract oracle. */
public final class M3GuavaRowSortedTableContract {
    private static long checks;
    private M3GuavaRowSortedTableContract() { }
    private static void eq(Object want,Object got){
        checks++;
        if(!Objects.equals(want,got))throw new AssertionError("check="+checks+" want="+want+" got="+got);
    }
    private static void reject(Class<? extends Throwable> kind,Runnable action){
        checks++;
        try{action.run();}catch(Throwable e){if(kind.isInstance(e))return;throw new AssertionError(e);}
        throw new AssertionError("Expected "+kind.getSimpleName());
    }
    private static void compare(RowSortedTable<Integer,Integer,Integer> a,
            RowSortedTable<Integer,Integer,Integer> b){
        eq(a.size(),b.size());eq(a.isEmpty(),b.isEmpty());
        eq(a,b);eq(a.hashCode(),b.hashCode());
        eq(a.cellSet(),b.cellSet());
        eq(a.rowMap(),b.rowMap());
        eq(new ArrayList<>(a.rowKeySet()),new ArrayList<>(b.rowKeySet()));
        eq(a.columnKeySet(),b.columnKeySet());
        for(int r=0;r<13;r++){
            eq(a.row(r),b.row(r));
            eq(new ArrayList<>(a.row(r).keySet()),new ArrayList<>(b.row(r).keySet()));
        }
        for(int c=0;c<13;c++)eq(a.column(c),b.column(c));
    }
    public static void main(String[] arguments){
        if(arguments.length!=0)throw new IllegalArgumentException("arguments");
        var ref=TreeBasedTable.<Integer,Integer,Integer>create();
        var m3=new M3GuavaRowSortedTable<Integer,Integer,Integer>(
                Comparator.naturalOrder(),Comparator.naturalOrder());
        Random rand=new Random(9311371L);
        for(int i=0;i<3200;i++){
            Integer r=rand.nextInt(13), c=rand.nextInt(13), v=rand.nextInt(500);
            switch(rand.nextInt(9)) {
                case 0,1 -> eq(ref.put(r,c,v),m3.put(r,c,v));
                case 2 -> eq(ref.remove(r,c),m3.remove(r,c));
                case 3 -> {eq(ref.row(r).put(c,v),m3.row(r).put(c,v));}
                case 4 -> {eq(ref.column(c).remove(r),m3.column(c).remove(r));}
                case 5 -> {eq(ref.rowMap().remove(r),m3.rowMap().remove(r));}
                case 6 -> {if(!ref.rowKeySet().isEmpty()) {
                    int k=ref.rowKeySet().first();eq(ref.rowKeySet().remove(k),m3.rowKeySet().remove(k));
                }}
                case 7 -> {if(!ref.rowKeySet().isEmpty()) {
                    int k=ref.rowKeySet().first();
                    eq(ref.rowMap().tailMap(k).remove(k),m3.rowMap().tailMap(k).remove(k));
                }}
                default -> {eq(ref.contains(r,c),m3.contains(r,c));}
            }
            compare(ref,m3);
        }
        reject(UnsupportedOperationException.class,
                ()->m3.rowMap().put(5,new TreeMap<>()));
        reject(UnsupportedOperationException.class,
                ()->m3.rowMap().subMap(1,9).put(2,new TreeMap<>()));
        var singleColumn=new M3GuavaRowSortedTable<String,String,Integer>(Comparator.naturalOrder());
        singleColumn.put("b","z",2);singleColumn.put("a","x",1);
        eq(java.util.List.of("a","b"),new ArrayList<>(singleColumn.rowKeySet()));
        eq(2,singleColumn.size());
        System.out.println("PASS M3_GUAVA_ROW_SORTED_TABLE realGuava assertions="+checks+" operations=3200");
    }
}
