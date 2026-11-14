/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0 */
/*
 * @test
 * @summary Real ordinary String concat and slicing preserve immutable leaves
 * @modules java.base/java.lang:open
 * @run main/othervm -ea -XX:+UnlockExperimentalVMOptions -XX:+UseM3SegmentedStrings SegmentedStringProbe
 * @run main/othervm -ea -XX:+UnlockExperimentalVMOptions -XX:+UseM3SegmentedStrings -XX:-CompactStrings SegmentedStringProbe
 */
import java.lang.reflect.Field;
import java.util.Arrays;
import java.nio.charset.StandardCharsets;

/** Real ordinary-String representation checks. No custom CharSequence wrapper. */
public class SegmentedStringProbe {
    static Field value, parts, ranges, cache, coder;
    static int checks;
    static void check(boolean yes) {
        checks++;
        if (!yes) throw new AssertionError("check=" + checks);
    }
    static Field field(String name) throws Exception {
        Field f = String.class.getDeclaredField(name); f.setAccessible(true); return f;
    }
    static String plus(String a, String b) { return a + b; }
    static String flat(char... chars) { return new String(chars); }
    static void unmaterialized(String s) throws Exception {
        check(value.get(s) == null); check(parts.get(s) != null); check(cache.get(s) == null);
    }
    static void exact(String s, char[] expected) throws Exception {
        check(s.length() == expected.length);
        for (int i = 0; i < expected.length; i++) check(s.charAt(i) == expected[i]);
        String original = new String(expected);
        check(s.equals(original)); check(original.equals(s));
        check(s.hashCode() == original.hashCode());
    }
    public static void main(String[] args) throws Exception {
        value = field("value"); parts = field("m3Parts"); ranges = field("m3Ranges");
        cache = field("m3Flat"); coder = field("coder");
        String left = flat('p','r','e','f','i','x','\u0100','\ud83d');
        String right = flat('\ude00','a','s','c','i','i','!');
        String joined = plus(left, right);
        unmaterialized(joined);
        String[] leaves = (String[]) parts.get(joined);
        check(leaves.length == 2); check(leaves[0] == left); check(leaves[1] == right);
        check(value.get(leaves[0]) == value.get(left)); check(value.get(leaves[1]) == value.get(right));
        char[] expected = {'p','r','e','f','i','x','\u0100','\ud83d','\ude00','a','s','c','i','i','!'};
        exact(joined, expected); unmaterialized(joined);
        String seam = joined.substring(7, 9);
        exact(seam, new char[] {'\ud83d','\ude00'}); unmaterialized(seam);
        String ascii = joined.substring(9, 14);
        exact(ascii, new char[] {'a','s','c','i','i'}); unmaterialized(ascii);
        check(((String[]) parts.get(ascii)).length == 1);
        check(((String[]) parts.get(ascii))[0] == right);
        String copied = new String(joined);
        exact(copied, expected); unmaterialized(copied);
        check(parts.get(copied) == parts.get(joined));
        String canonical = joined.intern();
        check(canonical == joined); unmaterialized(joined);
        check(new String(expected).intern() == joined);
        String joinedAgain = left.concat(right);
        check(joinedAgain.intern() == joined); unmaterialized(joinedAgain);
        System.gc(); exact(seam, new char[] {'\ud83d','\ude00'}); unmaterialized(seam);
        // An explicit contiguous Java consumer may populate only the separate cache.
        check(Arrays.equals(joined.getBytes(StandardCharsets.UTF_8), new String(expected).getBytes(StandardCharsets.UTF_8)));
        check(value.get(joined) == null); check(cache.get(joined) != null); check(parts.get(joined) == leaves);
        exact(joined, expected);
        for (int unit = 0; unit <= Character.MAX_VALUE; unit++) {
            String a = flat('A', (char) unit);
            String b = flat((char) (Character.MAX_VALUE - unit), 'Z');
            String s = a.concat(b);
            exact(s, new char[] {'A',(char) unit,(char) (Character.MAX_VALUE-unit),'Z'});
            unmaterialized(s);
            String slice = s.substring(1,3);
            exact(slice,new char[]{(char)unit,(char)(Character.MAX_VALUE-unit)});
            unmaterialized(slice);
        }
        System.out.println("SEGMENTED_STRING_PASS checks=" + checks);
    }
}
