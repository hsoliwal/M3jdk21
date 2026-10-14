/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.text;

import java.util.Objects;

/** Validated identifiers are data, never reflection or shell targets. */
public final class Ids {
  private Ids() {}
  public static String id(String value) {
    Objects.requireNonNull(value, "id");
    if (!value.matches("[A-Za-z0-9][A-Za-z0-9_.-]{0,127}")) {
      throw new IllegalArgumentException("invalid id: " + value);
    }
    return value;
  }
  public static String sha256(String value) {
    Objects.requireNonNull(value, "sha256");
    if (!value.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("invalid SHA-256");
    return value;
  }
}
