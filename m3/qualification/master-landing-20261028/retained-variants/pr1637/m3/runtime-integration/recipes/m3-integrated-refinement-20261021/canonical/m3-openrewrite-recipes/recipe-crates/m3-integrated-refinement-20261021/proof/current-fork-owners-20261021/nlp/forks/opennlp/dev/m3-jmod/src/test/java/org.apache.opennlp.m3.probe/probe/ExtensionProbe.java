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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import org.apache.uima.Constants;
import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.resource.metadata.ConfigurationParameter;

import opennlp.morfologik.builder.MorfologikDictionaryBuilder;
import opennlp.morfologik.lemmatizer.MorfologikLemmatizer;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.util.Span;
import opennlp.uima.util.UimaUtil;

/** Execute the real optional Java extensions from the linked image. */
public final class ExtensionProbe {
  public static void main(String[] args) throws Exception {
    morfologik();
    uima();
    System.out.println("OPENNLP_EXTENSIONS_PASS morfologik=10 uima=44");
  }

  private static void morfologik() throws Exception {
    Path directory = Files.createTempDirectory("opennlp-morfologik-probe-");
    try {
      Path words = directory.resolve("words.txt");
      Files.writeString(words, "car,cars,N\nrun,running,V\n");
      Files.writeString(directory.resolve("words.info"),
          "fsa.dict.separator=,\nfsa.dict.encoding=UTF-8\nfsa.dict.encoder=prefix\n");
      Path dictionary = new MorfologikDictionaryBuilder().build(words);
      String[] lemmas = new MorfologikLemmatizer(dictionary).lemmatize(
          new String[]{"Cars", "running", "absent"}, new String[]{"N", "V", "N"});
      if (!Arrays.equals(new String[]{"car", "run", null}, lemmas)) {
        throw new AssertionError("morfologik dictionary compilation/lookup changed");
      }
    } finally {
      try (var paths = Files.walk(directory)) {
        for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
          Files.delete(path);
        }
      }
    }
  }

  private static void uima() throws Exception {
    var factory = UIMAFramework.getResourceSpecifierFactory();
    var description = factory.createAnalysisEngineDescription();
    description.setFrameworkImplementation(Constants.JAVA_FRAMEWORK_NAME);
    description.setPrimitive(true);
    description.setAnnotatorImplementationName(opennlp.uima.tokenize.SimpleTokenizer.class.getName());
    var metadata = description.getAnalysisEngineMetaData();
    var types = factory.createTypeSystemDescription();
    types.addType("probe.Sentence", "sentence", CAS.TYPE_NAME_ANNOTATION);
    types.addType("probe.Token", "token", CAS.TYPE_NAME_ANNOTATION);
    metadata.setTypeSystem(types);
    String[] names = {UimaUtil.SENTENCE_TYPE_PARAMETER, UimaUtil.TOKEN_TYPE_PARAMETER};
    String[] values = {"probe.Sentence", "probe.Token"};
    for (int i = 0; i < names.length; i++) {
      var parameter = factory.createConfigurationParameter();
      parameter.setName(names[i]);
      parameter.setType(ConfigurationParameter.TYPE_STRING);
      parameter.setMandatory(true);
      metadata.getConfigurationParameterDeclarations().addConfigurationParameter(parameter);
      metadata.getConfigurationParameterSettings().setParameterValue(names[i], values[i]);
    }
    var engine = UIMAFramework.produceAnalysisEngine(description);
    try {
      CAS cas = engine.newCAS();
      String text = "Hi, composite \uD83D\uDE00 text.";
      cas.setDocumentText(text);
      var sentenceType = cas.getTypeSystem().getType("probe.Sentence");
      cas.addFsToIndexes(cas.createAnnotation(sentenceType, 0, text.length()));
      engine.process(cas);
      var tokens = new ArrayList<Span>();
      for (var annotation : cas.getAnnotationIndex(cas.getTypeSystem().getType("probe.Token"))) {
        tokens.add(new Span(annotation.getBegin(), annotation.getEnd()));
      }
      if (!Arrays.equals(SimpleTokenizer.INSTANCE.tokenizePos(text), tokens.toArray(Span[]::new))) {
        throw new AssertionError("UIMA annotator changed original UTF-16 token offsets");
      }
    } finally {
      engine.destroy();
    }
  }
}
