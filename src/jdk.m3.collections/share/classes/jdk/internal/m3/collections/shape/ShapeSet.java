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

import java.util.function.Consumer;

/**
 * Native unique-key shape with explicit value or reference equality.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/ShapeSet.java}
 * blob {@code 3da09e165b2c3854c0838e039bcaf16e9b53dde5} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections subpackages batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public interface ShapeSet<E> extends ShapeContainer {
    /** True if the key was newly inserted. */
    boolean add(E value);
    /** Presence under the configured equivalence relation. */
    boolean contains(E value);
    /** True if a live key was removed. */
    boolean remove(E value);
    /** Traverse without wrappers; callback mutation of this set is rejected. */
    void forEach(Consumer<? super E> action);
}
