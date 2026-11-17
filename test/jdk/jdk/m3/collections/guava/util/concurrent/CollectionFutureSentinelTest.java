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
package jdk.m3.collections.guava.util.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import jdk.m3.collections.guava.collect.ImmutableList;
import org.junit.jupiter.api.Test;

/**
 * CollectionFuture keeps per-input results in an Object[] of EMPTY sentinels instead of Present
 * wrappers: against the verbatim pre-change copy RefCollectionFuture, the same input futures
 * completed in random order with values, null values, failures and cancellations give equal
 * allAsList/successfulAsList outcomes (result lists with nulls where nothing was delivered,
 * failures and cancellation), and no sentinel ever reaches a result list.
 */
//FLLDCIM: LEETCODIFY - differential proof for the CollectionFuture EMPTY slots
class CollectionFutureSentinelTest {
    private static String outcome(Future<?> future) {
        if (!future.isDone()) {
            return "pending";
        }
        if (future.isCancelled()) {
            return "cancelled";
        }
        try {
            return "=" + future.get();
        } catch (ExecutionException e) {
            return "!" + e.getCause().getClass().getName() + ": " + e.getCause().getMessage();
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        }
    }

    private static void complete(SettableFuture<Object> input, int kind, int index) {
        switch (kind) {
            case 0 -> input.set("v" + index);
            case 1 -> input.set(null);
            case 2 -> input.setException(new IllegalStateException("input " + index));
            default -> input.cancel(false);
        }
    }

    private static void trial(Random random, boolean allMustSucceed) {
        int n = random.nextInt(12);
        List<SettableFuture<Object>> inputs = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            inputs.add(SettableFuture.create());
        }
        ImmutableList<SettableFuture<Object>> copy = ImmutableList.copyOf(inputs);
        CollectionFuture.ListFuture<Object> impl = new CollectionFuture.ListFuture<>(copy,
                allMustSucceed);
        RefCollectionFuture.ListFuture<Object> ref = new RefCollectionFuture.ListFuture<>(copy,
                allMustSucceed);
        int[] order = random.ints(0, Math.max(1, n)).distinct().limit(n).toArray();
        for (int i = 0; i < n; i++) {
            if (i < order.length) {
                complete(inputs.get(order[i]), random.nextInt(random.nextBoolean() ? 2 : 4),
                        order[i]);
            }
            assertEquals(outcome(ref), outcome(impl), "after completion " + i);
        }
        for (SettableFuture<Object> input : inputs) {
            input.set("late");
        }
        assertEquals(outcome(ref), outcome(impl), "final");
        assertTrue(!outcome(impl).contains("java.lang.Object@"), outcome(impl));
    }

    @Test
    void successfulAsListMatchesOldCopy() {
        Logger.getLogger(AggregateFuture.class.getName()).setLevel(Level.OFF);
        Random random = new Random(0xC011EL);
        for (int k = 0; k < 4000; k++) {
            trial(random, false);
        }
    }

    @Test
    void allAsListMatchesOldCopy() {
        Logger.getLogger(AggregateFuture.class.getName()).setLevel(Level.OFF);
        Random random = new Random(0xC011FL);
        for (int k = 0; k < 4000; k++) {
            trial(random, true);
        }
    }

    /** Each side gets its own inputs: cancelling an output cancels its inputs. */
    @Test
    void outputCancelMatchesOldCopy() {
        Random random = new Random(0xC0120L);
        for (int k = 0; k < 500; k++) {
            int n = 1 + random.nextInt(6);
            List<SettableFuture<Object>> mine = new ArrayList<>();
            List<SettableFuture<Object>> theirs = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                mine.add(SettableFuture.create());
                theirs.add(SettableFuture.create());
            }
            boolean all = random.nextBoolean();
            CollectionFuture.ListFuture<Object> impl =
                    new CollectionFuture.ListFuture<>(ImmutableList.copyOf(mine), all);
            RefCollectionFuture.ListFuture<Object> ref =
                    new RefCollectionFuture.ListFuture<>(ImmutableList.copyOf(theirs), all);
            mine.get(0).set(null);
            theirs.get(0).set(null);
            assertEquals(ref.cancel(false), impl.cancel(false), "cancel");
            assertEquals(outcome(ref), outcome(impl), "after cancel");
            assertEquals(theirs.get(n - 1).isCancelled(), mine.get(n - 1).isCancelled(), "inputs");
        }
    }
}
