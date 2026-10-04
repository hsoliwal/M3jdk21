// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.index;

import com.m3.indexdb.M3IndexDbSemanticNode;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

/** Canonical repository/project/library/module/package/file root hashes. */
public final class M3SemanticRootTable extends DataTable<M3SemanticRootTable.Row> {
    public M3SemanticRootTable(Recipe recipe) {
        super(
                recipe,
                "M3 semantic roots",
                "Canonical semantic roots generated after OpenRewrite fan-in; repository root is the whole semantic hash.");
    }

    public static final class Row {
        @Column(displayName = "Kind", description = "Root semantic level.")
        private final String kind;
        @Column(displayName = "Semantic key", description = "Stable root semantic address.")
        private final String semanticKey;
        @Column(displayName = "Node ID", description = "Stable root identity.")
        private final String nodeId;
        @Column(displayName = "Exact SHA-256", description = "Exact child-composition identity.")
        private final String exactSha256;
        @Column(displayName = "Structural SHA-256", description = "Whole normalized structural hash.")
        private final String structuralSha256;
        @Column(displayName = "Logic SHA-256", description = "Whole normalized logic hash.")
        private final String logicSha256;
        @Column(displayName = "Structural hash64", description = "Compact structural root.")
        private final String structuralHash64;
        @Column(displayName = "Logic hash64", description = "Compact logic root.")
        private final String logicHash64;
        @Column(displayName = "SimHash64", description = "Whole-root candidate similarity signal.")
        private final String simHash64;

        public Row(M3IndexDbSemanticNode node) {
            this.kind = node.kind().name();
            this.semanticKey = node.semanticKey();
            this.nodeId = node.nodeId();
            this.exactSha256 = node.fingerprint().exactSha256();
            this.structuralSha256 = node.fingerprint().structuralSha256();
            this.logicSha256 = node.fingerprint().logicSha256();
            this.structuralHash64 = node.fingerprint().structuralHash64Hex();
            this.logicHash64 = node.fingerprint().logicHash64Hex();
            this.simHash64 = node.fingerprint().simHash64Hex();
        }

        public String kind() { return kind; }
        public String semanticKey() { return semanticKey; }
        public String nodeId() { return nodeId; }
        public String exactSha256() { return exactSha256; }
        public String structuralSha256() { return structuralSha256; }
        public String logicSha256() { return logicSha256; }
        public String structuralHash64() { return structuralHash64; }
        public String logicHash64() { return logicHash64; }
        public String simHash64() { return simHash64; }
    }
}
