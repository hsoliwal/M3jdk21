/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Independent primitive wheel skip oracle for ordinary and far-future deadlines. */
public final class M3LongTimingWheelFastForwardSelfTest {
    private static long checks;
    private M3LongTimingWheelFastForwardSelfTest() { }

    private static void check(boolean value, String where) {
        checks++;
        if (!value) throw new AssertionError(where);
    }

    /** Same bounded random schedule applies to both exact original and candidate. */
    private static void ordinary() {
        for (int width : new int[] {1, 2, 4, 8, 16, 64}) {
            LongTimingWheel wheel = new LongTimingWheel(width, 1L, 100L);
            Random random = new Random(0x83bada88df00b88aL ^ width);
            Map<Long, Long> pending = new HashMap<>();
            List<Long> delivered = new ArrayList<>();
            long id = 1000L;
            long now = 99L;
            for (int step = 0; step < 2000; step++) {
                for (int i = random.nextInt(5); i >= 0; i--) {
                    long deadline = now + random.nextInt(100);
                    wheel.schedule(id, deadline);
                    pending.put(id, Math.max(deadline, wheel.currentTick()));
                    id++;
                }
                now += random.nextInt(7);
                Set<Long> expected = new HashSet<>();
                for (var entry : pending.entrySet()) {
                    if (entry.getValue() <= now) expected.add(entry.getKey());
                }
                delivered.clear();
                int count = wheel.advanceTo(now, delivered::add);
                check(count == delivered.size(), "count vs values at step " + step);
                check(expected.size() == delivered.size(), "expected count at step " + step);
                for (long item : delivered) {
                    check(expected.remove(item), "duplicate/early value " + item);
                    check(pending.remove(item) != null, "unexpected value");
                }
                check(expected.isEmpty(), "missing due");
                check(wheel.size() == pending.size(), "size");
                check(wheel.currentTick() == Math.max(100L, now + 1L), "cursor");
            }
            long limit = now + 200L;
            delivered.clear();
            int count = wheel.advanceTo(limit, delivered::add);
            check(count == pending.size(), "final drain size");
            check(new HashSet<>(delivered).equals(pending.keySet()), "final drain IDs");
            check(wheel.size() == 0 && wheel.currentTick() == limit + 1, "final state");
        }
        System.out.println("PASS normal checks=" + checks);
    }

    private static void huge() {
        long billion = 1_000_000_000L;
        LongTimingWheel empty = new LongTimingWheel(4, 1L, 0L);
        check(empty.advanceTo(billion, v -> { throw new AssertionError("unexpected"); }) == 0,
                "empty billion advance");
        check(empty.currentTick() == billion + 1, "empty next tick");
        empty.schedule(7L, billion + 4);
        check(empty.advanceTo(billion + 3, v -> { throw new AssertionError("early"); }) == 0,
                "future pending skip");
        check(empty.currentTick() == billion + 4, "skipped exact cursor");
        List<Long> ids = new ArrayList<>();
        check(empty.advanceTo(billion + 4, ids::add) == 1, "future due after skip");
        check(ids.equals(List.of(7L)), "future ID");

        LongTimingWheel spaced = new LongTimingWheel(4, 1L, 0L);
        spaced.schedule(99L, billion + 8);
        spaced.schedule(11L, billion);
        spaced.schedule(12L, billion + 4);
        List<Long> observed = new ArrayList<>();
        check(spaced.advanceTo(billion + 10, v -> {
            check(spaced.currentTick() == (v == 11L ? billion : v == 12L ? billion + 4 : billion + 8),
                    "callback saw incorrect due tick");
            observed.add(v);
        }) == 3, "separated deadlines emitted");
        check(observed.equals(List.of(11L, 12L, 99L)), "ordering after sparse skip");
        check(spaced.currentTick() == billion + 11 && spaced.size() == 0,
                "cursor/size after sparse skip");

        LongTimingWheel retry = new LongTimingWheel(2, 1L, 0L);
        retry.schedule(88L, billion + 2);
        retry.schedule(77L, billion);
        RuntimeException failure = new RuntimeException("callback failed");
        try {
            retry.advanceTo(billion + 3, v -> { if (v == 77L) throw failure; });
            throw new AssertionError("callback unexpectedly returned");
        } catch (RuntimeException received) {
            check(received == failure, "callback error identity");
        }
        check(retry.size() == 2 && retry.currentTick() == billion, "retry retained both IDs");
        observed.clear();
        check(retry.advanceTo(billion + 4, observed::add) == 2, "retry drain count");
        check(observed.equals(List.of(77L, 88L)), "retry drain order");

        LongTimingWheel bound = new LongTimingWheel(2, 1L, Long.MIN_VALUE);
        check(bound.advanceTo(Long.MAX_VALUE, v -> { throw new AssertionError("empty should not visit"); }) == 0,
                "signed long full-range empty skip");
        check(bound.currentTick() == Long.MAX_VALUE, "terminal cursor");
        check(bound.advanceTo(Long.MAX_VALUE, v -> { throw new AssertionError("terminal replay"); }) == 0,
                "terminal only once");
        try {
            bound.schedule(123L, Long.MAX_VALUE);
            throw new AssertionError("scheduled past terminal");
        } catch (IllegalStateException expected) {
            check("timing wheel exhausted".equals(expected.getMessage()), "terminal refusal");
        }
        LongTimingWheel overflow = new LongTimingWheel(2, 1L, Long.MIN_VALUE);
        overflow.schedule(88L, Long.MAX_VALUE - 1L);
        check(overflow.advanceTo(Long.MAX_VALUE - 2L, value -> {
            throw new AssertionError("early across signed range");
        }) == 0, "overflow-safe large pending skip");
        check(overflow.currentTick() == Long.MAX_VALUE - 1L,
                "full-range pending cursor");
        ids.clear();
        check(overflow.advanceTo(Long.MAX_VALUE - 1L, ids::add) == 1,
                "full-range pending delivery");
        check(ids.equals(List.of(88L)), "full-range ID");
        check(overflow.advanceTo(Long.MAX_VALUE, ids::add) == 0,
                "no replay after terminal tick");
        check(overflow.currentTick() == Long.MAX_VALUE, "full-range terminal cursor");
        System.out.println("PASS huge-gap checks=" + checks);
    }

    public static void main(String[] args) {
        if (args.length == 0 || "ordinary".equals(args[0])) ordinary();
        if (args.length == 0 || "huge".equals(args[0])) huge();
    }
}
