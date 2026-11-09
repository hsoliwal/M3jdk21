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

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;

/**
 * Dependency-light deterministic diff for key-addressed snapshots.
 *
 * <p>The algorithm classifies keys as inserted, updated or removed and delegates canonical lexical
 * ordering to {@link SnapshotDelta}. It has no wall-clock dependency and does not infer source
 * ordering, freshness, authorization or mutation success.</p>
 *
 * <p>Null values are supported when the caller's map supports them. Presence is determined with
 * {@link Map#containsKey(Object)}, so an explicit {@code null} value is distinct from an absent
 * key.</p>
 *
 * @since 1.0.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/io/SnapshotDiff.java}
 * blob {@code 8161e1b7cb835165e5cf7e5e3acccef95732f7a7} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common non-language batch b28 (canonical, utils, semantic, convergence, io, extracted, validation, relationbits) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class SnapshotDiff {

    private SnapshotDiff() {
        throw new AssertionError("No instances");
    }

    /**
     * Computes a deterministic delta using {@link Objects#equals(Object, Object)} for values.
     *
     * @param previous previous key/value snapshot
     * @param current current key/value snapshot
     * @param <V> snapshot value type
     * @return deterministic delta
     */
    public static <V> SnapshotDelta diff(
            Map<String, ? extends V> previous,
            Map<String, ? extends V> current) {
        return diff(previous, current, Objects::equals);
    }

    /**
     * Computes a deterministic delta using an explicit value equivalence relation.
     *
     * <p>The predicate is invoked only for keys that are present in both snapshots. It must be
     * deterministic for the same pair of values. Callers may use this overload to compare stable
     * projections or content hashes while keeping source-specific semantics outside this utility.</p>
     *
     * @param previous previous key/value snapshot
     * @param current current key/value snapshot
     * @param equivalent deterministic value equivalence predicate
     * @param <V> snapshot value type
     * @return deterministic delta
     */
    public static <V> SnapshotDelta diff(
            Map<String, ? extends V> previous,
            Map<String, ? extends V> current,
            BiPredicate<? super V, ? super V> equivalent) {
        Objects.requireNonNull(previous, "previous");
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(equivalent, "equivalent");
        requireNonNullKeys(previous, "previous");
        requireNonNullKeys(current, "current");

        ArrayList<String> inserted = new ArrayList<>();
        ArrayList<String> updated = new ArrayList<>();
        ArrayList<String> removed = new ArrayList<>();

        for (Map.Entry<String, ? extends V> entry : current.entrySet()) {
            String key = entry.getKey();
            if (!previous.containsKey(key)) {
                inserted.add(key);
            } else if (!equivalent.test(previous.get(key), entry.getValue())) {
                updated.add(key);
            }
        }

        for (String key : previous.keySet()) {
            if (!current.containsKey(key)) {
                removed.add(key);
            }
        }

        return new SnapshotDelta(inserted, updated, removed);
    }

    private static void requireNonNullKeys(Map<String, ?> snapshot, String name) {
        for (String key : snapshot.keySet()) {
            if (key == null) {
                throw new NullPointerException(name + " contains null key");
            }
        }
    }
}
