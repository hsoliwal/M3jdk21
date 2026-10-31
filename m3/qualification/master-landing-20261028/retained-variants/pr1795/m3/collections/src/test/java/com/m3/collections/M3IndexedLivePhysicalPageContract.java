/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

/** Source-owned Java21 differential for live sorted/filtered physical viewport pages. */
public final class M3IndexedLivePhysicalPageContract {
    private M3IndexedLivePhysicalPageContract() { }
    private static long checks;
    private record Pair(int key, int identity) { }

    private static void check(boolean assertion, String why) {
        checks++;
        if (!assertion) throw new AssertionError(why + " check=" + checks);
    }

    private static void refuses(Class<? extends Throwable> expected, Runnable callback) {
        checks++;
        try {
            callback.run();
        } catch (Throwable thrown) {
            if (expected.isInstance(thrown)) return;
            throw new AssertionError("expected " + expected.getSimpleName(), thrown);
        }
        throw new AssertionError("missing " + expected.getSimpleName());
    }

    private static void verifyPage(M3IndexedLiveView<Pair> view, int first, int budget) {
        int[] full = view.physicalIndices();
        int expected = Math.min(budget, full.length - first);
        int[] page = new int[expected + 4];
        Arrays.fill(page, -99);
        int written = view.copyPhysicalWindow(first, budget, page, 2);
        check(written == expected, "bounded output count");
        check(page[0] == -99 && page[1] == -99
                && page[expected + 2] == -99 && page[expected + 3] == -99,
                "destination guards");
        for (int i = 0; i < expected; i++) {
            check(page[i + 2] == full[first + i], "original occurrence order");
        }
    }

    private static void boundaries() {
        M3ArrayList<Pair> owner = new M3ArrayList<>(List.of(
                new Pair(2, 0), new Pair(3, 1), new Pair(2, 2), new Pair(4, 3)));
        var view = M3IndexedLiveView.filteredSorted(owner,
                item -> (item.key() & 1) == 0, Comparator.comparingInt(Pair::key));
        check(Arrays.equals(view.physicalIndices(), new int[]{0, 2, 3}),
                "stable ties keep two different physical occurrences");
        verifyPage(view, 0, Integer.MAX_VALUE);
        verifyPage(view, 1, 1);
        verifyPage(view, 3, Integer.MAX_VALUE);
        check(view.copyPhysicalWindow(3, 0, new int[0], 0) == 0,
                "empty page permits destination end");
        int[] guarded = new int[3];
        Arrays.fill(guarded, 77);
        refuses(NullPointerException.class,
                () -> view.copyPhysicalWindow(0, 1, null, 0));
        refuses(IndexOutOfBoundsException.class,
                () -> view.copyPhysicalWindow(-1, 1, guarded, 0));
        refuses(IndexOutOfBoundsException.class,
                () -> view.copyPhysicalWindow(4, 0, guarded, 0));
        refuses(IndexOutOfBoundsException.class,
                () -> view.copyPhysicalWindow(0, -1, guarded, 0));
        refuses(IndexOutOfBoundsException.class,
                () -> view.copyPhysicalWindow(0, 3, guarded, 1));
        refuses(IndexOutOfBoundsException.class,
                () -> view.copyPhysicalWindow(0, 1, guarded, -1));
        check(Arrays.equals(guarded, new int[]{77, 77, 77}),
                "invalid requests never partially overwrite");
        owner.set(1, new Pair(0, 5));
        verifyPage(view, 0, Integer.MAX_VALUE);
        owner.add(new Pair(2, 7));
        verifyPage(view, 1, 2);
        owner.remove(0);
        verifyPage(view, 0, Integer.MAX_VALUE);
        owner.sort(Comparator.comparingInt(Pair::key));
        verifyPage(view, 0, Integer.MAX_VALUE);

        AtomicReference<M3IndexedLiveView<Pair>> active = new AtomicReference<>();
        M3IndexedLiveView<Pair> invalidating = M3IndexedLiveView.filtered(owner, item -> {
            active.get().invalidate();
            return true;
        });
        active.set(invalidating);
        int[] untouched = {87, 87};
        refuses(ConcurrentModificationException.class,
                () -> invalidating.copyPhysicalWindow(0, 1, untouched, 1));
        check(Arrays.equals(untouched, new int[]{87, 87}),
                "callback invalidation writes no selected page");
    }

    private static Pair randomPair(Random random, int identity) {
        return new Pair(random.nextInt(13) - 6, identity);
    }

    private static void randomized() {
        Random random = new Random(0x4d3356494557504cL);
        for (int trial = 0; trial < 3_000; trial++) {
            M3ArrayList<Pair> owner = new M3ArrayList<>();
            int count = random.nextInt(48);
            for (int i = 0; i < count; i++) owner.add(randomPair(random, i));
            M3IndexedLiveView<Pair> view = M3IndexedLiveView.filteredSorted(owner,
                    pair -> pair != null && (pair.key() & 1) == 0,
                    Comparator.comparingInt(Pair::key));
            for (int pass = 0; pass < 6; pass++) {
                int size = view.size();
                int start = random.nextInt(size + 1);
                int max = pass == 0 ? Integer.MAX_VALUE : random.nextInt(size + 4);
                verifyPage(view, start, max);
                switch (pass % 4) {
                    case 0 -> owner.add(randomPair(random, trial * 10 + pass));
                    case 1 -> {
                        if (!owner.isEmpty()) owner.set(random.nextInt(owner.size()),
                                randomPair(random, trial * 10 + pass));
                    }
                    case 2 -> {
                        if (!owner.isEmpty()) owner.remove(random.nextInt(owner.size()));
                    }
                    default -> owner.sort(Comparator.comparingInt(Pair::key));
                }
            }
            check(Objects.equals(view.getClass(), M3IndexedLiveView.class),
                    "original live-view owner retained");
        }
    }

    public static void main(String[] args) {
        boundaries();
        randomized();
        System.out.println("M3_INDEXED_LIVE_PHYSICAL_PAGE_PASS checks=" + checks);
    }
}
