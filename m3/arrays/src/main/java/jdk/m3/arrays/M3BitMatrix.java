// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

/** Immutable dense Boolean matrix retained as row-major machine words. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/d4985c2a46d713724b3872c85633655e53d76662/synexia-mat-collections/src/main/java/com/synexia/matcollections/MatBitMatrix.java blob b242a869e88f; recipe m3/arrays/recipe/bitmatrix-20261106/relocate.py v3
public final class M3BitMatrix {
    /** Java is the unconditional default; native use requires explicit caller selection. */
    public enum Backend { JAVA, NATIVE_REQUIRED }

    private final int rows;
    private final int columns;
    private final int wordsPerRow;
    private final long[] words;

    private M3BitMatrix(int rows, int columns, int wordsPerRow, long[] words) {
        this(rows, columns, wordsPerRow, words, false);
    }

    /** Only a freshly allocated, unaliased result may transfer its ownership. */
    private M3BitMatrix(int rows, int columns, int wordsPerRow, long[] words,
            boolean ownedResult) {
        this.rows = rows;
        this.columns = columns;
        this.wordsPerRow = wordsPerRow;
        this.words = ownedResult ? words : words.clone();
        maskRowTails(this.words, rows, columns, wordsPerRow);
    }

    public static Builder builder(int rows, int columns) {
        return new Builder(rows, columns);
    }

    public int rows() { return rows; }
    public int columns() { return columns; }
    public long primitivePayloadBytes() { return 8L * words.length; }

    public boolean get(int row, int column) {
        Objects.checkIndex(row, rows);
        Objects.checkIndex(column, columns);
        int word = row * wordsPerRow + (column >>> 6);
        return (words[word] & (1L << (column & 63))) != 0L;
    }

    public int rowCardinality(int row) {
        Objects.checkIndex(row, rows);
        int base = row * wordsPerRow;
        int count = 0;
        for (int word = 0; word < wordsPerRow; word++) count += Long.bitCount(words[base + word]);
        return count;
    }

    /** Each set column is selected once, ordered by column, into one owned primitive array. */
    public M3IntPostingList setColumns(int row) {
        Objects.checkIndex(row, rows);
        int[] result = new int[rowCardinality(row)];
        int out = 0;
        int base = row * wordsPerRow;
        for (int word = 0; word < wordsPerRow; word++) {
            long value = words[base + word];
            while (value != 0) {
                int column = (word << 6) + Long.numberOfTrailingZeros(value);
                if (column < columns) result[out++] = column;
                value &= value - 1;
            }
        }
        // No defensive second copy: result was allocated here and was never exposed.
        return M3IntPostingList.trustedView(result, 0, result.length);
    }

    /** Transposition defaults to deterministic Java: no native library loading or JNI. */
    public M3BitMatrix transpose() {
        int targetWordsPerRow = wordsFor(rows);
        long[] target = transposeJavaWords(words, targetWordsPerRow);
        return new M3BitMatrix(columns, rows, targetWordsPerRow, target, true);
    }

    /**
     * Optional whole-matrix JNI batch. It receives one detached immutable source snapshot;
     * the full native result is checked against the Java oracle before publication.
     * Neither backend retains native pointers or publishes a partial matrix.
     */
    public M3BitMatrix transpose(Backend backend) {
        Objects.requireNonNull(backend, "backend");
        if (backend == Backend.JAVA) return transpose();
        if (!NativeBackend.AVAILABLE) {
            throw new UnsatisfiedLinkError("MAT_BIT_MATRIX_NATIVE_REQUIRED_UNAVAILABLE");
        }
        int targetWordsPerRow = wordsFor(rows);
        int expectedLength = checkedWordCount(columns, targetWordsPerRow);
        long[] sourceSnapshot = words.clone();
        long[] actual = transposeNative0(sourceSnapshot, rows, columns,
                wordsPerRow, targetWordsPerRow);
        if (actual == null || actual.length != expectedLength
                || !Arrays.equals(actual, transposeJavaWords(sourceSnapshot, targetWordsPerRow))) {
            throw new IllegalStateException("MAT_BIT_MATRIX_NATIVE_PARITY_FAILURE");
        }
        return new M3BitMatrix(columns, rows, targetWordsPerRow, actual, true);
    }

    /** Explicit native ABI probe; Java calls remain independent of native availability. */
    public static boolean nativeAvailable() { return NativeBackend.AVAILABLE; }

    /** Dense 64x64 tiles use six word-swaps; sparse and partial tiles use exact bit visits. */
    private long[] transposeJavaWords(long[] source, int targetWordsPerRow) {
        long[] target = new long[checkedWordCount(columns, targetWordsPerRow)];
        if (target.length == 0) return target; // zero-area dimensions may be Integer.MAX_VALUE
        int fullRows = 0;
        int fullWords = 0;
        if (rows >= 64 && columns >= 64) {
            int sample = 0;
            for (int local = 0; local < 64; local++) {
                sample += Long.bitCount(source[local * wordsPerRow]);
            }
            // Deterministic first full tile probe; a miss keeps the scalar oracle.
            // Individual dense tiles retain the exact >=256-population gate.
            if (sample >= 256) {
                fullRows = rows & ~63;
                fullWords = columns >>> 6;
            }
        }
        long[] tile = fullRows > 0 && fullWords > 0 ? new long[64] : null;
        for (int rowBase = 0; rowBase < fullRows; rowBase += 64) {
            int targetWord = rowBase >>> 6;
            for (int word = 0; word < wordsPerRow; word++) {
                if (word < fullWords) {
                    int population = 0;
                    for (int local = 0; local < 64; local++) {
                        long bits = source[(rowBase + local) * wordsPerRow + word];
                        tile[local] = bits;
                        population += Long.bitCount(bits);
                    }
                    if (population >= 256) {
                        transposeTile64(tile);
                        int firstColumn = word << 6;
                        for (int local = 0; local < 64; local++) {
                            target[(firstColumn + local) * targetWordsPerRow + targetWord] = tile[local];
                        }
                        continue;
                    }
                }
                for (int local = 0; local < 64; local++) {
                    long bits = source[(rowBase + local) * wordsPerRow + word];
                    while (bits != 0L) {
                        int column = (word << 6) + Long.numberOfTrailingZeros(bits);
                        if (column < columns) {
                            target[column * targetWordsPerRow + targetWord] |= 1L << local;
                        }
                        bits &= bits - 1L;
                    }
                }
            }
        }
        for (int row = fullRows; row < rows; row++) {
            int base = row * wordsPerRow;
            long rowMask = 1L << (row & 63);
            int targetWord = row >>> 6;
            for (int word = 0; word < wordsPerRow; word++) {
                long bits = source[base + word];
                while (bits != 0L) {
                    int column = (word << 6) + Long.numberOfTrailingZeros(bits);
                    if (column < columns) target[column * targetWordsPerRow + targetWord] |= rowMask;
                    bits &= bits - 1L;
                }
            }
        }
        return target;
    }

    /** In-place six-stage butterfly: bit (row,column) becomes bit (column,row). */
    private static void transposeTile64(long[] tile) {
        long mask = 0xffff_ffff_0000_0000L;
        for (int shift = 32; shift != 0; shift >>>= 1) {
            for (int at = 0; at < 64; at = (at + shift + 1) & ~shift) {
                long swap = (tile[at] ^ (tile[at + shift] << shift)) & mask;
                tile[at] ^= swap;
                tile[at + shift] ^= swap >>> shift;
            }
            if (shift > 1) mask ^= mask >>> (shift >>> 1);
        }
    }

    /** Overflow-safe even at Integer.MAX_VALUE columns without allocating that universe. */
    private static int wordsFor(int columns) {
        return (columns >>> 6) + ((columns & 63) == 0 ? 0 : 1);
    }

    private static int checkedWordCount(int rows, int wordsPerRow) {
        long result = (long) rows * wordsPerRow;
        if (result > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("matrix too large");
        }
        return (int) result;
    }

    private static void maskRowTails(long[] words, int rows, int columns, int wordsPerRow) {
        int remainder = columns & 63;
        if (remainder == 0 || wordsPerRow == 0) return;
        long mask = (1L << remainder) - 1L;
        for (int row = 0; row < rows; row++) words[row * wordsPerRow + wordsPerRow - 1] &= mask;
    }

    private static final class NativeBackend {
        private static final boolean AVAILABLE = load();

        private static boolean load() {
            String absolutePath = System.getProperty("m3.matmatrix.native.path", "");
            if (absolutePath.isBlank()) return false;
            try {
                System.load(Path.of(absolutePath).toAbsolutePath().normalize().toString());
                return nativeAbi0() == 1;
            } catch (UnsatisfiedLinkError | SecurityException | InvalidPathException error) {
                return false;
            }
        }
    }

    private static native int nativeAbi0();
    private static native long[] transposeNative0(long[] source, int rows, int columns,
            int wordsPerRow, int targetWordsPerRow);

    public static final class Builder {
        private final int rows;
        private final int columns;
        private final int wordsPerRow;
        private final long[] words;

        private Builder(int rows, int columns) {
            if (rows < 0 || columns < 0) throw new IllegalArgumentException("negative matrix dimension");
            this.rows = rows;
            this.columns = columns;
            this.wordsPerRow = wordsFor(columns);
            this.words = new long[checkedWordCount(rows, wordsPerRow)];
        }

        public Builder set(int row, int column) {
            return set(row, column, true);
        }

        public Builder set(int row, int column, boolean value) {
            Objects.checkIndex(row, rows);
            Objects.checkIndex(column, columns);
            int word = row * wordsPerRow + (column >>> 6);
            long mask = 1L << (column & 63);
            if (value) words[word] |= mask;
            else words[word] &= ~mask;
            return this;
        }

        public M3BitMatrix build() {
            return new M3BitMatrix(rows, columns, wordsPerRow, words);
        }
    }
}
