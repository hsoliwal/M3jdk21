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
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Pattern DECORATOR_DIFFERENTIAL lock probe (SYNC family): for every public method of the M3 class, and of every Set
 * view it returns, the call runs on a worker thread while the main thread holds the monitor of the decorator. The
 * verdict is BLOCKS (worker BLOCKED on exactly that monitor, by ThreadMXBean lock identity) or FREE (worker finished
 * while the monitor was held), plus the result class. Oracle and M3 verdicts must be equal. Extra probes: equals(self),
 * view methods under the PARENT monitor (lock sharing), and the monitor of a deserialized copy (lock == this survives).
 * Usage: {@code java -cp commons.jar;classes M3SyncProbe <m3 fqn> <commons fqn> <backing> <factory> <out.tsv>}; exit 0/4.
 */
public final class M3SyncProbe {
    private static final Logger LOG = Logger.getLogger(M3SyncProbe.class.getName());
    private static final Map<String, Supplier<Collection<Object>>> BACKINGS = Map.of(
            "list", ArrayList::new, "deque", java.util.LinkedList::new,
            "hashbag", org.apache.commons.collections4.bag.HashBag::new, "treebag", org.apache.commons.collections4.bag.TreeBag::new,
            "hashmultiset", org.apache.commons.collections4.multiset.HashMultiSet::new);
    private final List<String> mismatches = new ArrayList<>();
    private int checks;
    private final List<String> verdicts = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        M3SyncProbe p = new M3SyncProbe();
        Class<?> m3 = Class.forName(args[0]);
        Method of = Arrays.stream(Class.forName(args[1]).getMethods()).filter(m -> m.getName().equals(args[3])).findFirst().orElseThrow();
        Method xf = m3.getMethod(args[3], of.getParameterTypes());
        List<Method> methods = Arrays.stream(m3.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()) && !Modifier.isStatic(m.getModifiers()) && !m.isSynthetic())
                .sorted(Comparator.comparing(Method::toGenericString)).toList();
        for (Method m : methods) {
            Object o = of.invoke(null, filled(args[2]));
            Object x = xf.invoke(null, filled(args[2]));
            Method om = o.getClass().getMethod(m.getName(), m.getParameterTypes());
            p.compare(m.toGenericString(), probe(o, o, om, args(m)), probe(x, x, m, args(m)));
            if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == Object.class) {
                p.compare(m.getName() + "(self)", probe(o, o, om, new Object[] {o}), probe(x, x, m, new Object[] {x}));
            }
            if (Set.class.isAssignableFrom(m.getReturnType()) && m.getParameterCount() == 0) {
                p.views(om, m, o, x);
            }
        }
        Object o = roundTrip(of.invoke(null, filled(args[2])));
        Object x = roundTrip(xf.invoke(null, filled(args[2])));
        Method size = Collection.class.getMethod("size");
        p.compare("deserialized size() under own monitor", probe(o, o, size, new Object[0]), probe(x, x, size, new Object[0]));
        Files.write(Path.of(args[4] + ".verdicts"), p.verdicts.stream().map(r -> args[0] + "\t" + r).toList(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        Files.write(Path.of(args[4]), p.mismatches.stream().map(r -> args[0] + "\t" + r).toList(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        LOG.info(() -> "M3_SYNC_PROBE class=" + args[0] + " methods=" + methods.size() + " checks=" + p.checks + " mismatches=" + p.mismatches.size());
        System.exit(p.mismatches.isEmpty() ? 0 : 4);
    }

    private void views(Method om, Method m, Object o, Object x) throws ReflectiveOperationException {
        Object ov = om.invoke(o);
        Object xv = m.invoke(x);
        compare(m.getName() + "() view serial", String.valueOf(cls(roundTrip(ov))), String.valueOf(cls(roundTrip(xv))));
        for (Method vm : Set.class.getMethods()) {
            if (!Modifier.isStatic(vm.getModifiers())) {
                compare(m.getName() + "()." + vm.getName() + Arrays.toString(vm.getParameterTypes()) + " under parent monitor",
                        probe(o, ov, vm, args(vm)), probe(x, xv, vm, args(vm)));
            }
        }
        Method eq = Object.class.getMethod("equals", Object.class);
        compare(m.getName() + "().equals(self) under parent monitor", probe(o, ov, eq, new Object[] {ov}), probe(x, xv, eq, new Object[] {xv}));
    }

    private static Collection<Object> filled(String kind) {
        Collection<Object> c = BACKINGS.get(kind).get();
        List.of(3, 1, 4, 1, 5).forEach(c::add);
        return c;
    }

    private static Object[] args(Method m) {
        return Arrays.stream(m.getParameterTypes()).map(t -> t == int.class ? (Object) 1 : t.isArray() ? new Integer[0]
                : Collection.class.isAssignableFrom(t) ? new ArrayList<>(List.of(1, 9))
                : t == java.util.function.Predicate.class ? (java.util.function.Predicate<Object>) v -> false
                : t == java.util.function.Consumer.class ? (java.util.function.Consumer<Object>) v -> { }
                : t == java.util.function.IntFunction.class ? (java.util.function.IntFunction<Object[]>) Integer[]::new : (Object) 9).toArray();
    }

    /** Runs target.m(args) on a worker while this thread holds monitor; BLOCKS iff the worker blocks on that monitor. */
    private static String probe(Object monitor, Object target, Method m, Object[] args) {
        AtomicReference<Object> result = new AtomicReference<>();
        Thread t = new Thread(() -> result.set(call(m, target, args)));
        String verdict;
        synchronized (monitor) {
            t.start();
            long deadline = System.nanoTime() + 10_000_000_000L;
            while (true) {
                Thread.State s = t.getState();
                if (s == Thread.State.TERMINATED) {
                    verdict = "FREE";
                    break;
                }
                if (s == Thread.State.BLOCKED && blockedOn(t, monitor)) {
                    verdict = "BLOCKS";
                    break;
                }
                if (System.nanoTime() > deadline) {
                    verdict = "STUCK:" + s;
                    break;
                }
                Thread.onSpinWait();
            }
        }
        try {
            t.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return verdict + "/" + cls(result.get());
    }

    private static boolean blockedOn(Thread t, Object monitor) {
        ThreadInfo info = ManagementFactory.getThreadMXBean().getThreadInfo(t.threadId());
        return info != null && info.getLockInfo() != null && info.getLockInfo().getIdentityHashCode() == System.identityHashCode(monitor);
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

    private static String cls(Object v) {
        return v instanceof Throwable e ? "EX:" + e.getClass().getName() : v == null ? "null"
                : v instanceof Number || v instanceof Boolean ? String.valueOf(v) : "ok";
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

    private void compare(String where, String oracle, String m3) {
        checks++;
        verdicts.add(where + "\t" + oracle + "\t" + m3);
        if (!oracle.equals(m3)) {
            mismatches.add(where + "\toracle=" + oracle + "\tm3=" + m3);
        }
    }
}
