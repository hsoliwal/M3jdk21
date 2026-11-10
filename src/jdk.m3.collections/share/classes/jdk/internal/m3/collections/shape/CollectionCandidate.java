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

import java.util.Objects;
import jdk.internal.m3.collections.CollectionAspectMask;

/**
 * Immutable declared provider capabilities. A descriptor digest is not code attestation.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/CollectionCandidate.java}
 * blob {@code b5ac3b49202bc270c1454c8d42a52572851d3977} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public record CollectionCandidate(String id, CollectionRequirements.Kind kind,
                                  CollectionRequirements.Order order,
                                  CollectionRequirements.Concurrency concurrency,
                                  boolean valueEquality, boolean identityEquality,
                                  boolean nullable, int maximumSize, CandidateProfile profile) {
    /** Primitive semantic capability word for exact admission comparison. */
    public int aspects() {
        return CollectionAspectMask.shapeCandidate(this);
    }

    public CollectionCandidate {
        Objects.requireNonNull(id, "id"); Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(order, "order"); Objects.requireNonNull(concurrency, "concurrency");
        Objects.requireNonNull(profile, "profile");
        if (!id.matches("[a-z0-9][a-z0-9._-]{0,95}")) { throw new IllegalArgumentException("stable id"); }
        if (maximumSize < 0 || maximumSize > (1 << 28)) { throw new IllegalArgumentException("maximumSize"); }
    }

    public int missingAspects(CollectionRequirements required) {
        Objects.requireNonNull(required, "required");
        return CollectionAspectMask.missing(aspects(), required.aspects());
    }
}
