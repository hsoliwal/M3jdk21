// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.index;

import java.util.Objects;

/** Stable semantic identities. Content versions live in fingerprints, not in node IDs. */
final class M3SemanticIds {
    private M3SemanticIds() {}

    static String node(M3SemanticKind kind, String semanticKey) {
        Objects.requireNonNull(kind, "kind");
        String key = Objects.requireNonNull(semanticKey, "semanticKey");
        return M3SemanticFingerprint.sha256Utf16("M3_NODE_V1|" + kind + "|" + key);
    }
}
