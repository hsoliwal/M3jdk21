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
/**
 * Validator for hash strings (SHA-256, xxHash64).
 *
 * <p>
 * Provides validation for different hash formats with clear error messages.
 * Single responsibility: validates hash format correctness.
 * </p>
 *
 * <h2>Supported Hash Types</h2>
 * <ul>
 * <li>SHA-256: 64-character lowercase hexadecimal</li>
 * <li>xxHash64: 16-character lowercase hexadecimal</li>
 * <li>Generic hexadecimal strings</li>
 * </ul>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * // Validate SHA-256 hash
 * ValidationResult result = HashValidator.validateSha256(hash);
 * if (!result.isValid()) {
 * 	throw new IllegalArgumentException(result.getErrorMessage());
 * }
 *
 * // Validate xxHash64
 * ValidationResult result = HashValidator.validateHash64(hash);
 * result.orElseThrow(); // Throws if invalid
 *
 * // Batch validation
 * List<String> hashes = fetchHashes();
 * hashes.stream().map(HashValidator::validateSha256).filter(ValidationResult::isInvalid)
 * 		.forEach(r -> log.error("Invalid hash: {}", r.getErrorMessage()));
 * }</pre>
 *
 * @author Hitesh Soliwal
 * @since 1.0.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/validation/HashValidator.java}
 * blob {@code dd93e1d9fe82554c80f2cec775373298d12d9197} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common batch b29 (slf4j owners on the PlaceholderLogger facade) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class HashValidator {
    private static final PlaceholderLogger log = PlaceholderLogger.of(HashValidator.class);


    /** SHA-256 hash length in hex characters (32 bytes = 64 hex chars). */
    private static final int SHA256_HEX_LENGTH = 64;

    /** xxHash64 hash length in hex characters (8 bytes = 16 hex chars). */
    private static final int HASH64_HEX_LENGTH = 16;

    private HashValidator() {
        // Utility class - no instantiation
    }

    /**
     * Validates SHA-256 hash format.
     *
     * @param hash
     *            hash to validate
     * @return validation result
     */
    public static ValidationResult validateSha256(String hash) {
        return validateHash(hash, SHA256_HEX_LENGTH, "SHA-256");
    }

    /**
     * Validates xxHash64 format.
     *
     * @param hash
     *            hash to validate
     * @return validation result
     */
    public static ValidationResult validateHash64(String hash) {
        return validateHash(hash, HASH64_HEX_LENGTH, "xxHash64");
    }

    /**
     * Validates hexadecimal hash with specific length.
     *
     * @param hash
     *            hash to validate
     * @param expectedLength
     *            expected length in hex characters
     * @param hashType
     *            hash type name (for error messages)
     * @return validation result
     */
    public static ValidationResult validateHash(String hash, int expectedLength, String hashType) {
        List<String> errors = new ArrayList<>();

        if (hash == null) {
            errors.add("hash must not be null");
            return new ValidationResult(false, hashType, hash, errors);
        }

        if (hash.length() != expectedLength) {
            errors.add(String.format("hash must be exactly %d characters (got %d)", expectedLength, hash.length()));
        }

        if (!isHexString(hash)) {
            errors.add("hash must contain only lowercase hexadecimal characters (0-9, a-f)");
        }

        return new ValidationResult(errors.isEmpty(), hashType, hash, errors);
    }

    /**
     * Validates hexadecimal string format (any length).
     *
     * @param hex
     *            hex string to validate
     * @return validation result
     */
    public static ValidationResult validateHexString(String hex) {
        List<String> errors = new ArrayList<>();

        if (hex == null) {
            errors.add("hex string must not be null");
            return new ValidationResult(false, "hex", hex, errors);
        }

        if (hex.isEmpty()) {
            errors.add("hex string must not be empty");
        }

        if (!isHexString(hex)) {
            errors.add("must contain only lowercase hexadecimal characters (0-9, a-f)");
        }

        return new ValidationResult(errors.isEmpty(), "hex", hex, errors);
    }

    /**
     * Checks if string contains only lowercase hexadecimal characters.
     *
     * @param str
     *            string to check
     * @return true if all characters are hex digits (0-9, a-f)
     */
    private static boolean isHexString(String str) {
        if (str == null) {
            return false;
        }
        return str.chars().allMatch(c -> (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'));
    }

    /** Validation result for hash validation. */
    public static final class ValidationResult {

        private final boolean valid;
        private final String hashType;
        private final String value;
        private final List<String> errors;

        private ValidationResult(boolean valid, String hashType, String value, List<String> errors) {
            this.valid = valid;
            this.hashType = hashType;
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
         * Gets the hash type.
         *
         * @return hash type
         */
        public String getHashType() {
            return hashType;
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
            return String.format("%s hash validation failed: %s", hashType, String.join("; ", errors));
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
}
