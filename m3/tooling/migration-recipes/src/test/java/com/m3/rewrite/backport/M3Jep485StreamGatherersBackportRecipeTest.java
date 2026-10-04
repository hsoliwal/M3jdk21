// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.rewrite.scope.M3ContractMode;
import com.m3.rewrite.scope.M3EditScope;
import com.m3.rewrite.scope.M3RecipeScopeRegistry;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

final class M3Jep485StreamGatherersBackportRecipeTest {
    private static final String ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned/jdk24-jep485-stream-gatherers/";
    private static final String PRE =
            "/com/m3/rewrite/backport/jdk24-jep485-stream-gatherers/pre/";

    private static final Map<String, String> PREIMAGES = Map.of(
            "src/java.base/share/classes/java/util/stream/AbstractPipeline.java",
            "00-AbstractPipeline.java.before.txt",
            "src/java.base/share/classes/java/util/stream/ReferencePipeline.java",
            "04-ReferencePipeline.java.before.txt",
            "src/java.base/share/classes/java/util/stream/Stream.java",
            "05-Stream.java.before.txt",
            "src/java.base/share/classes/java/util/stream/package-info.java",
            "06-package-info.java.before.txt");

    @Test
    void compositePinsJdk24GaCrateAndExplicitLibraryApiAuthority() {
        var recipe = new M3Jep485StreamGatherersBackportRecipe();

        assertEquals("jdk-24+36", M3Jep485StreamGatherersBackportRecipe.DONOR_TAG);
        assertEquals(
                "33b26f79a986d015abdcd84b89842adc0a4bde64",
                M3Jep485StreamGatherersBackportRecipe.IMPLEMENTATION_COMMIT);
        assertEquals(
                "ef0dc2518e7636cc8a9ca580613ff5edeb4c19fd",
                M3Jep485StreamGatherersBackportRecipe.GRADUATION_COMMIT);
        assertEquals(
                "450636ae28b84ded083b6861c6cba85fbf87e16e",
                M3Jep485StreamGatherersBackportRecipe.MAP_CONCURRENT_FIX);

        assertEquals(1, recipe.getRecipeList().size());
        var crate = assertInstanceOf(
                M3Jdk21HashPinnedSnapshotRecipe.class, recipe.getRecipeList().getFirst());
        assertEquals("jdk24-jep485-stream-gatherers", crate.getCrateName());

        var policy =
                M3RecipeScopeRegistry.require(M3Jep485StreamGatherersBackportRecipe.class);
        assertEquals(M3EditScope.LIBRARY_API, policy.minimumScope());
        assertEquals(M3ContractMode.EXPLICIT_CONTRACT_CHANGE, policy.contractMode());
        assertEquals(1, recipe.maxCycles());
        assertTrue(recipe.getTags().contains("jdk24-ga"));
    }

    @Test
    void exactCurrentMasterStreamPreimagesReplayFifteenTargetsThenReachFixedPoint()
            throws Exception {
        var recipe = new M3Jep485StreamGatherersBackportRecipe();
        List<SourceFile> before = baseline();

        var first = recipe.run(new InMemoryLargeSourceSet(before), context(), 1);
        List<Result> changes = first.getChangeset().getAllResults();
        assertEquals(15, changes.size());

        Map<String, SourceFile> after = new TreeMap<>();
        before.forEach(source -> after.put(path(source), source));
        for (Result change : changes) {
            SourceFile source = change.getAfter();
            if (source != null) after.put(path(source), source);
        }

        for (ManifestRow row : manifest()) {
            assertEquals(resource(ROOT + row.resource()), after.get(row.path()).printAll());
        }

        assertTrue(
                recipe.run(new InMemoryLargeSourceSet(new ArrayList<>(after.values())), context(), 1)
                        .getChangeset()
                        .getAllResults()
                        .isEmpty());
    }

    @Test
    void driftedCurrentMasterStreamPreimageFailsClosed() throws Exception {
        List<SourceFile> sources = baseline();
        String target = "src/java.base/share/classes/java/util/stream/Stream.java";
        int index = indexOf(sources, target);
        SourceFile current = sources.get(index);
        sources.set(index, parse(target, current.printAll() + "\n// drift\n"));

        assertThrows(
                RuntimeException.class,
                () -> new M3Jep485StreamGatherersBackportRecipe()
                        .run(new InMemoryLargeSourceSet(sources), context(), 1)
                        .getChangeset()
                        .getAllResults());
    }

    private static List<SourceFile> baseline() throws Exception {
        List<SourceFile> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : new TreeMap<>(PREIMAGES).entrySet()) {
            result.add(parse(entry.getKey(), resource(PRE + entry.getValue())));
        }
        return result;
    }

    private static SourceFile parse(String path, String source) {
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
        return parsed.getFirst();
    }

    private static int indexOf(List<SourceFile> sources, String target) {
        for (int index = 0; index < sources.size(); index++) {
            if (path(sources.get(index)).equals(target)) return index;
        }
        throw new IllegalArgumentException(target);
    }

    private static List<ManifestRow> manifest() throws IOException {
        List<ManifestRow> rows = new ArrayList<>();
        for (String line : resource(ROOT + "manifest.tsv").lines().toList()) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] cells = line.split("\t", -1);
            rows.add(new ManifestRow(cells[0], cells[1], cells[2], cells[3]));
        }
        return List.copyOf(rows);
    }

    private record ManifestRow(String path, String before, String after, String resource) {}

    private static String resource(String name) throws IOException {
        try (var input =
                M3Jep485StreamGatherersBackportRecipeTest.class.getResourceAsStream(name)) {
            if (input == null) throw new IOException("missing resource " + name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
    }

    private static String path(SourceFile source) {
        return source.getSourcePath().toString().replace('\\', '/');
    }
}
