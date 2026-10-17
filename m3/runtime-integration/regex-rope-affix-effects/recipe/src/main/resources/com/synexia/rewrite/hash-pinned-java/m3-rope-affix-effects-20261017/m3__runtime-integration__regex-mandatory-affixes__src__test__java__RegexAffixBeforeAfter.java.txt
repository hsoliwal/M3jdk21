// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.regex.Pattern;

/**
 * Differential proof for the exact source-extracted full classes.
 * The owner is an instrumented immutable fixture, not a product M3JDK.
 */
public final class RegexAffixBeforeAfter {
    private static long checks;
    private static final Gate BEFORE = new Gate(RegexBefore.class);
    private static final Gate AFTER = new Gate(RegexAfter.class);

    public static void main(String[] arguments) throws Exception {
        explicitBoundaries();
        randomWitnesses();
        generalDifferential();
        cursorAndColdWork();
        warmCacheIdentity();
        unknownAndRefused();
        retainedBound();
        System.out.println("M3_REGEX_AFFIX_DIFFERENTIAL_PASS checks=" + checks);
    }

    private static M3String make(String text, int shape) {
        if (text.length() < 2) return M3String.text(text);
        int cut = Math.min(2, text.length() - 1);
        M3String source = M3String.join(M3String.text(text.substring(0, cut)),
                M3String.join(M3String.text(text.substring(cut, text.length() - 1)),
                        M3String.text(text.substring(text.length() - 1))));
        if (shape == 0) return source;
        M3String padded = M3String.join(M3String.text("PAD"),
                M3String.join(source, M3String.text("END")));
        if (shape == 1) return padded.slice(3, 3 + text.length());
        M3String shared = M3String.join(padded, padded);
        return shared.slice(padded.length() + 3, padded.length() + 3 + text.length());
    }

    private static void compare(M3String source, String expression, boolean prepared) {
        if (prepared) source.prepareFacts();
        M3StringFacts facts = source.factsIfPrepared();
        int before = RegexBefore.tryMatch(source, expression);
        int after = RegexAfter.tryMatch(source, expression);
        require(facts == source.factsIfPrepared(), "no forced whole-source facts");
        boolean expected = Pattern.matches(expression, source.toString());
        if (before >= 0) require((before == 1) == expected, "before/Pattern parity");
        if (after >= 0) require((after == 1) == expected, "after/Pattern parity");
        require(before < 0 || after >= 0, "no lost exact admission");
        if (before >= 0 && after >= 0) require(before == after, "admitted parity");
        require(RegexAfter.tryMatch(source, expression) == after, "warm decision parity");
    }

    private static void explicitBoundaries() throws Exception {
        String[] texts = {
                "aXcabcZZxyz", "abcZZxyzxYz", "abcxyz", "abcMIDxyz",
                "aXcZZxyz", "abcZZxQz", "abcZZxz", "bcZZxyz", "abcxy",
                "abc\nxyz", "abc\ud83d\ude42xyz", "abc\ud83dxyz",
                "aXcabc\ud83d\ude42xyz", "abc\ud83d\ude42xyzxYz"
        };
        String[] expressions = {
                "abc.*xyz", "abc.*xyz", "abc.*xyz", "abc.*xyz",
                "a.c.*xyz", "abc.*x.z", "abc.*xy*z", "a*bc.*xyz", "abc.*xyz*",
                "abc.*xyz", "abc.*xyz", "abc.*xyz", "abc.*xyz", "abc.*xyz"
        };
        for (int i = 0; i < texts.length; i++) {
            boolean rejected = i < 2 || i > 11;
            for (int shape = 0; shape < 3; shape++) {
                for (int prepared = 0; prepared < 2; prepared++) {
                    M3String source = make(texts[i], shape);
                    if (prepared != 0) source.prepareFacts();
                    require(AFTER.rejects(source, expressions[i]) == rejected,
                            "complete literal boundary case=" + i);
                    // Required grams are deliberately present on both negative
                    // boundaries. The old necessary conditions cannot reject.
                    if (rejected) require(!BEFORE.rejects(source, expressions[i]),
                            "new guard adds a decisive ordered boundary");
                    compare(source, expressions[i], prepared != 0);
                }
            }
        }
        M3String shared = M3String.join(M3String.text("abc"), M3String.text("xyz"));
        for (int i = 0; i < 8; i++) shared = M3String.join(shared, shared);
        compare(shared, "abc.*xyz", false);
        compare(shared.slice(6, shared.length() - 6), "abc.*xyz", true);
    }

    private static String literal(Random random, int length) {
        String alphabet = "abcABC012";
        StringBuilder text = new StringBuilder(length);
        for (int i = 0; i < length; i++) text.append(alphabet.charAt(random.nextInt(alphabet.length())));
        return text.toString();
    }

    private static void randomWitnesses() throws Exception {
        Random random = new Random(0x4d33414646495831L);
        for (int trial = 0; trial < 1_024; trial++) {
            String prefix = literal(random, 2 + random.nextInt(8));
            String suffix = literal(random, 2 + random.nextInt(8));
            String middle = literal(random, random.nextInt(12));
            String expression = prefix + ((trial & 1) == 0 ? ".*"
                    : "a*".repeat(33) + ".*") + suffix;
            String wrongPrefix = prefix.substring(0, 1)
                    + (prefix.charAt(1) == 'Q' ? 'R' : 'Q') + prefix.substring(2)
                    + prefix + middle + suffix;
            int index = suffix.length() - 2;
            String wrongSuffix = prefix + middle + suffix + suffix.substring(0, index)
                    + (suffix.charAt(index) == 'Q' ? 'R' : 'Q') + suffix.substring(index + 1);
            String[] texts = {prefix + middle + suffix, wrongPrefix, wrongSuffix};
            for (int i = 0; i < texts.length; i++) {
                M3String source = make(texts[i], trial % 3);
                boolean prepared = (trial & 2) == 0;
                if (prepared) source.prepareFacts();
                require(AFTER.rejects(source, expression) == (i != 0), "affix witness guard");
                if (i != 0) require(!BEFORE.rejects(source, expression), "grams do not replace order");
                compare(source, expression, prepared);
            }
        }
    }

    private static void generalDifferential() {
        Random random = new Random(0x4d33414646495832L);
        String[] tokens = {"a", "b", "c", "Q", ".", "a*", "b*", "c*", ".*"};
        String[] chunks = {"a", "b", "c", "ab", "", "\n", "\r", "\u0085",
                "\u2028", "\u2029", "\u03b2", "\ud83d", "\ude42"};
        for (int trial = 0; trial < 4_096; trial++) {
            StringBuilder regex = new StringBuilder();
            int count = 1 + random.nextInt(48);
            for (int i = 0; i < count; i++) regex.append(tokens[random.nextInt(tokens.length)]);
            if (regex.indexOf("*") < 0) regex.append("a*");
            StringBuilder text = new StringBuilder();
            for (int i = random.nextInt(18); i > 0; i--) {
                text.append(chunks[random.nextInt(chunks.length)]);
            }
            compare(make(text.toString(), trial % 3), regex.toString(), (trial & 1) == 0);
        }
    }

    private static void resetReads() {
        M3StringAtom.reads = 0;
        M3String.wrapperReads = 0;
    }

    private static void cursorAndColdWork() throws Exception {
        String prefix = "abcdefghijkl", suffix = "MNOPQRSTUVWX";
        M3String positive = make(prefix + "m".repeat(2048) + suffix, 2);
        positive.prepareFacts();
        resetReads();
        require(!BEFORE.rejects(positive, prefix + ".*" + suffix), "old positive guard");
        long oldWrapperReads = M3String.wrapperReads;
        resetReads();
        require(!AFTER.rejects(positive, prefix + ".*" + suffix), "new positive guard");
        require(M3StringAtom.reads == prefix.length() + suffix.length(), "affix scalar read bound");
        require(M3String.wrapperReads == oldWrapperReads, "affixes use cursor rather than root charAt");
        System.out.println("M3_REGEX_AFFIX_CURSOR_WORK scalar_reads=" + M3StringAtom.reads
                + " root_wrapper_reads=" + M3String.wrapperReads + " affix_units=24");

        for (boolean suffixFailure : new boolean[] {false, true}) {
            String text = suffixFailure ? "abc" + "m".repeat(2048) + "xyzxYz"
                    : "aXcabc" + "m".repeat(2048) + "xyz";
            M3String source = make(text, 2);
            source.prepareFacts();
            resetReads();
            require(RegexBefore.tryMatch(source, "abc.*xyz") == 0, "cold before rejection");
            long oldReads = M3StringAtom.reads;
            resetReads();
            require(RegexAfter.tryMatch(source, "abc.*xyz") == 0, "cold after rejection");
            long nextReads = M3StringAtom.reads;
            require(oldReads >= text.length(), "baseline enters full DP");
            require(nextReads <= 6, "affix rejection work independent of middle length");
            System.out.println("M3_REGEX_AFFIX_COLD_WORK suffix_failure=" + suffixFailure
                    + " text_units=" + text.length() + " before_reads=" + oldReads
                    + " after_reads=" + nextReads);
        }
    }

    private static void warmCacheIdentity() throws Exception {
        M3String source = make("abc" + "m".repeat(120) + "xyz", 2);
        String expression = "abc.*xyz";
        require(RegexAfter.tryMatch(source, expression) == 1, "warm seed");
        resetReads();
        require(RegexAfter.tryMatch(source, expression) == 1, "warm exact hit");
        require(M3StringAtom.reads == 0 && M3String.wrapperReads == 0,
                "warm exact root hit does not reread text");
        Object plan = AFTER.plan(expression);
        long[] closure = (long[]) field(plan.getClass(), "closure").get(plan);
        long incoming = closure[0];
        long coordinate = ((long) source.start() << 32)
                | Integer.toUnsignedLong(source.length());
        Field effects = field(AFTER.table.getClass(), "effects");
        @SuppressWarnings("unchecked")
        AtomicReferenceArray<Object> entries = (AtomicReferenceArray<Object>) effects.get(AFTER.table);
        int slot = (System.identityHashCode(source.owner()) ^ System.identityHashCode(plan)
                ^ Long.hashCode(Long.rotateLeft(coordinate, 13) ^ incoming)) & 255;
        Class<?> effectType = Class.forName("RegexAfter$Effect");
        Constructor<?> effect = effectType.getDeclaredConstructor(
                WeakReference.class, long.class, plan.getClass(), long.class, long.class);
        effect.setAccessible(true);
        Constructor<?> planConstructor = plan.getClass().getDeclaredConstructor(int[].class, int.class);
        planConstructor.setAccessible(true);
        Object differentPlan = planConstructor.newInstance(
                ((int[]) field(plan.getClass(), "atoms").get(plan)).clone(), expression.length());
        for (int mutation = 0; mutation < 5; mutation++) {
            WeakReference<M3StringOwner> owner = new WeakReference<>(mutation == 0
                    ? make(source.toString(), 0).owner() : source.owner());
            if (mutation == 4) owner.clear();
            Object wrong = effect.newInstance(owner, mutation == 1 ? coordinate + 1 : coordinate,
                    mutation == 2 ? differentPlan : plan,
                    mutation == 3 ? incoming ^ 1L : incoming, 0L);
            entries.set(slot, wrong);
            require(RegexAfter.tryMatch(source, expression) == 1,
                    "cache collision must reject wrong identity lane=" + mutation);
        }
        entries.set(slot, effect.newInstance(new WeakReference<>(source.owner()),
                coordinate, plan, incoming, -1L));
        resetReads();
        require(RegexAfter.tryMatch(source, expression) == -1,
                "unresolved cached UNKNOWN still delegates");
        require(M3StringAtom.reads > 0,
                "cached UNKNOWN remains eligible for necessary-condition checks");
        System.out.println("M3_REGEX_AFFIX_CACHE_PASS key_lanes=5 warm_text_reads=0");
    }

    private static void unknownAndRefused() {
        M3String source = make("abcMMMMMMM\ud83d\ude42MMMMMMMxyz", 2);
        String expression = "abc.*Q.*xyz";
        require(RegexAfter.tryMatch(source, expression) == -1, "unprepared possible surrogate delegates");
        source.prepareFacts();
        require(RegexAfter.tryMatch(source, expression) == 0, "new facts can resolve cached UNKNOWN");
        compare(make("abc\ud83d\ude42xyz", 1), "abc.*xyz", false);
        for (String regex : new String[] {"a**", "*a", "abc", "(a*)b", "a+b", "a?b",
                "a|b", "^ab", "[a-z]+", "\\Qa\\E", "(?i)ab", "", "β*", "a*".repeat(65)}) {
            require(RegexAfter.tryMatch(source, regex) == -1, "refused syntax/length");
        }
        require(RegexAfter.tryMatch(M3String.text("a".repeat(8193)), "a*") == -1,
                "existing source-work limit retained");
    }

    private static void retainedBound() throws Exception {
        Method before = RegexBefore.class.getDeclaredMethod("maximumRetainedPrimitiveBytes");
        Method after = RegexAfter.class.getDeclaredMethod("maximumRetainedPrimitiveBytes");
        before.setAccessible(true);
        after.setAccessible(true);
        long oldBytes = (long) before.invoke(null), newBytes = (long) after.invoke(null);
        require(newBytes - oldBytes == (64L + 256L) * 2L * Integer.BYTES, "bounded two-int plan growth");
        System.out.println("M3_REGEX_AFFIX_RETAINED_PRIMITIVES before=" + oldBytes
                + " after=" + newBytes + " maximum_delta=" + (newBytes - oldBytes));
    }

    private static Field field(Class<?> owner, String name) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static void require(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }

    private static final class Gate {
        final Object table;
        private final Method prepare, rejects;
        Gate(Class<?> owner) {
            try {
                Method tables = owner.getDeclaredMethod("tables");
                tables.setAccessible(true);
                table = tables.invoke(null);
                prepare = owner.getDeclaredMethod("prepare", String.class, table.getClass());
                prepare.setAccessible(true);
                Class<?> plan = Class.forName(owner.getName() + "$Plan");
                rejects = owner.getDeclaredMethod("rejectsByPrecompute", M3String.class, plan);
                rejects.setAccessible(true);
            } catch (ReflectiveOperationException failure) {
                throw new ExceptionInInitializerError(failure);
            }
        }
        Object plan(String expression) throws Exception {
            return prepare.invoke(null, expression, table);
        }
        boolean rejects(M3String source, String expression) throws Exception {
            return (boolean) rejects.invoke(null, source, plan(expression));
        }
    }
}
