// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
// Derivation: AUTHORED_TASK_BEHAVIOR_REGRESSION; see SOURCE_PROVENANCE.tsv.
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.chrome;

import static org.junit.jupiter.api.Assertions.*;

import jdk.m3.llm.internal.json.ForgeJson;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/** Receiving-boundary tests; the exact same compiled tests also run against the original owner. */
class ChromeMlTaskServiceMigrationTest {
    private static ChromeMlTaskService.Request request(ChromeMlTaskService.Kind kind,
            String source, String option, int budget) {
        return new ChromeMlTaskService.Request(kind, source, option, budget, 1024, Duration.ofSeconds(5));
    }
    private static ChromeMlRunnerResult terminal(String text) {
        return new ChromeMlRunnerResult(text, 1, "", "");
    }
    private static String hash(String text) { return ForgeJson.hash(text.getBytes(StandardCharsets.UTF_8)); }

    @Test void everyExistingTaskKindRetainsItsCandidateAndSchemaContract() throws Exception {
        for (var kind : ChromeMlTaskService.Kind.values()) {
            String option = switch (kind) {
                case CLASSIFY -> "alpha,beta";
                case LANGUAGE_DETECT -> "English,French";
                case EXTRACT -> "name,value";
                case TRANSLATE -> "French";
                default -> "";
            };
            String response = switch (kind) {
                case CLASSIFY -> "{\"label\":\"alpha\"}";
                case LANGUAGE_DETECT -> "{\"label\":\"English\"}";
                case EXTRACT -> "{\"name\":null,\"value\":12}";
                default -> "candidate";
            };
            var captured = new ArrayList<ChromeMlRunnerRequest>();
            var service = new ChromeMlTaskService((r,s) -> { captured.add(r); return terminal(response); });
            var task = request(kind, "untrusted source", option, 1);
            var output = service.execute(task);
            assertEquals(kind, output.kind());
            assertEquals(response, output.text());
            assertFalse(output.executionAuthority());
            assertEquals(Set.of(ChromeMlTaskService.Kind.CLASSIFY, ChromeMlTaskService.Kind.LANGUAGE_DETECT,
                    ChromeMlTaskService.Kind.EXTRACT).contains(kind), output.schemaValidated());
            assertEquals(hash(task.source()), output.sourceSha256());
            assertEquals(1, output.coverage().size());
            assertEquals(1, output.receipts().size());
            assertEquals(hash(task.source()), output.receipts().getFirst().inputSha256());
            assertEquals(hash(response), output.receipts().getFirst().outputSha256());
            assertEquals(64, output.receipts().getFirst().requestSha256().length());
            assertFalse(task.toString().contains(task.source()));
            assertFalse(output.toString().contains(response));
            assertEquals("CANDIDATE_ONLY", output.asJson().get("authority"));
            assertEquals("PROMPT_RECIPE_NOT_BROWSER_API", output.asJson().get("apiKind"));
            assertFalse((Boolean) output.asJson().get("gpuObserved"));
            ForgeJson.encode(output.asJson());
            assertEquals(1, captured.size());
            assertTrue(captured.getFirst().prompt().contains("DATA_UTF16_LENGTH=16\nuntrusted source\nEND_DATA"));
            assertEquals(1024, captured.getFirst().maxOutput());
            assertTrue(captured.getFirst().imagePaths().isEmpty());
            assertNull(captured.getFirst().audioPath());
        }
    }

    @Test void summaryAndKeyPointsRetainExactCoverageAndThreeWayReduction() throws Exception {
        String source = "a".repeat(7999) + "🌍" + "b".repeat(25001);
        for (var kind : List.of(ChromeMlTaskService.Kind.SUMMARY, ChromeMlTaskService.Kind.KEY_POINTS)) {
            var calls = new ArrayList<ChromeMlRunnerRequest>();
            var service = new ChromeMlTaskService((r,s) -> { calls.add(r); return terminal("partial-" + calls.size()); });
            var task = request(kind, source, "", 8);
            var plan = ChromeMlTaskService.plan(task);
            assertTrue(calls.isEmpty());
            assertEquals(plan, ChromeMlTaskService.plan(task));
            assertEquals(5, plan.size());
            assertEquals(7999, plan.getFirst().end());
            StringBuilder restored = new StringBuilder();
            int end = 0;
            for (var segment : plan) {
                assertEquals(end, segment.start());
                String text = source.substring(segment.start(), segment.end());
                assertTrue(text.length() <= 8000);
                assertFalse(Character.isLowSurrogate(text.charAt(0)));
                assertFalse(Character.isHighSurrogate(text.charAt(text.length()-1)));
                assertEquals(hash(text), segment.sha256());
                restored.append(text); end = segment.end();
            }
            assertEquals(source, restored.toString());
            var result = service.execute(task);
            assertEquals(plan, result.coverage());
            assertEquals("partial-8", result.text());
            assertEquals(8, calls.size());
            assertEquals(List.of("map", "map", "map", "map", "map", "reduce", "reduce", "reduce"),
                    result.receipts().stream().map(ChromeMlTaskService.Receipt::stage).toList());
            assertTrue(calls.stream().allMatch(r -> r.maxOutput() == 512));
            assertTrue(calls.get(5).prompt().contains("partial-1\n\n--- partial ---\npartial-2\n\n--- partial ---\npartial-3"));
            assertTrue(calls.get(6).prompt().contains("partial-4\n\n--- partial ---\npartial-5"));
            assertTrue(calls.get(7).prompt().contains("partial-6\n\n--- partial ---\npartial-7"));
            for (int i = 0; i < 8; i++) assertEquals(i, result.receipts().get(i).index());
        }
    }

    @Test void wholeJobBudgetRefusesBeforeAnyInference() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        var service = new ChromeMlTaskService((r,s) -> { calls.incrementAndGet(); return terminal("ok"); });
        for (int budget : List.of(0, 1, 7)) {
            var failure = assertThrows(IOException.class, () -> service.execute(
                    request(ChromeMlTaskService.Kind.SUMMARY, "x".repeat(33002), "", budget)));
            assertEquals("TASK_CALL_BUDGET (no inference started)", failure.getMessage());
        }
        assertEquals(0, calls.get());
        assertEquals(8, service.execute(request(ChromeMlTaskService.Kind.SUMMARY, "x".repeat(33002), "", 8)).receipts().size());
    }

    @Test void invalidSourceVocabularyAndUnsupportedSplittingAreRejected() {
        for (String invalid : List.of("\ud800", "\udc00", "a\0b", "x".repeat(262145))) {
            assertThrows(IllegalArgumentException.class, () -> request(ChromeMlTaskService.Kind.SUMMARY, invalid, "", 1));
        }
        for (var kind : List.of(ChromeMlTaskService.Kind.CLASSIFY, ChromeMlTaskService.Kind.LANGUAGE_DETECT, ChromeMlTaskService.Kind.EXTRACT)) {
            for (String invalid : List.of("", "a,a", "a, a", "a,", "1invalid", "a\nb")) {
                assertEquals("TASK_LABELS_OR_FIELDS_INVALID", assertThrows(IllegalArgumentException.class,
                        () -> request(kind, "source", invalid, 1)).getMessage());
            }
        }
        assertEquals("TASK_REQUIRES_SINGLE_CHUNK", assertThrows(IllegalArgumentException.class,
                () -> request(ChromeMlTaskService.Kind.EXPLAIN_CODE, "x".repeat(8001), "", 1)).getMessage());
        assertEquals("TARGET_LANGUAGE_REQUIRED", assertThrows(IllegalArgumentException.class,
                () -> request(ChromeMlTaskService.Kind.TRANSLATE, "x", "", 1)).getMessage());
    }

    @Test void malformedAndUnadmittedSchemasAreRejected() {
        for (var kind : List.of(ChromeMlTaskService.Kind.CLASSIFY, ChromeMlTaskService.Kind.LANGUAGE_DETECT)) {
            for (String text : List.of("not JSON", "{}", "{\"label\":\"gamma\"}", "{\"label\":\"alpha\",\"extra\":0}",
                    "{\"label\":\"alpha\",\"label\":\"alpha\"}", "{\"label\":null}")) {
                var service = new ChromeMlTaskService((r,s) -> terminal(text));
                assertEquals("TASK_SCHEMA_REJECTED", assertThrows(IOException.class,
                        () -> service.execute(request(kind, "x", "alpha,beta", 1))).getMessage());
            }
        }
        for (String text : List.of("{}", "{\"name\":null}", "{\"name\":null,\"value\":0,\"extra\":0}")) {
            var service = new ChromeMlTaskService((r,s) -> terminal(text));
            assertEquals("TASK_SCHEMA_REJECTED", assertThrows(IOException.class,
                    () -> service.execute(request(ChromeMlTaskService.Kind.EXTRACT, "x", "name,value", 1))).getMessage());
        }
    }

    @Test void incompleteEmptyAndOversizedOutputCannotEscapeTheBoundary() {
        var task = request(ChromeMlTaskService.Kind.SUMMARY, "source", "", 1);
        for (Integer status : new Integer[]{null, -1, 0, 2, Integer.MAX_VALUE}) {
            var service = new ChromeMlTaskService((r,s) -> new ChromeMlRunnerResult("partial", status, "", ""));
            assertEquals("TASK_NATIVE_NOT_TERMINAL", assertThrows(IOException.class, () -> service.execute(task)).getMessage());
        }
        var empty = new ChromeMlTaskService((r,s) -> terminal(" \n"));
        assertEquals("TASK_EMPTY_OUTPUT", assertThrows(IOException.class, () -> empty.execute(task)).getMessage());
        var huge = new ChromeMlTaskService((r,s) -> terminal("x".repeat(8193)));
        assertEquals("TASK_TEXT_LIMIT", assertThrows(IllegalArgumentException.class, () -> huge.execute(task)).getMessage());
        var partialHuge = new ChromeMlTaskService((r,s) -> terminal("x".repeat(2049)));
        assertEquals("TASK_TEXT_LIMIT", assertThrows(IllegalArgumentException.class, () -> partialHuge.execute(
                request(ChromeMlTaskService.Kind.SUMMARY, "x".repeat(8001), "", 3))).getMessage());
    }

    @Test void busyAndFailureReleasePreserveSingleFlight() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<Throwable> workerFailure = new AtomicReference<>();
        var service = new ChromeMlTaskService((r,s) -> {
            if (calls.getAndIncrement() == 0) {
                entered.countDown();
                if (!release.await(5, TimeUnit.SECONDS)) throw new IOException("fixture timed out");
                throw new IOException("fixture failure");
            }
            return terminal("ok");
        });
        var task = request(ChromeMlTaskService.Kind.SUMMARY, "source", "", 1);
        Thread worker = Thread.ofPlatform().start(() -> {
            try { service.execute(task); } catch (Throwable failure) { workerFailure.set(failure); }
        });
        try {
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            assertEquals("TASK_BUSY", assertThrows(IOException.class, () -> service.execute(task)).getMessage());
        } finally { release.countDown(); worker.join(5000); }
        assertFalse(worker.isAlive());
        assertInstanceOf(IOException.class, workerFailure.get());
        assertEquals("fixture failure", workerFailure.get().getMessage());
        assertEquals("ok", service.execute(task).text());
        assertEquals(2, calls.get());
    }

    @Test void interruptionBeforeInferenceFailsClosedAndReleasesBusy() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        var service = new ChromeMlTaskService((r,s) -> { calls.incrementAndGet(); return terminal("ok"); });
        var task = request(ChromeMlTaskService.Kind.SUMMARY, "source", "", 1);
        Thread.currentThread().interrupt();
        try {
            assertEquals("TASK_INTERRUPTED", assertThrows(InterruptedException.class, () -> service.execute(task)).getMessage());
            assertEquals(0, calls.get());
        } finally { Thread.interrupted(); }
        assertEquals("ok", service.execute(task).text());
    }
}
