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

import java.util.Objects;

/**
 * Canonical exact Java UTF-16 code-unit codec for opt-in MAT symbol pools.
 *
 * <p>Unlike the existing UTF-8 string codec, this representation preserves
 * isolated surrogate code units. It neither normalizes nor replaces text.
 * The encoded format is a signed 32-bit length followed by unsigned,
 * big-endian 16-bit code units. It changes no existing codec or JDK API.</p>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatUtf16Codec.java}
 * blob {@code 2e44a5f40c70c60321825250d9f4cdb023b3afac} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MatUtf16Codec implements MatCodec<String> {
    public static final MatUtf16Codec INSTANCE = new MatUtf16Codec();

    private MatUtf16Codec() { }

    @Override public String typeId() { return "synex:string-utf16/1"; }
    @Override public Class<String> javaType() { return String.class; }

    @Override
    public void encode(String value, MatSink sink) {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(sink, "sink");
        if (value.length() > (Integer.MAX_VALUE - Integer.BYTES) / Character.BYTES) {
            throw new IllegalArgumentException("UTF-16 symbol exceeds canonical byte capacity");
        }
        sink.i32(value.length());
        for (int index = 0; index < value.length(); index++) {
            sink.u16(value.charAt(index));
        }
    }

    @Override
    public String decode(MatSource source) {
        Objects.requireNonNull(source, "source");
        int units = source.i32();
        if (units < 0 || (long) units * Character.BYTES > source.remaining()) {
            throw new IllegalArgumentException("invalid UTF-16 symbol code-unit count");
        }
        char[] chars = new char[units];
        for (int index = 0; index < units; index++) {
            chars[index] = source.u16();
        }
        return new String(chars);
    }
}
