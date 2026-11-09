// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.intern;
import org.junit.jupiter.api.Test;

/** Scoped JUnit5 entrypoint for source-sealed pinned mmap regression. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/d5ad5090d87ef8477a56317e60ac0d29c65029f7/synexia-openrewrite-recipes/recipe-crates/m3-classspace-pinned-mapped-20261030/src/test/java/com/synexia/m3/classspace/M3ClassSpacePinnedMappedJUnitTest.java blob a63a52cebadf; recipe m3/intern/recipe/class-space-20261107/relocate.py v1
final class ClassSpacePinnedMappedJUnitTest {
    @Test void verifiesPinnedViewsAndExternalChanges() throws Exception {
        ClassSpacePinnedMappedSelfTest.main(new String[0]);
    }
}
