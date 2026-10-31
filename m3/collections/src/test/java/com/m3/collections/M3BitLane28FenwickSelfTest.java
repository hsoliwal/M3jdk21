// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

public final class M3BitLane28FenwickSelfTest {
    private M3BitLane28FenwickSelfTest() {}
    private static int checks;
    private static void eq(int expected, int actual, String where) {
        checks++;
        if(expected!=actual) throw new AssertionError(where+" expected="+expected+" actual="+actual);
    }
    private static void check(boolean condition, String where) {
        checks++;if(!condition) throw new AssertionError(where);
    }
    private static void threw(Runnable run, String where) {
        checks++;try{run.run();} catch(IndexOutOfBoundsException expected){return;}
        throw new AssertionError(where+" expected IndexOutOfBoundsException");
    }
    public static void main(String[] args) {
        M3BitLane28 lane = new M3BitLane28();
        TreeSet<Integer> oracle = new TreeSet<>();
        eq(0,lane.rank(0),"initial rank0");
        eq(0,lane.rank(M3Address28.MAX_SLOTS),"initial rank end");
        eq(-1,lane.select(0),"initial select");
        eq(0,lane.allocatedBlockCount(),"initial blocks");
        eq(0,(int)lane.indexPayloadBytes(),"index base payload");
        threw(()->lane.rank(-1),"negative rank");
        threw(()->lane.rank(M3Address28.MAX_SLOTS+1),"oversize rank");
        threw(()->lane.cardinality(5,4),"reversed range");
        int[] edges={0,1,63,64,65,4094,4095,4096,4097,(1<<20)-1,(1<<20),
            (1<<20)+4095,(1<<20)+4096, (1<<27),M3Address28.MAX_SLOT};
        for (int slot:edges){lane.set(slot);oracle.add(slot);}
        verify(lane,oracle,edges);
        Random random = new Random(0xC1A0E2026L);
        for (int i=0; i<15000; i++) {
            int slot = switch(random.nextInt(4)) {
                case 0 -> random.nextInt(1<<16);
                case 1 -> (random.nextInt(256)<<20) | random.nextInt(4096);
                case 2 -> random.nextInt(M3Address28.MAX_SLOTS);
                default -> M3Address28.MAX_SLOT-random.nextInt(1<<17);
            };
            switch(random.nextInt(4)) {
                case 0 -> {lane.set(slot); oracle.add(slot);}
                case 1 -> {lane.clear(slot);oracle.remove(slot);}
                case 2 -> {lane.flip(slot);if(!oracle.remove(slot))oracle.add(slot);}
                default -> {boolean v =random.nextBoolean();lane.set(slot,v);if(v)oracle.add(slot);else oracle.remove(slot);}
            }
            if (i%41==0) verify(lane,oracle,edges);
        }
        List<Integer> snapshot=new ArrayList<>(oracle);
        for(int slot:snapshot){lane.clear(slot);oracle.remove(slot);}
        eq(0,lane.cardinality(),"all cleared");
        eq(-1,lane.select(0),"all cleared select");
        eq(0,lane.rank(M3Address28.MAX_SLOTS),"all cleared rank");
        check(lane.indexPayloadBytes()>0,"index retained");
        System.out.println("PASS M3BitLane28FenwickSelfTest checks="+checks+" mutations=15000");
    }
    private static void verify(M3BitLane28 lane, TreeSet<Integer> oracle, int[] edges) {
        eq(oracle.size(),lane.cardinality(),"size");
        eq(oracle.size(),lane.rank(M3Address28.MAX_SLOTS),"end rank");
        eq(-1,lane.select(-1),"negative select");
        eq(-1,lane.select(oracle.size()),"upper select");
        if(!oracle.isEmpty()) {
            eq(oracle.first(),lane.select(0),"first select");
            eq(oracle.last(),lane.select(oracle.size()-1),"last select");
        }
        for(int edge:edges){
            eq(oracle.headSet(edge).size(),lane.rank(edge),"rank "+edge);
            eq(oracle.contains(edge)?1:0,lane.get(edge)?1:0,"get "+edge);
        }
        for(int sample=0;sample<6;sample++) {
            int a=sample * (M3Address28.MAX_SLOTS/6);
            int b = Math.min(M3Address28.MAX_SLOTS, a+9876543);
            eq(oracle.subSet(a,b).size(), lane.cardinality(a,b),"range");
            eq(oracle.ceiling(a)==null?-1:oracle.ceiling(a),lane.nextSetBit(a),"next");
            eq(oracle.floor(b-1)==null?-1:oracle.floor(b-1),lane.previousSetBit(b-1),"previous");
        }
        if(oracle.size()>0){
            int idx = oracle.size()/2;
            int selected=new ArrayList<>(oracle).get(idx);
            eq(selected,lane.select(idx),"middle select");
            eq(idx,lane.rank(selected),"rank/select inverse");
        }
        eq((int)((long)lane.allocatedBlockCount()*512L),(int)lane.payloadBytes(),"historical payload");
        eq((lane.allocatedRegionCount()+1)*1028,(int)lane.indexPayloadBytes(),"fenwick payload");
    }
}
