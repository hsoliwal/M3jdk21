// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.semantic;

/**
 * M3JDK21 compatibility facade for Synexia's canonical semantic hasher.
 *
 * <p>No semantic hash logic is owned here. The target preserves this API only so existing M3JDK21
 * tooling callers can migrate without a flag day.</p>
 */
@Deprecated(forRemoval = false)
public final class M3SemanticHasher {
    private M3SemanticHasher() {}

    public static Fingerprint method(String source) {
        return fromCanonical(
                com.synexia.rewrite.semantic.M3SemanticHasher.method(source));
    }

    public static Fingerprint file(String source) {
        return fromCanonical(
                com.synexia.rewrite.semantic.M3SemanticHasher.file(source));
    }

    private static Fingerprint fromCanonical(
            com.synexia.rewrite.semantic.M3SemanticHasher.Fingerprint fingerprint) {
        return new Fingerprint(
                fingerprint.contractHash(),
                fingerprint.logicHash(),
                fingerprint.architectureHash(),
                fingerprint.behavioralHash(),
                fingerprint.wholeHash());
    }

    /** Compatibility value mirroring the canonical Synexia fingerprint record. */
    public record Fingerprint(
            String contractHash,
            String logicHash,
            String architectureHash,
            String behavioralHash,
            String wholeHash) {}
}
