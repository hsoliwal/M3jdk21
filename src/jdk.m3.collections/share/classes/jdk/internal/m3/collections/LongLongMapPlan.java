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

import java.util.Objects;

/**
 * Immutable receipt for one deterministic primitive map compilation.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/collections/LongLongMapPlan.java}
 * blob {@code 5e4a3f1c73870e7fc15966da73d4444b7374eb31} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (algorithms.collections v6 batch (no-sprawl): identifiers and package only; bytecode equal to the source modulo names).
 */
public record LongLongMapPlan(
        LongLongMapSpec spec,
        LongLongMapRepresentation representation,
        String ruleId) {

    public LongLongMapPlan {
        Objects.requireNonNull(spec, "spec");
        Objects.requireNonNull(representation, "representation");
        Objects.requireNonNull(ruleId, "ruleId");
        if (ruleId.isBlank()) {
            throw new IllegalArgumentException("ruleId");
        }
    }

    public LongLongMapHandle open() {
        return new LongLongMapHandle(this);
    }
}
