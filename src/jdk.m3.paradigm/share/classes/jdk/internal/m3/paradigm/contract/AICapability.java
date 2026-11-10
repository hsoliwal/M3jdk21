/* ******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
/*
 * Copyright 2026 Synexia <hsoliwal@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jdk.internal.m3.paradigm.contract;

/**
 * AICapability - Closed enum of capabilities that implementations may require.
 *
 * <h2>AnOP Principle: Capabilities, Not Libraries</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  THE INVARIANT (NON-NEGOTIABLE):                                          ║
 * ╠═══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                           ║
 * ║  ✅ Capability = strategy preference ("needs immutable collections")      ║
 * ║  ❌ Library    = framework coupling ("use Guava")                         ║
 * ║                                                                           ║
 * ║  Hints may influence HOW an operation is implemented,                     ║
 * ║  but never WHAT operation is allowed, and never WHETHER it is allowed.    ║
 * ║                                                                           ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Rules</h2>
 * <ul>
 * <li>Closed enum - no runtime extension</li>
 * <li>Advisory only - may be ignored</li>
 * <li>Never expands authority</li>
 * <li>If unavailable, fallback (no failure)</li>
 * <li>Hints never add dependencies - if not on classpath, it doesn't exist</li>
 * </ul>
 *
 * @since IOP 1.0 - AnOP Capabilities
 * @see AICapabilities
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AICapability.java}
 * blob {@code 1629ff5b8e3cb1094669b201cf8a34c387ef2e74} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public enum AICapability {

    // --- COLLECTIONS ---

    /** Needs immutable collections (JDK or Guava-style). */
    IMMUTABLE_COLLECTIONS,

    /** Needs concurrent/thread-safe collections. */
    CONCURRENT_COLLECTIONS,

    /** Needs primitive collections (avoid boxing). */
    PRIMITIVE_COLLECTIONS,

    /** Needs ordered/sorted collections. */
    SORTED_COLLECTIONS,

    // --- PROCESSING ---

    /** Needs stream processing (java.util.stream or similar). */
    STREAM_PROCESSING,

    /** Needs parallel processing capability. */
    PARALLEL_PROCESSING,

    /** Needs async/non-blocking execution. */
    ASYNC_EXECUTION,

    /** Needs virtual thread support (Java 21+). */
    VIRTUAL_THREADS,

    // --- SERIALIZATION ---

    /** Needs JSON serialization/deserialization. */
    JSON_SERIALIZATION,

    /** Needs binary serialization (Protobuf, Avro, etc.). */
    BINARY_SERIALIZATION,

    /** Needs XML serialization. */
    XML_SERIALIZATION,

    /** Needs CSV parsing. */
    CSV_PARSING,

    // --- I/O ---

    /** Needs non-blocking I/O. */
    IO_NON_BLOCKING,

    /** Needs file system operations. */
    IO_FILE_SYSTEM,

    /** Needs network operations. */
    IO_NETWORK,

    /** Needs memory-mapped files. */
    IO_MEMORY_MAPPED,

    // --- CACHING ---

    /** Needs local caching capability. */
    CACHING,

    /** Needs TTL-based expiration. */
    CACHING_TTL,

    /** Needs LRU eviction. */
    CACHING_LRU,

    // --- SECURITY ---

    /** Needs cryptography (hashing, encryption). */
    CRYPTOGRAPHY,

    /** Needs input validation. */
    VALIDATION,

    /** Needs sanitization (XSS, SQL injection prevention). */
    SANITIZATION,

    // --- TEXT PROCESSING ---

    /** Needs regex processing. */
    REGEX,

    /** Needs string manipulation utilities. */
    STRING_UTILS,

    /** Needs text normalization (Unicode). */
    TEXT_NORMALIZATION,

    // --- DATE/TIME ---

    /** Needs date/time manipulation. */
    DATE_TIME,

    /** Needs timezone handling. */
    TIMEZONE,

    /** Needs duration/period calculations. */
    DURATION,

    // --- MATH ---

    /** Needs arbitrary precision math (BigDecimal/BigInteger). */
    ARBITRARY_PRECISION,

    /** Needs statistical operations. */
    STATISTICS,

    /** Needs random number generation (secure). */
    SECURE_RANDOM,

    // --- OBSERVABILITY ---

    /** Needs logging capability. */
    LOGGING,

    /** Needs metrics collection. */
    METRICS,

    /** Needs tracing/spans. */
    TRACING,

    // --- RESILIENCE ---

    /** Needs retry capability. */
    RETRY,

    /** Needs circuit breaker. */
    CIRCUIT_BREAKER,

    /** Needs timeout handling. */
    TIMEOUT,

    /** Needs rate limiting. */
    RATE_LIMITING
}
