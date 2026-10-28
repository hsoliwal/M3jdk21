// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
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
import java.util.TreeMap;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;

/** Shared bounded SDK proof for source-only, sealed java.base owner recipes. */
final class M3SourceOnlyRecipeProofSupport {
    private M3SourceOnlyRecipeProofSupport() { }
    static String resource(String path) throws Exception {
        try (var input = M3SourceOnlyRecipeProofSupport.class.getResourceAsStream(path)) {
            assertNotNull(input, path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    static String sha(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(text.getBytes(StandardCharsets.UTF_8)));
    }
    static Map<String, String> sources(String crate, boolean after) throws Exception {
        var result = new TreeMap<String, String>();
        String root = "/com/synexia/rewrite/hash-pinned-java/" + crate + "/";
        for (String line : resource(root + "manifest.tsv").lines().toList()) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] row = line.split("\t", -1);
            assertEquals(4, row.length);
            String text = resource(root + (after ? "" : "before-") + row[3]);
            assertEquals(row[after ? 2 : 1], sha(text), row[0]);
            result.put(row[0], text);
        }
        return result;
    }
    static InMemoryExecutionContext context() {
        var context = new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
        context.putMessage("com.synexia.rewrite.hashPinnedJavaSnapshotRecipe.parserClasspath", List.of());
        return context;
    }
    static List<SourceFile> parse(Map<String, String> sources) {
        var parser = JavaParser.fromJavaVersion().classpath(List.of()).build();
        var inputs = sources.entrySet().stream()
                .map(e -> Parser.Input.fromString(Path.of(e.getKey()), e.getValue())).toList();
        var parsed = parser.parseInputs(inputs, null, context()).toList();
        assertEquals(sources.size(), parsed.size());
        for (SourceFile file : parsed) {
            assertInstanceOf(J.CompilationUnit.class, file);
            assertEquals(sources.get(file.getSourcePath().toString()), file.printAll());
        }
        return new ArrayList<>(parsed);
    }
    static Map<String, String> run(Recipe recipe, List<SourceFile> input) {
        var result = new TreeMap<String, String>();
        for (var change : recipe.run(new InMemoryLargeSourceSet(input), context(), 1)
                .getChangeset().getAllResults()) {
            assertNotNull(change.getAfter());
            result.put(change.getAfter().getSourcePath().toString(), change.getAfter().printAll());
        }
        return result;
    }
    static Map<String, String> generate(String crate, Recipe recipe) throws Exception {
        var generated = run(recipe, parse(sources(crate, false)));
        assertEquals(sources(crate, true), generated);
        Path target = Path.of(System.getProperty("m3.candidate.root"));
        for (var e : generated.entrySet()) {
            Path output = target.resolve(e.getKey());
            Files.createDirectories(output.getParent());
            Files.writeString(output, e.getValue());
        }
        return generated;
    }
    static String syntax(String source) {
        SourceFile parsed = JavaParser.fromJavaVersion().classpath(List.of()).build()
                .parse(context(), source).findFirst().orElseThrow();
        assertInstanceOf(J.CompilationUnit.class, parsed);
        var stripped = new JavaIsoVisitor<Void>() {
            @Override public Space visitSpace(Space space, Space.Location location, Void unused) {
                return Space.EMPTY;
            }
        }.visit((J.CompilationUnit) parsed, null);
        assertNotNull(stripped);
        return ((J.CompilationUnit) stripped).printAll();
    }
    static void refuseChanges(String crate, Recipe recipe) throws Exception {
        var baseline = sources(crate, false);
        assertThrows(IllegalStateException.class, () -> run(recipe, List.of()));
        for (String path : baseline.keySet()) {
            var drift = new TreeMap<>(baseline);
            drift.put(path, drift.get(path) + "\n/* unreviewed drift */\n");
            assertThrows(IllegalStateException.class, () -> run(recipe, parse(drift)), path);
            var missing = new TreeMap<>(baseline);
            missing.remove(path);
            assertThrows(IllegalStateException.class, () -> run(recipe, parse(missing)), path);
        }
        var duplicate = parse(baseline);
        duplicate.add(duplicate.getFirst());
        assertThrows(IllegalStateException.class, () -> run(recipe, duplicate));
        var unrelated = new TreeMap<>(baseline);
        unrelated.put("src/main/java/Unrelated.java",
                "class Unrelated { String value = \"/* literal */ // kept\"; }\n");
        assertEquals(sources(crate, true), run(recipe, parse(unrelated)));
        var core = new M3HashPinnedJavaSnapshotRecipe(crate);
        var ctx = context();
        var inventory = core.getInitialValue(ctx);
        var original = parse(baseline);
        for (SourceFile file : original) core.getScanner(inventory).visit(file, ctx);
        String first = baseline.keySet().iterator().next();
        var changed = new TreeMap<>(baseline);
        changed.put(first, changed.get(first) + "\n/* drift after scan */\n");
        var late = assertThrows(org.openrewrite.internal.RecipeRunException.class,
                () -> core.getVisitor(inventory).visit(parse(changed).getFirst(), ctx));
        assertInstanceOf(IllegalStateException.class, late.getCause());
        assertTrue(late.getCause().getMessage().contains("target changed after scan"));
        String resourceRoot = "/com/synexia/rewrite/hash-pinned-java/" + crate + "/";
        String resourceName = resource(resourceRoot + "manifest.tsv").lines()
                .filter(line -> !line.isBlank() && !line.startsWith("#")).findFirst().orElseThrow()
                .split("\t", -1)[3];
        Path template = Path.of(M3SourceOnlyRecipeProofSupport.class.getResource(
                resourceRoot + resourceName).toURI());
        String text = Files.readString(template);
        try {
            Files.writeString(template, text + "\n/* resource drift */\n");
            assertThrows(IllegalStateException.class, () -> core.getInitialValue(ctx));
        } finally {
            Files.writeString(template, text);
        }
    }
    static void external(String script) throws Exception {
        Path crate = Path.of(System.getProperty("m3.crate.root"));
        Path log = crate.resolve("target").resolve(script + ".log");
        Files.createDirectories(log.getParent());
        var cmd = List.of("python3", crate.resolve("contract").resolve(script).toString(),
                "--jdk", System.getProperty("m3.jdk.home"),
                "--candidate", System.getProperty("m3.candidate.root"));
        Process child = new ProcessBuilder(cmd).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        assertTrue(child.waitFor(600, java.util.concurrent.TimeUnit.SECONDS), "compiler proof timed out");
        assertEquals(0, child.exitValue(), Files.readString(log));
    }
}
