// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * UTF-8 decoding layer: a byte shadow that feeds {@link M3FileCharShadow}s in one pass.
 *
 * <p>A multi-byte sequence split across blocks is carried (at most three bytes) and completed by the
 * next block, so the chars equal {@code new String(allBytes, UTF_8)} for any block split, including
 * the U+FFFD replacements for malformed input. Chars reach the layers in blocks of up to 4096.</p>
 */
public final class M3FileUtf8Shadow implements M3FileShadow {
    private final CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE);
    private final ByteBuffer carry = ByteBuffer.allocate(8);
    private final CharBuffer chars = CharBuffer.allocate(4096);
    private final M3FileCharShadow[] layers;
    private long charCount;
    private boolean finished;

    M3FileUtf8Shadow(M3FileCharShadow... layers) {
        this.layers = layers.clone();
        for (M3FileCharShadow layer : this.layers) Objects.requireNonNull(layer, "layer");
    }

    @Override
    public void update(byte[] bytes, int offset, int length) {
        if (finished) throw new IllegalStateException("utf-8 layer already finished");
        Objects.checkFromIndexSize(offset, length, bytes.length);
        int position = offset;
        int end = offset + length;
        while (carry.position() > 0 && position < end) {
            carry.put(bytes[position++]);
            carry.flip();
            decode(carry, false);
            carry.compact();
        }
        if (position < end) {
            ByteBuffer block = ByteBuffer.wrap(bytes, position, end - position);
            decode(block, false);
            carry.put(block);
        }
    }

    @Override
    public void finish() {
        if (finished) return;
        finished = true;
        carry.flip();
        decode(carry, true);
        carry.clear();
        while (decoder.flush(chars).isOverflow()) drain();
        drain();
        for (M3FileCharShadow layer : layers) layer.finish();
    }

    /** UTF-16 chars delivered to the layers. */
    public long charCount() {
        return charCount;
    }

    private void decode(ByteBuffer in, boolean endOfInput) {
        CoderResult result;
        do {
            result = decoder.decode(in, chars, endOfInput);
            drain();
        } while (result.isOverflow());
    }

    private void drain() {
        int length = chars.position();
        if (length == 0) return;
        for (M3FileCharShadow layer : layers) layer.update(chars.array(), 0, length);
        charCount += length;
        chars.clear();
    }
}
