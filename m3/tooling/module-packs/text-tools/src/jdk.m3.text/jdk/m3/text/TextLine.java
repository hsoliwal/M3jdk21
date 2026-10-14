/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.text;

import java.nio.charset.StandardCharsets;

/** Immutable line with its original ending; no platform normalization. */
public record TextLine(String content, String ending) {
  public TextLine {
    Canonical.validUnicode(content);
    Canonical.validUnicode(ending);
    if (ending.length() > 2) throw new IllegalArgumentException("ending exceeds donor line contract");
  }
  public long byteCount() {
    return (long) content.getBytes(StandardCharsets.UTF_8).length + ending.getBytes(StandardCharsets.UTF_8).length;
  }
}
