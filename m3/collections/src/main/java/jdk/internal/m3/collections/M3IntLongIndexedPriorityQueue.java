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
 * Indexed primitive priority queue over int IDs and long priorities.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/IntLongIndexedPriorityQueue.java}
 * blob {@code 66815181afcf9d8dfb4c13c4c67f66346c1cb408} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface M3IntLongIndexedPriorityQueue {
    int size();
    int capacity();
    boolean isEmpty();
    boolean contains(int id);
    long priorityOf(int id);
    void put(int id, long priority);
    void putAll(int[] ids, long[] priorities);
    int peekId();
    int pollId();
    boolean remove(int id);
    void clear();
}
