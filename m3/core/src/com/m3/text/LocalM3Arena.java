/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.text;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Explicit application-owned allocator. This first stage is NOT an intern cache.
 * It retains no backing arrays; each immutable piece owns its backing lifetime.
 * Construction/activation is explicit, outside java.lang.String and VM bootstrap.
 */
public final class LocalM3Arena {
    /** Starts a fresh owner namespace; no backing arrays are retained by this arena. */
    public LocalM3Arena() { }

    private final UUID owner = UUID.randomUUID();
    private final AtomicLong sequence = new AtomicLong();

    private StorageIdentity identity() {
        long record = sequence.updateAndGet(previous -> {
            if (previous == Long.MAX_VALUE) throw new IllegalStateException("identity exhausted");
            return previous + 1;
        });
        return new StorageIdentity(owner, 1, record);
    }

    /** Copies every UTF16 code unit, including unpaired surrogates, exactly once. */
    public LocalM3StringPiece copyUtf16(char[] input) {
        Objects.requireNonNull(input);
        byte[] bytes = new byte[Math.multiplyExact(input.length, 2)];
        for (int i = 0; i < input.length; i++) {
            char c = input[i];
            bytes[i * 2] = (byte)c;
            bytes[i * 2 + 1] = (byte)(c >>> 8);
        }
        return owned(bytes, LocalM3StringPiece.Encoding.UTF16_LE);
    }

    /** Copies Latin1 bytes; no charset guessing or decoding replacement policy. */
    public LocalM3StringPiece copyLatin1(byte[] input) {
        return owned(Objects.requireNonNull(input).clone(), LocalM3StringPiece.Encoding.LATIN1);
    }

    LocalM3StringPiece owned(byte[] bytes, LocalM3StringPiece.Encoding encoding) {
        return new LocalM3StringPiece(bytes, encoding, identity(), 0, bytes.length >> encoding.shift);
    }
}
