// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.source.util.JavacTask;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.RecipeSerializer;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.marker.BuildMetadata;
import org.openrewrite.marker.RecipesThatMadeChanges;
import org.openrewrite.text.PlainText;

/** Exact-current-input repair of two nested quotes and their bound Java-template hash. */
final class A3CodeStringRepairTest {
    private static final String CRATE = "a3-code-string-repair-20261006";
    private static final String ROOT = "/com/m3/rewrite/backport/jdk21-hash-pinned-text/" + CRATE + "/";
    private record Target(String path, String before, String after, String beforeHash, String afterHash) {}
    private static final List<Target> TARGETS = List.of(
            new Target("m3/tooling/a3/src/main/java/com/m3/a3/A3RegexMatrix.java",
                    "before-00-A3RegexMatrix.java.txt", "after-00-A3RegexMatrix.java.txt",
                    "f000b1eea411056baaca6051b220149f25334bc70bf69b4f0536839c6c78e404", "19f7e3fb8c5f4332ab721d1962ab9cac261c524a10d9d91272346636fbe86ada"),
            new Target("m3/tooling/migration-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/a3-regex-memory-lab/A3RegexMatrix.java.after",
                    "before-01-A3RegexMatrix.java.after.txt", "after-01-A3RegexMatrix.java.after.txt",
                    "f000b1eea411056baaca6051b220149f25334bc70bf69b4f0536839c6c78e404", "19f7e3fb8c5f4332ab721d1962ab9cac261c524a10d9d91272346636fbe86ada"),
            new Target("m3/tooling/migration-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/a3-regex-memory-lab/manifest.tsv",
                    "before-02-manifest.tsv.txt", "after-02-manifest.tsv.txt",
                    "b2febbb17ed96ae6f582eee2e16af3fc5e435c23e350708f56397bee9ac38ab5", "e92d3c16a897a02084ca749d092a02402c2f1d0225bcb9c8755c0304f838dabd"));

    @Test
    void retainedOwnerProducesExactlyThreeOutputsAndPreservesIdentityAtFixedPoint() throws IOException {
        var before = beforeSources();
        var inputMetadata = new BuildMetadata(UUID.fromString("c55e9507-c3f5-40b8-a847-d422ec3094ef"),
                Map.of("m3-ci-fixture", "metadata-preservation"));
        before.replaceAll(source -> source.withMarkers(source.getMarkers().add(inputMetadata)));
        SourceFile sentinel = text("unrelated/source.java.txt", "unchanged sentinel\n");
        before.add(sentinel);
        var errors = new ArrayList<Throwable>();
        var run = recipe().run(new InMemoryLargeSourceSet(before), new InMemoryExecutionContext(errors::add), 1);
        assertTrue(errors.isEmpty(), errors.toString());
        var changes = run.getChangeset().getAllResults();
        assertEquals(3, changes.size());
        var materialized = new TreeMap<String, String>();
        var after = new ArrayList<SourceFile>();
        for (var change : changes) {
            SourceFile old = assertInstanceOf(PlainText.class, change.getBefore());
            SourceFile next = assertInstanceOf(PlainText.class, change.getAfter());
            assertEquals(old.getId(), next.getId());
            assertEquals(old.getSourcePath(), next.getSourcePath());
            assertEquals(old.getMarkers(), next.getMarkers().removeByType(RecipesThatMadeChanges.class));
            var provenance = next.getMarkers().findAll(RecipesThatMadeChanges.class);
            assertEquals(1, provenance.size());
            assertEquals(1, provenance.get(0).getRecipes().size());
            var appliedStack = provenance.get(0).getRecipes().iterator().next();
            assertEquals(1, appliedStack.size());
            var applied = assertInstanceOf(M3Jdk21HashPinnedTextSnapshotRecipe.class, appliedStack.get(0));
            assertEquals(CRATE, applied.getCrateName());
            assertEquals(old.getCharset(), next.getCharset());
            assertEquals(old.isCharsetBomMarked(), next.isCharsetBomMarked());
            assertEquals(old.getFileAttributes(), next.getFileAttributes());
            assertTrue(materialized.put(normalized(next), next.printAll()) == null);
            after.add(next);
        }
        assertEquals(expectedAfter(), materialized);
        after.add(sentinel);
        var replayErrors = new ArrayList<Throwable>();
        var replay = recipe().run(new InMemoryLargeSourceSet(after), new InMemoryExecutionContext(replayErrors::add), 1);
        assertTrue(replayErrors.isEmpty(), replayErrors.toString());
        assertTrue(replay.getChangeset().getAllResults().isEmpty());
        assertEquals("unchanged sentinel\n", sentinel.printAll());

        // The optional proof directory receives actual OpenRewrite results, never resource copies.
        String destination = System.getProperty("m3.a3.repair.materialized");
        if (destination != null && !destination.isBlank()) {
            Path root = Path.of(destination);
            for (var entry : materialized.entrySet()) {
                Path path = root.resolve(entry.getKey());
                Files.createDirectories(path.getParent());
                Files.writeString(path, entry.getValue(), StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            }
        }
    }

    @Test
    void namedMavenEntryAndSerializedOwnerProduceTheSameOutputs() throws IOException {
        var serializer = new RecipeSerializer();
        var restored = assertInstanceOf(M3Jdk21HashPinnedTextSnapshotRecipe.class,
                serializer.read(serializer.write(recipe())));
        assertEquals(CRATE, restored.getCrateName());
        Recipe named = Environment.builder().scanYamlResources().build()
                .activateRecipes("com.m3.rewrite.backport.A3CodeStringRepair");
        for (Recipe entry : List.of(restored, named)) {
            var errors = new ArrayList<Throwable>();
            var run = entry.run(new InMemoryLargeSourceSet(beforeSources()),
                    new InMemoryExecutionContext(errors::add), 1);
            assertTrue(errors.isEmpty(), errors.toString());
            var observed = new TreeMap<String, String>();
            for (var change : run.getChangeset().getAllResults()) {
                assertNotNull(change.getBefore());
                SourceFile after = assertInstanceOf(PlainText.class, change.getAfter());
                assertTrue(observed.put(normalized(after), after.printAll()) == null);
            }
            assertEquals(expectedAfter(), observed);
        }
    }

    @Test
    void allMissingTargetsRefuseWithoutPartialOutputs() throws IOException {
        for (Target target : TARGETS) {
            var sources = beforeSources();
            sources.removeIf(s -> normalized(s).equals(target.path()));
            reject(sources, "required JDK21 text source missing: " + target.path());
        }
    }

    @Test
    void allDriftedTargetsRefuseWithoutPartialOutputs() throws IOException {
        for (Target target : TARGETS) {
            var sources = beforeSources();
            sources.removeIf(s -> normalized(s).equals(target.path()));
            sources.add(text(target.path(), resource(target.before()) + "// external drift\n"));
            reject(sources, "source drift: " + target.path());
        }
    }

    @Test
    void duplicateTargetsRefuseEvenWhenBothCopiesAreAdmittedImages() throws IOException {
        for (Target target : TARGETS) {
            var sources = beforeSources();
            sources.add(text(target.path(), resource(target.after())));
            reject(sources, "duplicate target: " + target.path());
        }
    }

    @Test
    void changedAfterScanTargetsRefuse() throws IOException {
        for (Target target : TARGETS) {
            var owner = recipe();
            var context = new InMemoryExecutionContext(failure -> {
                throw new AssertionError("Unexpected scan failure", failure);
            });
            var inventory = owner.getInitialValue(context);
            var scanner = owner.getScanner(inventory);
            for (SourceFile source : beforeSources()) scanner.visit(source, context);
            assertTrue(owner.generate(inventory, context).isEmpty());
            var visitor = owner.getVisitor(inventory);
            RuntimeException failure = assertThrows(RuntimeException.class,
                    () -> visitor.visit(text(target.path(), "changed after scan\n"), context));
            assertTrue(messages(failure).contains("JDK21 text target changed after scan: " + target.path()));
        }
    }

    @Test
    void onlyNestedQuoteEscapesAndTheirManifestHashChange() throws IOException {
        String quote = Character.toString((char) 34);
        String slash = Character.toString((char) 92);
        String broken = quote + "Pattern.compile(" + quote + "a+b?" + quote + ")" + quote;
        String repaired = quote + "Pattern.compile(" + slash + quote + "a+b?" + slash + quote + ")" + quote;
        for (int i = 0; i < 2; i++) {
            Target target = TARGETS.get(i);
            String before = resource(target.before()), after = resource(target.after());
            assertEquals(1, occurrences(before, broken));
            assertEquals(before.replace(broken, repaired), after);
            assertEquals(before.getBytes(StandardCharsets.UTF_8).length + 2,
                    after.getBytes(StandardCharsets.UTF_8).length);
            assertTrue(parse("A3RegexMatrix.java", before).stream()
                    .anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR));
            assertTrue(parse("A3RegexMatrix.java", after).stream()
                    .noneMatch(d -> d.getKind() == Diagnostic.Kind.ERROR));
        }
        assertEquals(resource(TARGETS.get(0).after()), resource(TARGETS.get(1).after()));
        String beforeManifest = resource(TARGETS.get(2).before());
        assertEquals(1, occurrences(beforeManifest, TARGETS.get(0).beforeHash()));
        assertEquals(beforeManifest.replace(TARGETS.get(0).beforeHash(), TARGETS.get(0).afterHash()),
                resource(TARGETS.get(2).after()));
    }

    @Test
    void everyBeforeAfterCombinationConvergesToTheSameThreeBoundFiles() throws IOException {
        for (int mask = 0; mask < 8; mask++) {
            var sources = new ArrayList<SourceFile>();
            var observed = new TreeMap<String, String>();
            for (int i = 0; i < TARGETS.size(); i++) {
                Target target = TARGETS.get(i);
                String value = resource((mask & (1 << i)) == 0 ? target.before() : target.after());
                sources.add(text(target.path(), value));
                observed.put(target.path(), value);
            }
            var errors = new ArrayList<Throwable>();
            var run = recipe().run(new InMemoryLargeSourceSet(sources),
                    new InMemoryExecutionContext(errors::add), 1);
            assertTrue(errors.isEmpty(), errors.toString());
            assertEquals(3 - Integer.bitCount(mask), run.getChangeset().getAllResults().size());
            for (var result : run.getChangeset().getAllResults()) {
                SourceFile after = assertInstanceOf(PlainText.class, result.getAfter());
                observed.put(normalized(after), after.printAll());
            }
            assertEquals(expectedAfter(), observed);
        }
    }

    @Test
    void manifestAdmitsOnlyTheThreeExactCurrentPreimagesAndExpectedOutputs() throws IOException {
        var known = new TreeMap<String, Target>();
        for (Target target : TARGETS) {
            known.put(target.path(), target);
            assertEquals(target.beforeHash(), hash(resource(target.before())));
            assertEquals(target.afterHash(), hash(resource(target.after())));
        }
        var seen = new HashSet<String>();
        String previous = "";
        for (String line : resource("manifest.tsv").lines().toList()) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] cells = line.split("\t", -1);
            assertEquals(4, cells.length);
            assertTrue(previous.compareTo(cells[0]) < 0);
            previous = cells[0];
            assertTrue(seen.add(cells[0]));
            Target target = known.get(cells[0]);
            assertNotNull(target, cells[0]);
            assertEquals(target.beforeHash(), cells[1]);
            assertEquals(target.afterHash(), cells[2]);
            assertEquals(target.after(), cells[3]);
        }
        assertEquals(known.keySet(), seen);
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + needle.length())) count++;
        return count;
    }

    private static List<Diagnostic<? extends JavaFileObject>> parse(String path, String source) throws IOException {
        var compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "A full JDK is required");
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        JavaFileObject input = new SimpleJavaFileObject(
                URI.create("string:///" + Path.of(path).getFileName()), JavaFileObject.Kind.SOURCE) {
            @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return source;
            }
        };
        try (var manager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            var task = (JavacTask) compiler.getTask(null, manager, diagnostics,
                    List.of("--release", "21", "-proc:none"), null, List.of(input));
            task.parse();
        }
        return List.copyOf(diagnostics.getDiagnostics());
    }

    private static ArrayList<SourceFile> beforeSources() throws IOException {
        var sources = new ArrayList<SourceFile>();
        for (Target target : TARGETS) sources.add(text(target.path(), resource(target.before())));
        return sources;
    }

    private static Map<String, String> expectedAfter() throws IOException {
        var result = new TreeMap<String, String>();
        for (Target target : TARGETS) result.put(target.path(), resource(target.after()));
        return result;
    }

    private static void reject(List<SourceFile> sources, String reason) {
        var errors = new ArrayList<Throwable>();
        try {
            var run = recipe().run(new InMemoryLargeSourceSet(sources), new InMemoryExecutionContext(errors::add), 1);
            assertTrue(run.getChangeset().getAllResults().isEmpty(), "Refusal must not emit partial results");
        } catch (RuntimeException failure) {
            errors.add(failure);
        }
        assertFalse(errors.isEmpty(), reason);
        assertTrue(errors.stream().anyMatch(failure -> messages(failure).contains(reason)), errors.toString());
    }

    private static String messages(Throwable failure) {
        var result = new StringBuilder();
        for (int depth = 0; failure != null && depth < 32; depth++, failure = failure.getCause())
            result.append(failure.getMessage()).append('\n');
        return result.toString();
    }

    private static String normalized(SourceFile source) {
        return source.getSourcePath().toString().replace('\\', '/');
    }

    private static PlainText text(String path, String value) {
        return PlainText.builder().sourcePath(Path.of(path)).text(value).build();
    }

    private static M3Jdk21HashPinnedTextSnapshotRecipe recipe() {
        return new M3Jdk21HashPinnedTextSnapshotRecipe(CRATE);
    }

    private static String hash(String value) {
        return M3Jdk21HashPinnedTextSnapshotRecipe.sha256(value);
    }

    private static String resource(String name) throws IOException {
        try (var input = A3CodeStringRepairTest.class.getResourceAsStream(ROOT + name)) {
            assertNotNull(input, name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
