// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.semantic;

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
    void compatibilityHasherMatchesCanonicalSynexiaOwnerExactly() {
        var target = M3SemanticHasher.method(methodOnly(BEFORE));
        var canonical =
                com.synexia.rewrite.semantic.M3SemanticHasher.method(
                        methodOnly(BEFORE));

        assertEquals(canonical.contractHash(), target.contractHash());
        assertEquals(canonical.logicHash(), target.logicHash());
        assertEquals(canonical.architectureHash(), target.architectureHash());
        assertEquals(canonical.behavioralHash(), target.behavioralHash());
        assertEquals(canonical.wholeHash(), target.wholeHash());

        var targetFile = M3SemanticHasher.file(BEFORE);
        var canonicalFile =
                com.synexia.rewrite.semantic.M3SemanticHasher.file(BEFORE);
        assertEquals(canonicalFile.contractHash(), targetFile.contractHash());
        assertEquals(canonicalFile.logicHash(), targetFile.logicHash());
        assertEquals(canonicalFile.architectureHash(), targetFile.architectureHash());
        assertEquals(canonicalFile.behavioralHash(), targetFile.behavioralHash());
        assertEquals(canonicalFile.wholeHash(), targetFile.wholeHash());
    }

    @Test
    void admittedAtomizationParityComesFromCanonicalOwner() {
        var before = M3SemanticHasher.method(methodOnly(BEFORE));
        var after = M3SemanticHasher.method(methodOnly(ATOMIZED));
        assertEquals(before.contractHash(), after.contractHash());
        assertEquals(before.logicHash(), after.logicHash());
        assertEquals(before.behavioralHash(), after.behavioralHash());
        assertNotEquals(before.architectureHash(), after.architectureHash());
        assertNotEquals(before.wholeHash(), after.wholeHash());
    }

    @Test
    void recipeIsThinTargetAdapterOverCanonicalSynexiaRecipe() {
        var adapter = new M3SemanticHashRecipe();
        assertEquals(1, adapter.getRecipeList().size());
        assertEquals(
                com.synexia.rewrite.semantic.M3SemanticHashRecipe.class,
                adapter.getRecipeList().getFirst().getClass());
        assertTrue(adapter.getTags().contains("target-adapter"));
        assertTrue(adapter.getTags().contains("synexia"));
    }

    @Test
    void delegatedRecipeEmitsCanonicalRowsWithoutMutation() {
        var adapter = new M3SemanticHashRecipe();
        rewriteRun(
                spec -> spec.recipe(adapter)
                        .dataTable(
                                com.synexia.rewrite.semantic.M3SemanticHashTable.Row.class,
                                rows -> {
                                    assertEquals(2, rows.size());
                                    assertTrue(
                                            rows.stream()
                                                    .anyMatch(
                                                            row ->
                                                                    row.level()
                                                                            .equals("METHOD")));
                                    assertTrue(
                                            rows.stream()
                                                    .anyMatch(
                                                            row ->
                                                                    row.level()
                                                                            .equals("FILE")));
                                    rows.forEach(
                                            row -> {
                                                assertEquals(
                                                        64,
                                                        row.contractHash().length());
                                                assertEquals(
                                                        64,
                                                        row.logicHash().length());
                                                assertEquals(
                                                        64,
                                                        row.architectureHash().length());
                                                assertEquals(
                                                        64,
                                                        row.behavioralHash().length());
                                                assertEquals(
                                                        64,
                                                        row.wholeHash().length());
                                            });
                                }),
                java(
                        BEFORE,
                        source ->
                                source.path(
                                        "src/java.base/share/classes/example/Sample.java")));
    }

    @Test
    void targetFacadeDoesNotRegressCanonicalPayloadSafety() {
        String fakeMarker = """
                private static int compute(int a, int b) {
                    String payload = "M3-IOP: PURE_INT_EXPRESSION";
                    return (a + b) * 31;
                }
                """;
        String ordinary = fakeMarker.replace(
                ""M3-IOP: PURE_INT_EXPRESSION"",
                ""ordinary"");
        assertEquals(
                M3SemanticHasher.method(ordinary).architectureHash(),
                M3SemanticHasher.method(fakeMarker).architectureHash());
    }

    private static String methodOnly(String source) {
        int method = source.indexOf("private static int compute");
        return source.substring(method, source.lastIndexOf('}'));
    }
}
