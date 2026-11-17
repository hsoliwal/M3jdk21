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
 * Concurrent counters for when all you need is a number going up. The implementations here stripe across cache lines to
 * keep incrementing threads off each other's toes, trading a more expensive {@code get} for a much cheaper
 * {@code inc}.
 * <p>
 * Use {@link jdk.m3.collections.jctools.counters.CountersFactory} to get an implementation suited to the running JVM rather than
 * picking one directly.
 */
package jdk.m3.collections.jctools.counters;
