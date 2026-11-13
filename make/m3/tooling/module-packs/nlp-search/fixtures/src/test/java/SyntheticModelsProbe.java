// SPDX-License-Identifier: Apache-2.0
// Temporary qualification fixture. Production source is unchanged fork code.
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import opennlp.tools.sentdetect.SentenceDetectorFactory;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.sentdetect.SentenceSample;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.TokenSample;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.CollectionObjectStream;
import opennlp.tools.util.Span;
import opennlp.tools.util.TrainingParameters;
import org.apache.lucene.analysis.M3TextReaders;
import org.apache.lucene.analysis.TokenFilterFactory;
import org.apache.lucene.analysis.opennlp.OpenNLPTokenizer;
import org.apache.lucene.analysis.opennlp.tools.NLPSentenceDetectorOp;
import org.apache.lucene.analysis.opennlp.tools.NLPTokenizerOp;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.SentenceAttribute;
import org.apache.lucene.util.AttributeFactory;

public final class SyntheticModelsProbe {
  private record Token(String term, int start, int end, int sentence) {}
  private record SplitText(String left, String right) implements CharSequence {
    public int length() { return left.length() + right.length(); }
    public char charAt(int index) {
      Objects.checkIndex(index, length());
      return index < left.length() ? left.charAt(index) : right.charAt(index - left.length());
    }
    public CharSequence subSequence(int start, int end) { throw new AssertionError("flatten/slice"); }
    public String toString() { throw new AssertionError("flatten"); }
  }

  private static TrainingParameters parameters() {
    TrainingParameters p = TrainingParameters.defaultParams();
    p.put(TrainingParameters.ITERATIONS_PARAM, 100);
    p.put(TrainingParameters.CUTOFF_PARAM, 1);
    p.put(TrainingParameters.THREADS_PARAM, 1);
    return p;
  }

  private static List<Token> expected(String text, SentenceModel sentences, TokenizerModel tokens) {
    List<Token> result = new ArrayList<>();
    Span[] outer = new SentenceDetectorME(sentences).sentPosDetect(text);
    TokenizerME tokenizer = new TokenizerME(tokens);
    for (int sentence = 0; sentence < outer.length; sentence++) {
      Span part = outer[sentence];
      for (Span token : tokenizer.tokenizePos(text.substring(part.getStart(), part.getEnd()))) {
        int start = part.getStart() + token.getStart();
        int end = part.getStart() + token.getEnd();
        result.add(new Token(text.substring(start, end), start, end, sentence));
      }
    }
    return List.copyOf(result);
  }

  private static List<Token> actual(Reader source, SentenceModel sentences, TokenizerModel tokens)
      throws Exception {
    try (OpenNLPTokenizer tokenizer = new OpenNLPTokenizer(AttributeFactory.DEFAULT_ATTRIBUTE_FACTORY,
        new NLPSentenceDetectorOp(sentences), new NLPTokenizerOp(tokens))) {
      tokenizer.setReader(source);
      CharTermAttribute term = tokenizer.addAttribute(CharTermAttribute.class);
      OffsetAttribute offset = tokenizer.addAttribute(OffsetAttribute.class);
      SentenceAttribute sentence = tokenizer.addAttribute(SentenceAttribute.class);
      List<Token> result = new ArrayList<>();
      tokenizer.reset();
      while (tokenizer.incrementToken()) {
        result.add(new Token(term.toString(), offset.startOffset(), offset.endOffset(),
            sentence.getSentenceIndex()));
      }
      tokenizer.end();
      return List.copyOf(result);
    }
  }

  public static void main(String[] args) throws Exception {
    String[][] corpus = {
      {"Dr. Ada builds indexes.", "Bob reads texts."},
      {"Ms. Eve tags a file.", "Ada writes code!"},
      {"M3 indexes Java files.", "Search retrieves lines."},
      {"Tokens keep exact offsets.", "Models load again."},
      {"Parse this file.", "Keep this string."}
    };
    List<SentenceSample> sentenceSamples = new ArrayList<>();
    List<TokenSample> tokenSamples = new ArrayList<>();
    for (int repeat = 0; repeat < 24; repeat++) {
      for (String[] parts : corpus) {
        String text = parts[0] + " " + parts[1];
        sentenceSamples.add(new SentenceSample(text,
            new Span(0, parts[0].length()), new Span(parts[0].length() + 1, text.length())));
        tokenSamples.add(new TokenSample(text, SimpleTokenizer.INSTANCE.tokenizePos(text)));
      }
    }
    SentenceModel sentenceModel;
    TokenizerModel tokenizerModel;
    try (var input = new CollectionObjectStream<>(sentenceSamples)) {
      sentenceModel = SentenceDetectorME.train("en", input,
          new SentenceDetectorFactory("en", false, null, new char[] {'.', '!', '?'}), parameters());
    }
    try (var input = new CollectionObjectStream<>(tokenSamples)) {
      tokenizerModel = TokenizerME.train(input,
          new opennlp.tools.tokenize.TokenizerFactory("en", null, false, null), parameters());
    }
    Path out = Path.of(args[0]);
    Files.createDirectories(out);
    try (var stream = Files.newOutputStream(out.resolve("sentence.bin"))) { sentenceModel.serialize(stream); }
    try (var stream = Files.newOutputStream(out.resolve("tokenizer.bin"))) { tokenizerModel.serialize(stream); }
    SentenceModel reloadedSentences = new SentenceModel(out.resolve("sentence.bin"));
    TokenizerModel reloadedTokens = new TokenizerModel(out.resolve("tokenizer.bin"));
    int comparisons = 0;
    for (String[] parts : corpus) {
      String text = parts[0] + " " + parts[1];
      List<Token> reference = expected(text, sentenceModel, tokenizerModel);
      if (reference.isEmpty() || reference.getLast().sentence() != 1) {
        throw new AssertionError("two-sentence training fixture: " + reference);
      }
      if (!reference.equals(expected(text, reloadedSentences, reloadedTokens))) {
        throw new AssertionError("reload inference mismatch");
      }
      if (!reference.equals(actual(new StringReader(text), reloadedSentences, reloadedTokens))) {
        throw new AssertionError("Lucene/OpenNLP inference mismatch");
      }
      for (int split = 0; split <= text.length(); split++) {
        SplitText segmented = new SplitText(text.substring(0, split), text.substring(split));
        if (!reference.equals(actual(M3TextReaders.reader(segmented), reloadedSentences, reloadedTokens))) {
          throw new AssertionError("composite reader mismatch at split " + split);
        }
        comparisons++;
      }
    }
    if (!actual(M3TextReaders.reader(new SplitText("", "")), reloadedSentences, reloadedTokens).isEmpty()) {
      throw new AssertionError("empty input");
    }
    if (!org.apache.lucene.analysis.TokenizerFactory.lookupClass("opennlp").getName()
        .equals("org.apache.lucene.analysis.opennlp.OpenNLPTokenizerFactory")
        || !TokenFilterFactory.lookupClass("opennlpPOS").getName()
        .equals("org.apache.lucene.analysis.opennlp.OpenNLPPOSFilterFactory")
        || !TokenFilterFactory.lookupClass("opennlpChunker").getName()
        .equals("org.apache.lucene.analysis.opennlp.OpenNLPChunkerFilterFactory")
        || !TokenFilterFactory.lookupClass("opennlpLemmatizer").getName()
        .equals("org.apache.lucene.analysis.opennlp.OpenNLPLemmatizerFilterFactory")) {
      throw new AssertionError("JPMS services unavailable");
    }
    System.out.println("NLP_SYNTHETIC_MODEL_BRIDGE_PASS corpus=" + corpus.length
        + " trainingSamples=" + sentenceSamples.size() + " compositeComparisons=" + comparisons);
  }
}
