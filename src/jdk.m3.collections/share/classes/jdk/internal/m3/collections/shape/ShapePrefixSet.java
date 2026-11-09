/* *****************************************************************************
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

import java.util.function.BiConsumer;

/**
 * Canonical absolute path antichain: retained ancestors subsume descendants.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/ShapePrefixSet.java}
 * blob {@code c3f4b6e0fa375b76dacac297e88fc9df6cb98c29} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections subpackages batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public interface ShapePrefixSet<E> extends ShapeContainer {
    /** Retain an uncovered path and remove descendants; first payload wins duplicate paths. */
    boolean add(String path, E value);
    /** Whether a retained ancestor (including the exact path) covers this path. */
    boolean covers(String path);
    /** Whether this exact path is retained, rather than merely covered. */
    boolean containsPath(String path);
    /** Remove an exact retained path; previously subsumed children are not resurrected. */
    boolean removePath(String path);
    /** Visit retained path/payload pairs in the admitted provider order. Mutation is rejected. */
    void forEach(BiConsumer<? super String, ? super E> action);
}
