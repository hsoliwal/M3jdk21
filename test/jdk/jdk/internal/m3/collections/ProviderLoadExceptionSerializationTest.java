/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import org.junit.jupiter.api.Test;

/**
 * Serialization contract of {@link ProviderLoadException} after relocation from the Synexia
 * {@code ProviderLoadException}. The Synexia ProviderSerializationCompatibilityTest proved (on the Synexia source, run
 * in the absorption proof) that 8132423011135672925 is the default UID of the historical shape; the relocated owner
 * keeps that explicit constant (bytecode identical modulo names). That test cannot run relocated: Java serialization
 * also requires equal simple class names, so a stream of the Synexia class never decodes into the M3 name. Here the
 * pinned UID and both directions within the M3 name are proved.
 */
final class ProviderLoadExceptionSerializationTest {
    @Test void keepsTheHistoricalSerialVersionUid() {
        assertEquals(8132423011135672925L, ObjectStreamClass.lookup(ProviderLoadException.class).getSerialVersionUID());
    }

    @Test void roundTripsLogicalIdMessageAndCause() throws Exception {
        ProviderLoadException original = new ProviderLoadException(42L, new IllegalArgumentException("fixture cause"));
        assertEquals("Provider load failed for logical id 42", original.getMessage());
        ProviderLoadException copy = (ProviderLoadException) decode(encode(original));
        assertEquals(42L, copy.logicalId());
        assertEquals(original.getMessage(), copy.getMessage());
        assertEquals("fixture cause", copy.getCause().getMessage());
        assertEquals(IllegalArgumentException.class, copy.getCause().getClass());
    }

    @Test void extremesAndNullCause() throws Exception {
        for (long id : new long[] {Long.MIN_VALUE, -1L, 0L, Long.MAX_VALUE}) {
            ProviderLoadException copy = (ProviderLoadException) decode(encode(new ProviderLoadException(id, null)));
            assertEquals(id, copy.logicalId());
            assertNull(copy.getCause());
        }
    }

    private static byte[] encode(Object value) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(value);
        }
        return bytes.toByteArray();
    }

    private static Object decode(byte[] bytes) throws Exception {
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return input.readObject();
        }
    }
}
