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
package com.m3.collections;

/**
 * Mutable navigable primitive long-to-long map contract: the first-party Synexia donor
 * {@code LongLongSortedMap} contract under the M3 family naming (sibling of {@link M3LongSortedSet}).
 *
 * <p>Keys are ordered by natural signed-long order and every long is a legal key and a legal value,
 * so the navigation calls take a caller-supplied default instead of returning a boxed null;
 * {@code firstKey} and {@code lastKey} throw {@link java.util.NoSuchElementException} on an empty
 * map. This is a target-internal primitive surface; {@link java.util.NavigableMap} remains the
 * public JDK contract owner.</p>
 */
public interface M3LongLongSortedMap {
    boolean put(long key, long value);

    boolean containsKey(long key);

    long getOrDefault(long key, long defaultValue);

    long getOrThrow(long key);

    boolean remove(long key);

    long firstKey();

    long lastKey();

    long floorKey(long key, long defaultValue);

    long ceilingKey(long key, long defaultValue);

    long lowerKey(long key, long defaultValue);

    long higherKey(long key, long defaultValue);

    void forEach(M3LongLongConsumer consumer);

    int size();

    boolean isEmpty();

    void clear();

    long[] keysToArray();

    long[] valuesToArray();
}
