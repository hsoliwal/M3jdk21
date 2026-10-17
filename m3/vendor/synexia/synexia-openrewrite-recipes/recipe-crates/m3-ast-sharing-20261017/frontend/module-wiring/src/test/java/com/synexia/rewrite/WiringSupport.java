// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.config.YamlResourceLoader;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.text.PlainText;

final class WiringSupport {
    private WiringSupport() { }
    static Recipe named(String name) throws Exception {
        // These are this task's sealed owners; the shared provider has broader upstream capabilities.
        if (name.equals("ASTModuleWiringCanonical")) {
            requireScope("text", "canonical", java.util.Set.of(
                    "synexia-mindex/compiler/pom.xml",
                    "synexia-mindex/compiler/src/main/resources/META-INF/services/com.sun.source.util.Plugin",
                    "synexia-mindex/compiler-openrewrite/pom.xml"));
        } else if (name.equals("ASTModuleWiringTarget")) {
            requireScope("text", "receiver", java.util.Set.of(
                    "m3/ports/compiletime/pom.xml",
                    "m3/ports/compiletime/src/main/resources/META-INF/services/com.sun.source.util.Plugin",
                    "m3/ports/compiletime-openrewrite/pom.xml"));
            requireScope("java", "receiver", java.util.Set.of(
                    "m3/ports/compiletime-openrewrite/src/main/java/com/m3/compiletime/ast/M3OpenRewriteSharedAST.java"));
        }
        var resource = WiringSupport.class.getResource("/META-INF/rewrite/m3-ast-module-wiring.yml");
        try (var input = resource.openStream()) {
            return Environment.builder().load(new YamlResourceLoader(input, resource.toURI(), new Properties()))
                    .build().activateRecipes("com.synexia.m3." + name + "20261017");
        }
    }

    private static void requireScope(String kind, String label, java.util.Set<String> expected) throws Exception {
        List<String[]> rows = manifest(kind, label);
        var actual = rows.stream().map(row -> row[0]).collect(java.util.stream.Collectors.toSet());
        if (rows.size() != expected.size() || !actual.equals(expected)) {
            throw new IllegalStateException("task target scope drift: " + label + "/" + kind);
        }
    }

    static List<String[]> manifest(String kind, String label) throws Exception {
        String resource = "/com/synexia/rewrite/hash-pinned-" + kind + "/m3-ast-wiring-" + label + "-20261017/manifest.tsv";
        try (var input = WiringSupport.class.getResourceAsStream(resource)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8).lines().filter(x -> !x.isBlank())
                    .map(x -> x.split("\t")).toList();
        }
    }

    static List<SourceFile> inputs(List<String[]> rows, Path root) throws Exception {
        List<SourceFile> result = new ArrayList<>();
        for (String[] row : rows) {
            Path path = Path.of(row[0]);
            if (Files.exists(root.resolve(path))) result.add(parse(path, Files.readString(root.resolve(path))));
        }
        return result;
    }

    static SourceFile parse(Path path, String text) {
        if (!path.toString().endsWith(".java")) return PlainText.builder().sourcePath(path).text(text).build();
        var errors = new ArrayList<Throwable>();
        SourceFile result = JavaParser.fromJavaVersion().build().parseInputs(List.of(Parser.Input.fromString(path, text)),
                null, new InMemoryExecutionContext(errors::add)).findFirst().orElseThrow();
        assertTrue(errors.isEmpty(), errors.toString());
        assertInstanceOf(J.CompilationUnit.class, result);
        assertEquals(text, result.printAll());
        return result.withSourcePath(path);
    }

    static List<SourceFile> run(Recipe recipe, List<SourceFile> before) {
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        List<Path> classpath = new ArrayList<>(JavaParser.runtimeClasspath());
        String plugin = System.getProperty("m3.sharedAst.plugin", "");
        if (!plugin.isBlank()) classpath.add(Path.of(plugin));
        context.putMessage("com.synexia.rewrite.hashPinnedJavaSnapshotRecipe.parserClasspath", classpath);
        var changes = recipe.run(new InMemoryLargeSourceSet(before), context).getChangeset().getAllResults();
        if (!errors.isEmpty()) throw new IllegalStateException("recipe refused candidate", errors.getFirst());
        return changes.stream().map(x -> x.getAfter()).toList();
    }

    static List<SourceFile> merge(List<SourceFile> before, List<SourceFile> changes) {
        var after = new ArrayList<>(before);
        after.removeIf(x -> changes.stream().anyMatch(y -> x.getSourcePath().equals(y.getSourcePath())));
        after.addAll(changes);
        return after;
    }

    static void write(Path root, SourceFile file) throws Exception {
        Path path = root.resolve(file.getSourcePath()).normalize();
        if (!path.startsWith(root.normalize())) throw new IllegalArgumentException("outside owner root");
        Files.createDirectories(path.getParent());
        Files.writeString(path, file.printAll());
    }

    static void materialize(String label, Recipe recipe, List<String[]> rows) throws Exception {
        String setting = System.getProperty("m3.wiring." + label, "");
        if (setting.isBlank()) return;
        Path root = Path.of(setting);
        List<SourceFile> current = inputs(rows, root);
        List<SourceFile> changes = run(recipe, current);
        // Admission completes for all paths before the first write.
        List<SourceFile> after = merge(current, changes);
        assertEquals(rows.size(), after.size());
        assertTrue(run(recipe, after).isEmpty());
        for (SourceFile change : changes) write(root, change);
    }

    static String hash(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
    }
}
