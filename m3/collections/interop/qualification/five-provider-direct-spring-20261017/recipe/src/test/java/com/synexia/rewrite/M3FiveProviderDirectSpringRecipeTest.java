/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.synexia.rewrite;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;
import static org.junit.jupiter.api.Assertions.*;

/** Independent actual SDK execution: exact postimages, refusal and fixed point. */
final class M3FiveProviderDirectSpringRecipeTest {
    private static final String ROOT =
            "/com/synexia/rewrite/five-provider-direct-spring-20261017/";

    private static M3GpuToolJoinManifest manifest() {
        return M3GpuToolJoinManifest.load(ROOT,
                M3FiveProviderDirectSpringRecipeTest.class::getResourceAsStream, () -> { });
    }

    private static SourceFile file(String name, String text) {
        return PlainText.builder().sourcePath(Path.of(name)).text(text).build();
    }

    private static List<SourceFile> installedSourceFiles() {
        var sources = new ArrayList<SourceFile>();
        for (var target : manifest().targets()) {
            assertTrue(target.addition());
            assertNotNull(target.text());
            assertEquals(target.after(), M3GpuToolJoinManifest.sha256(target.text()));
            sources.add(file(target.path(), target.text()));
        }
        return sources;
    }

    @Test void generatesExactlyFourBoundedFiles() {
        var errors = new ArrayList<Throwable>();
        var run = new M3FiveProviderDirectSpringRecipe().run(
                new InMemoryLargeSourceSet(List.of(file("unrelated.txt", "keep"))),
                new InMemoryExecutionContext(errors::add));
        assertTrue(errors.isEmpty(), errors.toString());
        var results = run.getChangeset().getAllResults();
        assertEquals(4, results.size());
        for (var target : manifest().targets()) {
            var candidates = results.stream()
                    .filter(r -> r.getAfter() != null
                            && r.getAfter().getSourcePath().toString().replace('\\', '/')
                                    .equals(target.path())).toList();
            assertEquals(1, candidates.size(), target.path());
            assertNull(candidates.getFirst().getBefore());
            assertEquals(target.text(), candidates.getFirst().getAfter().printAll());
        }
    }

    @Test void generatedSetIsSecondPassFixedPoint() {
        var errors = new ArrayList<Throwable>();
        var run = new M3FiveProviderDirectSpringRecipe().run(
                new InMemoryLargeSourceSet(installedSourceFiles()),
                new InMemoryExecutionContext(errors::add));
        assertTrue(errors.isEmpty(), errors.toString());
        assertTrue(run.getChangeset().getAllResults().isEmpty());
    }

    @Test void occupiedOrDriftedTargetRefuses() {
        var input = installedSourceFiles();
        var original = input.getFirst();
        input.set(0, file(original.getSourcePath().toString(), "occupied, not qualified"));
        var errors = new ArrayList<Throwable>();
        try {
            new M3FiveProviderDirectSpringRecipe().run(new InMemoryLargeSourceSet(input),
                    new InMemoryExecutionContext(errors::add));
        } catch (RuntimeException failure) {
            errors.add(failure);
        }
        assertFalse(errors.isEmpty(), "source drift cannot be admitted");
    }

    @Test void namedRecipeIsDiscoverable() {
        var named = Environment.builder().scanClassLoader(getClass().getClassLoader())
                .build().activateRecipes("com.synexia.m3.FiveProviderDirectSpring20261017");
        assertTrue(named.validateAll().stream().allMatch(v -> v.isValid()));
    }

    /** Optional receiving root: verifies actual product files, not template-only proofs. */
    @Test void installedTargetMatchesCanonicalGeneratedSources() throws Exception {
        String target = System.getProperty("m3.receiver.root");
        if (target == null) return;
        Path root = Path.of(target).toAbsolutePath().normalize();
        for (var output : manifest().targets()) {
            Path path = root.resolve(output.path()).normalize();
            assertTrue(path.startsWith(root));
            assertTrue(Files.isRegularFile(path), "missing installed receiver file");
            String actual = Files.readString(path);
            assertEquals(output.text(), actual, "postimage differs from source recipe");
            assertEquals(output.after(), M3GpuToolJoinManifest.sha256(actual));
        }
    }
}
