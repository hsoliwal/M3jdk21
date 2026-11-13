// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tools.cpu;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Owner identity, nested/concurrent borrow counts, and exception-safe release.
 * References to borrowed arrays remain inside their active callback only.
 * No native library is needed; this tests exactly the Java/JNI ownership atom.
 */
public final class M3ByteSuffixIndexBorrowOracle {
    private static long checks;

    private M3ByteSuffixIndexBorrowOracle() { }

    private static void check(boolean good, String message) {
        checks++;
        if (!good) throw new AssertionError(message + " check=" + checks);
    }

    public static void main(String[] args) throws InterruptedException {
        Random random = new Random(0x29b0a11L);
        for (int trial = 0; trial < 2000; trial++) {
            byte[] supplied = new byte[random.nextInt(128)];
            random.nextBytes(supplied);
            byte[] initial = supplied.clone();
            M3ByteSuffixIndex index = M3ByteSuffixIndex.copyOf(supplied);
            for (int repetition = 0; repetition < 4; repetition++) {
                int[] witness = index.withBorrowedNativeState(
                        (ownerText, ownerSuffixes, ownerMinimums, leafBase, packed, offsets) -> {
                            check(index.activeNativeArrayBorrowCount() == 3, "outer leases");
                            check(Arrays.equals(ownerText, initial), "borrowed text immutable");
                            int[] nested = index.withBorrowedNativeState(
                                    (innerText, innerSuffixes, innerMinimums,
                                            innerBase, innerPacked, innerOffsets) -> {
                                        check(index.activeNativeArrayBorrowCount() == 6,
                                                "nested leases");
                                        check(innerText == ownerText
                                                && innerSuffixes == ownerSuffixes
                                                && innerMinimums == ownerMinimums,
                                                "no clone across nested live borrows");
                                        check(innerBase == leafBase, "same index leaf base");
                                        return new int[]{ownerText.length, ownerSuffixes.length};
                                    }, packed, offsets);
                            check(index.activeNativeArrayBorrowCount() == 3,
                                    "nested lease released");
                            return nested;
                        }, new byte[0], new int[]{0});
                check(witness[0] == initial.length && witness[1] == initial.length,
                        "returned detached lengths");
                check(index.activeNativeArrayBorrowCount() == 0,
                        "all leases returned after callback");
            }
            try {
                index.withBorrowedNativeState((a,b,c,d,e,f) -> {
                    check(index.activeNativeArrayBorrowCount() == 3,
                            "exception still inside active lease");
                    throw new IllegalStateException("borrow failure sentinel");
                }, new byte[0], new int[]{0});
                throw new AssertionError("missing borrow exception");
            } catch (IllegalStateException expected) {
                check("borrow failure sentinel".equals(expected.getMessage()),
                        "original exception preserved");
            }
            check(index.activeNativeArrayBorrowCount() == 0,
                    "exception path must release every owner array");
            if (supplied.length > 0) supplied[0] ^= 31;
            check(Arrays.equals(index.bytes(), initial), "index owned source detached");
        }

        M3ByteSuffixIndex shared = M3ByteSuffixIndex.copyOf(new byte[]{1,2,3});
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger failures = new AtomicInteger();
        Thread borrower = new Thread(() -> {
            try {
                shared.withBorrowedNativeState((a,b,c,d,e,f) -> {
                    entered.countDown();
                    try {
                        if (!release.await(10, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("release deadline");
                        }
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(interrupted);
                    }
                    if (shared.activeNativeArrayBorrowCount() != 3) {
                        failures.incrementAndGet();
                    }
                    return new int[0];
                }, new byte[0], new int[]{0});
            } catch (Throwable failure) {
                failures.incrementAndGet();
            }
        }, "m3-suffix-borrow-lease-oracle");
        borrower.start();
        try {
            check(entered.await(10, TimeUnit.SECONDS),
                    "borrower entered synchronous callback");
            check(shared.activeNativeArrayBorrowCount() == 3,
                    "cross-thread owner-array borrow visible");
        } finally {
            release.countDown();
        }
        borrower.join(10_000L);
        check(!borrower.isAlive(), "borrower thread terminates");
        check(shared.activeNativeArrayBorrowCount() == 0
                && failures.get() == 0, "cross-thread borrow released");
        System.out.println("M3_SUFFIX_BORROW_JAVA_PASS checks=" + checks);
    }
}
