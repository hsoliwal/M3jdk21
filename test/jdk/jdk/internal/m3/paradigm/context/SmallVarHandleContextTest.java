// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.paradigm.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SmallVarHandleContextTest {
    @Test
    void supportsAcquireReleaseCasAndCopyWithFixedShape() {
        SmallVarHandleContext context =
                new SmallVarHandleContext(List.of("task", "cost", "result"), Map.of("task", "index files"));

        assertEquals("index files", context.getAcquire("task", String.class));
        assertNull(context.getAcquire("result"));

        context.setRelease("cost", 7L);
        assertTrue(context.compareAndSet("result", null, "ok"));
        assertFalse(context.compareAndSet("result", null, "wrong"));

        SmallVarHandleContext copy = context.copy();
        assertEquals(7L, copy.getAcquire("cost", Long.class));
        assertEquals("ok", copy.getAcquire("result", String.class));
        assertTrue(copy.snapshot().containsKey("result"));
        assertTrue(context.revision() >= 2);
    }

    @Test
    void rejectsUnknownKeysAndOversizedShapes() {
        SmallVarHandleContext context = new SmallVarHandleContext(List.of("known"));
        assertThrows(IllegalArgumentException.class, () -> context.getAcquire("missing"));

        List<String> tooMany = java.util.stream.IntStream.rangeClosed(0, SmallVarHandleContext.MAX_SLOTS)
                .mapToObj(i -> "k" + i)
                .toList();
        assertThrows(IllegalArgumentException.class, () -> new SmallVarHandleContext(tooMany));
    }
}
