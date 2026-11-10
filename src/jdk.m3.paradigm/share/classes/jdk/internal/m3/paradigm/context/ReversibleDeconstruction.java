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
 * Append-only critique revision.
 *
 * <p>A deconstructor is not immune from deconstruction. Revisions and
 * retractions preserve the prior critique as history rather than silently
 * replacing it.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/locality/ReversibleDeconstruction.java}
 * blob {@code d8cc3751e638935a7b1a421d23bb87ad1feeb387} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class ReversibleDeconstruction {
    public enum Action {
        RETAIN,
        REVISE,
        RETRACT
    }

    public record Receipt(
            String claimId,
            String originalText,
            String replacementText,
            String reason,
            Action action,
            boolean priorPreserved,
            boolean historyRewritten,
            boolean selfCritiqueAllowed,
            boolean semanticVerdict) {
    }

    private ReversibleDeconstruction() {
    }

    public static Receipt revise(
            String claimId, String originalText, String replacementText, String reason) {
        return receipt(claimId, originalText, replacementText, reason, Action.REVISE);
    }

    public static Receipt retract(String claimId, String originalText, String reason) {
        return receipt(claimId, originalText, "", reason, Action.RETRACT);
    }

    public static Receipt retain(String claimId, String originalText, String reason) {
        return receipt(claimId, originalText, originalText, reason, Action.RETAIN);
    }

    private static Receipt receipt(
            String claimId,
            String originalText,
            String replacementText,
            String reason,
            Action action) {
        String id = require(claimId, "claimId");
        String original = require(originalText, "originalText");
        String replacement = replacementText == null ? "" : replacementText.trim();
        String why = reason == null ? "" : reason.trim();
        if (action == Action.REVISE && replacement.isBlank()) {
            throw new IllegalArgumentException("replacementText");
        }
        return new Receipt(
                id,
                original,
                replacement,
                why,
                action,
                true,
                false,
                true,
                false);
    }

    private static String require(String value, String field) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field);
        }
        return normalized;
    }
}
