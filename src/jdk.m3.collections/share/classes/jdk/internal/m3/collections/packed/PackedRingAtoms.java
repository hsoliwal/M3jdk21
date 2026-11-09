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
package jdk.internal.m3.collections.packed;

/**
 * Shared zero-object scalar arithmetic for primitive circular-array collections.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedRingAtoms.java}
 * blob {@code c68a230f25260cbf5a1dd96ae130755a0ec36698} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
final class PackedRingAtoms {
    private PackedRingAtoms() { }

    static int powerOfTwoIndex(int head, int logical, int capacity) {
        return (head + logical) & (capacity - 1);
    }

    static int powerOfTwoPrevious(int index, int capacity) {
        return (index - 1) & (capacity - 1);
    }

    static int powerOfTwoNext(int index, int capacity) {
        return (index + 1) & (capacity - 1);
    }

    static int index(int head, int logical, int capacity) {
        int physical = head + logical;
        return physical >= capacity ? physical - capacity : physical;
    }

    static int next(int index, int capacity) {
        int candidate = index + 1;
        return candidate == capacity ? 0 : candidate;
    }

    static int normalizeHeadAfterRemoval(int head, int size) {
        return size == 0 ? 0 : head;
    }
}
