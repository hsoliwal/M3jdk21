// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.hash;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

/**
 * Emits a versioned whole-file M3 semantic fingerprint for parsed Java source.
 *
 * <p>The fingerprint deliberately has separate planes. Exact hash proves exact rendered source.
 * Token hash ignores formatting/comments but preserves Java lexical tokens and literal content.
 * Pattern/IOP hash preserves M3 semantic-memory markers that comments would otherwise remove.
 * The composite semantic hash is a clustering/index signal only; equality never proves behavioral
 * equivalence by itself.
 */
public final class M3SemanticHashRecipe extends Recipe {
    public static final String ALGORITHM = "M3-JAVA21-SEMANTIC-V1";
    private static final Pattern SEMANTIC_MEMORY = Pattern.compile(
            "(M3-(?:IOP|ATOM):[^\\r\\n*]*)"
                    + "|(@I[A-Za-z0-9_$]*Pattern(?:\\([^\\r\\n]*\\))?)");

    private transient M3SemanticHashTable hashes = new M3SemanticHashTable(this);

    @Override
    public String getDisplayName() {
        return "M3 compute Java 21 semantic hashes";
    }

    @Override
    public String getDescription() {
        return "Emits exact, normalized-token, pattern/IOP and composite semantic hashes without mutating source.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "java21",
                "semantic-hash",
                "structural-hash",
                "patternization",
                "iop",
                "non-mutating");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(
                    J.CompilationUnit compilationUnit, ExecutionContext context) {
                J.CompilationUnit cu = super.visitCompilationUnit(compilationUnit, context);
                Fingerprint fingerprint = fingerprint(cu.printAll());
                hashes.insertRow(
                        context,
                        new M3SemanticHashTable.Row(
                                cu.getSourcePath().toString().replace('\\', '/'),
                                ALGORITHM,
                                fingerprint.exactSha256(),
                                fingerprint.tokenSha256(),
                                fingerprint.patternIopSha256(),
                                fingerprint.semanticSha256()));
                return cu;
            }
        };
    }

    static Fingerprint fingerprint(String source) {
        String exact = sha256(source);
        String normalizedTokens = M3JavaTokenNormalizer.normalize(source);
        String token = sha256(normalizedTokens);
        String semanticMemory = semanticMemory(source);
        String patternIop = sha256(semanticMemory);
        String semantic = sha256(
                ALGORITHM + "\n"
                        + token + "\n"
                        + patternIop);
        return new Fingerprint(exact, token, patternIop, semantic);
    }

    static String semanticMemory(String source) {
        Matcher matcher = SEMANTIC_MEMORY.matcher(source);
        StringBuilder memory = new StringBuilder();
        while (matcher.find()) {
            String value = matcher.group().trim();
            if (!value.isEmpty()) {
                memory.append(value).append('\u001f');
            }
        }
        return memory.toString();
    }

    static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    record Fingerprint(
            String exactSha256,
            String tokenSha256,
            String patternIopSha256,
            String semanticSha256) {}
}
