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

/**
 * Insertion-ordered primitive long-to-long map contract.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/LongLongOrderedMap.java}
 * blob {@code 76dced6ac1945a12cc3c7aad1f2091a8c97c23b0} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface M3LongLongOrderedMap {
    boolean put(long key, long value);
    boolean containsKey(long key);
    long getOrDefault(long key, long defaultValue);
    long getOrThrow(long key);
    boolean remove(long key);
    int filterInPlace(M3LongLongPredicate predicate);
    void forEach(LongLongConsumer consumer);
    int size();
    boolean isEmpty();
    void clear();
    long[] keysToArray();
    long[] valuesToArray();
}
