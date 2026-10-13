/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.parallel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.stream.IntStream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

final class ParallelForTest {

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 7, 64, 1000, 100_003})
    void visitsEveryIndexExactlyOnce(int size) {
        AtomicIntegerArray visits = new AtomicIntegerArray(Math.max(1, size));
        ParallelFor.run(size, visits::incrementAndGet);
        IntStream.range(0, size).forEach(index -> assertEquals(1, visits.get(index), "index " + index));
    }

    @Test
    void chunksAreBoundedAndNonEmpty() {
        assertEquals(1, ParallelFor.chunkSize(1, 32));
        assertEquals(1, ParallelFor.chunkSize(255, 32));
        assertEquals(1000, ParallelFor.chunkSize(256_000, 32));
        assertTrue(ParallelFor.workers(1) == 1);
        assertTrue(ParallelFor.workers(1 << 20) >= 1);
    }

    @Test
    void propagatesWorkItemFailure() {
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> ParallelFor.run(10_000, index -> {
                    if (index == 4321) {
                        throw new IllegalStateException("boom at " + index);
                    }
                }));
        assertTrue(failure.getMessage().contains("boom") || failure.getCause() != null);
    }
}
