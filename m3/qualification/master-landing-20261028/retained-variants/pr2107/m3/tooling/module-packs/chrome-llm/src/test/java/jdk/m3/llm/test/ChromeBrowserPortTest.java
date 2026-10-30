/*
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: Apache-2.0
 * Standalone bridge regression fixtures; no browser/model inference is performed.
 */
package jdk.m3.llm.test;

import jdk.m3.llm.chrome.ChromeBrowserPort;
import jdk.m3.llm.chrome.ChromeProtocol;
import jdk.m3.llm.chrome.MechanicalModel.ChromeRequest;
import jdk.m3.llm.chrome.MechanicalModel.ChromeResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

/** Independent transport fixture; intentionally does not parse using the runtime JSON codec. */
@SuppressWarnings("try")
public final class ChromeBrowserPortTest {
    private static int checks;
    private static final ChromeRequest REQUEST = ChromeProtocol.request("node-π", "source text 📚\n",
            List.of("source:a, b", "source:[x]", "source:\u001fΩ"));
    private static final String OUTPUT = "Candidate 📚\nKeep source.";

    private ChromeBrowserPortTest() {}

    public static void main(String[] args) throws Exception {
        frozenRoots();
        happySessions();
        requestAndBudgetRefusals();
        responseRefusals();
        jsonLexicalRefusals();
        transportFailureAndCleanup();
        concurrency();
        if (args.length == 2) wireInterop(args[0], args[1]);
        System.out.println("ChromeBrowserPortTest PASS: " + checks + " checks; native Chrome inference NOT_EXECUTED");
    }

    private static void frozenRoots() {
        equal("311766686fa50936589f472c91ab632b1eabaf977846e96a22a2feca2dc8fb2e", REQUEST.inputSha256());
        equal("1e32b11bd3213de1658d319ad8897c7e80c6d54506019de1d883438496723fef", REQUEST.requestId());
        ChromeResult result = result(REQUEST, OUTPUT, "MODEL_CANDIDATE", true, false, 1, 0, REQUEST.sourceRefs());
        equal("bd454934b6e17b96b1b9749b00b611a6b00a10ed712a1a5e2fd21687ca80b983", result.resultId());
        String summaryRoot = ChromeBrowserPort.requestPacketId("summarize", "fixture-session", 7, REQUEST);
        equal("f223d91e16ddff94bc581315d8327bff68c82292e2f5b0237886e9dae6bc8a2d", summaryRoot);
        equal("254da60acfbd171786389c5f08618d92518433850ba6d666cf2fe8494161220c",
                ChromeBrowserPort.resultPacketId("summarize", "fixture-session", 7, summaryRoot, result));
        String promptRoot = ChromeBrowserPort.requestPacketId("prompt", "fixture-session", 7, REQUEST);
        equal("2ea63d89f3bb39a8e66b8cc6367e0396b054a931d4483ffdbb680046dd684d3a", promptRoot);
        equal("90af39cb7d6806526276ce7d5a338a9882d816d0967fdf49dce6fc4e7cba8e45",
                ChromeBrowserPort.resultPacketId("prompt", "fixture-session", 7, promptRoot, result));
        var one = ChromeProtocol.request("n", "text", List.of("a, b"));
        var two = ChromeProtocol.request("n", "text", List.of("a", "b"));
        equal(one.requestId(), two.requestId()); // The inherited list display ambiguity is real.
        check(!ChromeBrowserPort.requestPacketId("summarize", "s", 1, one)
                .equals(ChromeBrowserPort.requestPacketId("summarize", "s", 1, two)), "packet must bind reference boundaries");
        expect(IllegalArgumentException.class, () -> ChromeBrowserPort.requestPacketId("write", "s", 1, REQUEST));
        expect(IllegalArgumentException.class, () -> ChromeBrowserPort.requestPacketId("summarize", "invalid id", 1, REQUEST));
        expect(IllegalArgumentException.class, () -> ChromeBrowserPort.requestPacketId("summarize", "s", 0, REQUEST));
    }

    private static void happySessions() throws Exception {
        FakeTransport transport = new FakeTransport();
        var port = new ChromeBrowserPort(transport);
        equal(0, transport.opens);
        var session = port.open();
        expect(IllegalStateException.class, port::open);
        var candidate = session.summarize(REQUEST);
        equal(OUTPUT, candidate.text()); equal(1, candidate.modelCalls());
        equal(false, candidate.admitted()); equal(true, candidate.candidateOnly());
        expect(UnsupportedOperationException.class, () -> candidate.sourceRefs().add("forged"));
        session.close(); session.close();
        equal(1, transport.closes); equal(1, transport.generations);
        expect(IllegalStateException.class, () -> session.summarize(REQUEST));
        try (var prompt = port.openPrompt()) {
            equal(OUTPUT, prompt.prompt(REQUEST).text());
        }
        equal("prompt", transport.operation); equal(2, transport.opens); equal(2, transport.closes);
    }

    private static void requestAndBudgetRefusals() throws Exception {
        FakeTransport transport = new FakeTransport();
        var limits = new ChromeBrowserPort.Limits(64, 64, 3, 100, 1);
        try (var session = new ChromeBrowserPort(transport, limits).open()) {
            var forged = new ChromeRequest(REQUEST.requestId(), REQUEST.nodeId(), "changed", REQUEST.inputSha256(),
                    REQUEST.sourceRefs(), REQUEST.authority(), true, 0, 0);
            expect(IllegalArgumentException.class, () -> session.summarize(forged));
            var escalated = new ChromeRequest(REQUEST.requestId(), REQUEST.nodeId(), REQUEST.input(), REQUEST.inputSha256(),
                    REQUEST.sourceRefs(), "VERIFIED", true, 0, 0);
            expect(IllegalArgumentException.class, () -> session.summarize(escalated));
            expect(IllegalArgumentException.class, () -> session.summarize(ChromeProtocol.request("n", "📚".repeat(17), List.of())));
            expect(IllegalArgumentException.class, () -> session.summarize(ChromeProtocol.request("n", "text", List.of("1", "2", "3", "4"))));
            expect(IllegalArgumentException.class, () -> session.summarize(ChromeProtocol.request("n", "text", List.of("r".repeat(101)))));
            expect(IllegalArgumentException.class, () -> session.summarize(ChromeProtocol.request("n", "\u001f", List.of())));
            equal(0, transport.generations);
            session.summarize(REQUEST);
            expect(IllegalStateException.class, () -> session.summarize(REQUEST));
            equal(1, transport.generations);
        }
        expect(IllegalArgumentException.class, () -> new ChromeBrowserPort.Limits(0, 1, 1, 1, 1));
        expect(IllegalArgumentException.class, () -> new ChromeBrowserPort.Limits(1, 65537, 1, 1, 1));
        FakeTransport output = new FakeTransport();
        output.output = "📚".repeat(17);
        try (var session = new ChromeBrowserPort(output, limits).open()) {
            expect(IllegalArgumentException.class, () -> session.summarize(REQUEST));
        }
        equal(1, output.closes);
    }

    private static void responseRefusals() throws Exception {
        List<UnaryOperator<ChromeResult>> mutations = List.of(
                r -> result(REQUEST, r.text(), "VERIFIED", true, false, 1, 0, r.sourceRefs()),
                r -> result(REQUEST, r.text(), r.authority(), false, false, 1, 0, r.sourceRefs()),
                r -> result(REQUEST, r.text(), r.authority(), true, true, 1, 0, r.sourceRefs()),
                r -> result(REQUEST, r.text(), r.authority(), true, false, 0, 0, r.sourceRefs()),
                r -> result(REQUEST, r.text(), r.authority(), true, false, 2, 0, r.sourceRefs()),
                r -> result(REQUEST, r.text(), r.authority(), true, false, 1, 1, r.sourceRefs()),
                r -> result(REQUEST, "\t", r.authority(), true, false, 1, 0, r.sourceRefs()),
                r -> result(REQUEST, r.text(), r.authority(), true, false, 1, 0, List.of("different")),
                r -> new ChromeResult(r.status(), "0".repeat(64), r.sourceRequestId(), r.sourceRefs(),
                        r.text(), r.authority(), true, false, 1, 0));
        for (var mutate : mutations) {
            FakeTransport transport = new FakeTransport(); transport.resultMutation = mutate;
            try (var session = new ChromeBrowserPort(transport).open()) {
                expect(IllegalArgumentException.class, () -> session.summarize(REQUEST));
                expect(IllegalStateException.class, () -> session.summarize(REQUEST));
            }
            equal(1, transport.closes);
        }
        for (var mutate : List.<UnaryOperator<String>>of(
                wire -> wire.replace("\"sequence\":1", "\"sequence\":2"),
                wire -> wire.replace("\"operation\":\"summarize\"", "\"operation\":\"prompt\""),
                wire -> wire.replace("\"resultPacketId\":\"", "\"resultPacketId\":\"0"),
                wire -> wire.substring(0, wire.length() - 1) + ",\"extra\":true}")) {
            FakeTransport transport = new FakeTransport(); transport.wireMutation = mutate;
            try (var session = new ChromeBrowserPort(transport).open()) {
                expect(IllegalArgumentException.class, () -> session.summarize(REQUEST));
            }
        }
    }

    private static void jsonLexicalRefusals() throws Exception {
        for (var mutate : List.<UnaryOperator<String>>of(
                wire -> " " + wire,
                wire -> wire.replace("\"modelCalls\":1", "\"modelCalls\":1,\"modelCalls\":1"),
                wire -> wire.replace("\"modelCalls\":1", "\"modelCalls\":1e0"),
                wire -> wire.replace("Candidate", "\\u+041"),
                wire -> wire.replace("Candidate", "\\u-001"),
                wire -> wire.replace("Candidate", "\\ud800"),
                wire -> wire.replace("Candidate", "\\udc00"),
                wire -> wire.replace("Candidate", "\\u0043andidate"))) {
            FakeTransport transport = new FakeTransport(); transport.wireMutation = mutate;
            try (var session = new ChromeBrowserPort(transport).open()) {
                expect(IllegalArgumentException.class, () -> session.summarize(REQUEST));
            }
            equal(1, transport.closes);
        }
    }

    private static void transportFailureAndCleanup() throws Exception {
        FakeTransport transport = new FakeTransport(); transport.failExecution = true;
        try (var session = new ChromeBrowserPort(transport).open()) {
            expect(java.io.IOException.class, () -> session.summarize(REQUEST));
            expect(IllegalStateException.class, () -> session.summarize(REQUEST));
        }
        equal(1, transport.closes);
        FakeTransport badOpen = new FakeTransport(); badOpen.invalidOpen = true;
        expect(IllegalArgumentException.class, () -> new ChromeBrowserPort(badOpen).open());
        equal(1, badOpen.closes);
    }

    private static void concurrency() throws Exception {
        FakeTransport transport = new FakeTransport();
        transport.entered = new CountDownLatch(1); transport.release = new CountDownLatch(1);
        var port = new ChromeBrowserPort(transport);
        try (var session = port.open()) {
            AtomicReference<Throwable> problem = new AtomicReference<>();
            Thread worker = new Thread(() -> {
                try { session.summarize(REQUEST); } catch (Throwable error) { problem.set(error); }
            });
            worker.start();
            check(transport.entered.await(5, TimeUnit.SECONDS), "worker did not enter fixture transport");
            try {
                expect(IllegalStateException.class, () -> session.summarize(REQUEST));
                expect(IllegalStateException.class, port::openPrompt);
                expect(IllegalStateException.class, session::close);
            } finally { transport.release.countDown(); }
            worker.join(5000);
            check(!worker.isAlive(), "worker did not finish");
            equal(null, problem.get());
        }
        equal(1, transport.generations); equal(1, transport.closes);
    }

    /** Optional offline Java/Node pipe proof; the product itself never starts a process. */
    private static void wireInterop(String node, String fixturePath) throws Exception {
        Process process = new ProcessBuilder(node, fixturePath, "--java-fixture")
                .redirectError(ProcessBuilder.Redirect.INHERIT).start();
        try (var reader = process.inputReader(java.nio.charset.StandardCharsets.UTF_8);
                var writer = process.outputWriter(java.nio.charset.StandardCharsets.UTF_8)) {
            ChromeBrowserPort port = new ChromeBrowserPort(request -> {
                writer.write(request); writer.newLine(); writer.flush();
                String response = reader.readLine();
                if (response == null) throw new java.io.IOException("Node fixture closed without a response");
                return response;
            });
            try (var session = port.open()) { equal(OUTPUT, session.summarize(REQUEST).text()); }
            try (var prompt = port.openPrompt()) { equal(OUTPUT, prompt.prompt(REQUEST).text()); }
        } finally {
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroyForcibly(); throw new AssertionError("Node fixture did not close");
            }
        }
        equal(0, process.exitValue());
    }

    private static ChromeResult result(ChromeRequest request, String output, String authority, boolean candidate,
            boolean admitted, int modelCalls, int network, List<String> refs) {
        var unsigned = new ChromeResult("CANDIDATE_ONLY", "", request.requestId(), refs, output,
                authority, candidate, admitted, modelCalls, network);
        return new ChromeResult(unsigned.status(), ChromeProtocol.resultId(unsigned), unsigned.sourceRequestId(),
                refs, output, authority, candidate, admitted, modelCalls, network);
    }

    private static final class FakeTransport implements ChromeBrowserPort.Transport {
        int opens, closes, generations, sequence;
        String operation, id, output = OUTPUT;
        boolean failExecution, invalidOpen;
        CountDownLatch entered, release;
        UnaryOperator<ChromeResult> resultMutation = value -> value;
        UnaryOperator<String> wireMutation = value -> value;

        @Override public String exchange(String wire) throws Exception {
            String action = field(wire, "action");
            operation = field(wire, "operation"); id = field(wire, "sessionId");
            sequence = Integer.parseInt(match(wire, "\\\"sequence\\\":([0-9]+)"));
            if (action.equals("open")) {
                opens++; generations = 0;
                String limitJson = wire.substring(wire.indexOf("\"limits\":") + 9, wire.length() - 1);
                return base("opened") + ",\"limits\":" + limitJson + ",\"candidateOnly\":true,\"admitted\":"
                        + invalidOpen + ",\"modelCalls\":0,\"directNetworkCalls\":0}";
            }
            if (action.equals("close")) {
                closes++;
                return base("closed") + ",\"modelCalls\":" + generations + ",\"directNetworkCalls\":0}";
            }
            equal("execute", action);
            check(wire.contains("\"input\":" + quote(REQUEST.input())), "source text was not preserved in transport");
            String root = ChromeBrowserPort.requestPacketId(operation, id, sequence, REQUEST);
            equal(root, field(wire, "packetId"));
            generations++;
            if (entered != null) {
                entered.countDown();
                if (!release.await(5, TimeUnit.SECONDS)) throw new AssertionError("fixture release timed out");
            }
            if (failExecution) throw new java.io.IOException("fixture transport failure");
            ChromeResult result = resultMutation.apply(result(REQUEST, output, "MODEL_CANDIDATE", true, false, 1, 0, REQUEST.sourceRefs()));
            String response = base("result") + ",\"packetId\":" + quote(root) + ",\"resultPacketId\":"
                    + quote(ChromeBrowserPort.resultPacketId(operation, id, sequence, root, result))
                    + ",\"result\":" + resultJson(result) + "}";
            return wireMutation.apply(response);
        }

        private String base(String action) {
            return "{\"protocol\":\"M3_CHROME_BRIDGE_V1\",\"action\":" + quote(action) + ",\"operation\":"
                    + quote(operation) + ",\"sessionId\":" + quote(id) + ",\"sequence\":" + sequence;
        }
    }

    private static String resultJson(ChromeResult r) {
        return "{\"status\":" + quote(r.status()) + ",\"resultId\":" + quote(r.resultId())
                + ",\"sourceRequestId\":" + quote(r.sourceRequestId()) + ",\"sourceRefs\":["
                + String.join(",", r.sourceRefs().stream().map(ChromeBrowserPortTest::quote).toList())
                + "],\"text\":" + quote(r.text()) + ",\"authority\":" + quote(r.authority())
                + ",\"candidateOnly\":" + r.candidateOnly() + ",\"admitted\":" + r.admitted()
                + ",\"modelCalls\":" + r.modelCalls() + ",\"directNetworkCalls\":" + r.directNetworkCalls() + "}";
    }

    private static String quote(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\""); case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n"); case '\r' -> out.append("\\r"); case '\t' -> out.append("\\t");
                case '\b' -> out.append("\\b"); case '\f' -> out.append("\\f");
                default -> { if (c < 32 || c > 126) out.append(String.format(Locale.ROOT, "\\u%04x", (int)c)); else out.append(c); }
            }
        }
        return out.append('"').toString();
    }

    private static String field(String json, String name) { return match(json, "\\\"" + name + "\\\":\\\"([^\\\"]*)\\\""); }
    private static String match(String text, String expression) {
        var matcher = Pattern.compile(expression).matcher(text);
        if (!matcher.find()) throw new AssertionError("fixture field missing: " + expression);
        return matcher.group(1);
    }
    @FunctionalInterface private interface Checked { void run() throws Exception; }
    private static void expect(Class<? extends Throwable> type, Checked action) {
        try { action.run(); } catch (Throwable error) {
            check(type.isInstance(error), "expected " + type.getSimpleName() + ", got " + error);
            return;
        }
        throw new AssertionError("expected " + type.getSimpleName());
    }
    private static void equal(Object expected, Object actual) {
        check(java.util.Objects.equals(expected, actual), "expected " + expected + ", got " + actual);
    }
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
