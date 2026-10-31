// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite.semantic;

import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

/** Semantic-source fingerprint evidence emitted without mutating Java source. */
public final class M3SemanticHashTable extends DataTable<M3SemanticHashTable.Row> {
    public M3SemanticHashTable(Recipe recipe) {
        super(recipe, "M3 semantic hashes",
                "Layered Java semantic-source fingerprints for recipe proof and convergence.");
    }

    /** One method/file fingerprint. */
    public static final class Row {
        @Column(displayName = "Source path", description = "Repository-relative source path.")
        private final String sourcePath;
        @Column(displayName = "Level", description = "METHOD or FILE.")
        private final String level;
        @Column(displayName = "Symbol", description = "Method name or <file>.")
        private final String symbol;
        @Column(displayName = "Contract hash", description = "Declaration/type-surface fingerprint.")
        private final String contractHash;
        @Column(displayName = "Logic hash", description = "Canonical implementation fingerprint.")
        private final String logicHash;
        @Column(displayName = "Pattern/IOP hash", description = "Architecture-role fingerprint.")
        private final String architectureHash;
        @Column(displayName = "Behavioral hash", description = "Contract + logic fingerprint.")
        private final String behavioralHash;
        @Column(displayName = "Whole hash", description = "Behavioral + Pattern/IOP fingerprint.")
        private final String wholeHash;

        public Row(String sourcePath, String level, String symbol,
                   String contractHash, String logicHash, String architectureHash,
                   String behavioralHash, String wholeHash) {
            this.sourcePath = sourcePath;
            this.level = level;
            this.symbol = symbol;
            this.contractHash = contractHash;
            this.logicHash = logicHash;
            this.architectureHash = architectureHash;
            this.behavioralHash = behavioralHash;
            this.wholeHash = wholeHash;
        }

        public String sourcePath() { return sourcePath; }
        public String level() { return level; }
        public String symbol() { return symbol; }
        public String contractHash() { return contractHash; }
        public String logicHash() { return logicHash; }
        public String architectureHash() { return architectureHash; }
        public String behavioralHash() { return behavioralHash; }
        public String wholeHash() { return wholeHash; }
    }
}
