// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.index;

import com.m3.indexdb.M3IndexDbSemanticFingerprint;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

final class M3IndexDbSemanticFingerprintTest {
    @Test
    void normalizedLogicIgnoresLocalIdentifierSpellingButExactHashDoesNot() {
        J.MethodDeclaration left = method(
                "private static int compute(int a, int b) { return (a + b) * 31; }");
        J.MethodDeclaration right = method(
                "private static int compute(int x, int y) { return (x + y) * 31; }");

        M3IndexDbSemanticFingerprint l = M3JavaSemanticHasher.fingerprint("METHOD", left);
        M3IndexDbSemanticFingerprint r = M3JavaSemanticHasher.fingerprint("METHOD", right);

        assertNotEquals(l.exactSha256(), r.exactSha256());
        assertEquals(l.structuralSha256(), r.structuralSha256());
        assertEquals(l.logicSha256(), r.logicSha256());
        assertEquals(l.logicHash64(), r.logicHash64());
        assertEquals(l.simHash64(), r.simHash64());
    }

    @Test
    void compositionIsDeterministicAndOrderSensitive() {
        var a = M3IndexDbSemanticFingerprint.leaf("ATOM", List.of("A"), List.of("ADD"), "a+b");
        var b = M3IndexDbSemanticFingerprint.leaf("ATOM", List.of("B"), List.of("MUL"), "a*b");

        var ab1 = M3IndexDbSemanticFingerprint.compose(
                "METHOD",
                List.of(
                        new M3IndexDbSemanticFingerprint.Component("ATOM", a),
                        new M3IndexDbSemanticFingerprint.Component("ATOM", b)));
        var ab2 = M3IndexDbSemanticFingerprint.compose(
                "METHOD",
                List.of(
                        new M3IndexDbSemanticFingerprint.Component("ATOM", a),
                        new M3IndexDbSemanticFingerprint.Component("ATOM", b)));
        var ba = M3IndexDbSemanticFingerprint.compose(
                "METHOD",
                List.of(
                        new M3IndexDbSemanticFingerprint.Component("ATOM", b),
                        new M3IndexDbSemanticFingerprint.Component("ATOM", a)));

        assertEquals(ab1, ab2);
        assertNotEquals(ab1.logicSha256(), ba.logicSha256());
        assertNotEquals(ab1.structuralSha256(), ba.structuralSha256());
    }

    private static J.MethodDeclaration method(String declaration) {
        String source = "package x; final class A { " + declaration + " }";
        var context = new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
        J.CompilationUnit cu = (J.CompilationUnit) JavaParser.fromJavaVersion()
                .build()
                .parseInputs(
                        List.of(Parser.Input.fromString(Path.of("src/main/java/x/A.java"), source)),
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
