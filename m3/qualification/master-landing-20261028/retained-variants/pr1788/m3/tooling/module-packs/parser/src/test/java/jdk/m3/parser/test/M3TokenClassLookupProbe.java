/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.parser.test;

import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;
import jdk.m3.parser.grammar.M3GrammarLexer;
import jdk.m3.parser.grammar.M3LexerProfile;
import jdk.m3.parser.grammar.M3TokenStream;

/** Observes public token-class traversal; resource timings are not assertions. */
public final class M3TokenClassLookupProbe {
    private static volatile long sink;

    private M3TokenClassLookupProbe() { }

    public static void main(String[] arguments) {
        int warmup = arguments.length > 0 ? Integer.parseInt(arguments[0]) : 256;
        int repeats = arguments.length > 1 ? Integer.parseInt(arguments[1]) : 1024;
        if (arguments.length > 2 || warmup < 1 || warmup > 10000
                || repeats < 1 || repeats > 10000) {
            throw new IllegalArgumentException("expected warmup/repeats in [1, 10000]");
        }
        ThreadMXBean measurements = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!measurements.isThreadAllocatedMemorySupported()) {
            throw new IllegalStateException("thread allocation measurement unsupported");
        }
        measurements.setThreadAllocatedMemoryEnabled(true);
        boolean cpu = measurements.isCurrentThreadCpuTimeSupported();
        if (cpu) measurements.setThreadCpuTimeEnabled(true);
        String source = "identifier + 123 ".repeat(2048);
        M3TokenStream tokens = new M3GrammarLexer(M3LexerProfile.builder("token-class-probe-v1")
                .operators("===", "==", "+", "-").build()).lex(source);
        for (int index = 0; index < warmup; index++) sink = traverse(tokens);
        long thread = Thread.currentThread().threadId();
        long allocatedBefore = measurements.getThreadAllocatedBytes(thread);
        long cpuBefore = cpu ? measurements.getCurrentThreadCpuTime() : 0;
        long started = System.nanoTime();
        long checksum = 0;
        for (int index = 0; index < repeats; index++) {
            long observed = traverse(tokens);
            checksum += observed;
            sink = observed;
        }
        long elapsed = System.nanoTime() - started;
        long cpuElapsed = cpu ? measurements.getCurrentThreadCpuTime() - cpuBefore : -1;
        long allocated = measurements.getThreadAllocatedBytes(thread) - allocatedBefore;
        long lookups = (long) repeats * tokens.size();
        System.out.println("M3_TOKEN_CLASS_LOOKUP_PROBE warmup=" + warmup + " repeats=" + repeats
                + " utf16Units=" + source.length() + " tokens=" + tokens.size()
                + " lookups=" + lookups + " totalAllocatedBytes=" + allocated
                + " bytesPerTraversal=" + allocated / repeats
                + " bytesPerLookup=" + (double) allocated / lookups
                + " totalNanos=" + elapsed + " totalCpuNanos=" + cpuElapsed
                + " checksum=" + checksum + " sample=" + sink);
    }

    private static long traverse(M3TokenStream tokens) {
        long result = 0;
        for (int index = 0; index < tokens.size(); index++) {
            result += (tokens.tokenClass(index).ordinal() + 1L) * (index + 1L);
        }
        return result;
    }
}
