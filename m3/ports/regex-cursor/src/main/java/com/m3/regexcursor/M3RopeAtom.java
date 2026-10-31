// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.regexcursor;

import java.util.Objects;

/**
 * Proving-ground mirror of {@code java.lang.M3StringAtom}: a scalar spelling in storage width 1
 * (Latin-1) or 2 (UTF-16 big-endian), read in place by unit index.
 *
 * <p>The target atom addresses native or mapped memory ({@code address}); here a private byte
 * lane stands in for that storage. No {@code char[]} is ever produced from it.</p>
 */
public final class M3RopeAtom extends M3RopeOwner {
    private final byte[] payload;
    private final byte width;

    private M3RopeAtom(byte[] payload, byte width, int length, long presence) {
        super(length, presence);
        this.payload = payload;
        this.width = width;
    }

    /** Stores {@code text} with width 1 when every unit is Latin-1, else width 2. */
    public static M3RopeAtom of(CharSequence text) {
        Objects.requireNonNull(text, "text");
        int length = text.length();
        boolean latin1 = true;
        long presence = 0L;
        for (int index = 0; index < length; index++) {
            char unit = text.charAt(index);
            latin1 &= unit <= 0xFF;
            presence |= M3CursorFacts.bit(unit);
        }
        byte width = latin1 ? (byte) 1 : (byte) 2;
        byte[] payload = new byte[length * width];
        for (int index = 0; index < length; index++) {
            store(payload, width, index, text.charAt(index));
        }
        return new M3RopeAtom(payload, width, length, presence);
    }

    private static void store(byte[] payload, byte width, int index, char unit) {
        if (width == 1) {
            payload[index] = (byte) unit;
        } else {
            payload[index << 1] = (byte) (unit >>> 8);
            payload[(index << 1) + 1] = (byte) unit;
        }
    }

    /** Storage width in bytes per unit: 1 or 2. */
    public int storageWidth() {
        return width;
    }

    /** UTF-16 unit at {@code index}, read from storage in place. */
    int unit(int index) {
        if (width == 1) {
            return payload[index] & 0xFF;
        }
        int at = index << 1;
        return ((payload[at] & 0xFF) << 8) | (payload[at + 1] & 0xFF);
    }
}
