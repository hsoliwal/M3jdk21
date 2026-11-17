// SPDX-License-Identifier: Apache-2.0
package jdk.m3.db;

import java.util.Objects;

/** One stable semantic identity and its current leaf or recomposed fingerprint. */
public record SemanticNode(
        String nodeId,
        SemanticKind kind,
        String semanticKey,
        String sourcePath,
        String symbol,
        String patternRole,
        SemanticFingerprint fingerprint) {

    public SemanticNode {
        nodeId = requireSha(nodeId, "nodeId");
        kind = Objects.requireNonNull(kind, "kind");
        semanticKey = token(semanticKey, "semanticKey");
        sourcePath = Objects.requireNonNull(sourcePath, "sourcePath");
        symbol = token(symbol, "symbol");
        patternRole = token(patternRole, "patternRole");
        fingerprint = Objects.requireNonNull(fingerprint, "fingerprint");
        if (!nodeId.equals(SemanticIndex.nodeId(kind, semanticKey))) {
            throw new IllegalArgumentException("nodeId does not match semantic identity");
        }
    }

    public SemanticNode withFingerprint(SemanticFingerprint replacement) {
        return new SemanticNode(
                nodeId,
                kind,
                semanticKey,
                sourcePath,
                symbol,
                patternRole,
                replacement);
    }

    public SemanticNode(
            String nodeId,
            SemanticKind kind,
            String semanticKey,
            String sourcePath,
            String symbol,
            SemanticFingerprint fingerprint) {
        this(
                nodeId,
                kind,
                semanticKey,
                sourcePath,
                symbol,
                "M3:" + Objects.requireNonNull(kind, "kind").name(),
                fingerprint);
    }

    private static String requireSha(String value, String field) {
        String checked = Objects.requireNonNull(value, field);
        if (!checked.matches("[0-9a-f]{64}")) throw new IllegalArgumentException(field);
        return checked;
    }

    private static String token(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
        if (checked.isEmpty() || checked.indexOf('\0') >= 0) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }
}
