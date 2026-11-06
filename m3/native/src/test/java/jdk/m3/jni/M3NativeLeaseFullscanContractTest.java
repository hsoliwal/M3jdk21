// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.jni;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/** Actual JNI/Java parity and code-carrier proof when the native library is linked. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/64652588f0d4c23b1f6e0411090b2a796325ae0e/synexia-primitives/src/test/java/com/synexia/primitives/M3NativeLeaseFullscanContractTest.java blob 568ad8e3aad7; recipe m3/native/recipe/scoped-leases-20261106/relocate.py v1
final class M3NativeLeaseFullscanContractTest {
    @Test void fullscanNativePreflightAndCodeCarrierParity() throws Exception {
        boolean available = M3NativeLeaseScan.nativeAvailable();
        if (Boolean.getBoolean("jdk.m3.jni.requireNative")) {
            assertTrue(available, "native-required verification must not silently skip JNI");
        }
        Assumptions.assumeTrue(available, "optional native peer unavailable");
        M3NativeLeasePreflightOracle.main(new String[]{"candidate"});
        M3NativeLeaseCodeCarrierOracle.main(new String[0]);
        M3NativeLeasePreflightOracle.main(new String[]{"candidate"});
    }
}
