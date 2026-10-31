// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring;

import java.nio.ByteOrder;

/**
 * Compile-only seam for FrozenBytes.addressed(...).
 * The compatibility test deliberately does not exercise MIndexAddressedView or native kernels.
 */
public final class MIndexAddressedView {
  public enum Kind { BYTE, CHAR, SHORT, INT, LONG }

  private MIndexAddressedView() {}

  public static MIndexAddressedView of(FrozenBytes owner, Kind kind, ByteOrder order) {
    throw new UnsupportedOperationException("addressed-view API is outside this fixture");
  }
}
