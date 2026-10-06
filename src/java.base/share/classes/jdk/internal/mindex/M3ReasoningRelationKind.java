// SPDX-License-Identifier: Apache-2.0
package jdk.internal.mindex;

/**
 * Fixed relation dimensions for primitive M3 reasoning precompute.
 *
 * <p>The ordinal layout follows the Synexia MIndex reasoning donor so a frozen
 * {@code [node][relation-kind]} directory can be mapped mechanically while retaining M3JDK naming.
 * Adding a relation does not add fields to {@code java.lang.M3String}.</p>
 */
public enum M3ReasoningRelationKind {
    ATTACK,
    SUPPORT,
    DEFEAT,
    IMPLIES,
    CONTRADICTS,
    EQUIVALENT,
    CAUSES,
    PREFERS,
    EVIDENCE_FOR,
    EVIDENCE_AGAINST
}
