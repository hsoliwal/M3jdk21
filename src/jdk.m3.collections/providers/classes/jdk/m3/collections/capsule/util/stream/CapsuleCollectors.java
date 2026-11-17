/* *
 * Copyright (c) Michael Steindorfer <Centrum Wiskunde & Informatica> and Contributors.
 * All rights reserved.
 *
 * This file is licensed under the BSD 2-Clause License, which accompanies this project
 * and is available under https://opensource.org/licenses/BSD-2-Clause.
 */
package jdk.m3.collections.capsule.util.stream;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import jdk.m3.collections.capsule.SetMultimap;

@SuppressWarnings("missing-explicit-ctor")
public class CapsuleCollectors {

  public static final Set<Collector.Characteristics> UNORDERED =
      Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.UNORDERED));

  public static <T> Collector<T, ?, jdk.m3.collections.capsule.Set.Immutable<T>> toSet() {
    return new DefaultCollector<>(
        (Supplier<jdk.m3.collections.capsule.Set.Transient<T>>) jdk.m3.collections.capsule.Set.Transient::of,
        jdk.m3.collections.capsule.Set.Transient::__insert, (left, right) -> {
      left.__insertAll(right);
      return left;
    }, jdk.m3.collections.capsule.Set.Transient::freeze, UNORDERED);
  }

//  public static <T, K, V> Collector<T, ?, jdk.m3.collections.capsule.Map.Immutable<K, V>> toMap(
//      Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
//
//    /** extract key/value from type {@code T} and insert into multimap */
//    final BiConsumer<jdk.m3.collections.capsule.Map.Transient<K, V>, T> accumulator =
//        (map, element) -> map.__put(keyMapper.apply(element), valueMapper.apply(element));
//
//    return new DefaultCollector<>(
//        jdk.m3.collections.capsule.Map::of,
//        accumulator,
//        (left, right) -> {
//          left.union(right);
//          return left;
//        }, jdk.m3.collections.capsule.Map.Transient::freeze, UNORDERED);
//  }

  public static <T, K, V> Collector<T, ?, SetMultimap.Immutable<K, V>> toSetMultimap(
      Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {

    /** extract key/value from type {@code T} and insert into multimap */
    final BiConsumer<SetMultimap.Transient<K, V>, T> accumulator =
        (map, element) -> map.__insert(keyMapper.apply(element), valueMapper.apply(element));

    return new DefaultCollector<>(
        SetMultimap.Transient::of,
        accumulator,
        (left, right) -> {
          left.union(right);
          return left;
        }, SetMultimap.Transient::freeze, UNORDERED);
  }

}
