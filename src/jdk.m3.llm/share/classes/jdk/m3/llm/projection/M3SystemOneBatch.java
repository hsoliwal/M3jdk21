// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.projection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Portable typed judgments from caller-supplied logits, frozen in schema order.
 *
 * <p>This implements Noul, Choice and bounded integer Score result shapes. It does not include
 * a model, tokenizer, training procedure, or a Jev-compatible inference implementation. The
 * backend and model identities are explicit caller declarations, not authenticated attestations.
 * Probabilities and entropy concentration are model outputs, never semantic certainty or
 * authority to replace exact text or program behavior.</p>
 *
 * <p>The caller supplies one immutable {@link String}. Every backend call sees that same String
 * and identity. Only primitive lanes and {@link M3SemanticIdentity} survive in this batch;
 * question IDs, instructions, labels, the schema object and source text are not retained. The
 * caller keeps the schema for interpreting Choice positions and verifies its root against
 * {@code identity().schemaRoot()}.</p>
 */
public final class M3SystemOneBatch {
    private static final int MAX_QUESTIONS = 256;
    private static final int MAX_LEVELS = 255;
    private static final int MAX_PARALLELISM = 64;
    private static final int MAX_CONTEXT_UTF16 = 1_048_576;
    private static final int MAX_INSTRUCTIONS_UTF16 = 16_384;

    public enum Kind { NOUL, CHOICE, SCORE }

    /**
     * Immutable input schema atom. Choice labels are in probability order; Score covers every
     * integer in its inclusive bounds. Noul logits are always ordered {@code [false, true]}.
     */
    public record Question(
            String id, String instructions, Kind kind, List<String> choices,
            int minimumScore, int maximumScore) {
        public Question {
            id = label(id, "question id");
            Objects.requireNonNull(instructions, "instructions");
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(choices, "choices");
            if (instructions.length() > MAX_INSTRUCTIONS_UTF16 || choices.size() > MAX_LEVELS) {
                throw new IllegalArgumentException("question input exceeds its bound");
            }
            choices = List.copyOf(choices);
            switch (kind) {
                case NOUL -> {
                    if (!choices.isEmpty() || minimumScore != 0 || maximumScore != 1) {
                        throw new IllegalArgumentException("Noul has only false and true outcomes");
                    }
                }
                case CHOICE -> {
                    if (choices.size() < 2 || minimumScore != 0 || maximumScore != 0) {
                        throw new IllegalArgumentException("Choice requires 2..255 named outcomes");
                    }
                    Set<String> names = new HashSet<>();
                    for (String name : choices) {
                        if (!names.add(label(name, "choice label"))) {
                            throw new IllegalArgumentException("duplicate Choice label");
                        }
                    }
                }
                case SCORE -> {
                    long width = (long) maximumScore - minimumScore + 1;
                    if (!choices.isEmpty() || width < 1 || width > MAX_LEVELS) {
                        throw new IllegalArgumentException("Score requires 1..255 consecutive integer outcomes");
                    }
                }
            }
        }

        public static Question noul(String id, String instructions) {
            return new Question(id, instructions, Kind.NOUL, List.of(), 0, 1);
        }

        public static Question choice(String id, String instructions, List<String> choices) {
            return new Question(id, instructions, Kind.CHOICE, choices, 0, 0);
        }

        public static Question score(String id, String instructions, int minimum, int maximum) {
            return new Question(id, instructions, Kind.SCORE, List.of(), minimum, maximum);
        }

        public int outcomeCount() {
            return switch (kind) {
                case NOUL -> 2;
                case CHOICE -> choices.size();
                case SCORE -> (int) ((long) maximumScore - minimumScore + 1);
            };
        }
    }

    /** Caller-owned immutable instructions, bounded to 256 questions and 255 outcomes each. */
    public static final class Schema {
        private final List<Question> questions;
        private final String rootHash;

        public Schema(List<Question> questions) {
            Objects.requireNonNull(questions, "questions");
            if (questions.isEmpty() || questions.size() > MAX_QUESTIONS) {
                throw new IllegalArgumentException("schema requires 1..256 questions");
            }
            this.questions = List.copyOf(questions);
            Set<String> ids = new HashSet<>();
            M3SemanticIdentity.Hasher hash = M3SemanticIdentity
                    .hasher("SYNEXIA_SYSTEM_ONE_BATCH_SCHEMA_V1").integer(this.questions.size());
            for (Question question : this.questions) {
                if (!ids.add(question.id())) throw new IllegalArgumentException("duplicate question id");
                hash.text(question.id()).text(question.instructions()).text(question.kind().name())
                        .integer(question.minimumScore()).integer(question.maximumScore())
                        .integer(question.choices().size());
                for (String name : question.choices()) hash.text(name);
            }
            rootHash = hash.finish();
        }

        public int size() { return questions.size(); }
        public Question question(int index) { return questions.get(index); }
        public List<Question> questions() { return questions; }
        public String rootHash() { return rootHash; }
    }

    /**
     * Concurrent, interruption-cooperative scorer. Return one fresh, finite logit per outcome;
     * do not change returned arrays after returning or retain the context on this class's behalf.
     * The implementation must honor the supplied model/backend identity or refuse execution.
     */
    @FunctionalInterface
    public interface Backend {
        double[] logits(String context, Question question, M3SemanticIdentity identity) throws Exception;
    }

    /** Noul's probability of true; no separate confidence quantity is invented. */
    public static final class Noul {
        private final double probabilityFalse;
        private final double probabilityTrue;

        private Noul(double probabilityFalse, double probabilityTrue) {
            this.probabilityFalse = probabilityFalse;
            this.probabilityTrue = probabilityTrue;
        }

        public double probability() { return probabilityTrue; }
        public double falseProbability() { return probabilityFalse; }
        /** Argmax with false before true, so an exact tie selects false. */
        public boolean value() { return probabilityTrue > probabilityFalse; }
    }

    /** Complete distribution in the caller's schema order. No label or source text is retained. */
    public static final class Choice {
        private final double[] probabilities;

        private Choice(double[] ownedProbabilities) { probabilities = ownedProbabilities; }

        public double[] probabilities() { return probabilities.clone(); }
        /** Index of the maximum probability; ties choose the first schema position. */
        public int value() { return argmax(probabilities); }
        /** One minus normalized entropy, an open concentration statistic, not truth certainty. */
        public double confidence() { return concentration(probabilities); }
    }

    /** Complete distribution for every integer from minimum through maximum inclusive. */
    public static final class Score {
        private final int minimum;
        private final double[] probabilities;

        private Score(int minimum, double[] ownedProbabilities) {
            this.minimum = minimum;
            probabilities = ownedProbabilities;
        }

        public int minimum() { return minimum; }
        public int maximum() { return minimum + (probabilities.length - 1); }
        public double[] probabilities() { return probabilities.clone(); }
        /** Most likely integer; ties choose the smaller score. */
        public int value() { return minimum + argmax(probabilities); }
        public double expectedScore() {
            double value = 0;
            for (int i = 0; i < probabilities.length; i++) {
                value += ((long) minimum + i) * probabilities[i];
            }
            return value;
        }
        /** One minus normalized entropy, distinct from the numeric expected score. */
        public double confidence() { return concentration(probabilities); }
    }

    private final M3SemanticIdentity identity;
    private final byte[] kinds;
    private final int[] offsets;
    private final int[] minimumScores;
    private final double[] probabilities;

    private M3SystemOneBatch(
            M3SemanticIdentity identity, byte[] kinds, int[] offsets,
            int[] minimumScores, double[] probabilities) {
        this.identity = identity;
        this.kinds = kinds;
        this.offsets = offsets;
        this.minimumScores = minimumScores;
        this.probabilities = probabilities;
    }

    public M3SemanticIdentity identity() { return identity; }
    public int size() { return kinds.length; }
    public Kind kind(int question) { return Kind.values()[kinds[Objects.checkIndex(question, size())]]; }

    /** Returns a fresh numerical projection, never a view over retained mutable storage. */
    public double[] probabilities(int question) {
        Objects.checkIndex(question, size());
        return Arrays.copyOfRange(probabilities, offsets[question], offsets[question + 1]);
    }

    public Noul noul(int question) {
        requireKind(question, Kind.NOUL);
        return new Noul(probabilities[offsets[question]], probabilities[offsets[question] + 1]);
    }

    public Choice choice(int question) {
        requireKind(question, Kind.CHOICE);
        return new Choice(probabilities(question));
    }

    public Score score(int question) {
        requireKind(question, Kind.SCORE);
        return new Score(minimumScores[question], probabilities(question));
    }

    public boolean decisionAuthority() { return false; }
    public boolean substitutionAuthority() { return false; }

    public static M3SystemOneBatch evaluate(
            String context, Schema schema, String modelName, M3SemanticIdentity.Model model,
            String backendId, Backend backend, int parallelism) throws InterruptedException {
        return evaluate(context, schema, modelName, model, backendId, backend, parallelism, null);
    }

    /**
     * Evaluates an optional explicit owner/snapshot range in the same exact context. At most
     * {@code parallelism} backend calls are active and at most that many worker tasks are queued.
     * A result is published only after every question succeeds, in schema order.
     *
     * <p>Failures and caller interruption request interruption of the remaining workers. Backends
     * must cooperate; this API does not promise a deadline or terminate uncooperative code. Worker
     * threads are daemon threads. This class retains no context after a completed cooperative
     * evaluation, but cannot control a backend's own retention or external side effects.</p>
     *
     * @param scope null for content-derived whole-context identity
     */
    public static M3SystemOneBatch evaluate(
            String context, Schema schema, String modelName, M3SemanticIdentity.Model model,
            String backendId, Backend backend, int parallelism, M3SemanticIdentity.Scope scope)
            throws InterruptedException {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(schema, "schema");
        Objects.requireNonNull(backend, "backend");
        if (context.length() > MAX_CONTEXT_UTF16 || parallelism < 1 || parallelism > MAX_PARALLELISM) {
            throw new IllegalArgumentException("context or parallelism exceeds its bound");
        }
        checkInterrupted();
        M3SemanticIdentity.Builder builder = scope == null
                ? M3SemanticIdentity.builder(modelName, model, backendId, schema.rootHash(), 1)
                : M3SemanticIdentity.builder(modelName, model, backendId, schema.rootHash(), 1, scope);
        M3SemanticIdentity identity = builder.segment(context).build();
        checkInterrupted();

        int workers = Math.min(parallelism, schema.size());
        ExecutorService executor = Executors.newFixedThreadPool(workers,
                Thread.ofPlatform().daemon().name("system-one-batch-", 0).factory());
        ExecutorCompletionService<Void> completed = new ExecutorCompletionService<>(executor);
        List<Future<Void>> tasks = new ArrayList<>(workers);
        AtomicInteger next = new AtomicInteger();
        double[][] rows = new double[schema.size()][];
        try {
            for (int worker = 0; worker < workers; worker++) {
                tasks.add(completed.submit(() -> {
                    for (;;) {
                        checkInterrupted();
                        int index = next.getAndIncrement();
                        if (index >= schema.size()) return null;
                        Question question = schema.question(index);
                        double[] logits = backend.logits(context, question, identity);
                        checkInterrupted();
                        rows[index] = softmax(logits, question.outcomeCount());
                    }
                }));
            }
            for (int worker = 0; worker < workers; worker++) completed.take().get();
        } catch (ExecutionException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof InterruptedException interrupted) throw interrupted;
            if (cause instanceof RuntimeException runtime) throw runtime;
            if (cause instanceof Error error) throw error;
            throw new IllegalStateException("System One backend failed", cause);
        } finally {
            for (Future<Void> task : tasks) task.cancel(true);
            executor.shutdownNow();
        }
        checkInterrupted();

        byte[] kinds = new byte[schema.size()];
        int[] offsets = new int[schema.size() + 1];
        int[] minimumScores = new int[schema.size()];
        for (int i = 0; i < schema.size(); i++) {
            kinds[i] = (byte) schema.question(i).kind().ordinal();
            offsets[i + 1] = Math.addExact(offsets[i], rows[i].length);
            minimumScores[i] = schema.question(i).minimumScore();
        }
        double[] values = new double[offsets[schema.size()]];
        for (int i = 0; i < rows.length; i++) {
            System.arraycopy(rows[i], 0, values, offsets[i], rows[i].length);
        }
        checkInterrupted();
        return new M3SystemOneBatch(identity, kinds, offsets, minimumScores, values);
    }

    private void requireKind(int question, Kind expected) {
        if (kind(question) != expected) throw new IllegalArgumentException("question has a different kind");
    }

    private static String label(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank() || value.length() > 512 || value.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("invalid " + field);
        }
        return value;
    }

    private static void checkInterrupted() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) throw new InterruptedException("System One evaluation interrupted");
    }

    private static double[] softmax(double[] logits, int width) {
        Objects.requireNonNull(logits, "backend logits");
        if (logits.length != width) throw new IllegalArgumentException("backend logit width differs from schema");
        double[] values = logits.clone();
        double maximum = -Double.MAX_VALUE;
        for (double value : values) {
            if (!Double.isFinite(value)) throw new IllegalArgumentException("backend logits must be finite");
            maximum = Math.max(maximum, value);
        }
        double total = 0;
        for (int i = 0; i < values.length; i++) {
            values[i] = StrictMath.exp(values[i] - maximum);
            total += values[i];
        }
        for (int i = 0; i < values.length; i++) values[i] /= total;
        return values;
    }

    private static int argmax(double[] probabilities) {
        int best = 0;
        for (int i = 1; i < probabilities.length; i++) {
            if (probabilities[i] > probabilities[best]) best = i;
        }
        return best;
    }

    private static double concentration(double[] probabilities) {
        if (probabilities.length == 1) return 1;
        double entropy = 0;
        for (double probability : probabilities) {
            if (probability > 0) entropy -= probability * StrictMath.log(probability);
        }
        return Math.max(0, Math.min(1, 1 - entropy / StrictMath.log(probabilities.length)));
    }
}
