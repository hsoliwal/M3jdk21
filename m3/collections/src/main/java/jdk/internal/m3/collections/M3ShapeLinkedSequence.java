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
 * Stable, generation-checked, collection-local handles; zero is the absent link.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/ShapeLinkedSequence.java}
 * blob {@code 167157d947cac1332ea4c0b2ade71f5819464692} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections subpackages batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public interface M3ShapeLinkedSequence<E> extends M3ShapeContainer {
    /** First handle or zero if empty. */
    long firstHandle();
    /** Last handle or zero if empty. */
    long lastHandle();
    /** Add at the front and return its handle. */
    long addFirst(E value);
    /** Add at the end and return its handle. */
    long addLast(E value);
    /** Insert after a live handle. */
    long insertAfter(long handle, E value);
    /** Insert before a live handle. */
    long insertBefore(long handle, E value);
    /** Read the value at a live handle. */
    E get(long handle);
    /** Replace the value and return its previous value. */
    E set(long handle, E value);
    /** Following handle, or zero. */
    long next(long handle);
    /** Previous handle, or zero. */
    long previous(long handle);
    /** Remove a live handle, invalidating it before reuse. */
    E remove(long handle);
    /** Whether this numeric handle currently denotes a live slot in this collection. */
    boolean isLive(long handle);
    /** Traverse in link order; callback mutation is rejected. */
    void forEach(Consumer<? super E> action);
}
