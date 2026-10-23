/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
/* Modified 2026: compare available JNI selection and retain explicit unavailable-library refusal. */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/** Independent dense-grid exact oracle for native generic M3 object range map. */
public final class M3ObjectRangeMapSelfTest {
    private static long checks;
    private M3ObjectRangeMapSelfTest() { }
    private static void eq(Object expected, Object got, String why) {
        checks++;
        if (!Objects.equals(expected, got)) throw new AssertionError(
                why + " expected=" + expected + " actual=" + got + " check=" + checks);
    }
    private static void ok(boolean value, String why) {
        checks++;
        if (!value) throw new AssertionError(why + " check=" + checks);
    }
    private static void fails(Class<? extends Throwable> expected, Runnable action, String why) {
        checks++;
        try { action.run(); }
        catch (Throwable result) {
            if (expected.isInstance(result)) return;
            throw new AssertionError(why + " bad exception", result);
        }
        throw new AssertionError(why + " did not refuse");
    }
    private static final int N = 128;
    private static boolean within(Comparator<Integer> order, int from, int to, int point) {
        return order.compare(from, point) <= 0 && order.compare(point, to) < 0;
    }
    private static void randomized(boolean reverse) {
        Comparator<Integer> order = reverse ? Comparator.reverseOrder() : Comparator.naturalOrder();
        M3ObjectRangeMap<Integer, Integer> map = new M3ObjectRangeMap<>(order);
        boolean[] present = new boolean[N];
        Integer[] payload = new Integer[N];
        int[] flags = new int[N];
        Random random = new Random(0x4d3352616e676531L + (reverse ? 17 : 0));
        for (int trial = 0; trial < 6_000; trial++) {
            int x = random.nextInt(N + 1), y = random.nextInt(N + 1);
            int first = Math.min(x, y), last = Math.max(x, y);
            int from = reverse ? last : first;
            int to = reverse ? first : last;
            int mode = random.nextInt(8);
            if (mode <= 3) {
                Integer value = mode == 0 ? null : random.nextInt(20) - 10;
                int state = (trial % 3 == 0 ? Integer.MIN_VALUE : 0)
                        | (trial % 7 == 0 ? M3ObjectRangeMap.DIRTY : 0);
                map.put(from, to, value, state);
                for (int i = 0; i < N; i++) if (within(order, from, to, i)) {
                    present[i] = true; payload[i] = value;
                    flags[i] = state | M3ObjectRangeMap.LIVE;
                }
            } else if (mode <= 5) {
                boolean wasPresent = false;
                for (int i = 0; i < N; i++) if (within(order, from, to, i)) {
                    wasPresent |= present[i];
                }
                eq(wasPresent, map.remove(from, to), "remove changed set");
                for (int i = 0; i < N; i++) if (within(order, from, to, i)) {
                    present[i] = false; payload[i] = null; flags[i] = 0;
                }
            } else if (mode == 6) {
                int key = random.nextInt(N);
                if (present[key]) {
                    int before = flags[key];
                    eq(before, map.updateState(key, M3ObjectRangeMap.VISIBLE, 0), "update previous");
                    for (int i = 0; i < N; i++) {
                        // Only one whole owner span changes state. Find its boundaries by
                        // checking the span that contains the chosen coordinate.
                        if (present[i]) {
                            boolean sameSpan = false;
                            var v = map.spans();
                            for (int slot = 0; slot < v.size(); slot++) {
                                int a = v.startAt(slot), b = v.endAt(slot);
                                if (within(order, a, b, key) && within(order, a, b, i)) {
                                    sameSpan = true;
                                }
                            }
                            if (sameSpan) flags[i] |= M3ObjectRangeMap.VISIBLE;
                        }
                    }
                } else fails(IllegalArgumentException.class,
                        () -> map.updateState(key, M3ObjectRangeMap.VISIBLE, 0), "absent mutation");
            } else {
                int key = random.nextInt(N);
                eq(present[key], map.containsPoint(key), "present point");
                eq(payload[key], map.get(key), "point value");
            }
            // Invariant and content checks across the actual array owner, not a provider adapter.
            if (trial % 7 == 0) {
                for (int i = 0; i < N; i++) {
                    eq(present[i], map.containsPoint(i), "grid presence");
                    eq(payload[i], map.get(i), "grid payload");
                    eq(flags[i], map.stateAtPoint(i), "state flags full32");
                }
                var view = map.spans();
                eq(map.spanCount(), view.size(), "array lengths agree");
                for (int i = 0; i < view.size(); i++) {
                    ok(order.compare(view.startAt(i), view.endAt(i)) < 0, "span nonempty");
                    if (i > 0) ok(order.compare(view.endAt(i-1), view.startAt(i)) <= 0,
                            "sorted/disjoint comparator order");
                }
                var reversed = view.reversed().reversed();
                eq(view.size(), reversed.size(), "double reverse length");
                for (int i = 0; i < view.size(); i++) {
                    eq(view.physicalAt(i), reversed.physicalAt(i), "double reverse positions");
                }
                var selected = view.selected(Integer.MIN_VALUE, M3ObjectRangeMap.VISIBLE,
                        M3IndexPathMasks.Backend.JAVA);
                for (int i = 0; i < selected.size(); i++) {
                    int s = selected.stateAt(i);
                    ok((s & Integer.MIN_VALUE) != 0 && (s & M3ObjectRangeMap.VISIBLE) == 0,
                            "signed high-bit mask equality");
                }
                eq(0, view.slice(0,0).size(), "zero window");
            }
        }
    }
    private static void contracts() {
        var map = new M3ObjectRangeMap<Integer, String>(Comparator.naturalOrder());
        map.put(0, 20, "A", M3ObjectRangeMap.VISIBLE);
        eq("A", map.get(5), "basic get");
        eq("fallback", map.getOrDefault(99, "fallback"), "missing default");
        map.put(5, 10, "B", M3ObjectRangeMap.DIRTY);
        eq(3, map.spanCount(), "split original span");
        eq(List.of("A", "B", "A"), List.of(map.get(1), map.get(6), map.get(12)),
                "split preserves neighbors");
        var stable = map.spans();
        eq(3, stable.size(), "initial span count");
        var sorted = stable.sortedByValue(Comparator.naturalOrder());
        eq(3, sorted.size(), "sorted physical index count");
        eq("A", sorted.valueAt(0), "sorted stable first");
        eq("A", sorted.valueAt(1), "sorted stable second");
        eq("B", sorted.valueAt(2), "sorted final");
        AtomicInteger callbacks = new AtomicInteger();
        var filtered = stable.filtered(value -> {
            callbacks.incrementAndGet(); return "A".equals(value);
        });
        eq(3, callbacks.get(), "predicate once per span");
        eq(2, filtered.size(), "selected spans");
        eq(1, stable.slice(1,2).size(), "metadata slice");
        eq(3, map.intersecting(2,18).size(), "intersection set");
        eq(0, map.intersecting(20,20).size(), "empty intersection");
        map.updateState(7, Integer.MIN_VALUE, 0);
        eq(Integer.MIN_VALUE | M3ObjectRangeMap.DIRTY | M3ObjectRangeMap.LIVE,
                map.stateAtPoint(7), "state does not truncate high bits");
        fails(ConcurrentModificationException.class, stable::size, "generation rejects old index");
        fails(IllegalArgumentException.class, () -> map.put(22,2, "reversed"),
                "inverted comparator range");
        fails(IllegalArgumentException.class, () -> map.put(5,7, "live", 1),
                "reserved LIVE bit");
        fails(IllegalArgumentException.class,
                () -> map.updateState(5, 4, 4), "overlap states");
        fails(IllegalArgumentException.class, () -> map.spans().selected(1,1,
                M3IndexPathMasks.Backend.JAVA), "contradictory selected stages");
        if (M3IndexPathMasks.nativeAvailable()) {
            for (var index : List.of(map.spans(), map.spans().reversed())) {
                for (int[] masks : new int[][] {{1, 0}, {Integer.MIN_VALUE, 2},
                        {2, 4}, {0, Integer.MIN_VALUE}}) {
                    var javaSelected = index.selected(masks[0], masks[1], M3IndexPathMasks.Backend.JAVA);
                    var nativeSelected = index.selected(masks[0], masks[1],
                            M3IndexPathMasks.Backend.NATIVE_REQUIRED);
                    eq(javaSelected.size(), nativeSelected.size(), "native selected size");
                    for (int i = 0; i < javaSelected.size(); i++) {
                        eq(javaSelected.physicalAt(i), nativeSelected.physicalAt(i), "native selected order");
                        eq(javaSelected.startAt(i), nativeSelected.startAt(i), "native selected start");
                        eq(javaSelected.endAt(i), nativeSelected.endAt(i), "native selected end");
                        ok(javaSelected.valueAt(i) == nativeSelected.valueAt(i), "native value identity");
                        eq(javaSelected.stateAt(i), nativeSelected.stateAt(i), "native full32 state");
                    }
                }
            }
        } else {
            fails(UnsatisfiedLinkError.class, () -> map.spans().selected(1,0,
                    M3IndexPathMasks.Backend.NATIVE_REQUIRED), "explicit no JNI fallback");
        }
        ok(map.remove(0,20), "delete all");
        eq(0, map.spanCount(), "empty map");
        var nullable = new M3ObjectRangeMap<Integer, Integer>(Comparator.nullsFirst(Integer::compareTo));
        nullable.put(null, 10, null);
        eq(null, nullable.get(null), "nullable endpoint and value");
        ok(nullable.containsPoint(null), "nullable present not inferred from get");
        nullable.clear();
        eq(0, nullable.spanCount(), "clear removes all");
    }
    public static void main(String[] args) {
        if (args.length != 0) throw new IllegalArgumentException("usage: no args");
        contracts(); randomized(false); randomized(true);
        System.out.println("PASS M3ObjectRangeMapSelfTest checks=" + checks + " randomized=12000");
    }
}
