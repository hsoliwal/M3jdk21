// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;


/** Immutable line with its original ending; no platform normalization. */
public record TextLine(String content, String ending) {
  public TextLine {
    Canonical.validUnicode(content);
    Canonical.validUnicode(ending);
    if (ending.length() > 2) throw new IllegalArgumentException("ending exceeds donor line contract");
  }
  public long byteCount() {
    return Math.addExact(Utf8Segments.length(content), Utf8Segments.length(ending));
  }
}
