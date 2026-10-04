// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.m3.rewrite.scope.M3EditScope;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

final class M3JepUniverseAuthorityReconciliationRecipeTest {
    private static final String ROOT =
            "/com/m3/rewrite/backport/jep-universe-authority-reconciliation/";

    private record Target(String path, String resource, boolean absentBefore) {}

    private static final List<Target> TARGETS = List.of(
            existing(".github/workflows/m3-jdk21-recipe-crates.yml",
                    "00-m3-jdk21-recipe-crates.yml.txt"),
            additive(".github/workflows/m3-jep-universe-file-atoms.yml",
                    "01-m3-jep-universe-file-atoms.yml.txt"),
            existing("m3/backports/README.md", "02-backports-README.md.txt"),
            additive("m3/backports/RELEASE_DONOR_REFS.tsv", "03-RELEASE_DONOR_REFS.tsv.txt"),
            existing("m3/backports/file_delta_inventory.py", "04-file_delta_inventory.py.txt"),
            existing("m3/backports/generate_recipe_crates.py", "05-generate_recipe_crates.py.txt"),
            additive("m3/backports/jep_seed_packet_planner.py",
                    "06-jep_seed_packet_planner.py.txt"),
            additive("m3/backports/jep_upstream_inventory.py",
                    "07-jep_upstream_inventory.py.txt"),
            additive("m3/backports/release_donor_refs.py", "08-release_donor_refs.py.txt"),
            existing("m3/backports/test_file_delta_inventory.py",
                    "09-test_file_delta_inventory.py.txt"),
            existing("m3/backports/test_generate_recipe_crates.py",
                    "10-test_generate_recipe_crates.py.txt"),
            additive("m3/backports/test_jep_seed_packet_planner.py",
                    "11-test_jep_seed_packet_planner.py.txt"),
            additive("m3/backports/test_jep_upstream_inventory.py",
                    "12-test_jep_upstream_inventory.py.txt"),
            additive("m3/backports/test_release_donor_refs.py",
                    "13-test_release_donor_refs.py.txt"),
            additive("m3/backports/recipes/jep-universe-authority-reconciliation/README.md",
                    "14-packet-README.md.txt"),
            additive("m3/backports/recipes/jep-universe-authority-reconciliation/packet.tsv",
                    "15-packet.tsv.txt"),
            additive("m3/backports/recipes/jep-universe-authority-reconciliation/atom-evidence.tsv",
                    "16-atom-evidence.tsv.txt"),
            additive("m3/backports/recipes/jep-universe-authority-reconciliation/COMPOSITION_PLAN.tsv",
                    "17-COMPOSITION_PLAN.tsv.txt"),
            existing("m3/tooling/recipe-catalogue.tsv",
                    "18-recipe-catalogue.tsv.txt"),
            additive("m3/tooling/migration-recipes/src/main/resources/META-INF/rewrite/m3-jep-universe-authority-reconciliation.yml",
                    "19-recipe-registry.yml.txt"));

    @Test
    void exactBatchReplaysThenReachesFixedPoint() {
        M3JepUniverseAuthorityReconciliationRecipe recipe =
                new M3JepUniverseAuthorityReconciliationRecipe();
        assertEquals(M3EditScope.MULTI_MODULE, recipe.declaredScope());
        assertEquals(TARGETS.stream().map(Target::path).toList(), recipe.targetPaths());

        Map<String, SourceFile> first = run(baseline(), TARGETS.size());
        for (Target target : TARGETS) {
            assertEquals(resource(target.resource()), first.get(target.path()).printAll());
        }
        run(new ArrayList<>(first.values()), 0);
    }

    @Test
    void missingDriftedDuplicateAndOccupiedTargetsFailClosed() {
        List<SourceFile> missing = baseline();
        missing.removeFirst();
        assertThrows(RuntimeException.class, () -> run(missing, 0));

        List<SourceFile> drifted = baseline();
        SourceFile first = drifted.getFirst();
        drifted.set(0, text(path(first), first.printAll() + "# drift\n"));
        assertThrows(RuntimeException.class, () -> run(drifted, 0));

        List<SourceFile> duplicate = baseline();
        duplicate.add(duplicate.getFirst());
        assertThrows(RuntimeException.class, () -> run(duplicate, 0));

        List<SourceFile> occupied = baseline();
        Target additive = TARGETS.stream().filter(Target::absentBefore).findFirst().orElseThrow();
        occupied.add(text(additive.path(), "conflict\n"));
        assertThrows(RuntimeException.class, () -> run(occupied, 0));
    }

    @Test
    void unrelatedSourceRemainsUntouched() {
        List<SourceFile> input = baseline();
        input.add(text("unrelated.txt", "keep\n"));
        Map<String, SourceFile> result = run(input, TARGETS.size());
        assertEquals("keep\n", result.get("unrelated.txt").printAll());
    }

    private static Target existing(String path, String resource) {
        return new Target(path, resource, false);
    }

    private static Target additive(String path, String resource) {
        return new Target(path, resource, true);
    }

    private static List<SourceFile> baseline() {
        ArrayList<SourceFile> files = new ArrayList<>();
        for (Target target : TARGETS) {
            if (!target.absentBefore()) {
                files.add(text(target.path(), resource("before-" + target.resource())));
            }
        }
        return files;
    }

    private static Map<String, SourceFile> run(List<SourceFile> input, int expected) {
        var changes =
                new M3JepUniverseAuthorityReconciliationRecipe()
                        .run(new InMemoryLargeSourceSet(input), context())
                        .getChangeset()
                        .getAllResults();
        assertEquals(expected, changes.size());

        TreeMap<String, SourceFile> output = new TreeMap<>();
        input.forEach(file -> output.put(path(file), file));
        changes.forEach(change -> {
            assertNotNull(change.getAfter());
            output.put(path(change.getAfter()), change.getAfter());
        });
        return output;
    }

    private static PlainText text(String path, String source) {
        return PlainText.builder().sourcePath(Path.of(path)).text(source).build();
    }

    private static String path(SourceFile file) {
        return file.getSourcePath().toString().replace('\\', '/');
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(failure -> {
            throw new IllegalStateException(failure);
        });
    }

    private static String resource(String name) {
        try (var input =
                M3JepUniverseAuthorityReconciliationRecipeTest.class
                        .getResourceAsStream(ROOT + name)) {
            assertNotNull(input, name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (java.io.IOException failure) {
            throw new IllegalStateException(failure);
        }
    }
}
