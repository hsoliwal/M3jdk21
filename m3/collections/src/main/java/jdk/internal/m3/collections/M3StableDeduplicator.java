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
package jdk.internal.m3.collections;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Deterministic first-occurrence deduplication with stable input ordering.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/collections/StableDeduplicator.java}
 * blob {@code f65d7d36c0f0b99cd74d47a6ec2c451fcd0161df} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (algorithms collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3StableDeduplicator {
    private M3StableDeduplicator() {}

    public static <T> List<T> distinct(List<T> values) {
        return distinctBy(values, Function.identity());
    }

    public static <T, K> List<T> distinctBy(
            List<T> values,
            Function<? super T, ? extends K> keyFunction) {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(keyFunction, "keyFunction");
        LinkedHashMap<K, T> firstByKey = new LinkedHashMap<>();
        for (T value : values) {
            T checked = Objects.requireNonNull(value, "value");
            K key = Objects.requireNonNull(keyFunction.apply(checked), "deduplication key");
            firstByKey.putIfAbsent(key, checked);
        }
        return List.copyOf(firstByKey.values());
    }

    public static <T, K> List<K> duplicateKeys(
            List<T> values,
            Function<? super T, ? extends K> keyFunction) {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(keyFunction, "keyFunction");
        java.util.HashSet<K> seen = new java.util.HashSet<>();
        java.util.LinkedHashSet<K> duplicates = new java.util.LinkedHashSet<>();
        for (T value : values) {
            T checked = Objects.requireNonNull(value, "value");
            K key = Objects.requireNonNull(keyFunction.apply(checked), "deduplication key");
            if (!seen.add(key)) {
                duplicates.add(key);
            }
        }
        return List.copyOf(duplicates);
    }
}
