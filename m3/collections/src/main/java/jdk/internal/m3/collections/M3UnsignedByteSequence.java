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

import java.util.function.IntPredicate;

/**
 * Mutable unsigned 8-bit sequence backed by primitive bytes.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/UnsignedByteSequence.java}
 * blob {@code 20ea3aae4d03db38abdf2e445f94b55432d60718} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface M3UnsignedByteSequence {
    void add(int value);
    boolean addIf(int value, IntPredicate predicate);
    int getUnsigned(int index);
    int setUnsigned(int index, int value);
    int addClamped(int index, int delta);
    int removeAt(int index);
    boolean containsUnsigned(int value);
    void sortUnsigned();
    int filterInPlace(IntPredicate predicate);
    int size();
    boolean isEmpty();
    int capacity();
    void clear();
    void trimToSize();
    byte[] toByteArray();
    int[] toUnsignedIntArray();
}
