// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections.select;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class AdaptiveCollectionFactoryTest {
    private static final CollectionFamily<CharSequence> FAMILY =
            new CollectionFamily<>("adaptive-proof", CharSequence.class);
    private static final long MIB = 1L << 20;

    @Test
    void normalPressurePreservesBaselineOrderExactly() {
        FuzzyCollectionFactory factory = factory();
        CollectionRequest<CharSequence> request = request();
        CollectionPlan<CharSequence> baseline = factory.select(request);
        var adaptive = new AdaptiveCollectionFactory(factory,
                () -> snapshot(64, 16, AdaptiveCollectionFactory.Pressure.NORMAL))
                .select(request);

        assertEquals(baseline.ranked().stream()
                        .map(CollectionPlan.Candidate::providerId).toList(),
                adaptive.ranked().stream()
                        .map(AdaptiveCollectionFactory.Candidate::providerId).toList());
        assertEquals(baseline.selected().orElseThrow().providerId(),
                adaptive.selected().orElseThrow().providerId());
        assertEquals("fast-large", adaptive.create().toString());
    }

    @Test
    void hardPressurePrefersLowerPayloadWithoutRelaxingContracts() {
        FuzzyCollectionFactory factory = factory();
        CollectionRequest<CharSequence> request = request();
        var adaptive = new AdaptiveCollectionFactory(factory,
                () -> snapshot(64, 60, AdaptiveCollectionFactory.Pressure.HARD))
                .select(request);

        assertEquals("small-array", adaptive.selected().orElseThrow().providerId());
        assertEquals("small-array", adaptive.create().toString());
        assertTrue(adaptive.ranked().getFirst().weightedPayloadBytes()
                < adaptive.ranked().getLast().weightedPayloadBytes());
        assertTrue(adaptive.selectionBudgetBytes() < request.preferences().payloadTargetBytes());
        assertEquals(64L * MIB, adaptive.snapshot().maxHeap());
        assertEquals(4L * MIB, adaptive.snapshot().heapHeadroom());
    }

    @Test
    void softPressureRetainsTheFasterProviderWhenBothComfortablyFit() {
        FuzzyCollectionFactory factory = factory();
        CollectionRequest<CharSequence> request = request();
        var adaptive = new AdaptiveCollectionFactory(factory,
                () -> snapshot(64, 48, AdaptiveCollectionFactory.Pressure.SOFT))
                .select(request);

        assertEquals("fast-large", adaptive.selected().orElseThrow().providerId());
        assertEquals("fast-large", adaptive.create().toString());
    }

    @Test
    void externalAvailabilityCanOnlyTightenHeapHeadroom() {
        var snapshot = new AdaptiveCollectionFactory.Snapshot(
                1024L * MIB, 512L * MIB, 128L * MIB, 2L * MIB,
                AdaptiveCollectionFactory.Pressure.HARD);
        var adaptive = new AdaptiveCollectionFactory(factory(), () -> snapshot).select(request());

        assertEquals(2L * MIB, snapshot.effectiveHeadroom());
        assertEquals(1024L, adaptive.selectionBudgetBytes());
        assertEquals("small-array", adaptive.selected().orElseThrow().providerId());
    }

    @Test
    void pressureDoesNotMakeAnIneligibleProviderEligible() {
        CollectionProvider<CharSequence> singleThread = provider(
                "single-thread", CollectionRequest.Order.ANY, 4096L, 1000, 1000);
        FuzzyCollectionFactory factory = new FuzzyCollectionFactory(List.of(singleThread));
        var requirements = new CollectionRequest.Requirements(
                CollectionRequest.Order.ANY, true, false, false, false, 0);
        CollectionRequest<CharSequence> request = CollectionRequest.forFamily(FAMILY)
                .requiring(requirements);
        var selection = new AdaptiveCollectionFactory(factory,
                () -> snapshot(64, 63, AdaptiveCollectionFactory.Pressure.HARD))
                .select(request);

        assertTrue(selection.ranked().isEmpty());
        assertEquals(1, selection.baseline().rejected().size());
        assertTrue(selection.baseline().rejected().getFirst().reasons()
                .contains("concurrency is not provided"));
        assertThrows(IllegalStateException.class, selection::create);
    }

    @Test
    void fixedSnapshotProducesDeterministicReceiptAndSelection() {
        var snapshot = snapshot(128, 112, AdaptiveCollectionFactory.Pressure.SOFT);
        AdaptiveCollectionFactory adaptive = new AdaptiveCollectionFactory(factory(), () -> snapshot);
        var first = adaptive.select(request());
        var second = adaptive.select(request());

        assertEquals(first.receipt(), second.receipt());
        assertEquals(first.ranked(), second.ranked());
        assertEquals(64, first.receipt().length());
    }

    @Test
    void runtimeProbeReportsValidCurrentJvmBounds() {
        var snapshot = AdaptiveCollectionFactory.runtimeProbe().sample();

        assertTrue(snapshot.maxHeap() > 0);
        assertTrue(snapshot.committedHeap() >= snapshot.usedHeap());
        assertTrue(snapshot.usedHeap() >= 0);
        assertTrue(snapshot.heapHeadroom() >= 0);
        assertEquals(-1L, snapshot.availableToProcess());
    }

    @Test
    void customProbeCannotDowngradeJvmHeapPressure() {
        var snapshot = new AdaptiveCollectionFactory.Snapshot(
                100L * MIB, 100L * MIB, 90L * MIB, Long.MAX_VALUE,
                AdaptiveCollectionFactory.Pressure.NORMAL);

        assertEquals(AdaptiveCollectionFactory.Pressure.HARD, snapshot.pressure());
        assertEquals(10L * MIB, snapshot.effectiveHeadroom());
    }

    @Test
    void invalidSnapshotsFailBeforeSelection() {
        assertThrows(IllegalArgumentException.class,
                () -> new AdaptiveCollectionFactory.Snapshot(
                        0, 0, 0, -1, AdaptiveCollectionFactory.Pressure.NORMAL));
        assertThrows(IllegalArgumentException.class,
                () -> new AdaptiveCollectionFactory.Snapshot(
                        10, 4, 5, -1, AdaptiveCollectionFactory.Pressure.NORMAL));
        assertThrows(IllegalArgumentException.class,
                () -> new AdaptiveCollectionFactory.Snapshot(
                        10, 5, 4, -2, AdaptiveCollectionFactory.Pressure.NORMAL));
        assertThrows(IllegalArgumentException.class,
                () -> new AdaptiveCollectionFactory.Snapshot(
                        10, 11, 4, -1, AdaptiveCollectionFactory.Pressure.NORMAL));
    }

    private static FuzzyCollectionFactory factory() {
        return new FuzzyCollectionFactory(List.of(
                provider("fast-large", CollectionRequest.Order.ANY,
                        1_000_000L, 1000, 500),
                provider("small-array", CollectionRequest.Order.ANY,
                        8_000L, 600, 950)));
    }

    private static CollectionRequest<CharSequence> request() {
        return new CollectionRequest<>(
                FAMILY,
                128,
                CollectionRequest.Requirements.ANY,
                new CollectionRequest.Workload(1000, 0, 0, 0, 0, 0),
                new CollectionRequest.Preferences(10, 900, 0, 1_000_000L));
    }

    private static CollectionProvider<CharSequence> provider(
            String id, CollectionRequest.Order order, long bytes, int lookupFit, int allocationFit) {
        CollectionCapabilities capabilities = new CollectionCapabilities(
                order, false, false, false, false, Integer.MAX_VALUE, 0, 0, 0);
        return new CollectionProvider<>(
                id,
                "adaptive-proof-v1",
                FAMILY,
                capabilities,
                ignored -> new CollectionEstimate(
                        bytes, bytes, lookupFit, lookupFit, lookupFit, lookupFit, lookupFit,
                        allocationFit, "deterministic adaptive-selection fixture"),
                ignored -> id);
    }

    private static AdaptiveCollectionFactory.Snapshot snapshot(
            long maxMib, long usedMib, AdaptiveCollectionFactory.Pressure pressure) {
        long max = maxMib * MIB;
        long used = usedMib * MIB;
        return new AdaptiveCollectionFactory.Snapshot(max, max, used, -1L, pressure);
    }
}
