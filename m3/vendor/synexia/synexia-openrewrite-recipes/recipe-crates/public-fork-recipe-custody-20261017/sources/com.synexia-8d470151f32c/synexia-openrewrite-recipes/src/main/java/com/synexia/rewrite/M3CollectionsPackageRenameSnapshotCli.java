// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
package com.synexia.rewrite;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

/** Bounded adapter to the incumbent hash-pinned Java snapshot recipe. */
public final class M3CollectionsPackageRenameSnapshotCli {
    private M3CollectionsPackageRenameSnapshotCli() { }

    public static void main(String[] args) throws Exception {
        if (args.length < 3 || args.length > 4 || (args.length == 4 && !"--apply".equals(args[3]))) {
            throw new IllegalArgumentException("ROOT CRATE OUTPUT [--apply]");
        }
        Path root = Path.of(args[0]).toRealPath();
        String crate = args[1];
        Path output = Path.of(args[2]);
        var authority = new M3HashPinnedJavaSnapshotRecipe(crate);
        Map<String, String> originals = new LinkedHashMap<>();
        List<SourceFile> inputs = new ArrayList<>();
        for (String path : authority.targetPaths()) {
            Path file = safe(root, path);
            if (Files.exists(file)) {
                String text = Files.readString(file);
                originals.put(path, text);
                inputs.add(parse(path, text));
            }
        }
        Map<String, String> candidate = execute(crate, inputs);
        List<SourceFile> finalSources = new ArrayList<>();
        for (String path : authority.targetPaths()) {
            String text = candidate.getOrDefault(path, originals.get(path));
            if (text == null) throw new IllegalStateException("Missing output: " + path);
            candidate.put(path, text);
            finalSources.add(parse(path, text));
        }
        if (!execute(crate, finalSources).isEmpty()) throw new IllegalStateException("Snapshot fixedpoint failed");
        String required = authority.preimageManifest().entrySet().stream()
                .filter(e -> !"ABSENT".equals(e.getValue())).map(Map.Entry::getKey).sorted().findFirst().orElseThrow();
        List<SourceFile> drift = new ArrayList<>(finalSources);
        for (int i = 0; i < drift.size(); i++) {
            if (drift.get(i).getSourcePath().toString().equals(required)) {
                drift.set(i, parse(required, drift.get(i).printAll() + "\n// drift control\n"));
            }
        }
        refuse(crate, drift, "drift");
        refuse(crate, finalSources.stream().filter(s -> !s.getSourcePath().toString().equals(required)).toList(), "missing");
        List<SourceFile> duplicate = new ArrayList<>(finalSources);
        duplicate.add(finalSources.getFirst().withId(Tree.randomId()));
        refuse(crate, duplicate, "duplicate");
        var additive = authority.preimageManifest().entrySet().stream()
                .filter(e -> "ABSENT".equals(e.getValue())).map(Map.Entry::getKey).sorted().findFirst();
        if (additive.isPresent()) {
            List<SourceFile> occupied = new ArrayList<>(finalSources);
            for (int i = 0; i < occupied.size(); i++) {
                if (occupied.get(i).getSourcePath().toString().equals(additive.get())) {
                    occupied.set(i, parse(additive.get(), occupied.get(i).printAll() + "\n// occupied control\n"));
                }
            }
            refuse(crate, occupied, "occupied");
        }
        for (String path : authority.targetPaths()) {
            Path file = safe(root, path);
            if (originals.containsKey(path)) {
                if (!Files.readString(file).equals(originals.get(path))) throw new IllegalStateException("Concurrent drift: " + path);
            } else if (Files.exists(file)) throw new IllegalStateException("Concurrent occupied output: " + path);
        }
        Files.createDirectories(output);
        StringBuilder receipt = new StringBuilder("path\tbefore_sha256\tafter_sha256\tfixedpoint\trefusals\n");
        for (String path : authority.targetPaths()) {
            String text = candidate.get(path);
            Path destination = output.resolve("sources").resolve(path);
            Files.createDirectories(destination.getParent());
            Files.writeString(destination, text);
            receipt.append(path).append('\t').append(originals.containsKey(path)
                    ? M3CollectionsPackageRenameMetadataRecipe.sha256(originals.get(path)) : "ABSENT")
                    .append('\t').append(M3CollectionsPackageRenameMetadataRecipe.sha256(text))
                    .append("\tPASS\tDRIFT_MISSING_DUPLICATE_PASS;")
                    .append(additive.isPresent() ? "OCCUPIED_PASS" : "OCCUPIED_NOT_APPLICABLE_NO_ADDITIVE_TARGET").append('\n');
        }
        Files.writeString(output.resolve("SNAPSHOT_EXECUTION.tsv"), receipt.toString());
        if (args.length == 4) {
            for (String path : authority.targetPaths()) {
                Path destination = safe(root, path);
                Files.createDirectories(destination.getParent());
                Files.writeString(destination, candidate.get(path));
            }
        }
        System.out.println("snapshot_targets=" + candidate.size() + " fixedpoint=PASS refusal_controls="
                + (additive.isPresent() ? 4 : 3) + " apply=" + (args.length == 4));
    }

    private static Path safe(Path root, String path) throws Exception {
        Path file = root.resolve(path).normalize();
        if (!file.startsWith(root)) throw new IllegalArgumentException("Escaping path");
        for (Path current = file; current != null && current.startsWith(root); current = current.getParent()) {
            if (Files.isSymbolicLink(current)) throw new IllegalArgumentException("Symlink target");
        }
        return file;
    }

    private static SourceFile parse(String path, String text) {
        List<Throwable> errors = new ArrayList<>();
        var context = new InMemoryExecutionContext(errors::add);
        var sources = JavaParser.fromJavaVersion().build().parseInputs(
                List.of(Parser.Input.fromString(Path.of(path), text)), null, context).toList();
        if (!errors.isEmpty() || sources.size() != 1 || !(sources.getFirst() instanceof J.CompilationUnit)
                || !sources.getFirst().printAll().equals(text)) throw new IllegalStateException("Lossless Java parse failed: " + path);
        return sources.getFirst();
    }

    private static Map<String, String> execute(String crate, List<SourceFile> sources) {
        List<Throwable> errors = new ArrayList<>();
        var context = new InMemoryExecutionContext(errors::add);
        var changes = new M3HashPinnedJavaSnapshotRecipe(crate)
                .run(new InMemoryLargeSourceSet(sources), context).getChangeset().getAllResults();
        if (!errors.isEmpty()) throw new IllegalStateException("Snapshot recipe error", errors.getFirst());
        Map<String, String> outputs = new LinkedHashMap<>();
        for (var change : changes) {
            if (change.getAfter() == null) throw new IllegalStateException("Unexpected deletion");
            outputs.put(change.getAfter().getSourcePath().toString(), change.getAfter().printAll());
        }
        return outputs;
    }

    private static void refuse(String crate, List<SourceFile> sources, String control) {
        try {
            execute(crate, sources);
        } catch (RuntimeException expected) {
            return;
        }
        throw new IllegalStateException("Missing refusal: " + control);
    }
}
