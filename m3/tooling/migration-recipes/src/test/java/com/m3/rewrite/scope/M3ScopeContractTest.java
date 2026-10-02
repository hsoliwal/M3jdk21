// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.scope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

final class M3ScopeContractTest {
    @Test
    void scopeOrderingPromotionAndNextAreMechanical() {
        M3EditScope[] values = M3EditScope.values();
        for (M3EditScope granted : values) {
            for (M3EditScope required : values) {
                assertEquals(granted.ordinal() >= required.ordinal(), granted.permits(required));
                assertEquals(
                        granted.ordinal() >= required.ordinal() ? granted : required,
                        granted.promote(required));
            }
        }

        assertEquals(M3EditScope.VISIBILITY, M3EditScope.FILE.next());
        assertEquals(M3EditScope.PACKAGE, M3EditScope.VISIBILITY.next());
        assertEquals(M3EditScope.MODULE, M3EditScope.PACKAGE.next());
        assertEquals(M3EditScope.MULTI_MODULE, M3EditScope.MODULE.next());
        assertEquals(M3EditScope.LIBRARY_API, M3EditScope.MULTI_MODULE.next());
        assertEquals(M3EditScope.LIBRARY_API, M3EditScope.LIBRARY_API.next());
    }

    @Test
    void sourcePathInferenceKeepsIndependentFilesAtTheNarrowestBoundary() {
        assertEquals(
                M3EditScope.FILE,
                M3ScopeInference.forJavaPaths(List.of("src/main/java/com/acme/Foo.java")));
        assertEquals(
                M3EditScope.FILE,
                M3ScopeInference.forJavaPaths(List.of("src\\test\\java\\com\\acme\\FooTest.java")));
        assertEquals(
                M3EditScope.PACKAGE,
                M3ScopeInference.forJavaPaths(List.of(
                        "src/main/java/com/acme/Foo.java",
                        "src/main/java/com/acme/Bar.java")));
        assertEquals(
                M3EditScope.MODULE,
                M3ScopeInference.forJavaPaths(List.of(
                        "src/main/java/com/acme/Foo.java",
                        "src/main/java/com/acme/internal/Bar.java")));
        assertEquals(
                M3EditScope.MODULE,
                M3ScopeInference.forJavaPaths(List.of(
                        "src/main/java/com/acme/Foo.java",
                        "src/test/java/com/acme/FooTest.java")));
    }

    @Test
    void invalidInferenceInputsAreRefused() {
        assertThrows(NullPointerException.class, () -> M3ScopeInference.forJavaPaths(null));
        assertThrows(IllegalArgumentException.class, () -> M3ScopeInference.forJavaPaths(List.of()));
        assertThrows(
                NullPointerException.class,
                () -> M3ScopeInference.forJavaPaths(java.util.Collections.singletonList(null)));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3ScopeInference.forJavaPaths(List.of("README.md")));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3ScopeInference.forJavaPaths(List.of("src/main/java/com/acme/../Foo.java")));
    }

    @Test
    void policyPromotesOnlyAsFarAsItsTargetsRequire() {
        var inferred = new M3RecipeScopePolicy(
                M3EditScope.FILE,
                M3ContractMode.BEHAVIOR_AND_CONTRACT_PRESERVING,
                true);
        assertEquals(
                M3EditScope.FILE,
                inferred.resolve(List.of("src/main/java/a/A.java")));
        assertEquals(
                M3EditScope.PACKAGE,
                inferred.resolve(List.of(
                        "src/main/java/a/A.java",
                        "src/main/java/a/B.java")));
        assertEquals(
                M3EditScope.MODULE,
                inferred.resolve(List.of(
                        "src/main/java/a/A.java",
                        "src/main/java/b/B.java")));

        var visibilityFloor = new M3RecipeScopePolicy(
                M3EditScope.VISIBILITY,
                M3ContractMode.BEHAVIOR_AND_CONTRACT_PRESERVING,
                true);
        assertEquals(
                M3EditScope.VISIBILITY,
                visibilityFloor.resolve(List.of("src/main/java/a/A.java")));

        var moduleFloor = new M3RecipeScopePolicy(
                M3EditScope.MODULE,
                M3ContractMode.BEHAVIOR_AND_CONTRACT_PRESERVING,
                true);
        assertEquals(
                M3EditScope.MODULE,
                moduleFloor.resolve(List.of("src/main/java/a/A.java")));
    }
}
