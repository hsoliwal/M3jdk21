// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.index;

import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

/** Canonical semantic nodes emitted by the M3 Java semantic-index recipe. */
public final class M3SemanticNodeTable extends DataTable<M3SemanticNodeTable.Row> {
    public M3SemanticNodeTable(Recipe recipe) {
        super(
                recipe,
                "M3 semantic nodes",
                "Atoms and their normalized structural hierarchy with exact, structural, logic and SimHash fingerprints.");
    }

    public static final class Row {
        @Column(displayName = "Node ID", description = "Stable semantic identity independent of current content.")
        private final String nodeId;
        @Column(displayName = "Kind", description = "M3 semantic hierarchy kind.")
        private final String kind;
        @Column(displayName = "Semantic key", description = "Stable repository-relative semantic key.")
        private final String semanticKey;
        @Column(displayName = "Source path", description = "Source path when the node has a direct source owner.")
        private final String sourcePath;
        @Column(displayName = "Symbol", description = "Human-readable symbol or semantic role.")
        private final String symbol;
        @Column(displayName = "Exact SHA-256", description = "Exact UTF-16 content/preimage fingerprint.")
        private final String exactSha256;
        @Column(displayName = "Structural SHA-256", description = "Normalized structural fingerprint.")
        private final String structuralSha256;
        @Column(displayName = "Logic SHA-256", description = "Normalized logic/composition fingerprint.")
        private final String logicSha256;
        @Column(displayName = "Structural hash64", description = "Compact structural hash derived from the normalized structure.")
        private final String structuralHash64;
        @Column(displayName = "Logic hash64", description = "Compact normalized logic hash.")
        private final String logicHash64;
        @Column(displayName = "SimHash64", description = "64-bit locality-sensitive SimHash over normalized logic features.")
        private final String simHash64;
        @Column(displayName = "Normalized composition", description = "Deterministic composition representation used for logic identity.")
        private final String normalizedComposition;

        public Row(
                String nodeId,
                String kind,
                String semanticKey,
                String sourcePath,
                String symbol,
                M3SemanticFingerprint fingerprint) {
            this.nodeId = nodeId;
            this.kind = kind;
            this.semanticKey = semanticKey;
            this.sourcePath = sourcePath;
            this.symbol = symbol;
            this.exactSha256 = fingerprint.exactSha256();
            this.structuralSha256 = fingerprint.structuralSha256();
            this.logicSha256 = fingerprint.logicSha256();
            this.structuralHash64 = fingerprint.structuralHash64Hex();
            this.logicHash64 = fingerprint.logicHash64Hex();
            this.simHash64 = fingerprint.simHash64Hex();
            this.normalizedComposition = fingerprint.normalizedComposition();
        }

        public String nodeId() { return nodeId; }
        public String kind() { return kind; }
        public String semanticKey() { return semanticKey; }
        public String sourcePath() { return sourcePath; }
        public String symbol() { return symbol; }
        public String exactSha256() { return exactSha256; }
        public String structuralSha256() { return structuralSha256; }
        public String logicSha256() { return logicSha256; }
        public String structuralHash64() { return structuralHash64; }
        public String logicHash64() { return logicHash64; }
        public String simHash64() { return simHash64; }
        public String normalizedComposition() { return normalizedComposition; }
    }
}
