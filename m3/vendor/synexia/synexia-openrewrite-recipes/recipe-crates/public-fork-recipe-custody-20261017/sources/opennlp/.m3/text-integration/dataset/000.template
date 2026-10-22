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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import opennlp.tools.util.StringUtil;

/**
 * A default {@link TokenContextGenerator} which produces events for maxent decisions
 * for tokenization.
 */
public class DefaultTokenContextGenerator implements TokenContextGenerator {

  protected final Set<String> inducedAbbreviations;

  /**
   * Initializes a plain {@link DefaultTokenContextGenerator} instance.
   */
  public DefaultTokenContextGenerator() {
    this(Collections.emptySet());
  }

  /**
   * Initializes a customized {@link DefaultTokenContextGenerator} instance via
   * a set of {@code inducedAbbreviations}.
   *
   * @param inducedAbbreviations The induced abbreviations to be used for this instance.
   */
  public DefaultTokenContextGenerator(Set<String> inducedAbbreviations) {
    this.inducedAbbreviations = inducedAbbreviations;
  }

  @Override
  public String[] getContext(String sentence, int index) {
    List<String> preds = createContext(sentence, index);
    String[] context = new String[preds.size()];
    preds.toArray(context);
    return context;
  }

  /**
   * Computes a {@link List} of features for the specified {@code sentence}
   * at the specified {@code index}. Extensions of {@link DefaultTokenContextGenerator}
   * can override this method to create a customized behaviour.
   *
   * @param sentence
   *          The sentence to create features for.
   * @param index
   *          The positional index. Must be a non-negative number or {@code 0}.
   * @return A {@link List} of features for the specified {@code sentence} string
   *         at the specified index.
   */
  protected List<String> createContext(String sentence, int index) {
    // At most 18 features in the built-in generator; retain every extension hook.
    List<String> preds = new ArrayList<>(18);
    String prefix = sentence.substring(0, index);
    String suffix = sentence.substring(index);
    preds.add("p=" + prefix);
    preds.add("s=" + suffix);
    if (index > 0) {
      addCharPreds("p1", sentence.charAt(index - 1), preds);
      if (index > 1) {
        addCharPreds("p2", sentence.charAt(index - 2), preds);
        preds.add("p21=" + sentence.charAt(index - 2) + sentence.charAt(index - 1));
      }
      else {
        preds.add("p2=bok");
      }
      preds.add("p1f1=" + sentence.charAt(index - 1) + sentence.charAt(index));
    }
    else {
      preds.add("p1=bok");
    }
    addCharPreds("f1", sentence.charAt(index), preds);
    if (index + 1 < sentence.length()) {
      addCharPreds("f2", sentence.charAt(index + 1), preds);
      preds.add("f12=" + sentence.charAt(index) + sentence.charAt(index + 1));
    }
    else {
      preds.add("f2=bok");
    }
    if (sentence.charAt(0) == '&' && sentence.charAt(sentence.length() - 1) == ';') {
      preds.add("cc");//character code
    }

    if (index == sentence.length() - 1 && inducedAbbreviations.contains(sentence)) {
      preds.add("pabb");
    }

    return preds;
  }


  /**
   * Helper function for {@link #createContext} that appends to a given {@code key}
   * a fixed text sequence depending on {@code c}. The resulting combination is added
   * to the given list {@code preds}.
   *
   * <p>
   * The {@code _ws} class uses {@link StringUtil#isWhitespace(char)}, whose result depends
   * on the active {@link opennlp.tools.util.WhitespaceMode}; the feature strings it produces
   * are part of every trained tokenizer model, so training and decoding must use the same
   * {@link opennlp.tools.util.WhitespaceMode}.
   *
   * @param key The input string to process.
   * @param c   A character used to discriminate which fixed text shall be appended.
   * @param preds The list into which the resulting combinations will be added.
   */
  protected void addCharPreds(String key, char c, List<String> preds) {
    preds.add(key + "=" + c);
    if (Character.isLetter(c)) {
      preds.add(categoryFeature(key, 0));
      if (Character.isUpperCase(c)) {
        preds.add(categoryFeature(key, 1));
      }
    }
    else if (Character.isDigit(c)) {
      preds.add(categoryFeature(key, 2));
    }
    else if (StringUtil.isWhitespace(c)) {
      preds.add(categoryFeature(key, 3));
    }
    else {
      if (c == '.' || c == '?' || c == '!') {
        preds.add(categoryFeature(key, 4));
      }
      else if (c == '`' || c == '"' || c == '\'') {
        preds.add(categoryFeature(key, 5));
      }
      else if (c == '[' || c == '{' || c == '(') {
        preds.add(categoryFeature(key, 6));
      }
      else if (c == ']' || c == '}' || c == ')') {
        preds.add(categoryFeature(key, 7));
      }
    }
  }

  /** Fixed model vocabulary reuses literal backing but keeps fresh String wrapper identity. */
  private static String categoryFeature(String key, int category) {
    if (key != null) {
      String known = switch (key) {
        case "p1" -> switch (category) {
          case 0 -> "p1_alpha";
          case 1 -> "p1_caps";
          case 2 -> "p1_num";
          case 3 -> "p1_ws";
          case 4 -> "p1_eos";
          case 5 -> "p1_quote";
          case 6 -> "p1_lp";
          case 7 -> "p1_rp";
          default -> throw new IllegalArgumentException("unknown character category");
        };
        case "p2" -> switch (category) {
          case 0 -> "p2_alpha";
          case 1 -> "p2_caps";
          case 2 -> "p2_num";
          case 3 -> "p2_ws";
          case 4 -> "p2_eos";
          case 5 -> "p2_quote";
          case 6 -> "p2_lp";
          case 7 -> "p2_rp";
          default -> throw new IllegalArgumentException("unknown character category");
        };
        case "f1" -> switch (category) {
          case 0 -> "f1_alpha";
          case 1 -> "f1_caps";
          case 2 -> "f1_num";
          case 3 -> "f1_ws";
          case 4 -> "f1_eos";
          case 5 -> "f1_quote";
          case 6 -> "f1_lp";
          case 7 -> "f1_rp";
          default -> throw new IllegalArgumentException("unknown character category");
        };
        case "f2" -> switch (category) {
          case 0 -> "f2_alpha";
          case 1 -> "f2_caps";
          case 2 -> "f2_num";
          case 3 -> "f2_ws";
          case 4 -> "f2_eos";
          case 5 -> "f2_quote";
          case 6 -> "f2_lp";
          case 7 -> "f2_rp";
          default -> throw new IllegalArgumentException("unknown character category");
        };
        default -> null;
      };
      if (known != null) return new String(known);
    }
    // Preserve protected addCharPreds for custom and null keys.
    String suffix = switch (category) {
      case 0 -> "_alpha";
      case 1 -> "_caps";
      case 2 -> "_num";
      case 3 -> "_ws";
      case 4 -> "_eos";
      case 5 -> "_quote";
      case 6 -> "_lp";
      case 7 -> "_rp";
      default -> throw new IllegalArgumentException("unknown character category");
    };
    return key + suffix;
  }
}
