/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 *
 * @test
 * @summary Mechanical M3 internal precompute over shared canonical IDs
 * @modules java.base/jdk.internal.mindex
 * @run main M3InternalPrecomputeTest
 */

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import jdk.internal.mindex.M3AttackParityIndex;
import jdk.internal.mindex.M3ConditionalTable;
import jdk.internal.mindex.M3FormulaProbabilityIndex;
import jdk.internal.mindex.M3FormulaTable;
import jdk.internal.mindex.M3FormulaTruthIndex;
import jdk.internal.mindex.M3PostingIndex;
import jdk.internal.mindex.M3ProbabilityVector;
import jdk.internal.mindex.M3ReasoningGraph;
import jdk.internal.mindex.M3ReasoningRelationKind;
import jdk.internal.mindex.M3RegexDomainIndex;
import jdk.internal.mindex.M3RelationQueryIndex;
import jdk.internal.mindex.M3StringBacking;
import jdk.internal.mindex.M3Truth;

public class M3InternalPrecomputeTest {
    private static long checks;

    public static void main(String[] args) {
        postings();
        regex();
        formulasAndProbability();
        relations();
        System.out.println("M3_INTERNAL_PRECOMPUTE_PASS|checks=" + checks);
    }

    private static void postings() {
        M3PostingIndex.Builder builder = M3PostingIndex.builder();
        int doc0 = builder.addDocument(11L, 12L, 13L);
        int doc1 = builder.addDocument(11L, 14L, 13L);
        int doc2 = builder.addDocument(15L, 12L, 13L);
        check(doc0 == 0 && doc1 == 1 && doc2 == 2, "document row coordinates");
        M3PostingIndex index =
                builder.relationPair(doc0, 11L, 13L)
                        .relationPair(doc1, 11L, 13L)
                        .build();

        eq(new int[] {0, 1}, index.documents(11L), "token posting");
        eq(new int[] {0, 2}, index.documentsContainingAll(12L, 13L), "token AND");
        eq(new int[] {0, 2}, index.adjacentPairDocuments(12L, 13L), "adjacent pair");
        eq(new int[] {0}, index.phraseCandidates(11L, 12L, 13L), "phrase candidate");
        eq(new int[] {0, 1}, index.relationPairDocuments(11L, 13L), "relation pair");
        check(index.retainedPrimitiveBytes() > 0L, "posting payload accounting");
    }

    private static void regex() {
        TestBacking backing =
                new TestBacking(
                        Map.of(
                                101L, "alpha-001",
                                102L, "alpha-ERROR",
                                103L, "beta-ERROR",
                                104L, "omega"));

        M3RegexDomainIndex image =
                M3RegexDomainIndex.compile(
                        backing,
                        new long[] {104L, 102L, 101L, 103L, 102L},
                        "alpha-.*");

        check(image.size() == 4, "regex canonical ID dedup");
        check(image.matches(101L), "regex matches alpha");
        check(image.matches(102L), "regex matches alpha error");
        check(!image.matches(103L), "regex excludes beta");
        check(image.find(102L), "regex find");
        check(image.lookingAt(101L), "regex lookingAt");
        check(image.firstFindStart(101L) == 0, "regex first span start");
        check(image.firstFindEnd(101L) == "alpha-001".length(), "regex first span end");
        eq(new long[] {101L, 102L}, image.ids(M3RegexDomainIndex.Mode.MATCHES), "regex IDs");
        check(image.retainedPrimitiveBytes() > 0L, "regex payload accounting");

        expectIAE(
                () ->
                        M3RegexDomainIndex.compile(
                                backing,
                                new long[] {101L, 102L},
                                ".*",
                                0,
                                new M3RegexDomainIndex.Budget(1, 100, 1024)),
                "regex value budget");
    }

    private static void formulasAndProbability() {
        long ATOM_A = 201L;
        long ATOM_B = 202L;
        long F_A = 301L;
        long F_B = 302L;
        long F_NOT_A = 303L;
        long F_AND = 304L;
        long F_IMPLIES = 305L;

        M3FormulaTable formulas =
                M3FormulaTable.builder()
                        .atom(F_A, ATOM_A)
                        .atom(F_B, ATOM_B)
                        .not(F_NOT_A, F_A)
                        .and(F_AND, F_A, F_B)
                        .implies(F_IMPLIES, F_A, F_B)
                        .build();

        Map<Long, Map<Long, M3Truth>> modelTruth = new HashMap<>();
        modelTruth.put(
                401L,
                Map.of(
                        ATOM_A, M3Truth.TRUE,
                        ATOM_B, M3Truth.TRUE));
        modelTruth.put(
                402L,
                Map.of(
                        ATOM_A, M3Truth.TRUE,
                        ATOM_B, M3Truth.FALSE));
        modelTruth.put(
                403L,
                Map.of(
                        ATOM_A, M3Truth.BOTH,
                        ATOM_B, M3Truth.UNKNOWN));

        M3FormulaTruthIndex truth =
                M3FormulaTruthIndex.compile(
                        formulas,
                        new long[] {403L, 401L, 402L},
                        (model, atom) -> modelTruth.get(model).get(atom));

        check(truth.truth(F_AND, 401L) == M3Truth.TRUE, "formula AND true");
        check(truth.truth(F_AND, 402L) == M3Truth.FALSE, "formula AND false");
        check(truth.truth(F_NOT_A, 403L) == M3Truth.BOTH, "formula NOT both");
        eq(new long[] {401L}, truth.modelsWithTruth(F_IMPLIES, M3Truth.TRUE),
                "implication true model");
        eq(new long[] {402L}, truth.modelsWithTruth(F_IMPLIES, M3Truth.FALSE),
                "implication false model");

        M3ConditionalTable conditionals =
                M3ConditionalTable.builder(formulas)
                        .add(501L, F_A, F_B, 7)
                        .build();
        M3ConditionalTable.AssessmentImage assessments = conditionals.assess(truth);
        check(
                assessments.assessment(401L, 501L)
                        == M3ConditionalTable.Assessment.VERIFIED,
                "conditional verified");
        check(
                assessments.assessment(402L, 501L)
                        == M3ConditionalTable.Assessment.FALSIFIED,
                "conditional falsified");
        check(
                assessments.assessment(403L, 501L)
                        == M3ConditionalTable.Assessment.UNRESOLVED,
                "conditional unresolved");
        check(assessments.worldRank(402L) == 8, "conditional rank");

        M3ProbabilityVector probabilities =
                M3ProbabilityVector.builder()
                        .weight(401L, 2)
                        .weight(402L, 1)
                        .weight(403L, 1)
                        .build();
        check(probabilities.isNormalized(), "Q32 normalized");

        M3FormulaProbabilityIndex probabilityImage =
                M3FormulaProbabilityIndex.compile(truth, probabilities);
        long aTrue = probabilityImage.massQ32(F_A, M3Truth.TRUE);
        long aBoth = probabilityImage.massQ32(F_A, M3Truth.BOTH);
        check(aTrue + aBoth == M3ProbabilityVector.ONE_Q32,
                "formula asserted-positive probability mass");
        check(probabilityImage.retainedPrimitiveBytes() > 0L, "formula probability payload");
    }

    private static void relations() {
        M3ReasoningGraph graph =
                M3ReasoningGraph.builder()
                        .relation(601L, M3ReasoningRelationKind.ATTACK, 602L, 11, 1L)
                        .relation(602L, M3ReasoningRelationKind.ATTACK, 603L, 12, 2L)
                        .relation(603L, M3ReasoningRelationKind.ATTACK, 601L, 13, 4L)
                        .relation(601L, M3ReasoningRelationKind.SUPPORT, 604L, 21, 8L)
                        .acceptanceMasks(601L, 0x1L, 0x3L)
                        .rankingQ31(601L, 17)
                        .probabilityQ31(601L, Integer.MAX_VALUE / 2)
                        .build();

        check(
                graph.hasRelation(601L, M3ReasoningRelationKind.ATTACK, 602L),
                "direct attack");
        check(
                !graph.hasRelation(601L, M3ReasoningRelationKind.ATTACK, 604L),
                "typed relation separation");
        check(graph.skepticalMask(601L) == 0x1L, "skeptical lane");
        check(graph.credulousMask(601L) == 0x3L, "credulous lane");

        M3RelationQueryIndex attack =
                M3RelationQueryIndex.compile(graph, M3ReasoningRelationKind.ATTACK);
        check(attack.reachable(601L, 603L), "transitive attack reachability");
        check(attack.closure().sameComponent(601L, 603L), "SCC mutual reachability");
        check(
                attack.attackParity().orElseThrow().indirectlyAttacks(601L, 602L),
                "odd path attack");
        check(
                attack.attackParity().orElseThrow().supports(601L, 603L),
                "even path support");
        check(
                attack.attackParity().orElseThrow().supports(601L, 601L),
                "zero-length support");
        check(attack.retainedPrimitiveBytes() > 0L, "relation query payload");

        M3RelationQueryIndex support =
                M3RelationQueryIndex.compile(graph, M3ReasoningRelationKind.SUPPORT);
        check(support.directlyRelated(601L, 604L), "direct support");
        check(support.attackParity().isEmpty(), "no attack parity for support lane");

        expectIAE(
                () ->
                        M3AttackParityIndex.compile(
                                graph,
                                new M3AttackParityIndex.Budget(2, Long.MAX_VALUE)),
                "attack node budget");
    }

    private static void eq(int[] expected, int[] actual, String label) {
        check(Arrays.equals(expected, actual),
                label + " expected=" + Arrays.toString(expected)
                        + " actual=" + Arrays.toString(actual));
    }

    private static void eq(long[] expected, long[] actual, String label) {
        check(Arrays.equals(expected, actual),
                label + " expected=" + Arrays.toString(expected)
                        + " actual=" + Arrays.toString(actual));
    }

    private static void expectIAE(Runnable action, String label) {
        try {
            action.run();
            throw new AssertionError(label + " did not throw");
        } catch (IllegalArgumentException expected) {
            checks++;
        }
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }

    private static final class TestBacking implements M3StringBacking {
        private final Map<Long, String> values;

        TestBacking(Map<Long, String> values) {
            this.values = Map.copyOf(values);
        }

        @Override
        public void close() {}

        @Override
        public int length(long id) {
            return value(id).length();
        }

        @Override
        public int utf8Length(long id) {
            return value(id).getBytes(StandardCharsets.UTF_8).length;
        }

        @Override
        public int codePointCount(long id) {
            String value = value(id);
            return value.codePointCount(0, value.length());
        }

        @Override
        public int unpairedSurrogateCount(long id) {
            String value = value(id);
            int count = 0;
            for (int index = 0; index < value.length(); index++) {
                char unit = value.charAt(index);
                if (Character.isHighSurrogate(unit)) {
                    if (index + 1 < value.length()
                            && Character.isLowSurrogate(value.charAt(index + 1))) {
                        index++;
                    } else {
                        count++;
                    }
                } else if (Character.isLowSurrogate(unit)) {
                    count++;
                }
            }
            return count;
        }

        @Override
        public int hashCode(long id) {
            return value(id).hashCode();
        }

        @Override
        public char charAt(long id, int index) {
            return value(id).charAt(index);
        }

        @Override
        public CharBuffer utf16View(long id) {
            return CharBuffer.wrap(value(id)).asReadOnlyBuffer();
        }

        @Override
        public ByteBuffer utf8View(long id) {
            return ByteBuffer.wrap(value(id).getBytes(StandardCharsets.UTF_8))
                    .asReadOnlyBuffer();
        }

        @Override
        public String materialize(long id) {
            return value(id);
        }

        private String value(long id) {
            String value = values.get(id);
            if (value == null) throw new IllegalArgumentException("unknown test ID " + id);
            return value;
        }
    }
}
