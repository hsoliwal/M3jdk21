// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Deterministic create-only mmap image for {@link M3IndexPrecomputeManifest}.
 *
 * <p>The canonical manifest text is the payload. Its SHA-256 is stored both in the fixed header and
 * footer. Opening verifies file geometry, header/footer equality, payload SHA-256 and canonical
 * reparse before exposing the replay plan.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/0616288bfa7d91eb0abb754c7a29707f10318752/synexia-index-precompute/src/main/java/com/synexia/precompute/IndexPrecomputeManifestImage.java blob 4e9056dba113; recipe m3/ports/precompute/recipe/planner-family-20261107/relocate.py v4
public final class M3IndexPrecomputeManifestImage {
  private static final long MAGIC = 0x5359504d414e3031L; // SYPMAN01
  private static final int VERSION = 1;
  private static final int HEADER_BYTES = 48;
  private static final int FOOTER_BYTES = 32;

  private M3IndexPrecomputeManifestImage() {}

  public static void writeCreateOnly(
      M3IndexPrecomputeManifest manifest,
      Path output) throws IOException {
    Objects.requireNonNull(manifest, "manifest");
    Objects.requireNonNull(output, "output");
    byte[] payload = manifest.canonicalText().getBytes(StandardCharsets.UTF_8);
    byte[] root = HexFormat.of().parseHex(manifest.rootHash());
    if (root.length != 32) throw new IllegalArgumentException("manifest root is not SHA-256");

    long total = (long) HEADER_BYTES + payload.length + FOOTER_BYTES;
    if (total > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("precompute manifest image exceeds 2 GiB");
    }

    Path absolute = output.toAbsolutePath().normalize();
    Path parent = absolute.getParent();
    if (parent != null) Files.createDirectories(parent);

    ByteBuffer image = ByteBuffer.allocate(Math.toIntExact(total)).order(ByteOrder.BIG_ENDIAN);
    image.putLong(MAGIC);
    image.putInt(VERSION);
    image.putInt(payload.length);
    image.put(root);
    image.put(payload);
    image.put(root);
    image.flip();

    try (FileChannel channel = FileChannel.open(
        absolute,
        StandardOpenOption.CREATE_NEW,
        StandardOpenOption.WRITE)) {
      while (image.hasRemaining()) channel.write(image);
      channel.force(true);
    }
  }

  public static Mapped map(Path input) throws IOException {
    Objects.requireNonNull(input, "input");
    MappedByteBuffer mapped;
    try (FileChannel channel = FileChannel.open(input, StandardOpenOption.READ)) {
      long size = channel.size();
      if (size < HEADER_BYTES + FOOTER_BYTES || size > Integer.MAX_VALUE) {
        throw new IOException("invalid precompute manifest image size " + size);
      }
      mapped = channel.map(FileChannel.MapMode.READ_ONLY, 0, size);
    }
    mapped.order(ByteOrder.BIG_ENDIAN);

    require(mapped.getLong(0) == MAGIC, "not a Synexia precompute manifest image");
    require(mapped.getInt(8) == VERSION, "unsupported precompute manifest image version");
    int payloadLength = mapped.getInt(12);
    require(payloadLength >= 0, "negative manifest payload length");
    long expected = (long) HEADER_BYTES + payloadLength + FOOTER_BYTES;
    require(expected == mapped.capacity(), "manifest image length mismatch");

    byte[] headerRoot = new byte[32];
    ByteBuffer header = mapped.duplicate();
    header.position(16).get(headerRoot);

    byte[] footerRoot = new byte[32];
    ByteBuffer footer = mapped.duplicate();
    footer.position(HEADER_BYTES + payloadLength).get(footerRoot);
    require(java.util.Arrays.equals(headerRoot, footerRoot), "manifest header/footer root mismatch");

    ByteBuffer payload = mapped.duplicate();
    payload.position(HEADER_BYTES);
    payload.limit(HEADER_BYTES + payloadLength);
    ByteBuffer payloadSlice = payload.slice().asReadOnlyBuffer();

    byte[] calculated = sha256(payloadSlice.duplicate());
    require(java.util.Arrays.equals(headerRoot, calculated), "manifest payload SHA-256 mismatch");

    String canonical = StandardCharsets.UTF_8.decode(payloadSlice.duplicate()).toString();
    M3IndexPrecomputeManifest manifest;
    try {
      manifest = M3IndexPrecomputeManifest.parseCanonical(canonical);
    } catch (RuntimeException invalid) {
      throw new IOException("invalid canonical precompute manifest", invalid);
    }
    String rootHex = HexFormat.of().formatHex(headerRoot);
    require(rootHex.equals(manifest.rootHash()), "manifest root/canonical payload mismatch");

    return new Mapped(mapped, payloadSlice, manifest, rootHex);
  }

  private static byte[] sha256(ByteBuffer buffer) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.update(buffer);
      return digest.digest();
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException("SHA-256 unavailable", impossible);
    }
  }

  private static void require(boolean condition, String message) throws IOException {
    if (!condition) throw new IOException(message);
  }

  /** Read-only mapped replay view. */
  public static final class Mapped {
    private final ByteBuffer image;
    private final ByteBuffer payload;
    private final M3IndexPrecomputeManifest manifest;
    private final String rootHash;

    private Mapped(
        ByteBuffer image,
        ByteBuffer payload,
        M3IndexPrecomputeManifest manifest,
        String rootHash) {
      this.image = image.asReadOnlyBuffer().order(ByteOrder.BIG_ENDIAN);
      this.payload = payload.asReadOnlyBuffer();
      this.manifest = manifest;
      this.rootHash = rootHash;
    }

    public String rootHash() { return rootHash; }
    public int mappedBytes() { return image.capacity(); }
    public M3IndexPrecomputeManifest manifest() { return manifest; }
    public M3IndexPrecomputePlan plan() { return manifest.toPlan(); }

    /** Read-only canonical payload for zero-copy forwarding/audit tooling. */
    public ByteBuffer payloadView() {
      return payload.asReadOnlyBuffer();
    }
  }
}
