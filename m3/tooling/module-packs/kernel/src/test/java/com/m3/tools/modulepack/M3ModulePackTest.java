// SPDX-License-Identifier: Apache-2.0
package com.m3.tools.modulepack;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Real JUnit entry point sharing precisely the offline contract-case implementation. */
final class M3ModulePackTest {
    @TempDir Path work;

    @Test
    void inspectResolveAndRefuseInvalidPacks() throws Exception {
        assertTrue(M3ModuleChecks.run(work) >= 40);
    }
}
