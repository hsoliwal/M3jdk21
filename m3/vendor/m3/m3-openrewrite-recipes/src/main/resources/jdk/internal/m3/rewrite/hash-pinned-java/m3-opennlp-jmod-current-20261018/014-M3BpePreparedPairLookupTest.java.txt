/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package opennlp.tools.tokenize;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import opennlp.tools.tokenize.BPETokenizer.SymbolPair;
import opennlp.tools.util.Span;

/**
 * Oracle parity for the M3 recipe-produced prepared BPE pair lookup.
 * Baseline is the original greedy merge loop, retained here as a test oracle.
 */
class M3BpePreparedPairLookupTest {

  private static BPEModel model(List<SymbolPair> merges) {
    return new BPEModel(merges, new HashMap<>(), new BPETokenizerFactory("en"));
  }

  /**
   * Original behavior: last duplicate pair rank wins, choose lowest rank and
   * first occurrence, merge all non-overlapping occurrences of that pair.
   */
  private static String[] oracle(String word, List<SymbolPair> merges) {
    final Map<SymbolPair, Integer> ranks = new LinkedHashMap<>();
    for (int i = 0; i < merges.size(); i++) {
      ranks.put(merges.get(i), i);
    }

    List<String> current = new ArrayList<>(word.length());
    for (int i = 0; i < word.length(); i++) {
      current.add(i == word.length() - 1
          ? word.charAt(i) + BPETokenizer.END_OF_WORD
          : String.valueOf(word.charAt(i)));
    }

    while (current.size() > 1) {
      int bestRank = Integer.MAX_VALUE;
      SymbolPair bestPair = null;
      for (int i = 0; i < current.size() - 1; i++) {
        final SymbolPair pair = new SymbolPair(current.get(i), current.get(i + 1));
        final Integer rank = ranks.get(pair);
        if (rank != null && rank < bestRank) {
          bestRank = rank;
          bestPair = pair;
        }
      }
      if (bestPair == null) {
        break;
      }

      final List<String> next = new ArrayList<>();
      for (int i = 0; i < current.size();) {
        if (i < current.size() - 1
            && current.get(i).equals(bestPair.left())
            && current.get(i + 1).equals(bestPair.right())) {
          next.add(bestPair.left() + bestPair.right());
          i += 2;
        } else {
          next.add(current.get(i++));
        }
      }
      current = next;
    }

    for (int i = 0; i < current.size(); i++) {
      String part = current.get(i);
      if (part.endsWith(BPETokenizer.END_OF_WORD)) {
        current.set(i, part.substring(0, part.length() - BPETokenizer.END_OF_WORD.length()));
      }
    }
    return current.toArray(new String[0]);
  }

  @Test
  void preservesDuplicateRanksAndOverlappingPairMerges() {
    final List<SymbolPair> duplicates = List.of(
        new SymbolPair("a", "b"),
        new SymbolPair("b", "a"),
        new SymbolPair("a", "b"));
    final BPETokenizer tokenizer = new BPETokenizer(model(duplicates));
    // Keep both competing pairs before the end-of-word symbol so duplicate ranks matter.
    Assertions.assertArrayEquals(oracle("abab", duplicates), tokenizer.tokenize("abab"));
    Assertions.assertArrayEquals(new String[] {"a", "ba", "b"}, tokenizer.tokenize("abab"));

    final List<SymbolPair> overlapping = List.of(
        new SymbolPair("a", "a"),
        new SymbolPair("aa", "aa" + BPETokenizer.END_OF_WORD));
    final BPETokenizer overlappingTokenizer = new BPETokenizer(model(overlapping));
    Assertions.assertArrayEquals(oracle("aaaa", overlapping),
        overlappingTokenizer.tokenize("aaaa"));
  }

  @Test
  void matchesReferenceAcrossSeededMergeRankAndUtf16Cases() {
    final Random random = new Random(20261014L);
    final String[] units = {"a", "b", "c", "a", "é", "\uD83D", "\uDE80"};
    final String[] fragments = {"a", "b", "c", "aa", "ab", "ba", "bb", "é",
        "\uD83D", "\uDE80", "a" + BPETokenizer.END_OF_WORD};

    for (int modelIndex = 0; modelIndex < 120; modelIndex++) {
      final int expectedModelIndex = modelIndex;
      final List<SymbolPair> rules = new ArrayList<>();
      for (int i = 0, count = random.nextInt(24); i < count; i++) {
        if (!rules.isEmpty() && random.nextInt(4) == 0) {
          rules.add(rules.get(random.nextInt(rules.size())));
        } else {
          rules.add(new SymbolPair(fragments[random.nextInt(fragments.length)],
              fragments[random.nextInt(fragments.length)]));
        }
      }
      final BPETokenizer tokenizer = new BPETokenizer(model(rules));
      for (int n = 0; n < 25; n++) {
        final StringBuilder word = new StringBuilder();
        for (int i = 0, count = 1 + random.nextInt(12); i < count; i++) {
          word.append(units[random.nextInt(units.length)]);
        }
        final String input = word.toString();
        Assertions.assertArrayEquals(oracle(input, rules), tokenizer.tokenize(input),
            () -> "Model " + expectedModelIndex + ", word " + input);
      }
    }
  }

  @Test
  void preservesUtf16OffsetsForSubwordSpans() {
    final List<SymbolPair> rules = List.of(
        new SymbolPair("a", "b"),
        new SymbolPair("a", "a"));
    final BPETokenizer tokenizer = new BPETokenizer(model(rules));
    final String text = "aba aa";
    final Span[] spans = tokenizer.tokenizePos(text);
    final String[] first = oracle("aba", rules);
    final String[] second = oracle("aa", rules);
    Assertions.assertEquals(first.length + second.length, spans.length);
    int index = 0;
    int at = 0;
    for (String token : first) {
      Assertions.assertEquals(at, spans[index].getStart());
      at += token.length();
      Assertions.assertEquals(at, spans[index++].getEnd());
    }
    at = 4;
    for (String token : second) {
      Assertions.assertEquals(at, spans[index].getStart());
      at += token.length();
      Assertions.assertEquals(at, spans[index++].getEnd());
    }
  }
}
