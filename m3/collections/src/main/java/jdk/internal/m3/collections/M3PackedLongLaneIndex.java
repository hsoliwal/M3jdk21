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
 * Internal relocation protocol for auxiliary primitive indexes over PackedLongLaneStore.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongLaneIndex.java}
 * blob {@code f2f18a0c5f3074c5d3202d12ec11d06e783ffaf4} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
interface M3PackedLongLaneIndex {
    void ensureRowCapacity(int capacity);
    void validateInsert(int row);
    void insert(int row);
    void validateUpdate(int row, int lane, long newValue);
    void beforeUpdate(int row, int lane);
    void afterUpdate(int row, int lane);
    void remove(int row);
    void relocate(int fromRow, int toRow);
    void clear();
    long payloadBytes();
}
