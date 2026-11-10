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
package jdk.internal.m3.collections;

/**
 * Physical long-set representations available to the collection compiler.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/collections/LongSetRepresentation.java}
 * blob {@code a1e0ec12b701b19a81bf04fe39d010b20c6fd4a7} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (algorithms collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public enum LongSetRepresentation {
    BITMAP(false),
    DENSE_SORTED(true),
    SPARSE_SORTED(true),
    BATCH_SORTED(true),
    PAGED_SORTED(true),
    LINEAR_HASH(false),
    ROBIN_HOOD_HASH(false),
    SWISS_HASH(false);

    private final boolean navigable;

    LongSetRepresentation(boolean navigable) {
        this.navigable = navigable;
    }

    public boolean navigable() {
        return navigable;
    }
}
