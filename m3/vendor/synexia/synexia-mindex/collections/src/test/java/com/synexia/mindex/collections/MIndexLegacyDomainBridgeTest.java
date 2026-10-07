// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.synexia.mindex.MIndexObject;
import com.synexia.mindex.MIndexObjects;
import com.synexia.mindex.MIndexString;
import com.synexia.mindex.collection.MIndexObjectDomain;
import com.synexia.mindex.collection.MIndexStringDomain;
import org.junit.jupiter.api.Test;

final class MIndexLegacyDomainBridgeTest {
    private record LegacyValue(
            MIndexString name,
            int count) {
    }

    @Test
    void legacyStringDomainCollapsesOntoCanonicalStringSpace() {
        MIndexSpace<MIndexString> bridged =
                MIndexSpaces.fromDomain(
                        MIndexStringDomain.INSTANCE);

        assertSame(MIndexSpaces.strings(), bridged);
        assertTrue(bridged.javaEqualityCompatible());

        MIndexString value =
                MIndexString.literal("legacy-string-bridge");
        assertEquals(
                value.contentIndex(),
                bridged.id(value));
    }

    @Test
    void wrappersOverOneLegacyObjectDomainShareCanonicalAuthority() {
        MIndexObjectDomain<LegacyValue> legacy =
                new MIndexObjectDomain<>();
        MIndexSpace<MIndexObject<LegacyValue>> first =
                MIndexSpaces.fromDomain(legacy);
        MIndexSpace<MIndexObject<LegacyValue>> second =
                MIndexSpaces.fromDomain(legacy);

        assertNotSame(first, second);
        assertSame(
                legacy,
                first.identityAuthority());
        assertSame(
                legacy,
                second.identityAuthority());
        assertFalse(first.javaEqualityCompatible());

        MIndexObject<LegacyValue> left =
                MIndexObjects.index(
                        new LegacyValue(
                                MIndexString.literal("same"),
                                7));
        MIndexObject<LegacyValue> right =
                MIndexObjects.index(
                        new LegacyValue(
                                MIndexString.literal("same"),
                                7));

        int firstId = first.id(left);
        int secondId = second.id(right);
        assertEquals(firstId, secondId);

        MIndexCompositeIndex composites =
                new MIndexCompositeIndex();
        MIndexFrozenList<MIndexObject<LegacyValue>> one =
                MIndexFrozenList.ofIds(
                        first,
                        composites,
                        new int[] {firstId});
        MIndexFrozenList<MIndexObject<LegacyValue>> two =
                MIndexFrozenList.ofIds(
                        second,
                        composites,
                        new int[] {secondId});

        assertEquals(one.canonicalId(), two.canonicalId());
        assertEquals(one, two);
    }

    @Test
    void recursiveTransferResolvesLegacyWrappersByAuthority() {
        MIndexObjectDomain<LegacyValue> sourceDomain =
                new MIndexObjectDomain<>();
        MIndexObjectDomain<LegacyValue> targetDomain =
                new MIndexObjectDomain<>();

        MIndexSpace<MIndexObject<LegacyValue>> sourceBuilderSpace =
                MIndexSpaces.fromDomain(sourceDomain);
        MIndexSpace<MIndexObject<LegacyValue>> sourceBindingSpace =
                MIndexSpaces.fromDomain(sourceDomain);
        MIndexSpace<MIndexObject<LegacyValue>> targetSpace =
                MIndexSpaces.fromDomain(targetDomain);

        MIndexObject<LegacyValue> alpha =
                MIndexObjects.index(
                        new LegacyValue(
                                MIndexString.literal(
                                        "legacy-alpha"),
                                1));
        MIndexObject<LegacyValue> beta =
                MIndexObjects.index(
                        new LegacyValue(
                                MIndexString.literal(
                                        "legacy-beta"),
                                2));
        int alphaId = sourceBuilderSpace.id(alpha);
        int betaId = sourceBuilderSpace.id(beta);

        MIndexCompositeIndex sourceIndex =
                new MIndexCompositeIndex();
        MIndexCompositeIndex targetIndex =
                new MIndexCompositeIndex();
        MIndexFrozenSet<MIndexObject<LegacyValue>> sourceSet =
                MIndexFrozenSet.ofIds(
                        sourceBuilderSpace,
                        sourceIndex,
                        new int[] {alphaId, betaId});

        MIndexDomainTransfer<MIndexObject<LegacyValue>> legacyTransfer =
                (source, sourceId, target) ->
                        target.id(source.value(sourceId));

        MIndexCompositeTransferPlan plan =
                MIndexCompositeTransferPlan.between(
                                sourceIndex, targetIndex)
                        .bind(
                                sourceBindingSpace,
                                targetSpace,
                                legacyTransfer);

        MIndexCompositeRef moved =
                plan.transfer(
                        sourceIndex.space().ref(sourceSet));

        MIndexFrozenSet<MIndexObject<LegacyValue>> targetSet =
                MIndexFrozenSet.ofIds(
                        targetSpace,
                        targetIndex,
                        moved.copyLane());

        assertEquals(2, targetSet.size());
        assertTrue(
                targetSet.contains(
                        MIndexObjects.index(
                                new LegacyValue(
                                        MIndexString.literal(
                                                "legacy-alpha"),
                                        1))));
        assertTrue(
                targetSet.contains(
                        MIndexObjects.index(
                                new LegacyValue(
                                        MIndexString.literal(
                                                "legacy-beta"),
                                        2))));
    }
}
