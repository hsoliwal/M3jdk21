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

/**
 * Native mutable shape; intentionally independent of donor container APIs.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/ShapeContainer.java}
 * blob {@code 6bf0b533fa55321ce23db22292ed18b0548f961e} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections subpackages batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public interface ShapeContainer {
    /** Number of live elements or mappings. */
    int size();
    /** Remove all elements and release references; backing capacity may remain. */
    void clear();
    /** Structural storage counters, excluding headers, directories and caller objects. */
    ShapeStorage storage();
    /** Whether no live elements remain. */
    default boolean isEmpty() { return size() == 0; }
}
