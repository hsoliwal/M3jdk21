/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.synexia.rewrite;

import java.nio.file.Files;
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

/** Actual scheduler and named-recipe discovery; no output is promoted on SDK errors. */
class M3GuavaBiMapRecipeTest {
    private static final Path REPO=Path.of(System.getProperty("m3.repo.root")).toAbsolutePath().normalize();
    private static final Path TASK=Path.of(System.getProperty("m3.task.root")).toAbsolutePath().normalize();
    private static M3GpuToolJoinManifest manifest() {
        return M3GpuToolJoinManifest.load("/com/synexia/rewrite/m3-guava-bimap-20261017/",
                M3GuavaBiMapRecipeTest.class::getResourceAsStream, () -> {});
    }
    private static SourceFile text(String path,String value) {
        return PlainText.builder().sourcePath(Path.of(path)).text(value).build();
    }
    private static List<SourceFile> inputs(boolean after) throws Exception {
        var sources=new ArrayList<SourceFile>();
        for (var target:manifest().targets()) {
            if (after) {
                String value=target.text()==null ? Files.readString(REPO.resolve(target.path())) : target.text();
                sources.add(text(target.path(),value));
            } else if (!target.addition()) {
                Path before=TASK.resolve("before").resolve(target.path());
                String value=Files.readString(Files.exists(before)?before:REPO.resolve(target.path()));
                assertEquals(target.before(),M3GpuToolJoinManifest.sha256(value));
                sources.add(text(target.path(),value));
            }
        }
        return sources;
    }
    @Test void exactPostimagesThroughSdk() throws Exception {
        var errors=new ArrayList<Throwable>();
        var results=new M3GuavaBiMapRecipe().run(new InMemoryLargeSourceSet(inputs(false)),
                new InMemoryExecutionContext(errors::add)).getChangeset().getAllResults();
        assertTrue(errors.isEmpty(),errors.toString());
        var actual=new LinkedHashMap<String,String>();
        for (var result:results) {
            assertNotNull(result.getAfter());
            var after=result.getAfter();
            assertNull(actual.put(after.getSourcePath().toString().replace('\\','/'),after.printAll()));
        }
        int expected=0;
        for (var target:manifest().targets()) if (target.text()!=null) {
            assertEquals(target.text(),actual.get(target.path()),target.path());expected++;
        }
        assertEquals(expected,actual.size());
    }
    @Test void sdkFixedPoint() throws Exception {
        var errors=new ArrayList<Throwable>();
        var run=new M3GuavaBiMapRecipe().run(new InMemoryLargeSourceSet(inputs(true)),
                new InMemoryExecutionContext(errors::add));
        assertTrue(errors.isEmpty(),errors.toString()); assertTrue(run.getChangeset().getAllResults().isEmpty());
    }
    @Test void namedRecipeDiscovered() {
        var recipe=Environment.builder().scanClassLoader(getClass().getClassLoader()).build()
                .activateRecipes("com.synexia.m3.GuavaBiMapSdk20261017");
        assertTrue(recipe.validateAll().stream().allMatch(validation -> validation.isValid()));
    }
    @Test void sdkRefusesDrift() throws Exception {
        var sources=inputs(false);var first=sources.get(0);
        sources.set(0,text(first.getSourcePath().toString(),first.printAll()+"\n// drift"));
        var errors=new ArrayList<Throwable>();
        try { new M3GuavaBiMapRecipe().run(new InMemoryLargeSourceSet(sources),new InMemoryExecutionContext(errors::add)); }
        catch (RuntimeException failure) { errors.add(failure); }
        assertFalse(errors.isEmpty(),"drift must prevent candidate promotion");
    }
}
