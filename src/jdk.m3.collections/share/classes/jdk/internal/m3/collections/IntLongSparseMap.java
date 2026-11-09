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

/**
 * Sparse primitive int-index to long-value contract.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/IntLongSparseMap.java}
 * blob {@code 64dfa81b8416aa6ad566bb7bb0d64e5f16a699b6} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface IntLongSparseMap {
    boolean put(int index, long value);
    boolean containsIndex(int index);
    long getOrDefault(int index, long defaultValue);
    long getOrThrow(int index);
    boolean remove(int index);
    int indexAt(int ordinal);
    long valueAt(int ordinal);
    int size();
    void clear();
    int[] indicesToArray();
    long[] valuesToArray();
}
