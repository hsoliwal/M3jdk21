/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import java.util.ArrayList;
import java.util.List;
import jdk.m3.collections.bits.LongBitSet;
import org.junit.jupiter.api.Test;

/**
 * Product regression for live set-bit callbacks, including growth to the largest index.
 * The maximum-index case owns one 256 MiB backing array and runs with {@code -Xmx384m}.
 */
public final class LongBitSetTraversalBoundaryTest {
    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }

    @Test
    public void emptyAndNullCallbacks() {
        LongBitSet bits = LongBitSet.of(0);
        bits.forEachSetBit(null);
        int[] calls = {0};
        bits.forEachSetBit(index -> calls[0]++);
        check(calls[0] == 0, "empty callback count");
        bits.set(0);
        try {
            bits.forEachSetBit(null);
            throw new AssertionError("nonempty null callback accepted");
        } catch (NullPointerException expected) {
            check(bits.get(0), "null callback did not mutate the owner");
        }
    }

    @Test
    public void liveMutationOrder() {
        LongBitSet bits = LongBitSet.of(64);
        bits.set(1);
        bits.set(63);
        bits.set(129);
        List<Integer> visited = new ArrayList<>();
        bits.forEachSetBit(index -> {
            visited.add(index);
            if (index == 1) {
                bits.clear(63);
                bits.set(64);
                bits.set(0); // Already passed coordinates must not be revisited.
            } else if (index == 64) {
                bits.set(128);
            } else if (index == 128) {
                bits.clear(); // Future 129 disappears before the next callback.
            } else {
                throw new AssertionError("unexpected live callback: " + index);
            }
        });
        check(visited.equals(List.of(1, 64, 128)), "live ascending callback order");
        check(bits.isEmpty(), "clear-all callback visible");
    }

    @Test
    public void callbackExceptionIdentity() {
        LongBitSet bits = LongBitSet.of(64);
        bits.set(1);
        bits.set(63);
        RuntimeException sentinel = new RuntimeException("callback identity");
        int[] calls = {0};
        try {
            bits.forEachSetBit(index -> {
                calls[0]++;
                check(index == 1, "exception stops at first callback");
                bits.clear(63);
                throw sentinel;
            });
            throw new AssertionError("missing callback exception");
        } catch (RuntimeException observed) {
            check(observed == sentinel, "exact callback exception preserved");
        }
        check(calls[0] == 1 && !bits.get(63), "effects before callback failure preserved");
    }

    @Test
    public void maximumIndexAndCallbackGrowth() {
        LongBitSet bits = LongBitSet.of(64);
        bits.set(0);
        int[] calls = {0};
        bits.forEachSetBit(index -> {
            if (calls[0]++ == 0) {
                check(index == 0, "initial small-owner callback");
                bits.set(Integer.MAX_VALUE); // The callback invalidates entry-time capacity.
            } else {
                check(calls[0] == 2 && index == Integer.MAX_VALUE,
                        "visit callback-added maximum exactly once");
                bits.clear(Integer.MAX_VALUE);
                bits.set(1); // Terminal callbacks may also insert earlier coordinates.
            }
        });
        check(calls[0] == 2, "small-owner growth terminates after maximum");
        check(bits.get(1) && !bits.get(Integer.MAX_VALUE), "terminal mutation survives");

        bits.clear();
        bits.set(Integer.MAX_VALUE);
        calls[0] = 0;
        bits.forEachSetBit(index -> {
            check(index == Integer.MAX_VALUE, "pre-existing maximum index");
            calls[0]++;
        });
        check(calls[0] == 1, "single maximum callback terminates");

        bits.set(63);
        calls[0] = 0;
        bits.forEachSetBit(index -> {
            check(index == 63 && calls[0]++ == 0, "maximum removal callback");
            bits.clear(Integer.MAX_VALUE);
        });
        check(calls[0] == 1, "removed future maximum not delivered");

        bits.clear();
        bits.set(Integer.MAX_VALUE - 1);
        calls[0] = 0;
        bits.forEachSetBit(index -> {
            check(index == Integer.MAX_VALUE - 1 + calls[0], "last two indexes in order");
            if (calls[0]++ == 0) bits.set(Integer.MAX_VALUE);
        });
        check(calls[0] == 2, "maximum added by its immediate predecessor");
        bits.clear(Integer.MAX_VALUE - 1);
        RuntimeException sentinel = new RuntimeException("terminal callback identity");
        try {
            bits.forEachSetBit(index -> { throw sentinel; });
            throw new AssertionError("missing terminal callback exception");
        } catch (RuntimeException observed) {
            check(observed == sentinel, "terminal exception identity");
        }
        try {
            bits.forEachSetBit(null);
            throw new AssertionError("maximum index null callback accepted");
        } catch (NullPointerException expected) {
            check(bits.get(Integer.MAX_VALUE), "maximum survives null callback failure");
        }
        bits.clear();
        bits.forEachSetBit(null);
        check(bits.isEmpty(), "empty large owner allows null callback");
    }

    public static void main(String[] args) {
        LongBitSetTraversalBoundaryTest test = new LongBitSetTraversalBoundaryTest();
        test.emptyAndNullCallbacks();
        test.liveMutationOrder();
        test.callbackExceptionIdentity();
        test.maximumIndexAndCallbackGrowth();
        System.out.println("LONG_BITSET_BOUNDARY=PASS cases=4 maxHeap="
                + Runtime.getRuntime().maxMemory());
    }
}
