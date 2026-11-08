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

import jdk.m3.collections.LongLongConsumer;
import jdk.m3.collections.LongLongPredicate;

/**
 * Primitive long-to-many-long multimap contract.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/LongLongMultiMap.java}
 * blob {@code 994ce8f081e6ef1fc91dab2894b072652005e630} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface LongLongMultiMap {
    void add(long key, long value);
    boolean containsKey(long key);
    boolean containsEntry(long key, long value);
    int valueCount(long key);
    long[] values(long key);
    boolean remove(long key, long value);
    int removeAll(long key);
    int filterInPlace(LongLongPredicate predicate);
    void forEach(LongLongConsumer consumer);
    int distinctKeyCount();
    int size();
    boolean isEmpty();
    void clear();
}
