/*
 * Copyright (C) 2007 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jdk.m3.collections.guava.collect;

import static jdk.m3.collections.guava.base.Preconditions.checkNotNull;

import jdk.m3.collections.guava.annotations.GwtCompatible;
import jdk.m3.collections.guava.annotations.GwtIncompatible;
import jdk.m3.collections.guava.annotations.J2ktIncompatible;
import java.io.Serializable;

/** An ordering that uses the natural order of the values. */
@GwtCompatible
final class NaturalOrdering extends Ordering<Comparable<?>> implements Serializable {
  static final NaturalOrdering INSTANCE = new NaturalOrdering();

  /*
   * TODO(b/287198172): Consider eagerly initializing these, maybe making them static. But that
   * would lead to at least slighly more work at startup time.
   */
  private transient Ordering<Comparable<?>> nullsFirst;
  private transient Ordering<Comparable<?>> nullsLast;

  @Override
  @SuppressWarnings("unchecked") // TODO(kevinb): the right way to explain this??
  public int compare(Comparable<?> left, Comparable<?> right) {
    checkNotNull(left); // for GWT
    checkNotNull(right);
    return ((Comparable<Object>) left).compareTo(right);
  }

  @Override
  @SuppressWarnings("unchecked") // TODO(kevinb): the right way to explain this??
  public <S extends Comparable<?>> Ordering<S> nullsFirst() {
    Ordering<Comparable<?>> result = nullsFirst;
    if (result == null) {
      result = nullsFirst = super.<Comparable<?>>nullsFirst();
    }
    return (Ordering<S>) result;
  }

  @Override
  @SuppressWarnings("unchecked") // TODO(kevinb): the right way to explain this??
  public <S extends Comparable<?>> Ordering<S> nullsLast() {
    Ordering<Comparable<?>> result = nullsLast;
    if (result == null) {
      result = nullsLast = super.<Comparable<?>>nullsLast();
    }
    return (Ordering<S>) result;
  }

  @Override
  @SuppressWarnings("unchecked") // TODO(kevinb): the right way to explain this??
  public <S extends Comparable<?>> Ordering<S> reverse() {
    return (Ordering<S>) ReverseNaturalOrdering.INSTANCE;
  }

  // preserving singleton-ness gives equals()/hashCode() for free
  private Object readResolve() {
    return INSTANCE;
  }

  @Override
  public String toString() {
    return "Ordering.natural()";
  }

  private NaturalOrdering() {}

  @GwtIncompatible @J2ktIncompatible private static final long serialVersionUID = 0;
}
