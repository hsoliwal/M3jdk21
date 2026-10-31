// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Keeps the caller-visible selection and failure transcript bound to the original owner. */
class M3FirstVisibleAtomTest {
    @Test
    void preservesFirstVisibleCallbackAndFailureTape() {
        assertEquals("53a3ac80b4ba43ad3df0c2397041513fb02346c2ba2cb2ddebc12b7540329b99",
                M3FirstVisibleAtomContract.verify());
    }
}
