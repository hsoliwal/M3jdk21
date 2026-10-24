/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Focused oracle for standard stream terminals and bounded array materialization. */
public final class M3ObjectStreamTerminalContract {
    private static int assertions;
    private M3ObjectStreamTerminalContract() { }

    public static void main(String[] args) {
        terminals();
        arrayMaterialization();
        compositionAndBoundedPartition();
        System.out.println("M3ObjectStreamTerminalContract PASS assertions=" + assertions);
    }

    private static void terminals() {
        List<Integer> values = Arrays.asList(3, null, 1, 4);
        eq(Optional.of(3), M3ObjectStream.snapshot(values).findFirst());
        Integer any = M3ObjectStream.snapshot(values).findAny().orElseThrow();
        eq(true, values.contains(any) && any != null);
        eq(true, M3ObjectStream.snapshot(values).anyMatch(Objects::isNull));
        eq(false, M3ObjectStream.snapshot(values).allMatch(Objects::nonNull));
        eq(true, M3ObjectStream.snapshot(values).noneMatch(value -> value != null && value < 0));
        eq(false, M3ObjectStream.snapshot(values).noneMatch(value -> Objects.equals(value, 3)));
        eq(Optional.empty(), M3ObjectStream.<Integer>snapshot(List.of()).findFirst());
        eq(Optional.of(-4), M3ObjectStream.snapshot(List.of(3, -4, 5)).min(Integer::compareTo));
        eq(Optional.of(5), M3ObjectStream.snapshot(List.of(3, -4, 5)).max(Integer::compareTo));
        List<Integer> visited = new ArrayList<>();
        M3ObjectStream.snapshot(values).forEachOrdered(visited::add);
        eq(values, visited);
        List<Integer> visitedByForEach = new ArrayList<>();
        M3ObjectStream.snapshot(values).forEach(visitedByForEach::add);
        eq(values.size(), visitedByForEach.size());
        eq(true, visitedByForEach.containsAll(values));
        throwsType(NullPointerException.class,
                () -> M3ObjectStream.snapshot(Arrays.asList((Integer) null, 1)).findFirst());
        throwsType(NullPointerException.class,
                () -> M3ObjectStream.snapshot(Arrays.asList((Integer) null, 1)).findAny());
    }

    private static void arrayMaterialization() {
        List<Integer> values = Arrays.asList(3, null, 1, 4);
        Object[] plain = M3ObjectStream.snapshot(values).toArray(4);
        eq(values, Arrays.asList(plain));
        Integer[] typed = M3ObjectStream.snapshot(values).toArray(Integer[]::new, 4);
        eq(values, Arrays.asList(typed));
        throwsType(IllegalStateException.class, () -> M3ObjectStream.snapshot(values).toArray(3));
        throwsType(IllegalStateException.class,
                () -> M3ObjectStream.snapshot(values).toArray(Integer[]::new, 3));
    }

    private static void compositionAndBoundedPartition() {
        var concatenated = M3ObjectStream.concat(
                M3ObjectStream.snapshot(List.of(1, 2)),
                M3ObjectStream.snapshot(List.of(3, 4)));
        eq(true, concatenated.replayable());
        eq(List.of(1, 2, 3, 4), concatenated.toBoundedList(4));

        var oneShot = M3ObjectStream.once(java.util.stream.Stream.of(1));
        var oneShotConcat = M3ObjectStream.concat(oneShot, M3ObjectStream.snapshot(List.of(2)));
        eq(false, oneShotConcat.replayable());
        eq(List.of(1, 2), oneShotConcat.toBoundedList(2));
        throwsType(IllegalStateException.class, oneShotConcat::count);

        eq(Arrays.asList("a1", "b1", null),
                M3ObjectStream.snapshot(Arrays.asList("a1", "a2", "b1", null, null))
                        .distinctBy(value -> value == null ? null : value.substring(0, 1), 3)
                        .toBoundedList(3));
        throwsType(IllegalStateException.class,
                () -> M3ObjectStream.snapshot(Arrays.asList("a1", "a2", "b1", null))
                        .distinctBy(value -> value == null ? null : value.substring(0, 1), 2)
                        .count());

        var partitions = M3ObjectStream.snapshot(Arrays.asList(1, null, 2, 3, null))
                .partitionBy(value -> value != null && value % 2 != 0, 5);
        eq(Arrays.asList(null, 2, null), partitions.get(Boolean.FALSE));
        eq(List.of(1, 3), partitions.get(Boolean.TRUE));
        eq(List.of(Boolean.FALSE, Boolean.TRUE), new ArrayList<>(partitions.keySet()));
        throwsType(IllegalStateException.class,
                () -> M3ObjectStream.snapshot(List.of(1, 2, 3)).partitionBy(x -> x > 0, 2));
    }

    private static void eq(Object expected, Object actual) {
        assertions++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("expected=" + expected + ", actual=" + actual);
        }
    }

    private static void throwsType(Class<? extends Throwable> expected, Runnable operation) {
        assertions++;
        try {
            operation.run();
        } catch (Throwable failure) {
            if (expected.isInstance(failure)) return;
            throw new AssertionError("expected " + expected.getName() + " but got " + failure, failure);
        }
        throw new AssertionError("expected exception " + expected.getName());
    }
}
