// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.OptionalLong;

/**
 * Packed ID metadata over canonical Java String references. No byte/char arena,
 * independent text intern service or native payload is introduced by this class.
 * A miss crosses the ordinary String construction/intern boundary exactly once.
 * In an enabled matched M3JDK those Strings use the existing M3String runtime;
 * a stock JDK supplies its ordinary String representation for compatibility tests.
 *
 * <p>Live dictionaries assign overlay IDs and never claim durable coordinates.
 * Frozen dictionaries preserve caller-supplied language and word order, assign
 * base IDs, expose the original coordinate fingerprint, and refuse new words.
 * String references, IDs, hashes and table slots use parallel arrays; no Entry
 * object is retained per atom. A dictionary never removes or changes an ID.</p>
 */
public final class M3FileDictionary implements M3FileResolver {
  /** Ordered input for one immutable language generation. Local IDs equal word ordinals. */
  public record Language(int id, String tag, String version, List<String> words) {
    public Language {
      M3FileId.base(id, 0);
      Objects.requireNonNull(tag, "tag");
      Objects.requireNonNull(version, "version");
      words = List.copyOf(Objects.requireNonNull(words, "words"));
      // The unchanged donor coordinate hash uses UTF-8. Its durable packed dictionary
      // domain cannot distinguish encoder replacements of malformed UTF-16. Refuse
      // those generations here; live String atoms retain the entire UTF-16 domain.
      requireWellFormed(tag, "language tag");
      requireWellFormed(version, "version");
      for (String word : words) requireWellFormed(word, "word");
    }
  }

  private String[] words = new String[16];
  private long[] ids = new long[16];
  private int[] hashes = new int[16];
  private M3TextMetrics[] metrics = new M3TextMetrics[16];
  private M3TextFacts[] facts = new M3TextFacts[16];
  private int[] slots = new int[32];
  private int[] idSlots = new int[32];
  private int size;
  private int[] languages = new int[4];
  private int[] nextLocal = new int[4];
  private long[] fingerprints = new long[4];
  private int languageCount;
  private boolean frozen;

  /** A live overlay dictionary. Its ordinary String atoms remain caller-visible JDK values. */
  public M3FileDictionary() { }

  public static M3FileDictionary frozen(int languageId, String tag, String version,
      String... words) {
    return frozen(List.of(new Language(languageId, tag, version,
        Arrays.asList(Objects.requireNonNull(words, "words").clone()))));
  }

  public static M3FileDictionary frozen(List<Language> generations) {
    List<Language> input = List.copyOf(Objects.requireNonNull(generations, "generations"));
    M3FileDictionary result = new M3FileDictionary();
    for (Language generation : input) {
      if (result.languageSlot(generation.id()) >= 0) {
        throw new IllegalArgumentException("duplicate language generation");
      }
      int languageSlot = result.addLanguage(generation.id());
      String[] stable = generation.words().toArray(String[]::new);
      for (int local = 0; local < stable.length; local++) {
        String spelling = Objects.requireNonNull(stable[local], "word");
        if (result.lookup(generation.id(), spelling).isPresent()) {
          throw new IllegalArgumentException("duplicate spelling in frozen language");
        }
        result.add(M3FileId.base(generation.id(), local), spelling.intern());
      }
      result.nextLocal[languageSlot] = stable.length;
      result.fingerprints[languageSlot] = M3FileCoordinates.compute(generation.id(),
          generation.tag(), generation.version(), stable);
    }
    result.frozen = true;
    return result;
  }

  public synchronized int size() { return size; }
  public synchronized boolean isFrozen() { return frozen; }

  @Override public synchronized OptionalLong lookup(int languageId, CharSequence term) {
    M3FileId.base(languageId, 0);
    CharSequence text = Objects.requireNonNull(term, "term");
    int slot = findSlot(languageId, text, hash(text));
    return slots[slot] == 0 ? OptionalLong.empty() : OptionalLong.of(ids[slots[slot] - 1]);
  }

  @Override public synchronized long intern(int languageId, CharSequence term) {
    M3FileId.base(languageId, 0);
    CharSequence text = Objects.requireNonNull(term, "term");
    int textHash = hash(text);
    int slot = findSlot(languageId, text, textHash);
    if (slots[slot] != 0) return ids[slots[slot] - 1];
    if (frozen) throw new IllegalArgumentException("unknown spelling in frozen file dictionary");
    // Mutable CharSequence ingress is read only during this synchronized call.
    // The input must remain stable throughout it, as for the donor resolver contract.
    String spelling;
    if (text instanceof String string) spelling = string;
    else {
      StringBuilder ingress = new StringBuilder(text.length());
      for (int i = 0; i < text.length(); i++) ingress.append(text.charAt(i));
      spelling = ingress.toString();
    }
    int language = languageSlot(languageId);
    if (language < 0) language = addLanguage(languageId);
    int local = nextLocal[language];
    if (local == Integer.MAX_VALUE) throw new IllegalStateException("file dictionary row limit");
    long id = M3FileId.overlay(languageId, local);
    add(id, spelling.intern());
    nextLocal[language] = local + 1;
    return id;
  }

  @Override public synchronized String resolve(long id) { return words[row(id)]; }

  @Override public synchronized OptionalLong coordinateFingerprint(int languageId) {
    M3FileId.base(languageId, 0);
    int slot = languageSlot(languageId);
    return !frozen || slot < 0 ? OptionalLong.empty() : OptionalLong.of(fingerprints[slot]);
  }

  @Override public synchronized M3TextMetrics metrics(long id) {
    int row = row(id);
    M3TextMetrics value = metrics[row];
    if (value == null) metrics[row] = value = M3TextMetrics.of(words[row]);
    return value;
  }

  @Override public synchronized M3TextFacts facts(long id) {
    int row = row(id);
    M3TextFacts value = facts[row];
    if (value == null) facts[row] = value = M3TextFacts.of(words[row]);
    return value;
  }

  private int row(long id) {
    int language = M3FileId.language(id);
    if (M3FileId.isOverlay(id) == frozen) throw new IllegalArgumentException("foreign ID kind");
    long local = M3FileId.local(id);
    int languageSlot = languageSlot(language);
    if (languageSlot < 0 || local >= nextLocal[languageSlot]) {
      throw new IllegalArgumentException("unknown file dictionary ID");
    }
    // A separate primitive numeric table keeps reverse lookup independent of spelling
    // and constant expected time even when many live language rows are interleaved.
    int slot = findIdSlot(id);
    if (idSlots[slot] != 0) return idSlots[slot] - 1;
    throw new IllegalArgumentException("unknown file dictionary ID");
  }

  private int findIdSlot(long id) {
    long value = id;
    value ^= value >>> 33; value *= 0xff51afd7ed558ccdL;
    value ^= value >>> 33; value *= 0xc4ceb9fe1a85ec53L;
    int slot = (int) (value ^ (value >>> 33)) & (idSlots.length - 1);
    while (idSlots[slot] != 0 && ids[idSlots[slot] - 1] != id) {
      slot = (slot + 1) & (idSlots.length - 1);
    }
    return slot;
  }

  private int findSlot(int languageId, CharSequence text, int hash) {
    int slot = mixed(hash, languageId) & (slots.length - 1);
    while (slots[slot] != 0) {
      int row = slots[slot] - 1;
      if (hashes[row] == hash && M3FileId.language(ids[row]) == languageId
          && contentEquals(words[row], text)) return slot;
      slot = (slot + 1) & (slots.length - 1);
    }
    return slot;
  }

  private void add(long id, String text) {
    if (size >= Integer.MAX_VALUE - 8) throw new IllegalStateException("file dictionary row limit");
    if (size == words.length) {
      int capacity = Math.toIntExact(Math.min(Integer.MAX_VALUE - 8L,
          (long) words.length + (words.length >>> 1) + 1L));
      words = Arrays.copyOf(words, capacity); ids = Arrays.copyOf(ids, capacity);
      hashes = Arrays.copyOf(hashes, capacity); metrics = Arrays.copyOf(metrics, capacity);
      facts = Arrays.copyOf(facts, capacity);
    }
    if ((long) (size + 1) * 10 >= (long) slots.length * 6) {
      if (slots.length >= (1 << 30)) throw new IllegalStateException("file dictionary hash limit");
      slots = new int[slots.length << 1];
      idSlots = new int[slots.length];
      for (int row = 0; row < size; row++) {
        slots[findSlot(M3FileId.language(ids[row]), words[row], hashes[row])] = row + 1;
        idSlots[findIdSlot(ids[row])] = row + 1;
      }
    }
    int hash = text.hashCode();
    int slot = findSlot(M3FileId.language(id), text, hash);
    words[size] = text; ids[size] = id; hashes[size] = hash;
    slots[slot] = ++size;
    idSlots[findIdSlot(id)] = size;
  }

  private int languageSlot(int languageId) {
    return Arrays.binarySearch(languages, 0, languageCount, languageId);
  }

  private int addLanguage(int languageId) {
    int position = -languageSlot(languageId) - 1;
    if (position < 0) throw new IllegalArgumentException("duplicate language");
    if (languageCount == languages.length) {
      int capacity = Math.min(65536, languages.length * 2);
      languages = Arrays.copyOf(languages, capacity);
      nextLocal = Arrays.copyOf(nextLocal, capacity);
      fingerprints = Arrays.copyOf(fingerprints, capacity);
    }
    System.arraycopy(languages, position, languages, position + 1, languageCount - position);
    System.arraycopy(nextLocal, position, nextLocal, position + 1, languageCount - position);
    System.arraycopy(fingerprints, position, fingerprints, position + 1, languageCount - position);
    languages[position] = languageId; nextLocal[position] = 0; fingerprints[position] = 0;
    languageCount++;
    return position;
  }

  private static int hash(CharSequence text) {
    int value = 0;
    for (int i = 0; i < text.length(); i++) value = 31 * value + text.charAt(i);
    return value;
  }

  private static void requireWellFormed(String text, String field) {
    for (int i = 0; i < text.length(); i++) {
      char unit = text.charAt(i);
      if (Character.isHighSurrogate(unit)) {
        if (++i >= text.length() || !Character.isLowSurrogate(text.charAt(i))) {
          throw new IllegalArgumentException("frozen " + field + " contains malformed UTF-16");
        }
      } else if (Character.isLowSurrogate(unit)) {
        throw new IllegalArgumentException("frozen " + field + " contains malformed UTF-16");
      }
    }
  }

  private static boolean contentEquals(String owner, CharSequence text) {
    if (owner.length() != text.length()) return false;
    for (int i = 0; i < owner.length(); i++) if (owner.charAt(i) != text.charAt(i)) return false;
    return true;
  }

  private static int mixed(int hash, int languageId) {
    int value = hash ^ (languageId * 0x9e3779b9);
    value ^= value >>> 16; value *= 0x7feb352d;
    value ^= value >>> 15; value *= 0x846ca68b;
    return value ^ (value >>> 16);
  }
}
