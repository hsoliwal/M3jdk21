// Copyright 2026 Hitesh Soliwal and contributors
// Adapted from hsoliwal/com.synexia@50f48cbbe11f17fb8dc209bac3812f8940a1e267: MIndexGrammarLexer.java
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.parser.grammar;

import java.util.Arrays;
import java.util.Objects;

/** Generic zero-copy lexer driven by an immutable {@link M3LexerProfile}. */
public final class M3GrammarLexer {
  private static final char BACKTICK = 0x60;
  private final M3LexerProfile profile;
  private final M3GrammarLimits limits;

  public M3GrammarLexer(M3LexerProfile profile) {
    this(profile, M3GrammarLimits.defaults());
  }

  public M3GrammarLexer(M3LexerProfile profile, M3GrammarLimits limits) {
    this.profile = Objects.requireNonNull(profile, "profile");
    this.limits = Objects.requireNonNull(limits, "limits");
  }

  public M3LexerProfile profile() { return profile; }

  public M3TokenStream lex(CharSequence source) {
    return lex(source, new M3GrammarBudget(limits));
  }

  M3TokenStream lex(CharSequence input, M3GrammarBudget budget) {
    String source = budget.snapshot(input);
    M3TokenStream reused = profile.reuseTokens(source, limits, budget);
    if (reused != null) return reused;
    long remainingBeforeScan = budget.remainingWork();
    int capacity = Math.min(limits.maxTokens(), Math.max(8, Math.min(1024, source.length() >>> 2)));
    int[] starts = new int[capacity];
    int[] ends = new int[capacity];
    byte[] classes = new byte[capacity];
    int size = 0;
    int index = 0;
    while (index < source.length()) {
      budget.work(1);
      char ch = source.charAt(index);
      if (Character.isWhitespace(ch)) { index++; continue; }
      int comment = commentEnd(source, index, budget);
      if (comment >= 0) { index = comment; continue; }

      budget.tokens(size + 1);
      int start = index;
      M3TokenClass tokenClass;
      if (ch == '\'' && profile.singleQuoteString()) {
        index = quotedEnd(source, index, '\'', budget);
        tokenClass = M3TokenClass.STRING;
      } else if (ch == '"' && (profile.doubleQuoteString() || profile.doubleQuoteIdentifier())) {
        index = quotedEnd(source, index, '"', budget);
        tokenClass = profile.doubleQuoteIdentifier() ? M3TokenClass.QUOTED_IDENTIFIER : M3TokenClass.STRING;
      } else if (ch == BACKTICK && profile.backtickJsonLiteral()) {
        index = quotedEnd(source, index, BACKTICK, budget);
        tokenClass = M3TokenClass.JSON_LITERAL;
      } else if (ch == BACKTICK && profile.backtickIdentifier()) {
        index = quotedEnd(source, index, BACKTICK, budget);
        tokenClass = M3TokenClass.QUOTED_IDENTIFIER;
      } else if (ch == '[' && profile.bracketQuotedIdentifier()) {
        index = bracketQuotedEnd(source, index, budget);
        tokenClass = M3TokenClass.QUOTED_IDENTIFIER;
      } else if (ch == '#' && profile.hashNamePlaceholder() && hasIdentifierAfter(source, index)) {
        index = identifierEnd(source, index + 1, budget);
        tokenClass = M3TokenClass.NAME_PLACEHOLDER;
      } else if (ch == ':' && profile.colonValuePlaceholder() && hasIdentifierAfter(source, index)) {
        index = identifierEnd(source, index + 1, budget);
        tokenClass = M3TokenClass.VALUE_PLACEHOLDER;
      } else if (ch == '?' && profile.questionParameter()) {
        index++;
        while (index < source.length() && Character.isDigit(source.charAt(index))) {
          budget.work(1);
          index++;
        }
        tokenClass = M3TokenClass.POSITIONAL_PARAMETER;
      } else if (ch == ':' && profile.colonParameter() && hasIdentifierAfter(source, index)) {
        index = identifierEnd(source, index + 1, budget);
        tokenClass = M3TokenClass.POSITIONAL_PARAMETER;
      } else if (ch == '@' && profile.atParameter() && hasIdentifierAfter(source, index)) {
        index = identifierEnd(source, index + 1, budget);
        tokenClass = M3TokenClass.POSITIONAL_PARAMETER;
      } else if (ch == '$' && profile.dollarParameter() && hasIdentifierAfter(source, index)) {
        index = identifierEnd(source, index + 1, budget);
        tokenClass = M3TokenClass.POSITIONAL_PARAMETER;
      } else if (profile.hexNumbers() && isHexNumberStart(source, index)) {
        index = hexNumberEnd(source, index, budget);
        tokenClass = M3TokenClass.NUMBER;
      } else if (isNumberStart(source, index)) {
        index = numberEnd(source, index, budget);
        tokenClass = M3TokenClass.NUMBER;
      } else if (isIdentifierStart(ch)) {
        index = identifierEnd(source, index, budget);
        tokenClass = classifyWord(source, start, index, profile.specialNumberLiterals());
      } else {
        String operator = operatorAt(source, index, budget);
        index += operator == null ? 1 : operator.length();
        tokenClass = M3TokenClass.SYMBOL;
      }

      if (size == starts.length) {
        int next = M3GrammarBudget.capacity(starts.length, size + 1, limits.maxTokens());
        starts = Arrays.copyOf(starts, next);
        ends = Arrays.copyOf(ends, next);
        classes = Arrays.copyOf(classes, next);
      }
      starts[size] = start;
      ends[size] = index;
      classes[size] = (byte) tokenClass.ordinal();
      size++;
    }
    M3TokenStream result = new M3TokenStream(
        source, Arrays.copyOf(starts, size), Arrays.copyOf(ends, size), Arrays.copyOf(classes, size));
    if (limits.equals(budget.limits())) {
      profile.rememberTokens(result, limits, remainingBeforeScan - budget.remainingWork());
    }
    return result;
  }

  private int commentEnd(CharSequence source, int index, M3GrammarBudget budget) {
    if (profile.lineCommentDashDash() && startsWith(source, index, "--", budget)) return lineEnd(source, index + 2, budget);
    if (profile.lineCommentSlashSlash() && startsWith(source, index, "//", budget)) return lineEnd(source, index + 2, budget);
    if (profile.blockComments() && startsWith(source, index, "/*", budget)) {
      int cursor = index + 2;
      while (cursor + 1 < source.length()) {
        budget.work(1);
        if (source.charAt(cursor) == '*' && source.charAt(cursor + 1) == '/') return cursor + 2;
        cursor++;
      }
      throw new M3GrammarException("unterminated block comment at UTF-16 offset " + index);
    }
    return -1;
  }

  private static int lineEnd(CharSequence source, int index, M3GrammarBudget budget) {
    int cursor = index;
    while (cursor < source.length()) {
      budget.work(1);
      char ch = source.charAt(cursor++);
      if (ch == '\n' || ch == '\r') break;
    }
    return cursor;
  }

  private static int quotedEnd(CharSequence source, int index, char quote, M3GrammarBudget budget) {
    int cursor = index + 1;
    while (cursor < source.length()) {
      budget.work(1);
      char ch = source.charAt(cursor++);
      if (ch == '\\' && cursor < source.length()) { cursor++; continue; }
      if (ch == quote) {
        if (cursor < source.length() && source.charAt(cursor) == quote) { cursor++; continue; }
        return cursor;
      }
    }
    throw new M3GrammarException("unterminated quoted token at UTF-16 offset " + index);
  }

  private static int bracketQuotedEnd(CharSequence source, int index, M3GrammarBudget budget) {
    int cursor = index + 1;
    while (cursor < source.length()) {
      budget.work(1);
      char ch = source.charAt(cursor++);
      if (ch == ']') {
        if (cursor < source.length() && source.charAt(cursor) == ']') {
          cursor++;
          continue;
        }
        return cursor;
      }
    }
    throw new M3GrammarException(
        "unterminated bracket-quoted identifier at UTF-16 offset " + index);
  }

  private boolean hasIdentifierAfter(CharSequence source, int index) {
    return index + 1 < source.length() && isIdentifierStart(source.charAt(index + 1));
  }
  private int identifierEnd(CharSequence source, int index, M3GrammarBudget budget) {
    int cursor = index;
    while (cursor < source.length() && isIdentifierPart(source.charAt(cursor))) {
      budget.work(1);
      cursor++;
    }
    return cursor;
  }
  private boolean isIdentifierStart(char ch) {
    return Character.isUnicodeIdentifierStart(ch) || ch == '_' || (profile.dollarInIdentifier() && ch == '$');
  }
  private boolean isIdentifierPart(char ch) {
    return Character.isUnicodeIdentifierPart(ch) || ch == '_' || (profile.dollarInIdentifier() && ch == '$');
  }
  private static boolean isHexNumberStart(CharSequence source, int index) {
    return index + 2 < source.length()
        && source.charAt(index) == '0'
        && (source.charAt(index + 1) == 'x' || source.charAt(index + 1) == 'X')
        && isHexDigit(source.charAt(index + 2));
  }

  private static int hexNumberEnd(CharSequence source, int index, M3GrammarBudget budget) {
    int cursor = index + 2;
    while (cursor < source.length()) {
      budget.work(1);
      char ch = source.charAt(cursor);
      if (isHexDigit(ch) || ch == '_') cursor++;
      else break;
    }
    return cursor;
  }

  private static boolean isHexDigit(char ch) {
    return Character.isDigit(ch)
        || ch >= 'a' && ch <= 'f'
        || ch >= 'A' && ch <= 'F';
  }

  private static boolean isNumberStart(CharSequence source, int index) {
    char ch = source.charAt(index);
    return Character.isDigit(ch) || (ch == '.' && index + 1 < source.length() && Character.isDigit(source.charAt(index + 1)));
  }
  private static int numberEnd(CharSequence source, int index, M3GrammarBudget budget) {
    int cursor = index;
    boolean dot = false, exponent = false;
    if (source.charAt(cursor) == '.') { dot = true; cursor++; }
    while (cursor < source.length()) {
      budget.work(1);
      char ch = source.charAt(cursor);
      if (Character.isDigit(ch) || ch == '_') { cursor++; continue; }
      if (!dot && !exponent && ch == '.') { dot = true; cursor++; continue; }
      if (!exponent && (ch == 'e' || ch == 'E')) {
        exponent = true; cursor++;
        if (cursor < source.length() && (source.charAt(cursor) == '+' || source.charAt(cursor) == '-')) cursor++;
        continue;
      }
      break;
    }
    return cursor;
  }
  private static M3TokenClass classifyWord(
      CharSequence source, int start, int end, boolean specialNumbers) {
    if (equalsAsciiIgnoreCase(source, start, end, "true")
        || equalsAsciiIgnoreCase(source, start, end, "false")) {
      return M3TokenClass.BOOLEAN;
    }
    if (equalsAsciiIgnoreCase(source, start, end, "null")) {
      return M3TokenClass.NULL;
    }
    if (specialNumbers
        && (equalsAsciiSensitive(source, start, end, "Infinity")
            || equalsAsciiSensitive(source, start, end, "NaN"))) {
      return M3TokenClass.NUMBER;
    }
    return M3TokenClass.IDENTIFIER;
  }

  private static boolean equalsAsciiSensitive(
      CharSequence source, int start, int end, String value) {
    if (end - start != value.length()) return false;
    for (int i = 0; i < value.length(); i++) {
      if (source.charAt(start + i) != value.charAt(i)) return false;
    }
    return true;
  }
  private String operatorAt(CharSequence source, int index, M3GrammarBudget budget) {
    for (String operator : profile.operatorsForReadOnlyKernel()) if (startsWith(source, index, operator, budget)) return operator;
    return null;
  }
  private static boolean startsWith(CharSequence source, int index, String value, M3GrammarBudget budget) {
    budget.work(1L + value.length());
    if (index < 0 || value.length() > source.length() - index) return false;
    for (int i = 0; i < value.length(); i++) if (source.charAt(index + i) != value.charAt(i)) return false;
    return true;
  }
  private static boolean equalsAsciiIgnoreCase(CharSequence source, int start, int end, String value) {
    if (end - start != value.length()) return false;
    for (int i = 0; i < value.length(); i++) {
      char a = source.charAt(start + i), b = value.charAt(i);
      if (a == b) continue;
      if (a >= 'A' && a <= 'Z') a = (char) (a | 0x20);
      if (b >= 'A' && b <= 'Z') b = (char) (b | 0x20);
      if (a != b) return false;
    }
    return true;
  }
}
