/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: Apache-2.0
 * Analyzer observation design adapted from hsoliwal/lucene TestM3TextReaders.java;
 * the Apache Software Foundation source and exact revision are recorded in PROVENANCE.json.
 */
package org.apache.lucene.analysis;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionLengthAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

/** Differential tool contracts using actual complete Lucene classes and a hostile text fixture. */
public final class M3LuceneContractProbe {
  private static long checks;
  private static long characters;

  private M3LuceneContractProbe() {}

  private static void require(boolean condition, String contract) {
    checks++;
    if (!condition) throw new AssertionError(contract + " at assertion " + checks);
  }

  /** Test-only split storage; production continues borrowing its caller's actual M3 owner. */
  private record Borrowed(String left, String right) implements CharSequence {
    Borrowed(String text, int split) {
      this(text.substring(0, split), text.substring(split));
    }

    @Override public int length() {
      return Math.addExact(left.length(), right.length());
    }

    @Override public char charAt(int offset) {
      Objects.checkIndex(offset, length());
      characters++;
      return offset < left.length() ? left.charAt(offset) : right.charAt(offset - left.length());
    }

    @Override public CharSequence subSequence(int start, int end) {
      throw new AssertionError("caller text must not be sliced");
    }

    @Override public String toString() {
      throw new AssertionError("caller text must not be flattened");
    }

    @Override public int hashCode() {
      throw new AssertionError("generic CharSequence hashCode is not the String hash contract");
    }
  }

  private record Token(String term, int start, int end, int increment, int length, String type) {}
  private record Result(List<Token> tokens, int endOffset, int endIncrement) {}

  private static Result consume(TokenStream stream) throws IOException {
    try (stream) {
      var term = stream.addAttribute(CharTermAttribute.class);
      var offset = stream.addAttribute(OffsetAttribute.class);
      var increment = stream.addAttribute(PositionIncrementAttribute.class);
      var length = stream.addAttribute(PositionLengthAttribute.class);
      var type = stream.addAttribute(TypeAttribute.class);
      List<Token> tokens = new ArrayList<>();
      stream.reset();
      while (stream.incrementToken()) {
        tokens.add(new Token(term.toString(), offset.startOffset(), offset.endOffset(),
            increment.getPositionIncrement(), length.getPositionLength(), type.type()));
      }
      stream.end();
      return new Result(List.copyOf(tokens), offset.endOffset(), increment.getPositionIncrement());
    }
  }

  private static void compare(Analyzer analyzer, String text, int split) throws IOException {
    Result expected = consume(analyzer.tokenStream("body", text));
    try (Reader reader = M3TextReaders.reader(new Borrowed(text, split))) {
      require(expected.equals(consume(analyzer.tokenStream("body", reader))),
          "tokens/positions/offsets/types/final offsets " + analyzer.getClass().getName());
    }
    require(new BytesRef(text).equals(new BytesRef(new Borrowed(text, split))),
        "Lucene UTF-8 term representation");
  }

  private static List<String> corpus() {
    List<String> result = new ArrayList<>(List.of("", "a", "alpha beta", "Aa", "BB",
        "\u0000", "\u0000\u0000", "A\ud83d\ude00B", "\ud800", "\udc00", "x\ud800y\udc00",
        "İ ı Σ ς σ K ß", "\ud801\udc00 \ud801\udc28", "e\u0301 café 日本語", "x\u0000 y\r\n\t"));
    Random random = new Random(0x4d334c5543454e45L);
    for (int trial = 0; trial < 1600; trial++) {
      char[] units = new char[trial % 131];
      for (int i = 0; i < units.length; i++) units[i] = (char) random.nextInt(65536);
      result.add(new String(units));
    }
    return result;
  }

  private static void analyzers(List<String> words) throws IOException {
    Random random = new Random(0x4d3353504c4954L);
    for (Analyzer analyzer : List.of(new StandardAnalyzer(), new WhitespaceAnalyzer(),
        new KeywordAnalyzer())) {
      try (analyzer) {
        for (int i = 0; i < words.size(); i++) {
          String text = words.get(i);
          if (i < 16) {
            for (int split = 0; split <= text.length(); split++) compare(analyzer, text, split);
          } else {
            compare(analyzer, text, random.nextInt(text.length() + 1));
          }
        }
        for (String text : List.of("α x\ud83d\ude00 β ".repeat(2048), "x".repeat(8193))) {
          for (int split : new int[] {0, 1, text.length() / 2, text.length() - 1, text.length()}) {
            compare(analyzer, text, split);
          }
        }
      }
    }
  }

  /** Independent lookup oracle: Unicode simple lowercase is applied by code point. */
  private static String folded(String value, boolean ignoreCase) {
    if (!ignoreCase) return value;
    StringBuilder result = new StringBuilder();
    value.codePoints().map(Character::toLowerCase).forEach(result::appendCodePoint);
    return result.toString();
  }

  private static void maps(List<String> words) {
    for (boolean ignoreCase : new boolean[] {false, true}) {
      CharArrayMap<Integer> actual = new CharArrayMap<>(1, ignoreCase);
      CharArraySet members = new CharArraySet(1, ignoreCase);
      Map<String, Integer> expected = new HashMap<>();
      for (int i = 0; i < words.size(); i++) {
        String word = words.get(i);
        require(Objects.equals(expected.put(folded(word, ignoreCase), i), actual.put(word, i)),
            "put return value and collision replacement");
        members.add(word);
      }
      require(actual.size() == expected.size(), "rehash/deduplication size");
      require(members.size() == expected.size(), "set size");
      for (String word : words) {
        for (String query : List.of(word, word.toUpperCase(Locale.ROOT),
            word.toLowerCase(Locale.ROOT), word + "_missing")) {
          Integer value = expected.get(folded(query, ignoreCase));
          require(Objects.equals(value, actual.get(query)), "String cached-hash lookup");
          require(Objects.equals(value, actual.get(new Borrowed(query, query.length() / 2))),
              "generic UTF-16 character hash lookup");
          require(Objects.equals(value, actual.get(query.toCharArray(), 0, query.length())),
              "char[] lookup parity");
          require(actual.containsKey(new Borrowed(query, 0)) == (value != null),
              "generic map membership");
          require(members.contains(new Borrowed(query, query.length())) == (value != null),
              "generic set membership");
        }
      }
      try {
        actual.get((CharSequence) null);
        throw new AssertionError("null CharSequence lookup accepted");
      } catch (NullPointerException expectedNull) {
        checks++;
      }
      // put(CharSequence) still uses its explicit toString contract, unlike lookup(CharSequence).
      CharSequence differentlyRendered = new CharSequence() {
        @Override public int length() { return 3; }
        @Override public char charAt(int index) { return "raw".charAt(index); }
        @Override public CharSequence subSequence(int start, int end) {
          return "raw".subSequence(start, end);
        }
        @Override public String toString() { return "stored-rendering"; }
      };
      actual.put(differentlyRendered, -17);
      require(actual.get("stored-rendering") == -17, "put preserves explicit rendering boundary");
      require(actual.get(differentlyRendered) == null, "lookup uses logical characters");
    }
  }

  private static void compareVectors(Terms left, Terms right) throws IOException {
    require((left == null) == (right == null), "empty vectors");
    if (left == null) return;
    TermsEnum a = left.iterator();
    TermsEnum b = right.iterator();
    while (true) {
      BytesRef term = a.next();
      BytesRef other = b.next();
      require(Objects.equals(term, other), "exact UTF-8 term bytes and order");
      if (term == null) break;
      PostingsEnum ap = a.postings(null, PostingsEnum.ALL);
      PostingsEnum bp = b.postings(null, PostingsEnum.ALL);
      require(ap.nextDoc() == bp.nextDoc(), "term-vector document identity");
      require(ap.freq() == bp.freq(), "term frequency");
      for (int i = 0; i < ap.freq(); i++) {
        require(ap.nextPosition() == bp.nextPosition(), "term positions");
        require(ap.startOffset() == bp.startOffset(), "UTF-16 start offset");
        require(ap.endOffset() == bp.endOffset(), "UTF-16 end offset");
      }
      require(ap.nextDoc() == bp.nextDoc(), "term-vector exhaustion");
    }
  }

  private static void diskIndex(List<String> words) throws Exception {
    Path disk = Files.createTempDirectory("m3-lucene-reader-contract-");
    FieldType type = new FieldType(TextField.TYPE_NOT_STORED);
    type.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
    type.setStoreTermVectors(true);
    type.setStoreTermVectorPositions(true);
    type.setStoreTermVectorOffsets(true);
    type.freeze();
    try (Analyzer analyzer = new StandardAnalyzer(); FSDirectory directory = FSDirectory.open(disk)) {
      try (IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer))) {
        for (int id = 0; id < 128; id++) {
          String text = words.get(id) + " alpha beta gamma alpha";
          try (Reader borrowed = M3TextReaders.reader(new Borrowed(text, id % (text.length() + 1)))) {
            Document doc = new Document();
            doc.add(new StringField("id", Integer.toString(id), Field.Store.YES));
            doc.add(new Field("plain", text, type));
            doc.add(new Field("borrowed", borrowed, type));
            writer.addDocument(doc);
          }
        }
        writer.commit();
      }
      try (DirectoryReader reader = DirectoryReader.open(directory)) {
        require(reader.numDocs() == 128, "committed document count");
        for (int doc = 0; doc < reader.maxDoc(); doc++) {
          compareVectors(reader.termVectors().get(doc, "plain"),
              reader.termVectors().get(doc, "borrowed"));
        }
        IndexSearcher searcher = new IndexSearcher(reader);
        QueryParser plain = new QueryParser("plain", analyzer);
        QueryParser borrowed = new QueryParser("borrowed", analyzer);
        for (String query : List.of("alpha", "beta AND gamma", "\"alpha beta\"", "al*",
            "alpha~1", "[alpha TO gamma]", "\"gamma alpha\"", "日本語")) {
          TopDocs expected = searcher.search(plain.parse(query), 256);
          TopDocs actual = searcher.search(borrowed.parse(query), 256);
          require(expected.totalHits.equals(actual.totalHits), "query hit totals");
          require(expected.scoreDocs.length == actual.scoreDocs.length, "query hit array");
          for (int i = 0; i < expected.scoreDocs.length; i++) {
            require(expected.scoreDocs[i].doc == actual.scoreDocs[i].doc, "query document ordering");
            require(Float.floatToIntBits(expected.scoreDocs[i].score)
                == Float.floatToIntBits(actual.scoreDocs[i].score), "query score parity");
          }
        }
      }
    } finally {
      try (var paths = Files.walk(disk)) {
        for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
      }
    }
  }

  public static void main(String[] args) throws Exception {
    List<String> words = corpus();
    analyzers(words);
    maps(words);
    diskIndex(words);
    System.out.println("LUCENE_M3_CONTRACT_PASS assertions=" + checks + " charAt=" + characters
        + " analyzers=3 utf16_cases=" + words.size() + " disk_documents=128 flatten_calls=0");
  }
}
