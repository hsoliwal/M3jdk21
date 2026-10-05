// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.atom;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;
import org.openrewrite.Parser;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

/** Branch-complete semantic-domain tests for the shared pure-int admission oracle. */
final class M3PureIntBoundaryCoverageTest {
    @Test
    void eligibleAcceptsEverySupportedOperatorAndRefusesEveryMajorShapeBoundary() {
        for (String operator : List.of("+", "-", "*", "<<", ">>", ">>>", "&", "|", "^")) {
            assertTrue(eligible("private static int f(int a,int b){ return a " + operator + " b; }"), operator);
        }
        for (String method : List.of(
                "static int f(int a,int b){ return a+b; }",
                "private int f(int a,int b){ return a+b; }",
                "private static native int f(int a,int b);",
                "private static int f(int a,int b){ }",
                "private static int f(int a,int b){ int x=0; return a+b; }",
                "private static long f(int a,int b){ return a+b; }",
                "private static int f(int a,int b){ int x=a+b; }",
                "private static int f(long a,int b){ return (int)a+b; }",
                "private static int f(int a[],int b){ return b+1; }",
                "private static int f(int a,int... b){ return a+1; }",
                "private static int f(Integer a,int b){ return a+b; }",
                "private static int f(int m3$pureIntAtom,int b){ return m3$pureIntAtom+b; }",
                "private static int f(int a,int b){ return a; }",
                "private static int f(int a,int b){ return 1; }",
                "private static int f(int a,int b){ return a / b; }",
                "private static int f(int a,int b){ return a % b; }",
                "private static int f(int a,int b){ return a++; }",
                "private static int f(int a,int b){ return (a / b); }",
                "private static int f(int a,int b){ return -(a / b); }",
                "private static int f(int a,int b){ return (a / b) + b; }",
                "private static int f(int a,int b){ return a + (a / b); }",
                "private static int f(int a,int b){ return (int)(a + b); }",
                "private static int f(int a,int b){ return a > b ? a : b; }")) {
            assertFalse(eligible(method), method);
        }
        assertTrue(eligible("private static int f(int a,int b){ return ((~(-(+a))) + (b)); }"));
    }

    @Test
    void excessiveDepthAndNodeBudgetFailClosed() {
        String nested = "a+b";
        for (int i = 0; i < 132; i++) nested = "(" + nested + ")";
        assertFalse(eligible("private static int f(int a,int b){ return " + nested + "; }"));

        List<String> leaves = new ArrayList<>();
        for (int i = 0; i < 2050; i++) leaves.add((i & 1) == 0 ? "a" : "b");
        String expression = balanced(leaves, 0, leaves.size());
        assertFalse(eligible("private static int f(int a,int b){ return " + expression + "; }"));
    }

    @Test
    void atomizedShapeRequiresExactVariableInitializerAndReturnIdentity() {
        assertTrue(atomized("private static int f(int a,int b){ int m3$pureIntAtom=a+b; return m3$pureIntAtom; }"));
        for (String method : List.of(
                "static int f(int a,int b){ int m3$pureIntAtom=a+b; return m3$pureIntAtom; }",
                "private int f(int a,int b){ int m3$pureIntAtom=a+b; return m3$pureIntAtom; }",
                "private static native int f(int a,int b);",
                "private static int f(int a,int b){ int m3$pureIntAtom=a+b; }",
                "private static long f(int a,int b){ int m3$pureIntAtom=a+b; return m3$pureIntAtom; }",
                "private static int f(int a,int b){ return a+b; int z=0; }",
                "private static int f(int a,int b){ long m3$pureIntAtom=a+b; return (int)m3$pureIntAtom; }",
                "private static int f(int a,int b){ int m3$pureIntAtom=a+b, z=0; return m3$pureIntAtom; }",
                "private static int f(int a,int b){ int wrong=a+b; return wrong; }",
                "private static int f(int a,int b){ int m3$pureIntAtom; return m3$pureIntAtom; }",
                "private static int f(long a,int b){ int m3$pureIntAtom=(int)a+b; return m3$pureIntAtom; }",
                "private static int f(int m3$pureIntAtom,int b){ int x=m3$pureIntAtom+b; return x; }",
                "private static int f(int a,int b){ int m3$pureIntAtom=a/b; return m3$pureIntAtom; }",
                "private static int f(int a,int b){ int m3$pureIntAtom=a+b; int z=0; }",
                "private static int f(int a,int b){ int m3$pureIntAtom=a+b; return a+b; }",
                "private static int f(int a,int b){ int m3$pureIntAtom=a+b; return a; }")) {
            assertFalse(atomized(method), method);
        }
    }

    @Test
    void extractionHelpersRejectUnadmittedMethods() {
        J.MethodDeclaration plain = method("private static int f(int a,int b){ return a; }");
        assertThrows(IllegalArgumentException.class,
                () -> M3PureIntAtomEligibility.returnedExpression(plain));
        assertThrows(IllegalArgumentException.class,
                () -> M3PureIntAtomEligibility.atomizedVariable(plain));
        J.MethodDeclaration eligible = method("private static int f(int a,int b){ return a+b; }");
        assertTrue(M3PureIntAtomEligibility.returnedExpression(eligible) != null);
        J.MethodDeclaration atomized = method(
                "private static int f(int a,int b){ int m3$pureIntAtom=a+b; return m3$pureIntAtom; }");
        assertTrue(M3PureIntAtomEligibility.atomizedVariable(atomized) != null);
    }

    @Test
    void defensiveAstBranchesRejectMalformedOrImpossibleMethodShapes() {
        J.MethodDeclaration valid = method("private static int f(int a,int b){ return a+b; }");
        assertFalse(M3PureIntAtomEligibility.eligible(valid.withReturnTypeExpression(null)));

        J.Return returned = (J.Return) valid.getBody().getStatements().getFirst();
        J.MethodDeclaration missingExpression = valid.withBody(
                valid.getBody().withStatements(List.of(returned.withExpression(null))));
        assertFalse(M3PureIntAtomEligibility.eligible(missingExpression));

        J.MethodDeclaration noVariableParameter = valid.withParameters(List.of(
                new J.Empty(Tree.randomId(), Space.EMPTY, Markers.EMPTY)));
        assertFalse(M3PureIntAtomEligibility.eligible(noVariableParameter));

        J.VariableDeclarations first = (J.VariableDeclarations) valid.getParameters().getFirst();
        J.VariableDeclarations second = (J.VariableDeclarations) valid.getParameters().get(1);
        assertFalse(M3PureIntAtomEligibility.eligible(valid.withParameters(List.of(
                first.withTypeExpression(null), second))));

        J.VariableDeclarations.NamedVariable firstVariable = first.getVariables().getFirst();
        assertFalse(M3PureIntAtomEligibility.eligible(valid.withParameters(List.of(
                first.withVariables(List.of(firstVariable, firstVariable)), second))));

        J.VariableDeclarations.NamedVariable secondVariable = second.getVariables().getFirst();
        J.VariableDeclarations duplicateSecond = second.withVariables(List.of(
                secondVariable.withName(secondVariable.getName().withSimpleName("a"))));
        assertFalse(M3PureIntAtomEligibility.eligible(valid.withParameters(List.of(first, duplicateSecond))));

        J.Binary binary = (J.Binary) returned.getExpression();
        J.MethodDeclaration wrongRootType = valid.withBody(valid.getBody().withStatements(List.of(
                returned.withExpression(binary.withType(JavaType.Primitive.Boolean)))));
        assertFalse(M3PureIntAtomEligibility.eligible(wrongRootType));

        J.MethodDeclaration literalMethod = method("private static int f(int a,int b){ return 1; }");
        J.Return literalReturn = (J.Return) literalMethod.getBody().getStatements().getFirst();
        J.Literal literal = (J.Literal) literalReturn.getExpression();
        J.MethodDeclaration nonIntegerLiteral = literalMethod.withBody(
                literalMethod.getBody().withStatements(List.of(
                        literalReturn.withExpression(literal.withValue("not-an-int")))));
        assertFalse(M3PureIntAtomEligibility.eligible(nonIntegerLiteral));

        J.MethodDeclaration parenthesized = method("private static int f(int a,int b){ return (a+b); }");
        J.Return parenReturn = (J.Return) parenthesized.getBody().getStatements().getFirst();
        J.Parentheses<?> parens = (J.Parentheses<?>) parenReturn.getExpression();
        @SuppressWarnings({"rawtypes", "unchecked"})
        J.Parentheses malformedParens = ((J.Parentheses) parens).withTree(
                new J.Empty(Tree.randomId(), Space.EMPTY, Markers.EMPTY));
        J.MethodDeclaration nonExpressionParentheses = parenthesized.withBody(
                parenthesized.getBody().withStatements(List.of(
                        parenReturn.withExpression(malformedParens))));
        assertFalse(M3PureIntAtomEligibility.eligible(nonExpressionParentheses));

        J.MethodDeclaration localCarrier = method(
                "private static int f(int a,int b){ int x=1; return a+b; }");
        J.VariableDeclarations local = (J.VariableDeclarations) localCarrier.getBody().getStatements().getFirst();
        @SuppressWarnings({"rawtypes", "unchecked"})
        J.Parentheses nonExpressionButInt = ((J.Parentheses) parens).withTree(local);
        assertFalse(M3PureIntAtomEligibility.eligible(parenthesized.withBody(
                parenthesized.getBody().withStatements(List.of(
                        parenReturn.withExpression(nonExpressionButInt))))));

        J.MethodDeclaration atomized = method(
                "private static int f(int a,int b){ int m3$pureIntAtom=a+b; return m3$pureIntAtom; }");
        assertFalse(M3PureIntAtomEligibility.atomized(atomized.withReturnTypeExpression(null)));
        J.VariableDeclarations atom = (J.VariableDeclarations) atomized.getBody().getStatements().getFirst();
        assertFalse(M3PureIntAtomEligibility.atomized(atomized.withBody(
                atomized.getBody().withStatements(List.of(atom.withTypeExpression(null),
                        atomized.getBody().getStatements().get(1))))));
        J.VariableDeclarations.NamedVariable atomVariable = atom.getVariables().getFirst();
        J.VariableDeclarations malformedAtom = atom.withVariables(List.of(atomVariable, atomVariable));
        assertFalse(M3PureIntAtomEligibility.atomized(atomized.withBody(
                atomized.getBody().withStatements(List.of(malformedAtom,
                        atomized.getBody().getStatements().get(1))))));

        J.VariableDeclarations renamedParameter = first.withVariables(List.of(
                firstVariable.withName(firstVariable.getName().withSimpleName("m3$pureIntAtom"))));
        assertFalse(M3PureIntAtomEligibility.atomized(atomized.withParameters(List.of(
                renamedParameter, second))));
    }

    private static boolean eligible(String method) {
        return M3PureIntAtomEligibility.eligible(method(method));
    }

    private static boolean atomized(String method) {
        return M3PureIntAtomEligibility.atomized(method(method));
    }

    private static J.MethodDeclaration method(String method) {
        String source = "package lab; final class T { " + method + " }";
        var context = new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
        List<J.CompilationUnit> units;
        try (var parsed = JavaParser.fromJavaVersion().build().parseInputs(
                List.of(Parser.Input.fromString(Path.of("lab/T.java"), source)), null, context)) {
            units = parsed.map(J.CompilationUnit.class::cast).toList();
        }
        if (units.size() != 1) throw new AssertionError("parse count");
        return units.getFirst().getClasses().getFirst().getBody().getStatements().stream()
                .filter(J.MethodDeclaration.class::isInstance)
                .map(J.MethodDeclaration.class::cast)
                .filter(candidate -> "f".equals(candidate.getSimpleName()))
                .findFirst().orElseThrow();
    }

    private static String balanced(List<String> leaves, int start, int end) {
        if (end - start == 1) return leaves.get(start);
        int middle = (start + end) >>> 1;
        return "(" + balanced(leaves, start, middle) + "+" + balanced(leaves, middle, end) + ")";
    }
}
