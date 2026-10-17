// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

/** Exact immutable compiler-owner pin and additive source recipe JUnit. */
final class M3JavacConstantRegexPrecomputeRecipeTest {
    private static final String ROOT = "/com/synexia/rewrite/m3-javac-constant-regex/";
    private static final String OWNER =
            "synexia-mindex/compiler/src/main/java/com/synexia/mindex/compiler/"
                    + "MIndexCompilerASTPrecompute.java";
    private static final String[] TARGETS = {
            "synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexJdkConstantRegexPlan.java",
            "synexia-indexstring/src/test/java/com/synexia/indexstring/MIndexJdkConstantRegexPlanContract.java",
            "synexia-indexstring/src/test/java/com/synexia/indexstring/MIndexJdkConstantRegexPlanTest.java",
            "synexia-mindex/compiler/src/main/java/com/synexia/mindex/compiler/MIndexJavaConstantStringFacts.java",
            "synexia-mindex/compiler/src/test/java/com/synexia/mindex/compiler/MIndexJavaConstantStringFactsContract.java",
            "synexia-mindex/compiler/src/test/java/com/synexia/mindex/compiler/MIndexJavaConstantStringFactsTest.java",
            "synexia-openrewrite-recipes/src/main/java/com/synexia/rewrite/M3JavacConstantRegexPrecomputeRecipe.java"
    };
    private static final String[] RESOURCES = {
            "00-MIndexJdkConstantRegexPlan.java.txt",
            "01-MIndexJdkConstantRegexPlanContract.java.txt",
            "02-MIndexJdkConstantRegexPlanTest.java.txt",
            "03-MIndexJavaConstantStringFacts.java.txt",
            "04-MIndexJavaConstantStringFactsContract.java.txt",
            "05-MIndexJavaConstantStringFactsTest.java.txt",
            "06-M3JavacConstantRegexPrecomputeRecipe.java.txt"
    };

    private static String resource(String file) throws IOException {
        try (var stream = M3JavacConstantRegexPrecomputeRecipeTest.class
                .getResourceAsStream(ROOT + file)) {
            assertNotNull(stream, "missing content-sealed regex template: " + file);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static PlainText text(String path, String source) {
        return PlainText.builder().sourcePath(Path.of(path)).text(source).build();
    }

    @Test
    void exactCompilerOwnerAllowsSevenAdditionsAndOneStableReplay() throws IOException {
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        var owner = text(OWNER, resource("compiler-owner-before.java.txt"));
        var first = new M3JavacConstantRegexPrecomputeRecipe()
                .run(new InMemoryLargeSourceSet(List.of(owner)), context);
        assertTrue(errors.isEmpty(), errors.toString());
        var changes = first.getChangeset().getAllResults();
        assertEquals(TARGETS.length, changes.size(),
                "compiler profile owner is KEEP, seven candidates are absent-before");
        List<SourceFile> finalSources = new ArrayList<>();
        finalSources.add(owner);
        for (int target = 0; target < TARGETS.length; target++) {
            String path = TARGETS[target];
            var result = changes.stream()
                    .filter(r -> r.getAfter().getSourcePath().equals(Path.of(path)))
                    .findFirst().orElseThrow();
            assertTrue(result.getBefore() == null, "candidate must add, never overwrite: " + path);
            assertEquals(resource(RESOURCES[target]), result.getAfter().printAll());
            finalSources.add(result.getAfter());
        }
        var second = new M3JavacConstantRegexPrecomputeRecipe()
                .run(new InMemoryLargeSourceSet(finalSources), context);
        assertTrue(errors.isEmpty(), errors.toString());
        assertTrue(second.getChangeset().getAllResults().isEmpty(),
                "reapplying proven exact-source recipe must reach fixed point");
    }

    @Test
    void missingOwnerDriftDuplicateAndForeignCandidatesRefuseWholeBatch() throws IOException {
        reject(List.of());
        var owner = text(OWNER, resource("compiler-owner-before.java.txt"));
        reject(List.of(text(OWNER, owner.printAll() + "// unexpected mutation\n")));
        reject(List.of(owner, text(OWNER, owner.printAll())));
        for (String target : TARGETS) {
            reject(List.of(owner, text(target, "/* untrusted source preexisting */\n")));
        }
    }

    @Test
    void unrelatedSourceNeverReceivesMutation() throws IOException {
        var unrelated = text("Other.java", "class Other {}\n");
        var errors = new ArrayList<Throwable>();
        var run = new M3JavacConstantRegexPrecomputeRecipe().run(
                new InMemoryLargeSourceSet(List.of(
                        text(OWNER, resource("compiler-owner-before.java.txt")), unrelated)),
                new InMemoryExecutionContext(errors::add));
        assertTrue(errors.isEmpty(), errors.toString());
        assertEquals(7, run.getChangeset().getAllResults().size());
        assertTrue(run.getChangeset().getAllResults().stream()
                .noneMatch(r -> r.getAfter().getSourcePath().equals(unrelated.getSourcePath())));
    }

    private static void reject(List<SourceFile> files) {
        var errors = new ArrayList<Throwable>();
        try {
            var result = new M3JavacConstantRegexPrecomputeRecipe()
                    .run(new InMemoryLargeSourceSet(files), new InMemoryExecutionContext(errors::add));
            assertTrue(result.getChangeset().getAllResults().isEmpty(),
                    "unsafe source batch must never partially publish candidate changes");
            assertFalse(errors.isEmpty(), "source drift must report refusal");
        } catch (IllegalStateException expected) {
            assertNotNull(expected.getMessage());
        }
    }
}
