package jdk.internal.m3.collections.mat;

import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

/**
 * Identical-behaviour oracle: LogicEngineReference is the unpatched class renamed; both
 * engines run the same cases and must agree on statements (order included), rounds, derived
 * count, exception class and message, and on the private engine fields.
 */
final class ClosureIndexTest {
    private static final long[] SPECIAL = {
        Long.MIN_VALUE, Long.MIN_VALUE + 1, -1L, 0L, 1L, 2L, 3L, 1L << 32, (1L << 32) + 1,
        Integer.MAX_VALUE, Integer.MIN_VALUE, Long.MAX_VALUE - 1, Long.MAX_VALUE, 7L, 42L};
    private static int cases;
    private static int failures;
    private static int thrown;
    private static int probes;
    private static final Map<String, Integer> KINDS = new java.util.TreeMap<>();

    @Test
    void sameAsMaster() throws Exception {
        SplittableRandom random = new SplittableRandom(0x5eedL);
        for (int index = 0; index < 20000 && failures < 5; index++) {
            randomCase(random, index);
        }
        chainCases();
        collisionCase();
        nullCases();
        if (failures > 0) {
            throw new AssertionError(failures + " mismatches in " + cases + " cases (see output)");
        }
    }

    /** A lexicon p0..p(n-1) with the given flags and inverse ids (-1 for none). */
    private static PredicateLexicon lexicon(int[] flags, int[] inverse) {
        List<PredicateLexicon.Entry> entries = new ArrayList<>();
        for (int id = 0; id < flags.length; id++) {
            entries.add(new PredicateLexicon.Entry("p" + id, flags[id],
                    inverse[id] >= 0 ? "p" + inverse[id] : null, null));
        }
        return PredicateLexicon.compile(entries);
    }

    private static void randomCase(SplittableRandom random, int index) {
        int size = 1 + random.nextInt(6);
        int[] flags = new int[size];
        int[] inverse = new int[size];
        for (int id = 0; id < size; id++) {
            flags[id] = random.nextInt(8);
            inverse[id] = random.nextInt(3) == 0 ? random.nextInt(size) : -1;
        }
        long[] nodes = nodes(random, 1 + random.nextInt(random.nextBoolean() ? 4 : 12));
        int contexts = 1 + random.nextInt(3);
        List<Statement> seeds = new ArrayList<>();
        int n = random.nextInt(random.nextInt(10) == 0 ? 120 : 30);
        for (int k = 0; k < n; k++) {
            int predicate = random.nextInt(200) == 0
                    ? size + random.nextInt(2) : random.nextInt(size);
            seeds.add(random.nextInt(300) == 0 ? null : new Statement(
                    pick(random, nodes), predicate, pick(random, nodes),
                    SPECIAL[random.nextInt(contexts)]));
        }
        int maxRounds = 1 + random.nextInt(8);
        int maxStatements = 1 + random.nextInt(random.nextBoolean() ? 60 : 3000);
        compare("random#" + index, flags, inverse, seeds, maxRounds, maxStatements);
    }

    private static long[] nodes(SplittableRandom random, int count) {
        long[] nodes = new long[count];
        for (int k = 0; k < count; k++) {
            nodes[k] = random.nextBoolean()
                    ? SPECIAL[random.nextInt(SPECIAL.length)] : random.nextLong();
        }
        return nodes;
    }

    private static long pick(SplittableRandom random, long[] nodes) {
        return nodes[random.nextInt(nodes.length)];
    }

    private static void chainCases() {
        int[] flags = {PredicateFlags.TRANSITIVE,
            PredicateFlags.SYMMETRIC | PredicateFlags.TRANSITIVE, 0};
        int[] inverse = {-1, -1, 2};
        for (int length : new int[] {2, 3, 8, 33, 64, 65, 120}) {
            List<Statement> seeds = new ArrayList<>();
            for (int k = 0; k < length; k++) {
                seeds.add(new Statement(k, 0, k + 1, 0L));
                seeds.add(new Statement(length - k, 1, length - k - 1, 9L));
            }
            for (int rounds : new int[] {1, 5, 6, 7, 8, 64}) {
                compare("chain" + length + "/r" + rounds, flags, inverse, seeds, rounds, 1 << 22);
            }
        }
        List<Statement> big = new ArrayList<>();
        for (int k = 0; k < 120; k++) {
            big.add(new Statement(k, 0, k + 1, 0L));
        }
        long t0 = System.nanoTime();
        Object expected = run(true, flags, inverse, big, 64, 1 << 22);
        long t1 = System.nanoTime();
        Object actual = run(false, flags, inverse, big, 64, 1 << 22);
        long t2 = System.nanoTime();
        check("chain120-timed", expected, actual);
        System.out.printf("TIMING chain120 reference_ms=%d patched_ms=%d%n",
                (t1 - t0) / 1_000_000, (t2 - t1) / 1_000_000);
    }

    private static void collisionCase() throws Exception {
        Method keyHash = LogicEngine.class.getDeclaredMethod(
                "keyHash", int.class, long.class, long.class);
        keyHash.setAccessible(true);
        Map<Integer, Long> seen = new HashMap<>();
        long first = -1;
        long second = -1;
        for (long node = 0; second < 0; node++) {
            Long previous = seen.putIfAbsent((Integer) keyHash.invoke(null, 0, 5L, node), node);
            if (previous != null) {
                first = previous;
                second = node;
            }
        }
        System.out.println("COLLISION nodes " + first + " " + second);
        int[] flags = {PredicateFlags.TRANSITIVE};
        int[] inverse = {-1};
        List<Statement> seeds = new ArrayList<>(List.of(
                new Statement(second, 0, 100L, 5L),
                new Statement(1L, 0, first, 5L),
                new Statement(first, 0, 200L, 5L),
                new Statement(2L, 0, second, 5L),
                new Statement(first, 0, 300L, 5L),
                new Statement(second, 0, 400L, 5L),
                new Statement(300L, 0, second, 5L)));
        for (int rounds = 1; rounds <= 4; rounds++) {
            compare("collision/r" + rounds, flags, inverse, seeds, rounds, 1000);
        }
    }

    private static void nullCases() {
        int[] flags = {PredicateFlags.TRANSITIVE};
        int[] inverse = {-1};
        compare("nullSeeds", flags, inverse, null, 3, 10);
        compare("empty", flags, inverse, List.of(), 3, 10);
        compare("badBounds", flags, inverse, List.of(), 0, 10);
    }

    private static void compare(String name, int[] flags, int[] inverse,
            Collection<Statement> seeds, int maxRounds, int maxStatements) {
        cases++;
        Object expected = run(true, flags, inverse, seeds, maxRounds, maxStatements);
        Object actual = run(false, flags, inverse, seeds, maxRounds, maxStatements);
        String text = String.valueOf(expected);
        String kind = text.startsWith("EX:") ? text.replaceAll("[0-9]+", "N")
                : text.contains("rounds=" + maxRounds + ";") ? "OK_AT_MAXROUNDS" : "OK_FIXED_POINT";
        KINDS.merge(kind, 1, Integer::sum);
        if (text.startsWith("EX:")) {
            thrown++;
            if (text.contains("maxRounds=")) {
                probes++;
            }
        } else if (text.contains("rounds=" + maxRounds + ";")) {
            probes++;
        }
        check(name, expected, actual);
    }

    private static void check(String name, Object expected, Object actual) {
        if (!expected.equals(actual)) {
            failures++;
            System.out.println("MISMATCH " + name + "\n  expected " + expected
                    + "\n  actual   " + actual);
        }
    }

    private static Object run(boolean reference, int[] flags, int[] inverse,
            Collection<Statement> seeds, int maxRounds, int maxStatements) {
        PredicateLexicon lexicon = lexicon(flags, inverse);
        try {
            if (reference) {
                LogicEngineReference engine =
                        new LogicEngineReference(lexicon, maxRounds, maxStatements);
                String fields = fields(engine);
                LogicEngineReference.Result result = engine.closure(seeds);
                return "OK:rounds=" + result.rounds() + ";derived=" + result.derivedStatements()
                        + ";" + result.statements() + fields + fields(engine);
            }
            LogicEngine engine = new LogicEngine(lexicon, maxRounds, maxStatements);
            String fields = fields(engine);
            LogicEngine.Result result = engine.closure(seeds);
            return "OK:rounds=" + result.rounds() + ";derived=" + result.derivedStatements()
                    + ";" + result.statements() + fields + fields(engine);
        } catch (RuntimeException e) {
            return "EX:" + e.getClass().getName() + ":" + e.getMessage();
        }
    }

    private static String fields(Object engine) {
        StringBuilder out = new StringBuilder("|");
        for (Field field : sortedFields(engine.getClass())) {
            try {
                field.setAccessible(true);
                Object value = field.get(engine);
                out.append(field.getName()).append('=')
                        .append(value instanceof PredicateLexicon ? "lexicon" : value)
                        .append(';');
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }
        return out.toString();
    }

    private static Field[] sortedFields(Class<?> type) {
        Field[] fields = type.getDeclaredFields();
        Arrays.sort(fields, (a, b) -> a.getName().compareTo(b.getName()));
        return fields;
    }
}
