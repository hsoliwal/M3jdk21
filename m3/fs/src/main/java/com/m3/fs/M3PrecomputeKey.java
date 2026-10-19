// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted from the pinned Synexia owner; key metadata uses array columns.
package com.m3.fs;

import java.io.*;
import java.util.*;

/** Deterministic format-1 precompute identity; input ID objects are API projections. */
public final class M3PrecomputeKey {
  private static final int FORMAT = 1;
  private final String type;
  private final String algorithm;
  private final int algorithmVersion;
  private final byte[] inputs;
  private final String[] configNames;
  private final String[] configValues;
  private final byte[] canonical;
  private final byte[] id;

  public M3PrecomputeKey(String type, String algorithm, int algorithmVersion,
      List<M3ContentId> inputs, Map<String, String> configuration) {
    this.type = requireName(type, "type"); this.algorithm = requireName(algorithm, "algorithm");
    if (algorithmVersion < 0) throw new IllegalArgumentException("negative algorithm version");
    this.algorithmVersion = algorithmVersion;
    Objects.requireNonNull(inputs, "inputs"); this.inputs = new byte[Math.multiplyExact(inputs.size(), 32)]; int row = 0;
    for (M3ContentId input : inputs) { System.arraycopy(Objects.requireNonNull(input, "input").bytes(), 0, this.inputs, row * 32, 32); row++; }
    Objects.requireNonNull(configuration, "configuration");
    configNames = configuration.keySet().toArray(String[]::new); Arrays.sort(configNames); configValues = new String[configNames.length];
    for (int i = 0; i < configNames.length; i++) {
      requireName(configNames[i], "configuration key"); configValues[i] = Objects.requireNonNull(configuration.get(configNames[i]), "configuration value");
    }
    canonical = encode(); id = M3Columns.digest(canonical);
  }
  private M3PrecomputeKey(String type, String algorithm, int version, byte[] inputs, String[] names, String[] values) {
    this.type = type; this.algorithm = algorithm; algorithmVersion = version; this.inputs = inputs;
    configNames = names; configValues = values; canonical = encode(); id = M3Columns.digest(canonical);
  }
  public String type() { return type; }
  public String algorithm() { return algorithm; }
  public int algorithmVersion() { return algorithmVersion; }
  public List<M3ContentId> inputs() {
    return Collections.unmodifiableList(new AbstractList<>() {
      @Override public int size() { return inputs.length / 32; }
      @Override public M3ContentId get(int row) { Objects.checkIndex(row, size()); return M3ContentId.of(Arrays.copyOfRange(inputs, row * 32, row * 32 + 32)); }
    });
  }
  public Map<String, String> configuration() {
    return M3Columns.view(configNames.length, i -> configNames[i], i -> configValues[i],
        key -> key instanceof String s ? Arrays.binarySearch(configNames, s) : -1);
  }
  public M3ContentId id() { return M3ContentId.of(id); }
  public byte[] canonicalBytes() { return canonical.clone(); }
  void copyIdTo(byte[] output, int offset) { System.arraycopy(id, 0, output, offset, 32); }

  /** Validates embedded key bytes without constructing a key or input-ID object graph. */
  static void validateCanonical(byte[] encoded) throws IOException {
    try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(encoded))) {
      if (in.readInt() != FORMAT) throw new IOException("unsupported M3 precompute key format");
      requireName(M3Columns.readString(in, false), "type"); requireName(M3Columns.readString(in, false), "algorithm");
      if (in.readInt() < 0) throw new IOException("negative algorithm version");
      int count = M3Columns.readCount(in, 65536, 32); in.skipNBytes((long) count * 32);
      int config = M3Columns.readCount(in, 65536, 9); String previous = null;
      for (int i = 0; i < config; i++) {
        String name = requireName(M3Columns.readString(in, false), "configuration key");
        if (previous != null && previous.compareTo(name) >= 0) throw new IOException("unordered configuration keys");
        previous = name; M3Columns.readString(in, true);
      }
      if (in.read() != -1) throw new IOException("trailing precompute key bytes");
    } catch (IllegalArgumentException invalid) { throw new IOException("invalid precompute key", invalid); }
  }
  public static M3PrecomputeKey decode(byte[] encoded) throws IOException {
    byte[] bytes = Objects.requireNonNull(encoded, "encoded").clone(); validateCanonical(bytes);
    try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
      in.readInt(); String type = M3Columns.readString(in, false), algorithm = M3Columns.readString(in, false); int version = in.readInt();
      int count = in.readInt(); byte[] inputs = new byte[count * 32]; in.readFully(inputs);
      int config = in.readInt(); String[] names = new String[config], values = new String[config];
      for (int i = 0; i < config; i++) { names[i] = M3Columns.readString(in, false); values[i] = M3Columns.readString(in, true); }
      M3PrecomputeKey key = new M3PrecomputeKey(type, algorithm, version, inputs, names, values);
      if (!Arrays.equals(bytes, key.canonical)) throw new IOException("non-canonical precompute key bytes");
      return key;
    }
  }
  private byte[] encode() {
    try {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      try (DataOutputStream out = new DataOutputStream(bytes)) {
        out.writeInt(FORMAT); writeString(out, type); writeString(out, algorithm); out.writeInt(algorithmVersion);
        out.writeInt(inputs.length / 32); out.write(inputs); out.writeInt(configNames.length);
        for (int i = 0; i < configNames.length; i++) { writeString(out, configNames[i]); writeString(out, configValues[i]); }
      }
      return bytes.toByteArray();
    } catch (IOException impossible) { throw new AssertionError(impossible); }
  }
  private static String requireName(String value, String label) {
    Objects.requireNonNull(value, label); if (value.isEmpty() || value.indexOf('\0') >= 0) throw new IllegalArgumentException("invalid " + label); return value;
  }
  static void writeString(DataOutputStream out, String value) throws IOException { M3Columns.writeString(out, value); }
}
