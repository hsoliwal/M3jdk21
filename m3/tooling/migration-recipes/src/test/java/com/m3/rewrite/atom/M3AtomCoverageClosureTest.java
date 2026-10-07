// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.atom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

final class M3AtomCoverageClosureTest {
    @Test
    void eligibilityAcceptsEveryAdmittedPrimitiveOperatorAndWrapperShape() {
        for (String expression : List.of(
                "a + b",
                "a - b",
                "a * b",
                "a << b",
                "a >> b",
                "a >>> b",
                "a & b",
                "a | b",
                "a ^ b",
                "(a + b)",
                "+a + b",
                "-a + b",
                "~a + b",
                "1 + 2")) {
            assertTrue(
                    M3PureIntAtomEligibility.eligible(method(
                            "private static int f(int a, int b) { return "
                                    + expression
                                    + "; }")),
                    expression);
        }

        J.MethodDeclaration valid =
                method("private static int f(int a, int b) { return a + b; }");
        assertNotNull(M3PureIntAtomEligibility.returnedExpression(valid));
    }

    @Test
    void eligibilityRejectsEveryUnsafeSurfaceAndUnsupportedExpressionShape() {
        for (String member : List.of(
                "static int f(int a, int b) { return a + b; }",
                "private int f(int a, int b) { return a + b; }",
                "private static native int f(int a, int b);",
                "private static int f(int a, int b) { int x = a; return x + b; }",
                "private static long f(int a, int b) { return a + b; }",
                "private static int f(int a, int b) { a++; }",
                "private static int f(int a, int b) { return; }",
                "private static int f(long a, int b) { return b + 1; }",
                "private static int f(int m3$pureIntAtom, int b) { return m3$pureIntAtom + b; }",
                "private static int f(int a, int b) { return a; }",
                "private static int f(int a, int b) { return a > b ? a : b; }",
                "private static int f(int a, int b) { return X + b; }",
                "private static int f(int a, int b) { return a + X; }",
                "private static int f(int a, int b) { return 'x' + b; }",
                "private static int f(int a, int b) { return ++a + b; }",
                "private static int f(int a, int b) { return a / b; }",
                "private static int f(int a, int b) { return Math.max(a, b); }")) {
            assertFalse(M3PureIntAtomEligibility.eligible(method(member)), member);
        }

        String deep = "a + b";
        for (int index = 0; index < 132; index++) {
            deep = "(" + deep + ")";
        }
        assertFalse(M3PureIntAtomEligibility.eligible(method(
                "private static int f(int a, int b) { return " + deep + "; }")));

        assertThrows(
                IllegalArgumentException.class,
                () -> M3PureIntAtomEligibility.returnedExpression(
                        method("private static int f(int a) { return a; }")));
    }

    @Test
    void atomizedAdmissionCoversAllStructuralGuards() {
        J.MethodDeclaration valid = method(
                "private static int f(int a, int b) { "
                        + "int m3$pureIntAtom = a + b; "
                        + "return m3$pureIntAtom; }");
        assertTrue(M3PureIntAtomEligibility.atomized(valid));
        assertEquals(
                "m3$pureIntAtom",
                M3PureIntAtomEligibility.atomizedVariable(valid)
                        .getVariables()
                        .getFirst()
                        .getSimpleName());

        for (String member : List.of(
                "static int f(int a, int b) { int m3$pureIntAtom = a + b; return m3$pureIntAtom; }",
                "private int f(int a, int b) { int m3$pureIntAtom = a + b; return m3$pureIntAtom; }",
                "private static int f(int a, int b) { return a + b; }",
                "private static long f(int a, int b) { int m3$pureIntAtom = a + b; return m3$pureIntAtom; }",
                "private static int f(int a, int b) { a++; return a; }",
                "private static int f(int a, int b) { long m3$pureIntAtom = a + b; return (int) m3$pureIntAtom; }",
                "private static int f(int a, int b) { int m3$pureIntAtom = a + b, x = 0; return m3$pureIntAtom; }",
                "private static int f(int a, int b) { int x = a + b; return x; }",
                "private static int f(int a, int b) { int m3$pureIntAtom; return m3$pureIntAtom; }",
                "private static int f(long a, int b) { int m3$pureIntAtom = b + 1; return m3$pureIntAtom; }",
                "private static int f(int a, int b) { int m3$pureIntAtom = a; return m3$pureIntAtom; }",
                "private static int f(int a, int b) { int m3$pureIntAtom = a + b; a++; }",
                "private static int f(int a, int b) { int m3$pureIntAtom = a + b; return m3$pureIntAtom + 0; }",
                "private static int f(int a, int b) { int m3$pureIntAtom = a + b; return b; }")) {
            assertFalse(M3PureIntAtomEligibility.atomized(method(member)), member);
        }

        assertThrows(
                IllegalArgumentException.class,
                () -> M3PureIntAtomEligibility.atomizedVariable(
                        method("private static int f(int a) { return a; }")));
    }

    @Test
    void patternizationIsIdempotentAndRefusesNonAtomizedMethods() {
        M3PatternizePureIntAtomRecipe recipe = new M3PatternizePureIntAtomRecipe();
        J.MethodDeclaration plain =
                method("private static int f(int a, int b) { return a + b; }");
        J.MethodDeclaration plainResult = apply(recipe, plain);
        assertEquals(plain.getId(), plainResult.getId());
        assertFalse(M3PureIntAtomEligibility.atomized(plainResult));

        J.MethodDeclaration atomized = method(
                "private static int f(int a, int b) { "
                        + "int m3$pureIntAtom = a + b; "
                        + "return m3$pureIntAtom; }");
        J.MethodDeclaration once = apply(recipe, atomized);
        J.VariableDeclarations first =
                M3PureIntAtomEligibility.atomizedVariable(once);
        assertTrue(first.getComments().stream()
                .anyMatch(comment -> comment.printComment(cursorFor(first))
                        .contains("M3-IOP: PURE_INT_EXPRESSION")));

        int commentCount = first.getComments().size();
        J.MethodDeclaration twice = apply(recipe, once);
        assertEquals(
                commentCount,
                M3PureIntAtomEligibility.atomizedVariable(twice)
                        .getComments()
                        .size());
    }

    @Test
    void semanticDocumentationIsIdempotentForGeneratedTextAndExistingJavadoc() {
        M3DocumentPureIntAtomRecipe recipe = new M3DocumentPureIntAtomRecipe();
        J.MethodDeclaration plain =
                method("private static int f(int a, int b) { return a + b; }");
        J.MethodDeclaration plainResult = apply(recipe, plain);
        assertEquals(plain.getComments().size(), plainResult.getComments().size());

        J.MethodDeclaration atomized = method(
                "private static int f(int a, int b) { "
                        + "int m3$pureIntAtom = a + b; "
                        + "return m3$pureIntAtom; }");
        J.MethodDeclaration once = apply(recipe, atomized);
        assertTrue(once.getComments().stream().anyMatch(comment ->
                comment.printComment(cursorFor(once))
                        .contains("M3-ATOM: m3$pureIntAtom")));
        int comments = once.getComments().size();
        J.MethodDeclaration twice = apply(recipe, once);
        assertEquals(comments, twice.getComments().size());

        J.MethodDeclaration withJavadoc = method(
                "/** M3-ATOM: m3$pureIntAtom; Pattern/IOP: PURE_INT_EXPRESSION. */ "
                        + "private static int f(int a, int b) { "
                        + "int m3$pureIntAtom = a + b; "
                        + "return m3$pureIntAtom; }");
        J.MethodDeclaration unchanged = apply(recipe, withJavadoc);
        assertEquals(withJavadoc.getComments().size(), unchanged.getComments().size());

        J.MethodDeclaration indented = method(
                "\n    private static int f(int a, int b) { "
                        + "int m3$pureIntAtom = a + b; "
                        + "return m3$pureIntAtom; }");
        assertTrue(apply(recipe, indented).getComments().size() > indented.getComments().size());
    }

    @Test
    void inventoryAndCandidateTableCoverDetachedAndAccessorPaths() {
        M3InventoryPureIntAtomCandidates inventory = new M3InventoryPureIntAtomCandidates();
        J.MethodDeclaration eligible =
                method("private static int f(int a, int b) { return a + b; }");
        J.MethodDeclaration rejected =
                method("private static int f(int a) { return a; }");
        int[] detachedVisits = {0};
        Recipe fixtureCycle = new Recipe() {
            @Override public String getDisplayName() {
                return "Exercise detached inventory nodes in a real recipe cycle";
            }

            @Override public String getDescription() {
                return "Supplies the runner context while retaining detached-node inventory visits.";
            }

            @Override public List<Recipe> getRecipeList() {
                return List.of(inventory);
            }

            @Override public TreeVisitor<?, ExecutionContext> getVisitor() {
                return new TreeVisitor<Tree, ExecutionContext>() {
                    @Override public Tree preVisit(Tree tree, ExecutionContext scheduled) {
                        stopAfterPreVisit();
                        assertNotNull(inventory.getVisitor().visit(eligible, scheduled));
                        assertNotNull(inventory.getVisitor().visit(rejected, scheduled));
                        detachedVisits[0]++;
                        return tree;
                    }
                };
            }
        };
        fixtureCycle.run(new InMemoryLargeSourceSet(List.of(unit(""))), context(), 1);
        assertEquals(1, detachedVisits[0], "the scheduled fixture must visit both detached methods");

        M3FileAtomCandidateTable table = new M3FileAtomCandidateTable(inventory);
        M3FileAtomCandidateTable.Row row = new M3FileAtomCandidateTable.Row(
                "src/main/java/p/A.java",
                "f",
                "FILE",
                "BEHAVIOR_AND_CONTRACT_PRESERVING",
                "PURE_INT_EXPRESSION",
                M3AtomizePureIntReturnRecipe.class.getName());
        assertTrue(table.getDisplayName().contains("FILE"));
        assertEquals("src/main/java/p/A.java", row.sourcePath());
        assertEquals("f", row.methodName());
        assertEquals("FILE", row.scope());
        assertEquals("BEHAVIOR_AND_CONTRACT_PRESERVING", row.contractMode());
        assertEquals("PURE_INT_EXPRESSION", row.patternRole());
        assertEquals(M3AtomizePureIntReturnRecipe.class.getName(), row.recipeClass());
    }

    @Test
    void recipeMetadataAndConvergenceCompositionAreFullyDeclared() {
        List<Recipe> recipes = List.of(
                new M3AtomizePureIntReturnRecipe(),
                new M3PatternizePureIntAtomRecipe(),
                new M3DocumentPureIntAtomRecipe(),
                new M3InventoryPureIntAtomCandidates(),
                new M3PureIntConvergenceRecipe());
        for (Recipe recipe : recipes) {
            assertFalse(recipe.getDisplayName().isBlank());
            assertFalse(recipe.getDescription().isBlank());
            assertTrue(recipe.getTags().contains("m3"));
        }

        M3PureIntConvergenceRecipe convergence = new M3PureIntConvergenceRecipe();
        assertEquals(3, convergence.getRecipeList().size());
        assertEquals(M3AtomizePureIntReturnRecipe.class, convergence.getRecipeList().get(0).getClass());
        assertEquals(M3PatternizePureIntAtomRecipe.class, convergence.getRecipeList().get(1).getClass());
        assertEquals(M3DocumentPureIntAtomRecipe.class, convergence.getRecipeList().get(2).getClass());
        assertTrue(convergence.getTags().containsAll(Set.of(
                "convergence",
                "atomization",
                "patternization",
                "iop",
                "documentation",
                "file-local",
                "behavior-contract-preserving")));
    }

    private static J.MethodDeclaration apply(Recipe recipe, J.MethodDeclaration method) {
        Tree visited = recipe.getVisitor().visit(method, context());
        assertTrue(visited instanceof J.MethodDeclaration);
        return (J.MethodDeclaration) visited;
    }

    private static org.openrewrite.Cursor cursorFor(J tree) {
        return new org.openrewrite.Cursor(null, tree);
    }

    private static J.MethodDeclaration method(String member) {
        return unit(member).getClasses()
                .getFirst()
                .getBody()
                .getStatements()
                .stream()
                .filter(J.MethodDeclaration.class::isInstance)
                .map(J.MethodDeclaration.class::cast)
                .findFirst()
                .orElseThrow();
    }

    private static J.CompilationUnit unit(String member) {
        String source = "class T { static int X = 7; " + member + " }";
        return (J.CompilationUnit) JavaParser.fromJavaVersion()
                .build()
                .parseInputs(
                        List.of(Parser.Input.fromString(
                                Path.of("src/main/java/T.java"),
                                source)),
                        null,
                        context())
                .findFirst()
                .orElseThrow();
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
    }
}
