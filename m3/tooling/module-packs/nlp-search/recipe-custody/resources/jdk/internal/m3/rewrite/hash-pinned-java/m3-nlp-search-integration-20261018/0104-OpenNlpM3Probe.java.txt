/*
 * Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: Apache-2.0
 */
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import opennlp.tools.tokenize.M3Tokenizers;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.SubwordPiece;
import opennlp.tools.tokenize.SubwordTokenizer;
import opennlp.tools.tokenize.WordpieceEncoder;
import opennlp.tools.util.Span;

/**
 * Independent source-bundle oracle. The unchanged OpenNLP 2.4 JAR is loaded in a
 * separate class loader; no baseline tokenizer algorithm is copied into the test.
 * The new WordPiece encoder is checked against frozen results plus its explicit
 * String-snapshot path. Its 3.0 semantics do not replace the older WordpieceTokenizer.
 */
public final class OpenNlpM3Probe {
  private static long checks;
  private static final List<String> VOCABULARY = List.of(
      "[UNK]", "[CLS]", "[SEP]", "hello", "world", "##s", "!", "中", "文",
      "cafe", "i", "ος", "σ", "ς", "😀", ".", ",", "a", "##a", "x", "##x",
      "A", "Hello", "##o", "##l", "h", "##e");
  private static final String[] CASES = {
      "", " ", "Hello worlds!", "hello,world", "a\t\nb\r\nx", " \r a",
      "中😀文", "cafe\u0301 Café", "İ I i Σ ΟΣ ΣΑ AΣ", "\u0000x\uFFFD",
      "\uD83D\uDE00", "\uD83Dx\uDE00", "\uD800\uD800\uDC00\uDC00",
      "\u00A0\u2007\u202F\u2028\u2029", "\u00AD\uFEFF\u200D",
      "...... 123.45 +==-", "a\u0301\u0327\u0300b", "日本語 한국어",
      "a".repeat(101), "😀".repeat(101)
  };

  private record Shape(int start, int end, String type, long probabilityBits) { }
  private record Outcome(List<Shape> spans, String error, String message) { }
  private interface Work { Span[] run() throws Throwable; }

  /** An immutable segmented test input that forbids all payload materialization. */
  private static final class Segmented implements CharSequence {
    private final String[] parts;
    private final int[] ends;
    private final int length;
    private Segmented(String text, int split) {
      parts = new String[] {text.substring(0, split), text.substring(split)};
      ends = new int[] {split, text.length()};
      length = text.length();
    }
    @Override public int length() { return length; }
    @Override public char charAt(int index) {
      Objects.checkIndex(index, length);
      return index < ends[0] ? parts[0].charAt(index) : parts[1].charAt(index - ends[0]);
    }
    @Override public CharSequence subSequence(int start, int end) {
      throw new AssertionError("immutable path called subSequence");
    }
    @Override public String toString() {
      throw new AssertionError("immutable path flattened input");
    }
  }

  private static final class Legacy implements java.io.Closeable {
    private final URLClassLoader loader;
    private final Object instance;
    private final Method tokenize;
    private final Method newline;
    private final Method start;
    private final Method end;
    private final Method type;
    private final Method probability;
    private Legacy(String classpath) throws Exception {
      List<URL> urls = new ArrayList<>();
      for (String element : classpath.split(java.util.regex.Pattern.quote(
          System.getProperty("path.separator")))) {
        urls.add(Path.of(element).toUri().toURL());
      }
      // A null parent is essential in the module-path lane: a platform parent can
      // delegate OpenNLP's named module and accidentally compare the candidate to itself.
      loader = new URLClassLoader(urls.toArray(new URL[0]), null);
      Class<?> tokenizerClass = loader.loadClass("opennlp.tools.tokenize.SimpleTokenizer");
      instance = tokenizerClass.getField("INSTANCE").get(null);
      tokenize = tokenizerClass.getMethod("tokenizePos", String.class);
      newline = tokenizerClass.getMethod("setKeepNewLines", boolean.class);
      newline.setAccessible(true);
      Class<?> spanClass = loader.loadClass("opennlp.tools.util.Span");
      start = spanClass.getMethod("getStart");
      end = spanClass.getMethod("getEnd");
      type = spanClass.getMethod("getType");
      probability = spanClass.getMethod("getProb");
      check(tokenizerClass != SimpleTokenizer.class, "baseline must be independently loaded");
    }
    private void newlines(boolean enabled) throws Exception { newline.invoke(instance, enabled); }
    private Outcome tokenize(String text) {
      try {
        Object[] spans = (Object[]) tokenize.invoke(instance, text);
        List<Shape> result = new ArrayList<>(spans.length);
        for (Object span : spans) {
          result.add(new Shape((int) start.invoke(span), (int) end.invoke(span),
              (String) type.invoke(span),
              Double.doubleToLongBits((double) probability.invoke(span))));
        }
        return new Outcome(result, null, null);
      } catch (Throwable ex) {
        return failure(ex);
      }
    }
    @Override public void close() throws java.io.IOException { loader.close(); }
  }

  private static Outcome outcome(Work work) {
    try {
      List<Shape> result = new ArrayList<>();
      for (Span span : work.run()) {
        result.add(new Shape(span.getStart(), span.getEnd(), span.getType(),
            Double.doubleToLongBits(span.getProb())));
      }
      return new Outcome(result, null, null);
    } catch (Throwable ex) {
      return failure(ex);
    }
  }
  private static Outcome failure(Throwable error) {
    while (error instanceof InvocationTargetException && error.getCause() != null) {
      error = error.getCause();
    }
    return new Outcome(List.of(), error.getClass().getName(), error.getMessage());
  }
  private static void check(boolean condition, String description) {
    checks++;
    if (!condition) throw new AssertionError(description);
  }
  private static void equal(Object expected, Object actual, String description) {
    check(Objects.equals(expected, actual), description + "\nexpected=" + expected + "\nactual=" + actual);
  }

  private static void scannerCase(Legacy baseline, String text, int split) {
    Outcome expected = baseline.tokenize(text);
    equal(expected, outcome(() -> SimpleTokenizer.INSTANCE.tokenizePos(text)), "legacy String scanner");
    CharSequence view = new Segmented(text, split);
    equal(expected, outcome(() -> M3Tokenizers.simpleSpans(view)), "immutable scanner");
    equal(expected, outcome(() -> M3Tokenizers.simpleSpans(view, () -> { })), "observed scanner");
  }

  private static void scanner(Legacy baseline) throws Exception {
    Random random = new Random(0x4d334e4c50L);
    for (boolean keep : new boolean[] {false, true}) {
      baseline.newlines(keep);
      SimpleTokenizer.INSTANCE.setKeepNewLines(keep);
      for (String text : CASES) {
        for (int split = 0; split <= text.length(); split++) scannerCase(baseline, text, split);
      }
      for (int unit = 0; unit <= Character.MAX_VALUE; unit++) {
        scannerCase(baseline, "a" + (char) unit + "0", unit & 3);
      }
      for (int trial = 0; trial < 10000; trial++) {
        int length = random.nextInt(97);
        StringBuilder text = new StringBuilder(length);
        for (int i = 0; i < length; i++) text.append((char) random.nextInt(65536));
        String stable = text.toString();
        scannerCase(baseline, stable, random.nextInt(length + 1));
      }
      // Helpful NPE messages may be omitted by a hot JVM; their exact equality is
      // separately required in the isolated interpreter process below.
      Outcome oldNull = baseline.tokenize(null);
      Outcome newNull = outcome(() -> SimpleTokenizer.INSTANCE.tokenizePos(null));
      equal(oldNull.error(), newNull.error(), "legacy null exception class");
    }
    baseline.newlines(false);
    SimpleTokenizer.INSTANCE.setKeepNewLines(false);
    Outcome immutableNull = outcome(() -> M3Tokenizers.simpleSpans(null));
    equal(NullPointerException.class.getName(), immutableNull.error(), "borrowed null class");
    equal("text", immutableNull.message(), "borrowed null diagnostic");
  }

  private static void wordpieceCase(WordpieceEncoder encoder, String text, int split) {
    List<SubwordPiece> expected = encoder.encode(text);
    equal(expected, encoder.encodeImmutableView(new Segmented(text, split)), "WordPiece immutable records");
    SubwordTokenizer api = encoder;
    check(Arrays.equals(expected.stream().mapToInt(SubwordPiece::id).toArray(),
        api.encodeToIds(text)), "WordPiece ids default method");
    check(Arrays.equals(expected.stream().map(SubwordPiece::piece).toArray(String[]::new),
        api.encodeToPieces(text)), "WordPiece pieces default method");
  }

  private static void wordpiece() throws Exception {
    WordpieceEncoder uncased = new WordpieceEncoder(VOCABULARY, true);
    equal(List.of(new SubwordPiece("[CLS]", 1, 0, 0),
        new SubwordPiece("hello", 3, 0, 5), new SubwordPiece("world", 4, 6, 11),
        new SubwordPiece("##s", 5, 11, 12), new SubwordPiece("!", 6, 12, 13),
        new SubwordPiece("[SEP]", 2, 13, 13)),
        uncased.encodeImmutableView(new Segmented("Hello worlds!", 3)), "frozen BERT record oracle");
    equal(List.of(new SubwordPiece("[CLS]", 1, 0, 0),
        new SubwordPiece("中", 7, 0, 1), new SubwordPiece("😀", 14, 1, 3),
        new SubwordPiece("文", 8, 3, 4), new SubwordPiece("[SEP]", 2, 4, 4)),
        uncased.encodeImmutableView(new Segmented("中😀文", 2)), "split surrogate source coordinates");
    AtomicInteger snapshots = new AtomicInteger();
    CharSequence snapshotOnly = new CharSequence() {
      @Override public int length() { throw new AssertionError("legacy path read mutable length"); }
      @Override public char charAt(int index) { throw new AssertionError("legacy path read mutable unit"); }
      @Override public CharSequence subSequence(int start, int end) {
        throw new AssertionError("legacy path sliced mutable input");
      }
      @Override public String toString() { snapshots.incrementAndGet(); return "Hello worlds!"; }
    };
    equal(uncased.encode("Hello worlds!"), uncased.encode(snapshotOnly), "snapshot-only legacy path");
    equal(1, snapshots.get(), "one legacy snapshot");
    StringBuilder mutable = new StringBuilder("Hello worlds!");
    equal(uncased.encode(mutable.toString()), uncased.encode(mutable), "StringBuilder snapshot");
    for (boolean lower : new boolean[] {false, true}) {
      WordpieceEncoder encoder = new WordpieceEncoder(VOCABULARY, lower);
      for (String text : CASES) {
        for (int split = 0; split <= text.length(); split++) wordpieceCase(encoder, text, split);
      }
      Random random = new Random(lower ? 789123 : 928347);
      for (int trial = 0; trial < 10000; trial++) {
        int length = random.nextInt(65);
        StringBuilder text = new StringBuilder(length);
        for (int i = 0; i < length; i++) text.append((char) random.nextInt(65536));
        String stable = text.toString();
        wordpieceCase(encoder, stable, random.nextInt(length + 1));
      }
    }
    try {
      uncased.encodeImmutableView(null);
      throw new AssertionError("null immutable input accepted");
    } catch (IllegalArgumentException expected) {
      equal("text must not be null", expected.getMessage(), "WordPiece null diagnostic");
    }
    try {
      new WordpieceEncoder(List.of("[CLS]", "[SEP]", "[UNK]", ""));
      throw new AssertionError("empty vocabulary entry accepted");
    } catch (IllegalArgumentException expected) {
      equal("vocabulary must not contain an empty piece at index 3", expected.getMessage(),
          "WordPiece validation atom");
    }
    WordpieceEncoder limited = new WordpieceEncoder(VOCABULARY, true, 0);
    equal(List.of(new SubwordPiece("[CLS]", 1, 0, 0),
        new SubwordPiece("[UNK]", 0, 0, 5), new SubwordPiece("[SEP]", 2, 5, 5)),
        limited.encodeImmutableView(new Segmented("hello", 2)), "zero word limit");
  }

  private static void cancellation() {
    CharSequence input = new Segmented("a ".repeat(1000), 337);
    AtomicInteger points = new AtomicInteger();
    try {
      M3Tokenizers.simpleSpans(input, () -> {
        if (points.incrementAndGet() == 3) throw new CancellationException("host veto");
      });
      throw new AssertionError("host cancellation was ignored");
    } catch (CancellationException expected) {
      equal("host veto", expected.getMessage(), "host cancellation diagnostic");
      equal(3, points.get(), "bounded host checkpoint");
    }
    try {
      Thread.currentThread().interrupt();
      Outcome interruptedNull = outcome(() -> M3Tokenizers.simpleSpans(null));
      equal(NullPointerException.class.getName(), interruptedNull.error(), "null precedes interruption");
      try {
        M3Tokenizers.simpleSpans(input);
        throw new AssertionError("interruption was ignored");
      } catch (CancellationException expected) {
        equal("tokenization interrupted", expected.getMessage(), "interruption diagnostic");
        check(Thread.currentThread().isInterrupted(), "interrupt flag retained");
      }
    } finally {
      Thread.interrupted();
    }
    AtomicInteger calls = new AtomicInteger();
    equal(List.of(), Arrays.asList(M3Tokenizers.simpleSpans(
        new Segmented("", 0), calls::incrementAndGet)), "empty borrowed input");
    equal(2, calls.get(), "empty operation entry and exit checkpoint");
  }

  private static void concurrentReaders() throws Exception {
    SimpleTokenizer.INSTANCE.setKeepNewLines(false);
    CharSequence view = new Segmented("Hello worlds! 中😀文 cafe\u0301", 17);
    Outcome expected = outcome(() -> M3Tokenizers.simpleSpans(view));
    WordpieceEncoder encoder = new WordpieceEncoder(VOCABULARY, true);
    List<SubwordPiece> encoded = encoder.encodeImmutableView(view);
    var pool = Executors.newFixedThreadPool(4);
    try {
      List<java.util.concurrent.Callable<Boolean>> jobs = new ArrayList<>();
      for (int worker = 0; worker < 4; worker++) jobs.add(() -> {
        for (int i = 0; i < 1000; i++) {
          if (!expected.equals(outcome(() -> M3Tokenizers.simpleSpans(view, () -> { })))) return false;
          if (!encoded.equals(encoder.encodeImmutableView(view))) return false;
        }
        return true;
      });
      for (var future : pool.invokeAll(jobs)) check(future.get(), "four shared immutable readers");
    } finally {
      pool.shutdownNow();
      check(pool.awaitTermination(30, TimeUnit.SECONDS), "reader pool terminated");
    }
  }

  private static void nullDiagnostics(String baselinePath) throws Exception {
    List<String> command = new ArrayList<>();
    command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
    command.addAll(List.of("-Xint", "-XX:-OmitStackTraceInFastThrow",
        "-XX:+ShowCodeDetailsInExceptionMessages"));
    // Replay the same candidate patch when this probe runs in a linked-image or
    // module-path qualification lane, so the child cannot silently use stock code.
    for (String argument : java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments()) {
      if (argument.startsWith("--patch-module=")) command.add(argument);
    }
    String modulePath = System.getProperty("jdk.module.path");
    if (modulePath != null && !modulePath.isBlank()) command.addAll(List.of("--module-path", modulePath));
    if (ModuleLayer.boot().findModule("org.apache.opennlp.tools").isPresent()) {
      command.addAll(List.of("--add-modules", "org.apache.opennlp.tools"));
    }
    command.addAll(List.of("-cp", System.getProperty("java.class.path"),
        OpenNlpM3Probe.class.getName(), baselinePath, "--null-diagnostics"));
    Process process = new ProcessBuilder(command).inheritIO().start();
    if (!process.waitFor(60, TimeUnit.SECONDS)) {
      process.destroyForcibly();
      throw new AssertionError("null diagnostic child timed out");
    }
    equal(0, process.exitValue(), "isolated helpful NPE diagnostics");
  }

  public static void main(String[] args) throws Exception {
    if (args.length < 1) throw new IllegalArgumentException("baseline OpenNLP/SLF4J JAR classpath required");
    try (Legacy baseline = new Legacy(args[0])) {
      if (args.length > 1 && "--null-diagnostics".equals(args[1])) {
        for (int i = 0; i < 20000; i++) {
          equal(baseline.tokenize(null), outcome(() -> SimpleTokenizer.INSTANCE.tokenizePos(null)),
              "isolated legacy null diagnostics");
        }
        System.out.println("OPENNLP_NULL_DIAGNOSTICS_PASS checks=" + checks);
        return;
      }
      scanner(baseline);
      wordpiece();
      cancellation();
      concurrentReaders();
      nullDiagnostics(args[0]);
    } finally {
      SimpleTokenizer.INSTANCE.setKeepNewLines(false);
    }
    System.out.println("OPENNLP_M3_PASS checks=" + checks
        + " allUtf16Units=65536 randomScannerCases=20000 randomWordpieceCases=20000"
        + " immutableMaterialization=forbidden legacyOracle=independent-2.4.0-jar");
  }
}
