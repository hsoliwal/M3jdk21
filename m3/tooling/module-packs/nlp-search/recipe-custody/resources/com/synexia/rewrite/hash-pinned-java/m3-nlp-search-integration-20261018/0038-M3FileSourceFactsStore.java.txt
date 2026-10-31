// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Atomic per-file secondary storage for authenticated source-facts images. */
public final class M3FileSourceFactsStore implements AutoCloseable {
  private final Path directory;

  public M3FileSourceFactsStore(Path directory) {
    this.directory = Objects.requireNonNull(directory, "directory").toAbsolutePath().normalize();
  }

  public Path directory() {
    return directory;
  }

  public boolean contains(M3FileSourceFactsKey key) {
    return Files.isRegularFile(pathFor(key));
  }

  public M3File read(M3FileSourceFactsKey key, M3File base) throws IOException {
    M3FileSourceFactsKey checkedKey = Objects.requireNonNull(key, "key");
    M3File checkedBase = requireKey(checkedKey, base);
    Path image = pathFor(checkedKey);
    return Files.isRegularFile(image) ? M3FileSourceFactsImage.read(image, checkedBase) : null;
  }

  public void write(M3FileSourceFactsKey key, M3File file) throws IOException {
    M3FileSourceFactsKey checkedKey = Objects.requireNonNull(key, "key");
    M3File checkedFile = requireKey(checkedKey, file);
    M3FileSourceFactsImage.write(checkedFile, pathFor(checkedKey));
  }

  public boolean invalidate(M3FileSourceFactsKey key) throws IOException {
    return Files.deleteIfExists(pathFor(Objects.requireNonNull(key, "key")));
  }

  @Override
  public void close() {}

  private Path pathFor(M3FileSourceFactsKey key) {
    return directory.resolve(Objects.requireNonNull(key, "key").storageId() + ".midxfacts");
  }

  private static M3File requireKey(M3FileSourceFactsKey key, M3File file) {
    M3File checked = Objects.requireNonNull(file, "file");
    if (!key.equals(M3FileSourceFactsKey.of(key.sourceRef(), checked))) {
      throw new IllegalArgumentException("M3File does not match source-facts key");
    }
    return checked;
  }
}
