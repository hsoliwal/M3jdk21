/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: GPL-2.0-only
 */

/*
 * @test
 * @summary Progress capture rejects null tasks before fallback lookup and preserves executor dispatch
 * @run main/othervm ExecutorProgressCaptureValidation
 * @run main/othervm -Xint ExecutorProgressCaptureValidation
 */

import java.util.concurrent.Executor;
import java.util.concurrent.ProgressMonitor;
import java.util.concurrent.ProgressMonitors;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Supplier;

public class ExecutorProgressCaptureValidation {
    private static int checks;

    public static void main(String[] args) {
        Supplier<? extends ProgressMonitor> previous =
                ProgressMonitors.setSystem(() -> ProgressMonitor.NONE);
        try {
            nullBeforeThrowingSupplier();
            nullBeforeSideEffectingSupplier();
            validTaskUsesLegacyExecuteAndCapturedMonitor();
            validTaskPreservesTwoArgumentOverride();
            lexicalBindingBypassesFallback();
            rejectedTaskPreservesFailureIdentity();
            failedTaskPreservesFailureIdentityAndScope();
            validTaskPreservesSupplierFailure();
            nullSupplierResultUsesNone();
            System.out.println("ExecutorProgressCaptureValidation checks=" + checks);
        } finally {
            ProgressMonitors.setSystem(previous);
        }
    }

    private static void nullBeforeThrowingSupplier() {
        int[] lookups = {0};
        IllegalStateException marker = new IllegalStateException("fallback supplier");
        ProgressMonitors.setSystem(() -> {
            lookups[0]++;
            throw marker;
        });
        RejectingHooks executor = new RejectingHooks();
        expectNullPointerException(() -> executor.executeWithProgress(null));
        check(lookups[0] == 0, "null task reached throwing fallback supplier");
        check(executor.legacyCalls == 0, "null task reached legacy execute");
        check(executor.monitoredCalls == 0, "null task reached monitored override");
    }

    private static void nullBeforeSideEffectingSupplier() {
        int[] lookups = {0};
        ProgressMonitors.setSystem(() -> {
            lookups[0]++;
            return ProgressMonitor.NONE;
        });
        RejectingHooks executor = new RejectingHooks();
        expectNullPointerException(() -> executor.executeWithProgress(null));
        check(lookups[0] == 0, "null task caused fallback side effects");
        check(executor.legacyCalls == 0, "null task reached legacy execute");
        check(executor.monitoredCalls == 0, "null task reached monitored override");
    }

    private static void validTaskUsesLegacyExecuteAndCapturedMonitor() {
        int[] lookups = {0};
        int[] taskCalls = {0};
        CountingMonitor captured = new CountingMonitor();
        ProgressMonitor workerBinding = new ProgressMonitor() { };
        ProgressMonitors.setSystem(() -> {
            lookups[0]++;
            return captured;
        });
        DeferredExecutor executor = new DeferredExecutor();
        executor.executeWithProgress(() -> {
            taskCalls[0]++;
            check(ProgressMonitors.current() == captured,
                    "task did not see submission-time monitor");
        });
        check(lookups[0] == 1, "valid task did not capture exactly once");
        check(executor.calls == 1, "legacy execute was not called exactly once");
        check(executor.queued != null, "legacy execute did not receive a runnable");
        check(taskCalls[0] == 0, "deferred task ran during submission");
        check(captured.cancellationChecks == 0, "submission executed monitor checks");

        ProgressMonitors.setSystem(() -> {
            throw new AssertionError("task execution looked up fallback again");
        });
        ProgressMonitors.Scope scope = ProgressMonitors.bind(workerBinding);
        try (scope) {
            executor.queued.run();
            check(ProgressMonitors.current() == workerBinding,
                    "successful task leaked its captured scope");
        }
        check(taskCalls[0] == 1, "deferred task did not run once");
        check(lookups[0] == 1, "task re-resolved its captured monitor");
        check(captured.cancellationChecks == 2, "execution cancellation checks changed");
        checkBorrowed(captured);
    }

    private static void validTaskPreservesTwoArgumentOverride() {
        int[] lookups = {0};
        int[] monitoredCalls = {0};
        CountingMonitor captured = new CountingMonitor();
        Runnable task = () -> {
            throw new AssertionError("override did not execute task");
        };
        ProgressMonitors.setSystem(() -> {
            lookups[0]++;
            return captured;
        });
        Executor executor = new Executor() {
            @Override
            public void execute(Runnable command) {
                throw new AssertionError("two-argument override was bypassed");
            }

            @Override
            public void execute(Runnable command, ProgressMonitor monitor) {
                monitoredCalls[0]++;
                check(command == task, "monitored override received a different task");
                check(monitor == captured, "monitored override received a different monitor");
            }
        };
        executor.executeWithProgress(task);
        check(monitoredCalls[0] == 1, "monitored override was not called once");
        check(lookups[0] == 1, "monitored override capture count changed");
        check(captured.cancellationChecks == 0, "capture helper ran monitor checks");
        checkBorrowed(captured);
    }

    private static void lexicalBindingBypassesFallback() {
        int[] lookups = {0};
        int[] executions = {0};
        CountingMonitor lexical = new CountingMonitor();
        ProgressMonitors.setSystem(() -> {
            lookups[0]++;
            throw new IllegalStateException("lexical binding must bypass fallback");
        });
        Executor executor = command -> {
            executions[0]++;
            command.run();
        };
        ProgressMonitors.Scope scope = ProgressMonitors.bind(lexical);
        try (scope) {
            executor.executeWithProgress(() -> check(ProgressMonitors.current() == lexical,
                    "lexical monitor identity changed"));
            expectNullPointerException(() -> executor.executeWithProgress(null));
            check(ProgressMonitors.current() == lexical, "lexical binding was lost");
        }
        check(lookups[0] == 0, "lexical task called fallback supplier");
        check(executions[0] == 1, "null lexical task reached executor");
        check(lexical.cancellationChecks == 2, "lexical cancellation checks changed");
        checkBorrowed(lexical);
    }

    private static void rejectedTaskPreservesFailureIdentity() {
        int[] lookups = {0};
        int[] executions = {0};
        CountingMonitor captured = new CountingMonitor();
        RejectedExecutionException rejected = new RejectedExecutionException("executor marker");
        ProgressMonitors.setSystem(() -> {
            lookups[0]++;
            return captured;
        });
        Executor executor = command -> {
            executions[0]++;
            throw rejected;
        };
        expectSameFailure(rejected, () -> executor.executeWithProgress(() -> {
            throw new AssertionError("rejected task executed");
        }));
        check(lookups[0] == 1, "rejected task capture count changed");
        check(executions[0] == 1, "rejected task legacy dispatch changed");
        check(captured.cancellationChecks == 0, "rejected task ran monitor checks");
        checkBorrowed(captured);
    }

    private static void failedTaskPreservesFailureIdentityAndScope() {
        int[] taskCalls = {0};
        CountingMonitor captured = new CountingMonitor();
        ProgressMonitor workerBinding = new ProgressMonitor() { };
        IllegalArgumentException failed = new IllegalArgumentException("task marker");
        ProgressMonitors.setSystem(() -> captured);
        DeferredExecutor executor = new DeferredExecutor();
        executor.executeWithProgress(() -> {
            taskCalls[0]++;
            check(ProgressMonitors.current() == captured, "failing task lost captured monitor");
            throw failed;
        });
        ProgressMonitors.Scope scope = ProgressMonitors.bind(workerBinding);
        try (scope) {
            expectSameFailure(failed, executor.queued);
            check(ProgressMonitors.current() == workerBinding,
                    "failing task leaked its captured scope");
        }
        check(taskCalls[0] == 1, "failing task invocation count changed");
        check(executor.calls == 1, "failing task legacy dispatch changed");
        check(captured.cancellationChecks == 1,
                "failing task checked cancellation after abrupt completion");
        checkBorrowed(captured);
    }

    private static void validTaskPreservesSupplierFailure() {
        int[] lookups = {0};
        IllegalStateException failed = new IllegalStateException("valid-task fallback marker");
        ProgressMonitors.setSystem(() -> {
            lookups[0]++;
            throw failed;
        });
        RejectingHooks executor = new RejectingHooks();
        expectSameFailure(failed, () -> executor.executeWithProgress(() -> { }));
        check(lookups[0] == 1, "valid-task supplier failure count changed");
        check(executor.legacyCalls == 0, "supplier failure reached legacy execute");
        check(executor.monitoredCalls == 0, "supplier failure reached monitored override");
    }

    private static void nullSupplierResultUsesNone() {
        int[] lookups = {0};
        int[] taskCalls = {0};
        ProgressMonitors.setSystem(() -> {
            lookups[0]++;
            return null;
        });
        Executor executor = Runnable::run;
        executor.executeWithProgress(() -> {
            taskCalls[0]++;
            check(ProgressMonitors.current() == ProgressMonitor.NONE,
                    "null fallback result was not normalized to NONE");
        });
        check(lookups[0] == 1, "null-result fallback was not captured once");
        check(taskCalls[0] == 1, "null-result fallback task did not run once");
    }

    private static void expectNullPointerException(Runnable operation) {
        try {
            operation.run();
        } catch (RuntimeException failure) {
            if (!(failure instanceof NullPointerException)) {
                throw new AssertionError("Expected NullPointerException, got " + failure, failure);
            }
            checks++;
            return;
        }
        throw new AssertionError("Expected NullPointerException");
    }

    private static void expectSameFailure(RuntimeException expected, Runnable operation) {
        try {
            operation.run();
        } catch (RuntimeException actual) {
            check(actual == expected, "failure identity changed: " + actual);
            return;
        }
        throw new AssertionError("Expected original failure " + expected);
    }

    private static void checkBorrowed(CountingMonitor monitor) {
        check(monitor.begins == 0, "borrowed monitor was begun");
        check(monitor.cancels == 0, "borrowed monitor was cancelled");
        check(monitor.completions == 0, "borrowed monitor was completed");
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class DeferredExecutor implements Executor {
        int calls;
        Runnable queued;

        @Override
        public void execute(Runnable command) {
            calls++;
            queued = command;
        }
    }

    private static final class RejectingHooks implements Executor {
        int legacyCalls;
        int monitoredCalls;

        @Override
        public void execute(Runnable command) {
            legacyCalls++;
            throw new AssertionError("unexpected legacy execute call");
        }

        @Override
        public void execute(Runnable command, ProgressMonitor monitor) {
            monitoredCalls++;
            throw new AssertionError("unexpected monitored execute call");
        }
    }

    private static final class CountingMonitor implements ProgressMonitor {
        int begins;
        int cancels;
        int completions;
        int cancellationChecks;

        @Override
        public void begin(String task, long totalWork) {
            begins++;
        }

        @Override
        public void cancel() {
            cancels++;
        }

        @Override
        public void done() {
            completions++;
        }

        @Override
        public void checkCancelled() {
            cancellationChecks++;
        }
    }
}
