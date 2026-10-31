// Copyright 2026 Hitesh Soliwal and contributors
// Mapped pinned donor proof; see DONOR_PROBES.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import static com.m3.file.M3FileProbeAssertions.assertArrayEquals;
import static com.m3.file.M3FileProbeAssertions.assertEquals;
import static com.m3.file.M3FileProbeAssertions.assertTrue;

import com.m3.file.M3FileDictionary;
import com.m3.file.M3FileText;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

class M3FileAlgorithmTest {
  void directCharSequenceAdmissionReusesCanonicalScalarIds() {
    M3FileDictionary resolver = new M3FileDictionary();
    M3FileBuilder builder = new M3FileBuilder(resolver);
    builder.add(7, new Slice("alpha!", 0, 5), M3FileTokenKind.WORD);
    builder.add(7, new Slice("alpha?", 0, 5), M3FileTokenKind.WORD);
    builder.add(7, "beta", M3FileTokenKind.WORD);
    M3File file = builder.build();

    assertEquals(2, file.uniqueTokenCount());
    int alpha = file.localHandle(7, "alpha");
    assertTrue(alpha >= 0);
    assertEquals(2, file.occurrenceCount(alpha));
    assertEquals(0, file.firstOccurrence(alpha));
    assertEquals(1, file.lastOccurrence(alpha));
    assertEquals(
        M3FileText.intern(resolver, 7, "alpha").tokenIdAt(0),
        file.resolverTokenIdAt(0, 0));
  }

  void frequencyIndexRanksDeterministically() {
    M3FileDictionary resolver = new M3FileDictionary();
    M3FileBuilder builder = new M3FileBuilder(resolver);
    for (String value : List.of("b", "a", "c", "a", "b", "a", "d", "b")) {
      builder.add(0, value, M3FileTokenKind.WORD);
    }
    M3File file = builder.build();
    M3FileFrequencyIndex rank = file.frequencyIndex();

    int a = file.localHandle(0, "a");
    int b = file.localHandle(0, "b");
    int c = file.localHandle(0, "c");
    int d = file.localHandle(0, "d");
    assertArrayEquals(new int[] {b, a}, rank.topLocalHandles(2));
    assertEquals(3, rank.frequencyAtRank(0));
    assertEquals(3, rank.frequencyAtRank(1));
    assertTrue(rank.rankOfLocalHandle(c) < rank.rankOfLocalHandle(d));
    assertEquals(2, rank.countAtLeast(2));
    assertEquals(4, rank.countAtLeast(1));
  }

  void kindIndexAndSemanticProjectionPreserveSourceCoordinates() {
    M3FileDictionary resolver = new M3FileDictionary();
    M3File file =
        M3FileTokenizer.codeLike(
            resolver, 0, "alpha /* note */ + beta\n//tail\n42");
    M3FileKindIndex kinds = file.kindIndex();

    assertEquals(2, kinds.count(M3FileTokenKind.COMMENT));
    assertTrue(kinds.count(M3FileTokenKind.WORD) >= 2);

    M3FileProjection semantic = file.semanticProjection();
    M3FileProjection trivia = file.triviaProjection();
    assertEquals(file.tokenCount(), semantic.tokenCount() + trivia.tokenCount());
    for (int ordinal = 0; ordinal < semantic.tokenCount(); ordinal++) {
      M3FileTokenKind kind = semantic.tokenKindAt(ordinal);
      assertTrue(
          kind != M3FileTokenKind.WHITESPACE
              && kind != M3FileTokenKind.NEWLINE
              && kind != M3FileTokenKind.COMMENT);
      int sourceToken = semantic.sourceTokenIndexAt(ordinal);
      assertEquals(ordinal, semantic.sourceOrdinalOf(sourceToken));
      assertEquals(file.localHandleAt(sourceToken), semantic.localHandleAt(ordinal));
    }
  }

  void utf16CoordinatesResolveNearestTokenBoundaries() {
    M3FileDictionary resolver = new M3FileDictionary();
    M3File file = M3FileTokenizer.plainText(resolver, 0, "alpha beta");

    assertEquals(0, file.tokenBoundaryAtOrBeforeUtf16(0));
    assertEquals(0, file.tokenBoundaryAtOrBeforeUtf16(3));
    assertEquals(1, file.tokenBoundaryAtOrAfterUtf16(3));
    assertEquals(file.tokenCount(), file.tokenBoundaryAtOrBeforeUtf16(file.utf16Length()));
    assertEquals(file.tokenCount(), file.tokenBoundaryAtOrAfterUtf16(file.utf16Length()));
  }

  void randomizedFrequencyAndPostingsMatchNaiveOracle() {
    M3FileDictionary resolver = new M3FileDictionary();
    M3FileBuilder builder = new M3FileBuilder(resolver);
    SplittableRandom random = new SplittableRandom(0x4d494e4445584649L);
    int vocabulary = 32;
    int tokens = 4096;
    int[] expected = new int[vocabulary];

    for (int i = 0; i < tokens; i++) {
      int word = random.nextInt(vocabulary);
      expected[word]++;
      builder.add(0, "w" + word, M3FileTokenKind.WORD);
    }
    M3File file = builder.build();

    for (int word = 0; word < vocabulary; word++) {
      int handle = file.localHandle(0, "w" + word);
      assertTrue(handle >= 0);
      assertEquals(expected[word], file.occurrenceCount(handle));
      List<Integer> actual = new ArrayList<>();
      file.forEachOccurrence(handle, actual::add);
      assertEquals(expected[word], actual.size());
      for (int position : actual) assertEquals(handle, file.localHandleAt(position));
    }

    M3FileFrequencyIndex rank = file.frequencyIndex();
    for (int ordinal = 1; ordinal < rank.uniqueTokenCount(); ordinal++) {
      int previous = rank.frequencyAtRank(ordinal - 1);
      int current = rank.frequencyAtRank(ordinal);
      assertTrue(previous >= current);
      if (previous == current) {
        assertTrue(rank.localHandleAtRank(ordinal - 1) < rank.localHandleAtRank(ordinal));
      }
    }
  }

  private record Slice(CharSequence source, int start, int end) implements CharSequence {
    @Override
    public int length() { return end - start; }

    @Override
    public char charAt(int index) { return source.charAt(start + index); }

    @Override
    public CharSequence subSequence(int from, int to) {
      return new Slice(source, start + from, start + to);
    }
  }

  public static void main(String[] args) throws Exception {
    M3FileAlgorithmTest proof = new M3FileAlgorithmTest();
    proof.directCharSequenceAdmissionReusesCanonicalScalarIds();
    proof.frequencyIndexRanksDeterministically();
    proof.kindIndexAndSemanticProjectionPreserveSourceCoordinates();
    proof.utf16CoordinatesResolveNearestTokenBoundaries();
    proof.randomizedFrequencyAndPostingsMatchNaiveOracle();
    System.out.println("M3FileAlgorithmTest_PASS|donorMethods=5");
  }
}
