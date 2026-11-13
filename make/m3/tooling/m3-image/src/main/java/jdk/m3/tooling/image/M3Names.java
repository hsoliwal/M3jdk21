/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.image;

/** Java binary-name validation without imposing an ASCII-only policy. */
public final class M3Names {
    private M3Names() {
    }

    /** Returns whether a string is a dot-separated Java binary name. */
    public static boolean isBinaryName(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        int segmentStart = 0;
        while (segmentStart < value.length()) {
            int segmentEnd = value.indexOf('.', segmentStart);
            if (segmentEnd < 0) {
                segmentEnd = value.length();
            }
            if (segmentEnd == segmentStart || !validSegment(value, segmentStart, segmentEnd)) {
                return false;
            }
            segmentStart = segmentEnd + 1;
        }
        return value.charAt(value.length() - 1) != '.';
    }

    /** Validates and returns one binary name. */
    public static String requireBinaryName(String value, String label) {
        if (!isBinaryName(value)) {
            throw new IllegalArgumentException("Invalid " + label + ": " + value);
        }
        return value;
    }

    private static boolean validSegment(String value, int start, int end) {
        int codePoint = value.codePointAt(start);
        if (!Character.isJavaIdentifierStart(codePoint)) {
            return false;
        }
        for (int index = start + Character.charCount(codePoint); index < end;) {
            codePoint = value.codePointAt(index);
            if (!Character.isJavaIdentifierPart(codePoint)) {
                return false;
            }
            index += Character.charCount(codePoint);
        }
        return true;
    }
}
