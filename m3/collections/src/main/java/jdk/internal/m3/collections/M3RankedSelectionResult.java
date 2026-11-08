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

import java.util.List;
import java.util.Objects;

/**
 * Completed sorted values or explicit cancellation; cancellation never advertises a partial ranking.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/RankedSelectionResult.java}
 * blob {@code 278db121411e83229fca4db91fe331abf1bb463f} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 2 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public record M3RankedSelectionResult<E>(Status status, int visited, List<E> values, M3CollectionSelection selection) {
    /** Completion state after all visited-occurrence cleanup attempts. */
    public enum Status { COMPLETE, CANCELLED }
    public M3RankedSelectionResult {
        Objects.requireNonNull(status, "status"); Objects.requireNonNull(selection, "selection");
        values = List.copyOf(values);
        if (visited < 0 || values.size() > visited || (status == Status.CANCELLED && !values.isEmpty())) {
            throw new IllegalArgumentException("invalid selection outcome");
        }
    }
}
