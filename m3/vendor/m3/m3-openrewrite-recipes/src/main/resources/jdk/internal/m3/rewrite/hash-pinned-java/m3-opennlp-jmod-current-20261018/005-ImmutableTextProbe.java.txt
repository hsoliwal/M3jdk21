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

package probe;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import opennlp.tools.document.Annotation;
import opennlp.tools.document.Document;
import opennlp.tools.document.Layers;
import opennlp.tools.tokenize.TokenSample;
import opennlp.tools.tokenize.TokenizerFactory;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.CollectionObjectStream;
import opennlp.tools.util.Parameters;
import opennlp.tools.util.Span;
import opennlp.tools.util.TrainingParameters;

/** No-copy source ownership and exact trained-model span/probability parity. */
public final class ImmutableTextProbe {
  private static Method documentFactory;
  private static Method modelTokenizer;

  public static void main(String[] args) throws Exception {
    try {
      documentFactory = Document.class.getMethod("ofImmutableView", CharSequence.class);
      modelTokenizer = TokenizerME.class.getMethod("tokenizePosImmutableView", CharSequence.class);
    } catch (NoSuchMethodException missing) {
      throw new AssertionError("required immutable-view entry point is missing", missing);
    }
    documentOwnership();
    tokenizerParity();
    System.out.println("OPENNLP_IMMUTABLE_TEXT_PASS cases=2000 newlineModes=2");
  }

  private static Document document(CharSequence text) throws Exception {
    try {
      return (Document) documentFactory.invoke(null, text);
    } catch (InvocationTargetException failed) {
      if (failed.getCause() instanceof Exception cause) {
        throw cause;
      }
      throw failed;
    }
  }

  private static void documentOwnership() throws Exception {
    StableText text = new StableText("running \uD83D", "\uDE00 fast");
    Document empty = document(text);
    if (empty.text() != text) {
      throw new AssertionError("immutable document must retain the supplied owner");
    }
    var first = List.of(new Annotation<>(new Span(0, 7), "running"));
    var second = List.of(new Annotation<>(new Span(0, 7), "VBG"));
    Document tokens = empty.with(Layers.TOKENS, first);
    Document tags = document(new StableText("running ", "\uD83D\uDE00 fast"))
        .with(Layers.POS_TAGS, second);
    Document combined = tokens.merge(tags, Document.DuplicateLayerPolicy.REJECT);
    if (tokens.text() != text || combined.text() != text || !combined.get(Layers.POS_TAGS).equals(second)) {
      throw new AssertionError("with/merge lost source ownership or layer contents");
    }
    try {
      combined.merge(document(new StableText("different")), Document.DuplicateLayerPolicy.REJECT);
      throw new AssertionError("different original text must be refused");
    } catch (IllegalArgumentException expected) {
      // Expected content mismatch without flattening either source.
    }
    try {
      document(null);
      throw new AssertionError("null immutable source must be refused");
    } catch (IllegalArgumentException expected) {
      // Same null exception type as Document.of.
    }
    StringBuilder mutable = new StringBuilder("before");
    Document snapshot = Document.of(mutable);
    mutable.replace(0, mutable.length(), "after");
    if (!"before".contentEquals(snapshot.text())) {
      throw new AssertionError("legacy Document.of snapshot semantics changed");
    }
    try {
      combined.get(Layers.TOKENS).clear();
      throw new AssertionError("layers must stay immutable");
    } catch (UnsupportedOperationException expected) {
      // Expected immutable layer list.
    }
  }

  static TokenizerModel model() throws Exception {
    List<TokenSample> samples = new ArrayList<>();
    for (String word : List.of("year", "it", "yes", "test")) {
      samples.add(new TokenSample(word, new Span[]{new Span(0, word.length())}));
      samples.add(new TokenSample(word + ",", new Span[]{new Span(0, word.length()),
          new Span(word.length(), word.length() + 1)}));
    }
    TrainingParameters parameters = new TrainingParameters();
    parameters.put(Parameters.ITERATIONS_PARAM, 40);
    parameters.put(Parameters.CUTOFF_PARAM, 0);
    return TokenizerME.train(new CollectionObjectStream<>(samples),
        TokenizerFactory.create(null, "eng", null, true, null), parameters);
  }

  private static void tokenizerParity() throws Exception {
    TokenizerModel trained = model();
    TokenizerME oracle = new TokenizerME(trained);
    TokenizerME immutable = new TokenizerME(trained);
    String[] atoms = {"year,", "test", " ", "\r", "\n", "\t", "\u00A0", "\u2003",
        "\uD83D", "\uDE00", "\uD800", "\uDC00", "\u4E2D", "a\u0301", ".", "42"};
    Random random = new Random(19761018L);
    for (boolean keep : new boolean[]{false, true}) {
      oracle.setKeepNewLines(keep);
      immutable.setKeepNewLines(keep);
      for (int trial = 0; trial < 1000; trial++) {
        String[] leaves = new String[random.nextInt(12)];
        for (int index = 0; index < leaves.length; index++) {
          leaves[index] = atoms[random.nextInt(atoms.length)];
        }
        String flat = String.join("", leaves);
        Span[] expected = oracle.tokenizePos(flat);
        double[] probabilities = oracle.probs();
        Span[] actual;
        try {
          actual = (Span[]) modelTokenizer.invoke(immutable, new StableText(leaves));
        } catch (InvocationTargetException failed) {
          throw new AssertionError("immutable tokenization failed at trial " + trial, failed.getCause());
        }
        if (!Arrays.equals(expected, actual) || !Arrays.equals(probabilities, immutable.probs())) {
          throw new AssertionError("model spans/probabilities differ at trial " + trial + " keep=" + keep);
        }
      }
    }
  }

  /** Test-only segmented immutable source: flattening and slicing are forbidden. */
  static final class StableText implements CharSequence {
    private final String[] leaves;
    private final int length;

    StableText(String... input) {
      leaves = input.clone();
      int size = 0;
      for (String leaf : leaves) {
        size = Math.addExact(size, leaf.length());
      }
      length = size;
    }

    @Override public int length() {
      return length;
    }

    @Override public char charAt(int index) {
      if (index < 0 || index >= length) {
        throw new IndexOutOfBoundsException(index);
      }
      for (String leaf : leaves) {
        if (index < leaf.length()) {
          return leaf.charAt(index);
        }
        index -= leaf.length();
      }
      throw new AssertionError("segment lookup");
    }

    @Override public CharSequence subSequence(int start, int end) {
      throw new AssertionError("original source must not be sliced or flattened");
    }

    @Override public String toString() {
      throw new AssertionError("original source must not be flattened");
    }
  }
}
