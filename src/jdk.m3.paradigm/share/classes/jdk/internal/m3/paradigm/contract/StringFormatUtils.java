/* ******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.paradigm.contract;

import jdk.internal.m3.collections.PlaceholderLogger;

import java.util.Optional;
/**
 * StringFormatUtils - Common string formatting and padding utilities.
 *
 * <h2>Purpose</h2>
 * <p>
 * Centralizes string formatting patterns used in display and console output,
 * particularly for formatted tables and aligned output.
 * </p>
 *
 * <h2>Patterns Consolidated</h2>
 * <ul>
 * <li>Right-padding: {@code String.format("%-Ns", s)}</li>
 * <li>Repeated spaces: {@code " ".repeat(n)}</li>
 * <li>Whitespace normalization: {@code replaceAll("\\s+", " ").trim()}</li>
 * <li>String truncation and ellipsis</li>
 * </ul>
 *
 * @since IOP 1.0 - String Format Utilities
 * @m3.pattern synexia:bop
 * @m3.algorithm Combines width guards, String.format, String.repeat, regex replacement and UTF-16 substring operations.
 * @m3.dataStructures String inputs/results, primitive dimensions, an ellipsis-length constant and Optional wrappers.
 * @m3.description Reusable text padding, repetition, normalization, truncation and box formatting operations.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/utils/StringFormatUtils.java}
 * blob {@code b61f9a813c03c117022ec5ff03393ea60f605d42} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class StringFormatUtils {
    private static final PlaceholderLogger log = PlaceholderLogger.of(StringFormatUtils.class);


    /** Length of the ellipsis string "..." used in truncation.
     * @m3.pattern synexia:bop
     * @m3.algorithm Initializes a compile-time int constant to the three UTF-16 code units in the literal ellipsis.
     * @m3.dataStructures One static final int constant.
     * @m3.description Width constant used by the ellipsis truncation operation.
     */
    private static final int ELLIPSIS_LENGTH = 3;

    /**
     * @m3.pattern synexia:bop
     * @m3.algorithm Executes an empty private constructor body without initializing utility state.
     * @m3.dataStructures One transient utility instance if reflectively invoked; no declared instance fields.
     * @m3.description Private constructor restricts the static utility owner to its own implementation.
     */
    private StringFormatUtils() {
        // Utility class - no instantiation
    }

    // --- PADDING UTILITIES ---

    /**
     * Pads a string to the right with spaces to reach a target width.
     *
     * <p>
     * Replaces pattern: {@code String.format("%-Ns", str)}
     * </p>
     *
     * @param str
     *            the string to pad
     * @param width
     *            the target width
     * @return the right-padded string
     * @m3.pattern synexia:bop
     * @m3.algorithm Uses spaces for null input, preserves strings at least as wide as requested, and otherwise formats a left-justified field.
     * @m3.dataStructures String input/result, int width and a generated format String.
     * @m3.description Right-padding operation.
     */
    public static String padRight(final String str, final int width) {
        if (str == null) {
            return spaces(width);
        }
        if (str.length() >= width) {
            return str;
        }
        return String.format("%-" + width + "s", str);
    }

    /**
     * Pads a string to the left with spaces to reach a target width.
     *
     * @param str
     *            the string to pad
     * @param width
     *            the target width
     * @return the left-padded string
     * @m3.pattern synexia:bop
     * @m3.algorithm Uses spaces for null input, preserves strings at least as wide as requested, and otherwise formats a right-justified field.
     * @m3.dataStructures String input/result, int width and a generated format String.
     * @m3.description Left-padding operation.
     */
    public static String padLeft(final String str, final int width) {
        if (str == null) {
            return spaces(width);
        }
        if (str.length() >= width) {
            return str;
        }
        return String.format("%" + width + "s", str);
    }

    /**
     * Pads a string to the center with spaces.
     *
     * @param str
     *            the string to pad
     * @param width
     *            the target width
     * @return the center-padded string
     * @m3.pattern synexia:bop
     * @m3.algorithm Uses spaces for null input, preserves sufficiently wide strings, and splits remaining padding with the extra space on the right.
     * @m3.dataStructures String input/result and three int padding counts.
     * @m3.description Center-padding operation.
     */
    public static String padCenter(final String str, final int width) {
        if (str == null) {
            return spaces(width);
        }
        if (str.length() >= width) {
            return str;
        }
        int totalPadding = width - str.length();
        int leftPadding = totalPadding / 2;
        int rightPadding = totalPadding - leftPadding;
        return spaces(leftPadding) + str + spaces(rightPadding);
    }

    // --- SPACE UTILITIES ---

    /**
     * Generates a string of repeated spaces.
     *
     * <p>
     * Replaces pattern: {@code " ".repeat(n)}
     * </p>
     *
     * @param count
     *            the number of spaces
     * @return a string of repeated spaces
     * @m3.pattern synexia:bop
     * @m3.algorithm Returns an empty String for nonpositive counts and repeats the space literal for positive counts.
     * @m3.dataStructures An int count, the space literal and a String result.
     * @m3.description Repeated-space operation.
     */
    public static String spaces(final int count) {
        if (count <= 0) {
            return "";
        }
        return " ".repeat(Math.max(0, count));
    }

    /**
     * Generates a string of repeated characters.
     *
     * @param character
     *            the character to repeat
     * @param count
     *            the number of repetitions
     * @return a string of repeated characters
     * @m3.pattern synexia:bop
     * @m3.algorithm Returns an empty String for nonpositive counts, otherwise repeats String.valueOf(character).
     * @m3.dataStructures A char value, an int count and String values.
     * @m3.description Repeated-character operation.
     */
    public static String repeat(final char character, final int count) {
        if (count <= 0) {
            return "";
        }
        return String.valueOf(character).repeat(Math.max(0, count));
    }

    /**
     * Generates a horizontal line (repeated character).
     *
     * @param width
     *            the width of the line
     * @return a horizontal line
     * @m3.pattern synexia:bop
     * @m3.algorithm Delegates to repeat with the box-drawing character U+2500.
     * @m3.dataStructures A char literal, an int width and a String result.
     * @m3.description Default horizontal-line operation.
     */
    public static String line(final int width) {
        return repeat('─', width);
    }

    /**
     * Generates a horizontal line (repeated character).
     *
     * @param width
     *            the width of the line
     * @param character
     *            the character to use
     * @return a horizontal line
     * @m3.pattern synexia:bop
     * @m3.algorithm Delegates the supplied character and width to repeat.
     * @m3.dataStructures A char parameter, an int width and a String result.
     * @m3.description Caller-selected horizontal-line operation.
     */
    public static String line(final int width, final char character) {
        return repeat(character, width);
    }

    // --- NORMALIZATION UTILITIES ---

    /**
     * Normalizes whitespace in a string (multiple spaces become single space).
     *
     * <p>
     * Replaces pattern: {@code str.replaceAll("\\s+", " ").trim()}
     * </p>
     *
     * @param str
     *            the string to normalize
     * @return the normalized string with single spaces
     * @m3.pattern synexia:bop
     * @m3.algorithm Returns empty Optional for null, replaces regex whitespace runs with one space, then applies String.trim.
     * @m3.dataStructures String input/replacement/result, a regex literal and Optional wrapper.
     * @m3.description Whitespace-run replacement and trimming operation.
     */
    public static Optional<String> normalizeWhitespace(final String str) {
        if (str == null) {
            return Optional.empty();
        }
        return Optional.of(str.replaceAll("\\s+", " ").trim());
    }

    /**
     * Removes all whitespace from a string.
     *
     * @param str
     *            the string to process
     * @return the string with all whitespace removed
     * @m3.pattern synexia:bop
     * @m3.algorithm Returns empty Optional for null and otherwise replaces regex whitespace runs with an empty String.
     * @m3.dataStructures String input/result, regex and replacement literals and Optional wrapper.
     * @m3.description Regex-whitespace removal operation.
     */
    public static Optional<String> removeWhitespace(final String str) {
        if (str == null) {
            return Optional.empty();
        }
        return Optional.of(str.replaceAll("\\s+", ""));
    }

    // --- TRUNCATION UTILITIES ---

    /**
     * Truncates a string to a maximum length with ellipsis.
     *
     * @param str
     *            the string to truncate
     * @param maxLength
     *            the maximum length (including ellipsis)
     * @return the truncated string, or original if shorter
     * @m3.pattern synexia:bop
     * @m3.algorithm Preserves null or sufficiently short strings; otherwise keeps max(0,maxLength-3) UTF-16 code units and appends three dots.
     * @m3.dataStructures String input/prefix/result, int bounds and ELLIPSIS_LENGTH.
     * @m3.description UTF-16 prefix truncation with literal ellipsis.
     */
    public static String truncate(final String str, final int maxLength) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, Math.max(0, maxLength - ELLIPSIS_LENGTH)) + "...";
    }

    /**
     * Truncates a string to a maximum length without ellipsis.
     *
     * @param str
     *            the string to truncate
     * @param maxLength
     *            the maximum length
     * @return the truncated string, or original if shorter
     * @m3.pattern synexia:bop
     * @m3.algorithm Preserves null or sufficiently short strings; otherwise calls substring(0,maxLength), including its exception for a negative bound.
     * @m3.dataStructures String input/result and an int bound.
     * @m3.description UTF-16 prefix truncation operation.
     */
    public static String truncatePlain(final String str, final int maxLength) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength);
    }

    // --- BOX DRAWING UTILITIES ---

    /**
     * Creates a horizontal box line.
     *
     * @param width
     *            the width
     * @param leftChar
     *            left corner character
     * @param fillChar
     *            fill character
     * @param rightChar
     *            right corner character
     * @return the formatted line
     * @m3.pattern synexia:bop
     * @m3.algorithm Concatenates both corners for widths below two; otherwise inserts width-2 repeated fill characters between them.
     * @m3.dataStructures Three char parameters, an int width and String values.
     * @m3.description Corner-bounded horizontal-line operation.
     */
    public static String boxLine(final int width, final char leftChar, final char fillChar, final char rightChar) {
        if (width < 2) {
            return String.valueOf(leftChar) + rightChar;
        }
        return leftChar + repeat(fillChar, width - 2) + rightChar;
    }

    /**
     * Creates a formatted cell in a box (padded and bounded).
     *
     * @param content
     *            the cell content
     * @param width
     *            the cell width
     * @param leftBound
     *            left boundary character
     * @param rightBound
     *            right boundary character
     * @return the formatted cell
     * @m3.pattern synexia:bop
     * @m3.algorithm Right-pads content to width-2 and concatenates the two boundary characters; oversized content is preserved.
     * @m3.dataStructures String content/padded/result, two char bounds and an int width.
     * @m3.description Boundary-bounded padded-cell operation.
     */
    public static String boxCell(final String content, final int width, final char leftBound, final char rightBound) {
        String padded = padRight(content, width - 2);
        return leftBound + padded + rightBound;
    }
}
