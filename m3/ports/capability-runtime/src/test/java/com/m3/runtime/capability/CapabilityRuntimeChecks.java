// SPDX-License-Identifier: Apache-2.0
package com.m3.runtime.capability;

import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Standalone JDK21 oracle, independent of an external model/tool/JNI dependency. */
public final class CapabilityRuntimeChecks {
    private static final java.util.concurrent.atomic.AtomicLong assertions =
            new java.util.concurrent.atomic.AtomicLong();
    private CapabilityRuntimeChecks() { }

    private static void check(boolean value, String why) {
        assertions.incrementAndGet();
        if (!value) throw new AssertionError(why);
    }
    private static void equal(long expected, long actual, String why) {
        check(expected == actual, why + ": " + expected + " != " + actual);
    }
    private static void refuse(Class<? extends Throwable> type, Runnable call) {
        assertions.incrementAndGet();
        try { call.run(); }
        catch (Throwable thrown) {
            if (type.isInstance(thrown)) return;
            throw new AssertionError("wrong failure " + thrown, thrown);
        }
        throw new AssertionError("expected refusal " + type.getSimpleName());
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 0 && args.length != 2) throw new IllegalArgumentException("[native-path sha256]");
        boolean nativeEnabled = args.length == 2;
        if (nativeEnabled) LfCount.install(Path.of(args[0]), args[1]);
        byte[] original = "line1\r\nline2\n🚀\n".getBytes(StandardCharsets.UTF_8);
        SourceArtifact artifact = SourceArtifact.copyOf("source:1", original);
        SourceArtifact.Span root = artifact.whole();
        String initialSha = root.sha256();
        Arrays.fill(original, (byte) 0x55);
        check(artifact.root().equals(initialSha), "source identity must be SHA256 of snapshotted bytes");
        check(root.sha256().equals(initialSha), "whole range identity equals source root");
        equal(3, count(root.copy()), "all LF count");
        var first = root.slice(0, 7);
        var second = root.slice(7, root.length() - 7);
        equal(1, count(first.copy()), "first range count");
        equal(2, count(second.copy()), "second range count");
        check(first.sha256().equals(SourceArtifact.copyOf("first", first.copy()).root()),
                "byte range identity independent from source alias");
        byte[] exported = first.copy();
        exported[0] = 0;
        check(first.byteAt(0) != 0, "materialized compatibility copy cannot mutate source");
        refuse(IndexOutOfBoundsException.class, () -> root.slice(10, root.length()));
        refuse(IndexOutOfBoundsException.class, () -> root.byteAt(root.length()));
        refuse(IllegalArgumentException.class, () -> SourceArtifact.copyOf("", new byte[0]));
        refuse(IllegalArgumentException.class, () -> SourceArtifact.copyOf("source\n", new byte[0]));
        refuse(IllegalArgumentException.class, () -> SourceArtifact.copyOf("x", new byte[SourceArtifact.MAX_BYTES + 1]));
        var zero = SourceArtifact.copyOf("empty", new byte[0]);
        check(zero.root().equals(zero.whole().sha256()), "empty source and span roots");

        WorkingSet set = new WorkingSet(root.length());
        try (WorkingSet.Lease lease = set.acquire(first)) {
            equal(first.length(), set.reservedBytes(), "reservation");
            equal(first.length(), lease.length(), "lease window");
            check(lease.sourceRoot().equals(artifact.root()), "pinned source root");
            check(lease.rangeRoot().equals(first.sha256()), "pinned range root");
            equal(1, lease.countLf(), "leased LF count");
            check(lease.copy()[0] == first.byteAt(0), "lease materialization");
            try (WorkingSet.Lease other = set.acquire(second)) {
                equal(root.length(), set.reservedBytes(), "exact full-budget occupancy");
                refuse(IllegalStateException.class, () -> set.acquire(first));
                equal(2, other.countLf(), "leased tail LF count");
            }
            equal(first.length(), set.reservedBytes(), "returned tail reservation");
        }
        equal(0, set.reservedBytes(), "all leases released");
        WorkingSet.Lease reused = set.acquire(first);
        reused.close(); reused.close();
        equal(0, set.reservedBytes(), "idempotent close cannot underflow");
        refuse(IllegalStateException.class, reused::countLf);
        refuse(IllegalArgumentException.class, () -> new WorkingSet(-1));
        refuse(IllegalStateException.class, () -> new WorkingSet(2).acquire(root));
        try (WorkingSet.Lease empty = new WorkingSet(0).acquire(zero.whole())) {
            equal(0, empty.countLf(), "zero-budget empty lease");
        }

        Random random = new Random(918331L);
        for (int iteration = 0; iteration < 1800; iteration++) {
            byte[] source = new byte[random.nextInt(1536)];
            random.nextBytes(source);
            for (int i = 0; i < source.length; i += 13) source[i] = (byte) '\n';
            ByteBuffer heap = ByteBuffer.wrap(source);
            ByteBuffer direct = ByteBuffer.allocateDirect(source.length);
            direct.put(source).flip();
            ByteBuffer readOnly = direct.asReadOnlyBuffer();
            int start = source.length == 0 ? 0 : random.nextInt(source.length + 1);
            int length = random.nextInt(source.length - start + 1);
            long expected = 0;
            for (int i = start; i < start + length; i++) if (source[i] == (byte) '\n') expected++;
            equal(expected, LfCount.javaCount(heap, start, length), "JDK heap oracle " + iteration);
            equal(expected, LfCount.javaCount(direct, start, length), "JDK direct oracle " + iteration);
            equal(expected, LfCount.javaCount(readOnly, start, length), "JDK readonly oracle " + iteration);
            int beforePosition = direct.position();
            int beforeLimit = direct.limit();
            if (nativeEnabled) {
                equal(expected, LfCount.nativeCount(direct, start, length), "JNI direct parity " + iteration);
                equal(expected, LfCount.nativeCount(readOnly, start, length), "JNI readonly parity " + iteration);
                refuse(IllegalArgumentException.class, () -> LfCount.nativeCount(heap, start, length));
            } else {
                refuse(IllegalStateException.class, () -> LfCount.nativeCount(direct, start, length));
            }
            equal(beforePosition, direct.position(), "JNI/Java must not change position");
            equal(beforeLimit, direct.limit(), "JNI/Java must not change limit");
        }
        ByteBuffer direct = ByteBuffer.allocateDirect(6);
        direct.put(new byte[]{10, 0, 13, 10, 0, 10}).flip();
        direct.limit(4);
        equal(2, LfCount.javaCount(direct, 0, 4), "Java observes logical limit");
        if (nativeEnabled) equal(2, LfCount.nativeCount(direct, 0, 4), "JNI observes logical limit");
        refuse(IndexOutOfBoundsException.class, () -> LfCount.javaCount(direct, 0, 6));
        refuse(IndexOutOfBoundsException.class, () -> LfCount.nativeCount(direct, 0, 6));
        refuse(IndexOutOfBoundsException.class, () -> LfCount.javaCount(direct, -1, 1));
        refuse(IndexOutOfBoundsException.class, () -> LfCount.javaCount(direct, Integer.MAX_VALUE, 1));
        refuse(ReadOnlyBufferException.class, () -> direct.asReadOnlyBuffer().put((byte) 1));

        // Each worker has its own immutable span lease. Cancellation or one close
        // must not change another worker's coordinate or resident accounting.
        WorkingSet shared = new WorkingSet(24L * root.length());
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<?>[] jobs = new Future<?>[24];
            for (int i = 0; i < jobs.length; i++) {
                jobs[i] = executor.submit(() -> {
                    for (int j = 0; j < 150; j++) {
                        try (WorkingSet.Lease lease = shared.acquire(root)) {
                            equal(3, lease.countLf(), "concurrent independent lease");
                        }
                    }
                });
            }
            for (Future<?> job : jobs) job.get();
        }
        equal(0, shared.reservedBytes(), "virtual-thread source leases fully reclaimed");
        System.out.println("M3_CPU_CAPABILITY_PASS assertions=" + assertions.get()
                + " kernel=" + (nativeEnabled ? "JNI_DIRECT_C11" : "JAVA_BYTEBUFFER") );
    }

    private static long count(byte[] source) {
        long n = 0;
        for (byte value : source) if (value == (byte) '\n') n++;
        return n;
    }
}
