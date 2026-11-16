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
package jdk.internal.m3.paradigm.context;

import jdk.internal.m3.collections.TokenInterner;
import jdk.internal.m3.paradigm.annotation.ISpec;
import jdk.internal.m3.paradigm.annotation.Model;
import jdk.internal.m3.paradigm.annotation.Stability;

import java.nio.file.Path;
import java.time.Instant;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * IAlgebraicEffect - Algebraic Effect Types following Union of Union pattern.
 *
 * <p>This is a <strong>sealed hierarchy</strong> that models all possible
 * side effects in the system. Using sealed interfaces enables:
 * <ul>
 *   <li>Exhaustive pattern matching</li>
 *   <li>Compile-time effect tracking</li>
 *   <li>Pure function composition</li>
 *   <li>Effect handlers as interpreters</li>
 * </ul>
 *
 * <h2>Union of Union Structure</h2>
 * <pre>
 * IAlgebraicEffect (root union)
 *   ├── IIOEffect (IO operations union)
 *   │     ├── Read
 *   │     ├── Write
 *   │     └── Delete
 *   ├── IComputeEffect (pure computation union)
 *   │     ├── MapEffect
 *   │     ├── FilterEffect
 *   │     └── FoldEffect
 *   ├── IEventEffect (event emission union)
 *   │     ├── Emit
 *   │     ├── Publish
 *   │     └── Persist
 *   └── ILogEffect (logging union)
 *         ├── Log
 *         ├── Trace
 *         └── Metric
 * </pre>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Pattern matching on effects
 * String describe(IAlgebraicEffect effect) {
 *     return switch (effect) {
 *         case Read r -> "Read from " + r.path();
 *         case Write w -> "Write to " + w.path();
 *         case Delete d -> "Delete " + d.path();
 *         case MapEffect m -> "Map values";
 *         case FilterEffect f -> "Filter values";
 *         case FoldEffect fo -> "Fold values";
 *         case Emit e -> "Emit event: " + e.eventType();
 *         case Publish p -> "Publish to: " + p.topic();
 *         case Persist pe -> "Persist: " + pe.key();
 *         case Log l -> "Log: " + l.message();
 *         case Trace t -> "Trace: " + t.spanId();
 *         case Metric m -> "Metric: " + m.name();
 *     };
 * }
 * }</pre>
 *
 * @since Grand Java Patterns Proof - Union of Union
 */
@ISpec(
    value = "Algebraic Effect Types - Union of Union pattern for exhaustive effect handling",
    constraints = {
        "Root sealed interface permits only 4 effect categories",
        "Each category is itself a sealed interface (union of union)",
        "All concrete effects are records (product types)",
        "Pattern matching must be exhaustive at compile-time"
    },
    invariants = {
        "description() returns human-readable effect description",
        "timestamp() returns when effect was created",
        "Switch on IAlgebraicEffect covers all 12 concrete effect types"
    },
    examples = {
        "new Read(Path.of(\"/file\"))",
        "new MapEffect<>(String::toUpperCase, \"String\", \"String\")",
        "new Emit(\"OrderPlaced\", order)",
        "new Log(Level.INFO, \"message\")"
    },
    model = Model.ModelType.OPUS,
    stability = Stability.Level.STABLE
)
/**
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/union/IAlgebraicEffect.java}
 * blob {@code 1350e74b52068171f0f0b9276df62b0b5a5c1062} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch 2 (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public sealed interface IAlgebraicEffect
        permits IAlgebraicEffect.IIOEffect, IAlgebraicEffect.IComputeEffect,
                IAlgebraicEffect.IEventEffect, IAlgebraicEffect.ILogEffect {

    /**
     * Get effect description.
     *
     * @return human-readable description
     */
    String description();

    /**
     * Get effect timestamp.
     *
     * @return when effect was created
     */
    default Instant timestamp() {
        return Instant.now();
    }

    // --- IO Effect Union ---

    /** IO Effect - File system and network operations. */
    sealed interface IIOEffect extends IAlgebraicEffect
            permits Read, Write, Delete {
    }

    /**
     * Read effect - reading from a path.
     *
     * @param path source path
     * @param encoding character encoding
     */
    record Read(Path path, String encoding) implements IIOEffect {
        /**
         * Create read effect with default UTF-8 encoding.
         *
         * @param path source path
         */
        public Read(Path path) {
            this(path, "UTF-8");
        }

        @Override
        public String description() {
            return "Read from " + path;
        }
    }

    /**
     * Write effect - writing to a path.
     *
     * @param path target path
     * @param content content to write
     * @param append whether to append
     */
    record Write(Path path, byte[] content, boolean append) implements IIOEffect {
        /**
         * Create write effect with overwrite mode.
         *
         * @param path target path
         * @param content content to write
         */
        public Write(Path path, byte[] content) {
            this(path, content, false);
        }

        @Override
        public String description() {
            return (append ? "Append to " : "Write to ") + path;
        }
    }

    /**
     * Delete effect - removing a path.
     *
     * @param path path to delete
     * @param recursive whether to delete recursively
     */
    record Delete(Path path, boolean recursive) implements IIOEffect {
        /**
         * Create non-recursive delete effect.
         *
         * @param path path to delete
         */
        public Delete(Path path) {
            this(path, false);
        }

        @Override
        public String description() {
            return (recursive ? "Recursively delete " : "Delete ") + path;
        }
    }

    // --- Compute Effect Union ---

    /** Compute Effect - Pure computational transformations. */
    sealed interface IComputeEffect extends IAlgebraicEffect
            permits MapEffect, FilterEffect, FoldEffect {
    }

    /**
     * Map effect - transformation.
     *
     * @param mapper transformation function
     * @param inputType input type name
     * @param outputType output type name
     * @param <I> input type
     * @param <O> output type
     */
    record MapEffect<I, O>(
            Function<I, O> mapper,
            String inputType,
            String outputType
    ) implements IComputeEffect {
        @Override
        public String description() {
            return "Map " + inputType + " → " + outputType;
        }
    }

    /**
     * Filter effect - predicate selection.
     *
     * @param predicate filter predicate
     * @param predicateDescription human-readable description
     * @param <T> element type
     */
    record FilterEffect<T>(
            Predicate<T> predicate,
            String predicateDescription
    ) implements IComputeEffect {
        @Override
        public String description() {
            return "Filter by: " + predicateDescription;
        }
    }

    /**
     * Fold effect - reduction.
     *
     * @param identity initial value
     * @param accumulator accumulation function
     * @param <T> element type
     * @param <R> result type
     */
    record FoldEffect<T, R>(
            R identity,
            java.util.function.BiFunction<R, T, R> accumulator
    ) implements IComputeEffect {
        @Override
        public String description() {
            return "Fold with identity: " + identity;
        }
    }

    // --- Event Effect Union ---

    /** Event Effect - Event emission and persistence. */
    sealed interface IEventEffect extends IAlgebraicEffect
            permits Emit, Publish, Persist {
    }

    /**
     * Emit effect - emit domain event.
     *
     * @param eventType event type name
     * @param payload event payload
     */
    record Emit(String eventType, Object payload) implements IEventEffect {
        @Override
        public String description() {
            return "Emit event: " + eventType;
        }
    }

    /**
     * Publish effect - publish to topic/channel.
     *
     * @param topic target topic
     * @param message message payload
     * @param key optional partition key
     */
    record Publish(String topic, Object message, String key) implements IEventEffect {
        /**
         * Create publish effect without key.
         *
         * @param topic target topic
         * @param message message payload
         */
        public Publish(String topic, Object message) {
            this(topic, message, null);
        }

        @Override
        public String description() {
            return "Publish to topic: " + topic;
        }
    }

    /**
     * Persist effect - persist to store.
     *
     * @param key storage key
     * @param value value to persist
     * @param store store name
     */
    record Persist(String key, Object value, String store) implements IEventEffect {
        @Override
        public String description() {
            return "Persist " + key + " to " + store;
        }
    }

    // --- Log Effect Union ---

    /** Log Effect - Logging and observability. */
    sealed interface ILogEffect extends IAlgebraicEffect
            permits Log, Trace, Metric {
    }

    /**
     * Log effect - structured logging.
     *
     * @param level log level
     * @param message log message
     * @param context additional context
     */
    record Log(
            Level level,
            String message,
            java.util.Map<String, Object> context
    ) implements ILogEffect {

        /** Log levels. */
        public enum Level {
            /** Debug level. */
            DEBUG,
            /** Info level. */
            INFO,
            /** Warning level. */
            WARN,
            /** Error level. */
            ERROR
        }

        /**
         * Create log effect without context.
         *
         * @param level log level
         * @param message log message
         */
        public Log(Level level, String message) {
            this(level, message, java.util.Map.of());
        }

        @Override
        public String description() {
            return "[" + level + "] " + message;
        }
    }

    /**
     * Trace effect - distributed tracing.
     *
     * @param spanId span identifier
     * @param traceId trace identifier
     * @param parentSpanId parent span (null if root)
     * @param operation operation name
     */
    record Trace(
            String spanId,
            String traceId,
            String parentSpanId,
            String operation
    ) implements ILogEffect {
        @Override
        public String description() {
            return "Trace " + operation + " [" + spanId + "]";
        }
    }

    /**
     * Metric effect - metrics emission.
     *
     * @param name metric name
     * @param value metric value
     * @param type metric type
     * @param tags metric tags
     */
    record Metric(
            String name,
            double value,
            Type type,
            java.util.Map<String, String> tags
    ) implements ILogEffect {

        /** Metric types. */
        public enum Type {
            /** Counter - monotonically increasing. */
            COUNTER,
            /** Gauge - point-in-time value. */
            GAUGE,
            /** Histogram - distribution. */
            HISTOGRAM,
            /** Timer - duration measurement. */
            TIMER
        }

        /**
         * Create metric without tags.
         *
         * @param name metric name
         * @param value metric value
         * @param type metric type
         */
        public Metric(String name, double value, Type type) {
            this(name, value, type, java.util.Map.of());
        }

        @Override
        public String description() {
            return type + " " + name + " = " + value;
        }
    }
}
