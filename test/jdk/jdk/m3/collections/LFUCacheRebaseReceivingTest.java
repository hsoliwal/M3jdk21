/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

/** Receiving checks for the exact source-sealed primitive-array LFU owner. */
final class LFUCacheRebaseReceivingTest {
    @Test
    void retainsOriginalIdentityFrequencyEvictionAndFactoryContracts() throws Exception {
        LFUCacheContract.main(new String[0]);
    }

    @Test
    @ResourceLock(value = Resources.SYSTEM_OUT, mode = ResourceAccessMode.READ_WRITE)
    void preservesExactForcedRolloverState() throws Exception {
        PrintStream previous = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try (PrintStream sink = new PrintStream(captured, true, StandardCharsets.UTF_8)) {
            try {
                System.setOut(sink);
                LFUOverflowTrace.main(new String[0]);
            } finally {
                System.setOut(previous);
            }
        }
        assertEquals("LFU_ROLLOVER_TRACE_SHA256="
                + "c066a180dd7b76c25f0e1ed406bdbc1ec879840c6cb8a85ebd2c0f364bb5b442",
                captured.toString(StandardCharsets.UTF_8).trim());
    }
}
