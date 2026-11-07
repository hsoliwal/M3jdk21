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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/**
 * Deterministic big-endian canonical byte sink.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatSink.java}
 * blob {@code fd2045dfa2a5cd8247f3aa507d1c3fe9eb4885b4} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3MatSink {
    private byte[] bytes;
    private int size;

    public M3MatSink() {
        this(64);
    }

    public M3MatSink(int initialCapacity) {
        if (initialCapacity < 0) throw new IllegalArgumentException("negative capacity");
        bytes = new byte[Math.max(1, initialCapacity)];
    }

    public int size() { return size; }

    public M3MatSink u8(int value) {
        if ((value & ~0xff) != 0) throw new IllegalArgumentException("u8");
        ensure(1);
        bytes[size++] = (byte)value;
        return this;
    }

    public M3MatSink i16(short value) {
        ensure(2);
        bytes[size++] = (byte)(value >>> 8);
        bytes[size++] = (byte)value;
        return this;
    }

    public M3MatSink u16(char value) {
        ensure(2);
        bytes[size++] = (byte)(value >>> 8);
        bytes[size++] = (byte)value;
        return this;
    }

    public M3MatSink i32(int value) {
        ensure(4);
        bytes[size++] = (byte)(value >>> 24);
        bytes[size++] = (byte)(value >>> 16);
        bytes[size++] = (byte)(value >>> 8);
        bytes[size++] = (byte)value;
        return this;
    }

    public M3MatSink i64(long value) {
        ensure(8);
        for (int shift=56; shift>=0; shift-=8) bytes[size++] = (byte)(value >>> shift);
        return this;
    }

    public M3MatSink bytes(byte[] value) {
        Objects.requireNonNull(value, "value");
        ensure(value.length);
        System.arraycopy(value, 0, bytes, size, value.length);
        size += value.length;
        return this;
    }

    public M3MatSink blob(byte[] value) {
        Objects.requireNonNull(value, "value");
        i32(value.length);
        return bytes(value);
    }

    public M3MatSink utf8(String value) {
        return blob(Objects.requireNonNull(value, "value").getBytes(StandardCharsets.UTF_8));
    }

    public byte[] toByteArray() {
        return Arrays.copyOf(bytes, size);
    }

    private void ensure(int additional) {
        int required = Math.addExact(size, additional);
        if (required > bytes.length) {
            int next = bytes.length;
            while (next < required) next = Math.max(required, Math.multiplyExact(next, 2));
            bytes = Arrays.copyOf(bytes, next);
        }
    }
}
