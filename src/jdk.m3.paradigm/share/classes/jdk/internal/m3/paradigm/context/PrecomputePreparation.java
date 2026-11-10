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
 * Local prepared value with its deterministic selection and execution receipts.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/precompute/PrecomputePreparation.java}
 * blob {@code 54d71da74ae6812cd2ce9fad794f4f9e14f03f08} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public record PrecomputePreparation(
        Object value,
        PrecomputePlan plan,
        PrecomputeReceipt receipt) {

    public PrecomputePreparation {
        value = Objects.requireNonNull(value, "value");
        plan = Objects.requireNonNull(plan, "plan");
        receipt = Objects.requireNonNull(receipt, "receipt");
    }
}
