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
package jdk.m3.collections.guava.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.Supplier;

/**
 * The private connection lane of a stable {@link DirectedGraphConnections}, driven directly in
 * lockstep with a {@link java.util.ArrayList} holding the same connection instances: add only
 * absent connections (I7), remove members and non-members, step suspended iterators of both, and
 * check order, size, modCount and the lane invariants after every operation.
 */
final class LaneLockstep {

    private static final int PERCENT = 100;
    private static final int GROW_ADDS = 80;
    private static final int SHRINK_ADDS = 15;
    private static final int MAX_SUSPENDED = 4;
    private static final int RAW_ACTIONS = 2;
    private static final int OPEN_ODDS = 4;

    private final Collection<Object> lane;
    private final List<Object> list = new ArrayList<>();
    private final boolean[][] present;
    private final List<Iterator<?>[]> suspended = new ArrayList<>();
    private final StringBuilder trace = new StringBuilder();
    private int modCount;
    private int tag;

    @SuppressWarnings("unchecked")
    LaneLockstep(int universe) {
        this.lane = (Collection<Object>) LaneProbe.ordered(
                DirectedGraphConnections.of(ElementOrder.stable()));
        this.present = new boolean[2][universe];
    }

    Object add(boolean successor, int id) {
        Object connection = LaneProbe.connection(successor, new LaneKey(id, tag++));
        present[successor ? 1 : 0][id] = true;
        modCount++;
        return compare(() -> list.add(connection), () -> lane.add(connection), "add");
    }

    Object remove(boolean successor, int id) {
        Object probe = LaneProbe.connection(successor, new LaneKey(id, tag++));
        if (present[successor ? 1 : 0][id]) {
            present[successor ? 1 : 0][id] = false;
            modCount++;
        }
        return compare(() -> list.remove(probe), () -> lane.remove(probe), "remove");
    }

    private Object compare(Supplier<?> expected, Supplier<?> actual, String at) {
        Object left = LaneOutcomes.outcome(expected);
        Object right = LaneOutcomes.outcome(actual);
        LaneOutcomes.assertSameOutcome(left, right, at);
        LaneOutcomes.assertSameSequence(list.iterator(), lane.iterator(), at + " I1 order");
        assertEquals(list.size(), lane.size(), at + " I2 size");
        LaneProbe.check(lane, at);
        assertEquals(modCount, LaneProbe.modCount(lane), at + " I6 modCount");
        return right;
    }

    /** Adds an absent connection or removes one, member or not, then steps an iterator. */
    void randomStep(Random random, boolean growing) {
        boolean successor = random.nextBoolean();
        int id = random.nextInt(present[0].length);
        boolean member = present[successor ? 1 : 0][id];
        if (!member && random.nextInt(PERCENT) < (growing ? GROW_ADDS : SHRINK_ADDS)) {
            add(successor, id);
        } else {
            remove(successor, id);
        }
        touchIterators(random);
    }

    private void touchIterators(Random random) {
        if (random.nextInt(OPEN_ODDS) == 0 || suspended.isEmpty()) {
            if (suspended.size() == MAX_SUSPENDED) {
                suspended.remove(random.nextInt(MAX_SUSPENDED));
            }
            suspended.add(new Iterator<?>[] {list.iterator(), lane.iterator()});
        } else {
            Iterator<?>[] pair = suspended.get(random.nextInt(suspended.size()));
            LaneOutcomes.step(pair[0], pair[1], random.nextInt(RAW_ACTIONS), "lane iterator");
        }
    }

    /**
     * Runs a script of space-separated tokens: {@code +p3}/{@code +s3} add Pred/Succ of node 3,
     * {@code -p3}/{@code -s3} remove one, {@code i} opens an iterator pair, {@code n} calls
     * next() and {@code h} hasNext() on it. Returns the rendered outcomes.
     */
    String run(String script) {
        Arrays.stream(script.split(" ")).forEach(this::token);
        return trace.toString().trim();
    }

    private void token(String token) {
        Object outcome = switch (token.charAt(0)) {
            case '+' -> add(token.charAt(1) == 's', Integer.parseInt(token.substring(2)));
            case '-' -> remove(token.charAt(1) == 's', Integer.parseInt(token.substring(2)));
            case 'i' -> open();
            default -> scriptStep(token.charAt(0) == 'n' ? LaneOutcomes.NEXT
                    : LaneOutcomes.HAS_NEXT);
        };
        trace.append(' ').append(render(outcome));
    }

    private String open() {
        suspended.clear();
        suspended.add(new Iterator<?>[] {list.iterator(), lane.iterator()});
        return "i";
    }

    private Object scriptStep(int action) {
        Iterator<?>[] pair = suspended.get(0);
        return LaneOutcomes.step(pair[0], pair[1], action, "script step");
    }

    private static String render(Object outcome) {
        if (outcome instanceof Boolean value) {
            return value ? "T" : "F";
        }
        if (outcome instanceof RuntimeException e) {
            return e.getClass().getSimpleName();
        }
        if (LaneProbe.isConnection(outcome)) {
            String kind = LaneProbe.kind(outcome).substring(0, 1).toLowerCase(Locale.ROOT);
            return kind + ((LaneKey) LaneProbe.node(outcome)).id;
        }
        return String.valueOf(outcome);
    }
}
