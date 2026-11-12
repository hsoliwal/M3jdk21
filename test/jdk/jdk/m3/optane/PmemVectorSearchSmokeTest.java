// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.optane;

import jdk.m3.optane.PmemAccess;
import jdk.m3.optane.PmemBackend;
import jdk.m3.optane.PmemRegion;
import jdk.m3.optane.PmemSdk;
import jdk.m3.optane.PmemSearch;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Independent full-sort cosine oracle; the production kernel retains only top-k rows. */
public final class PmemVectorSearchSmokeTest {
    private static int assertions;
    private PmemVectorSearchSmokeTest() { }
    private static void check(boolean value, String name) {
        if (!value) throw new AssertionError(name);
        assertions++;
    }
    private static void rejects(Class<? extends Throwable> type, Runnable task) {
        try { task.run(); }
        catch (Throwable failure) {
            if (!type.isInstance(failure)) throw new AssertionError("unexpected refusal", failure);
            assertions++; return;
        }
        throw new AssertionError("missing refusal: " + type.getName());
    }
    private static byte[] encode(float[][] rows) {
        ByteBuffer bytes = ByteBuffer.allocate(rows.length * rows[0].length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (float[] row : rows) for (float value : row) bytes.putFloat(value);
        return bytes.array();
    }
    private static double cosine(float[] row, float[] query) {
        double dot = 0, a = 0, b = 0;
        for (int i = 0; i < row.length; i++) {
            dot += (double) row[i] * query[i];
            a += (double) row[i] * row[i]; b += (double) query[i] * query[i];
        }
        double score = Math.max(-1d, Math.min(1d, dot / Math.sqrt(b) / Math.sqrt(a)));
        return score == 0 ? 0 : score;
    }
    private static void verify(PmemRegion region, long offset, float[][] vectors, float[] query, int k) {
        Integer[] expected = new Integer[vectors.length];
        double[] scores = new double[vectors.length];
        for (int row = 0; row < vectors.length; row++) {
            expected[row] = row; scores[row] = cosine(vectors[row], query);
        }
        Arrays.sort(expected, Comparator.<Integer>comparingDouble(row -> scores[row]).reversed()
                .thenComparingInt(Integer::intValue));
        List<Long> progress = new ArrayList<>();
        PmemSearch.VectorMatches result = PmemSearch.topKCosineFloatLE(region, offset, vectors.length, query, k, progress::add);
        check(result.size() == Math.min(k, vectors.length), "result size");
        for (int i = 0; i < result.size(); i++) {
            check(result.row(i) == expected[i], "full-sort row oracle " + i);
            check(Double.doubleToLongBits(result.score(i)) == Double.doubleToLongBits(scores[expected[i]]),
                    "full-sort score oracle " + i);
        }
        check(progress.getFirst() == 0 && progress.getLast() == vectors.length, "progress endpoints");
        for (int i = 1; i < progress.size(); i++) check(progress.get(i) > progress.get(i - 1), "monotonic progress");
    }
    private static void randomAndBoundary(PmemBackend backend) throws Exception {
        Random random = new Random(0x504d454d41494cL);
        for (int[] shape : new int[][] {{537, 37}, {3, 16_387}}) {
            int count = shape[0], dimensions = shape[1];
            float[][] vectors = new float[count][dimensions];
            float[] query = new float[dimensions];
            for (int d = 0; d < dimensions; d++) query[d] = random.nextFloat() * 2 - 1;
            for (float[] row : vectors) for (int d = 0; d < dimensions; d++) row[d] = random.nextFloat() * 2 - 1;
            vectors[1] = vectors[0].clone(); // deterministic equal-score row-order tie
            byte[] bytes = encode(vectors);
            Path path = Files.createTempFile("pmem-vectors-", ".bin");
            try {
                Files.write(path, new byte[bytes.length + 9]);
                try (PmemRegion region = PmemSdk.open(path, 0, bytes.length + 9L, PmemAccess.READ_WRITE, backend)) {
                    region.write(3, bytes, 0, bytes.length);
                    for (int k : new int[] {1, 2, 17, count + 1}) verify(region, 3, vectors, query, k);
                    float[] mutable = query.clone();
                    var before = PmemSearch.topKCosineFloatLE(region, 3, count, mutable, 3);
                    var after = PmemSearch.topKCosineFloatLE(region, 3, count, mutable, 3,
                            done -> Arrays.fill(mutable, 0));
                    for (int i = 0; i < before.size(); i++) check(before.row(i) == after.row(i), "query snapshot");
                    check(PmemSearch.topKCosineFloatLE(region, region.length(), 0, query, 1).size() == 0, "empty run");
                }
            } finally { Files.deleteIfExists(path); }
        }
    }
    private static void refusals(PmemBackend backend) throws Exception {
        Path path = Files.createTempFile("pmem-vector-refusal-", ".bin");
        Files.write(path, encode(new float[][] {{1, 0}, {0, 1}, {-1, 0}}));
        PmemRegion region = PmemSdk.open(path, 0, 24, PmemAccess.READ_WRITE, backend);
        try {
            float[] query = {1, 0};
            rejects(IllegalArgumentException.class, () -> PmemSearch.topKCosineFloatLE(region, 0, -1, query, 1));
            rejects(IllegalArgumentException.class, () -> PmemSearch.topKCosineFloatLE(region, 0, Long.MAX_VALUE, query, 1));
            rejects(IndexOutOfBoundsException.class, () -> PmemSearch.topKCosineFloatLE(region, -1, 1, query, 1));
            rejects(IndexOutOfBoundsException.class, () -> PmemSearch.topKCosineFloatLE(region, 0, 4, query, 1));
            rejects(IllegalArgumentException.class, () -> PmemSearch.topKCosineFloatLE(region, 0, 1, query, 0));
            rejects(IllegalArgumentException.class, () -> PmemSearch.topKCosineFloatLE(region, 0, 1, query, PmemSearch.MAX_VECTOR_RESULTS + 1));
            rejects(IllegalArgumentException.class, () -> PmemSearch.topKCosineFloatLE(region, 0, 1, new float[0], 1));
            rejects(IllegalArgumentException.class, () -> PmemSearch.topKCosineFloatLE(region, 0, 1, new float[] {0, -0f}, 1));
            rejects(IllegalArgumentException.class, () -> PmemSearch.topKCosineFloatLE(region, 0, 1, new float[] {Float.NaN, 1}, 1));
            rejects(IllegalArgumentException.class, () -> PmemSearch.topKCosineFloatLE(region, 0, 1, new float[] {Float.POSITIVE_INFINITY, 1}, 1));
            rejects(NullPointerException.class, () -> PmemSearch.topKCosineFloatLE(region, 0, 1, query, 1, null));
            byte[] corrupt = encode(new float[][] {{Float.NaN, 1}});
            region.write(16, corrupt, 0, corrupt.length);
            rejects(IllegalArgumentException.class, () -> PmemSearch.topKCosineFloatLE(region, 0, 3, query, 1));
            region.write(16, new byte[8], 0, 8);
            rejects(IllegalArgumentException.class, () -> PmemSearch.topKCosineFloatLE(region, 0, 3, query, 1));
            rejects(CancellationException.class, () -> PmemSearch.topKCosineFloatLE(region, 0, 1, query, 1,
                    done -> { throw new CancellationException("caller cancelled"); }));
            try {
                rejects(CancellationException.class, () -> PmemSearch.topKCosineFloatLE(region, 24, 0, query, 1,
                        done -> Thread.currentThread().interrupt()));
                check(Thread.currentThread().isInterrupted(), "empty-run callback interrupt preserved");
            } finally { Thread.interrupted(); }
            Thread.currentThread().interrupt();
            try {
                rejects(CancellationException.class, () -> PmemSearch.topKCosineFloatLE(region, 0, 1, query, 1));
                check(Thread.currentThread().isInterrupted(), "interrupt preserved");
            } finally { Thread.interrupted(); }
            byte[] extremes = encode(new float[][] {{Float.MAX_VALUE, Float.MIN_VALUE}, {Float.MIN_VALUE, Float.MIN_VALUE}});
            region.write(0, extremes, 0, extremes.length);
            verify(region, 0, new float[][] {{Float.MAX_VALUE, Float.MIN_VALUE}, {Float.MIN_VALUE, Float.MIN_VALUE}},
                    new float[] {Float.MAX_VALUE, Float.MIN_VALUE}, 2);
        } finally { region.close(); Files.deleteIfExists(path); }
        rejects(IllegalStateException.class, () -> PmemSearch.topKCosineFloatLE(region, 0, 1, new float[] {1, 0}, 1));
    }
    private static void sparseReadOnly(PmemBackend backend) throws Exception {
        long origin = (1L << 31) + 11;
        float[][] vectors = {{1, 0}, {0, 1}, {1, 1}};
        Path path = Files.createTempFile("pmem-vector-sparse-", ".bin");
        try {
            try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "rw")) {
                file.setLength(origin + 24); file.seek(origin); file.write(encode(vectors));
            }
            try (PmemRegion region = PmemSdk.open(path, 0, origin + 24, PmemAccess.READ_ONLY, backend)) {
                verify(region, origin, vectors, new float[] {1, 1}, 3);
            }
        } finally { Files.deleteIfExists(path); }
    }
    private static void closeLease(PmemBackend backend) throws Exception {
        Path path = Files.createTempFile("pmem-vector-lease-", ".bin");
        Files.write(path, encode(new float[][] {{1, 0}, {0, 1}}));
        PmemRegion region = PmemSdk.open(path, 0, 16, PmemAccess.READ_ONLY, backend);
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        CountDownLatch closeStarted = new CountDownLatch(1), closed = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread scan = new Thread(() -> {
            try { PmemSearch.topKCosineFloatLE(region, 0, 2, new float[] {1, 0}, 1, done -> {
                if (done == 0) { entered.countDown();
                    try { if (!release.await(10, TimeUnit.SECONDS)) throw new AssertionError("release timeout"); }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
                }
            }); } catch (Throwable e) { failure.set(e); }
        }, "pmem-vector-scan");
        Thread closer = new Thread(() -> {
            closeStarted.countDown();
            try { region.close(); } catch (Throwable e) { failure.set(e); }
            finally { closed.countDown(); }
        }, "pmem-vector-close");
        scan.setDaemon(true); closer.setDaemon(true);
        try {
            scan.start(); check(entered.await(10, TimeUnit.SECONDS), "read lease entered");
            closer.start(); check(closeStarted.await(10, TimeUnit.SECONDS), "closer started");
            check(!closed.await(100, TimeUnit.MILLISECONDS), "close waits for full scan read lease");
        } finally {
            release.countDown(); scan.join(10_000); closer.join(10_000);
            if (scan.isAlive() || closer.isAlive()) throw new AssertionError("bounded close completion failed");
            region.close(); Files.deleteIfExists(path);
        }
        check(!scan.isAlive() && !closer.isAlive(), "bounded close completion");
        check(failure.get() == null, "no unsafe access after close: " + failure.get());
    }
    public static void run(PmemBackend backend) throws Exception {
        int before = assertions;
        randomAndBoundary(backend); refusals(backend); sparseReadOnly(backend); closeLease(backend);
        System.out.println("PASS exact vector retrieval " + backend + ": " + (assertions - before) + " assertions");
    }
    public static void main(String[] args) throws Exception {
        run(PmemBackend.FILE); run(PmemBackend.POSIX);
    }
}
