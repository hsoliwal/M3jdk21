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
 * Primitive long-to-unsigned-byte map contract.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/LongByteMap.java}
 * blob {@code 8bebd85fdcde95168559743d5fb29bc1508fbf9f} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface LongByteMap {
    boolean putUnsigned(long key, int value);
    boolean containsKey(long key);
    int getUnsignedOrDefault(long key, int defaultValue);
    int getUnsignedOrThrow(long key);
    int addClamped(long key, int delta, int initialValue);
    boolean replaceUnsigned(long key, int expectedValue, int newValue);
    boolean remove(long key);
    int filterInPlace(LongLongPredicate predicate);
    void forEach(LongLongConsumer consumer);
    int size();
    boolean isEmpty();
    int capacity();
    void clear();
    void trimToSize();
    long[] keysToArray();
    byte[] valuesToByteArray();
}
