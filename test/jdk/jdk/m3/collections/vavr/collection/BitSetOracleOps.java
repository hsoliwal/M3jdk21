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
package jdk.m3.collections.vavr.collection;

import java.util.ArrayList;
import java.util.function.Function;
import java.util.function.IntToLongFunction;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import jdk.m3.collections.vavr.Tuple2;
import jdk.m3.collections.vavr.collection.BitSetOracleKit.ArgKind;
import jdk.m3.collections.vavr.collection.BitSetOracleKit.Side;
import jdk.m3.collections.vavr.control.Option;

/**
 * The operation table of the lockstep oracle and the fingerprint of one run: the call log, the
 * outcome (exception class and message, or result shape, words, identity, text and hash) and the
 * receiver before and after.
 */
//FLLDCIM: LEETCODIFY — operation table and result fingerprints for the BitSet oracle
@SuppressWarnings("auxiliaryclass")
final class BitSetOracleOps {

    private BitSetOracleOps() {
    }

    /** A named operation, applied the same way to either side. */
    static final class Named {
        final String name;
        final Function<Side, Object> fn;

        Named(String name, Function<Side, Object> fn) {
            this.name = name;
            this.fn = fn;
        }
    }

    /** The fingerprint text of one run plus the raw result for cross checks. */
    static final class Outcome {
        final String text;
        final Object result;

        Outcome(String text, Object result) {
            this.text = text;
            this.result = result;
        }
    }

    static java.util.List<Named> ops() {
        java.util.List<Named> ops = new ArrayList<>();
        for (ArgKind k : ArgKind.values()) {
            ops.add(new Named("removeAll " + k, s -> s.recv.removeAll(k.make.apply(s))));
            ops.add(new Named("retainAll " + k, s -> s.recv.retainAll(k.make.apply(s))));
            ops.add(new Named("addAll " + k, s -> s.recv.addAll(k.make.apply(s))));
            ops.add(new Named("ofAll " + k, s -> s.ofAll(k.make.apply(s))));
            ops.add(new Named("orElse " + k, s -> s.recv.orElse(k.make.apply(s))));
        }
        setOps(ops);
        rankOps(ops);
        predicateOps(ops);
        builderOps(ops);
        return ops;
    }

    private static void setOps(java.util.List<Named> ops) {
        ops.add(new Named("diff same", s -> s.recv.diff(s.arg)));
        ops.add(new Named("diff hash", s -> s.recv.diff(HashSet.ofAll(s.plan.order))));
        ops.add(new Named("diff counted", s -> s.recv.diff(s.counted)));
        ops.add(new Named("diff other", s -> s.recv.diff(s.other)));
        ops.add(new Named("intersect same", s -> s.recv.intersect(s.arg)));
        ops.add(new Named("intersect hash", s -> s.recv.intersect(HashSet.ofAll(s.plan.order))));
        ops.add(new Named("intersect self", s -> s.recv.intersect(s.recv)));
        ops.add(new Named("intersect other", s -> s.recv.intersect(s.other)));
        ops.add(new Named("union same", s -> s.recv.union(s.arg)));
        ops.add(new Named("union hash", s -> s.recv.union(HashSet.ofAll(s.plan.order))));
        ops.add(new Named("union other", s -> s.recv.union(s.other)));
        ops.add(new Named("arg intersect recv", s -> s.arg.intersect(s.recv)));
        ops.add(new Named("arg union recv", s -> s.arg.union(s.recv)));
        ops.add(new Named("arg diff recv", s -> s.arg.diff(s.recv)));
    }

    private static void rankOps(java.util.List<Named> ops) {
        ops.add(new Named("take", s -> s.recv.take(s.plan.n)));
        ops.add(new Named("drop", s -> s.recv.drop(s.plan.n)));
        ops.add(new Named("takeRight", s -> s.recv.takeRight(s.plan.n)));
        ops.add(new Named("dropRight", s -> s.recv.dropRight(s.plan.n)));
        ops.add(new Named("tail", s -> s.recv.tail()));
        ops.add(new Named("tailOption", s -> s.recv.tailOption()));
        ops.add(new Named("last", s -> s.recv.last()));
        ops.add(new Named("lastOption", s -> s.recv.lastOption()));
        ops.add(new Named("max", s -> s.recv.max()));
        ops.add(new Named("init", s -> s.recv.init()));
        ops.add(new Named("head", s -> s.recv.head()));
    }

    private static void predicateOps(java.util.List<Named> ops) {
        ops.add(new Named("filter", s -> s.recv.filter(BitSetOracleKit.pred(s))));
        ops.add(new Named("reject", s -> s.recv.reject(BitSetOracleKit.pred(s))));
        ops.add(new Named("partition", s -> s.recv.partition(BitSetOracleKit.pred(s))));
        ops.add(new Named("span", s -> s.recv.span(BitSetOracleKit.pred(s))));
        ops.add(new Named("takeWhile", s -> s.recv.takeWhile(BitSetOracleKit.pred(s))));
        ops.add(new Named("dropWhile", s -> s.recv.dropWhile(BitSetOracleKit.pred(s))));
        ops.add(new Named("takeUntil", s -> s.recv.takeUntil(BitSetOracleKit.pred(s))));
        ops.add(new Named("groupBy", s -> s.recv.groupBy(BitSetOracleKit.classifier(s))));
        ops.add(new Named("slideBy", s -> s.recv.slideBy(BitSetOracleKit.classifier(s))));
        ops.add(new Named("distinctBy", s -> s.recv.distinctBy((a, b) -> 0)));
        ops.add(new Named("sliding", s -> s.recv.sliding(2)));
        ops.add(new Named("grouped", s -> s.recv.grouped(3)));
        ops.add(new Named("scan", s -> s.recv.scan(s.m.elem.apply(0), (a, b) -> b)));
    }

    private static void builderOps(java.util.List<Named> ops) {
        ops.add(new Named("of special", s -> s.of(s.plan.special.toArray())));
        ops.add(new Named("of order", s -> s.of(s.plan.order.toArray())));
        ops.add(new Named("tabulate", s -> s.tabulate(s.plan.special.size(), i -> {
            BitSetOracleKit.LOG.append("g ");
            return s.plan.special.get(i);
        })));
        ops.add(new Named("fill", s -> s.fill(s.plan.special.size(),
                BitSetOracleKit.supplier(s.plan.special))));
    }

    static Named named(String name) {
        return ops().stream().filter(op -> op.name.equals(name)).findFirst().orElseThrow();
    }

    static Outcome run(Named op, Side s) {
        String before = s.recv.toString();
        BitSetOracleKit.LOG.setLength(0);
        Object result = invoke(op, s);
        String calls = BitSetOracleKit.LOG.toString();
        BitSetOracleKit.LOG.setLength(0);
        String outcome = describe(result, s);
        String text = calls + " | " + outcome + " | " + BitSetOracleKit.LOG + " | " + before
                + " -> " + s.recv;
        return new Outcome(text, result);
    }

    private static Object invoke(Named op, Side s) {
        try {
            return op.fn.apply(s);
        } catch (RuntimeException | Error e) {
            return e;
        }
    }

    static String describe(Object r, Side s) {
        if (r instanceof Throwable t) {
            return "EX " + t.getClass().getName() + ": " + helpfulNpeFree(t.getMessage());
        } else if (r instanceof BitSet<?> set) {
            return describeSet(set, s);
        } else if (r instanceof Tuple2<?, ?> t) {
            return "(" + describe(t._1, s) + ", " + describe(t._2, s) + ")";
        } else if (r instanceof Option<?> o) {
            return o.isEmpty() ? "None" : "Some " + describe(o.get(), s);
        } else if (r instanceof Traversable<?> all) {
            return describeAll(all, s);
        }
        return r + " : " + (r == null ? "null" : r.getClass().getName());
    }

    private static String describeAll(Traversable<?> all, Side s) {
        StringBuilder sb = new StringBuilder(all.getClass().getSimpleName()).append('[');
        try {
            for (Object x : all) {
                sb.append(describe(x, s)).append("; ");
            }
        } catch (RuntimeException e) {
            sb.append(describe(e, s));
        }
        return sb.append(']').toString();
    }

    private static String describeSet(BitSet<?> set, Side s) {
        String id = (set == s.recv ? " =recv" : "") + (set == s.arg ? " =arg" : "")
                + (set == s.other ? " =other" : "") + (set == s.counted ? " =counted" : "");
        return shape(set) + id + " " + safe(set::toString) + " #" + safe(() -> "" + set.hashCode())
                + " len" + set.length();
    }

    static String shape(BitSet<?> set) {
        String name = set.getClass().getSimpleName().replaceFirst("^Ref", "");
        StringBuilder sb = new StringBuilder(name).append('/');
        if (set instanceof BitSetModule.AbstractBitSet<?> a) {
            appendWords(sb, a.getWordsNum(), a::getWord);
        } else if (set instanceof RefBitSetModule.RefAbstractBitSet<?> a) {
            appendWords(sb, a.getWordsNum(), a::getWord);
        }
        return sb.toString();
    }

    private static void appendWords(StringBuilder sb, int n, IntToLongFunction word) {
        sb.append(n);
        IntStream.range(0, n).filter(i -> word.applyAsLong(i) != 0).forEach(i -> sb.append(' ')
                .append(i).append(':').append(Long.toHexString(word.applyAsLong(i))));
    }

    private static String safe(Supplier<String> text) {
        try {
            return text.get();
        } catch (RuntimeException e) {
            return "EX " + e.getClass().getName() + ": " + helpfulNpeFree(e.getMessage());
        }
    }

    /** Helpful NPE text names the null variable only with -g; blank that name so builds compare. */
    private static String helpfulNpeFree(String message) {
        return message == null ? null : message.replaceAll("because \"[^\"]*\"", "because \"?\"");
    }
}
