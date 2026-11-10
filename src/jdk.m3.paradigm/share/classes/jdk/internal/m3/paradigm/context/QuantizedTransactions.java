/* ******************************************************************************
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
package jdk.internal.m3.paradigm.context;

import java.util.Objects;

/**
 * Single-attempt publication through the existing cell. The caller owns the cell and retry policy.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/quantized/QuantizedTransactions.java}
 * blob {@code 79bf46c8cfcd5ca89c2dd536289c8808c669c163} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class QuantizedTransactions {
    public enum Outcome { APPLIED, ALREADY_APPLIED, STALE, INACTIVE }
    private QuantizedTransactions() { }

    /**
     * Requires the exact current token. No-op idempotence does not advance revision.
     * Invalid preimages/schemas throw before mutation. A CAS race returns STALE.
     */
    public static Outcome apply(CompactContextCell<QuantizedContextValues> cell,
            CompactContextCell.Snapshot<QuantizedContextValues> expected,
            QuantizedSchema schema, QuantizedPatch patch) {
        Objects.requireNonNull(cell, "cell");
        Objects.requireNonNull(expected, "expected");
        Objects.requireNonNull(schema, "schema");
        Objects.requireNonNull(patch, "patch");
        if (cell.snapshot() != expected) return Outcome.STALE;
        if (expected.phase() != CompactContextCell.Phase.ACTIVE) return Outcome.INACTIVE;
        QuantizedContextValues before = Objects.requireNonNull(expected.value(), "initialized payload required");
        QuantizedContextValues after = patch.apply(schema, before);
        if (after == before) return Outcome.ALREADY_APPLIED;
        return cell.trySet(expected, after) ? Outcome.APPLIED : Outcome.STALE;
    }
}
