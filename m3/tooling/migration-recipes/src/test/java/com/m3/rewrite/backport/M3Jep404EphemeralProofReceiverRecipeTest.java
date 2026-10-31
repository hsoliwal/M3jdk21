// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

final class M3Jep404EphemeralProofReceiverRecipeTest {
    private static final String WORKFLOW =
            ".github/workflows/m3-jep404-generational-shenandoah-inventory.yml";
    private static final String POLICY =
            "m3/backports/recipes/jep-404-generational-shenandoah/MATERIALIZATION_POLICY.tsv";
    private static final String README =
            "m3/backports/recipes/jep-404-generational-shenandoah/README.md";
    private static final String TEST =
            "m3/backports/test_jep404_inventory_policy.py";
    private static final String PRE =
            "/com/m3/rewrite/backport/jep404-ephemeral-proof/pre/";

    @Test
    void exactControlPlanePreimagesReplayAndReachFixedPoint() throws IOException {
        var wrapper = new M3Jep404EphemeralProofReceiverRecipe();
        assertEquals(1, wrapper.maxCycles());
        assertEquals(1, wrapper.getRecipeList().size());

        var receiver =
                (M3Jdk21HashPinnedTextSnapshotRecipe) wrapper.getRecipeList().getFirst();
        assertEquals("jdk21-jep404-ephemeral-proof", receiver.getCrateName());
        assertEquals(List.of(WORKFLOW, POLICY, README, TEST), receiver.targetPaths());

        List<SourceFile> before =
                List.of(
                        file(WORKFLOW, resource(PRE + "00-workflow.yml.txt")),
                        file(POLICY, resource(PRE + "01-materialization-policy.tsv.txt")),
                        file(README, resource(PRE + "02-readme.md.txt")),
                        file(TEST, resource(PRE + "03-test_jep404_inventory_policy.py.txt")));

        var first = wrapper.run(new InMemoryLargeSourceSet(before), context(), 1);
        var changes = first.getChangeset().getAllResults();
        assertEquals(4, changes.size());
        assertTrue(changes.stream().allMatch(change -> change.getBefore() != null));

        List<SourceFile> after =
                changes.stream()
                        .map(
                                change -> {
                                    assertNotNull(change.getAfter());
                                    return change.getAfter();
                                })
                        .toList();

        assertTrue(
                wrapper.run(new InMemoryLargeSourceSet(after), context(), 1)
                        .getChangeset()
                        .getAllResults()
                        .isEmpty());
    }

    @Test
    void stalePreimageFailsWholeBatchAndUnrelatedFileStaysUntouched() throws IOException {
        var wrapper = new M3Jep404EphemeralProofReceiverRecipe();
        var unrelated = file("m3/backports/UNRELATED.txt", "unchanged\n");
        List<SourceFile> sources =
                new ArrayList<>(
                        List.of(
                                file(WORKFLOW, resource(PRE + "00-workflow.yml.txt")),
                                file(POLICY, resource(PRE + "01-materialization-policy.tsv.txt")),
                                file(README, resource(PRE + "02-readme.md.txt")),
                                file(TEST, resource(PRE + "03-test_jep404_inventory_policy.py.txt")),
                                unrelated));

        var passed = wrapper.run(new InMemoryLargeSourceSet(sources), context(), 1);
        assertTrue(
                passed.getChangeset().getAllResults().stream()
                        .noneMatch(
                                result ->
                                        unrelated.getSourcePath()
                                                .equals(result.getAfter().getSourcePath())));

        sources.set(
                1,
                file(
                        POLICY,
                        resource(PRE + "01-materialization-policy.tsv.txt")
                                + "drift\ttrue\n"));
        assertThrows(
                RuntimeException.class,
                () ->
                        wrapper.run(new InMemoryLargeSourceSet(sources), context(), 1)
                                .getChangeset()
                                .getAllResults());
    }

    @Test
    void receiverNeverClaimsCanonicalProductMutationOrPromotion() {
        var wrapper = new M3Jep404EphemeralProofReceiverRecipe();

        assertTrue(wrapper.getTags().contains("candidate-only"));
        assertTrue(wrapper.getDescription().contains("ephemeral CI worktree"));
    }

    private static PlainText file(String path, String text) {
        return PlainText.builder().sourcePath(Path.of(path)).text(text).build();
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(
                error -> {
                    throw new AssertionError(error);
                });
    }

    private static String resource(String path) throws IOException {
        try (var in = M3Jep404EphemeralProofReceiverRecipeTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "missing exact before fixture: " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
