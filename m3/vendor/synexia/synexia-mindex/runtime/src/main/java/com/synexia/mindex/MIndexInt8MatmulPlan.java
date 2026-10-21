// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Immutable precomputed right-hand matrix for signed-int8 matrix multiplication.
 *
 * <p>The right matrix is packed once into 8x8 tiles. This mirrors the locality of small systolic
 * arrays while keeping the semantic contract independent of any one TPU/NPU implementation.</p>
 */
public final class MIndexInt8MatmulPlan {
    public static final int TILE = 8;
    public static final int MAX_PACKED_BYTES = 256 * 1024 * 1024;
    public static final int MAX_OUTPUT_CELLS = 16 * 1024 * 1024;

    private final int inner;
    private final int columns;
    private final int paddedInner;
    private final int paddedColumns;
    private final byte[] packedRight;
    private final String contentSha256;

    private MIndexInt8MatmulPlan(
            int inner,
            int columns,
            int paddedInner,
            int paddedColumns,
            byte[] packedRight) {
        this.inner = inner;
        this.columns = columns;
        this.paddedInner = paddedInner;
        this.paddedColumns = paddedColumns;
        this.packedRight = packedRight;
        this.contentSha256 = digest(inner, columns, paddedInner, paddedColumns, packedRight);
    }

    public static MIndexInt8MatmulPlan compile(byte[] rightRowMajor, int inner, int columns) {
        Objects.requireNonNull(rightRowMajor, "rightRowMajor");
        if (inner < 1 || columns < 1) {
            throw new IllegalArgumentException("inner and columns must be positive");
        }
        int expected = Math.multiplyExact(inner, columns);
        if (rightRowMajor.length != expected) {
            throw new IllegalArgumentException(
                    "right matrix length " + rightRowMajor.length + " != " + expected);
        }

        int paddedInner = roundUpTile(inner);
        int paddedColumns = roundUpTile(columns);
        int packedLength = Math.multiplyExact(paddedInner, paddedColumns);
        if (packedLength > MAX_PACKED_BYTES) {
            throw new IllegalArgumentException("packed right matrix exceeds configured byte limit");
        }

        byte[] packed = new byte[packedLength];
        int tileColumns = paddedColumns / TILE;
        for (int k = 0; k < inner; k++) {
            int kTile = k / TILE;
            int kWithin = k % TILE;
            for (int column = 0; column < columns; column++) {
                int columnTile = column / TILE;
                int columnWithin = column % TILE;
                int tile = Math.addExact(Math.multiplyExact(kTile, tileColumns), columnTile);
                int packedOffset =
                        Math.addExact(
                                Math.multiplyExact(tile, TILE * TILE),
                                Math.addExact(kWithin * TILE, columnWithin));
                packed[packedOffset] = rightRowMajor[k * columns + column];
            }
        }
        return new MIndexInt8MatmulPlan(
                inner, columns, paddedInner, paddedColumns, packed);
    }

    public int inner() {
        return inner;
    }

    public int columns() {
        return columns;
    }

    public int paddedInner() {
        return paddedInner;
    }

    public int paddedColumns() {
        return paddedColumns;
    }

    public int packedBytes() {
        return packedRight.length;
    }

    public String contentSha256() {
        return contentSha256;
    }

    public byte[] packedRightCopy() {
        return packedRight.clone();
    }

    /** Executes using the process-selected tensor backend. */
    public int[] multiply(byte[] leftRowMajor, int rows, int[] bias) {
        return MIndexTensorRuntime.system().multiply(this, leftRowMajor, rows, bias);
    }

    /** Executes using an explicitly selected backend id. */
    public int[] multiply(String backendId, byte[] leftRowMajor, int rows, int[] bias) {
        return MIndexTensorRuntime.forBackend(backendId)
                .multiply(this, leftRowMajor, rows, bias);
    }

    byte[] packedRightUnsafe() {
        return packedRight;
    }

    int[] referenceMultiply(byte[] leftRowMajor, int rows, int[] bias) {
        validateExecution(leftRowMajor, rows, bias);
        int outputLength = Math.multiplyExact(rows, columns);
        int[] output = new int[outputLength];
        if (rows == 0) return output;

        int tileColumns = paddedColumns / TILE;
        for (int row = 0; row < rows; row++) {
            int leftBase = row * inner;
            int outputBase = row * columns;
            for (int column = 0; column < columns; column++) {
                long sum = bias == null ? 0L : bias[column];
                int columnTile = column / TILE;
                int columnWithin = column % TILE;
                for (int k = 0; k < inner; k++) {
                    int tile = (k / TILE) * tileColumns + columnTile;
                    int packedOffset =
                            tile * TILE * TILE + (k % TILE) * TILE + columnWithin;
                    sum += (long) leftRowMajor[leftBase + k] * packedRight[packedOffset];
                }
                output[outputBase + column] = Math.toIntExact(sum);
            }
        }
        return output;
    }

    void validateExecution(byte[] leftRowMajor, int rows, int[] bias) {
        Objects.requireNonNull(leftRowMajor, "leftRowMajor");
        if (rows < 0) throw new IllegalArgumentException("rows must be non-negative");
        int expectedLeft = Math.multiplyExact(rows, inner);
        if (leftRowMajor.length != expectedLeft) {
            throw new IllegalArgumentException(
                    "left matrix length " + leftRowMajor.length + " != " + expectedLeft);
        }
        int outputCells = Math.multiplyExact(rows, columns);
        if (outputCells > MAX_OUTPUT_CELLS) {
            throw new IllegalArgumentException("output matrix exceeds configured cell limit");
        }
        if (bias != null && bias.length != columns) {
            throw new IllegalArgumentException(
                    "bias length " + bias.length + " != columns " + columns);
        }
    }

    private static int roundUpTile(int value) {
        int remainder = value & (TILE - 1);
        return remainder == 0 ? value : Math.addExact(value, TILE - remainder);
    }

    private static String digest(
            int inner,
            int columns,
            int paddedInner,
            int paddedColumns,
            byte[] packedRight) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
        updateInt(digest, 1); // format
        updateInt(digest, inner);
        updateInt(digest, columns);
        updateInt(digest, paddedInner);
        updateInt(digest, paddedColumns);
        updateInt(digest, packedRight.length);
        digest.update(packedRight);
        return HexFormat.of().formatHex(digest.digest());
    }

    private static void updateInt(MessageDigest digest, int value) {
        digest.update((byte) (value >>> 24));
        digest.update((byte) (value >>> 16));
        digest.update((byte) (value >>> 8));
        digest.update((byte) value);
    }
}
