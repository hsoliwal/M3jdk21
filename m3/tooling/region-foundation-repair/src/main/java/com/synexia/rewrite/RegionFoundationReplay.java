// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.m3.rewrite.backport.M3Jdk21HashPinnedTextSnapshotRecipe;
import com.m3.rewrite.backport.M3VerbatimJavaPairRecipe;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.text.PlainText;

/** Thin, task-bounded orchestration of existing M3 OpenRewrite engines, not a new rewrite engine. */
public final class RegionFoundationReplay {
    public static final String TEST = "test/jdk/java/lang/String/M3RegionMatchesNonPositive.java";
    private RegionFoundationReplay() { }

    public static List<SourceFile> parseJava(Map<String, String> inputs) {
        var context = context();
        List<Parser.Input> files = new ArrayList<>();
        inputs.forEach((path, source) -> files.add(Parser.Input.fromString(Path.of(path), source)));
        List<SourceFile> parsed = JavaParser.fromJavaVersion().build().parseInputs(files, null, context).toList();
        if (parsed.size() != inputs.size() || parsed.stream().anyMatch(p -> !(p instanceof J.CompilationUnit))) {
            throw new IllegalStateException("Java parse refused");
        }
        for (SourceFile file : parsed) {
            if (!file.printAll().equals(inputs.get(file.getSourcePath().toString()))) {
                throw new IllegalStateException("Java source round-trip drift");
            }
        }
        return parsed;
    }

    public static Map<String, String> changes(Recipe recipe, List<SourceFile> sources) {
        var results = recipe.run(new InMemoryLargeSourceSet(sources), context(), 3);
        var changes = new LinkedHashMap<String, String>();
        for (var result : results.getChangeset().getAllResults()) {
            SourceFile after = result.getAfter();
            if (after == null) throw new IllegalStateException("deletion refused");
            changes.put(after.getSourcePath().toString().replace('\\', '/'), after.printAll());
        }
        return changes;
    }

    public static M3VerbatimJavaPairRecipe javaRecipe(RegionFoundationInputs.Pair pair) {
        return new M3VerbatimJavaPairRecipe(pair.path(), RegionFoundationInputs.BASE,
                "openjdk/jdk21u@6a4e4920193b1a2fe7780007f1613ba972433280", "JDK-8316879",
                pair.before(), pair.after());
    }

    public static List<SourceFile> textSources(Map<String, String> values) {
        return values.entrySet().stream().map(e -> (SourceFile) PlainText.builder()
                .sourcePath(Path.of(e.getKey())).text(e.getValue()).build()).toList();
    }

    public static M3Jdk21HashPinnedTextSnapshotRecipe textRecipe() {
        return new M3Jdk21HashPinnedTextSnapshotRecipe("m3-region-foundation-20261016");
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
    }

    /** Generates a separate delivery tree only after all engine/refusal/fixed-point tests pass in Maven. */
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("BASE_CHECKOUT DELIVERY_DIRECTORY");
        Path base = Path.of(args[0]).toRealPath();
        Path output = Path.of(args[1]).toAbsolutePath().normalize();
        if (output.startsWith(base) || base.startsWith(output)) throw new IllegalArgumentException("separate delivery required");
        var pairs = RegionFoundationInputs.candidates(base);
        if (Files.exists(base.resolve(TEST))) throw new IllegalStateException("absent-before test already exists");
        Map<String, String> results = new LinkedHashMap<>();
        results.putAll(changes(javaRecipe(pairs.getFirst()), parseJava(Map.of(pairs.getFirst().path(), pairs.getFirst().before()))));
        Map<String, String> text = new LinkedHashMap<>();
        for (var pair : pairs.subList(1, pairs.size())) text.put(pair.path(), pair.before());
        results.putAll(changes(textRecipe(), textSources(text)));
        if (results.size() != 4) throw new IllegalStateException("exact four-file delivery required");
        for (var pair : pairs) {
            if (!pair.after().equals(results.get(pair.path()))) throw new IllegalStateException("postimage mismatch");
        }
        if (!changes(javaRecipe(pairs.getFirst()), parseJava(Map.of(RegionFoundationInputs.STRING,
                results.get(RegionFoundationInputs.STRING)))).isEmpty()) throw new IllegalStateException("Java fixed point failed");
        text.clear();
        results.forEach((path, source) -> { if (!path.equals(RegionFoundationInputs.STRING)) text.put(path, source); });
        if (!changes(textRecipe(), textSources(text)).isEmpty()) throw new IllegalStateException("text fixed point failed");
        parseJava(Map.of(TEST, results.get(TEST)));
        StringBuilder receipt = new StringBuilder("path\tbefore_sha256\tafter_sha256\n");
        for (var entry : results.entrySet()) {
            Path target = output.resolve(entry.getKey());
            Files.createDirectories(target.getParent());
            Files.writeString(target, entry.getValue(), StandardCharsets.UTF_8);
            String before = pairs.stream().filter(p -> p.path().equals(entry.getKey()))
                    .map(p -> RegionFoundationInputs.sha256(p.before())).findFirst().orElse("ABSENT");
            receipt.append(entry.getKey()).append('\t').append(before).append('\t')
                    .append(RegionFoundationInputs.sha256(entry.getValue())).append('\n');
        }
        Files.writeString(output.resolve("delivery.tsv"), receipt.toString(), StandardCharsets.UTF_8);
        System.out.println("OPENREWRITE_DELIVERY_PASS files=4 fixed_point=2");
    }
}
