// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Stock {@link java.util.regex.Pattern} outcomes over the frozen {@link M3RegexOracleCorpus},
 * held in {@link M3RegexStringTrialImage} shards, plus a soundness check for match-pruning
 * candidates.
 *
 * <p>The oracle is the JDK: every {@code matches}, {@code lookingAt} and {@code find} cell comes
 * from {@code Pattern} inside the trial image, and {@link #oracleRoot()} binds the corpus roots
 * to the shard root hashes. A {@link Candidate} is any rule that claims {@code find} cannot
 * succeed for a (regex, input) pair, which is the only claim a prefix/trigram/literal pruning
 * consumer ever makes; {@link #check} counts how often the claim prunes and how often it
 * contradicts the oracle, retaining a bounded, ordered ledger of contradictions. M3 signals in
 * the images rank hard cases only and never contribute to a verdict.</p>
 */
public final class M3RegexOracleHarness {
    /** Candidate claim about one (regex, input) pair. */
    public enum Verdict { CANNOT_MATCH, MAY_MATCH }

    @FunctionalInterface
    public interface Candidate {
        Verdict classify(String regex, String input);
    }

    /** One contradiction: the candidate said {@code CANNOT_MATCH} but {@code find} succeeded. */
    public record Mismatch(int regexOrdinal, int stringOrdinal) {}

    /** Outcome of {@link #check}; {@code root} is derived from every other component. */
    public record Report(
            String oracleRoot,
            String candidateName,
            long pairs,
            long findTrue,
            long pruned,
            long unsound,
            List<Mismatch> retained,
            String root) {
        public Report {
            Objects.requireNonNull(oracleRoot, "oracleRoot");
            Objects.requireNonNull(candidateName, "candidateName");
            retained = List.copyOf(retained);
            StringBuilder material = new StringBuilder("M3_REGEX_ORACLE_REPORT_V1\n")
                    .append(oracleRoot).append('\n')
                    .append(candidateName).append('\n')
                    .append(pairs).append('\n')
                    .append(findTrue).append('\n')
                    .append(pruned).append('\n')
                    .append(unsound).append('\n');
            retained.forEach(row -> material
                    .append(row.regexOrdinal()).append('\t').append(row.stringOrdinal()).append('\n'));
            String expected = M3RegexOracleCorpus.sha256(material.toString());
            root = root == null || root.isBlank() ? expected : root;
            if (!expected.equals(root)) {
                throw new IllegalArgumentException("REPORT_ROOT_MISMATCH");
            }
        }

        public boolean sound() {
            return unsound == 0L;
        }
    }

    private final List<String> expressions;
    private final List<String> inputs;
    private final M3RegexStringTrialImage[] shards;
    private final String oracleRoot;

    private M3RegexOracleHarness(
            List<String> expressions,
            List<String> inputs,
            M3RegexStringTrialImage[] shards,
            String oracleRoot) {
        this.expressions = expressions;
        this.inputs = inputs;
        this.shards = shards;
        this.oracleRoot = oracleRoot;
    }

    /** Compiles the corpus into {@link M3RegexOracleCorpus#SHARD_COUNT} trial images (Java signal provider). */
    public static M3RegexOracleHarness compile(M3Progress monitor) {
        List<String> expressions = M3RegexOracleCorpus.expressions();
        List<String> inputs = M3RegexOracleCorpus.strings();
        M3RegexStringTrialImage[] shards = new M3RegexStringTrialImage[M3RegexOracleCorpus.SHARD_COUNT];
        StringBuilder material = new StringBuilder("M3_REGEX_ORACLE_HARNESS_V1\n")
                .append(M3RegexOracleCorpus.MATRIX_ROOT).append('\n')
                .append(shards.length).append('\n');
        for (int shard = 0; shard < shards.length; shard++) {
            List<M3RegexStringTrialImage.RegexSpec> specs = new ArrayList<>(M3RegexOracleCorpus.REGEX_PER_SHARD);
            for (String expression : M3RegexOracleCorpus.shard(expressions, shard)) {
                specs.add(new M3RegexStringTrialImage.RegexSpec(expression));
            }
            shards[shard] = M3RegexStringTrialImage.compile(specs, inputs, monitor);
            material.append(shards[shard].rootHash()).append('\n');
        }
        return new M3RegexOracleHarness(
                expressions, inputs, shards, M3RegexOracleCorpus.sha256(material.toString()));
    }

    public int regexCount() {
        return expressions.size();
    }

    public int stringCount() {
        return inputs.size();
    }

    public long pairCount() {
        return (long) expressions.size() * inputs.size();
    }

    public int shardCount() {
        return shards.length;
    }

    public String matrixRoot() {
        return M3RegexOracleCorpus.MATRIX_ROOT;
    }

    /** Digest over the matrix root and every shard image root (outcomes and signals). */
    public String oracleRoot() {
        return oracleRoot;
    }

    public M3RegexStringTrialImage shard(int shard) {
        return shards[shard];
    }

    public String expression(int regexOrdinal) {
        return expressions.get(regexOrdinal);
    }

    public String input(int stringOrdinal) {
        return inputs.get(stringOrdinal);
    }

    public boolean regexValid(int regexOrdinal) {
        return shardOf(regexOrdinal).regexValid(rowOf(regexOrdinal));
    }

    public boolean matches(int regexOrdinal, int stringOrdinal) {
        return shardOf(regexOrdinal).matches(rowOf(regexOrdinal), stringOrdinal);
    }

    public boolean lookingAt(int regexOrdinal, int stringOrdinal) {
        return shardOf(regexOrdinal).lookingAt(rowOf(regexOrdinal), stringOrdinal);
    }

    public boolean find(int regexOrdinal, int stringOrdinal) {
        return shardOf(regexOrdinal).find(rowOf(regexOrdinal), stringOrdinal);
    }

    /**
     * Runs {@code candidate} over every pair. A {@code CANNOT_MATCH} verdict is counted as pruned
     * when the oracle agrees and as unsound when {@code find} succeeded; the first
     * {@code maxRetained} contradictions are kept in (regex, input) order.
     */
    public Report check(String candidateName, Candidate candidate, int maxRetained) {
        Objects.requireNonNull(candidate, "candidate");
        if (maxRetained < 0) {
            throw new IllegalArgumentException("negative maxRetained");
        }
        long findTrue = 0L;
        long pruned = 0L;
        long unsound = 0L;
        List<Mismatch> retained = new ArrayList<>();
        for (int regex = 0; regex < expressions.size(); regex++) {
            String expression = expressions.get(regex);
            for (int string = 0; string < inputs.size(); string++) {
                boolean found = find(regex, string);
                if (found) {
                    findTrue++;
                }
                if (candidate.classify(expression, inputs.get(string)) != Verdict.CANNOT_MATCH) {
                    continue;
                }
                if (!found) {
                    pruned++;
                } else {
                    unsound++;
                    if (retained.size() < maxRetained) {
                        retained.add(new Mismatch(regex, string));
                    }
                }
            }
        }
        return new Report(oracleRoot, candidateName, pairCount(), findTrue, pruned, unsound, retained, "");
    }

    /** Never prunes; the trivially sound baseline. */
    public static Candidate neverPrunes() {
        return (regex, input) -> Verdict.MAY_MATCH;
    }

    /**
     * Prunes through {@link M3RegexShape}: a provable literal, strict prefix, strict suffix or
     * strict exact shape cannot {@code find} an input that lacks the literal in that position;
     * every general shape is {@code MAY_MATCH}.
     */
    public static Candidate literalShape() {
        return (regex, input) -> {
            M3RegexShape shape = M3RegexShape.analyze(regex);
            if (!shape.specialized()) {
                return Verdict.MAY_MATCH;
            }
            String literal = shape.literal();
            boolean possible = switch (shape.kind()) {
                case LITERAL -> input.contains(literal);
                case STRICT_PREFIX -> input.startsWith(literal);
                case STRICT_SUFFIX -> input.endsWith(literal);
                case STRICT_EXACT -> input.equals(literal);
                case GENERAL -> true;
            };
            return possible ? Verdict.MAY_MATCH : Verdict.CANNOT_MATCH;
        };
    }

    private M3RegexStringTrialImage shardOf(int regexOrdinal) {
        return shards[regexOrdinal / M3RegexOracleCorpus.REGEX_PER_SHARD];
    }

    private static int rowOf(int regexOrdinal) {
        return regexOrdinal % M3RegexOracleCorpus.REGEX_PER_SHARD;
    }
}
