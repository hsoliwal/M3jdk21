// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import org.openrewrite.Environment;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

final class M3ReleaseJepDenominatorCurrentRecipeTest {
    private static final String ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned-text/"
                    + "release-jep-denominator-current/";

    private static final Map<String, String> PREIMAGES = Map.of(
            "m3/backports/JEP_CATALOGUE.tsv",
            "before-03-JEP_CATALOGUE.tsv.txt",
            "m3/backports/program_status.py",
            "before-04-program_status.py.txt",
            "m3/backports/test_program_status.py",
            "before-05-test_program_status.py.txt",
            "m3/backports/verify.py",
            "before-06-verify.py.txt",
            "m3/backports/README.md",
            "before-07-README.md.txt");

    @Test
    void metadataAndScopeStayOutsideJdkProductMutation() {
        var recipe = new M3ReleaseJepDenominatorCurrentRecipe();
        assertEquals("release-jep-denominator-current", M3ReleaseJepDenominatorCurrentRecipe.CRATE);
        assertEquals(1, recipe.maxCycles());
        assertTrue(recipe.getTags().contains("denominator"));
        assertTrue(recipe.getTags().contains("release-authority"));
        assertEquals(1, recipe.getRecipeList().size());

        var policy = M3RecipeScopeRegistry.require(M3ReleaseJepDenominatorCurrentRecipe.class);
        assertEquals(M3EditScope.MULTI_MODULE, policy.minimumScope());
        assertEquals(M3ContractMode.BEHAVIOR_AND_CONTRACT_PRESERVING, policy.contractMode());
        assertFalse(policy.fileLocalMechanical(List.of("m3/backports/JEP_CATALOGUE.tsv")));
    }

    @Test
    void namedRecipeIsDiscoverable() {
        var activated =
                Environment.builder()
                        .scanRuntimeClasspath("com.m3.rewrite")
                        .build()
                        .activateRecipes("com.m3.jdk21.ReleaseJepDenominatorCurrent");
        assertEquals(
                "com.m3.jdk21.ReleaseJepDenominatorCurrent",
                activated.getRecipeList().getFirst().getName());
    }

    @Test
    void exactMasterPreimagesReplayNineTargetsAndReachFixedPoint() throws Exception {
        var recipe = new M3ReleaseJepDenominatorCurrentRecipe();
        List<SourceFile> before = baseline();

        var first = recipe.run(new InMemoryLargeSourceSet(before), context(), 1);
        List<Result> changes = first.getChangeset().getAllResults();
        assertEquals(9, changes.size());

        Map<String, SourceFile> after = new TreeMap<>();
        before.forEach(source -> after.put(path(source), source));
        for (Result change : changes) {
            if (change.getAfter() != null) {
                after.put(path(change.getAfter()), change.getAfter());
            }
        }

        for (ManifestRow row : manifest()) {
            SourceFile source = after.get(row.path());
            assertTrue(source != null, row.path());
            assertEquals(resource(ROOT + row.resource()), source.printAll());
        }

        assertTrue(
                recipe.run(
                                new InMemoryLargeSourceSet(new ArrayList<>(after.values())),
                                context(),
                                1)
                        .getChangeset()
                        .getAllResults()
                        .isEmpty());
    }

    @Test
    void staleMasterPreimageFailsClosed() throws Exception {
        List<SourceFile> before = baseline();
        String target = "m3/backports/JEP_CATALOGUE.tsv";
        int index = indexOf(before, target);
        SourceFile current = before.get(index);
        before.set(
                index,
                PlainText.builder()
                        .sourcePath(Path.of(target))
                        .text(current.printAll() + "# drift\n")
                        .build());

        assertThrows(
                RuntimeException.class,
                () -> new M3ReleaseJepDenominatorCurrentRecipe()
                        .run(new InMemoryLargeSourceSet(before), context(), 1)
                        .getChangeset()
                        .getAllResults());
    }

    private static List<SourceFile> baseline() throws IOException {
        List<SourceFile> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : new TreeMap<>(PREIMAGES).entrySet()) {
            result.add(
                    PlainText.builder()
                            .sourcePath(Path.of(entry.getKey()))
                            .text(resource(ROOT + entry.getValue()))
                            .build());
        }
        return result;
    }

    private static int indexOf(List<SourceFile> sources, String target) {
        for (int index = 0; index < sources.size(); index++) {
            if (path(sources.get(index)).equals(target)) {
                return index;
            }
        }
        throw new IllegalArgumentException(target);
    }

    private static List<ManifestRow> manifest() throws IOException {
        List<ManifestRow> rows = new ArrayList<>();
        for (String line : resource(ROOT + "manifest.tsv").lines().toList()) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] cells = line.split("\t", -1);
            rows.add(new ManifestRow(cells[0], cells[3]));
        }
        return List.copyOf(rows);
    }

    private record ManifestRow(String path, String resource) {}

    private static String resource(String name) throws IOException {
        try (var input =
                M3ReleaseJepDenominatorCurrentRecipeTest.class.getResourceAsStream(name)) {
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
