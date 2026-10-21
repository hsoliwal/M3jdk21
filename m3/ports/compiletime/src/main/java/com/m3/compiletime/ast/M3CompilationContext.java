// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.compiletime.ast;

import com.synexia.mindex.compiler.ast.MIndexJavaAST;
import com.synexia.mindex.compiler.ast.MIndexASTDocument;
import com.synexia.mindex.compiler.ast.MIndexASTOccurrence;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** An ordered compiler environment seal, with explicit session-only or reusable authority. */
public record M3CompilationContext(String sha256, List<String> facts, boolean reusable) {
    public M3CompilationContext {
        facts = List.copyOf(Objects.requireNonNull(facts, "facts"));
        if (!M3FileSourceFacts.contextSha256(facts).equals(sha256)) {
            throw new IllegalArgumentException("compiler context digest mismatch");
        }
    }

    /** Safe default when an integration has not captured the complete compiler environment. */
    public static M3CompilationContext isolated(List<String> orderedOptions) {
        List<String> facts = prefix(orderedOptions);
        facts.add("session");
        facts.add(UUID.randomUUID().toString());
        return new M3CompilationContext(M3FileSourceFacts.contextSha256(facts), facts, false);
    }

    /**
     * Reusable context after the caller sealed the actual source generation and ordered inputs.
     * Each content pin must cover the real dependency/processor/module/generated-source bytes.
     * Completeness is an integration obligation; a path or version label alone is insufficient.
     */
    public static M3CompilationContext sealed(List<String> orderedOptions,
            String sourceGenerationSha256, List<String> orderedContentSha256) {
        requireSha(sourceGenerationSha256);
        List<String> pins = List.copyOf(orderedContentSha256);
        pins.forEach(M3CompilationContext::requireSha);
        List<String> facts = prefix(orderedOptions);
        facts.add("source-generation");
        facts.add(sourceGenerationSha256);
        facts.add("ordered-content-pins");
        facts.add(Integer.toString(pins.size()));
        facts.addAll(pins);
        return new M3CompilationContext(M3FileSourceFacts.contextSha256(facts), facts, true);
    }

    private static List<String> prefix(List<String> options) {
        List<String> facts = new ArrayList<>();
        facts.add("M3_JAVAC_CONTEXT_V1");
        facts.add(System.getProperty("java.runtime.version"));
        facts.add(System.getProperty("java.vendor"));
        facts.add(Integer.toString(options.size()));
        facts.addAll(List.copyOf(options));
        return facts;
    }

    private static void requireSha(String value) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("content pin must be lowercase SHA-256");
        }
    }
}
