/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections.guava.base;

import static jdk.m3.collections.guava.base.Preconditions.checkNotNull;

import java.io.Serializable;
import jdk.m3.collections.eclipse.api.block.HashingStrategy;

/**
 * The adapters between {@link Equivalence} and {@link HashingStrategy}, the equality strategy
 * the collections of the module share. Each adapter gives back the object it adapts when it is
 * converted again, is equal to another adapter of an equal object, and is serializable when what
 * it wraps is.
 */
final class EquivalenceStrategies {

  private EquivalenceStrategies() {}

  @SuppressWarnings("unchecked") // a strategy only consumes its type: one of a supertype serves T
  static <T> HashingStrategy<T> toStrategy(Equivalence<T> equivalence) {
    if (equivalence instanceof StrategyEquivalence<?> adapter) {
      return (HashingStrategy<T>) adapter.strategy;
    }
    return new EquivalenceStrategy<>(equivalence);
  }

  @SuppressWarnings("unchecked") // an equivalence only consumes its type: a supertype's serves T
  static <T> Equivalence<T> toEquivalence(HashingStrategy<? super T> strategy) {
    checkNotNull(strategy, "strategy");
    if (strategy instanceof EquivalenceStrategy<?> adapter) {
      return (Equivalence<T>) adapter.equivalence;
    }
    return new StrategyEquivalence<>(strategy);
  }

  /** An equivalence seen as a hashing strategy. */
  @SuppressWarnings("serial") // the equivalence is serializable when the caller's equivalence is
  static final class EquivalenceStrategy<T> implements HashingStrategy<T> {

    private static final long serialVersionUID = 1L;

    final Equivalence<T> equivalence;

    EquivalenceStrategy(Equivalence<T> equivalence) {
      this.equivalence = equivalence;
    }

    @Override
    public int computeHashCode(T object) {
      return equivalence.hash(object);
    }

    @Override
    public boolean equals(T object1, T object2) {
      return equivalence.equivalent(object1, object2);
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof EquivalenceStrategy<?> that && equivalence.equals(that.equivalence);
    }

    @Override
    public int hashCode() {
      return equivalence.hashCode();
    }

    @Override
    public String toString() {
      return "HashingStrategy(" + equivalence + ")";
    }
  }

  /** A hashing strategy seen as an equivalence; serializable, as hashing strategies are. */
  static final class StrategyEquivalence<T> extends Equivalence<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    final HashingStrategy<? super T> strategy;

    StrategyEquivalence(HashingStrategy<? super T> strategy) {
      this.strategy = strategy;
    }

    @Override
    protected boolean doEquivalent(T a, T b) {
      return strategy.equals(a, b);
    }

    @Override
    protected int doHash(T t) {
      return strategy.computeHashCode(t);
    }

    @Override
    public boolean equals(Object object) {
      return object instanceof StrategyEquivalence<?> other && strategy.equals(other.strategy);
    }

    @Override
    public int hashCode() {
      return strategy.hashCode();
    }

    @Override
    public String toString() {
      return "Equivalence(" + strategy + ")";
    }
  }
}
