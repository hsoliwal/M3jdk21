// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package test;

import jdk.m3.optane.PmemSdk;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Maven entrypoints; unavailable optional native backends are reported as skipped. */
public final class PmemNativeOpenJUnitTest {
    private static void linux() {
        assumeTrue(System.getProperty("os.name").equals("Linux"), "Linux no-follow contract");
    }

    @Test void fileOpenContracts() throws Exception {
        linux();
        PmemNativeOpenContractTest.main(new String[] {"FILE"});
    }

    @Test void posixOpenContracts() throws Exception {
        linux();
        assumeTrue(PmemSdk.capabilities().nativeAvailable(), "m3pmemsdk JNI library required");
        PmemNativeOpenContractTest.main(new String[] {"POSIX"});
    }

    @Test void autoOpenContracts() throws Exception {
        linux();
        assumeTrue(PmemSdk.capabilities().nativeAvailable(), "m3pmemsdk JNI library required");
        PmemNativeOpenContractTest.main(new String[] {"AUTO"});
    }

    @Test void pmdkOpenContracts() throws Exception {
        linux();
        assumeTrue(PmemSdk.capabilities().libpmem2Compiled(), "real libpmem2 build required");
        PmemNativeOpenContractTest.main(new String[] {"PMDK"});
    }
}
