/* *****************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/**
 * Contract of {@link PlaceholderLogger}. Expected strings were produced by the reference {@code {}} formatter
 * (qualification/placeholder-logger-20261109, PLACEHOLDER_FORMAT_DIFFERENTIAL: 0 mismatches over 60,714 cases).
 */
final class PlaceholderLoggerTest {

    @Test
    void formatsPlaceholdersEscapesAndArrays() {
        assertEquals("a1b2c", PlaceholderLogger.format("a{}b{}c", new Object[] {1, 2}));
        assertEquals("1 {}", PlaceholderLogger.format("{} {}", new Object[] {1}));
        assertEquals("1", PlaceholderLogger.format("{}", new Object[] {1, 2}));
        assertEquals("{} 7", PlaceholderLogger.format("\\{} {}", new Object[] {7}));
        assertEquals("\\7", PlaceholderLogger.format("\\\\{}", new Object[] {7}));
        assertEquals("[1, 2]", PlaceholderLogger.format("{}", new Object[] {new int[] {1, 2}}));
        assertEquals("[a, [3]]", PlaceholderLogger.format("{}", new Object[] {new Object[] {"a", new long[] {3L}}}));
        assertEquals("null", PlaceholderLogger.format("{}", new Object[] {null}));
        assertNull(PlaceholderLogger.format(null, new Object[] {1}));
    }

    @Test
    void recursiveArraysAndFailingToStringAreBounded() {
        Object[] cyclic = new Object[2];
        cyclic[0] = "x";
        cyclic[1] = cyclic;
        assertEquals("[x, [...]]", PlaceholderLogger.format("{}", new Object[] {cyclic}));
        Object bad = new Object() {
            @Override public String toString() { throw new IllegalStateException("boom"); }
        };
        assertEquals("[FAILED toString()]", PlaceholderLogger.format("{}", new Object[] {bad}));
    }

    @Test
    void trailingThrowableIsLoggedNotFormatted() {
        IllegalStateException failure = new IllegalStateException("t");
        Capture capture = new Capture(Level.TRACE);
        PlaceholderLogger log = () -> capture;
        log.info("x {} {}", 1, failure);
        assertEquals(List.of("INFO|x 1 {}"), capture.lines);
        assertSame(failure, capture.thrown);
        log.warn("only {}", failure);
        assertEquals("WARNING|only {}", capture.lines.get(1));
    }

    @Test
    void disabledLevelsNeitherFormatNorLog() {
        Capture capture = new Capture(Level.WARNING);
        PlaceholderLogger log = () -> capture;
        Object expensive = new Object() {
            @Override public String toString() { throw new AssertionError("formatted while disabled"); }
        };
        log.debug("{}", expensive);
        log.info("{}", expensive);
        log.error("e {}", 5);
        assertEquals(List.of("ERROR|e 5"), capture.lines);
        assertTrue(log.isWarnEnabled());
        assertTrue(!log.isInfoEnabled());
    }

    @Test
    void namesFollowTheOwner() {
        assertEquals(PlaceholderLoggerTest.class.getName(), PlaceholderLogger.of(PlaceholderLoggerTest.class).getName());
    }

    /** A System.Logger that records enabled messages at or above a threshold. */
    private static final class Capture implements System.Logger {
        final List<String> lines = new ArrayList<>();
        final Level threshold;
        Throwable thrown;

        Capture(Level threshold) { this.threshold = threshold; }

        @Override public String getName() { return "capture"; }
        @Override public boolean isLoggable(Level level) { return level.getSeverity() >= threshold.getSeverity(); }
        @Override public void log(Level level, ResourceBundle bundle, String msg, Throwable t) {
            lines.add(level + "|" + msg);
            thrown = t;
        }
        @Override public void log(Level level, ResourceBundle bundle, String format, Object... params) {
            lines.add(level + "|" + format);
        }
        @Override public void log(Level level, Supplier<String> supplier) { lines.add(level + "|" + supplier.get()); }
    }
}
