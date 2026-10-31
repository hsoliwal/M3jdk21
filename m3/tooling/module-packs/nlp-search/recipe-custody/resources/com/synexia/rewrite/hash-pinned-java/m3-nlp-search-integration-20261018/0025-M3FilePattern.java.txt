// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import com.m3.file.M3FileText;
import java.util.Objects;

/** File-scoped prepared KMP pattern over compact local token handles. */
public final class M3FilePattern {
  private final M3File file;
  private final int[] handles;
  private final int[] failure;
  private final boolean present;

  private M3FilePattern(M3File file, int[] handles, int[] failure, boolean present) {
    this.file = Objects.requireNonNull(file, "file");
    this.handles = Objects.requireNonNull(handles, "handles");
    this.failure = Objects.requireNonNull(failure, "failure");
    this.present = present;
  }

  static M3FilePattern prepare(M3File file, M3FileText[] values) {
    Objects.requireNonNull(file, "file");
    Objects.requireNonNull(values, "values");
    int[] handles = new int[values.length];
    boolean present = true;
    for (int i = 0; i < values.length; i++) {
      M3FileText value = Objects.requireNonNull(values[i], "pattern token");
      int handle = file.localHandle(value);
      handles[i] = handle;
      if (handle < 0) present = false;
    }
    int[] failure = present ? failure(handles) : new int[handles.length];
    return new M3FilePattern(file, handles, failure, present);
  }

  static M3FilePattern fromLocalHandles(M3File file, int[] localHandles) {
    Objects.requireNonNull(file, "file");
    Objects.requireNonNull(localHandles, "localHandles");
    int[] copy = localHandles.clone();
    for (int handle : copy) {
      if (handle < 0 || handle >= file.uniqueTokenCount()) {
        throw new IllegalArgumentException("local handle outside file lexicon");
      }
    }
    return new M3FilePattern(file, copy, failure(copy), true);
  }

  public int length() {
    return handles.length;
  }

  public boolean presentInFile() {
    return present;
  }

  M3File file() {
    return file;
  }

  int handleAt(int index) {
    return handles[Objects.checkIndex(index, handles.length)];
  }

  int failureAt(int index) {
    return failure[Objects.checkIndex(index, failure.length)];
  }

  private static int[] failure(int[] pattern) {
    int[] failure = new int[pattern.length];
    int matched = 0;
    for (int i = 1; i < pattern.length; i++) {
      while (matched > 0 && pattern[i] != pattern[matched]) matched = failure[matched - 1];
      if (pattern[i] == pattern[matched]) matched++;
      failure[i] = matched;
    }
    return failure;
  }
}
