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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

/** Exact six-file source custody, real Java LST generation and zero-write replay. */
final class IndexedLiveViewRecipeTest {
    private static final String NAME = "m3-indexed-live-views-20261021";
    private static final String ROOT = "/com/synexia/rewrite/hash-pinned-java/" + NAME + "/";

    private record Row(String path, String before, String after, String template) { }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
    }

    private static String sha(String source) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(source.getBytes(StandardCharsets.UTF_8)));
    }

    private static String resource(String name) throws IOException {
        try (var stream = IndexedLiveViewRecipeTest.class.getResourceAsStream(ROOT + name)) {
            assertNotNull(stream, name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static List<Row> rows() throws Exception {
        List<Row> result = new ArrayList<>();
        for (String line : resource("manifest.tsv").lines().toList()) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] cell = line.split("\t", -1);
            assertEquals(4, cell.length);
            result.add(new Row(cell[0], cell[1], cell[2], cell[3]));
        }
        assertEquals(6, result.size());
        return result;
    }

    private static List<SourceFile> parse(String path, String text) {
        return JavaParser.fromJavaVersion().build().parseInputs(
                List.of(Parser.Input.fromString(Path.of(path), text)), null, context()).toList();
    }

    private static List<SourceFile> inputs() throws Exception {
        List<SourceFile> files = new ArrayList<>();
        Path pre = Path.of(System.getProperty("repo.root"),
                "m3/collections/qualification/indexed-live-views-20261021/pre");
        for (Row row : rows()) {
            if ("ABSENT".equals(row.before())) continue;
            String source = Files.readString(pre.resolve(Path.of(row.path()).getFileName() + ".txt"));
            assertEquals(row.before(), sha(source));
            List<SourceFile> parsed = parse(row.path(), source);
            assertEquals(1, parsed.size());
            files.add(parsed.get(0));
        }
        assertEquals(2, files.size());
        return files;
    }

    @Test void sixExactOutputsAndFixedPoint() throws Exception {
        var recipe = new M3HashPinnedJavaSnapshotRecipe(NAME);
        var changes = recipe.run(new InMemoryLargeSourceSet(inputs()), context(), 1)
                .getChangeset().getAllResults();
        assertEquals(6, changes.size());
        var postimages = new TreeMap<String, SourceFile>();
        for (var change : changes) {
            SourceFile after = change.getAfter();
            assertNotNull(after);
            assertNull(postimages.put(after.getSourcePath().toString().replace('\\', '/'), after));
        }
        for (Row row : rows()) {
            SourceFile output = postimages.get(row.path());
            assertNotNull(output, row.path());
            assertEquals(resource(row.template()), output.printAll());
            assertEquals(row.after(), sha(output.printAll()));
        }
        assertTrue(recipe.run(new InMemoryLargeSourceSet(List.copyOf(postimages.values())),
                context(), 1).getChangeset().getAllResults().isEmpty(),
                "replay on the generated six-file postimage must be a fixed point");
    }

    @Test void refusesSourceDrift() throws Exception {
        var inputs = inputs();
        var changed = new ArrayList<SourceFile>(inputs);
        Row first = rows().get(0);
        String original = inputs.get(0).printAll();
        changed.set(0, parse(first.path(), original + "\n// unrelated source drift\n").get(0));
        var recipe = new M3HashPinnedJavaSnapshotRecipe(NAME);
        assertThrows(RuntimeException.class,
                () -> recipe.run(new InMemoryLargeSourceSet(changed), context(), 1));
    }

    @Test void refusesMissingAndDuplicateOwners() throws Exception {
        var recipe = new M3HashPinnedJavaSnapshotRecipe(NAME);
        assertThrows(RuntimeException.class, () ->
                recipe.run(new InMemoryLargeSourceSet(List.of()), context(), 1));
        var source = inputs();
        source.add(source.get(0));
        assertThrows(RuntimeException.class,
                () -> recipe.run(new InMemoryLargeSourceSet(source), context(), 1));
    }
}
