// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.indexstring;

import java.io.BufferedOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Random;

/**
 * Differential dump driver of recipe/character-flags-20261106 (not a test, never run in the test phase): replays the
 * same inputs through the character-flags class named by {@code args[0]} (donor or relocated, by reflection) and
 * prints every result as ASCII, so the two dumps can be compared byte for byte. Sections: declared modifiers and
 * constants, the smoke oracle's 16 edge strings ({@code of}, {@code labels}, 256 pairs, 4,096 ordered triples), the
 * exhaustive {@code combine} table over the whole 10-bit flag space (1,024 x 1,024 = 1,048,576 ordered pairs, every
 * valid input), {@code labels} over the 1,024 valid values, 1,000,000 seeded random pairs mixing valid, high-bit,
 * arbitrary and negative ints (result or IAE), and 100,000 seeded random UTF-16 texts through {@code of}/{@code labels}.
 * {@code args[1] == "of-only"} restricts the dump to the {@code of}/{@code labels} sections, for a class without
 * {@code combine} (the pre-combine sibling jdk.m3.file.M3CharacterFlags).
 */
//FLLDCIM: RELOCATE - differential driver of recipe m3/ports/indexstring/recipe/character-flags-20261106 (gate 3); test scope tool, Claude-authored, no production code
public final class M3IndexCharacterFlagsDump {
  private static final String[] CASES = {
      "", " ", "\t\n", "\r\n", "A9_", "alpha", "é", "Ā", "Ω", "😀",
      "a😀b", " é ", String.valueOf(Character.MIN_HIGH_SURROGATE),
      String.valueOf(Character.MIN_LOW_SURROGATE), " ", " "};
  private static final String[] CONSTANTS = {"ASCII", "LATIN1", "HAS_ASCII_UPPER", "HAS_ASCII_LOWER",
      "HAS_SURROGATE", "BLANK", "EMPTY", "HAS_ASCII_DIGIT", "HAS_ASCII_WORD", "HAS_ASCII_SPACE", "REGEX_PRESENCE", "ALL"};
  private static final int FLAG_BITS = 10;
  private static final int FLAG_SPACE = 1 << FLAG_BITS;
  private static final int HIGH_BITS = Integer.SIZE - FLAG_BITS;
  private static final long SEED = 0x4d33464c414753L;
  private static final int RANDOM_PAIRS = 1_000_000;
  private static final int RANDOM_TEXTS = 100_000;
  private static final int MAX_TEXT_UNITS = 40;
  private static final int ASCII_LIMIT = 128;
  private static final int UNIT_LIMIT = 1 << Character.SIZE;
  private static final String IAE = "IAE";
  private static final String OF_ONLY = "of-only";

  private final Class<?> type;
  private final Method of;
  private final Method labels;
  private final Method combine;
  private final PrintStream out;
  private int iaeCount;

  private M3IndexCharacterFlagsDump(Class<?> type, boolean ofOnly, PrintStream out) throws NoSuchMethodException {
    this.type = type;
    this.of = type.getMethod("of", CharSequence.class);
    this.labels = type.getMethod("labels", int.class);
    this.combine = ofOnly ? null : type.getDeclaredMethod("combine", int.class, int.class);
    if (combine != null) combine.setAccessible(true);
    this.out = out;
  }

  public static void main(String[] args) throws Exception {
    boolean ofOnly = args.length > 1 && OF_ONLY.equals(args[1]);
    PrintStream out = new PrintStream(new BufferedOutputStream(System.out, 1 << 20), false, StandardCharsets.US_ASCII);
    M3IndexCharacterFlagsDump dump = new M3IndexCharacterFlagsDump(Class.forName(args[0]), ofOnly, out);
    dump.header();
    dump.cases();
    dump.allLabels();
    dump.randomTexts();
    if (!ofOnly) dump.pairsAndTriples();
    if (!ofOnly) dump.exhaustive();
    if (!ofOnly) dump.randomPairs();
    dump.total(ofOnly);
    out.flush();
  }

  private void header() throws ReflectiveOperationException {
    out.print("MODIFIERS of=" + Modifier.toString(of.getModifiers()) + " labels=" + Modifier.toString(labels.getModifiers()));
    out.println(combine == null ? "" : " combine=" + Modifier.toString(combine.getModifiers()));
    StringBuilder line = new StringBuilder("CONSTANTS");
    for (String name : CONSTANTS) line.append(' ').append(name).append('=').append(type.getField(name).getInt(null));
    out.println(line);
  }

  private void cases() throws ReflectiveOperationException {
    for (int i = 0; i < CASES.length; i++) {
      int flags = of(CASES[i]);
      out.println("CASE " + i + " units=" + units(CASES[i]) + " of=" + Integer.toHexString(flags) + " labels=" + labels(flags));
    }
  }

  private void pairsAndTriples() throws ReflectiveOperationException {
    for (int a = 0; a < CASES.length; a++) {
      for (int b = 0; b < CASES.length; b++) {
        int scanned = of(CASES[a] + CASES[b]);
        out.println("PAIR " + a + "," + b + " scan=" + Integer.toHexString(scanned) + " combine=" + combine(of(CASES[a]), of(CASES[b])));
        triples(a, b);
      }
    }
  }

  private void triples(int a, int b) throws ReflectiveOperationException {
    StringBuilder line = new StringBuilder("TRIPLES ").append(a).append(',').append(b);
    for (int c = 0; c < CASES.length; c++) {
      String scanned = Integer.toHexString(of(CASES[a] + CASES[b] + CASES[c]));
      String leftFirst = combineResult(combineResult(of(CASES[a]), of(CASES[b])), of(CASES[c]));
      String rightFirst = combineResult(of(CASES[a]), combineResult(of(CASES[b]), of(CASES[c])));
      line.append(' ').append(scanned).append('/').append(leftFirst).append('/').append(rightFirst);
    }
    out.println(line);
  }

  private void exhaustive() {
    for (int left = 0; left < FLAG_SPACE; left++) {
      StringBuilder line = new StringBuilder("COMBINE ").append(Integer.toHexString(left)).append(':');
      for (int right = 0; right < FLAG_SPACE; right++) line.append(' ').append(combine(left, right));
      out.println(line);
    }
  }

  private void allLabels() throws ReflectiveOperationException {
    for (int flags = 0; flags < FLAG_SPACE; flags++) out.println("LABELS " + Integer.toHexString(flags) + " " + labels(flags));
    out.println("LABELS_OUT_OF_DOMAIN " + labelsResult(FLAG_SPACE) + " " + labelsResult(-1) + " " + labelsResult(Integer.MIN_VALUE));
  }

  private void randomPairs() {
    Random random = new Random(SEED);
    for (int trial = 0; trial < RANDOM_PAIRS; trial++) {
      int left = draw(random);
      int right = draw(random);
      out.println("RANDOM " + Integer.toHexString(left) + " " + Integer.toHexString(right) + " " + combine(left, right));
    }
  }

  private static int draw(Random random) {
    return switch (random.nextInt(4)) {
      case 0 -> random.nextInt(FLAG_SPACE);
      case 1 -> random.nextInt(FLAG_SPACE) | (1 << (FLAG_BITS + random.nextInt(HIGH_BITS)));
      case 2 -> random.nextInt();
      default -> -1 - random.nextInt(Integer.MAX_VALUE);
    };
  }

  private void randomTexts() throws ReflectiveOperationException {
    Random random = new Random(SEED ^ RANDOM_TEXTS);
    for (int trial = 0; trial < RANDOM_TEXTS; trial++) {
      char[] units = new char[random.nextInt(MAX_TEXT_UNITS + 1)];
      for (int i = 0; i < units.length; i++) units[i] = (char) (random.nextBoolean() ? random.nextInt(ASCII_LIMIT) : random.nextInt(UNIT_LIMIT));
      int flags = of(new String(units));
      out.println("TEXT " + units(new String(units)) + " of=" + Integer.toHexString(flags) + " labels=" + labels(flags));
    }
  }

  private void total(boolean ofOnly) {
    out.println("TOTAL class=" + (ofOnly ? OF_ONLY : "full") + " cases=" + CASES.length + " labels=" + FLAG_SPACE + " random_texts=" + RANDOM_TEXTS
        + (ofOnly ? "" : " pairs=" + CASES.length * CASES.length + " triples=" + CASES.length * CASES.length * CASES.length
        + " exhaustive_pairs=" + (long) FLAG_SPACE * FLAG_SPACE + " random_pairs=" + RANDOM_PAIRS + " random_pairs_iae=" + iaeCount));
  }

  private int of(CharSequence text) throws ReflectiveOperationException {
    return (Integer) of.invoke(null, text);
  }

  private String labels(int flags) throws ReflectiveOperationException {
    return (String) labels.invoke(null, flags);
  }

  private String labelsResult(int flags) {
    try {
      return labels(flags);
    } catch (InvocationTargetException refused) {
      return refused.getCause().getClass().getSimpleName();
    } catch (ReflectiveOperationException error) {
      throw new IllegalStateException(error);
    }
  }

  /** Hex result of combine, or the simple name of the refusing exception (IAE for IllegalArgumentException). */
  private String combine(int left, int right) {
    try {
      return Integer.toHexString((Integer) combine.invoke(null, left, right));
    } catch (InvocationTargetException refused) {
      iaeCount += refused.getCause() instanceof IllegalArgumentException ? 1 : 0;
      return refused.getCause() instanceof IllegalArgumentException ? IAE : refused.getCause().getClass().getSimpleName();
    } catch (ReflectiveOperationException error) {
      throw new IllegalStateException(error);
    }
  }

  private String combineResult(int left, int right) {
    return combine(left, right);
  }

  private String combineResult(String left, int right) {
    return IAE.equals(left) ? IAE : combine(Integer.parseInt(left, 16), right);
  }

  private String combineResult(int left, String right) {
    return IAE.equals(right) ? IAE : combine(left, Integer.parseInt(right, 16));
  }

  private static String units(String text) {
    StringBuilder hex = new StringBuilder();
    for (int i = 0; i < text.length(); i++) hex.append(i == 0 ? "" : ",").append(Integer.toHexString(text.charAt(i)));
    return hex.length() == 0 ? "-" : hex.toString();
  }
}
