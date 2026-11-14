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

import java.util.List;
/**
 * Examples demonstrating how to extract complex validation logic to Validator
 * classes.
 *
 * <p>
 * This class shows before/after examples of common validation patterns and how
 * to refactor them using the validation framework.
 * </p>
 *
 * @author Hitesh Soliwal
 * @since 1.0.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/validation/ValidationExamples.java}
 * blob {@code 5ab797157fcbfd5d57a386846b1943272f6d1be3} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common batch b29 (slf4j owners on the PlaceholderLogger facade) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@SuppressWarnings("checkstyle:MagicNumber") // Example code uses literal numbers for clarity
public final class ValidationExamples {
    private static final PlaceholderLogger log = PlaceholderLogger.of(ValidationExamples.class);


    /** Maximum model name length. */
    private static final int MAX_MODEL_LENGTH = 100;

    /** Minimum session ID length. */
    private static final int MIN_SESSION_ID_LENGTH = 3;

    /** Maximum token limit. */
    private static final int MAX_TOKENS = 100000;

    /** Maximum number of tools allowed. */
    private static final int MAX_TOOLS = 50;

    /** Maximum number of tags allowed. */
    private static final int MAX_TAGS = 20;

    /** Benefits documentation placeholder. */
    private static final String BENEFITS = "See examples above";

    private ValidationExamples() {
        // Utility class - examples only
    }

    // --- Example 1: String Validation - Model/Session ID ---

    /** BEFORE: Inline validation with multiple checks. */
    public static class BeforeStringValidation {
        /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
        public BeforeStringValidation() {
        }


        /**
         * Sets the model with inline validation.
         *
         * @param model
         *            the model name
         */
        public void setModel(String model) {
            if (model == null || model.isBlank()) {
                throw new IllegalArgumentException("Model must not be blank");
            }
            if (model.length() > MAX_MODEL_LENGTH) {
                throw new IllegalArgumentException(
                        "Model must be at most " + MAX_MODEL_LENGTH + " characters (got " + model.length() + ")");
            }
            // ... use model
        }

        /**
         * Sets the session ID with inline validation.
         *
         * @param sessionId
         *            the session identifier
         */
        public void setSessionId(String sessionId) {
            if (sessionId == null || sessionId.isBlank()) {
                throw new IllegalArgumentException("Session ID must not be blank");
            }
            if (sessionId.length() < MIN_SESSION_ID_LENGTH) {
                throw new IllegalArgumentException(
                        "Session ID must be at least " + MIN_SESSION_ID_LENGTH + " characters");
            }
            if (!sessionId.matches("^[a-zA-Z0-9_-]+$")) {
                throw new IllegalArgumentException(
                        "Session ID must contain only letters, numbers, underscores, and hyphens");
            }
            // ... use sessionId
        }
    }

    /** AFTER: Extract to reusable validators. */
    public static class AfterStringValidation {
        /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
        public AfterStringValidation() {
        }


        // Define validators as static final (reusable)
        private static final StringValidator MODEL_VALIDATOR = StringValidator.builder("model")
                .notBlank()
                .maxLength(MAX_MODEL_LENGTH)
                .build();

        private static final StringValidator SESSION_ID_VALIDATOR = StringValidator.builder("sessionId")
                .notBlank()
                .minLength(MIN_SESSION_ID_LENGTH)
                .identifier() // Alphanumeric + underscore + hyphen
                .build();

        /**
         * Sets the model using validator.
         *
         * @param model
         *            the model name
         */
        public void setModel(String model) {
            MODEL_VALIDATOR.validate(model).orElseThrow();
            // ... use model
        }

        /**
         * Sets the session ID using validator.
         *
         * @param sessionId
         *            the session identifier
         */
        public void setSessionId(String sessionId) {
            SESSION_ID_VALIDATOR.validate(sessionId).orElseThrow();
            // ... use sessionId
        }
    }

    // --- Example 2: Numeric Validation - Token Limits ---

    /** BEFORE: Inline numeric validation with multiple checks. */
    public static class BeforeNumericValidation {
        /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
        public BeforeNumericValidation() {
        }


        /**
         * Sets max tokens with inline validation.
         *
         * @param maxTokens
         *            the maximum token count
         */
        public void setMaxTokens(int maxTokens) {
            if (maxTokens < 0) {
                throw new IllegalArgumentException("Max tokens must be non-negative");
            }
            if (maxTokens > MAX_TOKENS) {
                throw new IllegalArgumentException("Max tokens must be at most " + MAX_TOKENS);
            }
            // ... use maxTokens
        }

        /**
         * Sets row count with inline validation.
         *
         * @param rowCount
         *            the row count
         */
        public void setRowCount(long rowCount) {
            if (rowCount < 0) {
                throw new IllegalArgumentException("Row count must be non-negative: " + rowCount);
            }
            // ... use rowCount
        }
    }

    /** AFTER: Extract to reusable validators. */
    public static class AfterNumericValidation {
        /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
        public AfterNumericValidation() {
        }


        private static final NumericValidator.IntValidator MAX_TOKENS_VALIDATOR = NumericValidator.intBuilder(
                        "maxTokens")
                .nonNegative()
                .max(MAX_TOKENS)
                .build();

        private static final NumericValidator.LongValidator ROW_COUNT_VALIDATOR =
                NumericValidator.longBuilder("rowCount").nonNegative().build();

        /**
         * Sets max tokens using validator.
         *
         * @param maxTokens
         *            the maximum token count
         */
        public void setMaxTokens(int maxTokens) {
            MAX_TOKENS_VALIDATOR.validate(maxTokens).orElseThrow();
            // ... use maxTokens
        }

        /**
         * Sets row count using validator.
         *
         * @param rowCount
         *            the row count
         */
        public void setRowCount(long rowCount) {
            ROW_COUNT_VALIDATOR.validate(rowCount).orElseThrow();
            // ... use rowCount
        }
    }

    // --- Example 3: Collection Validation - Column Names ---

    /** BEFORE: Inline collection validation with complex checks. */
    public static class BeforeCollectionValidation {
        /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
        public BeforeCollectionValidation() {
        }


        /**
         * Sets columns with inline validation.
         *
         * @param columns
         *            the column names
         */
        public void setColumns(String[] columns) {
            if (columns == null || columns.length == 0) {
                throw new IllegalArgumentException("Columns array must not be null or empty");
            }
            if (java.util.Arrays.stream(columns).anyMatch(col -> col == null || col.isBlank())) {
                throw new IllegalArgumentException("Column names must not be blank");
            }
            // ... use columns
        }

        /**
         * Sets allowed tools with inline validation.
         *
         * @param tools
         *            the list of allowed tools
         */
        public void setAllowedTools(List<String> tools) {
            if (tools == null) {
                throw new IllegalArgumentException("Tools list must not be null");
            }
            if (tools.isEmpty()) {
                throw new IllegalArgumentException("Tools list must not be empty");
            }
            if (tools.size() > MAX_TOOLS) {
                throw new IllegalArgumentException("Tools list must contain at most " + MAX_TOOLS + " elements");
            }
            // ... use tools
        }
    }

    /** AFTER: Extract to reusable validators. */
    public static class AfterCollectionValidation {
        /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
        public AfterCollectionValidation() {
        }


        private static final CollectionValidator<String> COLUMNS_VALIDATOR = CollectionValidator.<String>builder(
                        "columns")
                .notEmpty()
                .allElementsMatch(col -> col != null && !col.isBlank(), "Column names must not be blank")
                .build();

        private static final CollectionValidator<String> TOOLS_VALIDATOR = CollectionValidator.<String>builder(
                        "allowedTools")
                .notNull()
                .notEmpty()
                .maxSize(MAX_TOOLS)
                .build();

        /**
         * Sets columns using validator.
         *
         * @param columns
         *            the column names
         */
        public void setColumns(String[] columns) {
            // Convert array to list for validation
            COLUMNS_VALIDATOR
                    .validate(columns == null ? null : List.of(columns))
                    .orElseThrow();
            // ... use columns
        }

        /**
         * Sets allowed tools using validator.
         *
         * @param tools
         *            the list of allowed tools
         */
        public void setAllowedTools(List<String> tools) {
            TOOLS_VALIDATOR.validate(tools).orElseThrow();
            // ... use tools
        }
    }

    // --- Example 4: Hash Validation - Integrity Checks ---

    /** BEFORE: Inline hash validation with manual checks. */
    public static class BeforeHashValidation {
        /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
        public BeforeHashValidation() {
        }


        private static final int SHA256_HEX_LENGTH = 64;
        private static final int HASH64_HEX_LENGTH = 16;

        /**
         * Validates a row hash.
         *
         * @param hash
         *            the SHA-256 hash
         */
        public void validateRowHash(String hash) {
            if (hash == null || hash.length() != SHA256_HEX_LENGTH) {
                throw new IllegalArgumentException("Hash must be exactly " + SHA256_HEX_LENGTH + " characters");
            }
            if (!hash.chars().allMatch(c -> (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'))) {
                throw new IllegalArgumentException("Hash must contain only lowercase hexadecimal characters");
            }
            // ... use hash
        }

        /**
         * Validates a fast hash.
         *
         * @param hash
         *            the 64-bit hash
         */
        public void validateFastHash(String hash) {
            if (hash == null || hash.length() != HASH64_HEX_LENGTH) {
                throw new IllegalArgumentException("Hash must be exactly " + HASH64_HEX_LENGTH + " characters");
            }
            if (!hash.chars().allMatch(c -> (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'))) {
                throw new IllegalArgumentException("Hash must contain only lowercase hexadecimal characters");
            }
            // ... use hash
        }
    }

    /** AFTER: Use HashValidator. */
    public static class AfterHashValidation {
        /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
        public AfterHashValidation() {
        }


        /**
         * Validates a row hash using HashValidator.
         *
         * @param hash
         *            the SHA-256 hash
         */
        public void validateRowHash(String hash) {
            HashValidator.validateSha256(hash).orElseThrow();
            // ... use hash
        }

        /**
         * Validates a fast hash using HashValidator.
         *
         * @param hash
         *            the 64-bit hash
         */
        public void validateFastHash(String hash) {
            HashValidator.validateHash64(hash).orElseThrow();
            // ... use hash
        }
    }

    // --- Example 5: Complex Validation - Builder Pattern ---

    /** BEFORE: Builder with inline validation scattered throughout. */
    public static class BeforeBuilderValidation {
        /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
        public BeforeBuilderValidation() {
        }


        /** Configuration builder with inline validation. */
        public static class ConfigBuilder {
            /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
            public ConfigBuilder() {
            }


            private String model;
            private int maxTokens;
            private List<String> tags;

            /**
             * Sets the model.
             *
             * @param model
             *            the model name
             * @return this builder
             */
            public ConfigBuilder model(String model) {
                if (model == null || model.isBlank()) {
                    throw new IllegalArgumentException("Model must not be blank");
                }
                this.model = model;
                return this;
            }

            /**
             * Sets the max tokens.
             *
             * @param maxTokens
             *            the token limit
             * @return this builder
             */
            public ConfigBuilder maxTokens(int maxTokens) {
                if (maxTokens < 1 || maxTokens > MAX_TOKENS) {
                    throw new IllegalArgumentException("Max tokens must be between 1 and " + MAX_TOKENS);
                }
                this.maxTokens = maxTokens;
                return this;
            }

            /**
             * Sets the tags.
             *
             * @param tags
             *            the tag list
             * @return this builder
             */
            public ConfigBuilder tags(List<String> tags) {
                if (tags == null) {
                    throw new IllegalArgumentException("Tags must not be null");
                }
                if (tags.isEmpty()) {
                    throw new IllegalArgumentException("Tags must not be empty");
                }
                if (tags.size() > MAX_TAGS) {
                    throw new IllegalArgumentException("Tags must contain at most " + MAX_TAGS + " elements");
                }
                this.tags = tags;
                return this;
            }
        }
    }

    /** AFTER: Builder with extracted validators. */
    public static class AfterBuilderValidation {
        /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
        public AfterBuilderValidation() {
        }


        /** Configuration builder with validator-based validation. */
        public static class ConfigBuilder {
            /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
            public ConfigBuilder() {
            }


            // Validators defined once, reused across all instances
            private static final StringValidator MODEL_VALIDATOR =
                    StringValidator.builder("model").notBlank().build();

            private static final NumericValidator.IntValidator MAX_TOKENS_VALIDATOR = NumericValidator.intBuilder(
                            "maxTokens")
                    .between(1, MAX_TOKENS)
                    .build();

            private static final CollectionValidator<String> TAGS_VALIDATOR = CollectionValidator.<String>builder(
                            "tags")
                    .notNull()
                    .notEmpty()
                    .maxSize(MAX_TAGS)
                    .build();

            private String model;
            private int maxTokens;
            private List<String> tags;

            /**
             * Sets the model using validator.
             *
             * @param model
             *            the model name
             * @return this builder
             */
            public ConfigBuilder model(String model) {
                MODEL_VALIDATOR.validate(model).orElseThrow();
                this.model = model;
                return this;
            }

            /**
             * Sets the max tokens using validator.
             *
             * @param maxTokens
             *            the token limit
             * @return this builder
             */
            public ConfigBuilder maxTokens(int maxTokens) {
                MAX_TOKENS_VALIDATOR.validate(maxTokens).orElseThrow();
                this.maxTokens = maxTokens;
                return this;
            }

            /**
             * Sets the tags using validator.
             *
             * @param tags
             *            the tag list
             * @return this builder
             */
            public ConfigBuilder tags(List<String> tags) {
                TAGS_VALIDATOR.validate(tags).orElseThrow();
                this.tags = tags;
                return this;
            }
        }
    }

    // --- Benefits Summary ---

    /**
     * Benefits of extracting validation to Validator classes:
     *
     * <ol>
     * <li><strong>Reusability:</strong> Define validation once, use everywhere</li>
     * <li><strong>Consistency:</strong> Same validation rules across codebase</li>
     * <li><strong>Clear Error Messages:</strong> Standardized, actionable error
     * messages</li>
     * <li><strong>Testability:</strong> Validators can be unit tested
     * independently</li>
     * <li><strong>Maintainability:</strong> Change validation rules in one
     * place</li>
     * <li><strong>Readability:</strong> Self-documenting validation rules</li>
     * <li><strong>DRY:</strong> Don't repeat validation logic</li>
     * <li><strong>Single Responsibility:</strong> Validators have one job</li>
     * </ol>
     *
     * @return benefits documentation string
     */
    public static String getBenefits() {
        return BENEFITS;
    }
}
