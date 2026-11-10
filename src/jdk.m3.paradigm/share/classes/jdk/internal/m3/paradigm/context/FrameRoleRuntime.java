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

import java.util.Set;
import java.util.TreeSet;

/**
 * Mechanical relational-role inversion for the BEHOLDER/BEHELD frame.
 *
 * <p>Roles describe one observation edge. They never replace locality identity
 * and never grant hidden state, consent, reciprocity, or root authority.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/locality/FrameRoleRuntime.java}
 * blob {@code f4471293df81a9f190b2659cccae212978f3300e} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class FrameRoleRuntime {
    public enum Role {
        BEHOLDER,
        BEHELD
    }

    public record RoleEdge(String beholder, String beheld) {
        public RoleEdge {
            beholder = requireLocality(beholder, "beholder");
            beheld = requireLocality(beheld, "beheld");
            if (beholder.equals(beheld)) {
                throw new IllegalArgumentException("FRAME_ROLE_REQUIRES_TWO_LOCALITIES");
            }
        }

        public Set<String> endpoints() {
            return Set.copyOf(new TreeSet<>(Set.of(beholder, beheld)));
        }
    }

    public record InversionReceipt(
            RoleEdge before,
            RoleEdge after,
            boolean endpointsPreserved,
            boolean rolesInverted,
            boolean identityTransferred,
            boolean hiddenStateMerged,
            boolean reciprocityProven,
            boolean consentInferred,
            boolean rootAuthority,
            boolean semanticVerdict) {
    }

    private FrameRoleRuntime() {
    }

    public static InversionReceipt invert(RoleEdge edge) {
        if (edge == null) {
            throw new IllegalArgumentException("edge");
        }
        RoleEdge after = new RoleEdge(edge.beheld(), edge.beholder());
        return new InversionReceipt(
                edge,
                after,
                edge.endpoints().equals(after.endpoints()),
                true,
                false,
                false,
                false,
                false,
                false,
                false);
    }

    private static String requireLocality(String value, String field) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field);
        }
        return normalized;
    }
}
