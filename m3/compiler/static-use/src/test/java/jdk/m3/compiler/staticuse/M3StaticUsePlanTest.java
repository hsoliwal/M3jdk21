// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compiler.staticuse;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/d5ad5090d87ef8477a56317e60ac0d29c65029f7/synexia-mindex/runtime/src/test/java/com/synexia/mindex/virtual/M3StaticUsePlanTest.java blob b55d86608284; recipe m3/compiler/static-use/recipe/static-use-plan-20261107/relocate.py v1
class M3StaticUsePlanTest {
    private static M3StaticUsePlan.Facts facts(M3StaticUsePlan.Escape escape,
            M3StaticUsePlan.Effect effect) {
        return new M3StaticUsePlan.Facts(4, escape, effect,
                false, false, false, false, false, false, false, true, true);
    }

    @Test void knownLocalParentFlagsLowerToParentWithoutRuntimeReferenceCounting() {
        var plan = M3StaticUsePlan.evaluate(facts(
                M3StaticUsePlan.Escape.PROVEN_LOCAL,
                M3StaticUsePlan.Effect.PARENT_BITS_ONLY));
        assertEquals(M3StaticUsePlan.Decision.DIRECT_PARENT, plan.decision());
        assertEquals(4, plan.staticUseSites());
        assertFalse(plan.runtimeMemorySampleRequired());
    }

    @Test void coldReadOnlyViewsCanBeLazyUntilReferenceIsNeeded() {
        assertEquals(M3StaticUsePlan.Decision.LAZY_HANDLE,
                M3StaticUsePlan.evaluate(facts(M3StaticUsePlan.Escape.PROVEN_LOCAL,
                        M3StaticUsePlan.Effect.READONLY_PARENT)).decision());
    }

    @Test void publishedAndIdentitySensitiveObjectsCannotBeDiscarded() {
        assertEquals(M3StaticUsePlan.Decision.MATERIALIZE,
                M3StaticUsePlan.evaluate(facts(M3StaticUsePlan.Escape.PUBLISHED,
                        M3StaticUsePlan.Effect.PARENT_BITS_ONLY)).decision());
        var escape = new M3StaticUsePlan.Facts(1, M3StaticUsePlan.Escape.PROVEN_LOCAL,
                M3StaticUsePlan.Effect.PARENT_BITS_ONLY,
                true, false, false, false, false, false, false, true, true);
        assertEquals(M3StaticUsePlan.Decision.MATERIALIZE,
                M3StaticUsePlan.evaluate(escape).decision());
        var nativeEscape = new M3StaticUsePlan.Facts(1, M3StaticUsePlan.Escape.PROVEN_LOCAL,
                M3StaticUsePlan.Effect.PARENT_BITS_ONLY,
                false, false, false, true, false, false, false, true, true);
        assertEquals(M3StaticUsePlan.Decision.MATERIALIZE,
                M3StaticUsePlan.evaluate(nativeEscape).decision());
    }

    @Test void unknownEffectsOrderingAndLifetimeRefuse() {
        assertEquals(M3StaticUsePlan.Decision.REFUSE,
                M3StaticUsePlan.evaluate(facts(M3StaticUsePlan.Escape.UNKNOWN,
                        M3StaticUsePlan.Effect.PARENT_BITS_ONLY)).decision());
        assertEquals(M3StaticUsePlan.Decision.REFUSE,
                M3StaticUsePlan.evaluate(facts(M3StaticUsePlan.Escape.PROVEN_LOCAL,
                        M3StaticUsePlan.Effect.UNKNOWN)).decision());
        var missingLifetime = new M3StaticUsePlan.Facts(1,
                M3StaticUsePlan.Escape.PROVEN_LOCAL, M3StaticUsePlan.Effect.PARENT_BITS_ONLY,
                false, false, false, false, false, false, false, true, false);
        assertEquals(M3StaticUsePlan.Decision.REFUSE,
                M3StaticUsePlan.evaluate(missingLifetime).decision());
    }

    @Test void onlyGrowthBoundaryNeedsPhysicalMemorySampling() {
        assertFalse(M3StaticUsePlan.mustSampleAtGrowth(512, 512));
        assertFalse(M3StaticUsePlan.mustSampleAtGrowth(1, 512));
        assertTrue(M3StaticUsePlan.mustSampleAtGrowth(513, 512));
        assertThrows(IllegalArgumentException.class,
                () -> M3StaticUsePlan.mustSampleAtGrowth(-1, 1));
    }
}
