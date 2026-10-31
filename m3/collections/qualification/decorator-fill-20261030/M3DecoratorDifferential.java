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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;
import java.util.Spliterator;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.MultiValuedMap;

/**
 * Pattern DECORATOR_DIFFERENTIAL v2: seeded reflective differential run of one generated owned decorator against the
 * Apache Commons Collections 4.5.0 decorator it mirrors (test oracle only). Two equal backings are built (collection, map,
 * bidi map, multi-valued map, trie or entry set) and each is wrapped by the named static factory of its side; extra factory
 * parameters get the same policy on both sides (Predicate, Transformer, Factory, Comparator, int). Every step either
 * mutates both backings directly (views must follow) or calls one public method the M3 class declares, on both sides with
 * equal arguments. Return values, exception classes, drained iterators, map iterators, entries and views (with
 * non-destructive mutation probes) and the backing state are compared after every step; factory null rejection, factory
 * identity on its own result and a serialization round trip are checked once.
 * Usage: {@code java -cp commons.jar;classes M3DecoratorDifferential.java <m3 fqn> <commons fqn> <backing> <factory> <out.tsv>}.
 * Exit 0 = no mismatch, 4 = mismatches appended to out.tsv.
 */
public final class M3DecoratorDifferential {
    private static final Logger LOG = Logger.getLogger(M3DecoratorDifferential.class.getName());
    private static final long[] SEEDS = {2L, 11L, 42L, 1_009L, 20_261_030L, 0xFEEDL, -5L, 555_555_555L};
    private static final int STEPS = 4_000;
    private static final Object ABSENT = "absent-key";
    private static final Map<String, Supplier<Object>> BACKINGS = Map.ofEntries(
            Map.entry("list", ArrayList::new), Map.entry("deque", java.util.LinkedList::new),
            Map.entry("treeset", java.util.TreeSet::new), Map.entry("hashset", java.util.HashSet::new),
            Map.entry("hashbag", org.apache.commons.collections4.bag.HashBag::new),
            Map.entry("treebag", org.apache.commons.collections4.bag.TreeBag::new),
            Map.entry("hashmultiset", org.apache.commons.collections4.multiset.HashMultiSet::new),
            Map.entry("hashmap", HashMap::new), Map.entry("treemap", java.util.TreeMap::new),
            Map.entry("linkedmap", org.apache.commons.collections4.map.LinkedMap::new),
            Map.entry("dualhashbidi", org.apache.commons.collections4.bidimap.DualHashBidiMap::new),
            Map.entry("dualtreebidi", org.apache.commons.collections4.bidimap.DualTreeBidiMap::new),
            Map.entry("treebidi", org.apache.commons.collections4.bidimap.TreeBidiMap::new),
            Map.entry("multimap", org.apache.commons.collections4.multimap.ArrayListValuedHashMap::new),
            Map.entry("trie", org.apache.commons.collections4.trie.PatriciaTrie::new),
            Map.entry("entryset", HashMap::new));

    private final String kind;
    private final Method m3Factory;
    private final Method oracleFactory;
    private final List<Method> methods;
    private final List<String> mismatches = new ArrayList<>();
    private long checks;

    private M3DecoratorDifferential(Class<?> m3, Class<?> oracle, String kind, String factory) throws NoSuchMethodException {
        this.kind = kind;
        this.oracleFactory = Arrays.stream(oracle.getMethods()).filter(m -> Modifier.isStatic(m.getModifiers()) && m.getName().equals(factory))
                .min(Comparator.comparing(Method::toGenericString)).orElseThrow();
        this.m3Factory = m3.getMethod(factory, oracleFactory.getParameterTypes());
        this.methods = Arrays.stream(m3.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()) && !Modifier.isStatic(m.getModifiers()) && !m.isSynthetic())
                .sorted(Comparator.comparing(Method::toGenericString)).toList();
    }

    public static void main(String[] args) throws Exception {
        M3DecoratorDifferential run = new M3DecoratorDifferential(Class.forName(args[0]), Class.forName(args[1]), args[2], args[3]);
        run.once();
        Arrays.stream(SEEDS).forEach(run::seed);
        Files.write(Path.of(args[4]), run.mismatches.stream().map(r -> args[0] + "\t" + r).toList(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        LOG.info(() -> "M3_DECORATOR_DIFF class=" + args[0] + " oracle=" + args[1] + " factory=" + args[3] + " methods=" + run.methods.size()
                + " seeds=" + SEEDS.length + " steps=" + STEPS + " checks=" + run.checks + " mismatches=" + run.mismatches.size());
        System.exit(run.mismatches.isEmpty() ? 0 : 4);
    }

    private void once() {
        compare("factory(null)", norm(build(oracleFactory, null)), norm(build(m3Factory, null)));
        Object o = build(oracleFactory, filled(new Random(7)));
        Object x = build(m3Factory, filled(new Random(7)));
        compare("factory(own result) identity", build(oracleFactory, o) == o, build(m3Factory, x) == x);
        compare("serial round trip", norm(roundTrip(o)), norm(roundTrip(x)));
    }

    private Object build(Method factory, Object owner) {
        Object[] args = new Object[factory.getParameterCount()];
        Random r = new Random(99);
        args[0] = owner == null || factory.getParameterTypes()[0].isInstance(owner) ? owner : target(owner);
        for (int i = 1; i < args.length; i++) {
            args[i] = arg(factory.getParameterTypes()[i], r);
        }
        return call(factory, null, args);
    }

    private Object target(Object owner) {
        return kind.equals("entryset") ? ((Map<?, ?>) owner).entrySet() : owner;
    }

    private static Object roundTrip(Object v) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
                out.writeObject(v);
            }
            try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
                return in.readObject();
            }
        } catch (Exception e) {
            return e;
        }
    }

    private Object filled(Random r) {
        Object owner = BACKINGS.get(kind).get();
        for (int i = r.nextInt(12); i > 0; i--) {
            mutate(owner, r, true);
        }
        return owner;
    }

    private Object key(Random r) {
        return kind.equals("trie") ? "k" + r.nextInt(10) : (Object) r.nextInt(10);
    }

    @SuppressWarnings("unchecked")
    private Object mutate(Object owner, Random r, boolean add) {
        Object k = key(r);
        int v = r.nextInt(10);
        if (owner instanceof MultiValuedMap<?, ?> mv) {
            return add ? ((MultiValuedMap<Object, Object>) mv).put(k, v) : mv.remove(k);
        }
        if (owner instanceof Map<?, ?> map) {
            return add ? ((Map<Object, Object>) map).put(k, kind.endsWith("bidi") ? (Object) (v + 10 * r.nextInt(3)) : v) : map.remove(k);
        }
        return add ? ((Collection<Object>) owner).add(k) : ((Collection<?>) owner).remove(k);
    }

    private void seed(long seed) {
        Random r = new Random(seed);
        Object b1 = filled(new Random(seed));
        Object b2 = filled(new Random(seed));
        Object o = build(oracleFactory, b1);
        Object x = build(m3Factory, b2);
        if (o instanceof Throwable || x instanceof Throwable) {
            compare("seed " + seed + " factory", norm(o), norm(x));
            return;
        }
        for (int step = 0; step < STEPS && mismatches.size() < 50; step++) {
            step(r, b1, b2, o, x, seed + "@" + step);
        }
    }

    private void step(Random r, Object b1, Object b2, Object o, Object x, String where) {
        if (r.nextInt(5) == 0) {
            long mutationSeed = r.nextLong();
            boolean add = r.nextBoolean();
            compare(where + " backing " + (add ? "add" : "remove"), norm(mutate(b1, new Random(mutationSeed), add)),
                    norm(mutate(b2, new Random(mutationSeed), add)));
        } else {
            Method m = methods.get(r.nextInt(methods.size()));
            long argSeed = r.nextLong();
            if (isEquals(m) && r.nextInt(3) == 0) {
                // equal but not identical argument: the true path of equals, which identity and Integer arguments never reach
                compare(where + " equals(copy of backing)", norm(callOracle(o, m, copy(b1))), norm(call(m, x, new Object[] {copy(b2)})));
            } else {
                compare(where + " " + m.getName() + Arrays.toString(m.getParameterTypes()), invokeOracle(o, m, argSeed),
                        norm(call(m, x, args(m.getParameterTypes(), new Random(argSeed)))));
            }
        }
        compare(where + " backing state", b1.toString(), b2.toString());
    }

    private static boolean isEquals(Method m) {
        return m.getName().equals("equals") && m.getParameterCount() == 1 && m.getParameterTypes()[0] == Object.class;
    }

    private static Object callOracle(Object o, Method m, Object arg) {
        try {
            Method om = o.getClass().getMethod(m.getName(), m.getParameterTypes());
            om.setAccessible(true);
            return call(om, o, new Object[] {arg});
        } catch (NoSuchMethodException e) {
            return "NO_ORACLE_METHOD";
        }
    }

    /** A fresh backing of the same kind with the same content: equal, never identical. */
    @SuppressWarnings("unchecked")
    private Object copy(Object owner) {
        Object c = BACKINGS.get(kind).get();
        if (c instanceof MultiValuedMap<?, ?> mv) {
            ((MultiValuedMap<Object, Object>) mv).putAll((MultiValuedMap<Object, Object>) owner);
        } else if (c instanceof Map<?, ?> map) {
            ((Map<Object, Object>) map).putAll((Map<Object, Object>) owner);
        } else {
            ((Collection<Object>) c).addAll((Collection<Object>) owner);
        }
        return kind.equals("entryset") ? ((Map<?, ?>) c).entrySet() : c;
    }

    private Object invokeOracle(Object o, Method m, long argSeed) {
        try {
            Method om = o.getClass().getMethod(m.getName(), m.getParameterTypes());
            om.setAccessible(true);
            return norm(call(om, o, args(m.getParameterTypes(), new Random(argSeed))));
        } catch (NoSuchMethodException e) {
            return "NO_ORACLE_METHOD";
        }
    }

    private static Object call(Method m, Object target, Object[] args) {
        try {
            return m.invoke(target, args);
        } catch (InvocationTargetException e) {
            return e.getCause();
        } catch (IllegalAccessException e) {
            return e;
        }
    }

    private Object[] args(Class<?>[] types, Random r) {
        return Arrays.stream(types).map(t -> arg(t, r)).toArray();
    }

    private Object arg(Class<?> t, Random r) {
        if (t == int.class || t == Integer.class || t == long.class) {
            int v = r.nextInt(8) - 1;
            return t == long.class ? (Object) (long) v : (Object) v;
        }
        if (t == boolean.class) {
            return r.nextBoolean();
        }
        if (t.isArray()) {
            return kind.equals("trie") ? new String[r.nextInt(6)] : new Integer[r.nextInt(6)];
        }
        if (Collection.class.isAssignableFrom(t) || t == Iterable.class) {
            List<Object> list = new ArrayList<>();
            for (int i = r.nextInt(4); i > 0; i--) {
                list.add(key(r));
            }
            return list;
        }
        return structured(t, r);
    }

    private Object structured(Class<?> t, Random r) {
        if (Map.class.isAssignableFrom(t)) {
            Map<Object, Object> map = new HashMap<>();
            for (int i = r.nextInt(3); i > 0; i--) {
                map.put(key(r), r.nextInt(10));
            }
            return map;
        }
        if (MultiValuedMap.class.isAssignableFrom(t)) {
            MultiValuedMap<Object, Object> mv = new org.apache.commons.collections4.multimap.ArrayListValuedHashMap<>();
            for (int i = r.nextInt(3); i > 0; i--) {
                mv.put(key(r), r.nextInt(10));
            }
            return mv;
        }
        if (t == Map.Entry.class) {
            return new AbstractMap.SimpleEntry<>(key(r), r.nextInt(10));
        }
        return functional(t, r);
    }

    private Object functional(Class<?> t, Random r) {
        int pick = r.nextInt(10);
        return switch (t.getName()) {
            case "java.util.function.Predicate" -> (java.util.function.Predicate<Object>) v -> v != null && v.hashCode() % 3 == pick % 3;
            case "java.util.function.Consumer" -> (java.util.function.Consumer<Object>) v -> { };
            case "java.util.function.BiConsumer" -> (java.util.function.BiConsumer<Object, Object>) (k, v) -> { };
            case "java.util.function.IntFunction" -> (java.util.function.IntFunction<Object[]>) Object[]::new;
            case "java.util.function.UnaryOperator" -> (java.util.function.UnaryOperator<Object>) v -> v;
            case "java.util.function.Function" -> (java.util.function.Function<Object, Object>) k -> pick;
            case "java.util.function.BiFunction" -> (java.util.function.BiFunction<Object, Object, Object>) (k, v) -> pick % 4 == 0 ? null : pick;
            case "java.util.Comparator" -> Comparator.nullsFirst(Comparator.comparing(Object::toString));
            case "org.apache.commons.collections4.Predicate" -> (org.apache.commons.collections4.Predicate<Object>) v -> v == null || v.hashCode() % 5 != 4;
            case "org.apache.commons.collections4.Transformer" -> (org.apache.commons.collections4.Transformer<Object, Object>)
                    v -> v instanceof Integer i ? (Object) (i + 1) : v;
            case "org.apache.commons.collections4.Factory" -> (org.apache.commons.collections4.Factory<Object>) () -> 7;
            case "java.lang.Object" -> r.nextInt(12) == 0 ? null : key(r);
            default -> r.nextInt(12) == 0 ? null : key(r);
        };
    }

    private static Object norm(Object v) {
        if (v == null || v instanceof Number || v instanceof String || v instanceof Boolean) {
            return String.valueOf(v);
        }
        if (v instanceof Throwable t) {
            return "EX:" + t.getClass().getName();
        }
        if (v instanceof Object[] a) {
            return "A" + Arrays.stream(a).map(M3DecoratorDifferential::norm).toList();
        }
        if (v instanceof MapIterator<?, ?> mi) {
            return "MI" + drainMap(mi) + attempt(mi::remove);
        }
        if (v instanceof ListIterator<?> li) {
            return "LI" + drain(li) + attempt(() -> li.add(null)) + attempt(() -> li.set(null)) + attempt(li::remove);
        }
        if (v instanceof Iterator<?> it) {
            return "I" + drain(it) + attempt(it::remove);
        }
        return structural(v);
    }

    @SuppressWarnings("unchecked")
    private static Object structural(Object v) {
        if (v instanceof Spliterator<?> s) {
            List<Object> out = new ArrayList<>();
            s.forEachRemaining(out::add);
            return "S" + out;
        }
        if (v instanceof Stream<?> s) {
            return "T" + s.toList();
        }
        if (v instanceof Map.Entry<?, ?> e) {
            return "E" + e.getKey() + "=" + e.getValue() + attempt(() -> ((Map.Entry<Object, Object>) e).setValue(e.getValue()));
        }
        if (v instanceof Collection<?> c) {
            return "C" + c.size() + c + attempt(() -> c.add(null)) + attempt(() -> c.removeIf(e -> false)) + attempt(() -> c.iterator().remove());
        }
        if (v instanceof Map<?, ?> m) {
            return "M" + m.size() + m + attempt(() -> ((Map<Object, Object>) m).putAll(Map.of())) + attempt(() -> m.remove(ABSENT));
        }
        if (v instanceof MultiValuedMap<?, ?> mv) {
            return "V" + mv.size() + mv + attempt(() -> mv.remove(ABSENT));
        }
        if (v instanceof Comparator<?> c) {
            Comparator<Object> cmp = (Comparator<Object>) c;
            return "K" + Integer.signum(cmp.compare(1, 2));
        }
        return "OBJ";
    }

    private static List<Object> drain(Iterator<?> it) {
        List<Object> out = new ArrayList<>();
        for (int i = 0; i < 64 && it.hasNext(); i++) {
            out.add(norm(it.next()));
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> drainMap(MapIterator<?, ?> mi) {
        List<Object> out = new ArrayList<>();
        for (int i = 0; i < 64 && mi.hasNext(); i++) {
            Object k = mi.next();
            out.add(k + "=" + mi.getValue() + attempt(() -> ((MapIterator<Object, Object>) mi).setValue(mi.getValue())));
        }
        return out;
    }

    private static String attempt(Runnable r) {
        try {
            r.run();
            return "/ok";
        } catch (RuntimeException e) {
            return "/" + e.getClass().getSimpleName();
        }
    }

    private void compare(String where, Object oracle, Object m3) {
        checks++;
        if (!String.valueOf(oracle).equals(String.valueOf(m3))) {
            mismatches.add(where + "\toracle=" + oracle + "\tm3=" + m3);
        }
    }
}
