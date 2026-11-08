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
package jdk.internal.m3.collections.shape;

import java.util.function.Consumer;

/**
 * Indexed ordered sequence; positions move on insertion/removal and are not stable handles.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/ShapeSequence.java}
 * blob {@code feeda3a86c3bd9f4e28ecc79e643fd98d4c84bbe} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections subpackages batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public interface ShapeSequence<E> extends ShapeContainer {
    /** Append an element. */
    void add(E value);
    /** Insert before index, accepting size as the append position. */
    void insert(int index, E value);
    /** Read a live position. */
    E get(int index);
    /** Replace and return the previous value. */
    E set(int index, E value);
    /** Remove and return the previous value; later positions shift left. */
    E removeAt(int index);
    /** Traverse in position order; callback mutation is rejected. */
    void forEach(Consumer<? super E> action);
}
