// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.rewrite.scope.M3ContractMode;
import com.m3.rewrite.scope.M3EditScope;
import com.m3.rewrite.scope.M3RecipeScopeRegistry;
import com.sun.source.util.JavacTask;
import java.io.IOException;
import java.net.URI;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.text.PlainText;

final class M3Jep458BackportRecipeTest {
    private static final String JAVA_ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned/jdk22-jep458-multifile-java/";
    private static final String TEXT_ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned-text/jdk22-jep458-multifile-text/";
    private static final String PRE_ROOT =
            "/com/m3/rewrite/backport/jdk22-jep458-current/pre/";

    private static final Map<String, String> JAVA_PRE = Map.ofEntries(
            Map.entry("src/jdk.compiler/share/classes/com/sun/tools/javac/code/Preview.java",
                    "01-Preview.java.before.txt"),
            Map.entry("src/jdk.compiler/share/classes/com/sun/tools/javac/main/Option.java",
                    "12-Option.java.before.txt"),
            Map.entry("test/langtools/tools/javac/launcher/GetResourceTest.java",
                    "14-GetResourceTest.java.before.txt"),
            Map.entry("test/langtools/tools/javac/launcher/SourceLauncherTest.java",
                    "19-SourceLauncherTest.java.before.txt"),
            Map.entry("test/langtools/tools/jdeps/listdeps/ListModuleDeps.java",
                    "21-ListModuleDeps.java.before.txt"));

    private static final Map<String, String> TEXT_PRE = Map.ofEntries(
            Map.entry("src/java.base/share/classes/module-info.java",
                    "00-module-info.java.before.txt"),
            Map.entry("make/CompileInterimLangtools.gmk",
                    "00-CompileInterimLangtools.gmk.before.txt"),
            Map.entry("src/java.base/share/classes/sun/launcher/resources/launcher.properties",
                    "01-java-base-launcher.properties.before.txt"),
            Map.entry("src/java.base/share/native/libjli/java.c",
                    "02-java.c.before.txt"),
            Map.entry("src/jdk.compiler/share/classes/com/sun/tools/javac/resources/launcher.properties",
                    "03-jdk-compiler-launcher.properties.before.txt"));

    @Test
    void compositeRecipePinsDonorCratesAndPublicContractAuthority() {
        Recipe recipe = new M3Jep458BackportRecipe();

        assertEquals(
                "517b1788198fc325961df61161f9b365c7b2524e",
                M3Jep458BackportRecipe.UPSTREAM_COMMIT);
        assertEquals(2, recipe.getRecipeList().size());

        var javaRecipe = assertInstanceOf(
                M3Jdk21HashPinnedSnapshotRecipe.class, recipe.getRecipeList().get(0));
        var textRecipe = assertInstanceOf(
                M3Jdk21HashPinnedTextSnapshotRecipe.class, recipe.getRecipeList().get(1));

        assertEquals("jdk22-jep458-multifile-java", javaRecipe.getCrateName());
        assertEquals("jdk22-jep458-multifile-text", textRecipe.getCrateName());
        assertEquals(
                M3EditScope.LIBRARY_API,
                M3RecipeScopeRegistry.require(M3Jep458BackportRecipe.class).minimumScope());
        assertEquals(
                M3ContractMode.EXPLICIT_CONTRACT_CHANGE,
                M3RecipeScopeRegistry.require(M3Jep458BackportRecipe.class).contractMode());
        assertEquals(1, recipe.maxCycles());
        assertTrue(recipe.getTags().contains("multi-module"));
        assertTrue(recipe.getTags().contains("hash-pinned"));
    }

    @Test
    void exactCurrentMasterPreimagesReplayAllTwentySixTargetsThenReachFixedPoint()
            throws Exception {
        M3Jep458BackportRecipe recipe = new M3Jep458BackportRecipe();
        List<SourceFile> before = baseline();

        var first = recipe.run(new InMemoryLargeSourceSet(before), context(), 1);
        List<Result> changes = first.getChangeset().getAllResults();
        assertEquals(26, changes.size());

        Map<String, SourceFile> after = new TreeMap<>();
        before.forEach(source -> after.put(path(source), source));
        for (Result change : changes) {
            SourceFile result = change.getAfter();
            if (result != null) {
                after.put(path(result), result);
            }
        }

        for (ManifestRow row : manifest(JAVA_ROOT)) {
            assertEquals(resource(JAVA_ROOT + row.resource()), after.get(row.path()).printAll());
        }
        for (ManifestRow row : manifest(TEXT_ROOT)) {
            assertEquals(resource(TEXT_ROOT + row.resource()), after.get(row.path()).printAll());
        }

        assertTrue(
                recipe.run(new InMemoryLargeSourceSet(new ArrayList<>(after.values())), context(), 1)
                        .getChangeset()
                        .getAllResults()
                        .isEmpty());
    }

    @Test
    void driftedCurrentMasterJavaPreimageFailsClosed() throws Exception {
        List<SourceFile> sources = baseline();
        String target = "src/jdk.compiler/share/classes/com/sun/tools/javac/code/Preview.java";
        int index = indexOf(sources, target);
        SourceFile current = sources.get(index);
        sources.set(index, parseJava(target, current.printAll() + "\n// drift\n"));

        assertThrows(
                RuntimeException.class,
                () -> new M3Jep458BackportRecipe()
                        .run(new InMemoryLargeSourceSet(sources), context(), 1)
                        .getChangeset()
                        .getAllResults());
    }

    @Test
    void reviewedPostimagesAreAlreadyACompositeFixedPoint() throws Exception {
        List<SourceFile> sources = new ArrayList<>();
        for (ManifestRow row : manifest(JAVA_ROOT)) {
            sources.add(parseJava(row.path(), resource(JAVA_ROOT + row.resource())));
        }
        for (ManifestRow row : manifest(TEXT_ROOT)) {
            sources.add(text(row.path(), resource(TEXT_ROOT + row.resource())));
        }

        assertTrue(
                new M3Jep458BackportRecipe()
                        .run(new InMemoryLargeSourceSet(sources), context(), 1)
                        .getChangeset()
                        .getAllResults()
                        .isEmpty());
    }

    @Test
    void descriptorLaneRetainsAllTargetsAndJavacModuleSyntax() throws Exception {
        assertEquals(21, manifest(JAVA_ROOT).size());
        assertEquals(5, manifest(TEXT_ROOT).size());
        String path = "src/java.base/share/classes/module-info.java";
        var descriptor = manifest(TEXT_ROOT).stream()
                .filter(row -> row.path().equals(path)).findFirst().orElseThrow();
        assertTrue(manifest(JAVA_ROOT).stream().noneMatch(row -> row.path().equals(path)));
        for (String source : List.of(resource(PRE_ROOT + "00-module-info.java.before.txt"),
                resource(TEXT_ROOT + descriptor.resource()))) {
            var diagnostics = new DiagnosticCollector<JavaFileObject>();
            var compiler = ToolProvider.getSystemJavaCompiler();
            try (var files = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
                var input = new SimpleJavaFileObject(URI.create("string:///module-info.java"),
                        JavaFileObject.Kind.SOURCE) {
                    @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                        return source;
                    }
                };
                var task = (JavacTask) compiler.getTask(null, files, diagnostics,
                        List.of("--release", "21", "-proc:none"), null, List.of(input));
                var unit = task.parse().iterator().next();
                assertEquals("java.base", unit.getModule().getName().toString());
                assertTrue(diagnostics.getDiagnostics().stream()
                        .noneMatch(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR),
                        diagnostics.getDiagnostics().toString());
            }
        }
    }

    @Test
    void descriptorTextDriftStillFailsClosed() throws Exception {
        List<SourceFile> sources = baseline();
        String path = "src/java.base/share/classes/module-info.java";
        int index = indexOf(sources, path);
        sources.set(index, text(path, sources.get(index).printAll() + "\n// drift\n"));
        assertThrows(RuntimeException.class, () -> new M3Jep458BackportRecipe()
                .run(new InMemoryLargeSourceSet(sources), context(), 1)
                .getChangeset().getAllResults());
    }

    private static List<SourceFile> baseline() throws Exception {
        List<SourceFile> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : new TreeMap<>(JAVA_PRE).entrySet()) {
            result.add(parseJava(entry.getKey(), resource(PRE_ROOT + entry.getValue())));
        }
        for (Map.Entry<String, String> entry : new TreeMap<>(TEXT_PRE).entrySet()) {
            result.add(text(entry.getKey(), resource(PRE_ROOT + entry.getValue())));
        }
        return result;
    }

    private static SourceFile parseJava(String path, String source) {
        List<SourceFile> parsed =
                JavaParser.fromJavaVersion()
                        .build()
                        .parseInputs(
                                List.of(Parser.Input.fromString(Path.of(path), source)),
                                null,
                                context())
                        .toList();
        if (parsed.size() != 1) {
            throw new IllegalStateException("unexpected Java parse count: " + path);
        }
        assertInstanceOf(J.CompilationUnit.class, parsed.getFirst());
        assertEquals(source, parsed.getFirst().printAll());
        return parsed.getFirst();
    }

    private static PlainText text(String path, String source) {
        return PlainText.builder().sourcePath(Path.of(path)).text(source).build();
    }

    private static int indexOf(List<SourceFile> sources, String target) {
        for (int index = 0; index < sources.size(); index++) {
            if (path(sources.get(index)).equals(target)) return index;
        }
        throw new IllegalArgumentException(target);
    }

    private static List<ManifestRow> manifest(String root) throws IOException {
        List<ManifestRow> rows = new ArrayList<>();
        for (String line : resource(root + "manifest.tsv").lines().toList()) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] cells = line.split("\t", -1);
            rows.add(new ManifestRow(cells[0], cells[1], cells[2], cells[3]));
        }
        return List.copyOf(rows);
    }

    private record ManifestRow(String path, String before, String after, String resource) {}

    private static String resource(String name) throws IOException {
        try (var input = M3Jep458BackportRecipeTest.class.getResourceAsStream(name)) {
            if (input == null) throw new IOException("missing resource " + name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> {
            throw new IllegalStateException(error);
        });
    }

    private static String path(SourceFile source) {
        return source.getSourcePath().toString().replace('\\', '/');
    }
}
