/*
 * Copyright (c) 2026, Hitesh Soliwal.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 *
 * @test
 * @summary Independent exact-byte oracle for composite and bounded-stream literals
 * @modules java.base/jdk.internal.m3 java.management
 * @run main/othervm -ea -esa M3CompositeLiteralChecks
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import jdk.internal.m3.M3ByteSlices;
import jdk.internal.m3.M3CompositeBytes;
import jdk.internal.m3.M3CompositeBytes.LiteralMatches;
import jdk.internal.m3.M3CompositeBytes.PreparedLiteral;
import jdk.internal.m3.M3CompositeBytes.StreamMatches;

public final class M3CompositeLiteralChecks {
    private static long checks;

    public static void main(String[] args) throws Exception {
        exhaustiveCutsAndRanges();
        randomizedDescriptors();
        ownershipAndBounds();
        longCoordinates();
        streamContracts();
        cancellationAndResumption();
        System.out.println("M3CompositeLiteralChecks PASS checks=" + checks);
    }

    private static void exhaustiveCutsAndRanges() throws Exception {
        List<byte[]> needles = new ArrayList<>();
        for (int n = 0; n <= 3; n++) {
            for (int bits = 0; bits < 1 << n; bits++) needles.add(binary(n, bits));
        }
        for (int n = 0; n <= 5; n++) {
            for (int bits = 0; bits < 1 << n; bits++) {
                byte[] text = binary(n, bits);
                M3ByteSlices pool = new M3ByteSlices();
                long empty = pool.intern(new byte[0]);
                for (int cut = 0; cut <= n; cut++) {
                    long left = pool.intern(Arrays.copyOfRange(text, 0, cut));
                    long right = pool.intern(Arrays.copyOfRange(text, cut, n));
                    try (M3CompositeBytes value = M3CompositeBytes.of(pool,
                            empty, left, empty, right, empty)) {
                        for (byte[] needle : needles) {
                            PreparedLiteral plan = M3CompositeBytes.prepareLiteral(needle);
                            for (int from = 0; from <= n; from++) {
                                for (int to = from; to <= n; to++) {
                                    verify(value, text, plan, needle, from, to);
                                }
                            }
                        }
                    }
                }
                // Every positive stream chunk size moves the invisible read seam.
                for (int chunk = 1; chunk <= n + 1; chunk++) {
                    for (byte[] needle : needles) {
                        PreparedLiteral plan = M3CompositeBytes.prepareLiteral(needle);
                        for (boolean overlap : new boolean[] {false, true}) {
                            TrackingInput input = new TrackingInput(text, chunk);
                            StreamMatches cursor = plan.streamMatches(input, 0, n + 4L, overlap);
                            verifyStream(cursor, oracle(text, needle, 0, n, overlap), 0);
                            require(input.closed == 0, "stream ownership");
                            require(input.maximumRequest <= 65536, "bounded stream scratch request");
                        }
                    }
                }
            }
        }
    }

    private static void randomizedDescriptors() {
        Random random = new Random(0x4d33524f50454cL);
        for (int round = 0; round < 180; round++) {
            byte[] text = new byte[random.nextInt(180)];
            random.nextBytes(text);
            if ((round & 1) == 0) {
                for (int i = 0; i < text.length; i++) text[i] &= 3;
            }
            M3ByteSlices pool = new M3ByteSlices();
            byte[] padded = new byte[text.length + 6];
            System.arraycopy(text, 0, padded, 3, text.length);
            long root = pool.intern(padded);
            long middle = pool.slice(root, 1, padded.length - 2);
            long nested = pool.slice(middle, 2, text.length);
            List<Long> handles = new ArrayList<>();
            handles.add(pool.intern(new byte[0]));
            for (int at = 0; at < text.length;) {
                int count = Math.min(text.length - at, 1 + random.nextInt(11));
                handles.add(pool.slice(nested, at, count));
                handles.add(pool.slice(nested, at, 0));
                at += count;
            }
            long[] pieces = handles.stream().mapToLong(Long::longValue).toArray();
            try (M3CompositeBytes raw = M3CompositeBytes.of(pool, pieces);
                 M3CompositeBytes clipped = raw.slice(0, raw.length());
                 M3CompositeBytes value = clipped.slice(0, clipped.length())) {
                for (int test = 0; test < 24; test++) {
                    int from = random.nextInt(text.length + 1);
                    int to = from + random.nextInt(text.length - from + 1);
                    byte[] needle;
                    if ((test & 1) == 0) {
                        int at = random.nextInt(text.length + 1);
                        int end = Math.min(text.length, at + random.nextInt(15));
                        needle = Arrays.copyOfRange(text, at, end);
                    } else {
                        needle = new byte[random.nextInt(15)];
                        random.nextBytes(needle);
                    }
                    verify(value, text, M3CompositeBytes.prepareLiteral(needle), needle, from, to);
                }
            }
        }
        // Repeated aliases carry occurrence coordinates, not content identities.
        M3ByteSlices pool = new M3ByteSlices();
        long atom = pool.intern(new byte[] {7, 8, 7});
        long empty = pool.intern(new byte[0]);
        try (M3CompositeBytes source = M3CompositeBytes.of(pool, atom, empty, atom, atom);
             M3CompositeBytes part = source.slice(1, 7);
             M3CompositeBytes nested = part.slice(1, 5)) {
            byte[] actual = {7, 7, 8, 7, 7};
            for (byte[] needle : List.of(new byte[0], new byte[] {7},
                    new byte[] {7, 7}, new byte[] {7, 8, 7})) {
                verify(nested, actual, M3CompositeBytes.prepareLiteral(needle), needle, 0, 5);
            }
        }
    }

    private static void ownershipAndBounds() {
        M3ByteSlices pool = new M3ByteSlices();
        byte[] input = {(byte) 0xf0, (byte) 0x80, 0, (byte) 0xff, (byte) 0x80};
        long atom = pool.intern(input);
        byte[] needle = {(byte) 0x80};
        PreparedLiteral plan = M3CompositeBytes.prepareLiteral(needle);
        needle[0] = 1;
        require(plan.byteLength() == 1, "immutable plan length");
        M3CompositeBytes value = M3CompositeBytes.of(pool, atom);
        LiteralMatches exhausted = value.matches(plan, 5, 5, true);
        require(exhausted.next() == -1, "empty nonempty-pattern range");
        require(value.indexOf(plan, 0) == 1, "raw bytes, no UTF-8 decoding");
        require(value.matches(plan, 0, true).next() == 1, "convenience cursor");
        expect(NullPointerException.class, () -> value.indexOf(null, 0));
        expect(IndexOutOfBoundsException.class, () -> value.indexOf(plan, -1));
        expect(IndexOutOfBoundsException.class, () -> value.matches(plan, 3, 2, true));
        expect(IndexOutOfBoundsException.class, () -> value.matches(plan, 0, 6, true));
        expect(IndexOutOfBoundsException.class, () -> value.matches(plan, Long.MAX_VALUE, true));
        try (M3CompositeBytes slice = value.slice(1, 4)) {
            LiteralMatches active = value.matches(plan, 0, true);
            value.close();
            expect(IllegalStateException.class, active::next);
            expect(IllegalStateException.class, exhausted::next);
            expect(IllegalStateException.class, () -> value.indexOf(null, -1, -1));
            expect(IllegalStateException.class, () -> value.matches(plan, 0, false));
            require(slice.indexOf(plan, 0) == 0, "slice retains independent leases");
        }
        require(pool.ownerCount(atom) == 0, "leases balanced");
        expect(NullPointerException.class, () -> M3CompositeBytes.prepareLiteral(null));
        expect(IllegalArgumentException.class,
                () -> M3CompositeBytes.prepareLiteral(new byte[M3CompositeBytes.MAX_LITERAL_BYTES + 1]));
        byte[] maximum = new byte[M3CompositeBytes.MAX_LITERAL_BYTES];
        require(M3CompositeBytes.prepareLiteral(maximum).byteLength() == maximum.length,
                "inclusive preparation budget");
    }

    private static void longCoordinates() throws Exception {
        M3ByteSlices pool = new M3ByteSlices();
        byte[] bytes = new byte[1 << 20];
        bytes[0] = 9;
        bytes[bytes.length - 1] = 7;
        long atom = pool.intern(bytes);
        long[] descriptors = new long[3072];
        Arrays.fill(descriptors, atom);
        PreparedLiteral seam = M3CompositeBytes.prepareLiteral(new byte[] {7, 9});
        PreparedLiteral empty = M3CompositeBytes.prepareLiteral(new byte[0]);
        try (M3CompositeBytes big = M3CompositeBytes.of(pool, descriptors)) {
            long at = 1L << 31;
            require(big.length() == 3L << 30, "long logical length");
            require(big.indexOf(seam, at - 2, at + 2) == at - 1, "seam above int maximum");
            require(big.indexOf(seam, at, at + 8) == -1, "strict long start");
            LiteralMatches hits = big.matches(seam, at - 1, at + (1 << 20) + 1L, true);
            require(hits.next() == at - 1 && hits.next() == at + (1 << 20) - 1L
                    && hits.next() == -1, "stateful long piece traversal");
            LiteralMatches terminal = big.matches(empty, big.length(), true);
            require(terminal.next() == big.length() && terminal.next() == -1,
                    "empty terminal boundary exactly once");
            try (M3CompositeBytes tail = big.slice(at - 1, 4)) {
                require(tail.indexOf(seam, 0) == 0, "clipped long descriptor offset");
            }
        }
        require(pool.ownerCount(atom) == 0, "repeated long leases released");
        byte[] data = {1, 2, 1, 2, 1};
        PreparedLiteral pair = M3CompositeBytes.prepareLiteral(new byte[] {1, 2, 1});
        for (long start : new long[] {0, (1L << 31) + 13, Long.MAX_VALUE - data.length}) {
            TrackingInput source = new TrackingInput(data, 1);
            verifyStream(pair.streamMatches(source, start, start + data.length, true),
                    new long[] {0, 2}, start);
            verifyStream(empty.streamMatches(new TrackingInput(data, 2), start,
                    start + data.length, false), new long[] {0, 1, 2, 3, 4, 5}, start);
        }
        TrackingInput unused = new TrackingInput(new byte[] {1}, 1);
        verifyStream(empty.streamMatches(unused, Long.MAX_VALUE, Long.MAX_VALUE, true),
                new long[] {0}, Long.MAX_VALUE);
        require(unused.reads == 0, "terminal boundary does not overflow or read");
    }

    private static void streamContracts() throws Exception {
        PreparedLiteral plan = M3CompositeBytes.prepareLiteral(new byte[] {1, 2, 1});
        byte[] text = {1, 2, 1, 2, 1, 9, 9};
        TrackingInput source = new TrackingInput(text, 65536);
        StreamMatches cursor = plan.streamMatches(source, 50, 55, true);
        require(cursor.next() == 50 && source.position == 5, "bounded read-ahead retained");
        require(cursor.next() == 52 && cursor.next() == -1, "buffer continues after first hit");
        require(source.position == 5 && source.closed == 0, "range end and caller ownership");
        expect(NullPointerException.class, () -> plan.streamMatches(null, 0, 0, true));
        expect(IndexOutOfBoundsException.class, () -> plan.streamMatches(source, -1, 1, true));
        expect(IndexOutOfBoundsException.class, () -> plan.streamMatches(source, 2, 1, true));
        TrackingInput tooShort = new TrackingInput(text, 1);
        require(plan.streamMatches(tooShort, 0, 2, false).next() == -1 && tooShort.reads == 0,
                "known bounded range cannot fit");
        StreamMatches zero = plan.streamMatches(new InputStream() {
            @Override public int read() { throw new AssertionError("no single-byte fallback"); }
            @Override public int read(byte[] b, int off, int len) { return 0; }
        }, 0, Long.MAX_VALUE, true);
        expect(IOException.class, zero::next);
        IOException failure = new IOException("source failure");
        StreamMatches broken = plan.streamMatches(new InputStream() {
            @Override public int read() throws IOException { throw failure; }
            @Override public int read(byte[] b, int off, int len) throws IOException { throw failure; }
        }, 0, Long.MAX_VALUE, true);
        try {
            broken.next();
            throw new AssertionError("I/O exception not propagated");
        } catch (IOException observed) {
            require(observed == failure, "original I/O exception identity");
        }
        byte[] large = new byte[2 * 65536 + 20];
        byte[] needle = new byte[80];
        Arrays.fill(needle, (byte) 4);
        needle[needle.length - 1] = 5;
        for (int cut = 1; cut < needle.length; cut++) {
            Arrays.fill(large, (byte) 0);
            int at = 65536 - cut;
            System.arraycopy(needle, 0, large, at, needle.length);
            TrackingInput fullReads = new TrackingInput(large, Integer.MAX_VALUE);
            StreamMatches hits = M3CompositeBytes.prepareLiteral(needle)
                    .streamMatches(fullReads, 0, Long.MAX_VALUE, true);
            require(hits.next() == at && hits.next() == -1, "every 64KiB literal cut");
            require(fullReads.maximumRequest == 65536 && fullReads.closed == 0,
                    "large input keeps bounded requests");
        }
    }

    private static void cancellationAndResumption() throws Exception {
        PreparedLiteral plan = M3CompositeBytes.prepareLiteral(new byte[] {1, 2, 1});
        M3ByteSlices pool = new M3ByteSlices();
        long atom = pool.intern(new byte[] {1, 2, 1, 2, 1});
        try (M3CompositeBytes source = M3CompositeBytes.of(pool, atom)) {
            LiteralMatches cursor = source.matches(plan, 0, true);
            require(cursor.next() == 0, "first overlap before cancellation");
            Thread.currentThread().interrupt();
            try {
                expect(CancellationException.class, cursor::next);
                expect(CancellationException.class, () -> source.indexOf(plan, 0));
                expect(CancellationException.class, () -> source.matches(plan, 0, true));
                expect(CancellationException.class, () -> M3CompositeBytes.prepareLiteral(new byte[0]));
                expect(CancellationException.class,
                        () -> plan.streamMatches(new TrackingInput(new byte[0], 1), 0, 0, true));
                require(Thread.currentThread().isInterrupted(), "entry polling preserves flag");
            } finally {
                Thread.interrupted();
            }
            require(cursor.next() == 2 && cursor.next() == -1, "resumed overlap frontier");
            Thread.currentThread().interrupt();
            try {
                expect(CancellationException.class, cursor::next);
                require(Thread.currentThread().isInterrupted(), "exhausted cursor checks flag");
            } finally {
                Thread.interrupted();
            }
        }
        // The second bulk read interrupts after transfer. The cursor must retain
        // those bytes and the prefix carried from the first positive short read.
        TrackingInput interruptedRead = new TrackingInput(new byte[] {1, 2, 1, 2, 1}, 2) {
            @Override public int read(byte[] b, int off, int len) throws IOException {
                int count = super.read(b, off, len);
                if (reads == 2) Thread.currentThread().interrupt();
                return count;
            }
        };
        StreamMatches resumed = plan.streamMatches(interruptedRead, 0, 5, true);
        try {
            expect(CancellationException.class, resumed::next);
            require(Thread.currentThread().isInterrupted(), "read-return flag preserved");
        } finally {
            Thread.interrupted();
        }
        verifyStream(resumed, new long[] {0, 2}, 0);
        TrackingInput partial = new TrackingInput(new byte[] {1, 2, 1, 2, 1}, 2) {
            @Override public int read(byte[] b, int off, int len) throws IOException {
                int count = super.read(b, off, len);
                if (reads == 1) {
                    InterruptedIOException problem = new InterruptedIOException("partial transfer");
                    problem.bytesTransferred = count;
                    throw problem;
                }
                return count;
            }
        };
        StreamMatches partialCursor = plan.streamMatches(partial, 0, 5, true);
        try {
            expect(InterruptedIOException.class, partialCursor::next);
            require(Thread.currentThread().isInterrupted(), "I/O interruption restores flag");
        } finally {
            Thread.interrupted();
        }
        verifyStream(partialCursor, new long[] {0, 2}, 0);
        midScanCancellation();
    }

    private static void midScanCancellation() throws Exception {
        M3ByteSlices pool = new M3ByteSlices();
        long zeros = pool.intern(new byte[65536]);
        long sentinel = pool.intern(new byte[] {1});
        long[] handles = new long[257];
        Arrays.fill(handles, zeros);
        handles[256] = sentinel;
        PreparedLiteral one = M3CompositeBytes.prepareLiteral(new byte[] {1});
        try (M3CompositeBytes source = M3CompositeBytes.of(pool, handles)) {
            LiteralMatches cursor = source.matches(one, 0, true);
            CountDownLatch ready = new CountDownLatch(1);
            AtomicReference<Throwable> failure = new AtomicReference<>();
            Thread worker = new Thread(() -> {
                try {
                    ready.countDown();
                    try {
                        cursor.next();
                        throw new AssertionError("scan completed before cancellation witness");
                    } catch (CancellationException expected) {
                        if (!Thread.currentThread().isInterrupted()) throw new AssertionError("lost flag");
                    }
                    Thread.interrupted();
                    if (cursor.next() != 16L * 1024 * 1024 || cursor.next() != -1) {
                        throw new AssertionError("mid-scan resume lost logical position");
                    }
                } catch (Throwable problem) {
                    failure.set(problem);
                }
            }, "m3-literal-cancellation-oracle");
            ThreadMXBean bean = ManagementFactory.getThreadMXBean();
            // Hold the existing pool monitor until next() has passed its entry
            // check and blocks while borrowing a piece. This deterministic
            // handoff does not assume that another thread beats a fast scan.
            synchronized (pool) {
                worker.start();
                ready.await();
                long deadline = System.nanoTime() + 30_000_000_000L;
                boolean borrowing = false;
                while (worker.isAlive() && System.nanoTime() < deadline) {
                    ThreadInfo info = bean.getThreadInfo(worker.threadId());
                    if (info != null && info.getThreadState() == Thread.State.BLOCKED
                            && info.getLockInfo() != null
                            && info.getLockInfo().getIdentityHashCode() == System.identityHashCode(pool)) {
                        borrowing = true;
                        break;
                    }
                    Thread.onSpinWait();
                }
                worker.interrupt();
                require(borrowing, "active scan blocked at the pool borrowing boundary");
            }
            worker.join(30_000);
            require(!worker.isAlive(), "bounded cancellation/resume completion");
            if (failure.get() != null) throw new AssertionError(failure.get());
            checks++;
        }
    }

    private static void verify(M3CompositeBytes value, byte[] text, PreparedLiteral plan,
                               byte[] needle, int from, int to) {
        long[] overlapping = oracle(text, needle, from, to, true);
        require(value.indexOf(plan, from, to) == (overlapping.length == 0 ? -1 : overlapping[0]),
                "first-hit independent oracle");
        for (boolean overlap : new boolean[] {false, true}) {
            long[] expected = overlap ? overlapping : oracle(text, needle, from, to, false);
            LiteralMatches cursor = value.matches(plan, from, to, overlap);
            for (long hit : expected) require(cursor.next() == hit, "ordered occurrence oracle");
            require(cursor.next() == -1 && cursor.next() == -1, "stable exhaustion");
        }
    }

    private static void verifyStream(StreamMatches cursor, long[] expected, long base) throws IOException {
        for (long hit : expected) require(cursor.next() == base + hit, "stream occurrence oracle");
        require(cursor.next() == -1 && cursor.next() == -1, "stream exhaustion");
    }

    /** Independent brute-force matching, deliberately no KMP table or recurrence. */
    private static long[] oracle(byte[] text, byte[] needle, int from, int to, boolean overlap) {
        long[] hits = new long[to - from + 1];
        int count = 0;
        for (int at = from; at <= to - needle.length;) {
            int compared = 0;
            while (compared < needle.length && text[at + compared] == needle[compared]) compared++;
            if (compared == needle.length) {
                hits[count++] = at;
                at += overlap || needle.length == 0 ? 1 : needle.length;
            } else {
                at++;
            }
        }
        return Arrays.copyOf(hits, count);
    }

    private static byte[] binary(int n, int bits) {
        byte[] result = new byte[n];
        for (int i = 0; i < n; i++) result[i] = (byte) (((bits >>> i) & 1) == 0 ? 0 : 255);
        return result;
    }

    private static class TrackingInput extends InputStream {
        private final byte[] bytes;
        private final int chunk;
        int position;
        int reads;
        int maximumRequest;
        int closed;

        TrackingInput(byte[] bytes, int chunk) {
            this.bytes = bytes;
            this.chunk = chunk;
        }

        @Override public int read() { throw new AssertionError("expected bounded bulk read"); }

        @Override public int read(byte[] b, int off, int len) throws IOException {
            if (len <= 0) throw new AssertionError("nonpositive read request");
            reads++;
            maximumRequest = Math.max(maximumRequest, len);
            if (position == bytes.length) return -1;
            int count = Math.min(Math.min(chunk, len), bytes.length - position);
            System.arraycopy(bytes, position, b, off, count);
            position += count;
            return count;
        }

        @Override public void close() { closed++; }
    }

    @FunctionalInterface
    private interface Throwing {
        void run() throws Exception;
    }

    private static void expect(Class<? extends Throwable> kind, Throwing action) {
        try {
            action.run();
        } catch (Throwable failure) {
            require(kind.isInstance(failure), "expected " + kind.getName() + ", got " + failure);
            return;
        }
        throw new AssertionError("missing " + kind.getName());
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
}
