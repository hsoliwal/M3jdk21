/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections.interop;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;
import jdk.m3.collections.M3LinkedList;
import jdk.m3.collections.M3TreeSet;

/**
 * Owner differential for mergeloss case 01 (qualification/mergeloss-01-15-20261106): every public static member of
 * M3LibraryCollections and M3LibraryCollectors present on the classpath is invoked by reflection with synthesized
 * arguments and driven by a seeded operation stream; one digest line per (member, round). Run once on master classes
 * and once on restored classes: common lines must be identical, extra lines must be restored members only.
 *
 * <p>usage: MergelossInteropDifferential ROUNDS SEED (the m3_arrays JNI library via -Dm3.arrays.native.path)</p>
 */
public final class MergelossInteropDifferential {
    private MergelossInteropDifferential() { }

    public static void main(String[] args) throws Exception {
        int rounds = Integer.parseInt(args[0]);
        long seed = Long.decode(args[1]);
        long lines = 0;
        for (Class<?> owner : List.of(M3LibraryCollections.class, M3LibraryCollectors.class)) {
            Method[] members = Arrays.stream(owner.getDeclaredMethods())
                    .filter(m -> Modifier.isPublic(m.getModifiers()) && Modifier.isStatic(m.getModifiers()))
                    .sorted(Comparator.comparing(Method::toGenericString)).toArray(Method[]::new);
            for (Method member : members) {
                for (int round = 0; round < rounds; round++) {
                    long mixed = seed ^ (round * 0x9E3779B97F4A7C15L) ^ member.toGenericString().hashCode();
                    String trace = drive(member, new Random(mixed));
                    System.out.println(owner.getSimpleName() + "." + member.getName()
                            + Arrays.toString(member.getParameterTypes()).replace(' ', '_') + " r=" + round
                            + " " + digest(trace) + " len=" + trace.length());
                    lines++;
                }
            }
        }
        System.out.println("DIFFERENTIAL_LINES " + lines);
    }

    private static String drive(Method member, Random random) {
        StringBuilder trace = new StringBuilder();
        try {
            Object[] arguments = arguments(member, random);
            if (arguments == null) return "UNSUPPORTED " + Arrays.toString(member.getParameterTypes());
            Object result = member.invoke(null, arguments);
            if (result instanceof Collector<?, ?, ?> collector) {
                List<Integer> data = new ArrayList<>();
                for (int i = 0, n = 50 + random.nextInt(400); i < n; i++) data.add(random.nextInt(60));
                trace.append("seq=").append(collect(collector, data, false));
                trace.append(" par=").append(collect(collector, data, true));
            } else {
                operate(result, random, trace);
            }
        } catch (InvocationTargetException failure) {
            trace.append("THROW ").append(failure.getCause().getClass().getName());
        } catch (ReflectiveOperationException | RuntimeException failure) {
            trace.append("THROW ").append(failure.getClass().getName());
        }
        return trace.toString();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String collect(Collector collector, List<Integer> data, boolean parallel) {
        try {
            Object result = (parallel ? data.parallelStream() : data.stream()).collect(collector);
            return describe(result);
        } catch (RuntimeException failure) {
            return "THROW " + failure.getClass().getName();
        }
    }

    private static Object[] arguments(Method member, Random random) {
        Class<?>[] types = member.getParameterTypes();
        Object[] values = new Object[types.length];
        int functions = 0;
        for (int i = 0; i < types.length; i++) {
            Class<?> type = types[i];
            if (type == Comparator.class) values[i] = Comparator.naturalOrder();
            else if (type == Function.class) {
                values[i] = functions++ == 0 ? (Function<Integer, Object>) x -> x % 7 : (Function<Integer, Object>) x -> "v" + x;
            } else if (type == BinaryOperator.class) values[i] = (BinaryOperator<Object>) (p, q) -> p + "|" + q;
            else if (type == org.springframework.util.AutoPopulatingList.ElementFactory.class) {
                values[i] = (org.springframework.util.AutoPopulatingList.ElementFactory<Object>) index -> "auto" + index;
            } else if (type == M3LinkedList.class) {
                M3LinkedList<Integer> list = new M3LinkedList<>();
                for (int k = 0; k < 20; k++) list.add(random.nextInt(40));
                values[i] = list;
            } else if (type == M3TreeSet.class) {
                M3TreeSet<Integer> set = new M3TreeSet<>();
                for (int k = 0; k < 20; k++) set.add(random.nextInt(40));
                values[i] = set;
            } else if (type == com.google.common.base.Predicate.class) {
                values[i] = (com.google.common.base.Predicate<Integer>) x -> x % 2 == 0;
            } else if (type == Locale.class) values[i] = Locale.ROOT;
            else return null;
        }
        return values;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void operate(Object target, Random random, StringBuilder trace) {
        for (int op = 0; op < 120; op++) {
            int element = random.nextInt(30);
            String key = (random.nextBoolean() ? "k" : "K") + random.nextInt(9);
            int kind = random.nextInt(4);
            Object answer;
            try {
                if (target instanceof com.google.common.collect.Multiset multiset) {
                    answer = switch (kind) {
                        case 0 -> multiset.add(element, random.nextInt(3));
                        case 1 -> multiset.remove(element, random.nextInt(3));
                        case 2 -> multiset.setCount(element, random.nextInt(4));
                        default -> multiset.count(element);
                    };
                } else if (target instanceof org.apache.commons.collections4.MultiSet multiset) {
                    answer = switch (kind) {
                        case 0 -> multiset.add(element, random.nextInt(3));
                        case 1 -> multiset.remove(element, random.nextInt(3));
                        case 2 -> multiset.setCount(element, random.nextInt(4));
                        default -> multiset.getCount(element);
                    };
                } else if (target instanceof org.apache.commons.collections4.Bag bag) {
                    answer = switch (kind) {
                        case 0 -> bag.add(element, 1 + random.nextInt(3));
                        case 1 -> bag.remove(element, random.nextInt(3));
                        case 2 -> bag.remove(element);
                        default -> bag.getCount(element);
                    };
                } else if (target instanceof org.eclipse.collections.api.bag.MutableBag bag) {
                    answer = switch (kind) {
                        case 0 -> bag.addOccurrences(element, random.nextInt(3));
                        case 1 -> bag.removeOccurrences(element, random.nextInt(3));
                        case 2 -> bag.setOccurrences(element, random.nextInt(4));
                        default -> bag.occurrencesOf(element);
                    };
                } else if (target instanceof com.google.common.collect.Multimap multimap) {
                    answer = switch (kind) {
                        case 0, 1 -> multimap.put(key, element);
                        case 2 -> multimap.remove(key, element);
                        default -> describe(multimap.get(key));
                    };
                } else if (target instanceof org.apache.commons.collections4.MultiValuedMap multimap) {
                    answer = switch (kind) {
                        case 0, 1 -> multimap.put(key, element);
                        case 2 -> multimap.removeMapping(key, element);
                        default -> describe(multimap.get(key));
                    };
                } else if (target instanceof com.google.common.collect.Table table) {
                    answer = kind < 3 ? table.put(key, element % 4, element) : table.remove(key, element % 4);
                } else if (target instanceof org.springframework.util.MultiValueMap map) {
                    if (kind < 2) { map.add(key, element); answer = "add"; }
                    else answer = kind == 2 ? describe(map.remove(key)) : describe(map.get(key));
                } else if (target instanceof Map map) {
                    answer = switch (kind) {
                        case 0, 1 -> map.put(key, element);
                        case 2 -> map.remove(key);
                        default -> map.get(key);
                    };
                } else if (target instanceof Collection collection) {
                    answer = switch (kind) {
                        case 0, 1 -> collection.add(element);
                        case 2 -> collection.remove(element);
                        default -> collection.contains(element);
                    };
                } else {
                    trace.append("OPAQUE ").append(target.getClass().getName());
                    return;
                }
            } catch (RuntimeException failure) {
                answer = "THROW " + failure.getClass().getName();
            }
            trace.append(op).append(':').append(describe(answer)).append(';');
        }
        trace.append(" final=").append(describe(target));
    }

    @SuppressWarnings("rawtypes")
    private static String describe(Object value) {
        if (value == null) return "null";
        StringBuilder out = new StringBuilder();
        if (value instanceof com.google.common.collect.Multiset multiset) {
            out.append("ms").append(multiset.size()).append('{');
            for (Object entry : multiset.entrySet()) {
                com.google.common.collect.Multiset.Entry e = (com.google.common.collect.Multiset.Entry) entry;
                out.append(e.getElement()).append('x').append(e.getCount()).append(',');
            }
            return out.append('}').toString();
        }
        if (value instanceof org.apache.commons.collections4.MultiValuedMap map) return "mv" + describe(map.asMap());
        if (value instanceof com.google.common.collect.Multimap map) return "mm" + describe(map.asMap());
        if (value instanceof com.google.common.collect.Table table) return "tb" + describe(table.rowMap());
        if (value instanceof Map map) {
            out.append('m').append(map.size()).append('{');
            for (Object entry : map.entrySet()) {
                Map.Entry e = (Map.Entry) entry;
                out.append(describe(e.getKey())).append('=').append(describe(e.getValue())).append(',');
            }
            return out.append('}').toString();
        }
        if (value instanceof Iterable iterable) {
            out.append('c').append('[');
            for (Object element : iterable) out.append(describe(element)).append(',');
            return out.append(']').toString();
        }
        return String.valueOf(value);
    }

    private static String digest(String trace) throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(trace.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash, 0, 8);
    }
}
