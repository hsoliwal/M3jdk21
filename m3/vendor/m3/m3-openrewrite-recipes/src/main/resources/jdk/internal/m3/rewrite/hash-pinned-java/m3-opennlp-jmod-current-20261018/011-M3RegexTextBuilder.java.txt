/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// SPDX-License-Identifier: Apache-2.0
package opennlp.tools.namefind;

import java.util.Arrays;

/**
 * Private regex input builder with an opt-in, segment-preserving CharSequence.
 *
 * <p>When {@code opennlp.m3.compositeRegex} is false (the default), input() returns
 * the original StringBuilder, preserving the existing matcher fast path. When
 * enabled, immutable source Strings and separator atoms remain separate and
 * the matcher reads logical UTF-16 code units without flattening the sentence.
 * One instance belongs to one find operation and is never pooled or shared.
 */
final class M3RegexTextBuilder implements CharSequence {
  static final String OPT_IN_PROPERTY = "opennlp.m3.compositeRegex";
  private static final String SPACE = " ";

  private final StringBuilder flat;
  private String[] parts;
  private int[] ends;
  private int count;
  private int length;
  private volatile int lastPart;

  M3RegexTextBuilder(int initialCapacity) {
    this(initialCapacity, Boolean.getBoolean(OPT_IN_PROPERTY));
  }

  M3RegexTextBuilder(int initialCapacity, boolean composite) {
    if (initialCapacity < 0) {
      throw new NegativeArraySizeException("capacity " + initialCapacity);
    }
    if (composite) {
      flat = null;
      parts = new String[8];
      ends = new int[8];
    } else {
      flat = new StringBuilder(initialCapacity);
    }
  }

  M3RegexTextBuilder append(String text) {
    if (flat != null) {
      flat.append(text);
    } else {
      add(text == null ? "null" : text);
    }
    return this;
  }

  M3RegexTextBuilder append(char unit) {
    if (flat != null) {
      flat.append(unit);
    } else {
      add(unit == ' ' ? SPACE : String.valueOf(unit));
    }
    return this;
  }

  /** Return the original StringBuilder on stock/default path, or this composite view. */
  CharSequence input() {
    return flat == null ? this : flat;
  }

  private void add(String part) {
    if (part.isEmpty()) return;
    final int nextLength;
    try {
      nextLength = Math.addExact(length, part.length());
    } catch (ArithmeticException tooLong) {
      throw new OutOfMemoryError("Required text length exceeds Java array size");
    }
    if (count == parts.length) {
      int next = count > (Integer.MAX_VALUE - 8) / 2
          ? Integer.MAX_VALUE - 8 : count * 2;
      if (next <= count) throw new OutOfMemoryError("Too many regex text parts");
      parts = Arrays.copyOf(parts, next);
      ends = Arrays.copyOf(ends, next);
    }
    parts[count] = part;
    ends[count++] = nextLength;
    length = nextLength;
  }

  @Override
  public int length() {
    return flat == null ? length : flat.length();
  }

  @Override
  public char charAt(int index) {
    if (flat != null) return flat.charAt(index);
    if (index < 0 || index >= length) {
      throw new StringIndexOutOfBoundsException(index);
    }
    int part = lastPart;
    int start = part == 0 ? 0 : ends[part - 1];
    if (index < start || index >= ends[part]) {
      // Strictly increasing ends: empty parts are deliberately not stored.
      int low = 0;
      int high = count - 1;
      while (low < high) {
        int mid = (low + high) >>> 1;
        if (index < ends[mid]) high = mid;
        else low = mid + 1;
      }
      part = low;
      start = part == 0 ? 0 : ends[part - 1];
      lastPart = part;
    }
    return parts[part].charAt(index - start);
  }

  @Override
  public CharSequence subSequence(int start, int end) {
    if (flat != null) return flat.subSequence(start, end);
    if (start < 0 || end < start || end > length) {
      throw new StringIndexOutOfBoundsException("start " + start + ", end " + end);
    }
    if (start == end) return "";
    StringBuilder materialized = new StringBuilder(end - start);
    int part = 0;
    while (part < count && ends[part] <= start) part++;
    while (part < count) {
      int partStart = part == 0 ? 0 : ends[part - 1];
      if (partStart >= end) break;
      int from = Math.max(start, partStart) - partStart;
      int to = Math.min(end, ends[part]) - partStart;
      materialized.append(parts[part], from, to);
      part++;
    }
    return materialized.toString();
  }

  @Override
  public String toString() {
    return flat != null ? flat.toString() : subSequence(0, length).toString();
  }
}
