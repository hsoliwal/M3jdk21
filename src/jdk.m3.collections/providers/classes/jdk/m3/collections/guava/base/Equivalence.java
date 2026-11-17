/*
 * Copyright (C) 2010 The Guava Authors
 * Modifications Copyright 2026 Hitesh Soliwal and contributors
 * Modified to add paired Java functional equality and hashing strategies.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

// Modified 2026 by Synexia contributors: conversions to and from HashingStrategy.
package jdk.m3.collections.guava.base;

import static jdk.m3.collections.guava.base.Preconditions.checkNotNull;

import jdk.m3.collections.guava.annotations.GwtCompatible;
import jdk.m3.collections.guava.annotations.GwtIncompatible;
import jdk.m3.collections.guava.annotations.J2ktIncompatible;
import java.io.Serializable;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.ToIntFunction;
import jdk.m3.collections.eclipse.api.block.HashingStrategy;

/**
 * A strategy for determining whether two instances are considered equivalent, and for computing
 * hash codes in a manner consistent with that equivalence. Two examples of equivalences are the
 * {@linkplain #identity() identity equivalence} and the {@linkplain #equals "equals" equivalence}.
 *
 * @author Bob Lee
 * @author Ben Yu
 * @author Gregory Kick
 * @since 10.0 (<a href="https://github.com/google/guava/wiki/Compatibility">mostly
 *     source-compatible</a> since 4.0)
 */
@GwtCompatible
/*
 * The type parameter is <T> rather than <T extends @Nullable> so that we can use T in the
 * doEquivalent and doHash methods to indicate that the parameter cannot be null.
 */
@SuppressWarnings({"UngroupedOverloads", "serial"})
public abstract class Equivalence<T> implements BiPredicate<T, T> {
  /** Constructor for use by subclasses. */
  protected Equivalence() {}

  /**
   * Returns {@code true} if the given objects are considered equivalent.
   *
   * <p>This method describes an <i>equivalence relation</i> on object references, meaning that for
   * all references {@code x}, {@code y}, and {@code z} (any of which may be null):
   *
   * <ul>
   *   <li>{@code equivalent(x, x)} is true (<i>reflexive</i> property)
   *   <li>{@code equivalent(x, y)} and {@code equivalent(y, x)} each return the same result
   *       (<i>symmetric</i> property)
   *   <li>If {@code equivalent(x, y)} and {@code equivalent(y, z)} are both true, then {@code
   *       equivalent(x, z)} is also true (<i>transitive</i> property)
   * </ul>
   *
   * <p>Note that all calls to {@code equivalent(x, y)} are expected to return the same result as
   * long as neither {@code x} nor {@code y} is modified.
   */
  public final boolean equivalent(T a, T b) {
    if (a == b) {
      return true;
    }
    if (a == null || b == null) {
      return false;
    }
    return doEquivalent(a, b);
  }

  /**
   * @deprecated Provided only to satisfy the {@link BiPredicate} interface; use {@link #equivalent}
   *     instead.
   * @since 21.0
   */
  @Deprecated
  @Override
  public final boolean test(T t, T u) {
    return equivalent(t, u);
  }

  /**
   * Implemented by the user to determine whether {@code a} and {@code b} are considered equivalent,
   * subject to the requirements specified in {@link #equivalent}.
   *
   * <p>This method should not be called except by {@link #equivalent}. When {@link #equivalent}
   * calls this method, {@code a} and {@code b} are guaranteed to be distinct, non-null instances.
   *
   * @since 10.0 (previously, subclasses would override equivalent())
   */
  protected abstract boolean doEquivalent(T a, T b);

  /**
   * <i>May</i> return {@code true} if {@code object} is a {@code Equivalence} that behaves
   * identically to this equivalence.
   *
   * <p><b>Warning: do not depend</b> on the behavior of this method.
   *
   * <p>Historically, {@code Equivalence} instances in this library have implemented this method to
   * recognize certain cases where distinct {@code Equivalence} instances would in fact behave
   * identically. However, as code migrates to {@code java.util.function}, that behavior will
   * disappear. It is best not to depend on it.
   */
  @Override
  public boolean equals(Object object) {
    return super.equals(object);
  }

  /**
   * Returns a hash code for {@code t}.
   *
   * <p>The {@code hash} has the following properties:
   *
   * <ul>
   *   <li>It is <i>consistent</i>: for any reference {@code x}, multiple invocations of {@code
   *       hash(x}} consistently return the same value provided {@code x} remains unchanged
   *       according to the definition of the equivalence. The hash need not remain consistent from
   *       one execution of an application to another execution of the same application.
   *   <li>It is <i>distributable across equivalence</i>: for any references {@code x} and {@code
   *       y}, if {@code equivalent(x, y)}, then {@code hash(x) == hash(y)}. It is <i>not</i>
   *       necessary that the hash be distributable across <i>inequivalence</i>. If {@code
   *       equivalence(x, y)} is false, {@code hash(x) == hash(y)} may still be true.
   *   <li>{@code hash(null)} is {@code 0}.
   * </ul>
   */
  public final int hash(T t) {
    if (t == null) {
      return 0;
    }
    return doHash(t);
  }

  /**
   * Implemented by the user to return a hash code for {@code t}, subject to the requirements
   * specified in {@link #hash}.
   *
   * <p>This method should not be called except by {@link #hash}. When {@link #hash} calls this
   * method, {@code t} is guaranteed to be non-null.
   *
   * @since 10.0 (previously, subclasses would override hash())
   */
  protected abstract int doHash(T t);

  /**
   * Returns a new equivalence relation for {@code F} which evaluates equivalence by first applying
   * {@code function} to the argument, then evaluating using {@code this}. That is, for any pair of
   * non-null objects {@code x} and {@code y}, {@code equivalence.onResultOf(function).equivalent(a,
   * b)} is true if and only if {@code equivalence.equivalent(function.apply(a), function.apply(b))}
   * is true.
   *
   * <p>For example:
   *
   * {@snippet :
   * Equivalence<Person> SAME_AGE = Equivalence.equals().onResultOf(GET_PERSON_AGE);
   * }
   *
   * <p>{@code function} will never be invoked with a null value.
   *
   * <p>Note that {@code function} must be consistent according to {@code this} equivalence
   * relation. That is, invoking {@link Function#apply} multiple times for a given value must return
   * equivalent results. For example, {@code
   * Equivalence.identity().onResultOf(Functions.toStringFunction())} is broken because it's not
   * guaranteed that {@link Object#toString}) always returns the same string instance.
   *
   * @since 10.0
   */
  public final <F> Equivalence<F> onResultOf(Function<? super F, ? extends T> function) {
    return new FunctionalEquivalence<>(function, this);
  }

  /**
   * Returns a wrapper of {@code reference} that implements {@link Wrapper#equals(Object)
   * Object.equals()} such that {@code wrap(a).equals(wrap(b))} if and only if {@code equivalent(a,
   * b)}.
   *
   * <p>The returned object is serializable if both this {@code Equivalence} and {@code reference}
   * are serializable (including when {@code reference} is null).
   *
   * @since 10.0
   */
  public final <S extends T> Wrapper<S> wrap(@ParametricNullness S reference) {
    return new Wrapper<>(this, reference);
  }

  /**
   * Wraps an object so that {@link #equals(Object)} and {@link #hashCode()} delegate to an {@link
   * Equivalence}.
   *
   * <p>For example, given an {@link Equivalence} for {@link String strings} named {@code equiv}
   * that tests equivalence using their lengths:
   *
   * {@snippet :
   * equiv.wrap("a").equals(equiv.wrap("b")) // true
   * equiv.wrap("a").equals(equiv.wrap("hello")) // false
   * }
   *
   * <p>Note in particular that an equivalence wrapper is never equal to the object it wraps.
   *
   * {@snippet :
   * equiv.wrap(obj).equals(obj) // always false
   * }
   *
   * @since 10.0
   */
  public static final class Wrapper<T extends Object> implements Serializable {
    /*
     * Equivalence's type argument is always non-nullable: Equivalence<Number>, never
     * Equivalence<@Nullable Number>. That can still produce wrappers of various types --
     * Wrapper<Number>, Wrapper<Integer>, Wrapper<@Nullable Integer>, etc. If we used just
     * Equivalence<? super T> below, no type could satisfy both that bound and T's own
     * bound. With this type, they have some overlap: in our example, Equivalence<Number>
     * and Equivalence<Object>.
     */
    private final Equivalence<? super T> equivalence;

    @ParametricNullness private final T reference;

    private Wrapper(Equivalence<? super T> equivalence, @ParametricNullness T reference) {
      this.equivalence = checkNotNull(equivalence);
      this.reference = reference;
    }

    /** Returns the (possibly null) reference wrapped by this instance. */
    @ParametricNullness
    public T get() {
      return reference;
    }

    /**
     * Returns {@code true} if {@link Equivalence#equivalent(Object, Object)} applied to the wrapped
     * references is {@code true} and both wrappers use the {@link Object#equals(Object) same}
     * equivalence.
     */
    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj instanceof Wrapper) {
        Wrapper<?> that = (Wrapper<?>) obj; // note: not necessarily a Wrapper<T>

        if (this.equivalence.equals(that.equivalence)) {
          /*
           * We'll accept that as sufficient "proof" that either equivalence should be able to
           * handle either reference, so it's safe to circumvent compile-time type checking.
           */
          @SuppressWarnings("unchecked")
          Equivalence<Object> equivalence = (Equivalence<Object>) this.equivalence;
          return equivalence.equivalent(this.reference, that.reference);
        }
      }
      return false;
    }

    /** Returns the result of {@link Equivalence#hash(Object)} applied to the wrapped reference. */
    @Override
    public int hashCode() {
      return equivalence.hash(reference);
    }

    /**
     * Returns a string representation for this equivalence wrapper. The form of this string
     * representation is not specified.
     */
    @Override
    public String toString() {
      return equivalence + ".wrap(" + reference + ")";
    }

    @GwtIncompatible @J2ktIncompatible private static final long serialVersionUID = 0;
  }

  /**
   * Returns an equivalence over iterables based on the equivalence of their elements. More
   * specifically, two iterables are considered equivalent if they both contain the same number of
   * elements, and each pair of corresponding elements is equivalent according to {@code this}. Null
   * iterables are equivalent to one another.
   *
   * <p>Note that this method performs a similar function for equivalences as {@link
   * jdk.m3.collections.guava.collect.Ordering#lexicographical} does for orderings.
   *
   * <p>The returned object is serializable if this object is serializable.
   *
   * @since 10.0
   */
  public final <S extends T> Equivalence<Iterable<S>> pairwise() {
    // Ideally, the returned equivalence would support Iterable<? extends T>. However,
    // the need for this is so rare that it's not worth making callers deal with the ugly wildcard.
    return new PairwiseEquivalence<>(this);
  }

  /**
   * Returns a predicate that evaluates to true if and only if the input is equivalent to {@code
   * target} according to this equivalence relation.
   *
   * @since 10.0
   */
  public final Predicate<T> equivalentTo(T target) {
    return new EquivalentToPredicate<>(this, target);
  }

  private static final class EquivalentToPredicate<T>
      implements Predicate<T>, Serializable {

    private final Equivalence<T> equivalence;
    private final T target;

    EquivalentToPredicate(Equivalence<T> equivalence, T target) {
      this.equivalence = checkNotNull(equivalence);
      this.target = target;
    }

    @Override
    public boolean apply(T input) {
      return equivalence.equivalent(input, target);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj instanceof EquivalentToPredicate) {
        EquivalentToPredicate<?> that = (EquivalentToPredicate<?>) obj;
        return equivalence.equals(that.equivalence) && Objects.equals(target, that.target);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hash(equivalence, target);
    }

    @Override
    public String toString() {
      return equivalence + ".equivalentTo(" + target + ")";
    }

    @GwtIncompatible @J2ktIncompatible private static final long serialVersionUID = 0;
  }

  /**
   * Returns an equivalence that delegates to {@link Object#equals} and {@link Object#hashCode}.
   * {@link Equivalence#equivalent} returns {@code true} if both values are null, or if neither
   * value is null and {@link Object#equals} returns {@code true}. {@link Equivalence#hash} returns
   * {@code 0} if passed a null value.
   *
   * @since 13.0
   * @since 8.0 (in Equivalences with null-friendly behavior)
   * @since 4.0 (in Equivalences)
   */
  public static Equivalence<Object> equals() {
    return Equals.INSTANCE;
  }

  /**
   * Returns an equivalence that uses {@code ==} to compare values and {@link
   * System#identityHashCode(Object)} to compute the hash code. {@link Equivalence#equivalent}
   * returns {@code true} if {@code a == b}, including in the case that a and b are both null.
   *
   * @since 13.0
   * @since 4.0 (in Equivalences)
   */
  public static Equivalence<Object> identity() {
    return Identity.INSTANCE;
  }

  /**
   * Returns this equivalence as a {@link HashingStrategy}, the equality strategy the collections of
   * the module {@code jdk.m3.collections} share: its {@code equals} is {@link #equivalent} and its
   * {@code computeHashCode} is {@link #hash}, so it handles nulls as this equivalence does, except
   * for an equivalence made by {@link #fromHashingStrategy(HashingStrategy)}: that one returns the
   * strategy it was made from, which handles nulls as it always did.
   *
   * <p>The strategy is serializable when this equivalence is.
   *
   * @return this equivalence as a hashing strategy
   */
  public HashingStrategy<T> asHashingStrategy() {
    return EquivalenceStrategies.toStrategy(this);
  }

  /**
   * Returns the {@link HashingStrategy} as an equivalence: {@link #equivalent} is its {@code
   * equals} and {@link #hash} is its {@code computeHashCode}, called with non-null values only (an
   * equivalence handles nulls itself, as {@link #equivalent} and {@link #hash} describe).
   *
   * <p>A strategy made by {@link #asHashingStrategy()} gives back the equivalence it was made from.
   *
   * @param <T> the type of the values the equivalence evaluates
   * @param strategy the hashing strategy, of {@code T} or a supertype
   * @return the strategy as an equivalence
   * @throws NullPointerException if the strategy is null
   */
  public static <T> Equivalence<T> fromHashingStrategy(HashingStrategy<? super T> strategy) {
    return EquivalenceStrategies.toEquivalence(strategy);
  }

  /**
   * Returns a new equivalence whose non-null comparisons and hashes are supplied by the given
   * functions.
   *
   * <p>The {@code equality} predicate must define a stable equivalence relation, and equivalent
   * values must receive the same result from {@code hashFunction}. Unequal values may have the
   * same hash. These laws are the caller's responsibility; this factory does not verify them.
   *
   * <p>The inherited reference and null rules still apply: identical references are equivalent,
   * one null and one non-null reference are not equivalent, and the hash of null is zero. Neither
   * callback is invoked for these cases. For distinct non-null references, {@code equality} is
   * invoked exactly once in argument order; for a non-null hash input, {@code hashFunction} is
   * invoked exactly once. Callback exceptions propagate unchanged. Results are not cached.
   *
   * <p>Each call creates a distinct strategy with identity-based strategy equality, even when
   * the same callbacks are supplied. Reuse one strategy when creating wrappers that should
   * compare equal. The returned strategy is not serializable. Its thread safety and stability
   * depend on the supplied callbacks and their captured state.
   *
   * <p>For example, the following groups strings by length, including through {@link #wrap}:
   *
   * {@snippet :
   * Equivalence<String> sameLength = Equivalence.fromFunctions(
   *     (left, right) -> left.length() == right.length(), String::length);
   * }
   *
   * @param <T> the type of values to compare
   * @param equality the predicate for distinct, non-null references
   * @param hashFunction the hash function for non-null references
   * @return a new strategy using the supplied functions
   * @throws NullPointerException if either callback is null; {@code equality} is checked first
   * @since M3JDK 21
   */
  public static <T> Equivalence<T> fromFunctions(
      BiPredicate<? super T, ? super T> equality, ToIntFunction<? super T> hashFunction) {
    return new CallbackEquivalence<>(
        Objects.requireNonNull(equality, "equality"),
        Objects.requireNonNull(hashFunction, "hashFunction"));
  }

  /** Holds only the explicit callback pair; the superclass owns null and reference handling. */
  private static final class CallbackEquivalence<T> extends Equivalence<T> {
    private final BiPredicate<? super T, ? super T> equality;
    private final ToIntFunction<? super T> hashFunction;

    private CallbackEquivalence(
        BiPredicate<? super T, ? super T> equality, ToIntFunction<? super T> hashFunction) {
      this.equality = equality;
      this.hashFunction = hashFunction;
    }

    @Override
    protected boolean doEquivalent(T a, T b) {
      return equality.test(a, b);
    }

    @Override
    protected int doHash(T value) {
      return hashFunction.applyAsInt(value);
    }
  }

  static final class Equals extends Equivalence<Object> implements Serializable {

    static final Equals INSTANCE = new Equals();

    @Override
    protected boolean doEquivalent(Object a, Object b) {
      return a.equals(b);
    }

    @Override
    protected int doHash(Object o) {
      return o.hashCode();
    }

    private Object readResolve() {
      return INSTANCE;
    }

    @GwtIncompatible @J2ktIncompatible private static final long serialVersionUID = 1;
  }

  static final class Identity extends Equivalence<Object> implements Serializable {

    static final Identity INSTANCE = new Identity();

    @Override
    protected boolean doEquivalent(Object a, Object b) {
      return false;
    }

    @Override
    protected int doHash(Object o) {
      return System.identityHashCode(o);
    }

    private Object readResolve() {
      return INSTANCE;
    }

    @GwtIncompatible @J2ktIncompatible private static final long serialVersionUID = 1;
  }
}
