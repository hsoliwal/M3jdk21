/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import jdk.m3.collections.sketch.LongCountMinSketch;

/** Source-local Count-Min saturation and unchanged public contract bridge. */
class M3CountMinSaturationTest {
    @Test
    void originalOrdinaryMatrixBehavior() {
        M3CountMinSaturationSelfTest.main(new String[] {"ordinary"});
    }

    @Test
    void saturatedCountIsNotAnEmptySentinel() {
        M3CountMinSaturationSelfTest.main(new String[] {"saturation"});
        LongCountMinSketch sketch = new LongCountMinSketch(1, 1);
        assertEquals(0L, sketch.estimate(7L));
        assertEquals(Long.MAX_VALUE, sketch.add(7L, Long.MAX_VALUE));
        assertEquals(Long.MAX_VALUE, sketch.estimate(7L));
        assertEquals(Long.MAX_VALUE, sketch.add(7L, 0L));
        sketch.clear();
        assertEquals(0L, sketch.estimate(7L));
        assertThrows(IllegalArgumentException.class, () -> sketch.add(7L, -1L));
    }

    @Test
    void exactlyTheExistingPublicMethodSurface() {
        var signatures = Arrays.stream(LongCountMinSketch.class.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers())
                        || Modifier.isProtected(m.getModifiers()))
                .map(m -> m.getName() + "/" + m.getParameterCount())
                .sorted().toList();
        assertEquals(java.util.List.of("add/1", "add/2", "clear/0", "depth/0",
                "estimate/1", "forError/2", "merge/1", "payloadBytes/0",
                "totalCount/0", "width/0"), signatures);
        assertEquals(1, LongCountMinSketch.class.getDeclaredConstructors().length);
        assertTrue(Modifier.isPublic(LongCountMinSketch.class
                .getDeclaredConstructors()[0].getModifiers()));
    }
}
