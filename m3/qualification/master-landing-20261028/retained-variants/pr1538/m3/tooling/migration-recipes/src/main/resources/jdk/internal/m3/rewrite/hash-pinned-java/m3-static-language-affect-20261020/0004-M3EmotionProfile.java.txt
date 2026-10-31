// SPDX-License-Identifier: Apache-2.0
package com.m3.ast;

import java.util.Arrays;
import java.util.Objects;

/**
 * Sparse dynamic affect composition over immutable ontology coordinates.
 *
 * <p>Emotion IDs are sorted unique; weights are unsigned Q16 stored in char. Context changes this
 * profile, never the underlying ontology.</p>
 */
public final class M3EmotionProfile {
    private final M3EmotionOntology ontology;
    private final int[] emotionIds;
    private final char[] weightsQ16;

    public M3EmotionProfile(
            M3EmotionOntology ontology,
            int[] emotionIds,
            char[] weightsQ16) {
        this.ontology = Objects.requireNonNull(ontology, "ontology");
        this.emotionIds = Objects.requireNonNull(emotionIds, "emotionIds").clone();
        this.weightsQ16 = Objects.requireNonNull(weightsQ16, "weightsQ16").clone();
        if (this.emotionIds.length != this.weightsQ16.length) {
            throw new IllegalArgumentException("emotion/weight length mismatch");
        }
        int previous = -1;
        for (int id : this.emotionIds) {
            if (id <= previous || id >= ontology.emotionCount()) {
                throw new IllegalArgumentException("emotion IDs must be ascending unique ontology IDs");
            }
            previous = id;
        }
    }

    public int size() { return emotionIds.length; }
    public int emotionIdAt(int index) { return emotionIds[Objects.checkIndex(index, size())]; }
    public int weightQ16At(int index) { return weightsQ16[Objects.checkIndex(index, size())]; }
    public double weightAt(int index) { return weightQ16At(index) / 65535.0d; }

    /**
     * Weighted average for one dimensional axis. Missing coordinates contribute no denominator.
     */
    public java.util.OptionalInt weightedAxisQ15(M3AffectAxis axis) {
        Objects.requireNonNull(axis, "axis");
        long weighted = 0L;
        long totalWeight = 0L;
        for (int index = 0; index < size(); index++) {
            var coordinate = ontology.affectQ15(emotionIds[index], axis);
            if (coordinate.isEmpty()) continue;
            int weight = weightQ16At(index);
            weighted += (long) coordinate.getAsInt() * weight;
            totalWeight += weight;
        }
        if (totalWeight == 0L) return java.util.OptionalInt.empty();
        return java.util.OptionalInt.of((int) (weighted / totalWeight));
    }

    public int[] emotionIds() { return emotionIds.clone(); }

    @Override public boolean equals(Object other) {
        return other instanceof M3EmotionProfile profile
                && ontology == profile.ontology
                && Arrays.equals(emotionIds, profile.emotionIds)
                && Arrays.equals(weightsQ16, profile.weightsQ16);
    }

    @Override public int hashCode() {
        int hash = 31 * System.identityHashCode(ontology) + Arrays.hashCode(emotionIds);
        return 31 * hash + Arrays.hashCode(weightsQ16);
    }
}
