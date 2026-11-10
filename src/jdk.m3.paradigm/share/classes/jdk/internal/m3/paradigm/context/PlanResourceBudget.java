/* ******************************************************************************
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
package jdk.internal.m3.paradigm.context;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.BooleanSupplier;
import java.util.stream.IntStream;

/**
 * Host-owned aggregate estimates for COP candidate selection, never execution permission.
 * All dimensions are summed across leaves. In particular latency is a conservative serial
 * estimate and memory is summed declared working memory, not a scheduler or an OS guarantee.
 * The first lexicographically ranked feasible combination is selected by bounded search.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/dynamic/PlanResourceBudget.java}
 * blob {@code 38be218f7a0f60182f9ba1a115c1612a370aeaa7} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public record PlanResourceBudget(Resources maximum, long maxSearchStates, double minimumScore) {
    private static final int MAX_LEAVES = 32;
    private static final int MAX_CHOICES = 1024;

    public PlanResourceBudget {
        maximum = Objects.requireNonNull(maximum, "maximum");
        maximum.requireBoundedInput();
        if (maxSearchStates < 1 || maxSearchStates > 1_000_000) {
            throw new IllegalArgumentException("search states must be within [1,1000000]");
        }
        score(minimumScore);
        minimumScore = minimumScore == 0.0 ? 0.0 : minimumScore;
    }

    /** Exact decimal sums of host estimates; no silent zero for missing metadata. */
    public record Resources(BigDecimal cost, BigDecimal latencyMicros,
                            BigDecimal cpuUnits, BigDecimal memoryBytes) {
        public static final Resources ZERO = new Resources(BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO);

        public Resources {
            cost = amount(cost);
            latencyMicros = amount(latencyMicros);
            cpuUnits = amount(cpuUnits);
            memoryBytes = amount(memoryBytes);
        }

        public static Resources of(long cost, long latencyMicros, long cpuUnits, long memoryBytes) {
            return new Resources(BigDecimal.valueOf(cost), BigDecimal.valueOf(latencyMicros),
                    BigDecimal.valueOf(cpuUnits), BigDecimal.valueOf(memoryBytes));
        }

        public static Resources from(CapabilityProfile profile) {
            Objects.requireNonNull(profile, "profile");
            return new Resources(metric(profile, CapabilityProfile.COST),
                    metric(profile, CapabilityProfile.LATENCY_MICROS),
                    metric(profile, CapabilityProfile.CPU_UNITS),
                    metric(profile, CapabilityProfile.MEMORY_BYTES));
        }

        public Resources plus(Resources other) {
            Objects.requireNonNull(other, "other");
            return new Resources(cost.add(other.cost), latencyMicros.add(other.latencyMicros),
                    cpuUnits.add(other.cpuUnits), memoryBytes.add(other.memoryBytes));
        }

        public boolean fits(Resources limit) {
            return cost.compareTo(limit.cost) <= 0 && latencyMicros.compareTo(limit.latencyMicros) <= 0
                    && cpuUnits.compareTo(limit.cpuUnits) <= 0 && memoryBytes.compareTo(limit.memoryBytes) <= 0;
        }

        private Resources minimum(Resources other) {
            return new Resources(cost.min(other.cost), latencyMicros.min(other.latencyMicros),
                    cpuUnits.min(other.cpuUnits), memoryBytes.min(other.memoryBytes));
        }

        private List<String> fields() {
            return List.of(cost.toEngineeringString(), latencyMicros.toEngineeringString(),
                    cpuUnits.toEngineeringString(), memoryBytes.toEngineeringString());
        }

        private static BigDecimal metric(CapabilityProfile profile, String key) {
            Double value = profile.metrics().get(key);
            if (value == null || !Double.isFinite(value) || value < 0) {
                throw new IllegalArgumentException("RESOURCE_ESTIMATE_MISSING_OR_INVALID:" + key);
            }
            return BigDecimal.valueOf(value);
        }

        private void requireBoundedInput() {
            if (java.util.stream.Stream.of(cost, latencyMicros, cpuUnits, memoryBytes)
                    .anyMatch(value -> value.precision() > 1024 || Math.abs((long) value.scale()) > 1024)) {
                throw new IllegalArgumentException("resource input outside bounded decimal domain");
            }
        }

        private static BigDecimal amount(BigDecimal value) {
            Objects.requireNonNull(value, "resource amount");
            if (value.signum() < 0) {
                throw new IllegalArgumentException("resource amount outside bounded nonnegative decimal domain");
            }
            return value.signum() == 0 ? BigDecimal.ZERO : value.stripTrailingZeros();
        }
    }

    public record Candidate(String capabilityId, double score, Resources resources) {
        public Candidate {
            capabilityId = CapabilityProfile.requireId(capabilityId, "capabilityId");
            PlanResourceBudget.score(score);
            resources = Objects.requireNonNull(resources, "resources");
            resources.requireBoundedInput();
            score = score == 0.0 ? 0.0 : score;
        }
    }

    public record Slot(String path, List<Candidate> candidates) {
        public Slot {
            path = CapabilityProfile.requireId(path, "path");
            candidates = List.copyOf(Objects.requireNonNull(candidates, "candidates"));
            if (candidates.size() > MAX_CHOICES) {
                throw new IllegalArgumentException("too many candidates");
            }
            if (candidates.stream().map(Candidate::capabilityId).distinct().count() != candidates.size()) {
                throw new IllegalArgumentException("DUPLICATE_CANDIDATE:" + path);
            }
        }
    }

    /** Successful bounded candidate selection. This does not certify global optimality. */
    public record Selection(Map<String, Candidate> candidates, Resources total,
                            long visitedStates, String rootHash) {
        public Selection {
            candidates = Collections.unmodifiableMap(new TreeMap<>(candidates));
            total = Objects.requireNonNull(total, "total");
            if (visitedStates < 1 || rootHash == null || !rootHash.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("invalid selection evidence");
            }
        }
    }

    public enum Failure { NO_FEASIBLE_SUPPLIED_CANDIDATES, SEARCH_LIMIT, CANCELLED }

    public static final class SearchException extends IllegalArgumentException {
        private static final long serialVersionUID = 1L;
        private final Failure failure;
        private final long visitedStates;

        private SearchException(Failure failure, long visitedStates) {
            super("COP_BUDGET_" + failure + ":visited=" + visitedStates);
            this.failure = failure;
            this.visitedStates = visitedStates;
        }

        public Failure failure() { return failure; }
        public long visitedStates() { return visitedStates; }
    }

    public Selection select(List<Slot> slots) {
        return select(slots, () -> false);
    }

    public Selection select(List<Slot> slots, BooleanSupplier cancelled) {
        Objects.requireNonNull(slots, "slots");
        Objects.requireNonNull(cancelled, "cancelled");
        if (slots.isEmpty() || slots.size() > MAX_LEAVES) {
            throw new IllegalArgumentException("budgeted plan must contain 1..32 leaves");
        }
        if (slots.stream().map(Slot::path).distinct().count() != slots.size()) {
            throw new IllegalArgumentException("PLAN_PATH_COLLISION");
        }
        Search search = new Search(cancelled);
        search.check();
        Comparator<Candidate> order = Comparator.comparingDouble(Candidate::score).reversed()
                .thenComparing(candidate -> candidate.resources().cost())
                .thenComparing(candidate -> candidate.resources().latencyMicros())
                .thenComparing(Candidate::capabilityId);
        List<Slot> ordered = slots.stream().sorted(Comparator.comparing(Slot::path)).map(slot -> {
            search.check();
            return new Slot(slot.path(), slot.candidates().stream()
                    .filter(candidate -> candidate.score() >= minimumScore)
                    .filter(candidate -> candidate.resources().fits(maximum)).sorted(order).toList());
        }).toList();
        if (ordered.stream().anyMatch(slot -> slot.candidates().isEmpty())) {
            throw new SearchException(Failure.NO_FEASIBLE_SUPPLIED_CANDIDATES, 0);
        }
        // Per-dimension minima are an admissible lower bound, not an executable combination.
        List<Resources> suffix = new ArrayList<>(Collections.nCopies(ordered.size() + 1, Resources.ZERO));
        IntStream.range(0, ordered.size()).map(index -> ordered.size() - 1 - index).forEach(index -> {
            search.check();
            Resources minimum = ordered.get(index).candidates().stream().map(Candidate::resources)
                    .reduce(Resources::minimum).orElseThrow();
            suffix.set(index, minimum.plus(suffix.get(index + 1)));
        });
        if (!suffix.getFirst().fits(maximum)) {
            throw new SearchException(Failure.NO_FEASIBLE_SUPPLIED_CANDIDATES, 0);
        }
        Selection selected = search.visit(ordered, suffix, 0, Resources.ZERO, new TreeMap<>());
        if (selected == null) {
            throw new SearchException(Failure.NO_FEASIBLE_SUPPLIED_CANDIDATES, search.visited);
        }
        return selected;
    }

    private final class Search {
        private final BooleanSupplier cancelled;
        private long visited;

        private Search(BooleanSupplier cancelled) { this.cancelled = cancelled; }

        private void check() {
            if (Thread.currentThread().isInterrupted() || cancelled.getAsBoolean()) {
                throw new SearchException(Failure.CANCELLED, visited);
            }
        }

        private Selection visit(List<Slot> slots, List<Resources> suffix, int index,
                                Resources total, TreeMap<String, Candidate> selected) {
            check();
            if (index == slots.size()) {
                return new Selection(selected, total, visited, seal(selected, total));
            }
            Slot slot = slots.get(index);
            return slot.candidates().stream().map(candidate -> {
                check();
                if (visited >= maxSearchStates) {
                    throw new SearchException(Failure.SEARCH_LIMIT, visited);
                }
                visited++;
                Resources next = total.plus(candidate.resources());
                if (!next.plus(suffix.get(index + 1)).fits(maximum)) {
                    return null;
                }
                selected.put(slot.path(), candidate);
                try {
                    return visit(slots, suffix, index + 1, next, selected);
                } finally {
                    selected.remove(slot.path());
                }
            }).filter(Objects::nonNull).findFirst().orElse(null);
        }
    }

    private String seal(Map<String, Candidate> candidates, Resources total) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream out = new DataOutputStream(bytes)) {
                write(out, "SYNEXIA-COP-RESOURCE-SELECTION/1");
                for (String value : maximum.fields()) { write(out, value); }
                out.writeLong(maxSearchStates);
                out.writeDouble(minimumScore);
                out.writeInt(candidates.size());
                for (Map.Entry<String, Candidate> entry : candidates.entrySet()) {
                    write(out, entry.getKey());
                    write(out, entry.getValue().capabilityId());
                    out.writeDouble(entry.getValue().score());
                    for (String value : entry.getValue().resources().fields()) { write(out, value); }
                }
                for (String value : total.fields()) { write(out, value); }
            }
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
        } catch (IOException exception) {
            throw new IllegalStateException("in-memory selection encoding failed", exception);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private static void write(DataOutputStream out, String value) throws IOException {
        byte[] encoded = value.getBytes(StandardCharsets.UTF_8);
        out.writeInt(encoded.length);
        out.write(encoded);
    }

    private static void score(double value) {
        if (!Double.isFinite(value) || value < 0 || value > 1) {
            throw new IllegalArgumentException("score must be finite within [0,1]");
        }
    }
}
