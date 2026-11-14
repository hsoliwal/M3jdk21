package jdk.internal.m3.collections;

import jdk.m3.collections.PrimitiveAdaptiveCharMap;
import jdk.m3.collections.PrimitiveDirectByteMap;
import jdk.m3.collections.PrimitiveKind;
import jdk.m3.collections.PrimitiveMpscRingBuffer;
import jdk.m3.collections.PrimitiveSemaphoreMpmcQueue;
import jdk.m3.collections.PrimitiveSortedGapSet;
import jdk.m3.collections.PrimitiveSpmcRingBuffer;
import jdk.m3.collections.PrimitiveTopK;
import jdk.m3.collections.bits.RoaringIntSet;
import jdk.m3.collections.bits.RoaringLongSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class FuzzyStructureFactoryTest {
    private final FuzzyStructureFactory factory = new FuzzyStructureFactory();

    @Test
    void typoHeavySortedSetChoosesGapStorage() {
        FuzzyStructureFactory.Plan plan =
                factory.plan("soreted long set mutation hevy");
        assertEquals(FuzzyStructureFactory.Family.SET, plan.family());
        assertEquals(FuzzyStructureFactory.Storage.SORTED_GAP, plan.storage());
        assertEquals(PrimitiveKind.LONG, plan.keyKind());
        assertInstanceOf(PrimitiveSortedGapSet.class, plan.create());
        assertTrue(plan.rationale().stream().anyMatch(v -> v.contains("fuzzyCorrections")));
    }

    @Test
    void denseByteToIntMapChoosesDirectDomainTable() {
        FuzzyStructureFactory.Plan plan = factory.plan("dense byte to int map");
        assertEquals(FuzzyStructureFactory.Storage.DIRECT, plan.storage());
        assertEquals(PrimitiveKind.BYTE, plan.keyKind());
        assertEquals(PrimitiveKind.INT, plan.valueKind());
        assertInstanceOf(PrimitiveDirectByteMap.class, plan.create());
    }

    @Test
    void topKIntentChoosesBoundedSelectionHeap() {
        FuzzyStructureFactory.Plan plan =
                factory.plan("top 100 smallest double values");
        assertEquals(FuzzyStructureFactory.Family.TOP_K, plan.family());
        assertEquals(100, plan.limit());
        assertEquals(PrimitiveKind.DOUBLE, plan.keyKind());
        PrimitiveTopK top = assertInstanceOf(PrimitiveTopK.class, plan.create());
        assertEquals(PrimitiveTopK.Mode.KEEP_SMALLEST, top.mode());
    }

    @Test
    void manyProducerOneConsumerQueueChoosesMpsc() {
        FuzzyStructureFactory.Plan plan =
                factory.plan("many producers one consumer long queue 4096");
        assertEquals(FuzzyStructureFactory.Concurrency.MPSC, plan.concurrency());
        assertEquals(4096, plan.expectedSize());
        assertInstanceOf(PrimitiveMpscRingBuffer.class, plan.create());
    }

    @Test
    void oneProducerManyConsumerQueueChoosesSpmc() {
        FuzzyStructureFactory.Plan plan =
                factory.plan("one producer many consumers int queue 256");
        assertEquals(FuzzyStructureFactory.Concurrency.SPMC, plan.concurrency());
        assertInstanceOf(PrimitiveSpmcRingBuffer.class, plan.create());
    }

    @Test
    void compressedIntSetChoosesRoaring() {
        assertInstanceOf(
                RoaringIntSet.class,
                factory.create("compressed int set"));
    }

    @Test
    void insertionOrderedLongSetChoosesFlatOrderedSet() {
        assertInstanceOf(
                PrimitiveOrderedSet.class,
                factory.create("insertion ordered long unique set"));
    }

    @Test
    void sparseCharToByteMapChoosesAdaptiveCharMap() {
        assertInstanceOf(
                PrimitiveAdaptiveCharMap.class,
                factory.create("sparse char to byte map 200"));
    }

    @Test
    void blockingMpmcQueueChoosesSemaphoreFacade() {
        assertInstanceOf(
                PrimitiveSemaphoreMpmcQueue.class,
                factory.create("blocking mpmc int queue 128"));
    }

    @Test
    void denseCollectionsExposesUnifiedFuzzyEntryPoint() {
        assertInstanceOf(
                RoaringIntSet.class,
                DenseCollections.fuzzy().createStructure("compressed int set"));
    }

    @Test
    void unifiedFacadeKeepsExistingAlgorithmFactory() {
        FuzzyPrimitiveFactory unified = FuzzyPrimitiveFactory.standard();
        assertInstanceOf(
                RoaringLongSet.class,
                unified.createStructure("compressed long set"));
        assertEquals(
                FuzzyAlgorithmFactory.standard().candidates().size(),
                unified.algorithms().candidates().size());
    }
}
