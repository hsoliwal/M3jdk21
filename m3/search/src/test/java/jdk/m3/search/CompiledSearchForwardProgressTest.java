// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/jdk/internal/m3/search/CompiledSearchForwardProgressTest.java (blob 980cfd41a27d) by M3_SEARCH_FORWARD v1.
package jdk.m3.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jdk.m3.collections.DefaultProgressMonitor;
import jdk.m3.collections.ProgressMonitor;
import jdk.m3.collections.ProgressState;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Guard reverse-search candidate progress without changing valid provider behavior. */
final class CompiledSearchForwardProgressTest {
    @Test
    void rejectsNonprogressingThirdPartyProviderInsteadOfRevisitingSameStart() {
        AtomicInteger probes = new AtomicInteger();
        CompiledSearch bad = new CompiledSearch() {
            @Override public int indexOf(byte[] bytes, int start) {
                probes.incrementAndGet();
                return 0;  // First probe is valid, second violates start >= 1.
            }
            @Override public int needleLength() { return 1; }
            @Override public String strategy() { return "nonprogressing-third-party"; }
        };
        var failure = assertThrows(IllegalStateException.class,
                () -> bad.lastIndexOf(new byte[] {1, 1, 1}, Integer.MAX_VALUE));
        assertEquals("compiled search returned an invalid byte offset", failure.getMessage());
        assertEquals(2, probes.get());

        probes.set(0);
        ByteBuffer direct = ByteBuffer.allocateDirect(6);
        direct.put(2, (byte) 1);
        direct.put(3, (byte) 1);
        direct.position(2);
        direct.limit(4);
        assertThrows(IllegalStateException.class,
                () -> bad.lastIndexOf(direct, Integer.MAX_VALUE));
        assertEquals(2, probes.get());
        assertEquals(2, direct.position());
        assertEquals(4, direct.limit());
    }

    @Test
    void retainsExactLastOccurrenceAndOverlappingMatchesForValidProviders() {
        byte[] text = {1, 2, 1, 2, 1};
        LinearCompiled provider = new LinearCompiled(new byte[] {1, 2, 1});
        assertEquals(-1, provider.lastIndexOf(text, -1));
        assertEquals(0, provider.lastIndexOf(text, 0));
        assertEquals(0, provider.lastIndexOf(text, 1));
        assertEquals(2, provider.lastIndexOf(text, 2));
        assertEquals(2, provider.lastIndexOf(text, Integer.MAX_VALUE));
        assertThrows(NullPointerException.class, () -> provider.lastIndexOf((byte[]) null, 0));
    }

    @Test
    void preservesFindLastContractAcrossSeededValidSearchProviders() {
        Random random = new Random(0x4d33524556455253L);
        for (int sample = 0; sample < 12_000; sample++) {
            byte[] data = new byte[random.nextInt(120)];
            byte[] needle = new byte[1 + random.nextInt(9)];
            for (int i = 0; i < data.length; i++) data[i] = (byte) random.nextInt(5);
            for (int i = 0; i < needle.length; i++) needle[i] = (byte) random.nextInt(5);
            LinearCompiled compiled = new LinearCompiled(needle);
            for (int from : new int[] {-5, -1, 0, 1, data.length / 2,
                    data.length - 1, data.length, Integer.MAX_VALUE}) {
                assertEquals(lastOracle(data, needle, from),
                        compiled.lastIndexOf(data, from), "sample=" + sample + " from=" + from);
            }
        }
    }

    @Test
    void preservesRemainingByteBufferRangesAndCallerState() {
        Random random = new Random(0x4d33425546464552L);
        for (int sample = 0; sample < 1_000; sample++) {
            byte[] data = new byte[random.nextInt(150)];
            byte[] needle = new byte[1 + random.nextInt(7)];
            random.nextBytes(data);
            random.nextBytes(needle);
            if (data.length >= needle.length && (sample & 1) == 0)
                System.arraycopy(needle, 0, data, data.length - needle.length, needle.length);
            LinearCompiled compiled = new LinearCompiled(needle);
            for (boolean direct : new boolean[] {false, true}) {
                ByteBuffer buffer = direct
                        ? ByteBuffer.allocateDirect(data.length + 7)
                        : ByteBuffer.allocate(data.length + 7);
                for (int index = 0; index < data.length; index++)
                    buffer.put(index + 3, data[index]);
                buffer.position(3);
                buffer.limit(data.length + 3);
                for (ByteBuffer view : new ByteBuffer[] {buffer, buffer.asReadOnlyBuffer(), buffer.slice()}) {
                    int pos = view.position(), limit = view.limit();
                    for (int from : new int[] {-1, 0, 1, data.length / 2,
                            data.length, Integer.MAX_VALUE}) {
                        assertEquals(lastOracle(data, needle, from), compiled.lastIndexOf(view, from),
                                "sample=" + sample + " direct=" + direct);
                        assertEquals(pos, view.position());
                        assertEquals(limit, view.limit());
                    }
                }
            }
        }
    }

    @Test
    void preservesOutOfWindowEarlyStopAndMonitorFailureIdentity() {
        AtomicInteger checks = new AtomicInteger();
        CompiledSearch sparse = new CompiledSearch() {
            @Override public int indexOf(byte[] values, int from) {
                if (from == 0) return 0;
                if (from == 1) return 2;
                return 8;
            }
            @Override public int needleLength() { return 1; }
            @Override public String strategy() { return "sparse"; }
        };
        byte[] data = new byte[10];
        assertEquals(2, sparse.lastIndexOf(data, 3));
        assertEquals(-1, sparse.lastIndexOf(data, -1));
        CancellationException failure = new CancellationException("postcheck");
        ProgressMonitor postcheck = polling(() -> {
            if (checks.incrementAndGet() == 2) throw failure;
        });
        assertSame(failure, assertThrows(CancellationException.class,
                () -> sparse.lastIndexOf(data, 3, postcheck)));
        assertEquals(2, checks.get());
        checks.set(0);
        ProgressMonitor precheck = polling(() -> { checks.incrementAndGet(); throw failure; });
        assertSame(failure, assertThrows(CancellationException.class,
                () -> sparse.lastIndexOf(data, 3, precheck)));
        assertEquals(1, checks.get());
        assertThrows(NullPointerException.class, () -> sparse.lastIndexOf((byte[]) null, 3));
    }

    private static int lastOracle(byte[] data, byte[] needle, int from) {
        for (int index = Math.min(from, data.length - needle.length); index >= 0; index--) {
            boolean match = true;
            for (int k = 0; k < needle.length; k++) match &= data[index + k] == needle[k];
            if (match) return index;
        }
        return -1;
    }

    private static ProgressMonitor polling(Runnable action) {
        return new ProgressMonitor() {
            @Override public void beginTask(String name, long total) {}
            @Override public void done() {}
            @Override public void worked(long units) {}
            @Override public void setProgress(long worked, long total) {}
            @Override public void setTaskName(String name) {}
            @Override public void subTask(String name) {}
            @Override public boolean isCanceled() { return false; }
            @Override public void setCanceled(boolean canceled) {}
            @Override public void checkCanceled() { action.run(); }
            @Override public ProgressState state() { return new DefaultProgressMonitor().state(); }
        };
    }

    private static final class LinearCompiled implements CompiledSearch {
        private final byte[] needle;
        private LinearCompiled(byte[] values) { this.needle = Arrays.copyOf(values, values.length); }
        @Override public int needleLength() { return needle.length; }
        @Override public String strategy() { return "linear-oracle"; }
        @Override public int indexOf(byte[] haystack, int from) {
            for (int index = Math.max(0, from); index <= haystack.length - needle.length; index++) {
                boolean match = true;
                for (int k = 0; k < needle.length; k++) match &= haystack[index + k] == needle[k];
                if (match) return index;
            }
            return -1;
        }
    }
}
