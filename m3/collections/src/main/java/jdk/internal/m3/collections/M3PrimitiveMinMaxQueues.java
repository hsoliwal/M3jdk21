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
package jdk.internal.m3.collections;

/**
 * Explicit factories for primitive double-ended priority queues; no policy auto-switching.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PrimitiveMinMaxQueues.java}
 * blob {@code 5df4676b027abe03b2a303b334d3d16f58bfa75e} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3PrimitiveMinMaxQueues {
    private M3PrimitiveMinMaxQueues() { }
    public static M3PackedByteMinMaxHeap bytes() { return new M3PackedByteMinMaxHeap(); }
    public static M3PackedByteMinMaxHeap bytes(int expectedSize) { return new M3PackedByteMinMaxHeap(expectedSize); }
    public static M3PackedByteMinMaxHeap bytes(byte[] values) { return new M3PackedByteMinMaxHeap(values); }
    public static M3PackedShortMinMaxHeap shorts() { return new M3PackedShortMinMaxHeap(); }
    public static M3PackedShortMinMaxHeap shorts(int expectedSize) { return new M3PackedShortMinMaxHeap(expectedSize); }
    public static M3PackedShortMinMaxHeap shorts(short[] values) { return new M3PackedShortMinMaxHeap(values); }
    public static M3PackedCharMinMaxHeap chars() { return new M3PackedCharMinMaxHeap(); }
    public static M3PackedCharMinMaxHeap chars(int expectedSize) { return new M3PackedCharMinMaxHeap(expectedSize); }
    public static M3PackedCharMinMaxHeap chars(char[] values) { return new M3PackedCharMinMaxHeap(values); }
    public static M3PackedIntMinMaxHeap ints() { return new M3PackedIntMinMaxHeap(); }
    public static M3PackedIntMinMaxHeap ints(int expectedSize) { return new M3PackedIntMinMaxHeap(expectedSize); }
    public static M3PackedIntMinMaxHeap ints(int[] values) { return new M3PackedIntMinMaxHeap(values); }
    public static M3PackedLongMinMaxHeap longs() { return new M3PackedLongMinMaxHeap(); }
    public static M3PackedLongMinMaxHeap longs(int expectedSize) { return new M3PackedLongMinMaxHeap(expectedSize); }
    public static M3PackedLongMinMaxHeap longs(long[] values) { return new M3PackedLongMinMaxHeap(values); }
    public static M3PackedFloatMinMaxHeap floats() { return new M3PackedFloatMinMaxHeap(); }
    public static M3PackedFloatMinMaxHeap floats(int expectedSize) { return new M3PackedFloatMinMaxHeap(expectedSize); }
    public static M3PackedFloatMinMaxHeap floats(float[] values) { return new M3PackedFloatMinMaxHeap(values); }
    public static M3PackedDoubleMinMaxHeap doubles() { return new M3PackedDoubleMinMaxHeap(); }
    public static M3PackedDoubleMinMaxHeap doubles(int expectedSize) { return new M3PackedDoubleMinMaxHeap(expectedSize); }
    public static M3PackedDoubleMinMaxHeap doubles(double[] values) { return new M3PackedDoubleMinMaxHeap(values); }
    public static M3PackedBooleanMinMaxQueue booleans() { return new M3PackedBooleanMinMaxQueue(); }
    public static M3PackedBooleanMinMaxQueue booleans(boolean[] values) { return new M3PackedBooleanMinMaxQueue(values); }
}
