/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.text;

/** Collection budgets, not an arbitrary-Java CPU/heap sandbox. */
public record Limits(int maxNodes, int maxEdges, int maxLines, long maxBytes) {
  public Limits {
    if (maxNodes < 1 || maxNodes > 10000 || maxEdges < 0 || maxEdges > 100000
        || maxLines < 0 || maxBytes < 0) throw new IllegalArgumentException("invalid limits");
  }
  public static Limits standard() { return new Limits(256, 2048, 100000, 16L * 1024 * 1024); }
  public void check(TextFrame frame) {
    if (frame.lines().size() > maxLines || frame.byteCount() > maxBytes) {
      throw new IllegalArgumentException("frame budget exceeded");
    }
  }
  public String sha256() {
    return new Canonical("m3-unix4j-limits-v1").number(maxNodes).number(maxEdges)
        .number(maxLines).number(maxBytes).finish();
  }
}
