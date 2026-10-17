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
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
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

final class MaskRecipeSupport {
    private MaskRecipeSupport() { }
    static Recipe named() throws Exception {
        var resource = MaskRecipeSupport.class.getResource("/META-INF/rewrite/m3-mask-zero.yml");
        try (var input = resource.openStream()) {
            return Environment.builder().load(new YamlResourceLoader(input, resource.toURI(), new Properties()))
                    .build().activateRecipes("com.synexia.m3.MaskZeroRefinement20261017");
        }
    }
    static List<String[]> manifest(String kind, String crate) throws Exception {
        return resource("/com/synexia/rewrite/hash-pinned-" + kind + "/" + crate + "/manifest.tsv").lines()
                .filter(x -> !x.isBlank() && !x.startsWith("#")).map(x -> x.split("\t")).toList();
    }
    static String resource(String path) throws Exception {
        try (var input = MaskRecipeSupport.class.getResourceAsStream(path)) {
            assertNotNull(input, path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    static List<SourceFile> parse(Map<String, String> texts) {
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        var java = texts.entrySet().stream().filter(e -> e.getKey().endsWith(".java"))
                .map(e -> Parser.Input.fromString(Path.of(e.getKey()), e.getValue())).toList();
        List<SourceFile> result = new ArrayList<>();
        if (!java.isEmpty()) {
            result.addAll(JavaParser.fromJavaVersion().build().parseInputs(java, null, context).toList());
            assertTrue(errors.isEmpty(), errors.toString());
            for (SourceFile file : result) {
                assertInstanceOf(J.CompilationUnit.class, file);
                assertEquals(texts.get(file.getSourcePath().toString()), file.printAll());
            }
        }
        texts.forEach((path, text) -> {
            if (!path.endsWith(".java")) result.add(PlainText.builder().sourcePath(Path.of(path)).text(text).build());
        });
        return result;
    }
    static List<SourceFile> inputs(List<String[]> rows, Path root) throws Exception {
        var texts = new TreeMap<String, String>();
        for (String[] row : rows) if (Files.exists(root.resolve(row[0]))) texts.put(row[0], Files.readString(root.resolve(row[0])));
        return parse(texts);
    }
    static List<SourceFile> run(Recipe recipe, List<SourceFile> before) {
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        context.putMessage("com.synexia.rewrite.hashPinnedJavaSnapshotRecipe.parserClasspath", JavaParser.runtimeClasspath());
        var results = recipe.run(new InMemoryLargeSourceSet(before), context).getChangeset().getAllResults();
        if (!errors.isEmpty()) throw new IllegalStateException("recipe refused inputs", errors.getFirst());
        return results.stream().map(result -> result.getAfter()).toList();
    }
    static List<SourceFile> merge(List<SourceFile> before, List<SourceFile> changes) {
        List<SourceFile> result = new ArrayList<>(before);
        result.removeIf(a -> changes.stream().anyMatch(b -> a.getSourcePath().equals(b.getSourcePath())));
        result.addAll(changes);
        return result;
    }
    static void write(Path root, List<SourceFile> files) throws Exception {
        for (SourceFile file : files) {
            Path path = root.resolve(file.getSourcePath()).normalize();
            assertTrue(path.startsWith(root.normalize()));
            Files.createDirectories(path.getParent());
            Files.writeString(path, file.printAll());
        }
    }
    static Path output() { return Path.of(System.getProperty("m3.mask.output", "target/sdk")); }
    static String hash(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
    }
}
