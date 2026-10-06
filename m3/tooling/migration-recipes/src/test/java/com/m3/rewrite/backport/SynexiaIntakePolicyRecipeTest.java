// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
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

final class SynexiaIntakePolicyRecipeTest {
    private static final String ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned-text/synexia-intake-policy/";

    private static final String WORKFLOW = ".github/workflows/m3-synexia-intake-policy.yml";
    private static final String PORTING = "m3/docs/m3jdk21-porting-invariant.md";
    private static final String DIRECT = "m3/docs/synexia-direct-copy-review.tsv";
    private static final String POLICY = "m3/docs/synexia-intake-policy.tsv";
    private static final String FOSS = "m3/runtime-integration/FOSS_REUSE_DECISION.tsv";
    private static final String VERIFY = "m3/runtime-integration/verify-synexia-intake.py";

    @Test
    void exactCurrentMasterPreimagesReplayAndThenReachFixedPoint() {
        var recipe = new M3Jdk21HashPinnedTextSnapshotRecipe("synexia-intake-policy");
        List<SourceFile> before = List.of(
                text(PORTING, "pre-01-porting.md.txt"),
                text(FOSS, "pre-04-foss.tsv.txt"));

        var first = recipe.run(new InMemoryLargeSourceSet(before), context(), 1);
        Map<String, String> actual = new TreeMap<>();
        for (var result : first.getChangeset().getAllResults()) {
            assertNotNull(result.getAfter());
            actual.put(normalized(result.getAfter().getSourcePath()), result.getAfter().printAll());
        }

        Map<String, String> expected = new TreeMap<>();
        expected.put(WORKFLOW, resource("00-workflow.yml.txt"));
        expected.put(PORTING, resource("01-porting.md.txt"));
        expected.put(DIRECT, resource("02-direct-copy.tsv.txt"));
        expected.put(POLICY, resource("03-policy.tsv.txt"));
        expected.put(FOSS, resource("04-foss.tsv.txt"));
        expected.put(VERIFY, resource("05-verifier.py.txt"));
        assertEquals(expected, actual);

        List<SourceFile> after = expected.entrySet().stream()
                .map(entry -> PlainText.builder()
                        .sourcePath(Path.of(entry.getKey()))
                        .text(entry.getValue())
                        .build())
                .map(SourceFile.class::cast)
                .toList();
        assertTrue(recipe.run(new InMemoryLargeSourceSet(after), context(), 1)
                .getChangeset().getAllResults().isEmpty());
    }

    @Test
    void anyExistingSourceDriftRefusesThePacket() {
        var recipe = new M3Jdk21HashPinnedTextSnapshotRecipe("synexia-intake-policy");
        List<SourceFile> before = new ArrayList<>();
        before.add(text(PORTING, "pre-01-porting.md.txt").withText(
                resource("pre-01-porting.md.txt") + "\n<!-- drift -->\n"));
        before.add(text(FOSS, "pre-04-foss.tsv.txt"));

        assertThrows(RuntimeException.class, () -> recipe.run(
                new InMemoryLargeSourceSet(before), context(), 1)
                .getChangeset().getAllResults());
    }

    @Test
    void policyPostimageNamesAllRequiredIntakeModes() {
        String policy = resource("03-policy.tsv.txt");
        for (String mode : List.of(
                "EXTERNAL_RECIPE_TOOL",
                "INDEPENDENT_APACHE_MODULE",
                "DATA_OR_METADATA_EXPORT",
                "TARGET_OWNED_PORT",
                "EXPLICIT_RELICENSE_PORT",
                "EXISTING_DIRECT_COPY_REVIEW")) {
            assertTrue(policy.contains(mode), mode);
        }
        assertTrue(resource("02-direct-copy.tsv.txt").contains("REVIEW_REQUIRED"));
        assertTrue(resource("05-verifier.py.txt").contains("copied_synexia_runtime_records"));
    }

    private static PlainText text(String path, String resource) {
        return PlainText.builder()
                .sourcePath(Path.of(path))
                .text(resource(resource))
                .build();
    }

    private static String resource(String name) {
        try (var stream = SynexiaIntakePolicyRecipeTest.class.getResourceAsStream(ROOT + name)) {
            assertNotNull(stream, name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
    }

    private static String normalized(Path path) {
        return path.toString().replace('\\', '/');
    }
}
