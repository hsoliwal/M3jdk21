// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.scope;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        assertEquals(M3EditScope.PACKAGE_VISIBILITY, M3EditScope.FILE.next());
        assertEquals(M3EditScope.MODULE, M3EditScope.PACKAGE_VISIBILITY.next());
        assertEquals(M3EditScope.MULTI_MODULE, M3EditScope.MODULE.next());
        assertEquals(M3EditScope.LIBRARY_API, M3EditScope.MULTI_MODULE.next());
        assertEquals(M3EditScope.LIBRARY_API, M3EditScope.LIBRARY_API.next());
    }

    @Test
    void sourcePathInferenceKeepsIndependentFilesAtTheNarrowestBoundary() {
        assertEquals(
                M3EditScope.FILE,
                M3ScopeInference.forJavaPaths(
                        List.of("src/main/java/com/acme/Foo.java")));
        assertEquals(
                M3EditScope.FILE,
                M3ScopeInference.forJavaPaths(
                        List.of("src\\test\\java\\com\\acme\\FooTest.java")));
        assertEquals(
                M3EditScope.PACKAGE_VISIBILITY,
                M3ScopeInference.forJavaPaths(
                        List.of(
                                "src/main/java/com/acme/Foo.java",
                                "src/main/java/com/acme/Bar.java")));
        assertEquals(
                M3EditScope.PACKAGE_VISIBILITY,
                M3ScopeInference.forJavaPaths(
                        List.of(
                                "src/main/java/Foo.java",
                                "src/main/java/Bar.java")));
        assertEquals(
                M3EditScope.MODULE,
                M3ScopeInference.forJavaPaths(
                        List.of(
                                "src/main/java/com/acme/Foo.java",
                                "src/main/java/com/acme/internal/Bar.java")));
        assertEquals(
                M3EditScope.MODULE,
                M3ScopeInference.forJavaPaths(
                        List.of(
                                "src/main/java/com/acme/Foo.java",
                                "src/test/java/com/acme/FooTest.java")));
    }

    @Test
    void invalidInferenceInputsAreRefused() {
        assertThrows(
                NullPointerException.class,
                () -> M3ScopeInference.forJavaPaths(null));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3ScopeInference.forJavaPaths(List.of()));
        assertThrows(
                NullPointerException.class,
                () -> M3ScopeInference.forJavaPaths(java.util.Collections.singletonList(null)));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3ScopeInference.forJavaPaths(List.of("README.md")));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3ScopeInference.forJavaPaths(
                        List.of("src/main/java/com/acme/../Foo.java")));
    }

    @Test
    void grantedScopeMustCoverTheDeclaredRecipeScope() {
        M3ScopedRecipe fileRecipe = recipe(
                M3EditScope.FILE,
                M3ContractMode.BEHAVIOR_AND_CONTRACT_PRESERVING);
        assertTrue(fileRecipe.fileLocalMechanical());
        assertDoesNotThrow(() -> fileRecipe.requireGrantedScope(M3EditScope.FILE));
        assertDoesNotThrow(() -> fileRecipe.requireGrantedScope(M3EditScope.MODULE));

        M3ScopedRecipe moduleRecipe = recipe(
                M3EditScope.MODULE,
                M3ContractMode.BEHAVIOR_AND_CONTRACT_PRESERVING);
        assertFalse(moduleRecipe.fileLocalMechanical());
        assertThrows(
                IllegalArgumentException.class,
                () -> moduleRecipe.requireGrantedScope(M3EditScope.PACKAGE_VISIBILITY));
        assertDoesNotThrow(() -> moduleRecipe.requireGrantedScope(M3EditScope.MODULE));
    }

    @Test
    void contractChangingRecipeNeedsExplicitLibraryApiAuthority() {
        M3ScopedRecipe changingRecipe = recipe(
                M3EditScope.FILE,
                M3ContractMode.EXPLICIT_CONTRACT_CHANGE);
        assertFalse(changingRecipe.fileLocalMechanical());
        assertThrows(
                IllegalArgumentException.class,
                () -> changingRecipe.requireGrantedScope(M3EditScope.MULTI_MODULE));
        assertDoesNotThrow(
                () -> changingRecipe.requireGrantedScope(M3EditScope.LIBRARY_API));
    }

    private static M3ScopedRecipe recipe(
            M3EditScope requiredScope,
            M3ContractMode contractMode) {
        return new M3ScopedRecipe() {
            @Override
            public M3EditScope requiredScope() {
                return requiredScope;
            }

            @Override
            public M3ContractMode contractMode() {
                return contractMode;
            }
        };
    }
}
