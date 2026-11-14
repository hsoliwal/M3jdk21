// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.intern;

import org.junit.jupiter.api.Test;

/** JUnit 5 wrapper over the standalone deterministic proof. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/d5ad5090d87ef8477a56317e60ac0d29c65029f7/synexia-openrewrite-recipes/recipe-crates/m3-classspace-region-intern-20261030/src/test/java/com/synexia/m3/classspace/M3ClassSpaceRegionJUnitTest.java blob 413efb10d9e2; recipe make/m3/intern/recipe/class-space-20261107/relocate.py v1
final class ClassSpaceRegionJUnitTest {
    @Test void allContracts() throws Exception {
        ClassSpaceRegionSelfTest.main(new String[0]);
    }
}
