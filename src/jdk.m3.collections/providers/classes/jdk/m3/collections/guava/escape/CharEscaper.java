/*
 * Copyright (C) 2006 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package jdk.m3.collections.guava.escape;

import static jdk.m3.collections.guava.base.Preconditions.checkNotNull;

import jdk.m3.collections.guava.annotations.GwtCompatible;

/**
 * An object that converts literal text into a format safe for inclusion in a particular context
 * (such as an XML document). Typically (but not always), the inverse process of "unescaping" the
 * text is performed automatically by the relevant parser.
 *
 * <p>For example, an XML escaper would convert the literal string {@code "Foo<Bar>"} into {@code
 * "Foo&lt;Bar&gt;"} to prevent {@code "<Bar>"} from being confused with an XML tag. When the
 * resulting XML document is parsed, the parser API will return this text as the original literal
 * string {@code "Foo<Bar>"}.
 *
 * <p>A {@code CharEscaper} instance is required to be stateless, and safe when used concurrently by
 * multiple threads.
 *
 * <p>Popular escapers are defined as constants in classes like {@link
 * jdk.m3.collections.guava.html.HtmlEscapers} and {@link jdk.m3.collections.guava.xml.XmlEscapers}. To create
 * your own escapers extend this class and implement the {@link #escape(char)} method.
 *
 * @author Sven Mawson
 * @since 15.0
 */
@GwtCompatible
@SuppressWarnings("EscapedEntity") // We do mean for the user to see "&lt;" etc.
public abstract class CharEscaper extends Escaper {
  /** Constructor for use by subclasses. */
  protected CharEscaper() {}

  /**
   * Returns the escaped form of a given literal string.
   *
   * @param string the literal string to be escaped
   * @return the escaped form of {@code string}
   * @throws NullPointerException if {@code string} is null
   */
  @Override
  public String escape(String string) {
    checkNotNull(string); // GWT specific check (do not optimize)
    // Inlineable fast-path loop which hands off to escapeSlow() only if needed
    int length = string.length();
    for (int index = 0; index < length; index++) {
      if (escape(string.charAt(index)) != null) {
        return escapeSlow(string, index);
      }
    }
    return string;
  }

  /**
   * Returns the escaped form of the given character, or {@code null} if this character does not
   * need to be escaped. If an empty array is returned, this effectively strips the input character
   * from the resulting text.
   *
   * <p>If the character does not need to be escaped, this method should return {@code null}, rather
   * than a one-character array containing the character itself. This enables the escaping algorithm
   * to perform more efficiently.
   *
   * <p>An escaper is expected to be able to deal with any {@code char} value, so this method should
   * not throw any exceptions.
   *
   * @param c the character to escape if necessary
   * @return the replacement characters, or {@code null} if no escaping was needed
   */
  protected abstract char [] escape(char c);

  final char [] escapeInternal(char c) {
    return escape(c);
  }

  /**
   * Returns the escaped form of a given literal string, starting at the given index. This method is
   * called by the {@link #escape(String)} method when it discovers that escaping is required. It is
   * protected to allow subclasses to override the fastpath escaping function to inline their
   * escaping test. See {@link CharEscaperBuilder} for an example usage.
   *
   * @param s the literal string to be escaped
   * @param index the index to start escaping from
   * @return the escaped form of {@code string}
   * @throws NullPointerException if {@code string} is null
   */
  protected final String escapeSlow(String s, int index) {
    int slen = s.length();
    char[] dest = Platform.charBufferFromThreadLocal();
    int cap = dest.length; //FLLDCIM: CAPACITY_LANE - the length the pre-change code held
    int destIndex = 0;
    int lastEscape = 0;
    for (; index < slen; index++) {
      char[] r = escape(s.charAt(index));
      if (r != null) {
        cap = shadowCap(cap, destIndex + (index - lastEscape) + r.length, slen - index);
        dest = Platform.reserve(dest, destIndex, index - lastEscape, r.length, cap);
        destIndex = Platform.append(s, lastEscape, index, r, dest, destIndex);
        lastEscape = index + 1;
      }
    }
    return Platform.finish(s, lastEscape, dest, destIndex, cap);
  }

  /**
   * The capacity lane: the destination length the pre-change escapeSlow holds after this escape.
   * Same growth points and int arithmetic, so the overflow AssertionError is thrown at the same
   * escape, before any allocation or copy.
   */
  //FLLDCIM: CAPACITY_LANE - pre-change growth rule replayed on one int, no allocation
  private static int shadowCap(int cap, int sizeNeeded, int remaining) {
    if (cap >= sizeNeeded) {
      return cap;
    }
    return Platform.checkedSize(sizeNeeded + DEST_PAD_MULTIPLIER * remaining);
  }

  /** The multiplier for padding to use when growing the escape buffer. */
  private static final int DEST_PAD_MULTIPLIER = 2;
}
