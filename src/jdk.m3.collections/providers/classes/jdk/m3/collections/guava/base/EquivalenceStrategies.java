/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections.guava.base;

import static jdk.m3.collections.guava.base.Preconditions.checkNotNull;

import java.io.Serializable;
import jdk.m3.collections.eclipse.api.block.HashingStrategy;

/**
 * The adapters between {@link Equivalence} and {@link HashingStrategy}, the equality strategy the
 * collections of the module share. Each adapter gives back the object it adapts when it is
 * converted again.
 */
final class EquivalenceStrategies {

  private EquivalenceStrategies() {}

  static <T> HashingStrategy<T> toStrategy(Equivalence<T> equivalence) {
    if (equivalence instanceof StrategyEquivalence<T> adapter) {
      return adapter.strategy;
    }
    return new EquivalenceStrategy<>(equivalence);
  }

  static <T> Equivalence<T> toEquivalence(HashingStrategy<T> strategy) {
    checkNotNull(strategy, "strategy");
    if (strategy instanceof EquivalenceStrategy<T> adapter) {
      return adapter.equivalence;
    }
    return new StrategyEquivalence<>(strategy);
  }

  /**
   * An equivalence seen as a hashing strategy. A hashing strategy is serializable; this one is when
   * its equivalence is.
   */
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
    public String toString() {
      return "HashingStrategy(" + equivalence + ")";
    }
  }

  /** A hashing strategy seen as an equivalence; serializable, as hashing strategies are. */
  static final class StrategyEquivalence<T> extends Equivalence<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    final HashingStrategy<T> strategy;

    StrategyEquivalence(HashingStrategy<T> strategy) {
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
