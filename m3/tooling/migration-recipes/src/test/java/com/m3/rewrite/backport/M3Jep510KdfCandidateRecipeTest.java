// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.rewrite.scope.M3ContractMode;
import com.m3.rewrite.scope.M3EditScope;
import com.m3.rewrite.scope.M3RecipeScopeRegistry;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

final class M3Jep510KdfCandidateRecipeTest {
    @Test
    void compositeOrdersProductBeforeSecurityTestsAndCarriesLibraryApiAuthority() {
        var recipe = new M3Jep510KdfCandidateRecipe();

        assertEquals(2, recipe.getRecipeList().size());
        assertInstanceOf(M3Jep510KdfBackportRecipe.class, recipe.getRecipeList().get(0));
        assertInstanceOf(M3Jep510KdfSecurityTestsRecipe.class, recipe.getRecipeList().get(1));

        var policy = M3RecipeScopeRegistry.require(M3Jep510KdfCandidateRecipe.class);
        assertEquals(M3EditScope.LIBRARY_API, policy.minimumScope());
        assertEquals(M3ContractMode.EXPLICIT_CONTRACT_CHANGE, policy.contractMode());

        var testPolicy = M3RecipeScopeRegistry.require(M3Jep510KdfSecurityTestsRecipe.class);
        assertEquals(M3EditScope.MODULE, testPolicy.minimumScope());
        assertEquals(
                M3ContractMode.BEHAVIOR_AND_CONTRACT_PRESERVING,
                testPolicy.contractMode());
        assertEquals(1, recipe.maxCycles());
    }

    @Test
    void namedCandidateRecipeIsDiscoverable() {
        var activated =
                Environment.builder()
                        .scanRuntimeClasspath("com.m3.rewrite")
                        .build()
                        .activateRecipes("com.m3.jdk21.Jep510KdfCandidate");
        assertEquals(
                "com.m3.jdk21.Jep510KdfCandidate",
                activated.getRecipeList().getFirst().getName());
    }

    @Test
    void securityTestCrateAddsFiveStructuredJavaTestsAndThenStops() {
        var recipe = new M3Jep510KdfSecurityTestsRecipe();
        var first =
                recipe.run(
                        new InMemoryLargeSourceSet(List.of()),
                        context(),
                        1);
        List<Result> results = first.getChangeset().getAllResults();

        assertEquals(5, results.size());
        assertTrue(
                results.stream()
                        .allMatch(
                                result ->
                                        result.getBefore() == null
                                                && result.getAfter()
                                                        instanceof org.openrewrite.java.tree.J.CompilationUnit));

        List<SourceFile> after =
                results.stream()
                        .map(Result::getAfter)
                        .map(
                                source -> {
                                    if (source == null) {
                                        throw new IllegalStateException("unexpected deletion");
                                    }
                                    return source;
                                })
                        .toList();

        assertTrue(
                recipe.run(
                                new InMemoryLargeSourceSet(after),
                                context(),
                                1)
                        .getChangeset()
                        .getAllResults()
                        .isEmpty());
    }

    @Test
    void securityTestCrateRejectsDivergentExistingTarget() {
        String path =
                "test/jdk/com/sun/crypto/provider/KDF/HKDFKnownAnswerTests.java";
        SourceFile drift =
                parse(
                        path,
                        """
                        public class HKDFKnownAnswerTests {
                            // divergent unreviewed target
                        }
                        """);

        assertThrows(
                RuntimeException.class,
                () ->
                        new M3Jep510KdfSecurityTestsRecipe()
                                .run(
                                        new InMemoryLargeSourceSet(List.of(drift)),
                                        context(),
                                        1)
                                .getChangeset()
                                .getAllResults());
    }

    @Test
    void securityTestRecipePinsTheFinalCumulativeLineageCommit() {
        assertEquals(
                "79456110fb6dd11ef19e9637c6f40ee7ce329481",
                M3Jep510KdfSecurityTestsRecipe.UPSTREAM_TEST_PIN);
        assertFalse(new M3Jep510KdfSecurityTestsRecipe().getTags().contains("promotion"));
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
            throw new IllegalStateException("unexpected parse count");
        }
        return parsed.getFirst();
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(
                error -> {
                    throw new AssertionError(error);
                });
    }
}
