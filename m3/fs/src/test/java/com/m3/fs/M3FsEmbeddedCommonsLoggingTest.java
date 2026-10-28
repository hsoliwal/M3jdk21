/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.fs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Test;

/** Embedded unchanged Commons Logging 1.2 API and JUL backend smoke contracts. */
final class M3FsEmbeddedCommonsLoggingTest {
    @Test void publicLoggerAndFactoryAreInOneM3FsArtifact() throws Exception {
        Class<?> api = Class.forName("org.apache.commons.logging.Log");
        Class<?> factory = Class.forName("org.apache.commons.logging.LogFactory");
        assertSame(Log.class, api);
        assertSame(LogFactory.class, factory);
        assertEquals(api.getProtectionDomain().getCodeSource().getLocation(),
                     factory.getProtectionDomain().getCodeSource().getLocation());
        assertNotNull(LogFactory.getLog("com.m3.fs.embedded"));
    }

    @Test void standardCommonsLoggingCachingAndMetadataStayOperational() {
        LogFactory factory = LogFactory.getFactory();
        Log first = factory.getInstance("com.m3.fs.caching");
        assertSame(first, factory.getInstance("com.m3.fs.caching"));
        assertNotNull(first);
        final String key = "com.m3.fs.embedded.test.attribute";
        Object before = factory.getAttribute(key);
        try {
            factory.setAttribute(key, "m3");
            assertEquals("m3", factory.getAttribute(key));
            first.debug("M3FS source-inlined logging smoke");
            first.warn("M3FS source-inlined logging warning smoke");
        } finally {
            if (before == null) factory.removeAttribute(key);
            else factory.setAttribute(key, before);
        }
    }
}
