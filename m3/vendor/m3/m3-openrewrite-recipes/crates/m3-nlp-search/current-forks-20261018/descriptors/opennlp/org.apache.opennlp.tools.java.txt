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

open module org.apache.opennlp.tools {
  requires transitive java.xml;
  requires transitive org.slf4j;
  requires org.apache.commons.xml.secure;
  requires io.github.classgraph;
  requires transitive zlibsvm.core;

  exports opennlp.tools.chunker;
  exports opennlp.tools.cmdline;
  exports opennlp.tools.cmdline.chunker;
  exports opennlp.tools.cmdline.depparse;
  exports opennlp.tools.cmdline.dictionary;
  exports opennlp.tools.cmdline.doccat;
  exports opennlp.tools.cmdline.entitylinker;
  exports opennlp.tools.cmdline.langdetect;
  exports opennlp.tools.cmdline.languagemodel;
  exports opennlp.tools.cmdline.lemmatizer;
  exports opennlp.tools.cmdline.namefind;
  exports opennlp.tools.cmdline.params;
  exports opennlp.tools.cmdline.parser;
  exports opennlp.tools.cmdline.postag;
  exports opennlp.tools.cmdline.sentdetect;
  exports opennlp.tools.cmdline.sentiment;
  exports opennlp.tools.cmdline.stopword;
  exports opennlp.tools.cmdline.tokenizer;
  exports opennlp.tools.commons;
  exports opennlp.tools.depparse;
  exports opennlp.tools.dictionary;
  exports opennlp.tools.dictionary.serializer;
  exports opennlp.tools.doccat;
  exports opennlp.tools.document;
  exports opennlp.tools.embeddings;
  exports opennlp.tools.entitylinker;
  exports opennlp.tools.formats;
  exports opennlp.tools.formats.ad;
  exports opennlp.tools.formats.brat;
  exports opennlp.tools.formats.conllu;
  exports opennlp.tools.formats.convert;
  exports opennlp.tools.formats.frenchtreebank;
  exports opennlp.tools.formats.irishsentencebank;
  exports opennlp.tools.formats.leipzig;
  exports opennlp.tools.formats.letsmt;
  exports opennlp.tools.formats.masc;
  exports opennlp.tools.formats.moses;
  exports opennlp.tools.formats.muc;
  exports opennlp.tools.formats.nkjp;
  exports opennlp.tools.formats.ontonotes;
  exports opennlp.tools.langdetect;
  exports opennlp.tools.languagemodel;
  exports opennlp.tools.lemmatizer;
  exports opennlp.tools.log;
  exports opennlp.tools.ml;
  exports opennlp.tools.ml.libsvm.doccat;
  exports opennlp.tools.ml.maxent;
  exports opennlp.tools.ml.maxent.io;
  exports opennlp.tools.ml.maxent.quasinewton;
  exports opennlp.tools.ml.model;
  exports opennlp.tools.ml.naivebayes;
  exports opennlp.tools.ml.perceptron;
  exports opennlp.tools.models;
  exports opennlp.tools.models.classgraph;
  exports opennlp.tools.models.dir;
  exports opennlp.tools.models.simple;
  exports opennlp.tools.monitoring;
  exports opennlp.tools.namefind;
  exports opennlp.tools.ngram;
  exports opennlp.tools.parser;
  exports opennlp.tools.parser.chunking;
  exports opennlp.tools.parser.lang.en;
  exports opennlp.tools.parser.lang.es;
  exports opennlp.tools.parser.treeinsert;
  exports opennlp.tools.postag;
  exports opennlp.tools.sentdetect;
  exports opennlp.tools.sentdetect.lang;
  exports opennlp.tools.sentdetect.lang.th;
  exports opennlp.tools.sentiment;
  exports opennlp.tools.stemmer;
  exports opennlp.tools.stemmer.hunspell;
  exports opennlp.tools.stemmer.snowball;
  exports opennlp.tools.stopword;
  exports opennlp.tools.termvector;
  exports opennlp.tools.tokenize;
  exports opennlp.tools.tokenize.lang;
  exports opennlp.tools.tokenize.lang.en;
  exports opennlp.tools.tokenize.lattice;
  exports opennlp.tools.tokenize.uax29;
  exports opennlp.tools.util;
  exports opennlp.tools.util.archive;
  exports opennlp.tools.util.eval;
  exports opennlp.tools.util.ext;
  exports opennlp.tools.util.featuregen;
  exports opennlp.tools.util.java;
  exports opennlp.tools.util.jvm;
  exports opennlp.tools.util.model;
  exports opennlp.tools.util.normalizer;
  exports opennlp.tools.util.wordvector;

  uses opennlp.tools.document.DocumentAnnotatorProvider;
  uses opennlp.tools.embeddings.TextEmbedderProvider;
  provides opennlp.tools.document.DocumentAnnotatorProvider
      with opennlp.tools.stemmer.StemmerAnnotatorProvider;
}
