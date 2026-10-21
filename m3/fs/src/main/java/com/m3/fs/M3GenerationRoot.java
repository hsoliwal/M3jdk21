// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted from the pinned Synexia owner; namespace metadata uses array columns.
package com.m3.fs;

import java.io.*;
import java.util.*;

/** Immutable copy-on-write namespace; format-1 Java String ordering remains authoritative. */
public final class M3GenerationRoot {
  private static final int FORMAT = 1;
  private static final int MAX_ENTRIES = 10_000_000;
  private final long generation;
  private final byte[] parent;
  private final M3IdTable entries;
  private final byte[] canonical;
  private final byte[] id;

  public M3GenerationRoot(long generation, M3ContentId parent, Map<String, M3ContentId> entries) {
    this(requireGeneration(generation), parent == null ? null : parent.bytes(),
        new M3IdTable(entries, M3GenerationRoot::normalizePath, MAX_ENTRIES));
  }
  private M3GenerationRoot(long generation, byte[] parent, M3IdTable entries) {
    if (generation < 0 || entries.names.length > MAX_ENTRIES) throw new IllegalArgumentException("invalid generation geometry");
    this.generation = generation; this.parent = parent; this.entries = entries;
    canonical = encode(); id = M3Columns.digest(canonical);
  }
  public static M3GenerationRoot empty() { return new M3GenerationRoot(0L, null, new M3IdTable(new String[0], new byte[0])); }
  public long generation() { return generation; }
  public Optional<M3ContentId> parent() { return parent == null ? Optional.empty() : Optional.of(M3ContentId.of(parent)); }
  public Map<String, M3ContentId> entries() { return entries.view(); }
  public M3ContentId id() { return M3ContentId.of(id); }
  public byte[] canonicalBytes() { return canonical.clone(); }
  public Optional<M3ContentId> entry(String path) {
    int row = entries.index(normalizePath(path)); return row < 0 ? Optional.empty() : Optional.of(entries.idAt(row));
  }
  public M3GenerationRoot put(String path, M3ContentId manifestId) {
    String normalized = normalizePath(path);
    byte[] manifest = Objects.requireNonNull(manifestId, "manifestId").bytes();
    return new M3GenerationRoot(Math.incrementExact(generation), id,
        entries.put(normalized, manifest));
  }
  private static long requireGeneration(long value) {
    if (value < 0) throw new IllegalArgumentException("negative generation");
    return value;
  }
  public M3GenerationRoot remove(String path) {
    int row = entries.index(normalizePath(path)); return row < 0 ? this
        : new M3GenerationRoot(Math.incrementExact(generation), id, entries.remove(row));
  }

  public record DirectoryEntry(String name, String path, Optional<M3ContentId> manifestId, boolean hasDescendants) {
    public DirectoryEntry {
      name = requireChildName(name); path = normalizePath(path);
      manifestId = Objects.requireNonNull(manifestId, "manifestId");
      if (manifestId.isEmpty() && !hasDescendants) throw new IllegalArgumentException("empty directory entry");
    }
    public boolean isFile() { return manifestId.isPresent(); }
    public boolean isDirectory() { return hasDescendants; }
  }
  public record DirectoryPage(List<DirectoryEntry> entries, Optional<String> nextAfter) {
    public DirectoryPage {
      entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
      nextAfter = Objects.requireNonNull(nextAfter, "nextAfter"); nextAfter.ifPresent(M3GenerationRoot::requireChildName);
    }
  }

  /** Bounded column scratch correctly combines file/descendant rows even across punctuation. */
  public DirectoryPage directoryPage(String directory, String afterName, int limit) {
    if (limit < 1 || limit > 65536) throw new IllegalArgumentException("directory page limit must be 1..65536");
    String normalized = normalizePath(directory), prefix = normalized.equals("/") ? "/" : normalized + "/";
    String cursor = afterName == null ? null : requireChildName(afterName);
    int capacity = limit + 1, count = 0;
    String[] names = new String[capacity]; int[] manifests = new int[capacity]; boolean[] descendants = new boolean[capacity];
    Arrays.fill(manifests, -1);
    int first = Arrays.binarySearch(entries.names, prefix); if (first < 0) first = -first - 1;
    for (int row = first; row < entries.names.length; row++) {
      String path = entries.names[row]; if (!path.startsWith(prefix)) break;
      if (path.length() == prefix.length()) continue;
      int slash = path.indexOf('/', prefix.length());
      String name = path.substring(prefix.length(), slash < 0 ? path.length() : slash);
      if (cursor != null && name.compareTo(cursor) <= 0) continue;
      int at = Arrays.binarySearch(names, 0, count, name);
      if (at < 0) {
        at = -at - 1; if (at == capacity) continue;
        int shift = Math.min(count, capacity - 1) - at;
        if (shift > 0) {
          System.arraycopy(names, at, names, at + 1, shift);
          System.arraycopy(manifests, at, manifests, at + 1, shift);
          System.arraycopy(descendants, at, descendants, at + 1, shift);
        }
        names[at] = name; manifests[at] = -1; descendants[at] = false;
        if (count < capacity) count++;
      }
      if (slash < 0) manifests[at] = row; else descendants[at] = true;
    }
    ArrayList<DirectoryEntry> result = new ArrayList<>(Math.min(count, limit));
    for (int row = 0; row < Math.min(count, limit); row++) {
      result.add(new DirectoryEntry(names[row], prefix + names[row],
          manifests[row] < 0 ? Optional.empty() : Optional.of(entries.idAt(manifests[row])), descendants[row]));
    }
    return new DirectoryPage(result, count > limit ? Optional.of(names[limit - 1]) : Optional.empty());
  }
  private static String requireChildName(String name) {
    Objects.requireNonNull(name, "child name");
    if (name.isEmpty() || name.equals(".") || name.equals("..") || name.indexOf('/') >= 0
        || name.indexOf('\\') >= 0 || name.indexOf('\0') >= 0) throw new IllegalArgumentException("invalid child name");
    return name;
  }
  public static String normalizePath(String input) {
    Objects.requireNonNull(input, "path");
    if (input.isEmpty() || !input.startsWith("/") || input.indexOf('\\') >= 0 || input.indexOf('\0') >= 0)
      throw new IllegalArgumentException("M3 logical path must be absolute, slash-separated and NUL-free");
    String[] segments = input.split("/", -1); int used = 0;
    for (String segment : segments) {
      if (segment.isEmpty() || segment.equals(".")) continue;
      if (segment.equals("..")) { if (used == 0) throw new IllegalArgumentException("M3 logical path escapes root"); used--; }
      else segments[used++] = segment;
    }
    return used == 0 ? "/" : "/" + String.join("/", Arrays.copyOf(segments, used));
  }
  private byte[] encode() {
    try {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      try (DataOutputStream out = new DataOutputStream(bytes)) {
        out.writeInt(FORMAT); out.writeLong(generation); out.writeBoolean(parent != null);
        if (parent != null) out.write(parent);
        out.writeInt(entries.names.length);
        for (int i = 0; i < entries.names.length; i++) { M3Columns.writeString(out, entries.names[i]); out.write(entries.ids, i * 32, 32); }
      }
      return bytes.toByteArray();
    } catch (IOException impossible) { throw new AssertionError(impossible); }
  }
  public static M3GenerationRoot decode(byte[] encoded) throws IOException {
    byte[] bytes = Objects.requireNonNull(encoded, "encoded").clone();
    try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
      if (in.readInt() != FORMAT) throw new IOException("unsupported M3 generation format");
      long generation = in.readLong(); byte[] parent = in.readBoolean() ? M3Columns.readId(in) : null;
      int count = M3Columns.readCount(in, MAX_ENTRIES, 37); String[] names = new String[count]; byte[] ids = new byte[count * 32];
      for (int i = 0; i < count; i++) { names[i] = normalizePath(M3Columns.readString(in, false)); in.readFully(ids, i * 32, 32); }
      if (in.read() != -1) throw new IOException("trailing generation bytes");
      M3GenerationRoot root = new M3GenerationRoot(generation, parent, new M3IdTable(names, ids));
      if (!Arrays.equals(root.canonical, bytes)) throw new IOException("non-canonical generation bytes");
      return root;
    } catch (IllegalArgumentException invalid) { throw new IOException("invalid M3 generation", invalid); }
  }
}
