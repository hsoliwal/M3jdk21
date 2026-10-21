/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;
import com.synexia.m3.M3FiveLibraryPreparedRowsRecipe;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

/** Reuses the existing Synexia OpenRewrite ScanningRecipe; no parallel source editor. */
final class M3FiveLibraryPreparedRowsRecipeTest {
    private static final String RESOURCE =
            "/com/synexia/rewrite/five-library-prepared-rows-20261018/";
    private static final String ROOT_PROPERTY = System.getProperty("m3.repo.root");
    private static final Path ROOT = ROOT_PROPERTY == null ? null :
            Path.of(ROOT_PROPERTY).toAbsolutePath().normalize();

    private static M3GpuToolJoinManifest load() {
        return M3GpuToolJoinManifest.load(RESOURCE,
                M3FiveLibraryPreparedRowsRecipeTest.class::getResourceAsStream, () -> { });
    }

    private static SourceFile source(String path, String text) {
        return PlainText.builder().sourcePath(Path.of(path)).text(text).build();
    }

    @Test void namedRecipeDiscovered() {
        var named = Environment.builder().scanClassLoader(getClass().getClassLoader()).build()
                .activateRecipes("com.synexia.m3.FiveLibraryPreparedRows20261018");
        assertTrue(named.validateAll().stream().allMatch(result -> result.isValid()));
    }

    @Test void actualOpenRewriteGenerationAndExactMaterializedPostimages() throws Exception {
        var errors = new ArrayList<Throwable>();
        var run = new M3FiveLibraryPreparedRowsRecipe().run(
                new InMemoryLargeSourceSet(List.<SourceFile>of()),
                new InMemoryExecutionContext(errors::add));
        assertTrue(errors.isEmpty(), errors.toString());
        Map<String, String> resultFiles = new HashMap<>();
        for (var result : run.getChangeset().getAllResults()) {
            assertNotNull(result.getAfter());
            var after = result.getAfter();
            assertNull(resultFiles.put(after.getSourcePath().toString().replace('\\', '/'),
                    after.printAll()));
        }
        int expected = 0;
        for (var entry : load().targets()) {
            if (entry.text() == null) continue;
            expected++;
            assertEquals(entry.text(), resultFiles.get(entry.path()), entry.path());
            assertEquals(entry.after(), M3GpuToolJoinManifest.sha256(entry.text()));
            if (ROOT != null) {
                assertEquals(entry.text(), Files.readString(ROOT.resolve(entry.path())),
                        "recipe output differs from product file " + entry.path());
            }
        }
        assertEquals(expected, resultFiles.size());
        assertEquals(5, expected);
    }

    @Test void fixedPointRequiresNoChanges() {
        List<SourceFile> inputs = new ArrayList<>();
        for (var target : load().targets()) {
            if (target.text() != null) inputs.add(source(target.path(), target.text()));
        }
        var errors = new ArrayList<Throwable>();
        var run = new M3FiveLibraryPreparedRowsRecipe().run(
                new InMemoryLargeSourceSet(inputs), new InMemoryExecutionContext(errors::add));
        assertTrue(errors.isEmpty(), errors.toString());
        assertTrue(run.getChangeset().getAllResults().isEmpty());
    }

    @Test void driftedAndDuplicateTargetsMustRefuse() {
        var target = load().targets().iterator().next();
        List<SourceFile> drift = List.of(source(target.path(), "not the sealed source"));
        var errors = new ArrayList<Throwable>();
        try {
            new M3FiveLibraryPreparedRowsRecipe().run(
                    new InMemoryLargeSourceSet(drift), new InMemoryExecutionContext(errors::add));
        } catch (RuntimeException failure) { errors.add(failure); }
        assertFalse(errors.isEmpty(), "unreviewed existing source must fail before mutation");
        List<SourceFile> duplicates = List.of(source(target.path(), target.text()),
                source(target.path(), target.text()));
        errors.clear();
        try {
            new M3FiveLibraryPreparedRowsRecipe().run(
                    new InMemoryLargeSourceSet(duplicates),
                    new InMemoryExecutionContext(errors::add));
        } catch (RuntimeException failure) { errors.add(failure); }
        assertFalse(errors.isEmpty(), "duplicate source ownership must fail");
    }
}
