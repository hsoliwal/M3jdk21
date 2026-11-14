/* *****************************************************************************
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
import java.util.List;
import java.util.Objects;

/**
 * Deterministic immutable delta between two key-addressed snapshots.
 *
 * <p>The three key lists are normalized into lexical order and copied at construction time. The
 * type deliberately contains no timestamps, clocks, logging, persistence, retry semantics or
 * business authority. It describes content change only; callers retain source identity, ordering,
 * freshness and mutation policy.</p>
 *
 * <p>Keys are strings because the shared snapshot/index surfaces already normalize external
 * identities to stable textual keys. An application that uses another native key type must perform
 * the canonical key projection before diffing.</p>
 *
 * @param insertedKeys keys present only in the current snapshot
 * @param updatedKeys keys present in both snapshots whose projected value changed
 * @param removedKeys keys present only in the previous snapshot
 * @since 1.0.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/io/SnapshotDelta.java}
 * blob {@code 7df4114f0f84739418340ec2e5d8960f141d75fd} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common non-language batch b28 (canonical, utils, semantic, convergence, io, extracted, validation, relationbits) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public record SnapshotDelta(
        List<String> insertedKeys,
        List<String> updatedKeys,
        List<String> removedKeys) {

    /**
     * Creates a normalized deterministic delta.
     *
     * @throws NullPointerException if a list or any key is null
     * @throws IllegalArgumentException if a key appears in more than one change class
     */
    public SnapshotDelta {
        insertedKeys = normalize(insertedKeys, "insertedKeys");
        updatedKeys = normalize(updatedKeys, "updatedKeys");
        removedKeys = normalize(removedKeys, "removedKeys");
        rejectOverlap(insertedKeys, updatedKeys, "insertedKeys", "updatedKeys");
        rejectOverlap(insertedKeys, removedKeys, "insertedKeys", "removedKeys");
        rejectOverlap(updatedKeys, removedKeys, "updatedKeys", "removedKeys");
    }

    /** Returns an empty delta. */
    public static SnapshotDelta empty() {
        return new SnapshotDelta(List.of(), List.of(), List.of());
    }

    /** Returns the inserted key count. */
    public int inserted() {
        return insertedKeys.size();
    }

    /** Returns the updated key count. */
    public int updated() {
        return updatedKeys.size();
    }

    /** Returns the removed key count. */
    public int removed() {
        return removedKeys.size();
    }

    /** Returns the total number of changed keys. */
    public int totalChanges() {
        return Math.addExact(Math.addExact(inserted(), updated()), removed());
    }

    /** Returns {@code true} when at least one key changed. */
    public boolean hasChanges() {
        return totalChanges() != 0;
    }

    private static List<String> normalize(List<String> keys, String name) {
        Objects.requireNonNull(keys, name);
        ArrayList<String> copy = new ArrayList<>(keys.size());
        for (String key : keys) {
            copy.add(Objects.requireNonNull(key, name + " contains null"));
        }
        copy.sort(String::compareTo);
        for (int index = 1; index < copy.size(); index++) {
            if (copy.get(index - 1).equals(copy.get(index))) {
                throw new IllegalArgumentException(name + " contains duplicate key: " + copy.get(index));
            }
        }
        return List.copyOf(copy);
    }

    private static void rejectOverlap(
            List<String> left,
            List<String> right,
            String leftName,
            String rightName) {
        int leftIndex = 0;
        int rightIndex = 0;
        while (leftIndex < left.size() && rightIndex < right.size()) {
            int order = left.get(leftIndex).compareTo(right.get(rightIndex));
            if (order == 0) {
                throw new IllegalArgumentException(
                        "key appears in both " + leftName + " and " + rightName + ": " + left.get(leftIndex));
            }
            if (order < 0) {
                leftIndex++;
            } else {
                rightIndex++;
            }
        }
    }
}
