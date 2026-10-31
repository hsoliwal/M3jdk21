// SPDX-License-Identifier: Apache-2.0
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArrayMap;
import org.apache.lucene.analysis.M3TextReaders;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.TokenizerFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.codecs.Codec;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.MMapDirectory;

/** Real disk index, source-built providers and nonflattening immutable UTF-16 ingress. */
public final class M3LuceneJmodCheck {
  private record Token(String term, int start, int end, int increment) {}
  private record Tokens(List<Token> tokens, int finalOffset) {}

  public static void main(String[] args) throws Exception {
    Path index = Path.of(args[0]);
    Files.createDirectory(index);
    String text = "Alpha naïve 東京 😀 beta";
    CharSequence borrowed = segmented(text);
    try (Analyzer analyzer = new StandardAnalyzer()) {
      check(tokens(analyzer, text).equals(tokens(analyzer, borrowed)), "borrowed UTF-16 token contract");
      for (String value : List.of("", "a\uD83D\uDE00b", "x\uD800y", "x\uDC00y", "Straße", "a\u0301")) {
        check(tokens(analyzer, value).equals(tokens(analyzer, segmented(value))),
            "Unicode token/offset parity");
      }
      try (Reader reader = M3TextReaders.reader(borrowed)) {
        char[] units = new char[text.length()];
        check(reader.read(units) == units.length, "borrowed reader length");
        check(java.util.Arrays.equals(units, text.toCharArray()), "borrowed code units");
        reader.close();
        check(reader.read() == -1, "reader close contract");
      }
      try (MMapDirectory directory = new MMapDirectory(index)) {
        try (IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer))) {
          writer.addDocument(document("one", borrowed));
          writer.addDocument(document("two", segmented("gamma quiet")));
          writer.commit();
        }
        try (DirectoryReader reader = DirectoryReader.open(directory)) {
          IndexSearcher searcher = new IndexSearcher(reader);
          var hits = searcher.search(new TermQuery(new Term("body", "naïve")), 10);
          check(hits.totalHits.value() == 1, "Unicode disk search");
          check("one".equals(searcher.storedFields().document(hits.scoreDocs[0].doc).get("id")),
              "stored field persisted");
          check(searcher.count(new QueryParser("body", analyzer).parse("alpha OR gamma")) == 2,
              "current queryparser source implementation");
        }
        try (IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer))) {
          writer.updateDocument(new Term("id", "one"), document("one", segmented("replacement")));
          writer.deleteDocuments(new Term("id", "two"));
          writer.commit();
        }
        try (DirectoryReader reader = DirectoryReader.open(directory)) {
          IndexSearcher searcher = new IndexSearcher(reader);
          check(reader.numDocs() == 1, "update/delete/commit contract");
          check(searcher.count(new TermQuery(new Term("body", "replacement"))) == 1,
              "replacement persisted");
          check(searcher.count(new TermQuery(new Term("body", "naïve"))) == 0,
              "old terms deleted");
        }
        String file = java.util.Arrays.stream(directory.listAll())
            .filter(name -> !name.equals("write.lock"))
            .filter(name -> {
              try { return directory.fileLength(name) > 1; }
              catch (java.io.IOException error) { throw new RuntimeException(error); }
            }).findFirst().orElseThrow();
        IndexInput original = directory.openInput(file, IOContext.DEFAULT);
        IndexInput clone = original.clone();
        check(original.readByte() == clone.readByte(), "independent cloned input cursor");
        original.close();
        try {
          clone.readByte();
          throw new AssertionError("clone survived original close");
        } catch (AlreadyClosedException expected) {
          // Existing Lucene ownership contract: closing the original invalidates its clones.
        }
      }
    }
    CharArrayMap<Boolean> map = new CharArrayMap<>(4, false);
    map.put("key", Boolean.TRUE);
    StringBuilder mutable = new StringBuilder("key");
    check(Boolean.TRUE.equals(map.get(mutable)), "mutable lookup before mutation");
    mutable.setCharAt(0, 'X');
    check(map.get(mutable) == null, "no stale cache on mutable CharSequence");
    check(Codec.availableCodecs().contains("SimpleText"), "source-built codecs service");
    for (String name : List.of("standard", "whitespace", "japanese", "korean", "openNlp", "icu")) {
      check(TokenizerFactory.availableTokenizers().contains(name), "missing tokenizer service: " + name);
    }
    // Dictionary resources are read through actual named-module providers.
    for (String name : List.of("japanese", "korean")) {
      try (var tokenizer = TokenizerFactory.forName(name, new HashMap<>()).create()) {
        tokenizer.setReader(new java.io.StringReader(name.equals("japanese") ? "東京都" : "대한민국"));
        tokenizer.reset();
        check(tokenizer.incrementToken(), "dictionary resource unavailable: " + name);
        while (tokenizer.incrementToken()) {}
        tokenizer.end();
      }
    }
    try (var tokenizer = new org.apache.lucene.analysis.icu.segmentation.ICUTokenizer()) {
      tokenizer.setReader(new java.io.StringReader("ภาษาไทย 東京 café"));
      tokenizer.reset();
      check(tokenizer.incrementToken(), "ICU rule and Unicode data resources");
      while (tokenizer.incrementToken()) {}
      tokenizer.end();
    }
    try (Analyzer polish = new org.apache.lucene.analysis.morfologik.MorfologikAnalyzer()) {
      check(!tokens(polish, "rowery").tokens().isEmpty(), "Polish dictionary resources");
    }
    var ukrainian = ClassLoader.getSystemResource("ua/net/nlp/ukrainian.dict");
    check(ukrainian != null, "resource-only Ukrainian named module");
    try (Analyzer analyzer = new org.apache.lucene.analysis.morfologik.MorfologikAnalyzer(
        morfologik.stemming.Dictionary.read(ukrainian))) {
      check(!tokens(analyzer, "Україна").tokens().isEmpty(), "Ukrainian dictionary and metadata resources");
    }
    System.out.println("Source-built Lucene JMOD disk search, mutation, Unicode and services passed");
  }

  private static Document document(String id, CharSequence body) {
    Document document = new Document();
    document.add(new StringField("id", id, Field.Store.YES));
    document.add(new StoredField("source", "current-fork"));
    document.add(new TextField("body", M3TextReaders.reader(body)));
    return document;
  }

  private static Tokens tokens(Analyzer analyzer, CharSequence input) throws Exception {
    try (TokenStream stream = input instanceof String text
        ? analyzer.tokenStream("body", text)
        : analyzer.tokenStream("body", M3TextReaders.reader(input))) {
      var term = stream.addAttribute(CharTermAttribute.class);
      var offset = stream.addAttribute(OffsetAttribute.class);
      var increment = stream.addAttribute(PositionIncrementAttribute.class);
      List<Token> result = new ArrayList<>();
      stream.reset();
      while (stream.incrementToken()) {
        result.add(new Token(term.toString(), offset.startOffset(), offset.endOffset(),
            increment.getPositionIncrement()));
      }
      stream.end();
      return new Tokens(List.copyOf(result), offset.endOffset());
    }
  }

  private static CharSequence segmented(String text) {
    // Every UTF-16 unit is a separate immutable part, including split surrogate pairs.
    char[][] parts = new char[text.length()][];
    for (int i = 0; i < text.length(); i++) parts[i] = new char[] {text.charAt(i)};
    return new CharSequence() {
      public int length() { return parts.length; }
      public char charAt(int index) { return parts[index][0]; }
      public CharSequence subSequence(int start, int end) {
        throw new AssertionError("borrowed text was sliced through a flattening API");
      }
      public String toString() { throw new AssertionError("borrowed text was flattened"); }
    };
  }

  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }
}

