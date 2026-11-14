// Copyright 2026 Hitesh Soliwal, Synexia, and contributors.
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.lexicon;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Standalone header tests; this gate does not validate hierarchy bodies. */
public final class HierarchyHeaderTest {
    private static final String[] LEGACY = {
        "M3LEX001", "SYNARR01", "MIWROWS1", "SYNDICT1", "SYNTULP1", "SYNSEMG1", "SNXSTR01",
        "SNXLEX01", "M3PAIR01", "M3LIMG01", "M3GEO001", "M3FOLD01", "M3RXW001", "M3SEPB01"
    };

    // Wire facts: Synexia MappedHierarchicalLexicon 4d08f568e0c44c717dc3595d5de5965bfc31b835,
    // and Format 25a1b5e2b8fd3ffbd4c3d6da0eaeb6df3c8344b3. No donor runtime dependency.
    private static ByteBuffer header(int capacity, int version) {
        ByteBuffer bytes = ByteBuffer.allocate(Math.max(128, capacity));
        bytes.order(ByteOrder.LITTLE_ENDIAN);
        bytes.putLong(0x31524549484e5953L).putInt(version);
        bytes.position(0);
        bytes.limit(capacity);
        return bytes;
    }

    private static M3LexiconFormat hierarchy() {
        return M3LexiconFormat.valueOf("SYNHIER1");
    }

    private static void requireHeader(ByteBuffer bytes) throws M3LexiconFormatException {
        assertSame(hierarchy(), M3LexiconFormat.checkHeader(bytes));
        assertSame(hierarchy(), M3LexiconFormat.sniff(bytes).orElseThrow());
    }

    @Test
    void hierarchyHeaderFacts() throws Exception {
        ByteBuffer bytes = header(128, 1);
        assertEquals(ByteOrder.LITTLE_ENDIAN, bytes.order());
        byte[] magic = new byte[8];
        bytes.get(0, magic);
        assertArrayEquals("SYNHIER1".getBytes(StandardCharsets.US_ASCII), magic);
        requireHeader(bytes);
        assertEquals(0x53594e4849455231L, hierarchy().magic());
        assertSame(ByteOrder.LITTLE_ENDIAN, hierarchy().order());
        assertEquals(128, hierarchy().headerBytes());
        assertEquals("NONE", hierarchy().m3Owner());
    }

    @Test
    void everyTruncatedHeaderRejected() {
        for (int length = 0; length < 128; length++) {
            ByteBuffer bytes = header(length, 1);
            var failure = assertThrows(M3LexiconFormatException.class,
                    () -> M3LexiconFormat.checkHeader(bytes), "cut " + length);
            assertEquals(length >= 8 ? "truncated header" : "unknown lexicon image magic",
                    failure.getMessage());
            assertEquals(length >= 8, M3LexiconFormat.sniff(bytes).isPresent());
        }
    }

    @Test
    void versionBoundariesAndByteOrder() {
        int[] versions = {Integer.MIN_VALUE, -1, 0, 2, 255, 256, Integer.MAX_VALUE};
        for (int version : versions) {
            ByteBuffer bytes = header(128, version);
            var failure = assertThrows(M3LexiconFormatException.class,
                    () -> M3LexiconFormat.checkHeader(bytes));
            assertEquals("unsupported version", failure.getMessage());
        }
        ByteBuffer wrong = header(128, 1);
        wrong.order(ByteOrder.BIG_ENDIAN).putInt(8, 1);
        assertThrows(M3LexiconFormatException.class, () -> M3LexiconFormat.checkHeader(wrong));
    }

    @Test
    void everyMagicByteMutationRefused() {
        for (int index = 0; index < 8; index++) {
            for (int bit = 0; bit < 8; bit++) {
                ByteBuffer bytes = header(128, 1);
                bytes.put(index, (byte) (bytes.get(index) ^ (1 << bit)));
                assertTrue(M3LexiconFormat.sniff(bytes).isEmpty());
                assertThrows(M3LexiconFormatException.class,
                        () -> M3LexiconFormat.checkHeader(bytes));
            }
        }
    }

    @Test
    void absoluteOriginAndBufferEffects() throws Exception {
        ByteOrder[] orders = {ByteOrder.BIG_ENDIAN, ByteOrder.LITTLE_ENDIAN};
        for (boolean direct : new boolean[] {false, true}) {
            for (boolean readOnly : new boolean[] {false, true}) {
                for (ByteOrder order : orders) {
                    ByteBuffer storage = direct
                            ? ByteBuffer.allocateDirect(256) : ByteBuffer.allocate(256);
                    storage.position(37);
                    storage.put(header(128, 1));
                    storage.position(37);
                    storage.limit(165);
                    ByteBuffer bytes = storage.slice();
                    if (readOnly) {
                        bytes = bytes.asReadOnlyBuffer();
                    }
                    bytes.order(order);
                    bytes.position(19);
                    bytes.mark();
                    bytes.position(23);
                    requireHeader(bytes);
                    assertEquals(23, bytes.position());
                    assertEquals(128, bytes.limit());
                    assertSame(order, bytes.order());
                    bytes.reset();
                    assertEquals(19, bytes.position());
                }
            }
        }
    }

    @Test
    void legacyFormatsKeepCoordinates() throws Exception {
        assertEquals(15, M3LexiconFormat.values().length);
        int[] sizes = {64, 64, 104, 128, 8, 8, 72, 40, 176, 224, 256, 64, 128, 64};
        for (int index = 0; index < LEGACY.length; index++) {
            var format = M3LexiconFormat.valueOf(LEGACY[index]);
            assertEquals(index, format.ordinal());
            assertSame(format, M3LexiconFormat.values()[index]);
            assertEquals(sizes[index], format.headerBytes());
            ByteBuffer bytes = ByteBuffer.allocate(Math.max(16, format.headerBytes()));
            bytes.putLong(0, format.magic());
            bytes.order(format.order()).putInt(8, index == 2 ? 3 : 1);
            assertSame(format, M3LexiconFormat.checkHeader(bytes));
            assertSame(format, M3LexiconFormat.sniff(bytes).orElseThrow());
        }
        assertEquals(14, hierarchy().ordinal());
    }

    @Test
    void publicValuesAreIndependent() throws Exception {
        var values = M3LexiconFormat.values();
        Arrays.fill(values, null);
        assertNotNull(M3LexiconFormat.values()[0]);
        ByteBuffer bytes = ByteBuffer.allocate(64);
        bytes.putLong(0, M3LexiconFormat.M3LEX001.magic()).putInt(8, 1);
        assertSame(M3LexiconFormat.M3LEX001, M3LexiconFormat.checkHeader(bytes));
        requireHeader(header(128, 1));
    }

    @Test
    void nullContractAndNoBodyAdmission() throws Exception {
        assertThrows(NullPointerException.class, () -> M3LexiconFormat.sniff(null));
        assertThrows(NullPointerException.class, () -> M3LexiconFormat.checkHeader(null));
        ByteBuffer bytes = header(128, 1);
        // Header recognition deliberately does not admit the zero or corrupted hierarchy body.
        bytes.putInt(12, Integer.MIN_VALUE);
        bytes.putLong(24, Long.MAX_VALUE);
        requireHeader(bytes);
    }

    @Test
    void concurrentCursorIsolation() throws Exception {
        var executor = Executors.newFixedThreadPool(4);
        try {
            List<Callable<Boolean>> jobs = new ArrayList<>();
            for (int worker = 0; worker < 12; worker++) {
                jobs.add(() -> {
                    for (int index = 0; index < 250; index++) {
                        ByteBuffer bytes = header(128, 1);
                        bytes.position(12);
                        requireHeader(bytes);
                        assertEquals(12, bytes.position());
                    }
                    return true;
                });
            }
            for (var future : executor.invokeAll(jobs)) {
                assertTrue(future.get());
            }
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }
}
