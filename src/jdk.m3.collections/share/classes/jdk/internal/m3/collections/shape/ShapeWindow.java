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
 * Latest-N values in arrival order, with explicit bounded overwrite behavior.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/ShapeWindow.java}
 * blob {@code 2b16c00741e2f50b19df427a58e1f2ecc33e5c48} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections subpackages batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public interface ShapeWindow<E> extends ShapeContainer {
    /** Exact logical capacity, independent of allocated storage. */
    int capacity();
    /** Append and evict oldest if full; return whether eviction occurred. Zero capacity throws. */
    boolean append(E value);
    /** Read a zero-based position, oldest first. */
    E get(int index);
    /** Visit oldest to newest; mutation during traversal is rejected. */
    void forEach(Consumer<? super E> action);
}
