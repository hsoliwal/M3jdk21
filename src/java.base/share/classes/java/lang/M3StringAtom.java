/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 */
package java.lang;

import java.util.Objects;

import jdk.internal.misc.Unsafe;

/**
 * Canonical scalar M3String owner.
 *
 * <p>The spelling is native/mapped storage addressed by {@code address}. No byte[] or char[]
 * payload is retained by this owner.</p>
 */
final class M3StringAtom extends M3StringOwner {
    private static final Unsafe UNSAFE = Unsafe.getUnsafe();

    /** Retains mapped/native ownership when the address is not process-life local memory. */
    final Object payloadOwner;
    final long address;
    final byte storageWidth;
    final boolean bigEndian;
    final long canonicalId;

    M3StringAtom(
            Object payloadOwner,
            long address,
            byte storageWidth,
            boolean bigEndian,
            int length,
            byte coder,
            int javaHash,
            long canonicalId,
            long structuralHash64) {
        super(ATOM, length, coder, javaHash, structuralHash64);
        if (length != 0 && address == 0L) throw new IllegalArgumentException("M3 atom address");
        if (storageWidth != 1 && storageWidth != 2) throw new IllegalArgumentException("M3 atom width");
        this.payloadOwner = payloadOwner;
        this.address = address;
        this.storageWidth = storageWidth;
        this.bigEndian = bigEndian;
        this.canonicalId = canonicalId;
    }

    static M3StringAtom local(
            byte[] compactValue, byte coder, long canonicalId, long structuralHash64) {
        int length = compactValue.length >> coder;
        int javaHash =
                coder == String.LATIN1
                        ? StringLatin1.hashCode(compactValue)
                        : StringUTF16.hashCode(compactValue);
        byte width = coder == String.LATIN1 ? (byte) 1 : (byte) 2;
        long bytes = Math.max(1L, (long) length * width);
        long address = UNSAFE.allocateMemory(bytes);
        if (length == 0) {
            UNSAFE.putByte(address, (byte) 0);
        } else if (width == 1) {
            for (int index = 0; index < length; index++) {
                UNSAFE.putByte(address + index, compactValue[index]);
            }
        } else {
            for (int index = 0; index < length; index++) {
                char unit = StringUTF16.charAt(compactValue, index);
                long at = address + ((long) index << 1);
                UNSAFE.putByte(at, (byte) (unit >>> 8));
                UNSAFE.putByte(at + 1L, (byte) unit);
            }
        }
        return new M3StringAtom(
                null, address, width, true, length, coder, javaHash, canonicalId, structuralHash64);
    }

    static M3StringAtom mapped(
            Object payloadOwner,
            long address,
            byte storageWidth,
            boolean bigEndian,
            int length,
            byte coder,
            int javaHash,
            long canonicalId,
            long structuralHash64) {
        return new M3StringAtom(
                payloadOwner,
                address,
                storageWidth,
                bigEndian,
                length,
                coder,
                javaHash,
                canonicalId,
                structuralHash64);
    }

    @Override
    char charAt(int index) {
        Objects.checkIndex(index, length);
        if (storageWidth == 1) return (char) (UNSAFE.getByte(address + index) & 0xff);
        long at = address + ((long) index << 1);
        int first = UNSAFE.getByte(at) & 0xff;
        int second = UNSAFE.getByte(at + 1L) & 0xff;
        return bigEndian
                ? (char) ((first << 8) | second)
                : (char) (first | (second << 8));
    }

    boolean contentEquals(byte[] compactValue, byte valueCoder) {
        if (valueCoder != coder || (compactValue.length >> valueCoder) != length) return false;
        for (int index = 0; index < length; index++) {
            char candidate =
                    valueCoder == String.LATIN1
                            ? StringLatin1.charAt(compactValue, index)
                            : StringUTF16.charAt(compactValue, index);
            if (candidate != charAt(index)) return false;
        }
        return true;
    }

    @Override
    M3StringFacts computeFacts() {
        return M3StringFacts.scan(M3String.whole(this));
    }
}
