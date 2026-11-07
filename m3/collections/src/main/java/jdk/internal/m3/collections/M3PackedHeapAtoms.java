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

/**
 * Shared zero-object integer geometry for binary heaps.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedHeapAtoms.java}
 * blob {@code d704ba21e08d0489d5db399c5033cb04b4adab6d} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
final class M3PackedHeapAtoms {
    private M3PackedHeapAtoms() { }

    static int parent(int index) {
        return (index - 1) >>> 1;
    }

    static int left(int index) {
        return (index << 1) + 1;
    }

    static int right(int index) {
        return (index << 1) + 2;
    }

    static int firstLeaf(int size) {
        return size >>> 1;
    }

    static int lastParent(int size) {
        return (size >>> 1) - 1;
    }

    static boolean hasChildren(int index, int size) {
        return index < firstLeaf(size);
    }
}
