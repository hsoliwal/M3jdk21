// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.jni;

import org.junit.jupiter.api.Test;

//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/64652588f0d4c23b1f6e0411090b2a796325ae0e/synexia-primitives/src/test/java/com/synexia/primitives/M3LeaseScopesTest.java blob ef2bb6ffa26e; recipe m3/native/recipe/scoped-leases-20261106/relocate.py v1
class M3LeaseScopesTest {
    @Test void nestedExceptionalAndConcurrentLeaseBalance() throws Exception {
        M3LeaseScopesSelfTest.main(new String[0]);
    }
}
