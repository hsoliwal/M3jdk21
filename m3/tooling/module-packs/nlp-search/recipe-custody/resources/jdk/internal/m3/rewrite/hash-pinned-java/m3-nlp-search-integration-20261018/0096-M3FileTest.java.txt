// Copyright 2026 Hitesh Soliwal and contributors
// Mapped pinned donor proof; see DONOR_PROBES.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import static com.m3.file.M3FileProbeAssertions.assertArrayEquals;
import static com.m3.file.M3FileProbeAssertions.assertEquals;
import static com.m3.file.M3FileProbeAssertions.assertFalse;
import static com.m3.file.M3FileProbeAssertions.assertThrows;
import static com.m3.file.M3FileProbeAssertions.assertTrue;

import com.m3.file.M3FileDictionary;
import com.m3.file.M3FileText;
import java.util.ArrayList;
import java.util.List;

class M3FileTest {
  void losslessFileUsesLocalLexiconPostingsAndOneByteHandles() {
    M3FileDictionary resolver = new M3FileDictionary();
    String source = "alpha beta alpha\r\nbeta alpha\n";
    M3File file = M3FileTokenizer.plainText(resolver, 0, source);

    assertEquals(source, file.materialize());
    assertEquals(10, file.tokenCount());
    assertEquals(5, file.uniqueTokenCount());
    assertEquals(1, file.tokenWidthBytes());
    assertEquals(3, file.lineCount());
    assertEquals(0, file.lineStartUtf16(0));
    assertEquals(18, file.lineStartUtf16(1));
    assertEquals(source.length(), file.lineStartUtf16(2));

    M3FileText alpha = M3FileText.intern(resolver, 0, "alpha");
    assertEquals(3, file.occurrenceCount(alpha));
    assertEquals(0, file.firstOccurrence(alpha));
    assertEquals(4, file.firstOccurrence(alpha, 1));
    assertEquals(8, file.lastOccurrence(alpha));
    assertEquals(4, file.lastOccurrence(alpha, 8));

    List<Integer> positions = new ArrayList<>();
    file.forEachOccurrence(alpha, positions::add);
    assertEquals(List.of(0, 4, 8), positions);
  }

  void preparedKmpRetainsOverlappingMatchesWithoutMaterializingStrings() {
    M3FileDictionary resolver = new M3FileDictionary();
    M3File file = M3FileTokenizer.plainText(resolver, 0, "a a a a");
    M3FileText a = M3FileText.intern(resolver, 0, "a");
    M3FileText space = M3FileText.intern(resolver, 0, " ");

    M3FilePattern pattern = file.preparePattern(a, space, a);
    assertTrue(pattern.presentInFile());
    assertEquals(0, file.indexOf(pattern));
    assertArrayEquals(new int[] {0, 2, 4}, file.findAll(pattern, 10));

    M3FileText absent = M3FileText.intern(resolver, 0, "absent");
    M3FilePattern miss = file.preparePattern(absent);
    assertFalse(miss.presentInFile());
    assertEquals(-1, file.indexOf(miss));
  }

  void rollingIndexConfirmsCandidatesExactly() {
    M3FileDictionary resolver = new M3FileDictionary();
    M3File file = M3FileTokenizer.plainText(resolver, 0, "x y z x y z q");
    M3FileRollingIndex rolling = file.rollingIndex();

    assertTrue(rolling.sameTokens(0, 6, 5));
    assertFalse(rolling.sameTokens(0, 2, 3));
    assertEquals(6, rolling.indexOf(file.span(0, 5), 1));
  }

  void ahoCorasickFindsSeveralTokenPatternsInOnePass() {
    M3FileDictionary resolver = new M3FileDictionary();
    M3File file =
        M3FileTokenizer.plainText(resolver, 0, "alpha beta alpha beta");
    M3FileText alpha = M3FileText.intern(resolver, 0, "alpha");
    M3FileText beta = M3FileText.intern(resolver, 0, "beta");
    M3FileText space = M3FileText.intern(resolver, 0, " ");

    M3FilePattern one = file.preparePattern(alpha);
    M3FilePattern two = file.preparePattern(alpha, space, beta);
    M3FilePattern empty = file.preparePattern();
    M3FileMultiPattern multi = file.prepareMultiPattern(one, two, empty);

    List<String> matches = new ArrayList<>();
    multi.scan(
        (pattern, start, end) ->
            matches.add(pattern + ":" + start + ":" + end));

    assertTrue(matches.contains("0:0:1"));
    assertTrue(matches.contains("0:4:5"));
    assertTrue(matches.contains("1:0:3"));
    assertTrue(matches.contains("1:4:7"));
    assertTrue(matches.contains("2:0:0"));
    assertTrue(matches.contains("2:7:7"));
  }

  void codeLikeTokenizerIsLosslessAcrossStringsCommentsAndCrLf() {
    M3FileDictionary resolver = new M3FileDictionary();
    String source =
        "String s = \"// not comment\"; // comment\r\n/*x\ny*/ int n=1;";
    M3File file = M3FileTokenizer.codeLike(resolver, 0, source);

    assertEquals(source, file.materialize());
    assertEquals(3, file.lineCount());
    assertTrue(file.retainedPayloadBytes() > 0L);
  }

  void widthSelectionIsUnsignedAndDeterministic() {
    M3FileTokenStore bytes =
        M3FileTokenStore.freeze(new int[] {0, 255}, 2, 256);
    M3FileTokenStore shorts =
        M3FileTokenStore.freeze(new int[] {0, 256}, 2, 257);
    M3FileTokenStore ints =
        M3FileTokenStore.freeze(new int[] {0, 65_536}, 2, 65_537);

    assertEquals(1, bytes.widthBytes());
    assertEquals(255, bytes.get(1));
    assertEquals(2, shorts.widthBytes());
    assertEquals(256, shorts.get(1));
    assertEquals(4, ints.widthBytes());
    assertEquals(65_536, ints.get(1));
  }

  void fileLexiconFreezesAndPreparedPatternsAreFileScoped() {
    M3FileDictionary resolver = new M3FileDictionary();
    M3File first = M3FileTokenizer.plainText(resolver, 0, "a b");
    M3File second = M3FileTokenizer.plainText(resolver, 0, "a b");
    M3FileText a = M3FileText.intern(resolver, 0, "a");

    assertTrue(first.lexicon().isFrozen());
    assertThrows(
        IllegalStateException.class, () -> first.lexicon().intern(a));
    M3FilePattern pattern = first.preparePattern(a);
    assertThrows(
        IllegalArgumentException.class, () -> second.indexOf(pattern));
  }

  void splitCrLfAcrossManualTokensStillBuildsOneLineBreak() {
    M3FileDictionary resolver = new M3FileDictionary();
    M3FileBuilder builder = new M3FileBuilder(resolver);
    builder.add(
        M3FileText.intern(resolver, 0, "left\r"),
        M3FileTokenKind.WORD);
    builder.add(
        M3FileText.intern(resolver, 0, "\nright"),
        M3FileTokenKind.WORD);
    M3File file = builder.build();

    assertEquals("left\r\nright", file.materialize());
    assertEquals(2, file.lineCount());
    assertEquals(6, file.lineStartUtf16(1));
  }

  public static void main(String[] args) throws Exception {
    M3FileTest proof = new M3FileTest();
    proof.losslessFileUsesLocalLexiconPostingsAndOneByteHandles();
    proof.preparedKmpRetainsOverlappingMatchesWithoutMaterializingStrings();
    proof.rollingIndexConfirmsCandidatesExactly();
    proof.ahoCorasickFindsSeveralTokenPatternsInOnePass();
    proof.codeLikeTokenizerIsLosslessAcrossStringsCommentsAndCrLf();
    proof.widthSelectionIsUnsignedAndDeterministic();
    proof.fileLexiconFreezesAndPreparedPatternsAreFileScoped();
    proof.splitCrLfAcrossManualTokensStillBuildsOneLineBreak();
    System.out.println("M3FileTest_PASS|donorMethods=8");
  }
}
