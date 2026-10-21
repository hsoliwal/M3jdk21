/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.Arrays;

/** Test that all three M3JDK21 JNI projections coexist in one original library. */
public final class M3ViewportUnionContract {
    private M3ViewportUnionContract() { }
    private static void check(boolean passes) {
        if (!passes) throw new AssertionError("M3 JNI viewport superset mismatch");
    }
    public static void main(String[] args) {
        String path = System.getProperty("m3.native.path");
        if (path == null) throw new IllegalArgumentException("m3.native.path required");
        System.load(path);
        int[] rows = {7, 8, 7, 9, 7};
        int[] masks = {1, 0, Integer.MIN_VALUE | 1, 1, 0};
        int[] all = {-9, -9, -9, -9, -9};
        check(M3MaskViewportNative.project(rows, masks, 0, 5, 1, 0, all, 1) == 3);
        check(Arrays.equals(all, new int[]{-9, 7, 7, 9, -9}));
        int[] page = {-9, -9, -9, -9};
        check(M3MaskViewportNative.projectPage(rows, masks, 0, 5, 1, 0, 1, 2, page, 1) == 2);
        check(Arrays.equals(page, new int[]{-9, 7, 9, -9}));
        int[] frameRows = new int[4], slots = new int[4], states = new int[4];
        check(M3MaskViewportNative.projectFramePage(rows, masks, 0, 5, 1, 0, 1, 2,
                frameRows, 1, slots, 1, states, 1) == 2);
        check(Arrays.equals(frameRows, new int[]{0, 7, 9, 0}));
        check(Arrays.equals(slots, new int[]{0, 2, 3, 0}));
        check(Arrays.equals(states, new int[]{0, Integer.MIN_VALUE | 1, 1, 0}));
        int[] alias = {7, 8, 7, 9, 7};
        check(M3MaskViewportNative.projectPage(alias, masks, 0, 5, 1, 0, 1, 2,
                alias, 2) == 2 && Arrays.equals(alias, new int[]{7, 8, 7, 9, 7}));
        int[] guard = page.clone();
        try {
            M3MaskViewportNative.projectPage(new int[]{7, -1}, new int[]{1, 0},
                    0, 2, 1, 0, 0, 0, page, 0);
            throw new AssertionError("missing failure on hidden invalid row");
        } catch (IllegalArgumentException expected) {
            check(Arrays.equals(page, guard));
        }
        System.out.println("M3_VIEWPORT_UNION_PASS");
    }
}
