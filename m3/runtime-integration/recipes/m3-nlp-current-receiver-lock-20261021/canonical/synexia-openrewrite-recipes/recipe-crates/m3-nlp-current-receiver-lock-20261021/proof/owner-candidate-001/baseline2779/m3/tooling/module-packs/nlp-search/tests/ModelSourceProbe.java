/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.nlp.search.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jdk.m3.nlp.search.SourceDocument;
import jdk.m3.nlp.search.SourceSpans;
import opennlp.tools.sentdetect.SentenceDetectorFactory;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.sentdetect.SentenceSample;
import opennlp.tools.tokenize.TokenSample;
import opennlp.tools.tokenize.TokenizerFactory;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.CollectionObjectStream;
import opennlp.tools.util.Parameters;
import opennlp.tools.util.Span;
import opennlp.tools.util.TrainingParameters;

/** Source integration with actual trained, serialized, and reloaded OpenNLP models. */
public final class ModelSourceProbe {
    private ModelSourceProbe() { }

    public static void main(String[] args) throws Exception {
        TrainingParameters parameters = new TrainingParameters();
        parameters.put(Parameters.ITERATIONS_PARAM, 40);
        parameters.put(Parameters.CUTOFF_PARAM, 0);
        List<TokenSample> samples = new ArrayList<>();
        for (String word : List.of("year", "world", "hello", "test")) {
            samples.add(new TokenSample(word, new Span[] {new Span(0, word.length())}));
            samples.add(new TokenSample(word + ",", new Span[] {
                    new Span(0, word.length()), new Span(word.length(), word.length() + 1)}));
        }
        TokenizerModel trained = TokenizerME.train(new CollectionObjectStream<>(samples),
                TokenizerFactory.create(null, "eng", null, true, null), parameters);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        trained.serialize(bytes);
        TokenizerModel loaded = new TokenizerModel(new ByteArrayInputStream(bytes.toByteArray()));
        String text = "Hello, world.\nMs. Alice walks. Bob runs. \ud83d\ude00 42\ud800";
        SourceDocument source = SourceDocument.ofImmutableView("model:source", guarded(text));
        TokenizerME oracle = new TokenizerME(loaded);
        TokenizerME consumer = new TokenizerME(loaded);
        for (boolean keep : new boolean[] {false, true}) {
            oracle.setKeepNewLines(keep);
            consumer.setKeepNewLines(keep);
            Span[] expected = oracle.tokenizePos(text);
            compare(SourceSpans.modelTokens(source, consumer), expected);
            require(Arrays.equals(oracle.probs(), consumer.probs()), "Tokenizer model probabilities changed");
        }

        List<SentenceSample> sentences = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            sentences.add(new SentenceSample("Ms. Alice walks. Bob runs.", new Span(0, 16), new Span(17, 26)));
        }
        SentenceModel sentenceModel = SentenceDetectorME.train("eng", new CollectionObjectStream<>(sentences),
                new SentenceDetectorFactory("eng", true, null, new char[] {'.', '?', '!'}), parameters);
        bytes.reset();
        sentenceModel.serialize(bytes);
        SentenceModel loadedSentence = new SentenceModel(new ByteArrayInputStream(bytes.toByteArray()));
        SentenceDetectorME sentenceOracle = new SentenceDetectorME(loadedSentence);
        SentenceDetectorME sentenceConsumer = new SentenceDetectorME(loadedSentence);
        Span[] expected = sentenceOracle.sentPosDetect(text);
        compare(SourceSpans.sentences(source, sentenceConsumer), expected);
        require(Arrays.equals(sentenceOracle.probs(), sentenceConsumer.probs()), "Sentence probabilities changed");
        System.out.println("ModelSourceProbe PASS: real token/sentence model train/serialize/reload; "
                + "immutable source offsets and probability parity");
    }

    private static void compare(SourceSpans actual, Span[] expected) {
        require(actual.size() == expected.length, "Model span count changed");
        for (int i = 0; i < expected.length; i++) {
            require(actual.start(i) == expected[i].getStart() && actual.end(i) == expected[i].getEnd(),
                    "Model source offsets changed");
        }
    }

    private static CharSequence guarded(String text) {
        return new CharSequence() {
            @Override public int length() { return text.length(); }
            @Override public char charAt(int index) { return text.charAt(index); }
            @Override public CharSequence subSequence(int start, int end) {
                throw new AssertionError("Model integration must provide its own range descriptor");
            }
            @Override public String toString() { throw new AssertionError("Model flattened the source"); }
        };
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
