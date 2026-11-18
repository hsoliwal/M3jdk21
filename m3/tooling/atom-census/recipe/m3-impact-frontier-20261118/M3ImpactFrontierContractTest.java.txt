/* SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.tooling.atoms.contract;

import org.junit.jupiter.api.Test;

/** Maven/JUnit binding to the same dependency-free refusal and closure corpus. */
final class M3ImpactFrontierContractTest {
    @Test
    void deterministicFileAndNativeImpactIsFailClosed() throws Exception {
        M3ImpactFrontierSelfTest.main(new String[0]);
    }
}
