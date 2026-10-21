/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Random;

/** Source-owned Java 21 test: a tree sibling shift publishes all keys or none. */
public final class M3TreeWindowShiftContract {
    private static long checks;
    private M3TreeWindowShiftContract() { }
    private static void yes(boolean value, String why) {
        checks++;
        if (!value) throw new AssertionError(why + " at check " + checks);
    }
    private static void refuses(Runnable action) {
        checks++;
        try { action.run(); }
        catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("shift was required to refuse");
    }
    private static M3TreeWindowPlan owner(int parent, int... children) {
        M3TreeWindowPlan p = new M3TreeWindowPlan();
        int n = children.length;
        int[] parents = new int[n], physical = new int[n];
        long[] handles = new long[n];
        Arrays.fill(parents, parent);
        Arrays.fill(physical, -1);
        for (int i = 0; i < n; i++) handles[i] = i + 1L;
        p.plan(n, parents, children, physical);
        p.commit(n, parents, children, physical, handles);
        return p;
    }
    private static void has(M3TreeWindowPlan p, int parent, int child, long handle) {
        yes(p.handleOf(parent, child) == handle, "native handle lookup changed");
    }
    private static long[] keys(M3TreeWindowPlan owner) {
        try {
            Field field = M3TreeWindowPlan.class.getDeclaredField("keys");
            field.setAccessible(true);
            return ((long[]) field.get(owner)).clone();
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }
    private static void boundaries() {
        var p = owner(-1, 3, 5, 9);
        p.shiftSiblings(-1, 5, 2);
        has(p, -1, 3, 1); has(p, -1, 7, 2); has(p, -1, 11, 3);
        has(p, -1, 5, 0); has(p, -1, 9, 0);
        yes(p.entryOf(2) == 1 && p.entryOf(3) == 2, "native handles retained");
        p.shiftSiblings(-1, 0, 0);
        has(p, -1, 7, 2);
        var other = owner(21, 1, 2, 3);
        other.shiftSiblings(-1, 0, 999);
        has(other, 21, 1, 1); has(other, 21, 2, 2); has(other, 21, 3, 3);

        var overflow = owner(-1, 5, Integer.MAX_VALUE);
        long[] before = keys(overflow);
        refuses(() -> overflow.shiftSiblings(-1, 5, 1));
        yes(Arrays.equals(before, keys(overflow)), "late overflow partially mutated key lane");
        has(overflow, -1, 5, 1); has(overflow, -1, Integer.MAX_VALUE, 2);
        refuses(() -> overflow.shiftSiblings(-1, 5, Integer.MAX_VALUE));
        yes(Arrays.equals(before, keys(overflow)), "early overflow mutated key lane");

        var collision = owner(-1, 0, 1, 5);
        before = keys(collision);
        refuses(() -> collision.shiftSiblings(-1, 1, -1));
        yes(Arrays.equals(before, keys(collision)), "resident sibling collision lost a handle");
        refuses(() -> collision.shiftSiblings(-1, 1, -2));
        yes(Arrays.equals(before, keys(collision)), "negative final index mutated key lane");
        var underflow = owner(-1, -1, 2);
        before = keys(underflow);
        refuses(() -> underflow.shiftSiblings(-1, -1, Integer.MIN_VALUE));
        yes(Arrays.equals(before, keys(underflow)), "signed overflow hid an invalid index");

        var left = owner(-1, 0, 2, 3, 7);
        left.shiftSiblings(-1, 2, -1);
        has(left,-1,0,1); has(left,-1,1,2); has(left,-1,2,3); has(left,-1,6,4);
    }
    private static void randomized() {
        Random r = new Random(0x4d33545348494654L);
        for (int trial=0;trial<35_000;trial++) {
            int parent = trial % 13 == 0 ? -1 : r.nextInt(50);
            int count = 1 + r.nextInt(72);
            int[] indices = new int[count];
            int current = r.nextInt(100);
            for (int i=0;i<count;i++) indices[i] = (current += 1 + r.nextInt(4));
            M3TreeWindowPlan owner = owner(parent, indices);
            int at = r.nextInt(count), first = indices[at];
            int delta = 1 + r.nextInt(12);
            owner.shiftSiblings(parent, first, delta);
            yes(owner.size()==count, "count changed");
            for (int i=0;i<count;i++) {
                has(owner,parent,indices[i]+(i>=at ? delta:0),i+1L);
                yes(owner.entryOf(i+1L)==i,"native handle position changed");
            }
            if (at>0 && trial%23==0) {
                long[] before=keys(owner);
                int invalid = indices[at-1] - (indices[at]+delta);
                refuses(() -> owner.shiftSiblings(parent,first+delta,invalid));
                yes(Arrays.equals(before,keys(owner)),"collision changed original keys");
            }
        }
    }
    public static void main(String[] args) {
        boundaries(); randomized();
        System.out.println("M3_TREE_WINDOW_SHIFT_PASS checks="+checks);
    }
}
