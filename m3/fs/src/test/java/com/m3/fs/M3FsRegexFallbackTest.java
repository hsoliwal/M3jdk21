// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.fs;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Differential tests use the untouched public RE2J artifact only as a test oracle. */
class M3FsRegexFallbackTest {
  @TempDir Path temp;
  @BeforeAll static void requireNative() {
    assertTrue(Boolean.getBoolean("m3.fs.test"), "native test profile must be explicit");
    assertTrue(M3FS.nativeAvailable(), "M3FS JNI is required");
  }

  /** Test-only portable images for one/two dots, anchors and dotall; no runtime parser. */
  static M3FsRegexImage dots(String expression, M3FsRegexImage.Semantics semantics) {
    boolean begin = expression.startsWith("\\A"), end = expression.endsWith("\\z");
    boolean all = expression.contains("?s:") || expression.equals("[\\s\\S]");
    int length = expression.equals("..") ? 2 : 1;
    int count = length + 1 + (begin ? 1 : 0) + (end ? 1 : 0);
    byte[] op = new byte[count]; int[] out = new int[count], other = new int[count], classes = new int[count];
    Arrays.fill(out, -1); Arrays.fill(other, -1); Arrays.fill(classes, -1);
    long low = -1L, high = -1L;
    char[] starts, ends;
    if (all || semantics == M3FsRegexImage.Semantics.RE2) {
      if (!all) low &= ~(1L << '\n');
      starts = new char[] {128}; ends = new char[] {65535};
    } else {
      low &= ~(1L << '\n') & ~(1L << '\r');
      starts = new char[] {128, 134, 8234}; ends = new char[] {132, 8231, 65535};
    }
    int state = 0;
    if (begin) { op[state] = 4; out[state] = state + 1; state++; }
    for (int i = 0; i < length; i++, state++) { op[state] = 1; out[state] = state + 1; classes[state] = 0; }
    if (end) { op[state] = 5; out[state] = state + 1; state++; }
    op[state] = 6;
    return M3FsRegexImage.fromImage(new M3FsRegexImage.Image(expression, semantics, op, out, other,
        classes, new long[] {low}, new long[] {high}, new int[] {0}, new int[] {starts.length},
        starts, ends, 0, state, length, length, 0, 0, 0, true));
  }

  private static List<String> corpus() {
    List<String> values = new ArrayList<>(List.of("", "a", "ab", "\n", "\r", "\u0085", "\u2028", "\u2029",
        "\ud83d\ude00", "\ud800", "\udfff", "a\ud83d\ude00", "\ud83d\ude00a", "\ud83d\ude00\ud83d\ude03", "\udfff\ud800"));
    char[] alphabet = {'a', 'b', '0', '\n', '\r', '\u0085', '\u2028', '\u00e9', '\u03c0', '\ud800', '\ud83d', '\ude00', '\udfff'};
    Random random = new Random(0x3f5a11);
    for (int i = 0; i < 256; i++) {
      char[] value = new char[random.nextInt(9)];
      for (int j = 0; j < value.length; j++) value[j] = alphabet[random.nextInt(alphabet.length)];
      values.add(new String(value));
    }
    return values;
  }

  @Test void defaultFallbackMatchesUntouchedBackendsForEveryModeAndUtf16Boundary() throws Exception {
    List<String> values = corpus(); M3FsIndexImage image = M3FsIndexImage.of(values.toArray(String[]::new));
    try (M3FS fs = M3FS.create(temp.resolve("differential.sqlite"))) {
      fs.createMIndexFile("/values", 0644, image);
      for (M3FsRegexImage.Semantics semantics : M3FsRegexImage.Semantics.values()) {
        for (String expression : List.of(".", "..", "\\A.\\z", "[\\s\\S]")) {
          M3FsRegexImage program = dots(expression, semantics);
          var jdk = java.util.regex.Pattern.compile(expression);
          var re2 = com.google.re2j.Pattern.compile(expression);
          for (M3FS.RegexMode mode : M3FS.RegexMode.values()) {
            var raw = fs.nativeRegex("/values", program, mode);
            assertTrue(raw.fallbackCount() > 0, "fixture must exercise unsupported native values");
            var resolved = fs.nativeRegex("/values", program, mode, image);
            assertEquals(0, resolved.fallbackCount());
            for (int i = 0; i < values.size(); i++) {
              boolean expected;
              if (semantics == M3FsRegexImage.Semantics.JDK) {
                var matcher = jdk.matcher(values.get(i));
                expected = switch (mode) { case MATCHES -> matcher.matches(); case FIND -> matcher.find(); case LOOKING_AT -> matcher.lookingAt(); };
              } else {
                var matcher = re2.matcher(values.get(i));
                expected = switch (mode) { case MATCHES -> matcher.matches(); case FIND -> matcher.find(); case LOOKING_AT -> matcher.lookingAt(); };
              }
              assertEquals(expected, resolved.matches(i), semantics + " " + expression + " " + mode + " row " + i);
              if (!raw.requiresFallback(i)) assertEquals(raw.matches(i), resolved.matches(i));
            }
          }
        }
      }
    }
  }

  @Test void re2SelectionDoesNotSubstituteJdkSemanticsAndCallerOverrideStillOwnsAnswers() throws Exception {
    M3FsIndexImage image = M3FsIndexImage.of("\ud83d\ude00\r");
    try (M3FS fs = M3FS.create(temp.resolve("selection.sqlite"))) {
      fs.createMIndexFile("/values", 0644, image);
      assertFalse(fs.nativeRegex("/values", dots("..", M3FsRegexImage.Semantics.JDK), M3FS.RegexMode.MATCHES, image).matches(0));
      assertTrue(fs.nativeRegex("/values", dots("..", M3FsRegexImage.Semantics.RE2), M3FS.RegexMode.MATCHES, image).matches(0));
      int[] calls = {0};
      var overridden = fs.nativeRegex("/values", dots("..", M3FsRegexImage.Semantics.RE2), M3FS.RegexMode.MATCHES, image,
          (value, mode) -> { calls[0]++; return false; });
      assertEquals(1, calls[0]); assertFalse(overridden.matches(0)); assertEquals(0, overridden.fallbackCount());
    }
  }

  @Test void fallbackCompilationRemainsLazyAndImageAndNativeAdmissionKeepPrecedence() throws Exception {
    M3FsIndexImage ascii = M3FsIndexImage.of("a"), supplementary = M3FsIndexImage.of("\ud83d\ude00");
    try (M3FS fs = M3FS.create(temp.resolve("admission.sqlite"))) {
      fs.createMIndexFile("/ascii", 0644, ascii); fs.createMIndexFile("/supplementary", 0644, supplementary);
      for (M3FsRegexImage.Semantics semantics : M3FsRegexImage.Semantics.values()) {
        var image = dots(".", semantics).image();
        var invalidExpression = M3FsRegexImage.fromImage(new M3FsRegexImage.Image("[", semantics, image.op(), image.out1(), image.out2(),
            image.classId(), image.asciiLow(), image.asciiHigh(), image.rangeOffset(), image.rangeCount(), image.rangeLow(), image.rangeHigh(),
            image.startState(), image.matchState(), image.minLength(), image.maxLength(), 0, 0, 0, true));
        assertTrue(fs.nativeRegex("/ascii", invalidExpression, M3FS.RegexMode.MATCHES, ascii).matches(0));
        assertThrows(IllegalArgumentException.class, () -> fs.nativeRegex("/supplementary", invalidExpression, M3FS.RegexMode.MATCHES, ascii));
        assertThrows(IOException.class, () -> fs.nativeRegex("/missing", invalidExpression, M3FS.RegexMode.MATCHES, supplementary));
        if (semantics == M3FsRegexImage.Semantics.JDK)
          assertThrows(java.util.regex.PatternSyntaxException.class, () -> fs.nativeRegex("/supplementary", invalidExpression, M3FS.RegexMode.MATCHES, supplementary));
        else assertThrows(RuntimeException.class, () -> fs.nativeRegex("/supplementary", invalidExpression, M3FS.RegexMode.MATCHES, supplementary));
      }
    }
  }

  @Test void readOnlyAndRetainedSnapshotUseTheAutomaticRe2Backend() throws Exception {
    Path database = temp.resolve("views.sqlite"); M3FsIndexImage original = M3FsIndexImage.of("\ud83d\ude00");
    M3FsRegexImage program = dots(".", M3FsRegexImage.Semantics.RE2);
    try (M3FS fs = M3FS.create(database)) {
      fs.createMIndexFile("/values", 0644, original);
      try (M3FS snapshot = M3FS.openSnapshot(database); M3FS readOnly = M3FS.openReadOnly(database)) {
        assertTrue(readOnly.nativeRegex("/values", program, M3FS.RegexMode.MATCHES, original).matches(0));
        fs.writeMIndexImage("/values", M3FsIndexImage.of("ab"));
        assertTrue(snapshot.nativeRegex("/values", program, M3FS.RegexMode.MATCHES, original).matches(0));
        assertThrows(IllegalArgumentException.class, () -> readOnly.nativeRegex("/values", program, M3FS.RegexMode.MATCHES, original));
      }
    }
  }
}
