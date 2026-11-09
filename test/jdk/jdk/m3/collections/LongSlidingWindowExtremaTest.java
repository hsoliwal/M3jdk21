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

/** Existing public contract and independently exercised lazy extrema recovery. */
class LongSlidingWindowExtremaTest {
    @Test
    void originalWindowContractAndExtremaProof() throws Exception {
        LongSlidingWindowExtremaSelfTest.main(new String[0]);
    }

    @Test
    void publicMethodSetAndConstructorStayUnchanged() {
        var names = Arrays.stream(LongSlidingWindow.class.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers())
                        || Modifier.isProtected(m.getModifiers()))
                .map(m -> m.getName() + "/" + m.getParameterCount())
                .sorted().toList();
        assertEquals(java.util.List.of("add/2", "average/0", "capacity/0", "clear/0",
                "forEach/1", "getFromOldest/1", "isEmpty/0", "isFull/0", "max/0",
                "min/0", "newest/0", "oldest/0", "payloadBytes/0", "size/0", "sum/0"), names);
        assertEquals(1, LongSlidingWindow.class.getDeclaredConstructors().length);
        assertTrue(Modifier.isPublic(LongSlidingWindow.class.getDeclaredConstructors()[0]
                .getModifiers()));
    }
}
