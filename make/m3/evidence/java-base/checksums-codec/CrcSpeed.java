// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
import java.util.SplittableRandom;
import java.util.function.Supplier;
import java.util.zip.Checksum;
import org.apache.commons.codec.digest.PureJavaCrc32;
import org.apache.commons.codec.digest.PureJavaCrc32C;

/**
 * CHECKSUMS_TO_JDK v1 speed: nanoseconds per call of update(byte[], 0, size) plus getValue for PureJavaCrc32 and
 * PureJavaCrc32C, best of 7 rounds after warm-up, for sizes 16 B to 64 KiB. Run once with the codec classes before
 * the overlay and once after. The checksum of all values is printed so the work cannot be optimized away.
 */
public final class CrcSpeed {
    private static final int[] SIZES = {16, 64, 256, 1024, 4096, 65536};
    private static long sink;

    public static void main(String[] args) {
        byte[] data = new byte[65536];
        new SplittableRandom(42).nextBytes(data);
        report("PureJavaCrc32", PureJavaCrc32::new, data);
        report("PureJavaCrc32C", PureJavaCrc32C::new, data);
        System.out.println("sink " + Long.toHexString(sink));
    }

    private static void report(String name, Supplier<Checksum> factory, byte[] data) {
        Checksum checksum = factory.get();
        for (int size : SIZES) {
            long calls = Math.max(2_000, (64L << 20) / size);
            for (int warm = 0; warm < 3; warm++) {
                time(checksum, data, size, calls);
            }
            double best = Double.MAX_VALUE;
            for (int round = 0; round < 7; round++) {
                best = Math.min(best, time(checksum, data, size, calls));
            }
            System.out.printf("%s size=%d ns/call=%.1f GB/s=%.2f%n", name, size, best, size / best);
        }
    }

    private static double time(Checksum checksum, byte[] data, int size, long calls) {
        long start = System.nanoTime(), acc = 0;
        for (long i = 0; i < calls; i++) {
            checksum.reset();
            checksum.update(data, 0, size);
            acc += checksum.getValue();
        }
        sink += acc;
        return (System.nanoTime() - start) / (double) calls;
    }
}
