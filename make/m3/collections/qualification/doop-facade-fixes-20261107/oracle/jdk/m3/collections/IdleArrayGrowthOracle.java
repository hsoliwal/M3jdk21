// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.util.Arrays;

/** Independent reduced-state Java 21 proof of two-lane idle allocation; NOT production store. */
//FLLDCIM: RELOCATE - donor blob 4d7d82a826c2 at ff83fd2a685b; recipe make/m3/collections/qualification/doop-facade-fixes-20261107/apply.py v1
public final class IdleArrayGrowthOracle {
    private IdleArrayGrowthOracle() { }

    private interface Copier {
        Object[] objects(Object[] existing, int desired);
        long[] longs(long[] existing, int desired);
    }

    private static final class Idle {
        Object[] objects = new Object[0];
        long[] ids = new long[0];
        int count;
        final int maxIdle = 4;
        void ensure(int required, Copier copies) {
            if (required <= objects.length) return;
            int capacity = Math.min(maxIdle,
                    Math.max(required, Math.max(1, objects.length * 2)));
            Object[] nextObjects = java.util.Objects.requireNonNull(
                    copies.objects(objects, capacity), "idle facade copy");
            long[] nextIds = java.util.Objects.requireNonNull(
                    copies.longs(ids, capacity), "idle ID copy");
            if (nextObjects.length != capacity || nextIds.length != capacity) {
                throw new IllegalStateException("incomplete M3 idle facade backing growth");
            }
            objects = nextObjects;
            ids = nextIds;
        }
        void put(long id, Object value) {
            ensure(count + 1, NORMAL);
            objects[count] = value;
            ids[count++] = id;
        }
        Object remove(long id) {
            for (int i = 0; i < count; i++) {
                if (ids[i] == id) {
                    Object value = objects[i];
                    int last = --count;
                    objects[i] = objects[last];
                    ids[i] = ids[last];
                    objects[last] = null;
                    ids[last] = 0;
                    return value;
                }
            }
            return null;
        }
    }
    private static final Copier NORMAL = new Copier() {
        public Object[] objects(Object[] array, int size) { return Arrays.copyOf(array, size); }
        public long[] longs(long[] array, int size) { return Arrays.copyOf(array, size); }
    };
    private static void must(boolean b, String why) {
        if (!b) throw new AssertionError(why);
    }
    public static void main(String[] args) {
        for (int i=0;i<10_000;i++) {
            Idle idle = new Idle();
            Object token = new Object();
            Copier secondFails = new Copier() {
                public Object[] objects(Object[] array, int n) { return Arrays.copyOf(array, n); }
                public long[] longs(long[] array, int n) { throw new IllegalStateException("synthetic second copy"); }
            };
            try { idle.ensure(1, secondFails); throw new AssertionError("missing error"); }
            catch (IllegalStateException expected) {
                must(expected.getMessage().contains("synthetic"), "wrong exception");
            }
            must(idle.objects.length == 0 && idle.ids.length == 0, "partial publication");
            idle.put(11L, token);
            must(idle.count == 1, "first insert lost");
            try { idle.ensure(2, secondFails); throw new AssertionError("missing second error"); }
            catch (IllegalStateException expected) { /* expected */ }
            must(idle.objects.length == 1 && idle.ids.length == 1, "published a partial second growth");
            must(idle.count == 1, "lost existing idle count");
            Copier malformed = new Copier() {
                public Object[] objects(Object[] array, int n) { return Arrays.copyOf(array, n - 1); }
                public long[] longs(long[] array, int n) { return Arrays.copyOf(array, n); }
            };
            try { idle.ensure(2, malformed); throw new AssertionError("accepted malformed array"); }
            catch (IllegalStateException expected) { /* expected */ }
            must(idle.ids.length == 1 && idle.objects.length == 1, "malformed result published");
            idle.put(12L, new Object());
            must(idle.count == 2, "failed recovery");
            must(idle.remove(11L) == token, "existing object identity lost");
            must(idle.remove(12L) != null, "second object lost");
            must(idle.count == 0, "lease balance mismatch");
        }
        System.out.println("PASS: 10000 idle-lane failure atomicity, malformed copy and recovery cycles");
    }
}
