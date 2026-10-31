/*******************************************************************************
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
package com.m3.collections.apache;

/**
 * Fake millisecond clock shared by the M3 map, the Commons 4.5.0 oracle and the test policies. The oracle's two
 * {@code System.currentTimeMillis()} call sites are redirected here by {@link M3PassiveExpiringOracle}, so the class
 * and its method must stay public: the oracle classes live in another class loader. With a tick of {@code n}, every
 * read advances the clock by {@code n}, which turns the number and order of clock reads into observable results.
 */
public final class M3PassiveExpiringTestClock {
    private static long now;
    private static long tick;

    private M3PassiveExpiringTestClock() {
    }

    /** The redirected {@code System.currentTimeMillis()}: returns the time, then advances it by the tick. */
    public static long currentTimeMillis() {
        long t = now;
        now = t + tick;
        return t;
    }

    /** Sets the time and the advance per read. */
    public static void set(long time, long ticksPerRead) {
        now = time;
        tick = ticksPerRead;
    }

    /** Sets the time and keeps the tick. */
    public static void rewind(long time) {
        now = time;
    }

    /** Reads the time without advancing it. */
    public static long peek() {
        return now;
    }

    /** Moves the time by {@code millis}, which may be negative. */
    public static void advance(long millis) {
        now += millis;
    }
}
