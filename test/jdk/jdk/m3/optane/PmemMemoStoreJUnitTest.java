// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package test;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Maven acceptance of the independent FILE and explicitly enabled POSIX contracts. */
public final class PmemMemoStoreJUnitTest {
    @Test void fileContracts() throws Exception {
        PmemMemoStoreSmokeTest.main(new String[0]);
    }

    @Test void posixContracts() throws Exception {
        assumeTrue(Boolean.getBoolean("pmem.streaming.native"),
                "m3-optane-native profile required");
        PmemMemoStoreSmokeTest.main(new String[] {"--require-posix"});
    }
}
