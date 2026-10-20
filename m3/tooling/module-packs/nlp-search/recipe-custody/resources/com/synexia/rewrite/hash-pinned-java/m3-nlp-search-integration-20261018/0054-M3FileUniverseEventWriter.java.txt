// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import com.m3.file.M3FileResolver;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/**
 * Streaming MIDXUNI1 envelope over the canonical one-pass MIDXEVT2 event lanes.
 *
 * <p>The outer stream contains only file boundaries and the known plan count. Each file's inner
 * frames are emitted directly to the caller output as the existing indexer observes them; no
 * complete file payload or event buffer is materialized.</p>
 */
public final class M3FileUniverseEventWriter {
  private static final byte[] MAGIC = "MIDXUNI1".getBytes(StandardCharsets.US_ASCII);
  private static final int FILE_STARTED = 1;
  private static final int FILE_FINISHED = 2;
  private static final int UNIVERSE_FINISHED = 3;

  private M3FileUniverseEventWriter() {}

  /** Stream plain-text files in the plan's captured metadata order. */
  public static Summary plainText(
      M3FileUniversePlan plan,
      M3FileResolver resolver,
      int languageId,
      OutputStream output)
      throws IOException {
    return write(plan, resolver, languageId, false, output);
  }

  /** Stream code-like files in the plan's captured metadata order. */
  public static Summary codeLike(
      M3FileUniversePlan plan,
      M3FileResolver resolver,
      int languageId,
      OutputStream output)
      throws IOException {
    return write(plan, resolver, languageId, true, output);
  }

  private static Summary write(
      M3FileUniversePlan plan,
      M3FileResolver resolver,
      int languageId,
      boolean codeLike,
      OutputStream output)
      throws IOException {
    Objects.requireNonNull(plan, "plan");
    Objects.requireNonNull(resolver, "resolver");
    DataOutputStream data = new DataOutputStream(Objects.requireNonNull(output, "output"));
    data.write(MAGIC);
    data.writeInt(plan.size());

    long byteCount = 0L;
    int tokenCount = 0;
    List<M3FileUniversePlan.Entry> entries = plan.entries();
    for (int ordinal = 0; ordinal < entries.size(); ordinal++) {
      M3FileUniversePlan.Entry entry = entries.get(ordinal);
      data.writeByte(FILE_STARTED);
      data.writeInt(ordinal);
      writeString(data, entry.path().toString());

      M3FileEventWriter embedded = M3FileEventWriter.embedded(data);
      M3File indexed =
          M3FileStreamIndexer.planned(entry, resolver, languageId, codeLike, embedded);
      if (!embedded.isFinished()) throw new IOException("embedded MIDXEVT2 stream did not finish");
      M3FileSourceFacts sourceFacts = indexed.sourceFacts();
      M3FileAttributes before = sourceFacts.attributesBeforeRead();
      if (!entry.path().toString().equals(before.normalizedPath())
          || !entry.attributes().sameContentIdentity(before)) {
        throw new IOException("Universe plan metadata no longer matches source: " + entry.path());
      }

      data.writeByte(FILE_FINISHED);
      data.writeInt(ordinal);
      byteCount = Math.addExact(byteCount, sourceFacts.byteFacts().byteCount());
      tokenCount = Math.addExact(tokenCount, indexed.tokenCount());
    }
    data.writeByte(UNIVERSE_FINISHED);
    data.writeInt(plan.size());
    data.flush();
    return new Summary(plan.size(), byteCount, tokenCount);
  }

  private static void writeString(DataOutputStream output, String value) throws IOException {
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    output.writeInt(bytes.length);
    output.write(bytes);
  }

  /** Aggregate payload and token totals published after the terminal Universe frame. */
  public record Summary(int fileCount, long byteCount, int tokenCount) {
    public Summary {
      if (fileCount < 0 || byteCount < 0L || tokenCount < 0) {
        throw new IllegalArgumentException("negative Universe summary value");
      }
    }
  }
}
