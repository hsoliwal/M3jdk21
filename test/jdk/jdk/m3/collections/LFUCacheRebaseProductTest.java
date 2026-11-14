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

/** Original-first identity LFU behavior and exact timestamp-overflow parity. */
final class LFUCacheRebaseProductTest {
    private static final String EXPECTED_TRACE =
            "LFU_ROLLOVER_TRACE_SHA256=c066a180dd7b76c25f0e1ed406bdbc1ec879840c6cb8a85ebd2c0f364bb5b442";

    @Test
    void preservesIdentityKeysFrequenciesAndEvictionOrder() throws Exception {
        // The independent Synexia reference oracle, now run against the
        // original jdk.m3.collections receiver and required ArrayNative JNI.
        LFUCacheContract.main(new String[0]);
    }

    @Test
    void preservesRepeatedTimestampRolloverState() throws Exception {
        PrintStream previous = System.out;
        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        try (PrintStream output = new PrintStream(capture, true, StandardCharsets.UTF_8)) {
            System.setOut(output);
            LFUOverflowTrace.main(new String[0]);
        } finally {
            System.setOut(previous);
        }
        assertEquals(EXPECTED_TRACE, capture.toString(StandardCharsets.UTF_8).trim());
    }
}
