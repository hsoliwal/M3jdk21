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

/**
 * Package-local backport of the three OpenNLP 3.0 ParamChecks atoms used by
 * WordpieceEncoder, SubwordPiece and MappedText. Exception types and messages
 * retain the donor contract; unrelated validation APIs are not imported.
 *
 * <p>Adapted by Hitesh Soliwal and Contributors to the Synexia Project, 2026.
 * See the integration-20261018 OpenNLP PROVENANCE.json for exact donor custody.</p>
 */
final class WordpieceChecks {
  private WordpieceChecks() { }

  static <T> T requireNonNullArg(T value, String name) {
    if (value == null) {
      throw new IllegalArgumentException(name + " must not be null");
    }
    return value;
  }

  static <T extends CharSequence> T requireNonEmpty(T value, String name) {
    if (value == null || value.isEmpty()) {
      throw new IllegalArgumentException(name + " must not be null or empty");
    }
    return value;
  }

  static int requireNonNegative(int value, String name) {
    if (value < 0) {
      throw new IllegalArgumentException(name + " must not be negative");
    }
    return value;
  }
}
