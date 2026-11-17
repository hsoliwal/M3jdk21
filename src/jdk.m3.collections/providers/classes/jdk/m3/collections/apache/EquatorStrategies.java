/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections.apache;

import java.util.Objects;

import jdk.m3.collections.eclipse.api.block.HashingStrategy;

/**
 * The adapters between {@link Equator} and {@link HashingStrategy}, the equality strategy the
 * collections of the module share. Each adapter gives back the object it adapts when it is
 * converted again.
 */
final class EquatorStrategies {

    private EquatorStrategies() {
    }

    static <T> HashingStrategy<T> toStrategy(final Equator<T> equator) {
        if (equator instanceof StrategyEquator<T> adapter) {
            return adapter.strategy;
        }
        return new EquatorStrategy<>(equator);
    }

    static <T> Equator<T> toEquator(final HashingStrategy<T> strategy) {
        Objects.requireNonNull(strategy, "strategy");
        if (strategy instanceof EquatorStrategy<T> adapter) {
            return adapter.equator;
        }
        return new StrategyEquator<>(strategy);
    }

    /**
     * An equator seen as a hashing strategy. A hashing strategy is serializable; this one is when
     * its equator is.
     */
    @SuppressWarnings("serial") // the equator is serializable when the caller's equator is
    static final class EquatorStrategy<T> implements HashingStrategy<T> {

        private static final long serialVersionUID = 1L;

        final Equator<T> equator;

        EquatorStrategy(final Equator<T> equator) {
            this.equator = equator;
        }

        @Override
        public int computeHashCode(final T object) {
            return equator.hash(object);
        }

        @Override
        public boolean equals(final T object1, final T object2) {
            return equator.equate(object1, object2);
        }

        @Override
        public String toString() {
            return "HashingStrategy(" + equator + ")";
        }
    }

    /** A hashing strategy seen as an equator. */
    static final class StrategyEquator<T> implements Equator<T> {

        final HashingStrategy<T> strategy;

        StrategyEquator(final HashingStrategy<T> strategy) {
            this.strategy = strategy;
        }

        @Override
        public boolean equate(final T o1, final T o2) {
            return strategy.equals(o1, o2);
        }

        @Override
        public int hash(final T o) {
            return strategy.computeHashCode(o);
        }

        @Override
        public String toString() {
            return "Equator(" + strategy + ")";
        }
    }
}
