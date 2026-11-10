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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AIConstantLocation - IOP annotation specifying where constants must live.
 *
 * <h2>IOP Authority: Constant Location Contract</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  Constants must have ONE SEMANTIC HOME.                                   ║
 * ║  No scattering. No duplication. No ambiguity.                             ║
 * ╠═══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                           ║
 * ║  LOCATION CONTRACT:                                                       ║
 * ║  ─────────────────                                                        ║
 * ║  │ Kind              │ Location Class                │                   ║
 * ║  ├───────────────────┼───────────────────────────────┤                   ║
 * ║  │ Numeric limits    │ Limits, Bounds, Defaults      │                   ║
 * ║  │ String messages   │ Messages                      │                   ║
 * ║  │ Protocol/API      │ *Constants                    │                   ║
 * ║  │ Error codes       │ ErrorCodes                    │                   ║
 * ║  │ Regex patterns    │ Patterns                      │                   ║
 * ║  │ Configuration     │ Defaults                      │                   ║
 * ║  │ Feature flags     │ Features                      │                   ║
 * ║                                                                           ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 *     &#64;code
 *     &#64;AIConstantLocation(Location.LIMITS)
 *     public final class Limits {
 *
 *         public static final int MAX_RETRIES = 3;
 *         public static final int MAX_CONNECTIONS = 100;
 *         public static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
 *
 *         private Limits() {
 *         }
 *     }
 *
 *     &#64;AIConstantLocation(Location.ERROR_CODES)
 *     public final class ErrorCodes {
 *
 *         public static final int INVALID_STATE = 1001;
 *         public static final int TIMEOUT = 1002;
 *         public static final int NOT_FOUND = 1003;
 *
 *         private ErrorCodes() {
 *         }
 *     }
 * }
 * </pre>
 *
 * @since IOP 1.0 - Constant Location
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AIConstantLocation.java}
 * blob {@code 941b47be15de344129a3a9a2425b41d092eb1459} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface AIConstantLocation {

    /**
     * Location type for constants in this class.
     *
     * @return location
     */
    Location value();

    /**
     * Sub-domain (for domain-specific constants).
     *
     * @return domain name
     */
    String domain() default "";

    /** Constant location types. */
    enum Location {
        // --- NUMERIC CONSTANTS ---

        /**
         * Numeric limits (MAX_*, MIN_*).
         * <p>
         * Class names: Limits, Bounds
         */
        LIMITS,

        /**
         * Default values (DEFAULT_*).
         * <p>
         * Class names: Defaults
         */
        DEFAULTS,

        /**
         * Threshold values (THRESHOLD_*).
         * <p>
         * Class names: Thresholds
         */
        THRESHOLDS,

        /**
         * Timeout values (*_TIMEOUT_*, Duration).
         * <p>
         * Class names: Timeouts
         */
        TIMEOUTS,

        // --- STRING CONSTANTS ---

        /**
         * User-facing messages.
         * <p>
         * Class names: Messages
         */
        MESSAGES,

        /**
         * Log message templates.
         * <p>
         * Class names: LogMessages
         */
        LOG_MESSAGES,

        /**
         * Validation messages.
         * <p>
         * Class names: ValidationMessages
         */
        VALIDATION_MESSAGES,

        // --- CODES & IDENTIFIERS ---

        /**
         * Error codes.
         * <p>
         * Class names: ErrorCodes
         */
        ERROR_CODES,

        /**
         * Status codes.
         * <p>
         * Class names: StatusCodes
         */
        STATUS_CODES,

        /**
         * Event types.
         * <p>
         * Class names: EventTypes
         */
        EVENT_TYPES,

        /**
         * HTTP-related constants.
         * <p>
         * Class names: HttpConstants
         */
        HTTP,

        // --- PATTERNS & FORMATS ---

        /**
         * Regex patterns.
         * <p>
         * Class names: Patterns, Regexes
         */
        PATTERNS,

        /**
         * Format strings.
         * <p>
         * Class names: Formats
         */
        FORMATS,

        /**
         * Date/time formats.
         * <p>
         * Class names: DateFormats
         */
        DATE_FORMATS,

        // --- CONFIGURATION ---

        /**
         * Property keys.
         * <p>
         * Class names: PropertyKeys
         */
        PROPERTY_KEYS,

        /**
         * Environment variable names.
         * <p>
         * Class names: EnvVars
         */
        ENV_VARS,

        /**
         * Feature flags.
         * <p>
         * Class names: Features, FeatureFlags
         */
        FEATURE_FLAGS,

        // --- PROTOCOL & API ---

        /**
         * API constants.
         * <p>
         * Class names: *Constants, *ApiConstants
         */
        API,

        /**
         * Header names.
         * <p>
         * Class names: Headers
         */
        HEADERS,

        /**
         * Query parameter names.
         * <p>
         * Class names: QueryParams
         */
        QUERY_PARAMS,

        // --- DOMAIN SPECIFIC ---

        /**
         * Domain-specific constants.
         * <p>
         * Class names: *Constants (e.g., OrderConstants)
         */
        DOMAIN
    }
}
