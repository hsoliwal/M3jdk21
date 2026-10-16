/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

/** JNI is an independent proof peer, not a production map/predicate backend. */
public final class M3FilteredMapsNativeSelfTest {
    private static Method nativeFirst;
    private static long calls, checks;
    private M3FilteredMapsNativeSelfTest() { }
    private static int first(int[] pairs,int count,int value) throws Exception {
        calls++;
        try { return (Integer)nativeFirst.invoke(null,pairs,count,value); }
        catch(InvocationTargetException failure) {
            if(failure.getCause() instanceof RuntimeException ex)throw ex;
            throw failure;
        }
    }
    private static void eq(Object a,Object b) {
        checks++;if(!Objects.equals(a,b))throw new AssertionError(a+" != "+b);
    }
    private static Integer key(int[] keys,int count,int index) {
        return index<0||index>=count?null:keys[index];
    }
    private static Object entry(Integer key) {
        return key==null?null:new AbstractMap.SimpleImmutableEntry<>(key,key+1);
    }
    public static void main(String[] args) throws Exception {
        System.load(args[0]);
        nativeFirst=Class.forName("RunSearchJni").getDeclaredMethod("first",int[].class,int.class,int.class);
        nativeFirst.setAccessible(true);
        for(int mask=0;mask<128;mask++)for(int bounds=0;bounds<3;bounds++)for(boolean descending:new boolean[]{false,true}) {
            TreeMap<Integer,Integer> owner=new TreeMap<>();
            int[] keys=new int[7],pairs=new int[18];int count=0;
            for(int i=0;i<7;i++) {
                int k=i*4;boolean admitted=(mask&(1<<i))!=0;
                owner.put(k,admitted?k+1:-k-1);
                boolean inRange=bounds==0||bounds==1&&k>=4&&k<=20||bounds==2&&k>4&&k<20;
                if(admitted&&inRange){keys[count]=k;pairs[2*count]=k;pairs[2*count+1]=k+1;count++;}
            }
            pairs[2*count]=-99;pairs[2*count+1]=Integer.MIN_VALUE;
            int[] before=pairs.clone();
            NavigableMap<Integer,Integer> range=bounds==0?owner:owner.subMap(4,bounds==1,20,bounds==1);
            if(descending)range=range.descendingMap();
            var view=M3FilteredMaps.filtered(range,(k,v)->v>0);
            for(int q=-1;q<=25;q++) {
                int ceiling=first(pairs,count,q),higher=first(pairs,count,q+1);
                Integer low=key(keys,count,ceiling-1),floor=key(keys,count,higher-1);
                Integer ceil=key(keys,count,ceiling),high=key(keys,count,higher);
                eq(view.lowerKey(q),descending?high:low);eq(view.floorKey(q),descending?ceil:floor);
                eq(view.ceilingKey(q),descending?floor:ceil);eq(view.higherKey(q),descending?low:high);
                eq(view.lowerEntry(q),entry(descending?high:low));eq(view.floorEntry(q),entry(descending?ceil:floor));
                eq(view.ceilingEntry(q),entry(descending?floor:ceil));eq(view.higherEntry(q),entry(descending?low:high));
            }
            checks++;if(!Arrays.equals(before,pairs))throw new AssertionError("JNI changed input");
        }
        int[][] bad={null,new int[1],new int[]{1,1},new int[]{0,65537},new int[]{3,9,1,2},new int[0]};
        int[] sizes={0,1,1,1,2,-1};int refusals=0;
        for(int i=0;i<bad.length;i++)try{first(bad[i],sizes[i],0);throw new AssertionError("missing refusal");}
            catch(IllegalArgumentException expected){refusals++;}
        System.out.println("M3_FILTERED_MAPS_JNI_PASS checks="+checks+" calls="+calls+" refusals="+refusals);
    }
}
