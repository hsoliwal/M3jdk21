/* Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 */
package com.synexia.rewrite;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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

/** Tests the real OpenRewrite scheduler; source drift and partial replay fail closed. */
final class M3FiveProviderIndexedProjectionsRecipeTest {
    private static final String ROOT =
            "/com/synexia/rewrite/m3-five-provider-indexed-projections-20261018/";

    private static M3GpuToolJoinManifest manifest() {
        return M3GpuToolJoinManifest.load(ROOT,
                M3FiveProviderIndexedProjectionsRecipeTest.class::getResourceAsStream, () -> { });
    }
    private static SourceFile file(String path, String text) {
        return PlainText.builder().sourcePath(Path.of(path)).text(text).build();
    }
    private static String resource(String name) {
        try (InputStream stream =
                     M3FiveProviderIndexedProjectionsRecipeTest.class.getResourceAsStream(ROOT + name)) {
            assertNotNull(stream, name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (java.io.IOException failure) {
            throw new AssertionError(failure);
        }
    }
    private static List<SourceFile> inputs(boolean after) {
        var result = new ArrayList<SourceFile>();
        for (var target : manifest().targets()) {
            String baseName = target.path().substring(target.path().lastIndexOf('/') + 1);
            String contents = after ? target.text() : resource("preimages/" + baseName);
            if (!after) assertEquals(target.before(),
                    M3GpuToolJoinManifest.sha256(contents), target.path());
            result.add(file(target.path(), contents));
        }
        return result;
    }
    @Test void generatesOnlyFourExactSealedPostimages() {
        var errors = new ArrayList<Throwable>();
        var result = new M3FiveProviderIndexedProjectionsRecipe().run(
                new InMemoryLargeSourceSet(inputs(false)), new InMemoryExecutionContext(errors::add));
        assertTrue(errors.isEmpty(), errors.toString());
        var changed = new HashMap<String, String>();
        for (var item : result.getChangeset().getAllResults()) {
            assertNotNull(item.getAfter());
            assertNull(changed.put(item.getAfter().getSourcePath().toString().replace('\\','/'),
                    item.getAfter().printAll()), "duplicate result");
        }
        assertEquals(4, changed.size());
        for (var target : manifest().targets()) {
            assertEquals(target.text(), changed.get(target.path()), target.path());
        }
    }
    @Test void secondPassIsFixedPoint() {
        var errors = new ArrayList<Throwable>();
        var result = new M3FiveProviderIndexedProjectionsRecipe().run(
                new InMemoryLargeSourceSet(inputs(true)), new InMemoryExecutionContext(errors::add));
        assertTrue(errors.isEmpty(), errors.toString());
        assertTrue(result.getChangeset().getAllResults().isEmpty());
    }
    private static void refuses(List<SourceFile> source) {
        var errors = new ArrayList<Throwable>();
        try {
            new M3FiveProviderIndexedProjectionsRecipe().run(
                    new InMemoryLargeSourceSet(source), new InMemoryExecutionContext(errors::add));
        } catch (RuntimeException failure) {
            errors.add(failure);
        }
        assertFalse(errors.isEmpty(), "unsafe batch admitted");
    }
    @Test void refuseMissingDuplicateAndDrift() {
        var drift = inputs(false);
        drift.set(0, file(drift.get(0).getSourcePath().toString(), drift.get(0).printAll() + "bad"));
        refuses(drift);
        var missing = inputs(false); missing.remove(0); refuses(missing);
        var duplicate = inputs(false); duplicate.add(duplicate.get(0)); refuses(duplicate);
    }
    @Test void canonicalYamlIsDiscovered() {
        var recipe = Environment.builder().scanClassLoader(getClass().getClassLoader()).build()
                .activateRecipes("com.synexia.m3.FiveProviderIndexedProjections20261018");
        assertTrue(recipe.validateAll().stream().allMatch(v -> v.isValid()));
    }
}
