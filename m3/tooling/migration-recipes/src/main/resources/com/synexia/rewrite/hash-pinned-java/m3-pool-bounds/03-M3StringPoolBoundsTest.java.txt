// SPDX-License-Identifier: Apache-2.0
/*
 * @test
 * @summary Pool bounds: the VM-local native byte budget is reserved at admission and released
 *          through the local reference queue; exhaustion takes the Bits.reserveMemory route
 *          (drain, reference processing, one GC, bounded retries) and then refuses admission with
 *          null instead of OutOfMemoryError, so every consumer keeps its flat result; the canonical
 *          tuple retention cap keeps composing without retention; a lexicon failure is reported
 *          once; a flat replace target drives the search without entering the pool and the results
 *          survive refusal flat
 * @modules java.base/java.lang:+open
 * @run main/othervm -Dm3.string.pool.maxBytes=16777216 -Dm3.string.pool.maxTuples=64 M3StringPoolBoundsTest
 * @run main/othervm -Dm3.string.pool.maxBytes=16777216 -Dm3.string.pool.maxTuples=64 -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringPoolBoundsTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringPoolBoundsTest
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.BooleanSupplier;

public class M3StringPoolBoundsTest {
    private static final long MIB = 1L << 20;
    private static final long REFUSAL_HEADROOM_LIMIT = 64L * MIB;
    private static final long NOISE = 1L << 16;
    private static long checks;
    private static boolean flagOn;
    private static long maxLocalBytes;
    private static long maxRetainedTuples;

    private static Method internChars;
    private static Method internUnit;
    private static Method localNativeBytes;
    private static Method retainedTuples;
    private static Method parseBound;
    private static Method initializeLexicon;
    private static Method poolConcat;
    private static Method canonicalize;
    private static Method join;
    private static Method joinDesignated;
    private static Method replaceFlatTarget;
    private static Method replaceAt;
    private static Method m3Concat;
    private static Field nextSlowReserveNanos;
    private static Field lexicon;
    private static Field lexiconFailureReported;
    private static Field owner;
    private static Field stringM3;

    /** Strong holds that must not live in any test frame (the back-pressure proof drops them). */
    private static Object hold;
    private static final List<Object> HOLDS = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        bind();
        bounds();
        accounting();
        refusal();
        tupleCap();
        lexiconOnce();
        replaceTransient();
        refusedConsumers();
        System.out.println("M3StringPoolBoundsTest checks=" + checks + " flagOn=" + flagOn
                + " maxBytes=" + maxLocalBytes + " maxTuples=" + maxRetainedTuples);
    }

    private static void bind() throws Exception {
        Class<?> pool = Class.forName("java.lang.M3StringPool");
        Class<?> m3 = Class.forName("java.lang.M3String");
        Class<?> helper = Class.forName("java.lang.StringConcatHelper");
        internChars = accessible(pool.getDeclaredMethod("internChars", char[].class, int.class, int.class));
        internUnit = accessible(pool.getDeclaredMethod("internUnit", char.class));
        localNativeBytes = accessible(pool.getDeclaredMethod("localNativeBytes"));
        retainedTuples = accessible(pool.getDeclaredMethod("retainedTuples"));
        parseBound = accessible(pool.getDeclaredMethod("parseBound", String.class, long.class));
        initializeLexicon = accessible(pool.getDeclaredMethod("initializeLexicon", String.class));
        poolConcat = accessible(pool.getDeclaredMethod("concat", m3, m3));
        canonicalize = accessible(m3.getDeclaredMethod("canonicalize", String.class));
        join = accessible(m3.getDeclaredMethod("join", String[].class));
        joinDesignated = accessible(m3.getDeclaredMethod("joinDesignated",
                String.class, String.class, String.class, String[].class, int.class));
        replaceFlatTarget = accessible(m3.getDeclaredMethod("replaceFlatTarget",
                String.class, String.class, m3, int.class));
        replaceAt = accessible(m3.getDeclaredMethod("replaceAt", m3, m3, int.class));
        m3Concat = accessible(helper.getDeclaredMethod("m3Concat", String[].class, String[].class));
        nextSlowReserveNanos = accessible(pool.getDeclaredField("nextSlowReserveNanos"));
        lexicon = accessible(pool.getDeclaredField("lexicon"));
        lexiconFailureReported = accessible(pool.getDeclaredField("lexiconFailureReported"));
        owner = accessible(m3.getDeclaredField("owner"));
        stringM3 = accessible(String.class.getDeclaredField("m3"));
        flagOn = (boolean) accessible(m3.getDeclaredField("ready")).get(null);
        maxLocalBytes = (long) accessible(pool.getDeclaredField("MAX_LOCAL_BYTES")).get(null);
        maxRetainedTuples = (long) accessible(pool.getDeclaredField("MAX_RETAINED_TUPLES")).get(null);
    }

    /** Property parsing and the effective bounds (MaxDirectMemorySize semantics). */
    private static void bounds() throws Exception {
        check(parse(null, 7L) == 7L, "absent property falls back");
        check(parse("0", 7L) == 0L, "zero is an explicit bound");
        check(parse("123", 7L) == 123L, "plain byte count");
        check(parse("-1", 7L) == 7L, "-1 falls back as MaxDirectMemorySize");
        check(parse("junk", 7L) == 7L, "unparsable falls back");
        check(parse("", 7L) == 7L, "empty falls back");
        String bytesProperty = System.getProperty("m3.string.pool.maxBytes");
        long expectedBytes = bytesProperty == null
                ? Runtime.getRuntime().maxMemory() : Long.parseLong(bytesProperty);
        check(maxLocalBytes == expectedBytes, "MAX_LOCAL_BYTES " + maxLocalBytes + " vs " + expectedBytes);
        String tuplesProperty = System.getProperty("m3.string.pool.maxTuples");
        long expectedTuples = tuplesProperty == null ? 1L << 20 : Long.parseLong(tuplesProperty);
        check(maxRetainedTuples == expectedTuples, "MAX_RETAINED_TUPLES " + maxRetainedTuples);
    }

    /** Bytes are accounted exactly per admitted atom and come back through the reference queue. */
    private static void accounting() throws Exception {
        long baseline = bytes();
        long latin1 = 100_000L;
        Object first = intern(latin1Chars(0x4c31, (int) latin1));
        check(first != null, "latin1 admission");
        long afterFirst = bytes();
        checkDelta(afterFirst - baseline, latin1, "latin1 bytes accounted once");
        Object again = intern(latin1Chars(0x4c31, (int) latin1));
        check(owner.get(again) == owner.get(first), "same content hits the same atom");
        checkDelta(bytes() - afterFirst, 0L, "a hit reserves nothing");
        long utf16Units = 50_000L;
        Object wide = intern(utf16Chars(0x5531, (int) utf16Units));
        check(wide != null, "utf16 admission");
        checkDelta(bytes() - afterFirst, utf16Units * 2L, "utf16 bytes accounted at two per unit");
        Object unit = internUnit.invoke(null, 'q');
        check(owner.get(internUnit.invoke(null, 'q')) == owner.get(unit), "unit lane canonical");
        first = null;
        again = null;
        wide = null;
        unit = null;
        check(gcUntil(() -> bytesQuiet() <= baseline + (flagOn ? NOISE : 0L)),
                "reclaimed blocks return their bytes through the local queue: " + bytesQuiet() + " vs " + baseline);
    }

    /** Exhaustion: refusal without OutOfMemoryError, the hold-off, and the GC-assisted retry. */
    private static void refusal() throws Exception {
        settle();
        long headroom = maxLocalBytes - bytes();
        if (headroom > REFUSAL_HEADROOM_LIMIT) {
            System.out.println("refusal: skipped, headroom " + headroom + " bytes exceeds the proof budget");
            return;
        }
        check(headroom >= MIB, "refusal proof needs at least 1 MiB of headroom, found " + headroom);
        int length = (int) (headroom / 2L + 1L);
        long before = bytes();
        hold = intern(latin1Chars(0x5231, length));
        check(hold != null, "first half of the headroom is admitted");
        checkDelta(bytes() - before, length, "first half accounted");
        long afterHold = bytes();

        armSlowPath();
        long started = System.nanoTime();
        Object refused = intern(latin1Chars(0x5232, length));
        long elapsedMillis = (System.nanoTime() - started) / 1_000_000L;
        check(refused == null, "over budget: admission refused with null, not thrown");
        check(bytes() == afterHold, "a refused admission leaves the accounting untouched");
        check(nextSlowReserveNanos.getLong(null) - System.nanoTime() > 0L, "a failed cycle arms the hold-off");
        System.out.println("refusal: GC-assisted cycle refused after " + elapsedMillis + " ms");
        check(intern(latin1Chars(0x5233, length)) == null, "held off: refused again without a cycle");
        check(bytes() == afterHold, "held-off refusal leaves the accounting untouched");
        check(internUnit.invoke(null, 'é') != null, "the unit lane never refuses");

        Object oversized = intern(latin1Chars(0x5234, (int) Math.min(Integer.MAX_VALUE - 8L, maxLocalBytes + 1L)));
        check(oversized == null, "a request above the whole budget is refused, never thrown");

        hold = null;
        armSlowPath();
        Object admitted = intern(latin1Chars(0x5232, length));
        check(admitted != null, "the slow path reclaimed the dropped atom (gc + reference processing + drain)");
        checkDelta(bytes() - before, length, "the reclaimed bytes were handed to the new atom");
        admitted = null;
        check(gcUntil(() -> bytesQuiet() <= before + (flagOn ? NOISE : 0L)), "refusal proof cleaned up");
    }

    /** Beyond the cap a composition still forms a correct tuple, just without canonical retention. */
    private static void tupleCap() throws Exception {
        long registered = tuples();
        check(registered <= maxRetainedTuples, "registered tuples never exceed the cap");
        if (maxRetainedTuples > 4096L) {
            Object left = intern(latin1Chars(0x5431, 9));
            Object right = intern(latin1Chars(0x5432, 11));
            Object tuple = poolConcat.invoke(null, left, right);
            check(owner.get(poolConcat.invoke(null, left, right)) == owner.get(tuple), "below the cap: canonical reuse");
            System.out.println("tupleCap: cap " + maxRetainedTuples + " not reachable in this run");
            return;
        }
        int mine = (int) maxRetainedTuples + 8;
        int expectedRetained = (int) Math.max(0L, Math.min(mine, maxRetainedTuples - registered));
        int retainedMine = 0;
        for (int index = 0; index < mine; index++) {
            char[] leftChars = latin1Chars(0x5500 + index, 5);
            char[] rightChars = latin1Chars(0x5600 + index, 6);
            Object left = intern(leftChars);
            Object right = intern(rightChars);
            Object tuple = poolConcat.invoke(null, left, right);
            HOLDS.add(left);
            HOLDS.add(right);
            HOLDS.add(tuple);
            check(sameChars((CharSequence) tuple, concat(leftChars, rightChars)), "composition content " + index);
            if (owner.get(poolConcat.invoke(null, left, right)) == owner.get(tuple)) retainedMine++;
            check(tuples() <= maxRetainedTuples, "cap holds while filling, index " + index);
        }
        if (flagOn) {
            check(retainedMine <= expectedRetained, "retained " + retainedMine + " within " + expectedRetained);
        } else {
            check(retainedMine == expectedRetained, "retained exactly " + expectedRetained + ", found " + retainedMine);
        }
        HOLDS.clear();
        check(gcUntil(() -> tuplesQuiet() <= registered), "expunged tuples free the cap: " + tuplesQuiet());
        Object left = intern(latin1Chars(0x5701, 7));
        Object right = intern(latin1Chars(0x5702, 8));
        Object tuple = poolConcat.invoke(null, left, right);
        if (tuples() <= maxRetainedTuples) {
            check(owner.get(poolConcat.invoke(null, left, right)) == owner.get(tuple), "retention resumes under the cap");
        }
    }

    /** A lexicon that cannot be opened is reported once per VM, whatever the failure. */
    private static void lexiconOnce() throws Exception {
        Object previous = lexicon.get(null);
        PrintStream previousErr = System.err;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        Path missing = Path.of(System.getProperty("java.io.tmpdir"),
                "m3-pool-bounds-missing-" + System.nanoTime() + ".lex");
        Path corrupt = Files.createTempFile("m3-pool-bounds-corrupt-", ".lex");
        try {
            Files.write(corrupt, new byte[128]);
            lexiconFailureReported.setBoolean(null, false);
            System.setErr(new PrintStream(captured, true, StandardCharsets.UTF_8));
            for (int round = 0; round < 3; round++) {
                lexicon.set(null, null);
                initializeLexicon.invoke(null, missing.toString());
                check(lexicon.get(null) != null, "failure leaves the unavailable sentinel, round " + round);
            }
            lexicon.set(null, null);
            initializeLexicon.invoke(null, corrupt.toString());
            check(lexicon.get(null) != null, "corrupt image leaves the unavailable sentinel");
        } finally {
            System.setErr(previousErr);
            lexicon.set(null, previous);
            try {
                Files.deleteIfExists(corrupt);
            } catch (IOException mappedOnWindows) {
                corrupt.toFile().deleteOnExit();
            }
        }
        String report = captured.toString(StandardCharsets.UTF_8);
        int reports = report.split("M3StringPool: lexicon ", -1).length - 1;
        check(reports == 1, "exactly one report for four failures, found " + reports + ": " + report.strip());
        check(report.contains(missing.toString()), "the report names the first failing file");
    }

    /** A flat target only drives the search: nothing but the replacement is admitted. */
    private static void replaceTransient() throws Exception {
        char[] target = latin1Chars(0x5831, 40);
        char[] replacement = latin1Chars(0x5832, 12);
        char[] content = concat(concat(concat("alpha-".toCharArray(), target), concat("-beta-".toCharArray(), target)),
                "-gamma".toCharArray());
        char[] expected = concat(concat(concat("alpha-".toCharArray(), replacement), concat("-beta-".toCharArray(), replacement)),
                "-gamma".toCharArray());
        Object receiver = intern(content);
        Object replacementM3 = intern(replacement);
        String targetString = new String(target);
        String positions = new String(content);
        int found = positions.indexOf(targetString);
        check(found == 6, "first match");
        long before = bytes();
        Object replaced = replaceFlatTarget.invoke(receiver, positions, targetString, replacementM3, found);
        check(sameChars((CharSequence) replaced, expected), "flat-target replacement content");
        checkDelta(bytes() - before, 0L, "flat target replacement admits nothing");
        Object targetM3 = intern(target);
        long beforeAt = bytes();
        Object replacedM3 = replaceAt.invoke(receiver, targetM3, replacementM3, found);
        check(sameChars((CharSequence) replacedM3, expected), "M3-target replacement content");
        checkDelta(bytes() - beforeAt, 0L, "M3 target replacement admits nothing");
        if (!flagOn) return;

        // Public consumers (receivers are M3-backed only under the flag).
        String source = new String(content);
        check(stringM3.get(source) != null, "flag-on receiver is M3-backed");
        check(sameChars(source.replace(targetString, new String(replacement)), expected), "String.replace content");
        check(source.replace(new String(latin1Chars(0x5833, 9)), "x") == source, "absent target keeps identity");
        check(sameChars(source.replaceAll(targetString, new String(replacement)), expected), "replaceAll literal lane");
        check(sameChars(source.replaceFirst("beta", "BETA"), new String(content).replace("beta", "BETA").toCharArray()),
                "replaceFirst literal lane");
    }

    /** With the budget exhausted every composition keeps its flat result and refuses quietly. */
    private static void refusedConsumers() throws Exception {
        settle();
        long before = bytes();
        long headroom = maxLocalBytes - before;
        if (headroom > REFUSAL_HEADROOM_LIMIT) {
            System.out.println("refusedConsumers: skipped, headroom " + headroom);
            return;
        }
        check(headroom >= 1024L, "refusal fallback needs headroom, found " + headroom);
        char[] leftChars = latin1Chars(0x5931, 13);
        char[] rightChars = latin1Chars(0x5932, 17);
        char[] contentChars = concat(concat("pre-".toCharArray(), leftChars), "-post".toCharArray());
        String receiver = new String(contentChars);
        exhaust();
        long filled = bytes();
        check(maxLocalBytes - filled < 4L, "budget exhausted to under four bytes: " + (maxLocalBytes - filled));
        String left = new String(leftChars);
        String right = new String(rightChars);
        check(stringM3.get(left) == null && stringM3.get(right) == null, "refused Strings are flat");
        check(canonicalize.invoke(null, left) == null, "canonicalize refuses");
        check(join.invoke(null, (Object) new String[] {left, right}) == null, "join refuses");
        check(joinDesignated.invoke(null, "", "", "-", new String[] {left, right}, 2) == null, "joinDesignated refuses");
        String folded = (String) m3Concat.invoke(null, new String[] {"c0", null, "c2"}, new String[] {left, right});
        check(sameChars(folded, concat(concat("c0".toCharArray(), leftChars), concat(rightChars, "c2".toCharArray()))),
                "refused m3Concat builds the flat one-array result");
        check(stringM3.get(folded) == null, "refused m3Concat result is flat");
        check(sameChars(left.concat(right), concat(leftChars, rightChars)), "String.concat under refusal");
        check(sameChars(String.join("-", left, right), concat(concat(leftChars, "-".toCharArray()), rightChars)),
                "String.join under refusal");
        check(sameChars(left + right, concat(leftChars, rightChars)), "indy concat under refusal");
        check(sameChars(receiver.replace(left, right), concat(concat("pre-".toCharArray(), rightChars), "-post".toCharArray())),
                "String.replace under refusal");
        check(sameChars(receiver.replace("", "+"), emptyTargetOracle(contentChars, '+')),
                "empty-target replace under refusal");
        check(sameChars(receiver.replaceAll(left, right), concat(concat("pre-".toCharArray(), rightChars), "-post".toCharArray())),
                "replaceAll literal lane under refusal");
        check(bytes() == filled, "refused consumers reserve nothing");
        HOLDS.clear();
        check(gcUntil(() -> bytesQuiet() <= before + (flagOn ? NOISE : 0L)), "refusal fallback cleaned up");
    }

    /** Fills the budget with held junk atoms of shrinking sizes until fewer than four bytes remain. */
    private static void exhaust() throws Exception {
        int seed = 0x6000;
        for (int size : new int[] {1 << 20, 1 << 16, 1 << 12, 256, 16, 4}) {
            for (int attempts = 0; attempts < 64; attempts++) {
                Object junk = intern(latin1Chars(seed++, size));
                if (junk == null) break;
                HOLDS.add(junk);
            }
        }
    }

    /** Lets earlier garbage atoms return their bytes so headroom is measured on live atoms only. */
    private static void settle() throws Exception {
        for (int round = 0; round < 3; round++) {
            System.gc();
            Thread.sleep(10L);
            bytes();
        }
    }

    private static char[] emptyTargetOracle(char[] content, char insert) {
        char[] oracle = new char[content.length * 2 + 1];
        oracle[0] = insert;
        for (int index = 0; index < content.length; index++) {
            oracle[2 * index + 1] = content[index];
            oracle[2 * index + 2] = insert;
        }
        return oracle;
    }

    private static boolean sameChars(CharSequence actual, char[] expected) {
        if (actual.length() != expected.length) return false;
        for (int index = 0; index < expected.length; index++) {
            if (actual.charAt(index) != expected[index]) return false;
        }
        return true;
    }

    private static char[] concat(char[] left, char[] right) {
        char[] joined = new char[left.length + right.length];
        System.arraycopy(left, 0, joined, 0, left.length);
        System.arraycopy(right, 0, joined, left.length, right.length);
        return joined;
    }

    private static Object intern(char[] source) throws Exception {
        return internChars.invoke(null, source, 0, source.length);
    }

    private static char[] latin1Chars(int seed, int length) {
        Random random = new Random(seed * 0x9e3779b97f4a7c15L);
        char[] chars = new char[length];
        for (int index = 0; index < length; index++) chars[index] = (char) ('a' + random.nextInt(26));
        return chars;
    }

    private static char[] utf16Chars(int seed, int length) {
        char[] chars = latin1Chars(seed, length);
        chars[0] = 'Ā';
        return chars;
    }

    private static long parse(String value, long fallback) throws Exception {
        return (long) parseBound.invoke(null, value, fallback);
    }

    private static long bytes() throws Exception {
        return (long) localNativeBytes.invoke(null);
    }

    private static long bytesQuiet() {
        try {
            return bytes();
        } catch (Exception failure) {
            throw new AssertionError(failure);
        }
    }

    private static long tuples() throws Exception {
        return (long) retainedTuples.invoke(null);
    }

    private static long tuplesQuiet() {
        try {
            return tuples();
        } catch (Exception failure) {
            throw new AssertionError(failure);
        }
    }

    private static void armSlowPath() throws Exception {
        nextSlowReserveNanos.setLong(null, System.nanoTime());
    }

    private static boolean gcUntil(BooleanSupplier condition) throws Exception {
        for (int round = 0; round < 200; round++) {
            if (condition.getAsBoolean()) return true;
            System.gc();
            Thread.sleep(5L);
        }
        return condition.getAsBoolean();
    }

    private static void checkDelta(long delta, long expected, String message) {
        if (flagOn) {
            check(delta >= expected && delta - expected < NOISE, message + ": " + delta + " vs " + expected);
        } else {
            check(delta == expected, message + ": " + delta + " vs " + expected);
        }
    }

    private static <T extends java.lang.reflect.AccessibleObject> T accessible(T member) {
        member.setAccessible(true);
        return member;
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
