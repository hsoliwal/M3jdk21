/* *****************************************************************************
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
package jdk.internal.m3.collections.select;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Construction-time pressure overlay for the existing deterministic collection catalogue.
 *
 * <p>Hard collection requirements are still filtered exclusively by {@link FuzzyCollectionFactory}.
 * Pressure may reorder only already-eligible providers. It never changes ordering, equality,
 * concurrency, null, view, or mutation contracts, and it never migrates a live collection.</p>
 *
 * <p>The default probe uses effective {@link Runtime#maxMemory()} and current used heap. A caller
 * that already owns cgroup/JNI/native pressure evidence may inject a stricter snapshot without
 * changing this selector or the collection providers.</p>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/selection/AdaptiveCollectionFactory.java}
 * blob {@code 15bd9c629e5941867152dfbdfa4c0fdcd086c607} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections catalogue chain after API_UNION U3 (v6.2): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class AdaptiveCollectionFactory {
    private static final int SOFT_HEAP_PERCENT = 70;
    private static final int HARD_HEAP_PERCENT = 85;
    private static final long MIN_SELECTION_BUDGET = 1024L;
    private static final long MIN_HEAP_RESERVE = 1L << 20;
    private static final long MAX_HEAP_RESERVE = 64L << 20;

    private final FuzzyCollectionFactory factory;
    private final Probe probe;

    public AdaptiveCollectionFactory(FuzzyCollectionFactory factory, Probe probe) {
        this.factory = Objects.requireNonNull(factory, "factory");
        this.probe = Objects.requireNonNull(probe, "probe");
    }

    /** Standard catalogue with automatic JVM heap-pressure sampling. */
    public static AdaptiveCollectionFactory standard() {
        return new AdaptiveCollectionFactory(FuzzyCollectionFactory.standard(), runtimeProbe());
    }

    /** Existing custom catalogue with automatic JVM heap-pressure sampling. */
    public static AdaptiveCollectionFactory runtime(FuzzyCollectionFactory factory) {
        return new AdaptiveCollectionFactory(factory, runtimeProbe());
    }

    /**
     * Selects once from a point-in-time pressure sample. Provider construction remains lazy until
     * {@link Selection#create()} is invoked.
     */
    public <T> Selection<T> select(CollectionRequest<T> request) {
        Objects.requireNonNull(request, "request");
        CollectionPlan<T> baseline = factory.select(request);
        Snapshot snapshot = Objects.requireNonNull(probe.sample(), "pressure probe returned null");
        long budget = selectionBudget(snapshot);
        List<Candidate> ranked = rank(request, baseline, snapshot, budget);
        return new Selection<>(request, snapshot, budget, baseline, ranked);
    }

    /** Convenience for callers that only need the automatically selected empty collection. */
    public <T> T create(CollectionRequest<T> request) {
        return select(request).create();
    }

    public enum Pressure { NORMAL, SOFT, HARD }

    /**
     * Memory snapshot used only for construction-time representation ranking.
     *
     * @param availableToProcess additional process/native/cgroup availability, or -1 when unknown
     */
    public record Snapshot(long maxHeap, long committedHeap, long usedHeap,
            long availableToProcess, Pressure pressure) {
        public Snapshot {
            Objects.requireNonNull(pressure, "pressure");
            if (maxHeap <= 0 || committedHeap < 0 || usedHeap < 0
                    || committedHeap < usedHeap || committedHeap > maxHeap
                    || availableToProcess < -1) {
                throw new IllegalArgumentException("invalid collection pressure snapshot");
            }
            Pressure heapPressure = classify(usedHeap, maxHeap);
            if (pressure.ordinal() < heapPressure.ordinal()) {
                pressure = heapPressure;
            }
        }

        public long heapHeadroom() {
            return Math.max(0L, maxHeap - usedHeap);
        }

        /** Optional native/container evidence can only tighten the JVM headroom. */
        public long effectiveHeadroom() {
            long heap = heapHeadroom();
            return availableToProcess < 0 ? heap : Math.min(heap, availableToProcess);
        }
    }

    @FunctionalInterface
    public interface Probe {
        Snapshot sample();
    }

    /** Immutable adaptive ranking record; this is planning metadata, never per-element state. */
    public record Candidate(String providerId, String modelVersion, long weightedPayloadBytes,
            int baselineScore, int pressureFit, int allocationFit, int adaptiveScore) {
        public Candidate {
            Objects.requireNonNull(providerId, "providerId");
            Objects.requireNonNull(modelVersion, "modelVersion");
            if (weightedPayloadBytes < 0 || baselineScore < 0 || baselineScore > 1000
                    || pressureFit < 0 || pressureFit > 1000
                    || allocationFit < 0 || allocationFit > 1000
                    || adaptiveScore < 0 || adaptiveScore > 1000) {
                throw new IllegalArgumentException("invalid adaptive collection candidate");
            }
        }
    }

    /** Selection receipt plus the original immutable baseline plan. */
    public static final class Selection<T> {
        private final CollectionRequest<T> request;
        private final Snapshot snapshot;
        private final long selectionBudgetBytes;
        private final CollectionPlan<T> baseline;
        private final List<Candidate> ranked;
        private final String receipt;

        private Selection(CollectionRequest<T> request, Snapshot snapshot, long selectionBudgetBytes,
                CollectionPlan<T> baseline, List<Candidate> ranked) {
            this.request = request;
            this.snapshot = snapshot;
            this.selectionBudgetBytes = selectionBudgetBytes;
            this.baseline = baseline;
            this.ranked = List.copyOf(ranked);
            this.receipt = buildReceipt();
        }

        public CollectionRequest<T> request() { return request; }
        public Snapshot snapshot() { return snapshot; }
        public long selectionBudgetBytes() { return selectionBudgetBytes; }
        public CollectionPlan<T> baseline() { return baseline; }
        public List<Candidate> ranked() { return ranked; }
        public String receipt() { return receipt; }
        public Optional<Candidate> selected() { return ranked.stream().findFirst(); }

        /**
         * Constructs exactly the selected eligible provider. An empty eligible set preserves the
         * baseline factory's failure and rejection detail.
         */
        public T create() {
            return selected().map(candidate -> baseline.create(candidate.providerId()))
                    .orElseGet(baseline::create);
        }

        private String buildReceipt() {
            try {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                try (DataOutputStream out = new DataOutputStream(bytes)) {
                    field(out, "m3-adaptive-collection-selection-v1");
                    field(out, baseline.receipt());
                    out.writeLong(snapshot.maxHeap());
                    out.writeLong(snapshot.committedHeap());
                    out.writeLong(snapshot.usedHeap());
                    out.writeLong(snapshot.availableToProcess());
                    field(out, snapshot.pressure().name());
                    out.writeLong(selectionBudgetBytes);
                    out.writeInt(ranked.size());
                    for (Candidate candidate : ranked) {
                        field(out, candidate.providerId());
                        field(out, candidate.modelVersion());
                        out.writeLong(candidate.weightedPayloadBytes());
                        out.writeInt(candidate.baselineScore());
                        out.writeInt(candidate.pressureFit());
                        out.writeInt(candidate.allocationFit());
                        out.writeInt(candidate.adaptiveScore());
                    }
                }
                return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                        .digest(bytes.toByteArray()));
            } catch (IOException | NoSuchAlgorithmException failure) {
                throw new IllegalStateException("cannot build adaptive selection receipt", failure);
            }
        }
    }

    public static Probe runtimeProbe() {
        return () -> {
            Runtime runtime = Runtime.getRuntime();
            long committed = runtime.totalMemory();
            long used = Math.max(0L, committed - runtime.freeMemory());
            long max = runtime.maxMemory();
            return new Snapshot(max, committed, used, -1L, classify(used, max));
        };
    }

    private static Pressure classify(long used, long max) {
        if (used >= fraction(max, HARD_HEAP_PERCENT)) return Pressure.HARD;
        if (used >= fraction(max, SOFT_HEAP_PERCENT)) return Pressure.SOFT;
        return Pressure.NORMAL;
    }

    private static long selectionBudget(Snapshot snapshot) {
        if (snapshot.pressure() == Pressure.NORMAL) return Long.MAX_VALUE;
        long reserve = Math.min(MAX_HEAP_RESERVE,
                Math.max(MIN_HEAP_RESERVE, snapshot.maxHeap() >>> 5));
        long available = snapshot.effectiveHeadroom();
        long usable = available > reserve ? available - reserve : 0L;
        int shift = snapshot.pressure() == Pressure.SOFT ? 1 : 2;
        return Math.max(MIN_SELECTION_BUDGET, usable >>> shift);
    }

    private static <T> List<Candidate> rank(CollectionRequest<T> request,
            CollectionPlan<T> baseline, Snapshot snapshot, long budget) {
        List<Candidate> result = new ArrayList<>(baseline.ranked().size());
        for (CollectionPlan.Candidate candidate : baseline.ranked()) {
            long bytes = candidate.estimate()
                    .weightedPayloadBytes(request.workload().emptyPermille());
            int pressureFit = pressureFit(budget, bytes);
            int adaptive = adaptiveScore(snapshot.pressure(), candidate.score(),
                    pressureFit, candidate.allocationFit());
            result.add(new Candidate(candidate.providerId(), candidate.modelVersion(), bytes,
                    candidate.score(), pressureFit, candidate.allocationFit(), adaptive));
        }
        if (snapshot.pressure() == Pressure.NORMAL) {
            return List.copyOf(result);
        }
        result.sort(Comparator.comparingInt(Candidate::adaptiveScore).reversed()
                .thenComparing(Comparator.comparingInt(Candidate::pressureFit).reversed())
                .thenComparingLong(Candidate::weightedPayloadBytes)
                .thenComparing(Comparator.comparingInt(Candidate::baselineScore).reversed())
                .thenComparing(Candidate::providerId));
        return List.copyOf(result);
    }

    private static int adaptiveScore(
            Pressure pressure, int baselineScore, int pressureFit, int allocationFit) {
        return switch (pressure) {
            case NORMAL -> baselineScore;
            case SOFT -> (2 * baselineScore + pressureFit + allocationFit) / 4;
            case HARD -> (baselineScore + 2 * pressureFit + allocationFit) / 4;
        };
    }

    private static int pressureFit(long budget, long bytes) {
        if (budget == Long.MAX_VALUE || bytes == 0L) return 1000;
        BigInteger target = BigInteger.valueOf(budget);
        return target.multiply(BigInteger.valueOf(1000L))
                .divide(target.add(BigInteger.valueOf(bytes))).intValueExact();
    }

    private static long fraction(long amount, int percent) {
        return (amount / 100L) * percent + ((amount % 100L) * percent) / 100L;
    }

    private static void field(DataOutputStream out, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.write(bytes);
    }
}
