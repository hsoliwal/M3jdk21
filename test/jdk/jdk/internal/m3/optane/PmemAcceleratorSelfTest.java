// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.optane;

import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

/** Executable JDK-only contract oracle: no Optane device required. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/56cbdc6312594f06e21ef56f0ce33b605383270b/synexia-m3-jmods/accelerator-pmem/src/test/java/com/synexia/m3/accelerator/pmem/PmemAcceleratorSelfTest.java blob fbebe6786ee9; recipe make/m3/optane/recipe/pmem-accelerator-20261107/relocate.py v4
public final class PmemAcceleratorSelfTest {
    private PmemAcceleratorSelfTest() { }
    private static int cases;
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        cases++;
    }
    private static void throwsType(Class<? extends Throwable> type, Runnable work) {
        try { work.run(); }
        catch (Throwable ex) { check(type.isInstance(ex), ex.toString()); return; }
        throw new AssertionError("expected " + type.getName());
    }
    public static void main(String[] args) throws Exception {
        Path small = Files.createTempFile("m3-pmem-q8-", ".bin");
        Path large = Files.createTempFile("m3-pmem-long-", ".bin");
        try {
            Random random = new Random(0x4d33504dL);
            byte[] bytes = new byte[4096];
            random.nextBytes(bytes);
            Files.write(small, bytes);
            try (PmemAccelerator.Mapping map = PmemAccelerator.open(small, false)) {
                check(map.sizeBytes() == bytes.length, "size");
                check(map.read(0, 16)[0] == bytes[0], "read");
                check(map.read(bytes.length - 1, 1)[0] == bytes[bytes.length - 1], "last byte");
                throwsType(IndexOutOfBoundsException.class, () -> map.read(bytes.length, 1));
                // Invalid 2GiB+ requests MUST fail before allocating Java arrays.
                // Run this suite with -Xmx64m: old code threw OutOfMemoryError.
                throwsType(IndexOutOfBoundsException.class,
                        () -> map.read(0, Integer.MAX_VALUE));
                throwsType(IndexOutOfBoundsException.class,
                        () -> map.read(Long.MAX_VALUE, 1));
                check(map.read(bytes.length, 0).length == 0, "empty final byte range");
                throwsType(IndexOutOfBoundsException.class,
                        () -> map.matVecQ8(0, Integer.MAX_VALUE, 1, 1f, new float[]{1f}));
                throwsType(IndexOutOfBoundsException.class,
                        () -> map.matVecQ8(Long.MAX_VALUE, 1, 1, 1f, new float[]{1f}));
                // Contract: native vector products fold in exact Java FP32 column order.
                final int precisionCols = 80, precisionOffset = 128;
                for (int seed = 0; seed < 2048; seed++) {
                    float[] activations = new float[precisionCols];
                    for (int col = 0; col < precisionCols; col++) {
                        int factor = (seed * 11 + col * 107) % 4093 - 2046;
                        activations[col] = factor * (col % 3 == 0 ? 10000000f
                                : col % 3 == 1 ? 0.000001f : 1f);
                    }
                    float reference = 0;
                    for (int col = 0; col < precisionCols; col++)
                        reference += (float)bytes[precisionOffset + col] * activations[col];
                    float measured = map.matVecQ8(precisionOffset, 1, precisionCols,
                            1f, activations)[0];
                    check(Float.floatToRawIntBits(measured)
                                    == Float.floatToRawIntBits(reference),
                            "JNI FP32 Q8 reduction order seed=" + seed);
                }

                // Cross 64-row native output tile boundaries without altering
                // FP32 reduction order, scale multiplication, or signed zero.
                final int tileColumns = 7;
                final int tileOffset = 8;
                float[] tileInput = {2f, -0.125f, 0.75f, 1e-5f, -400f, 3.5f, -2f};
                for (int tileRows : new int[]{1, 63, 64, 65, 127, 128, 129, 257, 580}) {
                    for (float tileScale : new float[]{1f, -1.5f, 0f, -0f}) {
                        float[] tiled = map.matVecQ8(tileOffset, tileRows,
                                tileColumns, tileScale, tileInput);
                        check(tiled.length == tileRows, "tiled result length");
                        for (int row = 0; row < tileRows; row++) {
                            float expected = 0f;
                            for (int col = 0; col < tileColumns; col++) {
                                expected += (float) bytes[tileOffset + row*tileColumns + col]
                                        * tileInput[col];
                            }
                            expected *= tileScale;
                            check(Float.floatToRawIntBits(expected)
                                            == Float.floatToRawIntBits(tiled[row]),
                                    "tiled FP32 row=" + row + " rows=" + tileRows
                                            + " scale=" + tileScale);
                        }
                    }
                }
                throwsType(IndexOutOfBoundsException.class, () -> map.read(-1, 1));
                throwsType(IllegalArgumentException.class, () -> map.read(0, -1));
                throwsType(IllegalArgumentException.class, () -> map.matVecQ8(0, 1, 2, Float.NaN, new float[2]));
                for (int i=0; i<44; i++) {
                    int rows = 1 + random.nextInt(5);
                    int cols = 1 + random.nextInt(53);
                    int offset = random.nextInt(bytes.length - rows * cols + 1);
                    float[] x = new float[cols];
                    for (int c=0; c<cols; c++) x[c] = (random.nextFloat() - .5f) * 10;
                    float scale = .125f;
                    float[] got = map.matVecQ8(offset, rows, cols, scale, x);
                    for (int r=0; r<rows; r++) {
                        float expected = 0;
                        for (int c=0; c<cols; c++) expected += bytes[offset + r * cols + c] * x[c];
                        expected *= scale;
                        check(Math.abs(got[r] - expected) < .0025f + Math.abs(expected) * .00002f,
                                "matvec parity pass=" + i + " row=" + r);
                    }
                }
                throwsType(IndexOutOfBoundsException.class,
                        () -> map.matVecQ8(bytes.length - 2, 2, 2, 1, new float[2]));
            }
            PmemAccelerator.Mapping closed = PmemAccelerator.open(small, false);
            closed.close(); closed.close();
            throwsType(IllegalStateException.class, closed::sizeBytes);
            try (RandomAccessFile f = new RandomAccessFile(large.toFile(), "rw")) {
                f.seek(2_147_483_650L);
                f.write(123);
            }
            try (PmemAccelerator.Mapping map = PmemAccelerator.open(large, false)) {
                check(map.sizeBytes() == 2_147_483_651L, "long size");
                check(map.read(2_147_483_650L, 1)[0] == (byte)123, "long offset");
            }
            try (PmemAccelerator.Mapping map = PmemAccelerator.open(small, false)) {
                if (map.daxState() != PmemAccelerator.DaxState.CONFIRMED) {
                    try { PmemAccelerator.open(small, true); throw new AssertionError("DAX gate"); }
                    catch (java.io.IOException expected) { cases++; }
                }
            }
            check(PmemAccelerator.cpuFeatureMask() >= 0, "CPU features");
            System.out.println("PASS m3-pmem Java21/JNI cases=" + cases);
        } finally {
            Files.deleteIfExists(small);
            Files.deleteIfExists(large);
        }
    }
}
