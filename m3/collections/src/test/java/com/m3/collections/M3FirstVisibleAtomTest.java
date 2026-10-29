// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Modified 2026: bind the transcript to the reviewed comparator-safe nearest-entry successor.
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Keeps selection and failure prefixes bound to the qualified comparator-safe owner successor. */
class M3FirstVisibleAtomTest {
    @Test
    void preservesFirstVisibleCallbackAndFailureTape() {
        assertEquals("b7eb24b2f8232833a7687d1035887a7808aa520d510d32f1abc2fc04732bde22",
                M3FirstVisibleAtomContract.verify());
    }
}
