/*
 * SPDX-License-Identifier: Apache-2.0
 * Additional M3 String compatibility tests. The Apache upstream tests are unchanged.
 */
package opennlp.tools.namefind;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class M3RegexTextBuilderTest {
  private static final String[] ATOMS = {
      "a", "b", " ", "\n", "\uD83D\uDE00", "\uD800", "\uDC00", "", "abc", null
  };
  private static final String[] EXPRESSIONS = {
      "\\b\\w+\\b", ".", "(?s).+", "a?", "\\s+", "(?<=a)b",
      "[\\uD800-\\uDBFF][\\uDC00-\\uDFFF]"
  };

  @Test
  void defaultUsesTheOriginalStringBuilderMatcherInput() {
    M3RegexTextBuilder fallback = new M3RegexTextBuilder(20, false);
    assertTrue(fallback.input() instanceof StringBuilder);
    fallback.append("before").append(' ').append("after");
    assertEquals("before after", fallback.toString());
    assertEquals("before after", fallback.input().toString());
  }

  @Test
  void compositeRetainsCodeUnitsAndMatcherResults() {
    check(new String[0]);
    check(new String[]{null, "", "a", "\uD83D\uDE00", "b", "", ""});
    check(new String[]{"a", "", "b", "", "c"});
    Random random = new Random(314159L);
    for (int seed = 0; seed < 200; seed++) {
      String[] tokens = new String[random.nextInt(8)];
      for (int part = 0; part < tokens.length; part++) {
        tokens[part] = ATOMS[random.nextInt(ATOMS.length)];
      }
      check(tokens);
    }
  }

  @Test
  void boundsMatchTheCharacterSequenceContract() {
    M3RegexTextBuilder composite = new M3RegexTextBuilder(8, true);
    composite.append("ab");
    assertThrows(IndexOutOfBoundsException.class, () -> composite.charAt(-1));
    assertThrows(IndexOutOfBoundsException.class, () -> composite.charAt(2));
    assertThrows(IndexOutOfBoundsException.class, () -> composite.subSequence(2, 1));
    assertThrows(IndexOutOfBoundsException.class, () -> composite.subSequence(0, 3));
  }

  private static void check(String[] tokens) {
    StringBuilder original = new StringBuilder(tokens.length * 10);
    M3RegexTextBuilder segmented = new M3RegexTextBuilder(tokens.length * 10, true);
    assertSame(segmented, segmented.input());
    for (int i = 0; i < tokens.length; i++) {
      original.append(tokens[i]);
      segmented.append(tokens[i]);
      if (i + 1 < tokens.length) {
        original.append(' ');
        segmented.append(' ');
      }
    }

    assertEquals(original.length(), segmented.length());
    assertEquals(original.toString(), segmented.toString());
    for (int i = 0; i < original.length(); i++) {
      assertEquals(original.charAt(i), segmented.charAt(i), "UTF-16 unit " + i);
    }
    for (int begin = 0; begin <= original.length(); begin++) {
      for (int end = begin; end <= original.length(); end++) {
        assertEquals(original.substring(begin, end),
            segmented.subSequence(begin, end).toString(), "range " + begin + ":" + end);
      }
    }

    for (String expression : EXPRESSIONS) {
      Pattern pattern = Pattern.compile(expression);
      Matcher expected = pattern.matcher(original);
      Matcher actual = pattern.matcher(segmented.input());
      while (true) {
        boolean left = expected.find();
        boolean right = actual.find();
        assertEquals(left, right, expression);
        if (!left) break;
        assertEquals(expected.start(), actual.start(), expression);
        assertEquals(expected.end(), actual.end(), expression);
        assertEquals(expected.group(), actual.group(), expression);
      }
      assertEquals(pattern.matcher(original).replaceAll("_"),
          pattern.matcher(segmented.input()).replaceAll("_"), expression);
    }
  }
}
