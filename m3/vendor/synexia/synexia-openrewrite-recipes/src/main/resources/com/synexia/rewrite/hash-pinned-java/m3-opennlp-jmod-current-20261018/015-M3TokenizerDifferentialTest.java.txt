/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import opennlp.tools.util.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Compares the refactor to a frozen pre-refactor scanner, not to its own shared method. */
class M3TokenizerDifferentialTest {
  private record SpanValue(int start, int end, String type, long probabilityBits) { }

  private record Outcome(List<SpanValue> spans, String exception, String message) { }

  private record Composite(String left, String right) implements CharSequence {
    public int length() {
      return left.length() + right.length();
    }

    public char charAt(int index) {
      Objects.checkIndex(index, length());
      return index < left.length() ? left.charAt(index) : right.charAt(index - left.length());
    }

    public CharSequence subSequence(int start, int end) {
      throw new AssertionError("input must not be sliced");
    }

    @Override
    public String toString() {
      throw new AssertionError("input must not be flattened");
    }
  }

  private static Outcome outcome(Supplier<Span[]> call) {
    try {
      List<SpanValue> result = Arrays.stream(call.get()).map(span -> new SpanValue(
          span.getStart(), span.getEnd(), span.getType(), Double.doubleToLongBits(span.getProb())))
          .toList();
      return new Outcome(result, null, null);
    } catch (RuntimeException failure) {
      return new Outcome(List.of(), failure.getClass().getName(), failure.getMessage());
    }
  }

  private static void compare(String text, int split) {
    if (text == null) {
      // A hot JVM may reuse message-free exceptions. Exact message comparison is forked below.
      assertThrows(NullPointerException.class,
          () -> M3BaselineSimpleTokenizer.INSTANCE.tokenizePos(null));
      assertThrows(NullPointerException.class, () -> SimpleTokenizer.INSTANCE.tokenizePos(null));
      return;
    }
    Outcome expected = outcome(() -> M3BaselineSimpleTokenizer.INSTANCE.tokenizePos(text));
    assertEquals(expected, outcome(() -> SimpleTokenizer.INSTANCE.tokenizePos(text)));
    if (text != null) {
      Composite composite = new Composite(text.substring(0, split), text.substring(split));
      assertEquals(expected, outcome(() -> M3Tokenizers.simpleSpans(composite)));
      assertEquals(expected, outcome(() -> M3Tokenizers.simpleSpans(composite, () -> { })));
    }
  }

  @Test
  void everyUtf16UnitWithClassBoundariesAndBothNewlineSettings() {
    synchronized (SimpleTokenizer.INSTANCE) {
      boolean previous = SimpleTokenizer.INSTANCE.keepNewLines;
      try {
        for (boolean keep : new boolean[] {false, true}) {
          SimpleTokenizer.INSTANCE.setKeepNewLines(keep);
          M3BaselineSimpleTokenizer.INSTANCE.setKeepNewLines(keep);
          compare(null, 0);
          compare("", 0);
          for (int value = Character.MIN_VALUE; value <= Character.MAX_VALUE; value++) {
            char unit = (char) value;
            String text = "A0 " + unit + unit + "Z9";
            compare(text, 4);
          }
          for (String text : List.of("\n", "\r", "\r\n", "a\n", "\u001c\r", "...+++",
              "\u0085\u00a0\u2028\u2029", "A\ud83d\ude00B", "\ud800\udc00\udc00")) {
            for (int split = 0; split <= text.length(); split++) {
              compare(text, split);
            }
          }
        }
      } finally {
        SimpleTokenizer.INSTANCE.setKeepNewLines(previous);
        M3BaselineSimpleTokenizer.INSTANCE.setKeepNewLines(false);
      }
    }
  }

  @Test
  void randomizedCompositeOffsetsAndReentrantReaders() throws Exception {
    synchronized (SimpleTokenizer.INSTANCE) {
      boolean previous = SimpleTokenizer.INSTANCE.keepNewLines;
      try {
        SimpleTokenizer.INSTANCE.setKeepNewLines(false);
        M3BaselineSimpleTokenizer.INSTANCE.setKeepNewLines(false);
        Random random = new Random(20261015L);
        for (int trial = 0; trial < 10000; trial++) {
          char[] units = new char[trial % 257];
          for (int i = 0; i < units.length; i++) {
            units[i] = (char) random.nextInt(65536);
          }
          compare(new String(units), random.nextInt(units.length + 1));
        }
        try (var pool = Executors.newFixedThreadPool(4)) {
          List<Callable<Boolean>> jobs = new ArrayList<>();
          for (int i = 0; i < 128; i++) {
            final String text = "A\ud83d\ude00B\u0301\u0085" + i;
            jobs.add(() -> {
              compare(text, 2);
              return true;
            });
          }
          for (var result : pool.invokeAll(jobs)) {
            assertTrue(result.get());
          }
        }
      } finally {
        SimpleTokenizer.INSTANCE.setKeepNewLines(previous);
      }
    }
  }

  @Test
  void exactNullMessagesInAnIsolatedInterpreter() throws Exception {
    Path output = Files.createTempFile("m3-tokenizer-null-", ".log");
    String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
    String java = Path.of(System.getProperty("java.home"), "bin", executable).toString();
    String classpath = System.getProperty("surefire.test.class.path",
        System.getProperty("java.class.path"));
    Process child = null;
    try {
      child = new ProcessBuilder(java, "-Xint", "-XX:-OmitStackTraceInFastThrow",
          "-XX:+ShowCodeDetailsInExceptionMessages", "-cp", classpath,
          M3TokenizerNullProbe.class.getName()).redirectErrorStream(true)
          .redirectOutput(output.toFile()).start();
      assertTrue(child.waitFor(45, TimeUnit.SECONDS), "isolated null probe timed out");
      String report;
      try (var input = Files.newInputStream(output)) {
        report = new String(input.readNBytes(8192), StandardCharsets.UTF_8);
      }
      assertEquals(0, child.exitValue(), report);
      assertTrue(report.contains("M3_NULL_MESSAGE_PROBE_PASS comparisons=20000"), report);
    } finally {
      if (child != null && child.isAlive()) {
        child.destroyForcibly().waitFor(5, TimeUnit.SECONDS);
      }
      Files.deleteIfExists(output);
    }
  }

  @Test
  void cancellationReachesTheScannerWithoutFlattening() {
    AtomicInteger checkpoints = new AtomicInteger();
    Composite input = new Composite("a".repeat(300), "b".repeat(300));
    assertThrows(CancellationException.class, () -> M3Tokenizers.simpleSpans(input, () -> {
      if (checkpoints.incrementAndGet() == 3) {
        throw new CancellationException("during scan");
      }
    }));
    assertEquals(3, checkpoints.get());
  }
}
