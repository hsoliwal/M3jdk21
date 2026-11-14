/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */

/*
 * @test
 * @summary jdk.m3.logging.M3StructuredFormatter: one performance row per atom (escape, unescape, row) against its
 *          java.util reference; asserts only that escaping plain text allocates nothing.
 * @modules jdk.management jdk.m3.logging
 * @run junit M3StructuredFormatterPerformanceTest
 */

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.Locale;
import java.util.function.IntUnaryOperator;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import jdk.m3.logging.M3StructuredFormatter;
import org.junit.jupiter.api.Test;

class M3StructuredFormatterPerformanceTest {

    private static final int WARMUP = 3;
    private static final int ROUNDS = 7;
    private static final int OPS = 20_000;
    private static final com.sun.management.ThreadMXBean THREADS =
            (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
    private static long sink;

    private static double nsPerOp(IntUnaryOperator op) {
        long best = Long.MAX_VALUE;
        for (int round = 0; round < WARMUP + ROUNDS; round++) {
            long t0 = System.nanoTime();
            for (int i = 0; i < OPS; i++) {
                sink += op.applyAsInt(i);
            }
            long nanos = System.nanoTime() - t0;
            if (round >= WARMUP) {
                best = Math.min(best, nanos);
            }
        }
        return (double) best / OPS;
    }

    private static void row(String atom, IntUnaryOperator m3, IntUnaryOperator reference) {
        double fast = nsPerOp(m3);
        double slow = nsPerOp(reference);
        System.out.println(String.format(Locale.ROOT, "LOGPERF %s\t%.1f\t%.1f\t%.2f", atom, fast, slow,
                slow / Math.max(fast, 1e-9)));
    }

    private static String chain(String s) {
        return s.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n").replace("\r", "\\r");
    }

    @Test
    void escapeUnescapeAndRowAgainstTheirReferences() {
        String plain = "loaded 1234 rows from orders.tsv in 15 ms";
        String special = "loaded 1234 rows\nfrom orders\t2026.tsv in 15 ms\\";
        row("escape(plain)", i -> M3StructuredFormatter.escape(plain).length(), i -> chain(plain).length());
        row("escape(special)", i -> M3StructuredFormatter.escape(special).length(), i -> chain(special).length());
        String escaped = M3StructuredFormatter.escape(special);
        row("unescape", i -> M3StructuredFormatter.unescape(escaped).length(),
                i -> escaped.replace("\\n", "\n").replace("\\t", "\t").replace("\\r", "\r").replace("\\\\", "\\")
                        .length());
        LogRecord record = new LogRecord(Level.INFO, plain);
        record.setInstant(Instant.parse("2026-10-31T12:34:56.789Z"));
        record.setLoggerName("jdk.m3.demo");
        record.setSourceClassName("Demo");
        record.setSourceMethodName("run");
        SimpleFormatter simple = new SimpleFormatter();
        row("row (vs SimpleFormatter.format)", i -> M3StructuredFormatter.row(record, plain, "").length(),
                i -> simple.format(record).length());
        long bytes0 = THREADS.getCurrentThreadAllocatedBytes();
        for (int i = 0; i < OPS; i++) {
            sink += M3StructuredFormatter.escape(plain).length();
        }
        long bytes = THREADS.getCurrentThreadAllocatedBytes() - bytes0;
        assertTrue(bytes < OPS, "escaping plain text allocates nothing: " + bytes + " bytes for " + OPS + " calls");
        assertTrue(sink != Long.MIN_VALUE);
    }
}
