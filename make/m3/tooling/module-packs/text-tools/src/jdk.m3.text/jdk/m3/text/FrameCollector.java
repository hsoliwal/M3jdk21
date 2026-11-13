/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Counts output before retention. A failed collection never produces a partial success. */
public final class FrameCollector implements Consumer<TextLine> {
  private final Limits limits;
  private final List<TextLine> lines = new ArrayList<>();
  private long bytes;
  private boolean failed;
  private boolean sealed;
  public FrameCollector(Limits limits) { this.limits = java.util.Objects.requireNonNull(limits); }
  @Override public void accept(TextLine line) {
    if (sealed || failed) throw new IllegalStateException("collector closed or failed");
    long next;
    try { next = Math.addExact(bytes, line.byteCount()); }
    catch (RuntimeException ex) { failed = true; throw ex; }
    if (lines.size() >= limits.maxLines() || next > limits.maxBytes()) {
      failed = true;
      throw new IllegalArgumentException("output budget exceeded");
    }
    lines.add(line);
    bytes = next;
  }
  public TextFrame frame() {
    sealed = true;
    if (failed) throw new IllegalStateException("partial output rejected");
    return new TextFrame(lines);
  }
}
