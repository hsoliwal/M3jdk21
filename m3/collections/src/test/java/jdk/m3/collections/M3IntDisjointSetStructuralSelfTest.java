/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import java.lang.reflect.Field;

/** Candidate-only witness of the one-pass atom; not part of public DSU contract. */
public final class M3IntDisjointSetStructuralSelfTest {
    private M3IntDisjointSetStructuralSelfTest() { }

    public static long run() {
        IntDisjointSet dsu = new IntDisjointSet(8);
        final int[] parent;
        try {
            Field field = IntDisjointSet.class.getDeclaredField("parent");
            field.setAccessible(true);
            parent = (int[]) field.get(dsu);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError(error);
        }
        for (int i = 0; i < parent.length - 1; i++) parent[i] = i + 1;
        if (dsu.find(0) != 7) throw new AssertionError("lost original root");
        if (parent[0] != 2 || parent[2] != 4 || parent[4] != 6) {
            throw new AssertionError("expected one-pass path halving, not two-pass full compression");
        }
        if (dsu.find(0) != 7 || parent[0] != 4) {
            throw new AssertionError("repeated find must continue reducing path length");
        }
        dsu.reset();
        for (int i = 0; i < 8; i++) {
            if (dsu.find(i) != i || dsu.componentSize(i) != 1) {
                throw new AssertionError("reset must erase prior path structure");
            }
        }
        return 8 + 3 + 2 + 16;
    }

    public static void main(String[] args) {
        System.out.println("M3IntDisjointSetStructuralSelfTest PASS checks=" + run());
    }
}
