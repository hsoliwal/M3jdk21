/* *****************************************************************************
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
package jdk.internal.m3.collections.packed;

/**
 * Explicit factories for primitive double-ended priority queues; no policy auto-switching.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PrimitiveMinMaxQueues.java}
 * blob {@code 5df4676b027abe03b2a303b334d3d16f58bfa75e} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PrimitiveMinMaxQueues {
    private PrimitiveMinMaxQueues() { }
    public static PackedByteMinMaxHeap bytes() { return new PackedByteMinMaxHeap(); }
    public static PackedByteMinMaxHeap bytes(int expectedSize) { return new PackedByteMinMaxHeap(expectedSize); }
    public static PackedByteMinMaxHeap bytes(byte[] values) { return new PackedByteMinMaxHeap(values); }
    public static PackedShortMinMaxHeap shorts() { return new PackedShortMinMaxHeap(); }
    public static PackedShortMinMaxHeap shorts(int expectedSize) { return new PackedShortMinMaxHeap(expectedSize); }
    public static PackedShortMinMaxHeap shorts(short[] values) { return new PackedShortMinMaxHeap(values); }
    public static PackedCharMinMaxHeap chars() { return new PackedCharMinMaxHeap(); }
    public static PackedCharMinMaxHeap chars(int expectedSize) { return new PackedCharMinMaxHeap(expectedSize); }
    public static PackedCharMinMaxHeap chars(char[] values) { return new PackedCharMinMaxHeap(values); }
    public static PackedIntMinMaxHeap ints() { return new PackedIntMinMaxHeap(); }
    public static PackedIntMinMaxHeap ints(int expectedSize) { return new PackedIntMinMaxHeap(expectedSize); }
    public static PackedIntMinMaxHeap ints(int[] values) { return new PackedIntMinMaxHeap(values); }
    public static PackedLongMinMaxHeap longs() { return new PackedLongMinMaxHeap(); }
    public static PackedLongMinMaxHeap longs(int expectedSize) { return new PackedLongMinMaxHeap(expectedSize); }
    public static PackedLongMinMaxHeap longs(long[] values) { return new PackedLongMinMaxHeap(values); }
    public static PackedFloatMinMaxHeap floats() { return new PackedFloatMinMaxHeap(); }
    public static PackedFloatMinMaxHeap floats(int expectedSize) { return new PackedFloatMinMaxHeap(expectedSize); }
    public static PackedFloatMinMaxHeap floats(float[] values) { return new PackedFloatMinMaxHeap(values); }
    public static PackedDoubleMinMaxHeap doubles() { return new PackedDoubleMinMaxHeap(); }
    public static PackedDoubleMinMaxHeap doubles(int expectedSize) { return new PackedDoubleMinMaxHeap(expectedSize); }
    public static PackedDoubleMinMaxHeap doubles(double[] values) { return new PackedDoubleMinMaxHeap(values); }
    public static PackedBooleanMinMaxQueue booleans() { return new PackedBooleanMinMaxQueue(); }
    public static PackedBooleanMinMaxQueue booleans(boolean[] values) { return new PackedBooleanMinMaxQueue(values); }
}
