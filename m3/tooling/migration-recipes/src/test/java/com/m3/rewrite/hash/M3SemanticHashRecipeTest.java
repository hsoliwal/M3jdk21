// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.hash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openrewrite.java.Assertions.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

final class M3SemanticHashRecipeTest implements RewriteTest {
    @Test
    void formattingAndOrdinaryCommentsDoNotChangeSemanticFingerprint() {
        String left = """
                package example;
                final class A {
                    private static int sum(int a, int b) {
                        return a + b;
                    }
                }
                """;
        String right = """
                package example; // ordinary comment
                final class A{private static int sum(int a,int b){return a+b;}}
                """;

        var a = M3SemanticHashRecipe.fingerprint(left);
        var b = M3SemanticHashRecipe.fingerprint(right);

        assertNotEquals(a.exactSha256(), b.exactSha256());
        assertEquals(a.tokenSha256(), b.tokenSha256());
        assertEquals(a.patternIopSha256(), b.patternIopSha256());
        assertEquals(a.semanticSha256(), b.semanticSha256());
    }

    @Test
    void literalAndOperatorTokenChangesAlterSemanticFingerprint() {
        var literal21 = M3SemanticHashRecipe.fingerprint(
                "final class A { static int x() { return 21; } }");
        var literal27 = M3SemanticHashRecipe.fingerprint(
                "final class A { static int x() { return 27; } }");
        assertNotEquals(literal21.tokenSha256(), literal27.tokenSha256());
        assertNotEquals(literal21.semanticSha256(), literal27.semanticSha256());

        String compact = M3JavaTokenNormalizer.normalize(
                "final class A { void x(int a) { a++; } }");
        String separated = M3JavaTokenNormalizer.normalize(
                "final class A { void x(int a) { a + +a; } }");
        assertNotEquals(compact, separated);
    }

    @Test
    void stringCharAndTextBlockContentsRemainHashVisible() {
        String left = """
                final class A {
                    String s = "a b";
                    char c = 'x';
                    String t = """
                            one two
                            """;
                }
                """;
        String right = left.replace("one two", "one  two");
        assertNotEquals(
                M3SemanticHashRecipe.fingerprint(left).tokenSha256(),
                M3SemanticHashRecipe.fingerprint(right).tokenSha256());
    }

    @Test
    void patternAndIopMemoryParticipateEvenThoughCommentsAreTokenIgnored() {
        String left = """
                final class A {
                    // M3-IOP: PURE_INT_EXPRESSION
                    private static int x(int a) { return a + 1; }
                }
                """;
        String right = left.replace("PURE_INT_EXPRESSION", "OTHER_ROLE");

        var a = M3SemanticHashRecipe.fingerprint(left);
        var b = M3SemanticHashRecipe.fingerprint(right);
        assertEquals(a.tokenSha256(), b.tokenSha256());
        assertNotEquals(a.patternIopSha256(), b.patternIopSha256());
        assertNotEquals(a.semanticSha256(), b.semanticSha256());
    }

    @Test
    void openRewriteRecipeEmitsOneWholeFileRowWithoutMutation() {
        var recipe = new M3SemanticHashRecipe();
        rewriteRun(
                spec -> spec.recipe(recipe)
                        .dataTable(M3SemanticHashTable.Row.class, rows -> {
                            assertEquals(1, rows.size());
                            var row = rows.getFirst();
                            assertEquals(
                                    "src/main/java/example/A.java",
                                    row.sourcePath());
                            assertEquals(M3SemanticHashRecipe.ALGORITHM, row.algorithm());
                            assertEquals(64, row.exactSha256().length());
                            assertEquals(64, row.tokenSha256().length());
                            assertEquals(64, row.patternIopSha256().length());
                            assertEquals(64, row.semanticSha256().length());
                        }),
                java(
                        """
                        package example;
                        final class A {
                            private static int x(int a) {
                                return a + 1;
                            }
                        }
                        """,
                        source -> source.path("src/main/java/example/A.java")));
        assertTrue(recipe.getTags().contains("non-mutating"));
        assertTrue(recipe.getTags().contains("semantic-hash"));
    }
}
