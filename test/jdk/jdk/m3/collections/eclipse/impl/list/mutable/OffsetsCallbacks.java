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

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import jdk.m3.collections.eclipse.api.block.function.Function2;
import jdk.m3.collections.eclipse.api.block.predicate.Predicate;
import jdk.m3.collections.eclipse.api.block.predicate.Predicate2;
import jdk.m3.collections.eclipse.api.block.procedure.Procedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.ObjectIntProcedure;
import jdk.m3.collections.eclipse.api.list.MutableList;

/**
 * Callback context of the CompositeFastList lockstep proof: every caller callback (predicate,
 * comparator, contains, procedure, equals, hashCode) traces its argument, reads its owner back
 * (get, size, isEmpty), may mutate the owner once and may throw on a seeded call.
 */
final class OffsetsCallbacks {

    /** The owner and seeded behaviour of the callbacks of the operation now running. */
    static final class Ctx {
        MutableList<Object> owner;
        StringBuilder trace = new StringBuilder();
        Random rnd = new Random(0);
        String op = "";
        OffsetsState.Coverage cov;
        boolean active;
        int side;
        int calls;
        int throwAt;
        int mutationsLeft;
    }

    static final Ctx CTX = new Ctx();

    private OffsetsCallbacks() {
    }

    static void begin(MutableList<Object> owner, long seed, int throwAt, int mutations) {
        CTX.owner = owner;
        CTX.trace = new StringBuilder();
        CTX.rnd = new Random(seed);
        CTX.calls = 0;
        CTX.throwAt = throwAt;
        CTX.mutationsLeft = mutations;
        CTX.active = true;
    }

    static String end() {
        CTX.active = false;
        CTX.owner = null;
        return CTX.trace.toString();
    }

    /** One callback: trace, owner read-back, maybe one tracked mutation, maybe a throw. */
    static void hit(String kind, Object arg) {
        Ctx c = CTX;
        if (!c.active) {
            return;
        }
        c.calls++;
        c.trace.append(kind).append(':').append(render(arg));
        int j = c.rnd.nextInt(Math.max(c.owner.size(), 0) + 3) - 1;
        c.trace.append(" g").append(j).append(outcome(() -> c.owner.get(j)));
        c.trace.append(" s").append(c.owner.size()).append(c.owner.isEmpty()).append(';');
        OffsetsState.countReentrant(c);
        mutateOnce(c);
        if (c.calls == c.throwAt) {
            throw new IllegalStateException("boom " + c.calls);
        }
    }

    private static void mutateOnce(Ctx c) {
        if (c.mutationsLeft <= 0 || c.rnd.nextInt(6) != 0) {
            return;
        }
        c.mutationsLeft--;
        boolean add = c.rnd.nextBoolean();
        Integer element = c.rnd.nextInt(21);
        c.trace.append(add ? " add" : " rm0");
        c.trace.append(outcome(() -> add ? c.owner.add(element) : c.owner.remove(0)));
    }

    /** Integer value, 100 + probe key, -1 for null: the order and membership key of elements. */
    static int key(Object element) {
        if (element == null) {
            return -1;
        }
        if (element instanceof Probe probe) {
            return 100 + probe.key;
        }
        return element instanceof Integer value ? value : -2;
    }

    /** Text of a result that never calls equals, hashCode or a callback. */
    static String render(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Integer number) {
            return "i" + number;
        }
        if (value instanceof Object[] array) {
            return renderArray(array);
        }
        if (value instanceof Probe || value instanceof Boolean || value instanceof String) {
            return value.toString();
        }
        return value instanceof AbstractMutableList<?> ? "list" : normalize(
                value.getClass().getSimpleName());
    }

    static String renderArray(Object[] array) {
        StringBuilder text = new StringBuilder("[").append(array.length).append(':');
        for (Object each : array) {
            text.append(render(each)).append(',');
        }
        return text.append(']').toString();
    }

    /** The value as text, or the exception class and message. */
    static String outcome(Supplier<?> action) {
        try {
            return "=" + render(action.get());
        } catch (RuntimeException failure) {
            return fault(failure);
        }
    }

    static String fault(RuntimeException failure) {
        return "!" + failure.getClass().getName() + ":" + normalize(failure.getMessage());
    }

    /** The reference copy differs only in its class name; messages built from it agree. */
    static String normalize(String message) {
        return String.valueOf(message).replace("LegacyCompositeFastList", "CompositeFastList");
    }

    static Predicate<Object> predicate(int mod, int rem) {
        return each -> {
            hit("pr", each);
            return Math.floorMod(key(each), mod) == rem;
        };
    }

    static java.util.function.Predicate<Object> filter(int mod, int rem) {
        return each -> {
            hit("fi", each);
            return Math.floorMod(key(each), mod) == rem;
        };
    }

    static Predicate2<Object, Integer> predicate2(int mod) {
        return (each, rem) -> {
            hit("p2", each);
            return Math.floorMod(key(each), mod) == rem;
        };
    }

    static Comparator<Object> comparator() {
        return (left, right) -> {
            hit("cmp", left);
            return Integer.compare(key(left), key(right));
        };
    }

    static UnaryOperator<Object> operator() {
        return each -> {
            hit("op", each);
            return each instanceof Integer value ? Integer.valueOf((value + 1) % 21) : each;
        };
    }

    static Procedure<Object> procedure() {
        return each -> hit("pc", each);
    }

    static ObjectIntProcedure<Object> indexed() {
        return (each, index) -> hit("ix" + index, each);
    }

    static Function2<Integer, Object, Integer> injector() {
        return (total, each) -> {
            hit("in", each);
            return total + key(each);
        };
    }

    /** An element whose equals and hashCode are traced callbacks; equal by key. */
    static final class Probe implements Serializable {
        private static final long serialVersionUID = 1L;
        final int id;
        final int key;

        Probe(int id, int key) {
            this.id = id;
            this.key = key;
        }

        @Override
        public boolean equals(Object other) {
            hit("eq", this);
            return other instanceof Probe probe && probe.key == this.key;
        }

        @Override
        public int hashCode() {
            hit("hc", this);
            return this.key;
        }

        @Override
        public String toString() {
            return "p" + this.id + "k" + this.key;
        }
    }

    /** A collection whose contains is a traced callback; membership by key. */
    static final class ProbeSet extends AbstractCollection<Object> {
        private final List<Object> members;

        ProbeSet(List<Object> members) {
            this.members = members;
        }

        @Override
        public boolean contains(Object candidate) {
            hit("ct", candidate);
            int wanted = key(candidate);
            return this.members.stream().anyMatch(member -> key(member) == wanted);
        }

        @Override
        public Iterator<Object> iterator() {
            return this.members.iterator();
        }

        @Override
        public int size() {
            return this.members.size();
        }
    }
}
