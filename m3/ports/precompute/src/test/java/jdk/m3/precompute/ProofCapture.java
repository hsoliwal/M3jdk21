// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Runs a relocated proof main (gate 2 of the UTF-16 atom relocation) with {@code System.out} captured, restores the
 * stream, echoes the captured receipt line to the real output so the surefire log carries the figures, and returns
 * it trimmed for the wrapper test's assertion. Test scope only.
 */
final class ProofCapture {
    private ProofCapture() { }

    /** A proof main invocation. */
    interface Proof {
        void run() throws Exception;
    }

    static String run(Proof proof) throws Exception {
        PrintStream saved = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (PrintStream capture = new PrintStream(buffer, true, StandardCharsets.UTF_8)) {
            System.setOut(capture);
            proof.run();
        } finally {
            System.setOut(saved);
        }
        String line = buffer.toString(StandardCharsets.UTF_8).trim();
        saved.println(line);
        return line;
    }
}
