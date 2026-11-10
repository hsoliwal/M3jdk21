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
package jdk.internal.m3.paradigm.contract;

/**
 * IAICapability - Closed enum of capabilities that implementations may require.
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
 * @see IAICapabilities
 * @m3.pattern synexia:aiop
 * @m3.algorithm Enumerates the IAICapability choices described as Closed vocabulary of requested implementation capabilities; tokens grant no access and supply no implementation. Selection records an annotation value; execution remains the consumer responsibility.
 * @m3.dataStructures Enum containing 38 constants in the existing source order.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAICapability.java}
 * blob {@code 9b975401e4498f32f6c3d4f3238c0ca937bb9046} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public enum IAICapability {

    // --- COLLECTIONS ---

    /** Needs immutable collections (JDK or Guava-style).
     * @m3.pattern synexia:aiop
     * @m3.algorithm Names the IAICapability#IMMUTABLE_COLLECTIONS policy choice: Needs immutable collections (JDK or Guava-style). This enum token does not implement the selected runtime behavior.
     * @m3.dataStructures Existing constant of IAICapability; its declared order and identity are preserved.
     */
    IMMUTABLE_COLLECTIONS,

    /** Needs concurrent/thread-safe collections.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Names the IAICapability#CONCURRENT_COLLECTIONS policy choice: Needs concurrent/thread-safe collections. This enum token does not implement the selected runtime behavior.
     * @m3.dataStructures Existing constant of IAICapability; its declared order and identity are preserved.
     */
    CONCURRENT_COLLECTIONS,

    /** Needs primitive collections (avoid boxing).
     * @m3.pattern synexia:aiop
     * @m3.algorithm Names the IAICapability#PRIMITIVE_COLLECTIONS policy choice: Needs primitive collections (avoid boxing). This enum token does not implement the selected runtime behavior.
     * @m3.dataStructures Existing constant of IAICapability; its declared order and identity are preserved.
     */
    PRIMITIVE_COLLECTIONS,

    /** Needs ordered/sorted collections.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Names the IAICapability#SORTED_COLLECTIONS policy choice: Needs ordered/sorted collections. This enum token does not implement the selected runtime behavior.
     * @m3.dataStructures Existing constant of IAICapability; its declared order and identity are preserved.
     */
    SORTED_COLLECTIONS,

    // --- PROCESSING ---

    /** Needs stream processing (java.util.stream or similar).
     * @m3.pattern synexia:aiop
     * @m3.algorithm Names the IAICapability#STREAM_PROCESSING policy choice: Needs stream processing (java.util.stream or similar). This enum token does not implement the selected runtime behavior.
     * @m3.dataStructures Existing constant of IAICapability; its declared order and identity are preserved.
     */
    STREAM_PROCESSING,

    /** Needs parallel processing capability.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Names the IAICapability#PARALLEL_PROCESSING policy choice: Needs parallel processing capability. This enum token does not implement the selected runtime behavior.
     * @m3.dataStructures Existing constant of IAICapability; its declared order and identity are preserved.
     */
    PARALLEL_PROCESSING,

    /** Needs async/non-blocking execution.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Names the IAICapability#ASYNC_EXECUTION policy choice: Needs async/non-blocking execution. This enum token does not implement the selected runtime behavior.
     * @m3.dataStructures Existing constant of IAICapability; its declared order and identity are preserved.
     */
    ASYNC_EXECUTION,

    /** Needs virtual thread support (Java 21+).
     * @m3.pattern synexia:aiop
     * @m3.algorithm Names the IAICapability#VIRTUAL_THREADS policy choice: Needs virtual thread support (Java 21+). This enum token does not implement the selected runtime behavior.
     * @m3.dataStructures Existing constant of IAICapability; its declared order and identity are preserved.
     */
    VIRTUAL_THREADS,

    // --- SERIALIZATION ---

    /** Needs JSON serialization/deserialization.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Names the IAICapability#JSON_SERIALIZATION policy choice: Needs JSON serialization/deserialization. This enum token does not implement the selected runtime behavior.
     * @m3.dataStructures Existing constant of IAICapability; its declared order and identity are preserved.
     */
    JSON_SERIALIZATION,

    /** Needs binary serialization (Protobuf, Avro, etc.).
     * @m3.pattern synexia:aiop
     * @m3.algorithm Names the IAICapability#BINARY_SERIALIZATION policy choice: Needs binary serialization (Protobuf, Avro, etc.). This enum token does not implement the selected runtime behavior.
     * @m3.dataStructures Existing constant of IAICapability; its declared order and identity are preserved.
     */
    BINARY_SERIALIZATION,

    /** Needs XML serialization.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Names the IAICapability#XML_SERIALIZATION policy choice: Needs XML serialization. This enum token does not implement the selected runtime behavior.
     * @m3.dataStructures Existing constant of IAICapability; its declared order and identity are preserved.
     */
    XML_SERIALIZATION,

    /** Needs CSV parsing.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Names the IAICapability#CSV_PARSING policy choice: Needs CSV parsing. This enum token does not implement the selected runtime behavior.
     * @m3.dataStructures Existing constant of IAICapability; its declared order and identity are preserved.
     */
    CSV_PARSING,

    // --- I/O ---

    /** Needs non-blocking I/O.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Names the IAICapability#IO_NON_BLOCKING policy choice: Needs non-blocking I/O. This enum token does not implement the selected runtime behavior.
     * @m3.dataStructures Existing constant of IAICapability; its declared order and identity are preserved.
     */
    IO_NON_BLOCKING,

    /** Needs file system operations.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Names the IAICapability#IO_FILE_SYSTEM policy choice: Needs file system operations. This enum token does not implement the selected runtime behavior.
     * @m3.dataStructures Existing constant of IAICapability; its declared order and identity are preserved.
     */
    IO_FILE_SYSTEM,

    /** Needs network operations.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Names the IAICapability#IO_NETWORK policy choice: Needs network operations. This enum token does not implement the selected runtime behavior.
     * @m3.dataStructures Existing constant of IAICapability; its declared order and identity are preserved.
     */
    IO_NETWORK,

    /** Needs memory-mapped files.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Names the IAICapability#IO_MEMORY_MAPPED policy choice: Needs memory-mapped files. This enum token does not implement the selected runtime behavior.
     * @m3.dataStructures Existing constant of IAICapability; its declared order and identity are preserved.
     */
    IO_MEMORY_MAPPED,

    // --- CACHING ---

    /** Needs local caching capability.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Names the IAICapability#CACHING policy choice: Needs local caching capability. This enum token does not implement the selected runtime behavior.
     * @m3.dataStructures Existing constant of IAICapability; its declared order and identity are preserved.
     */
    CACHING,

    /** Needs TTL-based expiration.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Names the IAICapability#CACHING_TTL policy choice: Needs TTL-based expiration. This enum token does not implement the selected runtime behavior.
     * @m3.dataStructures Existing constant of IAICapability; its declared order and identity are preserved.
     */
    CACHING_TTL,

    /** Needs LRU eviction.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Names the IAICapability#CACHING_LRU policy choice: Needs LRU eviction. This enum token does not implement the selected runtime behavior.
     * @m3.dataStructures Existing constant of IAICapability; its declared order and identity are preserved.
     */
    CACHING_LRU,

    // --- SECURITY ---

    /** Needs cryptography (hashing, encryption).
     * @m3.pattern synexia:aiop
     * @m3.algorithm Names the IAICapability#CRYPTOGRAPHY policy choice: Needs cryptography (hashing, encryption). This enum token does not implement the selected runtime behavior.
     * @m3.dataStructures Existing constant of IAICapability; its declared order and identity are preserved.
     */
    CRYPTOGRAPHY,

    /** Needs input validation.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Names the IAICapability#VALIDATION policy choice: Needs input validation. This enum token does not implement the selected runtime behavior.
     * @m3.dataStructures Existing constant of IAICapability; its declared order and identity are preserved.
     */
    VALIDATION,

    /** Needs sanitization (XSS, SQL injection prevention).
     * @m3.pattern synexia:aiop
     * @m3.algorithm Names the IAICapability#SANITIZATION policy choice: Needs sanitization (XSS, SQL injection prevention). This enum token does not implement the selected runtime behavior.
     * @m3.dataStructures Existing constant of IAICapability; its declared order and identity are preserved.
     */
    SANITIZATION,

    // --- TEXT PROCESSING ---

    /** Needs regex processing.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Names the IAICapability#REGEX policy choice: Needs regex processing. This enum token does not implement the selected runtime behavior.
     * @m3.dataStructures Existing constant of IAICapability; its declared order and identity are preserved.
     */
    REGEX,

    /** Needs string manipulation utilities.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Names the IAICapability#STRING_UTILS policy choice: Needs string manipulation utilities. This enum token does not implement the selected runtime behavior.
     * @m3.dataStructures Existing constant of IAICapability; its declared order and identity are preserved.
     */
    STRING_UTILS,

    /** Needs text normalization (Unicode).
     * @m3.pattern synexia:aiop
     * @m3.algorithm Names the IAICapability#TEXT_NORMALIZATION policy choice: Needs text normalization (Unicode). This enum token does not implement the selected runtime behavior.
     * @m3.dataStructures Existing constant of IAICapability; its declared order and identity are preserved.
     */
    TEXT_NORMALIZATION,

    // --- DATE/TIME ---

    /** Needs date/time manipulation.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Names the IAICapability#DATE_TIME policy choice: Needs date/time manipulation. This enum token does not implement the selected runtime behavior.
     * @m3.dataStructures Existing constant of IAICapability; its declared order and identity are preserved.
     */
    DATE_TIME,

    /** Needs timezone handling.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Names the IAICapability#TIMEZONE policy choice: Needs timezone handling. This enum token does not implement the selected runtime behavior.
     * @m3.dataStructures Existing constant of IAICapability; its declared order and identity are preserved.
     */
    TIMEZONE,

    /** Needs duration/period calculations.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Names the IAICapability#DURATION policy choice: Needs duration/period calculations. This enum token does not implement the selected runtime behavior.
     * @m3.dataStructures Existing constant of IAICapability; its declared order and identity are preserved.
     */
    DURATION,

    // --- MATH ---

    /** Needs arbitrary precision math (BigDecimal/BigInteger).
     * @m3.pattern synexia:aiop
     * @m3.algorithm Names the IAICapability#ARBITRARY_PRECISION policy choice: Needs arbitrary precision math (BigDecimal/BigInteger). This enum token does not implement the selected runtime behavior.
     * @m3.dataStructures Existing constant of IAICapability; its declared order and identity are preserved.
     */
    ARBITRARY_PRECISION,

    /** Needs statistical operations.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Names the IAICapability#STATISTICS policy choice: Needs statistical operations. This enum token does not implement the selected runtime behavior.
     * @m3.dataStructures Existing constant of IAICapability; its declared order and identity are preserved.
     */
    STATISTICS,

    /** Needs random number generation (secure).
     * @m3.pattern synexia:aiop
     * @m3.algorithm Names the IAICapability#SECURE_RANDOM policy choice: Needs random number generation (secure). This enum token does not implement the selected runtime behavior.
     * @m3.dataStructures Existing constant of IAICapability; its declared order and identity are preserved.
     */
    SECURE_RANDOM,

    // --- OBSERVABILITY ---

    /** Needs logging capability.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Names the IAICapability#LOGGING policy choice: Needs logging capability. This enum token does not implement the selected runtime behavior.
     * @m3.dataStructures Existing constant of IAICapability; its declared order and identity are preserved.
     */
    LOGGING,

    /** Needs metrics collection.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Names the IAICapability#METRICS policy choice: Needs metrics collection. This enum token does not implement the selected runtime behavior.
     * @m3.dataStructures Existing constant of IAICapability; its declared order and identity are preserved.
     */
    METRICS,

    /** Needs tracing/spans.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Names the IAICapability#TRACING policy choice: Needs tracing/spans. This enum token does not implement the selected runtime behavior.
     * @m3.dataStructures Existing constant of IAICapability; its declared order and identity are preserved.
     */
    TRACING,

    // --- RESILIENCE ---

    /** Needs retry capability.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Names the IAICapability#RETRY policy choice: Needs retry capability. This enum token does not implement the selected runtime behavior.
     * @m3.dataStructures Existing constant of IAICapability; its declared order and identity are preserved.
     */
    RETRY,

    /** Needs circuit breaker.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Names the IAICapability#CIRCUIT_BREAKER policy choice: Needs circuit breaker. This enum token does not implement the selected runtime behavior.
     * @m3.dataStructures Existing constant of IAICapability; its declared order and identity are preserved.
     */
    CIRCUIT_BREAKER,

    /** Needs timeout handling.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Names the IAICapability#TIMEOUT policy choice: Needs timeout handling. This enum token does not implement the selected runtime behavior.
     * @m3.dataStructures Existing constant of IAICapability; its declared order and identity are preserved.
     */
    TIMEOUT,

    /** Needs rate limiting.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Names the IAICapability#RATE_LIMITING policy choice: Needs rate limiting. This enum token does not implement the selected runtime behavior.
     * @m3.dataStructures Existing constant of IAICapability; its declared order and identity are preserved.
     */
    RATE_LIMITING
}
