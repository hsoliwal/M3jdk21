/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.synexia.rewrite;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;
import static org.junit.jupiter.api.Assertions.*;

/** Exact source-seal, SDK execution, missing and drift-refusal proof. */
class M3Jdk21ArrayOnlyEntryFacadesRecipeTest {
    private static final String ROOT = "/com/synexia/rewrite/jdk21-array-only-entry-facades-20261018/";
    private static M3GpuToolJoinManifest manifest() {
        return M3GpuToolJoinManifest.load(ROOT,
                M3Jdk21ArrayOnlyEntryFacadesRecipeTest.class::getResourceAsStream, () -> { });
    }

    @Test void generatedJavaMatchesTemplates() {
        var errors = new ArrayList<Throwable>();
        var changes = new M3Jdk21ArrayOnlyEntryFacadesRecipe().run(
                new InMemoryLargeSourceSet(List.of()), new InMemoryExecutionContext(errors::add))
                .getChangeset().getAllResults();
        assertTrue(errors.isEmpty(), errors.toString());
        var generated = new HashMap<String, String>();
        for (var result : changes) {
            SourceFile file = result.getAfter();
            assertNotNull(file);
            assertNull(generated.put(file.getSourcePath().toString().replace('\\', '/'), file.printAll()));
        }
        assertEquals(3, generated.size());
        for (var expected : manifest().targets()) {
            assertEquals(expected.text(), generated.get(expected.path()), expected.path());
            assertEquals(expected.after(), M3GpuToolJoinManifest.sha256(expected.text()));
        }
    }

    @Test void generatedFilesAreFixedPoint() {
        var existing = new ArrayList<SourceFile>();
        for (var item : manifest().targets()) {
            existing.add(PlainText.builder().sourcePath(Path.of(item.path())).text(item.text()).build());
        }
        var errors = new ArrayList<Throwable>();
        var result = new M3Jdk21ArrayOnlyEntryFacadesRecipe().run(
                new InMemoryLargeSourceSet(existing), new InMemoryExecutionContext(errors::add));
        assertTrue(errors.isEmpty(), errors.toString());
        assertTrue(result.getChangeset().getAllResults().isEmpty());
    }

    @Test void occupiedOrDriftedTargetRefuses() {
        var item = manifest().targets().iterator().next();
        var invalid = PlainText.builder().sourcePath(Path.of(item.path()))
                .text(item.text() + "\n// divergent generated source\n").build();
        var errors = new ArrayList<Throwable>();
        try {
            new M3Jdk21ArrayOnlyEntryFacadesRecipe().run(
                    new InMemoryLargeSourceSet(List.of(invalid)),
                    new InMemoryExecutionContext(errors::add));
        } catch (RuntimeException e) { errors.add(e); }
        assertFalse(errors.isEmpty(), "drift must refuse admission");
    }

    @Test void namedSdkRecipeIsDiscoverable() {
        var recipe = Environment.builder().scanClassLoader(getClass().getClassLoader()).build()
                .activateRecipes("com.synexia.m3.Jdk21ArrayOnlyEntryFacades20261018");
        assertTrue(recipe.validateAll().stream().allMatch(v -> v.isValid()));
    }
}
