// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.lexicon;

/**
 * RFC 4180 separator monoid with the quote state carried across atom seams (Commons CSV
 * {@code CSVFormat.RFC4180} lexer semantics: delimiter ',', quote '"', CR, LF or CRLF record ends,
 * whitespace allowed after a closing quote). For every entry state an atom stores its exit state and the
 * separator/record-start bits it produces; {@code combine} is function composition
 * ({@code exit = R.exit[L.exit[e]]}, {@code bits = L.bits[e] | R.bits[L.exit[e]] << L.len}), hence
 * associative with the empty atom as identity. A delimiter inside a quoted field is never a separator.
 */
public final class M3CsvBoundarySummary {
    /** At the start of a field. */
    public static final int FIELD_START = 0;
    /** Inside an unquoted field. */
    public static final int UNQUOTED = 1;
    /** Inside a quoted field. */
    public static final int QUOTED = 2;
    /** Just read a quote inside a quoted field: closing quote or first half of an escaped quote. */
    public static final int QUOTE_SEEN = 3;
    /** After a closing quote, skipping whitespace before the next separator. */
    public static final int CLOSED = 4;
    /** Just read a CR outside quotes; a following LF continues the same record end. */
    public static final int AFTER_CR = 5;
    /** Commons CSV refusal (invalid character after a closing quote); absorbing. */
    public static final int ERROR = 6;
    static final int STATES = 7;
    private static final int SEPARATOR = 1 << 8;
    private static final int RECORD = 1 << 9;

    private final int length;
    private final int[] exit;
    private final long[][] separators;
    private final long[][] records;

    private M3CsvBoundarySummary(int length, int[] exit, long[][] separators, long[][] records) {
        this.length = length;
        this.exit = exit;
        this.separators = separators;
        this.records = records;
    }

    /** Summarizes one atom for every entry state. */
    public static M3CsvBoundarySummary of(CharSequence atom) {
        int n = atom.length();
        int[] exit = new int[STATES];
        long[][] separators = new long[STATES][];
        long[][] records = new long[STATES][];
        for (int entry = 0; entry < STATES; entry++) {
            separators[entry] = M3BitLanes.lanes(n);
            records[entry] = M3BitLanes.lanes(n);
            int state = entry;
            for (int index = 0; index < n; index++) {
                int step = step(state, atom.charAt(index));
                markStep(step, separators[entry], records[entry], index);
                state = step & 0xff;
            }
            exit[entry] = state;
        }
        return new M3CsvBoundarySummary(n, exit, separators, records);
    }

    /** Monoid product: {@code L ++ R} (function composition over entry states). */
    public M3CsvBoundarySummary combine(M3CsvBoundarySummary right) {
        int n = length + right.length;
        int[] composedExit = new int[STATES];
        long[][] composedSeparators = new long[STATES][];
        long[][] composedRecords = new long[STATES][];
        for (int entry = 0; entry < STATES; entry++) {
            int middle = exit[entry];
            composedExit[entry] = right.exit[middle];
            composedSeparators[entry] = M3BitLanes.copyOf(separators[entry], n);
            M3BitLanes.orShifted(composedSeparators[entry], right.separators[middle], right.length, length);
            composedRecords[entry] = M3BitLanes.copyOf(records[entry], n);
            M3BitLanes.orShifted(composedRecords[entry], right.records[middle], right.length, length);
        }
        return new M3CsvBoundarySummary(n, composedExit, composedSeparators, composedRecords);
    }

    /** @return separator-unit bits of the whole text read from {@link #FIELD_START} */
    public long[] separators() {
        return separators[FIELD_START].clone();
    }

    /** @return record-start bits (first unit of each record end) read from {@link #FIELD_START} */
    public long[] recordEnds() {
        return records[FIELD_START].clone();
    }

    /** @return true when Commons CSV would refuse the text (bad char after a quote, or EOF inside quotes) */
    public boolean refused() {
        int end = exit[FIELD_START];
        return end == ERROR || end == QUOTED;
    }

    /** @return number of UTF-16 units summarized */
    public int length() {
        return length;
    }

    /** Transition: low byte = next state, plus SEPARATOR / RECORD flags for the consumed unit. */
    static int step(int state, char unit) {
        switch (state) {
            case QUOTED:
                return unit == '"' ? QUOTE_SEEN : QUOTED;
            case QUOTE_SEEN:
                return unit == '"' ? QUOTED : afterValue(unit);
            case CLOSED:
                return afterValue(unit);
            case ERROR:
                return ERROR;
            case AFTER_CR:
                return unit == '\n' ? FIELD_START | SEPARATOR : fieldUnit(FIELD_START, unit);
            default:
                return fieldUnit(state, unit);
        }
    }

    private static int fieldUnit(int state, char unit) {
        int end = endOfValue(unit);
        if (end >= 0) {
            return end;
        }
        return state == FIELD_START && unit == '"' ? QUOTED : UNQUOTED;
    }

    private static int afterValue(char unit) {
        int end = endOfValue(unit);
        if (end >= 0) {
            return end;
        }
        return Character.isWhitespace(unit) ? CLOSED : ERROR;
    }

    private static int endOfValue(char unit) {
        if (unit == ',') {
            return FIELD_START | SEPARATOR;
        }
        if (unit == '\r') {
            return AFTER_CR | SEPARATOR | RECORD;
        }
        return unit == '\n' ? FIELD_START | SEPARATOR | RECORD : -1;
    }

    private static void markStep(int step, long[] separators, long[] records, int index) {
        if ((step & SEPARATOR) != 0) {
            M3BitLanes.set(separators, index);
        }
        if ((step & RECORD) != 0) {
            M3BitLanes.set(records, index);
        }
    }
}
