// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.jni;

import org.junit.jupiter.api.Test;

//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/64652588f0d4c23b1f6e0411090b2a796325ae0e/synexia-primitives/src/test/java/com/synexia/primitives/M3ScopedLeaseWindowTest.java blob 18e1b9080358; recipe m3/native/recipe/scoped-leases-20261106/relocate.py v1
class M3ScopedLeaseWindowTest {
    @Test void boundedScansMatchLegacyAndOptionalJni() {
        M3ScopedLeaseWindowSelfTest.main(new String[0]);
    }
}
