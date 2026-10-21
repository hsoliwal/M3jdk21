// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Metadata-first immutable admission plan for ordered multi-file streaming.
 *
 * <p>The plan captures each path's immutable filesystem attributes once, orders the entries by the
 * canonical metadata comparator, and is then consumed by the streaming Universe writer. Payload
 * bytes are not opened or buffered by this class.</p>
 */
public final class M3FileUniversePlan {
  /** One normalized source path paired with the attributes captured before payload traversal. */
  public record Entry(Path path, M3FileAttributes attributes) {
    public Entry {
      path = normalize(Objects.requireNonNull(path, "path"));
      attributes = Objects.requireNonNull(attributes, "attributes");
      if (!path.toString().equals(attributes.normalizedPath())) {
        throw new IllegalArgumentException("plan path does not match captured attribute path");
      }
    }

    private static Path normalize(Path path) {
      return path.toAbsolutePath().normalize();
    }
  }

  private final List<Entry> entries;

  private M3FileUniversePlan(List<Entry> entries) {
    this.entries = List.copyOf(entries);
  }

  /**
   * Capture a metadata-only plan.
   *
   * <p>Inputs are normalized to absolute paths, duplicate normalized paths are rejected, non-regular
   * files are rejected, and no payload stream is opened. The returned entries are immutable and
   * strictly ordered by {@link M3FileAttributeOrder#CREATION_THEN_MODIFIED_THEN_PATH}.</p>
   */
  public static M3FileUniversePlan capture(Collection<Path> paths) throws IOException {
    Collection<Path> supplied = Objects.requireNonNull(paths, "paths");
    List<Entry> captured = new ArrayList<>(supplied.size());
    Set<Path> seen = new HashSet<>();
    for (Path suppliedPath : supplied) {
      Path path = normalize(Objects.requireNonNull(suppliedPath, "path"));
      if (!seen.add(path)) {
        throw new IOException("duplicate normalized Universe path: " + path);
      }
      M3FileAttributes attributes = M3FileAttributes.capture(path);
      if (!attributes.isRegularFile()) {
        throw new IOException("Universe plan source is not a regular file: " + path);
      }
      captured.add(new Entry(path, attributes));
    }

    captured.sort(Comparator.comparing(Entry::attributes,
        M3FileAttributeOrder.CREATION_THEN_MODIFIED_THEN_PATH));
    for (int index = 1; index < captured.size(); index++) {
      if (M3FileAttributeOrder.compare(
              captured.get(index - 1).attributes(), captured.get(index).attributes()) >= 0) {
        throw new IOException("Universe metadata order is not strict at index " + index);
      }
    }
    return new M3FileUniversePlan(captured);
  }

  public List<Entry> entries() {
    return entries;
  }

  public int size() {
    return entries.size();
  }

  public boolean isEmpty() {
    return entries.isEmpty();
  }

  /** Return the immutable normalized path projection in the same stream order. */
  public List<Path> paths() {
    return entries.stream().map(Entry::path).toList();
  }

  private static Path normalize(Path path) {
    return path.toAbsolutePath().normalize();
  }
}
