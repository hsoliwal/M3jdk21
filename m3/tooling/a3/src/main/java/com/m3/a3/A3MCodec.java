// SPDX-License-Identifier: Apache-2.0
package com.m3.a3;

import java.util.Objects;

/** Strict deterministic codec/root helper for the A3 mastery receipt. */
final class A3MCodec {
    private static final String HEADER =
            "schema\troot\tpinRoot\tlabTsvSha256\tfixtureCount\tscheduleCount\tresultCount"
                    + "\tapplicationCount\tcompileCount\tchangedCount\tregexStableCount";

    private A3MCodec() {}

    static String render(A3M.Receipt receipt) {
        A3M.Receipt checked = Objects.requireNonNull(receipt, "receipt");
        return HEADER
                + "\n"
                + checked.schema()
                + "\t"
                + checked.root()
                + "\t"
                + checked.pinRoot()
                + "\t"
                + checked.labTsvSha256()
                + "\t"
                + checked.fixtureCount()
                + "\t"
                + checked.scheduleCount()
                + "\t"
                + checked.resultCount()
                + "\t"
                + checked.applicationCount()
                + "\t"
                + checked.compileCount()
                + "\t"
                + checked.changedCount()
                + "\t"
                + checked.regexStableCount()
                + "\n";
    }

    static A3M.Receipt parse(String tsv) {
        String[] lines = Objects.requireNonNull(tsv, "tsv").split("\\R", -1);
        if (lines.length != 3 || !HEADER.equals(lines[0]) || !lines[2].isEmpty()) {
            throw new IllegalArgumentException("invalid A3M receipt shape");
        }
        String[] cells = lines[1].split("\\t", -1);
        if (cells.length != 11) {
            throw new IllegalArgumentException("invalid A3M receipt columns");
        }
        return new A3M.Receipt(
                cells[0],
                cells[1],
                cells[2],
                cells[3],
                number(cells[4], "fixtureCount"),
                number(cells[5], "scheduleCount"),
                number(cells[6], "resultCount"),
                longNumber(cells[7], "applicationCount"),
                longNumber(cells[8], "compileCount"),
                number(cells[9], "changedCount"),
                number(cells[10], "regexStableCount"));
    }

    static void requireCounts(
            int fixtures,
            int schedules,
            int results,
            long applications,
            long compiles,
            int changed,
            int regexStable) {
        if (fixtures < 1
                || schedules < 1
                || results != Math.multiplyExact(fixtures, schedules)
                || applications < results
                || compiles < results
                || changed < 1
                || changed > results
                || regexStable != results) {
            throw new IllegalArgumentException("invalid A3M counts");
        }
    }

    static String receiptRoot(
            String pinRoot,
            String labSha,
            int fixtures,
            int schedules,
            int results,
            long applications,
            long compiles,
            int changed,
            int regexStable) {
        return A3Fs.sha(
                A3M.SCHEMA
                        + "\n"
                        + pinRoot
                        + "\n"
                        + labSha
                        + "\n"
                        + fixtures
                        + "\n"
                        + schedules
                        + "\n"
                        + results
                        + "\n"
                        + applications
                        + "\n"
                        + compiles
                        + "\n"
                        + changed
                        + "\n"
                        + regexStable
                        + "\n");
    }

    static String path(String value) {
        String checked = text(value, "path").replace('\\', '/');
        if (checked.startsWith("/")
                || checked.startsWith("../")
                || checked.endsWith("/..")
                || checked.contains("/../")
                || checked.contains("//")) {
            throw new IllegalArgumentException("path");
        }
        for (String part : checked.split("/", -1)) {
            if (part.isEmpty() || ".".equals(part) || "..".equals(part)) {
                throw new IllegalArgumentException("path");
            }
        }
        return checked;
    }

    static String text(String value, String field) {
        String checked = Objects.toString(value, "").strip();
        if (checked.isEmpty()
                || checked.indexOf('\0') >= 0
                || checked.indexOf('\n') >= 0
                || checked.indexOf('\r') >= 0
                || checked.indexOf('\t') >= 0) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    static String sha(String value, String field) {
        String checked = text(value, field);
        if (!checked.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static int number(String value, String field) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException(field, failure);
        }
    }

    private static long longNumber(String value, String field) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException(field, failure);
        }
    }
}
