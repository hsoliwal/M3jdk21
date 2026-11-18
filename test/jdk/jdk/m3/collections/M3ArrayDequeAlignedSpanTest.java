/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/** Qualify the product's original dual-lane ring and unchanged public facade. */
class M3ArrayDequeAlignedSpanTest {
    @Test void originalOwnerAndTwoLaneOffsetProof() {
        assertTrue(M3ArrayDequeAlignedSpanContract.run() > 30_000,
                "Java21 identity, state-mask and wrapped-ring differential");
    }

    @Test void publicMethodSurfaceKeepsExistingIdentity() {
        var names = Arrays.stream(M3ArrayDeque.class.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers())
                        || Modifier.isProtected(method.getModifiers()))
                .map(method -> method.getName() + "/" + method.getParameterCount())
                .sorted().toList();
        assertTrue(names.contains("physicalIndex/1"));
        assertTrue(names.contains("selectedPhysicalIndices/3"));
        assertTrue(names.contains("clear/0"));
        assertTrue(names.contains("removeFirstOccurrence/1"));
        assertTrue(names.contains("removeLastOccurrence/1"));
        assertEquals(1, Arrays.stream(M3ArrayDeque.class.getDeclaredFields())
                .filter(field -> field.getName().equals("values")).count());
        assertEquals(1, Arrays.stream(M3ArrayDeque.class.getDeclaredFields())
                .filter(field -> field.getName().equals("states")).count());
    }
}
