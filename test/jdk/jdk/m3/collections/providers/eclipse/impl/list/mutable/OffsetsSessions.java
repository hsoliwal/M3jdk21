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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicReference;

import jdk.m3.collections.eclipse.api.list.MutableList;
import jdk.m3.collections.eclipse.impl.list.mutable.OffsetsTwin.LanePair;
import jdk.m3.collections.eclipse.impl.list.mutable.OffsetsTwin.NamedOp;
import jdk.m3.collections.eclipse.impl.parallel.BatchIterable;

/**
 * Seeded multi-step sessions (iterator, list iterator, sub-list views), traversals, equality,
 * serialization and direct mutation of caller-held lanes, for the lockstep proof.
 */
final class OffsetsSessions {

    private OffsetsSessions() {
    }

    private static String outcome(java.util.function.Supplier<?> action) {
        return OffsetsCallbacks.outcome(action);
    }

    private static int[] draws(OffsetsTwin twin, int count, int bound) {
        int[] drawn = new int[count];
        for (int k = 0; k < count; k++) {
            drawn[k] = twin.master.nextInt(bound);
        }
        return drawn;
    }

    private static int span(OffsetsTwin twin) {
        return Math.max(twin.patched.size(), 0) + 3;
    }

    static NamedOp iterate(OffsetsTwin twin) {
        int[] kinds = draws(twin, 1 + twin.master.nextInt(14), 4);
        int[] picks = draws(twin, kinds.length, span(twin));
        return OffsetsOps.named("iterator", (t, s) -> {
            MutableList<Object> list = t.list(s);
            Iterator<Object> it = list.iterator();
            StringBuilder out = new StringBuilder();
            for (int k = 0; k < kinds.length; k++) {
                out.append(iteratorStep(list, it, kinds[k], picks[k] - 1));
            }
            return out.toString();
        });
    }

    private static String iteratorStep(MutableList<Object> list, Iterator<Object> it, int kind,
            int j) {
        return switch (kind) {
            case 0 -> outcome(it::hasNext);
            case 1 -> outcome(it::next);
            case 2 -> outcome(() -> {
                it.remove();
                return "rm";
            });
            default -> outcome(() -> list.get(j));
        };
    }

    static NamedOp listIterate(OffsetsTwin twin) {
        if (OffsetsOps.rare(twin)) {
            return OffsetsOps.get(twin);
        }
        int start = twin.master.nextInt(span(twin)) - 1;
        int[] kinds = draws(twin, 1 + twin.master.nextInt(12), 9);
        List<Object> elements = OffsetsOps.elements(twin, kinds.length);
        return OffsetsOps.named("listIterator", (t, s) -> listWalk(t, s, start, kinds, elements));
    }

    private static String listWalk(OffsetsTwin twin, int side, int start, int[] kinds,
            List<Object> elements) {
        boolean flattens = OffsetsState.laneCount(twin.list(side)) != 1;
        ListIterator<Object> it = twin.list(side).listIterator(start);
        if (flattens) {
            OffsetsState.assertFlattened(twin, side);
        }
        StringBuilder out = new StringBuilder();
        for (int k = 0; k < kinds.length; k++) {
            out.append(listStep(it, kinds[k], elements.get(k)));
        }
        return out.toString();
    }

    private static String listStep(ListIterator<Object> it, int kind, Object element) {
        return switch (kind) {
            case 0 -> outcome(it::hasNext);
            case 1 -> outcome(it::next);
            case 2 -> outcome(it::hasPrevious);
            case 3 -> outcome(it::previous);
            case 4 -> outcome(it::nextIndex);
            case 5 -> outcome(it::previousIndex);
            default -> outcome(() -> listChange(it, kind, element));
        };
    }

    /** Kind 6 adds, 7 sets, 8 removes through the list iterator. */
    private static String listChange(ListIterator<Object> it, int kind, Object element) {
        if (kind == 6) {
            it.add(element);
            return "add";
        }
        if (kind == 7) {
            it.set(element);
            return "set";
        }
        it.remove();
        return "rm";
    }

    static NamedOp subList(OffsetsTwin twin) {
        int from = twin.master.nextInt(span(twin)) - 1;
        int to = from + twin.master.nextInt(8) - 1;
        int[] kinds = draws(twin, 1 + twin.master.nextInt(10), 10);
        int[] picks = draws(twin, kinds.length, Math.max(to - from, 0) + 3);
        List<Object> elements = OffsetsOps.elements(twin, kinds.length);
        return OffsetsOps.named("subList", (t, s) -> {
            AtomicReference<MutableList<Object>> view =
                    new AtomicReference<>(t.list(s).subList(from, to));
            StringBuilder out = new StringBuilder();
            for (int k = 0; k < kinds.length; k++) {
                out.append(subStep(view, kinds[k], picks[k] - 1, elements.get(k)));
            }
            return out.append(contents(view.get())).toString();
        });
    }

    private static String subStep(AtomicReference<MutableList<Object>> view, int kind, int j,
            Object element) {
        MutableList<Object> sub = view.get();
        return switch (kind) {
            case 0 -> outcome(() -> sub.get(j));
            case 1 -> outcome(() -> sub.set(j, element));
            case 2 -> outcome(() -> sub.add(element));
            case 3 -> outcome(() -> subInsert(sub, j, element));
            case 4 -> outcome(() -> sub.remove(j));
            case 5 -> outcome(sub::size);
            case 6 -> outcome(() -> subClear(sub));
            case 7 -> outcome(() -> sub.addAll(List.of(3, 4)));
            case 8 -> outcome(() -> subNest(view, j));
            default -> contents(sub);
        };
    }

    private static String subInsert(MutableList<Object> sub, int j, Object element) {
        sub.add(j, element);
        return "ok";
    }

    private static String subClear(MutableList<Object> sub) {
        sub.clear();
        return "ok";
    }

    private static String subNest(AtomicReference<MutableList<Object>> view, int j) {
        int from = Math.max(j, 0);
        view.set(view.get().subList(from, from + 1));
        return "nest";
    }

    /** Every element seen through the view, which reads through the twin. */
    private static String contents(MutableList<Object> sub) {
        StringBuilder out = new StringBuilder("{");
        for (int i = 0; i < sub.size(); i++) {
            int at = i;
            out.append(outcome(() -> sub.get(at)));
        }
        return out.append('}').toString();
    }

    static NamedOp visit(OffsetsTwin twin) {
        return switch (twin.master.nextInt(5)) {
            case 0 -> visitor("forEachWithIndex", l -> l.forEachWithIndex(
                    OffsetsCallbacks.indexed()));
            case 1 -> visitor("reverseForEachWithIndex", l -> l.reverseForEachWithIndex(
                    OffsetsCallbacks.indexed()));
            case 2 -> visitor("each", l -> l.each(OffsetsCallbacks.procedure()));
            case 3 -> visitor("reverseForEach", l -> l.reverseForEach(
                    OffsetsCallbacks.procedure()));
            default -> OffsetsOps.named("injectInto", (t, s) -> t.list(s).injectInto(
                    Integer.valueOf(0), OffsetsCallbacks.injector()));
        };
    }

    private static NamedOp visitor(String name,
            java.util.function.Consumer<MutableList<Object>> action) {
        return OffsetsOps.named(name, (t, s) -> {
            action.accept(t.list(s));
            return "ok";
        });
    }

    static NamedOp batch(OffsetsTwin twin) {
        int batchSize = 1 + twin.master.nextInt(5);
        return OffsetsOps.named("batch" + batchSize, (t, s) -> {
            BatchIterable<Object> batches = OffsetsState.batch(t.list(s));
            int count = batches.getBatchCount(batchSize);
            StringBuilder out = new StringBuilder().append(count);
            for (int k = 0; k <= count; k++) {
                int section = k;
                out.append(outcome(() -> {
                    batches.batchForEach(OffsetsCallbacks.procedure(), section, count);
                    return section;
                }));
            }
            return out.toString();
        });
    }

    static NamedOp equality(OffsetsTwin twin) {
        return switch (twin.master.nextInt(5)) {
            case 0 -> quiet("equalsTwin", (t, s) -> t.list(s).equals(t.list(1 - s)));
            case 1 -> quiet("equalsSnapshot", (t, s) -> t.list(s).equals(
                    new ArrayList<>(OffsetsState.elements(t.list(s)))));
            case 2 -> quiet("snapshotEquals", (t, s) -> new ArrayList<>(
                    OffsetsState.elements(t.list(s))).equals(t.list(s)));
            case 3 -> quiet("hashCode", (t, s) -> t.list(s).hashCode());
            default -> quiet("toString", (t, s) -> t.list(s).toString());
        };
    }

    private static NamedOp quiet(String name, OffsetsTwin.TwinOp op) {
        return new NamedOp(name, op, OffsetsTwin.NONE, OffsetsTwin.QUIET);
    }

    static NamedOp serialize(OffsetsTwin twin) {
        if (OffsetsOps.rare(twin)) {
            return OffsetsOps.get(twin);
        }
        return new NamedOp("serialize", (t, s) -> {
            MutableList<Object> copy = roundTrip(t.list(s));
            t.replace(s, copy);
            if (s == 0) {
                OffsetsState.assertDeserialized(copy);
            }
            return "ok";
        }, OffsetsTwin.NONE, OffsetsTwin.QUIET | OffsetsTwin.REBIND);
    }

    @SuppressWarnings("unchecked")
    static MutableList<Object> roundTrip(MutableList<Object> list) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(list);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
        try (ObjectInputStream in = new ObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray()))) {
            return (MutableList<Object>) in.readObject();
        } catch (IOException | ClassNotFoundException failure) {
            throw new IllegalStateException(failure);
        }
    }

    /** The caller resizes one of its lanes behind the composites' back. */
    static NamedOp external(OffsetsTwin twin) {
        if (twin.handles.isEmpty()) {
            return OffsetsOps.get(twin);
        }
        int h = twin.master.nextInt(twin.handles.size());
        LanePair pair = twin.handles.get(h);
        int kind = twin.master.nextInt(5);
        int j = twin.master.nextInt(pair.patched().size() + 3) - 1;
        Object element = OffsetsOps.element(twin);
        return OffsetsOps.named("external" + kind, (t, s) -> laneChange(
                pair.lane(s), kind, j, element));
    }

    private static Object laneChange(FastList<Object> lane, int kind, int j, Object element) {
        switch (kind) {
            case 0 -> lane.add(element);
            case 1 -> lane.add(j, element);
            case 2 -> lane.remove(j);
            case 3 -> lane.clear();
            default -> lane.removeIf(
                    (java.util.function.Predicate<Object>) e -> OffsetsCallbacks.key(e) % 3 == 0);
        }
        return lane.size();
    }
}
