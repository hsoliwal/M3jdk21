// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Standalone Java 21 differential contract; no Maven/JUnit or EMF required. */
public final class M3ObservableListContract {
    private static long checks;

    private M3ObservableListContract() { }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        M3ObservableList<String> list = new M3ObservableList<>();
        List<String> events = new ArrayList<>();
        M3ObservableList.Listener<String> listener = (owner, change, index, count, before, after, epoch) -> {
            check(owner == list && epoch == events.size() + 1L, "event generation and owner");
            events.add(change + ":" + index + ":" + count + ":" + before + ":" + after);
        };
        list.addListener(listener);
        list.add(null);
        list.add("x");
        check(list.equals(Arrays.asList(null, "x")), "null and encounter order");
        list.set(0, null); // Even a same-reference set is observable.
        check(list.remove(1).equals("x"), "remove value");
        list.addAll(List.of("a", "b", "c"));
        list.subList(1, 3).clear();
        check(list.equals(Arrays.asList(null, "c")), "backed sublist removal");
        list.clear();
        list.clear(); // No event for no change.
        check(events.equals(List.of(
                "ADD:0:1:null:null", "ADD:1:1:null:x", "SET:0:1:null:null",
                "REMOVE:1:1:x:null", "ADD:1:3:null:null", "REMOVE:1:2:null:null",
                "CLEAR:0:2:null:null")), "ordered atom and range events: " + events);
        check(list.generation() == 7L && list.size() == 0, "no-op event/generation");
        check(list.removeListener(listener) && list.listenerCount() == 0,
                "subscription identity");

        List<String> trace = new ArrayList<>();
        M3ObservableList.Listener<String> late = (owner, change, index, count, before, after, epoch) ->
                trace.add("late");
        M3ObservableList.Listener<String> early = new M3ObservableList.Listener<>() {
            @Override public void changed(M3ObservableList<String> owner,
                    M3ObservableList.Change change, int index, int count,
                    String before, String after, long epoch) {
                trace.add("early");
                if (trace.size() == 1) owner.addListener(late);
                try {
                    owner.add("reentrant");
                    throw new AssertionError("reentrant mutation succeeded");
                } catch (IllegalStateException expected) {
                    check(owner.size() >= 1, "committed before notification");
                }
            }
        };
        list.addListener(early);
        list.add("alpha");
        list.add("beta");
        check(trace.equals(List.of("early", "early", "late")), "copy-on-write subscribers");
        check(list.equals(List.of("alpha", "beta")), "reentrancy rejected");
        list.removeListener(early);
        list.removeListener(late);

        M3ObservableList<Integer> observable = new M3ObservableList<>();
        ArrayList<Integer> oracle = new ArrayList<>();
        Random random = new Random(0x5eedd21L);
        int[] notifications = {0};
        observable.addListener((owner, change, index, count, before, after, epoch) -> {
            check(count > 0 && index >= 0 && index <= owner.size() + count,
                    "valid notification range");
            notifications[0]++;
        });
        for (int step = 0; step < 10_000; step++) {
            int size = oracle.size();
            int action = random.nextInt(6);
            switch (action) {
                case 0 -> {
                    int at = random.nextInt(size + 1);
                    Integer value = random.nextBoolean() ? null : random.nextInt(300);
                    oracle.add(at, value);
                    observable.add(at, value);
                }
                case 1 -> {
                    if (size > 0) {
                        int at = random.nextInt(size);
                        check(java.util.Objects.equals(oracle.remove(at), observable.remove(at)),
                                "remove differential");
                    }
                }
                case 2 -> {
                    if (size > 0) {
                        int at = random.nextInt(size);
                        Integer value = random.nextBoolean() ? null : random.nextInt(300);
                        check(java.util.Objects.equals(oracle.set(at, value), observable.set(at, value)),
                                "set differential");
                    }
                }
                case 3 -> {
                    List<Integer> batch = List.of(random.nextInt(30), random.nextInt(30));
                    int at = random.nextInt(size + 1);
                    oracle.addAll(at, batch);
                    observable.addAll(at, batch);
                }
                case 4 -> {
                    if (size > 0) {
                        int from = random.nextInt(size);
                        int to = from + random.nextInt(size - from + 1);
                        oracle.subList(from, to).clear();
                        observable.subList(from, to).clear();
                    }
                }
                default -> {
                    if (step % 173 == 0) {
                        oracle.clear();
                        observable.clear();
                    }
                }
            }
            check(observable.equals(oracle) && oracle.equals(observable),
                    "list differential step " + step);
        }
        int before = notifications[0];
        observable.addAll(observable); // Aliased source snapshots first.
        check(observable.size() == oracle.size() * 2,
                "bulk source aliasing");
        check(notifications[0] == before + (oracle.isEmpty() ? 0 : 1),
                "one notification per bulk mutation");
        try {
            observable.add(-1, 3);
            throw new AssertionError("invalid index allowed");
        } catch (IndexOutOfBoundsException expected) {
            check(notifications[0] == before + (oracle.isEmpty() ? 0 : 1),
                    "failure must not notify");
        }
        System.out.println("M3_OBSERVABLE_LIST_CONTRACT_PASS checks=" + checks
                + " notifications=" + notifications[0]);
    }
}
