/*
 * Copyright 2015 by Thomas Mauch
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
package jdk.m3.collections.brownies;

import java.util.Iterator;

/**
 * Iterator which prevents changes through the remove() method.
 *
 * @author Thomas Mauch
 */
public class ImmutableIterator<E> implements Iterator<E> {

    Iterator<E> iterator;

    /** Constructor */
    public ImmutableIterator(Iterator<E> iterator) {
        if (iterator == null) {
            throw new IllegalArgumentException("Iterator may not be null");
        }
        this.iterator = iterator;
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public E next() {
        return iterator.next();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

}