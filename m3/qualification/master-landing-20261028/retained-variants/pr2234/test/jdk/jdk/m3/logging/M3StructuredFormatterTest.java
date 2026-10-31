/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */

/*
 * @test
 * @summary jdk.m3.logging.M3StructuredFormatter: one JUnit test per atom (escape, unescape, row) and the formatter
 *          end to end: one tab-separated line per record, fixed columns, reversible escapes.
 * @modules jdk.m3.logging
 * @run junit M3StructuredFormatterTest
 */

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;
import jdk.m3.logging.M3StructuredFormatter;
import org.junit.jupiter.api.Test;

class M3StructuredFormatterTest {

    private static final int COLUMNS = 10;
    private static final String SPECIALS = "\t\n\r\\";

    private static LogRecord record(Level level, String message) {
        LogRecord record = new LogRecord(level, message);
        record.setInstant(Instant.parse("2026-10-31T12:34:56.789Z"));
        record.setLoggerName("jdk.m3.demo");
        record.setSourceClassName("Demo");
        record.setSourceMethodName("run");
        record.setLongThreadID(42L);
        record.setSequenceNumber(7L);
        return record;
    }

    @Test
    void escapeLeavesPlainTextAsItIsAndMapsEachSeparator() {
        String plain = "plain text, no separators: = ; | é 漢";
        assertSame(plain, M3StructuredFormatter.escape(plain), "no allocation for plain text");
        assertEquals("", M3StructuredFormatter.escape(null));
        assertEquals("", M3StructuredFormatter.escape(""));
        assertEquals("a\\tb\\nc\\rd\\\\e", M3StructuredFormatter.escape("a\tb\nc\rd\\e"));
        assertEquals("\\\\t", M3StructuredFormatter.escape("\\t"), "a literal backslash-t stays two characters");
        String escaped = M3StructuredFormatter.escape("x\ty\nz");
        assertTrue(escaped.indexOf('\t') < 0 && escaped.indexOf('\n') < 0 && escaped.indexOf('\r') < 0);
    }

    @Test
    void unescapeInvertsEscapeAndRefusesMalformedFields() {
        Random random = new Random(1);
        for (int round = 0; round < 2_000; round++) {
            StringBuilder text = new StringBuilder();
            for (int i = random.nextInt(40); i > 0; i--) {
                int kind = random.nextInt(4);
                if (kind == 0) {
                    text.append(SPECIALS.charAt(random.nextInt(SPECIALS.length())));
                } else if (kind == 1) {
                    text.appendCodePoint(0x1F600 + random.nextInt(64)); // surrogate pairs pass through
                } else {
                    text.append((char) ('a' + random.nextInt(26)));
                }
            }
            String s = text.toString();
            assertEquals(s, M3StructuredFormatter.unescape(M3StructuredFormatter.escape(s)), s);
        }
        String noBackslash = "nothing to restore";
        assertSame(noBackslash, M3StructuredFormatter.unescape(noBackslash));
        assertThrows(IllegalArgumentException.class, () -> M3StructuredFormatter.unescape("ends with \\"));
        assertThrows(IllegalArgumentException.class, () -> M3StructuredFormatter.unescape("bad \\x escape"));
        assertThrows(NullPointerException.class, () -> M3StructuredFormatter.unescape(null));
    }

    @Test
    void rowHasTheTenColumnsInOrderOnOneLine() {
        LogRecord record = record(Level.WARNING, "ignored: the row takes the message argument");
        String row = M3StructuredFormatter.row(record, "two\nlines\tand a tab", "java.lang.IllegalStateException: x");
        assertTrue(row.endsWith("\n") && row.indexOf('\n') == row.length() - 1, "exactly one line");
        String[] cells = row.substring(0, row.length() - 1).split("\t", -1);
        assertEquals(COLUMNS, cells.length);
        assertEquals(COLUMNS, M3StructuredFormatter.HEADER.split("\t", -1).length);
        assertEquals("2026-10-31T12:34:56.789Z", cells[0]);
        assertEquals("WARNING", cells[1]);
        assertEquals(Integer.toString(Level.WARNING.intValue()), cells[2]);
        assertEquals("jdk.m3.demo", cells[3]);
        assertEquals("42", cells[4]);
        assertEquals("Demo", cells[5]);
        assertEquals("run", cells[6]);
        assertEquals("7", cells[7]);
        assertEquals("two\nlines\tand a tab", M3StructuredFormatter.unescape(cells[8]));
        assertEquals("java.lang.IllegalStateException: x", cells[9]);
        LogRecord bare = new LogRecord(Level.INFO, null);
        String[] empty = M3StructuredFormatter.row(bare, null, null).split("\t", -1);
        assertEquals("", empty[3], "an absent logger is an empty field");
        assertEquals("", empty[8], "an absent message is an empty field");
    }

    @Test
    void formatAppliesParametersAndWritesTheThrowable() {
        M3StructuredFormatter formatter = new M3StructuredFormatter();
        LogRecord record = record(Level.SEVERE, "loaded {0} rows from {1}");
        record.setParameters(new Object[] {3, "a\tb.tsv"});
        record.setThrown(new IllegalArgumentException("bad\nrow"));
        String[] cells = formatter.format(record).split("\t", -1);
        assertEquals("loaded 3 rows from a\tb.tsv", M3StructuredFormatter.unescape(cells[8]));
        assertEquals("java.lang.IllegalArgumentException: bad\nrow", M3StructuredFormatter.unescape(cells[9].strip()));
        assertEquals(M3StructuredFormatter.HEADER + "\n", formatter.getHead(null));
    }

    @Test
    void aHandlerWritesTheHeaderThenOneLinePerRecord() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        StreamHandler handler = new StreamHandler(bytes, new M3StructuredFormatter());
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);
        logger.info("first");
        logger.log(Level.WARNING, "second\nwith a break", new RuntimeException("boom"));
        logger.fine("below the level: not written");
        handler.flush();
        logger.removeHandler(handler);
        String[] lines = bytes.toString(StandardCharsets.UTF_8).split("\n", -1);
        assertEquals(M3StructuredFormatter.HEADER, lines[0]);
        assertEquals(4, lines.length, "header, two records and the empty tail after the last line feed");
        assertEquals("first", lines[1].split("\t", -1)[8]);
        assertEquals("second\nwith a break", M3StructuredFormatter.unescape(lines[2].split("\t", -1)[8]));
        assertEquals("java.lang.RuntimeException: boom", lines[2].split("\t", -1)[9]);
    }
}
