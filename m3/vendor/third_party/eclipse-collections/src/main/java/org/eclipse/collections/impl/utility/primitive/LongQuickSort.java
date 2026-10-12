/*
 * Copyright (c) 2022 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.utility.primitive;

import org.eclipse.collections.api.block.comparator.primitive.LongComparator;

/**
 * LongQuickSort is an implementation of the Quick Sort algorithm as described in Donald Knuth's TAOCP with some
 * optimizations. It supports indirect array sorting based on primitive comparators and/or key values extracted from
 * the array values if a sort order other thant the natural one of the array elements is required.
 *
 * This file was automatically generated from template file primitiveSort.stg.
 *
 */

public final class LongQuickSort
{
    private static final int SORT_SMALL_SIZE = 9;

    private LongQuickSort()
    {
    }

    public static void sort(long[] array, int left, int right, LongComparator comparator)
    {
        int size = right - left + 1;

        if (size <= LongQuickSort.SORT_SMALL_SIZE)
        {
            LongQuickSort.insertionSort(array, left, right, comparator);
        }
        else
        {
            // Initialize new stage
            int mid = (right - (right / 2)) + (left / 2);
            long leftVal = array[left];
            long rightVal = array[right];
            long midVal = array[mid];

            int swapIndex = -1;

            if (comparator.compare(leftVal, midVal) > 0 && comparator.compare(leftVal, rightVal) > 0)
            {
                swapIndex = (comparator.compare(midVal, rightVal) < 0) ? right : mid;
            }
            else if (comparator.compare(leftVal, midVal) < 0 && comparator.compare(leftVal, rightVal) < 0)
            {
                swapIndex = (comparator.compare(midVal, rightVal) < 0) ? mid : right;
            }

            if (swapIndex > 0)
            {
                swap(array, left, swapIndex);
            }

            long pivot = array[left];

            int i = left + 1;
            int j = right;

            while (i < j)
            {
                // Compare: Key(i) : Key, skip all which are <= pivot or until hit j
                while (comparator.compare(array[i], pivot) <= 0 && i < j)
                {
                    i++;
                }

                // Compare Key : Key(j), skip all which are > pivot or until hit i
                while (comparator.compare(pivot, array[j]) < 0 && j > i - 1)
                {
                    j--;
                }

                if (i < j)
                {
                    swap(array, i, j);
                }
                else
                {
                    swap(array, left, j);
                }
            }

            // Sort partitions, skipping sequences of elements equal to the pivot
            if (right > j + 1)
            {
                int from = j + 1;
                while (right > from && comparator.compare(pivot, array[from]) == 0)
                {
                    from++;
                }
                if (right > from)
                {
                    LongQuickSort.sort(array, from, right, comparator);
                }
            }

            if (left < j - 1)
            {
                int to = j - 1;
                while (to > left && comparator.compare(pivot, array[to]) == 0)
                {
                    to--;
                }
                if (left < to)
                {
                    LongQuickSort.sort(array, left, to, comparator);
                }
            }
        }
    }

    private static void insertionSort(long[] array, int left, int right, LongComparator comparator)
    {
        for (int j = left + 1; j <= right; j++)
        {
            if (comparator.compare(array[j - 1], array[j]) > 0)
            {
                long key = array[j];
                int i = j - 1;

                do
                {
                    array[i + 1] = array[i];
                    i--;
                }
                while (i > -1 && comparator.compare(key, array[i]) < 0);

                array[i + 1] = key;
            }
        }
    }

    private static void swap(long[] array, int i1, int i2)
    {
        long value = array[i1];
        array[i1] = array[i2];
        array[i2] = value;
    }
}
