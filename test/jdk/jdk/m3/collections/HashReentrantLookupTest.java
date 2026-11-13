/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

/** Runs the re-entrant key/value callback contract of the hash slot-map owners under the Maven suite. */
//FLLDCIM: MRL - recipe HASH_REENTRANT_GUARD v1, make/m3/collections/qualification/hash-reentrant-guard-20261107
final class HashReentrantLookupTest {
    @Test void keyAndValueCallbacksNeverDamageOrMisdirectTheOwner() {
        HashReentrantLookupSelfTest.main(new String[0]);
    }
}
