// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.index;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

final class M3JavaAtomExtractorTest {
    @Test
    void extractsNestedBehavioralAtomsInDeterministicPreorder() {
        J.MethodDeclaration method = method(
                """
                private static int compute(int a, int b) {
                    int x = a + b;
                    if (x > 0) {
                        x *= 2;
                    }
                    return x;
                }
                """);

        var atoms = M3JavaAtomExtractor.atoms(method.getBody());
        List<String> roles = atoms.stream().map(M3JavaAtomExtractor.Atom::role).toList();

        assertFalse(atoms.isEmpty());
        assertEquals("DECLARATION", roles.getFirst());
        assertTrue(roles.contains("BINARY"));
        assertTrue(roles.contains("IF"));
        assertTrue(roles.contains("ASSIGNMENT_OPERATION"));
        assertTrue(roles.contains("RETURN"));
        assertTrue(roles.contains("LITERAL"));
        assertTrue(atoms.stream().allMatch(atom -> atom.fingerprint().logicSha256().length() == 64));
    }

    @Test
    void localIdentifierRenameKeepsEquivalentNestedLogicHashes() {
        var left = M3JavaAtomExtractor.atoms(method(
                """
                private static int compute(int a, int b) {
                    return (a + b) * 31;
                }
                """).getBody());
        var right = M3JavaAtomExtractor.atoms(method(
                """
                private static int compute(int x, int y) {
                    return (x + y) * 31;
                }
                """).getBody());

        assertEquals(left.size(), right.size());
        for (int index = 0; index < left.size(); index++) {
            assertEquals(left.get(index).role(), right.get(index).role());
            assertEquals(
                    left.get(index).fingerprint().logicSha256(),
                    right.get(index).fingerprint().logicSha256());
            assertEquals(
                    left.get(index).fingerprint().structuralSha256(),
                    right.get(index).fingerprint().structuralSha256());
        }
    }

    @Test
    void operatorChangeChangesRelevantAtomLogicButNotAllStructure() {
        var addition = M3JavaAtomExtractor.atoms(method(
                """
                private static int compute(int a, int b) {
                    return a + b;
                }
                """).getBody());
        var subtraction = M3JavaAtomExtractor.atoms(method(
                """
                private static int compute(int a, int b) {
                    return a - b;
                }
                """).getBody());

        Set<String> plusLogic = addition.stream()
                .map(atom -> atom.fingerprint().logicSha256())
                .collect(Collectors.toSet());
        Set<String> minusLogic = subtraction.stream()
                .map(atom -> atom.fingerprint().logicSha256())
                .collect(Collectors.toSet());
        assertNotEquals(plusLogic, minusLogic);

        var plusBinary = addition.stream()
                .filter(atom -> atom.role().equals("BINARY"))
                .findFirst()
                .orElseThrow();
        var minusBinary = subtraction.stream()
                .filter(atom -> atom.role().equals("BINARY"))
                .findFirst()
                .orElseThrow();
        assertEquals(
                plusBinary.fingerprint().structuralSha256(),
                minusBinary.fingerprint().structuralSha256());
        assertNotEquals(
                plusBinary.fingerprint().logicSha256(),
                minusBinary.fingerprint().logicSha256());
    }

    @Test
    void extractsAllAdmittedAtomRolesAndRejectsNullTree() {
        J.MethodDeclaration method = method(
                """
                private static int all(int[] values, Object monitor) {
                    int x = 0;
                    x = x + 1;
                    x += 2;
                    Object s = new String("x");
                    int[] a = new int[] {1};
                    if (monitor instanceof String) x = -x;
                    for (int i = 0; i < values.length; i++) x += values[i];
                    for (int value : values) x ^= value;
                    while (x < 10) x++;
                    do x--; while (x > 5);
                    x = x > 0 ? (int) (long) x : ~x;
                    synchronized (monitor) { x += monitor.hashCode(); }
                    try {
                        if (x == 0) throw new IllegalStateException();
                    } catch (IllegalStateException error) {
                        x += error.hashCode();
                    }
                    switch (x) {
                        case 1 -> x += a.length;
                        default -> x += s.hashCode();
                    }
                    return x;
                }
                """);

        Set<String> roles = M3JavaAtomExtractor.atoms(method.getBody())
                .stream()
                .map(M3JavaAtomExtractor.Atom::role)
                .collect(Collectors.toSet());

        for (String role : Set.of(
                "DECLARATION",
                "ASSIGNMENT",
                "ASSIGNMENT_OPERATION",
                "BINARY",
                "UNARY",
                "CALL",
                "CONSTRUCTOR",
                "ARRAY_CONSTRUCTION",
                "RETURN",
                "THROW",
                "IF",
                "FOR",
                "FOREACH",
                "WHILE",
                "DO_WHILE",
                "SWITCH",
                "TERNARY",
                "CAST",
                "INSTANCEOF",
                "SYNCHRONIZED",
                "TRY",
                "LITERAL")) {
            assertTrue(roles.contains(role), role);
        }

        org.junit.jupiter.api.Assertions.assertThrows(
                NullPointerException.class,
                () -> M3JavaAtomExtractor.atoms(null));
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
