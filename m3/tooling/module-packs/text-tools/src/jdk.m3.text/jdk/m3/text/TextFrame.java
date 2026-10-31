/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.text;

import java.util.ArrayList;
import java.util.List;

/** Ordered line snapshot; frame hashing distinguishes equal text with different line framing. */
public record TextFrame(List<TextLine> lines) {
  public TextFrame { lines = List.copyOf(lines); }
  public static TextFrame fromString(String text) {
    Canonical.validUnicode(text);
    List<TextLine> result = new ArrayList<>();
    int start = 0;
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (c != '\r' && c != '\n') continue;
      String content = text.substring(start, i);
      String ending = String.valueOf(c);
      if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
        ending = "\r\n";
        i++;
      }
      result.add(new TextLine(content, ending));
      start = i + 1;
    }
    if (start < text.length()) result.add(new TextLine(text.substring(start), ""));
    return new TextFrame(result);
  }
  public long byteCount() {
    long n = 0;
    for (TextLine line : lines) n = Math.addExact(n, line.byteCount());
    return n;
  }
  public String text() {
    StringBuilder result = new StringBuilder();
    for (TextLine line : lines) result.append(line.content()).append(line.ending());
    return result.toString();
  }
  public String sha256() {
    Canonical hash = new Canonical("m3-unix4j-frame-v1").number(lines.size());
    for (TextLine line : lines) hash.text(line.content()).text(line.ending());
    return hash.finish();
  }
}
