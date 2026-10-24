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

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

final class M3LongIndexOrderFlipRecipeTest {
    private static final String CRATE = "m3jdk21-long-index-order-flip-20261020";
    private static final String ROOT = "/com/synexia/rewrite/hash-pinned-java/" + CRATE + "/";
    @Test void exactPreimagesAndFixedPoint() {
        var ctx2 = context();
        var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
        var complete = expected();
        var before = sourcePath();
        var old = resource("M3LongIndexProjection.java.before.txt");
        Map<String, String> baseline = Map.of(before, old);
        var completeChanges = recipe.run(new InMemoryLargeSourceSet(parse(ctx2, baseline)),
                ctx2, 1).getChangeset().getAllResults();
        Map<String, String> outputs = new TreeMap<>();
        for (var change : completeChanges) {
            assertInstanceOf(J.CompilationUnit.class, change.getAfter());
            outputs.put(change.getAfter().getSourcePath().toString().replace('\\', '/'),
                    change.getAfter().printAll());
        }
        assertEquals(complete, outputs);
        var ctx3 = context();
        assertTrue(recipe.run(new InMemoryLargeSourceSet(parse(ctx3, complete)), ctx3, 1)
                .getChangeset().getAllResults().isEmpty());
    }

    @Test void refusalAndHashSeals() throws Exception {
        List<String[]> rows = rows();
        assertEquals(3, rows.size());
        assertEquals(expected().keySet().stream().toList(),
                rows.stream().map(line -> line[0]).toList());
        for (String[] row : rows) assertEquals(row[2], sha(resource(row[3])));
        assertEquals(rows.get(0)[1], sha(resource("M3LongIndexProjection.java.before.txt")));
        var ctx = context();
        var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
        var inventory = recipe.getInitialValue(ctx);
        var scanner = recipe.getScanner(inventory);
        parse(ctx, Map.of(sourcePath(), "package drift; class Wrong {}\n"))
                .forEach(file -> scanner.visit(file, ctx));
        assertTrue(assertThrows(IllegalStateException.class,
                () -> recipe.generate(inventory, ctx)).getMessage().contains("source drift"));
    }

    @Test void canonicalRecipeDiscoverability() {
        var active = Environment.builder().scanRuntimeClasspath("com.synexia.rewrite").build()
                .activateRecipes("com.synexia.rewrite.M3LongIndexOrderFlip20261020");
        assertTrue(contains(active));
    }

    private static boolean contains(org.openrewrite.Recipe recipe) {
        if (recipe instanceof M3HashPinnedJavaSnapshotRecipe own)
            return CRATE.equals(own.getCrateName());
        return recipe.getRecipeList().stream().anyMatch(M3LongIndexOrderFlipRecipeTest::contains);
    }

    private static String sourcePath() {
        return "m3/collections/src/main/java/com/m3/collections/M3LongIndexProjection.java";
    }
    private static Map<String, String> expected() {
        Map<String, String> results = new TreeMap<>();
        for (String[] row : rows()) results.put(row[0], resource(row[3]));
        return results;
    }
    private static List<String[]> rows() {
        return resource("manifest.tsv").lines().filter(l -> !l.isBlank() && !l.startsWith("#"))
                .map(l -> l.split("\t", -1)).toList();
    }
    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(failure -> { throw new AssertionError(failure); });
    }
    private static List<SourceFile> parse(InMemoryExecutionContext ctx, Map<String, String> entries) {
        return JavaParser.fromJavaVersion().build()
                .parseInputs(new TreeMap<>(entries).entrySet().stream()
                    .map(e -> Parser.Input.fromString(Path.of(e.getKey()), e.getValue())).toList(),
                    null, ctx).toList();
    }
    private static String sha(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(text.getBytes(StandardCharsets.UTF_8)));
    }
    private static String resource(String name) {
        try (var stream = M3LongIndexOrderFlipRecipeTest.class.getResourceAsStream(ROOT + name)) {
            assertNotNull(stream, name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) { throw new IllegalStateException(ex); }
    }
}
