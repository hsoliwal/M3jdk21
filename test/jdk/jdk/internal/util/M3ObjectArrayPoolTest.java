/*
 * @test
 * @summary M3 Object[] reuse, memory-pressure epoch and thread confinement
 * @modules java.base/jdk.internal.util
 * @run main/othervm M3ObjectArrayPoolTest
 */
import jdk.internal.util.M3ObjectArrayPool;
import java.util.concurrent.atomic.AtomicReference;

public class M3ObjectArrayPoolTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        M3ObjectArrayPool.clearThreadCache();
        Object[] first;
        Object marker = new Object();
        try (var lease = M3ObjectArrayPool.acquire(16)) {
            first = lease.array();
            first[0] = marker;
        }
        try (var lease = M3ObjectArrayPool.acquire(16)) {
            check(lease.array() == first, "expected reuse");
            check(lease.array()[0] == null, "retained reference");
            lease.close();
            boolean closed = false;
            try { lease.array(); } catch (IllegalStateException expected) {
                closed = true;
            }
            check(closed, "closed lease accepted access");
        }
        Object[] occupiedFirst;
        Object[] occupiedSecond;
        try (var firstLease = M3ObjectArrayPool.acquire(32);
             var secondLease = M3ObjectArrayPool.acquire(32)) {
            occupiedFirst = firstLease.array();
            occupiedSecond = secondLease.array();
            occupiedFirst[0] = marker;
            occupiedSecond[0] = marker;
        }
        try (var lease = M3ObjectArrayPool.acquire(32)) {
            check(lease.array() == occupiedSecond || lease.array() == occupiedFirst,
                    "one exact-size cached slot must survive");
            check(lease.array()[0] == null, "cached slot retained a reference");
        }

        M3ObjectArrayPool.signalMemoryPressure();
        try (var lease = M3ObjectArrayPool.acquire(16)) {
            check(lease.array() != first, "pressure did not invalidate cache");
        }
        M3ObjectArrayPool.setCachingEnabled(false);
        Object[] disabled;
        try (var lease = M3ObjectArrayPool.acquire(16)) {
            disabled = lease.array();
        }
        try (var lease = M3ObjectArrayPool.acquire(16)) {
            check(lease.array() != disabled, "disabled cache reused array");
        }
        M3ObjectArrayPool.setCachingEnabled(true);
        try (var lease = M3ObjectArrayPool.acquire(0)) {
            check(lease.array().length == 0, "zero-length fallback");
            lease.close();
            lease.close();
        }
        boolean negative = false;
        try { M3ObjectArrayPool.acquire(-1); }
        catch (NegativeArraySizeException expected) { negative = true; }
        check(negative, "negative length was accepted");
        try (var lease = M3ObjectArrayPool.acquire(257)) {
            check(lease.array().length == 257, "fallback length");
        }
        AtomicReference<Throwable> failure = new AtomicReference<>();
        try (var lease = M3ObjectArrayPool.acquire(8)) {
            Thread thread = new Thread(() -> {
                try {
                    lease.array();
                    failure.set(new AssertionError("cross-thread array access was accepted"));
                } catch (Throwable expected) {
                    failure.set(expected);
                }
            });
            thread.start();
            thread.join();
            check(failure.get() instanceof IllegalStateException,
                    "cross-thread close was accepted");
        }
        M3ObjectArrayPool.clearThreadCache();
    }
}
