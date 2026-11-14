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
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
/**
 * Validator for string inputs with composable validation rules.
 *
 * <p>
 * Provides common string validation patterns with clear error messages. Follows
 * single responsibility principle - each validator checks one thing.
 * </p>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * // Simple validation
 * ValidationResult result = StringValidator.builder("email").notBlank().maxLength(255)
 * 		.matches("^[^@]+@[^@]+$", "Must be valid email").validate("user@example.com");
 *
 * if (!result.isValid()) {
 * 	throw new IllegalArgumentException(result.getErrorMessage());
 * }
 *
 * // Batch validation
 * StringValidator validator = StringValidator.builder("username").notBlank().minLength(3).maxLength(20).alphanumeric()
 * 		.build();
 *
 * List<String> usernames = List.of("john", "ab", "valid_user_123");
 * usernames.forEach(name -> {
 * 	ValidationResult result = validator.validate(name);
 * 	if (!result.isValid()) {
 * 		log.error("{}", result.getErrorMessage());
 * 	}
 * });
 * }</pre>
 *
 * @author Hitesh Soliwal
 * @since 1.0.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/validation/StringValidator.java}
 * blob {@code 64f5abef992e0d0db932e2183aaa99cf0c3497ca} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common batch b29 (slf4j owners on the PlaceholderLogger facade) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class StringValidator {
    private static final PlaceholderLogger log = PlaceholderLogger.of(StringValidator.class);


    private final String fieldName;
    private final List<Rule> rules;

    private StringValidator(String fieldName, List<Rule> rules) {
        this.fieldName = fieldName;
        this.rules = List.copyOf(rules);
    }

    /**
     * Creates a new builder for field validation.
     *
     * @param fieldName
     *            name of the field being validated (for error messages)
     * @return new builder
     */
    public static Builder builder(String fieldName) {
        return new Builder(fieldName);
    }

    /**
     * Validates a string value against all configured rules.
     *
     * @param value
     *            value to validate
     * @return validation result
     */
    public ValidationResult validate(String value) {
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
        private final String value;
        private final List<String> errors;

        private ValidationResult(boolean valid, String fieldName, String value, List<String> errors) {
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
         * Gets the validated value.
         *
         * @return value
         */
        public String getValue() {
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
    private static final class Rule {

        private final Predicate<String> predicate;
        private final String errorMessage;

        Rule(Predicate<String> predicate, String errorMessage) {
            this.predicate = predicate;
            this.errorMessage = errorMessage;
        }

        boolean test(String value) {
            return predicate.test(value);
        }

        String errorMessage() {
            return errorMessage;
        }
    }

    /** Builder for constructing string validators with composable rules. */
    public static final class Builder {

        private static final int DEFAULT_MAX_LENGTH = 1000;
        private final String fieldName;
        private final List<Rule> rules = new ArrayList<>();

        private Builder(String fieldName) {
            if (fieldName == null || fieldName.isBlank()) {
                throw new IllegalArgumentException("Field name must not be blank");
            }
            this.fieldName = fieldName;
        }

        /**
         * Validates that string is not null.
         *
         * @return this builder
         */
        public Builder notNull() {
            rules.add(new Rule(value -> value != null, "must not be null"));
            return this;
        }

        /**
         * Validates that string is not null and not empty.
         *
         * @return this builder
         */
        public Builder notEmpty() {
            rules.add(new Rule(value -> value != null && !value.isEmpty(), "must not be empty"));
            return this;
        }

        /**
         * Validates that string is not null and not blank (whitespace only).
         *
         * @return this builder
         */
        public Builder notBlank() {
            rules.add(new Rule(value -> value != null && !value.isBlank(), "must not be blank"));
            return this;
        }

        /**
         * Validates minimum length.
         *
         * @param minLength
         *            minimum length (inclusive)
         * @return this builder
         */
        public Builder minLength(int minLength) {
            if (minLength < 0) {
                throw new IllegalArgumentException("Min length must be non-negative");
            }
            rules.add(new Rule(
                    value -> value != null && value.length() >= minLength,
                    String.format("must be at least %d characters", minLength)));
            return this;
        }

        /**
         * Validates maximum length.
         *
         * @param maxLength
         *            maximum length (inclusive)
         * @return this builder
         */
        public Builder maxLength(int maxLength) {
            if (maxLength < 0) {
                throw new IllegalArgumentException("Max length must be non-negative");
            }
            rules.add(new Rule(
                    value -> value == null || value.length() <= maxLength,
                    String.format("must be at most %d characters", maxLength)));
            return this;
        }

        /**
         * Validates exact length.
         *
         * @param exactLength
         *            exact length required
         * @return this builder
         */
        public Builder exactLength(int exactLength) {
            if (exactLength < 0) {
                throw new IllegalArgumentException("Exact length must be non-negative");
            }
            rules.add(new Rule(
                    value -> value != null && value.length() == exactLength,
                    String.format("must be exactly %d characters", exactLength)));
            return this;
        }

        /**
         * Validates against regex pattern.
         *
         * @param pattern
         *            regex pattern
         * @param errorMessage
         *            error message if pattern doesn't match
         * @return this builder
         */
        public Builder matches(String pattern, String errorMessage) {
            Pattern compiledPattern = Pattern.compile(pattern);
            rules.add(new Rule(
                    value -> value != null && compiledPattern.matcher(value).matches(), errorMessage));
            return this;
        }

        /**
         * Validates against regex pattern.
         *
         * @param pattern
         *            compiled regex pattern
         * @param errorMessage
         *            error message if pattern doesn't match
         * @return this builder
         */
        public Builder matches(Pattern pattern, String errorMessage) {
            rules.add(new Rule(value -> value != null && pattern.matcher(value).matches(), errorMessage));
            return this;
        }

        /**
         * Validates alphanumeric characters only (a-z, A-Z, 0-9).
         *
         * @return this builder
         */
        public Builder alphanumeric() {
            rules.add(new Rule(
                    value -> value != null && value.matches("^[a-zA-Z0-9]+$"),
                    "must contain only alphanumeric characters"));
            return this;
        }

        /**
         * Validates alphanumeric with underscores and hyphens.
         *
         * @return this builder
         */
        public Builder identifier() {
            rules.add(new Rule(
                    value -> value != null && value.matches("^[a-zA-Z0-9_-]+$"),
                    "must contain only letters, numbers, underscores, and hyphens"));
            return this;
        }

        /**
         * Validates numeric characters only.
         *
         * @return this builder
         */
        public Builder numeric() {
            rules.add(new Rule(
                    value -> value != null && value.matches("^[0-9]+$"), "must contain only numeric characters"));
            return this;
        }

        /**
         * Validates email format (basic).
         *
         * @return this builder
         */
        public Builder email() {
            rules.add(new Rule(
                    value -> value != null && value.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"),
                    "must be a valid email address"));
            return this;
        }

        /**
         * Validates URL format (basic).
         *
         * @return this builder
         */
        public Builder url() {
            rules.add(new Rule(value -> value != null && value.matches("^https?://[^\\s]+$"), "must be a valid URL"));
            return this;
        }

        /**
         * Validates that string starts with prefix.
         *
         * @param prefix
         *            required prefix
         * @return this builder
         */
        public Builder startsWith(String prefix) {
            rules.add(new Rule(
                    value -> value != null && value.startsWith(prefix), String.format("must start with '%s'", prefix)));
            return this;
        }

        /**
         * Validates that string ends with suffix.
         *
         * @param suffix
         *            required suffix
         * @return this builder
         */
        public Builder endsWith(String suffix) {
            rules.add(new Rule(
                    value -> value != null && value.endsWith(suffix), String.format("must end with '%s'", suffix)));
            return this;
        }

        /**
         * Validates that string contains substring.
         *
         * @param substring
         *            required substring
         * @return this builder
         */
        public Builder contains(String substring) {
            rules.add(new Rule(
                    value -> value != null && value.contains(substring),
                    String.format("must contain '%s'", substring)));
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
        public Builder custom(Predicate<String> predicate, String errorMessage) {
            rules.add(new Rule(predicate, errorMessage));
            return this;
        }

        /**
         * Builds the validator.
         *
         * @return configured string validator
         */
        public StringValidator build() {
            return new StringValidator(fieldName, rules);
        }

        /**
         * Builds and immediately validates a value.
         *
         * @param value
         *            value to validate
         * @return validation result
         */
        public ValidationResult validate(String value) {
            return build().validate(value);
        }
    }
}
