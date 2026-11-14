// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.chrome;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** Tests the actual runner interface/request/result at the pinned PR head. No native/GPU claim. */
public final class ChromeMlContextAdvisorSelfTest {
    private static final AtomicInteger ASSERTIONS = new AtomicInteger();
    private ChromeMlContextAdvisorSelfTest() { }
    @FunctionalInterface private interface Checked { void run() throws Exception; }
    private static final ChromeMlContextAdvisor.Access ALLOW = packet -> { };
    private static final ChromeMlContextAdvisor.Limits LIMITS = ChromeMlContextAdvisor.Limits.standard();
    public static void main(String[] args) throws Exception {
        group("projection allowlist, ownership, scalar bounds and redaction", ChromeMlContextAdvisorSelfTest::projection);
        group("consent, successful native completion and immutable proposals", ChromeMlContextAdvisorSelfTest::success);
        group("failed native output, swallowed stream errors and diagnostics", ChromeMlContextAdvisorSelfTest::failures);
        group("cooperative deadlines, revocation, interruption and close", ChromeMlContextAdvisorSelfTest::lifecycle);
        group("single flight, no queues, deterministic requests and no cache", ChromeMlContextAdvisorSelfTest::concurrency);
        System.out.println("PASS advisor assertions=" + ASSERTIONS.get() + " java=" + Runtime.version());
        System.out.println("native/GPU/Google-OAuth=NOT_EXECUTED; fake runner at actual production interface");
    }
    private static void group(String name, Checked test) throws Exception { test.run(); System.out.println("PASS " + name); }
    private static ChromeMlContextPacket packet() {
        return ChromeMlContextPacket.capture("context-id", 7, Map.of("topic", "COP", "token", "NEVER_EXPOSE"), Set.of("topic"), List.of());
    }
    private static ChromeMlRunnerResult result(String text) { return new ChromeMlRunnerResult(text, 1, "private stdout", "private stderr"); }
    private static void projection() throws Exception {
        Map<String, Object> source = new HashMap<>(); source.put("topic", "original");
        var first = ChromeMlContextPacket.capture("id", 0, source, Set.of("topic"), List.of());
        source.put("topic", "changed");
        check(first.encoded().contains("original") && !first.encoded().contains("changed"));
        check(!first.toString().contains("original"));
        fails(UnsupportedOperationException.class, () -> first.fields().clear());
        fails(UnsupportedOperationException.class, () -> first.evidenceKeys().clear());
        var guarded = new AbstractMap<String, Object>() {
            @Override public Set<Entry<String, Object>> entrySet() { throw new AssertionError("Visited private entries"); }
            @Override public boolean containsKey(Object key) { check("topic".equals(key)); return true; }
            @Override public Object get(Object key) { check("topic".equals(key)); return "public"; }
        };
        check(ChromeMlContextPacket.capture("id", 0, guarded, Set.of("topic"), List.of()).encoded().contains("public"));
        Map<String, Object> nulls = new HashMap<>(); nulls.put("topic", null);
        var absent = ChromeMlContextPacket.capture("id", 0, Map.of(), Set.of("topic"), List.of());
        var explicitNull = ChromeMlContextPacket.capture("id", 0, nulls, Set.of("topic"), List.of());
        check(!absent.root().equals(explicitNull.root()));
        Map<String, Object> values = Map.of("a", "é😀", "b", true, "c", 1L, "d", new BigDecimal("1.20"));
        check(ChromeMlContextPacket.capture("id", 1, values, Set.of("d", "a", "b", "c"), List.of()).root()
                .equals(ChromeMlContextPacket.capture("id", 1, new java.util.TreeMap<>(values), Set.of("a", "b", "c", "d"), List.of()).root()));
        check(!ChromeMlContextPacket.capture("id", 2, values, values.keySet(), List.of()).root()
                .equals(ChromeMlContextPacket.capture("id", 1, values, values.keySet(), List.of()).root()));
        var key = new ChromeMlContextPacket.EvidenceKey(ChromeMlContextPacket.Source.GMAIL, "message-private");
        var evidence = new ChromeMlContextPacket.Evidence(key, "private-mail-body");
        var projected = ChromeMlContextPacket.capture("id", 0, Map.of(), Set.of(), List.of(evidence));
        check(projected.evidenceKeys().contains(key) && projected.encoded().contains("private-mail-body"));
        check(!evidence.toString().contains("private-mail-body") && !key.toString().contains("message-private"));
        fails(IllegalArgumentException.class, () -> ChromeMlContextPacket.capture("id", 0, Map.of(), Set.of(), List.of(evidence, evidence)));
        for (Object invalid : List.of(Double.NaN, Float.POSITIVE_INFINITY, new ArrayList<>(), new AtomicInteger(1),
                BigInteger.ONE.shiftLeft(1000), new BigDecimal("1e9999"), "a\0b", "\ud800", "a".repeat(4097))) {
            fails(IllegalArgumentException.class, () -> ChromeMlContextPacket.capture("id", 0, Map.of("x", invalid), Set.of("x"), List.of()));
        }
        for (Object valid : List.of((byte) 1, (short) 2, 3, 4L, 1.25f, 2.5d, BigInteger.TEN, new BigDecimal("1.2"), true, "😀"))
            check(ChromeMlContextPacket.capture("id", 0, Map.of("x", valid), Set.of("x"), List.of()).root().length() == 64);
        fails(IllegalArgumentException.class, () -> ChromeMlContextPacket.capture("id", -1, Map.of(), Set.of(), List.of()));
        fails(IllegalArgumentException.class, () -> ChromeMlContextPacket.capture(" ", 0, Map.of(), Set.of(), List.of()));
        fails(IllegalArgumentException.class, () -> ChromeMlContextPacket.capture("id", 0, Map.of(), Set.of("../private"), List.of()));
        Map<String, Object> large = new java.util.TreeMap<>();
        for (int i = 0; i < 6; i++) large.put("k" + i, "x".repeat(4096));
        fails(IllegalArgumentException.class, () -> ChromeMlContextPacket.capture("id", 0, large, large.keySet(), List.of()));
        check(ChromeMlContextPacket.hash("abc").equals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"));
    }
    private static void success() throws Exception {
        AtomicInteger calls = new AtomicInteger(), checks = new AtomicInteger();
        ChromeMlRunner runner = (request, sink) -> {
            calls.incrementAndGet();
            check(!request.prompt().contains("NEVER_EXPOSE"));
            check(request.prompt().contains("COP") && request.prompt().contains("host-task"));
            check(request.imagePaths().isEmpty() && request.audioPath() == null && !request.noSafetyReserve());
            check(request.maxOutput() == LIMITS.outputTokens());
            sink.accept("candidate"); return result("candidate");
        };
        var advisor = new ChromeMlContextAdvisor(runner, LIMITS);
        var suggestion = advisor.advise(packet(), "Suggest a ranking", p -> checks.incrementAndGet());
        check(calls.get() == 1 && checks.get() >= 3);
        check(suggestion.text().equals("candidate") && !suggestion.executionAuthority());
        check(!suggestion.toString().contains("candidate"));
        check(suggestion.contextRevision() == 7 && suggestion.contextRoot().equals(packet().root()));
        check(suggestion.responseRoot().equals(ChromeMlContextPacket.hash("candidate")));
        int oldChecks = checks.get(); suggestion.requireCurrentAccess(); check(checks.get() == oldChecks + 1);
        fails(SecurityException.class, () -> advisor.advise(packet(), "task", p -> { throw new SecurityException("NO_CONSENT"); }));
        check(calls.get() == 1);
        var key = new ChromeMlContextPacket.EvidenceKey(ChromeMlContextPacket.Source.DRIVE, "doc");
        var evidence = ChromeMlContextPacket.capture("id", 0, Map.of(), Set.of(), List.of(new ChromeMlContextPacket.Evidence(key, "body")));
        fails(SecurityException.class, () -> advisor.advise(evidence, "task", p -> {
            if (!p.evidenceKeys().isEmpty()) throw new SecurityException("CONNECTOR_CONSENT_REQUIRED");
        }));
        check(calls.get() == 1);
    }
    private static void failures() throws Exception {
        for (Integer status : new Integer[]{null, -1, 0, 2, 9}) {
            var advisor = new ChromeMlContextAdvisor((r, s) -> new ChromeMlRunnerResult("candidate", status, "", ""), LIMITS);
            fails(IOException.class, () -> advisor.advise(packet(), "task", ALLOW));
        }
        for (String text : List.of("", " ", "a".repeat(8193), "é".repeat(5000), "\ud800", "a\0b")) {
            var advisor = new ChromeMlContextAdvisor((r, s) -> result(text), LIMITS);
            fails(Exception.class, () -> advisor.advise(packet(), "task", ALLOW));
        }
        var nullResult = new ChromeMlContextAdvisor((r, s) -> null, LIMITS);
        fails(IOException.class, () -> nullResult.advise(packet(), "task", ALLOW));
        var stream = new ChromeMlContextAdvisor((r, sink) -> {
            try { sink.accept("x".repeat(5000)); sink.accept("x".repeat(5000)); }
            catch (IOException deliberatelySwallowed) { /* The advisor must retain rejection. */ }
            return result("candidate");
        }, LIMITS);
        fails(IOException.class, () -> stream.advise(packet(), "task", ALLOW));
        var diagnostics = new ChromeMlContextAdvisor((r, s) -> { throw new IOException("SECRET=" + r.prompt()); }, LIMITS);
        try { diagnostics.advise(packet(), "private instruction", ALLOW); throw new AssertionError(); }
        catch (IOException expected) {
            check(expected.getCause() == null && !expected.getMessage().contains("SECRET"));
        }
        AtomicInteger calls = new AtomicInteger();
        var bounded = new ChromeMlContextAdvisor((r, s) -> { calls.incrementAndGet(); return result("ok"); }, LIMITS);
        fails(IllegalArgumentException.class, () -> bounded.advise(packet(), "é".repeat(4096), ALLOW));
        check(calls.get() == 0);
        fails(IllegalArgumentException.class, () -> bounded.advise(packet(), "\ud800", ALLOW));
        check(bounded.advise(packet(), "task", ALLOW).text().equals("ok"));
    }
    private static void lifecycle() throws Exception {
        AtomicLong time = new AtomicLong();
        var advisor = new ChromeMlContextAdvisor((r, sink) -> { time.set(Duration.ofMinutes(3).toNanos()); return result("late"); }, LIMITS, time::get);
        fails(IOException.class, () -> advisor.advise(packet(), "task", ALLOW));
        AtomicBoolean consent = new AtomicBoolean(true);
        ChromeMlContextAdvisor.Access access = p -> { if (!consent.get()) throw new SecurityException("REVOKED"); };
        var revoked = new ChromeMlContextAdvisor((r, s) -> { consent.set(false); return result("stale"); }, LIMITS);
        fails(SecurityException.class, () -> revoked.advise(packet(), "task", access));
        consent.set(true);
        var recheck = new ChromeMlContextAdvisor((r, s) -> result("proposal"), LIMITS);
        var suggestion = recheck.advise(packet(), "task", access);
        consent.set(false); fails(SecurityException.class, suggestion::requireCurrentAccess);
        AtomicLong revision = new AtomicLong(7);
        var stale = new ChromeMlContextAdvisor((r, s) -> { revision.incrementAndGet(); return result("stale"); }, LIMITS);
        fails(SecurityException.class, () -> stale.advise(packet(), "task", p -> {
            if (revision.get() != p.revision()) throw new SecurityException("STALE");
        }));
        AtomicInteger calls = new AtomicInteger();
        var closed = new ChromeMlContextAdvisor((r, s) -> { calls.incrementAndGet(); return result("ok"); }, LIMITS);
        closed.close(); fails(IOException.class, () -> closed.advise(packet(), "task", ALLOW)); check(calls.get() == 0);
        var interrupted = new ChromeMlContextAdvisor((r, s) -> { throw new InterruptedException(); }, LIMITS);
        try { fails(InterruptedException.class, () -> interrupted.advise(packet(), "task", ALLOW)); check(Thread.currentThread().isInterrupted()); }
        finally { Thread.interrupted(); }
        Thread.currentThread().interrupt();
        try { fails(IOException.class, () -> recheck.advise(packet(), "task", ALLOW)); check(Thread.currentThread().isInterrupted()); }
        finally { Thread.interrupted(); }
        for (Duration invalid : List.of(Duration.ZERO, Duration.ofMillis(-1), Duration.ofMinutes(6)))
            fails(IllegalArgumentException.class, () -> new ChromeMlContextAdvisor.Limits(8192, 8192, 256, invalid));
    }
    private static void concurrency() throws Exception {
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        var advisor = new ChromeMlContextAdvisor((r, s) -> {
            calls.incrementAndGet(); entered.countDown();
            if (!release.await(5, TimeUnit.SECONDS)) throw new IOException("test timeout");
            return result("ok");
        }, LIMITS);
        try (var threads = Executors.newVirtualThreadPerTaskExecutor()) {
            var task = threads.submit(() -> advisor.advise(packet(), "task", ALLOW));
            check(entered.await(5, TimeUnit.SECONDS));
            fails(IllegalStateException.class, () -> advisor.advise(packet(), "task", ALLOW));
            check(calls.get() == 1);
            advisor.close(); release.countDown();
            fails(java.util.concurrent.ExecutionException.class, () -> task.get(5, TimeUnit.SECONDS));
        } finally { release.countDown(); }
        AtomicInteger invoked = new AtomicInteger();
        var reused = new ChromeMlContextAdvisor((r, s) -> { invoked.incrementAndGet(); return result("ok"); }, LIMITS);
        String first = reused.advise(packet(), "same", ALLOW).requestRoot();
        for (int i = 0; i < 64; i++) check(reused.advise(packet(), "same", ALLOW).requestRoot().equals(first));
        check(invoked.get() == 65); // No hidden result cache crossing invocations/consent epochs.
        check(!reused.advise(packet(), "different", ALLOW).requestRoot().equals(first));
    }
    private static void check(boolean value) { int n = ASSERTIONS.incrementAndGet(); if (!value) throw new AssertionError("Assertion " + n); }
    private static void fails(Class<? extends Throwable> type, Checked test) throws Exception {
        ASSERTIONS.incrementAndGet();
        try { test.run(); } catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("Expected " + type.getSimpleName() + ", got " + failure, failure);
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }
}
