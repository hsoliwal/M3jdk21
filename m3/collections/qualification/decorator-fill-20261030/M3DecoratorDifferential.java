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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;
import java.util.Spliterator;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Pattern DECORATOR_DIFFERENTIAL v1: seeded reflective differential run of one generated owned decorator against the
 * Apache Commons Collections 4.5.0 decorator it mirrors (test oracle only). Two equal backings are wrapped, one by each
 * factory. Every step either mutates both backings directly (views must follow) or invokes one public method that the
 * M3 class declares, on both sides with equal arguments. Return values, exception classes, drained iterators and views,
 * and the backing state are compared after every step. Factory identity, null rejection and serialization are checked once.
 * Usage: {@code java -cp commons.jar;classes M3DecoratorDifferential.java <m3 fqn> <commons fqn> <backing> <out.tsv>}
 * with backing one of list, deque, treeset, hashbag, treebag. Exit 0 = no mismatch, 4 = mismatches written to out.tsv.
 */
public final class M3DecoratorDifferential {
    private static final Logger LOG = Logger.getLogger(M3DecoratorDifferential.class.getName());
    private static final long[] SEEDS = {2L, 11L, 42L, 1_009L, 20_261_030L, 0xFEEDL, -5L, 555_555_555L};
    private static final int STEPS = 4_000;
    private static final Map<String, Supplier<Collection<Integer>>> BACKINGS = Map.of(
            "list", ArrayList::new, "deque", java.util.LinkedList::new, "treeset", java.util.TreeSet::new,
            "hashbag", org.apache.commons.collections4.bag.HashBag::new, "treebag", org.apache.commons.collections4.bag.TreeBag::new);

    private final Method m3Factory;
    private final Method oracleFactory;
    private final Supplier<Collection<Integer>> backing;
    private final List<Method> methods;
    private final List<String> mismatches = new ArrayList<>();
    private long checks;

    private M3DecoratorDifferential(Class<?> m3, Class<?> oracle, Supplier<Collection<Integer>> backing) {
        this.m3Factory = factory(m3);
        this.oracleFactory = factory(oracle);
        this.backing = backing;
        this.methods = Arrays.stream(m3.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()) && !Modifier.isStatic(m.getModifiers()) && !m.isSynthetic())
                .sorted(Comparator.comparing(Method::toGenericString)).toList();
    }

    public static void main(String[] args) throws Exception {
        M3DecoratorDifferential run = new M3DecoratorDifferential(Class.forName(args[0]), Class.forName(args[1]), BACKINGS.get(args[2]));
        run.once();
        Arrays.stream(SEEDS).forEach(run::seed);
        Files.write(Path.of(args[3]), run.mismatches.stream().map(r -> args[0] + "\t" + r).toList(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        LOG.info(() -> "M3_DECORATOR_DIFF class=" + args[0] + " oracle=" + args[1] + " methods=" + run.methods.size() + " seeds="
                + SEEDS.length + " steps=" + STEPS + " checks=" + run.checks + " mismatches=" + run.mismatches.size());
        System.exit(run.mismatches.isEmpty() ? 0 : 4);
    }

    private static Method factory(Class<?> c) {
        return Arrays.stream(c.getMethods()).filter(m -> Modifier.isStatic(m.getModifiers()) && m.getParameterCount() == 1)
                .filter(m -> m.getName().startsWith("unmodifiable")).findFirst().orElseThrow();
    }

    private void once() throws Exception {
        compare("factory(null)", call(oracleFactory, null, new Object[] {null}), call(m3Factory, null, new Object[] {null}));
        Object o = oracleFactory.invoke(null, filled(new Random(7)));
        Object x = m3Factory.invoke(null, filled(new Random(7)));
        compare("factory(self) identity", oracleFactory.invoke(null, o) == o, m3Factory.invoke(null, x) == x);
        compare("serial round trip", norm(roundTrip(o)), norm(roundTrip(x)));
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

    private Collection<Integer> filled(Random r) {
        Collection<Integer> c = backing.get();
        r.ints(r.nextInt(12), 0, 10).forEach(c::add);
        return c;
    }

    private void seed(long seed) {
        Random r = new Random(seed);
        Collection<Integer> b1 = filled(new Random(seed));
        Collection<Integer> b2 = filled(new Random(seed));
        try {
            Object o = oracleFactory.invoke(null, b1);
            Object x = m3Factory.invoke(null, b2);
            for (int step = 0; step < STEPS && mismatches.size() < 50; step++) {
                step(r, b1, b2, o, x, seed + "@" + step);
            }
        } catch (ReflectiveOperationException e) {
            mismatches.add("seed " + seed + "\tfactory failed\t" + e);
        }
    }

    private void step(Random r, Collection<Integer> b1, Collection<Integer> b2, Object o, Object x, String where) {
        if (r.nextInt(5) == 0) {
            int v = r.nextInt(10);
            boolean add = r.nextBoolean();
            compare(where + " backing " + (add ? "add " : "remove ") + v, add ? b1.add(v) : b1.remove(v), add ? b2.add(v) : b2.remove(v));
        } else {
            Method m = methods.get(r.nextInt(methods.size()));
            long argSeed = r.nextLong();
            compare(where + " " + m.getName() + Arrays.toString(m.getParameterTypes()), invokeOracle(o, m, argSeed),
                    norm(call(m, x, args(m.getParameterTypes(), new Random(argSeed)))));
        }
        compare(where + " backing state", b1.toString(), b2.toString());
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

    private static Object[] args(Class<?>[] types, Random r) {
        return Arrays.stream(types).map(t -> arg(t, r)).toArray();
    }

    private static Object arg(Class<?> t, Random r) {
        if (t == int.class || t == Integer.class) {
            return r.nextInt(8) - 1;
        }
        if (t == boolean.class) {
            return r.nextBoolean();
        }
        if (t.isArray()) {
            return new Integer[r.nextInt(6)];
        }
        if (Collection.class.isAssignableFrom(t) || t == Iterable.class) {
            return new ArrayList<>(r.ints(r.nextInt(4), 0, 10).boxed().toList());
        }
        return functional(t, r);
    }

    private static Object functional(Class<?> t, Random r) {
        int pick = r.nextInt(10);
        return switch (t.getName()) {
            case "java.util.function.Predicate" -> (java.util.function.Predicate<Object>) v -> v instanceof Integer i && (i + pick) % 3 == 0;
            case "java.util.function.Consumer" -> (java.util.function.Consumer<Object>) v -> { };
            case "java.util.function.IntFunction" -> (java.util.function.IntFunction<Integer[]>) Integer[]::new;
            case "java.util.function.UnaryOperator" -> (java.util.function.UnaryOperator<Object>) v -> v;
            case "java.util.Comparator" -> Comparator.nullsFirst(Comparator.<Integer>naturalOrder());
            default -> r.nextInt(12) == 0 ? null : (Object) r.nextInt(10);
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
        if (v instanceof ListIterator<?> li) {
            return "LI" + drain(li) + attempt(() -> li.add(null)) + attempt(() -> li.set(null)) + attempt(li::remove);
        }
        if (v instanceof Iterator<?> it) {
            return "I" + drain(it) + attempt(it::remove);
        }
        return structural(v);
    }

    private static Object structural(Object v) {
        if (v instanceof Spliterator<?> s) {
            List<Object> out = new ArrayList<>();
            s.forEachRemaining(out::add);
            return "S" + out;
        }
        if (v instanceof Stream<?> s) {
            return "T" + s.toList();
        }
        if (v instanceof Collection<?> c) {
            return "C" + c.size() + c + attempt(() -> c.add(null)) + attempt(c::clear) + attempt(() -> c.iterator().remove());
        }
        if (v instanceof Comparator<?> c) {
            @SuppressWarnings("unchecked")
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
