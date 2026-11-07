// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.optane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Surefire entry of the donor oracle {@link M3PmemAcceleratorSelfTest} (recipe m3/optane/recipe/pmem-accelerator-20261107,
 * census L008): runs its main once and asserts the printed figure. Needs libm3pmemaccel.so on java.library.path
 * (Linux; the CMake target of src/main/native), so it runs under the profile m3-optane-native only
 * (-Dpmem.accelerator.native=true); the default filter excludes it.
 */
//FLLDCIM: RELOCATE - M3JDK-authored JUnit entry of recipe m3/optane/recipe/pmem-accelerator-20261107 (census L008); the oracle is the donor's M3PmemAcceleratorSelfTest
final class M3PmemAcceleratorJUnitTest {
    /** The donor oracle's own counter: 2,048 column-order cases, the tile lanes x rows, 44 parities and the fixed checks. */
    static final String FIGURE = "PASS m3-pmem Java21/JNI cases=";
    /** The figure the donor prints (develop 56cbdc631259, JIT and -Xint, WSL Ubuntu 24.04 / Temurin 21.0.13, 2026-11-07). */
    static final int CASES = 7879;

    @Test
    @EnabledIfSystemProperty(named = "pmem.accelerator.native", matches = "true")
    void donorSelfTestPasses() throws Exception {
        PrintStream original = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try (PrintStream sink = new PrintStream(captured, true, StandardCharsets.UTF_8)) {
            System.setOut(sink);
            M3PmemAcceleratorSelfTest.main(new String[0]);
        } finally {
            System.setOut(original);
        }
        String out = captured.toString(StandardCharsets.UTF_8).trim();
        assertTrue(out.startsWith(FIGURE), out);
        int cases = Integer.parseInt(out.substring(FIGURE.length()).trim());
        assertEquals(CASES, cases, out);
    }
}
