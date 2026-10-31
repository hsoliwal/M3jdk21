/* Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
import com.m3.text.*;
import java.util.*;
import java.util.regex.*;

/** Differential Java-21 proof for no-copy, long-offset, rope-indexed composite text. */
public final class M3RopeTextIndexTest {
    private static long checks;
    private static void yes(boolean value, String what) {
        checks++;
        if (!value) throw new AssertionError(what + " (check " + checks + ")");
    }
    private static void equal(long a, long b, String what) { yes(a == b, what + ": " + a + " != " + b); }
    private static void equal(Object a, Object b, String what) { yes(Objects.equals(a, b), what + ": " + a + " != " + b); }
    private static void rejected(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); throw new AssertionError("expected " + type.getName()); }
        catch (RuntimeException failure) { yes(type.isInstance(failure), "wrong exception " + failure); }
    }
    private static LocalM3StringPiece piece(LocalM3Arena a, String text) {
        return a.copyUtf16(text.toCharArray());
    }
    private static void regexParity(M3RopeTextIndex rope, String oracle, String regex, int flags) {
        Pattern pattern = Pattern.compile(regex, flags);
        Matcher actual = pattern.matcher(rope.window(0, rope.utf16Length()));
        Matcher expected = pattern.matcher(oracle);
        for (int i = 0; i < 100; i++) {
            boolean foundA = actual.find();
            boolean foundB = expected.find();
            yes(foundA == foundB, "regex result " + regex + " " + i);
            if (!foundB) return;
            equal(actual.start(), expected.start(), "regex start");
            equal(actual.end(), expected.end(), "regex end");
            equal(actual.groupCount(), expected.groupCount(), "group count");
            for (int j = 0; j <= actual.groupCount(); j++) {
                equal(actual.group(j), expected.group(j), "group text " + j);
                equal(actual.start(j), expected.start(j), "capture start " + j);
                equal(actual.end(j), expected.end(j), "capture end " + j);
            }
        }
        throw new AssertionError("unexpected excessive matches");
    }
    private static void edgeCases() {
        LocalM3Arena a = new LocalM3Arena();
        M3StringPiece[] segments = {piece(a, "ab"), piece(a, ""), piece(a, "c\ud83d"), piece(a, "\ude00X"), piece(a, "yz\r"), piece(a, "\nAB"), piece(a, "a\ud800")};
        M3RopeTextIndex r = M3RopeTextIndex.of(segments);
        String oracle = "abc\ud83d\ude00Xyz\r\nABa\ud800";
        Arrays.fill(segments, piece(a, "wrong"));
        equal(r.utf16Length(), oracle.length(), "length");
        equal(r.segmentCount(), 6, "omits empties");
        for (int i = 0; i < oracle.length(); i++) yes(r.charAt(i) == oracle.charAt(i), "charAt " + i);
        for (int from = 0; from <= oracle.length(); from++) {
            for (int to = from; to <= oracle.length(); to++) {
                CharSequence w = r.window(from, to);
                equal(w.length(), to - from, "window length");
                equal(w.toString(), oracle.substring(from, to), "window text");
                equal(w.subSequence(0, w.length()).toString(), oracle.substring(from, to), "window subSequence");
                for (int i = 0; i < w.length(); i++) yes(w.charAt(i) == oracle.charAt(from + i), "window charAt");
            }
            for (String literal : List.of("", "a", "abc", "c\ud83d\ude00X", "\r\n", "AB", "\ud800", "notfound")) {
                equal(r.findLiteral(literal, from), oracle.indexOf(literal, from), "KMP " + literal + "/" + from);
            }
        }
        regexParity(r, oracle, "(c\\x{1F600}X)", 0);
        regexParity(r, oracle, "(?<=bc)\\x{1F600}", 0);
        regexParity(r, oracle, "(AB)(a)", 0);
        regexParity(r, oracle, "(?s)(c.*)(A)", 0);
        regexParity(r, oracle, "[\\s\\S]", 0);
        regexParity(r, oracle, "(?m)^AB", Pattern.MULTILINE);
        regexParity(r, oracle, "(a)\\1", 0);
        regexParity(r, oracle, "", 0);
        rejected(IndexOutOfBoundsException.class, () -> r.charAt(-1));
        rejected(IndexOutOfBoundsException.class, () -> r.charAt(oracle.length()));
        rejected(IndexOutOfBoundsException.class, () -> r.window(-1, 1));
        rejected(IndexOutOfBoundsException.class, () -> r.window(4, 2));
        rejected(IndexOutOfBoundsException.class, () -> r.window(0, oracle.length() + 1));
        rejected(IndexOutOfBoundsException.class, () -> r.findLiteral("x", oracle.length() + 1));
        rejected(NullPointerException.class, () -> M3RopeTextIndex.of((M3StringPiece[]) null));
        rejected(NullPointerException.class, () -> M3RopeTextIndex.of(piece(a, "ok"), null));
        rejected(NullPointerException.class, () -> r.findLiteral(null, 0));
        rejected(NullPointerException.class, () -> r.matcher(null, 0, 1));
        M3RopeTextIndex zero = M3RopeTextIndex.of();
        equal(zero.utf16Length(), 0, "zero length");
        equal(zero.findLiteral("", 0), 0, "zero empty search");
        equal(zero.findLiteral("a", 0), -1, "zero nonempty search");
        equal(zero.window(0, 0).toString(), "", "zero window");
        M3StringPiece nested = M3StringPiece.join(piece(a, "f"), piece(a, "g"));
        M3RopeTextIndex n = M3RopeTextIndex.of(piece(a, "e"), nested, piece(a, "h"));
        equal(n.window(0, n.utf16Length()).toString(), "efgh", "joined piece leaf");
        equal(n.findLiteral("fgh", 0), 1, "nested cross seam");
    }
    private static void randomized() {
        Random random = new Random(8232349L);
        LocalM3Arena arena = new LocalM3Arena();
        char[] alphabet = {'a','b','c','d','\ud800','\udc00','\ud83d','\ude00','\n','\r','\u0000','\uffff'};
        for (int iteration = 0; iteration < 600; iteration++) {
            int size = random.nextInt(90);
            char[] units = new char[size];
            for (int i = 0; i < size; i++) units[i] = alphabet[random.nextInt(alphabet.length)];
            String oracle = new String(units);
            List<M3StringPiece> parts = new ArrayList<>();
            int index = 0;
            while (index < size) {
                int chunk = Math.min(size-index, 1+random.nextInt(6));
                parts.add(arena.copyUtf16(Arrays.copyOfRange(units,index,index+chunk)));
                index+=chunk;
                if(random.nextBoolean()) parts.add(piece(arena,""));
            }
            M3RopeTextIndex rope = M3RopeTextIndex.of(parts.toArray(M3StringPiece[]::new));
            equal(rope.utf16Length(), size, "random length");
            for (int i = 0; i < size; i++) yes(rope.charAt(i) == units[i], "random char");
            for (int trial=0;trial<18;trial++) {
                int start=random.nextInt(size+1), end=start+random.nextInt(size-start+1);
                equal(rope.window(start,end).toString(),oracle.substring(start,end),"random window");
                int l=random.nextInt(6);
                char[] p=new char[l];
                for(int i=0;i<l;i++)p[i]=alphabet[random.nextInt(alphabet.length)];
                String pattern=new String(p);
                equal(rope.findLiteral(pattern,start),oracle.indexOf(pattern,start),"random KMP");
            }
            if(size<70) regexParity(rope,oracle,"(a|b+)(c*)",0);
        }
    }
    private static void largeLogicalText() {
        LocalM3Arena arena = new LocalM3Arena();
        char[] million = new char[1_100_000];
        Arrays.fill(million,'z');
        million[0] = 'A';
        million[million.length-1]='B';
        M3StringPiece leaf = arena.copyUtf16(million);
        M3StringPiece[] segments=new M3StringPiece[2000];
        Arrays.fill(segments,leaf);
        M3RopeTextIndex rope=M3RopeTextIndex.of(segments);
        yes(rope.utf16Length()>Integer.MAX_VALUE, "large > 32-bit");
        equal(rope.utf16Length(),2_200_000_000L,"large length");
        yes(rope.charAt(rope.utf16Length()-1)=='B',"long final char");
        yes(rope.charAt(rope.utf16Length()-1_100_000)=='A',"long segment boundary");
        yes(rope.charAt((long)Integer.MAX_VALUE+12)=='z',"large offset");
        equal(rope.findLiteral("BA", rope.utf16Length()-1_100_003),rope.utf16Length()-1_100_001,"long KMP seam");
        CharSequence window=rope.window(rope.utf16Length()-4,rope.utf16Length());
        equal(window.toString(),"zzzB","tail window");
        Matcher m=rope.matcher(Pattern.compile("z+(B)"),rope.utf16Length()-8,rope.utf16Length());
        yes(m.find(),"large window regex");
        equal(m.group(1),"B","large regex capture");
        rejected(IllegalArgumentException.class,()->rope.window(0,rope.utf16Length()));
    }
    public static void main(String[] args) {
        edgeCases(); randomized(); largeLogicalText();
        System.out.println("M3RopeTextIndexTest PASS checks="+checks);
    }
}
