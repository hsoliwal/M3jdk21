// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.verbatim;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

/**
 * FILE-local, non-mutating OpenRewrite preimage ledger for real JDK21 Java sources.
 *
 * <p>The repository byte gate remains authoritative for exact on-disk identity. This recipe emits
 * the exact UTF-8 SHA-256 over OpenRewrite's lossless rendered Java source so hash-pinned recipes
 * can be generated and checked using the same representation they will later transform.</p>
 */
public final class M3Jdk21SourceFingerprintRecipe extends Recipe {
    private transient FingerprintTable fingerprints = new FingerprintTable(this);

    @Override
    public String getDisplayName() {
        return "Inventory exact JDK21 Java source preimages";
    }

    @Override
    public String getDescription() {
        return "Emits one FILE-local rendered-source SHA-256 row per Java compilation unit "
                + "without modifying source.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "jdk21",
                "verbatim",
                "preimage",
                "sha256",
                "file-local",
                "non-mutating",
                "recipe-first");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(
                    J.CompilationUnit compilationUnit,
                    ExecutionContext context) {
                J.CompilationUnit value =
                        super.visitCompilationUnit(compilationUnit, context);
                String rendered = value.printAll();
                byte[] utf8 = rendered.getBytes(StandardCharsets.UTF_8);
                fingerprints.insertRow(
                        context,
                        new Row(
                                normalized(value.getSourcePath()),
                                HexFormat.of().formatHex(digest().digest(utf8)),
                                utf8.length,
                                lineCount(rendered)));
                return value;
            }
        };
    }

    private static int lineCount(String text) {
        if (text.isEmpty()) return 0;
        int lines = 1;
        for (int index = 0; index < text.length(); index++) {
            if (text.charAt(index) == '\n') lines++;
        }
        return lines;
    }

    private static String normalized(Path path) {
        return path.normalize().toString().replace('\\', '/');
    }

    private static MessageDigest digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    public static final class FingerprintTable extends DataTable<Row> {
        FingerprintTable(Recipe recipe) {
            super(
                    recipe,
                    "M3 JDK21 Java preimages",
                    "Exact rendered Java source fingerprints for FILE-local hash-pinned recipe admission.");
        }
    }

    public static final class Row {
        @Column(displayName = "Source path", description = "Repository-relative Java source path.")
        private final String sourcePath;
        @Column(displayName = "SHA-256", description = "SHA-256 of exact rendered UTF-8 Java source.")
        private final String sha256;
        @Column(displayName = "UTF-8 bytes", description = "Rendered UTF-8 byte length.")
        private final int utf8Bytes;
        @Column(displayName = "Line count", description = "Rendered line count.")
        private final int lineCount;

        Row(String sourcePath, String sha256, int utf8Bytes, int lineCount) {
            this.sourcePath = sourcePath;
            this.sha256 = sha256;
            this.utf8Bytes = utf8Bytes;
            this.lineCount = lineCount;
        }

        public String sourcePath() { return sourcePath; }
        public String sha256() { return sha256; }
        public int utf8Bytes() { return utf8Bytes; }
        public int lineCount() { return lineCount; }
    }
}
