/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Strict-Java21, source-owned oracle for model-change -> resident-native row updates. */
public final class M3WindowPlanChangeContract {
    private static long assertions;
    private M3WindowPlanChangeContract() { }

    private static void yes(boolean result, String note) {
        assertions++;
        if (!result) throw new AssertionError(note + " at check " + assertions);
    }

    private static void refused(Class<? extends Throwable> kind, Runnable operation) {
        assertions++;
        try { operation.run(); }
        catch (Throwable error) {
            if (kind.isInstance(error)) return;
            throw new AssertionError("wrong refusal type for " + kind.getSimpleName(), error);
        }
        throw new AssertionError("missing expected " + kind.getSimpleName());
    }

    private static M3WindowPlan configured(int first, int count) {
        M3WindowPlan plan = new M3WindowPlan();
        plan.plan(first,count);
        plan.commit();
        return plan;
    }

    private static void boundaries() {
        M3WindowPlan p = configured(10, 5);
        yes(p.changedCount(8, 3) == 1 && p.changedEntry(8, 3) == 0, "prefix intersection");
        yes(p.changedCount(12, 10) == 3 && p.changedEntry(12, 10) == 2, "suffix intersection");
        yes(p.changedCount(0, 10) == 0 && p.changedEntry(0, 10) == -1, "outside prefix");
        yes(p.changedCount(15, 3) == 0 && p.changedEntry(15, 3) == -1, "outside suffix");
        yes(p.changedCount(10, 0) == 0 && p.changedEntry(10, 0) == -1, "empty change");
        yes(p.changedCount(0, Integer.MAX_VALUE) == 5 && p.changedEntry(0,Integer.MAX_VALUE) == 0,
                "full logical range");
        refused(IllegalArgumentException.class, () -> p.changedCount(-1, 0));
        refused(IllegalArgumentException.class, () -> p.changedCount(5, -1));
        refused(IllegalArgumentException.class, () -> p.changedEntry(Integer.MAX_VALUE, 1));
        int[] dirty = {7, 10, 10, 13, 14, 15, 100};
        int[] result = {-99, -99, -99, -99, -99, -99, -99};
        yes(p.projectChangedRows(dirty, 0, dirty.length, result, 2)==4, "selected dirty count");
        yes(Arrays.equals(result, new int[]{-99,-99,0,0,3,4,-99}), "duplicates, bounded target, order");
        int[] originally = result.clone();
        refused(IndexOutOfBoundsException.class, () -> p.projectChangedRows(dirty, 0, dirty.length, result, 4));
        yes(Arrays.equals(result, originally), "capacity refused without writes");
        refused(IllegalArgumentException.class, () -> p.projectChangedRows(new int[]{10,-1},0,2,result,0));
        yes(Arrays.equals(result, originally), "negative element refused without writes");
        refused(IndexOutOfBoundsException.class, () -> p.projectChangedRows(dirty,Integer.MAX_VALUE,1,result,0));
        refused(IndexOutOfBoundsException.class, () -> p.projectChangedRows(dirty,0,dirty.length,result,-1));
        refused(NullPointerException.class, () -> p.projectChangedRows(null,0,0,result,0));
        refused(NullPointerException.class, () -> p.projectChangedRows(dirty,0,0,null,0));
        int[] alias = {8, 10, 12, 14, 19, 10};
        yes(p.projectChangedRows(alias,0,alias.length,alias,2)==4, "overlapping source/output");
        yes(Arrays.equals(alias,new int[]{8,10,0,2,4,0}), "alias copy preserves original input scan");
        int[] none = {0,1,2};
        yes(p.projectChangedRows(none,0,none.length,none,1)==0
                && Arrays.equals(none,new int[]{0,1,2}), "offscreen alias no work");
        yes(p.first()==10 && p.size()==5, "deltas never mutate current geometry");
        p.plan(Integer.MAX_VALUE - 2, 2); p.commit();
        yes(p.changedCount(Integer.MAX_VALUE-1, 1)==1, "safe max int intersection");
        yes(p.changedEntry(Integer.MAX_VALUE-1,1)==1, "safe max int offset");
        p.clear();
        yes(p.changedCount(0,500)==0 && p.changedEntry(0,500)==-1, "cleared empty window");
    }

    private static void randomized() {
        Random r = new Random(0x4d3357494e444f57L);
        for (int trial=0;trial<40_000;trial++) {
            int first = r.nextInt(2500), count=r.nextInt(140);
            M3WindowPlan p=configured(first,count);
            int from=r.nextInt(3000), length=r.nextInt(250);
            int expectedCount=0, expectedEntry=-1;
            for (int i=from;i<from+length;i++) {
                if (i>=first && i<first+count) {
                    if (expectedEntry==-1) expectedEntry=i-first;
                    expectedCount++;
                }
            }
            yes(p.changedCount(from,length)==expectedCount, "range count "+trial);
            yes(p.changedEntry(from,length)==expectedEntry, "range start "+trial);
            int inputSize=r.nextInt(128);
            int[] dirty=new int[inputSize];
            for(int i=0;i<inputSize;i++) dirty[i]=r.nextInt(3000);
            int start=r.nextInt(inputSize+1), used=r.nextInt(inputSize-start+1);
            List<Integer> expected=new ArrayList<>();
            for(int i=start;i<start+used;i++) {
                if(dirty[i]>=first && dirty[i]<first+count) expected.add(dirty[i]-first);
            }
            int prefix=r.nextInt(4), trailing=r.nextInt(4);
            int[] output=new int[prefix+expected.size()+trailing];
            Arrays.fill(output,-75);
            yes(p.projectChangedRows(dirty,start,used,output,prefix)==expected.size(), "project count "+trial);
            for(int i=0;i<expected.size();i++) yes(output[prefix+i]==expected.get(i), "exact row order "+trial);
            for(int i=0;i<prefix;i++) yes(output[i]==-75,"prefix not written "+trial);
            for(int i=prefix+expected.size();i<output.length;i++) yes(output[i]==-75,"trailer not written "+trial);
            yes(p.first()==first && p.size()==count, "source untouched "+trial);
            if(expected.size()<=dirty.length) {
                int[] copy=dirty.clone();
                int offset=dirty.length-expected.size();
                yes(p.projectChangedRows(copy,start,used,copy,offset)==expected.size(), "alias size "+trial);
                for(int i=0;i<expected.size();i++) yes(copy[offset+i]==expected.get(i),"alias projection "+trial);
            }
        }
    }

    public static void main(String[] args) {
        boundaries(); randomized();
        System.out.println("M3_WINDOW_DELTA_PROJECTION_PASS checks="+assertions);
    }
}
