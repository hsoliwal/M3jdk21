// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
package com.synexia.rewrite;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

/** Exact, hash-admitted Java string metadata edits; source package renaming is a separate recipe. */
public final class M3CollectionsPackageRenameMetadataRecipe extends Recipe {
    private final String path;
    private final String expectedHash;
    private final Map<String, Replacement> replacements;
    private volatile String observedPostimageHash;

    private record Replacement(String token, Object value) { }

    public M3CollectionsPackageRenameMetadataRecipe(String path, String expectedHash,
            Map<String, String> literalTokens, ExecutionContext context) {
        this.path = path.replace('\\', '/');
        this.expectedHash = expectedHash;
        if (!expectedHash.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("Invalid SHA-256");
        if (this.path.startsWith("/") || this.path.contains("..")) throw new IllegalArgumentException("Unsafe source path");
        this.replacements = new LinkedHashMap<>();
        StringBuilder fixture = new StringBuilder("class M3MetadataLiteralValues {\n");
        int index = 0;
        for (var entry : literalTokens.entrySet()) {
            requireOrdinaryString(entry.getKey());
            requireOrdinaryString(entry.getValue());
            fixture.append("String value").append(index++).append(" = ").append(entry.getValue()).append(";\n");
        }
        fixture.append("}\n");
        var parsed = JavaParser.fromJavaVersion().build().parse(context, fixture.toString()).toList();
        if (parsed.size() != 1 || !(parsed.getFirst() instanceof J.CompilationUnit unit)) {
            throw new IllegalArgumentException("Replacement literals did not parse as Java");
        }
        Map<String, Object> values = new LinkedHashMap<>();
        new JavaIsoVisitor<Map<String, Object>>() {
            @Override public J.Literal visitLiteral(J.Literal literal, Map<String, Object> sink) {
                if (literal.getValue() instanceof String) sink.put(literal.getValueSource(), literal.getValue());
                return literal;
            }
        }.visit(unit, values);
        for (var entry : literalTokens.entrySet()) {
            if (!values.containsKey(entry.getValue())) throw new IllegalArgumentException("Not a Java string literal: " + entry.getValue());
            replacements.put(entry.getKey(), new Replacement(entry.getValue(), values.get(entry.getValue())));
        }
    }

    private static void requireOrdinaryString(String token) {
        if (!token.startsWith("\"") || !token.endsWith("\"") || token.startsWith("\"\"\"")
                || token.indexOf('\n') >= 0 || token.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("Only admitted ordinary Java string tokens are supported");
        }
    }

    @Override public String getDisplayName() { return "M3 hash-admitted collection namespace literal metadata"; }
    @Override public String getDescription() {
        return "Updates only exact inventoried Java literal tokens in one SHA-256-admitted file using the Java LST.";
    }

    @Override public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            private boolean admitted;
            @Override public J.CompilationUnit visitCompilationUnit(J.CompilationUnit unit, ExecutionContext ctx) {
                admitted = unit.getSourcePath().toString().replace('\\', '/').equals(path);
                if (!admitted) return unit;
                String actualHash = sha256(unit.printAll());
                if (!actualHash.equals(expectedHash) && !actualHash.equals(observedPostimageHash)) {
                    throw new IllegalArgumentException("Source hash drift: " + path);
                }
                J.CompilationUnit result = super.visitCompilationUnit(unit, ctx);
                observedPostimageHash = sha256(result.printAll());
                return result;
            }
            @Override public J.Literal visitLiteral(J.Literal literal, ExecutionContext ctx) {
                J.Literal result = super.visitLiteral(literal, ctx);
                if (!admitted || !(result.getValue() instanceof String)) return result;
                Replacement replacement = replacements.get(result.getValueSource());
                return replacement == null ? result : result.withValue(replacement.value()).withValueSource(replacement.token());
            }
        };
    }

    public static String sha256(String source) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(source.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
