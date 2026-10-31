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
/*
 * Pattern DECORATOR_DIFFERENTIAL lock-scope trace probe (SYNC family; written by the adversarial verify pass that refuted
 * the BLOCKS/FREE probe as blind to WHERE the backing call runs). The backing is a recording java.lang.reflect.Proxy over the
 * factory parameter interface; every call that reaches the backing (or a Set/Collection the backing returned) records
 * method name + Thread.holdsLock(decorator). Oracle and M3 traces must be equal per public method and per view method.
 * Usage: java -cp commons.jar;classes;. M3LockTraceProbe <m3 fqn> <commons fqn> <backing> <factory> <out.tsv>; exit 0/4.
 */
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Logger;

public final class M3LockTraceProbe {
    private static final Logger LOG = Logger.getLogger(M3LockTraceProbe.class.getName());
    private static final Map<String, Supplier<Collection<Object>>> BACKINGS = Map.of(
            "list", ArrayList::new, "deque", java.util.LinkedList::new,
            "hashbag", org.apache.commons.collections4.bag.HashBag::new, "treebag", org.apache.commons.collections4.bag.TreeBag::new,
            "hashmultiset", org.apache.commons.collections4.multiset.HashMultiSet::new);

    /** One side: recording backing plus the decorator that owns the monitor. */
    static final class Side {
        final List<String> trace = new ArrayList<>();
        Object owner;

        Object record(Object target, Class<?> iface) {
            return Proxy.newProxyInstance(M3LockTraceProbe.class.getClassLoader(), new Class<?>[] {iface}, (p, m, a) -> {
                trace.add(m.getName() + ":" + (owner != null && Thread.holdsLock(owner) ? "L" : "-"));
                Object r;
                try {
                    r = m.invoke(target, a);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
                return r instanceof Set<?> s && m.getReturnType() == Set.class ? record(s, Set.class) : r;
            });
        }
    }

    public static void main(String[] args) throws Exception {
        Class<?> m3 = Class.forName(args[0]);
        Method of = Arrays.stream(Class.forName(args[1]).getMethods()).filter(m -> m.getName().equals(args[3])).findFirst().orElseThrow();
        Method xf = m3.getMethod(args[3], of.getParameterTypes());
        Class<?> iface = of.getParameterTypes()[0];
        List<Method> methods = Arrays.stream(m3.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()) && !Modifier.isStatic(m.getModifiers()) && !m.isSynthetic())
                .sorted(Comparator.comparing(Method::toGenericString)).toList();
        List<String> mismatches = new ArrayList<>();
        int checks = 0;
        for (Method m : methods) {
            Side so = new Side();
            Side sx = new Side();
            Object o = of.invoke(null, so.record(filled(args[2]), iface));
            Object x = xf.invoke(null, sx.record(filled(args[2]), iface));
            so.owner = o;
            sx.owner = x;
            Method om = o.getClass().getMethod(m.getName(), m.getParameterTypes());
            Object ov = call(om, o, argsFor(m));
            Object xv = call(m, x, argsFor(m));
            checks++;
            if (!so.trace.equals(sx.trace)) {
                mismatches.add(m.toGenericString() + "\toracle=" + so.trace + "\tm3=" + sx.trace);
            }
            if (Set.class.isAssignableFrom(m.getReturnType()) && m.getParameterCount() == 0 && ov instanceof Set && xv instanceof Set) {
                for (Method vm : Set.class.getMethods()) {
                    if (Modifier.isStatic(vm.getModifiers())) {
                        continue;
                    }
                    so.trace.clear();
                    sx.trace.clear();
                    call(vm, ov, argsFor(vm));
                    call(vm, xv, argsFor(vm));
                    checks++;
                    if (!so.trace.equals(sx.trace)) {
                        mismatches.add(m.getName() + "()." + vm.getName() + Arrays.toString(vm.getParameterTypes()) + "\toracle=" + so.trace
                                + "\tm3=" + sx.trace);
                    }
                }
            }
        }
        Files.write(Path.of(args[4]), mismatches.stream().map(r -> args[0] + "\t" + r).toList(), StandardCharsets.UTF_8);
        final int n = checks;
        LOG.info(() -> "M3_LOCK_TRACE class=" + args[0] + " methods=" + methods.size() + " checks=" + n + " mismatches=" + mismatches.size());
        System.exit(mismatches.isEmpty() ? 0 : 4);
    }

    private static Collection<Object> filled(String kind) {
        Collection<Object> c = BACKINGS.get(kind).get();
        List.of(3, 1, 4, 1, 5).forEach(c::add);
        return c;
    }

    private static Object[] argsFor(Method m) {
        return Arrays.stream(m.getParameterTypes()).map(t -> t == int.class ? (Object) 1 : t.isArray() ? new Integer[0]
                : Collection.class.isAssignableFrom(t) ? new ArrayList<>(List.of(1, 9))
                : t == java.util.function.Predicate.class ? (java.util.function.Predicate<Object>) v -> false
                : t == java.util.function.Consumer.class ? (java.util.function.Consumer<Object>) v -> { }
                : t == java.util.function.IntFunction.class ? (java.util.function.IntFunction<Object[]>) Integer[]::new : (Object) 9).toArray();
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
}
