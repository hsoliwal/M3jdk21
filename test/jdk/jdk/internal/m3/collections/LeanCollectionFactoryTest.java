package jdk.internal.m3.collections;

import jdk.m3.collections.PrimitiveKind;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LeanCollectionFactoryTest {

    private final LeanCollectionFactory factory = LeanCollectionFactory.standard();

    @Test
    void standardFactoryIsStableAndReusesCanonicalOwners() {
        assertSame(factory, LeanCollectionFactory.standard());
        assertNotNull(factory.structures());
        assertSame(FuzzyAlgorithmFactory.standard(), factory.algorithms());
        assertSame(FuzzyProviderCollectionFactory.standard(), factory.providers());
    }

    @Test
    void structurePlanAndCreationStayOnExistingConcreteOwner() {
        LeanCollectionFactory.Plan plan = factory.planStructure("long map 128");

        assertEquals(LeanCollectionFactory.Domain.STRUCTURE, plan.domain());
        assertTrue(plan.candidateId().startsWith("structure:map:"));
        assertInstanceOf(FuzzyStructureFactory.Plan.class, plan.delegatePlan());

        Object created = factory.createStructure("long map 128");
        assertEquals(plan.implementationType(), created.getClass().getName());
    }

    @Test
    void algorithmPlanDelegatesToExistingStructuredSelector() {
        AlgorithmRequirements requirements =
                AlgorithmRequirements.builder(AlgorithmRequirements.Purpose.RANGE_INDEX)
                        .items(4096, 1000, 0)
                        .range(
                                AlgorithmRequirements.RangeAggregation.SUM,
                                AlgorithmRequirements.RangeMutation.IMMUTABLE,
                                AlgorithmRequirements.RangeQuery.RANGE)
                        .build();

        LeanCollectionFactory.Plan plan = factory.planAlgorithm(requirements);
        AlgorithmSelection selection = factory.selectAlgorithm(requirements);

        assertEquals(LeanCollectionFactory.Domain.ALGORITHM, plan.domain());
        assertEquals(selection.candidateId(), plan.candidateId());
        assertEquals(selection.implementationType().getName(), plan.implementationType());
        assertSame(
                selection.implementationType(),
                factory.selectAlgorithm(selection.candidateId(), requirements)
                        .implementationType());
        assertInstanceOf(AlgorithmSelection.class, plan.delegatePlan());
    }

    @Test
    void providerPlanKeepsPagingAndResidencyAsHardSemantics() {
        ProviderCollectionRequirements requirements =
                ProviderCollectionRequirements.builder(
                                ProviderCollectionRequirements.Purpose.ROWS,
                                PrimitiveKind.LONG,
                                PrimitiveKind.INT)
                        .maximumLogicalId(1L << 24)
                        .maxResidentRows(64)
                        .build();

        LeanCollectionFactory.Plan plan = factory.planProvider(requirements);

        assertEquals(LeanCollectionFactory.Domain.PROVIDER, plan.domain());
        assertEquals("bounded-provider-rows", plan.candidateId());
        assertEquals(
                BoundedProviderBackedPrimitiveRows.class.getName(),
                plan.implementationType());
        assertInstanceOf(FuzzyProviderCollectionFactory.Plan.class, plan.delegatePlan());

        PrimitiveRowProvider provider =
                (rowId, target) -> {
                    target[0] = rowId;
                    target[1] = rowId * 2L;
                };
        assertInstanceOf(
                BoundedProviderBackedPrimitiveRows.class,
                factory.createProviderRows(requirements, provider));
    }

    @Test
    void providerTreePlanPreservesOffsetVersusCursorIdWidthContracts() {
        ProviderCollectionRequirements offset =
                ProviderCollectionRequirements.builder(
                                ProviderCollectionRequirements.Purpose.TREE,
                                PrimitiveKind.LONG)
                        .idWidth(ProviderCollectionRequirements.IdWidth.INT32)
                        .paging(ProviderCollectionRequirements.Paging.OFFSET)
                        .maximumLogicalId(1_000_000L)
                        .pageSize(128)
                        .build();

        ProviderCollectionRequirements cursor =
                ProviderCollectionRequirements.builder(
                                ProviderCollectionRequirements.Purpose.TREE,
                                PrimitiveKind.LONG)
                        .idWidth(ProviderCollectionRequirements.IdWidth.LONG64)
                        .paging(ProviderCollectionRequirements.Paging.CURSOR)
                        .maximumLogicalId(1L << 40)
                        .pageSize(128)
                        .build();

        assertEquals("offset-provider-tree", factory.planProvider(offset).candidateId());
        assertEquals("cursor-provider-long-tree", factory.planProvider(cursor).candidateId());
    }
}
