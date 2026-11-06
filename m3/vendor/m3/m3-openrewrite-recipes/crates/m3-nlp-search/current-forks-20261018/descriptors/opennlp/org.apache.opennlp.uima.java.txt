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

open module org.apache.opennlp.uima {
  requires transitive org.apache.opennlp.tools;
  requires transitive uimaj.core;
  requires org.slf4j;

  exports opennlp.uima.chunker;
  exports opennlp.uima.dictionary;
  exports opennlp.uima.doccat;
  exports opennlp.uima.namefind;
  exports opennlp.uima.normalizer;
  exports opennlp.uima.parser;
  exports opennlp.uima.postag;
  exports opennlp.uima.sentdetect;
  exports opennlp.uima.tokenize;
  exports opennlp.uima.util;
}
