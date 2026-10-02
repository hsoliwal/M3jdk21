// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.hash;

import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

/** Multi-plane M3 semantic fingerprints emitted without mutating Java source. */
public final class M3SemanticHashTable extends DataTable<M3SemanticHashTable.Row> {
    public M3SemanticHashTable(Recipe recipe) {
        super(
                recipe,
                "M3 Java semantic hashes",
                "Versioned exact, structural and pattern/IOP hashes for Java source files.");
    }

    public static final class Row {
        @Column(displayName = "Source path", description = "Repository-relative Java source path.")
        private final String sourcePath;
        @Column(displayName = "Algorithm", description = "M3 semantic-hash algorithm version.")
        private final String algorithm;
        @Column(displayName = "Exact SHA-256", description = "Exact OpenRewrite source rendering SHA-256.")
        private final String exactSha256;
        @Column(displayName = "Token SHA-256", description = "Whitespace/comment-insensitive Java-token SHA-256.")
        private final String tokenSha256;
        @Column(displayName = "Pattern/IOP SHA-256", description = "M3 pattern/IOP semantic-memory SHA-256.")
        private final String patternIopSha256;
        @Column(displayName = "Semantic SHA-256", description = "Versioned composite semantic fingerprint.")
        private final String semanticSha256;

        public Row(
                String sourcePath,
                String algorithm,
                String exactSha256,
                String tokenSha256,
                String patternIopSha256,
                String semanticSha256) {
            this.sourcePath = sourcePath;
            this.algorithm = algorithm;
            this.exactSha256 = exactSha256;
            this.tokenSha256 = tokenSha256;
            this.patternIopSha256 = patternIopSha256;
            this.semanticSha256 = semanticSha256;
        }

        public String sourcePath() { return sourcePath; }
        public String algorithm() { return algorithm; }
        public String exactSha256() { return exactSha256; }
        public String tokenSha256() { return tokenSha256; }
        public String patternIopSha256() { return patternIopSha256; }
        public String semanticSha256() { return semanticSha256; }
    }
}
