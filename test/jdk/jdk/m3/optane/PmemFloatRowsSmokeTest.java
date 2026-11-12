// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
/*
 * @test id=file
 * @summary Bounded FP32 rows use the FILE byte-read oracle without JNI
 * @requires (os.family == "linux") & (vm.bits == "64")
 * @modules jdk.m3.io/jdk.m3.optane
 * @run main/othervm -Xmx512m test.PmemFloatRowsSmokeTest FILE
 */
/*
 * @test id=posix
 * @summary Native FP32 row gather matches FILE for bounds, bits and 64-bit identities
 * @requires (os.family == "linux") & (vm.bits == "64")
 * @modules jdk.m3.io/jdk.m3.optane
 * @run main/othervm -Xcheck:jni -Xmx512m test.PmemFloatRowsSmokeTest
 * @run main/othervm -Xint -Xcheck:jni -Xmx512m test.PmemFloatRowsSmokeTest
 */
package test;

import jdk.m3.optane.PmemAccess;
import jdk.m3.optane.PmemBackend;
import jdk.m3.optane.PmemFloatRows;
import jdk.m3.optane.PmemRegion;
import jdk.m3.optane.PmemSdk;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.SplittableRandom;

/** Actual FILE/POSIX parity against the pre-existing byte-read API and JDK LE decoder. */
public final class PmemFloatRowsSmokeTest {
    private static final float GUARD = -1337.25f;
    private static long checks;

    private PmemFloatRowsSmokeTest() {}

    @FunctionalInterface private interface Action { void run() throws Exception; }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }

    private static void equal(float actual, float expected, String label) {
        check(Float.floatToRawIntBits(actual) == Float.floatToRawIntBits(expected), label);
    }

    private static void refused(Class<? extends Throwable> type, float[] output,
                                Action action, String label) throws Exception {
        float[] before = output.clone();
        Throwable thrown = null;
        try { action.run(); }
        catch (Throwable failure) { thrown = failure; }
        check(thrown != null && type.isInstance(thrown), label + ": " + thrown);
        for (int i = 0; i < output.length; i++) equal(output[i], before[i], label + " output " + i);
    }

    private static void scalar(PmemRegion file, long offset, int columns, long row,
                               float[] output, int start) {
        byte[] bytes = new byte[columns * Float.BYTES];
        file.read(offset + row * columns * Float.BYTES, bytes, 0, bytes.length);
        ByteBuffer decoder = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < columns; i++) output[start + i] = decoder.getFloat();
    }

    private static void tableCase(PmemBackend backend, int columns) throws Exception {
        int rows = 7, origin = 3, offset = 6;
        byte[] bytes = new byte[origin + offset + rows * columns * Float.BYTES + 11];
        ByteBuffer seed = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        seed.position(origin + offset); // intentionally unaligned FP32 payload
        int[] specials = {0, 0x80000000, 0x3f800000, 0xbf400000, 0x7f800000,
                0xff800000, 0x7fc12345, 0xffc54321, 1, 0x007fffff, 0x7f7fffff, 0xff7fffff};
        SplittableRandom random = new SplittableRandom(0x464c4f4154L + columns);
        for (int i = 0; i < rows * columns; i++) {
            int bits = i < specials.length ? specials[i] : random.nextInt();
            if ((bits & 0x7f800000) == 0x7f800000 && (bits & 0x007fffff) != 0)
                bits |= 0x00400000; // quiet NaNs: payload preservation has a portable oracle
            seed.putInt(bits);
        }
        Path path = Files.createTempFile("m3-float-rows-", ".bin");
        try {
            Files.write(path, bytes);
            try (PmemRegion file = PmemSdk.open(path, origin, bytes.length - origin,
                         PmemAccess.READ_ONLY, PmemBackend.FILE);
                 PmemRegion region = PmemSdk.open(path, origin, bytes.length - origin,
                         PmemAccess.READ_ONLY, backend)) {
                PmemFloatRows table = new PmemFloatRows(region, offset, rows, columns);
                check(table.rowCount() == rows && table.columns() == columns, "shape metadata");
                long[] selection = {Long.MAX_VALUE, 6, 1, 6, 0, 4, -1};
                float[] output = new float[5 * columns + 9];
                float[] expected = new float[output.length];
                Arrays.fill(output, GUARD);
                Arrays.fill(expected, GUARD);
                for (int i = 0; i < 5; i++) scalar(file, offset, columns, selection[i + 1], expected, 4 + i * columns);
                table.gather(selection, 1, 5, output, 4);
                for (int i = 0; i < output.length; i++) equal(output[i], expected[i], "gather " + backend + "/" + columns + "/" + i);
                check(selection[0] == Long.MAX_VALUE && selection[1] == 6 && selection[6] == -1,
                        "selection input unchanged");
                if (columns == 3) {
                    long[] batch = new long[1031];
                    float[] large = new float[batch.length * columns + 4];
                    float[] oracle = new float[large.length];
                    Arrays.fill(large, GUARD);
                    Arrays.fill(oracle, GUARD);
                    for (int i = 0; i < batch.length; i++) {
                        batch[i] = (i * 5L + 2) % rows;
                        scalar(file, offset, columns, batch[i], oracle, 2 + i * columns);
                    }
                    table.gather(batch, 0, batch.length, large, 2);
                    for (int i = 0; i < large.length; i++) equal(large[i], oracle[i], "multiple ID windows");
                    batch[1025] = rows;
                    refused(IndexOutOfBoundsException.class, large,
                            () -> table.gather(batch, 0, batch.length, large, 2), "late invalid ID in third window");
                }
                selection[1] = -1;
                equal(output[4], expected[4], "selection is not retained");

                // Reuse overlapping output windows: output is independent of the mapping.
                Arrays.fill(output, GUARD);
                table.read(2, output, 2);
                Arrays.fill(expected, GUARD);
                scalar(file, offset, columns, 2, expected, 2);
                for (int i = 0; i < output.length; i++) equal(output[i], expected[i], "row output window");
                output[2] = 42.0f;
                table.read(2, output, 1);
                scalar(file, offset, columns, 2, expected, 1);
                for (int i = 0; i < columns; i++) equal(output[1 + i], expected[1 + i], "no output/storage alias");
                float[] beforeEmpty = output.clone();
                table.gather(new long[0], 0, 0, output, output.length);
                for (int i = 0; i < output.length; i++) equal(output[i], beforeEmpty[i], "empty gather");

                refused(IndexOutOfBoundsException.class, output, () -> table.read(-1, output, 0), "negative row");
                refused(IndexOutOfBoundsException.class, output, () -> table.read(rows, output, 0), "row upper bound");
                refused(IndexOutOfBoundsException.class, output, () -> table.read(Long.MAX_VALUE, output, 0), "row overflow");
                refused(IndexOutOfBoundsException.class, output, () -> table.read(0, output, output.length), "short output");
                refused(IndexOutOfBoundsException.class, output, () -> table.read(0, output, -1), "negative output");
                refused(IndexOutOfBoundsException.class, output, () -> table.gather(new long[]{0, 1, rows}, 0, 3, output, 0), "late invalid ID preflight");
                refused(IndexOutOfBoundsException.class, output, () -> table.gather(new long[]{0, 1, -1}, 0, 3, output, 0), "late negative ID preflight");
                refused(IndexOutOfBoundsException.class, output, () -> table.gather(new long[]{0}, Integer.MAX_VALUE, 1, output, 0), "selection offset overflow");
                refused(IndexOutOfBoundsException.class, output, () -> table.gather(new long[]{0}, 0, Integer.MAX_VALUE, output, 0), "selection count overflow");
                refused(IndexOutOfBoundsException.class, output, () -> table.gather(new long[]{0}, 0, -1, output, 0), "negative count");
                refused(IndexOutOfBoundsException.class, output, () -> table.gather(new long[]{0}, 0, 1, output, Integer.MAX_VALUE), "output offset overflow");
                refused(NullPointerException.class, output, () -> table.gather(null, 0, 0, output, 0), "null selection");
                refused(NullPointerException.class, output, () -> table.gather(new long[0], 0, 0, null, 0), "null output");
                refused(IllegalArgumentException.class, output, () -> new PmemFloatRows(region, offset, 1, 0), "zero columns");
                refused(IllegalArgumentException.class, output, () -> new PmemFloatRows(region, offset, 1, -1), "negative columns");
                refused(IndexOutOfBoundsException.class, output, () -> new PmemFloatRows(region, offset, -1, 1), "negative rows");
                refused(IndexOutOfBoundsException.class, output, () -> new PmemFloatRows(region, offset, Long.MAX_VALUE, Integer.MAX_VALUE), "shape product overflow");
                refused(IndexOutOfBoundsException.class, output, () -> new PmemFloatRows(region, Long.MAX_VALUE, 1, 1), "table offset overflow");
                refused(IndexOutOfBoundsException.class, output, () -> new PmemFloatRows(region, -1, 0, 1), "negative byte offset");
                refused(IndexOutOfBoundsException.class, output, () -> new PmemFloatRows(region, region.length(), 1, 1), "table beyond mapping");
                PmemFloatRows empty = new PmemFloatRows(region, region.length(), 0, Integer.MAX_VALUE);
                empty.gather(new long[0], 0, 0, output, output.length);
                refused(IndexOutOfBoundsException.class, output, () -> empty.read(0, output, 0), "empty table row");
            }
            PmemRegion closed = PmemSdk.open(path, 0, bytes.length, PmemAccess.READ_ONLY, backend);
            PmemFloatRows table = new PmemFloatRows(closed, origin + offset, rows, columns);
            closed.close();
            closed.close();
            float[] guard = new float[columns];
            Arrays.fill(guard, GUARD);
            refused(IllegalStateException.class, guard, () -> table.read(0, guard, 0), "read after close");
            refused(IllegalStateException.class, guard, () -> table.gather(new long[0], 0, 0, guard, 0), "empty gather after close");
            refused(IllegalStateException.class, guard, () -> new PmemFloatRows(closed, 0, 0, 1), "view after close");
        } finally { Files.deleteIfExists(path); }
    }

    private static void longIds(PmemBackend backend) throws Exception {
        long rows = (1L << 31) + 7, offset = 5, length = offset + rows * Float.BYTES + 7;
        Path path = Files.createTempFile("m3-float-rows-sparse-", ".bin");
        long[] ids = {rows - 1, 0, rows - 2, rows - 1};
        float[] expected = {3.25f, -0.0f, -17.5f, 3.25f};
        try {
            try (FileChannel file = FileChannel.open(path, StandardOpenOption.WRITE)) {
                file.write(ByteBuffer.wrap(new byte[1]), length - 1);
                for (int i = 0; i < 3; i++) {
                    ByteBuffer value = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(expected[i]);
                    value.flip();
                    while (value.hasRemaining()) file.write(value, offset + ids[i] * Float.BYTES + value.position());
                }
            }
            try (PmemRegion region = PmemSdk.open(path, 0, length, PmemAccess.READ_ONLY, backend)) {
                PmemFloatRows table = new PmemFloatRows(region, offset, rows, 1);
                float[] output = new float[4];
                table.gather(ids, 0, ids.length, output, 0);
                for (int i = 0; i < output.length; i++) equal(output[i], expected[i], "64-bit row identity " + backend);
                table.read(rows - 1, output, 2);
                equal(output[2], expected[0], "64-bit single row");
                refused(IndexOutOfBoundsException.class, output,
                        () -> table.gather(new long[]{0, Long.MAX_VALUE}, 0, 2, output, 0), "64-bit invalid late row");
            }
        } finally { Files.deleteIfExists(path); }
    }

    public static void main(String[] args) throws Exception {
        boolean fileOnly = args.length == 1 && args[0].equals("FILE");
        for (PmemBackend backend : fileOnly ? new PmemBackend[]{PmemBackend.FILE}
                : new PmemBackend[]{PmemBackend.FILE, PmemBackend.POSIX}) {
            for (int columns : new int[]{1, 3, 17, 4097, 16385}) tableCase(backend, columns);
            longIds(backend);
        }
        PmemRegion foreign = (PmemRegion) Proxy.newProxyInstance(PmemRegion.class.getClassLoader(),
                new Class<?>[]{PmemRegion.class}, (proxy, method, values) -> null);
        refused(IllegalArgumentException.class, new float[0],
                () -> new PmemFloatRows(foreign, 0, 0, 1), "foreign owner");
        refused(NullPointerException.class, new float[0],
                () -> new PmemFloatRows(null, 0, 0, 1), "null owner");
        System.out.println("PASS PmemFloatRowsSmokeTest checks=" + checks + " backends="
                + (fileOnly ? "FILE" : "FILE,POSIX") + " physical_pmem_verified=false");
    }
}
