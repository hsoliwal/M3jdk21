// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

class M3ASTHierarchyRecipeTest {
    private static final List<String> LABELS = List.of("canonical", "receiver");
    private static String crate(String label) { return "m3-ast-hierarchy-" + label + "-20261017"; }

    @Test void exactCandidatesAndFixedPoint() throws Exception {
        boolean materialize = Boolean.getBoolean("m3.hierarchy.materialize");
        boolean applicationOnly = Boolean.getBoolean("m3.hierarchy.applicationOnly");
        if (applicationOnly && !materialize) {
            throw new IllegalArgumentException("applicationOnly requires materialize");
        }
        List<String> labels = List.of(System.getProperty("m3.hierarchy.labels",
                String.join(",", LABELS)).split(",", -1));
        if (labels.isEmpty() || !LABELS.containsAll(labels)
                || labels.stream().distinct().count() != labels.size()) {
            throw new IllegalArgumentException("invalid or duplicate hierarchy recipe label");
        }
        for (String label : labels) {
            var recipe = new M3HashPinnedJavaSnapshotRecipe(crate(label));
            if (!applicationOnly) {
                List<SourceFile> before = inputs(recipe, Path.of("preimages", label));
                List<SourceFile> changes = run(recipe, before);
                List<SourceFile> after = merge(before, changes);
                assertEquals(recipe.targetPaths().size(), after.size());
                assertTrue(run(recipe, after).isEmpty(), label + " fixed point");
                Collections.reverse(after);
                assertTrue(run(recipe, after).isEmpty(), label + " input order");
                for (SourceFile file : after) write(Path.of("target/candidate", label), file);
            }
            if (materialize) {
                Path root = Path.of(System.getProperty(label.equals("canonical")
                        ? "m3.hierarchy.synexia" : "m3.hierarchy.target"));
                List<SourceFile> current = inputs(recipe, root);
                List<SourceFile> applied = run(recipe, current);
                for (SourceFile file : applied) write(root, file);
                List<SourceFile> after = merge(current, applied);
                assertEquals(recipe.targetPaths().size(), after.size());
                assertTrue(run(recipe, after).isEmpty(), label + " applied fixed point");
                for (SourceFile file : after) {
                    assertEquals(file.printAll(), Files.readString(root.resolve(file.getSourcePath())),
                            label + " applied readback " + file.getSourcePath());
                }
            }
        }
    }

    @Test void sourceDriftDuplicatesAndMissingPreimagesRefuse() throws Exception {
        var recipe = new M3HashPinnedJavaSnapshotRecipe(crate("canonical"));
        List<SourceFile> before = inputs(recipe, Path.of("preimages/canonical"));
        assertThrows(IllegalStateException.class, () -> run(recipe, List.of()));
        List<SourceFile> duplicate = new ArrayList<>(before);
        duplicate.add(before.getFirst());
        assertThrows(IllegalStateException.class, () -> run(recipe, duplicate));
        SourceFile hostile = parse(before.getFirst().getSourcePath(),
                "class Unrelated { String decoy = \"M3AST // import java.util.*;\"; }");
        before.set(0, hostile);
        assertThrows(IllegalStateException.class, () -> run(recipe, before));
    }

    @Test void templateDriftRefusesBeforeAnyCandidate() throws Exception {
        String name = "/com/synexia/rewrite/hash-pinned-java/" + crate("canonical") + "/manifest.tsv";
        Path manifest = Path.of(getClass().getResource(name).toURI());
        Path template = manifest.getParent().resolve(Files.readAllLines(manifest).getFirst().split("\t")[3]);
        byte[] original = Files.readAllBytes(template);
        try {
            Files.writeString(template, new String(original, java.nio.charset.StandardCharsets.UTF_8) + "\n// drift\n");
            var recipe = new M3HashPinnedJavaSnapshotRecipe(crate("canonical"));
            assertThrows(IllegalStateException.class,
                    () -> run(recipe, inputs(recipe, Path.of("preimages/canonical"))));
        } finally { Files.write(template, original); }
    }

    private static List<SourceFile> inputs(M3HashPinnedJavaSnapshotRecipe recipe, Path root) throws Exception {
        List<org.openrewrite.Parser.Input> inputs = new ArrayList<>();
        var expected = new java.util.LinkedHashMap<Path, String>();
        for (String name : recipe.targetPaths()) if (Files.exists(root.resolve(name))) {
            Path path = Path.of(name);
            String text = Files.readString(root.resolve(path));
            expected.put(path, text);
            inputs.add(org.openrewrite.Parser.Input.fromString(path, text));
        }
        if (inputs.isEmpty()) return new ArrayList<>();
        List<Throwable> errors = new ArrayList<>();
        var context = new InMemoryExecutionContext(errors::add);
        var parser = JavaParser.fromJavaVersion().build();
        List<SourceFile> sources = new ArrayList<>();
        for (SourceFile source : parser.parseInputs(inputs, null, context).toList()) {
            Path path = source.getSourcePath();
            assertInstanceOf(J.CompilationUnit.class, source);
            assertEquals(expected.remove(path), source.printAll(), "preimage parse " + path);
            sources.add(source);
        }
        if (!errors.isEmpty()) throw new IllegalStateException("preimage parser failure", errors.getFirst());
        assertTrue(expected.isEmpty(), "all preimages parsed");
        return sources;
    }

    private static SourceFile parse(Path path, String text) {
        List<Throwable> errors = new ArrayList<>();
        var context = new InMemoryExecutionContext(errors::add);
        var parser = JavaParser.fromJavaVersion().build();
        SourceFile source = parser.parseInputs(List.of(org.openrewrite.Parser.Input.fromString(path, text)), null, context)
                .findFirst().orElseThrow();
        if (!errors.isEmpty()) throw new IllegalStateException("preimage parser failure", errors.getFirst());
        assertInstanceOf(J.CompilationUnit.class, source);
        assertEquals(text, source.printAll());
        return source.withSourcePath(path);
    }

    private static List<SourceFile> run(M3HashPinnedJavaSnapshotRecipe recipe, List<SourceFile> before) {
        List<Throwable> errors = new ArrayList<>();
        var context = new InMemoryExecutionContext(errors::add);
        String classpath = System.getProperty("m3.hierarchy.parserClasspath", "");
        if (!classpath.isEmpty()) context.putMessage("com.synexia.rewrite.hashPinnedJavaSnapshotRecipe.parserClasspath",
                java.util.Arrays.stream(classpath.split(java.io.File.pathSeparator)).map(Path::of).toList());
        var result = recipe.run(new InMemoryLargeSourceSet(before), context);
        if (!errors.isEmpty()) throw new IllegalStateException("recipe refused candidate", errors.getFirst());
        return result.getChangeset().getAllResults().stream().map(x -> x.getAfter()).toList();
    }

    private static List<SourceFile> merge(List<SourceFile> before, List<SourceFile> changes) {
        var after = new ArrayList<>(before);
        after.removeIf(a -> changes.stream().anyMatch(b -> a.getSourcePath().equals(b.getSourcePath())));
        after.addAll(changes);
        return after;
    }

    private static void write(Path root, SourceFile file) throws Exception {
        Path path = root.resolve(file.getSourcePath()).normalize();
        if (!path.startsWith(root.normalize())) throw new IllegalArgumentException("outside root");
        Files.createDirectories(path.getParent());
        Files.writeString(path, file.printAll());
    }
}
