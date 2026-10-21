/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.synexia.rewrite;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

/** Real Maven/OpenRewrite recipe admission, fixed point, drift and refusal tests. */
public final class M3IndexedMultivalueOccurrenceRecipeTest {
    private static final String ROOT = "/com/synexia/rewrite/m3-five-library-occurrences-20261018/";
    private static final String[] PATHS = {
        "m3/collections/src/main/java/com/m3/collections/M3IndexedMultiValueMap.java",
        "m3/collections/src/test/java/com/m3/collections/M3IndexedMultiValueOccurrenceSelfTest.java",
        "m3/collections/src/test/java/com/m3/collections/M3IndexedMultiValueOccurrenceTest.java",
        "AGENTS.md",
        "m3/collections/qualification/m3-five-library-occurrences-20261018/README.md",
        "m3/collections/qualification/m3-five-library-occurrences-20261018/CATALOGUE.tsv",
        ".github/workflows/m3-five-library-occurrences-20261018.yml"
    };
    private static final String[] NAMES = {
        "M3IndexedMultiValueMap.java",
        "M3IndexedMultiValueOccurrenceSelfTest.java",
        "M3IndexedMultiValueOccurrenceTest.java",
        "AGENTS.md", "README.md", "CATALOGUE.tsv",
        "m3-five-library-occurrences-20261018.yml"
    };

    private static String read(String resource) {
        try (var input = M3IndexedMultivalueOccurrenceRecipeTest.class
                .getResourceAsStream(ROOT + resource)) {
            if (input == null) throw new IllegalStateException("missing fixture: " + resource);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException problem) {
            throw new IllegalStateException(problem);
        }
    }

    private static SourceFile source(String path, String content) {
        return PlainText.builder().sourcePath(Path.of(path)).text(content).build();
    }

    private static List<SourceFile> original() {
        return List.of(
            source(PATHS[0], read("preimage/M3IndexedMultiValueMap.java.txt")),
            source(PATHS[3], read("preimage/AGENTS.md.txt")));
    }

    private static List<SourceFile> installed() {
        var sources = new ArrayList<SourceFile>();
        for (int i = 0; i < PATHS.length; i++) {
            sources.add(source(PATHS[i], read("templates/" + NAMES[i] + ".txt")));
        }
        return sources;
    }

    private static List<Throwable> failures(List<SourceFile> sources) {
        var errors = new ArrayList<Throwable>();
        try {
            new M3IndexedMultivalueOccurrenceRecipe().run(
                    new InMemoryLargeSourceSet(sources), new InMemoryExecutionContext(errors::add));
        } catch (RuntimeException failure) {
            errors.add(failure);
        }
        return errors;
    }

    @Test void exactSourceSealedPostimages() {
        var errors = new ArrayList<Throwable>();
        var result = new M3IndexedMultivalueOccurrenceRecipe()
                .run(new InMemoryLargeSourceSet(original()), new InMemoryExecutionContext(errors::add));
        assertTrue(errors.isEmpty(), errors.toString());
        var files = new LinkedHashMap<String, String>();
        for (var change : result.getChangeset().getAllResults()) {
            assertNotNull(change.getAfter());
            var output = change.getAfter();
            assertNull(files.put(output.getSourcePath().toString(), output.printAll()));
        }
        assertEquals(PATHS.length, files.size());
        for (int i = 0; i < PATHS.length; i++) {
            assertEquals(read("templates/" + NAMES[i] + ".txt"), files.get(PATHS[i]), PATHS[i]);
        }
    }

    @Test void secondRunIsFixedPoint() {
        var errors = new ArrayList<Throwable>();
        var result = new M3IndexedMultivalueOccurrenceRecipe()
                .run(new InMemoryLargeSourceSet(installed()), new InMemoryExecutionContext(errors::add));
        assertTrue(errors.isEmpty(), errors.toString());
        assertTrue(result.getChangeset().getAllResults().isEmpty());
    }

    @Test void missingOccupiedAndDriftedInputsRefuse() {
        assertFalse(failures(List.of()).isEmpty());
        assertFalse(failures(List.of(original().get(0))).isEmpty());
        var occupied = new ArrayList<>(original());
        occupied.add(source(PATHS[1], "unqualified already-occupied owner"));
        assertFalse(failures(occupied).isEmpty());
        var drift = new ArrayList<>(original());
        drift.set(0, source(PATHS[0], drift.get(0).printAll() + "// corrupted source"));
        assertFalse(failures(drift).isEmpty());
        var reversed = new ArrayList<>(original());
        java.util.Collections.reverse(reversed);
        assertTrue(failures(reversed).isEmpty());
    }

    @Test void namedRecipeDiscovery() {
        var named = Environment.builder().scanClassLoader(getClass().getClassLoader())
                .build().activateRecipes("com.synexia.m3.IndexedMultivalueOccurrence20261018");
        assertTrue(named.validateAll().stream().allMatch(v -> v.isValid()));
    }
}
