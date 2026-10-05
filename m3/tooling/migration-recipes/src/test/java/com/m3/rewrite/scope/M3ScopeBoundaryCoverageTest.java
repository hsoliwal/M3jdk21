// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.scope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Boundary-complete tests for the exact physical-scope classifier; no production exemptions. */
final class M3ScopeBoundaryCoverageTest {
    @Test
    void everySupportedRepositoryLayoutIsClassifiedAtItsNarrowestBoundary() {
        assertEquals(M3EditScope.FILE, M3ScopeInference.forPaths(List.of("README.md")));
        assertEquals(M3EditScope.FILE, M3ScopeInference.forPaths(List.of("docs/readme.md")));
        assertEquals(M3EditScope.FILE, M3ScopeInference.forPaths(List.of("make/Makefile")));
        assertEquals(M3EditScope.FILE, M3ScopeInference.forPaths(List.of("make/modules/java.base/Java.gmk")));
        assertEquals(M3EditScope.FILE, M3ScopeInference.forPaths(List.of("test/jdk/Foo.java")));
        assertEquals(M3EditScope.FILE, M3ScopeInference.forPaths(List.of("src/java.base/share/classes/java/lang/String.java")));
        assertEquals(M3EditScope.FILE, M3ScopeInference.forPaths(List.of("src/java.base/share/native/libjava/String.c")));
        assertEquals(M3EditScope.FILE, M3ScopeInference.forPaths(List.of("src/java.base/Foo.java")));
        assertEquals(M3EditScope.FILE, M3ScopeInference.forPaths(List.of("src/main/java/a/A.java")));
        assertEquals(M3EditScope.FILE, M3ScopeInference.forPaths(List.of("src/test/java/a/A.java")));

        assertEquals(M3EditScope.PACKAGE, M3ScopeInference.forPaths(List.of(
                "src/java.base/share/classes/java/lang/String.java",
                "src/java.base/share/classes/java/lang/Object.java")));
        assertEquals(M3EditScope.MODULE, M3ScopeInference.forPaths(List.of(
                "src/java.base/share/classes/java/lang/String.java",
                "src/java.base/share/classes/java/util/List.java")));
        assertEquals(M3EditScope.MODULE, M3ScopeInference.forPaths(List.of(
                "src/java.base/share/classes/java/lang/String.java",
                "src/java.base/linux/classes/java/lang/Linux.java")));
        assertEquals(M3EditScope.MULTI_MODULE, M3ScopeInference.forPaths(List.of(
                "src/java.base/share/classes/java/lang/String.java",
                "src/jdk.compiler/share/classes/com/sun/tools/javac/Main.java")));
        assertEquals(M3EditScope.PACKAGE, M3ScopeInference.forPaths(List.of(
                "make/modules/java.base/A.gmk", "make/modules/java.base/B.gmk")));
        assertEquals(M3EditScope.MULTI_MODULE, M3ScopeInference.forPaths(List.of(
                "make/modules/java.base/A.gmk", "make/modules/jdk.compiler/B.gmk")));
        assertEquals(M3EditScope.PACKAGE, M3ScopeInference.forPaths(List.of(
                "docs/a.txt", "docs/b.txt")));
        assertEquals(M3EditScope.MULTI_MODULE, M3ScopeInference.forPaths(List.of(
                "docs/a.txt", "other/b.txt")));
    }

    @Test
    void everyCanonicalPathRefusalIsExercisedIndependently() {
        for (String invalid : List.of(
                "", "   ", "/abs/Foo.java", "C:/src/Foo.java", "C:\\src\\Foo.java",
                "a//Foo.java", "a/../Foo.java", "../Foo.java", "a/Foo.java/..",
                "a/./Foo.java", "./Foo.java", "a/", "a/b/", "src/", "test/jdk", "make/modules/java.base")) {
            assertThrows(IllegalArgumentException.class,
                    () -> M3ScopeInference.forPaths(List.of(invalid)), invalid);
        }
        String nul = "a/" + (char) 0 + "Foo.java";
        assertThrows(IllegalArgumentException.class,
                () -> M3ScopeInference.forPaths(List.of(nul)));
        assertThrows(NullPointerException.class, () -> M3ScopeInference.forPaths(null));
        assertThrows(IllegalArgumentException.class, () -> M3ScopeInference.forPaths(List.of()));
        assertThrows(NullPointerException.class,
                () -> M3ScopeInference.forPaths(java.util.Collections.singletonList(null)));
    }

    @Test
    void javaOnlyEntryPointAcceptsBothSeparatorsAndRejectsNonJavaTargets() {
        assertEquals(M3EditScope.FILE,
                M3ScopeInference.forJavaPaths(List.of("src\\main\\java\\a\\A.java")));
        assertEquals(M3EditScope.PACKAGE,
                M3ScopeInference.forJavaPaths(List.of(
                        "src/main/java/a/A.java", "src/main/java/a/B.java")));
        assertThrows(NullPointerException.class, () -> M3ScopeInference.forJavaPaths(null));
        assertThrows(IllegalArgumentException.class, () -> M3ScopeInference.forJavaPaths(List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> M3ScopeInference.forJavaPaths(List.of("src/main/java/a/A.kt")));
    }

    @Test
    void policyContractModeAndScopeEdgesAreObservable() {
        var explicit = new M3RecipeScopePolicy(
                M3EditScope.LIBRARY_API, M3ContractMode.EXPLICIT_CONTRACT_CHANGE, false);
        assertEquals(M3EditScope.LIBRARY_API,
                explicit.resolve(List.of("src/main/java/a/A.java")));
        assertTrue(!explicit.fileLocalMechanical(List.of("src/main/java/a/A.java")));

        var inferred = new M3RecipeScopePolicy(
                M3EditScope.FILE, M3ContractMode.BEHAVIOR_AND_CONTRACT_PRESERVING, true);
        assertTrue(inferred.fileLocalMechanical(List.of("src/main/java/a/A.java")));
        assertTrue(!inferred.fileLocalMechanical(List.of(
                "src/main/java/a/A.java", "src/main/java/a/B.java")));

        assertThrows(NullPointerException.class,
                () -> new M3RecipeScopePolicy(null,
                        M3ContractMode.BEHAVIOR_AND_CONTRACT_PRESERVING, false));
        assertThrows(NullPointerException.class,
                () -> new M3RecipeScopePolicy(M3EditScope.FILE, null, false));
    }
}
