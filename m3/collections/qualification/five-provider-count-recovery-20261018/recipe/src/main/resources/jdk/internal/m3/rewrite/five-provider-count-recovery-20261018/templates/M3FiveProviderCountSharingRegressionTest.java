/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections.interop;

import static org.junit.jupiter.api.Assertions.*;

import com.m3.collections.M3CountedBag;
import com.m3.collections.M3IndexPathMasks;
import com.m3.collections.M3ObjectCounts;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Cross-provider identity and 32-bit bitmap JNI regression proof on actual M3 owners. */
final class M3FiveProviderCountSharingRegressionTest {
    @Test void liveGuavaEclipseApacheShareOneM3CountAndJdkSpringAreIndependent() {
        M3ObjectCounts<String> source = new M3ObjectCounts<>();
        source.add(null, 2);
        source.setFlags(null, 0x80000001);
        source.add("keep", 3);
        source.setFlags("keep", 0x80000001);
        source.add("drop", 11);
        source.setFlags("drop", 0x00000002);
        var captured = M3FiveProviderFlagSelection.capture(source, 0x80000000, 0x2,
                M3IndexPathMasks.Backend.JAVA);
        M3CountedBag<String> shared = captured.m3Backend();
        var guava = captured.guavaMultiset();
        var eclipse = captured.eclipseBag();
        var apache = captured.apacheMultiSet();
        assertSame(shared, ((M3GuavaMultiset<String>) guava).m3Backend());
        assertSame(shared, ((M3EclipseBag<String>) eclipse).m3Backend());
        assertSame(shared, ((M3ApacheMultiSet<String>) apache).m3Backend());
        assertEquals(5L, captured.totalCount());
        assertEquals(0x80000001, captured.flags(null));
        Map<String,Integer> jdk = captured.jdkCounts();
        var spring = captured.springCountLists();
        assertEquals(List.of(3), spring.get("keep"));

        assertEquals(3, guava.add("keep", 2));       // Guava previous-count convention
        assertEquals(5, apache.getCount("keep"));
        assertEquals(6, eclipse.addOccurrences("keep", 1)); // Eclipse new-count convention
        assertEquals(6, guava.count("keep"));
        assertEquals(6, apache.remove("keep", 2));  // Apache previous-count convention
        assertEquals(4, eclipse.occurrencesOf("keep"));
        assertTrue(eclipse.removeOccurrences(null, 2));
        assertEquals(0, guava.count(null));
        assertEquals(0, captured.flags(null));
        assertEquals(3 + 0 + 0, source.count("keep")); // original count owner unaffected
        assertEquals(3, jdk.get("keep"));
        assertEquals(List.of(3), spring.get("keep"));
        assertEquals(4L, captured.totalCount());
        assertEquals(1, shared.distinctSize());
        source.verifyInvariants();
    }

    @Test void nativeParityAndRefusalIncludeEmptyAndBitThirtyOne() {
        M3ObjectCounts<Integer> source = new M3ObjectCounts<>();
        Random random = new Random(0x7382128b4L);
        for (int key = 0; key < 256; key++) {
            source.add(key, 1 + random.nextInt(13));
            source.setFlags(key, random.nextInt());
        }
        for (int i = 0; i < 64; i++) {
            int required = random.nextInt() & 0x80040001;
            int excluded = random.nextInt() & 0x20008002;
            var reference = M3FiveProviderFlagSelection.capture(source, required, excluded,
                    M3IndexPathMasks.Backend.JAVA);
            assertTrue(reference.totalCount() >= 0);
            if (M3IndexPathMasks.nativeAvailable()) {
                var nativeResult = M3FiveProviderFlagSelection.capture(source, required, excluded,
                        M3IndexPathMasks.Backend.NATIVE_REQUIRED);
                assertEquals(reference.jdkCounts(), nativeResult.jdkCounts());
                assertEquals(reference.totalCount(), nativeResult.totalCount());
            } else {
                assertThrows(UnsatisfiedLinkError.class, () ->
                        M3FiveProviderFlagSelection.capture(source, required, excluded,
                                M3IndexPathMasks.Backend.NATIVE_REQUIRED));
            }
        }
    }
}
