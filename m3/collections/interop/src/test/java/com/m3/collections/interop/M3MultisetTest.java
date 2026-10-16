/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections.interop;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
final class M3MultisetTest {
    @Test void actualGuavaAndStorageContracts() throws Exception {
        assertTrue(M3MultisetContract.run(false)>0);
    }
}
