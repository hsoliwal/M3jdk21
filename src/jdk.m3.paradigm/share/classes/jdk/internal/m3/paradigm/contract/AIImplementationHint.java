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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AIImplementationHint - Master AnOP annotation for implementation guidance.
 *
 * <h2>AnOP Advisory: Implementation Strategy</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  This is the PRIMARY annotation for guiding LLM code generation.         ║
 * ║  It covers: algorithms, data structures, performance, error handling,    ║
 * ║  concurrency, memory management, I/O strategies, and trade-offs.         ║
 * ║                                                                           ║
 * ║  CRITICAL: These are HINTS, not commands. The LLM uses judgment.         ║
 * ║  IOP (interfaces + JavaDoc) remains the AUTHORITY.                       ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 * {@code
 *
 * &#64;AIImplementationHint(algorithm = Algorithm.BINARY_SEARCH, timeComplexity = "O(log n)", spaceComplexity = "O(1)", threadSafety = ThreadSafety.IMMUTABLE)
 * int findIndex(List<Integer> sortedList, int target);
 *
 * &#64;AIImplementationHint(pattern = Pattern.BUILDER, style = Style.FLUENT, nullHandling = NullHandling.REJECT_EARLY, validation = Validation.FAIL_FAST)
 * class QueryBuilder {
 * }
 *
 * @AIImplementationHint(dataStructure = DataStructure.CONCURRENT_HASH_MAP, caching = Caching.LRU, maxSize = 10000, eviction = Eviction.SIZE_BASED)
 * class UserCache {
 * }
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Implementation Hints
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AIImplementationHint.java}
 * blob {@code 23ba43149412c6e4f5973a0e5146416b07d8cc4d} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.SOURCE)
@Documented
@Repeatable(AIImplementationHints.class)
public @interface AIImplementationHint {

    // --- ALGORITHM HINTS ---

    /**
     * Recommended algorithm.
     *
     * @return algorithm
     */
    Algorithm algorithm() default Algorithm.UNSPECIFIED;

    /**
     * Time complexity (Big-O notation). Examples: "O(1)", "O(log n)", "O(n)",
     * "O(n log n)", "O(n²)"
     *
     * @return time complexity
     */
    String timeComplexity() default "";

    /**
     * Space complexity (Big-O notation).
     *
     * @return space complexity
     */
    String spaceComplexity() default "";

    /**
     * Algorithm must be stable (preserve order of equal elements).
     *
     * @return true if stability required
     */
    boolean stable() default false;

    /**
     * Algorithm must be deterministic (same input = same output).
     *
     * @return true if deterministic required
     */
    boolean deterministic() default true;

    // --- DATA STRUCTURE HINTS ---

    /**
     * Recommended data structure.
     *
     * @return data structure
     */
    DataStructure dataStructure() default DataStructure.UNSPECIFIED;

    /**
     * Initial capacity hint.
     *
     * @return initial capacity
     */
    int initialCapacity() default -1;

    /**
     * Maximum size hint.
     *
     * @return max size
     */
    int maxSize() default -1;

    /**
     * Load factor for hash-based structures.
     *
     * @return load factor
     */
    float loadFactor() default 0.75f;

    // --- DESIGN PATTERN HINTS ---

    /**
     * Recommended design pattern.
     *
     * @return pattern
     */
    Pattern pattern() default Pattern.UNSPECIFIED;

    /**
     * Code style preference.
     *
     * @return style
     */
    Style style() default Style.STANDARD;

    // --- THREAD SAFETY & CONCURRENCY ---

    /**
     * Thread safety requirement.
     *
     * @return thread safety level
     */
    ThreadSafety threadSafety() default ThreadSafety.UNSPECIFIED;

    /**
     * Concurrency strategy.
     *
     * @return concurrency approach
     */
    Concurrency concurrency() default Concurrency.UNSPECIFIED;

    /**
     * Lock strategy if synchronization needed.
     *
     * @return lock strategy
     */
    LockStrategy lockStrategy() default LockStrategy.UNSPECIFIED;

    // --- ERROR HANDLING ---

    /**
     * Null handling strategy.
     *
     * @return null handling
     */
    NullHandling nullHandling() default NullHandling.UNSPECIFIED;

    /**
     * Validation strategy.
     *
     * @return validation approach
     */
    Validation validation() default Validation.UNSPECIFIED;

    /**
     * Error recovery strategy.
     *
     * @return error handling
     */
    ErrorHandling errorHandling() default ErrorHandling.UNSPECIFIED;

    // --- PERFORMANCE & MEMORY ---

    /**
     * Caching strategy.
     *
     * @return caching approach
     */
    Caching caching() default Caching.NONE;

    /**
     * Eviction policy for caches/pools.
     *
     * @return eviction policy
     */
    Eviction eviction() default Eviction.UNSPECIFIED;

    /**
     * Memory optimization strategy.
     *
     * @return memory strategy
     */
    MemoryStrategy memoryStrategy() default MemoryStrategy.UNSPECIFIED;

    /**
     * Lazy loading preference.
     *
     * @return true for lazy
     */
    boolean lazy() default false;

    /**
     * Pooling preference.
     *
     * @return true for pooling
     */
    boolean pooled() default false;

    /**
     * Interning preference (for identity/deduplication).
     *
     * @return true for interning
     */
    boolean interned() default false;

    // --- I/O HINTS ---

    /**
     * I/O strategy.
     *
     * @return I/O approach
     */
    IOStrategy ioStrategy() default IOStrategy.UNSPECIFIED;

    /**
     * Buffer size hint.
     *
     * @return buffer size bytes
     */
    int bufferSize() default -1;

    // --- TRADE-OFFS ---

    /**
     * Optimization priority.
     *
     * @return what to optimize for
     */
    Optimize optimize() default Optimize.BALANCED;

    /**
     * Trade-off preferences.
     *
     * @return trade-offs accepted
     */
    TradeOff[] tradeOffs() default {};

    // --- DOCUMENTATION ---

    /**
     * Implementation notes for LLM.
     *
     * @return notes
     */
    String[] notes() default {};

    /**
     * Known edge cases to handle.
     *
     * @return edge cases
     */
    String[] edgeCases() default {};

    /**
     * Constraints that must be satisfied.
     *
     * @return constraints
     */
    String[] constraints() default {};

    /**
     * Anti-patterns to avoid.
     *
     * @return anti-patterns
     */
    String[] avoid() default {};

    /**
     * Reference implementations or libraries to consider.
     *
     * @return references
     */
    String[] references() default {};

    // --- ENUMS - ALGORITHMS ---

    /** Algorithm categories. */
    enum Algorithm {
        UNSPECIFIED,

        // Searching
        LINEAR_SEARCH,
        BINARY_SEARCH,
        INTERPOLATION_SEARCH,
        HASH_LOOKUP,
        TREE_SEARCH,
        GRAPH_BFS,
        GRAPH_DFS,

        // Sorting
        QUICKSORT,
        MERGESORT,
        HEAPSORT,
        TIMSORT,
        INSERTION_SORT,
        RADIX_SORT,
        COUNTING_SORT,
        BUCKET_SORT,

        // String
        KMP,
        RABIN_KARP,
        BOYER_MOORE,
        REGEX,
        TRIE_BASED,

        // Graph
        DIJKSTRA,
        A_STAR,
        BELLMAN_FORD,
        FLOYD_WARSHALL,
        TOPOLOGICAL_SORT,
        KRUSKAL,
        PRIM,

        // Dynamic Programming
        MEMOIZATION,
        TABULATION,
        DIVIDE_AND_CONQUER,

        // Hashing
        CONSISTENT_HASHING,
        BLOOM_FILTER,
        CUCKOO_HASHING,

        // Compression
        HUFFMAN,
        LZ77,
        RLE,

        // Numeric
        NEWTON_RAPHSON,
        BINARY_EXPONENTIATION,
        SIEVE_OF_ERATOSTHENES,

        // Machine Learning
        GRADIENT_DESCENT,
        BACKPROPAGATION,

        // Other
        TWO_POINTER,
        SLIDING_WINDOW,
        GREEDY,
        BACKTRACKING,
        BRANCH_AND_BOUND
    }

    // --- ENUMS - DATA STRUCTURES ---

    /** Data structure recommendations. */
    enum DataStructure {
        UNSPECIFIED,

        // Arrays
        ARRAY,
        DYNAMIC_ARRAY,
        CIRCULAR_BUFFER,

        // Lists
        ARRAY_LIST,
        LINKED_LIST,
        DOUBLY_LINKED_LIST,
        SKIP_LIST,

        // Sets
        HASH_SET,
        TREE_SET,
        LINKED_HASH_SET,
        ENUM_SET,
        BIT_SET,

        // Maps
        HASH_MAP,
        TREE_MAP,
        LINKED_HASH_MAP,
        ENUM_MAP,
        WEAK_HASH_MAP,
        IDENTITY_HASH_MAP,

        // Concurrent
        CONCURRENT_HASH_MAP,
        CONCURRENT_SKIP_LIST_MAP,
        COPY_ON_WRITE_ARRAY_LIST,
        BLOCKING_QUEUE,
        CONCURRENT_LINKED_QUEUE,

        // Queues
        ARRAY_DEQUE,
        PRIORITY_QUEUE,
        DELAY_QUEUE,

        // Stacks
        STACK,
        ARRAY_STACK,

        // Trees
        BINARY_TREE,
        BST,
        AVL_TREE,
        RED_BLACK_TREE,
        B_TREE,
        TRIE,
        SEGMENT_TREE,
        FENWICK_TREE,

        // Graphs
        ADJACENCY_LIST,
        ADJACENCY_MATRIX,
        EDGE_LIST,

        // Specialized
        BLOOM_FILTER,
        LRU_CACHE,
        RING_BUFFER,
        DISJOINT_SET,

        // Immutable (Guava/Eclipse Collections)
        IMMUTABLE_LIST,
        IMMUTABLE_SET,
        IMMUTABLE_MAP,
        PERSISTENT_LIST,
        PERSISTENT_MAP
    }

    // --- ENUMS - PATTERNS ---

    /** Design patterns (GoF + modern). */
    enum Pattern {
        UNSPECIFIED,

        // Creational
        SINGLETON,
        FACTORY,
        ABSTRACT_FACTORY,
        BUILDER,
        PROTOTYPE,
        OBJECT_POOL,
        DEPENDENCY_INJECTION,

        // Structural
        ADAPTER,
        BRIDGE,
        COMPOSITE,
        DECORATOR,
        FACADE,
        FLYWEIGHT,
        PROXY,

        // Behavioral
        CHAIN_OF_RESPONSIBILITY,
        COMMAND,
        INTERPRETER,
        ITERATOR,
        MEDIATOR,
        MEMENTO,
        OBSERVER,
        STATE,
        STRATEGY,
        TEMPLATE_METHOD,
        VISITOR,

        // Concurrency
        MONITOR,
        READ_WRITE_LOCK,
        THREAD_POOL,
        PRODUCER_CONSUMER,
        FUTURE,
        ACTIVE_OBJECT,

        // Functional
        MONAD,
        FUNCTOR,
        OPTION_MAYBE,
        EITHER,
        PIPELINE,

        // Enterprise
        REPOSITORY,
        UNIT_OF_WORK,
        SPECIFICATION,
        CQRS,
        EVENT_SOURCING,
        SAGA
    }

    // --- ENUMS - STYLE ---

    /** Code style preferences. */
    enum Style {
        /** Standard imperative style. */
        STANDARD,

        /** Fluent/chained API (return this). */
        FLUENT,

        /** Functional style (immutable, no side effects). */
        FUNCTIONAL,

        /** Reactive/stream-based. */
        REACTIVE,

        /** Defensive programming (check everything). */
        DEFENSIVE,

        /** Minimal/concise. */
        MINIMAL,

        /** Verbose/explicit (for clarity). */
        EXPLICIT,

        /** DSL-like (domain-specific language). */
        DSL
    }

    // --- ENUMS - THREAD SAFETY ---

    /** Thread safety levels. */
    enum ThreadSafety {
        UNSPECIFIED,

        /** Immutable - inherently thread-safe. */
        IMMUTABLE,

        /** Thread-safe via synchronization. */
        SYNCHRONIZED,

        /** Thread-safe via atomic operations. */
        ATOMIC,

        /** Thread-safe via locks. */
        LOCKED,

        /** Thread-local (no sharing). */
        THREAD_LOCAL,

        /** Not thread-safe (single-threaded use only). */
        NOT_THREAD_SAFE,

        /** Thread-compatible (can be made safe externally). */
        THREAD_COMPATIBLE
    }

    // --- ENUMS - CONCURRENCY ---

    /** Concurrency strategies. */
    enum Concurrency {
        UNSPECIFIED,

        /** Single-threaded only. */
        SINGLE_THREADED,

        /** Lock-based synchronization. */
        LOCK_BASED,

        /** Lock-free algorithms. */
        LOCK_FREE,

        /** Wait-free algorithms. */
        WAIT_FREE,

        /** Compare-and-swap (CAS). */
        CAS,

        /** Actor model. */
        ACTOR,

        /** Software transactional memory. */
        STM,

        /** Async/await. */
        ASYNC,

        /** Virtual threads (Java 21+). */
        VIRTUAL_THREADS,

        /** Fork-join. */
        FORK_JOIN
    }

    // --- ENUMS - LOCK STRATEGY ---

    /** Lock strategies. */
    enum LockStrategy {
        UNSPECIFIED,

        /** No locking needed. */
        NONE,

        /** Intrinsic lock (synchronized). */
        INTRINSIC,

        /** ReentrantLock. */
        REENTRANT,

        /** ReadWriteLock. */
        READ_WRITE,

        /** StampedLock. */
        STAMPED,

        /** Optimistic locking. */
        OPTIMISTIC,

        /** Pessimistic locking. */
        PESSIMISTIC,

        /** Spin lock. */
        SPIN
    }

    // --- ENUMS - NULL HANDLING ---

    /** Null handling strategies. */
    enum NullHandling {
        UNSPECIFIED,

        /** Reject null early (throw NPE/IAE). */
        REJECT_EARLY,

        /** Return Optional instead of null. */
        OPTIONAL,

        /** Use null object pattern. */
        NULL_OBJECT,

        /** Use default value. */
        DEFAULT_VALUE,

        /** Allow null (document behavior). */
        ALLOW,

        /** Use @Nullable/@NonNull annotations. */
        ANNOTATED
    }

    // --- ENUMS - VALIDATION ---

    /** Validation strategies. */
    enum Validation {
        UNSPECIFIED,

        /** Fail fast (first error stops). */
        FAIL_FAST,

        /** Collect all errors. */
        COLLECT_ALL,

        /** Lazy validation (on use). */
        LAZY,

        /** No validation (trust input). */
        NONE,

        /** JSR-303 Bean Validation. */
        BEAN_VALIDATION,

        /** Preconditions (Guava-style). */
        PRECONDITIONS,

        /** Assertions (dev only). */
        ASSERTIONS
    }

    // --- ENUMS - ERROR HANDLING ---

    /** Error handling strategies. */
    enum ErrorHandling {
        UNSPECIFIED,

        /** Throw exception. */
        THROW,

        /** Return error code/enum. */
        ERROR_CODE,

        /** Return Result/Either type. */
        RESULT_TYPE,

        /** Silent failure (log only). */
        SILENT,

        /** Retry with backoff. */
        RETRY,

        /** Fallback to default. */
        FALLBACK,

        /** Circuit breaker. */
        CIRCUIT_BREAKER,

        /** Compensating action. */
        COMPENSATE
    }

    // --- ENUMS - CACHING ---

    /** Caching strategies. */
    enum Caching {
        /** No caching. */
        NONE,

        /** LRU (Least Recently Used). */
        LRU,

        /** LFU (Least Frequently Used). */
        LFU,

        /** FIFO (First In First Out). */
        FIFO,

        /** Time-based expiration. */
        TTL,

        /** Write-through cache. */
        WRITE_THROUGH,

        /** Write-behind cache. */
        WRITE_BEHIND,

        /** Read-through cache. */
        READ_THROUGH,

        /** Cache-aside. */
        CACHE_ASIDE,

        /** Memoization (pure functions). */
        MEMOIZATION
    }

    // --- ENUMS - EVICTION ---

    /** Eviction policies. */
    enum Eviction {
        UNSPECIFIED,

        /** Size-based eviction. */
        SIZE_BASED,

        /** Time-based eviction. */
        TIME_BASED,

        /** Weight-based eviction. */
        WEIGHT_BASED,

        /** Reference-based (weak/soft). */
        REFERENCE_BASED,

        /** Manual eviction only. */
        MANUAL,

        /** No eviction (unbounded). */
        NONE
    }

    // --- ENUMS - MEMORY STRATEGY ---

    /** Memory optimization strategies. */
    enum MemoryStrategy {
        UNSPECIFIED,

        /** Minimize memory usage. */
        MINIMIZE_MEMORY,

        /** Object pooling. */
        POOLING,

        /** Flyweight pattern. */
        FLYWEIGHT,

        /** String interning. */
        INTERNING,

        /** Weak references. */
        WEAK_REFERENCES,

        /** Soft references. */
        SOFT_REFERENCES,

        /** Off-heap storage. */
        OFF_HEAP,

        /** Memory-mapped files. */
        MEMORY_MAPPED,

        /** Compact data structures. */
        COMPACT
    }

    // --- ENUMS - I/O STRATEGY ---

    /** I/O strategies. */
    enum IOStrategy {
        UNSPECIFIED,

        /** Synchronous blocking I/O. */
        BLOCKING,

        /** Non-blocking I/O (NIO). */
        NON_BLOCKING,

        /** Asynchronous I/O. */
        ASYNC,

        /** Buffered I/O. */
        BUFFERED,

        /** Memory-mapped I/O. */
        MEMORY_MAPPED,

        /** Streaming (process as you go). */
        STREAMING,

        /** Batch processing. */
        BATCH,

        /** Direct I/O (bypass OS cache). */
        DIRECT
    }

    // --- ENUMS - OPTIMIZATION ---

    /** Optimization priorities. */
    enum Optimize {
        /** Balanced approach. */
        BALANCED,

        /** Optimize for speed. */
        SPEED,

        /** Optimize for memory. */
        MEMORY,

        /** Optimize for throughput. */
        THROUGHPUT,

        /** Optimize for latency. */
        LATENCY,

        /** Optimize for simplicity. */
        SIMPLICITY,

        /** Optimize for maintainability. */
        MAINTAINABILITY,

        /** Optimize for testability. */
        TESTABILITY,

        /** Optimize for scalability. */
        SCALABILITY
    }

    // --- ENUMS - TRADE-OFFS ---

    /** Acceptable trade-offs. */
    enum TradeOff {
        /** Accept higher memory for speed. */
        MEMORY_FOR_SPEED,

        /** Accept slower speed for less memory. */
        SPEED_FOR_MEMORY,

        /** Accept complexity for performance. */
        SIMPLICITY_FOR_PERFORMANCE,

        /** Accept eventual consistency for availability. */
        CONSISTENCY_FOR_AVAILABILITY,

        /** Accept latency for throughput. */
        LATENCY_FOR_THROUGHPUT,

        /** Accept more code for type safety. */
        BREVITY_FOR_TYPE_SAFETY,

        /** Accept runtime cost for compile-time safety. */
        RUNTIME_FOR_COMPILE_SAFETY
    }
}
