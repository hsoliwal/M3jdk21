/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 *
 * @test
 * @summary Adaptive M3 UTF-16 masked projection, range/run/bitmap selection and flat nested masks
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringProjectionTest
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringProjectionTest
 */

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

public final class M3StringProjectionTest {
    private static final Class<?> PROJECTION;
    private static final Method SELECT, SELECT_NESTED, KIND, SOURCE_INDEX, GET_CHARS, HASH,
            COUNT, OFFSET, EQUALS, STARTS, ENDS, TO_ARRAY, CODEPOINT, MATERIALIZE, PREPARE, INDEX_OF, COMPARE;
    private static final Field ROOT;
    private static long checks;
    static {
        try {
            PROJECTION = Class.forName("java.lang.M3StringProjection");
            SELECT = method(PROJECTION, "select", CharSequence.class, boolean[].class);
            SELECT_NESTED = method(PROJECTION, "select", boolean[].class);
            KIND = method(PROJECTION, "kind");
            SOURCE_INDEX = method(PROJECTION, "sourceIndexAt", int.class);
            GET_CHARS = method(PROJECTION, "getChars", int.class, int.class, char[].class, int.class);
            HASH = method(PROJECTION, "javaHashCode");
            COMPARE = method(PROJECTION, "compareContent", CharSequence.class);
            COUNT = method(PROJECTION, "codePointCount", int.class, int.class);
            OFFSET = method(PROJECTION, "offsetByCodePoints", int.class, int.class);
            EQUALS = method(PROJECTION, "contentEquals", CharSequence.class);
            STARTS = method(PROJECTION, "startsWith", CharSequence.class);
            ENDS = method(PROJECTION, "endsWith", CharSequence.class);
            TO_ARRAY = method(PROJECTION, "toCharArray");
            CODEPOINT = method(PROJECTION, "codePointAt", int.class);
            MATERIALIZE = method(PROJECTION, "materialize");
            Class<?> plan = Class.forName("java.lang.M3StringProjection$LiteralPlan");
            PREPARE = method(plan, "prepare", CharSequence.class);
            INDEX_OF = method(PROJECTION, "indexOf", plan, int.class);
            ROOT = PROJECTION.getDeclaredField("root");
            ROOT.setAccessible(true);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    public static void main(String[] args) throws Exception {
        exactStrategies();
        explicitCases();
        directCanonicalM3Root();
        differentialRandom();
        concurrentReaders();
        for (Field f : PROJECTION.getDeclaredFields()) {
            if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
            check(f.getType() != byte[].class && f.getType() != char[].class
                    && f.getType() != boolean[].class, "no retained text/mask payload");
        }
        System.out.println("M3_STRING_PROJECTION_PASS checks=" + checks);
    }

    private static void exactStrategies() throws Exception {
        String text = "a".repeat(4096);
        boolean[] empty = new boolean[4096];
        check(kind(select(text, empty)).equals("EMPTY"), "empty kind");
        Arrays.fill(empty, true);
        check(kind(select(text, empty)).equals("IDENTITY"), "identity kind");
        Arrays.fill(empty, false);
        Arrays.fill(empty, 100, 300, true);
        check(kind(select(text, empty)).equals("RANGE"), "single range kind");
        Arrays.fill(empty, false);
        empty[10] = empty[201] = empty[4090] = true;
        check(kind(select(text, empty)).equals("RUNS"), "sparse selected runs");
        for (int i = 0; i < empty.length; i++) empty[i] = (i & 1) == 0;
        check(kind(select(text, empty)).equals("BITMAP"), "fragmented selection bitmap");
        Object masked = select(text, empty);
        Object next = invoke(SELECT_NESTED, masked, new boolean[((CharSequence)masked).length()]);
        check(kind(next).equals("EMPTY"), "nested empty normalized");
    }

    private static void explicitCases() throws Exception {
        String original = new String(new char[]{'A','\ud83d','B','\ude03','\u0000','x','\ud83d','\ude42','z'});
        boolean[] include = {true, true, false, true, true, true, true, true, false};
        Object selection = verify(original, include, "surrogate seam across mask");
        CharSequence selected = (CharSequence) selection;
        check(selected.charAt(1) == '\ud83d' && selected.charAt(2) == '\ude03',
                "surrogate pair formed after mask deletion");
        check((int)invoke(CODEPOINT, selection, 1) == 0x1f603, "paired seam code point");
        check(Pattern.compile("A.\\x{0}x.*", Pattern.DOTALL).matcher(selected).matches(),
                "JDK Pattern observes logical view without whole-text copy");
        char[] detached = (char[]) invoke(TO_ARRAY, selection);
        detached[0] = 'Z';
        check(selected.charAt(0) == 'A', "array mutation cannot change canonical view");

        Object sliced = selected.subSequence(1, selected.length() - 1);
        String expectedSlice = selected.toString().substring(1, selected.length() - 1);
        check(sliced.toString().equals(expectedSlice), "slice logical coordinates");
        check(ROOT.get(sliced) == ROOT.get(selection), "subSequence shares immutable root");
        boolean[] nested = new boolean[((CharSequence)sliced).length()];
        for (int i = 0; i < nested.length; i++) nested[i] = (i % 3) != 1;
        Object again = invoke(SELECT_NESTED, sliced, nested);
        check(again.toString().equals(reference(expectedSlice, nested)), "nested selection content");
        check(ROOT.get(again) == ROOT.get(selection), "nested selection collapses to same root");
        check(invoke(SELECT_NESTED, again, all(((CharSequence)again).length())) == again,
                "nested identity selection preserves view");
        check(Pattern.compile(".*", Pattern.DOTALL).matcher((CharSequence)again).matches(),
                "JDK matcher accepts collapsed view");

        try {
            select(original, new boolean[2]);
            throw new AssertionError("wrong mask length accepted");
        } catch (InvocationTargetException expected) {
            check(expected.getCause() instanceof IllegalArgumentException, "mask length refusal");
        }
        try {
            select(new StringBuilder("abc"), all(3));
            throw new AssertionError("mutable CharSequence accepted");
        } catch (InvocationTargetException expected) {
            check(expected.getCause() instanceof IllegalArgumentException, "mutable source refusal");
        }
    }

    /** Test the internal M3String root itself on a source-built M3JDK21 image. */
    private static void directCanonicalM3Root() throws Exception {
        try {
            Field field = String.class.getDeclaredField("m3");
            field.setAccessible(true);
            String text = new String(new char[]{'x','\ud83d','\ude03','y','\u0000','z'});
            text.length();
            Object internal = field.get(text);
            check(internal instanceof CharSequence, "M3-backed source is available");
            boolean[] keep = {true, true, true, false, true, true};
            Object projection = select((CharSequence) internal, keep);
            check(projection.toString().equals("x\ud83d\ude03\u0000z"),
                    "direct canonical root projection");
            check(ROOT.get(projection) == internal, "original internal M3 owner retained");
        } catch (NoSuchFieldException stockJdkProofOnly) {
            // The independent Java 21 source proof runs without a patched HotSpot/AbstractString.
            // The actual jtreg run on M3JDK21 must enter the direct M3 branch above.
        }
    }

    private static void differentialRandom() throws Exception {
        Random random = new Random(0x75ae3L);
        for (int trial = 0; trial < 1_800; trial++) {
            int length = trial % 10 == 0 ? 2048 : random.nextInt(120);
            char[] units = new char[length];
            for (int i = 0; i < length; i++) {
                units[i] = switch (random.nextInt(9)) {
                    case 0 -> '\ud83d';
                    case 1 -> '\ude03';
                    case 2 -> 0;
                    case 3 -> '\u00ff';
                    case 4 -> '\u0100';
                    default -> (char)('A' + random.nextInt(26));
                };
            }
            String text = new String(units);
            boolean[] mask = new boolean[length];
            for (int i = 0; i < length; i++) mask[i] = random.nextInt(100) < (trial % 3 == 0 ? 7 : trial % 3 == 1 ? 50 : 94);
            Object projected = verify(text, mask, "trial " + trial);
            CharSequence logical = (CharSequence)projected;
            if (logical.length() > 0) {
                int from = random.nextInt(logical.length());
                int to = from + random.nextInt(logical.length() - from + 1);
                CharSequence slice = logical.subSequence(from, to);
                check(slice.toString().equals(logical.toString().substring(from, to)), "random slice");
                boolean[] next = new boolean[slice.length()];
                for (int i = 0; i < next.length; i++) next[i] = random.nextBoolean();
                Object nested = invoke(SELECT_NESTED, slice, next);
                String expected = reference(slice.toString(), next);
                check(nested.toString().equals(expected), "nested projection oracle");
                check(ROOT.get(projected) == ROOT.get(nested), "nested owner sharing");
            }
        }
    }

    private static Object verify(String source, boolean[] mask, String label) throws Exception {
        Object projection = select(source, mask);
        CharSequence chars = (CharSequence) projection;
        String expected = reference(source, mask);
        check(chars.length() == expected.length(), label + " length");
        check(expected.contentEquals(chars), label + " content");
        check(chars.toString().equals(expected), label + " materialization");
        check(invoke(MATERIALIZE, projection).equals(expected), label + " explicit export");
        check(Arrays.equals((char[])invoke(TO_ARRAY, projection), expected.toCharArray()), label + " array");
        check((int)invoke(HASH, projection) == expected.hashCode(), label + " hash");
        check((boolean)invoke(EQUALS, projection, expected), label + " contentEquals");
        check((int)invoke(COMPARE, projection, expected) == 0, label + " comparison equal");
        check(Integer.signum((int)invoke(COMPARE, projection, "~"))
                == Integer.signum(expected.compareTo("~")), label + " comparison ordering");
        check((boolean)invoke(STARTS, projection, expected.substring(0, expected.length() / 2)), label + " prefix");
        check((boolean)invoke(ENDS, projection, expected.substring(expected.length() / 2)), label + " suffix");
        check((int)invoke(COUNT, projection, 0, chars.length())
                == expected.codePointCount(0, expected.length()), label + " codepoint count");
        check((int)invoke(OFFSET, projection, 0, expected.codePointCount(0, expected.length()))
                == expected.length(), label + " codepoint forward");
        check((int)invoke(OFFSET, projection, expected.length(),
                -expected.codePointCount(0, expected.length())) == 0,
                label + " codepoint reverse");
        char[] destination = new char[chars.length() + 6];
        Arrays.fill(destination, '!');
        invoke(GET_CHARS, projection, 0, chars.length(), destination, 3);
        check(new String(destination, 3, chars.length()).equals(expected), label + " bulk copy");
        check(destination[0] == '!' && destination[destination.length - 1] == '!', label + " copy fences");
        if (!expected.isEmpty()) {
            int midpoint = expected.length() / 2;
            String needle = expected.substring(midpoint, Math.min(expected.length(), midpoint + 3));
            Object prepared = invoke(PREPARE, null, needle);
            check((int)invoke(INDEX_OF, projection, prepared, 0) == expected.indexOf(needle),
                    label + " prepared KMP");
            check((int)invoke(INDEX_OF, projection, prepared, midpoint + 1)
                    == expected.indexOf(needle, midpoint + 1), label + " KMP fromIndex");
        }
        Object emptyPlan = invoke(PREPARE, null, "");
        check((int)invoke(INDEX_OF, projection, emptyPlan, 1000000) == expected.length(),
                label + " empty search clamp");
        return projection;
    }

    private static void concurrentReaders() throws Exception {
        String source = "alpha-\ud83d\ude42-".repeat(1000);
        boolean[] mask = new boolean[source.length()];
        for (int i = 0; i < mask.length; i++) mask[i] = (i % 7) != 2;
        Object projected = select(source, mask);
        String expected = reference(source, mask);
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread[] threads = new Thread[6];
        for (int t = 0; t < threads.length; t++) {
            threads[t] = new Thread(() -> {
                try {
                    start.await();
                    for (int j = 0; j < 50; j++) {
                        if (!expected.equals(projected.toString())
                                || (int)invoke(HASH, projected) != expected.hashCode()) {
                            throw new AssertionError("concurrent immutable projection changed");
                        }
                    }
                } catch (Throwable e) { failure.compareAndSet(null, e); }
            });
            threads[t].start();
        }
        start.countDown();
        for (Thread t : threads) t.join();
        check(failure.get() == null, "concurrent readers: " + failure.get());
    }

    private static Object select(CharSequence input, boolean[] mask) throws Exception {
        return invoke(SELECT, null, input, mask);
    }
    private static boolean[] all(int count) { boolean[] mask = new boolean[count]; Arrays.fill(mask, true); return mask; }
    private static String kind(Object view) throws Exception { return invoke(KIND, view).toString(); }
    private static String reference(String source, boolean[] selected) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < selected.length; i++) if (selected[i]) result.append(source.charAt(i));
        return result.toString();
    }
    private static Method method(Class<?> owner, String name, Class<?>... types) throws ReflectiveOperationException {
        Method method = owner.getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method;
    }
    private static Object invoke(Method method, Object owner, Object... args) throws Exception {
        return method.invoke(owner, args);
    }
    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
}
