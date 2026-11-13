/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import jdk.m3.logging.M3StructuredFormatter;

/**
 * Differential fixture for the jdk.m3.logging restore (recipe m3/qualification/restore-jdk-m3-logging-20261107):
 * writes the formatter's output over a fixed record set and the escape/unescape atoms over a fixed string set to the
 * file named by args[0] (UTF-8). Run once against the module compiled from the receiver head 7c23ff3612ef and once
 * against the module compiled from this branch; the two files must be byte-identical. Every input is fixed (instants,
 * sequence numbers, thread ids, levels, messages, throwables), so the output has no run-to-run variance.
 */
public final class M3StructuredFormatterDifferential {

    private static final long EPOCH_SECOND = 1_700_000_000L;
    private static final long SECOND_STEP = 7_919L;
    private static final int NANO_STEP = 123_457;
    private static final int RECORDS = 64;
    private static final Level[] LEVELS = {Level.SEVERE, Level.WARNING, Level.INFO, Level.CONFIG, Level.FINE,
            Level.FINER, Level.FINEST, Level.ALL, Level.OFF};
    private static final String[] FIELDS = {"", "plain", "tab\tsep", "lf\nsep", "cr\rsep", "back\\slash",
            "mix\t\n\r\\end", "\\t literal backslash t", "trailing\\", "unicode " + (char) 0x00e7 + (char) 0x2603
            + (char) 0x4e2d, "   spaces   ", "a\t\t\tb", "\\\\double"};
    private static final String[] MALFORMED = {"bad\\x", "dangling\\", "\\", "ok\\tthen\\q"};

    private M3StructuredFormatterDifferential() {
    }

    public static void main(String[] args) throws IOException {
        List<String> lines = new ArrayList<>();
        M3StructuredFormatter formatter = new M3StructuredFormatter();
        lines.add("HEAD " + M3StructuredFormatter.HEADER);
        lines.add("GETHEAD " + M3StructuredFormatter.escape(formatter.getHead(null)));
        for (int i = 0; i < RECORDS; i++) {
            lines.add("FORMAT " + i + " " + M3StructuredFormatter.escape(formatter.format(record(i))));
        }
        lines.add("ESCAPE null " + M3StructuredFormatter.escape(null));
        for (int i = 0; i < FIELDS.length; i++) {
            String escaped = M3StructuredFormatter.escape(FIELDS[i]);
            lines.add("ESCAPE " + i + " " + escaped);
            lines.add("ROUNDTRIP " + i + " " + FIELDS[i].equals(M3StructuredFormatter.unescape(escaped)));
            lines.add("IDENTITY " + i + " " + (escaped == FIELDS[i]));
        }
        for (int i = 0; i < MALFORMED.length; i++) {
            lines.add("MALFORMED " + i + " " + malformed(MALFORMED[i]));
        }
        lines.add("ROW null-message-thrown " + M3StructuredFormatter.escape(
                M3StructuredFormatter.row(record(0), null, null)));
        Files.write(Path.of(args[0]), (String.join("\n", lines) + "\n").getBytes(StandardCharsets.UTF_8));
        System.out.println("DIFFERENTIAL_LINES " + lines.size());
    }

    private static String malformed(String field) {
        try {
            return "ACCEPTED " + M3StructuredFormatter.escape(M3StructuredFormatter.unescape(field));
        } catch (IllegalArgumentException e) {
            return "REFUSED " + e.getClass().getName() + ": " + M3StructuredFormatter.escape(e.getMessage());
        }
    }

    private static LogRecord record(int i) {
        LogRecord record = new LogRecord(LEVELS[i % LEVELS.length], message(i));
        record.setInstant(Instant.ofEpochSecond(EPOCH_SECOND + i * SECOND_STEP, (long) i * NANO_STEP));
        record.setSequenceNumber(i);
        record.setLongThreadID(1_000L + i);
        record.setLoggerName(i % 3 == 0 ? null : "jdk.m3.logging.fixture." + FIELDS[i % FIELDS.length]);
        record.setSourceClassName(i % 5 == 0 ? null : "jdk.m3.logging.Fixture" + i);
        record.setSourceMethodName(i % 7 == 0 ? null : "method" + FIELDS[i % FIELDS.length]);
        if (i % 2 == 0) {
            record.setParameters(new Object[] {i, FIELDS[i % FIELDS.length]});
        }
        if (i % 4 == 0) {
            record.setThrown(new IllegalStateException("boom\twith tab " + i));
        }
        return record;
    }

    private static String message(int i) {
        if (i % 2 == 0) {
            return "param {0} and {1} " + FIELDS[(i / 2) % FIELDS.length];
        }
        return i % 3 == 0 ? null : "message " + FIELDS[i % FIELDS.length];
    }
}
