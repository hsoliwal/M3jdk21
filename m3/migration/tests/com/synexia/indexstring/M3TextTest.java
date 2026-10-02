// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring;

import com.m3.indexstring.M3Text;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public final class M3TextTest {
  private static long checks;
  private M3TextTest() {}
  public static void main(String[] args) throws Exception {
    ownershipAndLegacySeparation(); unicodeAndValues(); searchAndRegex(); outputs(); bounds(); randomDifferential(); concurrentAdmission();
    System.out.println("M3_TEXT checks=" + checks);
  }
  private static void ownershipAndLegacySeparation() throws Exception {
    FrozenChars a = FrozenChars.fromString("abcdef");
    FrozenChars equalButOtherOwner = FrozenChars.fromString("abcdef");
    MIndexJoinedChars retained = MIndexJoinedChars.ofRetained(a);
    MIndexJoinedChars distinct = MIndexJoinedChars.ofRetained(equalButOtherOwner);
    check(!retained.sharesBackingWith(distinct));
    check(retained == MIndexJoinedChars.ofRetained(a));
    check(MIndexJoinedChars.of(a).sharesBackingWith(MIndexJoinedChars.of(equalButOtherOwner)));
    MIndexJoinedChars splitSameOwner = MIndexJoinedChars.ofRetained(a.subSequence(0, 3), a.subSequence(3, 6));
    check(retained.sharesBackingWith(splitSameOwner));
    MIndexJoinedChars differentOwner = MIndexJoinedChars.ofRetained(a.subSequence(0, 3), equalButOtherOwner.subSequence(3, 6));
    check(differentOwner.retainedParts().length == 2);
    M3Text left = M3Text.of(a.subSequence(0, 3));
    M3Text right = M3Text.of(a.subSequence(3, 6));
    M3Text joined = left.concat(right);
    check(joined.asLegacyView().sharesBackingWith(retained));
    Field payload = FrozenChars.class.getDeclaredField("data"); payload.setAccessible(true);
    Object canonical = payload.get(a);
    for (FrozenChars part : joined.asLegacyView().retainedParts()) check(payload.get(part) == canonical);
    for (FrozenChars part : joined.substring(1, 5).asLegacyView().retainedParts()) check(payload.get(part) == canonical);
    FrozenChars[] exposed = retained.retainedParts(); exposed[0] = equalButOtherOwner;
    check(retained.retainedParts()[0] == a);
    check(joined.substring(2, 2).asLegacyView().retainedParts().length == 0);
    M3Text repeated = M3Text.of(a).repeat(4);
    check(repeated.toString().equals("abcdef".repeat(4)));
    for (FrozenChars part : repeated.asLegacyView().retainedParts()) check(payload.get(part) == canonical);
    M3Text admitted = M3Text.fromString("local-admission");
    check(admitted.asLegacyView().sharesBackingWith(M3Text.fromString(new String("local-admission")).asLegacyView()));
  }
  private static void unicodeAndValues() {
    M3Text split = M3Text.fromString("a\ud83d").concat(M3Text.fromString("\ude00\0z"));
    String oracle = "a\ud83d\ude00\0z";
    check(split.toString().equals(oracle));
    check(Arrays.equals(split.codePoints().toArray(), oracle.codePoints().toArray()));
    check(split.substring(1, 2).toString().equals("\ud83d"));
    check(split.substring(2, 3).toString().equals("\ude00"));
    check(split.equals(M3Text.fromString(oracle)));
    check(split.hashCode() == oracle.hashCode());
    check(split.hashCode() == split.asLegacyView().stringHash());
    check(!split.equals(oracle)); // Do not violate String.equals symmetry.
    check(split.contentEquals(oracle));
    M3Text aa = M3Text.fromString("Aa"), bb = M3Text.fromString("BB");
    check(aa.hashCode() == bb.hashCode()); check(!aa.equals(bb));
    check(M3Text.fromString("").hashCode() == 0);
    for (int unit = 0; unit <= Character.MAX_VALUE; unit++) {
      String text = "L" + (char) unit + "R";
      M3Text value = M3Text.fromString("L").concat(M3Text.fromString(String.valueOf((char) unit))).concat(M3Text.fromString("R"));
      check(value.hashCode() == text.hashCode());
      check(value.substring(1, 2).charAt(0) == (char) unit);
      check(value.contentEquals(text));
      check(value.compareTo(M3Text.fromString(text)) == 0);
    }
  }
  private static void searchAndRegex() {
    M3Text input = M3Text.fromString("x\ud83d").concat(M3Text.fromString("\ude00abab\0"));
    String text = input.toString();
    String[] patterns = {"", "x", "\ud83d", "\ude00", "\ud83d\ude00", "abab", "ab", "\0", "missing"};
    int[] starts = {Integer.MIN_VALUE, -1, 0, 1, 2, 3, text.length(), text.length() + 1, Integer.MAX_VALUE};
    for (String pattern : patterns) for (int from : starts) {
      check(input.indexOf(pattern, from) == text.indexOf(pattern, from));
      check(input.lastIndexOf(pattern, from) == text.lastIndexOf(pattern, from));
      check(input.startsWith(pattern, from) == text.startsWith(pattern, from));
    }
    for (String pattern : patterns) check(input.endsWith(pattern) == text.endsWith(pattern));
    Pattern[] regexes = {Pattern.compile("(ab)\\1"), Pattern.compile("(?<=😀)(ab)+"),
        Pattern.compile("\\x{1f600}"), Pattern.compile("(?<letter>x).*(?<end>\\x00)", Pattern.DOTALL),
        Pattern.compile("^x", Pattern.MULTILINE), Pattern.compile("(?i:AB)")};
    for (Pattern pattern : regexes) {
      var expected = pattern.matcher(text); var actual = input.matcher(pattern);
      while (expected.find()) {
        check(actual.find()); check(expected.start() == actual.start()); check(expected.end() == actual.end());
        for (int group = 0; group <= expected.groupCount(); group++) check(java.util.Objects.equals(expected.group(group), actual.group(group)));
      }
      check(!actual.find());
      check(pattern.matcher(text).replaceAll("[$0]").equals(input.matcher(pattern).replaceAll("[$0]")));
      var regionExpected = pattern.matcher(text).region(1, text.length()).useTransparentBounds(true).useAnchoringBounds(false);
      var regionActual = input.matcher(pattern).region(1, text.length()).useTransparentBounds(true).useAnchoringBounds(false);
      check(regionExpected.find() == regionActual.find());
    }
  }
  private static void outputs() throws Exception {
    M3Text text = M3Text.fromString("A\ud83d").concat(M3Text.fromString("\ude00B"));
    char[] output = text.toCharArray(); output[0] = '!'; check(text.charAt(0) == 'A');
    char[] destination = ".......".toCharArray(); text.getChars(1, 4, destination, 2);
    check(new String(destination).equals("..\ud83d\ude00B.."));
    char[] before = destination.clone();
    refuses(IndexOutOfBoundsException.class, () -> text.getChars(0, 4, destination, 5));
    check(Arrays.equals(destination, before));
    check(Arrays.equals(text.encode(StandardCharsets.UTF_8).copy(), text.toString().getBytes(StandardCharsets.UTF_8)));
    StringWriter writer = new StringWriter();
    try (var reader = text.asReader()) { check(reader.transferTo(writer) == text.length()); }
    check(writer.toString().equals(text.toString()));
  }
  private static void bounds() {
    M3Text text = M3Text.fromString("ab");
    refuses(NullPointerException.class, () -> M3Text.fromString(null));
    refuses(NullPointerException.class, () -> text.concat(null));
    refuses(IndexOutOfBoundsException.class, () -> text.substring(2, 1));
    refuses(IndexOutOfBoundsException.class, () -> text.charAt(text.length()));
    refuses(IllegalArgumentException.class, () -> text.repeat(-1));
    refuses(OutOfMemoryError.class, () -> text.repeat(Integer.MAX_VALUE));
    check(text.repeat(0).isEmpty()); check(text.repeat(1) == text);
    check(text.concat(M3Text.fromString("")) == text);
  }
  private static void randomDifferential() {
    Random random = new Random(0x4d33466163616465L);
    for (int sample = 0; sample < 1800; sample++) {
      int size = random.nextInt(45); char[] units = new char[size];
      for (int i = 0; i < size; i++) units[i] = (char) (random.nextBoolean() ? random.nextInt(6) : random.nextInt(65536));
      String expected = new String(units);
      int seam = random.nextInt(size + 1);
      M3Text actual = M3Text.fromString(expected.substring(0, seam)).concat(M3Text.fromString(expected.substring(seam)));
      int from = random.nextInt(size + 1), to = from + random.nextInt(size - from + 1);
      M3Text range = actual.substring(from, to);
      check(range.toString().equals(expected.substring(from, to))); check(range.hashCode() == expected.substring(from, to).hashCode());
      String needle = expected.substring(from, to);
      check(actual.indexOf(needle) == expected.indexOf(needle)); check(actual.lastIndexOf(needle) == expected.lastIndexOf(needle));
      check(Arrays.equals(actual.chars().toArray(), expected.chars().toArray()));
      check(Arrays.equals(actual.codePoints().toArray(), expected.codePoints().toArray()));
      String other = expected + (char) random.nextInt(65536);
      check(Integer.signum(actual.compareTo(M3Text.fromString(other))) == Integer.signum(expected.compareTo(other)));
    }
  }
  private static void concurrentAdmission() throws Exception {
    FrozenChars a = FrozenChars.fromString("owner"), b = FrozenChars.fromString("range");
    MIndexJoinedChars expected = MIndexJoinedChars.ofRetained(a, b);
    try (var workers = Executors.newFixedThreadPool(4)) {
      Callable<Boolean> task = () -> {
        for (int i = 0; i < 2000; i++) if (MIndexJoinedChars.ofRetained(a, b) != expected) return false;
        return true;
      };
      for (var future : workers.invokeAll(Arrays.asList(task, task, task, task))) check(future.get());
    }
  }
  private static void check(boolean passed) { if (!passed) throw new AssertionError("check " + checks); checks++; }
  private static void refuses(Class<? extends Throwable> type, Task task) {
    try { task.run(); } catch (Throwable error) { if (type.isInstance(error)) { checks++; return; } throw new AssertionError("wrong exception", error); }
    throw new AssertionError("missing " + type.getName());
  }
  @FunctionalInterface private interface Task { void run() throws Exception; }
}
