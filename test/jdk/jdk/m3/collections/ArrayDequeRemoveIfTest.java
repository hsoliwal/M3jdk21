/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

/** Owning-module JUnit entry point for the standalone public-contract corpus. */
final class ArrayDequeRemoveIfTest {
    @Test void preservesRemovalContracts() throws Exception {
        ArrayDequeRemoveIfContract.run();
    }
}
