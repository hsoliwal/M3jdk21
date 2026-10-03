// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.packs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.openrewrite.test.SourceSpecs.text;
import static org.openrewrite.xml.Assertions.xml;

import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.xml.XmlParser;

/** Positive, negative, scope and fixed-point proof for the reusable recipe adapters. */
final class M3ModulePackRecipesTest implements RewriteTest {
    @Test
    void reactorRegistrationReachesFixedPoint() {
        rewriteRun(spec -> spec.recipe(new M3RegisterModulePacksRecipe())
                        .cycles(2).expectedCyclesThatMakeChanges(1),
                xml(M3RegisterModulePacksRecipe.resource("reactor-before.xml"),
                        M3RegisterModulePacksRecipe.resource("reactor-after.xml"),
                        source -> source.path("m3/pom.xml")));
    }

    @Test
    void nonTargetPomIsUntouched() {
        rewriteRun(spec -> spec.recipe(new M3RegisterModulePacksRecipe()),
                xml(M3RegisterModulePacksRecipe.resource("reactor-before.xml"),
                        source -> source.path("other/pom.xml")));
    }

    @Test
    void reactorDriftAndMissingResourcesAreRefused() {
        var context = new InMemoryExecutionContext();
        var changed = XmlParser.builder().build().parse(context,
                M3RegisterModulePacksRecipe.resource("reactor-before.xml").replace(
                        "M3JDK21 Maven control reactor", "Unreviewed reactor"))
                .findFirst().orElseThrow().withSourcePath(Path.of("m3/pom.xml"));
        assertThrows(RuntimeException.class,
                () -> new M3RegisterModulePacksRecipe().getVisitor().visit(changed, context));
        assertThrows(IllegalStateException.class,
                () -> M3RegisterModulePacksRecipe.resource("missing.xml"));
    }

    @Test
    void realJdkArtifactInventoryProducesNoSourceChange() {
        var recipe = new M3InventoryModulePackRecipe(
                List.of(Path.of(System.getProperty("java.home"), "jmods", "java.base.jmod").toString()),
                List.of("java.base"));
        assertEquals(List.of("java.base"), recipe.getRoots());
        assertEquals(1, recipe.getArtifactPaths().size());
        assertEquals("java.base",
                recipe.getInitialValue(new InMemoryExecutionContext()).getFirst().descriptor().name());
        rewriteRun(spec -> spec.recipe(recipe).cycles(2).expectedCyclesThatMakeChanges(0),
                text("untouched"));
    }

    @Test
    void artifactAndGraphFailuresDoNotBecomeReadiness() {
        var missing = new M3InventoryModulePackRecipe(List.of("missing-archive.jmod"), List.of("java.base"));
        assertThrows(UncheckedIOException.class,
                () -> missing.getInitialValue(new InMemoryExecutionContext()));
        var missingRoot = new M3InventoryModulePackRecipe(
                List.of(Path.of(System.getProperty("java.home"), "jmods", "java.base.jmod").toString()),
                List.of("not.available"));
        assertThrows(IllegalArgumentException.class,
                () -> missingRoot.getInitialValue(new InMemoryExecutionContext()));
        assertThrows(NullPointerException.class,
                () -> new M3InventoryModulePackRecipe(null, List.of("java.base")));
    }
}
