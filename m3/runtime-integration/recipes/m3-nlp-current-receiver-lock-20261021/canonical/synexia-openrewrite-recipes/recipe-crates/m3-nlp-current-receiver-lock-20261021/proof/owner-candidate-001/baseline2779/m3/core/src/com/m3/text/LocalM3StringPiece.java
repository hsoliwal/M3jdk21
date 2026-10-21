/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.text;

import java.util.Objects;

/**
 * Range coordinates are UTF16 code units within one immutable owned backing.
 * Subranges retain the whole backing. Releasing an arena never invalidates them.
 * Identity describes the backing, not the range; compare coordinates as well.
 */
public final class LocalM3StringPiece implements M3StringPiece {
    public enum Encoding {
        LATIN1(0), UTF16_LE(1);
        final int shift;
        Encoding(int shift) { this.shift = shift; }
    }
    private final byte[] backing;
    private final Encoding encoding;
    private final StorageIdentity identity;
    private final int codeUnitOffset;
    private final int codeUnitLength;

    LocalM3StringPiece(byte[] owned, Encoding encoding, StorageIdentity identity,
                       int offset, int length) {
        this.backing = owned;
        this.encoding = encoding;
        this.identity = identity;
        this.codeUnitOffset = offset;
        this.codeUnitLength = length;
    }

    public StorageIdentity storageIdentity() { return identity; }
    public Encoding encoding() { return encoding; }
    public int codeUnitOffset() { return codeUnitOffset; }
    /** Payload retained by this handle, including bytes outside its range. */
    public int retainedBackingBytes() { return backing.length; }
    @Override public int length() { return codeUnitLength; }
    @Override public char charAt(int index) {
        Objects.checkIndex(index, codeUnitLength);
        int byteIndex = (codeUnitOffset + index) << encoding.shift;
        int low = backing[byteIndex] & 255;
        return (char)(encoding == Encoding.LATIN1 ? low : low | ((backing[byteIndex + 1] & 255) << 8));
    }
    @Override public LocalM3StringPiece subSequence(int start, int end) {
        Objects.checkFromToIndex(start, end, codeUnitLength);
        return new LocalM3StringPiece(backing, encoding, identity, codeUnitOffset + start, end - start);
    }
    @Override public String toString() { return flatten(); }
}
