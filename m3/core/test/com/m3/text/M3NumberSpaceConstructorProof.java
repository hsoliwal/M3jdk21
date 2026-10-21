/* Copyright 2026 Hitesh Soliwal
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.text;

import java.util.Objects;

/** Independent public-contract proof; run against preimage and generated target. */
public final class M3NumberSpaceConstructorProof {
    private static long checks;

    public static void main(String[] args) {
        M3NumberSpace space = M3NumberSpace.INSTANCE;
        require(M3NumberSpace.MIN_VALUE == 0 && M3NumberSpace.MAX_VALUE == 10_000, "fixed inclusive domain");
        require(space.precomputedValueCount() == 10_001, "value count");
        StorageIdentity shared = space.storageIdentity();
        int expectedOffset = 0;
        for (int value = 0; value <= 10_000; value++) {
            String oracle = Integer.toString(value);
            LocalM3StringPiece piece = space.number(value);
            require(piece == space.number(value), "cached reference " + value);
            require(shared.equals(piece.storageIdentity()), "shared backing owner " + value);
            require(piece.encoding() == LocalM3StringPiece.Encoding.UTF16_LE, "UTF-16 encoding " + value);
            require(piece.codeUnitOffset() == expectedOffset, "contiguous offset " + value);
            require(piece.length() == oracle.length(), "length " + value);
            for (int index = 0; index < oracle.length(); index++) {
                require(piece.charAt(index) == oracle.charAt(index), "character " + value + ":" + index);
            }
            require(piece.flatten().equals(oracle), "flatten " + value);
            require(space.spelling(value).equals(oracle), "spelling " + value);
            require(space.sourceRecordId(value).equals(oracle), "source identity spelling " + value);
            require(space.parse(piece) == piece, "parse cached M3 piece " + value);
            require(space.parse(new NoStringView("xx" + oracle + "yy", 2, oracle.length())) == piece,
                    "parse sliced foreign sequence without toString " + value);
            expectedOffset += oracle.length();
        }
        require(expectedOffset == 38_895, "total decimal UTF-16 units");
        require(space.number(0).retainedBackingBytes() == 77_790, "single exact UTF-16 backing size");
        require(space.number(10_000).retainedBackingBytes() == 77_790, "last view retains same backing size");

        for (int value : new int[] {Integer.MIN_VALUE, -1, 10_001, Integer.MAX_VALUE}) {
            expect(IndexOutOfBoundsException.class, () -> space.number(value));
            expect(IndexOutOfBoundsException.class, () -> space.spelling(value));
            expect(IndexOutOfBoundsException.class, () -> space.sourceRecordId(value));
        }
        expect(NullPointerException.class, () -> space.parse(null));
        String[] invalid = {
            "", "00", "01", "00000", "010000", "-0", "+0", "-1", "+1", "10001", "99999",
            "2147483647", "9223372036854775807", "9".repeat(80), " 1", "1 ", "1\t", "1\n", "1\r",
            "1 0", "1e3", "0x10", "1_000", "1.0", "10x", "\0", "1\0", "\u0661", "\uFF11",
            "\u0141", "\uD800", "\uDC00", "\uD83D\uDE00"
        };
        for (String spelling : invalid) {
            expect(NumberFormatException.class, () -> space.parse(spelling));
            expect(NumberFormatException.class, () -> space.parse(new NoStringView("xx" + spelling + "yy", 2, spelling.length())));
        }
        for (int value : new int[] {0, 9, 10, 99, 100, 999, 1_000, 9_999, 10_000}) {
            LocalM3StringPiece piece = space.number(value);
            expect(IndexOutOfBoundsException.class, () -> piece.charAt(-1));
            expect(IndexOutOfBoundsException.class, () -> piece.charAt(piece.length()));
            require(piece.subSequence(0, piece.length()).flatten().equals(Integer.toString(value)), "full view slice");
            require(piece.subSequence(piece.length(), piece.length()).length() == 0, "empty end slice");
        }
        System.out.println("M3_NUMBER_SPACE_CONSTRUCTOR_CONTRACT_PASS checks=" + checks
                + " values=10001 utf16Units=" + expectedOffset + " sharedBackingBytes=77790"
                + " invalidParseInputs=" + invalid.length + " parserToStringCalls=0");
    }

    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        try {
            action.run();
        } catch (Throwable failure) {
            require(type.isInstance(failure), "expected " + type.getName() + " but received " + failure);
            return;
        }
        throw new AssertionError("missing " + type.getName());
    }

    private static final class NoStringView implements CharSequence {
        private final String source;
        private final int start;
        private final int length;

        private NoStringView(String source, int start, int length) {
            Objects.checkFromIndexSize(start, length, source.length());
            this.source = source;
            this.start = start;
            this.length = length;
        }

        @Override public int length() { return length; }
        @Override public char charAt(int index) {
            Objects.checkIndex(index, length);
            return source.charAt(start + index);
        }
        @Override public CharSequence subSequence(int from, int to) {
            Objects.checkFromToIndex(from, to, length);
            return new NoStringView(source, start + from, to - from);
        }
        @Override public String toString() {
            throw new AssertionError("parser materialized input via toString");
        }
    }
}
