/*
 * Copyright 2010 by Thomas Mauch
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
 *
 * $Id$
 */
package jdk.m3.collections.brownies.helper;

/**
 * The class CapacityHelper provides a method for calculating a new capacity.
 *
 * @author Thomas Mauch
 */
@SuppressWarnings("missing-explicit-ctor")
public class CapacityHelper {

    /**
     * Calculate new capacity.
     * The capacity will not shrink, so the returned capacity can be equal to oldCapacity.
     *
     * @param minCapacity the desired minimum capacity
     * @param oldCapacity the old capacity in use
     * @param defaultCapacity the default capacity which will never be undercut
     * @return  the new capacity to use
     */
    public static int calculateCapacity(int minCapacity, int oldCapacity, int defaultCapacity) {
        // Note: Same behavior as in ArrayList.ensureCapacity()
        if (minCapacity <= oldCapacity) {
            return oldCapacity; // do not shrink
        }

        minCapacity = Math.max(minCapacity, defaultCapacity);
        int newCapacity = oldCapacity + (oldCapacity >> 1);
        if (newCapacity < minCapacity) {
            newCapacity = minCapacity;
        }

        return newCapacity;
    }

}
