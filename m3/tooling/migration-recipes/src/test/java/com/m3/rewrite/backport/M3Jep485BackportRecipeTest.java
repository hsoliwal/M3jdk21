// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.rewrite.scope.M3ContractMode;
import com.m3.rewrite.scope.M3EditScope;
import com.m3.rewrite.scope.M3RecipeScopeRegistry;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

final class M3Jep485BackportRecipeTest {
    private static final String ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned/"
                    + M3Jep485BackportRecipe.CRATE
                    + "/";

    @Test
    void wrapperDeclaresFinalJep485CrateAndExplicitLibraryApiAuthority() {
        Recipe recipe = new M3Jep485BackportRecipe();
        assertEquals(1, recipe.getRecipeList().size());
        var snapshot = assertInstanceOf(
                M3Jdk21HashPinnedSnapshotRecipe.class, recipe.getRecipeList().getFirst());
        assertEquals(M3Jep485BackportRecipe.CRATE, snapshot.getCrateName());
        assertEquals(1, recipe.maxCycles());
        assertTrue(recipe.getTags().contains("jep-485"));
        assertTrue(recipe.getTags().contains("library-api"));

        var policy = M3RecipeScopeRegistry.require(M3Jep485BackportRecipe.class);
        assertEquals(M3EditScope.LIBRARY_API, policy.minimumScope());
        assertEquals(M3ContractMode.EXPLICIT_CONTRACT_CHANGE, policy.contractMode());
        assertFalse(policy.fileLocalMechanical(
                List.of("src/java.base/share/classes/java/util/stream/Stream.java")));

        Recipe named = Environment.builder().scanYamlResources().build()
                .activateRecipes("com.m3.rewrite.backport.Jep485");
        assertEquals(1, named.getRecipeList().size());
        assertInstanceOf(M3Jep485BackportRecipe.class, named.getRecipeList().getFirst());
    }

    @Test
    void manifestPinsFourExistingOwnersAndElevenAdditions() throws Exception {
        List<ManifestRow> rows = manifest();
        assertEquals(15, rows.size());
        assertEquals(4, rows.stream().filter(row -> !"ABSENT".equals(row.before())).count());
        assertEquals(11, rows.stream().filter(row -> "ABSENT".equals(row.before())).count());
        assertEquals(
                List.of(
                        "src/java.base/share/classes/java/util/stream/Gatherer.java",
                        "src/java.base/share/classes/java/util/stream/GathererOp.java",
                        "src/java.base/share/classes/java/util/stream/Gatherers.java"),
                rows.stream()
                        .filter(row -> row.path().startsWith(
                                "src/java.base/share/classes/java/util/stream/Gather"))
                        .map(ManifestRow::path)
                        .toList());
        assertTrue(rows.stream().anyMatch(row ->
                row.path().endsWith("GatherersMapConcurrentTest.java")));
        assertTrue(rows.stream().anyMatch(row ->
                row.path().endsWith("GathererShortCircuitTest.java")));
    }

    @Test
    void exactCurrentCheckoutReplaysAllFifteenTargetsThenReachesFixedPoint()
            throws Exception {
        Path repository = repositoryRoot();
        List<SourceFile> before = currentPreimages(repository);
        assertEquals(4, before.size());

        M3Jep485BackportRecipe recipe = new M3Jep485BackportRecipe();
        List<Result> changes = recipe.run(
                        new InMemoryLargeSourceSet(before), context(), 1)
                .getChangeset()
                .getAllResults();
        assertEquals(15, changes.size());

        Map<String, SourceFile> after = new TreeMap<>();
        before.forEach(source -> after.put(path(source), source));
        for (Result change : changes) {
            SourceFile candidate = change.getAfter();
            assertNotNull(candidate);
            assertInstanceOf(J.CompilationUnit.class, candidate);
            after.put(path(candidate), candidate);
        }

        for (ManifestRow row : manifest()) {
            SourceFile candidate = after.get(row.path());
            assertNotNull(candidate, row.path());
            assertEquals(resource(row.resource()), candidate.printAll(), row.path());
        }

        assertTrue(recipe.run(
                        new InMemoryLargeSourceSet(new ArrayList<>(after.values())),
                        context(),
                        1)
                .getChangeset()
                .getAllResults()
                .isEmpty());
    }

    @Test
    void sourceDriftAndOccupiedAbsentTargetFailClosed() throws Exception {
        Path repository = repositoryRoot();
        List<SourceFile> drifted = currentPreimages(repository);
        int stream = indexOf(
                drifted, "src/java.base/share/classes/java/util/stream/Stream.java");
        SourceFile current = drifted.get(stream);
        drifted.set(
                stream,
                parseJava(
                        path(current),
                        current.printAll() + "\n// deliberate drift\n"));

        assertThrows(
                RuntimeException.class,
                () -> new M3Jep485BackportRecipe()
                        .run(new InMemoryLargeSourceSet(drifted), context(), 1)
                        .getChangeset()
                        .getAllResults());

        List<SourceFile> occupied = currentPreimages(repository);
        occupied.add(parseJava(
                "src/java.base/share/classes/java/util/stream/Gatherer.java",
                "package java.util.stream; public interface Gatherer<T,A,R> {}\n"));

        assertThrows(
                RuntimeException.class,
                () -> new M3Jep485BackportRecipe()
                        .run(new InMemoryLargeSourceSet(occupied), context(), 1)
                        .getChangeset()
                        .getAllResults());
    }

    @Test
    void reviewedPostimagesExposeTheFinalGathererApiShape() throws Exception {
        String gatherer = resource("01-Gatherer.java.txt");
        String gatherers = resource("03-Gatherers.java.txt");
        String stream = resource("05-Stream.java.txt");

        assertTrue(gatherer.contains("public interface Gatherer<T, A, R>"));
        assertTrue(gatherers.contains("public final class Gatherers"));
        assertTrue(gatherers.contains("windowFixed(int windowSize)"));
        assertTrue(gatherers.contains("windowSliding(int windowSize)"));
        assertTrue(gatherers.contains("Gatherer<T, ?, R> fold("));
        assertTrue(gatherers.contains("Gatherer<T, ?, R> scan("));
        assertTrue(gatherers.contains("Gatherer<T,?,R> mapConcurrent("));
        assertTrue(stream.contains(
                "default <R> Stream<R> gather(Gatherer<? super T, ?, R> gatherer)"));
    }

    private static List<SourceFile> currentPreimages(Path repository) throws Exception {
        List<SourceFile> result = new ArrayList<>();
        for (ManifestRow row : manifest()) {
            Path source = repository.resolve(row.path()).normalize();
            if ("ABSENT".equals(row.before())) {
                assertFalse(Files.exists(source), "ABSENT target already occupied: " + row.path());
                continue;
            }
            assertTrue(Files.isRegularFile(source), "missing current owner: " + row.path());
            result.add(parseJava(
                    row.path(),
                    Files.readString(source, StandardCharsets.UTF_8)));
        }
        return result;
    }

    private static Path repositoryRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        for (Path candidate = current; candidate != null; candidate = candidate.getParent()) {
            if (Files.isRegularFile(candidate.resolve(
                            "m3/tooling/migration-recipes/pom.xml"))
                    && Files.isRegularFile(candidate.resolve(
                            "src/java.base/share/classes/java/util/stream/Stream.java"))) {
                return candidate;
            }
        }
        throw new IllegalStateException("M3JDK21 repository root not found from " + current);
    }

    private static int indexOf(List<SourceFile> sources, String target) {
        for (int index = 0; index < sources.size(); index++) {
            if (target.equals(path(sources.get(index)))) {
                return index;
            }
        }
        throw new IllegalArgumentException(target);
    }

    private static SourceFile parseJava(String path, String source) {
        List<SourceFile> parsed = JavaParser.fromJavaVersion()
                .build()
                .parseInputs(
                        List.of(Parser.Input.fromString(Path.of(path), source)),
                        null,
                        context())
                .toList();
        assertEquals(1, parsed.size(), path);
        SourceFile result = parsed.getFirst();
        assertInstanceOf(J.CompilationUnit.class, result, path);
        assertEquals(source, result.printAll(), path);
        return result;
    }

    private static List<ManifestRow> manifest() throws IOException {
        List<ManifestRow> rows = new ArrayList<>();
        for (String line : resource("manifest.tsv").lines().toList()) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] cells = line.split("\t", -1);
            assertEquals(4, cells.length, line);
            rows.add(new ManifestRow(cells[0], cells[1], cells[2], cells[3]));
        }
        return List.copyOf(rows);
    }

    private static String resource(String name) throws IOException {
        try (var input = M3Jep485BackportRecipeTest.class.getResourceAsStream(ROOT + name)) {
            if (input == null) {
                throw new IOException("missing JEP485 resource: " + name);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> {
            throw new IllegalStateException(error);
        });
    }

    private static String path(SourceFile source) {
        return source.getSourcePath().normalize().toString().replace('\\', '/');
    }

    private record ManifestRow(String path, String before, String after, String resource) {}
}
