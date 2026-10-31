// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
package com.m3.ast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.TreeMap;

/**
 * Frozen categorical + dimensional emotion ontology over M3 canonical coordinates.
 *
 * <p>The ontology is immutable. Runtime emotion is a separate sparse composition. Labels and
 * relation names are resolved through the product M3 lexicon/string coordinate space; no second
 * String table is retained.</p>
 */
public final class M3EmotionOntology {
    /** Cold-path bridge from reviewed label text to a positive M3 lexicon/string coordinate. */
    @FunctionalInterface
    public interface LabelIds {
        long id(String label);
    }

    public static final int TYPE_PRIMARY = 1;
    public static final int TYPE_INTENSITY = 2;
    public static final int TYPE_BLEND = 3;
    public static final int TYPE_FINE_GRAINED = 4;

    private final M3AtomStore atoms;
    private final M3RelationStore relations;
    private final int[] emotionAtoms;
    private final long[] labelIds;
    private final int[] affectOffsets;
    private final byte[] affectAxes;
    private final short[] affectValues;
    private final int[] labelSlots;

    private M3EmotionOntology(
            M3AtomStore atoms,
            M3RelationStore relations,
            int[] emotionAtoms,
            long[] labelIds,
            int[] affectOffsets,
            byte[] affectAxes,
            short[] affectValues,
            int[] labelSlots) {
        this.atoms = atoms;
        this.relations = relations;
        this.emotionAtoms = emotionAtoms;
        this.labelIds = labelIds;
        this.affectOffsets = affectOffsets;
        this.affectAxes = affectAxes;
        this.affectValues = affectValues;
        this.labelSlots = labelSlots;
    }

    public static Builder builder(LabelIds labels) {
        return new Builder(labels);
    }

    public M3AtomStore atoms() { return atoms; }
    public M3RelationStore relations() { return relations; }
    public int emotionCount() { return emotionAtoms.length; }

    public int emotionAtom(int emotionId) {
        return emotionAtoms[checkEmotion(emotionId)];
    }

    public long labelId(int emotionId) {
        return labelIds[checkEmotion(emotionId)];
    }

    public OptionalInt emotionByLabelId(long labelId) {
        if (labelId <= 0L) return OptionalInt.empty();
        int mask = labelSlots.length - 1;
        int slot = mix32(labelId) & mask;
        while (true) {
            int encoded = labelSlots[slot];
            if (encoded == 0) return OptionalInt.empty();
            int emotion = encoded - 1;
            if (labelIds[emotion] == labelId) return OptionalInt.of(emotion);
            slot = (slot + 1) & mask;
        }
    }

    public OptionalInt affectQ15(int emotionId, M3AffectAxis axis) {
        int row = checkEmotion(emotionId);
        int wanted = Objects.requireNonNull(axis, "axis").ordinal();
        int low = affectOffsets[row];
        int high = affectOffsets[row + 1];
        while (low < high) {
            int middle = (low + high) >>> 1;
            int candidate = Byte.toUnsignedInt(affectAxes[middle]);
            if (candidate < wanted) low = middle + 1;
            else high = middle;
        }
        return low < affectOffsets[row + 1] && Byte.toUnsignedInt(affectAxes[low]) == wanted
                ? OptionalInt.of(affectValues[low])
                : OptionalInt.empty();
    }

    public boolean related(int leftEmotion, long relationWordId, int rightEmotion) {
        return relations.related(
                emotionAtom(leftEmotion), relationWordId, emotionAtom(rightEmotion));
    }

    public long primitiveBytes() {
        return relations.primitiveBytes()
                + 4L * (emotionAtoms.length + affectOffsets.length + labelSlots.length)
                + 8L * labelIds.length
                + affectAxes.length
                + 2L * affectValues.length;
    }

    private int checkEmotion(int emotionId) {
        return Objects.checkIndex(emotionId, emotionAtoms.length);
    }

    /**
     * Built-in Plutchik-style categorical spine. It intentionally supplies no empirical VAD
     * numbers; dimensional values must come from an explicitly licensed/provenance-bound donor.
     */
    public static M3EmotionOntology plutchikCore(LabelIds labels) {
        Builder b = builder(labels);
        int joy = b.add("joy", TYPE_PRIMARY);
        int trust = b.add("trust", TYPE_PRIMARY);
        int fear = b.add("fear", TYPE_PRIMARY);
        int surprise = b.add("surprise", TYPE_PRIMARY);
        int sadness = b.add("sadness", TYPE_PRIMARY);
        int disgust = b.add("disgust", TYPE_PRIMARY);
        int anger = b.add("anger", TYPE_PRIMARY);
        int anticipation = b.add("anticipation", TYPE_PRIMARY);

        int ecstasy = b.add("ecstasy", TYPE_INTENSITY);
        int serenity = b.add("serenity", TYPE_INTENSITY);
        int admiration = b.add("admiration", TYPE_INTENSITY);
        int acceptance = b.add("acceptance", TYPE_INTENSITY);
        int terror = b.add("terror", TYPE_INTENSITY);
        int apprehension = b.add("apprehension", TYPE_INTENSITY);
        int amazement = b.add("amazement", TYPE_INTENSITY);
        int distraction = b.add("distraction", TYPE_INTENSITY);
        int grief = b.add("grief", TYPE_INTENSITY);
        int pensiveness = b.add("pensiveness", TYPE_INTENSITY);
        int loathing = b.add("loathing", TYPE_INTENSITY);
        int boredom = b.add("boredom", TYPE_INTENSITY);
        int rage = b.add("rage", TYPE_INTENSITY);
        int annoyance = b.add("annoyance", TYPE_INTENSITY);
        int vigilance = b.add("vigilance", TYPE_INTENSITY);
        int interest = b.add("interest", TYPE_INTENSITY);

        b.intensity(ecstasy, joy, serenity);
        b.intensity(admiration, trust, acceptance);
        b.intensity(terror, fear, apprehension);
        b.intensity(amazement, surprise, distraction);
        b.intensity(grief, sadness, pensiveness);
        b.intensity(loathing, disgust, boredom);
        b.intensity(rage, anger, annoyance);
        b.intensity(vigilance, anticipation, interest);

        b.opposites(joy, sadness);
        b.opposites(trust, disgust);
        b.opposites(fear, anger);
        b.opposites(surprise, anticipation);

        int love = b.add("love", TYPE_BLEND);
        int submission = b.add("submission", TYPE_BLEND);
        int awe = b.add("awe", TYPE_BLEND);
        int disapproval = b.add("disapproval", TYPE_BLEND);
        int remorse = b.add("remorse", TYPE_BLEND);
        int contempt = b.add("contempt", TYPE_BLEND);
        int aggressiveness = b.add("aggressiveness", TYPE_BLEND);
        int optimism = b.add("optimism", TYPE_BLEND);

        b.blend(love, joy, trust);
        b.blend(submission, trust, fear);
        b.blend(awe, fear, surprise);
        b.blend(disapproval, surprise, sadness);
        b.blend(remorse, sadness, disgust);
        b.blend(contempt, disgust, anger);
        b.blend(aggressiveness, anger, anticipation);
        b.blend(optimism, anticipation, joy);
        return b.freeze();
    }

    /**
     * GoEmotions label vocabulary only: 27 fine-grained emotions plus neutral. No dataset rows are
     * embedded and no crosswalk is invented here.
     */
    public static M3EmotionOntology goEmotionsVocabulary(LabelIds labels) {
        Builder b = builder(labels);
        String[] names = {
            "admiration", "amusement", "anger", "annoyance", "approval", "caring",
            "confusion", "curiosity", "desire", "disappointment", "disapproval", "disgust",
            "embarrassment", "excitement", "fear", "gratitude", "grief", "joy", "love",
            "nervousness", "optimism", "pride", "realization", "relief", "remorse", "sadness",
            "surprise", "neutral"
        };
        for (String name : names) b.add(name, TYPE_FINE_GRAINED);
        return b.freeze();
    }

    public static final class Builder {
        private record DraftEmotion(
                int provisionalId,
                String label,
                long labelId,
                int type,
                M3AffectVector affect) { }

        private record DraftRelation(
                int from,
                int to,
                String relationLabel,
                M3InteractionMode mode,
                int ordinal,
                long metadata) { }

        private final LabelIds labels;
        private final TreeMap<String, DraftEmotion> emotions = new TreeMap<>();
        private final List<DraftRelation> relations = new ArrayList<>();
        private boolean frozen;

        private Builder(LabelIds labels) {
            this.labels = Objects.requireNonNull(labels, "labels");
        }

        public int add(String label, int type) {
            return add(label, type, M3AffectVector.builder().build());
        }

        public int add(String label, int type, M3AffectVector affect) {
            ensureMutable();
            String normalized = normalize(label);
            if (type < 0) throw new IllegalArgumentException("emotion type < 0");
            DraftEmotion existing = emotions.get(normalized);
            if (existing != null) return existing.provisionalId();
            long labelId = positive(labels.id(normalized), "emotion label");
            int provisional = emotions.size();
            emotions.put(normalized, new DraftEmotion(
                    provisional, normalized, labelId, type,
                    Objects.requireNonNull(affect, "affect")));
            return provisional;
        }

        public Builder relation(
                int from, String relationLabel, int to, M3InteractionMode mode) {
            ensureMutable();
            requireProvisional(from);
            requireProvisional(to);
            relations.add(new DraftRelation(
                    from, to, normalize(relationLabel),
                    Objects.requireNonNull(mode, "mode"), 0, 0L));
            return this;
        }

        public Builder opposites(int left, int right) {
            return relation(left, "opposite", right, M3InteractionMode.OPPOSE)
                    .relation(right, "opposite", left, M3InteractionMode.OPPOSE);
        }

        /** strong -> primary -> weak */
        public Builder intensity(int strong, int primary, int weak) {
            return relation(strong, "intensity_parent", primary, M3InteractionMode.CONNECT)
                    .relation(primary, "intensity_child", strong, M3InteractionMode.CONNECT)
                    .relation(primary, "intensity_parent", weak, M3InteractionMode.CONNECT)
                    .relation(weak, "intensity_child", primary, M3InteractionMode.CONNECT);
        }

        public Builder blend(int blend, int... components) {
            if (components == null || components.length < 2) {
                throw new IllegalArgumentException("blend requires at least two components");
            }
            for (int component : components) {
                relation(blend, "blend_component", component, M3InteractionMode.CONNECT);
            }
            return this;
        }

        public M3EmotionOntology freeze() {
            ensureMutable();
            frozen = true;
            List<DraftEmotion> sorted = List.copyOf(emotions.values());
            int count = sorted.size();
            int[] oldToNew = new int[count];
            for (int newId = 0; newId < count; newId++) {
                oldToNew[sorted.get(newId).provisionalId()] = newId;
            }

            M3AtomStore.Builder atomBuilder = M3AtomStore.builder();
            int[] emotionAtoms = new int[count];
            long[] labelIds = new long[count];
            int[] affectOffsets = new int[count + 1];
            int affectCount = 0;
            for (int id = 0; id < count; id++) {
                DraftEmotion emotion = sorted.get(id);
                emotionAtoms[id] = atomBuilder.intern(
                        M3AtomDomain.EMOTION,
                        emotion.type(),
                        emotion.labelId(),
                        emotion.affect().packedCore());
                labelIds[id] = emotion.labelId();
                affectCount = Math.addExact(affectCount, emotion.affect().size());
                affectOffsets[id + 1] = affectCount;
            }
            M3AtomStore atoms = atomBuilder.freeze();

            byte[] affectAxes = new byte[affectCount];
            short[] affectValues = new short[affectCount];
            int affectCursor = 0;
            for (DraftEmotion emotion : sorted) {
                for (int index = 0; index < emotion.affect().size(); index++) {
                    affectAxes[affectCursor] = (byte) emotion.affect().axisAt(index).ordinal();
                    affectValues[affectCursor] = (short) emotion.affect().valueQ15At(index);
                    affectCursor++;
                }
            }

            M3RelationStore.Builder relationBuilder = new M3RelationStore.Builder(atoms);
            for (DraftRelation relation : relations) {
                long relationId = positive(labels.id(relation.relationLabel()), "relation label");
                relationBuilder.edge(
                        emotionAtoms[oldToNew[relation.from()]],
                        relationId,
                        emotionAtoms[oldToNew[relation.to()]],
                        relation.ordinal(),
                        relation.metadata(),
                        relation.mode());
            }
            M3RelationStore relationStore = relationBuilder.freeze();

            int slots = 2;
            while (slots < Math.max(2, count * 2)) slots <<= 1;
            int[] labelSlots = new int[slots];
            int mask = slots - 1;
            for (int emotion = 0; emotion < count; emotion++) {
                int slot = mix32(labelIds[emotion]) & mask;
                while (labelSlots[slot] != 0) slot = (slot + 1) & mask;
                labelSlots[slot] = emotion + 1;
            }

            return new M3EmotionOntology(
                    atoms, relationStore, emotionAtoms, labelIds,
                    affectOffsets, affectAxes, affectValues, labelSlots);
        }

        private void requireProvisional(int id) {
            if (id < 0 || id >= emotions.size()) {
                throw new IllegalArgumentException("unknown emotion id: " + id);
            }
        }

        private void ensureMutable() {
            if (frozen) throw new IllegalStateException("emotion builder already frozen");
        }
    }

    private static long positive(long value, String label) {
        if (value <= 0L) throw new IllegalArgumentException(label + " coordinate must be positive");
        return value;
    }

    private static String normalize(String value) {
        String normalized = Objects.requireNonNull(value, "value").strip()
                .toLowerCase(java.util.Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
        if (normalized.isEmpty()) throw new IllegalArgumentException("empty emotion/relation label");
        return normalized;
    }

    private static int mix32(long value) {
        long x = value;
        x ^= x >>> 33;
        x *= 0xff51afd7ed558ccdL;
        x ^= x >>> 33;
        x *= 0xc4ceb9fe1a85ec53L;
        x ^= x >>> 33;
        return (int) (x ^ (x >>> 32));
    }
}
