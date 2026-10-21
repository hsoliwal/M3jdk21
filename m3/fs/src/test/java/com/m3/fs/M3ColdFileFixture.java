// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.fs;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Independent test-only encoder of the pinned SMFILE01/SMFSA001 contracts using scalar tokens. */
final class M3ColdFileFixture {
  static final long GENERATION = 0x17ad453bc90e8f21L;
  private static final long BASE_A = 0x9e3779b185ebca87L, BASE_B = 0xc2b2ae3d27d4eb4fL;
  final Path source;
  final byte[] raw;
  final int[] handles, utf16, lines, frequencies, first, last, postingStarts, postings;
  final long[] utf8;
  final String[] lexicon;
  final long fingerprint;
  int utf8LanePosition;

  static M3ColdFileFixture fromSource(Path source) throws IOException {
    return new M3ColdFileFixture(source, Files.readString(source, StandardCharsets.UTF_8));
  }

  private M3ColdFileFixture(Path source, String text) {
    this.source = source;
    raw = text.getBytes(StandardCharsets.UTF_8);
    int count = text.codePointCount(0, text.length());
    handles = new int[count]; utf16 = new int[count + 1]; utf8 = new long[count + 1];
    Map<String, Integer> unique = new LinkedHashMap<>();
    List<Integer> line = new ArrayList<>(); line.add(0);
    int token = 0;
    for (int at = 0; at < text.length(); token++) {
      int end = at + Character.charCount(text.codePointAt(at));
      String value = text.substring(at, end);
      handles[token] = unique.computeIfAbsent(value, ignored -> unique.size());
      utf16[token + 1] = end;
      utf8[token + 1] = utf8[token] + value.getBytes(StandardCharsets.UTF_8).length;
      if (value.equals("\n")) line.add(end);
      at = end;
    }
    lines = line.stream().mapToInt(Integer::intValue).toArray();
    lexicon = unique.keySet().toArray(String[]::new);
    frequencies = new int[lexicon.length]; first = new int[lexicon.length]; last = new int[lexicon.length];
    Arrays.fill(first, -1); Arrays.fill(last, -1);
    for (int i = 0; i < handles.length; i++) {
      int h = handles[i]; frequencies[h]++; if (first[h] == -1) first[h] = i; last[h] = i;
    }
    postingStarts = new int[lexicon.length + 1];
    for (int i = 0; i < lexicon.length; i++) postingStarts[i + 1] = postingStarts[i] + frequencies[i];
    int[] cursor = postingStarts.clone(); postings = new int[count];
    for (int i = 0; i < count; i++) postings[cursor[handles[i]]++] = i;
    fingerprint = fingerprint();
  }

  int tokenCount() { return handles.length; }
  M3ColdFileFixture sourceFacts() { return this; }
  long utf8Offset(int boundary) { return utf8[boundary]; }
  int localHandle(int language, String text) {
    if (language != 7) throw new IllegalArgumentException("fixture language");
    for (int i = 0; i < lexicon.length; i++) if (lexicon[i].equals(text)) return i;
    return -1;
  }
  int firstOccurrence(int handle) { return first[handle]; }
  private int width() { return lexicon.length <= 256 ? 1 : lexicon.length <= 65536 ? 2 : 4; }

  byte[] semanticImage() throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (DataOutputStream out = new DataOutputStream(bytes)) {
      out.writeLong(0x534d46494c453031L); out.writeInt(1);
      out.writeInt(handles.length); out.writeInt(lexicon.length); out.writeInt(width());
      out.writeInt(lines.length); out.writeInt(lexicon.length == 0 ? 0 : 1);
      out.writeInt(lexicon.length); out.writeInt(0); out.writeLong(fingerprint);
      if (lexicon.length != 0) { out.writeInt(7); out.writeLong(GENERATION); }
      for (int i = 0; i <= lexicon.length; i++) out.writeInt(i);
      align(out, 8);
      for (int i = 0; i < lexicon.length; i++) out.writeLong((7L << 48) | (i + 1));
      for (int h : handles) {
        switch (width()) { case 1 -> out.writeByte(h); case 2 -> out.writeShort(h); default -> out.writeInt(h); }
      }
      for (int ignored : handles) out.writeByte(0);
      align(out, 4);
      for (int v : utf16) out.writeInt(v);
      for (int v : lines) out.writeInt(v);
      for (int v : frequencies) out.writeInt(v);
      for (int v : first) out.writeInt(v);
      for (int v : last) out.writeInt(v);
      for (int v : postingStarts) out.writeInt(v);
      for (int v : postings) out.writeInt(v);
    }
    return bytes.toByteArray();
  }

  byte[] sourceFactsImage() throws IOException { return sourceFactsImage(false); }
  byte[] sourceFactsImage(boolean reverseMapKeys) throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (DataOutputStream out = new DataOutputStream(bytes)) {
      out.writeLong(0x534d465341303031L); out.writeInt(1); out.writeLong(fingerprint);
      out.writeInt(handles.length); out.writeInt(lexicon.length);
      attributes(out, 120, reverseMapKeys); attributes(out, 121, reverseMapKeys);
      out.writeLong(raw.length); out.write(sha(raw));
      long[] histogram = new long[256]; for (byte b : raw) histogram[Byte.toUnsignedInt(b)]++;
      int unique = 0; double entropy = 0;
      for (long n : histogram) {
        out.writeLong(n);
        if (n > 0) { unique++; double p = (double) n / raw.length; entropy -= p * (Math.log(p) / Math.log(2.0)); }
      }
      writeBytes(out, Arrays.copyOf(raw, Math.min(64, raw.length)));
      writeBytes(out, Arrays.copyOfRange(raw, Math.max(0, raw.length - 64), raw.length));
      out.writeInt(unique); out.writeLong(Double.doubleToLongBits(entropy));
      out.writeInt((raw.length + 524287) / 524288);
      for (int start = 0; start < raw.length; start += 524288) {
        int length = Math.min(524288, raw.length - start); out.writeInt(length);
        out.write(sha(Arrays.copyOfRange(raw, start, start + length)));
      }
      utf8LanePosition = out.size();
      writeLongs(out, utf8);
      out.writeInt(handles.length + 1); for (int i = 0; i <= handles.length; i++) out.writeInt(i);
      byte[][] hashes = new byte[lexicon.length][];
      out.writeInt(lexicon.length * 32);
      for (int i = 0; i < lexicon.length; i++) { hashes[i] = sha(lexicon[i].getBytes(StandardCharsets.UTF_8)); out.write(hashes[i]); }
      long[] pa = new long[handles.length + 1], pb = new long[handles.length + 1];
      long[] powA = new long[handles.length + 1], powB = new long[handles.length + 1];
      powA[0] = powB[0] = 1;
      for (int i = 0; i < handles.length; i++) {
        ByteBuffer h = ByteBuffer.wrap(hashes[handles[i]]);
        pa[i + 1] = pa[i] * BASE_A + rangeMix(h.getLong(0) ^ h.getLong(16));
        pb[i + 1] = pb[i] * BASE_B + rangeMix(h.getLong(8) ^ h.getLong(24));
        powA[i + 1] = powA[i] * BASE_A; powB[i + 1] = powB[i] * BASE_B;
      }
      writeLongs(out, pa); writeLongs(out, pb); writeLongs(out, powA); writeLongs(out, powB);
      out.flush(); byte[] body = bytes.toByteArray(); out.write(sha(body));
    }
    return bytes.toByteArray();
  }

  private void attributes(DataOutputStream out, int access, boolean reverse) throws IOException {
    string(out, source.toAbsolutePath().normalize().toString()); out.writeLong(raw.length);
    out.writeLong(100); out.writeInt(20); out.writeLong(110); out.writeInt(30);
    out.writeLong(access); out.writeInt(40);
    out.writeBoolean(true); out.writeBoolean(false); out.writeBoolean(false); out.writeBoolean(false);
    string(out, "fixture-file-key"); string(out, null); string(out, null);
    out.writeBoolean(false); out.writeInt(0);
    for (int i = 0; i < 5; i++) out.writeBoolean(false);
    out.writeInt(0); out.writeInt(0); out.writeInt(2);
    // Java UTF-16 map order intentionally differs from unsigned UTF-8 order here.
    string(out, reverse ? "\ue000" : "😀"); string(out, "one");
    string(out, reverse ? "😀" : "\ue000"); string(out, "two");
  }

  private long fingerprint() {
    long h = 0xcbf29ce484222325L;
    for (byte b : "SYNEXIA-MINDEX-FILE-IMAGE-1".getBytes(StandardCharsets.UTF_8)) h = (h ^ Byte.toUnsignedInt(b)) * 0x100000001b3L;
    for (int v : new int[] {1, handles.length, lexicon.length, width(), lines.length, lexicon.length == 0 ? 0 : 1, lexicon.length}) h = mix(h, v);
    if (lexicon.length != 0) { h = mix(h, 7); h = mix(h, GENERATION); }
    for (int i = 0; i <= lexicon.length; i++) h = mix(h, i);
    for (int i = 0; i < lexicon.length; i++) h = mix(h, (7L << 48) | (i + 1));
    for (int v : handles) h = mix(h, v);
    for (int ignored : handles) h = mix(h, 0);
    for (int[] lane : new int[][] {utf16, lines, frequencies, first, last, postingStarts, postings}) {
      for (int v : lane) h = mix(h, v);
    }
    return h;
  }
  private static long mix(long a, long b) { return mix64(a ^ Long.rotateLeft(b, 29) ^ 0x9e3779b97f4a7c15L); }
  private static long mix64(long z) {
    z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L; z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL; return z ^ (z >>> 31);
  }
  private static long rangeMix(long value) {
    long z = value + 0xd6e8feb86659fd93L;
    z = (z ^ (z >>> 32)) * 0xd6e8feb86659fd93L;
    z = (z ^ (z >>> 32)) * 0xd6e8feb86659fd93L;
    return z ^ (z >>> 32);
  }
  private static void align(DataOutputStream out, int width) throws IOException { while (out.size() % width != 0) out.writeByte(0); }
  private static void string(DataOutputStream out, String value) throws IOException {
    if (value == null) out.writeInt(-1); else writeBytes(out, value.getBytes(StandardCharsets.UTF_8));
  }
  private static void writeBytes(DataOutputStream out, byte[] values) throws IOException { out.writeInt(values.length); out.write(values); }
  private static void writeLongs(DataOutputStream out, long[] values) throws IOException { out.writeInt(values.length); for (long v : values) out.writeLong(v); }
  static byte[] sha(byte[] bytes) {
    try { return MessageDigest.getInstance("SHA-256").digest(bytes); }
    catch (java.security.NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
  }
  static byte[] resign(byte[] bytes) {
    byte[] out = bytes.clone(); byte[] digest = sha(Arrays.copyOf(out, out.length - 32));
    System.arraycopy(digest, 0, out, out.length - 32, 32); return out;
  }
}
