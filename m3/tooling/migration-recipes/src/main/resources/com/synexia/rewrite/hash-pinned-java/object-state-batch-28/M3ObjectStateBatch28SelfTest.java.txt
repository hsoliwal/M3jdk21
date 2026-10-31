/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Random;

/** Deterministic generation-safe array/offset/mask differential oracle. */
public final class M3ObjectStateBatch28SelfTest {
    private static long assertions;
    private M3ObjectStateBatch28SelfTest() { }
    private static void check(boolean expected) {
        assertions++;
        if (!expected) throw new AssertionError("state-batch check " + assertions);
    }
    private static void equal(int left, int right) { check(left == right); }
    private static void refuse(Class<? extends Throwable> type, Runnable operation) {
        assertions++;
        try { operation.run(); } catch (Throwable error) {
            if (type.isInstance(error)) return;
            throw new AssertionError("wrong refusal: " + error, error);
        }
        throw new AssertionError("missing refusal " + type);
    }
    private static void boundaries() {
        var state = new M3ObjectStateSlots28();
        long a = state.allocate(0x80), b = state.allocate(0x01);
        long[] order = {17, a, b, a, 19};
        equal(M3ObjectStateBatch28.countMatching(state, order, 1, 3, 0x80, 0), 2);
        equal(M3ObjectStateBatch28.firstMatchingIndex(state, order, 1, 3, 0x01, 0), 2);
        equal(M3ObjectStateBatch28.updateMatching(state, order, 1, 3,
                0x80, 0, 0x01, 0x80), 1);
        equal(state.state(a), 1);
        equal(state.state(b), 1);
        equal(M3ObjectStateBatch28.update(state, order, 1, 3, 0x02, 0), 2);
        equal(state.state(a), 3);
        equal(state.state(b), 3);
        check(Arrays.equals(order, new long[]{17,a,b,a,19}));
        equal(M3ObjectStateBatch28.updateMatching(state, order, 1, 3, 0x01, 0x01, 4, 0), 0);
        refuse(IllegalArgumentException.class, () -> M3ObjectStateBatch28.update(state, order, 1, 3, 0x80, 0x80));
        refuse(IllegalArgumentException.class, () -> M3ObjectStateBatch28.update(state, order, 1, 3, 256, 0));
        refuse(IllegalArgumentException.class, () -> M3ObjectStateBatch28.countMatching(state, order, 1, 3, -1, 0));
        refuse(IndexOutOfBoundsException.class, () -> M3ObjectStateBatch28.update(state, order, 1, 5, 4, 0));
        refuse(NullPointerException.class, () -> M3ObjectStateBatch28.update(state, null, 0, 0, 4, 0));
        refuse(NullPointerException.class, () -> M3ObjectStateBatch28.update(null, order, 0, 0, 4, 0));
        long invalid = state.allocate(0x80);
        long[] mixed = {a,b,invalid};
        state.release(invalid);
        long oldStateA = state.state(a), oldStateB = state.state(b);
        refuse(IllegalStateException.class, () -> M3ObjectStateBatch28.update(state,mixed,0,3,4,0));
        refuse(IllegalStateException.class, () -> M3ObjectStateBatch28.updateMatching(state,mixed,0,3,1,0,4,0));
        equal(state.state(a), (int)oldStateA);equal(state.state(b), (int)oldStateB);
        equal(M3ObjectStateBatch28.firstMatchingIndex(state,mixed,0,3,1,0),0);
        state.release(a);state.release(b);
    }
    private static void multiPageAndReuse() {
        var state = new M3ObjectStateSlots28();
        long[] handles = new long[4097];
        for (int i = 0; i < handles.length; i++) handles[i] = state.allocate(i % 2 == 0 ? 0x80 : 0);
        equal(state.allocatedStateBlocks(), 2);
        equal(M3ObjectStateBatch28.countMatching(state, handles, 0, 4097, 0x80, 0), 2049);
        equal(M3ObjectStateBatch28.update(state,handles,4095,2,1,0),2);
        equal(state.state(handles[4095]),1);equal(state.state(handles[4096]),0x81);
        long stale = handles[4096];state.release(stale);long fresh = state.allocate(7);
        equal(M3SlotAllocator28.slot(stale),M3SlotAllocator28.slot(fresh));
        refuse(IllegalStateException.class,()->M3ObjectStateBatch28.update(state,handles,4095,2,4,0));
        equal(state.state(handles[4095]),1);
        for(int i=0;i<4096;i++)state.release(handles[i]);state.release(fresh);
    }
    private static void randomized() {
        long operations=0;
        for(int seed=0;seed<96;seed++) {
            var rng=new Random(1733L+seed);var state=new M3ObjectStateSlots28();
            long[] handles=new long[32];int[] expected=new int[32];
            for(int i=0;i<handles.length;i++){
                expected[i]=rng.nextInt(256);handles[i]=state.allocate(expected[i]);
            }
            long[] order=new long[48];int[] ordinal=new int[48];
            for(int i=0;i<order.length;i++){int selected=rng.nextInt(handles.length);order[i]=handles[selected];ordinal[i]=selected;}
            for(int op=0;op<512;op++){
                int from=rng.nextInt(order.length+1), len=rng.nextInt(order.length-from+1);
                int a=rng.nextInt(256),b=rng.nextInt(256), set=rng.nextInt(256);
                int clear=rng.nextInt(256)&~set;
                int first=-1,count=0;
                for(int i=from;i<from+len;i++){
                    int flags=expected[ordinal[i]];
                    if((flags&a)==a&&(flags&b)==0){if(first<0)first=i;count++;}
                }
                equal(M3ObjectStateBatch28.firstMatchingIndex(state,order,from,len,a,b),first);
                equal(M3ObjectStateBatch28.countMatching(state,order,from,len,a,b),count);
                int changed=0;
                for(int i=from;i<from+len;i++){
                    int slot=ordinal[i],old=expected[slot];
                    if((old&a)==a&&(old&b)==0){
                        int next=(old|set)&~clear&0xff;
                        if(next!=old)changed++;
                        expected[slot]=next;
                    }
                }
                equal(M3ObjectStateBatch28.updateMatching(state,order,from,len,a,b,set,clear),changed);
                for(int i=0;i<handles.length;i++)equal(state.state(handles[i]),expected[i]);
                operations++;
            }
            for(long h:handles)state.release(h);
        }
        System.out.println("PASS M3_OBJECT_STATE_BATCH checks="+assertions+" randomized_operations="+operations);
    }
    public static void main(String[] args) {
        if(args.length!=0)throw new IllegalArgumentException("no args");
        boundaries();multiPageAndReuse();randomized();
    }
}
