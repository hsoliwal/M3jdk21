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

package jdk.internal.m3.paradigm.annotation;

/**
 * AnopConstants - Centralized constants for AnOP module.
 *
 * <p>
 * All string literals, magic numbers, and configuration constants are defined
 * here to enforce the "NO FREE STRINGS" principle.
 *
 * <h2>Organization</h2>
 * <ul>
 * <li>Error Messages - Validation and exception messages</li>
 * <li>Field Names - Common field identifiers</li>
 * <li>Model Types - AI model type strings</li>
 * <li>Stability Levels - Code stability indicators</li>
 * <li>Timing Constants - Duration values in seconds</li>
 * <li>Size Limits - Maximum values for collections</li>
 * <li>Hash Lengths - SHA-256 and quick hash sizes</li>
 * <li>Default Values - Configuration defaults</li>
 * </ul>
 *
 * @since m3-anop 1.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/AnopConstants.java}
 * blob {@code ff0ac57453aeea989a22b62275dcd2f505104a55} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class AnopConstants {

    // --- ERROR MESSAGES - Validation and Exception Messages ---

    /**  */
    public static final String ERROR_SPEC_NULL = "spec cannot be null";

    /** Error message for null record ID. */
    public static final String ERROR_RECORD_ID_NULL = "record.id cannot be null";

    /** Error message for null record. */
    public static final String ERROR_RECORD_NULL = "record cannot be null";

    /**  */
    public static final String ERROR_ID_NULL = "id cannot be null";

    /**  */
    public static final String ERROR_SPEC_NAME_NULL = "specName cannot be null";

    /**  */
    public static final String ERROR_SPEC_HASH_NULL = "specHash cannot be null";

    /**  */
    public static final String ERROR_MODEL_NULL = "model cannot be null";

    /**  */
    public static final String ERROR_DATE_FROM_NULL = "from cannot be null";

    /**  */
    public static final String ERROR_DATE_TO_NULL = "to cannot be null";

    /**  */
    public static final String ERROR_SPECS_NULL = "specs cannot be null";

    /** Error message for utility class instantiation. */
    public static final String ERROR_UTILITY_CLASS = "Utility class";

    /**  */
    public static final String ERROR_PARAM_INDEX = "Parameter at index ";

    /**  */
    public static final String ERROR_SUFFIX_CANNOT_BE_NULL = " cannot be null";

    /**  */
    public static final String ERROR_PARAMETER_NULL_AT_INDEX = " cannot be null";

    // --- FIELD NAMES - Common Field Identifiers ---

    /**  */
    public static final String FIELD_MODEL = "model";

    /**  */
    public static final String FIELD_FQCN = "fqcn";

    /**  */
    public static final String FIELD_GENERATION_COUNT = "generationCount";

    /**  */
    public static final String FIELD_SPEC_HASH = "specHash";

    /**  */
    public static final String FIELD_SPEC_NAME = "specName";

    /**  */
    public static final String FIELD_IMPL_NAME = "implName";

    /**  */
    public static final String FIELD_PREV_SPEC_HASH = "prevSpecHash";

    /**  */
    public static final String FIELD_GENERATED_AT = "generatedAt";

    /**  */
    public static final String FIELD_TEST_PASSED = "testPassed";

    /**  */
    public static final String FIELD_TOKENS_USED = "tokensUsed";

    /**  */
    public static final String FIELD_DURATION_MS = "durationMs";

    // --- MODEL TYPES - AI Model Identifiers ---

    /**  */
    public static final String MODEL_OPUS = "OPUS";

    /**  */
    public static final String MODEL_SONNET = "SONNET";

    /**  */
    public static final String MODEL_HAIKU = "HAIKU";

    /**  */
    public static final String MODEL_OLLAMA = "OLLAMA";

    // --- STABILITY LEVELS - Code Stability Indicators ---

    /**  */
    public static final String STABILITY_ALPHA = "ALPHA";

    /**  */
    public static final String STABILITY_BETA = "BETA";

    /**  */
    public static final String STABILITY_STABLE = "STABLE";

    /**  */
    public static final String STABILITY_FROZEN = "FROZEN";

    // --- REGISTRY TYPES - Generation History Storage ---

    /**  */
    public static final String REGISTRY_JSONL = "JSONL";

    /**  */
    public static final String REGISTRY_SQLITE = "SQLITE";

    /**  */
    public static final String REGISTRY_BOTH = "BOTH";

    /**  */
    public static final String REGISTRY_NONE = "NONE";

    // --- TIMING CONSTANTS - Duration Values (in Seconds) ---

    /**  */
    public static final int DURATION_7_DAYS_SECONDS = 7 * 24 * 60 * 60;

    /**  */
    public static final int DURATION_30_DAYS_SECONDS = 30 * 24 * 60 * 60;

    /**  */
    public static final int DEFAULT_MAX_RETRIES = 3;

    // --- SIZE LIMITS - Maximum Values for Collections and Strings ---

    /**  */
    public static final int DEFAULT_MAX_HISTORY_SIZE = 100;

    /**  */
    public static final int SHA_256_PREFIX_LENGTH = 7;

    /**  */
    public static final int QUICK_HASH_START_INDEX = 7;

    /**  */
    public static final int QUICK_HASH_END_INDEX = 23;

    /**  */
    public static final int QUICK_HASH_LENGTH = 16;

    /**  */
    public static final int MIN_HASH_LENGTH_FOR_QUICK = 23;

    /**  */
    public static final String DEFAULT_CLASS_SUFFIX = "Impl";

    /**  */
    public static final String DEFAULT_PACKAGE_SUFFIX = "impl";

    // --- LOGGING AND DEBUG CONSTANTS ---

    /**  */
    public static final String LOG_PROCESSING_SPEC = "Processing spec: { }";

    /**  */
    public static final String LOG_COMPLETED_SPEC = "Completed spec: { } with status: { }";

    /**  */
    public static final String LOG_FAILED_SPEC = "Failed to process spec: { }";

    /**  */
    public static final String LOG_RECORDING_GENERATION = "Recording generation for spec: { }";

    /**  */
    public static final String LOG_RECORDED_GENERATION = "Recorded generation { } for { }";

    /**  */
    public static final String LOG_NO_PREVIOUS_GENERATION = "No previous generation found for { }";

    /**  */
    public static final String LOG_FROZEN_SPEC = "Spec { } is frozen, skipping regeneration";

    /**  */
    public static final String LOG_QUICK_HASH_UNCHANGED = "Quick hash unchanged for { }";

    /**  */
    public static final String LOG_FULL_HASH_UNCHANGED = "Full hash unchanged for { }";

    /**  */
    public static final String LOG_SPEC_CHANGED = "Spec { } has changed, regeneration needed";

    /**  */
    public static final String LOG_GENERATION_STARTED = "Generating implementation for { }";

    /**  */
    public static final String LOG_GENERATION_COMPLETED = "Generated { } in { }ms, tokens: { }";

    /**  */
    public static final String LOG_RUNNING_TESTS = "Running tests for { }";

    // --- PROCESSING STATUS MESSAGES ---

    /**  */
    public static final String STATUS_FROZEN = "Frozen - never regenerate";

    /**  */
    public static final String STATUS_QUICK_HASH_UNCHANGED = "Quick hash unchanged";

    /**  */
    public static final String STATUS_FULL_HASH_UNCHANGED = "Full hash unchanged";

    /**  */
    public static final String STATUS_NO_PREVIOUS = "No previous generation";

    /**  */
    public static final String STATUS_SPEC_CHANGED = "Spec changed";

    /**  */
    public static final String LOG_PROCESSING_STARTED = "Processing batch of { } specs";

    // --- PRIVATE CONSTRUCTOR ---

    /**  */
    private AnopConstants() {
        throw new UnsupportedOperationException(ERROR_UTILITY_CLASS);
    }
}
