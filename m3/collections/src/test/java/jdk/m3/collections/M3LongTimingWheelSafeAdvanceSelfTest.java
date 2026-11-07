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

/** Independent due-time oracle and explicit original-code RED scenarios. */
public final class M3LongTimingWheelSafeAdvanceSelfTest {
    private static long checks;
    private M3LongTimingWheelSafeAdvanceSelfTest() { }

    private static void require(boolean ok, String why) {
        checks++;
        if (!ok) throw new AssertionError(why);
    }

    private static void ordinary() {
        Random random = new Random(0x6255afe70becc11aL);
        for (int wheelSize : new int[] {1, 2, 4, 8, 16, 64}) {
            LongTimingWheel wheel = new LongTimingWheel(wheelSize, 1L, 0L);
            Map<Long, Long> pending = new HashMap<>();
            long id = 100L;
            List<Long> report = new ArrayList<>();
            for (int step = 0; step < 3000; step++) {
                // Exercise 1, 3 and 5 new events at a single timeline position.
                int inserted = 1 + random.nextInt(5);
                for (int i = 0; i < inserted; i++) {
                    long deadline = step + random.nextInt(51);
                    wheel.schedule(id, deadline);
                    pending.put(id, deadline);
                    id++;
                }
                final long now = step;
                Set<Long> expectedDue = new HashSet<>();
                for (var entry : pending.entrySet()) {
                    if (entry.getValue() <= now) expectedDue.add(entry.getKey());
                }
                report.clear();
                int count = wheel.advanceTo(now, report::add);
                require(count == report.size(), "emission count at " + step);
                require(report.size() == expectedDue.size(), "due size at " + step);
                for (Long value : report) {
                    require(expectedDue.remove(value), "duplicate or early emission: " + value);
                    require(pending.remove(value) != null, "unexpected due value");
                }
                require(expectedDue.isEmpty(), "missing due events");
                require(wheel.size() == pending.size(), "tracked size diverged");
                require(wheel.currentTick() == step + 1L, "tick cursor drift");
                if ((step & 127) == 0) {
                    require(wheel.advanceTo(now, report::add) == 0, "old tick revisited");
                }
            }
            int complete = wheel.advanceTo(3100L, value -> {
                require(pending.remove(value) != null, "unexpected final value");
            });
            require(complete >= 0 && wheel.size() == 0 && pending.isEmpty(), "final drain failed");
            wheel.clear();
            require(wheel.size() == 0, "clear did not zero size");
        }
    }

    private static void callbackFailure() {
        LongTimingWheel wheel = new LongTimingWheel(2, 1L, 0L);
        wheel.schedule(40L, 4L); // future rotation in same modulo bucket.
        wheel.schedule(20L, 0L);
        wheel.schedule(30L, 0L);
        RuntimeException identity = new RuntimeException("expected callback failure");
        List<Long> delivered = new ArrayList<>();
        try {
            wheel.advanceTo(0L, v -> {
                if (v == 20L) throw identity;
                delivered.add(v);
            });
            throw new AssertionError("callback failure swallowed");
        } catch (RuntimeException caught) {
            require(caught == identity, "original callback exception identity lost");
        }
        require(delivered.equals(List.of(30L)), "successfully emitted item changed");
        require(wheel.size() == 2, "failing and pending nodes orphaned: " + wheel.size());
        delivered.clear();
        require(wheel.advanceTo(0L, delivered::add) == 1, "failing node could not retry");
        require(delivered.equals(List.of(20L)), "retry order wrong");
        require(wheel.size() == 1, "future node lost");
        delivered.clear();
        require(wheel.advanceTo(4L, delivered::add) == 1, "future modulo rotation lost");
        require(delivered.equals(List.of(40L)), "future emission wrong");
        require(wheel.size() == 0, "wheel leaked node after recovery");
        // Another callback exception at the first item, then successful retry.
        wheel.schedule(1L, 6L);
        wheel.schedule(2L, 6L);
        AssertionError error = new AssertionError("expected error");
        try { wheel.advanceTo(6L, value -> { throw error; }); throw new AssertionError("Error swallowed"); }
        catch (AssertionError caught) { require(caught == error, "Error identity lost"); }
        require(wheel.size() == 2, "Error orphaned nodes");
        delivered.clear();
        require(wheel.advanceTo(6L, delivered::add) == 2, "Error retry failed");
        require(delivered.equals(List.of(2L, 1L)), "Error retry order drift");
    }

    private static void finalTick() {
        LongTimingWheel wheel = new LongTimingWheel(2, 1L, Long.MAX_VALUE);
        wheel.schedule(44L, Long.MAX_VALUE);
        List<Long> received = new ArrayList<>();
        require(wheel.advanceTo(Long.MAX_VALUE, received::add) == 1,
                "terminal tick does not complete exactly once");
        require(received.equals(List.of(44L)), "terminal tick output");
        require(wheel.currentTick() == Long.MAX_VALUE, "terminal cursor wrapped");
        require(wheel.advanceTo(Long.MAX_VALUE, received::add) == 0, "terminal tick replayed");
        require(received.size() == 1 && wheel.size() == 0, "terminal re-delivery");
        try { wheel.schedule(45L, Long.MAX_VALUE); throw new AssertionError("scheduled after final tick"); }
        catch (IllegalStateException expected) { require("timing wheel exhausted".equals(expected.getMessage()), "wrong terminal refusal"); }
    }

    public static void main(String[] args) {
        if (args.length == 0 || "ordinary".equals(args[0])) ordinary();
        if (args.length == 0 || "callback".equals(args[0])) callbackFailure();
        if (args.length == 0 || "limit".equals(args[0])) finalTick();
        System.out.println("PASS checks=" + checks + " mode=" + (args.length == 0 ? "all" : args[0]));
    }
}
