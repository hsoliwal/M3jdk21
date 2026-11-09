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
package jdk.internal.m3.collections.mat;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/**
 * Checked reader for canonical Mat codec payloads.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatSource.java}
 * blob {@code 962d120f127919a1b388522451995a13094dd1d8} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MatSource {
    private final byte[] bytes;
    private int position;

    public MatSource(byte[] bytes) {
        this.bytes = Objects.requireNonNull(bytes, "bytes");
    }

    public int remaining() { return bytes.length - position; }
    public int position() { return position; }

    public int u8() {
        require(1);
        return Byte.toUnsignedInt(bytes[position++]);
    }

    public short i16() {
        require(2);
        return (short)((u8() << 8) | u8());
    }

    public char u16() {
        return (char)(Short.toUnsignedInt(i16()));
    }

    public int i32() {
        require(4);
        return (u8() << 24) | (u8() << 16) | (u8() << 8) | u8();
    }

    public long i64() {
        require(8);
        long value = 0;
        for (int i=0;i<8;i++) value = (value << 8) | u8();
        return value;
    }

    public byte[] bytes(int length) {
        if (length < 0) throw new IllegalArgumentException("negative length");
        require(length);
        byte[] result = Arrays.copyOfRange(bytes, position, position + length);
        position += length;
        return result;
    }

    public byte[] blob() {
        int length = i32();
        if (length < 0) throw new IllegalArgumentException("negative blob length");
        return bytes(length);
    }

    public String utf8() {
        return new String(blob(), StandardCharsets.UTF_8);
    }

    public void end() {
        if (position != bytes.length) throw new IllegalArgumentException("trailing canonical bytes");
    }

    private void require(int count) {
        if (count < 0 || count > bytes.length - position) throw new IllegalArgumentException("truncated canonical bytes");
    }
}
