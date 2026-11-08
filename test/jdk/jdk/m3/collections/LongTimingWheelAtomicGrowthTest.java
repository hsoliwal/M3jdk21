/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/** Real wheel source/JUnit adapter: previous lifecycle and new OOME atomicity. */
class LongTimingWheelAtomicGrowthTest {
    @Test
    void originalCallbackAndMaxTickRegressionIsPreserved() {
        LongTimingWheelSafeAdvanceSelfTest.main(new String[0]);
    }

    @Test
    void allThreeGrowthCopiesAreAtomicAndRecoverable() {
        LongTimingWheelAtomicGrowthSelfTest.main(new String[0]);
    }

    @Test
    void publicJavaSurfaceIsExactlyUnchanged() {
        var methods = Arrays.stream(LongTimingWheel.class.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers())
                        || Modifier.isProtected(m.getModifiers()))
                .map(m -> m.getName() + "/" + m.getParameterCount())
                .sorted().toList();
        assertEquals(java.util.List.of("advanceTo/2", "clear/0", "currentTick/0",
                "payloadBytes/0", "schedule/2", "size/0", "tickNanos/0",
                "wheelSize/0"), methods);
        assertEquals(1, LongTimingWheel.class.getDeclaredConstructors().length);
        assertTrue(Modifier.isPublic(LongTimingWheel.class
                .getDeclaredConstructors()[0].getModifiers()));
    }
}
