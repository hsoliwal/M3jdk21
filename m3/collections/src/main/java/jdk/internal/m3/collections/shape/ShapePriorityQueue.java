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

import java.util.function.ObjLongConsumer;

/**
 * Least signed-long priority first; equal-priority order is unspecified.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/ShapePriorityQueue.java}
 * blob {@code 89d884da18f04ee698003d0fa0b7e32e5fc7c127} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections subpackages batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public interface ShapePriorityQueue<E> extends ShapeContainer {
    /** Retain a payload and primitive priority; full insertion throws without mutation. */
    void add(E value, long priority);
    /** Least retained priority; throws NoSuchElementException when empty. */
    long firstPriority();
    /** Payload at the least priority; throws NoSuchElementException when empty. */
    E first();
    /** Remove payload at the least priority; throws NoSuchElementException when empty. */
    E removeFirst();
    /** Visit payload and priority in unspecified storage order; mutation is rejected. */
    void forEach(ObjLongConsumer<? super E> action);
}
