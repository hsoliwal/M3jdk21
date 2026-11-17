/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections.apache;

import java.io.Serializable;
import java.util.Objects;

import jdk.m3.collections.eclipse.api.block.HashingStrategy;

/**
 * The adapters between {@link Equator} and {@link HashingStrategy}, the equality strategy the
 * collections of the module share. Each adapter gives back the object it adapts when it is
 * converted again, is equal to another adapter of an equal object, and is serializable when what
 * it wraps is.
 */
final class EquatorStrategies {

    private EquatorStrategies() {
    }

    @SuppressWarnings("unchecked") // a strategy only consumes its type: one of a supertype serves T
    static <T> HashingStrategy<T> toStrategy(final Equator<T> equator) {
        if (equator instanceof StrategyEquator<?> adapter) {
            return (HashingStrategy<T>) adapter.strategy;
        }
        return new EquatorStrategy<>(equator);
    }

    @SuppressWarnings("unchecked") // an equator only consumes its type: one of a supertype serves T
    static <T> Equator<T> toEquator(final HashingStrategy<? super T> strategy) {
        Objects.requireNonNull(strategy, "strategy");
        if (strategy instanceof EquatorStrategy<?> adapter) {
            return (Equator<T>) adapter.equator;
        }
        return new StrategyEquator<>(strategy);
    }

    /** An equator seen as a hashing strategy. */
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
        public boolean equals(final Object other) {
            return other instanceof EquatorStrategy<?> that && equator.equals(that.equator);
        }

        @Override
        public int hashCode() {
            return equator.hashCode();
        }

        @Override
        public String toString() {
            return "HashingStrategy(" + equator + ")";
        }
    }

    /** A hashing strategy seen as an equator; serializable, as hashing strategies are. */
    static final class StrategyEquator<T> implements Equator<T>, Serializable {

        private static final long serialVersionUID = 1L;

        final HashingStrategy<? super T> strategy;

        StrategyEquator(final HashingStrategy<? super T> strategy) {
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
        public boolean equals(final Object other) {
            return other instanceof StrategyEquator<?> that && strategy.equals(that.strategy);
        }

        @Override
        public int hashCode() {
            return strategy.hashCode();
        }

        @Override
        public String toString() {
            return "Equator(" + strategy + ")";
        }
    }
}
