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

/**
 * Explicit tie semantics applied to compare(candidate, current).
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/shapes/packed/BoundaryRelation.java}
 * blob {@code b484fc3dcf22a02c69ff5ff4e6415d29ec056251} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (algorithms shapes.packed batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public enum M3BoundaryRelation {
    GREATER, GREATER_OR_EQUAL, LESS, LESS_OR_EQUAL;

    boolean accepts(int comparison) {
        return switch (this) {
            case GREATER -> comparison > 0;
            case GREATER_OR_EQUAL -> comparison >= 0;
            case LESS -> comparison < 0;
            case LESS_OR_EQUAL -> comparison <= 0;
        };
    }
}
