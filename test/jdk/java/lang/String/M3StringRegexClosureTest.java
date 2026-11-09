/*
 * SPDX-License-Identifier: Apache-2.0 OR (GPL-2.0-only WITH Classpath-exception-2.0)
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * This new first-party test is offered under either license option above.
 *
 * @test
 * @summary Bounded M3 regex epsilon closure equals independent finite graph reachability
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED M3StringRegexClosureTest
 */
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Random;

public final class M3StringRegexClosureTest {
    private static MethodHandle closure;
    private static long checks;

    private M3StringRegexClosureTest() {}

    private static long graph(long bits, long stars) {
        long previous;
        do {
            previous = bits;
            bits |= (bits & stars) << 1;
        } while (bits != previous);
        return bits;
    }

    private static void check(int count, long stars, long bits) throws Throwable {
        long expected = graph(bits, stars);
        long actual = (long) closure.invokeExact(bits, stars);
        checks++;
        if (actual != expected || (actual | bits) != actual
                || (actual & ~((1L << (count + 1)) - 1)) != 0) {
            throw new AssertionError("closure n=" + count + " stars=" + stars
                    + " bits=" + bits + " actual=" + actual + " expected=" + expected);
        }
        long repeated = (long) closure.invokeExact(actual, stars);
        if (repeated != actual) throw new AssertionError("closure is not idempotent");
    }

    public static void main(String[] args) throws Throwable {
        if (args.length != 0) throw new IllegalArgumentException("no args");
        Class<?> owner = Class.forName("java.lang.M3StringRegexDp");
        closure = MethodHandles.privateLookupIn(owner, MethodHandles.lookup()).findStatic(
                owner, "close", MethodType.methodType(long.class, long.class, long.class));
        for (int count = 1; count <= 9; count++) {
            for (long stars = 0; stars < (1L << count); stars++) {
                for (long bits = 0; bits < (1L << (count + 1)); bits++) {
                    check(count, stars, bits);
                }
            }
        }
        if (checks != 699_048) throw new AssertionError("incomplete exhaustive corpus");
        for (int low = 0; low < 32; low++) {
            for (int high = low; high < 32; high++) {
                long stars = ((1L << (high - low + 1)) - 1) << low;
                for (int bit = 0; bit <= 32; bit++) {
                    check(32, stars, 1L << bit);
                    check(32, stars, (1L << bit) | (1L << 32));
                }
                check(32, stars, (1L << 33) - 1);
                check(32, stars, 0);
            }
        }
        Random random = new Random(0x4d33434c4f535552L);
        for (int trial = 0; trial < 20_000; trial++) {
            long stars = Integer.toUnsignedLong(random.nextInt());
            long bits = random.nextLong() & ((1L << 33) - 1);
            check(32, stars, bits);
        }
        System.out.println("M3_STRING_REGEX_CLOSURE_TEST_PASS checks=" + checks);
    }
}
