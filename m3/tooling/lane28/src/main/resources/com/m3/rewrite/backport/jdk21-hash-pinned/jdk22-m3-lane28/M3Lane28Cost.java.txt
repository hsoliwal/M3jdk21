// SPDX-License-Identifier: Apache-2.0
import java.util.function.LongSupplier;
import jdk.internal.mindex.M3BitLane28;

/** Bounded cost probe; descriptive observations, not a benchmark acceptance gate. */
public class M3Lane28Cost {
    private static volatile long sink;
    private static M3BitLane28 fixture() {
        M3BitLane28 bits = new M3BitLane28();
        for (int region : new int[]{0, 1 << 20, 127 << 20, 255 << 20}) {
            for (int i = 0; i < 32768; i += 5) bits.set(region + i);
        }
        return bits;
    }
    public static void main(String[] args) throws Exception {
        String library = System.getProperty("lane28.native");
        if (library != null) Class.forName("jdk.internal.mindex.Lane28Loader")
                .getMethod("load", String.class).invoke(null, library);
        long admission = 0, firstRank = 0;
        for (int i = 0; i < 24; i++) {
            long start = System.nanoTime(); M3BitLane28 bits = fixture();
            long ready = System.nanoTime(); int rank = bits.rank(128 << 20);
            long end = System.nanoTime();
            if (rank != 3 * 6554) throw new AssertionError("cold rank");
            if (i >= 8) { admission += ready - start; firstRank += end - ready; }
        }
        System.out.printf("COST case=admission iterations=16 ns=%d%n", admission);
        System.out.printf("COST case=first-rank iterations=16 ns=%d%n", firstRank);
        M3BitLane28 bits = fixture(); bits.rank(128 << 20);
        measure("warm-rank", 20000, () -> bits.rank(128 << 20));
        measure("warm-select", 20000, () -> bits.select(17000));
        measure("java-full-count", 1000, () -> bits.cardinality(0, 1 << 28));
        measure("jni-full-count", 1000, () -> bits.cardinalityNative(0, 1 << 28));
        measure("java-partial-count", 1000, () -> bits.cardinality(7, 20000));
        measure("jni-partial-count", 1000, () -> bits.cardinalityNative(7, 20000));
        if (bits.cardinality(0, 1 << 28) != bits.cardinalityNative(0, 1 << 28))
            throw new AssertionError("full parity");
        if (bits.cardinality(7, 20000) != bits.cardinalityNative(7, 20000))
            throw new AssertionError("partial parity");
        System.out.printf("COST pages=%d payloadBytes=%d sink=%d%n", bits.allocatedBlockCount(), bits.payloadBytes(), sink);
    }
    private static void measure(String name, int iterations, LongSupplier operation) {
        long sum = 0;
        for (int i = 0; i < 3000; i++) sum += operation.getAsLong();
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) sum += operation.getAsLong();
        long end = System.nanoTime(); sink = sum;
        System.out.printf("COST case=%s iterations=%d ns=%d%n", name, iterations, end - start);
    }
}
