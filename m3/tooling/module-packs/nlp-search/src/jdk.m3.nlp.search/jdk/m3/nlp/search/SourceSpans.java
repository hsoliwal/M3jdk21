/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.nlp.search;

import java.util.Objects;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.tokenize.M3Tokenizers;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.util.Span;

/** Immutable primitive coordinate planes over one retained source; no per-token text payload. */
public final class SourceSpans {
    private final SourceDocument source;
    private final int[] starts;
    private final int[] ends;

    private SourceSpans(SourceDocument source, Span[] spans) {
        this.source = Objects.requireNonNull(source, "source");
        Objects.requireNonNull(spans, "spans");
        starts = new int[spans.length];
        ends = new int[spans.length];
        for (int i = 0; i < spans.length; i++) {
            Span span = Objects.requireNonNull(spans[i], "span");
            int start = span.getStart();
            int end = span.getEnd();
            if (start < 0 || end < start || end > source.length()
                    || i > 0 && start < starts[i - 1]) {
                throw new IllegalArgumentException("Spans must be ordered and within the original source");
            }
            starts[i] = start;
            ends[i] = end;
        }
    }

    /** Copy only coordinates from existing OpenNLP results. Annotation values remain with OpenNLP. */
    public static SourceSpans of(SourceDocument source, Span[] spans) { return new SourceSpans(source, spans); }

    /** Run the actual SimpleTokenizer state machine on the retained immutable source. */
    public static SourceSpans simpleTokens(SourceDocument source) {
        Objects.requireNonNull(source, "source");
        return of(source, M3Tokenizers.simpleSpans(source.text()));
    }

    /** Run the actual trained model; model probabilities remain available on the caller's tokenizer. */
    public static SourceSpans modelTokens(SourceDocument source, TokenizerME tokenizer) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(tokenizer, "tokenizer");
        return of(source, tokenizer.tokenizePosImmutableView(source.text()));
    }

    /**
     * Run the existing CharSequence sentence detector. Its unchanged context generator may
     * materialize individual feature windows through the range descriptor; the source owner is
     * never asked to flatten or slice itself. Coordinates still refer to the full original text.
     */
    public static SourceSpans sentences(SourceDocument source, SentenceDetectorME detector) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(detector, "detector");
        return of(source, detector.sentPosDetect(source.view(0, source.length())));
    }

    public SourceDocument source() { return source; }
    public int size() { return starts.length; }
    public int start(int index) { return starts[Objects.checkIndex(index, starts.length)]; }
    public int end(int index) { return ends[Objects.checkIndex(index, ends.length)]; }
    public CharSequence view(int index) { return source.view(start(index), end(index)); }
    public int astOccurrence(int index) { return source.astOccurrence(start(index), end(index)); }
    /** Explicit copies for callers needing independent mutable coordinate arrays. */
    public int[] starts() { return starts.clone(); }
    public int[] ends() { return ends.clone(); }
}
