/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/** Original-owner live, filtered and range-sorted primitive viewport oracle. */
public final class M3LongSortedPageContract {
    private static long checks;
    private M3LongSortedPageContract() { }
    private static void check(boolean ok, String why) {
        checks++;
        if (!ok) throw new AssertionError(why + " check=" + checks);
    }
    private static void refuses(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable caught) {
            if (type.isInstance(caught)) return;
            throw new AssertionError("wrong refusal", caught);
        }
        throw new AssertionError("missing " + type.getName());
    }
    public static void main(String[] ignored) {
        Random random = new Random(0x6d334c6f6e67534cL);
        for (int trial = 0; trial < 4000; trial++) {
            M3LongTreeSet owner = new M3LongTreeSet();
            for (int i = 0, n = random.nextInt(80); i < n; i++) {
                long value = random.nextInt(130) - 65;
                owner.add(value);
            }
            if (trial % 3 == 0) owner.add(Long.MIN_VALUE);
            if (trial % 7 == 0) owner.add(Long.MAX_VALUE);
            // Nested primitive live wrappers must continue to evaluate the
            // parent predicate and preserve signed natural sort order.
            M3LongSortedSet view = M3LongSortedViews.filter(owner, v -> v % 3 != 0);
            view = M3LongSortedViews.filter(view, v -> v != -2);
            if (trial % 5 == 0)
                view = M3LongSortedViews.subSet(view, -32, 33);
            if (trial % 11 == 0)
                view = M3LongSortedViews.unmodifiable(view);
            long[] oracle = view.toArray();
            int skip = trial % 19 == 0 ? Integer.MAX_VALUE : random.nextInt(95);
            int limit = random.nextInt(45);
            long[] output = new long[limit + 5];
            Arrays.fill(output, 778899L);
            int count = M3LongSortedViews.copyPage(view, skip, limit, output, 2);
            int expected = skip >= oracle.length ? 0 : Math.min(limit, oracle.length - skip);
            check(count == expected, "matched page length");
            for (int i = 0; i < count; i++)
                check(output[2 + i] == oracle[skip + i], "filtered sorted order");
            check(output[0] == 778899L && output[1] == 778899L
                && output[limit + 2] == 778899L, "out-of-page fence");
            long[] old = output.clone();
            final M3LongSortedSet stable = view;
            refuses(IndexOutOfBoundsException.class, () ->
                M3LongSortedViews.copyPage(stable, -1, 1, output, 0));
            refuses(IndexOutOfBoundsException.class, () ->
                M3LongSortedViews.copyPage(stable, 0, -1, output, 0));
            refuses(IndexOutOfBoundsException.class, () ->
                M3LongSortedViews.copyPage(stable, 0, limit, output, output.length));
            refuses(NullPointerException.class, () ->
                M3LongSortedViews.copyPage(stable, 0, limit, null, 0));
            check(Arrays.equals(old, output), "preflight refusal atomicity");
            // One edit after earlier traversal must be visible in a new call:
            // no retained source snapshot or cached predicate truth.
            if (trial % 7 == 0) {
                owner.add(33);
                long[] current = stable.toArray();
                long[] fresh = new long[current.length + 3];
                int copied = M3LongSortedViews.copyPage(stable, 0, fresh.length, fresh, 0);
                check(copied == current.length, "live updated size");
                for (int j = 0; j < copied; j++)
                    check(fresh[j] == current[j], "live updated order");
            }
        }
        M3LongTreeSet owner = new M3LongTreeSet();
        owner.add(5);
        AtomicInteger probes = new AtomicInteger();
        M3LongSortedSet observed = M3LongSortedViews.filter(owner, v -> {
            probes.incrementAndGet();
            return true;
        });
        check(M3LongSortedViews.copyPage(observed, 0, 0, new long[0], 0) == 0,
            "zero-demand result");
        check(probes.get() == 0, "zero demand does not evaluate predicate");
        System.out.println("M3_LONG_SORTED_PAGE_PASS checks=" + checks);
    }
}
