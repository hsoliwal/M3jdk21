/* *****************************************************************************
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
package jdk.m3.collections.eclipse.impl.list.mutable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

import jdk.m3.collections.eclipse.api.list.MutableList;
import jdk.m3.collections.eclipse.impl.list.mutable.OffsetsCallbacks.Probe;
import jdk.m3.collections.eclipse.impl.list.mutable.OffsetsCallbacks.ProbeSet;
import jdk.m3.collections.eclipse.impl.list.mutable.OffsetsTwin.LanePair;
import jdk.m3.collections.eclipse.impl.list.mutable.OffsetsTwin.NamedOp;

/**
 * Seeded generators of single operations (lane growth, indexed access, bulk mutation, queries);
 * every parameter is drawn before the operation runs, so both twins get the same one.
 */
final class OffsetsOps {

    private static final int LANE_CAP = 40;
    private static final int NONE = OffsetsTwin.NONE;

    private record Gen(int weight, Function<OffsetsTwin, NamedOp> make) {
    }

    private static final List<Gen> GENS = List.of(
            new Gen(8, OffsetsOps::addAllList), new Gen(3, OffsetsOps::addAllFast),
            new Gen(2, OffsetsOps::addComposited), new Gen(1, OffsetsOps::addCompositedArray),
            new Gen(3, OffsetsOps::emptyLane), new Gen(6, OffsetsOps::addOne),
            new Gen(10, OffsetsOps::addAt), new Gen(8, OffsetsOps::get),
            new Gen(6, OffsetsOps::set), new Gen(9, OffsetsOps::removeAt),
            new Gen(3, OffsetsOps::removeObject), new Gen(1, OffsetsOps::clear),
            new Gen(2, OffsetsOps::resetSize), new Gen(2, OffsetsOps::reverse),
            new Gen(1, OffsetsOps::sort), new Gen(1, OffsetsOps::shuffle),
            new Gen(2, OffsetsOps::replaceAll), new Gen(4, OffsetsOps::retainAll),
            new Gen(3, OffsetsOps::removeAll), new Gen(2, OffsetsOps::bulkIterable),
            new Gen(3, OffsetsOps::removeIf), new Gen(3, OffsetsSessions::iterate),
            new Gen(1, OffsetsSessions::listIterate), new Gen(2, OffsetsSessions::subList),
            new Gen(3, OffsetsOps::query), new Gen(2, OffsetsOps::toArray),
            new Gen(2, OffsetsSessions::visit), new Gen(2, OffsetsSessions::batch),
            new Gen(2, OffsetsSessions::equality), new Gen(1, OffsetsSessions::serialize),
            new Gen(4, OffsetsSessions::external));
    private static final int TOTAL = GENS.stream().mapToInt(Gen::weight).sum();

    private OffsetsOps() {
    }

    static NamedOp pick(OffsetsTwin twin) {
        int roll = twin.master.nextInt(TOTAL);
        int k = 0;
        while (roll >= GENS.get(k).weight()) {
            roll -= GENS.get(k).weight();
            k++;
        }
        return GENS.get(k).make().apply(twin);
    }

    /** A build-up step: a private lane, an emptied private lane or, if allowed, a caller lane. */
    static NamedOp lane(OffsetsTwin twin) {
        int roll = twin.master.nextInt(6);
        if (twin.foreign && roll < 2) {
            return roll == 0 ? addAllFast(twin) : addComposited(twin);
        }
        return roll == 2 ? emptyLane(twin) : addAllList(twin);
    }

    static NamedOp named(String name, OffsetsTwin.TwinOp op) {
        return new NamedOp(name, op, NONE, 0);
    }

    /** Integer 0..20 (70%), a probe (20%) or null (10%); null is a real element here. */
    static Object element(OffsetsTwin twin) {
        int roll = twin.master.nextInt(10);
        if (roll == 0) {
            return null;
        }
        if (roll <= 2) {
            return new Probe(twin.probes++, twin.master.nextInt(6));
        }
        return twin.master.nextInt(21);
    }

    static List<Object> elements(OffsetsTwin twin, int count) {
        List<Object> made = new ArrayList<>();
        for (int k = 0; k < count; k++) {
            made.add(element(twin));
        }
        return made;
    }

    /** Uniform in [-2, size + 2], near a lane start, or near the end. */
    static int index(OffsetsTwin twin) {
        int size = Math.max(twin.patched.size(), 0);
        Random master = twin.master;
        int roll = master.nextInt(10);
        if (roll < 5) {
            return master.nextInt(size + 5) - 2;
        }
        if (roll < 8) {
            long[] starts = OffsetsState.starts(OffsetsState.lanes(twin.patched));
            return (int) starts[master.nextInt(starts.length)] + master.nextInt(3) - 1;
        }
        return size - 1 + master.nextInt(3);
    }

    private static boolean full(OffsetsTwin twin) {
        return OffsetsState.laneCount(twin.patched) >= LANE_CAP;
    }

    static NamedOp addAllList(OffsetsTwin twin) {
        if (full(twin)) {
            return get(twin);
        }
        List<Object> source = elements(twin, twin.master.nextInt(13));
        return named("addAllList", (t, s) -> t.list(s).addAll(new ArrayList<>(source)));
    }

    private static LanePair pair(OffsetsTwin twin) {
        Object[] source = elements(twin, twin.master.nextInt(13)).toArray();
        int spare = twin.master.nextInt(3);
        LanePair pair = new LanePair(lane(source, spare), lane(source, spare));
        twin.handles.add(pair);
        return pair;
    }

    private static FastList<Object> lane(Object[] source, int spare) {
        FastList<Object> lane = new FastList<>(source.length + spare);
        lane.addAll(Arrays.asList(source));
        return lane;
    }

    static NamedOp addAllFast(OffsetsTwin twin) {
        if (!twin.foreign || full(twin)) {
            return get(twin);
        }
        LanePair pair = pair(twin);
        return named("addAllFast", (t, s) -> t.list(s).addAll(pair.lane(s)));
    }

    static NamedOp addComposited(OffsetsTwin twin) {
        if (!twin.foreign || full(twin)) {
            return get(twin);
        }
        LanePair pair = pair(twin);
        return named("addComposited", (t, s) -> {
            OffsetsState.addComposited(t.list(s), pair.lane(s));
            return "ok";
        });
    }

    static NamedOp addCompositedArray(OffsetsTwin twin) {
        List<Object> source = elements(twin, twin.master.nextInt(4));
        return named("addCompositedArray", (t, s) -> {
            OffsetsState.addComposited(t.list(s), new ArrayList<>(source));
            return "ok";
        });
    }

    /** A private lane that ends empty: add a unique marker, then remove it by value. */
    static NamedOp emptyLane(OffsetsTwin twin) {
        if (full(twin)) {
            return get(twin);
        }
        Integer marker = 1000 + twin.master.nextInt(1_000_000);
        return named("emptyLane", (t, s) -> {
            t.list(s).addAll(List.of(marker));
            return t.list(s).remove((Object) marker);
        });
    }

    static NamedOp addOne(OffsetsTwin twin) {
        Object element = element(twin);
        return named("add", (t, s) -> t.list(s).add(element));
    }

    static NamedOp addAt(OffsetsTwin twin) {
        int i = index(twin);
        Object element = element(twin);
        return new NamedOp("addAt", (t, s) -> {
            t.list(s).add(i, element);
            return "ok";
        }, i, OffsetsTwin.INSERT);
    }

    static NamedOp get(OffsetsTwin twin) {
        int i = index(twin);
        return new NamedOp("get", (t, s) -> t.list(s).get(i), i, OffsetsTwin.INDEXED);
    }

    static NamedOp set(OffsetsTwin twin) {
        int i = index(twin);
        Object element = element(twin);
        return new NamedOp("set", (t, s) -> t.list(s).set(i, element), i, OffsetsTwin.INDEXED);
    }

    static NamedOp removeAt(OffsetsTwin twin) {
        int i = index(twin);
        return new NamedOp("removeAt", (t, s) -> t.list(s).remove(i), i, OffsetsTwin.INDEXED);
    }

    static NamedOp removeObject(OffsetsTwin twin) {
        Object element = element(twin);
        return named("removeObject", (t, s) -> t.list(s).remove(element));
    }

    static NamedOp clear(OffsetsTwin twin) {
        return named("clear", (t, s) -> {
            t.list(s).clear();
            return "ok";
        });
    }

    static NamedOp resetSize(OffsetsTwin twin) {
        return named("resetSize", (t, s) -> {
            OffsetsState.resetSize(t.list(s));
            return "ok";
        });
    }

    static NamedOp reverse(OffsetsTwin twin) {
        return named("reverseThis", (t, s) -> t.list(s).reverseThis());
    }

    /** Flattening or deserializing ends the fast path; private seeds run them a third as often. */
    static boolean rare(OffsetsTwin twin) {
        return !twin.foreign && twin.master.nextInt(3) != 0;
    }

    static NamedOp sort(OffsetsTwin twin) {
        if (rare(twin)) {
            return get(twin);
        }
        int variant = twin.master.nextInt(4);
        return named("sort" + variant, (t, s) -> {
            MutableList<Object> list = t.list(s);
            Comparator<Object> order = variant % 2 == 0 ? null : OffsetsCallbacks.comparator();
            if (variant < 2) {
                list.sort(order);
            } else {
                list.sortThis(order);
            }
            OffsetsState.assertFlattened(t, s);
            return "ok";
        });
    }

    static NamedOp shuffle(OffsetsTwin twin) {
        if (rare(twin)) {
            return get(twin);
        }
        long shuffleSeed = twin.master.nextLong();
        return named("shuffleThis", (t, s) -> t.list(s).shuffleThis(new Random(shuffleSeed)));
    }

    static NamedOp replaceAll(OffsetsTwin twin) {
        return named("replaceAll", (t, s) -> {
            t.list(s).replaceAll(OffsetsCallbacks.operator());
            return "ok";
        });
    }

    private static ProbeSet probeSet(OffsetsTwin twin) {
        return new ProbeSet(elements(twin, twin.master.nextInt(8)));
    }

    static NamedOp retainAll(OffsetsTwin twin) {
        ProbeSet keep = new ProbeSet(elements(twin, 6 + twin.master.nextInt(16)));
        return named("retainAll", (t, s) -> t.list(s).retainAll(keep));
    }

    static NamedOp removeAll(OffsetsTwin twin) {
        ProbeSet drop = probeSet(twin);
        return named("removeAll", (t, s) -> t.list(s).removeAll(drop));
    }

    static NamedOp bulkIterable(OffsetsTwin twin) {
        ProbeSet members = probeSet(twin);
        if (twin.master.nextBoolean()) {
            return named("retainAllIterable", (t, s) -> t.list(s).retainAllIterable(members));
        }
        return named("removeAllIterable", (t, s) -> t.list(s).removeAllIterable(members));
    }

    static NamedOp removeIf(OffsetsTwin twin) {
        int mod = 2 + twin.master.nextInt(4);
        int rem = twin.master.nextInt(mod);
        int variant = twin.master.nextInt(3);
        if (variant == 0) {
            return named("removeIf", (t, s) -> t.list(s).removeIf(
                    OffsetsCallbacks.predicate(mod, rem)));
        }
        if (variant == 1) {
            return named("removeIfFilter", (t, s) -> t.list(s).removeIf(
                    OffsetsCallbacks.filter(mod, rem)));
        }
        return named("removeIfWith", (t, s) -> t.list(s).removeIfWith(
                OffsetsCallbacks.predicate2(mod), rem));
    }

    static NamedOp query(OffsetsTwin twin) {
        Object element = element(twin);
        ProbeSet members = probeSet(twin);
        return switch (twin.master.nextInt(5)) {
            case 0 -> named("indexOf", (t, s) -> t.list(s).indexOf(element));
            case 1 -> named("lastIndexOf", (t, s) -> t.list(s).lastIndexOf(element));
            case 2 -> named("contains", (t, s) -> t.list(s).contains(element));
            case 3 -> named("containsAll", (t, s) -> t.list(s).containsAll(members));
            default -> named("isEmpty", (t, s) -> t.list(s).isEmpty() + "/" + t.list(s).size());
        };
    }

    static NamedOp toArray(OffsetsTwin twin) {
        int size = Math.max(twin.patched.size(), 0);
        int[] lengths = {0, Math.max(size - 1, 0), size, size + 3};
        int length = lengths[twin.master.nextInt(lengths.length)];
        if (twin.master.nextBoolean()) {
            return named("toArray", (t, s) -> t.list(s).toArray());
        }
        return named("toArrayInto" + length, (t, s) -> {
            Object[] target = new Object[length];
            Arrays.fill(target, 77);
            return t.list(s).toArray(target);
        });
    }
}
