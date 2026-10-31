// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/** Stable key for one source-facts sidecar and its semantic M3File shape. */
public record M3FileSourceFactsKey(
    String sourceRef, long contentFingerprint64, int tokenCount, int uniqueTokenCount) {
  public M3FileSourceFactsKey {
    sourceRef = Objects.requireNonNull(sourceRef, "sourceRef");
    if (sourceRef.isBlank()) throw new IllegalArgumentException("sourceRef must not be blank");
    if (tokenCount < 0 || uniqueTokenCount < 0) {
      throw new IllegalArgumentException("negative M3File source-facts shape");
    }
  }

  static M3FileSourceFactsKey of(String sourceRef, M3File file) {
    M3File checked = Objects.requireNonNull(file, "file");
    return new M3FileSourceFactsKey(
        sourceRef, M3FileImage.contentFingerprint64(checked), checked.tokenCount(), checked.uniqueTokenCount());
  }

  /** Content-derived filename component; mutable paths and hostnames never enter the identity. */
  public String storageId() {
    try {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      try (DataOutputStream out = new DataOutputStream(bytes)) {
        byte[] ref = sourceRef.getBytes(StandardCharsets.UTF_8);
        out.writeInt(ref.length);
        out.write(ref);
        out.writeLong(contentFingerprint64);
        out.writeInt(tokenCount);
        out.writeInt(uniqueTokenCount);
      }
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
    } catch (IOException impossible) {
      throw new AssertionError("byte-array stream cannot fail", impossible);
    } catch (NoSuchAlgorithmException impossible) {
      throw new AssertionError("SHA-256 is required by the JDK", impossible);
    }
  }
}
