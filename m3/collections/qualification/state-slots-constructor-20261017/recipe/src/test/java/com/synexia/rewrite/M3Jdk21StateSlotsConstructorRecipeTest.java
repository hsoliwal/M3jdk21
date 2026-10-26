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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

/** Contract-preserving replay of an implicit-to-explicit public constructor atom. */
final class M3Jdk21StateSlotsConstructorRecipeTest {
    private static final String CRATE = "m3jdk21-state-slots-constructor-20261017";
    private static final String ROOT = "/com/synexia/rewrite/hash-pinned-java/" + CRATE + "/";
    private static final String OWNER = "m3/collections/src/main/java/com/m3/collections/M3ObjectStateSlots28.java";

    private static String resource(String path) {
        try (var stream = M3Jdk21StateSlotsConstructorRecipeTest.class.getResourceAsStream(ROOT + path)) {
            assertNotNull(stream, path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) { throw new IllegalStateException(e); }
    }
    private static List<SourceFile> parse(String content) {
        var ctx = new InMemoryExecutionContext(problem -> { throw new AssertionError(problem); });
        return JavaParser.fromJavaVersion().build().parseInputs(
                List.of(Parser.Input.fromString(Path.of(OWNER), content)), null, ctx).toList();
    }
    private static List<SourceFile> run(List<SourceFile> input) {
        var ctx = new InMemoryExecutionContext(problem -> { throw new AssertionError(problem); });
        return new M3HashPinnedJavaSnapshotRecipe(CRATE).run(
                new InMemoryLargeSourceSet(input), ctx, 1).getChangeset().getAllResults()
                .stream().map(ch -> ch.getAfter()).toList();
    }
    @Test void exactPreimageAndPublicApiArePreserved() {
        String[] row = resource("manifest.tsv").trim().split("\t", -1);
        assertEquals(4, row.length);
        assertEquals(OWNER, row[0]);
        assertTrue(row[1].matches("[0-9a-f]{64}"));
        assertTrue(row[2].matches("[0-9a-f]{64}"));
        String before = resource("pre/M3ObjectStateSlots28.java.txt");
        String after = resource("M3ObjectStateSlots28.java.txt");
        assertFalse(before.contains("public M3ObjectStateSlots28()"));
        assertTrue(after.contains("public M3ObjectStateSlots28() { }"));
        assertEquals(after, before.replace("public final class M3ObjectStateSlots28 {\n",
                "public final class M3ObjectStateSlots28 {\n"
                + "    /** Retains the original implicit public no-argument constructor contract. */\n"
                + "    public M3ObjectStateSlots28() { }\n\n"));
    }
    @Test void realLSTGenerationAndFixedPoint() {
        var output = run(parse(resource("pre/M3ObjectStateSlots28.java.txt")));
        assertEquals(1, output.size());
        assertNotNull(output.getFirst());
        assertEquals(resource("M3ObjectStateSlots28.java.txt"), output.getFirst().printAll());
        assertTrue(run(parse(resource("M3ObjectStateSlots28.java.txt"))).isEmpty());
    }
    @Test void missingAndDriftedSourcesRefuse() {
        assertThrows(RuntimeException.class, () -> run(List.of()));
        assertThrows(RuntimeException.class, () ->
                run(parse(resource("pre/M3ObjectStateSlots28.java.txt") +
                        "// unauthorized source change\n")));
    }
}
