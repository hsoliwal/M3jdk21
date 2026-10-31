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
import com.m3.collections.spring.M3MultiValueMap;
import com.m3.collections.spring.M3MultiValueMapAdapter;
import com.m3.collections.spring.M3SingleValueMapView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.MultiValueMapAdapter;

/**
 * Evidence for widening M3SingleValueMapView's target to any {@code Map<K, List<V>>}. For four target kinds the
 * widened view runs the same seeded operations as Spring 6.2's own first-value view over the provider
 * counterpart; outcomes and target text are compared with and without the classes of the target's lists.
 * Output goes through JUL.
 */
public final class SingleValueViewWideningDiscriminator {
    private static final Logger LOG = logger();
    private static final String[] OPS = {"get", "put", "remove", "containsKey", "containsValue", "putAll", "values",
        "entrySet", "forEach", "equals", "hashCode", "targetAdd", "targetEmptyList", "targetNullList", "clear"};

    private SingleValueViewWideningDiscriminator() {
    }

    /** A target kind: the M3-side target, the provider-side target and the provider's first-value view of it. */
    private record Kind(String name, Supplier<Map<Integer, List<Integer>>> m3, Supplier<Map<Integer, List<Integer>>> spring) {
    }

    public static void main(String[] args) {
        List<Kind> kinds = List.of(
                new Kind("M3MultiValueMap", M3MultiValueMap::new, LinkedMultiValueMap::new),
                new Kind("M3MultiValueMapAdapter", () -> new M3MultiValueMapAdapter<>(new LinkedHashMap<>()),
                        () -> new MultiValueMapAdapter<>(new LinkedHashMap<>())),
                new Kind("LinkedMultiValueMap", LinkedMultiValueMap::new, LinkedMultiValueMap::new),
                new Kind("LinkedHashMap<K,List<V>>", LinkedHashMap::new, LinkedHashMap::new));
        for (Kind kind : kinds) {
            Map<String, int[]> rows = new TreeMap<>();
            for (int seed = 0; seed < 8; seed++) {
                Random random = new Random(0x51E7_0000L + seed);
                Map<Integer, List<Integer>> m3Target = kind.m3().get();
                Map<Integer, List<Integer>> springTarget = kind.spring().get();
                Map<Integer, Integer> m3View = new M3SingleValueMapView<>(m3Target);
                Map<Integer, Integer> springView = springView(springTarget);
                for (int step = 0; step < 2_000; step++) {
                    String op = OPS[random.nextInt(OPS.length - (random.nextInt(50) == 0 ? 0 : 1))];
                    long opSeed = random.nextLong();
                    String a = outcome(op, m3View, m3Target, new Random(opSeed));
                    String b = outcome(op, springView, springTarget, new Random(opSeed));
                    int[] row = rows.computeIfAbsent(op, k -> new int[3]);
                    row[0]++;
                    if (strip(a).equals(strip(b))) { row[1]++; }
                    if (a.equals(b)) { row[2]++; }
                    if (!strip(a).equals(strip(b))) {
                        LOG.info(kind.name() + " " + op + " protocol difference: m3=" + a + " spring=" + b);
                        resync(m3Target, springTarget);
                    } else if (!a.equals(b)) {
                        resync(m3Target, springTarget);
                    }
                }
            }
            int ops = 0;
            int protocol = 0;
            int exact = 0;
            for (Map.Entry<String, int[]> row : rows.entrySet()) {
                ops += row.getValue()[0];
                protocol += row.getValue()[1];
                exact += row.getValue()[2];
                LOG.info(kind.name() + "\t" + row.getKey() + "\t" + row.getValue()[0] + "\t" + row.getValue()[1] + "\t" + row.getValue()[2]);
            }
            LOG.info(kind.name() + "\tTOTAL\t" + ops + "\t" + protocol + "\t" + exact);
        }
    }

    private static Map<Integer, Integer> springView(Map<Integer, List<Integer>> target) {
        if (target instanceof MultiValueMap<Integer, Integer> multi) { return multi.asSingleValueMap(); }
        return new MultiValueMapAdapter<>(target).asSingleValueMap();
    }

    /** Copies the provider target's mappings into the M3 target (fresh lists) so one difference counts once. */
    private static void resync(Map<Integer, List<Integer>> m3Target, Map<Integer, List<Integer>> springTarget) {
        m3Target.clear();
        for (Map.Entry<Integer, List<Integer>> e : springTarget.entrySet()) {
            springTarget.put(e.getKey(), e.getValue() == null ? null : new ArrayList<>(e.getValue()));
        }
        for (Map.Entry<Integer, List<Integer>> e : springTarget.entrySet()) {
            m3Target.put(e.getKey(), e.getValue() == null ? null : new ArrayList<>(e.getValue()));
        }
    }

    private static String outcome(String op, Map<Integer, Integer> view, Map<Integer, List<Integer>> target, Random r) {
        Integer k = r.nextInt(6);
        Integer v = r.nextInt(7) == 0 ? null : r.nextInt(6);
        Object value;
        try {
            value = apply(op, view, target, k, v);
        } catch (RuntimeException e) {
            value = "throws " + e.getClass().getSimpleName();
        }
        return value + " | " + target + " | " + classes(target);
    }

    private static Object apply(String op, Map<Integer, Integer> view, Map<Integer, List<Integer>> target, Integer k, Integer v) {
        switch (op) {
            case "get": return view.get(k);
            case "put": return view.put(k, v);
            case "remove": return view.remove(k);
            case "containsKey": return view.containsKey(k);
            case "containsValue": return view.containsValue(v);
            case "putAll": view.putAll(Map.of(k, v == null ? 0 : v)); return view.size();
            case "values": return new ArrayList<>(view.values());
            case "entrySet": return view.entrySet().toString();
            case "forEach": {
                List<String> seen = new ArrayList<>();
                view.forEach((a, b) -> seen.add(a + "=" + b));
                return seen;
            }
            case "equals": return view.equals(new HashMap<>(view)) + "/" + view.equals(Map.of(k, k));
            case "hashCode": return view.hashCode() + "/" + view.size() + "/" + view.isEmpty() + "/" + view;
            case "targetAdd": {
                List<Integer> list = target.get(k);
                if (list == null || list.getClass().getName().contains("Single")) {
                    list = new ArrayList<>(list == null ? List.of() : list);
                    target.put(k, list);
                }
                list.add(v);
                return list.size();
            }
            case "targetEmptyList": target.put(k, new ArrayList<>()); return view.get(k);
            case "targetNullList": target.put(k, null); return view.get(k);
            case "clear": view.clear(); return view.size();
            default: throw new AssertionError(op);
        }
    }

    /** The class of every value list in the target; only the installed list class may differ. */
    private static String classes(Map<Integer, List<Integer>> target) {
        List<String> names = new ArrayList<>();
        for (List<Integer> list : target.values()) { names.add(list == null ? "null" : list.getClass().getSimpleName()); }
        return Arrays.toString(names.toArray());
    }

    /** The outcome without the list classes. */
    private static String strip(String outcome) {
        return outcome.substring(0, outcome.lastIndexOf(" | "));
    }

    private static Logger logger() {
        Logger log = Logger.getLogger(SingleValueViewWideningDiscriminator.class.getName());
        log.setUseParentHandlers(false);
        ConsoleHandler console = new ConsoleHandler();
        console.setFormatter(new Formatter() {
            @Override public String format(LogRecord record) {
                return record.getMessage() + System.lineSeparator();
            }
        });
        log.addHandler(console);
        return log;
    }
}
