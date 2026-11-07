/*******************************************************************************
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
package jdk.m3.collections;

/**
 * Finite work/retention limits for canonical immutable-value structures.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatLimits.java}
 * blob {@code 3d435b69034d3e0d83c315a3c9c37c1d0e8a6ee7} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public record M3MatLimits(
        int maxEntries,
        int maxInputValues,
        int maxValueBytes,
        int maxArenaBytes,
        long maxStagingBytes,
        int maxRows,
        int maxMemoEntries,
        int maxMemoArity,
        int maxImageBytes) {

    public static final M3MatLimits DEFAULT = new M3MatLimits(
            1_048_576,
            2_097_152,
            16 * 1024 * 1024,
            512 * 1024 * 1024,
            768L * 1024 * 1024,
            1_048_576,
            1_048_576,
            256,
            768 * 1024 * 1024);

    public static final M3MatLimits UNBOUNDED = new M3MatLimits(
            Integer.MAX_VALUE,
            Integer.MAX_VALUE,
            Integer.MAX_VALUE,
            Integer.MAX_VALUE,
            Long.MAX_VALUE,
            Integer.MAX_VALUE,
            Integer.MAX_VALUE,
            Integer.MAX_VALUE,
            Integer.MAX_VALUE);

    public M3MatLimits {
        if (maxEntries < 0 || maxInputValues < 0 || maxValueBytes < 0
                || maxArenaBytes < 0 || maxStagingBytes < 0 || maxRows < 0
                || maxMemoEntries < 0 || maxMemoArity < 0 || maxImageBytes < 0) {
            throw new IllegalArgumentException("MAT limits must be non-negative");
        }
    }

    void inputValues(long value) {
        if (value < 0 || value > maxInputValues) fail("input value", value, maxInputValues);
    }

    void entries(long value) {
        if (value < 0 || value > maxEntries) fail("entry", value, maxEntries);
    }

    void valueBytes(long value) {
        if (value < 0 || value > maxValueBytes) fail("value byte", value, maxValueBytes);
    }

    void arenaBytes(long value) {
        if (value < 0 || value > maxArenaBytes) fail("arena byte", value, maxArenaBytes);
    }

    void stagingBytes(long value) {
        if (value < 0 || value > maxStagingBytes) fail("staging byte", value, maxStagingBytes);
    }

    void rows(long value) {
        if (value < 0 || value > maxRows) fail("row", value, maxRows);
    }

    void memoEntries(long value) {
        if (value < 0 || value > maxMemoEntries) fail("memo entry", value, maxMemoEntries);
    }

    void memoArity(long value) {
        if (value < 0 || value > maxMemoArity) fail("memo arity", value, maxMemoArity);
    }

    void imageBytes(long value) {
        if (value < 0 || value > maxImageBytes) fail("image byte", value, maxImageBytes);
    }

    private static void fail(String kind, long actual, long limit) {
        throw new IllegalArgumentException(kind + " budget exceeded: " + actual + " > " + limit);
    }
}
