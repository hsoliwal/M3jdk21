// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/jdk/internal/m3/search/JavaAdaptiveSearchEngineTest.java (blob 9ae0a1fa1944) by M3_SEARCH_RELOCATE v1.
package jdk.m3.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class JavaAdaptiveSearchEngineTest {
    @Test
    void findsExactNeedlesWithPrecomputedStrategies() {
        M3SearchEngine engine = new M3JavaAdaptiveSearchEngine();
        try (M3CompiledSearch shortPattern = engine.compile(bytes("aba"));
             M3CompiledSearch longPattern = engine.compile(bytes("needle-value"))) {
            assertEquals(2, shortPattern.indexOf(bytes("xxabaaba"), 0));
            assertEquals(7, longPattern.indexOf(bytes("prefix-needle-value-suffix"), 0));
            assertEquals(-1, longPattern.indexOf(bytes("no match"), 0));
            assertEquals("KMP", shortPattern.strategy());
            assertEquals("BOYER_MOORE_HORSPOOL", longPattern.strategy());
        }
    }

    @Test
    void findsLastExactNeedleWithoutChangingForwardSemantics() {
        M3SearchEngine engine = new M3JavaAdaptiveSearchEngine();
        byte[] overlapping = bytes("ababa");
        try (M3CompiledSearch kmp = engine.compile(bytes("aba"));
             M3CompiledSearch horspool = engine.compile(bytes("needle-value"))) {
            assertEquals(2, kmp.lastIndexOf(overlapping, Integer.MAX_VALUE));
            assertEquals(0, kmp.lastIndexOf(overlapping, 1));
            assertEquals(-1, kmp.lastIndexOf(overlapping, -1));

            byte[] repeated = bytes("needle-value/needle-value");
            assertEquals(13, horspool.lastIndexOf(repeated, Integer.MAX_VALUE));
            assertEquals(0, horspool.lastIndexOf(repeated, 12));
            assertEquals(-1, horspool.lastIndexOf(repeated, -1));

            assertEquals(0, kmp.indexOf(overlapping, 0));
            assertEquals(13, horspool.indexOf(repeated, 1));
        }
    }

    @Test
    void searchesHeapAndDirectBufferRemainingRangesWithoutMutatingState() {
        M3SearchEngine engine = new M3JavaAdaptiveSearchEngine();
        byte[] payload = bytes("xxababa/needle-value/needle-valueyy");
        for (boolean direct : new boolean[] {false, true}) {
            ByteBuffer buffer = direct
                    ? ByteBuffer.allocateDirect(payload.length + 6)
                    : ByteBuffer.allocate(payload.length + 6);
            for (int index = 0; index < payload.length; index++) {
                buffer.put(3 + index, payload[index]);
            }
            buffer.position(3);
            buffer.limit(3 + payload.length);
            int position = buffer.position();
            int limit = buffer.limit();
            try (M3CompiledSearch kmp = engine.compile(bytes("aba"));
                 M3CompiledSearch horspool = engine.compile(bytes("needle-value"))) {
                assertEquals(2, kmp.indexOf(buffer, 0));
                assertEquals(4, kmp.lastIndexOf(buffer, Integer.MAX_VALUE));
                assertEquals(8, horspool.indexOf(buffer, 0));
                assertEquals(21, horspool.lastIndexOf(buffer, Integer.MAX_VALUE));
            }
            assertEquals(position, buffer.position());
            assertEquals(limit, buffer.limit());
        }
    }

    @Test
    void compiledPatternDoesNotRetainCallerArray() {
        byte[] needle = bytes("needle-value");
        M3CompiledSearch compiled = new M3JavaAdaptiveSearchEngine().compile(needle);
        String strategy = compiled.strategy();
        needle[0] = 'X';
        assertEquals(2, compiled.indexOf(bytes("xxneedle-value"), 0));
        assertNotEquals("", strategy);
        compiled.close();
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
