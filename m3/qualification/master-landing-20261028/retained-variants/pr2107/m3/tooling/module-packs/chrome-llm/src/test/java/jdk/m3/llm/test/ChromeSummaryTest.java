/*
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: Apache-2.0
 * M3JDK 21 product regression checks for the Synexia Chrome summary donation.
 * A deterministic test provider proves contracts; it does not execute Chrome AI.
 */
package jdk.m3.llm.test;

import jdk.m3.llm.chrome.*;
import jdk.m3.llm.chrome.MechanicalModel.*;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ProgressMonitor;

/** Standalone product test; the recipe host may invoke this same main through JUnit. */
public final class ChromeSummaryTest {
    private static int checks;
    private static final SummaryTreePlanner.TreeConfig CONFIG =
            new SummaryTreePlanner.TreeConfig(1024, 64, 2, 256, 1024, 128);

    public static void main(String[] args) throws Exception {
        frozenDonorRoots();
        exactUnicodeCoverage();
        rejectMalformedUnicode();
        rejectResignedNoncanonicalPlans();
        enforceNodeBudgets();
        serialSessionAndInvalidPlans();
        closeSessionOnRejectedResults();
        cancellation();
        borrowedMonitor();
        mechanicalConservation();
        fixtureUnicodeAndCheckpoint();
        System.out.println("ChromeSummaryTest PASS: " + checks + " checks; native Chrome inference NOT_EXECUTED");
    }

    private static void frozenDonorRoots() throws Exception {
        String source = "MUST keep source.\n".repeat(120) + "\uD83D\uDCDA".repeat(200);
        var plan = SummaryTreePlanner.plan("repo://fixture", source, CONFIG);
        equal("88824c20ecdd12f66cd5be95e8d2d2ae7162803a45bf99efc542f83418c45905", plan.sourceSha256());
        equal("def51626741277b6dc3b05f18455db89810157bec7d9f6a34af9c56429bd3f9d", plan.mechanicalRoot());
        equal(3, plan.leafCount());
        equal(6, plan.nodes().size());
        equal(3, plan.levelCount());
        var request = ChromeProtocol.request("node", "source text", List.of("source:a", "source:b"));
        equal("59bf571018498c25ce9e3feb9d247db8b78cc0f3b5a6c7ca565fd28ea43cfac8", request.requestId());
        var single = SummaryTreePlanner.plan("repo://single", "source text", CONFIG);
        var result = new SummaryTreeExecutor(new DeterministicChromeSummarizerPort()).execute(single, "source text");
        equal("3918f22f0fe18c3f15ef6d3df92689c623b55d5d13149b6749fd5126a0b2ecfd", result.candidateRoot());
    }

    private static void exactUnicodeCoverage() throws Exception {
        for (String source : List.of("a".repeat(1025), "é".repeat(800),
                "a".repeat(1023) + "\uD83D\uDE00x", "a".repeat(1024) + "\nend",
                "\uFEFF" + "नमस्ते\uD83D\uDE00\r\n".repeat(200))) {
            var plan = SummaryTreePlanner.plan("repo://unicode", source, CONFIG);
            check(plan.verify(source), "valid plan rejected");
            byte[] bytes = source.getBytes(StandardCharsets.UTF_8);
            ByteArrayOutputStream restored = new ByteArrayOutputStream();
            int cursor = 0;
            for (var leaf : plan.nodes()) {
                if (!leaf.kind().equals("LEAF")) continue;
                equal(cursor, leaf.startByte());
                int width = leaf.endByte() - leaf.startByte();
                check(width > 0 && width <= CONFIG.maxLeafBytes(), "leaf budget");
                StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(bytes, cursor, width));
                restored.write(bytes, cursor, width);
                cursor = leaf.endByte();
            }
            equal(bytes.length, cursor);
            check(java.util.Arrays.equals(bytes, restored.toByteArray()), "source byte loss");
            equal(plan, SummaryTreePlanner.plan("repo://unicode", source, CONFIG));
        }
    }

    private static void rejectMalformedUnicode() {
        for (String text : List.of("\uD800", "\uDC00", "x\uD800y", "\uD800\uD800")) {
            expect(IllegalArgumentException.class, () -> SummaryTreePlanner.plan("source", text, CONFIG));
            expect(IllegalArgumentException.class, () -> ChromeProtocol.request("node", text, List.of("source")));
        }
        expect(IllegalArgumentException.class, () -> SummaryTreePlanner.plan("bad\uD800", "text", CONFIG));
        check(!SummaryTreePlanner.plan("source", "?", CONFIG).verify("\uD800"), "replacement bytes accepted");
    }

    private static void rejectResignedNoncanonicalPlans() {
        String text = "source";
        var plan = SummaryTreePlanner.plan("source", text, CONFIG);
        var leaf = plan.root();
        var wrongLevel = new SummaryNode(leaf.nodeId(), leaf.kind(), 7, leaf.ordinal(),
                leaf.startByte(), leaf.endByte(), leaf.inputSha256(), leaf.children(), leaf.coverageLeafOrdinals());
        check(!reseal(plan, List.of(wrongLevel), plan.levelCount(), CONFIG).verify(text), "forged leaf level");
        var wrongChildren = new SummaryNode(leaf.nodeId(), leaf.kind(), leaf.level(), leaf.ordinal(),
                leaf.startByte(), leaf.endByte(), leaf.inputSha256(), List.of("missing"), leaf.coverageLeafOrdinals());
        check(!reseal(plan, List.of(wrongChildren), plan.levelCount(), CONFIG).verify(text), "forged leaf edge");
        check(!reseal(plan, List.of(leaf, leaf), plan.levelCount(), CONFIG).verify(text), "duplicate node identity");
        check(!reseal(plan, plan.nodes(), 9, CONFIG).verify(text), "forged level count");
        String many = "x".repeat(2500);
        var tree = SummaryTreePlanner.plan("source", many, CONFIG);
        var reordered = new ArrayList<>(tree.nodes());
        Collections.reverse(reordered);
        check(!reseal(tree, reordered, tree.levelCount(), CONFIG).verify(many), "noncanonical order");
        var tiny = new SummaryTreePlanner.TreeConfig(1024, 64, 2, 256, 1024, 2);
        check(!reseal(tree, tree.nodes(), tree.levelCount(), tiny).verify(many), "resigned node budget");
        var wrongCoverage = new SummaryNode(leaf.nodeId(), leaf.kind(), 0, 0, 0, leaf.endByte(),
                leaf.inputSha256(), List.of(), List.of(0, 1));
        check(!reseal(plan, List.of(wrongCoverage), 1, CONFIG).verify(text), "coverage widening");
    }

    private static SummaryTreePlan reseal(SummaryTreePlan plan, List<SummaryNode> nodes,
            int levels, SummaryTreePlanner.TreeConfig config) {
        String root = Hashing.hash("CPULLM_JAVA_SUMMARY_TREE_V1", plan.sourceRef(), plan.sourceSha256(),
                plan.sourceBytes(), config, nodes.stream().map(SummaryNode::nodeId).toList(), plan.rootNodeId());
        return new SummaryTreePlan(plan.sourceRef(), plan.sourceSha256(), plan.sourceBytes(), config,
                plan.leafCount(), levels, nodes, plan.rootNodeId(), root);
    }

    private static void enforceNodeBudgets() {
        var one = new SummaryTreePlanner.TreeConfig(1024, 1, 2, 256, 1024, 1);
        equal(1, SummaryTreePlanner.plan("source", "x", one).nodes().size());
        expect(IllegalArgumentException.class, () -> SummaryTreePlanner.plan("source", "x".repeat(1025), one));
        var two = new SummaryTreePlanner.TreeConfig(1024, 1, 2, 256, 1024, 2);
        expect(IllegalArgumentException.class, () -> SummaryTreePlanner.plan("source", "x".repeat(1025), two));
        expect(IllegalArgumentException.class, () -> SummaryTreePlanner.plan("source", "", CONFIG));
    }

    private static void serialSessionAndInvalidPlans() throws Exception {
        String text = "original source.\n".repeat(150);
        var plan = SummaryTreePlanner.plan("source", text, CONFIG);
        ProbePort port = new ProbePort("summary");
        var candidate = new SummaryTreeExecutor(port).execute(plan, text);
        equal(1, port.opens); equal(1, port.closes); equal(plan.nodes().size(), port.calls);
        equal(plan.mechanicalRoot(), candidate.mechanicalRoot());
        check(candidate.candidateOnly() && !candidate.admitted() && !candidate.mergeAuthorized(), "authority widened");
        ProbePort untouched = new ProbePort("summary");
        expect(IllegalArgumentException.class, () -> new SummaryTreeExecutor(untouched).execute(plan, text + "changed"));
        equal(0, untouched.opens);
    }

    private static void closeSessionOnRejectedResults() {
        var plan = SummaryTreePlanner.plan("source", "source", CONFIG);
        for (String output : List.of(" ", "x".repeat(257), "\uD800")) {
            ProbePort port = new ProbePort(output);
            expect(Exception.class, () -> new SummaryTreeExecutor(port).execute(plan, "source"));
            equal(1, port.closes); equal(1, port.calls);
        }
        var request = ChromeProtocol.request("node", "source", List.of("source"));
        var forged = new ChromeRequest(request.requestId(), request.nodeId(), "different", request.inputSha256(),
                request.sourceRefs(), request.authority(), true, 0, 0);
        expect(IllegalArgumentException.class, () -> ChromeProtocol.verify(forged, response(request, "summary")));
    }

    private static void cancellation() {
        var plan = SummaryTreePlanner.plan("source", "source", CONFIG);
        ProbePort port = new ProbePort("summary");
        Thread.currentThread().interrupt();
        try {
            expect(CancellationException.class, () -> new SummaryTreeExecutor(port).execute(plan, "source"));
            check(Thread.currentThread().isInterrupted(), "interrupt flag cleared");
        } finally { Thread.interrupted(); }
        equal(0, port.opens);
        port.interruptOnCall = true;
        try {
            expect(CancellationException.class, () -> new SummaryTreeExecutor(port).execute(plan, "source"));
        } finally { Thread.interrupted(); }
        equal(1, port.closes); equal(1, port.calls);
    }

    private static void mechanicalConservation() throws Exception {
        var a = new SourceRecord("a", "scope", "code", "boundary", "class Same { int x = 1; }", Authority.OBSERVED);
        var b = new SourceRecord("b", "scope", "code", "boundary", "class Same { int x = 2; }", Authority.OBSERVED);
        var c = new SourceRecord("c", "scope", "code", "boundary", a.text(), Authority.OBSERVED);
        var owner = new MechanicalSupersetConsolidator(MechanicalPatternCatalog.defaultCatalog());
        var bundle = owner.consolidate(List.of(a, b, c));
        equal(3, bundle.evidence().size()); equal(2, bundle.claims().size()); equal(1, bundle.conflicts().size());
        equal(bundle, owner.consolidate(List.of(c, a, b)));
        var repaired = OmissionRepair.repair(bundle.claims(), Set.of());
        equal(2, repaired.restoredClaimIds().size()); equal(repaired.restoredClaimIds(), repaired.finalClaimIds());
        expect(IllegalArgumentException.class, () -> OmissionRepair.repair(bundle.claims(), Set.of("unknown")));
        var first = JavaChromeAiMechanicalPipeline.builder().treeConfig(CONFIG).summarizer(new ProbePort("one"))
                .promptPort(ignored -> Set.of()).build().run("run", List.of(a, b));
        var second = JavaChromeAiMechanicalPipeline.builder().treeConfig(CONFIG).summarizer(new ProbePort("two"))
                .promptPort(ignored -> Set.of()).build().run("run", List.of(a, b));
        equal(first.assembly().mechanicalRoot(), second.assembly().mechanicalRoot());
        check(!first.assembly().candidateRoot().equals(second.assembly().candidateRoot()), "model output not receipted");
        equal(2, first.assembly().finalClaimIds().size());
    }

    private static void borrowedMonitor() throws Exception {
        String source = "x".repeat(2500);
        var plan = SummaryTreePlanner.plan("source", source, CONFIG);
        class Monitor implements ProgressMonitor {
            long work;
            boolean cancelled;
            public void begin(String name, long total) { throw new AssertionError("borrowed begin"); }
            public void done() { throw new AssertionError("borrowed done"); }
            public void worked(long units) { work += units; }
            public boolean isCancelled() { return cancelled; }
        }
        Monitor monitor = new Monitor();
        ProbePort port = new ProbePort("summary");
        new SummaryTreeExecutor(port).execute(plan, source, monitor);
        equal((long) plan.nodes().size(), monitor.work);
        equal(1, port.closes);
        Monitor cancelled = new Monitor();
        cancelled.cancelled = true;
        ProbePort untouched = new ProbePort("summary");
        expect(CancellationException.class, () -> new SummaryTreeExecutor(untouched).execute(plan, source, cancelled));
        equal(0, untouched.opens); equal(0L, cancelled.work);
        Monitor mid = new Monitor() { public void worked(long units) { super.worked(units); this.cancelled = true; } };
        ProbePort bounded = new ProbePort("summary");
        expect(CancellationException.class, () -> new SummaryTreeExecutor(bounded).execute(plan, source, mid));
        equal(1, bounded.calls); equal(1, bounded.closes); equal(1L, mid.work);
        Monitor rejected = new Monitor();
        ProbePort invalid = new ProbePort(" ");
        expect(IllegalArgumentException.class, () -> new SummaryTreeExecutor(invalid).execute(plan, source, rejected));
        equal(0L, rejected.work); equal(1, invalid.closes);
    }

    @SuppressWarnings("try") // Exercises the inherited adapter checked-close signature.
    private static void fixtureUnicodeAndCheckpoint() throws Exception {
        var request = ChromeProtocol.request("node", "a".repeat(511) + "\uD83D\uDE00tail", List.of("source"));
        try (var session = new DeterministicChromeSummarizerPort().open()) {
            var result = session.summarize(request);
            check(StandardCharsets.UTF_8.newEncoder().canEncode(result.text()), "fixture split surrogate");
            ChromeProtocol.verify(request, result);
        }
        Path dir = Files.createTempDirectory("m3-summary-checkpoint-");
        try {
            var repository = new FileCheckpointRepository(dir);
            var point = new Checkpoint("run", PipelineStage.ASSEMBLED, "a".repeat(64), "b".repeat(64));
            repository.save(point);
            equal(point, repository.load("run").orElseThrow());
            expect(IllegalArgumentException.class, () -> repository.load("../escape"));
        } finally {
            try (var paths = Files.list(dir)) { for (var path : paths.toList()) Files.delete(path); }
            Files.delete(dir);
        }
    }

    private static final class ProbePort implements ChromeSummarizerPort {
        final String output;
        int opens, calls, closes;
        boolean active, interruptOnCall;
        ProbePort(String output) { this.output = output; }
        public Session open() {
            opens++;
            return new Session() {
                public ChromeResult summarize(ChromeRequest request) {
                    check(!active, "overlapping provider call");
                    active = true;
                    try { calls++; if (interruptOnCall) Thread.currentThread().interrupt(); return response(request, output); }
                    finally { active = false; }
                }
                public void close() { closes++; }
            };
        }
    }

    private static ChromeResult response(ChromeRequest request, String text) {
        var unsigned = new ChromeResult("CANDIDATE_ONLY", "", request.requestId(), request.sourceRefs(),
                text, "MODEL_CANDIDATE", true, false, 1, 0);
        return new ChromeResult(unsigned.status(), ChromeProtocol.resultId(unsigned), unsigned.sourceRequestId(),
                unsigned.sourceRefs(), text, unsigned.authority(), true, false, 1, 0);
    }
    private static void equal(Object expected, Object actual) { check(java.util.Objects.equals(expected, actual), expected + " != " + actual); }
    private static void check(boolean yes, String message) { checks++; if (!yes) throw new AssertionError(message); }
    @FunctionalInterface private interface Action { void run() throws Exception; }
    private static void expect(Class<? extends Throwable> type, Action action) {
        checks++;
        try { action.run(); }
        catch (Throwable failure) { if (type.isInstance(failure)) return; throw new AssertionError("expected " + type + ", got " + failure, failure); }
        throw new AssertionError("expected " + type);
    }
}
