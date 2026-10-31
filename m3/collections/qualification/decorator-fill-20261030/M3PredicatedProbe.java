/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.ResettableIterator;

/**
 * Pattern DECORATOR_DIFFERENTIAL probe (PREDICATED family): deterministic scripts the seeded differential cannot reach.
 * Both sides start from fresh, equal backings with one counting {@link Pol} per side and lane; after every call the result
 * (exception class and contract message included), the backing state and the per-lane predicate call counts are compared.
 * <ul>
 * <li>VIEW_SWEEP (refutation R1): every interface method of every view reachable in two hops from the decorator (for
 * example {@code headSet(8).headSet(6).add(4)}, {@code headSet(6).addAll([4])}, {@code entrySet().iterator()}) is called
 * with accepted, rejected and null arguments. Each call is replayed from a fresh decorator, so calls never interfere.
 * <li>MAP_ITERATOR (R2): getKey/getValue/setValue before next(), after remove() and after reset(); remove() twice;
 * re-drain after reset; next() past the end.
 * <li>LIST_ITERATOR (skeptic missed semantics 3): backward navigation, set/add of accepted, rejected and null elements.
 * <li>SERIAL (R7): the NotSerializableException message with a non-serializable predicate, a non-serializable backing
 * (a proxy) or both, so the stream order of lanes and backing is observable.
 * </ul>
 * Usage: {@code java -cp commons.jar;classes;probes M3PredicatedProbe <m3 fqn> <commons fqn> <backing> <factory> <out.tsv>
 * [policy]}. Exit 0 = no mismatch, 4 = mismatches appended to out.tsv.
 */
public final class M3PredicatedProbe {
    private static final Logger LOG = Logger.getLogger(M3PredicatedProbe.class.getName());
    private static final Integer REJECTED = 4;
    private static final int DEPTH = 2;
    private static final int VARIANTS = 3;
    private static final Object SKIP = new Object();
    private static final Map<String, Supplier<Object>> BACKINGS = Map.ofEntries(
            Map.entry("list", ArrayList::new), Map.entry("deque", java.util.LinkedList::new),
            Map.entry("treeset", java.util.TreeSet::new), Map.entry("hashset", java.util.HashSet::new),
            Map.entry("hashbag", org.apache.commons.collections4.bag.HashBag::new),
            Map.entry("treebag", org.apache.commons.collections4.bag.TreeBag::new),
            Map.entry("hashmultiset", org.apache.commons.collections4.multiset.HashMultiSet::new),
            Map.entry("hashmap", HashMap::new), Map.entry("treemap", java.util.TreeMap::new),
            Map.entry("linkedmap", org.apache.commons.collections4.map.LinkedMap::new));
    private static boolean rejectNull;

    /** Serializable counting predicate with a stable toString. */
    static final class Pol implements org.apache.commons.collections4.Predicate<Object>, java.io.Serializable {
        private static final long serialVersionUID = 1L;
        private int calls;

        @Override
        public boolean evaluate(final Object v) {
            calls++;
            return !rejects(v);
        }

        @Override
        public String toString() {
            return "Pol";
        }
    }

    /** R7: a predicate that is not serializable (named, so the exception message is stable). */
    static final class NonSerialPol implements org.apache.commons.collections4.Predicate<Object> {
        @Override
        public boolean evaluate(final Object v) {
            return !rejects(v);
        }

        @Override
        public String toString() {
            return "NonSerialPol";
        }
    }

    /** R7: forwards every call of a proxy backing to a real backing; not serializable, so the proxy is not either. */
    static final class Forward implements InvocationHandler {
        private final Object target;

        Forward(final Object target) {
            this.target = target;
        }

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            try {
                return method.invoke(target, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }
    }

    record Pair(Object o, Object x, Object b1, Object b2, List<Pol> po, List<Pol> px) {
    }

    record Step(Method method, int variant, boolean root) {
    }

    private final String kind;
    private final String policy;
    private final Method m3Factory;
    private final Method oracleFactory;
    private final List<Method> rootMethods;
    private final List<String> mismatches = new ArrayList<>();
    private long checks;

    private M3PredicatedProbe(Class<?> m3, Class<?> oracle, String kind, String factory, String policy) throws NoSuchMethodException {
        this.kind = kind;
        this.policy = policy;
        this.oracleFactory = Arrays.stream(oracle.getMethods()).filter(m -> Modifier.isStatic(m.getModifiers()) && m.getName().equals(factory))
                .min(Comparator.comparing(Method::toGenericString)).orElseThrow();
        this.m3Factory = m3.getMethod(factory, oracleFactory.getParameterTypes());
        this.rootMethods = Arrays.stream(m3.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()) && !Modifier.isStatic(m.getModifiers()) && !m.isSynthetic())
                .sorted(Comparator.comparing(Method::toGenericString)).toList();
    }

    public static void main(String[] args) throws Exception {
        String policy = args.length > 5 && !args[5].equals("-") ? args[5] : "both";
        rejectNull = policy.equals("rejectnull");
        M3PredicatedProbe p = new M3PredicatedProbe(Class.forName(args[0]), Class.forName(args[1]), args[2], args[3], policy);
        p.sweep(new ArrayList<>(), 0);
        p.mapIterator();
        p.listIterators();
        p.serial();
        Files.write(Path.of(args[4]), p.mismatches.stream().map(r -> args[0] + "\t" + r).toList(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        LOG.info(() -> "M3_PREDICATED_PROBE class=" + args[0] + " backing=" + args[2] + " policy=" + policy + " checks=" + p.checks
                + " mismatches=" + p.mismatches.size());
        System.exit(p.mismatches.isEmpty() ? 0 : 4);
    }

    static boolean rejects(final Object v) {
        return v == null ? rejectNull : v.hashCode() % 5 == 4;
    }

    @SuppressWarnings("unchecked")
    private Object backing() {
        Object b = BACKINGS.get(kind).get();
        for (int k = 0; k < 10; k++) {
            int v = (k + 2) % 10;
            if (b instanceof Map<?, ?> m && !rejects(k) && !rejects(v)) {
                ((Map<Object, Object>) m).put(k, v);
            } else if (b instanceof Collection<?> c && !rejects(k)) {
                ((Collection<Object>) c).add(k);
                if (k % 3 == 0 && !(b instanceof java.util.Set)) {
                    ((Collection<Object>) c).add(k);
                }
            }
        }
        return b;
    }

    private List<Pol> pols() {
        List<Pol> out = new ArrayList<>();
        for (int i = 1; i < oracleFactory.getParameterCount(); i++) {
            out.add(new Pol());
        }
        return out;
    }

    private Object[] factoryArgs(Object backing, List<?> lanes) {
        Object[] a = new Object[lanes.size() + 1];
        a[0] = backing;
        for (int i = 1; i < a.length; i++) {
            boolean off = policy.equals("keynull") && i == 1 || policy.equals("valuenull") && i == 2;
            a[i] = off ? null : lanes.get(i - 1);
        }
        return a;
    }

    private Pair fresh() {
        Object b1 = backing();
        Object b2 = backing();
        List<Pol> po = pols();
        List<Pol> px = pols();
        return new Pair(call(oracleFactory, null, factoryArgs(b1, po)), call(m3Factory, null, factoryArgs(b2, px)), b1, b2, po, px);
    }

    /** VIEW_SWEEP node: replay the path from a fresh decorator, then call every method of the node's view type. */
    private void sweep(List<Step> path, int depth) {
        List<Method> methods = path.isEmpty() ? rootMethods : viewMethods(path.get(path.size() - 1).method().getReturnType());
        for (Method m : methods) {
            for (int v = 0; v < (m.getParameterCount() == 0 ? 1 : VARIANTS) && mismatches.size() < 50; v++) {
                Object[] a = args(m, v);
                if (a == null) {
                    continue;
                }
                Pair p = fresh();
                Object[] node = replay(p, path);
                if (node == null) {
                    return;
                }
                Object ro = invoke(m, node[0], a, path.isEmpty());
                Object rx = invoke(m, node[1], args(m, v), path.isEmpty());
                String where = "VIEW_SWEEP " + label(path) + m.getName() + Arrays.toString(m.getParameterTypes()) + " v" + v;
                compare(where, norm(identity(m, node[0], ro)), norm(identity(m, node[1], rx)));
                compareState(where, p);
                List<Step> next = new ArrayList<>(path);
                next.add(new Step(m, v, path.isEmpty()));
                if (depth < DEPTH && isView(m.getReturnType()) && !(ro instanceof Throwable) && !(rx instanceof Throwable)) {
                    sweep(next, depth + 1);
                }
            }
        }
    }

    /** hashCode()/toString() inherited from Object are identity values: compared as IDENTITY, not as numbers or class names. */
    private static Object identity(Method m, Object target, Object result) {
        if (m.getParameterCount() == 0 && (m.getName().equals("hashCode") || m.getName().equals("toString"))) {
            try {
                return target.getClass().getMethod(m.getName()).getDeclaringClass() == Object.class ? "IDENTITY_" + m.getName() : result;
            } catch (NoSuchMethodException e) {
                return e;
            }
        }
        return result;
    }

    private static String label(List<Step> path) {
        StringBuilder s = new StringBuilder();
        path.forEach(st -> s.append(st.method().getName()).append("/v").append(st.variant()).append('.'));
        return s.toString();
    }

    private Object[] replay(Pair p, List<Step> path) {
        Object o = p.o();
        Object x = p.x();
        for (Step st : path) {
            Object[] a = args(st.method(), st.variant());
            o = invoke(st.method(), o, a, st.root());
            x = invoke(st.method(), x, args(st.method(), st.variant()), st.root());
            if (o instanceof Throwable || x instanceof Throwable || o == null || x == null) {
                return null;
            }
        }
        return new Object[] {o, x};
    }

    private static boolean isView(Class<?> t) {
        return t.isInterface() && (Collection.class.isAssignableFrom(t) || Map.class.isAssignableFrom(t) || Iterator.class.isAssignableFrom(t));
    }

    private static List<Method> viewMethods(Class<?> type) {
        return Arrays.stream(type.getMethods()).filter(m -> !Modifier.isStatic(m.getModifiers()))
                .sorted(Comparator.comparing(Method::toGenericString)).toList();
    }

    /** Root calls go through the receiver's own public method (accessible); view calls through the interface method. */
    private static Object invoke(Method m, Object target, Object[] a, boolean root) {
        try {
            Method callee = root ? target.getClass().getMethod(m.getName(), m.getParameterTypes()) : m;
            callee.setAccessible(true);
            return callee.invoke(target, a);
        } catch (InvocationTargetException e) {
            return e.getCause();
        } catch (ReflectiveOperationException | RuntimeException e) {
            return e;
        }
    }

    private Object[] args(Method m, int variant) {
        Object[] a = new Object[m.getParameterCount()];
        for (int i = 0; i < a.length; i++) {
            a[i] = arg(m.getParameterTypes()[i], variant);
            if (a[i] == SKIP) {
                return null;
            }
        }
        return a;
    }

    @SuppressWarnings("unchecked")
    private static Object arg(Class<?> t, int v) {
        Object element = v == 0 ? (Object) 3 : v == 1 ? REJECTED : null;
        if (t == int.class || t == Integer.class || t == long.class) {
            return t == long.class ? (Object) (long) Math.min(v, 1) : (Object) Math.min(v, 1);
        }
        if (t == boolean.class) {
            return v != 1;
        }
        if (t.isArray()) {
            return new Object[3];
        }
        if (Collection.class.isAssignableFrom(t) || t == Iterable.class) {
            return new ArrayList<>(Arrays.asList(element));
        }
        if (Map.class.isAssignableFrom(t)) {
            Map<Object, Object> m = new LinkedHashMap<>();
            m.put(v == 2 ? REJECTED : 3, v == 1 ? REJECTED : 3);
            return m;
        }
        return switch (t.getName()) {
            case "java.lang.Object" -> element;
            case "java.util.function.Predicate" -> (java.util.function.Predicate<Object>) x -> v == 2 || v == 1 && Objects.equals(x, 3);
            case "java.util.function.Consumer" -> (java.util.function.Consumer<Object>) x -> { };
            case "java.util.function.BiConsumer" -> (java.util.function.BiConsumer<Object, Object>) (x, y) -> { };
            case "java.util.function.Function" -> (java.util.function.Function<Object, Object>) x -> element;
            case "java.util.function.UnaryOperator" -> (java.util.function.UnaryOperator<Object>) x -> v == 0 ? x : element;
            case "java.util.function.BiFunction" -> (java.util.function.BiFunction<Object, Object, Object>) (x, y) -> element;
            case "java.util.function.IntFunction" -> (java.util.function.IntFunction<Object[]>) Object[]::new;
            case "java.util.Comparator" -> Comparator.nullsFirst(Comparator.comparing(Object::toString));
            default -> SKIP;
        };
    }

    /** R2: the MapIterator state machine, incl. ResettableIterator.reset(), on both sides from fresh maps. */
    private void mapIterator() {
        Method mi = rootMethods.stream().filter(m -> m.getName().equals("mapIterator") && m.getParameterCount() == 0).findFirst().orElse(null);
        if (mi == null) {
            return;
        }
        String[] script = {"getKey", "getValue", "setValue3", "remove", "hasNext", "next", "getKey", "getValue", "setValue3", "setValueR",
            "remove", "getKey", "getValue", "setValue3", "remove", "next", "reset", "getKey", "getValue", "setValue3", "drain", "next",
            "getKey", "reset", "next", "remove", "reset", "getKey", "remove", "hasNext", "next", "getKey"};
        Pair p = fresh();
        Object io = invoke(mi, p.o(), new Object[0], true);
        Object ix = invoke(mi, p.x(), new Object[0], true);
        compare("MAP_ITERATOR resettable", io instanceof ResettableIterator, ix instanceof ResettableIterator);
        if (!(io instanceof MapIterator<?, ?>) || !(ix instanceof MapIterator<?, ?>)) {
            compare("MAP_ITERATOR " + mi, norm(io), norm(ix));
            return;
        }
        for (int i = 0; i < script.length; i++) {
            String where = "MAP_ITERATOR " + i + ":" + script[i];
            compare(where, norm(mapOp((MapIterator<?, ?>) io, script[i])), norm(mapOp((MapIterator<?, ?>) ix, script[i])));
            compareState(where, p);
        }
    }

    @SuppressWarnings("unchecked")
    private static Object mapOp(MapIterator<?, ?> it, String op) {
        MapIterator<Object, Object> m = (MapIterator<Object, Object>) it;
        try {
            return switch (op) {
                case "getKey" -> m.getKey();
                case "getValue" -> m.getValue();
                case "setValue3" -> m.setValue(3);
                case "setValueR" -> m.setValue(REJECTED);
                case "hasNext" -> m.hasNext();
                case "next" -> m.next();
                case "reset" -> {
                    ((ResettableIterator<?>) m).reset();
                    yield "reset";
                }
                case "remove" -> {
                    m.remove();
                    yield "removed";
                }
                default -> {
                    List<Object> keys = new ArrayList<>();
                    while (m.hasNext()) {
                        keys.add(m.next() + "=" + m.getValue());
                    }
                    yield keys;
                }
            };
        } catch (RuntimeException e) {
            return e;
        }
    }

    /** Skeptic missed semantics 3: backward navigation and validated set/add on the list iterator views. */
    private void listIterators() {
        String[] script = {"hasPrevious", "previous", "next", "next", "previous", "hasPrevious", "nextIndex", "previousIndex", "setR", "set3",
            "addR", "add3", "previous", "set3", "remove", "remove", "next", "addNull", "setNull", "previousIndex", "nextIndex", "previous",
            "setNull", "next", "next"};
        for (Method m : rootMethods) {
            if (!m.getName().equals("listIterator")) {
                continue;
            }
            Pair p = fresh();
            Object[] a = m.getParameterCount() == 0 ? new Object[0] : new Object[] {1};
            Object io = invoke(m, p.o(), a, true);
            Object ix = invoke(m, p.x(), a.clone(), true);
            if (!(io instanceof ListIterator<?>) || !(ix instanceof ListIterator<?>)) {
                compare("LIST_ITERATOR " + m, norm(io), norm(ix));
                continue;
            }
            for (int i = 0; i < script.length; i++) {
                String where = "LIST_ITERATOR " + m.getParameterCount() + " " + i + ":" + script[i];
                compare(where, norm(listOp((ListIterator<?>) io, script[i])), norm(listOp((ListIterator<?>) ix, script[i])));
                compareState(where, p);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Object listOp(ListIterator<?> it, String op) {
        ListIterator<Object> l = (ListIterator<Object>) it;
        try {
            return switch (op) {
                case "hasPrevious" -> l.hasPrevious();
                case "previous" -> l.previous();
                case "next" -> l.next();
                case "nextIndex" -> l.nextIndex();
                case "previousIndex" -> l.previousIndex();
                case "setR" -> run(() -> l.set(REJECTED));
                case "set3" -> run(() -> l.set(3));
                case "setNull" -> run(() -> l.set(null));
                case "addR" -> run(() -> l.add(REJECTED));
                case "add3" -> run(() -> l.add(3));
                case "addNull" -> run(() -> l.add(null));
                default -> run(l::remove);
            };
        } catch (RuntimeException e) {
            return e;
        }
    }

    private static Object run(Runnable r) {
        r.run();
        return "done";
    }

    /** R7: serialization with non-serializable lanes and/or a non-serializable (proxy) backing; messages name the culprit. */
    private void serial() {
        for (String c : List.of("lanes", "backing", "both")) {
            boolean lanes = !c.equals("backing");
            boolean proxy = !c.equals("lanes");
            compare("SERIAL " + c, serialCase(oracleFactory, lanes, proxy), serialCase(m3Factory, lanes, proxy));
        }
    }

    private Object serialCase(Method factory, boolean nonSerialLanes, boolean proxyBacking) {
        Object real = backing();
        Object b = proxyBacking ? Proxy.newProxyInstance(M3PredicatedProbe.class.getClassLoader(),
                new Class<?>[] {factory.getParameterTypes()[0]}, new Forward(real)) : real;
        List<Object> lanes = new ArrayList<>();
        for (int i = 1; i < factory.getParameterCount(); i++) {
            lanes.add(nonSerialLanes ? new NonSerialPol() : new Pol());
        }
        Object d = call(factory, null, factoryArgs(b, lanes));
        if (d instanceof Throwable) {
            return norm(d);
        }
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
                out.writeObject(d);
            }
            try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
                return "OK:" + in.readObject();
            }
        } catch (Exception e) {
            return "EX:" + e.getClass().getName() + ":" + e.getMessage();
        }
    }

    private static Object call(Method m, Object target, Object[] a) {
        try {
            return m.invoke(target, a);
        } catch (InvocationTargetException e) {
            return e.getCause();
        } catch (IllegalAccessException e) {
            return e;
        }
    }

    private void compareState(String where, Pair p) {
        compare(where + " backing state", String.valueOf(p.b1()), String.valueOf(p.b2()));
        compare(where + " predicate calls per lane", p.po().stream().map(x -> x.calls).toList(), p.px().stream().map(x -> x.calls).toList());
    }

    private static Object norm(Object v) {
        try {
            return norm0(v);
        } catch (RuntimeException e) {
            return "NORM_EX:" + e.getClass().getName() + message(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Object norm0(Object v) {
        if (v == null || v instanceof Number || v instanceof String || v instanceof Boolean) {
            return String.valueOf(v);
        }
        if (v instanceof Throwable t) {
            return "EX:" + t.getClass().getName() + message(t);
        }
        if (v instanceof Object[] a) {
            return "A" + Arrays.stream(a).map(M3PredicatedProbe::norm).toList();
        }
        if (v instanceof Iterator<?> it) {
            return (v instanceof MapIterator ? "MI" : v instanceof ListIterator ? "LI" : "I") + shape(v);
        }
        if (v instanceof Spliterator<?> s) {
            List<Object> out = new ArrayList<>();
            s.forEachRemaining(out::add);
            return "S" + out;
        }
        if (v instanceof Stream<?> s) {
            return "T" + s.toList();
        }
        if (v instanceof Map.Entry<?, ?> e) {
            return "E" + e.getKey() + "=" + e.getValue() + attempt(() -> ((Map.Entry<Object, Object>) e).setValue(REJECTED))
                    + attempt(() -> ((Map.Entry<Object, Object>) e).setValue(e.getValue()));
        }
        if (v instanceof Collection<?> c) {
            return "C" + shape(c) + c.size() + c;
        }
        if (v instanceof Map<?, ?> m) {
            return "M" + shape(m) + m.size() + m;
        }
        if (v instanceof Comparator<?> c) {
            return "K" + Integer.signum(((Comparator<Object>) c).compare(1, 2));
        }
        return ownToString(v) ? "O:" + v : "OBJ";
    }

    private static boolean ownToString(Object v) {
        try {
            return v.getClass().getMethod("toString").getDeclaringClass() != Object.class;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private static String attempt(Runnable r) {
        try {
            r.run();
            return "/ok";
        } catch (RuntimeException e) {
            return "/" + e.getClass().getSimpleName() + message(e);
        }
    }

    private static final List<Class<?>> SHAPES = List.of(java.util.List.class, java.util.Set.class, java.util.SortedSet.class,
            java.util.NavigableSet.class, java.util.Queue.class, java.util.Deque.class, java.util.RandomAccess.class,
            java.io.Serializable.class, org.apache.commons.collections4.Bag.class, org.apache.commons.collections4.SortedBag.class,
            org.apache.commons.collections4.MultiSet.class, java.util.SortedMap.class, java.util.NavigableMap.class,
            org.apache.commons.collections4.IterableMap.class, org.apache.commons.collections4.OrderedMap.class,
            org.apache.commons.collections4.BidiMap.class, ListIterator.class, MapIterator.class, ResettableIterator.class);

    private static String shape(Object v) {
        StringBuilder bits = new StringBuilder("<");
        SHAPES.forEach(c -> bits.append(c.isInstance(v) ? '1' : '0'));
        return bits.append('>').toString();
    }

    /** IAE messages and non-helpful NPE messages (requireNonNull names) are contract; helpful NPE text is not. */
    private static String message(Throwable t) {
        boolean keep = t instanceof IllegalArgumentException || t instanceof NullPointerException
                && t.getMessage() != null && !t.getMessage().contains("because");
        return keep ? ":" + t.getMessage() : "";
    }

    private void compare(String where, Object oracle, Object m3) {
        checks++;
        if (!String.valueOf(oracle).equals(String.valueOf(m3))) {
            mismatches.add(where + "\toracle=" + oracle + "\tm3=" + m3);
        }
    }
}
