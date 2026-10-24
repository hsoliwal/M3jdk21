/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.java.JavaParser;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

/** Real SDK execution: source-sealed generation, fixed point, drift and foreign-target refusal. */
final class M3IndexMaskWindowRecipeTest {
    private static final String CRATE = "m3-index-mask-window-20261017";
    private static final String ROOT = "/com/synexia/rewrite/hash-pinned-java/" + CRATE + "/";

    @Test void generatesExactlyThreeJavaSourcesAndConverges() {
        var errors = new ArrayList<Throwable>();
        var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
        var run = recipe.run(new InMemoryLargeSourceSet(List.of(PlainText.builder()
                .sourcePath(Path.of("unrelated.md")).text("unrelated\n").build())),
                new InMemoryExecutionContext(errors::add), 1);
        assertTrue(errors.isEmpty(), errors.toString());
        Map<String, String> results = new TreeMap<>();
        run.getChangeset().getAllResults().forEach(result -> results.put(
                result.getAfter().getSourcePath().toString().replace('\\', '/'),
                result.getAfter().printAll()));
        assertEquals(expected(), results);
        assertEquals(3, results.size());
        var repeated = recipe.run(new InMemoryLargeSourceSet(parsed(results)),
                new InMemoryExecutionContext(errors::add), 1);
        assertTrue(errors.isEmpty(), errors.toString());
        assertTrue(repeated.getChangeset().getAllResults().isEmpty(), "fixed point");
    }

    @Test void refusesDriftAndDuplicateOwner() {
        Map<String, String> expected = expected();
        var entry = expected.entrySet().iterator().next();
        reject(List.of(parsed(Map.of(entry.getKey(), entry.getValue() + "\n// drift")).getFirst()));
        reject(List.of(
                PlainText.builder().sourcePath(Path.of(entry.getKey()))
                        .text(entry.getValue()).build()));
        SourceFile exact = parsed(Map.of(entry.getKey(), entry.getValue())).getFirst();
        reject(List.of(exact, exact));
    }

    private static void reject(List<SourceFile> sources) {
        var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
        var context = new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
        var inventory = recipe.getInitialValue(context);
        for (SourceFile source : sources) recipe.getScanner(inventory).visit(source, context);
        assertThrows(IllegalStateException.class, () -> recipe.generate(inventory, context));
    }

    private static Map<String, String> expected() {
        Map<String, String> images = new TreeMap<>();
        for (String line : text("manifest.tsv").lines().toList()) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] cells = line.split("\t", -1);
            assertEquals(4, cells.length);
            assertEquals("ABSENT", cells[1]);
            images.put(cells[0], text(cells[3]));
        }
        assertFalse(images.isEmpty());
        return images;
    }

    private static List<SourceFile> parsed(Map<String, String> sources) {
        return JavaParser.fromJavaVersion().build().parseInputs(sources.entrySet().stream()
                .map(entry -> Parser.Input.fromString(Path.of(entry.getKey()), entry.getValue()))
                .toList(), null, new InMemoryExecutionContext(
                        error -> { throw new AssertionError(error); })).toList();
    }

    private static String text(String resource) {
        try (var input = M3IndexMaskWindowRecipeTest.class.getResourceAsStream(ROOT + resource)) {
            if (input == null) throw new IllegalStateException("missing " + resource);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (java.io.IOException failure) {
            throw new IllegalStateException(failure);
        }
    }
}
