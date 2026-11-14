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

/** Runs bounded baseline parity, signed billion-tick leaps and all inherited proof. */
class LongTimingWheelFastForwardTest {
    @Test
    void independentDeadlineAndLargeGapOracle() {
        LongTimingWheelFastForwardSelfTest.main(new String[] {"ordinary"});
        LongTimingWheelFastForwardSelfTest.main(new String[] {"huge"});
    }

    @Test
    void inheritedCallbackTerminalAndThreeLaneGrowthProofs() {
        LongTimingWheelSafeAdvanceSelfTest.main(new String[0]);
        LongTimingWheelAtomicGrowthSelfTest.main(new String[0]);
    }

    @Test
    void existingPublicInterfaceIsBytecodeCompatible() {
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
