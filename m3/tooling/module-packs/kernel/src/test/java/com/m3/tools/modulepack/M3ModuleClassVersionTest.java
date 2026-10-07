// SPDX-License-Identifier: Apache-2.0
package com.m3.tools.modulepack;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Real JUnit wrapper; the standalone runner does not claim to execute this class. */
final class M3ModuleClassVersionTest {
    @TempDir Path directory;

    @Test
    void effectiveJarAndJmodVersionsAgreeWithJava21Vm() throws Exception {
        assertEquals(639, M3ModuleClassVersionChecks.run(directory));
    }
}
