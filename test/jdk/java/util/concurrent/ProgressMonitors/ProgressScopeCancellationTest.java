/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */

/*
 * @test
 * @summary Preserve borrowed progress scope cancellation and cleanup through existing-variable resources
 * @run main/othervm ProgressScopeCancellationTest
 * @run main/othervm -Xint ProgressScopeCancellationTest
 */

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ProgressMonitor;
import java.util.concurrent.ProgressMonitors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ProgressScopeCancellationTest {
    private static final Object INPUT = new Object();
    private static final Object RESULT = new Object();
    private static int assertions;

    private enum Route { RUN, CALL, CONSUMER, FUNCTION, SUPPLIER }

    private static final class Probe implements ProgressMonitor {
        final List<String> events = new ArrayList<>();
        final CancellationException cancellation = new CancellationException("probe");
        int checks;
        int cancelAt;
        int completions;
        boolean cancelled;

        @Override
        public void checkCancelled() {
            require(ProgressMonitors.current() == this, "check sees borrowed monitor");
            events.add("check" + ++checks);
            if (checks == cancelAt) {
                throw cancellation;
            }
            ProgressMonitor.super.checkCancelled();
        }

        @Override
        public boolean isCancelled() { return cancelled; }

        @Override
        public void cancel() { cancelled = true; }

        @Override
        public void done() { completions++; }
    }

    private static Object invoke(Route route, ProgressMonitor monitor, Runnable body)
            throws Exception {
        return switch (route) {
            case RUN -> {
                ProgressMonitors.run(body, monitor);
                yield RESULT;
            }
            case CALL -> ProgressMonitors.call(() -> {
                body.run();
                return RESULT;
            }, monitor);
            case CONSUMER -> {
                Consumer<Object> consumer = value -> {
                    require(value == INPUT, "consumer input identity");
                    body.run();
                };
                consumer.accept(INPUT, monitor);
                yield RESULT;
            }
            case FUNCTION -> {
                Function<Object, Object> function = value -> {
                    require(value == INPUT, "function input identity");
                    body.run();
                    return RESULT;
                };
                yield function.apply(INPUT, monitor);
            }
            case SUPPLIER -> {
                Supplier<Object> supplier = () -> {
                    body.run();
                    return RESULT;
                };
                yield supplier.get(monitor);
            }
        };
    }

    public static void main(String[] args) throws Exception {
        require(!Thread.currentThread().isInterrupted(), "uninterrupted test entry");
        ProgressMonitor previous = ProgressMonitors.current();
        Probe parent = new Probe();
        final ProgressMonitors.Scope parentScope = ProgressMonitors.bind(parent);
        try (parentScope) {
            for (Route route : Route.values()) {
                normal(route, parent);
                cancellation(route, parent, 1);
                cancellation(route, parent, 2);
                bodyFailure(route, parent);
                monitorCancellation(route, parent);
                interruption(route, parent);
                nullMonitor(route, parent);
                nested(route, parent);
            }
            checkedFailure(parent);
            nullTask(parent);
        }
        require(ProgressMonitors.current() == previous, "outer binding restored");
        require(parent.completions == 0, "outer borrowed monitor never completed");
        for (Route route : Route.values()) {
            cleanupSuppression(route);
        }
        System.out.println("PROGRESS_SCOPE_CANCELLATION_PASS routes=5 assertions=" + assertions);
    }

    private static void normal(Route route, Probe parent) throws Exception {
        Probe monitor = new Probe();
        Object result = invoke(route, monitor, () -> {
            require(ProgressMonitors.current() == monitor, route + " body binding");
            monitor.events.add("body");
        });
        require(result == RESULT, route + " result identity");
        events(monitor, "check1", "body", "check2");
        restored(monitor, parent);
    }

    private static void cancellation(Route route, Probe parent, int check) throws Exception {
        Probe monitor = new Probe();
        monitor.cancelAt = check;
        try {
            invoke(route, monitor, () -> monitor.events.add("body"));
            throw new AssertionError(route + " cancellation missing");
        } catch (CancellationException actual) {
            require(actual == monitor.cancellation, route + " cancellation identity");
            require(actual.getSuppressed().length == 0, "balanced cancellation cleanup");
        }
        if (check == 1) {
            events(monitor, "check1");
        } else {
            events(monitor, "check1", "body", "check2");
        }
        restored(monitor, parent);
    }

    private static void bodyFailure(Route route, Probe parent) throws Exception {
        Probe monitor = new Probe();
        RuntimeException expected = new RuntimeException("body");
        try {
            invoke(route, monitor, () -> {
                monitor.events.add("body");
                throw expected;
            });
            throw new AssertionError(route + " body failure missing");
        } catch (RuntimeException actual) {
            require(actual == expected, route + " body failure identity");
            require(actual.getSuppressed().length == 0, "balanced failure cleanup");
        }
        events(monitor, "check1", "body");
        restored(monitor, parent);
    }

    private static void monitorCancellation(Route route, Probe parent) throws Exception {
        Probe monitor = new Probe();
        try {
            invoke(route, monitor, () -> {
                monitor.events.add("body");
                monitor.cancel();
            });
            throw new AssertionError(route + " monitor cancellation missing");
        } catch (CancellationException actual) {
            require(actual != monitor.cancellation, "default monitor cancellation used");
        }
        events(monitor, "check1", "body", "check2");
        restored(monitor, parent);
    }

    private static void interruption(Route route, Probe parent) throws Exception {
        Probe monitor = new Probe();
        Thread.currentThread().interrupt();
        try {
            try {
                invoke(route, monitor, () -> monitor.events.add("body"));
                throw new AssertionError(route + " interruption cancellation missing");
            } catch (CancellationException actual) {
                require(Thread.currentThread().isInterrupted(), "interrupt flag retained");
            }
            events(monitor, "check1");
            restored(monitor, parent);
        } finally {
            Thread.interrupted();
        }
    }

    private static void nullMonitor(Route route, Probe parent) throws Exception {
        boolean[] ran = {false};
        try {
            invoke(route, null, () -> ran[0] = true);
            throw new AssertionError(route + " null monitor accepted");
        } catch (NullPointerException expected) {
            require(!ran[0], "null monitor cannot invoke body");
            require(ProgressMonitors.current() == parent, "null monitor preserves parent");
        }
    }

    private static void nested(Route route, Probe parent) throws Exception {
        Probe monitor = new Probe();
        Probe child = new Probe();
        invoke(route, monitor, () -> {
            monitor.events.add("body");
            final ProgressMonitors.Scope childScope = ProgressMonitors.bind(child);
            try (childScope) {
                require(ProgressMonitors.current() == child, "child binding installed");
            }
            require(ProgressMonitors.current() == monitor, "child restores borrowed monitor");
        });
        events(monitor, "check1", "body", "check2");
        require(child.completions == 0, "child borrowed monitor never completed");
        restored(monitor, parent);
    }

    private static void checkedFailure(Probe parent) throws Exception {
        Probe monitor = new Probe();
        Exception expected = new Exception("checked body");
        try {
            ProgressMonitors.call(() -> {
                monitor.events.add("body");
                throw expected;
            }, monitor);
            throw new AssertionError("checked body failure missing");
        } catch (Exception actual) {
            require(actual == expected, "checked body failure identity");
        }
        events(monitor, "check1", "body");
        restored(monitor, parent);
    }

    private static void nullTask(Probe parent) throws Exception {
        try {
            ProgressMonitors.run(null, null);
            throw new AssertionError("null run task accepted");
        } catch (NullPointerException expected) {
            require("task".equals(expected.getMessage()), "run task checked before binding");
        }
        try {
            ProgressMonitors.call(null, null);
            throw new AssertionError("null call task accepted");
        } catch (NullPointerException expected) {
            require("task".equals(expected.getMessage()), "call task checked before binding");
        }
        require(ProgressMonitors.current() == parent, "null tasks preserve parent");
    }

    private static void cleanupSuppression(Route route) throws InterruptedException {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread worker = new Thread(() -> {
            Probe monitor = new Probe();
            Probe child = new Probe();
            ProgressMonitors.Scope[] nested = new ProgressMonitors.Scope[1];
            RuntimeException primary = new RuntimeException("primary");
            try {
                try {
                    invoke(route, monitor, () -> {
                        monitor.events.add("body");
                        nested[0] = ProgressMonitors.bind(child);
                        throw primary;
                    });
                    throw new AssertionError("unbalanced cleanup did not fail");
                } catch (RuntimeException actual) {
                    require(actual == primary, "body remains primary exception");
                    Throwable[] suppressed = actual.getSuppressed();
                    require(suppressed.length == 1, "one suppressed cleanup failure");
                    require(suppressed[0] instanceof IllegalStateException,
                            "LIFO violation remains cleanup failure");
                }
                events(monitor, "check1", "body");
                require(ProgressMonitors.current() == child, "failed close preserves child");
                nested[0].close();
                require(ProgressMonitors.current() == monitor, "child restores failed outer scope");
                require(monitor.completions == 0 && child.completions == 0,
                        "failed cleanup never completes borrowed monitors");
                // The inaccessible, deliberately unclosed outer scope belongs only
                // to this terminating worker, so it cannot pollute another scenario.
            } catch (Throwable actual) {
                failure.set(actual);
            }
        }, "progress-scope-cleanup-" + route);
        worker.start();
        worker.join(10000);
        require(!worker.isAlive(), "cleanup worker terminated");
        if (failure.get() != null) {
            throw new AssertionError(route + " cleanup suppression failed", failure.get());
        }
    }

    private static void restored(Probe monitor, Probe parent) {
        require(ProgressMonitors.current() == parent, "previous binding restored");
        require(monitor.completions == 0, "borrowed monitor never completed");
    }

    private static void events(Probe monitor, String... expected) {
        require(monitor.events.equals(List.of(expected)), "event order: " + monitor.events);
    }

    private static void require(boolean condition, String message) {
        assertions++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
