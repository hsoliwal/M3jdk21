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

/** Exact-current-input repair of two invalid Java literals and the foundation Maven environment. */
final class JccCiRepairTest {
    private static final String CRATE = "jcc-ci-repair-20261006";
    private static final String ROOT = "/com/m3/rewrite/backport/jdk21-hash-pinned-text/" + CRATE + "/";
    private record Target(String path, String before, String after, String beforeHash, String afterHash) {}
    private static final List<Target> TARGETS = List.of(
            new Target(".github/workflows/m3-foundation.yml",
                    "before-00-m3-foundation.yml.txt", "after-00-m3-foundation.yml.txt",
                    "2a81b9ebfa5d477977dba4632a0e7c89f2ebb89606eb46491197536335d63fee",
                    "76f3b96be528f33556fde14d811e3df9462f828353967237eed1a46ccc6ce91f"),
            new Target("m3/tooling/migration-recipes/src/test/java/com/m3/rewrite/M3A3RegexMemoryLabDeliveryRecipeTest.java",
                    "before-01-M3A3RegexMemoryLabDeliveryRecipeTest.java.txt",
                    "after-01-M3A3RegexMemoryLabDeliveryRecipeTest.java.txt",
                    "58d01008f3dc6e79d988e855d76ba90eb0a13d39a76e7eb4027d75442389ec94",
                    "f34470a701cf71b4b4abbe1fa9a5091392a930426ec84d700f13e53edd244629"),
            new Target("m3/tooling/migration-recipes/src/test/java/com/m3/rewrite/M3A3RegexMemoryWorkflowRecipeTest.java",
                    "before-02-M3A3RegexMemoryWorkflowRecipeTest.java.txt",
                    "after-02-M3A3RegexMemoryWorkflowRecipeTest.java.txt",
                    "69882638771a411cf0866f789a7b3ef7e7e7040cde38a1c40ef4a743d37168e2",
                    "3f3ab065d9e87e9f6095629a56b052642ec9d0883a466430e90a4a4355edd05b"));

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
        String destination = System.getProperty("m3.ci.materialized");
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
                .activateRecipes("com.m3.rewrite.backport.JccCiRepair");
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
    void exactBrokenJavaInputsFailParsingAndOneByteRepairsPreserveAllOtherText() throws IOException {
        String slash = Character.toString((char) 92);
        String broken = ".replace('" + slash + "', '/')";
        String repaired = ".replace('" + slash + slash + "', '/')";
        for (Target target : TARGETS) {
            if (!target.path().endsWith(".java")) continue;
            String before = resource(target.before());
            String after = resource(target.after());
            assertEquals(1, occurrences(before, broken));
            assertEquals(before.replace(broken, repaired), after);
            assertEquals(before.getBytes(StandardCharsets.UTF_8).length + 1,
                    after.getBytes(StandardCharsets.UTF_8).length);
            assertEquals(2, occurrences(before, "@Test"));
            assertEquals(2, occurrences(after, "@Test"));
            List<Diagnostic<? extends JavaFileObject>> rejected = parse(target.path(), before);
            assertTrue(rejected.stream().anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR
                    && d.getCode().contains("unclosed.char.lit")), rejected.toString());
            assertTrue(parse(target.path(), after).stream().noneMatch(d -> d.getKind() == Diagnostic.Kind.ERROR));
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
        try (var input = JccCiRepairTest.class.getResourceAsStream(ROOT + name)) {
            assertNotNull(input, name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
