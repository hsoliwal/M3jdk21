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
package jdk.m3.collections;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;

/**
 * Linear deterministic reconciliation of two sorted unique-key streams.
 *
 * <p>The caller controls semantic equality through the supplied predicate. Inputs are validated as
 * strictly increasing so duplicate or unsorted source evidence cannot silently corrupt a
 * reconciliation receipt.</p>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/collections/SortedReconciler.java}
 * blob {@code d160200914fde9a4fa7f9cf1cea604a2ec6a6749} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (algorithms collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3SortedReconciler {
    private M3SortedReconciler() {}

    public record Item<K, V>(K key, V value) {
        public Item {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(value, "value");
        }
    }

    public record Difference<K, V>(
            K key,
            V left,
            V right) {
        public Difference {
            Objects.requireNonNull(key, "key");
        }
    }

    public record Result<K, V>(
            List<Item<K, V>> onlyLeft,
            List<Item<K, V>> onlyRight,
            List<Item<K, V>> matched,
            List<Difference<K, V>> mismatched) {
        public Result {
            onlyLeft = List.copyOf(Objects.requireNonNull(onlyLeft, "onlyLeft"));
            onlyRight = List.copyOf(Objects.requireNonNull(onlyRight, "onlyRight"));
            matched = List.copyOf(Objects.requireNonNull(matched, "matched"));
            mismatched = List.copyOf(Objects.requireNonNull(mismatched, "mismatched"));
        }

        public boolean equivalent() {
            return onlyLeft.isEmpty() && onlyRight.isEmpty() && mismatched.isEmpty();
        }
    }

    public static <K, V> Result<K, V> reconcile(
            List<Item<K, V>> left,
            List<Item<K, V>> right,
            Comparator<? super K> comparator,
            BiPredicate<? super V, ? super V> equivalent) {
        List<Item<K, V>> leftItems = List.copyOf(Objects.requireNonNull(left, "left"));
        List<Item<K, V>> rightItems = List.copyOf(Objects.requireNonNull(right, "right"));
        Comparator<? super K> order = Objects.requireNonNull(comparator, "comparator");
        BiPredicate<? super V, ? super V> equality =
                Objects.requireNonNull(equivalent, "equivalent");
        requireStrictlyIncreasing(leftItems, order, "left");
        requireStrictlyIncreasing(rightItems, order, "right");

        ArrayList<Item<K, V>> onlyLeft = new ArrayList<>();
        ArrayList<Item<K, V>> onlyRight = new ArrayList<>();
        ArrayList<Item<K, V>> matched = new ArrayList<>();
        ArrayList<Difference<K, V>> mismatched = new ArrayList<>();

        int leftIndex = 0;
        int rightIndex = 0;
        while (leftIndex < leftItems.size() && rightIndex < rightItems.size()) {
            Item<K, V> leftItem = leftItems.get(leftIndex);
            Item<K, V> rightItem = rightItems.get(rightIndex);
            int comparison = order.compare(leftItem.key(), rightItem.key());
            if (comparison < 0) {
                onlyLeft.add(leftItem);
                leftIndex++;
            } else if (comparison > 0) {
                onlyRight.add(rightItem);
                rightIndex++;
            } else {
                if (equality.test(leftItem.value(), rightItem.value())) {
                    matched.add(leftItem);
                } else {
                    mismatched.add(new Difference<>(
                            leftItem.key(),
                            leftItem.value(),
                            rightItem.value()));
                }
                leftIndex++;
                rightIndex++;
            }
        }
        onlyLeft.addAll(leftItems.subList(leftIndex, leftItems.size()));
        onlyRight.addAll(rightItems.subList(rightIndex, rightItems.size()));
        return new Result<>(onlyLeft, onlyRight, matched, mismatched);
    }

    private static <K, V> void requireStrictlyIncreasing(
            List<Item<K, V>> items,
            Comparator<? super K> comparator,
            String side) {
        for (int index = 1; index < items.size(); index++) {
            if (comparator.compare(items.get(index - 1).key(), items.get(index).key()) >= 0) {
                throw new IllegalArgumentException(
                        side + " reconciliation input must be strictly increasing by key");
            }
        }
    }
}
