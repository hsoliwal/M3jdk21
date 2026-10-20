// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import com.m3.file.M3FileResolver;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Validator for the streaming MIDXUNI1 envelope and its embedded MIDXEVT2 file streams. */
public final class M3FileUniverseEventReader {
  private static final byte[] MAGIC = "MIDXUNI1".getBytes(StandardCharsets.US_ASCII);
  private static final int FILE_STARTED = 1;
  private static final int FILE_FINISHED = 2;
  private static final int UNIVERSE_FINISHED = 3;
  private static final int MAX_FILE_COUNT = 1_000_000;

  private M3FileUniverseEventReader() {}

  /** Read and validate one complete Universe stream without closing the caller-owned input. */
  public static Summary read(InputStream input, M3FileStreamEvents.Sink sink)
      throws IOException {
    return read(input, sink, M3FileEventReader.DEFAULT_MAX_FRAME_BYTES);
  }

  /** Read with an explicit bounded embedded MIDXEVT2 frame size. */
  public static Summary read(
      InputStream input, M3FileStreamEvents.Sink sink, int maxFrameBytes)
      throws IOException {
    return readEnvelope(input, sink, maxFrameBytes, M3FileEventReader::readEmbedded);
  }

  /** Read all embedded files into detached immutable snapshots without reopening source paths. */
  public static SnapshotSummary readSnapshots(InputStream input, M3FileResolver resolver)
      throws IOException {
    return readSnapshots(input, resolver, M3FileEventReader.DEFAULT_MAX_FRAME_BYTES);
  }

  /** Read detached snapshots with an explicit bounded embedded MIDXEVT2 frame size. */
  public static SnapshotSummary readSnapshots(
      InputStream input, M3FileResolver resolver, int maxFrameBytes) throws IOException {
    Objects.requireNonNull(resolver, "resolver");
    List<M3FileSnapshot> snapshots = new ArrayList<>();
    Summary summary =
        readEnvelope(
            input,
            M3FileStreamEvents.noOp(),
            maxFrameBytes,
            (embedded, ignored, bound) -> {
              M3FileSnapshot snapshot =
                  M3FileEventReader.readEmbeddedM3File(embedded, resolver, bound).snapshot();
              snapshots.add(snapshot);
              return summaryOf(snapshot);
            });
    if (snapshots.size() != summary.fileCount()) {
      throw invalid("Universe snapshot count mismatch");
    }
    List<SnapshotFile> files = new ArrayList<>(snapshots.size());
    for (int ordinal = 0; ordinal < snapshots.size(); ordinal++) {
      FileSummary file = summary.files().get(ordinal);
      files.add(new SnapshotFile(file.ordinal(), file.path(), snapshots.get(ordinal)));
    }
    return new SnapshotSummary(files);
  }

  /** Read and compose all embedded files into one resolver-owned Universe snapshot. */
  public static M3FileUniverseSnapshot readSnapshot(InputStream input, M3FileResolver resolver)
      throws IOException {
    return readSnapshot(input, resolver, M3FileEventReader.DEFAULT_MAX_FRAME_BYTES);
  }

  /** Read and compose a Universe snapshot with an explicit embedded frame bound. */
  public static M3FileUniverseSnapshot readSnapshot(
      InputStream input, M3FileResolver resolver, int maxFrameBytes) throws IOException {
    Objects.requireNonNull(resolver, "resolver");
    return M3FileUniverseSnapshot.from(readSnapshots(input, resolver, maxFrameBytes), resolver);
  }

  private static Summary readEnvelope(
      InputStream input,
      M3FileStreamEvents.Sink sink,
      int maxFrameBytes,
      EmbeddedDecoder decoder)
      throws IOException {
    Objects.requireNonNull(input, "input");
    Objects.requireNonNull(sink, "sink");
    Objects.requireNonNull(decoder, "decoder");
    if (maxFrameBytes < 73) {
      throw new IllegalArgumentException("maxFrameBytes is too small for a token frame");
    }
    DataInputStream data = new DataInputStream(input);
    byte[] magic = new byte[MAGIC.length];
    try {
      data.readFully(magic);
    } catch (EOFException truncated) {
      throw invalid("truncated MIDXUNI1 magic", truncated);
    }
    if (!Arrays.equals(MAGIC, magic)) throw invalid("invalid MIDXUNI1 magic");

    int fileCount = readInt(data, "Universe file count");
    if (fileCount < 0 || fileCount > MAX_FILE_COUNT) {
      throw invalid("invalid Universe file count: " + fileCount);
    }
    List<FileSummary> files = new ArrayList<>(fileCount);
    M3FileAttributes previous = null;
    for (int ordinal = 0; ordinal < fileCount; ordinal++) {
      if (data.readUnsignedByte() != FILE_STARTED) {
        throw invalid("missing Universe file start");
      }
      int encodedOrdinal = readInt(data, "Universe file start ordinal");
      if (encodedOrdinal != ordinal) throw invalid("non-contiguous Universe file ordinal");
      String path = readString(data, "Universe file path", maxFrameBytes);
      M3FileEventReader.Summary eventSummary = decoder.read(data, sink, maxFrameBytes);
      M3FileAttributes current = eventSummary.attributesBefore();
      if (!path.equals(current.normalizedPath())) {
        throw invalid("Universe path does not match embedded MIDXEVT2 metadata");
      }
      if (previous != null
          && M3FileAttributeOrder.CREATION_THEN_MODIFIED_THEN_PATH.compare(previous, current)
              >= 0) {
        throw invalid("Universe files are not in deterministic metadata order");
      }
      if (data.readUnsignedByte() != FILE_FINISHED) {
        throw invalid("missing Universe file finish");
      }
      int finishOrdinal = readInt(data, "Universe file finish ordinal");
      if (finishOrdinal != ordinal) throw invalid("mismatched Universe file finish ordinal");
      files.add(new FileSummary(ordinal, path, eventSummary));
      previous = current;
    }

    if (data.readUnsignedByte() != UNIVERSE_FINISHED) {
      throw invalid("missing Universe finish");
    }
    if (readInt(data, "Universe finish count") != fileCount) {
      throw invalid("Universe finish count mismatch");
    }
    if (data.read() != -1) throw invalid("trailing bytes after Universe finish");
    return new Summary(files);
  }

  @FunctionalInterface
  private interface EmbeddedDecoder {
    M3FileEventReader.Summary read(
        InputStream input, M3FileStreamEvents.Sink sink, int maxFrameBytes) throws IOException;
  }

  private static M3FileEventReader.Summary summaryOf(M3FileSnapshot snapshot) {
    M3FileSourceFactsSnapshot facts = snapshot.sourceFactsSnapshot();
    M3FilePrimitiveSnapshot primitive = snapshot.primitiveSnapshot();
    return new M3FileEventReader.Summary(
        facts.attributesBeforeRead(),
        facts.attributesAfterRead(),
        primitive.tokenCount(),
        primitive.uniqueTokenCount(),
        facts.byteFactsSnapshot().byteCount(),
        facts.contentSha256().bytes());
  }

  public record FileSummary(int ordinal, String path, M3FileEventReader.Summary eventSummary) {
    public FileSummary {
      if (ordinal < 0) throw new IllegalArgumentException("negative Universe ordinal");
      path = Objects.requireNonNull(path, "path");
      eventSummary = Objects.requireNonNull(eventSummary, "eventSummary");
    }
  }

  public record Summary(List<FileSummary> files) {
    public Summary {
      files = List.copyOf(Objects.requireNonNull(files, "files"));
      for (int ordinal = 0; ordinal < files.size(); ordinal++) {
        if (files.get(ordinal).ordinal() != ordinal) {
          throw new IllegalArgumentException("Universe summary ordinals are not contiguous");
        }
      }
    }

    public int fileCount() {
      return files.size();
    }

    public long byteCount() {
      long total = 0L;
      for (FileSummary file : files) {
        total = Math.addExact(total, file.eventSummary().byteCount());
      }
      return total;
    }

    public int tokenCount() {
      int total = 0;
      for (FileSummary file : files) {
        total = Math.addExact(total, file.eventSummary().tokenCount());
      }
      return total;
    }
  }

  /** One ordered envelope entry paired with its detached canonical snapshot. */
  public record SnapshotFile(int ordinal, String path, M3FileSnapshot snapshot) {
    public SnapshotFile {
      if (ordinal < 0) throw new IllegalArgumentException("negative Universe ordinal");
      path = Objects.requireNonNull(path, "path");
      snapshot = Objects.requireNonNull(snapshot, "snapshot");
    }
  }

  /** Immutable result of replaying all embedded file lanes. */
  public record SnapshotSummary(List<SnapshotFile> files) {
    public SnapshotSummary {
      files = List.copyOf(Objects.requireNonNull(files, "files"));
      for (int ordinal = 0; ordinal < files.size(); ordinal++) {
        if (files.get(ordinal).ordinal() != ordinal) {
          throw new IllegalArgumentException("Universe snapshot ordinals are not contiguous");
        }
      }
    }

    public int fileCount() {
      return files.size();
    }

    public long byteCount() {
      long total = 0L;
      for (SnapshotFile file : files) {
        total =
            Math.addExact(
                total, file.snapshot().sourceFactsSnapshot().byteFactsSnapshot().byteCount());
      }
      return total;
    }

    public int tokenCount() {
      int total = 0;
      for (SnapshotFile file : files) {
        total = Math.addExact(total, file.snapshot().primitiveSnapshot().tokenCount());
      }
      return total;
    }
  }

  private static int readInt(DataInputStream input, String label) throws IOException {
    try {
      return input.readInt();
    } catch (EOFException truncated) {
      throw invalid("truncated " + label, truncated);
    }
  }

  private static String readString(DataInputStream input, String label, int maxBytes)
      throws IOException {
    int length = readInt(input, label + " length");
    if (length < 0 || length > maxBytes) throw invalid("invalid " + label + " length");
    byte[] bytes = new byte[length];
    try {
      input.readFully(bytes);
    } catch (EOFException truncated) {
      throw invalid("truncated " + label, truncated);
    }
    return new String(bytes, StandardCharsets.UTF_8);
  }

  private static IOException invalid(String message) {
    return new IOException(message);
  }

  private static IOException invalid(String message, Throwable cause) {
    return new IOException(message, cause);
  }
}
