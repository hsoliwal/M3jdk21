// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.intern;

import org.junit.jupiter.api.Test;

/** Standalone Maven JUnit 5 adapter; production mutation remains guarded by recipe seals. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/d5ad5090d87ef8477a56317e60ac0d29c65029f7/synexia-openrewrite-recipes/recipe-crates/m3-classspace-atomic-batch-20261030/src/test/java/com/synexia/m3/classspace/M3ClassSpaceAtomicBatchJUnitTest.java blob ef317504a3ab; recipe m3/intern/recipe/class-space-20261107/relocate.py v1
final class M3ClassSpaceAtomicBatchJUnitTest {
    @Test void allContracts() throws Exception {
        M3ClassSpaceAtomicBatchSelfTest.main(new String[0]);
    }
}
