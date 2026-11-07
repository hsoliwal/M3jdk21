// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import jdk.m3.collections.M3LongLongConsumer;

/** Primitive-long bag/multiset contract. */
//FLLDCIM: RELOCATE - donor blob 26af98f01de2 at commit d5ad5090d87e (repository, path, URL and digests: m3/collections/recipe/four-library-bags-20261106/PROVENANCE.tsv); recipe m3/collections/recipe/four-library-bags-20261106/relocate.py v1
public interface M3PackedLongBagContract {
    /**
     * Adds one occurrence of a value.
     * @param value the value to add
     * @return the count of the value after the addition
     * @throws ArithmeticException if the total number of occurrences would overflow
     */
    long add(long value);

    /**
     * Adds a positive number of occurrences of a value.
     * @param value the value to add
     * @param occurrences the positive number of occurrences to add
     * @return the count of the value after the addition
     * @throws IllegalArgumentException if occurrences is not positive
     * @throws ArithmeticException if the total number of occurrences would overflow
     */
    long add(long value, long occurrences);

    /**
     * Returns the number of occurrences of a value.
     * @param value the value to count
     * @return the occurrence count, or zero if the value is absent
     */
    long count(long value);

    /**
     * Removes up to the requested number of occurrences of a value.
     * @param value the value to remove
     * @param occurrences the positive maximum number of occurrences to remove
     * @return the count of the value after the removal, or zero if it is absent
     * @throws IllegalArgumentException if occurrences is not positive
     */
    long remove(long value, long occurrences);

    /**
     * Removes all occurrences of a value.
     * @param value the value to remove
     * @return the number of occurrences removed, or zero if the value was absent
     */
    long removeAll(long value);

    /**
     * Tests whether a value has any occurrences in this bag.
     * @param value the value to test
     * @return whether the value has a positive occurrence count
     */
    boolean contains(long value);

    /**
     * Returns the number of distinct values in this bag.
     * @return the number of values with positive occurrence counts
     */
    int distinctSize();

    /**
     * Returns the total number of occurrences in this bag.
     * @return the sum of all occurrence counts
     */
    long size();

    /**
     * Tests whether this bag contains no occurrences.
     * @return whether the total number of occurrences is zero
     */
    boolean isEmpty();

    /**
     * Passes each distinct value and its occurrence count to the consumer.
     * <p>Visitation order and behavior when this bag is modified during
     * traversal are unspecified.
     * @param consumer the callback receiving the value as its first argument
     *                 and the occurrence count as its second argument
     * @throws NullPointerException if consumer is null
     */
    void forEachCount(M3LongLongConsumer consumer);

    /** Removes all occurrences from this bag. */
    void clear();
}
