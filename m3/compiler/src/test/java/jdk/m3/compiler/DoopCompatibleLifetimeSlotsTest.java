// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compiler;

import org.junit.jupiter.api.Test;

//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/64652588f0d4c23b1f6e0411090b2a796325ae0e/synexia-openrewrite-recipes/src/test/java/com/synexia/rewrite/M3DoopCompatibleLifetimeSlotsTest.java blob 095b2df2ebb3; recipe m3/compiler/lifetime-slots/recipe/relocate.py v1
class DoopCompatibleLifetimeSlotsTest {
    @Test void strictTypeGroupDifferential() {
        DoopCompatibleLifetimeSlotsSelfTest.main(new String[0]);
    }
}
