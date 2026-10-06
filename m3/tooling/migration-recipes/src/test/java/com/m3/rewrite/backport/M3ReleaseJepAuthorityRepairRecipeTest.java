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

final class M3ReleaseJepAuthorityRepairRecipeTest {
    private static final String ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned-text/"
                    + "release-jep-authority-repair-20261006/";

    @Test
    void metadataScopeAndNamedRecipeAreExplicit() {
        var recipe = new M3ReleaseJepAuthorityRepairRecipe();
        assertEquals("release-jep-authority-repair-20261006", M3ReleaseJepAuthorityRepairRecipe.CRATE);
        assertEquals(1, recipe.maxCycles());
        assertTrue(recipe.getTags().contains("denominator"));
        assertTrue(recipe.getTags().contains("multi-module"));
        assertEquals(1, recipe.getRecipeList().size());

        var policy = M3RecipeScopeRegistry.require(M3ReleaseJepAuthorityRepairRecipe.class);
        assertEquals(M3EditScope.MULTI_MODULE, policy.minimumScope());
        assertEquals(M3ContractMode.BEHAVIOR_AND_CONTRACT_PRESERVING, policy.contractMode());
        assertFalse(policy.fileLocalMechanical(List.of("m3/backports/JEP_CATALOGUE.tsv")));

        var activated =
                Environment.builder()
                        .scanRuntimeClasspath("com.m3.rewrite")
                        .build()
                        .activateRecipes("com.m3.jdk21.ReleaseJepAuthorityRepair");
        assertEquals(
                "com.m3.jdk21.ReleaseJepAuthorityRepair",
                activated.getRecipeList().getFirst().getName());
    }

    @Test
    void exactCurrentMasterPreimagesReplayNineTargetsAndReachFixedPoint()
            throws Exception {
        var recipe = new M3ReleaseJepAuthorityRepairRecipe();
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

        String catalogue = after.get("m3/backports/JEP_CATALOGUE.tsv").printAll();
        assertTrue(catalogue.contains("24\t404\tGenerational Shenandoah (Experimental)"));
        assertFalse(catalogue.contains("26\t401\tValue Classes and Objects"));
        assertTrue(catalogue.indexOf("25\t520\t") < catalogue.indexOf("25\t521\t"));

        String authority = after.get("m3/backports/RELEASE_JEP_AUTHORITY.tsv").printAll();
        assertTrue(authority.contains(
                "24\tRELEASED\t404,450,472,475,478,479,483,484,485,486,487,488,489,490,491,492,493,494,495,496,497,498,499,501"));
        assertTrue(authority.contains(
                "26\tRELEASED\t500,504,516,517,522,524,525,526,529,530"));

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
    void staleCurrentMasterPreimageFailsClosed() throws Exception {
        List<SourceFile> before = baseline();
        int index = indexOf(before, "m3/backports/JEP_CATALOGUE.tsv");
        SourceFile current = before.get(index);
        before.set(
                index,
                PlainText.builder()
                        .sourcePath(current.getSourcePath())
                        .text(current.printAll() + "# drift\n")
                        .build());

        assertThrows(
                RuntimeException.class,
                () -> new M3ReleaseJepAuthorityRepairRecipe()
                        .run(new InMemoryLargeSourceSet(before), context(), 1)
                        .getChangeset()
                        .getAllResults());
    }

    private static List<SourceFile> baseline() throws IOException {
        List<SourceFile> result = new ArrayList<>();
        for (ManifestRow row : manifest()) {
            if (!"ABSENT".equals(row.before())) {
                result.add(
                        PlainText.builder()
                                .sourcePath(Path.of(row.path()))
                                .text(resource(ROOT + "before-" + row.resource()))
                                .build());
            }
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
            assertEquals(4, cells.length);
            rows.add(new ManifestRow(cells[0], cells[1], cells[3]));
        }
        return List.copyOf(rows);
    }

    private static String resource(String name) throws IOException {
        try (var input =
                M3ReleaseJepAuthorityRepairRecipeTest.class.getResourceAsStream(name)) {
            if (input == null) throw new IOException("missing resource " + name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(
                error -> {
                    throw new AssertionError(error);
                });
    }

    private static String path(SourceFile source) {
        return source.getSourcePath().toString().replace('\\', '/');
    }

    private record ManifestRow(String path, String before, String resource) {}
}
