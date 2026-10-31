// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.fs;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Deterministic identity of one precompute operation.
 *
 * <p>Changing an input content ID, algorithm name/version or configuration creates a new key instead
 * of invalidating unrelated cached results.</p>
 */
public final class M3PrecomputeKey {
  private static final int FORMAT = 1;

  private final String type;
  private final String algorithm;
  private final int algorithmVersion;
  private final List<M3ContentId> inputs;
  private final Map<String, String> configuration;
  private final byte[] canonical;
  private final M3ContentId id;

  public M3PrecomputeKey(
      String type,
      String algorithm,
      int algorithmVersion,
      List<M3ContentId> inputs,
      Map<String, String> configuration) {
    this.type = requireName(type, "type");
    this.algorithm = requireName(algorithm, "algorithm");
    if (algorithmVersion < 0) throw new IllegalArgumentException("negative algorithm version");
    this.algorithmVersion = algorithmVersion;
    this.inputs = List.copyOf(Objects.requireNonNull(inputs, "inputs"));
    this.inputs.forEach(id -> Objects.requireNonNull(id, "input"));
    TreeMap<String, String> sorted = new TreeMap<>();
    Objects.requireNonNull(configuration, "configuration")
        .forEach(
            (key, value) ->
                sorted.put(
                    requireName(key, "configuration key"),
                    Objects.requireNonNull(value, "configuration value")));
    this.configuration = Map.copyOf(sorted);
    this.canonical = encode();
    this.id = M3ContentId.digest(canonical);
  }

  public String type() { return type; }
  public String algorithm() { return algorithm; }
  public int algorithmVersion() { return algorithmVersion; }
  public List<M3ContentId> inputs() { return inputs; }
  public Map<String, String> configuration() { return configuration; }
  public M3ContentId id() { return id; }
  public byte[] canonicalBytes() { return canonical.clone(); }

  /** Reconstructs the exact deterministic key from its canonical bytes. */
  public static M3PrecomputeKey decode(byte[] encoded) throws IOException {
    byte[] bytes = Objects.requireNonNull(encoded, "encoded");
    try (java.io.DataInputStream in =
        new java.io.DataInputStream(new java.io.ByteArrayInputStream(bytes))) {
      if (in.readInt() != FORMAT) {
        throw new IOException("unsupported M3 precompute key format");
      }
      String type = readString(in, "type");
      String algorithm = readString(in, "algorithm");
      int version = in.readInt();
      if (version < 0) throw new IOException("negative algorithm version");

      int inputCount = in.readInt();
      if (inputCount < 0 || inputCount > 65536) {
        throw new IOException("invalid precompute input count");
      }
      java.util.ArrayList<M3ContentId> inputs = new java.util.ArrayList<>(inputCount);
      for (int index = 0; index < inputCount; index++) {
        byte[] id = in.readNBytes(M3ContentId.BYTES);
        if (id.length != M3ContentId.BYTES) {
          throw new IOException("truncated precompute input ID");
        }
        inputs.add(M3ContentId.of(id));
      }

      int configCount = in.readInt();
      if (configCount < 0 || configCount > 65536) {
        throw new IOException("invalid precompute configuration count");
      }
      java.util.TreeMap<String, String> configuration = new java.util.TreeMap<>();
      for (int index = 0; index < configCount; index++) {
        String name = readString(in, "configuration key");
        String value = readString(in, "configuration value");
        if (configuration.putIfAbsent(name, value) != null) {
          throw new IOException("duplicate precompute configuration key");
        }
      }
      if (in.read() != -1) throw new IOException("trailing precompute key bytes");

      M3PrecomputeKey decoded =
          new M3PrecomputeKey(type, algorithm, version, inputs, configuration);
      if (!java.util.Arrays.equals(decoded.canonicalBytes(), bytes)) {
        throw new IOException("non-canonical precompute key bytes");
      }
      return decoded;
    } catch (IllegalArgumentException invalid) {
      throw new IOException("invalid M3 precompute key", invalid);
    }
  }

  private static String readString(java.io.DataInputStream in, String field)
      throws IOException {
    int length = in.readInt();
    if (length < 0 || length > 1024 * 1024) {
      throw new IOException("invalid " + field + " byte length");
    }
    byte[] bytes = in.readNBytes(length);
    if (bytes.length != length) throw new IOException("truncated " + field);
    try {
      return java.nio.charset.StandardCharsets.UTF_8
          .newDecoder()
          .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
          .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
          .decode(java.nio.ByteBuffer.wrap(bytes))
          .toString();
    } catch (java.nio.charset.CharacterCodingException invalid) {
      throw new IOException("invalid UTF-8 in " + field, invalid);
    }
  }

  private byte[] encode() {
    try {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      try (DataOutputStream out = new DataOutputStream(bytes)) {
        out.writeInt(FORMAT);
        writeString(out, type);
        writeString(out, algorithm);
        out.writeInt(algorithmVersion);
        out.writeInt(inputs.size());
        for (M3ContentId input : inputs) out.write(input.bytes());
        out.writeInt(configuration.size());
        for (Map.Entry<String, String> entry : new TreeMap<>(configuration).entrySet()) {
          writeString(out, entry.getKey());
          writeString(out, entry.getValue());
        }
      }
      return bytes.toByteArray();
    } catch (IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  private static String requireName(String value, String label) {
    String checked = Objects.requireNonNull(value, label);
    if (checked.isEmpty() || checked.indexOf('\0') >= 0) {
      throw new IllegalArgumentException(label + " must be non-empty and NUL-free");
    }
    return checked;
  }

  static void writeString(DataOutputStream out, String value) throws IOException {
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    out.writeInt(bytes.length);
    out.write(bytes);
  }
}
