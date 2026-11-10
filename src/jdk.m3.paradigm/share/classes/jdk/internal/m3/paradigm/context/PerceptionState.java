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

/**
 * Monotonic epistemic state of one perception.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextPerceptionState.java}
 * blob {@code 12a8432c3ca1a0348f251dff5aa86d4948c71a11} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public enum PerceptionState {
    OBSERVED,
    NORMALIZED,
    CORRELATED,
    INTERPRETED,
    PROPOSED,
    VERIFIED,
    ADMITTED,
    REJECTED,
    EXPIRED;

    /** Terminal perceptions cannot advance further. */
    public boolean terminal() {
        return this == ADMITTED || this == REJECTED || this == EXPIRED;
    }

    /** Whether this state can move to the requested state without regression. */
    public boolean canAdvanceTo(PerceptionState next) {
        if (next == null) {
            return false;
        }
        if (next == this) {
            return !terminal();
        }
        return !terminal() && next.ordinal() > ordinal();
    }
}
