// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
package com.synexia.rewrite;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

/** Serial application of inventoried Java literal metadata through the canonical recipe. */
public final class M3CollectionsPackageRenameMetadataCli {
    private M3CollectionsPackageRenameMetadataCli() { }
    private record Candidate(Path path, String before, String after) { }

    public static void main(String[] args) throws Exception {
        if (args.length < 3 || args.length > 4 || (args.length == 4 && !args[3].equals("--apply"))) {
            throw new IllegalArgumentException("ROOT MANIFEST_JSON RECEIPT_TSV [--apply]");
        }
        Path root = Path.of(args[0]).toRealPath();
        JsonNode manifest = new ObjectMapper().readTree(Path.of(args[1]).toFile());
        if (!manifest.isArray()) throw new IllegalArgumentException("Manifest must be an array");
        Map<String, Map<String, String>> files = new LinkedHashMap<>();
        Map<String, String> expectedHashes = new LinkedHashMap<>();
        for (JsonNode row : manifest) {
            String path = required(row, "path"), oldToken = required(row, "old"), newToken = required(row, "new");
            if (!path.endsWith(".java")) throw new IllegalArgumentException("Java source only: " + path);
            M3CollectionsPackageRenameCli.source(root, path);
            String previous = files.computeIfAbsent(path, ignored -> new LinkedHashMap<>()).putIfAbsent(oldToken, newToken);
            if (previous != null && !previous.equals(newToken)) throw new IllegalArgumentException("Contradictory token: " + path);
            String expectedHash = required(row, "expected_sha256");
            if (!expectedHash.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("Invalid expected_sha256: " + path);
            String priorHash = expectedHashes.putIfAbsent(path, expectedHash);
            if (priorHash != null && !priorHash.equals(expectedHash)) throw new IllegalArgumentException("Contradictory source hashes: " + path);
        }
        List<Candidate> candidates = new ArrayList<>();
        StringBuilder receipt = new StringBuilder("path\tbefore_sha256\tafter_sha256\tchanged\tfixedpoint\n");
        for (String path : files.keySet().stream().sorted().toList()) {
            Path target = M3CollectionsPackageRenameCli.source(root, path);
            String before = Files.readString(target);
            String beforeHash = M3CollectionsPackageRenameMetadataRecipe.sha256(before);
            if (!beforeHash.equals(expectedHashes.get(path))) throw new IllegalArgumentException("Inventoried source hash drift: " + path);
            Map<String, String> tokens = files.get(path);
            SourceFile source = parse(path, before);
            Set<String> sourceTokens = new HashSet<>();
            new JavaIsoVisitor<Set<String>>() {
                @Override public J.Literal visitLiteral(J.Literal literal, Set<String> sink) {
                    if (literal.getValue() instanceof String) sink.add(literal.getValueSource());
                    return literal;
                }
            }.visit(source, sourceTokens);
            for (var token : tokens.entrySet()) {
                if (!sourceTokens.contains(token.getKey()) && !sourceTokens.contains(token.getValue())) {
                    throw new IllegalArgumentException("Neither admitted preimage nor postimage token exists: " + path + " " + token.getKey());
                }
            }
            List<Throwable> errors = new ArrayList<>();
            var ctx = new InMemoryExecutionContext(errors::add);
            var recipe = new M3CollectionsPackageRenameMetadataRecipe(path, beforeHash, tokens, ctx);
            var changes = recipe.run(new InMemoryLargeSourceSet(List.of(source)), ctx).getChangeset().getAllResults();
            if (!errors.isEmpty()) throw new IllegalStateException("Recipe errors: " + path, errors.getFirst());
            String after = changes.isEmpty() ? before : changes.getFirst().getAfter().printAll();
            String afterHash = M3CollectionsPackageRenameMetadataRecipe.sha256(after);
            var fixed = new M3CollectionsPackageRenameMetadataRecipe(path, afterHash, tokens, ctx)
                    .run(new InMemoryLargeSourceSet(List.of(parse(path, after))), ctx).getChangeset().getAllResults();
            if (!errors.isEmpty() || !fixed.isEmpty()) throw new IllegalStateException("No fixedpoint: " + path);
            candidates.add(new Candidate(target, before, after));
            receipt.append(path).append('\t').append(beforeHash).append('\t').append(afterHash)
                    .append('\t').append(!before.equals(after)).append("\tPASS\n");
        }
        // Recheck every preimage before any file mutation; candidate generation is read-only.
        for (Candidate candidate : candidates) {
            M3CollectionsPackageRenameCli.source(root, root.relativize(candidate.path()).toString());
            if (!Files.readString(candidate.path()).equals(candidate.before())) throw new IllegalStateException("Concurrent source drift: " + candidate.path());
        }
        Path receiptPath = Path.of(args[2]);
        if (receiptPath.getParent() != null) Files.createDirectories(receiptPath.getParent());
        Files.writeString(receiptPath, receipt.toString());
        Path candidateRoot = receiptPath.toAbsolutePath().getParent().resolve("metadata-candidates");
        for (Candidate candidate : candidates) {
            Path destination = candidateRoot.resolve(root.relativize(candidate.path()));
            Files.createDirectories(destination.getParent());
            Files.writeString(destination, candidate.after());
        }
        if (args.length == 4) for (Candidate candidate : candidates) {
            if (!candidate.before().equals(candidate.after())) Files.writeString(candidate.path(), candidate.after());
        }
        System.out.println("metadata_files=" + candidates.size() + " changed="
                + candidates.stream().filter(c -> !c.before().equals(c.after())).count()
                + " apply=" + (args.length == 4));
    }

    private static String required(JsonNode row, String field) {
        if (!row.hasNonNull(field) || !row.get(field).isTextual()) throw new IllegalArgumentException("Missing text field: " + field);
        return row.get(field).asText();
    }

    private static SourceFile parse(String path, String text) {
        List<Throwable> errors = new ArrayList<>();
        var ctx = new InMemoryExecutionContext(errors::add);
        var parsed = JavaParser.fromJavaVersion().build().parseInputs(
                List.of(Parser.Input.fromString(Path.of(path), text)), null, ctx).toList();
        if (!errors.isEmpty() || parsed.size() != 1 || !(parsed.getFirst() instanceof J.CompilationUnit)
                || !parsed.getFirst().printAll().equals(text)) throw new IllegalArgumentException("Lossless Java parse failed: " + path);
        return parsed.getFirst();
    }
}
