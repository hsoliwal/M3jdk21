// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.optane;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import test.PmemCheckpointStreamSmokeTest;

/** Product-owned Maven entry points for the checkpoint stream contract oracle. */
final class PmemCheckpointStreamJUnitTest {
    @Test
    void verifiesFilePublicationRecoveryAndStreamOwnership() throws Exception {
        PmemCheckpointStreamSmokeTest.main(new String[0]);
    }

    @Test
    @EnabledIfSystemProperty(named = "pmem.streaming.native", matches = "true")
    void verifiesPosixPublicationRecoveryAndStreamOwnership() throws Exception {
        PmemCheckpointStreamSmokeTest.main(new String[]{"--require-posix"});
    }
}
