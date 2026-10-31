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

/** Actual incumbent OpenRewrite/LST proof of the two ABSENT-before sorted-snapshot targets. */
final class M3LongSortedSnapshotRecipeTest {
    private static final String ROOT =
            "/com/synexia/rewrite/hash-pinned-java/m3long-sorted-snapshot-20261017/";
    private static final String MAIN =
            "m3/collections/src/main/java/com/m3/collections/M3LongSortedSnapshot.java";
    private static final String TEST =
            "m3/collections/src/test/java/com/m3/collections/M3LongSortedSnapshotSelfTest.java";

    @Test
    void generatedJavaSourcesEqualSealedImagesAndReplayIsFixedPoint() {
        var recipe = new M3HashPinnedJavaSnapshotRecipe("m3long-sorted-snapshot-20261017");
        var result = recipe.run(
                new InMemoryLargeSourceSet(List.of(
                        PlainText.builder().sourcePath(Path.of("README.md"))
                                .text("unrelated source\n").build())),
                context(), 1);
        Map<String, String> generated = new TreeMap<>();
        result.getChangeset().getAllResults().forEach(change -> {
            assertTrue(change.getBefore() == null);
            generated.put(
                    change.getAfter().getSourcePath().toString().replace('\\', '/'),
                    change.getAfter().printAll());
        });
        assertEquals(Map.of(
                MAIN, resource("M3LongSortedSnapshot.java.txt"),
                TEST, resource("M3LongSortedSnapshotSelfTest.java.txt")), generated);

        var secondPass = recipe.run(
                new InMemoryLargeSourceSet(parse(generated)),
                context(), 1);
        assertTrue(secondPass.getChangeset().getAllResults().isEmpty());
    }

    @Test
    void unknownDriftOccupiedNonJavaAndDuplicateTargetsFailClosed() {
        var recipe = new M3HashPinnedJavaSnapshotRecipe("m3long-sorted-snapshot-20261017");
        var ctx = context();
        SourceFile original = parse(Map.of(MAIN, resource("M3LongSortedSnapshot.java.txt"))).getFirst();

        var drift = recipe.getInitialValue(ctx);
        SourceFile modified = parse(Map.of(
                MAIN, resource("M3LongSortedSnapshot.java.txt") + "// unreviewed drift\n")).getFirst();
        recipe.getScanner(drift).visit(modified, ctx);
        assertThrows(IllegalStateException.class, () -> recipe.generate(drift, ctx));

        var nonJava = recipe.getInitialValue(ctx);
        recipe.getScanner(nonJava).visit(
                PlainText.builder().sourcePath(Path.of(MAIN)).text(original.printAll()).build(), ctx);
        assertThrows(IllegalStateException.class, () -> recipe.generate(nonJava, ctx));

        var duplicate = recipe.getInitialValue(ctx);
        recipe.getScanner(duplicate).visit(original, ctx);
        recipe.getScanner(duplicate).visit(original, ctx);
        assertThrows(IllegalStateException.class, () -> recipe.generate(duplicate, ctx));
    }

    private static List<SourceFile> parse(Map<String, String> files) {
        return JavaParser.fromJavaVersion().build()
                .parseInputs(files.entrySet().stream()
                                .map(entry -> Parser.Input.fromString(
                                        Path.of(entry.getKey()), entry.getValue())).toList(),
                        null, context())
                .toList();
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
    }

    private static String resource(String name) {
        try (var input = M3LongSortedSnapshotRecipeTest.class
                .getResourceAsStream(ROOT + name)) {
            if (input == null) throw new IllegalStateException("missing recipe resource: " + name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }
}
