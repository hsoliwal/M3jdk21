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

import jdk.m3.arrays.SelectionBits;

import java.util.Objects;

/**
 * Exact, size-sensitive canonical codec for immutable finite-universe bitmaps.
 *
 * <p>The wire representation is a big-endian int bit-universe length followed
 * by exactly ceil(length / 64) big-endian long words. Bit order and the finite
 * universe size are significant; nonzero padding bits are rejected on decode.
 * This optional codec reuses {@link MatPool}, without a second intern table.</p>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatBitsCodec.java}
 * blob {@code 5b28947ef6bab63201d78a89284913778582c0cf} at com.synexia develop {@code 35474ccf} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MatBitsCodec implements MatCodec<SelectionBits> {
    public static final MatBitsCodec INSTANCE = new MatBitsCodec();

    private MatBitsCodec() { }

    @Override public String typeId() { return "synex:bits-universe/1"; }
    @Override public Class<SelectionBits> javaType() { return SelectionBits.class; }

    @Override
    public void encode(SelectionBits value, MatSink sink) {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(sink, "sink");
        sink.i32(value.size());
        for (int index = 0; index < value.wordCount(); index++) {
            sink.i64(value.word(index));
        }
    }

    @Override
    public SelectionBits decode(MatSource source) {
        Objects.requireNonNull(source, "source");
        int universe = source.i32();
        if (universe < 0) throw new IllegalArgumentException("negative bitmap universe");
        int wordCount = SelectionBits.wordsFor(universe);
        if ((long) wordCount * Long.BYTES > source.remaining()) {
            throw new IllegalArgumentException("truncated canonical bitmap words");
        }
        long[] words = new long[wordCount];
        for (int index = 0; index < wordCount; index++) {
            words[index] = source.i64();
        }
        int partialBits = universe & 63;
        if (partialBits != 0 && wordCount != 0
                && (words[wordCount - 1] >>> partialBits) != 0L) {
            throw new IllegalArgumentException("noncanonical bitmap padding bits");
        }
        return SelectionBits.ofWords(universe, words);
    }
}
