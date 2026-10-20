// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * One-physical-read raw-byte admission for files that must not be interpreted as text.
 *
 * <p>No probe read is performed. Filesystem metadata is sampled before and after the one payload
 * traversal and a changed source fails closed.</p>
 */
public final class M3ByteFileStreamIndexer {
  private M3ByteFileStreamIndexer() {}

  public static M3ByteFile index(Path path) throws IOException {
    Path source = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
    M3FileAttributes before = M3FileAttributes.capture(source);
    if (!before.isRegularFile()) {
      throw new IOException("M3ByteFile source is not a regular file: " + source);
    }

    M3FileByteFacts.Builder facts = new M3FileByteFacts.Builder();
    byte[] buffer = new byte[M3FileByteFacts.CHUNK_BYTES];
    try (InputStream input = Files.newInputStream(source)) {
      for (int read; (read = input.read(buffer)) >= 0; ) {
        if (read == 0) continue;
        facts.accept(buffer, 0, read);
      }
    }

    M3FileByteFacts frozen = facts.finish();
    M3FileAttributes after = M3FileAttributes.capture(source);
    M3ByteFile result = new M3ByteFile(before, after, frozen);
    if (!result.stableDuringRead()) {
      throw new IOException(
          "source file changed during one-pass M3ByteFile admission: " + source);
    }
    return result;
  }
}
