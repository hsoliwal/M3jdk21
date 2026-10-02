// SPDX-License-Identifier: Apache-2.0
package com.m3.migration;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JsonTest {
  private JsonTest() {}
  public static void main(String[] args) {
    Map<String, Object> value = new LinkedHashMap<>();
    value.put("unicode", "\0\ud83d\ude00\ud800\n\"\\");
    value.put("array", List.of(true, false, new BigDecimal("1.25"), "x"));
    value.put("null", null);
    if (!value.equals(Json.parse(Json.write(value)))) throw new AssertionError("JSON round trip");
    String[] bad = {"", "{}{}", "{\"x\":1,\"x\":2}", "[1,]", "{\"x\":}", "01", "+1", "NaN", "1e", "\"\\x\"", "\"\n\"", "[".repeat(70) + "]".repeat(70)};
    for (String text : bad) {
      try { Json.parse(text); throw new AssertionError("accepted malformed JSON: " + text); }
      catch (IllegalArgumentException expected) { /* required refusal */ }
    }
    if (!Json.parse("-2.3e+2").equals(new BigDecimal("-2.3e+2"))) throw new AssertionError("number");
    Map<String, Object> unordered = new LinkedHashMap<>();
    unordered.put("z", 1); unordered.put("a", 2);
    if (!Json.write(unordered).equals("{\"a\":2,\"z\":1}")) throw new AssertionError("object serialization must sort keys for cross-JVM plan identity");
    String nonAsciiEscape = "\"" + "\\" + "uFFＦＦ" + "\"";
    try { Json.parse(nonAsciiEscape); throw new AssertionError("accepted non-ASCII JSON escape digits"); }
    catch (IllegalArgumentException expected) { /* JSON hex digits are ASCII */ }
    System.out.println("JSON 16 cases passed");
  }
}
