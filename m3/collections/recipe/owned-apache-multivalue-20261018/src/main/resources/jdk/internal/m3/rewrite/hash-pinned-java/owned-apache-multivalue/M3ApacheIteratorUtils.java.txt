// Modified 2026 by Hitesh Soliwal and Synexia contributors: owned M3 array storage, pinned bounded helper closure and documented query-contract repairs.
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.m3.collections.apache;
import java.util.*;
import org.apache.commons.collections4.Transformer;
@SuppressWarnings("deprecation") // Bounded donor Transformer atom.
final class M3ApacheIteratorUtils {
static final Iterator<Object> EMPTY_ITERATOR = M3ApacheEmptyIterator.emptyIterator();
public static <I, O> Iterator<O> transformedIterator(final Iterator<? extends I> iterator,
            final Transformer<? super I, ? extends O> transformer) {

        Objects.requireNonNull(iterator, "iterator");
        Objects.requireNonNull(transformer, "transformer");
        return new M3ApacheTransformIterator<>(iterator, transformer);
    }
}
