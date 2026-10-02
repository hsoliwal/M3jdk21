/* Copyright 2026 Hitesh Soliwal; SPDX-License-Identifier: Apache-2.0 */
import com.m3.indexstring.M3String;
import com.m3.indexstring.M3StringArena;
import com.synexia.indexstring.FrozenByteInterner;
import com.synexia.indexstring.FrozenBytes;
import com.synexia.indexstring.FrozenChars;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public final class M3StringTest {
    static int checks;
    static void check(boolean value) {
        checks++;
        if (!value) throw new AssertionError("check " + checks);
    }
    static void same(Object expected, Object actual) { check(expected.equals(actual)); }
    static void expect(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); throw new AssertionError("missing " + type); }
        catch (Throwable failure) { check(type.isInstance(failure)); }
    }
    static void differential() throws Exception {
        Random random = new Random(210035);
        M3StringArena arena = new M3StringArena(64, 4096);
        for (int trial = 0; trial < 600; trial++) {
            char[] units = new char[random.nextInt(45)];
            for (int i = 0; i < units.length; i++) units[i] = (char)random.nextInt(65536);
            String reference = new String(units);
            int seam = random.nextInt(reference.length() + 1);
            M3String value = arena.fromString(reference.substring(0, seam))
                .concat(arena.fromString(reference.substring(seam)));
            same(reference, value.toString());
            check(value.hashCode() == reference.hashCode());
            check(value.equals(arena.fromString(reference)));
            check(!value.equals(reference) && !reference.equals(value));
            check(value.contentEquals(reference));
            check(Arrays.equals(value.chars().toArray(), reference.chars().toArray()));
            check(Arrays.equals(value.codePoints().toArray(), reference.codePoints().toArray()));
            check(Arrays.equals(value.toCharArray(), reference.toCharArray()));
            char[] writable = value.toCharArray();
            Arrays.fill(writable, '!'); same(reference, value.toString());
            Arrays.fill(units, '!'); same(reference, value.toString());
            for (int i = 0; i <= reference.length(); i++) {
                M3String suffix = value.substring(i);
                same(reference.substring(i), suffix.toString());
                check(suffix.hashCode() == reference.substring(i).hashCode());
                check(value.indexOf(reference.substring(i), -1) == reference.indexOf(reference.substring(i), -1));
                check(value.lastIndexOf(reference.substring(i), reference.length()) == reference.lastIndexOf(reference.substring(i), reference.length()));
            }
            String other = reference + "z";
            check(Integer.signum(value.compareTo(arena.fromString(other))) == Integer.signum(reference.compareTo(other)));
            char[] output = new char[reference.length() + 4];
            value.getChars(0, value.length(), output, 2);
            same(reference, new String(output, 2, reference.length()));
            StringWriter writer = new StringWriter(); value.writeTo(writer); same(reference, writer.toString());
            for (String name : new String[]{"UTF-8", "UTF-16BE", "UTF-16LE", "ISO-8859-1", "ISO-2022-JP"}) {
                Charset charset = Charset.forName(name);
                check(Arrays.equals(reference.getBytes(charset), value.getBytes(charset)));
            }
        }
    }
    static void seamsAndIdentity() throws Exception {
        M3StringArena arena = new M3StringArena(4, 32);
        M3String high = arena.fromString("A\ud83d"), low = arena.fromString("\ude00B\u0000\ud800");
        M3String joined = high.concat(low);
        String text = "A\ud83d\ude00B\u0000\ud800";
        same(text, joined.toString());
        same("\ud83d", joined.substring(1, 2).toString());
        check(joined.codePointAt(1) == 0x1f600);
        check(joined.codePointCount(0, joined.length()) == text.codePointCount(0, text.length()));
        check(Arrays.equals(text.codePoints().toArray(), joined.codePoints().toArray()));
        check(high.concat(low).sharesBackingWith(joined));
        arena.clear(); same(text, joined.toString()); check(arena.retainedPayloadBytes() == 0);
        check(joined.equals(M3String.fromString(text)));
        check(!joined.sharesBackingWith(M3String.fromString(text)));
        M3String chain = joined;
        for (int i = 0; i < 5000; i++) chain = M3String.fromString("").concat(chain);
        same(text, chain.toString());
        M3String whole = arena.fromString("retained-owner");
        M3String normalized = whole.substring(0, 3).concat(whole.substring(3));
        check(normalized.segmentCount() == 1);
        check(normalized.sharesBackingWith(whole));
        check(whole.substring(1, 2).retainedPayloadBytes() == 2L * whole.length());
        check(whole.substring(1, 2).compact().retainedPayloadBytes() == 2);
        char[] source = {'a', '\ud800'};
        M3String admitted = M3String.fromChars(source); source[0] = '!'; same("a\ud800", admitted.toString());
        check(admitted.concat(M3String.fromChars(new char[]{'b'})).segmentCount() == 2);
        char[] largeOwner = new char[100]; Arrays.fill(largeOwner, '#'); largeOwner[0] = '$';
        M3String retainedSlice = M3String.fromChars(largeOwner).substring(0, 1);
        M3String equivalent = new M3StringArena(2, 10).fromString("$");
        check(equivalent.sharesBackingWith(retainedSlice));
        check(equivalent.retainedPayloadBytes() == retainedSlice.retainedPayloadBytes());
        M3String repeated = M3String.fromString("aaa");
        check(repeated.indexOf("aa", 0) == 0 && repeated.lastIndexOf("aa", 99) == 1);
        check(repeated.indexOf("", 99) == 3 && repeated.lastIndexOf("", -1) == -1);
        expect(NullPointerException.class, () -> repeated.concat(null));
        expect(IndexOutOfBoundsException.class, () -> repeated.substring(2, 1));
        expect(IndexOutOfBoundsException.class, () -> repeated.charAt(3));
        M3StringArena noCache = new M3StringArena(0, 0);
        M3String live = noCache.fromString("live"); same("live", live.toString()); check(noCache.cachedAtoms() == 0);
        M3StringArena collisions = new M3StringArena(10, 100);
        same("Aa", collisions.fromString("Aa").toString());
        same("BB", collisions.fromString("BB").toString());
        check(!collisions.fromString("Aa").equals(collisions.fromString("BB")));
        FrozenByteInterner raw = new FrozenByteInterner(4, 64);
        String collisionA = new String(new char[]{45580, 40283, 30766, 47151});
        String collisionB = new String(new char[]{8015, 52553, 29480, 53135});
        FrozenBytes atomA = raw.internUtf16(collisionA), atomB = raw.internUtf16(collisionB);
        check(atomA.hash32() == atomB.hash32());
        check(atomA != atomB && !Arrays.equals(atomA.copy(), atomB.copy()));
        check(raw.internUtf16(collisionA) == atomA && raw.internUtf16(collisionB) == atomB);
        same(collisionA, FrozenChars.fromUtf16Bytes(atomA).toString());
        same(collisionB, FrozenChars.fromUtf16Bytes(atomB).toString());
        check(FrozenChars.fromUtf16Bytes(atomA) == FrozenChars.fromUtf16Bytes(atomA));
        raw.clear(); same(collisionA, FrozenChars.fromUtf16Bytes(atomA).toString());
        expect(IllegalArgumentException.class, () -> FrozenChars.fromUtf16Bytes(FrozenBytes.copyOf(new byte[]{1})));
        expect(IllegalStateException.class, () -> FrozenChars.fromUtf16Bytes(atomA).directUtf16Bytes());
        try (var workers = Executors.newFixedThreadPool(8)) {
            var tasks = new java.util.ArrayList<java.util.concurrent.Future<M3String>>();
            for (int i = 0; i < 80; i++) tasks.add(workers.submit(() -> arena.fromString("race\ud800")));
            M3String first = tasks.getFirst().get();
            for (var task : tasks) check(first.sharesBackingWith(task.get()));
        }
        check(arena.cachedAtoms() <= 4 && arena.retainedPayloadBytes() <= 32);
        same("nullx", M3String.fromConcatOperands(null, "x").toString());
        same("nullnull", M3String.fromConcatOperands(null, null).toString());
        for (String expression : new String[]{"(?s).*", "(A)(.*)(B)", "(?<=A).", "^A.*$", "(B)\\1?", "\\x{1f600}"}) {
            Pattern pattern = Pattern.compile(expression, Pattern.DOTALL | Pattern.UNICODE_CHARACTER_CLASS);
            var reference = pattern.matcher(text); var actual = joined.matcher(pattern);
            while (true) {
                boolean found = reference.find(); check(found == actual.find()); if (!found) break;
                check(reference.start() == actual.start() && reference.end() == actual.end());
                for (int group = 0; group <= reference.groupCount(); group++) check(java.util.Objects.equals(reference.group(group), actual.group(group)));
            }
            same(pattern.matcher(text).replaceAll("x"), joined.matcher(pattern).replaceAll("x"));
        }
    }
    public static void main(String[] args) throws Exception {
        differential(); seamsAndIdentity();
        System.out.println("M3_RETAINED_TEXT_PASS checks=" + checks);
    }
}
