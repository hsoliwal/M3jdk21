/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** A real stock-JDK-compiled client that references no M3 progress API. */
public final class OldCallableClient {
    private OldCallableClient() { }

    private static final class Dual implements Runnable, Callable<Object> {
        private final Object value;
        int runs;
        Dual(Object value) { this.value = value; }
        @Override public void run() { runs++; }
        @Override public Object call() { return value; }
    }

    public static void main(String[] args) throws Exception {
        Object value = new Object();
        Callable<Object> lambda = () -> value;
        Callable<Object> anonymous = new Callable<>() {
            @Override public Object call() { return value; }
        };
        Dual dual = new Dual(value);
        if (lambda.call() != value || anonymous.call() != value || dual.call() != value) {
            throw new AssertionError("old result identity");
        }
        ((Runnable) dual).run();
        if (dual.runs != 1) throw new AssertionError("old Runnable contract");
        IOException marker = new IOException("legacy checked exception");
        Callable<Object> fails = () -> { throw marker; };
        try {
            fails.call();
            throw new AssertionError("expected checked exception");
        } catch (IOException actual) {
            if (actual != marker) throw new AssertionError("legacy exception identity");
        }
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            if (executor.submit((Callable<Object>) dual).get(15, TimeUnit.SECONDS) != value) {
                throw new AssertionError("old executor result");
            }
            try {
                executor.submit(fails).get(15, TimeUnit.SECONDS);
                throw new AssertionError("expected future failure");
            } catch (ExecutionException actual) {
                if (actual.getCause() != marker) throw new AssertionError("legacy future cause identity");
            }
        } finally {
            executor.shutdownNow();
            if (!executor.awaitTermination(15, TimeUnit.SECONDS)) throw new AssertionError("worker did not stop");
        }
        System.out.println("PASS old Callable lambda/anonymous/class/dual Runnable and checked exception identity; finite client witness");
    }
}
