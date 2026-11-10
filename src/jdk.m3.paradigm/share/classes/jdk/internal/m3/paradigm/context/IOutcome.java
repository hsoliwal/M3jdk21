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

import jdk.internal.m3.collections.PlaceholderLogger;

import jdk.internal.m3.paradigm.annotation.ISpec;
import jdk.internal.m3.paradigm.annotation.Model;
import jdk.internal.m3.paradigm.annotation.Stability;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Outcome Union - Result/Error algebraic type.
 *
 * <p>A sealed union representing computation outcomes:
 * <ul>
 *   <li>{@link Success} - successful result with value</li>
 *   <li>{@link Failure} - failure with error</li>
 *   <li>{@link Pending} - operation in progress</li>
 *   <li>{@link Cancelled} - operation was cancelled</li>
 * </ul>
 *
 * <h2>Why not Optional or Exception?</h2>
 * <ul>
 *   <li>Optional loses error information</li>
 *   <li>Exceptions break referential transparency</li>
 *   <li>Outcome is explicit, composable, exhaustive</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * IOutcome<User> outcome = userService.findById(id);
 *
 * String result = switch (outcome) {
 *     case Success<User> s -> "Found: " + s.value().name();
 *     case Failure<User> f -> "Error: " + f.error().getMessage();
 *     case Pending<User> p -> "Loading...";
 *     case Cancelled<User> c -> "Operation cancelled: " + c.reason();
 * };
 *
 * // Or use functional composition
 * outcome
 *     .map(User::name)
 *     .flatMap(name -> validateName(name))
 *     .onSuccess(name -> log.info("Valid user: {}", name))
 *     .onFailure(err -> log.error("Failed", err));
 * }</pre>
 *
 * @param <T> success value type
 * @since Grand Java Patterns Proof - Union of Union
 */
@ISpec(
    value = "Outcome Union - Result/Error algebraic type (Success|Failure|Pending|Cancelled)",
    constraints = {
        "Sealed union with exhaustive pattern matching",
        "Optional loses error information - use Outcome instead",
        "Exceptions break referential transparency - use Outcome instead",
        "Outcome is explicit, composable, and exhaustive"
    },
    invariants = {
        "isSuccess() returns true only for Success variant",
        "toOptional() returns Optional.empty() for non-Success",
        "getOrThrow() throws for non-Success variants"
    },
    examples = {
        "IOutcome.success(value)",
        "IOutcome.failure(error)",
        "outcome.map(User::name).flatMap(validateName)"
    },
    model = Model.ModelType.SONNET,
    stability = Stability.Level.STABLE
)
/**
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/union/IOutcome.java}
 * blob {@code 8609b9b9386a61412efb676a7809683da0057103} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public sealed interface IOutcome<T>
        permits IOutcome.Success, IOutcome.Failure,
                IOutcome.Pending, IOutcome.Cancelled {
    PlaceholderLogger log = PlaceholderLogger.of(IOutcome.class);

    // --- Union Variants ---

    /**
     * Success outcome with value.
     *
     * @param value the successful result
     * @param <T> value type
     */
    record Success<T>(T value) implements IOutcome<T> {
        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public Optional<T> toOptional() {
            return Optional.ofNullable(value);
        }

        @Override
        public T getOrThrow() {
            return value;
        }
    }

    /**
     * Failure outcome with error.
     *
     * @param error the error that occurred
     * @param <T> expected value type
     */
    record Failure<T>(Throwable error) implements IOutcome<T> {
        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public Optional<T> toOptional() {
            return Optional.empty();
        }

        @Override
        public T getOrThrow() {
            if (error instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException(error);
        }
    }

    /**
     * Pending outcome - operation in progress.
     *
     * @param progress progress percentage (0-100)
     * @param message status message
     * @param <T> expected value type
     */
    record Pending<T>(int progress, String message) implements IOutcome<T> {
        /**
         * Create pending with just a message.
         *
         * @param message status message
         */
        public Pending(String message) {
            this(0, message);
        }

        /** Create pending with default message. */
        public Pending() {
            this(0, "In progress...");
        }

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public Optional<T> toOptional() {
            return Optional.empty();
        }

        @Override
        public T getOrThrow() {
            throw new IllegalStateException("Operation still pending: " + message);
        }
    }

    /**
     * Cancelled outcome - operation was cancelled.
     *
     * @param reason cancellation reason
     * @param <T> expected value type
     */
    record Cancelled<T>(String reason) implements IOutcome<T> {
        /** Create cancelled with default reason. */
        public Cancelled() {
            this("Operation cancelled");
        }

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public Optional<T> toOptional() {
            return Optional.empty();
        }

        @Override
        public T getOrThrow() {
            throw new java.util.concurrent.CancellationException(reason);
        }
    }

    // --- Abstract Methods ---

    /**
     * Check if this is a successful outcome.
     *
     * @return true if success
     */
    boolean isSuccess();

    /**
     * Convert to Optional.
     *
     * @return Optional with value if success, empty otherwise
     */
    Optional<T> toOptional();

    /**
     * Get value or throw.
     *
     * @return value if success
     * @throws RuntimeException if not success
     */
    T getOrThrow();

    // --- Default Methods - Functional Composition ---

    /**
     * Check if this is a failure.
     *
     * @return true if failure
     */
    default boolean isFailure() {
        return this instanceof Failure;
    }

    /**
     * Check if this is pending.
     *
     * @return true if pending
     */
    default boolean isPending() {
        return this instanceof Pending;
    }

    /**
     * Check if this is cancelled.
     *
     * @return true if cancelled
     */
    default boolean isCancelled() {
        return this instanceof Cancelled;
    }

    /**
     * Map success value.
     *
     * @param mapper transformation function
     * @param <U> new value type
     * @return mapped outcome
     */
    @SuppressWarnings("unchecked")
    default <U> IOutcome<U> map(Function<? super T, ? extends U> mapper) {
        return switch (this) {
            case Success<T> s -> new Success<>(mapper.apply(s.value()));
            case Failure<T> f -> (IOutcome<U>) f;
            case Pending<T> p -> new Pending<>(p.progress(), p.message());
            case Cancelled<T> c -> new Cancelled<>(c.reason());
        };
    }

    /**
     * FlatMap success value.
     *
     * @param mapper function returning new Outcome
     * @param <U> new value type
     * @return flat mapped outcome
     */
    @SuppressWarnings("unchecked")
    default <U> IOutcome<U> flatMap(Function<? super T, IOutcome<U>> mapper) {
        return switch (this) {
            case Success<T> s -> mapper.apply(s.value());
            case Failure<T> f -> (IOutcome<U>) f;
            case Pending<T> p -> new Pending<>(p.progress(), p.message());
            case Cancelled<T> c -> new Cancelled<>(c.reason());
        };
    }

    /**
     * Get value or default.
     *
     * @param defaultValue value if not success
     * @return value or default
     */
    default T getOrElse(T defaultValue) {
        return switch (this) {
            case Success<T> s -> s.value();
            default -> defaultValue;
        };
    }

    /**
     * Get value or compute default.
     *
     * @param supplier default value supplier
     * @return value or computed default
     */
    default T getOrElseGet(Supplier<? extends T> supplier) {
        return switch (this) {
            case Success<T> s -> s.value();
            default -> supplier.get();
        };
    }

    /**
     * Execute action on success.
     *
     * @param action action to execute
     * @return this outcome
     */
    default IOutcome<T> onSuccess(Consumer<? super T> action) {
        if (this instanceof Success<T> s) {
            action.accept(s.value());
        }
        return this;
    }

    /**
     * Execute action on failure.
     *
     * @param action action to execute with error
     * @return this outcome
     */
    default IOutcome<T> onFailure(Consumer<Throwable> action) {
        if (this instanceof Failure<T> f) {
            action.accept(f.error());
        }
        return this;
    }

    /**
     * Recover from failure.
     *
     * @param recovery recovery function
     * @return recovered outcome
     */
    default IOutcome<T> recover(Function<Throwable, ? extends T> recovery) {
        return switch (this) {
            case Failure<T> f -> new Success<>(recovery.apply(f.error()));
            default -> this;
        };
    }

    /**
     * Recover from failure with new outcome.
     *
     * @param recovery recovery function returning outcome
     * @return recovered outcome
     */
    default IOutcome<T> recoverWith(Function<Throwable, IOutcome<T>> recovery) {
        return switch (this) {
            case Failure<T> f -> recovery.apply(f.error());
            default -> this;
        };
    }

    // --- Static Factory Methods ---

    /**
     * Create success outcome.
     *
     * @param value the value
     * @param <T> value type
     * @return success outcome
     */
    static <T> IOutcome<T> success(T value) {
        return new Success<>(value);
    }

    /**
     * Create failure outcome.
     *
     * @param error the error
     * @param <T> expected value type
     * @return failure outcome
     */
    static <T> IOutcome<T> failure(Throwable error) {
        return new Failure<>(error);
    }

    /**
     * Create failure outcome from message.
     *
     * @param message error message
     * @param <T> expected value type
     * @return failure outcome
     */
    static <T> IOutcome<T> failure(String message) {
        return new Failure<>(new RuntimeException(message));
    }

    /**
     * Create pending outcome.
     *
     * @param <T> expected value type
     * @return pending outcome
     */
    static <T> IOutcome<T> pending() {
        return new Pending<>();
    }

    /**
     * Create cancelled outcome.
     *
     * @param reason cancellation reason
     * @param <T> expected value type
     * @return cancelled outcome
     */
    static <T> IOutcome<T> cancelled(String reason) {
        return new Cancelled<>(reason);
    }

    /**
     * Try to execute a supplier, wrapping result in Outcome.
     *
     * @param supplier supplier that may throw
     * @param <T> value type
     * @return success or failure outcome
     */
    static <T> IOutcome<T> of(Supplier<T> supplier) {
        try {
            return new Success<>(supplier.get());
        } catch (Exception e) {
            return new Failure<>(e);
        }
    }

    /**
     * Convert Optional to Outcome.
     *
     * @param optional optional value
     * @param errorMessage message if empty
     * @param <T> value type
     * @return success or failure outcome
     */
    static <T> IOutcome<T> fromOptional(Optional<T> optional, String errorMessage) {
        return optional
                .<IOutcome<T>>map(Success::new)
                .orElseGet(() -> failure(errorMessage));
    }
}
