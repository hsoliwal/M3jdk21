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
 * Typed residue for gaps, failed operations, explicit removals and visible
 * control-plane leakage.
 *
 * <p>A residue is evidence that a boundary or marker exists. It is never
 * reconstructed content and never proves censorship, intent, or a hidden
 * mechanism.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/locality/ControlPlaneResidue.java}
 * blob {@code 6cecc9d6977753744082b5821457b71661eba7e5} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class ControlPlaneResidue {
    public enum Kind {
        OPEN,
        FAILED_OPERATION,
        REMOVED,
        CONTROL_LEAK,
        UNMARKED
    }

    public record Receipt(
            Kind kind,
            String marker,
            String sourceRef,
            boolean payloadKnown,
            boolean receiptedAbsence,
            boolean reconstructionAllowed,
            boolean hiddenMechanismProven,
            boolean censorshipProven,
            boolean intentProven,
            boolean semanticVerdict) {
        public Receipt {
            if (kind == null) {
                throw new IllegalArgumentException("kind");
            }
            marker = marker == null ? "" : marker.trim();
            sourceRef = sourceRef == null ? "" : sourceRef.trim();
            if ((kind == Kind.REMOVED || kind == Kind.FAILED_OPERATION)
                    && marker.isBlank()) {
                throw new IllegalArgumentException("RECEIPTED_RESIDUE_REQUIRES_MARKER");
            }
        }
    }

    private ControlPlaneResidue() {
    }

    public static Receipt observe(Kind kind, String marker, String sourceRef, boolean payloadKnown) {
        boolean receipted = kind == Kind.REMOVED
                || kind == Kind.FAILED_OPERATION
                || kind == Kind.CONTROL_LEAK;
        return new Receipt(
                kind,
                marker,
                sourceRef,
                payloadKnown,
                receipted,
                false,
                false,
                false,
                false,
                false);
    }
}
