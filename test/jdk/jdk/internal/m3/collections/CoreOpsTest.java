/*
 * Copyright 2026 Synexia <hsoliwal@gmail.com>
 * Licensed under the Apache License, Version 2.0.
 */
package jdk.internal.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CoreOpsTest {

    @Test
    void preservesStringAndParsingContract() {
        assertTrue(CoreOps.isBlank("  "));
        assertTrue(CoreOps.isNotBlank("x"));
        assertEquals("x", CoreOps.trim(" x "));
        assertNull(CoreOps.trimToNull("  "));
        assertEquals(17, CoreOps.parseInt("17", -1));
        assertEquals(-1, CoreOps.parseInt("bad", -1));
        assertEquals(42L, CoreOps.parseLong("42", -1));
        assertTrue(CoreOps.parseBoolean("yes", false));
        assertFalse(CoreOps.parseBoolean("off", true));
    }

    @Test
    void preservesNullCollectionAndSafeExecutionContract() {
        assertEquals("b", CoreOps.coalesce(null, "b", "c"));
        assertEquals("fallback", CoreOps.defaultIfNull(null, "fallback"));
        assertTrue(CoreOps.isEmpty(List.of()));
        assertTrue(CoreOps.isNotEmpty(List.of("x")));
        assertEquals(1, CoreOps.size(Map.of("k", "v")));
        assertEquals("fallback", CoreOps.safe(() -> { throw new IllegalStateException("boom"); }, "fallback"));
        assertFalse(CoreOps.safeRun(() -> { throw new IllegalStateException("boom"); }));
    }

    @Test
    void preservesFailFastGuards() {
        assertEquals("x", CoreOps.requireNonBlank(" x ", "value"));
        assertThrows(NullPointerException.class, () -> CoreOps.requireNonNull(null, "value"));
        assertThrows(IllegalArgumentException.class, () -> CoreOps.require(false, "bad"));
        assertThrows(IllegalStateException.class, () -> CoreOps.checkState(false, "bad"));
    }
}
