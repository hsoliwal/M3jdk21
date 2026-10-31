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
package org.apache.lucene.analysis;

import java.io.Reader;
import java.util.Objects;

/** M3 text ingress using Lucene's existing reader owner, without flattening a composite. */
public final class M3TextReaders {
  private M3TextReaders() {}

  /**
   * Borrows a stable, immutable UTF-16 sequence for an Analyzer/Tokenizer Reader boundary. The
   * caller owns the backing lifetime; no text, lexicon or intern pool is duplicated. Closing the
   * reader releases its reference. The returned reader follows Lucene's existing reusable-reader
   * EOF/close semantics, including read-after-close returning EOF.
   */
  public static Reader reader(CharSequence text) {
    Objects.requireNonNull(text, "text");
    ReusableStringReader reader = new ReusableStringReader();
    reader.setValue(text);
    return reader;
  }
}
