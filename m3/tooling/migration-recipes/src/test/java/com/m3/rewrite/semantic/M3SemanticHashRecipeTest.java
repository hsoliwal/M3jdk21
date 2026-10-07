// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite.semantic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openrewrite.java.Assertions.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

final class M3SemanticHashRecipeTest implements RewriteTest {
    private static final String BEFORE = """
            package example;
            final class Sample {
                private static int compute(int a, int b) {
                    return (a + b) * 31;
                }
            }
            """;

    private static final String ATOMIZED = """
            package example;
            final class Sample {
                /** M3-ATOM: m3$pureIntAtom; Pattern/IOP: PURE_INT_EXPRESSION. */
                private static int compute(int a, int b) {
                    /* M3-IOP: PURE_INT_EXPRESSION */
                    int m3$pureIntAtom = (a + b) * 31;
                    return m3$pureIntAtom;
                }
            }
            """;

    @Test
    void formattingAndCommentsDoNotChangeBehavioralFingerprint() {
        String reformatted = """
                package example;
                final class Sample {
                    private static int compute( int a , int b ) {
                        // irrelevant documentation/formatting
                        return ( a + b ) * 31 ;
                    }
                }
                """;
        var left = M3SemanticHasher.method(methodOnly(BEFORE));
        var right = M3SemanticHasher.method(methodOnly(reformatted));
        assertEquals(left.contractHash(), right.contractHash());
        assertEquals(left.logicHash(), right.logicHash());
        assertEquals(left.behavioralHash(), right.behavioralHash());
    }

    @Test
    void admittedAtomizationPreservesBehaviorButAddsArchitectureIdentity() {
        var before = M3SemanticHasher.method(methodOnly(BEFORE));
        var after = M3SemanticHasher.method(methodOnly(ATOMIZED));
        assertEquals(before.contractHash(), after.contractHash());
        assertEquals(before.logicHash(), after.logicHash());
        assertEquals(before.behavioralHash(), after.behavioralHash());
        assertNotEquals(before.architectureHash(), after.architectureHash());
        assertNotEquals(before.wholeHash(), after.wholeHash());
    }

    @Test
    void realLogicChangeChangesBehavioralFingerprint() {
        var before = M3SemanticHasher.method(methodOnly(BEFORE));
        var changed = M3SemanticHasher.method(methodOnly(BEFORE.replace("(a + b) * 31", "(a - b) * 31")));
        assertNotEquals(before.logicHash(), changed.logicHash());
        assertNotEquals(before.behavioralHash(), changed.behavioralHash());
    }

    @Test
    void openRewriteRecipeEmitsMethodAndFileRowsWithoutMutation() {
        var recipe = new M3SemanticHashRecipe();
        rewriteRun(
                spec -> spec.recipe(recipe).dataTable(M3SemanticHashTable.Row.class, rows -> {
                    assertEquals(2, rows.size());
                    assertTrue(rows.stream().anyMatch(row -> row.level().equals("METHOD")));
                    assertTrue(rows.stream().anyMatch(row -> row.level().equals("FILE")));
                    rows.forEach(row -> {
                        assertEquals(64, row.contractHash().length());
                        assertEquals(64, row.logicHash().length());
                        assertEquals(64, row.behavioralHash().length());
                        assertEquals(64, row.wholeHash().length());
                    });
                }),
                java(BEFORE, source -> source.path("src/main/java/example/Sample.java")));
    }

    private static String methodOnly(String source) {
        int method = source.indexOf("private static int compute");
        return source.substring(method, source.lastIndexOf('}'));
    }
}
