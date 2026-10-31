/* SPDX-License-Identifier: Apache-2.0 */

import com.m3.vcpu.internal.NativeVcpu;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * JNI end-to-end smoke test: loader chain, the six entry points, int[] and direct ByteBuffer uploads, the
 * RV32IM kernel (sum 1..100 on 64 cores) and the status-to-exception mapping, on "cpu" and (if present) "opencl".
 */
public final class NativeVcpuSmoke {
    private static int checks;
    private static int failures;

    private NativeVcpuSmoke() {
    }

    private static void check(boolean ok, String what) {
        checks++;
        if (!ok) {
            failures++;
            System.out.println("FAIL " + what);
        }
    }

    private static void expect(Class<? extends Throwable> type, Runnable action, String what) {
        try {
            action.run();
            check(false, what + ": no exception");
        } catch (Throwable thrown) {
            check(type.isInstance(thrown), what + ": got " + thrown);
        }
    }

    static int i(int imm, int rs1, int f3, int rd, int op) {
        return ((imm & 0xFFF) << 20) | (rs1 << 15) | (f3 << 12) | (rd << 7) | op;
    }

    static int r(int f7, int rs2, int rs1, int f3, int rd) {
        return (f7 << 25) | (rs2 << 20) | (rs1 << 15) | (f3 << 12) | (rd << 7) | 0x33;
    }

    static int b(int imm, int rs2, int rs1, int f3) {
        return (((imm >> 12) & 1) << 31) | (((imm >> 5) & 0x3F) << 25) | (rs2 << 20) | (rs1 << 15) | (f3 << 12)
                | (((imm >> 1) & 0xF) << 8) | (((imm >> 11) & 1) << 7) | 0x63;
    }

    static int[] sumProgram() {
        return new int[] {i(0, 0, 0, 10, 0x13), i(1, 0, 0, 11, 0x13), i(101, 0, 0, 12, 0x13), r(0, 11, 10, 0, 10),
            i(1, 11, 0, 11, 0x13), b(-8, 12, 11, 4), 0x0000000B};
    }


    /* Crosses every 256-int JNI staging boundary, preserving logical positions
     * and duplicate/signed values without requiring the GPU backend. */
    private static void verifyStagedTransfer(long session, String backend) {
        int[] sizes = {0, 1, 2, 255, 256, 257, 511, 512, 513, 4095, 4096, 4097, 8193};
        java.util.SplittableRandom random = new java.util.SplittableRandom(0x4d334a4e49534cL);
        for (int size : sizes) {
            long buffer = NativeVcpu.alloc(session, (size + 32L) * Integer.BYTES);
            try {
                int[] src = new int[size + 13];
                int[] dst = new int[size + 19];
                for (int i = 0; i < src.length; i++) src[i] = random.nextInt();
                int[] before = src.clone();
                java.util.Arrays.fill(dst, 0x51a7e1);
                NativeVcpu.upload(buffer, 7L * Integer.BYTES, src, 3, size);
                NativeVcpu.download(buffer, 7L * Integer.BYTES, dst, 5, size);
                boolean correct = java.util.Arrays.equals(before, src);
                for (int i = 0; i < dst.length; i++) {
                    int value = i >= 5 && i < size + 5 ? src[i - 2] : 0x51a7e1;
                    if (dst[i] != value) { correct = false; break; }
                }
                check(correct, backend + ": staged int[] copy size " + size);
                expect(IndexOutOfBoundsException.class,
                        () -> NativeVcpu.upload(buffer, Long.MAX_VALUE, new int[1], 0, 1),
                        backend + ": invalid native addressable byte range");
            } finally {
                NativeVcpu.close(buffer);
            }
            expect(IllegalStateException.class,
                    () -> NativeVcpu.download(buffer, 0, new int[1], 0, 1),
                    backend + ": retired staging buffer");
        }
    }

    /* Either full completion before retirement, or IllegalStateException
     * after retirement, is admissible; corrupted data or process crash is not. */
    private static void verifyConcurrentClose(long session) {
        long handle = NativeVcpu.alloc(session, 131072);
        int[] input = new int[8192];
        java.util.Arrays.fill(input, 0x1f6e5bd);
        java.util.concurrent.CountDownLatch ready = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> error =
                new java.util.concurrent.atomic.AtomicReference<>();
        Thread worker = new Thread(() -> {
            try {
                ready.await();
                for (int attempt = 0; attempt < 500; attempt++) {
                    try {
                        NativeVcpu.upload(handle, 0, input, 0, input.length);
                        int[] output = new int[input.length];
                        NativeVcpu.download(handle, 0, output, 0, output.length);
                        if (!java.util.Arrays.equals(input, output)) {
                            throw new AssertionError("buffer changed during serialized transfer");
                        }
                    } catch (IllegalStateException retired) {
                        return;
                    }
                }
            } catch (Throwable failure) {
                error.compareAndSet(null, failure);
            }
        }, "m3vcpu-jni-transfer-close");
        worker.start();
        ready.countDown();
        NativeVcpu.close(handle);
        try { worker.join(); }
        catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError(interrupted);
        }
        NativeVcpu.close(handle);
        check(error.get() == null, "concurrent retired-buffer transfer " + error.get());
        expect(IllegalStateException.class,
                () -> NativeVcpu.upload(handle, 0, input, 0, 1), "retired handle reused");
    }

    private static void runBackend(String backend) {
        long session;
        try {
            session = NativeVcpu.open(backend, -1);
        } catch (UnsupportedOperationException unavailable) {
            System.out.println("SKIP " + backend + ": " + unavailable.getMessage());
            return;
        }
        int cores = 64;
        int words = 32;
        long mem = NativeVcpu.alloc(session, 4L * cores * words);
        long regs = NativeVcpu.alloc(session, 4L * cores * 32);
        long state = NativeVcpu.alloc(session, 4L * cores * 6);
        long config = NativeVcpu.alloc(session, 12);
        try {
            int[] image = new int[cores * words];
            int[] program = sumProgram();
            for (int core = 0; core < cores; core++) {
                System.arraycopy(program, 0, image, core * words, program.length);
            }
            int[] st = new int[cores * 6];
            for (int core = 0; core < cores; core++) {
                st[core * 6 + 3] = -1;
            }
            NativeVcpu.upload(mem, 0, image, 0, image.length);
            NativeVcpu.upload(state, 0, st, 0, st.length);
            ByteBuffer cfg = ByteBuffer.allocateDirect(12).order(ByteOrder.nativeOrder());
            cfg.putInt(cores).putInt(words).putInt(1000);
            NativeVcpu.upload(config, 0, cfg, 0, 12);
            NativeVcpu.launch(session, NativeVcpu.RV32IM_SLICE, cores, 0, new long[] {mem, regs, state, config});
            int[] out = new int[cores * 6];
            NativeVcpu.download(state, 0, out, 0, out.length);
            boolean all = true;
            for (int core = 0; core < cores; core++) {
                all &= out[core * 6 + 1] == 1 && out[core * 6 + 5] == 5050 && out[core * 6 + 2] == 304;
            }
            check(all, backend + ": 64 cores halt with exit 5050 after 304 instructions");
            int[] one = new int[2];
            NativeVcpu.download(state, 4L * 6 * 7 + 4, one, 1, 1);
            check(one[1] == 1, backend + ": offset download reads core 7 STATUS");

            expect(IllegalArgumentException.class, () -> NativeVcpu.alloc(session, 0), backend + " alloc(0)");
            expect(IndexOutOfBoundsException.class, () -> NativeVcpu.upload(mem, 0, image, 0, image.length + 1),
                    backend + " upload past the array");
            expect(IndexOutOfBoundsException.class, () -> NativeVcpu.upload(config, 4, new int[3], 0, 3),
                    backend + " upload past the buffer");
            expect(IllegalArgumentException.class, () -> NativeVcpu.upload(config, 0, "text", 0, 1),
                    backend + " upload of a non-buffer object");
            expect(UnsupportedOperationException.class,
                    () -> NativeVcpu.launch(session, "nope", 1, 0, new long[] {mem}), backend + " unknown kernel");
            if ("cpu".equals(backend)) {
                verifyStagedTransfer(session, backend);
                verifyConcurrentClose(session);
            }
            NativeVcpu.upload(config, 0, new int[] {cores * 2, words, 10}, 0, 3);
            expect(IndexOutOfBoundsException.class,
                    () -> NativeVcpu.launch(session, NativeVcpu.RV32IM_SLICE, cores, 0,
                            new long[] {mem, regs, state, config}), backend + " geometry guard");
            expect(IllegalArgumentException.class,
                    () -> NativeVcpu.launch(session, NativeVcpu.RV32IM_SLICE, cores, 0,
                            new long[] {mem, mem, state, config}), backend + " aliased buffers");
            expect(IllegalStateException.class, () -> NativeVcpu.alloc(12345L, 8), backend + " forged handle");
            NativeVcpu.close(regs);
            NativeVcpu.close(regs);
            expect(IllegalStateException.class, () -> NativeVcpu.download(regs, 0, new int[1], 0, 1),
                    backend + " closed buffer");
        } finally {
            NativeVcpu.close(session);
            NativeVcpu.close(session);
        }
        expect(IllegalStateException.class, () -> NativeVcpu.download(mem, 0, new int[1], 0, 1),
                backend + " buffer of a closed session");
        NativeVcpu.close(mem);
        NativeVcpu.close(state);
        NativeVcpu.close(config);
        System.out.println(backend + ": smoke done");
    }

    public static void main(String[] args) {
        NativeVcpu.load();
        check(NativeVcpu.isLoaded(), "loaded");
        expect(UnsupportedOperationException.class, () -> NativeVcpu.open("nope", -1), "unknown backend");
        runBackend("cpu");
        runBackend("opencl");
        System.out.printf("NativeVcpuSmoke: %d checks, %d failures -> %s%n", checks, failures,
                failures == 0 ? "PASS" : "FAIL");
        System.exit(failures == 0 ? 0 : 1);
    }
}
