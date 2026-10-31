// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
// Derivation: AUTHORED_JUNIT_ADAPTER_FOR_EXTRACTED_GROUPS; see SOURCE_PROVENANCE.tsv.
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.projection;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** JUnit host for the fourteen retained incumbent projection contract groups. */
final class ExtractiveProjectionTest {
    @Test
    void existingProjectionContracts(@TempDir Path directory) throws Exception {
        ExtractiveProjectionSelfTest.main(new String[] {directory.toString()});
    }
}
