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
 */
// M3 packaging adaptation Copyright 2026 Hitesh Soliwal and contributors.
// SPDX-License-Identifier: Apache-2.0
// Golden cases retain Lucene/OpenNLP test provenance in the canonical task's
// dependency-probe-correction/PROVENANCE.json. No production algorithm is copied.
package com.m3.nlpsearch.packchecks;

import com.m3.nlpsearch.M3SourceInputStream;
import com.m3.runtime.capability.SourceArtifact;
import de.hhn.mi.configuration.KernelType;
import de.hhn.mi.configuration.SvmConfigurationImpl;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import opennlp.tools.chunker.ChunkerModel;
import opennlp.tools.doccat.BagOfWordsFeatureGenerator;
import opennlp.tools.doccat.DocumentSample;
import opennlp.tools.lemmatizer.DictionaryLemmatizer;
import opennlp.tools.lemmatizer.LemmatizerModel;
import opennlp.tools.ml.libsvm.doccat.DocumentCategorizerSVM;
import opennlp.tools.ml.libsvm.doccat.SvmDoccatConfiguration;
import opennlp.tools.ml.libsvm.doccat.SvmDoccatModel;
import opennlp.tools.ml.libsvm.doccat.TermWeightingStrategy;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.CollectionObjectStream;
import org.apache.lucene.analysis.M3TextReaders;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.opennlp.OpenNLPChunkerFilter;
import org.apache.lucene.analysis.opennlp.OpenNLPLemmatizerFilter;
import org.apache.lucene.analysis.opennlp.OpenNLPPOSFilter;
import org.apache.lucene.analysis.opennlp.OpenNLPTokenizer;
import org.apache.lucene.analysis.opennlp.tools.NLPChunkerOp;
import org.apache.lucene.analysis.opennlp.tools.NLPLemmatizerOp;
import org.apache.lucene.analysis.opennlp.tools.NLPNERTaggerOp;
import org.apache.lucene.analysis.opennlp.tools.NLPPOSTaggerOp;
import org.apache.lucene.analysis.opennlp.tools.NLPSentenceDetectorOp;
import org.apache.lucene.analysis.opennlp.tools.NLPTokenizerOp;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.analysis.uk.UkrainianMorfologikAnalyzer;
import org.apache.lucene.util.AttributeFactory;

/** Concrete original fixture cases executed against the packaged dependency owners. */
public final class PackDependencyChecks {
    private static final String MODEL_DIRECTORY =
            "lucene/analysis/opennlp/src/test-files/org/apache/lucene/analysis/opennlp";
    private static final Map<String, String> MODEL_HASHES = Map.of(
            "en-test-sent.bin", "9c86c23ec6ccda56daec5b988751d9954f4a62098664e51edac0e8995ad6ba9e",
            "en-test-tokenizer.bin", "91412b897deeebd22bff1ae9270c497a4a2bf72801c316dae3e696f61d274a90",
            "en-test-pos-maxent.bin", "20afedc677c53f5cb820c06af026253a3c94c1bf35c002ac7afba5bf7033d3aa",
            "en-test-chunker.bin", "23592d805e1e8b13357cfae9d8bc623f37dd93839fe50860439825b2c55943a6",
            "en-test-lemmatizer.bin", "5da5c6f1087093fb15942a478ea655e0fb1eea4012355cf02ac6a0e7d380edc9",
            "en-test-ner.bin", "a54e24c2e4b441b12f6971ca7e4196e4116c3750db01e86148d94d3e63058b70",
            "en-test-lemmas.dict", "5ee39992af141f967f8691ec63f18a37244cc1cd3d1e726904f126a6cb3612b2");

    // Lucene TestOpenNLPTokenizerFactory, TestOpenNLPPOSFilterFactory,
    // and TestOpenNLPChunkerFilterFactory at 42f261c60e1436646dd28d8cd0eff8bedf6e42da.
    private static final String SENTENCES =
            "Sentence number 1 has 6 words. Sentence number 2, 5 words.";
    private static final String[] TERMS = {
            "Sentence", "number", "1", "has", "6", "words", ".",
            "Sentence", "number", "2", ",", "5", "words", "."
    };
    private static final int[] STARTS = {0, 9, 16, 18, 22, 24, 29, 31, 40, 47, 48, 50, 52, 57};
    private static final int[] ENDS = {8, 15, 17, 21, 23, 29, 30, 39, 46, 48, 49, 51, 57, 58};
    private static final String[] POS = {
            "NN", "NN", "CD", "VBZ", "CD", "NNS", ".",
            "NN", "NN", "CD", ",", "CD", "NNS", "."
    };
    private static final String[] CHUNKS = {
            "B-NP", "I-NP", "I-NP", "B-VP", "B-NP", "I-NP", "O",
            "B-NP", "I-NP", "I-NP", "O", "B-NP", "I-NP", "O"
    };

    // Lucene TestOpenNLPLemmatizerFilterFactory's original tiny-model expectations.
    private static final String LEMMA_SENTENCE = "They sent him running in the evening.";
    private static final String[] LEMMA_POS = {"NNP", "VBD", "PRP", "VBG", "IN", "DT", "NN", "."};
    private static final String[] DICTIONARY_LEMMAS = {
            "they", "send", "he", "run", "in", "the", "evening", "."
    };
    private static final String[] MAXENT_LEMMAS = {
            "they", "send", "he", "runn", "in", "the", "evening", "."
    };

    private PackDependencyChecks() {}

    public static void main(String[] args) throws Exception {
        check(args.length == 1, "expected one absolute Lucene source-root argument");
        Path requestedRoot = Path.of(args[0]);
        check(requestedRoot.isAbsolute(), "absolute Lucene source root required");
        Path sourceRoot = requestedRoot.toRealPath();
        Path modelsRoot = sourceRoot.resolve(MODEL_DIRECTORY).toRealPath();
        check(modelsRoot.startsWith(sourceRoot), "model directory must remain within Lucene source");

        namedOwner(M3SourceInputStream.class, "com.m3.nlpsearch");
        namedOwner(SourceArtifact.class, "com.m3.runtime.capability");
        namedOwner(M3TextReaders.class, "org.apache.lucene.core");
        namedOwner(OpenNLPTokenizer.class, "org.apache.lucene.analysis.opennlp");
        namedOwner(TokenizerModel.class, "org.apache.opennlp.tools");
        namedOwner(DocumentCategorizerSVM.class, "org.apache.opennlp.tools");
        namedOwner(SvmConfigurationImpl.class, "zlibsvm.core");
        namedOwner(libsvm.svm.class, "libsvm");
        namedOwner(UkrainianMorfologikAnalyzer.class, "org.apache.lucene.analysis.morfologik");
        Module dictionaryModule = ModuleLayer.boot().findModule("morfologik.ukrainian.search")
                .orElseThrow(() -> new AssertionError("Ukrainian dictionary module is unresolved"));
        check(!dictionaryModule.getDescriptor().isAutomatic(), "explicit dictionary module required");

        Models models = new Models(
                readModel(modelsRoot, "en-test-sent.bin", SentenceModel::new),
                readModel(modelsRoot, "en-test-tokenizer.bin", TokenizerModel::new),
                readModel(modelsRoot, "en-test-pos-maxent.bin", POSModel::new),
                readModel(modelsRoot, "en-test-chunker.bin", ChunkerModel::new),
                readModel(modelsRoot, "en-test-lemmatizer.bin", LemmatizerModel::new),
                readModel(modelsRoot, "en-test-lemmas.dict", DictionaryLemmatizer::new));
        TokenNameFinderModel ner = readModel(modelsRoot, "en-test-ner.bin", TokenNameFinderModel::new);
        new NLPNERTaggerOp(ner).reset();
        System.out.println("NER_MODEL_LOAD_PASS prediction_parity=NOT_CLAIMED");

        checkStream("tokenizer", tokenizer(models, SENTENCES), SENTENCES.length(),
                TERMS, STARTS, ENDS, null);
        checkStream("pos", tagged(models, SENTENCES), SENTENCES.length(),
                TERMS, STARTS, ENDS, POS);
        checkStream("chunker",
                new OpenNLPChunkerFilter(tagged(models, SENTENCES), new NLPChunkerOp(models.chunker())),
                SENTENCES.length(), TERMS, STARTS, ENDS, CHUNKS);
        checkStream("dictionary-lemma",
                new OpenNLPLemmatizerFilter(tagged(models, LEMMA_SENTENCE),
                        new NLPLemmatizerOp(models.dictionary(), null)),
                LEMMA_SENTENCE.length(), DICTIONARY_LEMMAS, null, null, LEMMA_POS);
        checkStream("maxent-lemma",
                new OpenNLPLemmatizerFilter(tagged(models, LEMMA_SENTENCE),
                        new NLPLemmatizerOp(null, models.lemmatizer())),
                LEMMA_SENTENCE.length(), MAXENT_LEMMAS, null, null, LEMMA_POS);

        checkUkrainianDictionary();
        checkLibSvmTraining();
        System.out.println("PACK_DEPENDENCY_CHECKS_PASS models=6 dictionary=1 tokenizer=14 pos=14"
                + " chunks=14 lemma_modes=2 ukrainian=6 libsvm_samples=16 libsvm_queries=2");
    }

    private static OpenNLPTokenizer tokenizer(Models models, String text) throws IOException {
        OpenNLPTokenizer tokenizer = new OpenNLPTokenizer(
                AttributeFactory.DEFAULT_ATTRIBUTE_FACTORY,
                new NLPSentenceDetectorOp(models.sentences()), new NLPTokenizerOp(models.tokenizer()));
        tokenizer.setReader(M3TextReaders.reader(text));
        return tokenizer;
    }

    private static TokenStream tagged(Models models, String text) throws IOException {
        return new OpenNLPPOSFilter(tokenizer(models, text), new NLPPOSTaggerOp(models.pos()));
    }

    private static void checkStream(String label, TokenStream input, int finalOffset,
            String[] expectedTerms, int[] starts, int[] ends, String[] expectedTypes) throws IOException {
        try (TokenStream stream = input) {
            CharTermAttribute term = stream.addAttribute(CharTermAttribute.class);
            OffsetAttribute offset = stream.addAttribute(OffsetAttribute.class);
            TypeAttribute type = stream.addAttribute(TypeAttribute.class);
            stream.reset();
            int count = 0;
            while (stream.incrementToken()) {
                check(count < expectedTerms.length, label + ": unexpected extra token");
                check(expectedTerms[count].equals(term.toString()),
                        label + ": term " + count + " expected " + expectedTerms[count] + ", got " + term);
                if (starts != null) {
                    check(offset.startOffset() == starts[count] && offset.endOffset() == ends[count],
                            label + ": UTF-16 offsets differ at token " + count);
                }
                if (expectedTypes != null) {
                    check(expectedTypes[count].equals(type.type()),
                            label + ": type " + count + " expected " + expectedTypes[count]
                                    + ", got " + type.type());
                }
                count++;
            }
            check(count == expectedTerms.length, label + ": missing tokens");
            stream.end();
            check(offset.endOffset() == finalOffset, label + ": final offset differs");
        }
        System.out.println("ORIGINAL_GOLDEN_CASE_PASS " + label);
    }

    private static void checkUkrainianDictionary() throws IOException {
        // Lucene TestUkrainianAnalyzer.testSpecialCharsTokenStream, unchanged expected stems.
        String input = "м'яса м'я\u0301са м\u02BCяса м\u2019яса м\u2018яса м\u0060яса";
        String[] stems = {"м'ясо", "м'ясо", "м'ясо", "м'ясо", "м'ясо", "м'ясо"};
        try (UkrainianMorfologikAnalyzer analyzer = new UkrainianMorfologikAnalyzer()) {
            checkStream("ukrainian-dictionary",
                    analyzer.tokenStream("fixture", M3TextReaders.reader(input)),
                    input.length(), stems, null, null, null);
        }
    }

    private static void checkLibSvmTraining() throws IOException {
        // OpenNLP DocumentCategorizerSVMTest.testLinearBinaryClassifiesCorrectly at
        // 528c9edd9560ed44515370fa831a60bc36111129: same samples, queries and configuration.
        List<DocumentSample> samples = List.of(
                new DocumentSample("sports",
                        new String[] {"football", "goal", "team", "match", "player", "stadium"}),
                new DocumentSample("sports",
                        new String[] {"basketball", "score", "team", "court", "player", "game"}),
                new DocumentSample("sports",
                        new String[] {"tennis", "match", "serve", "court", "player", "game"}),
                new DocumentSample("sports",
                        new String[] {"football", "match", "goal", "referee", "stadium", "game"}),
                new DocumentSample("sports",
                        new String[] {"basketball", "team", "score", "ball", "arena", "game"}),
                new DocumentSample("sports",
                        new String[] {"football", "goal", "player", "match", "game", "coach"}),
                new DocumentSample("sports",
                        new String[] {"tennis", "serve", "player", "court", "match", "game"}),
                new DocumentSample("sports",
                        new String[] {"basketball", "team", "court", "player", "score", "game"}),
                new DocumentSample("technology",
                        new String[] {"computer", "software", "code", "program", "algorithm", "system"}),
                new DocumentSample("technology",
                        new String[] {"internet", "network", "server", "cloud", "data", "system"}),
                new DocumentSample("technology",
                        new String[] {"programming", "software", "developer", "code", "api", "system"}),
                new DocumentSample("technology",
                        new String[] {"computer", "algorithm", "data", "machine", "learning", "system"}),
                new DocumentSample("technology",
                        new String[] {"software", "code", "program", "developer", "debug", "system"}),
                new DocumentSample("technology",
                        new String[] {"computer", "software", "algorithm", "code", "system", "database"}),
                new DocumentSample("technology",
                        new String[] {"network", "server", "data", "cloud", "system", "api"}),
                new DocumentSample("technology",
                        new String[] {"programming", "code", "developer", "software", "system", "debug"}));
        SvmDoccatConfiguration configuration = new SvmDoccatConfiguration.Builder()
                .setSvmConfiguration(new SvmConfigurationImpl.Builder()
                        .setKernelType(KernelType.LINEAR).setProbability(true).build())
                .setTermWeightingStrategy(TermWeightingStrategy.BINARY)
                .setScaleFeatures(false).build();
        BagOfWordsFeatureGenerator features = new BagOfWordsFeatureGenerator();
        try (CollectionObjectStream<DocumentSample> training = new CollectionObjectStream<>(samples)) {
            SvmDoccatModel model = DocumentCategorizerSVM.train("eng", training, configuration, features);
            check(model.getNumberOfCategories() == 2, "LibSVM category count differs");
            DocumentCategorizerSVM categorizer = new DocumentCategorizerSVM(model, features);
            checkCategory(categorizer, "sports",
                    new String[] {"football", "goal", "team", "match", "player", "game", "stadium", "coach"});
            checkCategory(categorizer, "technology",
                    new String[] {"computer", "software", "code", "algorithm", "system", "developer", "api", "database"});
        }
        System.out.println("ORIGINAL_GOLDEN_CASE_PASS libsvm-training-prediction");
    }

    private static void checkCategory(DocumentCategorizerSVM categorizer, String expected, String[] words) {
        double[] probabilities = categorizer.categorize(words);
        check(probabilities.length == 2, "LibSVM outcome count differs");
        double sum = 0;
        for (double probability : probabilities) {
            check(probability >= 0.0, "LibSVM returned a negative or NaN probability");
            sum += probability;
        }
        check(Math.abs(sum - 1.0) < 0.05, "LibSVM probability sum differs");
        check(expected.equals(categorizer.getBestCategory(probabilities)),
                "LibSVM expected category " + expected + ", got "
                        + categorizer.getBestCategory(probabilities));
    }

    private static <T> T readModel(Path root, String name, ModelReader<T> reader) throws IOException {
        String expectedHash = Objects.requireNonNull(MODEL_HASHES.get(name), "unlisted model");
        Path file = root.resolve(name);
        check(Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)
                        && file.toRealPath().getParent().equals(root),
                "regular in-root model file required: " + name);
        byte[] bytes;
        try (InputStream input = Files.newInputStream(file)) {
            bytes = input.readNBytes(SourceArtifact.MAX_BYTES + 1);
        }
        check(bytes.length <= SourceArtifact.MAX_BYTES, "model exceeds source byte budget: " + name);
        SourceArtifact source = SourceArtifact.copyOf("lucene-opennlp-fixture/" + name, bytes);
        check(expectedHash.equals(source.root()), "fixture source hash differs: " + name);
        try (M3SourceInputStream input = M3SourceInputStream.open(source.whole())) {
            T model = reader.read(input);
            System.out.println("MODEL_SOURCE_PASS " + name + " " + source.root());
            return model;
        }
    }

    private static void namedOwner(Class<?> type, String expected) {
        Module owner = type.getModule();
        check(expected.equals(owner.getName()) && !owner.getDescriptor().isAutomatic(),
                type.getName() + ": expected explicit module " + expected + ", got " + owner.getName());
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @FunctionalInterface
    private interface ModelReader<T> {
        T read(InputStream input) throws IOException;
    }

    private record Models(SentenceModel sentences, TokenizerModel tokenizer, POSModel pos,
                          ChunkerModel chunker, LemmatizerModel lemmatizer,
                          DictionaryLemmatizer dictionary) {}
}
