/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: GPL-2.0-only
 *
 * @test
 * @summary Progress convenience defaults preserve provider dispatch and borrowed lifecycle
 * @run main/othervm DefaultOverloads
 * @run main/othervm -Xint DefaultOverloads
 */

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ProgressMonitor;
import java.util.concurrent.ProgressMonitors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

public class DefaultOverloads {
    private static int checks;

    /*
     * An existing abstract provider supplies only the established full-argument
     * hooks. Its concrete subclass has no new implementation obligation.
     */
    private abstract static class ExistingHooks implements ProgressMonitor {
        final List<String> events = new ArrayList<>();
        String task;
        long total;
        long units;
        long completed;
        int begins;
        int updates;
        Throwable beginFailure;
        Throwable workFailure;

        @Override
        public void begin(String task, long totalWork) {
            events.add("begin");
            this.task = task;
            total = totalWork;
            begins++;
            propagate(beginFailure);
        }

        @Override
        public void worked(long units) {
            events.add("worked");
            this.units = units;
            completed += units;
            updates++;
            propagate(workFailure);
        }

        @Override
        public void message(String text) {
            events.add("message");
        }

        @Override
        public boolean isCancelled() {
            events.add("isCancelled");
            return false;
        }

        @Override
        public void cancel() {
            events.add("cancel");
        }

        @Override
        public void checkCancelled() {
            events.add("checkCancelled");
            ProgressMonitor.super.checkCancelled();
        }

        @Override
        public void done() {
            events.add("done");
        }
    }

    private static final class Provider extends ExistingHooks { }

    /*
     * These concrete short forms predate implementation of ProgressMonitor in
     * the child. Class inheritance must take precedence over interface defaults.
     */
    private abstract static class ExistingShortForms {
        String task;
        int begins;
        int updates;

        public void begin(String task) {
            this.task = task;
            begins++;
        }

        public void worked() {
            updates++;
        }
    }

    private static final class ShortFormProvider extends ExistingShortForms
            implements ProgressMonitor {
        @Override
        public void begin(String task, long totalWork) {
            throw new AssertionError("interface default bypassed existing short begin");
        }

        @Override
        public void worked(long units) {
            throw new AssertionError("interface default bypassed existing short worked");
        }
    }

    public static void main(String[] args) throws Exception {
        check(!Thread.currentThread().isInterrupted(), "uninterrupted test entry");
        methodShape();
        inheritedHookDispatch();
        existingClassMethodsWin();
        failureIdentity();
        methodReferences();
        noOpAndInterruption();
        weightedChildren();
        unknownAndZeroBudgets();
        borrowedScope();
        System.out.println("PROGRESS_MONITOR_DEFAULT_OVERLOADS_PASS checks=" + checks);
    }

    private static void methodShape() throws Exception {
        Method begin = ProgressMonitor.class.getDeclaredMethod("begin", String.class);
        Method worked = ProgressMonitor.class.getDeclaredMethod("worked");
        for (Method method : new Method[] { begin, worked }) {
            check(method.isDefault(), method + " is a default method");
            check(Modifier.isPublic(method.getModifiers()), method + " is public");
            check(method.getReturnType() == void.class, method + " returns void");
            check(method.getExceptionTypes().length == 0,
                    method + " adds no checked exceptions");
        }
        for (Method method : ProgressMonitor.class.getMethods()) {
            check(!Modifier.isAbstract(method.getModifiers()),
                    "no new abstract provider obligation: " + method);
        }
    }

    private static void inheritedHookDispatch() {
        for (String task : new String[] { new String("task"), "", "\uD83D\uDE80", null }) {
            Provider provider = new Provider();
            ProgressMonitor monitor = provider;
            monitor.begin(task);
            check(provider.begins == 1, "one full-argument begin invocation");
            check(provider.task == task, "task reference, including null, is unchanged");
            check(provider.total == ProgressMonitor.UNKNOWN_WORK, "unknown work sentinel");
            events(provider, "begin");

            monitor.worked();
            check(provider.updates == 1, "one full-argument worked invocation");
            check(provider.units == 1L && provider.completed == 1L, "one work unit");
            events(provider, "begin", "worked");

            monitor.worked();
            check(provider.updates == 2 && provider.completed == 2L,
                    "one additional invocation and unit on repetition");
            events(provider, "begin", "worked", "worked");
        }
    }

    private static void existingClassMethodsWin() {
        ShortFormProvider provider = new ShortFormProvider();
        ProgressMonitor monitor = provider;
        String task = new String("existing class method");
        monitor.begin(task);
        monitor.worked();
        check(provider.begins == 1 && provider.task == task, "inherited short begin wins");
        check(provider.updates == 1, "inherited short worked wins");
        monitor.begin(null);
        check(provider.begins == 2 && provider.task == null,
                "existing class method retains null handling");
    }

    private static void failureIdentity() {
        for (Throwable failure : new Throwable[] {
                new IllegalArgumentException("provider failure"),
                new AssertionError("provider error") }) {
            Provider begin = new Provider();
            begin.beginFailure = failure;
            expectSame(failure, () -> begin.begin(null));
            check(begin.begins == 1 && begin.task == null
                    && begin.total == ProgressMonitor.UNKNOWN_WORK,
                    "begin failure occurs in the selected full-argument hook");
            events(begin, "begin");

            Provider worked = new Provider();
            worked.workFailure = failure;
            expectSame(failure, worked::worked);
            check(worked.updates == 1 && worked.units == 1L,
                    "worked failure occurs in the selected full-argument hook");
            events(worked, "worked");
        }
    }

    private static void methodReferences() {
        Provider provider = new Provider();
        BiConsumer<String, Long> oldBegin = provider::begin;
        LongConsumer oldWorked = provider::worked;
        String task = new String("known total");
        oldBegin.accept(task, 8L);
        oldWorked.accept(3L);
        check(provider.task == task && provider.total == 8L,
                "existing begin method reference keeps its arguments");
        check(provider.units == 3L && provider.updates == 1,
                "existing worked method reference keeps its argument");
        events(provider, "begin", "worked");

        Consumer<String> newBegin = provider::begin;
        Runnable newWorked = provider::worked;
        newBegin.accept(null);
        newWorked.run();
        check(provider.begins == 2 && provider.task == null
                && provider.total == ProgressMonitor.UNKNOWN_WORK,
                "short begin method reference dispatches the existing hook");
        check(provider.updates == 2 && provider.units == 1L && provider.completed == 4L,
                "short worked method reference dispatches the existing hook");
        events(provider, "begin", "worked", "begin", "worked");
    }

    private static void noOpAndInterruption() {
        for (ProgressMonitor monitor : new ProgressMonitor[] {
                ProgressMonitor.NONE, new ProgressMonitor() { }, new Provider() }) {
            Thread.currentThread().interrupt();
            try {
                monitor.begin(null);
                monitor.worked();
                check(Thread.currentThread().isInterrupted(),
                        "convenience operations retain interruption");
                if (monitor instanceof Provider provider) {
                    check(provider.begins == 1 && provider.updates == 1,
                            "interrupted caller still reaches existing hooks");
                    events(provider, "begin", "worked");
                }
            } finally {
                Thread.interrupted();
            }
        }
        check(!Thread.currentThread().isInterrupted(), "test restores interruption");
    }

    private static void weightedChildren() {
        Provider shortParent = new Provider();
        Provider fullParent = new Provider();
        ProgressMonitor shortChild = shortParent.child(null, 10L, 3L);
        ProgressMonitor fullChild = fullParent.child(null, 10L, 3L);
        shortChild.begin(null);
        fullChild.begin(null, ProgressMonitor.UNKNOWN_WORK);
        events(shortParent);
        events(fullParent);
        for (long expected : new long[] { 3L, 6L, 10L, 10L }) {
            shortChild.worked();
            fullChild.worked(1L);
            check(shortParent.completed == expected, "weighted single-unit credit");
            check(shortParent.completed == fullParent.completed
                    && shortParent.updates == fullParent.updates,
                    "short and full forms have identical weighted accounting");
            check(shortParent.events.equals(fullParent.events),
                    "short and full forms have identical parent callbacks");
        }
        shortChild.done();
        fullChild.done();
        shortChild.worked();
        fullChild.worked(1L);
        check(shortParent.completed == 10L && fullParent.completed == 10L,
                "completion and later updates retain the parent budget");
        events(shortParent, "worked", "worked", "worked");
        events(fullParent, "worked", "worked", "worked");
    }

    private static void unknownAndZeroBudgets() {
        Provider unknownParent = new Provider();
        ProgressMonitor unknown = unknownParent.child(null, 7L, ProgressMonitor.UNKNOWN_WORK);
        unknown.begin("unknown");
        unknown.worked();
        unknown.worked();
        check(unknownParent.completed == 0L, "unknown total defers credit");
        events(unknownParent);
        unknown.done();
        unknown.done();
        unknown.worked();
        check(unknownParent.completed == 7L && unknownParent.updates == 1,
                "unknown total is credited once at completion");
        events(unknownParent, "worked");

        Provider zeroParent = new Provider();
        ProgressMonitor zero = zeroParent.child(null, 0L, 2L);
        zero.worked();
        zero.done();
        check(zeroParent.completed == 0L, "zero parent budget remains zero");
        events(zeroParent);
        check(ProgressMonitor.NONE.child(null, 7L, 2L) == ProgressMonitor.NONE,
                "no-op child retains the no-op owner");
    }

    @SuppressWarnings("try")
    private static void borrowedScope() {
        ProgressMonitor previous = ProgressMonitors.current();
        Provider borrowed = new Provider();
        String task = new String("borrowed scope");
        try (ProgressMonitors.Scope scope = ProgressMonitors.bind(borrowed)) {
            check(ProgressMonitors.current() == borrowed, "scope exposes the borrowed monitor");
            ProgressMonitors.current().begin(task);
            ProgressMonitors.current().worked();
            check(borrowed.task == task && borrowed.total == ProgressMonitor.UNKNOWN_WORK
                    && borrowed.updates == 1 && borrowed.completed == 1L,
                    "scoped convenience calls reach the borrowed provider");
            events(borrowed, "begin", "worked");
        }
        check(ProgressMonitors.current() == previous, "scope restores the previous monitor");
        events(borrowed, "begin", "worked");
    }

    private static void propagate(Throwable failure) {
        if (failure instanceof RuntimeException exception) {
            throw exception;
        }
        if (failure instanceof Error error) {
            throw error;
        }
        if (failure != null) {
            throw new AssertionError("unsupported test failure", failure);
        }
    }

    private static void expectSame(Throwable expected, Runnable operation) {
        try {
            operation.run();
        } catch (Throwable actual) {
            check(actual == expected, "provider failure identity");
            return;
        }
        throw new AssertionError("provider failure was not propagated");
    }

    private static void events(Provider provider, String... expected) {
        check(provider.events.equals(List.of(expected)),
                "callback sequence: " + provider.events);
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
