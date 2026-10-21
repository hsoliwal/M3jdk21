// SPDX-License-Identifier: Apache-2.0
package com.m3.ast;

import java.util.HashMap;
import java.util.Map;

/** Dependency-free qualification for immutable affect/emotion geometry. */
public final class M3AffectGeometrySelfTest {
    private static long checks;

    private M3AffectGeometrySelfTest() { }

    public static void main(String[] args) {
        Map<String, Long> ids = new HashMap<>();
        M3EmotionOntology.LabelIds labels =
                label -> ids.computeIfAbsent(label, ignored -> (long) ids.size() + 1L);

        M3EmotionOntology plutchik = M3EmotionOntology.plutchikCore(labels);
        equal(32, plutchik.emotionCount(), "Plutchik count");

        int joy = plutchik.emotionByLabelId(labels.id("joy")).orElseThrow();
        int sadness = plutchik.emotionByLabelId(labels.id("sadness")).orElseThrow();
        long opposite = labels.id("opposite");
        check(plutchik.related(joy, opposite, sadness), "joy opposite sadness");
        check(plutchik.related(sadness, opposite, joy), "sadness opposite joy");
        equal(M3InteractionKind.EMOTIONAL_CONTEXT,
                plutchik.relations().outgoingEdge(
                        plutchik.emotionAtom(joy), 0).interactionKind(),
                "emotion interaction");

        M3EmotionOntology fine = M3EmotionOntology.goEmotionsVocabulary(labels);
        equal(28, fine.emotionCount(), "GoEmotions vocabulary count");

        M3AffectVector joyAxes = M3AffectVector.builder()
                .put(M3AffectAxis.VALENCE, 25000)
                .put(M3AffectAxis.AROUSAL, 12000)
                .put(M3AffectAxis.DOMINANCE, 8000)
                .put(M3AffectAxis.GOAL_RELEVANCE, 16000)
                .build();
        M3EmotionOntology custom = buildCustom(labels, joyAxes);

        int customJoy = custom.emotionByLabelId(labels.id("custom_joy")).orElseThrow();
        equal(25000, custom.affectQ15(customJoy, M3AffectAxis.VALENCE).orElseThrow(),
                "valence");
        equal(16000, custom.affectQ15(customJoy, M3AffectAxis.GOAL_RELEVANCE).orElseThrow(),
                "appraisal");
        equal(25000,
                M3AffectVector.unpackCore(
                        custom.atoms().metadataBits(custom.emotionAtom(customJoy)),
                        M3AffectAxis.VALENCE),
                "packed valence");

        M3EmotionProfile profile = new M3EmotionProfile(
                custom, new int[] {customJoy}, new char[] {(char) 65535});
        equal(25000, profile.weightedAxisQ15(M3AffectAxis.VALENCE).orElseThrow(),
                "weighted valence");

        System.out.println("M3_AFFECT_GEOMETRY_PASS checks=" + checks);
    }

    private static M3EmotionOntology buildCustom(
            M3EmotionOntology.LabelIds labels, M3AffectVector axes) {
        M3EmotionOntology.Builder builder = M3EmotionOntology.builder(labels);
        builder.add("custom_joy", M3EmotionOntology.TYPE_FINE_GRAINED, axes);
        return builder.freeze();
    }


    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }

    private static void equal(long expected, long actual, String label) {
        checks++;
        if (expected != actual) throw new AssertionError(label + ": " + expected + " != " + actual);
    }

    private static void equal(Object expected, Object actual, String label) {
        checks++;
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(label + ": " + expected + " != " + actual);
        }
    }
}
