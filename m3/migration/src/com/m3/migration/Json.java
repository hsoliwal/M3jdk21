// SPDX-License-Identifier: Apache-2.0
package com.m3.migration;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Dependency-free, bounded JSON syntax adapter for migration tooling, not a runtime dependency. */
final class Json {
  private static final int MAX_CHARS = 16 * 1024 * 1024;
  private final String text;
  private int at;
  private Json(String text) {
    if (text.length() > MAX_CHARS) throw new IllegalArgumentException("JSON exceeds 16 Mi characters");
    this.text = text;
  }
  static Object parse(String text) {
    Json parser = new Json(text);
    Object result = parser.value(0);
    parser.space();
    if (parser.at != text.length()) throw parser.bad("trailing input");
    return result;
  }
  static Map<String, Object> object(Object value) {
    if (!(value instanceof Map<?, ?> map)) throw new IllegalArgumentException("expected object");
    Map<String, Object> result = new LinkedHashMap<>();
    map.forEach((key, item) -> {
      if (!(key instanceof String name)) throw new IllegalArgumentException("non-string object key");
      result.put(name, item);
    });
    return result;
  }
  static List<Object> array(Object value) {
    if (!(value instanceof List<?> list)) throw new IllegalArgumentException("expected array");
    return new ArrayList<>(list);
  }
  static String string(Object value) {
    if (!(value instanceof String text)) throw new IllegalArgumentException("expected string");
    return text;
  }
  static int integer(Object value) {
    if (!(value instanceof BigDecimal number)) throw new IllegalArgumentException("expected integer");
    try { return number.intValueExact(); }
    catch (ArithmeticException error) { throw new IllegalArgumentException("integer out of range", error); }
  }
  static String write(Object value) {
    StringBuilder out = new StringBuilder();
    append(value, out, 0);
    return out.toString();
  }
  private static void append(Object value, StringBuilder out, int depth) {
    if (depth > 64) throw new IllegalArgumentException("JSON nesting exceeds 64");
    if (value == null) out.append("null");
    else if (value instanceof String text) quote(text, out);
    else if (value instanceof Boolean || value instanceof BigDecimal
        || value instanceof Integer || value instanceof Long) out.append(value);
    else if (value instanceof Map<?, ?> map) {
      out.append('{'); boolean first = true;
      for (var entry : new java.util.TreeMap<>(object(map)).entrySet()) {
        if (!first) out.append(','); first = false;
        quote(string(entry.getKey()), out); out.append(':'); append(entry.getValue(), out, depth + 1);
      }
      out.append('}');
    } else if (value instanceof List<?> list) {
      out.append('['); boolean first = true;
      for (Object item : list) {
        if (!first) out.append(','); first = false; append(item, out, depth + 1);
      }
      out.append(']');
    } else throw new IllegalArgumentException("unsupported JSON value type");
  }
  private static void quote(String text, StringBuilder out) {
    out.append('"');
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (c == '"' || c == '\\') out.append('\\').append(c);
      else if (c < 32 || Character.isSurrogate(c)) {
        out.append("\\u");
        for (int shift = 12; shift >= 0; shift -= 4) out.append("0123456789abcdef".charAt((c >>> shift) & 15));
      } else out.append(c);
    }
    out.append('"');
  }
  private Object value(int depth) {
    if (depth > 64) throw bad("nesting exceeds 64");
    space();
    if (at == text.length()) throw bad("missing value");
    return switch (text.charAt(at)) {
      case '{' -> objectValue(depth + 1);
      case '[' -> arrayValue(depth + 1);
      case '"' -> stringValue();
      case 't' -> literal("true", Boolean.TRUE);
      case 'f' -> literal("false", Boolean.FALSE);
      case 'n' -> literal("null", null);
      default -> number();
    };
  }
  private Map<String, Object> objectValue(int depth) {
    at++; space(); Map<String, Object> result = new LinkedHashMap<>();
    if (take('}')) return result;
    for (;;) {
      space(); if (at == text.length() || text.charAt(at) != '"') throw bad("missing object key");
      String key = stringValue();
      if (result.containsKey(key)) throw bad("duplicate object key: " + key);
      space(); require(':'); result.put(key, value(depth)); space();
      if (take('}')) return result;
      require(',');
    }
  }
  private List<Object> arrayValue(int depth) {
    at++; space(); List<Object> result = new ArrayList<>();
    if (take(']')) return result;
    for (;;) {
      result.add(value(depth)); space(); if (take(']')) return result; require(',');
    }
  }
  private String stringValue() {
    require('"'); StringBuilder out = new StringBuilder();
    while (at < text.length()) {
      char c = text.charAt(at++);
      if (c == '"') return out.toString();
      if (c < 32) throw bad("unescaped control character");
      if (c != '\\') { out.append(c); continue; }
      if (at == text.length()) throw bad("incomplete escape");
      char escape = text.charAt(at++);
      switch (escape) {
        case '"', '\\', '/' -> out.append(escape);
        case 'b' -> out.append('\b'); case 'f' -> out.append('\f');
        case 'n' -> out.append('\n'); case 'r' -> out.append('\r'); case 't' -> out.append('\t');
        case 'u' -> {
          if (text.length() - at < 4) throw bad("short Unicode escape");
          int unit = 0;
          for (int i = 0; i < 4; i++) {
            char hex = text.charAt(at++);
            int digit = hex < 128 ? Character.digit(hex, 16) : -1;
            if (digit < 0) throw bad("bad Unicode escape");
            unit = (unit << 4) | digit;
          }
          out.append((char) unit);
        }
        default -> throw bad("unknown escape");
      }
    }
    throw bad("unterminated string");
  }
  private Object literal(String token, Object value) {
    if (!text.startsWith(token, at)) throw bad("invalid literal");
    at += token.length(); return value;
  }
  private BigDecimal number() {
    int start = at;
    take('-');
    if (!take('0')) digits();
    if (take('.')) digits();
    if (take('e') || take('E')) { if (!take('+')) take('-'); digits(); }
    try { return new BigDecimal(text.substring(start, at)); }
    catch (NumberFormatException error) { throw bad("invalid number"); }
  }
  private void digits() {
    int start = at;
    while (at < text.length() && text.charAt(at) >= '0' && text.charAt(at) <= '9') at++;
    if (start == at) throw bad("missing digit");
  }
  private void space() {
    while (at < text.length() && " \n\r\t".indexOf(text.charAt(at)) >= 0) at++;
  }
  private boolean take(char expected) {
    if (at < text.length() && text.charAt(at) == expected) { at++; return true; }
    return false;
  }
  private void require(char expected) { if (!take(expected)) throw bad("expected " + expected); }
  private IllegalArgumentException bad(String message) { return new IllegalArgumentException(message + " at " + at); }
}
