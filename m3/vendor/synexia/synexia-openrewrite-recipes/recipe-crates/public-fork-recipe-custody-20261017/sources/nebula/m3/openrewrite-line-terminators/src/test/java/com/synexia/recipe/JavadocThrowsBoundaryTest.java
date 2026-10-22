// SPDX-License-Identifier: Apache-2.0
package com.synexia.recipe;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.java.Java21Parser;
import org.openrewrite.java.tree.J;

/** Regression corpus from the immutable donor that blocked full source packet closure. */
final class JavadocThrowsBoundaryTest {
    @Test void completeJdkCollectionDonorIsLossless() throws Exception {
        var root = Path.of(System.getProperty("m3.nebula.currentRoot"));
        var path = root.resolve("m3/stream-segment-proof/proof/donor-inventory/jdk21-donors/java.base/java/util/Collection.java");
        var original = Files.readAllBytes(path);
        assertLossless(new String(original, StandardCharsets.UTF_8));
        assertArrayEquals(original, Files.readAllBytes(path));
    }

    @Test void throwsAndExceptionTagsPreserveSpacingAndLineTerminators() {
        for (var newline : List.of("\n", "\r\n", "\r")) {
            for (var tag : List.of("throws", "exception")) {
                for (var gap : List.of("", " ", "  ")) {
                    var source = "class A {\n/** Summary.\n * @param x value\n * " + gap + "@" + tag
                            + " IllegalArgumentException when x is negative\n * @return x\n */\n"
                            + "int f(int x) { if (x < 0) throw new IllegalArgumentException(); return x; }\n}\n";
                    assertLossless(source.replace("\n", newline));
                }
            }
        }
    }

    private static void assertLossless(String source) {
        var errors = new ArrayList<Throwable>();
        var files = Java21Parser.builder().build().parseInputs(
                List.of(Parser.Input.fromString(Path.of("A.java"), source)), null,
                new InMemoryExecutionContext(errors::add)).toList();
        assertTrue(errors.isEmpty(), errors.toString());
        assertEquals(1, files.size());
        assertInstanceOf(J.CompilationUnit.class, files.getFirst());
        assertEquals(source, files.getFirst().printAll());
    }
}
