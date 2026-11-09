// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.ast;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Maven/JUnit entry point for the independent scalar cyclic-CFG oracle. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/56cbdc6312594f06e21ef56f0ce33b605383270b/synexia-openrewrite-recipes/crates/m3-kotlin-null-flow-20261031/src/test/java/com/m3/ast/internal/M3NullFlowWorklistTest.java blob e008bc0c68fa; recipe m3/ast/recipe/null-flow-20261107/relocate.py v4
final class NullFlowWorklistTest {
    @Test void seededWorklistAndScalarHaveIdenticalFixedPoints() {
        assertTrue(NullFlowWorklistProbe.prove() > 100_000L);
    }
}
