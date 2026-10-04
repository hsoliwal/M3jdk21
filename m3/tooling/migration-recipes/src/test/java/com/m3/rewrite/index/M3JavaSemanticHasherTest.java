// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.index;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

final class M3JavaSemanticHasherTest {
    @Test
    void normalizedLogicCoversTheJavaBehavioralVocabularyUsedByAtomIndexing() {
        J.MethodDeclaration method = method(
                """
                private static int exercise(int[] values, Object monitor) {
                    int x = 1;
                    x = x + 1;
                    x += 2;
                    Object text = new String("x");
                    int[] copy = new int[] {1, 2, 3};

                    if (monitor instanceof String) {
                        x = -x;
                    }

                    for (int i = 0; i < values.length; i++) {
                        x += values[i];
                    }

                    for (int value : values) {
                        x ^= value;
                    }

                    while (x < 20) {
                        x++;
                    }

                    do {
                        x--;
                    } while (x > 10);

                    x = x > 0 ? (int) (long) x : ~x;

                    synchronized (monitor) {
                        x += monitor.hashCode();
                    }

                    try {
                        if (x == 0) {
                            throw new IllegalStateException("zero");
                        }
                    } catch (IllegalStateException error) {
                        x += error.hashCode();
                    }

                    switch (x) {
                        case 1 -> x += copy.length;
                        default -> x += text.hashCode();
                    }

                    return x;
                }
                """);

        String logic = M3JavaSemanticHasher.fingerprint("METHOD", method)
                .normalizedComposition();

        for (String token : List.of(
                "DECLARE:",
                "ASSIGN",
                "ASSIGN_OP:",
                "NEW:java.lang.String",
                "NEW_ARRAY:",
                "IF",
                "INSTANCEOF:",
                "UNARY:",
                "FOR",
                "FOREACH",
                "WHILE",
                "DO_WHILE",
                "TERNARY",
                "CAST:",
                "SYNCHRONIZED",
                "CALL:hashCode:0",
                "TRY",
                "THROW",
                "SWITCH",
                "RETURN",
                "BINARY:",
                "LITERAL:",
                "IDENT_TYPE:")) {
            assertTrue(logic.contains(token), token + " absent from " + logic);
        }
    }

    @Test
    void unknownTypeFallbackAndArrayTypeAreStable() {
        J.MethodDeclaration method = method(
                """
                private static Object arrays(Object[] values) {
                    Object first = values[0];
                    return first;
                }
                """);
        String logic = M3JavaSemanticHasher.fingerprint("METHOD", method)
                .normalizedComposition();
        assertTrue(logic.contains("ARRAY:java.lang.Object"));
        assertTrue(logic.contains("java.lang.Object"));
    }

    private static J.MethodDeclaration method(String declaration) {
        String source = "package x; final class A { " + declaration + " }";
        var context = new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
        J.CompilationUnit cu = (J.CompilationUnit) JavaParser.fromJavaVersion()
                .build()
                .parseInputs(
                        List.of(Parser.Input.fromString(
                                Path.of("src/java.base/share/classes/x/A.java"),
                                source)),
                        null,
                        context)
                .findFirst()
                .orElseThrow();
        return (J.MethodDeclaration) cu.getClasses()
                .getFirst()
                .getBody()
                .getStatements()
                .getFirst();
    }
}
