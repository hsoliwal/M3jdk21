// SPDX-License-Identifier: Apache-2.0
package com.m3.a3;

import java.util.Objects;
import java.util.Set;

/** Shared value/contract validation for A3 serial FILE work evidence. */
final class A3WorkValues {

    private static final Set<String> SCOPES =
            Set.of("FILE", "PACKAGE", "MODULE", "MULTI_MODULE", "LIBRARY_API");

    private A3WorkValues() {}

    static String fileScope(String value) {
        String checked = scope(value);
        if (!"FILE".equals(checked)) {
            throw new IllegalArgumentException(
                    "A3 work atoms must remain FILE-local");
        }
        return checked;
    }

    static String targetPath(String value, A3Work.TargetState state) {
        Objects.requireNonNull(state, "state");
        if (state == A3Work.TargetState.FEATURE) {
            String checked = Objects.toString(value, "");
            if (!checked.isEmpty()) {
                throw new IllegalArgumentException("feature residue target");
            }
            return "";
        }
        return sourcePath(value);
    }

    static String targetSha256(String value, A3Work.TargetState state) {
        Objects.requireNonNull(state, "state");
        if (state == A3Work.TargetState.PRESENT) {
            return sha256(value);
        }
        String expected =
                switch (state) {
                    case ABSENT -> "ABSENT";
                    case OUTSIDE_A3 -> "UNSCANNED";
                    case FEATURE -> "FEATURE";
                    case PRESENT -> throw new AssertionError();
                };
        if (!expected.equals(value)) {
            throw new IllegalArgumentException("targetSha256");
        }
        return expected;
    }

    static String sourcePath(String value) {
        String checked = text(value, "path").replace('\\', '/');
        if (checked.startsWith("/") || checked.contains("//")) {
            throw new IllegalArgumentException("path");
        }
        for (String part : checked.split("/", -1)) {
            if (part.isEmpty() || ".".equals(part) || "..".equals(part)) {
                throw new IllegalArgumentException("path");
            }
        }
        return checked;
    }

    static String gitSha(String value) {
        String checked = text(value, "commit").toLowerCase(java.util.Locale.ROOT);
        if (!checked.matches("[0-9a-f]{40}")) {
            throw new IllegalArgumentException("commit");
        }
        return checked;
    }

    static String sha256(String value) {
        String checked = text(value, "sha256").toLowerCase(java.util.Locale.ROOT);
        if (!checked.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("sha256");
        }
        return checked;
    }

    static String scope(String value) {
        String checked = text(value, "scope");
        if (!SCOPES.contains(checked)) {
            throw new IllegalArgumentException("scope");
        }
        return checked;
    }

    static int parseInt(String value, String field) {
        long parsed = parseLong(value, field);
        if (parsed > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(field);
        }
        return (int) parsed;
    }

    static long parseLong(String value, String field) {
        try {
            long parsed = Long.parseLong(text(value, field));
            if (parsed < 0) {
                throw new IllegalArgumentException(field);
            }
            return parsed;
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException(field, failure);
        }
    }

    static String text(String value, String field) {
        String checked = clean(value);
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    static String clean(String value) {
        String checked = Objects.toString(value, "").strip();
        if (checked.indexOf('\0') >= 0
                || checked.indexOf('\r') >= 0
                || checked.indexOf('\n') >= 0
                || checked.indexOf('\t') >= 0) {
            throw new IllegalArgumentException("TSV value");
        }
        return checked;
    }

}
