// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
// Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
package com.m3.llm.chrome;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** Exercises actual Java owners with a deterministic runner fixture, not real Nano inference. */
public final class ChromeMlReferenceSelectionSelfTest {
    private static int checks;
    private static final ChromeMlContextAdvisor.Access ALLOW = p -> { };
    private static final ChromeMlContextAdvisor.Limits DEFAULT = ChromeMlContextAdvisor.Limits.standard();
    private ChromeMlReferenceSelectionSelfTest() { }
    @FunctionalInterface private interface Checked { void run() throws Exception; }
    private static Method api() throws NoSuchMethodException {
        return ChromeMlContextAdvisor.class.getMethod("selectEvidence", ChromeMlContextPacket.class,
                String.class, ChromeMlContextAdvisor.Access.class, int.class);
    }
    private static Object invoke(Method m, Object owner, Object... args) throws Exception {
        try { return m.invoke(owner, args); }
        catch (InvocationTargetException failure) {
            if (failure.getCause() instanceof Exception ex) throw ex;
            if (failure.getCause() instanceof Error ex) throw ex;
            throw new AssertionError(failure.getCause());
        }
    }
    private static Object select(ChromeMlContextAdvisor advisor, ChromeMlContextPacket p, String task,
                                 ChromeMlContextAdvisor.Access access, int bytes) throws Exception {
        return invoke(api(), advisor, p, task, access, bytes);
    }
    private static Object value(Object selection, String method) throws Exception {
        return invoke(selection.getClass().getMethod(method), selection);
    }
    private static Object at(Object selection, String method, int index) throws Exception {
        return invoke(selection.getClass().getMethod(method, int.class), selection, index);
    }
    private static ChromeMlContextPacket.Evidence evidence(String id, String text) {
        return new ChromeMlContextPacket.Evidence(
                new ChromeMlContextPacket.EvidenceKey(ChromeMlContextPacket.Source.HOST, id), text);
    }
    private static List<ChromeMlContextPacket.Evidence> records() {
        return List.of(evidence("id:00", "MUST NOT delete 7 atoms\r\nreturn a + b;  \n"),
                evidence("id:01", "naïve 数据 🧭"), evidence("id:02", "3:foo; 0:;\n```java\nreturn false;\n```"));
    }
    private static ChromeMlContextPacket packet(List<ChromeMlContextPacket.Evidence> rows) {
        return ChromeMlContextPacket.capture("context:private", 7,
                Map.of("task", "REVIEW", "secret", "NEVER_SENT"), Set.of("task"), rows);
    }
    private static ChromeMlRunnerResult result(String text) { return new ChromeMlRunnerResult(text, 1, "SECRET stdout", "SECRET stderr"); }
    private static ChromeMlContextAdvisor advisor(String reply) {
        return new ChromeMlContextAdvisor((request, sink) -> result(reply), DEFAULT);
    }
    private static int bytes(String text) { return text.getBytes(StandardCharsets.UTF_8).length; }
    private static int evidenceBytes(ChromeMlContextPacket.Evidence e) {
        return bytes(e.key().source().name()) + bytes(e.key().id()) + bytes(e.excerpt());
    }
    private static void check(boolean ok) { checks++; if (!ok) throw new AssertionError("check " + checks); }
    private static void fails(Class<? extends Throwable> type, String message, Checked run) throws Exception {
        checks++;
        try { run.run(); } catch (Throwable error) {
            if (type.isInstance(error) && (message == null || String.valueOf(error.getMessage()).contains(message))) return;
            throw new AssertionError("expected " + type + ":" + message + " got " + error, error);
        }
        throw new AssertionError("missing refusal " + message);
    }
    private static void successfulSelection() throws Exception {
        List<ChromeMlContextPacket.Evidence> rows = records();
        var caller = new ArrayList<>(rows); Collections.reverse(caller);
        var p = packet(caller); caller.clear(); String root = p.root();
        AtomicInteger calls = new AtomicInteger(), access = new AtomicInteger();
        var advisor = new ChromeMlContextAdvisor((request, sink) -> {
            calls.incrementAndGet(); String prompt = request.prompt();
            for (var e : rows) check(prompt.contains(e.excerpt()));
            check(!prompt.contains("NEVER_SENT")); check(!prompt.contains("id:00"));
            check(prompt.contains("REVIEW") && prompt.contains("[2,0]"));
            check(request.maxOutput() == DEFAULT.outputTokens());
            check(request.contextTokens() == null && !request.noSafetyReserve());
            sink.accept("[2,"); sink.accept("0]"); return result("[2,0]");
        }, DEFAULT);
        Object s = select(advisor, p, "Select the records", ignored -> access.incrementAndGet(), 65536);
        check(calls.get() == 1 && access.get() >= 3);
        check(value(s, "size").equals(2)); check(value(s, "executionAuthority").equals(false));
        check(value(s, "contextRoot").equals(root)); check(value(s, "contextRevision").equals(7L));
        check(at(s, "sourceOrdinalAt", 0).equals(2)); check(at(s, "sourceOrdinalAt", 1).equals(0));
        check(at(s, "evidenceAt", 0).equals(rows.get(2))); check(at(s, "evidenceAt", 1).equals(rows.get(0)));
        check(value(s, "responseRoot").equals(ChromeMlContextPacket.hash("[2,0]")));
        check(value(s, "evidenceBytes").equals(evidenceBytes(rows.get(2)) + evidenceBytes(rows.get(0))));
        check(p.root().equals(root) && p.evidenceKeys().size() == 3);
        check(!s.toString().contains("id:00") && !s.toString().contains("return"));
        int prior = access.get(); value(s, "requireCurrentAccess"); check(access.get() > prior);
        fails(IndexOutOfBoundsException.class, null, () -> at(s, "sourceOrdinalAt", -1));
        fails(IndexOutOfBoundsException.class, null, () -> at(s, "evidenceAt", 2));
    }
    private static void grammarAndBudgets() throws Exception {
        var p = packet(records());
        for (String raw : List.of("[0,0]", "[-1]", "[-0]", "[3]", "[0.0]", "[1e0]", "[00]", "[true]",
                "[\"0\"]", "[null]", "{}", "[0,]", "[,0]", "[0", "[0]more", "```json\n[0]\n```",
                "[99999999999999999999]", "[٠]", "[０]", "\u00a0[0]", "[0]\u00a0", "[+0]", "[[0]]")) {
            fails(IOException.class, "ADVISOR_REFERENCE_FORMAT", () -> select(advisor(raw), p, "task", ALLOW, 65536));
        }
        fails(IOException.class, "ADVISOR_NO_REFERENCES", () -> select(advisor("[ ]"), p, "task", ALLOW, 65536));
        for (String raw : List.of("[0]", " \t\r\n[ 0 , 2 ]\n", "[2,1,0]")) {
            check(value(select(advisor(raw), p, "task", ALLOW, 65536), "executionAuthority").equals(false));
        }
        var small = new ChromeMlContextAdvisor((r, s) -> result("[0]"),
                new ChromeMlContextAdvisor.Limits(8192, 3, 128, Duration.ofSeconds(10)));
        int payload = evidenceBytes(records().getFirst());
        check(value(select(small, p, "task", ALLOW, payload), "evidenceBytes").equals(payload));
        fails(IOException.class, "ADVISOR_EVIDENCE_LIMIT", () -> select(small, p, "task", ALLOW, payload - 1));
        fails(IOException.class, "ADVISOR_OUTPUT_LIMIT_OR_EMPTY", () -> select(advisor(" ".repeat(510) + "[0]"), p, "task", ALLOW, 65536));
        for (int invalid : new int[]{0, -1, 65537, Integer.MAX_VALUE})
            fails(IllegalArgumentException.class, null, () -> select(advisor("[0]"), p, "task", ALLOW, invalid));
        AtomicInteger calls = new AtomicInteger();
        var empty = new ChromeMlContextAdvisor((r, s) -> { calls.incrementAndGet(); return result("[0]"); }, DEFAULT);
        fails(IllegalArgumentException.class, "ADVISOR_NO_EVIDENCE", () -> select(empty, packet(List.of()), "task", ALLOW, 65536));
        fails(IllegalArgumentException.class, null, () -> select(empty, p, "é".repeat(4096), ALLOW, 65536));
        check(calls.get() == 0);
        check(value(select(empty, p, "task", ALLOW, 65536), "size").equals(1));
    }
    private static void lifecycle() throws Exception {
        var p = packet(records());
        AtomicInteger calls = new AtomicInteger();
        var a = new ChromeMlContextAdvisor((r, s) -> { calls.incrementAndGet(); return result("[0]"); }, DEFAULT);
        fails(SecurityException.class, null, () -> select(a, p, "task", ignored -> { throw new SecurityException("denied"); }, 65536));
        check(calls.get() == 0);
        for (Integer status : new Integer[]{null, -1, 0, 2, 9}) {
            var bad = new ChromeMlContextAdvisor((r, s) -> new ChromeMlRunnerResult("[0]", status, "secret", "secret"), DEFAULT);
            fails(IOException.class, "ADVISOR_NATIVE_INCOMPLETE", () -> select(bad, p, "task", ALLOW, 65536));
        }
        var none = new ChromeMlContextAdvisor((r, s) -> null, DEFAULT);
        fails(IOException.class, "ADVISOR_NATIVE_INCOMPLETE", () -> select(none, p, "task", ALLOW, 65536));
        var swallowed = new ChromeMlContextAdvisor((r, sink) -> {
            try { sink.accept(" ".repeat(513)); } catch (IOException deliberatelySwallowed) { }
            return result("[0]");
        }, DEFAULT);
        fails(IOException.class, "ADVISOR_STREAM_REJECTED", () -> select(swallowed, p, "task", ALLOW, 65536));
        var diagnostic = new ChromeMlContextAdvisor((r, s) -> { throw new IOException("SECRET " + r.prompt()); }, DEFAULT);
        try { select(diagnostic, p, "task", ALLOW, 65536); throw new AssertionError(); }
        catch (IOException expected) { check(expected.getCause() == null); check(expected.getMessage().equals("ADVISOR_RUNNER_FAILED")); }
        AtomicBoolean allowed = new AtomicBoolean(true);
        ChromeMlContextAdvisor.Access access = ignored -> { if (!allowed.get()) throw new SecurityException("revoked"); };
        Object saved = select(a, p, "task", access, 65536);
        allowed.set(false);
        fails(SecurityException.class, null, () -> at(saved, "evidenceAt", 0));
        fails(SecurityException.class, null, () -> value(saved, "requireCurrentAccess"));
        allowed.set(true);
        var revoked = new ChromeMlContextAdvisor((r, s) -> { allowed.set(false); return result("[0]"); }, DEFAULT);
        fails(SecurityException.class, null, () -> select(revoked, p, "task", access, 65536));
        AtomicLong time = new AtomicLong();
        var late = new ChromeMlContextAdvisor((r, s) -> { time.set(Duration.ofMinutes(3).toNanos()); return result("[0]"); }, DEFAULT, time::get);
        fails(IOException.class, "ADVISOR_DEADLINE", () -> select(late, p, "task", ALLOW, 65536));
        a.close(); fails(IOException.class, "ADVISOR_CLOSED", () -> select(a, p, "task", ALLOW, 65536));
        var interrupted = new ChromeMlContextAdvisor((r, s) -> { throw new InterruptedException(); }, DEFAULT);
        try { fails(InterruptedException.class, null, () -> select(interrupted, p, "task", ALLOW, 65536)); check(Thread.currentThread().isInterrupted()); }
        finally { Thread.interrupted(); }
    }
    private static void rootsAndPermutations() throws Exception {
        Random random = new Random(0x4d334a415641L);
        for (int trial = 0; trial < 256; trial++) {
            int count = 1 + random.nextInt(16);
            List<ChromeMlContextPacket.Evidence> rows = new ArrayList<>();
            for (int i = 0; i < count; i++) rows.add(evidence(String.format(java.util.Locale.ROOT, "id:%02d", i),
                    "MUST NOT " + i + " 😀\r\n  "));
            var p = packet(rows); Collections.shuffle(rows, random); var shuffled = packet(rows);
            check(p.root().equals(shuffled.root()));
            List<Integer> indices = new ArrayList<>(); for (int i = 0; i < count; i++) indices.add(i);
            Collections.shuffle(indices, random); indices = indices.subList(0, 1 + random.nextInt(count));
            String raw = indices.toString(); var a = advisor(raw);
            Object s = select(a, p, "task", ALLOW, 65536), t = select(a, shuffled, "task", ALLOW, 65536);
            check(value(s, "selectionRoot").equals(value(t, "selectionRoot")));
            for (int i = 0; i < indices.size(); i++) {
                check(at(s, "sourceOrdinalAt", i).equals(indices.get(i)));
                var e = (ChromeMlContextPacket.Evidence) at(s, "evidenceAt", i);
                check(e.key().id().equals(String.format(java.util.Locale.ROOT, "id:%02d", indices.get(i))));
            }
        }
        var p = packet(records()); var a = advisor("[0]"); Object s = select(a, p, "task", ALLOW, 65536);
        Object task = select(a, p, "other", ALLOW, 65536);
        check(!value(s, "requestRoot").equals(value(task, "requestRoot")));
        var changed = ChromeMlContextPacket.capture("context:private", 8, Map.of("task", "REVIEW"), Set.of("task"), records());
        check(!value(s, "selectionRoot").equals(value(select(a, changed, "task", ALLOW, 65536), "selectionRoot")));
        var otherId = packet(List.of(evidence("DIFFERENT", records().getFirst().excerpt())));
        var one = packet(List.of(records().getFirst()));
        check(!value(select(a, one, "task", ALLOW, 65536), "requestRoot")
                .equals(value(select(a, otherId, "task", ALLOW, 65536), "requestRoot")));
        check(!value(s, "selectionRoot").equals(value(select(advisor(" [0] "), p, "task", ALLOW, 65536), "selectionRoot")));
    }
    private static void singleFlight() throws Exception {
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        var p = packet(records());
        var a = new ChromeMlContextAdvisor((r, sink) -> { entered.countDown();
            if (!release.await(5, TimeUnit.SECONDS)) throw new IOException("test timeout"); return result("[0]"); }, DEFAULT);
        try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
            var future = pool.submit(() -> select(a, p, "task", ALLOW, 65536));
            check(entered.await(5, TimeUnit.SECONDS));
            fails(IllegalStateException.class, "ADVISOR_BUSY", () -> select(a, p, "task", ALLOW, 65536));
            fails(IllegalStateException.class, "ADVISOR_BUSY", () -> a.advise(p, "legacy", ALLOW));
            release.countDown(); check(value(future.get(5, TimeUnit.SECONDS), "size").equals(1));
        } finally { release.countDown(); }
        List<ChromeMlContextPacket.Evidence> rows = new ArrayList<>();
        for (int i = 0; i < 16; i++) rows.add(evidence(String.format(java.util.Locale.ROOT, "id:%02d", i), "code atom " + i + " ".repeat(100)));
        String raw = "[0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15]";
        Object selection = select(advisor(raw), packet(rows), "task", ALLOW, 65536);
        System.out.println("WORK_WITNESS replyUtf8Bytes=" + bytes(raw) + " exactEvidenceUtf8Bytes=" + value(selection, "evidenceBytes"));
        check(value(selection, "size").equals(16));
    }
    public static void main(String[] args) throws Exception {
        try { api(); }
        catch (NoSuchMethodException missing) { throw new AssertionError("MISSING_JAVA_REFERENCE_SELECTION", missing); }
        successfulSelection(); grammarAndBudgets(); lifecycle(); rootsAndPermutations(); singleFlight();
        System.out.println("PASS reference-selection assertions=" + checks + " seededTrials=256");
        System.out.println("actualJavaOwners=true; runner=fixture; liveChromeML/JNIInference=NOT_EXECUTED");
    }
}
