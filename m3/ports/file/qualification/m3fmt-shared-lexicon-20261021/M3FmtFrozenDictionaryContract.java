// Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/** Strict dictionary admission and word-order verification using the real SDK owner. */
public final class M3FmtFrozenDictionaryContract {
  private static int checks;
  private M3FmtFrozenDictionaryContract() {}

  private static void ok(boolean predicate, String message) {
    checks++;
    if (!predicate) throw new AssertionError(message);
  }

  private static void refuses(Action operation) throws Exception {
    checks++;
    try { operation.run(); }
    catch (IllegalArgumentException | IOException expected) { return; }
    throw new AssertionError("expected refusal");
  }

  @FunctionalInterface private interface Action { void run() throws Exception; }

  public static void main(String[] arguments) throws Exception {
    var english = new M3FileDictionary.Language(
        7, "en", "frozen-v1", List.of("apple", " ", "orange", "λ", "class X {}"));
    var french = new M3FileDictionary.Language(
        8, "fr", "frozen-v2", List.of("pomme", " ", "orange"));
    var generations = List.of(english, french);
    M3FileDictionary dictionary = M3FileDictionary.frozen(generations);
    M3FmtFrozenDictionary sender = new M3FmtFrozenDictionary(
        dictionary, List.of(french, english));
    M3FmtFrozenDictionary receiver = new M3FmtFrozenDictionary(
        M3FileDictionary.frozen(generations), generations);
    ok(sender.catalogRoot().equals(receiver.catalogRoot()), "order-invariant metadata roster");
    var frame = M3FmtLexiconFrame.seal(sender, Map.of("format", "java"),
        new byte[] {0, 0, 0, 1, 0},
        new long[] {M3FileId.base(7, 0), M3FileId.base(7, 1),
            M3FileId.base(8, 0), 0, M3FileId.base(7, 4)},
        new int[] {0, 0, 0, 0, 2, 2},
        new byte[] {'\r', '\n'});
    var restored = M3FmtLexiconFrame.read(M3FmtLexiconFrame.write(frame), receiver);
    ok(new String(M3FmtLexiconFrame.materialize(restored, receiver), StandardCharsets.UTF_8)
        .equals("apple pomme\r\nclass X {}"), "exact frozen source reconstruction");
    refuses(() -> sender.writeFrozenUtf8(M3FileId.overlay(7, 0), java.io.OutputStream.nullOutputStream()));
    refuses(() -> sender.writeFrozenUtf8(M3FileId.base(7, 200), java.io.OutputStream.nullOutputStream()));
    refuses(() -> sender.writeFrozenUtf8(M3FileId.base(23, 0), java.io.OutputStream.nullOutputStream()));
    refuses(() -> new M3FmtFrozenDictionary(
        dictionary, List.of(new M3FileDictionary.Language(
            7, "en", "frozen-v1", List.of("orange", " ", "apple", "λ", "class X {}")), french)));
    refuses(() -> new M3FmtFrozenDictionary(dictionary, List.of(english)));
    var changedVersion = new M3FileDictionary.Language(
        7, "en", "frozen-v3", english.words());
    var incompatible = new M3FmtFrozenDictionary(dictionary, List.of(changedVersion, french));
    ok(!incompatible.catalogRoot().equals(sender.catalogRoot()), "version changes identity");
    refuses(() -> M3FmtLexiconFrame.read(M3FmtLexiconFrame.write(frame), incompatible));
    System.out.println("M3FMT_FROZEN_DICTIONARY_PASS checks=" + checks);
  }
}
