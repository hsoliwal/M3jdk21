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
package jdk.internal.m3.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
/**
 * Validator for collection inputs with composable validation rules.
 *
 * <p>
 * Provides common collection validation patterns with clear error messages.
 * Follows single responsibility principle - each validator checks one thing.
 * </p>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * // Simple validation
 * ValidationResult result = CollectionValidator.builder("tags").notNull().notEmpty().minSize(1).maxSize(10)
 * 		.validate(tags);
 *
 * if (!result.isValid()) {
 * 	throw new IllegalArgumentException(result.getErrorMessage());
 * }
 *
 * // Element validation
 * CollectionValidator<String> validator = CollectionValidator.<String>builder("emails").notNull()
 * 		.allElementsMatch(email -> email.matches("^[^@]+@[^@]+$"), "All elements must be valid email addresses")
 * 		.build();
 * }</pre>
 *
 * @param <T>
 *            element type
 * @author Hitesh Soliwal
 * @since 1.0.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/validation/CollectionValidator.java}
 * blob {@code 486421e702c818bc4a6ccb81ea88c5eddc47e154} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common batch b29 (slf4j owners on the PlaceholderLogger facade) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class CollectionValidator<T> {
    private static final PlaceholderLogger log = PlaceholderLogger.of(CollectionValidator.class);


    private final String fieldName;
    private final List<Rule<T>> rules;

    private CollectionValidator(String fieldName, List<Rule<T>> rules) {
        this.fieldName = fieldName;
        this.rules = List.copyOf(rules);
    }

    /**
     * Creates a new builder for collection validation.
     *
     * @param fieldName
     *            name of the field being validated (for error messages)
     * @param <T>
     *            element type
     * @return new builder
     */
    public static <T> Builder<T> builder(String fieldName) {
        return new Builder<>(fieldName);
    }

    /**
     * Validates a collection against all configured rules.
     *
     * @param value
     *            collection to validate
     * @return validation result
     */
    public ValidationResult validate(Collection<T> value) {
        List<String> errors = new ArrayList<>();

        //FLLDCIM: MRL — T2-L2 filter+map+collect; behaviour preserved
        rules.stream()
                .filter(rule -> !rule.test(value))
                .map(Rule::errorMessage)
                .forEach(errors::add);

        return new ValidationResult(errors.isEmpty(), fieldName, value, errors);
    }

    /** Validation result containing success status and error messages. */
    public static final class ValidationResult {

        private final boolean valid;
        private final String fieldName;
        private final Collection<?> value;
        private final List<String> errors;

        private ValidationResult(boolean valid, String fieldName, Collection<?> value, List<String> errors) {
            this.valid = valid;
            this.fieldName = fieldName;
            this.value = value;
            this.errors = List.copyOf(errors);
        }

        /**
         * Checks if validation passed.
         *
         * @return true if valid
         */
        public boolean isValid() {
            return valid;
        }

        /**
         * Checks if validation failed.
         *
         * @return true if invalid
         */
        public boolean isInvalid() {
            return !valid;
        }

        /**
         * Gets the field name.
         *
         * @return field name
         */
        public String getFieldName() {
            return fieldName;
        }

        /**
         * Gets the validated collection.
         *
         * @return collection
         */
        public Collection<?> getValue() {
            return value;
        }

        /**
         * Gets all error messages.
         *
         * @return list of error messages (empty if valid)
         */
        public List<String> getErrors() {
            return errors;
        }

        /**
         * Gets first error message if any.
         *
         * @return first error message
         */
        public Optional<String> getFirstError() {
            return errors.isEmpty() ? Optional.empty() : Optional.of(errors.get(0));
        }

        /**
         * Gets formatted error message for all errors.
         *
         * @return combined error message
         */
        public String getErrorMessage() {
            if (valid) {
                return "";
            }
            return String.format("%s validation failed: %s", fieldName, String.join("; ", errors));
        }

        /**
         * Throws IllegalArgumentException if invalid.
         *
         * @return this result for chaining
         * @throws IllegalArgumentException
         *             if validation failed
         */
        public ValidationResult orElseThrow() {
            if (!valid) {
                throw new IllegalArgumentException(getErrorMessage());
            }
            return this;
        }

        /**
         * Throws custom exception if invalid.
         *
         * @param exceptionSupplier
         *            exception supplier
         * @param <X>
         *            exception type
         * @return this result for chaining
         * @throws X
         *             if validation failed
         */
        public <X extends Throwable> ValidationResult orElseThrow(
                java.util.function.Supplier<? extends X> exceptionSupplier) throws X {
            if (!valid) {
                throw exceptionSupplier.get();
            }
            return this;
        }
    }

    /** Validation rule - predicate with error message. */
    private static final class Rule<T> {

        private final Predicate<Collection<T>> predicate;
        private final String errorMessage;

        Rule(Predicate<Collection<T>> predicate, String errorMessage) {
            this.predicate = predicate;
            this.errorMessage = errorMessage;
        }

        boolean test(Collection<T> value) {
            return predicate.test(value);
        }

        String errorMessage() {
            return errorMessage;
        }
    }

    /**
     * Builder for constructing collection validators with composable rules.
     *
     * @param <T>
     *            element type
     */
    public static final class Builder<T> {

        private final String fieldName;
        private final List<Rule<T>> rules = new ArrayList<>();

        private Builder(String fieldName) {
            if (fieldName == null || fieldName.isBlank()) {
                throw new IllegalArgumentException("Field name must not be blank");
            }
            this.fieldName = fieldName;
        }

        /**
         * Validates that collection is not null.
         *
         * @return this builder
         */
        public Builder<T> notNull() {
            rules.add(new Rule<>(value -> value != null, "must not be null"));
            return this;
        }

        /**
         * Validates that collection is not null and not empty.
         *
         * @return this builder
         */
        public Builder<T> notEmpty() {
            rules.add(new Rule<>(value -> value != null && !value.isEmpty(), "must not be empty"));
            return this;
        }

        /**
         * Validates minimum size.
         *
         * @param minSize
         *            minimum size (inclusive)
         * @return this builder
         */
        public Builder<T> minSize(int minSize) {
            if (minSize < 0) {
                throw new IllegalArgumentException("Min size must be non-negative");
            }
            rules.add(new Rule<>(
                    value -> value != null && value.size() >= minSize,
                    String.format("must contain at least %d elements", minSize)));
            return this;
        }

        /**
         * Validates maximum size.
         *
         * @param maxSize
         *            maximum size (inclusive)
         * @return this builder
         */
        public Builder<T> maxSize(int maxSize) {
            if (maxSize < 0) {
                throw new IllegalArgumentException("Max size must be non-negative");
            }
            rules.add(new Rule<>(
                    value -> value == null || value.size() <= maxSize,
                    String.format("must contain at most %d elements", maxSize)));
            return this;
        }

        /**
         * Validates exact size.
         *
         * @param exactSize
         *            exact size required
         * @return this builder
         */
        public Builder<T> exactSize(int exactSize) {
            if (exactSize < 0) {
                throw new IllegalArgumentException("Exact size must be non-negative");
            }
            rules.add(new Rule<>(
                    value -> value != null && value.size() == exactSize,
                    String.format("must contain exactly %d elements", exactSize)));
            return this;
        }

        /**
         * Validates that all elements match a predicate.
         *
         * @param predicate
         *            element predicate
         * @param errorMessage
         *            error message if any element doesn't match
         * @return this builder
         */
        public Builder<T> allElementsMatch(Predicate<T> predicate, String errorMessage) {
            rules.add(new Rule<>(value -> value != null && value.stream().allMatch(predicate), errorMessage));
            return this;
        }

        /**
         * Validates that no elements are null.
         *
         * @return this builder
         */
        public Builder<T> noNullElements() {
            rules.add(new Rule<>(
                    value -> value != null && value.stream().noneMatch(e -> e == null),
                    "must not contain null elements"));
            return this;
        }

        /**
         * Validates that collection contains a specific element.
         *
         * @param element
         *            required element
         * @return this builder
         */
        public Builder<T> contains(T element) {
            rules.add(new Rule<>(
                    value -> value != null && value.contains(element),
                    String.format("must contain element: %s", element)));
            return this;
        }

        /**
         * Validates that collection contains all specified elements.
         *
         * @param elements
         *            required elements
         * @return this builder
         */
        @SafeVarargs
        @SuppressWarnings("varargs") // LINT_SUPPRESS (declared, b29): source-retention
        public final Builder<T> containsAll(T... elements) {
            //FLLDCIM: MRL — T2-L1 for-each → Arrays.asList; behaviour preserved
            final List<T> requiredElements = elements == null ? List.of() : List.of(elements);
            rules.add(new Rule<>(
                    value -> value != null && value.containsAll(requiredElements),
                    String.format("must contain all elements: %s", requiredElements)));
            return this;
        }

        /**
         * Validates using custom predicate.
         *
         * @param predicate
         *            validation predicate
         * @param errorMessage
         *            error message if predicate fails
         * @return this builder
         */
        public Builder<T> custom(Predicate<Collection<T>> predicate, String errorMessage) {
            rules.add(new Rule<>(predicate, errorMessage));
            return this;
        }

        /**
         * Builds the validator.
         *
         * @return configured collection validator
         */
        public CollectionValidator<T> build() {
            return new CollectionValidator<>(fieldName, rules);
        }

        /**
         * Builds and immediately validates a value.
         *
         * @param value
         *            collection to validate
         * @return validation result
         */
        public ValidationResult validate(Collection<T> value) {
            return build().validate(value);
        }
    }
}
