// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compiler;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/d5ad5090d87ef8477a56317e60ac0d29c65029f7/synexia-mindex/runtime/src/test/java/com/synexia/mindex/virtual/M3StaticUsePlanTest.java blob b55d86608284; recipe m3/compiler/static-use/recipe/static-use-plan-20261107/relocate.py v1
class StaticUsePlanTest {
    private static StaticUsePlan.Facts facts(StaticUsePlan.Escape escape,
            StaticUsePlan.Effect effect) {
        return new StaticUsePlan.Facts(4, escape, effect,
                false, false, false, false, false, false, false, true, true);
    }

    @Test void knownLocalParentFlagsLowerToParentWithoutRuntimeReferenceCounting() {
        var plan = StaticUsePlan.evaluate(facts(
                StaticUsePlan.Escape.PROVEN_LOCAL,
                StaticUsePlan.Effect.PARENT_BITS_ONLY));
        assertEquals(StaticUsePlan.Decision.DIRECT_PARENT, plan.decision());
        assertEquals(4, plan.staticUseSites());
        assertFalse(plan.runtimeMemorySampleRequired());
    }

    @Test void coldReadOnlyViewsCanBeLazyUntilReferenceIsNeeded() {
        assertEquals(StaticUsePlan.Decision.LAZY_HANDLE,
                StaticUsePlan.evaluate(facts(StaticUsePlan.Escape.PROVEN_LOCAL,
                        StaticUsePlan.Effect.READONLY_PARENT)).decision());
    }

    @Test void publishedAndIdentitySensitiveObjectsCannotBeDiscarded() {
        assertEquals(StaticUsePlan.Decision.MATERIALIZE,
                StaticUsePlan.evaluate(facts(StaticUsePlan.Escape.PUBLISHED,
                        StaticUsePlan.Effect.PARENT_BITS_ONLY)).decision());
        var escape = new StaticUsePlan.Facts(1, StaticUsePlan.Escape.PROVEN_LOCAL,
                StaticUsePlan.Effect.PARENT_BITS_ONLY,
                true, false, false, false, false, false, false, true, true);
        assertEquals(StaticUsePlan.Decision.MATERIALIZE,
                StaticUsePlan.evaluate(escape).decision());
        var nativeEscape = new StaticUsePlan.Facts(1, StaticUsePlan.Escape.PROVEN_LOCAL,
                StaticUsePlan.Effect.PARENT_BITS_ONLY,
                false, false, false, true, false, false, false, true, true);
        assertEquals(StaticUsePlan.Decision.MATERIALIZE,
                StaticUsePlan.evaluate(nativeEscape).decision());
    }

    @Test void unknownEffectsOrderingAndLifetimeRefuse() {
        assertEquals(StaticUsePlan.Decision.REFUSE,
                StaticUsePlan.evaluate(facts(StaticUsePlan.Escape.UNKNOWN,
                        StaticUsePlan.Effect.PARENT_BITS_ONLY)).decision());
        assertEquals(StaticUsePlan.Decision.REFUSE,
                StaticUsePlan.evaluate(facts(StaticUsePlan.Escape.PROVEN_LOCAL,
                        StaticUsePlan.Effect.UNKNOWN)).decision());
        var missingLifetime = new StaticUsePlan.Facts(1,
                StaticUsePlan.Escape.PROVEN_LOCAL, StaticUsePlan.Effect.PARENT_BITS_ONLY,
                false, false, false, false, false, false, false, true, false);
        assertEquals(StaticUsePlan.Decision.REFUSE,
                StaticUsePlan.evaluate(missingLifetime).decision());
    }

    @Test void onlyGrowthBoundaryNeedsPhysicalMemorySampling() {
        assertFalse(StaticUsePlan.mustSampleAtGrowth(512, 512));
        assertFalse(StaticUsePlan.mustSampleAtGrowth(1, 512));
        assertTrue(StaticUsePlan.mustSampleAtGrowth(513, 512));
        assertThrows(IllegalArgumentException.class,
                () -> StaticUsePlan.mustSampleAtGrowth(-1, 1));
    }
}
