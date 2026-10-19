import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/**
 * Source-blind finite public-API proof. Run "oracle" first, then target class name.
 * No candidate source, private members, balance formula or throughput assertions.
 */
public final class M3TreeMapSortedBulkProof {
    static final int[] SIZES = {0,1,2,3,4,7,8,15,16,31,32,255,256,4095};
    static final String TARGET = "com.m3.collections.M3TreeMap";
    static final MessageDigest DIGEST;
    static int scenarios, checks, invariantCalls, providerLoads;
    static Factory factory;
    static {
        try { DIGEST = MessageDigest.getInstance("SHA-256"); }
        catch (Exception e) { throw new ExceptionInInitializerError(e); }
    }

    static final class Box {
        final String id;
        int revision;
        Box(String id) { this.id = id; }
        public String toString() { return id + "@" + revision; }
    }

    // Throws only during the selected, pre-frozen gated-copy scenario.
    // Positive comparison count/order is never compared or asserted.
    static final class GateComparator implements Comparator<Integer> {
        final int kind;
        boolean blocked;
        GateComparator(int kind) { this.kind = kind; }
        public int compare(Integer a, Integer b) {
            if (blocked) throw new ComparatorBlocked();
            if (kind == 2) {
                if (a == b) return 0;
                if (a == null) return -1;
                if (b == null) return 1;
            }
            return kind == 1 ? Integer.compare(b, a) : Integer.compare(a, b);
        }
    }
    static final class ComparatorBlocked extends RuntimeException {}

    static final class Factory {
        final Class<?> type;
        final Constructor<?> normal, comparator, lazy;
        final Method invariant;
        Factory(String name) throws Exception {
            type = Class.forName(name);
            normal = type.getConstructor();
            comparator = type.getConstructor(Comparator.class);
            Constructor<?> found = null;
            for (Constructor<?> c : type.getConstructors()) {
                Class<?>[] p = c.getParameterTypes();
                if (p.length == 2 && p[0] == Comparator.class
                        && p[1].getName().equals("com.m3.collections.M3LazyValueProvider")) {
                    found = c;
                }
            }
            lazy = found;
            Method inv = null;
            try { inv = type.getMethod("verifyInvariants"); }
            catch (NoSuchMethodException e) {
                if (name.equals(TARGET)) throw e;
            }
            invariant = inv;
        }

        @SuppressWarnings("unchecked")
        NavigableMap<Integer,Box> make(Comparator<Integer> c, boolean useDefault,
                boolean useLazy) throws Exception {
            Object made;
            try {
                if (useLazy && lazy != null) {
                    Class<?> providerType = lazy.getParameterTypes()[1];
                    Object provider = Proxy.newProxyInstance(providerType.getClassLoader(),
                            new Class<?>[]{providerType}, (proxy, method, args) -> {
                        if (method.getDeclaringClass() == Object.class) {
                            return switch (method.getName()) {
                                case "toString" -> "SourceBlindProofProvider";
                                case "hashCode" -> System.identityHashCode(proxy);
                                case "equals" -> proxy == args[0];
                                default -> throw new AssertionError(method.toString());
                            };
                        }
                        if (method.getName().equals("load")) {
                            providerLoads++;
                            return new Box("provider:" + args[0]);
                        }
                        if (method.isDefault()) {
                            return InvocationHandler.invokeDefault(proxy, method,
                                    args == null ? new Object[0] : args);
                        }
                        throw new AssertionError("Unqualified provider API: " + method);
                    });
                    made = lazy.newInstance(c, provider);
                } else {
                    made = useDefault ? normal.newInstance() : comparator.newInstance(c);
                }
            } catch (InvocationTargetException e) { throw rethrow(e.getCause()); }
            if (!(made instanceof NavigableMap<?,?>)) {
                throw new AssertionError("Target is not NavigableMap");
            }
            return (NavigableMap<Integer,Box>) made;
        }

        void verify(NavigableMap<Integer,Box> map) throws Exception {
            if (invariant != null && type.isInstance(map)) {
                try { invariant.invoke(map); invariantCalls++; }
                catch (InvocationTargetException e) { throw rethrow(e.getCause()); }
            }
        }
    }

    static Exception rethrow(Throwable t) {
        if (t instanceof Error e) throw e;
        return t instanceof Exception e ? e : new RuntimeException(t);
    }
    static void fail(String id, String text) {
        throw new AssertionError(id + ": " + text);
    }
    static void equal(String id, Object expected, Object actual) {
        checks++;
        if (!Objects.equals(expected, actual))
            fail(id, "expected=" + brief(expected) + " actual=" + brief(actual));
    }
    static String brief(Object value) {
        String s = String.valueOf(value);
        return s.length() <= 512 ? s : s.substring(0, 512) + "...[truncated]";
    }
    static void record(String text) {
        DIGEST.update(text.getBytes(StandardCharsets.UTF_8));
        DIGEST.update((byte) '\n');
    }
    static String entryText(Map.Entry<Integer,Box> e) {
        return e == null ? "<none>" : String.valueOf(e.getKey()) + "=" + e.getValue();
    }
    static List<Map.Entry<Integer,Box>> entries(Map<Integer,Box> map) {
        List<Map.Entry<Integer,Box>> result = new ArrayList<>(map.size());
        for (Map.Entry<Integer,Box> e : map.entrySet())
            result.add(new AbstractMap.SimpleImmutableEntry<>(e));
        return result;
    }
    static void recordState(String id, List<Map.Entry<Integer,Box>> state) {
        record(id + ":size=" + state.size());
        for (Map.Entry<Integer,Box> e : state) record(entryText(e));
    }
    static <T> T firstOrNull(Iterable<T> iterable) {
        Iterator<T> i = iterable.iterator();
        return i.hasNext() ? i.next() : null;
    }

    static void check(String id, NavigableMap<Integer,Box> expected,
            NavigableMap<Integer,Box> actual) throws Exception {
        // Detach original observations before reading matching candidate projections.
        List<Map.Entry<Integer,Box>> frozen = entries(expected);
        List<Integer> keys = new ArrayList<>(expected.navigableKeySet());
        List<Box> values = new ArrayList<>(expected.values());
        List<Map.Entry<Integer,Box>> descending = entries(expected.descendingMap());
        recordState(id, frozen);
        equal(id + "/size", frozen.size(), actual.size());
        equal(id + "/empty", expected.isEmpty(), actual.isEmpty());
        equal(id + "/entries", frozen, entries(actual));
        equal(id + "/keys", keys, new ArrayList<>(actual.navigableKeySet()));
        equal(id + "/values", values, new ArrayList<>(actual.values()));
        equal(id + "/descending", descending, entries(actual.descendingMap()));
        equal(id + "/descendingKeys", new ArrayList<>(expected.descendingKeySet()),
                new ArrayList<>(actual.descendingKeySet()));
        equal(id + "/firstEntry", expected.firstEntry(), actual.firstEntry());
        equal(id + "/lastEntry", expected.lastEntry(), actual.lastEntry());
        if (!expected.isEmpty()) {
            equal(id + "/firstKey", expected.firstKey(), actual.firstKey());
            equal(id + "/lastKey", expected.lastKey(), actual.lastKey());
        } else {
            equal(id + "/firstKey-empty", NoSuchElementException.class,
                    exceptionType(actual::firstKey));
            equal(id + "/lastKey-empty", NoSuchElementException.class,
                    exceptionType(actual::lastKey));
        }
        int[] queries = {-100001,-33,-1,0,1,17,100001};
        for (int q : queries) {
            equal(id + "/lower/" + q, expected.lowerEntry(q), actual.lowerEntry(q));
            equal(id + "/floor/" + q, expected.floorEntry(q), actual.floorEntry(q));
            equal(id + "/ceiling/" + q, expected.ceilingEntry(q), actual.ceilingEntry(q));
            equal(id + "/higher/" + q, expected.higherEntry(q), actual.higherEntry(q));
            equal(id + "/lowerKey/" + q, expected.lowerKey(q), actual.lowerKey(q));
            equal(id + "/floorKey/" + q, expected.floorKey(q), actual.floorKey(q));
            equal(id + "/ceilingKey/" + q, expected.ceilingKey(q), actual.ceilingKey(q));
            equal(id + "/higherKey/" + q, expected.higherKey(q), actual.higherKey(q));
            equal(id + "/get/" + q, expected.get(q), actual.get(q));
            equal(id + "/contains/" + q, expected.containsKey(q), actual.containsKey(q));
        }
        factory.verify(actual);
    }

    interface Action { Object run() throws Exception; }
    static Class<?> exceptionType(Action action) throws Exception {
        try { action.run(); return null; }
        catch (Exception e) { return e.getClass(); }
    }

    static TreeMap<Integer,Box> source(int n, Comparator<Integer> c, String id) {
        TreeMap<Integer,Box> s = new TreeMap<>(c);
        Box repeated = new Box(id + "/shared");
        for (int i = 0; i < n; i++) {
            Integer key = c instanceof GateComparator g && g.kind == 2 && i == 0
                    ? null : Integer.valueOf(2 * i - n);
            Box value = i % 7 == 0 ? null
                    : i % 5 == 0 ? repeated : new Box(id + "/v" + i);
            s.put(key, value);
        }
        return s;
    }
    static TreeMap<Integer,Box> expectedCopy(Comparator<Integer> c, Map<Integer,Box> source) {
        TreeMap<Integer,Box> result = new TreeMap<>(c);
        result.putAll(source);
        return result;
    }

    static void putBoth(String id, NavigableMap<Integer,Box> expected,
            NavigableMap<Integer,Box> actual, Integer k, Box v) {
        Box old = expected.put(k, v); // Freeze original return before candidate call.
        record(id + "/put/" + k + "/old=" + old);
        equal(id + "/put-return", old, actual.put(k, v));
    }
    static void removeBoth(String id, NavigableMap<Integer,Box> expected,
            NavigableMap<Integer,Box> actual, Integer k) {
        Box old = expected.remove(k);
        record(id + "/remove/" + k + "/old=" + old);
        equal(id + "/remove-return", old, actual.remove(k));
    }

    static void mutations(String id, NavigableMap<Integer,Box> expected,
            NavigableMap<Integer,Box> actual) throws Exception {
        for (int i = 0; i < 48; i++) {
            int k = (i * 37 % 173) - 87;
            if (i % 3 == 0) removeBoth(id, expected, actual, k);
            else putBoth(id, expected, actual, k, new Box(id + "/m" + i));
            if (i % 8 == 7) check(id + "/mixed-" + i, expected, actual);
        }
        List<Integer> keys = new ArrayList<>(expected.keySet());
        for (int i = 0, removed = 0; i < keys.size() && removed < 256; i += 3, removed++) {
            removeBoth(id, expected, actual, keys.get(i));
            if (removed % 32 == 31) check(id + "/delete-" + removed, expected, actual);
        }
        for (int i = 0; i < 48; i++)
            putBoth(id, expected, actual, 1000 + i * 11, new Box(id + "/gap" + i));
        check(id + "/after-gaps", expected, actual);
        int drained = 0;
        while (!expected.isEmpty()) {
            Map.Entry<Integer,Box> got = (drained % 2 == 0)
                    ? expected.pollFirstEntry() : expected.pollLastEntry();
            Map.Entry<Integer,Box> frozen = new AbstractMap.SimpleImmutableEntry<>(got);
            record(id + "/poll/" + drained + "/" + entryText(frozen));
            Map.Entry<Integer,Box> observed = (drained % 2 == 0)
                    ? actual.pollFirstEntry() : actual.pollLastEntry();
            equal(id + "/poll-return", frozen, observed);
            equal(id + "/poll-size", expected.size(), actual.size());
            if (++drained % 128 == 0) check(id + "/drain-" + drained, expected, actual);
        }
        equal(id + "/poll-empty", expected.pollFirstEntry(), actual.pollFirstEntry());
        equal(id + "/poll-last-empty", expected.pollLastEntry(), actual.pollLastEntry());
        check(id + "/drained", expected, actual);
        for (int i = 0; i < 48; i++)
            putBoth(id, expected, actual, (i * 19 % 53) - 26, new Box(id + "/refill" + i));
        check(id + "/refill", expected, actual);
        expected.clear();
        actual.clear();
        check(id + "/clear", expected, actual);
        for (int i = 31; i >= 0; i--)
            putBoth(id, expected, actual, i * 3 - 45, new Box(id + "/reuse" + i));
        check(id + "/reuse", expected, actual);
    }

    static void independence(String id, TreeMap<Integer,Box> src,
            TreeMap<Integer,Box> expected, NavigableMap<Integer,Box> actual) throws Exception {
        Box shared = firstOrNull(src.values().stream().filter(Objects::nonNull).toList());
        if (shared != null) shared.revision++;
        check(id + "/shared-value", expected, actual);
        src.put(200000, new Box(id + "/source-only"));
        if (!src.isEmpty()) src.pollFirstEntry();
        src.clear();
        src.put(-200000, new Box(id + "/source-after-clear"));
        check(id + "/source-independent", expected, actual);
    }

    static void bulk(String mode, int n, GateComparator cmp, boolean useDefault,
            boolean gated, boolean lazy) throws Exception {
        String id = (lazy ? "lazy" : gated ? "gated" : "bulk") + "/" + mode + "/" + n;
        TreeMap<Integer,Box> src = source(n, cmp, id);
        TreeMap<Integer,Box> expected = new TreeMap<>(cmp);
        NavigableMap<Integer,Box> actual = factory.make(cmp, useDefault, lazy);
        equal(id + "/comparator", cmp, actual.comparator());
        if (gated) cmp.blocked = true;
        try {
            // The original expected operation completes before the candidate operation.
            expected.putAll(src);
            record(id + "/original-putAll=success");
            actual.putAll(src);
        } finally {
            if (gated) cmp.blocked = false;
        }
        check(id + "/copied", expected, actual);
        independence(id, src, expected, actual);
        mutations(id, expected, actual);
        scenarios++;
    }

    static void sourceView(boolean reverse) throws Exception {
        Comparator<Integer> c = reverse ? new GateComparator(1) : null;
        String id = "range-source/" + reverse;
        TreeMap<Integer,Box> parent = source(32, c, id);
        List<Integer> order = new ArrayList<>(parent.keySet());
        NavigableMap<Integer,Box> src = parent.subMap(order.get(4), true, order.get(27), false);
        TreeMap<Integer,Box> expected = expectedCopy(c, src);
        NavigableMap<Integer,Box> actual = factory.make(c, false, false);
        actual.putAll(src);
        check(id + "/copy", expected, actual);
        parent.clear();
        check(id + "/independent", expected, actual);
        mutations(id, expected, actual);
        scenarios++;
    }

    static void descendingSource() throws Exception {
        String id = "descending-source";
        TreeMap<Integer,Box> parent = source(32, null, id);
        NavigableMap<Integer,Box> src = parent.descendingMap();
        @SuppressWarnings("unchecked")
        Comparator<Integer> cmp = (Comparator<Integer>) src.comparator();
        TreeMap<Integer,Box> expected = expectedCopy(cmp, src);
        NavigableMap<Integer,Box> actual = factory.make(cmp, false, false);
        actual.putAll(src);
        check(id, expected, actual);
        parent.clear();
        check(id + "/independent", expected, actual);
        mutations(id, expected, actual);
        scenarios++;
    }

    static void fallback(int kind) throws Exception {
        String id = "fallback/" + kind;
        Comparator<Integer> dest = kind == 1 ? new GateComparator(1) : null;
        TreeMap<Integer,Box> expected = new TreeMap<>(dest);
        NavigableMap<Integer,Box> actual = factory.make(dest, false, false);
        if (kind == 0 || kind == 3) {
            putBoth(id, expected, actual, -999, new Box(id + "/old"));
            putBoth(id, expected, actual, 0, new Box(id + "/overlap"));
        }
        if (kind == 3) {
            expected.clear();
            actual.clear();
        }
        Map<Integer,Box> src = source(kind == 3 ? 256 : 32, null, id);
        if (kind == 2) {
            LinkedHashMap<Integer,Box> unsorted = new LinkedHashMap<>();
            List<Map.Entry<Integer,Box>> items = entries(src);
            for (int i = items.size() - 1; i >= 0; i--) {
                Map.Entry<Integer,Box> e = items.get(i);
                unsorted.put(e.getKey(), e.getValue());
            }
            src = unsorted;
        }
        expected.putAll(src);
        record(id + "/original-putAll=success");
        actual.putAll(src);
        check(id, expected, actual);
        mutations(id, expected, actual);
        scenarios++;
    }

    static void destinationViews() throws Exception {
        String id = "destination-views";
        TreeMap<Integer,Box> expected = new TreeMap<>();
        NavigableMap<Integer,Box> actual = factory.make(null, true, false);
        for (int k : new int[]{-20,-3,20})
            putBoth(id, expected, actual, k, new Box(id + "/seed" + k));
        TreeMap<Integer,Box> src = new TreeMap<>();
        for (int k : new int[]{-2,2,4}) src.put(k, new Box(id + "/in" + k));
        NavigableMap<Integer,Box> ev = expected.descendingMap()
                .subMap(8,true,-8,true).headMap(-5,true);
        NavigableMap<Integer,Box> av = actual.descendingMap()
                .subMap(8,true,-8,true).headMap(-5,true);
        ev.putAll(src);
        av.putAll(src);
        check(id + "/base", expected, actual);
        check(id + "/nested", ev, av);
        Integer first = ev.firstKey();
        equal(id + "/view-key-removal", ev.navigableKeySet().remove(first),
                av.navigableKeySet().remove(first));
        check(id + "/backing", expected, actual);
        ev.clear();
        av.clear();
        check(id + "/view-clear", expected, actual);
        scenarios++;
    }

    static void rangeReject() throws Exception {
        String id = "range-reject";
        TreeMap<Integer,Box> expected = new TreeMap<>();
        NavigableMap<Integer,Box> actual = factory.make(null, true, false);
        TreeMap<Integer,Box> src = new TreeMap<>();
        src.put(-2, new Box(id + "/accepted"));
        src.put(12, new Box(id + "/rejected"));
        NavigableMap<Integer,Box> ev = expected.subMap(-10,true,10,false);
        NavigableMap<Integer,Box> av = actual.subMap(-10,true,10,false);
        Class<?> original = exceptionType(() -> { ev.putAll(src); return null; });
        equal(id + "/original-type", IllegalArgumentException.class, original);
        List<Map.Entry<Integer,Box>> frozen = entries(expected);
        recordState(id + "/original-partial", frozen);
        equal(id + "/exception", original,
                exceptionType(() -> { av.putAll(src); return null; }));
        equal(id + "/partial-entries", frozen, entries(actual));
        check(id + "/post-state", expected, actual);
        scenarios++;
    }

    static void emptyDescendingTarget() throws Exception {
        String id = "empty-descending-target";
        TreeMap<Integer,Box> expected = new TreeMap<>();
        NavigableMap<Integer,Box> actual = factory.make(null, true, false);
        NavigableMap<Integer,Box> ev = expected.descendingMap();
        NavigableMap<Integer,Box> av = actual.descendingMap();
        @SuppressWarnings("unchecked")
        Comparator<Integer> cmp = (Comparator<Integer>) ev.comparator();
        TreeMap<Integer,Box> src = source(32, cmp, id);
        ev.putAll(src);
        av.putAll(src);
        check(id + "/base", expected, actual);
        check(id + "/view", ev, av);
        Integer key = ev.firstKey();
        removeBoth(id, ev, av, key);
        check(id + "/backing", expected, actual);
        scenarios++;
    }

    static final class ReportingMap extends LinkedHashMap<Integer,Box> {
        int reports;
        @Override public void forEach(java.util.function.BiConsumer<? super Integer,? super Box> f) {
            reports++;
            super.forEach(f);
        }
    }

    static void reportingSource() throws Exception {
        String id = "reporting-map-source";
        ReportingMap src = new ReportingMap();
        for (int k : new int[]{17,-3,0,42,-11}) src.put(k, new Box(id + "/v" + k));
        TreeMap<Integer,Box> expected = new TreeMap<>();
        NavigableMap<Integer,Box> actual = factory.make(null, true, false);
        expected.putAll(src);
        int originalReports = src.reports;
        src.reports = 0;
        actual.putAll(src);
        int targetReports = src.reports;
        check(id, expected, actual);
        System.out.println("DIAGNOSTIC ordinarySourceForEach original=" + originalReports
                + " target=" + targetReports + " (dispatch count not asserted)");
        scenarios++;
    }

    @SuppressWarnings("unchecked")
    static void ancillary(boolean linked, boolean oracle) throws Exception {
        String name = oracle ? (linked ? "java.util.LinkedHashMap" : "java.util.HashMap")
                : (linked ? "com.m3.collections.M3LinkedHashMap" : "com.m3.collections.M3HashMap");
        Class<?> type;
        try { type = Class.forName(name); }
        catch (ClassNotFoundException e) {
            System.out.println("UNAVAILABLE ancillary=" + name + " (no owner pass claimed)");
            return;
        }
        Constructor<?> ctor;
        try { ctor = type.getConstructor(); }
        catch (NoSuchMethodException e) {
            System.out.println("UNAVAILABLE public-noarg=" + name + " (no owner pass claimed)");
            return;
        }
        Map<Integer,Box> actual;
        try { actual = (Map<Integer,Box>) ctor.newInstance(); }
        catch (InvocationTargetException e) { throw rethrow(e.getCause()); }
        Map<Integer,Box> expected = linked ? new LinkedHashMap<>() : new HashMap<>();
        String id = "ancillary/" + (linked ? "linked" : "hash");
        TreeMap<Integer,Box> sorted = source(32, null, id);
        expected.putAll(sorted);
        actual.putAll(sorted);
        equal(id + "/sorted-copy", expected, actual);
        if (linked) equal(id + "/ordered-copy", entries(expected), entries(actual));
        sorted.clear();
        equal(id + "/copy-independent", expected, actual);
        ReportingMap ordinary = new ReportingMap();
        for (int k : new int[]{7,-3,0,99,-31}) ordinary.put(k, new Box(id + "/new" + k));
        expected.putAll(ordinary);
        int originalReports = ordinary.reports;
        ordinary.reports = 0;
        actual.putAll(ordinary);
        equal(id + "/ordinary-copy", expected, actual);
        if (linked) equal(id + "/ordinary-order", entries(expected), entries(actual));
        for (int k : new int[]{7,0,-31}) {
            Box old = expected.remove(k);
            equal(id + "/remove", old, actual.remove(k));
        }
        for (int k : new int[]{-31,0,7}) {
            Box v = new Box(id + "/refill" + k);
            Box old = expected.put(k, v);
            equal(id + "/refill", old, actual.put(k, v));
        }
        equal(id + "/post-state", expected, actual);
        if (linked) equal(id + "/refill-order", entries(expected), entries(actual));
        Method inv = null;
        try { inv = type.getMethod("verifyInvariants"); }
        catch (NoSuchMethodException ignored) {}
        if (inv != null) {
            try { inv.invoke(actual); }
            catch (InvocationTargetException e) { throw rethrow(e.getCause()); }
        }
        expected.clear(); actual.clear();
        equal(id + "/clear", expected, actual);
        System.out.println("PASS ancillary=" + name + " ordinarySourceForEachOriginal="
                + originalReports + " ordinarySourceForEachTarget=" + ordinary.reports
                + " (dispatch count diagnostic)");
    }

    public static void main(String[] args) throws Exception {
        String name = args.length == 0 || args[0].equals("oracle")
                ? "java.util.TreeMap" : args[0];
        factory = new Factory(name);
        for (int n : SIZES) {
            bulk("natural-default", n, null, true, false, false);
            bulk("explicit-natural", n, new GateComparator(0), false, false, false);
            bulk("reverse", n, new GateComparator(1), false, false, false);
            bulk("null-friendly", n, new GateComparator(2), false, false, false);
            bulk("explicit-natural", n, new GateComparator(0), false, true, false);
            bulk("reverse", n, new GateComparator(1), false, true, false);
            bulk("null-friendly", n, new GateComparator(2), false, true, false);
        }
        sourceView(false);
        sourceView(true);
        descendingSource();
        for (int kind = 0; kind < 4; kind++) fallback(kind);
        destinationViews();
        emptyDescendingTarget();
        reportingSource();
        rangeReject();
        for (int n : new int[]{0,1,32,4095})
            bulk("explicit-values", n, new GateComparator(0), false, false, true);
        boolean oracle = name.equals("java.util.TreeMap");
        ancillary(false, oracle);
        ancillary(true, oracle);
        String hash = HexFormat.of().formatHex(DIGEST.digest());
        System.out.println("PASS scenarios=" + scenarios + " checks=" + checks
                + " expectedSHA256=" + hash + " target=" + name
                + " invariantCalls=" + invariantCalls
                + " lazyProviderAvailable=" + (factory.lazy != null)
                + " providerLoadsDiagnostic=" + providerLoads);
    }
}
