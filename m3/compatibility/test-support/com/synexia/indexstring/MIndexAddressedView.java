// Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring;

import java.nio.ByteOrder;

/**
 * Fail-fast compile seam for FrozenBytes.addressed in the compatibility oracle.
 * This test deliberately does not exercise addressed-view behavior.
 */
public final class MIndexAddressedView {
    public enum Kind { BYTE, CHAR, SHORT, INT, LONG }

    private MIndexAddressedView() {}

    static MIndexAddressedView of(FrozenBytes owner, Kind kind, ByteOrder order) {
        throw new UnsupportedOperationException("addressed-view behavior is outside this compatibility test");
    }
}
