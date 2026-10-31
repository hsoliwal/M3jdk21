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
import com.m3.collections.apache.M3BidiMap;
import com.m3.collections.apache.M3DualLinkedHashBidiMap;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.management.ManagementFactory;
import java.lang.ref.Reference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.collections4.bidimap.DualLinkedHashBidiMap;

/**
 * Owner-convergence evidence for the two owned Commons BidiMap bodies: master's M3BidiMap (two M3LinkedHashMap
 * owners, recorded as the DualHashBidiMap row) and PR #1468's M3DualLinkedHashBidiMap (two slot tables). Every
 * seeded operation runs on Commons DualLinkedHashBidiMap (oracle), on both owners and on Commons
 * DualHashBidiMap; the outcome and both directions' text are compared per operation kind. A second pass
 * measures allocation per steady-state operation and retained heap per mapping. Output goes through JUL.
 */
public final class BidiOwnerConvergenceDiscriminator {
    private static final Logger LOG = logger();
    private static final String[] KINDS = {"put", "inversePut", "remove", "removeValue", "keySetRemove", "valuesRemove",
        "entryRemove", "keyIteratorRemove", "entrySetValue", "mapIteratorSetValue", "putAll", "valuesContainsAll",
        "rawArrayEntrySetValue", "staleValuesIteratorRemove", "entryAfterRemoval", "serializationRoundTrip"};
    private static final int SEEDS = 8;
    private static final int STEPS = 4_000;

    private BidiOwnerConvergenceDiscriminator() {
    }

    public static void main(String[] args) throws Exception {
        List<Supplier<BidiMap<Integer, Integer>>> owners = List.of(DualLinkedHashBidiMap::new, M3BidiMap::new,
                M3DualLinkedHashBidiMap::new, DualHashBidiMap::new);
        String[] names = {"commons-linked", "m3-bidimap", "m3-dual-linked", "commons-hash"};
        Map<String, int[]> agree = new TreeMap<>();
        Map<String, int[]> total = new TreeMap<>();
        for (int seed = 0; seed < SEEDS; seed++) {
            Random random = new Random(0x5EED_0000L + seed);
            List<BidiMap<Integer, Integer>> maps = new ArrayList<>();
            for (Supplier<BidiMap<Integer, Integer>> owner : owners) { maps.add(owner.get()); }
            for (int step = 0; step < STEPS; step++) {
                String kind = KINDS[random.nextInt(KINDS.length)];
                long opSeed = random.nextLong();
                String[] seen = new String[maps.size()];
                for (int i = 0; i < maps.size(); i++) {
                    seen[i] = outcome(kind, maps.get(i), new Random(opSeed));
                }
                int[] a = agree.computeIfAbsent(kind, k -> new int[names.length]);
                total.computeIfAbsent(kind, k -> new int[1])[0]++;
                for (int i = 0; i < maps.size(); i++) { if (seen[i].equals(seen[0])) { a[i]++; } }
                boolean hashContent = maps.get(3).equals(maps.get(0)) && maps.get(3).inverseBidiMap().equals(maps.get(0).inverseBidiMap());
                // The two provider quirks can leave the directions out of step; later operations on such a state
                // are outside every contract, so every map restarts from the oracle's forward mappings.
                if (!hashContent || !mirrored(maps.get(0)) ||!state(maps.get(1)).equals(state(maps.get(0))) || !state(maps.get(2)).equals(state(maps.get(0)))) {
                    // Re-synchronise every map from the oracle so one divergence is counted once, not forever.
                    Map<Integer, Integer> snapshot = new LinkedHashMap<>(maps.get(0));
                    for (int i = 1; i < maps.size(); i++) { maps.set(i, owners.get(i).get()); maps.get(i).putAll(snapshot); }
                    maps.set(0, owners.get(0).get());
                    maps.get(0).putAll(snapshot);
                }
            }
        }
        LOG.info("kind | ops | " + String.join(" | ", names) + " (operations whose outcome and state text equal commons-linked)");
        for (Map.Entry<String, int[]> row : agree.entrySet()) {
            int n = total.get(row.getKey())[0];
            StringBuilder line = new StringBuilder(row.getKey()).append(" | ").append(n);
            for (int value : row.getValue()) { line.append(" | ").append(value); }
            LOG.info(line.toString());
        }
        footprint("commons-linked", DualLinkedHashBidiMap::new);
        footprint("m3-bidimap", M3BidiMap::new);
        footprint("m3-dual-linked", M3DualLinkedHashBidiMap::new);
    }

    /** One operation; the result is the operation's value or exception plus both directions' text afterwards. */
    private static String outcome(String kind, BidiMap<Integer, Integer> m, Random r) {
        Integer k = key(r);
        Integer v = key(r);
        Object value;
        try {
            value = apply(kind, m, r, k, v);
        } catch (RuntimeException | Error e) {
            value = "throws " + e.getClass().getSimpleName();
        }
        return value + " " + state(m);
    }

    private static Object apply(String kind, BidiMap<Integer, Integer> m, Random r, Integer k, Integer v) {
        switch (kind) {
            case "put": return m.put(k, v);
            case "inversePut": return m.inverseBidiMap().put(v, k);
            case "remove": return m.remove(k);
            case "removeValue": return m.removeValue(v);
            case "keySetRemove": return m.keySet().remove(k);
            case "valuesRemove": return m.values().remove(v);
            case "entryRemove": return m.entrySet().remove(Map.entry(k, v));
            case "putAll": m.putAll(Map.of(k, v)); return null;
            case "valuesContainsAll": return m.values().containsAll(List.of(v)) + "/" + m.values().contains(v);
            case "keyIteratorRemove": return removeAt(m.keySet().iterator(), r);
            case "entrySetValue": {
                Iterator<Map.Entry<Integer, Integer>> it = m.entrySet().iterator();
                return it.hasNext() ? it.next().setValue(v) : "empty";
            }
            case "mapIteratorSetValue": {
                MapIterator<Integer, Integer> it = m.mapIterator();
                if (!it.hasNext()) { return "empty"; }
                it.next();
                return it.setValue(v);
            }
            case "rawArrayEntrySetValue": {
                Object[] entries = m.entrySet().toArray();
                if (entries.length == 0) { return "empty"; }
                @SuppressWarnings("unchecked") Map.Entry<Integer, Integer> e = (Map.Entry<Integer, Integer>) entries[0];
                return e.setValue(v) + " " + e;
            }
            case "staleValuesIteratorRemove": {
                Iterator<Integer> it = m.values().iterator();
                if (!it.hasNext()) { return "empty"; }
                Integer delivered = it.next();
                m.put(m.keySet().iterator().next(), v);
                it.remove();
                return delivered;
            }
            case "entryAfterRemoval": {
                Iterator<Map.Entry<Integer, Integer>> it = m.entrySet().iterator();
                if (!it.hasNext()) { return "empty"; }
                Map.Entry<Integer, Integer> e = it.next();
                m.put(e.getKey(), v);
                m.remove(e.getKey());
                return e.getValue();
            }
            case "serializationRoundTrip": return roundTrip(m);
            default: throw new AssertionError(kind);
        }
    }

    private static Object removeAt(Iterator<Integer> it, Random r) {
        int skip = r.nextInt(3);
        Integer last = null;
        for (int i = 0; i <= skip && it.hasNext(); i++) { last = it.next(); }
        if (last == null) { return "empty"; }
        it.remove();
        return last;
    }

    private static Object roundTrip(BidiMap<Integer, Integer> m) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ObjectOutputStream out = new ObjectOutputStream(bytes)) { out.writeObject(m); }
            try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
                return "copy " + state((BidiMap<?, ?>) in.readObject());
            }
        } catch (java.io.IOException | ClassNotFoundException e) {
            return "throws " + e.getClass().getSimpleName();
        }
    }

    private static Integer key(Random r) {
        int x = r.nextInt(9);
        return x == 8 ? null : x;
    }

    private static boolean mirrored(BidiMap<Integer, Integer> m) {
        BidiMap<Integer, Integer> inverse = m.inverseBidiMap();
        if (inverse.size() != m.size()) { return false; }
        for (Map.Entry<Integer, Integer> e : new ArrayList<>(m.entrySet())) {
            if (!inverse.containsKey(e.getValue()) || !java.util.Objects.equals(inverse.get(e.getValue()), e.getKey())) { return false; }
        }
        return true;
    }

    private static String state(BidiMap<?, ?> m) {
        return m + " / " + m.inverseBidiMap();
    }

    /** Allocation per steady-state put/get/getKey and retained heap per mapping (40 copies of 10,000 mappings). */
    private static void footprint(String who, Supplier<BidiMap<Integer, Integer>> make) {
        Integer[] boxes = new Integer[20_000];
        for (int i = 0; i < boxes.length; i++) { boxes[i] = Integer.valueOf(1_000_000 + i); }
        Function<BidiMap<Integer, Integer>, BidiMap<Integer, Integer>> fill = m -> {
            for (int i = 0; i < 10_000; i++) { m.put(boxes[i], boxes[i + 10_000]); }
            return m;
        };
        BidiMap<Integer, Integer> m = fill.apply(make.get());
        com.sun.management.ThreadMXBean bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        long id = Thread.currentThread().threadId();
        long sink = 0;
        for (int round = 0; round < 2; round++) {
            long before = bean.getThreadAllocatedBytes(id);
            for (int i = 0; i < 200_000; i++) {
                int j = i % 10_000;
                m.put(boxes[j], boxes[j + 10_000]);
                sink += m.get(boxes[j]) + m.getKey(boxes[j + 10_000]);
            }
            if (round == 1) {
                double perOp = (bean.getThreadAllocatedBytes(id) - before) / 200_000.0;
                Object[] hold = new Object[40];
                long base = used();
                for (int c = 0; c < hold.length; c++) { hold[c] = fill.apply(make.get()); }
                double perMapping = (used() - base) / (40.0 * 10_000);
                Reference.reachabilityFence(hold);
                LOG.info(String.format("%s: %.1f B allocated per put+get+getKey, %.1f B retained per mapping (sink %d)",
                        who, perOp, perMapping, sink & 1));
            }
        }
    }

    private static long used() {
        for (int i = 0; i < 3; i++) { ManagementFactory.getMemoryMXBean().gc(); }
        return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
    }

    private static Logger logger() {
        Logger log = Logger.getLogger(BidiOwnerConvergenceDiscriminator.class.getName());
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
