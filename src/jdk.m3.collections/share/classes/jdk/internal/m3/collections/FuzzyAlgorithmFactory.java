/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Deterministic fuzzy selector over semantically compatible algorithm kernels.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/FuzzyAlgorithmFactory.java}
 * blob {@code 4dff6f27dc25638af24768ec63d38e36a9750fdf} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (primitives algorithm candidates (v6.3): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class FuzzyAlgorithmFactory {
    private static final FuzzyAlgorithmFactory STANDARD =
            new FuzzyAlgorithmFactory(StandardAlgorithmCandidates.create());

    private final List<AlgorithmCandidate> candidates;

    public FuzzyAlgorithmFactory(List<AlgorithmCandidate> candidates) {
        Objects.requireNonNull(candidates, "candidates");
        if (candidates.isEmpty()) throw new IllegalArgumentException("empty candidate registry");
        ArrayList<AlgorithmCandidate> copy = new ArrayList<>(candidates);
        copy.sort(Comparator.comparing(AlgorithmCandidate::id));
        Set<String> ids = new HashSet<>();
        for (AlgorithmCandidate candidate : copy) {
            Objects.requireNonNull(candidate, "candidate");
            if (!ids.add(candidate.id())) {
                throw new IllegalArgumentException("duplicate candidate id: " + candidate.id());
            }
        }
        this.candidates = List.copyOf(copy);
    }

    public static FuzzyAlgorithmFactory standard() {
        return STANDARD;
    }

    public List<AlgorithmCandidate> candidates() {
        return candidates;
    }

    public FuzzyAlgorithmFactory withCandidate(AlgorithmCandidate candidate) {
        Objects.requireNonNull(candidate, "candidate");
        ArrayList<AlgorithmCandidate> next = new ArrayList<>(candidates.size() + 1);
        next.addAll(candidates);
        next.add(candidate);
        return new FuzzyAlgorithmFactory(next);
    }

    public AlgorithmSelection select(AlgorithmRequirements requirements) {
        Objects.requireNonNull(requirements, "requirements");
        ArrayList<Scored> supported = new ArrayList<>();
        ArrayList<String> rejected = new ArrayList<>();
        long minimumBytes = Long.MAX_VALUE;

        for (AlgorithmCandidate candidate : candidates) {
            String rejection = candidate.rejection(requirements);
            if (rejection != null) {
                if (candidate.purpose() == requirements.purpose()) {
                    rejected.add(candidate.id() + ": " + rejection);
                }
                continue;
            }
            long bytes = candidate.estimatedBytes(requirements);
            minimumBytes = Math.min(minimumBytes, bytes);
            supported.add(new Scored(candidate, bytes));
        }

        if (supported.isEmpty()) {
            throw new IllegalArgumentException(
                    "No algorithm candidate satisfies " + requirements
                            + ". Rejections: " + String.join(" | ", rejected));
        }

        ArrayList<Ranked> ranked = new ArrayList<>(supported.size());
        for (Scored item : supported) {
            double score = score(item.candidate(), item.bytes(), minimumBytes, requirements);
            String rationale = item.candidate().description()
                    + "; score=" + String.format(Locale.ROOT, "%.2f", score)
                    + "; estimatedBytes=" + item.bytes();
            ranked.add(new Ranked(item.candidate(), item.bytes(), score, rationale));
        }

        ranked.sort(
                Comparator.comparingDouble(Ranked::score).reversed()
                        .thenComparingLong(Ranked::bytes)
                        .thenComparing(item -> item.candidate().id()));

        List<AlgorithmSelection.Alternative> alternatives =
                ranked.stream()
                        .map(item -> new AlgorithmSelection.Alternative(
                                item.candidate().id(),
                                item.candidate().implementationType(),
                                round(item.score()),
                                item.bytes(),
                                item.rationale()))
                        .toList();
        return new AlgorithmSelection(requirements, alternatives.getFirst(), alternatives);
    }

    public AlgorithmSelection selectPinned(String candidateId, AlgorithmRequirements requirements) {
        Objects.requireNonNull(candidateId, "candidateId");
        Objects.requireNonNull(requirements, "requirements");
        AlgorithmCandidate candidate = candidates.stream()
                .filter(value -> value.id().equals(candidateId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown candidate: " + candidateId));
        String rejection = candidate.rejection(requirements);
        if (rejection != null) {
            throw new IllegalArgumentException(candidateId + " rejected: " + rejection);
        }
        long bytes = candidate.estimatedBytes(requirements);
        AlgorithmSelection.Alternative pinned =
                new AlgorithmSelection.Alternative(
                        candidate.id(),
                        candidate.implementationType(),
                        100.0d,
                        bytes,
                        "pinned candidate; hard semantic contract satisfied");
        return new AlgorithmSelection(requirements, pinned, List.of(pinned));
    }

    private static double score(
            AlgorithmCandidate candidate,
            long bytes,
            long minimumBytes,
            AlgorithmRequirements requirements) {
        AlgorithmCandidate.Traits traits = candidate.traits();
        AlgorithmRequirements.Priorities priorities = requirements.priorities();

        double queryDemand = operationDemand(
                requirements.scale().queries(),
                requirements.scale().updates());
        double updateDemand = operationDemand(
                requirements.scale().updates(),
                requirements.scale().queries());

        double numerator = 0.0d;
        double denominator = 0.0d;

        numerator += priorities.memory() * memoryQuality(bytes, minimumBytes);
        denominator += priorities.memory();

        double queryVolume = volumeFactor(
                requirements.scale().queries(),
                baselineItems(requirements));
        double updateVolume = volumeFactor(
                requirements.scale().updates(),
                baselineItems(requirements));
        double preprocessingAmortization = preprocessingFactor(
                requirements.scale().queries(),
                baselineItems(requirements));

        double preprocessingWeight =
                priorities.preprocessing() * preprocessingAmortization;
        numerator += preprocessingWeight * traits.preprocessing();
        denominator += preprocessingWeight;

        double queryWeight = priorities.queryLatency() * queryDemand * queryVolume;
        numerator += queryWeight * traits.query();
        denominator += queryWeight;

        double updateWeight = priorities.updateLatency() * updateDemand * updateVolume;
        numerator += updateWeight * traits.update();
        denominator += updateWeight;

        numerator += priorities.throughput() * traits.throughput();
        denominator += priorities.throughput();

        numerator += priorities.allocation() * traits.allocation();
        denominator += priorities.allocation();

        numerator += priorities.locality() * traits.locality();
        denominator += priorities.locality();

        return denominator == 0.0d ? 50.0d : numerator / denominator;
    }

    private static long baselineItems(AlgorithmRequirements requirements) {
        return switch (requirements.purpose()) {
            case SHORTEST_PATH, GRAPH_ANALYSIS, SPANNING_FOREST, MAX_FLOW, TREE_INDEX ->
                    Math.max(1L, requirements.scale().vertices());
            case MATCHING -> Math.max(
                    1L,
                    Math.max(requirements.scale().items(), requirements.scale().vertices()));
            case TEXT_SEARCH -> Math.max(
                    1L,
                    Math.max(requirements.scale().items(), requirements.scale().patternSymbols()));
            case RANGE_INDEX, CONNECTIVITY, RANK_QUANTILE, WINDOW,
                    COORDINATE_COMPRESSION, MONOTONE_SEARCH ->
                    Math.max(1L, requirements.scale().items());
        };
    }

    private static double volumeFactor(long operations, long baselineItems) {
        double ratio = operations / (double) Math.max(1L, baselineItems);
        return Math.min(4.0d, 1.0d + 2.0d * ratio);
    }

    private static double preprocessingFactor(long queries, long baselineItems) {
        double ratio = Math.min(4.0d, queries / (double) Math.max(1L, baselineItems));
        return 1.0d / (1.0d + ratio);
    }

    private static double operationDemand(long primary, long secondary) {
        long total;
        if (primary > Long.MAX_VALUE - secondary) total = Long.MAX_VALUE;
        else total = primary + secondary;
        if (total == 0L) return 1.0d;
        return 0.25d + 1.75d * (primary / (double) total);
    }

    private static double memoryQuality(long bytes, long minimumBytes) {
        if (bytes <= 0L) return 100.0d;
        if (minimumBytes <= 0L) return 50.0d;
        return Math.min(100.0d, 100.0d * minimumBytes / (double) bytes);
    }

    private static double round(double value) {
        return Math.rint(value * 100.0d) / 100.0d;
    }

    private record Scored(AlgorithmCandidate candidate, long bytes) {}
    private record Ranked(
            AlgorithmCandidate candidate,
            long bytes,
            double score,
            String rationale) {}
}
