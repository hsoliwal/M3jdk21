// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.jni;

import org.junit.jupiter.api.Test;

/** Canonical Maven/JUnit bridge to the standalone original-owner Java21 oracle. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/64652588f0d4c23b1f6e0411090b2a796325ae0e/synexia-primitives/src/test/java/com/synexia/primitives/M3JniArrayBatchTest.java blob 3a80882cb016; recipe m3/native/recipe/scoped-leases-20261106/relocate.py v1
final class JniArrayBatchTest {
    @Test void allManagedBatchAndNativeSelectorContracts() throws Exception {
        JniArrayBatchSelfTest.main(new String[0]);
    }
}
