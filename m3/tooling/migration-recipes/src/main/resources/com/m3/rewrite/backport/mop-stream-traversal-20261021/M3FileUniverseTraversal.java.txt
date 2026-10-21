// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Lazy metadata-first traversal for a file-system universe.
 *
 * <p>No file payload is read and no whole-system tree is retained. Immediate directory children
 * are sorted deterministically, independent roots/drives may run in parallel, and each emitted file
 * carries the same immutable metadata snapshot consumed by {@link M3FileStreamIndexer#planned}.</p>
 */
public final class M3FileUniverseTraversal {
  private M3FileUniverseTraversal() {
    throw new AssertionError("No instances");
  }

  public record Options(int maxDepth, Set<String> extensions, boolean parallelRoots) {
    public Options {
      if (maxDepth < 0) throw new IllegalArgumentException("maxDepth");
      Set<String> normalized = new HashSet<>();
      for (String extension : Objects.requireNonNull(extensions, "extensions")) {
        String value = Objects.requireNonNull(extension, "extension").strip().toLowerCase(Locale.ROOT);
        if (value.startsWith(".")) value = value.substring(1);
        if (value.isEmpty() || value.indexOf('/') >= 0 || value.indexOf('\\') >= 0) {
          throw new IllegalArgumentException("invalid extension: " + extension);
        }
        normalized.add(value);
      }
      extensions = Set.copyOf(normalized);
    }

    public static Options allRegularFiles() {
      return new Options(Integer.MAX_VALUE, Set.of(), false);
    }

    public static Options sourceFiles(boolean parallelRoots, String... extensions) {
      return new Options(Integer.MAX_VALUE, Set.of(extensions), parallelRoots);
    }

    boolean accepts(Path path) {
      if (extensions.isEmpty()) return true;
      Path namePath = path.getFileName();
      String name = namePath == null ? "" : namePath.toString();
      int dot = name.lastIndexOf('.');
      return dot >= 0
          && dot + 1 < name.length()
          && extensions.contains(name.substring(dot + 1).toLowerCase(Locale.ROOT));
    }
  }

  public static Stream<M3FileUniversePlan.Entry> stream(
      Collection<Path> roots,
      Options options,
      M3ProgressMonitor monitor,
      long parentWork) {
    List<Path> orderedRoots =
        Objects.requireNonNull(roots, "roots").stream()
            .map(path -> Objects.requireNonNull(path, "root").toAbsolutePath().normalize())
            .distinct()
            .sorted(Comparator.comparing(Path::toString))
            .toList();
    Options checked = Objects.requireNonNull(options, "options");
    M3ProgressMonitor parent = Objects.requireNonNull(monitor, "monitor");
    if (parentWork < 0L) throw new IllegalArgumentException("parentWork must be non-negative");
    if (orderedRoots.isEmpty()) return Stream.empty();

    long quotient = parentWork / orderedRoots.size();
    long remainder = parentWork % orderedRoots.size();
    Stream<Integer> rootIndexes =
        Stream.iterate(0, value -> value + 1).limit(orderedRoots.size());
    if (checked.parallelRoots()) rootIndexes = rootIndexes.parallel();

    return rootIndexes.flatMap(index -> {
      long weight = quotient + (index < remainder ? 1L : 0L);
      M3ProgressMonitor child = parent.split(weight);
      child.beginTask("Traverse " + orderedRoots.get(index), M3ProgressMonitor.UNKNOWN);
      return streamRoot(orderedRoots.get(index), checked, child).onClose(child::done);
    });
  }

  public static Stream<M3FileUniversePlan.Entry> streamRoot(
      Path root, Options options, M3ProgressMonitor monitor) {
    Path start = Objects.requireNonNull(root, "root").toAbsolutePath().normalize();
    if (!Files.exists(start, LinkOption.NOFOLLOW_LINKS)) {
      throw new UncheckedIOException(new java.nio.file.NoSuchFileException(start.toString()));
    }
    Iterator<M3FileUniversePlan.Entry> iterator =
        new TraversalIterator(
            start,
            Objects.requireNonNull(options, "options"),
            Objects.requireNonNull(monitor, "monitor"));
    return StreamSupport.stream(
        Spliterators.spliteratorUnknownSize(
            iterator,
            java.util.Spliterator.ORDERED
                | java.util.Spliterator.NONNULL
                | java.util.Spliterator.IMMUTABLE),
        false);
  }

  private record Pending(Path path, int depth) {}

  private static final class TraversalIterator implements Iterator<M3FileUniversePlan.Entry> {
    private final Options options;
    private final M3ProgressMonitor monitor;
    private final Deque<Pending> pending = new ArrayDeque<>();
    private M3FileUniversePlan.Entry next;
    private boolean prepared;

    private TraversalIterator(Path root, Options options, M3ProgressMonitor monitor) {
      this.options = options;
      this.monitor = monitor;
      pending.push(new Pending(root, 0));
    }

    @Override
    public boolean hasNext() {
      prepare();
      return next != null;
    }

    @Override
    public M3FileUniversePlan.Entry next() {
      prepare();
      if (next == null) throw new java.util.NoSuchElementException();
      M3FileUniversePlan.Entry result = next;
      next = null;
      prepared = false;
      return result;
    }

    private void prepare() {
      if (prepared) return;
      prepared = true;
      try {
        while (!pending.isEmpty()) {
          monitor.checkCanceled();
          Pending item = pending.pop();
          Path path = item.path();

          if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            if (item.depth() < options.maxDepth()) {
              List<Path> children = immediateChildren(path);
              for (int index = children.size() - 1; index >= 0; index--) {
                pending.push(new Pending(children.get(index), item.depth() + 1));
              }
            }
            continue;
          }
          if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || !options.accepts(path)) {
            continue;
          }
          M3FileAttributes attributes = M3FileAttributes.capture(path);
          if (!attributes.isRegularFile()) continue;
          monitor.worked(1L);
          next = new M3FileUniversePlan.Entry(path, attributes);
          return;
        }
        monitor.done();
      } catch (IOException failure) {
        monitor.done();
        throw new UncheckedIOException(failure);
      }
    }

    private static List<Path> immediateChildren(Path directory) throws IOException {
      List<Path> children = new ArrayList<>();
      try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
        for (Path child : stream) children.add(child.toAbsolutePath().normalize());
      }
      children.sort(Comparator.comparing(Path::toString));
      return children;
    }
  }
}
