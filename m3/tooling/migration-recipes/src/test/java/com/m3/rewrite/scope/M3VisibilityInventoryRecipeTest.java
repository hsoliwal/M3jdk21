// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.scope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openrewrite.java.Assertions.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

final class M3VisibilityInventoryRecipeTest implements RewriteTest {
    @Test
    void inventoriesExplicitAndImplicitJavaVisibilityWithoutMutation() {
        rewriteRun(
                spec -> spec.recipe(new M3VisibilityInventoryRecipe())
                        .dataTable(M3VisibilityInventoryRecipe.Row.class, rows -> {
                            assertTrue(rows.stream().anyMatch(row ->
                                    row.declarationKind().equals("TYPE")
                                            && row.symbol().equals("Sample")
                                            && row.visibility().equals("PUBLIC")));
                            assertTrue(rows.stream().anyMatch(row ->
                                    row.declarationKind().equals("FIELD")
                                            && row.symbol().equals("privateField")
                                            && row.visibility().equals("PRIVATE")
                                            && !row.escapesFile()
                                            && !row.librarySurface()));
                            assertTrue(rows.stream().anyMatch(row ->
                                    row.symbol().equals("packageField")
                                            && row.visibility().equals("PACKAGE")
                                            && row.escapesFile()
                                            && !row.librarySurface()));
                            assertTrue(rows.stream().anyMatch(row ->
                                    row.symbol().equals("protectedField")
                                            && row.visibility().equals("PROTECTED")
                                            && row.librarySurface()));
                            assertTrue(rows.stream().anyMatch(row ->
                                    row.symbol().equals("publicField")
                                            && row.visibility().equals("PUBLIC")
                                            && row.librarySurface()));
                            assertTrue(rows.stream().anyMatch(row ->
                                    row.owner().equals("p.Contract")
                                            && row.declarationKind().equals("METHOD")
                                            && row.symbol().equals("apply(int)")
                                            && row.visibility().equals("PUBLIC")));
                            assertTrue(rows.stream().anyMatch(row ->
                                    row.owner().equals("p.Contract")
                                            && row.declarationKind().equals("FIELD")
                                            && row.symbol().equals("VALUE")
                                            && row.visibility().equals("PUBLIC")));
                            assertTrue(rows.stream().anyMatch(row ->
                                    row.owner().equals("p.Marker")
                                            && row.declarationKind().equals("METHOD")
                                            && row.symbol().equals("value()")
                                            && row.visibility().equals("PUBLIC")));
                            assertTrue(rows.stream().anyMatch(row ->
                                    row.owner().equals("p.Marker")
                                            && row.declarationKind().equals("FIELD")
                                            && row.symbol().equals("CODE")
                                            && row.visibility().equals("PUBLIC")));
                            assertTrue(rows.stream().allMatch(row ->
                                    row.patternRole().equals(
                                            "M3:VISIBILITY:" + row.visibility())));
                        }),
                java(
                                """
                                package p;
                                public final class Sample {
                                    private int privateField;
                                    int packageField;
                                    protected int protectedField;
                                    public int publicField;

                                    private void privateMethod() {}
                                    void packageMethod() {}
                                    protected void protectedMethod() {}
                                    public void publicMethod() {}
                                }

                                interface Contract {
                                    int VALUE = 1;
                                    int apply(int value);
                                    private int helper() { return 1; }
                                }
                                """)
                        .path("src/java.base/share/classes/p/Sample.java"),
                java(
                                """
                                package p;
                                public @interface Marker {
                                    int CODE = 7;
                                    String value();
                                }
                                """)
                        .path("src/java.base/share/classes/p/Marker.java"));
    }

    @Test
    void visibilityLevelPredicatesAreMechanical() {
        assertFalse(M3VisibilityLevel.PRIVATE.escapesFile());
        assertTrue(M3VisibilityLevel.PACKAGE.escapesFile());
        assertTrue(M3VisibilityLevel.PROTECTED.escapesFile());
        assertTrue(M3VisibilityLevel.PUBLIC.escapesFile());

        assertFalse(M3VisibilityLevel.PRIVATE.librarySurface());
        assertFalse(M3VisibilityLevel.PACKAGE.librarySurface());
        assertTrue(M3VisibilityLevel.PROTECTED.librarySurface());
        assertTrue(M3VisibilityLevel.PUBLIC.librarySurface());

        assertEquals(4, M3VisibilityLevel.values().length);
    }

    @Test
    void recipeMetadataDeclaresVisibilityInventoryOnly() {
        var recipe = new M3VisibilityInventoryRecipe();
        assertTrue(recipe.getDisplayName().contains("visibility"));
        assertTrue(recipe.getDescription().contains("without changing source"));
        assertTrue(recipe.getTags().contains("visibility"));
        assertTrue(recipe.getTags().contains("non-mutating"));
        assertTrue(recipe.getTags().contains("file-local-analysis"));
    }
}
