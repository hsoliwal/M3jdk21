// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.atom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

/** Covers recipe metadata plus idempotent TextComment/Javadoc documentation branches. */
final class M3RecipeMetadataAndDocumentationCoverageTest {
    private static final String DOC = "M3-ATOM: m3$pureIntAtom; Pattern/IOP: PURE_INT_EXPRESSION.";

    @Test
    void recipeMetadataIsExecutableContractSurface() {
        for (Recipe recipe : List.of(
                new M3AtomizePureIntReturnRecipe(),
                new M3PatternizePureIntAtomRecipe(),
                new M3DocumentPureIntAtomRecipe(),
                new M3PureIntConvergenceRecipe(),
                new M3InventoryPureIntAtomCandidates())) {
            assertFalse(recipe.getDisplayName().isBlank());
            assertFalse(recipe.getDescription().isBlank());
            assertFalse(recipe.getTags().isEmpty());
        }
        assertTrue(new M3DocumentPureIntAtomRecipe().getTags().containsAll(
                Set.of("documentation", "javadoc", "iop")));
    }

    @Test
    void documenterHandlesNoIndentAndUnrelatedTextCommentThenStops() {
        String sameLine = "package lab; final class T { private static int f(int a,int b){ int m3$pureIntAtom=a+b; return m3$pureIntAtom; } }\n";
        String documented = applyOne(new M3DocumentPureIntAtomRecipe(), sameLine);
        assertTrue(documented.contains(DOC));
        assertEquals(documented, applyOne(new M3DocumentPureIntAtomRecipe(), documented));

        String unrelated = """
                package lab;
                final class T {
                    /* unrelated */
                    private static int f(int a,int b){ int m3$pureIntAtom=a+b; return m3$pureIntAtom; }
                }
                """;
        String after = applyOne(new M3DocumentPureIntAtomRecipe(), unrelated);
        assertTrue(after.contains("unrelated"));
        assertTrue(after.contains(DOC));
    }

    @Test
    void existingJavadocBothMatchesAndFallsThroughWithoutLosingComments() {
        String matching = """
                package lab;
                final class T {
                    /** M3-ATOM: m3$pureIntAtom; Pattern/IOP: PURE_INT_EXPRESSION. */
                    private static int f(int a,int b){ int m3$pureIntAtom=a+b; return m3$pureIntAtom; }
                }
                """;
        assertEquals(matching, applyOne(new M3DocumentPureIntAtomRecipe(), matching));

        String unrelated = """
                package lab;
                final class T {
                    /** existing semantic note */
                    private static int f(int a,int b){ int m3$pureIntAtom=a+b; return m3$pureIntAtom; }
                }
                """;
        String after = applyOne(new M3DocumentPureIntAtomRecipe(), unrelated);
        assertTrue(after.contains("existing semantic note"));
        assertTrue(after.contains(DOC));
    }

    @Test
    void textCommentMarkerIsRecognizedWithoutAReparse() {
        String source = "package lab; final class T { private static int f(int a,int b){ int m3$pureIntAtom=a+b; return m3$pureIntAtom; } }\n";
        var context = new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
        List<SourceFile> parsed;
        try (var stream = JavaParser.fromJavaVersion().build().parseInputs(
                List.of(Parser.Input.fromString(Path.of("lab/T.java"), source)), null, context)) {
            parsed = stream.toList();
        }
        Recipe recipe = new M3DocumentPureIntAtomRecipe();
        var first = recipe.run(new InMemoryLargeSourceSet(parsed), context, 1)
                .getChangeset().getAllResults();
        assertEquals(1, first.size());
        SourceFile postimage = first.getFirst().getAfter();
        assertTrue(postimage.printAll().contains(DOC));

        var secondContext = new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
        var second = recipe.run(new InMemoryLargeSourceSet(List.of(postimage)), secondContext, 1)
                .getChangeset().getAllResults();
        assertTrue(second.isEmpty());
    }

    private static String applyOne(Recipe recipe, String source) {
        var context = new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
        List<SourceFile> parsed;
        try (var stream = JavaParser.fromJavaVersion().build().parseInputs(
                List.of(Parser.Input.fromString(Path.of("lab/T.java"), source)), null, context)) {
            parsed = stream.toList();
        }
        var results = recipe.run(new InMemoryLargeSourceSet(parsed), context, 1)
                .getChangeset().getAllResults();
        if (results.isEmpty()) return source;
        assertEquals(1, results.size());
        return results.getFirst().getAfter().printAll();
    }
}
