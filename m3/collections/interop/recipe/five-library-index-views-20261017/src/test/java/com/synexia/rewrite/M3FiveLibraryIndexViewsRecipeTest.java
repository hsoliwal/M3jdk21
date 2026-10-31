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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
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
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.text.PlainText;

/** Executes the real hash-pinned Java LST recipe over all two ABSENT-source targets. */
final class M3FiveLibraryIndexViewsRecipeTest {
    private static final String CRATE = "m3jdk21-five-library-index-views-20261017";
    private static final String ROOT = "/com/synexia/rewrite/hash-pinned-java/" + CRATE + "/";

    @Test void generateBothExactPostimagesAndRepeatFixedPoint() {
        Map<String, String> images = images();
        var first = recipe().run(new InMemoryLargeSourceSet(List.of(seed())), context(), 1);
        Map<String, String> actual = new TreeMap<>();
        first.getChangeset().getAllResults().forEach(change ->
                actual.put(normalize(change.getAfter().getSourcePath()), change.getAfter().printAll()));
        assertEquals(images, actual);
        var again = recipe().run(new InMemoryLargeSourceSet(parse(actual)), context(), 1);
        assertTrue(again.getChangeset().getAllResults().isEmpty());
    }

    @Test void allPartialResumeOrdersRemainExact() {
        var images = images();
        List<Map.Entry<String, String>> pairs = new ArrayList<>(images.entrySet());
        assertEquals(2, pairs.size());
        for (int mask = 0; mask < 4; mask++) {
            Map<String, String> already = new TreeMap<>();
            for (int j = 0; j < pairs.size(); j++) if ((mask & (1 << j)) != 0) {
                already.put(pairs.get(j).getKey(), pairs.get(j).getValue());
            }
            List<SourceFile> existing = new ArrayList<>(parse(already));
            existing.add(seed());
            var result = recipe().run(new InMemoryLargeSourceSet(existing), context(), 1);
            result.getChangeset().getAllResults().forEach(change ->
                    already.put(normalize(change.getAfter().getSourcePath()), change.getAfter().printAll()));
            assertEquals(images, already);
        }
    }

    @Test void occupiedOrWrongSourceKindFailsClosed() {
        var row = images().entrySet().iterator().next();
        var pinned = recipe();
        var context = context();
        var inventory = pinned.getInitialValue(context);
        pinned.getScanner(inventory).visit(parse(Map.of(row.getKey(), row.getValue() + "// drift\n")).getFirst(), context);
        assertThrows(IllegalStateException.class, () -> pinned.generate(inventory, context));
        var another = recipe();
        var ctx = context();
        var second = another.getInitialValue(ctx);
        another.getScanner(second).visit(PlainText.builder().sourcePath(Path.of(row.getKey()))
                .text(row.getValue()).build(), ctx);
        assertThrows(IllegalStateException.class, () -> another.generate(second, ctx));
    }

    private static M3HashPinnedJavaSnapshotRecipe recipe() {
        return new M3HashPinnedJavaSnapshotRecipe(CRATE);
    }
    private static Map<String, String> images() {
        Map<String, String> map = new TreeMap<>();
        for (String line : read("manifest.tsv").lines().toList()) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] cols = line.split("\t", -1);
            if (cols.length != 4 || !cols[1].equals("ABSENT")) throw new IllegalStateException("invalid manifest row");
            map.put(cols[0], read(cols[3]));
        }
        return map;
    }
    private static List<SourceFile> parse(Map<String, String> map) {
        return JavaParser.fromJavaVersion().build().parseInputs(map.entrySet().stream()
                .map(row -> Parser.Input.fromString(Path.of(row.getKey()), row.getValue())).toList(),
                null, context()).toList();
    }
    private static String normalize(Path path) { return path.toString().replace('\\', '/'); }
    private static SourceFile seed() {
        return PlainText.builder().sourcePath(Path.of("README.md")).text("seed\n").build();
    }
    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
    }
    private static String read(String path) {
        try (var in = M3FiveLibraryIndexViewsRecipeTest.class.getResourceAsStream(ROOT + path)) {
            if (in == null) throw new IllegalStateException("missing resource: " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) { throw new IllegalStateException(e); }
    }
}
