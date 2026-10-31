/*
 * @test
 * @summary Backed filtered/sorted views, stack-safe predicate composition and Java contracts
 * @modules java.base/java.util:open jdk.management
 * @run main/othervm -Xss256k CollectionViewsContract
 */
/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
import java.util.CollectionViews;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.Predicate;

/** Isolated receiver proof. No replacement collection, native predicate engine, or JDK ABI. */
public final class CollectionViewsContract {
    private static long checks;
    private static int methods;
    private CollectionViewsContract() { }
    private static void check(boolean value) {
        checks++;
        if (!value) throw new AssertionError("check=" + checks);
    }
    private static void equal(Object actual, Object expected) { check(Objects.equals(actual, expected)); }
    private interface Action { void run() throws Exception; }
    private static void fails(Class<? extends Throwable> type, Action action) throws Exception {
        try { action.run(); }
        catch (Throwable error) {
            if (type.isInstance(error)) { checks++; return; }
            throw new AssertionError("expected=" + type.getName(), error);
        }
        throw new AssertionError("missing=" + type.getName());
    }
    private static Field field(Class<?> type, String name) throws NoSuchFieldException {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try { Field f = c.getDeclaredField(name); f.setAccessible(true); return f; }
            catch (NoSuchFieldException absent) { /* Continue through the existing private hierarchy. */ }
        }
        throw new NoSuchFieldException(name);
    }
    private static Object predicate(Collection<?> view) throws Exception {
        return field(view.getClass(), "predicate").get(view);
    }
    private static void lazyAndOrder() {
        methods++;
        List<Integer> root = new ArrayList<>(List.of(1, 2, 3, 4));
        List<String> trace = new ArrayList<>();
        Collection<Integer> one = CollectionViews.filtered(root, x -> { trace.add("a" + x); return x % 2 == 0; });
        Collection<Integer> two = CollectionViews.filtered(one, x -> { trace.add("b" + x); return x > 2; });
        Spliterator<Integer> cursor = two.spliterator();
        check(!cursor.hasCharacteristics(Spliterator.SIZED));
        check(!cursor.hasCharacteristics(Spliterator.SUBSIZED));
        cursor.estimateSize(); cursor.characteristics(); two.iterator();
        check(trace.isEmpty());
        equal(two.stream().findFirst().orElseThrow(), 4);
        equal(trace, List.of("a1", "a2", "b2", "a3", "a4", "b4"));
        trace.clear(); equal(new ArrayList<>(two), List.of(4));
        check(!trace.isEmpty()); // Collection copy may request size; the wrapper contract is unchanged.
        root.add(6); equal(two.stream().toList(), List.of(4, 6));
    }
    private static void failuresAndAdmission() throws Exception {
        methods++;
        ArrayList<Integer> root = new ArrayList<>(List.of(0, 1, 2, 3));
        AtomicInteger later = new AtomicInteger();
        Collection<Integer> view = CollectionViews.filtered(
                CollectionViews.filtered(root, x -> x != 1), x -> {
                    later.incrementAndGet(); if (x == 2) throw new ArithmeticException("predicate"); return true;
                });
        check(!view.contains(1)); equal(later.get(), 0);
        fails(ArithmeticException.class, () -> view.contains(2)); equal(later.get(), 1);
        equal(root, List.of(0, 1, 2, 3));
        Collection<Integer> evens = CollectionViews.filtered(
                CollectionViews.filtered(root, x -> x >= 0), x -> x % 2 == 0);
        fails(IllegalArgumentException.class, () -> evens.add(7));
        fails(IllegalArgumentException.class, () -> evens.addAll(List.of(4, 5, 6)));
        equal(root, List.of(0, 1, 2, 3));
        check(evens.add(4)); check(evens.remove(2));
        check(!evens.remove(1)); equal(root, List.of(0, 1, 3, 4));
        evens.clear(); equal(root, List.of(1, 3));
        fails(NullPointerException.class, () -> CollectionViews.filtered((Collection<Integer>) null, x -> true));
        fails(NullPointerException.class, () -> CollectionViews.filtered(root, null));
        fails(NullPointerException.class, () -> evens.removeIf(null));
        fails(NullPointerException.class, () -> evens.spliterator().tryAdvance(null));
    }
    private static void bulkAndIterator() throws Exception {
        methods++;
        ArrayList<Integer> root = new ArrayList<>(Arrays.asList(null, 0, 1, 2, 2, 3, 4, 5));
        Collection<Integer> v = CollectionViews.filtered(CollectionViews.filtered(root, Objects::nonNull), x -> x % 2 == 0);
        Iterator<Integer> i = v.iterator();
        check(i.hasNext()); check(i.hasNext()); equal(i.next(), 0);
        fails(UnsupportedOperationException.class, i::remove);
        check(v.removeAll(List.of(2))); equal(root, Arrays.asList(null, 0, 1, 3, 4, 5));
        check(v.retainAll(List.of(4))); equal(root, Arrays.asList(null, 1, 3, 4, 5));
        check(v.removeIf(x -> true)); equal(root, Arrays.asList(null, 1, 3, 5));
        Iterator<Integer> empty = v.iterator(); check(!empty.hasNext());
        fails(NoSuchElementException.class, empty::next);
        Collection<Integer> nullable = CollectionViews.filtered(root, x -> x == null);
        check(nullable.iterator().hasNext()); equal(nullable.iterator().next(), null);
        check(nullable.remove(null)); equal(root, List.of(1, 3, 5));
    }
    private static <E> NavigableSet<E> expected(NavigableSet<E> source, Predicate<? super E> p) {
        TreeSet<E> out = new TreeSet<>(source.comparator());
        for (E x : source) if (p.test(x)) out.add(x);
        return out;
    }
    private static void sortedRanges() throws Exception {
        methods++;
        Comparator<Integer> nullable = Comparator.nullsFirst(Comparator.naturalOrder());
        for (Comparator<Integer> order : List.of(Comparator.<Integer>naturalOrder(), Comparator.<Integer>reverseOrder(), nullable)) {
            NavigableSet<Integer> root = new TreeSet<>(order);
            for (int i = -20; i <= 20; i++) root.add(i);
            if (order == nullable) root.add(null);
            Predicate<Integer> test = x -> x == null || x % 3 == 0;
            NavigableSet<Integer> filtered = CollectionViews.filtered(CollectionViews.filtered(root, x -> true), test);
            check(filtered.comparator() == order);
            Collection<Integer> erased = CollectionViews.filtered((Collection<Integer>) filtered, x -> true);
            check(erased instanceof NavigableSet<?>);
            for (NavigableSet<Integer> view : List.of(filtered, filtered.descendingSet())) {
                NavigableSet<Integer> oracle = expected(view == filtered ? root : root.descendingSet(), test);
                equal(view.stream().toList(), oracle.stream().toList());
                equal(view.first(), oracle.first()); equal(view.last(), oracle.last());
                for (int probe = -25; probe <= 25; probe++) {
                    equal(view.lower(probe), oracle.lower(probe)); equal(view.floor(probe), oracle.floor(probe));
                    equal(view.ceiling(probe), oracle.ceiling(probe)); equal(view.higher(probe), oracle.higher(probe));
                }
                int low = view.comparator().compare(-10, 10) < 0 ? -10 : 10;
                int high = -low;
                NavigableSet<Integer> range = view.subSet(low, false, high, true);
                NavigableSet<Integer> expectedRange = oracle.subSet(low, false, high, true);
                equal(range.stream().toList(), expectedRange.stream().toList());
                for (int probe : new int[]{-100, -10, 0, 10, 100}) {
                    equal(range.ceiling(probe), expectedRange.ceiling(probe));
                    equal(range.floor(probe), expectedRange.floor(probe));
                }
            }
        }
        TreeSet<Integer> root = new TreeSet<>(List.of(0, 1, 2, 3, 4));
        NavigableSet<Integer> view = CollectionViews.filtered(CollectionViews.filtered(root, x -> true), x -> x % 2 == 0);
        equal(view.pollFirst(), 0); equal(view.descendingSet().pollFirst(), 4);
        equal(root, new TreeSet<>(List.of(1, 2, 3)));
        root.add(6); equal(view.last(), 6);
        view.headSet(3, true).clear(); equal(root, new TreeSet<>(List.of(1, 3, 6)));
        fails(IllegalArgumentException.class, () -> view.subSet(10, true, 0, true));
    }
    private record Key(int value, boolean visible) { }
    private static void storedRepresentative() {
        methods++;
        TreeSet<Key> root = new TreeSet<>(Comparator.comparingInt(Key::value));
        root.add(new Key(1, true)); root.add(new Key(2, false));
        NavigableSet<Key> view = CollectionViews.filtered(CollectionViews.filtered(root, x -> true), Key::visible);
        check(view.contains(new Key(1, false))); check(!view.contains(new Key(2, true)));
        check(view.remove(new Key(1, false))); check(root.size() == 1);
    }
    private record Tagged(int order, int serial) { }
    private static void snapshotsAndPurpose() throws Exception {
        methods++;
        List<Tagged> values = List.of(new Tagged(2, 0), new Tagged(1, 1), new Tagged(2, 2), new Tagged(1, 3));
        Collection<Tagged> noSize = new AbstractCollection<>() {
            @Override public Iterator<Tagged> iterator() { return values.iterator(); }
            @Override public int size() { throw new AssertionError("eager size"); }
        };
        AtomicInteger count = new AtomicInteger();
        List<Tagged> snapshot = CollectionViews.filteredSortedSnapshot(noSize, x -> {
            count.incrementAndGet(); return true;
        }, Comparator.comparingInt(Tagged::order));
        equal(count.get(), 4); equal(snapshot.stream().map(Tagged::serial).toList(), List.of(1, 3, 0, 2));
        fails(UnsupportedOperationException.class, () -> snapshot.add(values.getFirst()));
        TreeSet<Integer> root = new TreeSet<>(List.of(0, 1, 2, 3));
        NavigableSet<Integer> v = CollectionViews.filtered(CollectionViews.filtered(root, x -> true), x -> x % 2 == 0);
        NavigableSet<Integer> readOnly = Collections.unmodifiableNavigableSet(v);
        root.add(4); equal(readOnly.last(), 4);
        fails(UnsupportedOperationException.class, readOnly::pollFirst);
        NavigableSet<Integer> checked = Collections.checkedNavigableSet(v, Integer.class);
        check(checked.add(6)); equal(root.last(), 6);
        SortedSet<Integer> sync = Collections.synchronizedSortedSet(v);
        synchronized (sync) { equal(new ArrayList<>(sync), List.of(0, 2, 4, 6)); }
        Collection<Integer> hiddenOwner = CollectionViews.filtered(readOnly, x -> x >= 2);
        fails(UnsupportedOperationException.class, () -> hiddenOwner.add(8));
    }
    private static void randomOracle() {
        methods++;
        Random random = new Random(0x4d33464cL);
        for (int round = 0; round < 100; round++) {
            ArrayList<Integer> root = new ArrayList<>();
            for (int i = 0; i < 100; i++) root.add(random.nextInt(128) - 64);
            int mod = 2 + random.nextInt(7), lo = -48 + random.nextInt(24);
            Collection<Integer> v = root;
            List<Predicate<Integer>> terms = new ArrayList<>();
            terms.add(x -> x >= lo); terms.add(x -> x % mod != 0); terms.add(x -> x <= 48);
            for (Predicate<Integer> p : terms) v = CollectionViews.filtered(v, p);
            List<Integer> oracle = new ArrayList<>();
            for (Integer x : root) {
                boolean selected = true; for (Predicate<Integer> p : terms) if (!p.test(x)) { selected = false; break; }
                if (selected) oracle.add(x);
            }
            equal(v.stream().toList(), oracle); equal(v.parallelStream().toList(), oracle);
            equal(v.size(), oracle.size()); equal(v.isEmpty(), oracle.isEmpty());
            for (int probe = -64; probe < 64; probe++) equal(v.contains(probe), oracle.contains(probe));
            List<Integer> sorted = new ArrayList<>(oracle); sorted.sort(null);
            equal(CollectionViews.sortedSnapshot(v, null), sorted);
        }
    }
    private static void deferredPlanAndBranching() throws Exception {
        methods++;
        Collection<Integer> root = new ArrayList<>(List.of(1, 2, 3, 4));
        Collection<Integer> prefix = CollectionViews.filtered(CollectionViews.filtered(root, x -> true), x -> x > 1);
        Object old = predicate(prefix); Field plans = field(old.getClass(), "plan");
        check(plans.get(old) == null); prefix.spliterator().estimateSize(); check(plans.get(old) == null);
        check(prefix.contains(2)); Object priorPlan = plans.get(old); check(priorPlan != null);
        Collection<Integer> branchA = CollectionViews.filtered(prefix, x -> x % 2 == 0);
        Collection<Integer> branchB = CollectionViews.filtered(prefix, x -> x % 2 != 0);
        equal(branchA.stream().toList(), List.of(2, 4)); equal(branchB.stream().toList(), List.of(3));
        equal(prefix.stream().toList(), List.of(2, 3, 4)); check(plans.get(old) == priorPlan);
        Object fresh = predicate(branchA);
        for (Field f : fresh.getClass().getDeclaredFields()) { f.setAccessible(true); check(f.get(fresh) != old); }
        Object term = field(fresh.getClass(), "tail").get(fresh);
        while (term != null) {
            Object p = field(term.getClass(), "predicate").get(term);
            check(p != old && p != fresh && p != prefix && p != priorPlan);
            term = field(term.getClass(), "previous").get(term);
        }
        AtomicBoolean gate = new AtomicBoolean(true);
        Collection<Integer> dynamic = CollectionViews.filtered(CollectionViews.filtered(root, x -> true), x -> gate.get());
        check(dynamic.contains(1)); Object plan = plans.get(predicate(dynamic));
        gate.set(false); check(!dynamic.contains(1)); check(plans.get(predicate(dynamic)) == plan);
        // Deliberately toggled only between operations: executable plan caching is not result caching.
    }
    private static void deepAndPublication() throws Exception {
        methods++;
        Collection<Integer> v = new ArrayList<>(List.of(42));
        for (int i = 0; i < 20_000; i++) v = CollectionViews.filtered(v, x -> x != null);
        equal(v.iterator().next(), 42); equal(v.stream().findFirst().orElseThrow(), 42);
        check(v.contains(42)); equal(v.size(), 1);
        NavigableSet<Integer> sorted = new TreeSet<>(List.of(0, 2, 4));
        for (int i = 0; i < 20_000; i++) sorted = CollectionViews.filtered(sorted, x -> x % 2 == 0);
        equal(sorted.descendingSet().first(), 4); equal(sorted.ceiling(1), 2); equal(sorted.pollLast(), 4);
        AtomicInteger calls = new AtomicInteger(); Collection<Integer> value = List.of(42);
        for (int i = 0; i < 64; i++) value = CollectionViews.filtered(value, x -> { calls.incrementAndGet(); return true; });
        final Collection<Integer> concurrent = value;
        CountDownLatch go = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newFixedThreadPool(8)) {
            List<Future<?>> jobs = new ArrayList<>();
            for (int i = 0; i < 8; i++) jobs.add(pool.submit(() -> {
                try { go.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
                for (int n = 0; n < 100; n++) if (!concurrent.contains(42)) throw new AssertionError("publication");
            }));
            go.countDown(); for (Future<?> job : jobs) job.get(20, TimeUnit.SECONDS);
        }
        equal(calls.get(), 8 * 100 * 64);
    }
    @SuppressWarnings("unchecked")
    private static void warmAllocation() throws Exception {
        methods++;
        Collection<Integer> v = List.of(42);
        for (int i = 0; i < 64; i++) v = CollectionViews.filtered(v, x -> x != null);
        Predicate<Integer> predicate = (Predicate<Integer>) predicate(v);
        for (int i = 0; i < 100_000; i++) if (!predicate.test(42)) throw new AssertionError();
        com.sun.management.ThreadMXBean bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!bean.isThreadAllocatedMemorySupported()) { System.out.println("allocation_observation=UNAVAILABLE"); return; }
        bean.setThreadAllocatedMemoryEnabled(true); long id = Thread.currentThread().threadId();
        long before = bean.getThreadAllocatedBytes(id); int accepted = 0;
        for (int i = 0; i < 100_000; i++) if (predicate.test(42)) accepted++;
        long bytes = bean.getThreadAllocatedBytes(id) - before;
        equal(accepted, 100_000);
        System.out.println("warm_predicate_tests=100000 depth=64 allocated_bytes=" + bytes);
    }
    private static int nativeFirst(Method method, int[] pairs, int runs, int key) throws Exception {
        try { return (Integer) method.invoke(null, pairs, runs, key); }
        catch (InvocationTargetException e) {
            if (e.getCause() instanceof Exception x) throw x;
            if (e.getCause() instanceof Error x) throw x;
            throw new AssertionError(e.getCause());
        }
    }
    private static void nativeProjection(String library) throws Exception {
        methods++;
        System.load(library);
        Method first = RunSearchJni.class.getDeclaredMethod("first", int[].class, int.class, int.class);
        first.setAccessible(true);
        TreeSet<Integer> root = new TreeSet<>(); for (int i = 0; i < 512; i++) root.add(i);
        NavigableSet<Integer> selected = CollectionViews.filtered(CollectionViews.filtered(root, x -> x % 3 == 0), x -> x < 400);
        List<Integer> snapshot = CollectionViews.sortedSnapshot(selected.descendingSet(), null);
        int[] pairs = new int[2 * snapshot.size() + 4];
        for (int i = 0; i < snapshot.size(); i++) { pairs[2*i] = 2*snapshot.get(i); pairs[2*i+1] = pairs[2*i]+1; }
        pairs[2*snapshot.size()] = -99; pairs[2*snapshot.size()+1] = Integer.MIN_VALUE;
        for (int key = -1; key <= 1025; key++) {
            int expected = 0; while (expected < snapshot.size() && pairs[2*expected+1] <= key) expected++;
            equal(nativeFirst(first, pairs, snapshot.size(), key), expected);
        }
        fails(IllegalArgumentException.class, () -> nativeFirst(first, null, 0, 0));
        fails(IllegalArgumentException.class, () -> nativeFirst(first, new int[]{0, 1}, -1, 0));
        fails(IllegalArgumentException.class, () -> nativeFirst(first, new int[]{0, 1}, 2, 0));
        fails(IllegalArgumentException.class, () -> nativeFirst(first, new int[]{2, 1}, 1, 0));
        equal(nativeFirst(first, new int[0], 0, 0), 0);
        System.out.println("native_projection=PASS queries=1027 production_native_changes=0");
    }
    public static void main(String[] args) throws Exception {
        boolean baseline = Arrays.asList(args).contains("--baseline");
        lazyAndOrder(); failuresAndAdmission(); bulkAndIterator(); sortedRanges();
        storedRepresentative(); snapshotsAndPurpose(); randomOracle();
        if (!baseline) { deferredPlanAndBranching(); deepAndPublication(); warmAllocation(); }
        for (int i = 0; i < args.length; i++) if (args[i].equals("--native")) nativeProjection(args[++i]);
        System.out.println("CollectionViewsContract methods=" + methods + " checks=" + checks + " baseline=" + baseline + " PASS");
    }
}
