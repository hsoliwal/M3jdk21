// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.jni;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * Gate 2 of the relocation on the Java path: the donor probe's 1,025 concat inputs through concat(false) and
 * concat(true) (the exact Java fallback when the optional library is absent), and the donor self-tests' full and
 * window scans, replayed by the differential driver against this module's own classes. Prints the parity figure
 * and the digest of the replay (the same text the differential gate compares with the donor's).
 */
//FLLDCIM: RELOCATE - gate 2 wrapper of recipe m3/native/recipe/scoped-leases-20261106 (new, test scope)
final class ScopedLeaseJavaParityTest {
    @Test void javaPathConcatParityAndScansOfTheRelocatedFamily() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ScopedLeaseDump dump = new ScopedLeaseDump(ScopedLeaseTable.class.getPackageName(),
                new PrintStream(bytes, true, StandardCharsets.UTF_8));
        int cases = dump.concatParity();
        dump.deterministicScan();
        dump.windowScans();
        String replay = bytes.toString(StandardCharsets.UTF_8);
        assertEquals(ScopedLeaseDump.CONCAT_CASES, cases);
        assertTrue(replay.contains("CONCAT_PARITY cases=1025 native=" + JniByteConcat.nativeLibraryLoaded()));
        assertTrue(replay.contains("window cap=1024 "));
        System.out.println("M3_JNI_CONCAT_JAVA_PARITY_PASS cases=" + cases + " native="
                + JniByteConcat.nativeLibraryLoaded() + " replay_lines=" + replay.lines().count()
                + " replay_sha256_16=" + ScopedLeaseDump.sha16(bytes.toByteArray()));
    }
}
