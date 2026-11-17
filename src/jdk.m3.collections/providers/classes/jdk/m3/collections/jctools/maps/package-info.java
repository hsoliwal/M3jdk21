/*
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
/**
 * Lock free maps and sets, the bulk of which is Cliff Click's NonBlockingHashMap and its relatives. These offer the
 * same correctness properties as {@link java.util.concurrent.ConcurrentHashMap} while scaling substantially better for
 * high update rates - see the {@link jdk.m3.collections.jctools.maps.NonBlockingHashMap} class documentation for the details.
 * <ol>
 * <li>{@link jdk.m3.collections.jctools.maps.NonBlockingHashMap} - the general purpose lock free map.
 * <li>{@link jdk.m3.collections.jctools.maps.NonBlockingHashMapLong} - adds a primitive {@code long} keyed API
 * ({@code put(long, V)} and friends) next to the boxed {@code Map} view.
 * <li>{@link jdk.m3.collections.jctools.maps.NonBlockingIdentityHashMap} - reference equality on keys.
 * <li>{@link jdk.m3.collections.jctools.maps.NonBlockingHashSet} and {@link jdk.m3.collections.jctools.maps.NonBlockingSetInt} - the set flavours.
 * </ol>
 * Unlike the queues in {@link jdk.m3.collections.jctools.queues}, these implement the full {@link java.util.Map} and
 * {@link java.util.Set} contracts, iterators included.
 */
package jdk.m3.collections.jctools.maps;
