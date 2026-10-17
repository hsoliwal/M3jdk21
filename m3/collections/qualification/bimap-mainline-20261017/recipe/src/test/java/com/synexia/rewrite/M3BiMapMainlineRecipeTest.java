/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.synexia.rewrite;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;
import static org.junit.jupiter.api.Assertions.*;

/** Real OpenRewrite scanning, generation, fixed-point and adversarial admission checks. */
final class M3BiMapMainlineRecipeTest {
    private static final String ROOT = "/com/synexia/rewrite/m3-bimap-mainline-20261017/";
    private static M3GpuToolJoinManifest manifest() {
        return M3GpuToolJoinManifest.load(ROOT,
                M3BiMapMainlineRecipeTest.class::getResourceAsStream, () -> {});
    }
    private static SourceFile source(String path, String content) {
        return PlainText.builder().sourcePath(Path.of(path)).text(content).build();
    }
    private static List<SourceFile> after() {
        var sources = new ArrayList<SourceFile>();
        for (var target : manifest().targets()) {
            assertTrue(target.addition());
            assertNotNull(target.text());
            assertEquals(target.after(), M3GpuToolJoinManifest.sha256(target.text()));
            sources.add(source(target.path(), target.text()));
        }
        return sources;
    }
    @Test void exactSdkGeneration() {
        var errors = new ArrayList<Throwable>();
        var run = new M3BiMapMainlineRecipe().run(
            new InMemoryLargeSourceSet(List.of(source("other.txt", "unrelated"))),
            new InMemoryExecutionContext(errors::add));
        assertTrue(errors.isEmpty(), errors.toString());
        var emitted = new LinkedHashMap<String,String>();
        for (var result : run.getChangeset().getAllResults()) {
            assertNull(result.getBefore());
            assertNotNull(result.getAfter());
            String path = result.getAfter().getSourcePath().toString().replace('\\','/');
            assertNull(emitted.put(path, result.getAfter().printAll()));
        }
        assertEquals(5, emitted.size());
        for (var target : manifest().targets()) {
            assertEquals(target.text(), emitted.get(target.path()), target.path());
        }
    }
    @Test void installedPostimageIsFixedPoint() {
        var errors = new ArrayList<Throwable>();
        var run = new M3BiMapMainlineRecipe().run(new InMemoryLargeSourceSet(after()),
                new InMemoryExecutionContext(errors::add));
        assertTrue(errors.isEmpty(), errors.toString());
        assertTrue(run.getChangeset().getAllResults().isEmpty());
    }
    @Test void existingWrongSourceMustRefuse() {
        var sources = after();
        SourceFile original = sources.get(0);
        sources.set(0, source(original.getSourcePath().toString(), "occupied drift"));
        var errors = new ArrayList<Throwable>();
        try {
            new M3BiMapMainlineRecipe().run(new InMemoryLargeSourceSet(sources),
                    new InMemoryExecutionContext(errors::add));
        } catch (RuntimeException failure) {
            errors.add(failure);
        }
        assertFalse(errors.isEmpty(), "occupied source cannot be overwritten");
    }
    @Test void recipeDiscoverableByName() {
        var active = Environment.builder().scanClassLoader(getClass().getClassLoader())
                .build().activateRecipes("com.synexia.m3.BiMapMainline20261017");
        assertTrue(active.validateAll().stream().allMatch(v -> v.isValid()));
    }
}
