// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compiler.lifetime;

import org.junit.jupiter.api.Test;

//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/64652588f0d4c23b1f6e0411090b2a796325ae0e/synexia-openrewrite-recipes/src/test/java/com/synexia/rewrite/M3DoopLifetimeSlotPlannerTest.java blob 2438a313d460; recipe m3/compiler/lifetime-slots/recipe/relocate.py v1
class M3DoopLifetimeSlotPlannerTest {
    @Test void deterministicIntervalReuseDifferential() {
        M3DoopLifetimeSlotPlannerSelfTest.main(new String[0]);
    }
}
