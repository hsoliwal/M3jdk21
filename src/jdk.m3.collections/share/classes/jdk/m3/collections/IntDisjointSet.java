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
package jdk.m3.collections;

/**
 * Union-find over int ordinals {@code [0, elements())}. Ordinals outside that range throw
 * {@link IndexOutOfBoundsException}.
 *
 * <p>API_UNION U4 (2026-11-09): one owner for the concept. The Synexia contract (the common-collections
 * IntDisjointSet) and the members of the former M3 class meet here; {@link #of(int)} returns the array
 * implementation (union by size, path halving).</p>
 */
public interface IntDisjointSet {
    /** Returns {@code size} singleton components; a negative size throws IllegalArgumentException. */
    static IntDisjointSet of(int size) { return new ArrayIntDisjointSet(size); }

    /** Returns the number of ordinals. */
    int elements();

    /** Returns the number of components. */
    int components();

    /** Returns the representative of {@code value}'s component. */
    int find(int value);

    /** Returns the size of {@code value}'s component. */
    int componentSize(int value);

    /** Returns whether {@code left} and {@code right} are in one component. */
    boolean connected(int left, int right);

    /** Joins the components of {@code left} and {@code right}; returns whether they were separate. */
    boolean union(int left, int right);

    /** Restores every ordinal to a singleton component. */
    void reset();

    /** Returns the payload size in bytes. */
    long payloadBytes();

    /** Returns the number of ordinals (M3 name of {@link #elements()}). */
    default int size() { return elements(); }
}
