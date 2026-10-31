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

import java.util.Objects;
import java.util.concurrent.CancellationException;

import opennlp.tools.util.Span;

/** Metadata-first M3 entrypoints; existing Tokenizer interfaces and String methods are unchanged. */
public final class M3Tokenizers {
  private M3Tokenizers() { }

  /** Reuses SimpleTokenizer's exact character classes and UTF-16 offsets without copying text. */
  public static Span[] simpleSpans(CharSequence text) {
    return simpleSpans(text, null);
  }

  /**
   * The caller owns stable immutable backing and the existing SimpleTokenizer configuration.
   * The optional host checkpoint may check a progress monitor or throw on cancellation.
   * Existing whitespace-mode, punctuation, newline and surrogate semantics remain authoritative.
   */
  public static Span[] simpleSpans(CharSequence text, Runnable checkpoint) {
    Objects.requireNonNull(text, "text");
    check(checkpoint);
    CharSequence source = checkpoint == null ? text : new ObservedText(text, checkpoint);
    Span[] spans = SimpleTokenizer.INSTANCE.tokenizePosSequence(source);
    check(checkpoint);
    return spans;
  }

  private static void check(Runnable checkpoint) {
    if (Thread.currentThread().isInterrupted()) throw new CancellationException("tokenization interrupted");
    if (checkpoint != null) checkpoint.run();
  }

  private record ObservedText(CharSequence source, Runnable checkpoint) implements CharSequence {
    @Override public int length() { return source.length(); }
    @Override public char charAt(int index) {
      if ((index & 255) == 0) check(checkpoint);
      return source.charAt(index);
    }
    @Override public CharSequence subSequence(int start, int end) {
      throw new UnsupportedOperationException("span-only tokenization does not slice payloads");
    }
  }
}
