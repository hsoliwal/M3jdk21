// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
import com.sun.management.ThreadMXBean;
import java.lang.M3StringRegexRecoveryTest;
import java.lang.management.ManagementFactory;
import java.util.Arrays;

/** Prepared actual-owner kernel cost, with test MethodHandle invocation included. */
public final class RegexJavaBenchmark {
    private RegexJavaBenchmark() { }

    public static void main(String[] args) throws Throwable {
        if (args.length != 0) throw new IllegalArgumentException("no arguments");
        ThreadMXBean memory = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!memory.isThreadAllocatedMemorySupported()) {
            throw new AssertionError("allocation counter required");
        }
        memory.setThreadAllocatedMemoryEnabled(true);
        long thread = Thread.currentThread().threadId();
        for (String scenario : new String[]{"dense-live", "early-dead", "incoming-dead",
                "late-dead", "small-live"}) {
            for (int width : new int[]{1, 2}) {
                long prepareStart = System.nanoTime();
                long beforePreparation = memory.getThreadAllocatedBytes(thread);
                try (M3StringRegexRecoveryTest.BenchmarkFixture fixture =
                        M3StringRegexRecoveryTest.benchmark(scenario, width)) {
                    long preparedBytes = memory.getThreadAllocatedBytes(thread) - beforePreparation;
                    long prepareNs = System.nanoTime() - prepareStart;
                    long start = System.nanoTime();
                    fixture.run(1);
                    long firstNs = System.nanoTime() - start;
                    fixture.run(2_000);
                    long[] times = new long[7];
                    long[] allocations = new long[7];
                    for (int sample = 0; sample < times.length; sample++) {
                        long allocatedBefore = memory.getThreadAllocatedBytes(thread);
                        start = System.nanoTime();
                        fixture.run(5_000);
                        times[sample] = System.nanoTime() - start;
                        allocations[sample] = memory.getThreadAllocatedBytes(thread) - allocatedBefore;
                    }
                    Arrays.sort(times);
                    Arrays.sort(allocations);
                    System.out.println("M3_REGEX_JAVA_BENCH scenario=" + scenario + " width=" + width
                            + " units=8192 tokens=" + (scenario.equals("small-live") ? 7 : 32)
                            + " warmup_calls=2000 samples=7 calls_per_sample=5000"
                            + " prepare_ns=" + prepareNs + " prepare_allocated_bytes=" + preparedBytes
                            + " first_ns=" + firstNs + " median_ns_per_call=" + times[3] / 5_000.0
                            + " min_ns_per_call=" + times[0] / 5_000.0
                            + " median_allocated_bytes_per_call=" + allocations[3] / 5_000.0
                            + " fixture_native_bytes=" + fixture.nativeBytes()
                            + " method_handle_invocation_included=true actual_owner=true");
                }
            }
        }
    }
}
