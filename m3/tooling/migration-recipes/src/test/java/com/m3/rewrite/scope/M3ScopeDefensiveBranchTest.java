// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.scope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

final class M3ScopeDefensiveBranchTest {
    @Test
    void openJdkAndBuildPathFallbacksCoverNonClassAndEmptyRelativeBranches() {
        assertEquals(
                M3EditScope.PACKAGE,
                M3ScopeInference.forPaths(List.of(
                        "src/demo/share/native/A.txt",
                        "src/demo/share/native/B.txt")));

        assertThrows(
                IllegalArgumentException.class,
                () -> M3ScopeInference.forPaths(List.of("test/")));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3ScopeInference.forPaths(List.of("test/jdk/java/")));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3ScopeInference.forPaths(List.of("make/modules/")));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3ScopeInference.forPaths(List.of("src/")));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3ScopeInference.forPaths(List.of("src/demo/")));
    }
}
