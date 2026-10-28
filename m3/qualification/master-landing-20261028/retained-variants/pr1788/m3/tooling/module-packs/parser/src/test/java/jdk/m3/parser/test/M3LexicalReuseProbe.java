/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.parser.test;

import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;
import jdk.m3.parser.grammar.M3GrammarLexer;
import jdk.m3.parser.grammar.M3LexerProfile;
import jdk.m3.parser.grammar.M3TokenStream;

/** Cold and warm lexical allocation/CPU observations; timings are never conformance assertions. */
public final class M3LexicalReuseProbe {
    private static volatile M3TokenStream sink;

    private M3LexicalReuseProbe() { }

    public static void main(String[] arguments) {
        int warmup = arguments.length > 0 ? Integer.parseInt(arguments[0]) : 256;
        int repeats = arguments.length > 1 ? Integer.parseInt(arguments[1]) : 1024;
        if (arguments.length > 2 || warmup < 1 || warmup > 10000 || repeats < 1 || repeats > 10000) {
            throw new IllegalArgumentException("expected warmup/repeats in [1, 10000]");
        }
        ThreadMXBean measurements = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!measurements.isThreadAllocatedMemorySupported()) {
            throw new IllegalStateException("thread allocation measurement unsupported");
        }
        measurements.setThreadAllocatedMemoryEnabled(true);
        boolean cpu = measurements.isCurrentThreadCpuTimeSupported();
        if (cpu) measurements.setThreadCpuTimeEnabled(true);
        M3GrammarLexer lexer = new M3GrammarLexer(M3LexerProfile.builder("lexical-reuse-probe-v1")
                .operators("===", "==", "+", "-").build());
        String first = new String("identifier + 123 ".repeat(2048));
        String second = new String(first);
        if (first == second) throw new AssertionError("cold fixture must have distinct source owners");
        observe("cold-distinct-generation", lexer, first, second, warmup, repeats, measurements, cpu);
        observe("warm-same-generation", lexer, first, first, warmup, repeats, measurements, cpu);
        M3TokenStream retained = sink;
        System.out.println("M3_LEXICAL_REUSE_PROBE_SHAPE utf16Units=" + first.length()
                + " tokens=" + retained.size() + " primitiveBytes=" + retained.primitivePayloadBytes()
                + " sample=" + sample(retained));
    }

    private static void observe(String mode, M3GrammarLexer lexer, String first, String second,
            int warmup, int repeats, ThreadMXBean measurements, boolean cpu) {
        for (int index = 0; index < warmup; index++) sink = lexer.lex((index & 1) == 0 ? first : second);
        // Start every measured cold run from the opposite owner, independently of warmup parity.
        sink = lexer.lex(second);
        long thread = Thread.currentThread().threadId();
        long allocatedBefore = measurements.getThreadAllocatedBytes(thread);
        long cpuBefore = cpu ? measurements.getCurrentThreadCpuTime() : 0;
        long started = System.nanoTime();
        long checksum = 0;
        for (int index = 0; index < repeats; index++) {
            M3TokenStream result = lexer.lex((index & 1) == 0 ? first : second);
            checksum += sample(result);
            sink = result;
        }
        long elapsed = System.nanoTime() - started;
        long cpuElapsed = cpu ? measurements.getCurrentThreadCpuTime() - cpuBefore : -1;
        long allocated = measurements.getThreadAllocatedBytes(thread) - allocatedBefore;
        System.out.println("M3_LEXICAL_REUSE_PROBE mode=" + mode + " warmup=" + warmup
                + " repeats=" + repeats + " totalAllocatedBytes=" + allocated
                + " bytesPerOperation=" + (allocated / repeats) + " totalNanos=" + elapsed
                + " nanosPerOperation=" + (elapsed / repeats) + " totalCpuNanos=" + cpuElapsed
                + " cpuNanosPerOperation=" + (cpu ? cpuElapsed / repeats : -1)
                + " checksum=" + checksum);
    }

    private static long sample(M3TokenStream tokens) {
        return tokens.size() * 31L + tokens.start(0) * 17L + tokens.end(tokens.size() - 1)
                + tokens.hashCodeAt(0) + tokens.hashCodeAt(tokens.size() - 1);
    }
}
