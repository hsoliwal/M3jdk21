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

import java.util.function.Consumer;

/**
 * Single-threaded two-ended sequence; empty end access throws, and null policy is explicit.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/ShapeDeque.java}
 * blob {@code a73616f284482e1f1b773e2aff5944744b966bff} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections subpackages batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public interface M3ShapeDeque<E> extends M3ShapeContainer {
    /** Prepend; throws without mutation when the live bound is reached. */
    void addFirst(E value);
    /** Append; throws without mutation when the live bound is reached. */
    void addLast(E value);
    /** Read the first value, throwing NoSuchElementException when empty. */
    E first();
    /** Read the last value, throwing NoSuchElementException when empty. */
    E last();
    /** Remove the first value, throwing NoSuchElementException when empty. */
    E removeFirst();
    /** Remove the last value, throwing NoSuchElementException when empty. */
    E removeLast();
    /** Read a zero-based position from the first end. */
    E get(int index);
    /** Visit first to last; mutation during traversal is rejected. */
    void forEach(Consumer<? super E> action);
}
