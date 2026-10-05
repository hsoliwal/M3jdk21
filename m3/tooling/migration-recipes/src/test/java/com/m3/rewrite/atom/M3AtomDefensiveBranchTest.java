// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.atom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

final class M3AtomDefensiveBranchTest {
    @Test
    void scalarIntAdmissionRejectsVarargsNestedInvalidUnaryAndOversizedComposition() {
        assertFalse(M3PureIntAtomEligibility.eligible(method(
                "private static int f(int... a) { return 1 + 2; }")));
        assertFalse(M3PureIntAtomEligibility.eligible(method(
                "private static int f(int a, int b) { return -X + b; }")));

        String oversized = balanced("a", 12);
        assertTrue(oversized.length() < 100_000);
        assertFalse(M3PureIntAtomEligibility.eligible(method(
                "private static int f(int a) { return " + oversized + "; }")));
    }

    @Test
    void atomizedAdmissionRejectsNativeAndParameterNameCollisionShapes() {
        assertFalse(M3PureIntAtomEligibility.atomized(
                method("private static native int f(int a, int b);")));

        // JavaParser retains this syntactic shape even though javac would reject the local
        // redeclaration; the eligibility oracle must still fail closed before any rewrite.
        J.MethodDeclaration duplicate = method(
                "private static int f(int m3$pureIntAtom, int b) { "
                        + "int m3$pureIntAtom = b + 1; "
                        + "return m3$pureIntAtom; }");
        assertFalse(M3PureIntAtomEligibility.atomized(duplicate));
    }

    @Test
    void documentationScansNonMatchingTextAndJavadocBeforeAddingSemanticMemory() {
        M3DocumentPureIntAtomRecipe recipe = new M3DocumentPureIntAtomRecipe();

        J.MethodDeclaration unrelatedText = method(
                "/* unrelated */ private static int f(int a, int b) { "
                        + "int m3$pureIntAtom = a + b; "
                        + "return m3$pureIntAtom; }");
        int textBefore = unrelatedText.getComments().size();
        J.MethodDeclaration textAfter = apply(recipe, unrelatedText);
        assertTrue(textAfter.getComments().size() > textBefore);

        J.MethodDeclaration unrelatedJavadoc = method(
                "/** unrelated javadoc */ private static int f(int a, int b) { "
                        + "int m3$pureIntAtom = a + b; "
                        + "return m3$pureIntAtom; }");
        int javadocBefore = unrelatedJavadoc.getComments().size();
        J.MethodDeclaration javadocAfter = apply(recipe, unrelatedJavadoc);
        assertTrue(javadocAfter.getComments().size() > javadocBefore);
    }

    @Test
    void patternizationScansUnrelatedLocalCommentsBeforeAddingMarker() {
        M3PatternizePureIntAtomRecipe recipe = new M3PatternizePureIntAtomRecipe();
        J.MethodDeclaration source = method(
                "private static int f(int a, int b) { "
                        + "/* unrelated */ int m3$pureIntAtom = a + b; "
                        + "return m3$pureIntAtom; }");

        J.VariableDeclarations before =
                M3PureIntAtomEligibility.atomizedVariable(source);
        int comments = before.getComments().size();
        J.MethodDeclaration result = apply(recipe, source);
        assertTrue(
                M3PureIntAtomEligibility.atomizedVariable(result)
                        .getComments()
                        .size()
                        > comments);
    }

    private static String balanced(String leaf, int depth) {
        if (depth == 0) {
            return leaf;
        }
        String child = balanced(leaf, depth - 1);
        return "(" + child + " + " + child + ")";
    }

    private static J.MethodDeclaration apply(Recipe recipe, J.MethodDeclaration method) {
        Tree visited = recipe.getVisitor().visit(method, context());
        assertTrue(visited instanceof J.MethodDeclaration);
        return (J.MethodDeclaration) visited;
    }

    private static J.MethodDeclaration method(String member) {
        String source = "class T { static int X = 7; " + member + " }";
        J.CompilationUnit unit = (J.CompilationUnit) JavaParser.fromJavaVersion()
                .build()
                .parseInputs(
                        List.of(Parser.Input.fromString(
                                Path.of("src/main/java/T.java"),
                                source)),
                        null,
                        context())
                .findFirst()
                .orElseThrow();
        return unit.getClasses()
                .getFirst()
                .getBody()
                .getStatements()
                .stream()
                .filter(J.MethodDeclaration.class::isInstance)
                .map(J.MethodDeclaration.class::cast)
                .findFirst()
                .orElseThrow();
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
    }
}
