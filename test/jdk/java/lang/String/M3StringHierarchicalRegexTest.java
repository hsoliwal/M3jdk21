/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary Hierarchical regex effects preserve canonical M3String owner/range semantics
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/jdk.internal.mindex=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringHierarchicalRegexTest
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/jdk.internal.mindex=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringHierarchicalRegexTest
 */
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Random;
import java.util.regex.Pattern;

public final class M3StringHierarchicalRegexTest {
    private record RouteCase(String value, String regex, boolean accessExpected,
            boolean readsRequired, boolean unknownExpected, boolean interrupted, String label) { }

    /**
     * Proxy wraps the existing Access API; it never fabricates an M3 owner or a regex result.
     * Access dispatch proves relation traversal entry, not that its result was returned.
     * In particular, the 64-atom helper refuses before Access: zero dispatch at that boundary
     * proves no owner work, while the source-sealed production selector supplies the call guard.
     */
    private static void verifyProductionRoutes(Field backing, Method decision,
            Field access, Method evaluate, String required) throws Exception {
        Class<?> m3 = decision.getParameterTypes()[0];
        Method owner = m3.getDeclaredMethod("owner");
        Method start = m3.getDeclaredMethod("start");
        owner.setAccessible(true);
        start.setAccessible(true);
        Class<?> tuple = Class.forName("java.lang.M3StringTuple");
        Class<?> accessType = evaluate.getParameterTypes()[1];
        Object original = access.get(null);
        if (original == null) throw new AssertionError("deterministic Access initialization");
        // A backed two-unit view makes repeat construct a shared DAG even if a bootstrap
        // literal had no M3 backing. Keep the old required source and its assertions unchanged.
        String seed = required.substring(0, 2);
        String terminal = required.substring(required.length() - 1);
        if (backing.get(seed) == null || backing.get(terminal) == null) {
            throw new AssertionError("known-backed route seed/range");
        }
        required = String.join("", seed.repeat(2048), terminal);
        Object requiredStorage = backing.get(required);
        if (requiredStorage == null || !tuple.isInstance(owner.invoke(requiredStorage))) {
            throw new AssertionError("required production tuple not exercised");
        }

        String mismatch = String.join("", required.substring(0, required.length() - 1), "c");
        String boundary255 = required.substring(required.length() - 255);
        String boundary256 = required.substring(required.length() - 256);
        String shifted = required.substring(1);
        String trimmed = required.substring(1, required.length() - 1);
        String right = required.substring(required.length() / 2);
        for (String range : new String[]{boundary255, boundary256, shifted, trimmed, right}) {
            Object storage = backing.get(range);
            if (storage == null || owner.invoke(storage) != owner.invoke(requiredStorage)
                    || (Integer) start.invoke(storage) <= 0) {
                throw new AssertionError("required shared-owner nonzero range not exercised");
            }
        }

        var routes = new ArrayList<RouteCase>();
        String high = String.valueOf('\ud800');
        String low = String.valueOf('\udc00');
        Object highStorage = backing.get(high);
        Object lowStorage = backing.get(low);
        if (highStorage == null || lowStorage == null
                || owner.invoke(highStorage) == owner.invoke(lowStorage)) {
            throw new AssertionError("required distinct surrogate unit owners");
        }
        for (int count : new int[]{32, 33, 63, 64}) {
            // A single star keeps the independent Pattern oracle free of overlapping-star blowup.
            String regex = "a".repeat(count - 2) + "a*b";
            boolean selected = count > 32 && count < Long.SIZE;
            routes.add(new RouteCase(required, regex, selected, selected, false, false,
                    "atoms-" + count + "-match"));
            routes.add(new RouteCase(mismatch, regex, selected, selected, false, false,
                    "atoms-" + count + "-mismatch"));
        }
        for (int count : new int[]{33, 63}) {
            String regex = "a".repeat(count - 2) + "a*b";
            routes.add(new RouteCase(boundary255, regex, false, false, false, false,
                    "length-255-atoms-" + count));
            // At 256, the relation work budget may refuse before a leaf read; rolling may decide.
            routes.add(new RouteCase(boundary256, regex, true, false, false, false,
                    "length-256-atoms-" + count));
            for (String range : new String[]{shifted, trimmed, right}) {
                routes.add(new RouteCase(range, regex, true, false, false, false,
                        "shared-range-atoms-" + count));
            }
            String dotRegex = "a".repeat(count - 2) + ".*b";
            for (String unit : new String[]{"\n", "\r", "\u0085", "\u2028", "\u2029", "\u0100",
                    "\ud800", "\udc00", "\ud800\udc00"}) {
                String value = String.join("", required.substring(0, required.length() - 1),
                        unit, required.substring(required.length() - 1));
                boolean surrogate = Character.isSurrogate(unit.charAt(0));
                routes.add(new RouteCase(value, dotRegex, true, false, surrogate, false,
                        "UTF16-atoms-" + count + "-unit-" + Integer.toHexString(unit.charAt(0))));
            }
            // The pair crosses distinct canonical unit owners and separately joined elements.
            String separatedPair = String.join("",
                    required.substring(0, required.length() - 1), high, low,
                    required.substring(required.length() - 1));
            routes.add(new RouteCase(separatedPair, dotRegex, true, false, true, false,
                    "cross-owner-surrogate-pair-atoms-" + count));
        }
        routes.add(new RouteCase(required, "a".repeat(31) + "a*b",
                true, true, false, true, "interrupted-33-atoms"));

        int[] calls = new int[2];
        Object[] trackedSource = new Object[1];
        Thread testThread = Thread.currentThread();
        Object proxy = Proxy.newProxyInstance(accessType.getClassLoader(),
                new Class<?>[]{accessType}, (ignored, method, arguments) -> {
                    if (Thread.currentThread() == testThread
                            && method.getDeclaringClass() == accessType) {
                        if (arguments != null && arguments.length > 0
                                && arguments[0] == trackedSource[0]) calls[0]++;
                        if (method.getName().equals("codeUnitAt")) calls[1]++;
                    }
                    try {
                        return method.invoke(original, arguments);
                    } catch (InvocationTargetException failure) {
                        throw failure.getCause();
                    }
                });
        int routeCases = 0;
        int accessCases = 0;
        int noAccessCases = 0;
        int readCases = 0;
        int unknownCases = 0;
        int directCases = 0;
        try {
            access.set(null, proxy);
            for (RouteCase route : routes) {
                Object storage = backing.get(route.value());
                if (storage == null || !tuple.isInstance(owner.invoke(storage))) {
                    throw new AssertionError("required route tuple: " + route.label());
                }
                boolean expected = Pattern.compile(route.regex())
                        .matcher(new StringBuilder(route.value())).matches();
                calls[0] = calls[1] = 0;
                trackedSource[0] = storage;
                if (route.interrupted()) Thread.currentThread().interrupt();
                try {
                    int actual = (Integer) decision.invoke(null, storage, route.regex());
                    int dispatches = calls[0];
                    int reads = calls[1];
                    if (route.accessExpected() ? dispatches == 0 : dispatches != 0) {
                        throw new AssertionError("relation Access entry: " + route.label());
                    }
                    if (route.readsRequired() && reads == 0) {
                        throw new AssertionError("required relation leaf read: " + route.label());
                    }
                    if (route.unknownExpected() ? actual != -1 : actual != (expected ? 1 : 0)) {
                        throw new AssertionError("production route decision: " + route.label());
                    }
                    if (route.value().matches(route.regex()) != expected) {
                        throw new AssertionError("public route parity: " + route.label());
                    }
                    if (route.interrupted() && !Thread.currentThread().isInterrupted()) {
                        throw new AssertionError("relation acquired cancellation semantics");
                    }
                } finally {
                    if (route.interrupted()) Thread.interrupted();
                }
                routeCases++;
                if (route.accessExpected()) accessCases++; else noAccessCases++;
                if (route.readsRequired()) readCases++;
                if (route.unknownExpected()) unknownCases++;
            }

            // These separate Result assertions prove actual helper decisions and DAG reuse.
            // Their budget is exactly the production caller's budget, not a larger test allowance.
            for (int count : new int[]{33, 63}) {
                int[] atoms = new int[count];
                for (int index = 0; index < count - 1; index++) atoms[index] = 'a';
                atoms[count - 2] |= 1 << 16;
                atoms[count - 1] = 'b';
                String regex = "a".repeat(count - 2) + "a*b";
                for (String value : new String[]{required, mismatch}) {
                    boolean expected = Pattern.compile(regex).matcher(new StringBuilder(value)).matches();
                    Object summary = evaluate.invoke(null, backing.get(value), original, atoms,
                            count, 128, Math.min(1_000_000L, (long) value.length() * (count + 1)),
                            null, null);
                    int actual = (Integer) summary.getClass().getMethod("decision").invoke(summary);
                    long reads = (Long) summary.getClass().getMethod("codeUnitReads").invoke(summary);
                    int reuse = (Integer) summary.getClass().getMethod("reusedSummaries").invoke(summary);
                    if (actual != (expected ? 1 : 0) || reuse == 0 || reads >= value.length()) {
                        throw new AssertionError("bounded direct relation result/reuse: atoms=" + count);
                    }
                    directCases++;
                }
            }
        } finally {
            access.set(null, original);
        }
        if (routeCases != 39 || accessCases != 33 || noAccessCases != 6 || readCases != 5
                || unknownCases != 8 || directCases != 4) {
            throw new AssertionError("route proof inventory drift");
        }
        System.out.println("M3_HIERARCHICAL_REGEX_ROUTES cases=" + routeCases
                + " accessCases=" + accessCases + " noAccessCases=" + noAccessCases
                + " requiredReadCases=" + readCases + " surrogateUnknownCases=" + unknownCases
                + " directHelperCases=" + directCases
                + " scope=ENTRY_PLUS_SEPARATE_HELPER_RESULTS PASS");
    }

    public static void main(String[] args) throws Exception {
        Field backing = String.class.getDeclaredField("m3");
        backing.setAccessible(true);
        Class<?> m3 = Class.forName("java.lang.M3String");
        Method decision = Class.forName("java.lang.M3StringRegexDp")
                .getDeclaredMethod("tryMatch", m3, String.class);
        decision.setAccessible(true);
        Random random = new Random(0x4d334450L);
        String[] patterns = {"a*b*", "a.*b", "a*b*a*", ".*", "a*.*b*", "a*b*a*b*"};
        long checks = 0;
        for (int trial = 0; trial < 2048; trial++) {
            String block = String.join("", "aa", trial % 3 == 0 ? "b" : "a");
            String value = block.repeat(1 + random.nextInt(1000));
            int from = random.nextInt(value.length());
            String view = value.substring(from, from + random.nextInt(value.length() - from + 1));
            for (String regex : patterns) {
                boolean expected = Pattern.compile(regex).matcher(new StringBuilder(view)).matches();
                if (view.matches(regex) != expected) throw new AssertionError("String regex parity");
                Object source = backing.get(view);
                if (source != null) {
                    int actual = (Integer) decision.invoke(null, source, regex);
                    if (actual >= 0 && (actual == 1) != expected) throw new AssertionError("DP decision parity");
                }
                checks++;
            }
        }
        for (String unit : new String[]{"\n", "\r", "\u0085", "\u2028", "\u2029", "\u0100",
                "\ud800", "\udc00", "\ud800\udc00"}) {
            String value = String.join("", "a", unit, "b");
            boolean expected = Pattern.compile("a.*b").matcher(new StringBuilder(value)).matches();
            if (value.matches("a.*b") != expected) throw new AssertionError("seam/code-point fallback");
            checks++;
        }
        String required = String.join("", "aa".repeat(2048), "b");
        if (backing.get(required) == null) throw new AssertionError("required M3 path not exercised");
        // Warm the lazy adapter through the actual eligible production path.
        String warmRegex = "a*".repeat(32) + "b";
        if ((Integer) decision.invoke(null, backing.get(required), warmRegex) != 1
                || !Pattern.compile(warmRegex).matcher(new StringBuilder(required)).matches()) {
            throw new AssertionError("required eligible 33-atom DAG warmup");
        }
        // Exercise the exact receiver Access adapter on a real shared M3String DAG, not a fixture.
        Class<?> dp = Class.forName("java.lang.M3StringRegexDp");
        Field access = dp.getDeclaredField("RELATION_ACCESS");
        access.setAccessible(true);
        Class<?> relations = Class.forName("jdk.internal.mindex.M3DotStarRelations");
        Method evaluate = relations.getMethod("evaluate", Object.class,
                Class.forName("jdk.internal.mindex.M3DotStarRelations$Access"), int[].class,
                int.class, int.class, long.class,
                Class.forName("jdk.internal.mindex.M3DotStarRelations$Join"), Runnable.class);
        Object summary = evaluate.invoke(null, backing.get(required), access.get(null),
                new int[]{'a' | (1 << 16), 'b'}, 2, 128, (long) required.length() * 3, null, null);
        int actual = (Integer) summary.getClass().getMethod("decision").invoke(summary);
        long reads = (Long) summary.getClass().getMethod("codeUnitReads").invoke(summary);
        int reuse = (Integer) summary.getClass().getMethod("reusedSummaries").invoke(summary);
        if (actual != 1 || reuse == 0 || reads >= required.length()) {
            throw new AssertionError("required hierarchical reuse did not execute");
        }
        Thread.currentThread().interrupt();
        try {
            if (!required.matches("a*b") || !Thread.currentThread().isInterrupted()) {
                throw new AssertionError("String matching acquired cancellation semantics");
            }
        } finally { Thread.interrupted(); }
        verifyProductionRoutes(backing, decision, access, evaluate, required);
        System.out.println("M3_HIERARCHICAL_REGEX_TARGET checks=" + checks + " PASS");
    }
}
