/*
 * @test
 * @summary Composite rope metadata, long indexing and zero-copy shared slices
 * @modules java.base/jdk.internal.m3
 * @run main/othervm M3CompositeBytesTest
 */

import jdk.internal.m3.M3ByteSlices;
import jdk.internal.m3.M3CompositeBytes;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class M3CompositeBytesTest {
    public static void main(String[] args) {
        verifyCompositesAndLeases();
        verifyVeryLargeLogicalSequence();
        System.out.println("M3CompositeBytesTest PASS");
    }

    private static void verifyCompositesAndLeases() {
        M3ByteSlices pool = new M3ByteSlices();
        long hello = pool.intern("hello".getBytes(StandardCharsets.UTF_8));
        long space = pool.intern(" ".getBytes(StandardCharsets.UTF_8));
        long world = pool.intern("world".getBytes(StandardCharsets.UTF_8));
        try (M3CompositeBytes a = M3CompositeBytes.of(pool, hello, space);
             M3CompositeBytes b = M3CompositeBytes.of(pool, world);
             M3CompositeBytes joined = a.concat(b);
             M3CompositeBytes part = joined.slice(3, 6)) {
            require(joined.length() == 11 && joined.pieceCount() == 3, "joined descriptor count");
            require(joined.byteAt(0) == 'h' && joined.byteAt(10) == 'd', "boundary byteAt");
            require(Arrays.equals(part.materialize(), "lo wor".getBytes(StandardCharsets.UTF_8)),
                    "cross-piece slice");
            require(Arrays.equals(joined.materialize(), "hello world".getBytes(StandardCharsets.UTF_8)),
                    "explicit materialization");
            require(pool.ownerCount(hello) == 3, "atom shared by 3 composites");
            try (M3CompositeBytes empty = joined.slice(11, 0)) {
                require(empty.length() == 0 && empty.materialize().length == 0, "empty slice");
            }
            expect(IndexOutOfBoundsException.class, () -> joined.byteAt(11));
        }
        require(pool.ownerCount(hello) == 0 && pool.ownerCount(world) == 0,
                "explicit leases balanced after close");
        M3CompositeBytes closed = M3CompositeBytes.of(pool, hello);
        closed.close();
        closed.close();
        expect(IllegalStateException.class, () -> closed.byteAt(0));
    }

    private static void verifyVeryLargeLogicalSequence() {
        M3ByteSlices pool = new M3ByteSlices();
        byte[] atom = new byte[1 << 20];
        Arrays.fill(atom, (byte) 41);
        long canonical = pool.intern(atom);
        long[] pieces = new long[3072];
        Arrays.fill(pieces, canonical);
        try (M3CompositeBytes big = M3CompositeBytes.of(pool, pieces)) {
            require(big.length() == 3L * 1024 * 1024 * 1024, "3 GiB composite length");
            require(big.byteAt(3L * 1024 * 1024 * 1024 - 1) == 41,
                    "long-indexed byte access without materialization");
            expect(IllegalStateException.class, big::materialize);
            try (M3CompositeBytes tail = big.slice(big.length() - 2, 2)) {
                require(tail.length() == 2 && tail.byteAt(1) == 41, "long offset slice");
            }
        }
        require(pool.ownerCount(canonical) == 0, "all repeated leases balanced");
    }

    private static void expect(Class<? extends Throwable> type, Runnable operation) {
        try {
            operation.run();
            throw new AssertionError("Expected " + type.getSimpleName());
        } catch (Throwable exception) {
            if (!type.isInstance(exception)) {
                throw new AssertionError(exception);
            }
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
