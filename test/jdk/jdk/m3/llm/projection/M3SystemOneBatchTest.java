// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.projection;

import static org.junit.jupiter.api.Assertions.*;

import jdk.m3.llm.projection.M3SystemOneBatch.Backend;
import jdk.m3.llm.projection.M3SystemOneBatch.Question;
import jdk.m3.llm.projection.M3SystemOneBatch.Schema;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** Product-facing contracts using a synthetic logits backend, never a Jev model. */
@Timeout(15)
final class M3SystemOneBatchTest {
    private static final String SOURCE = "exact immutable source \uD800 / \uDC00 / \0";
    private static final M3SemanticIdentity.Model MODEL = new M3SemanticIdentity.Model(
            "1".repeat(64), "2".repeat(64), "3".repeat(64), "4".repeat(64));

    @Test
    void noulUsesFalseTrueLogitOrderWithExactImmutableContext() throws Exception {
        Schema schema = new Schema(List.of(Question.noul("valid", "Is the source valid?")));
        var batch = evaluate(schema, (context, question, identity) -> {
            assertSame(SOURCE, context);
            assertSame(schema.question(0), question);
            assertEquals(schema.rootHash(), identity.schemaRoot());
            return new double[] {0.0, 1.0986122886681098};
        }, 1);
        assertArrayEquals(new double[] {0.25, 0.75}, batch.probabilities(0), 1e-15);
        assertEquals(0.75, batch.noul(0).probability(), 1e-15);
        assertEquals(0.25, batch.noul(0).falseProbability(), 1e-15);
        assertTrue(batch.noul(0).value());
        assertEquals(schema.rootHash(), batch.identity().schemaRoot());
    }

    @Test
    void preservesCompleteChoiceAndBoundedIntegerScoreDistributions() throws Exception {
        Schema schema = new Schema(List.of(
                Question.choice("colour", "Choose one", List.of("red", "green", "blue")),
                Question.score("quality", "Score the evidence", -2, 1)));
        var batch = evaluate(schema, (context, question, identity) ->
                question.id().equals("colour")
                        ? new double[] {0.0, 0.6931471805599453, 0.0}
                        : new double[] {0.0, 0.0, 0.6931471805599453, 1.3862943611198906}, 2);
        assertEquals(2, batch.size());
        var choice = batch.choice(0);
        assertArrayEquals(new double[] {0.25, 0.5, 0.25}, choice.probabilities(), 1e-15);
        assertEquals(1, choice.value());
        assertEquals("green", schema.question(0).choices().get(choice.value()));
        var score = batch.score(1);
        assertArrayEquals(new double[] {0.125, 0.125, 0.25, 0.5}, score.probabilities(), 1e-15);
        assertEquals(-2, score.minimum());
        assertEquals(1, score.maximum());
        assertEquals(1, score.value());
        assertEquals(0.125, score.expectedScore(), 1e-15);
    }

    @Test
    void entropyConcentrationDoesNotInventSemanticCertaintyAndTiesUseFirstOutcome() throws Exception {
        Schema schema = new Schema(List.of(
                Question.noul("boolean", ""),
                Question.choice("choice", "", List.of("a", "b", "c")),
                Question.score("score", "", -1, 1),
                Question.score("fixed", "", 7, 7)));
        var batch = evaluate(schema, (context, question, identity) ->
                new double[question.outcomeCount()], 2);
        assertFalse(batch.noul(0).value());
        assertEquals(0.5, batch.noul(0).probability());
        assertEquals(0, batch.choice(1).value());
        assertEquals(0.0, batch.choice(1).confidence(), 1e-15);
        assertEquals(-1, batch.score(2).value());
        assertEquals(0.0, batch.score(2).expectedScore(), 1e-15);
        assertEquals(0.0, batch.score(2).confidence(), 1e-15);
        assertEquals(7, batch.score(3).value());
        assertEquals(7.0, batch.score(3).expectedScore());
        assertEquals(1.0, batch.score(3).confidence());
        assertFalse(batch.decisionAuthority());
        assertFalse(batch.substitutionAuthority());
    }

    @Test
    void finiteExtremeLogitsNormalizeWithoutOverflow() throws Exception {
        Schema schema = new Schema(List.of(Question.choice("q", "", List.of("a", "b"))));
        var first = evaluate(schema, (context, question, identity) ->
                new double[] {Double.MAX_VALUE, -Double.MAX_VALUE}, 1);
        assertArrayEquals(new double[] {1.0, 0.0}, first.probabilities(0));
        assertEquals(1.0, first.choice(0).confidence());
        var equal = evaluate(schema, (context, question, identity) ->
                new double[] {-Double.MAX_VALUE, -Double.MAX_VALUE}, 1);
        assertArrayEquals(new double[] {0.5, 0.5}, equal.probabilities(0));
    }

    @Test
    void malformedBackendOutputIsRefusedInsteadOfBecomingAProbability() {
        Schema schema = new Schema(List.of(Question.noul("q", "")));
        for (double bad : new double[] {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> evaluate(schema,
                    (context, question, identity) -> new double[] {0.0, bad}, 1));
        }
        for (int length : new int[] {0, 1, 3}) {
            assertThrows(IllegalArgumentException.class, () -> evaluate(schema,
                    (context, question, identity) -> new double[length], 1));
        }
        assertThrows(NullPointerException.class, () -> evaluate(schema,
                (context, question, identity) -> null, 1));
    }

    @Test
    void parallelCompletionFreezesInSchemaOrderAndHonoursWorkerBound() throws Exception {
        Schema schema = new Schema(IntStream.range(0, 6)
                .mapToObj(i -> Question.noul("q" + i, "")).toList());
        AtomicInteger active = new AtomicInteger();
        AtomicInteger maximum = new AtomicInteger();
        CountDownLatch firstTwo = new CountDownLatch(2);
        var batch = evaluate(schema, (context, question, identity) -> {
            int index = Integer.parseInt(question.id().substring(1));
            int now = active.incrementAndGet();
            maximum.accumulateAndGet(now, Math::max);
            try {
                if (index < 2) {
                    firstTwo.countDown();
                    if (!firstTwo.await(5, TimeUnit.SECONDS)) throw new AssertionError("missing parallel worker");
                }
                if ((index & 1) == 0) Thread.sleep(15);
                return switch (index % 3) {
                    case 0 -> new double[] {0.0, 0.0};
                    case 1 -> new double[] {0.0, 1.0986122886681098};
                    default -> new double[] {1.0986122886681098, 0.0};
                };
            } finally {
                active.decrementAndGet();
            }
        }, 2);
        assertEquals(2, maximum.get());
        assertEquals(0, active.get());
        double[] expected = {0.5, 0.75, 0.25, 0.5, 0.75, 0.25};
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], batch.noul(i).probability(), 1e-15);
        }
    }

    @Test
    void failureCancelsCooperativePeersWithoutPublishingAPartialBatch() throws Exception {
        Schema schema = new Schema(List.of(Question.noul("fail", ""), Question.noul("wait", "")));
        CountDownLatch bothStarted = new CountDownLatch(2);
        CountDownLatch peerInterrupted = new CountDownLatch(1);
        IllegalStateException marker = new IllegalStateException("backend failure marker");
        Throwable observed = assertThrows(IllegalStateException.class, () -> evaluate(schema,
                (context, question, identity) -> {
                    bothStarted.countDown();
                    if (!bothStarted.await(5, TimeUnit.SECONDS)) throw new AssertionError("missing peer");
                    if (question.id().equals("fail")) throw marker;
                    try {
                        new CountDownLatch(1).await();
                        throw new AssertionError("unreachable");
                    } catch (InterruptedException interrupted) {
                        peerInterrupted.countDown();
                        throw interrupted;
                    }
                }, 2));
        assertSame(marker, observed);
        assertTrue(peerInterrupted.await(5, TimeUnit.SECONDS));
    }

    @Test
    void callerInterruptionCancelsCooperativeWorkers() throws Exception {
        Schema schema = new Schema(List.of(Question.noul("q", "")));
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch workerInterrupted = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread caller = Thread.ofPlatform().daemon().start(() -> {
            try {
                evaluate(schema, (context, question, identity) -> {
                    started.countDown();
                    try {
                        new CountDownLatch(1).await();
                        throw new AssertionError("unreachable");
                    } catch (InterruptedException interrupted) {
                        workerInterrupted.countDown();
                        throw interrupted;
                    }
                }, 1);
                failure.set(new AssertionError("cancelled evaluation returned a result"));
            } catch (Throwable thrown) {
                failure.set(thrown);
            }
        });
        assertTrue(started.await(5, TimeUnit.SECONDS));
        caller.interrupt();
        caller.join(5_000);
        assertFalse(caller.isAlive());
        assertInstanceOf(InterruptedException.class, failure.get());
        assertTrue(workerInterrupted.await(5, TimeUnit.SECONDS));
    }

    @Test
    void schemaInputsBackendArraysAndExportedArraysCannotMutateFrozenResults() throws Exception {
        ArrayList<String> labels = new ArrayList<>(List.of("left", "right"));
        Question question = Question.choice("q", "", labels);
        ArrayList<Question> questions = new ArrayList<>(List.of(question));
        Schema schema = new Schema(questions);
        labels.clear();
        questions.clear();
        double[] logits = {0.0, 1.0986122886681098};
        var batch = evaluate(schema, (context, q, identity) -> logits, 1);
        Arrays.fill(logits, -99);
        double[] exported = batch.probabilities(0);
        Arrays.fill(exported, -99);
        var choice = batch.choice(0);
        double[] choiceExport = choice.probabilities();
        Arrays.fill(choiceExport, -99);
        assertEquals(List.of("left", "right"), schema.question(0).choices());
        assertThrows(UnsupportedOperationException.class, () -> schema.questions().clear());
        assertArrayEquals(new double[] {0.25, 0.75}, batch.probabilities(0), 1e-15);
        assertArrayEquals(new double[] {0.25, 0.75}, choice.probabilities(), 1e-15);
    }

    @Test
    void identityBindsExactContextSchemaModelAndBackendIndependentOfWorkerCount() throws Exception {
        Schema schema = new Schema(List.of(Question.noul("q", "")));
        Backend zeros = (context, question, identity) -> new double[question.outcomeCount()];
        var first = evaluate(schema, zeros, 1);
        var repeat = evaluate(schema, zeros, 2);
        assertEquals(first.identity().rootHash(), repeat.identity().rootHash());
        var changedContext = M3SystemOneBatch.evaluate(SOURCE.replace('\uD800', '\uD801'), schema,
                "model", MODEL, "backend", zeros, 1);
        assertNotEquals(first.identity().rootHash(), changedContext.identity().rootHash());
        Schema changedSchema = new Schema(List.of(Question.noul("q", "different exact instructions")));
        assertNotEquals(first.identity().rootHash(), evaluate(changedSchema, zeros, 1).identity().rootHash());
        var changedModel = new M3SemanticIdentity.Model(
                "5".repeat(64), "2".repeat(64), "3".repeat(64), "4".repeat(64));
        assertNotEquals(first.identity().rootHash(), M3SystemOneBatch.evaluate(SOURCE, schema,
                "model", changedModel, "backend", zeros, 1).identity().rootHash());
        assertNotEquals(first.identity().rootHash(), M3SystemOneBatch.evaluate(SOURCE, schema,
                "model", MODEL, "other-backend", zeros, 1).identity().rootHash());
    }

    @Test
    void explicitOwnerSnapshotAndRangeAreBoundBeforeBackendExecution() throws Exception {
        Schema schema = new Schema(List.of(Question.noul("q", "")));
        var scope = new M3SemanticIdentity.Scope("a".repeat(64), "b".repeat(64), "c".repeat(64),
                101, 103, 100, 104);
        var batch = M3SystemOneBatch.evaluate("abcd", schema, "model", MODEL, "backend",
                (context, question, identity) -> {
                    assertEquals(scope, identity.scope());
                    assertEquals("abcd", context);
                    return new double[] {0, 0};
                }, 1, scope);
        assertEquals(scope, batch.identity().scope());
        AtomicInteger called = new AtomicInteger();
        assertThrows(IllegalArgumentException.class, () -> M3SystemOneBatch.evaluate("abc", schema,
                "model", MODEL, "backend", (context, question, identity) -> {
                    called.incrementAndGet();
                    return new double[] {0, 0};
                }, 1, scope));
        assertEquals(0, called.get());
    }

    @Test
    void invalidSchemaAndResourceBoundsAreRefusedBeforeBackendExecution() {
        assertThrows(IllegalArgumentException.class, () -> new Schema(List.of()));
        assertThrows(IllegalArgumentException.class, () -> new Schema(List.of(
                Question.noul("q", "a"), Question.noul("q", "b"))));
        assertThrows(IllegalArgumentException.class, () -> Question.choice("q", "", List.of("a", "a")));
        assertThrows(IllegalArgumentException.class, () -> Question.choice("q", "", List.of("a")));
        assertThrows(IllegalArgumentException.class, () -> Question.score("q", "", 2, 1));
        assertThrows(IllegalArgumentException.class, () -> Question.score("q", "", Integer.MIN_VALUE, Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> Question.score("q", "", 0, 255));
        assertThrows(IllegalArgumentException.class, () -> new Schema(IntStream.range(0, 257)
                .mapToObj(i -> Question.noul("q" + i, "")).toList()));
        Schema schema = new Schema(List.of(Question.noul("q", "")));
        AtomicInteger called = new AtomicInteger();
        Backend backend = (context, question, identity) -> {
            called.incrementAndGet();
            return new double[] {0, 0};
        };
        for (int workers : new int[] {0, 65}) {
            assertThrows(IllegalArgumentException.class, () -> evaluate(schema, backend, workers));
        }
        assertThrows(IllegalArgumentException.class, () -> M3SystemOneBatch.evaluate("x".repeat(1_048_577),
                schema, "model", MODEL, "backend", backend, 1));
        assertEquals(0, called.get());
    }

    @Test
    void typedAccessorsRefuseWrongQuestionKindsAndOutOfBounds() throws Exception {
        Schema schema = new Schema(List.of(Question.noul("q", "")));
        var batch = evaluate(schema, (context, question, identity) -> new double[] {0, 0}, 1);
        assertThrows(IllegalArgumentException.class, () -> batch.choice(0));
        assertThrows(IllegalArgumentException.class, () -> batch.score(0));
        assertThrows(IndexOutOfBoundsException.class, () -> batch.noul(1));
        assertThrows(IndexOutOfBoundsException.class, () -> batch.probabilities(-1));
    }

    @Test
    void frozenBatchRetainsNeitherSourceTextNorQuestionTextNorSchemaObjects() throws Exception {
        String instructions = "Private question instructions that must not remain in the frozen batch";
        Schema schema = new Schema(List.of(Question.noul("private-question-id", instructions)));
        var batch = evaluate(schema, (context, question, identity) -> new double[] {0, 1}, 1);
        assertFalse(retains(batch, SOURCE, new IdentityHashMap<>()));
        assertFalse(retains(batch, instructions, new IdentityHashMap<>()));
        assertFalse(retains(batch, schema, new IdentityHashMap<>()));
        assertFalse(retains(batch, schema.question(0), new IdentityHashMap<>()));
    }

    private static M3SystemOneBatch evaluate(Schema schema, Backend backend, int parallelism)
            throws InterruptedException {
        return M3SystemOneBatch.evaluate(SOURCE, schema, "model", MODEL, "backend", backend, parallelism);
    }

    private static boolean retains(Object value, Object wanted, IdentityHashMap<Object, Boolean> seen)
            throws IllegalAccessException {
        if (value == null) return false;
        if (value == wanted || (value instanceof String && value.equals(wanted))) return true;
        if (seen.put(value, Boolean.TRUE) != null) return false;
        Class<?> type = value.getClass();
        if (type.isArray()) {
            if (type.getComponentType().isPrimitive()) return false;
            for (int i = 0; i < Array.getLength(value); i++) {
                if (retains(Array.get(value, i), wanted, seen)) return true;
            }
        } else if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) if (retains(item, wanted, seen)) return true;
        } else if (type.getPackageName().equals(M3SystemOneBatch.class.getPackageName()) && !type.isEnum()) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) continue;
                field.setAccessible(true);
                if (retains(field.get(value), wanted, seen)) return true;
            }
        }
        return false;
    }
}
