// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import com.m3.file.M3FileResolver;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Durable directory image for an immutable detached Universe snapshot. */
public final class M3FileUniverseSnapshotImage {
  private static final byte[] MAGIC = "MIDXUIMG1".getBytes(StandardCharsets.US_ASCII);
  private static final int FORMAT = 1;
  private static final int DIGEST_BYTES = 32;
  private static final int MAX_FILE_COUNT = 1_000_000;
  private static final int MAX_PATH_BYTES = 1 << 20;
  private static final long MAX_MANIFEST_BYTES = 64L << 20;
  private static final String MANIFEST_NAME = "manifest.midxu";

  private M3FileUniverseSnapshotImage() {}

  /** Publish one complete Universe image without rereading any source path. */
  public static void write(M3FileUniverseSnapshot universe, Path directory)
      throws IOException {
    M3FileUniverseSnapshot checked = Objects.requireNonNull(universe, "universe");
    M3FileResolver resolver = checked.resolver();
    Path target = normalize(directory);
    if (Files.exists(target)) throw new IOException("Universe image target already exists");
    Path parent = target.getParent();
    if (parent == null) throw new IOException("Universe image target has no parent");
    Files.createDirectories(parent);
    Path staging = Files.createTempDirectory(parent, target.getFileName() + ".staging-");
    boolean published = false;
    try {
      List<M3FileUniverseSnapshot.File> files = checked.files();
      if (files.size() > MAX_FILE_COUNT) throw new IOException("Universe image has too many files");
      for (int ordinal = 0; ordinal < files.size(); ordinal++) {
        M3FileUniverseSnapshot.File file = files.get(ordinal);
        if (file.ordinal() != ordinal) throw new IOException("Universe file ordinals are not contiguous");
        if (!file.snapshot().hasSourceFacts()) {
          throw new IOException("Universe image requires source facts for every file");
        }
        M3FileImage.writeSnapshot(file.snapshot(), resolver, staging.resolve(semanticName(ordinal)));
        M3FileSourceFactsImage.writeSnapshot(
            file.snapshot(), M3FileImage.contentFingerprint64(file.snapshot(), resolver),
            staging.resolve(factsName(ordinal)));
      }
      M3FileUniverseTokenIndexImage.write(
          checked.tokenIndex(), staging.resolve(M3FileUniverseTokenIndexImage.NAME));
      writeManifest(staging.resolve(MANIFEST_NAME), files, resolver);
      try {
        Files.move(staging, target, StandardCopyOption.ATOMIC_MOVE);
      } catch (AtomicMoveNotSupportedException unsupported) {
        Files.move(staging, target);
      }
      published = true;
    } finally {
      if (!published) deleteTree(staging);
    }
  }

  /** Reopen every authenticated per-file image and compose the existing Universe snapshot. */
  public static M3FileUniverseSnapshot read(Path directory, M3FileResolver resolver)
      throws IOException {
    M3FileResolver checkedResolver = Objects.requireNonNull(resolver, "resolver");
    Path target = normalize(directory);
    if (Files.isSymbolicLink(target) || !Files.isDirectory(target)) {
      throw new IOException("Universe image is not a directory");
    }
    List<ManifestFile> manifest = readManifest(target.resolve(MANIFEST_NAME));
    Set<String> expected = new HashSet<>();
    expected.add(MANIFEST_NAME);
    expected.add(M3FileUniverseTokenIndexImage.NAME);
    List<M3FileUniverseEventReader.SnapshotFile> files = new ArrayList<>(manifest.size());
    int tokenCount = 0;
    for (ManifestFile entry : manifest) {
      String semantic = semanticName(entry.ordinal());
      String facts = factsName(entry.ordinal());
      expected.add(semantic);
      expected.add(facts);
      Path semanticPath = target.resolve(semantic);
      Path factsPath = target.resolve(facts);
      requireRegularFile(semanticPath, semantic);
      requireRegularFile(factsPath, facts);
      try (MappedM3File mapped = MappedM3File.open(semanticPath, checkedResolver)) {
        if (mapped.contentFingerprint64() != entry.fingerprint64()
            || mapped.tokenCount() != entry.tokenCount()
            || mapped.uniqueTokenCount() != entry.uniqueTokenCount()) {
          throw new IOException("Universe image manifest does not match semantic child");
        }
        M3FileSnapshot snapshot = M3FileSourceFactsImage.readMappedSnapshot(factsPath, mapped);
        M3FileSourceFactsSnapshot sourceFacts = snapshot.sourceFactsSnapshot();
        if (sourceFacts.byteFactsSnapshot().byteCount() != entry.byteCount()
            || !sourceFacts.attributesBeforeRead().normalizedPath().equals(entry.path())) {
          throw new IOException("Universe image manifest does not match source facts child");
        }
        files.add(new M3FileUniverseEventReader.SnapshotFile(
            entry.ordinal(), entry.path(), snapshot));
        tokenCount = Math.addExact(tokenCount, snapshot.primitiveSnapshot().tokenCount());
      }
    }
    M3FileUniverseTokenIndexImage.Data persistedTokenIndex =
        M3FileUniverseTokenIndexImage.read(
            target.resolve(M3FileUniverseTokenIndexImage.NAME), files.size(), tokenCount);
    try (var paths = Files.list(target)) {
      for (Path path : paths.toList()) {
        if (!expected.contains(path.getFileName().toString())) {
          throw new IOException("unexpected file in Universe image: " + path.getFileName());
        }
      }
    }
    return M3FileUniverseSnapshot.from(
        new M3FileUniverseEventReader.SnapshotSummary(files),
        checkedResolver,
        persistedTokenIndex);
  }

  private static void writeManifest(
      Path target, List<M3FileUniverseSnapshot.File> files, M3FileResolver resolver)
      throws IOException {
    try {
      ByteArrayOutputStream bodyBytes = new ByteArrayOutputStream();
      try (DataOutputStream body = new DataOutputStream(bodyBytes)) {
        body.write(MAGIC);
        body.writeInt(FORMAT);
        body.writeInt(files.size());
        for (int ordinal = 0; ordinal < files.size(); ordinal++) {
          M3FileUniverseSnapshot.File file = files.get(ordinal);
          byte[] path = file.path().getBytes(StandardCharsets.UTF_8);
          if (path.length > MAX_PATH_BYTES) throw new IOException("Universe path is too long");
          M3FileSnapshot snapshot = file.snapshot();
          M3FileSourceFactsSnapshot facts = snapshot.sourceFactsSnapshot();
          body.writeInt(ordinal);
          body.writeInt(path.length);
          body.write(path);
          body.writeLong(M3FileImage.contentFingerprint64(snapshot, resolver));
          body.writeInt(snapshot.primitiveSnapshot().tokenCount());
          body.writeInt(snapshot.primitiveSnapshot().uniqueTokenCount());
          body.writeLong(facts.byteFactsSnapshot().byteCount());
        }
      }
      byte[] body = bodyBytes.toByteArray();
      byte[] digest = sha256(body);
      ByteArrayOutputStream image = new ByteArrayOutputStream(body.length + DIGEST_BYTES);
      image.write(body);
      image.write(digest);
      Files.write(target, image.toByteArray());
    } catch (RuntimeException invalid) {
      throw new IOException("cannot encode Universe image manifest", invalid);
    }
  }

  private static List<ManifestFile> readManifest(Path source) throws IOException {
    if (Files.isSymbolicLink(source) || !Files.isRegularFile(source) || Files.size(source) > MAX_MANIFEST_BYTES) {
      throw new IOException("missing or oversized Universe image manifest");
    }
    byte[] image = Files.readAllBytes(source);
    if (image.length < MAGIC.length + Integer.BYTES * 2 + DIGEST_BYTES) {
      throw new IOException("truncated Universe image manifest");
    }
    int bodyLength = image.length - DIGEST_BYTES;
    byte[] body = java.util.Arrays.copyOf(image, bodyLength);
    byte[] expectedDigest = java.util.Arrays.copyOfRange(image, bodyLength, image.length);
    if (!MessageDigest.isEqual(expectedDigest, sha256(body))) {
      throw new IOException("Universe image manifest checksum mismatch");
    }
    try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(body))) {
      byte[] magic = input.readNBytes(MAGIC.length);
      if (!java.util.Arrays.equals(MAGIC, magic)) throw new IOException("invalid Universe image magic");
      if (input.readInt() != FORMAT) throw new IOException("unsupported Universe image format");
      int count = input.readInt();
      if (count < 0 || count > MAX_FILE_COUNT) throw new IOException("invalid Universe image file count");
      List<ManifestFile> files = new ArrayList<>(count);
      for (int ordinal = 0; ordinal < count; ordinal++) {
        if (input.readInt() != ordinal) throw new IOException("non-contiguous Universe image ordinal");
        int length = input.readInt();
        if (length < 0 || length > MAX_PATH_BYTES) throw new IOException("invalid Universe image path length");
        byte[] path = input.readNBytes(length);
        if (path.length != length) throw new IOException("truncated Universe image path");
        files.add(
            new ManifestFile(
                ordinal,
                new String(path, StandardCharsets.UTF_8),
                input.readLong(),
                input.readInt(),
                input.readInt(),
                input.readLong()));
      }
      if (input.read() != -1) throw new IOException("trailing Universe image manifest bytes");
      return files;
    } catch (EOFException | RuntimeException malformed) {
      throw new IOException("malformed Universe image manifest", malformed);
    }
  }

  private static void requireRegularFile(Path path, String label) throws IOException {
    if (Files.isSymbolicLink(path) || !Files.isRegularFile(path)) {
      throw new IOException("missing Universe image component: " + label);
    }
  }

  private static String semanticName(int ordinal) {
    return String.format(java.util.Locale.ROOT, "file-%08d.midxf", ordinal);
  }

  private static String factsName(int ordinal) {
    return String.format(java.util.Locale.ROOT, "file-%08d.midxfs", ordinal);
  }

  private static Path normalize(Path path) {
    return Objects.requireNonNull(path, "directory").toAbsolutePath().normalize();
  }

  private static byte[] sha256(byte[] value) throws IOException {
    try {
      return MessageDigest.getInstance("SHA-256").digest(value);
    } catch (NoSuchAlgorithmException impossible) {
      throw new IOException("SHA-256 is unavailable", impossible);
    }
  }

  private static void deleteTree(Path root) throws IOException {
    if (root == null || !Files.exists(root)) return;
    try (var paths = Files.walk(root)) {
      for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
        Files.deleteIfExists(path);
      }
    }
  }

  private record ManifestFile(
      int ordinal, String path, long fingerprint64, int tokenCount, int uniqueTokenCount,
      long byteCount) {
    private ManifestFile {
      if (tokenCount < 0 || uniqueTokenCount < 0 || byteCount < 0L) {
        throw new IllegalArgumentException("invalid Universe image manifest file facts");
      }
    }
  }
}
