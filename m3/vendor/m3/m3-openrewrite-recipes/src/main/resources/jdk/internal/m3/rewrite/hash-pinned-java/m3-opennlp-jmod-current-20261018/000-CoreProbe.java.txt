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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import opennlp.tools.dictionary.Dictionary;
import opennlp.tools.document.Annotation;
import opennlp.tools.document.Document;
import opennlp.tools.document.DocumentAnnotatorProvider;
import opennlp.tools.document.Layers;
import opennlp.tools.stemmer.StemmerAnnotator;
import opennlp.tools.tokenize.M3Tokenizers;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.tokenize.WordpieceEncoder;
import opennlp.tools.util.Span;
import opennlp.tools.util.StringList;
import opennlp.tools.util.ext.ProviderSpec;
import opennlp.tools.util.ext.Providers;

/** Real OpenNLP model, serialization, XML, service, and source-offset behavior. */
public final class CoreProbe {
  public static void main(String[] args) throws Exception {
    MessageDigest result = MessageDigest.getInstance("SHA-256");
    TokenizerModel trained = ImmutableTextProbe.model();
    ByteArrayOutputStream modelBytes = new ByteArrayOutputStream();
    trained.serialize(modelBytes);
    TokenizerME before = new TokenizerME(trained);
    TokenizerME after = new TokenizerME(new TokenizerModel(
        new ByteArrayInputStream(modelBytes.toByteArray())));
    for (String text : List.of("test,", "year, yes, it", "a\r\nb", "\u4E2D\uD83D\uDE00a\u0301", "")) {
      Span[] expected = before.tokenizePos(text);
      Span[] actual = after.tokenizePos(text);
      if (!Arrays.equals(expected, actual) || !Arrays.equals(before.probs(), after.probs())) {
        throw new AssertionError("model save/reload changed token spans or probabilities");
      }
      if (!Arrays.equals(SimpleTokenizer.INSTANCE.tokenizePos(text),
          M3Tokenizers.simpleSpans(new ImmutableTextProbe.StableText(text)))) {
        throw new AssertionError("simple scanner changed UTF-16 spans");
      }
      update(result, Arrays.toString(actual));
      update(result, Arrays.toString(after.probs()));
    }
    for (boolean lowerCase : new boolean[]{false, true}) {
      WordpieceEncoder encoder = new WordpieceEncoder(
          List.of("[CLS]", "[SEP]", "[UNK]", "hello", "world", "##s", "!", "\u4E2D"), lowerCase);
      String text = "Hello worlds! \u4E2D\uD83D\uDE00";
      var normal = encoder.encode(text);
      var composite = encoder.encodeImmutableView(
          new ImmutableTextProbe.StableText("Hello ", "worlds! \u4E2D\uD83D", "\uDE00"));
      if (!normal.equals(composite)) {
        throw new AssertionError("WordPiece original offsets changed across a split surrogate");
      }
      update(result, composite.toString());
    }
    Dictionary dictionary = new Dictionary(false);
    dictionary.put(new StringList("Stra\u00DFe"));
    ByteArrayOutputStream dictionaryBytes = new ByteArrayOutputStream();
    dictionary.serialize(dictionaryBytes);
    Dictionary restored = new Dictionary(new ByteArrayInputStream(dictionaryBytes.toByteArray()));
    if (!dictionary.equals(restored)) {
      throw new AssertionError("secure XML dictionary serialization changed content");
    }
    ProviderSpec spec = ProviderSpec.of(Map.of("algorithm", "porter"));
    var provider = Providers.of(DocumentAnnotatorProvider.class).select(spec);
    Document document = Document.of("running").with(Layers.TOKENS,
        List.of(new Annotation<>(new Span(0, 7), "running")));
    var stems = provider.create(spec).annotate(document).get(StemmerAnnotator.STEMS);
    if (stems.size() != 1 || !"run".equals(stems.getFirst().value())) {
      throw new AssertionError("built-in module service failed to run the actual stemmer");
    }
    update(result, stems.toString());
    System.out.println("OPENNLP_CORE_BEHAVIOR=" + HexFormat.of().formatHex(result.digest()));
  }

  private static void update(MessageDigest hash, String value) {
    hash.update(value.getBytes(StandardCharsets.UTF_8));
    hash.update((byte) 0);
  }
}
