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
 * <p>Lineage: absorbed from Synexia {@code m3-mat-collections/src/main/java/jdk/internal/m3/matcollections/MatStorageKind.java}
 * blob {@code ad646ff0ddc379b3e92ca41ff651cf1254a09c6c} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public enum M3MatStorageKind {
    PRIMITIVE_INT_LIST, PRIMITIVE_LONG_LIST, ID_LIST,
    VECTOR_CONSTANT, VECTOR_PACKED, VECTOR_PLAIN,
    SET_SORTED_ARRAY, SET_RUNS, SET_BITMAP,
    MAP_DIRECT, MAP_SORTED, MAP_HASH,
    MULTIMAP_CSR, GROUP_CSR, ORDER_PERMUTATION, RING_BUFFER,
    BITVECTOR_RANK_SELECT, SET_ROARING, POSTING_CSR, RANGE_SET, RANGE_MAP,
    GRAPH_CSR, GRAPH_BIT_MATRIX
}
