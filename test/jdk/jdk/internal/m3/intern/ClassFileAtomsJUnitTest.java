// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.intern;

import org.junit.jupiter.api.Test;

/** JUnit admission for the same compiled Java 21 deterministic self-test oracle. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/d5ad5090d87ef8477a56317e60ac0d29c65029f7/synexia-openrewrite-recipes/recipe-crates/m3-class-space-symbol-atoms-20261030/src/test/java/com/synexia/m3/classspace/M3ClassFileAtomsJUnitTest.java blob 7ff77c774a55; recipe make/m3/intern/recipe/class-space-20261107/relocate.py v1
final class ClassFileAtomsJUnitTest {
    @Test
    void canonicalSignalsAndExactInterning() throws Exception {
        ClassFileAtomsSelfTest.main(new String[0]);
    }
}
