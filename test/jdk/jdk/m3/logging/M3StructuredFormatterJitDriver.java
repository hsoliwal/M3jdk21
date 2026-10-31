/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */

import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import jdk.m3.logging.M3StructuredFormatter;

/**
 * The hot callers of the JIT pass: each M3StructuredFormatter atom in a loop long enough for C2 to compile it.
 * M3StructuredFormatterJitTest runs this class in a forked C2-only JVM with -XX:+PrintInlining.
 */
public final class M3StructuredFormatterJitDriver {

    private static final int ROUNDS = 40;
    private static final int CALLS = 20_000;
    private static long sink;

    private M3StructuredFormatterJitDriver() {
    }

    public static void main(String[] args) {
        String plain = "loaded 1234 rows from orders.tsv";
        String special = "loaded\t1234 rows\nfrom orders.tsv\\";
        String escaped = M3StructuredFormatter.escape(special);
        LogRecord record = new LogRecord(Level.INFO, plain);
        record.setInstant(Instant.parse("2026-10-31T12:34:56.789Z"));
        record.setLoggerName("jdk.m3.demo");
        for (int round = 0; round < ROUNDS; round++) {
            sink += drive(plain, special, escaped, record);
        }
        System.out.println("JIT_DRIVER_DONE " + (sink != Long.MIN_VALUE));
    }

    private static long drive(String plain, String special, String escaped, LogRecord record) {
        long acc = 0;
        for (int i = 0; i < CALLS; i++) {
            acc += M3StructuredFormatter.escape((i & 1) == 0 ? plain : special).length();
            acc += M3StructuredFormatter.unescape(escaped).length();
            acc += M3StructuredFormatter.row(record, plain, "").length();
        }
        return acc;
    }
}
