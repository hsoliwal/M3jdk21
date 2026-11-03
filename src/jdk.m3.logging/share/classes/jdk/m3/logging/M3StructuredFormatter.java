/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */
package jdk.m3.logging;

import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * A {@link Formatter} that writes each {@link LogRecord} as one tab-separated line with a fixed column order, so a
 * log is a table that {@code cut}, {@code awk} and {@code grep} read without parsing prose. The columns are
 * {@value #HEADER}, one record per line, each line ended by a line feed:
 * <ul>
 * <li>{@code instant}: {@link java.time.Instant#toString} of the record's instant (ISO-8601, UTC);</li>
 * <li>{@code level}, {@code level_value}: the level's name (not localized) and its integer value;</li>
 * <li>{@code logger}, {@code thread}, {@code source_class}, {@code source_method}, {@code sequence}: the record's
 * fields, an absent one as an empty field;</li>
 * <li>{@code message}: {@link #formatMessage(LogRecord)}, localized and with its parameters applied;</li>
 * <li>{@code thrown}: {@link Throwable#toString} of the record's throwable, empty when there is none.</li>
 * </ul>
 * A tab, line feed, carriage return or backslash inside a field is written as {@code \t}, {@code \n}, {@code \r} or
 * {@code \\} ({@link #escape}), so a record never spans two lines and {@link #unescape} restores every field.
 * {@link #getHead} writes the column names as the first line.
 *
 * <p>Use: {@code java.util.logging.FileHandler.formatter = jdk.m3.logging.M3StructuredFormatter} in
 * logging.properties, or {@code handler.setFormatter(new M3StructuredFormatter())}.
 *
 * <p>The atoms {@link #escape}, {@link #unescape} and {@link #row} state their contracts below; each has its own
 * JUnit test (test/jdk/jdk/m3/logging/M3StructuredFormatterTest.java) and performance row
 * (M3StructuredFormatterPerformanceTest.java).
 *
 * @since 21
 */
public class M3StructuredFormatter extends Formatter {

    /** The column names, in order, tab-separated: the first line of a log ({@link #getHead}). */
    public static final String HEADER = "instant\tlevel\tlevel_value\tlogger\tthread\tsource_class\tsource_method"
            + "\tsequence\tmessage\tthrown";
    private static final char TAB = '\t';
    private static final char LINE_FEED = '\n';
    private static final char CARRIAGE_RETURN = '\r';
    private static final char BACKSLASH = '\\';
    private static final char ESCAPED_TAB = 't';
    private static final char ESCAPED_LINE_FEED = 'n';
    private static final char ESCAPED_CARRIAGE_RETURN = 'r';
    private static final String EMPTY = "";
    /** Room for a few escapes before the builder grows. */
    private static final int ESCAPE_SLACK = 16;
    /** A typical line: ten fields and a short message. */
    private static final int ROW_CAPACITY = 160;

    /** Creates a structured formatter. */
    public M3StructuredFormatter() {
    }

    /** One line: the record's columns ({@link #row}), the throwable written by {@link Throwable#toString}. */
    @Override
    public String format(LogRecord record) {
        Throwable thrown = record.getThrown();
        return row(record, formatMessage(record), thrown == null ? EMPTY : thrown.toString());
    }

    /** The column names, ended by a line feed. */
    @Override
    public String getHead(Handler handler) {
        return HEADER + LINE_FEED;
    }

    /**
     * Escapes the field separators of TEXT.
     * Requires: nothing; TEXT may be null.
     * Ensures: the empty string for null; TEXT itself when it holds no tab, line feed, carriage return or backslash;
     * otherwise TEXT with each of them written as a backslash and t, n, r or a second backslash.
     * Effects: allocates only when TEXT holds a character to escape.
     * Cost: one pass over TEXT.
     *
     * @param text the field text, or null
     * @return the escaped field
     */
    public static String escape(String text) {
        if (text == null) {
            return EMPTY;
        }
        int first = 0;
        while (first < text.length() && !special(text.charAt(first))) {
            first++;
        }
        if (first == text.length()) {
            return text;
        }
        StringBuilder out = new StringBuilder(text.length() + ESCAPE_SLACK).append(text, 0, first);
        for (int i = first; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case TAB -> out.append(BACKSLASH).append(ESCAPED_TAB);
                case LINE_FEED -> out.append(BACKSLASH).append(ESCAPED_LINE_FEED);
                case CARRIAGE_RETURN -> out.append(BACKSLASH).append(ESCAPED_CARRIAGE_RETURN);
                case BACKSLASH -> out.append(BACKSLASH).append(BACKSLASH);
                default -> out.append(c);
            }
        }
        return out.toString();
    }

    private static boolean special(char c) {
        return c == TAB || c == LINE_FEED || c == CARRIAGE_RETURN || c == BACKSLASH;
    }

    /**
     * Restores a field written by {@link #escape}.
     * Requires: FIELD is not null and every backslash in it starts one of the four escapes.
     * Ensures: the text whose escape is FIELD, so unescape(escape(s)) equals s for every string s; FIELD itself when
     * it holds no backslash.
     * Effects: allocates only when FIELD holds an escape; throws IllegalArgumentException for a malformed escape.
     * Cost: one pass over FIELD.
     *
     * @param field an escaped field
     * @return the field's text
     * @throws IllegalArgumentException if a backslash does not start one of the four escapes
     */
    public static String unescape(String field) {
        int first = field.indexOf(BACKSLASH);
        if (first < 0) {
            return field;
        }
        StringBuilder out = new StringBuilder(field.length()).append(field, 0, first);
        for (int i = first; i < field.length(); i++) {
            char c = field.charAt(i);
            if (c != BACKSLASH) {
                out.append(c);
                continue;
            }
            if (++i == field.length()) {
                throw new IllegalArgumentException("dangling backslash at the end of " + field);
            }
            switch (field.charAt(i)) {
                case ESCAPED_TAB -> out.append(TAB);
                case ESCAPED_LINE_FEED -> out.append(LINE_FEED);
                case ESCAPED_CARRIAGE_RETURN -> out.append(CARRIAGE_RETURN);
                case BACKSLASH -> out.append(BACKSLASH);
                default -> throw new IllegalArgumentException("unknown escape at " + (i - 1) + " of " + field);
            }
        }
        return out.toString();
    }

    /**
     * The line of RECORD.
     * Requires: RECORD is not null; MESSAGE and THROWN may be null.
     * Ensures: the ten columns of {@link #HEADER}, in that order, each escaped, separated by tabs and ended by one
     * line feed; MESSAGE and THROWN fill the last two columns.
     * Effects: reads RECORD only.
     * Cost: one pass over the fields.
     *
     * @param record the log record
     * @param message the formatted message, or null
     * @param thrown the text of the record's throwable, or null
     * @return the record's line, ended by a line feed
     */
    public static String row(LogRecord record, String message, String thrown) {
        StringBuilder out = new StringBuilder(ROW_CAPACITY);
        out.append(record.getInstant()).append(TAB)
                .append(escape(record.getLevel().getName())).append(TAB)
                .append(record.getLevel().intValue()).append(TAB)
                .append(escape(record.getLoggerName())).append(TAB)
                .append(record.getLongThreadID()).append(TAB)
                .append(escape(record.getSourceClassName())).append(TAB)
                .append(escape(record.getSourceMethodName())).append(TAB)
                .append(record.getSequenceNumber()).append(TAB)
                .append(escape(message)).append(TAB)
                .append(escape(thrown));
        return out.append(LINE_FEED).toString();
    }
}
