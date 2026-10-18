/* Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 */
package com.synexia.rewrite;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;
import static org.junit.jupiter.api.Assertions.*;

/** Actual OpenRewrite scheduler, source-preimage, drift and fixed-point qualification. */
final class M3FiveProviderCensusSuccessorGateRecipeTest {
    private static final String ROOT =
            "/com/synexia/rewrite/m3-five-provider-census-successor-gate-20261018/";
    private static String resource(String sub) {
        try (InputStream in =
                M3FiveProviderCensusSuccessorGateRecipeTest.class.getResourceAsStream(ROOT + sub)) {
            assertNotNull(in, sub);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new AssertionError(e);
        }
    }
    private static PlainText source(String text) {
        return PlainText.builder().sourcePath(Path.of(
                ".github/workflows/m3-five-provider-current-census-20261017.yml"))
                .text(text).build();
    }
    private static void rejects(String text) {
        var errors = new ArrayList<Throwable>();
        try {
            new M3FiveProviderCensusSuccessorGateRecipe().run(
                    new InMemoryLargeSourceSet(java.util.List.of(source(text))),
                    new InMemoryExecutionContext(errors::add));
        } catch (RuntimeException problem) { errors.add(problem); }
        assertFalse(errors.isEmpty(), "unsafe source admitted");
    }
    @Test void oneSourceTemplateChangesOnlyTheIntendedWorkflow() {
        var errors = new ArrayList<Throwable>();
        var run = new M3FiveProviderCensusSuccessorGateRecipe().run(
                new InMemoryLargeSourceSet(java.util.List.of(source(resource(
                        "preimages/m3-five-provider-current-census-20261017.yml")))),
                new InMemoryExecutionContext(errors::add));
        assertTrue(errors.isEmpty(), errors.toString());
        var changed = run.getChangeset().getAllResults();
        assertEquals(1, changed.size());
        assertEquals(resource("templates/m3-five-provider-current-census-20261017.yml"),
                changed.get(0).getAfter().printAll());
    }
    @Test void exactSecondPassIsFixedPoint() {
        var errors = new ArrayList<Throwable>();
        var run = new M3FiveProviderCensusSuccessorGateRecipe().run(
                new InMemoryLargeSourceSet(java.util.List.of(source(resource(
                        "templates/m3-five-provider-current-census-20261017.yml")))),
                new InMemoryExecutionContext(errors::add));
        assertTrue(errors.isEmpty(), errors.toString());
        assertTrue(run.getChangeset().getAllResults().isEmpty());
    }
    @Test void accidentalDriftAndMissingInputAreRefused() {
        rejects(resource("preimages/m3-five-provider-current-census-20261017.yml") + "# bad");
        var errors = new ArrayList<Throwable>();
        try {
            new M3FiveProviderCensusSuccessorGateRecipe().run(
                    new InMemoryLargeSourceSet(java.util.List.of()),
                    new InMemoryExecutionContext(errors::add));
        } catch (RuntimeException problem) { errors.add(problem); }
        assertFalse(errors.isEmpty(), "missing guard was accepted");
    }
    @Test void namedDeclarativeRecipeIsDiscovered() {
        var recipe = Environment.builder().scanRuntimeClasspath().build()
                .activateRecipes("com.synexia.m3.FiveProviderCensusSuccessorGate20261018");
        assertTrue(recipe.validateAll().stream().allMatch(v -> v.isValid()));
    }
}
