// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
/*
 * @test
 * @summary FP32 JNI preflight and read/close lifetime contracts
 * @requires (os.family == "linux") & (vm.bits == "64")
 * @modules jdk.m3.io/jdk.m3.optane
 * @compile --patch-module jdk.m3.io=${test.src} PmemFloatRowsContractTest.java
 * @run main/othervm -Xcheck:jni --patch-module=jdk.m3.io=${test.classes} jdk.m3.optane.PmemFloatRowsContractTest
 * @run main/othervm -Xint -Xcheck:jni --patch-module=jdk.m3.io=${test.classes} jdk.m3.optane.PmemFloatRowsContractTest
 */
package jdk.m3.optane;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Package-private JNI preflight and canonical read-lease contract tests. */
public final class PmemFloatRowsContractTest {
    private PmemFloatRowsContractTest() {}

    @FunctionalInterface private interface Action { void run() throws Exception; }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) throw new AssertionError("lease latch timeout");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError(interrupted);
        }
    }

    private static void refused(Class<? extends Throwable> type, float[] output, Action action) throws Exception {
        float[] before = output.clone();
        Throwable thrown = null;
        try { action.run(); }
        catch (Throwable failure) { thrown = failure; }
        check(thrown != null && type.isInstance(thrown), "wrong JNI refusal: " + thrown);
        check(Arrays.equals(before, output), "native validation changed output");
    }

    private static void nativePreflight(Path path) throws Exception {
        long handle = NativePmem.open(path.toRealPath().toString().getBytes(StandardCharsets.UTF_8),
                0, Files.size(path), PmemAccess.READ_ONLY.ordinal(), PmemBackend.POSIX.ordinal());
        try {
            float[] output = new float[12];
            Arrays.fill(output, -77.0f);
            refused(IndexOutOfBoundsException.class, output,
                    () -> NativePmem.floatRowsLE(handle, 1, 3, 2, 0, new long[]{0, 1, 3}, 3, output, 0));
            refused(IndexOutOfBoundsException.class, output,
                    () -> NativePmem.floatRowsLE(handle, 1, 3, 2, 0, new long[]{0, 1, -1}, 3, output, 0));
            long[] lateInvalid = new long[600];
            lateInvalid[599] = 3;
            float[] large = new float[1200];
            Arrays.fill(large, -77.0f);
            refused(IndexOutOfBoundsException.class, large,
                    () -> NativePmem.floatRowsLE(handle, 1, 3, 2, 0, lateInvalid, 600, large, 0));
            refused(IndexOutOfBoundsException.class, output,
                    () -> NativePmem.floatRowsLE(handle, 1, Long.MAX_VALUE, Integer.MAX_VALUE, 0, null, 1, output, 0));
            refused(IndexOutOfBoundsException.class, output,
                    () -> NativePmem.floatRowsLE(handle, Long.MAX_VALUE, 3, 2, 0, null, 1, output, 0));
            refused(IndexOutOfBoundsException.class, output,
                    () -> NativePmem.floatRowsLE(handle, 1, 3, 2, 2, null, 2, output, 0));
            refused(IndexOutOfBoundsException.class, output,
                    () -> NativePmem.floatRowsLE(handle, 1, 3, 2, 0, new long[]{0}, 2, output, 0));
            refused(IndexOutOfBoundsException.class, output,
                    () -> NativePmem.floatRowsLE(handle, 1, 3, 2, 0, null, 1, output, output.length));
            refused(IndexOutOfBoundsException.class, output,
                    () -> NativePmem.floatRowsLE(handle, 1, 3, 2, 0, null, -1, output, 0));
            refused(IllegalArgumentException.class, output,
                    () -> NativePmem.floatRowsLE(handle, 1, 3, 0, 0, null, 0, output, 0));
            refused(NullPointerException.class, output,
                    () -> NativePmem.floatRowsLE(handle, 1, 3, 2, 0, null, 0, null, 0));
            NativePmem.floatRowsLE(handle, 1, 3, 2, 1, null, 2, output, 3);
            check(output[3] == 3.0f && output[4] == 4.0f && output[5] == 5.0f && output[6] == 6.0f,
                    "native consecutive-row selector");
            check(output[2] == -77.0f && output[7] == -77.0f, "native output guard");
        } finally { NativePmem.close(handle); }
        refused(IllegalStateException.class, new float[0],
                () -> NativePmem.floatRowsLE(0, 0, 0, 1, 0, null, 0, new float[0], 0));
    }

    private static void lease(Path path, PmemBackend backend) throws Exception {
        PmemRegion region = PmemSdk.open(path, 0, Files.size(path), PmemAccess.READ_ONLY, backend);
        try {
            PmemFloatRows table = new PmemFloatRows(region, 1, 3, 2);
            CountDownLatch held = new CountDownLatch(1), release = new CountDownLatch(1);
            CountDownLatch closeStarted = new CountDownLatch(1), closeReturned = new CountDownLatch(1);
            AtomicReference<Throwable> failure = new AtomicReference<>();
            Thread reader = new Thread(() -> {
                try {
                    PmemMapping.withReadLease(region, () -> {
                        held.countDown();
                        await(release);
                        float[] output = new float[6];
                        table.gather(new long[]{2, 0, 2}, 0, 3, output, 0);
                        check(Arrays.equals(output, new float[]{5, 6, 1, 2, 5, 6}), "nested gather values");
                        return null;
                    });
                } catch (Throwable problem) { failure.compareAndSet(null, problem); }
            }, "float-row-reader");
            Thread closer = new Thread(() -> {
                try { closeStarted.countDown(); region.close(); }
                catch (Throwable problem) { failure.compareAndSet(null, problem); }
                finally { closeReturned.countDown(); }
            }, "float-row-closer");
            reader.start();
            await(held);
            closer.start();
            await(closeStarted);
            try { check(!closeReturned.await(70, TimeUnit.MILLISECONDS), "close bypassed read lease"); }
            finally { release.countDown(); }
            reader.join(5_000);
            closer.join(5_000);
            check(!reader.isAlive() && !closer.isAlive(), "lease threads did not exit");
            check(failure.get() == null, "lease failure: " + failure.get());
            check(!region.isOpen(), "close did not complete");
            float[] closedOutput = new float[6];
            refused(IllegalStateException.class, closedOutput,
                    () -> table.gather(new long[]{2, 0, 2}, 0, 3, closedOutput, 0));
        } finally { region.close(); }
    }

    public static void main(String[] args) throws Exception {
        Path path = Files.createTempFile("m3-float-contract-", ".bin");
        try {
            ByteBuffer bytes = ByteBuffer.allocate(25).order(ByteOrder.LITTLE_ENDIAN);
            bytes.put((byte) 0x7f);
            for (int i = 1; i <= 6; i++) bytes.putFloat(i);
            Files.write(path, bytes.array());
            nativePreflight(path);
            lease(path, PmemBackend.FILE);
            lease(path, PmemBackend.POSIX);
            System.out.println("PASS PmemFloatRowsContractTest JNI_preflight=true close_read_lease=FILE,POSIX");
        } finally { Files.deleteIfExists(path); }
    }
}
