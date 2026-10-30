// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ProgressMonitor;
import java.util.stream.Collectors;
import jdk.m3.llm.summary.Data;
import jdk.m3.llm.summary.Summaries;
import jdk.m3.llm.summary.Summary;

/** Boundary cancellation, borrowed lifecycle and complete donor-result parity. */
public final class SummariesChecks {
    private static int checks;

    private SummariesChecks() { }

    public static void main(String[] args) {
        options();
        parity();
        lifecycle();
        cancellation();
        failures();
        System.out.println("M3_SUMMARIES_FACADE_PASS checks=" + checks);
    }

    private static void check(boolean accepted, String message) {
        checks++;
        if (!accepted) throw new AssertionError(message);
    }

    private static void throwsType(Class<? extends Throwable> kind, Runnable task) {
        checks++;
        try {
            task.run();
        } catch (Throwable failure) {
            if (kind.isInstance(failure)) return;
            throw new AssertionError("expected " + kind.getName(), failure);
        }
        throw new AssertionError("missing " + kind.getName());
    }

    private static List<Map<String, Object>> records() {
        return List.of(Data.obj("record_id", "r1", "source_uri", "memory:one",
                "profile_id", "test-profile", "content_sha256", Data.sha("one"),
                "title", "Important evidence", "body",
                "Array ownership matters. The result preserves exact evidence. "
                        + "Unicode Straße and café remain available. "
                        + "Perhaps unknown claims need provenance."),
                Data.obj("record_id", "r2", "source_uri", "memory:two",
                        "profile_id", "test-profile", "content_sha256", Data.sha("two"),
                        "title", "Source relations", "body",
                        "Queries use shared evidence. Arrays preserve ownership. "
                                + "The conclusion requires explicit source identity."));
    }

    private static void options() {
        throwsType(IllegalArgumentException.class, () -> new Summaries.Options(255, 1, 1, 2));
        throwsType(IllegalArgumentException.class, () -> new Summaries.Options(256, 0, 1, 2));
        throwsType(IllegalArgumentException.class, () -> new Summaries.Options(256, 1, 0, 2));
        throwsType(IllegalArgumentException.class, () -> new Summaries.Options(256, 1, 1, 1));
        check(new Summaries.Options(256, 1, 1, 2).fanIn() == 2, "minimum options");
        check(Summaries.Options.defaults().equals(new Summaries.Options(4096, 3, 5, 4)),
                "documented defaults");
        Set<String> keys = Arrays.stream(Summaries.Method.values())
                .map(Summaries.Method::scoreKey).collect(Collectors.toSet());
        check(keys.equals(Set.of("luhn", "sumbasic", "edmundson", "lexrank", "query")),
                "existing score keys");
    }

    private static void parity() {
        List<Map<String, Object>> input = records();
        String before = Data.json(input);
        for (Summaries.Options options : List.of(new Summaries.Options(256, 1, 2, 2),
                new Summaries.Options(512, 3, 3, 3), Summaries.Options.defaults())) {
            for (String query : List.of("", "ownership evidence", "Straße", "unknown")) {
                Map<String, Object> expected = Summary.hierarchical(input, query,
                        options.chunkCharacters(), options.leafSentences(),
                        options.parentSentences(), options.fanIn());
                check(expected.equals(Summaries.hierarchical(input, query, options)),
                        "whole output equals donor");
                check(expected.equals(Summaries.hierarchical(input, query, options, null)),
                        "null monitor equals donor");
                for (Map<String, Object> node : Data.rows(expected.get("nodes"))) {
                    Map<String, Object> scores = Data.map(node.get("method_scores"));
                    for (Summaries.Method method : Summaries.Method.values()) {
                        check(scores.containsKey(method.scoreKey()), "existing method score retained");
                    }
                }
            }
        }
        check(before.equals(Data.json(input)), "caller snapshot unchanged");
    }

    private static void lifecycle() {
        Monitor monitor = new Monitor();
        Summaries.hierarchical(records(), "evidence", Summaries.Options.defaults(), monitor);
        check(monitor.units == 2 && monitor.begins == 0 && monitor.completions == 0,
                "borrowed lifecycle and two phase units");
        check(monitor.events.equals(List.of("check", "work:1", "check", "check", "work:1", "check")),
                "cancellation boundaries surround callbacks and algorithm");
    }

    private static void cancellation() {
        List<Map<String, Object>> unreadable = new AbstractList<>() {
            @Override public Map<String, Object> get(int index) {
                throw new AssertionError("cancelled work touched source");
            }
            @Override public int size() {
                throw new AssertionError("cancelled work enumerated source");
            }
        };
        Monitor before = new Monitor();
        before.cancelled = true;
        throwsType(CancellationException.class,
                () -> Summaries.hierarchical(unreadable, "", Summaries.Options.defaults(), before));
        check(before.units == 0 && before.completions == 0, "pre-cancellation stops before source access");
        Monitor atAdmission = new Monitor();
        atAdmission.cancelAtUnit = 1;
        throwsType(CancellationException.class,
                () -> Summaries.hierarchical(unreadable, "", Summaries.Options.defaults(), atAdmission));
        check(atAdmission.units == 1 && atAdmission.completions == 0,
                "first callback cancellation prevents computation");
        Monitor atReturn = new Monitor();
        atReturn.cancelAtUnit = 2;
        throwsType(CancellationException.class,
                () -> Summaries.hierarchical(records(), "", Summaries.Options.defaults(), atReturn));
        check(atReturn.units == 2 && atReturn.completions == 0,
                "completion callback cancellation prevents return");
        Thread.currentThread().interrupt();
        try {
            throwsType(CancellationException.class,
                    () -> Summaries.hierarchical(unreadable, "", Summaries.Options.defaults(), null));
            check(Thread.currentThread().isInterrupted(), "interruption is preserved");
        } finally {
            Thread.interrupted();
        }
    }

    private static void failures() {
        Monitor monitor = new Monitor();
        throwsType(NullPointerException.class,
                () -> Summaries.hierarchical(null, "", Summaries.Options.defaults(), monitor));
        throwsType(NullPointerException.class,
                () -> Summaries.hierarchical(records(), null, Summaries.Options.defaults(), monitor));
        throwsType(NullPointerException.class,
                () -> Summaries.hierarchical(records(), "", null, monitor));
        check(monitor.units == 0 && monitor.completions == 0, "bad arguments do not receive work credit");
        throwsType(IllegalArgumentException.class,
                () -> Summaries.hierarchical(List.of(), "", Summaries.Options.defaults(), monitor));
        check(monitor.units == 1 && monitor.completions == 0, "source failure retains borrowed lifecycle");
        CallbackFailure failure = new CallbackFailure();
        ProgressMonitor throwing = new ProgressMonitor() {
            @Override public void worked(long units) { throw failure; }
        };
        checks++;
        try {
            Summaries.hierarchical(records(), "", Summaries.Options.defaults(), throwing);
            throw new AssertionError("missing callback failure");
        } catch (CallbackFailure actual) {
            check(actual == failure, "callback failure identity preserved");
        }
    }

    private static final class CallbackFailure extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    private static final class Monitor implements ProgressMonitor {
        private final List<String> events = new ArrayList<>();
        private long units;
        private long cancelAtUnit = Long.MAX_VALUE;
        private int begins;
        private int completions;
        private boolean cancelled;

        @Override public void begin(String name, long total) { begins++; }
        @Override public void done() { completions++; }
        @Override public boolean isCancelled() { return cancelled; }
        @Override public void checkCancelled() {
            events.add("check");
            ProgressMonitor.super.checkCancelled();
        }
        @Override public void worked(long additional) {
            units += additional;
            events.add("work:" + additional);
            if (units >= cancelAtUnit) cancelled = true;
        }
    }
}
