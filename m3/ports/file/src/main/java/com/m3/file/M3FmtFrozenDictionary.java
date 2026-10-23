// Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * M3FMT binding to the existing M3JDK21 frozen M3FileDictionary.
 * No copied string payload, second dictionary, cached Entry or mutable overlay is retained.
 * Language/tag/version/word order are sealed by one deterministic SHA-256 manifest root.
 */
public final class M3FmtFrozenDictionary implements M3FmtLexiconFrame.Lexicon {
  private static final byte[] DOMAIN =
      "M3FMT_FROZEN_DICTIONARY_V1".getBytes(StandardCharsets.US_ASCII);
  private final M3FileDictionary dictionary;
  private final int[] languages;
  private final int[] counts;
  private final String catalogRoot;

  public M3FmtFrozenDictionary(
      M3FileDictionary dictionary, List<M3FileDictionary.Language> generations) throws IOException {
    this.dictionary = Objects.requireNonNull(dictionary, "dictionary");
    if (!dictionary.isFrozen()) throw new IllegalArgumentException("dictionary must be frozen");
    ArrayList<M3FileDictionary.Language> sorted = new ArrayList<>(
        Objects.requireNonNull(generations, "generations"));
    sorted.sort(Comparator.comparingInt(M3FileDictionary.Language::id));
    if (sorted.isEmpty()) throw new IllegalArgumentException("empty lexicon manifest");
    languages = new int[sorted.size()];
    counts = new int[sorted.size()];
    MessageDigest hash = digest();
    DataOutputStream stream = new DataOutputStream(
        new DigestOutputStream(OutputStream.nullOutputStream(), hash));
    stream.writeInt(DOMAIN.length);
    stream.write(DOMAIN);
    stream.writeInt(sorted.size());
    long total = 0L;
    for (int slot = 0; slot < sorted.size(); slot++) {
      M3FileDictionary.Language entry = sorted.get(slot);
      if (slot > 0 && sorted.get(slot - 1).id() == entry.id()) {
        throw new IllegalArgumentException("duplicate frozen language");
      }
      int language = entry.id();
      int count = entry.words().size();
      languages[slot] = language;
      counts[slot] = count;
      total = Math.addExact(total, count);
      stream.writeInt(language);
      text(stream, entry.tag());
      text(stream, entry.version());
      stream.writeInt(count);
      for (int row = 0; row < count; row++) {
        long id = M3FileId.base(language, row);
        String spelling;
        try {
          spelling = dictionary.resolve(id);
        } catch (RuntimeException missing) {
          throw new IllegalArgumentException("frozen dictionary has missing ID", missing);
        }
        if (!spelling.equals(entry.words().get(row))) {
          throw new IllegalArgumentException("frozen dictionary word order mismatch");
        }
        text(stream, spelling);
      }
    }
    if (total != dictionary.size()) {
      throw new IllegalArgumentException("manifest omits or duplicates dictionary IDs");
    }
    stream.flush();
    catalogRoot = HexFormat.of().formatHex(hash.digest());
  }

  @Override public String catalogRoot() { return catalogRoot; }

  @Override public void writeFrozenUtf8(long tokenId, OutputStream destination) throws IOException {
    Objects.requireNonNull(destination, "destination");
    if (M3FileId.isOverlay(tokenId)) throw new IOException("mutable overlay IDs are not transferable");
    int language = M3FileId.language(tokenId);
    int slot = java.util.Arrays.binarySearch(languages, language);
    if (slot < 0 || M3FileId.local(tokenId) >= counts[slot]) {
      throw new IOException("unknown frozen M3File word ID");
    }
    try {
      destination.write(utf8(dictionary.resolve(tokenId)));
    } catch (RuntimeException missing) {
      throw new IOException("unresolvable frozen M3File ID", missing);
    }
  }

  private static void text(DataOutputStream stream, String value) throws IOException {
    byte[] bytes = utf8(value);
    stream.writeInt(bytes.length);
    stream.write(bytes);
  }

  private static byte[] utf8(String value) throws IOException {
    try {
      ByteBuffer encoded = StandardCharsets.UTF_8.newEncoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT)
          .encode(CharBuffer.wrap(Objects.requireNonNull(value, "word")));
      byte[] bytes = new byte[encoded.remaining()];
      encoded.get(bytes);
      return bytes;
    } catch (CharacterCodingException invalid) {
      throw new IOException("malformed frozen dictionary UTF-16", invalid);
    }
  }

  private static MessageDigest digest() {
    try { return MessageDigest.getInstance("SHA-256"); }
    catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
  }
}
