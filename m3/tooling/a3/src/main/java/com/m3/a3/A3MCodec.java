// SPDX-License-Identifier: Apache-2.0
package com.m3.a3;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Strict deterministic codec/root helper for the A3 mastery receipt. */
final class A3MCodec {
    private static final String HEADER =
            "schema\troot\tpinRoot\tlabTsvSha256\tfixtureCount\tscheduleCount\tresultCount"
                    + "\tapplicationCount\tcompileCount\tchangedCount\tregexStableCount";

    private static final String LAB_HEADER =
            "fixture\tschedule\tsubset\tbeforeSha\tafterSha\tapplications\tcompiles"
                    + "\tchanged\tfixedPoint\tbehaviorStable\tcontractStable"
                    + "\tlexicalDataStable\tregexMatrixStable";

    record LabSummary(
            int fixtureCount,
            int scheduleCount,
            int resultCount,
            long applicationCount,
            long compileCount,
            int changedCount,
            int regexStableCount,
            Set<String> scheduleNames) {
        LabSummary {
            scheduleNames = Set.copyOf(Objects.requireNonNull(scheduleNames, "scheduleNames"));
            requireCounts(
                    fixtureCount,
                    scheduleCount,
                    resultCount,
                    applicationCount,
                    compileCount,
                    changedCount,
                    regexStableCount);
        }
    }

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

    static LabSummary summarizeLab(String tsv) {
        String[] lines = Objects.requireNonNull(tsv, "tsv").split("\\R", -1);
        if (lines.length < 3 || !LAB_HEADER.equals(lines[0]) || !lines[lines.length - 1].isEmpty()) {
            throw new IllegalArgumentException("invalid A3M lab TSV shape");
        }

        Set<Integer> fixtures = new HashSet<>();
        Set<String> schedules = new HashSet<>();
        Set<String> coordinates = new HashSet<>();
        long applications = 0L;
        long compiles = 0L;
        int changed = 0;
        int regexStable = 0;
        int results = 0;

        for (int index = 1; index < lines.length - 1; index++) {
            String[] cells = lines[index].split("\\t", -1);
            if (cells.length != 13) {
                throw new IllegalArgumentException("invalid A3M lab row");
            }
            int fixture = number(cells[0], "fixture");
            if (fixture < 0) {
                throw new IllegalArgumentException("fixture");
            }
            String schedule = text(cells[1], "schedule");
            text(cells[2], "subset");
            sha(cells[3], "beforeSha");
            sha(cells[4], "afterSha");
            int rowApplications = number(cells[5], "applications");
            int rowCompiles = number(cells[6], "compiles");
            if (rowApplications < 1 || rowCompiles < 1) {
                throw new IllegalArgumentException("A3M lab work counts");
            }
            boolean rowChanged = strictBoolean(cells[7], "changed");
            boolean fixed = strictBoolean(cells[8], "fixedPoint");
            boolean behavior = strictBoolean(cells[9], "behaviorStable");
            boolean contract = strictBoolean(cells[10], "contractStable");
            boolean lexical = strictBoolean(cells[11], "lexicalDataStable");
            boolean regex = strictBoolean(cells[12], "regexMatrixStable");
            if (!fixed || !behavior || !contract || !lexical || !regex) {
                throw new IllegalArgumentException("A3M lab contains an unqualified row");
            }
            String coordinate = fixture + "\u001f" + schedule;
            if (!coordinates.add(coordinate)) {
                throw new IllegalArgumentException("duplicate A3M lab coordinate");
            }

            fixtures.add(fixture);
            schedules.add(schedule);
            applications = Math.addExact(applications, rowApplications);
            compiles = Math.addExact(compiles, rowCompiles);
            changed += rowChanged ? 1 : 0;
            regexStable++;
            results++;
        }

        for (int fixture = 0; fixture < fixtures.size(); fixture++) {
            if (!fixtures.contains(fixture)) {
                throw new IllegalArgumentException("non-contiguous A3M fixture ordinals");
            }
        }
        return new LabSummary(
                fixtures.size(),
                schedules.size(),
                results,
                applications,
                compiles,
                changed,
                regexStable,
                Set.copyOf(schedules));
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

    private static boolean strictBoolean(String value, String field) {
        String checked = text(value, field);
        if (!"true".equals(checked) && !"false".equals(checked)) {
            throw new IllegalArgumentException(field);
        }
        return Boolean.parseBoolean(checked);
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
