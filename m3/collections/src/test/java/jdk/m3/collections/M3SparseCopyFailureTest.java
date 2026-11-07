/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import java.lang.ref.Reference;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Retains the actual constrained-heap counterexample; no allocation stub can mask it. */
final class M3SparseCopyFailureTest {
    @Test void failedSparseCopyPreservesSourceDestinationAndExposedAlias() throws Exception {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        String classpath = System.getProperty("surefire.test.class.path",
                System.getProperty("java.class.path"));
        Process child = new ProcessBuilder(java, "-Xmx32m", "-cp", classpath,
                AllocationWitness.class.getName()).redirectErrorStream(true).start();
        boolean completed = child.waitFor(30, TimeUnit.SECONDS);
        if (!completed) child.destroyForcibly();
        assertTrue(completed, "bounded heap witness timed out");
        String output = new String(child.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, child.exitValue(), output);
        assertTrue(output.contains("ALLOCATION_FAILED_VALUES_PRESERVED"), output);
    }

    public static final class AllocationWitness {
        private AllocationWitness() { }
        public static void main(String[] args) {
            int page = 1 << 20;
            BigArray source = BigArray.sparse(PrimitiveKind.LONG, 2L * page, 20);
            BigArray target = BigArray.sparse(PrimitiveKind.LONG, 2L * page, 20);
            source.setBits(page, 17);
            target.setBits(0, 29);
            long[] alias = (long[]) target.segmentArray(0);
            byte[] retained = new byte[8 << 20];
            boolean allocationFailed = false;
            try {
                source.copyTo(0, target, 0, 2L * page);
            } catch (OutOfMemoryError expected) {
                allocationFailed = true;
            }
            if (!allocationFailed || source.getBits(page) != 17 || target.getBits(0) != 29
                    || target.getBits(page) != 0 || alias[0] != 29 || target.length() != 2L * page) {
                throw new AssertionError("copy published values before allocation completed");
            }
            Reference.reachabilityFence(alias);
            Reference.reachabilityFence(retained);
            System.out.println("ALLOCATION_FAILED_VALUES_PRESERVED");
        }
    }
}
