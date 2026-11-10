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
 * 	&#64;code
 * 	&#64;AIConstantLocation(Location.LIMITS)
 * 	public final class Limits {
 *
 * 		public static final int MAX_RETRIES = 3;
 * 		public static final int MAX_CONNECTIONS = 100;
 * 		public static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
 *
 * 		private Limits() {
 * 		}
 * 	}
 *
 * 	&#64;AIConstantLocation(Location.ERROR_CODES)
 * 	public final class ErrorCodes {
 *
 * 		public static final int INVALID_STATE = 1001;
 * 		public static final int TIMEOUT = 1002;
 * 		public static final int NOT_FOUND = 1003;
 *
 * 		private ErrorCodes() {
 * 		}
 * 	}
 * }
 * </pre>
 *
 * @since IOP 1.0 - Constant Location
 * @m3.pattern synexia:iop
 * @m3.algorithm Declares the semantic home and optional subdomain for a class of constants. The annotation records metadata and does not itself perform enforcement or generation.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.TYPE}, preserving the declared element and nested-enum schema.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAIConstantLocation.java}
 * blob {@code 03481722d31b4736c74b7d1b86dbfc351b4e2ef1} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IAIConstantLocation {

    /**
     * Location type for constants in this class.
     *
     * @return location
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares IAIConstantLocation#value metadata for Location type for constants in this class. Consuming tooling must interpret this value; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Location}, requiring an explicit value with no default.
     */
    Location value();

    /**
     * Sub-domain (for domain-specific constants).
     *
     * @return domain name
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares IAIConstantLocation#domain metadata for Sub-domain (for domain-specific constants). Consuming tooling must interpret this value; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}, with exact default {@code ""}.
     */
    String domain() default "";

    /** Constant location types.
     * @m3.pattern synexia:iop
     * @m3.algorithm Enumerates 21 symbolic choices for semantic constant home; interpretation belongs to consuming tooling.
     * @m3.dataStructures Nested IAIConstantLocation.Location enum with the existing 21 constants in declaration order.
     */
    enum Location {
        // --- NUMERIC CONSTANTS ---

        /**
         * Numeric limits (MAX_*, MIN_*).
         * <p>
         * Class names: Limits, Bounds
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the IAIConstantLocation.Location#LIMITS choice for semantic constant home: Numeric limits (MAX_*, MIN_*). <p> Class names: Limits, Bounds The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIConstantLocation.Location, preserving its declared position and identity.
         */
        LIMITS,

        /**
         * Default values (DEFAULT_*).
         * <p>
         * Class names: Defaults
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the IAIConstantLocation.Location#DEFAULTS choice for semantic constant home: Default values (DEFAULT_*). <p> Class names: Defaults The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIConstantLocation.Location, preserving its declared position and identity.
         */
        DEFAULTS,

        /**
         * Threshold values (THRESHOLD_*).
         * <p>
         * Class names: Thresholds
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the IAIConstantLocation.Location#THRESHOLDS choice for semantic constant home: Threshold values (THRESHOLD_*). <p> Class names: Thresholds The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIConstantLocation.Location, preserving its declared position and identity.
         */
        THRESHOLDS,

        /**
         * Timeout values (*_TIMEOUT_*, Duration).
         * <p>
         * Class names: Timeouts
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the IAIConstantLocation.Location#TIMEOUTS choice for semantic constant home: Timeout values (*_TIMEOUT_*, Duration). <p> Class names: Timeouts The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIConstantLocation.Location, preserving its declared position and identity.
         */
        TIMEOUTS,

        // --- STRING CONSTANTS ---

        /**
         * User-facing messages.
         * <p>
         * Class names: Messages
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the IAIConstantLocation.Location#MESSAGES choice for semantic constant home: User-facing messages. <p> Class names: Messages The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIConstantLocation.Location, preserving its declared position and identity.
         */
        MESSAGES,

        /**
         * Log message templates.
         * <p>
         * Class names: LogMessages
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the IAIConstantLocation.Location#LOG_MESSAGES choice for semantic constant home: Log message templates. <p> Class names: LogMessages The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIConstantLocation.Location, preserving its declared position and identity.
         */
        LOG_MESSAGES,

        /**
         * Validation messages.
         * <p>
         * Class names: ValidationMessages
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the IAIConstantLocation.Location#VALIDATION_MESSAGES choice for semantic constant home: Validation messages. <p> Class names: ValidationMessages The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIConstantLocation.Location, preserving its declared position and identity.
         */
        VALIDATION_MESSAGES,

        // --- CODES & IDENTIFIERS ---

        /**
         * Error codes.
         * <p>
         * Class names: ErrorCodes
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the IAIConstantLocation.Location#ERROR_CODES choice for semantic constant home: Error codes. <p> Class names: ErrorCodes The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIConstantLocation.Location, preserving its declared position and identity.
         */
        ERROR_CODES,

        /**
         * Status codes.
         * <p>
         * Class names: StatusCodes
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the IAIConstantLocation.Location#STATUS_CODES choice for semantic constant home: Status codes. <p> Class names: StatusCodes The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIConstantLocation.Location, preserving its declared position and identity.
         */
        STATUS_CODES,

        /**
         * Event types.
         * <p>
         * Class names: EventTypes
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the IAIConstantLocation.Location#EVENT_TYPES choice for semantic constant home: Event types. <p> Class names: EventTypes The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIConstantLocation.Location, preserving its declared position and identity.
         */
        EVENT_TYPES,

        /**
         * HTTP-related constants.
         * <p>
         * Class names: HttpConstants
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the IAIConstantLocation.Location#HTTP choice for semantic constant home: HTTP-related constants. <p> Class names: HttpConstants The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIConstantLocation.Location, preserving its declared position and identity.
         */
        HTTP,

        // --- PATTERNS & FORMATS ---

        /**
         * Regex patterns.
         * <p>
         * Class names: Patterns, Regexes
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the IAIConstantLocation.Location#PATTERNS choice for semantic constant home: Regex patterns. <p> Class names: Patterns, Regexes The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIConstantLocation.Location, preserving its declared position and identity.
         */
        PATTERNS,

        /**
         * Format strings.
         * <p>
         * Class names: Formats
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the IAIConstantLocation.Location#FORMATS choice for semantic constant home: Format strings. <p> Class names: Formats The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIConstantLocation.Location, preserving its declared position and identity.
         */
        FORMATS,

        /**
         * Date/time formats.
         * <p>
         * Class names: DateFormats
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the IAIConstantLocation.Location#DATE_FORMATS choice for semantic constant home: Date/time formats. <p> Class names: DateFormats The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIConstantLocation.Location, preserving its declared position and identity.
         */
        DATE_FORMATS,

        // --- CONFIGURATION ---

        /**
         * Property keys.
         * <p>
         * Class names: PropertyKeys
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the IAIConstantLocation.Location#PROPERTY_KEYS choice for semantic constant home: Property keys. <p> Class names: PropertyKeys The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIConstantLocation.Location, preserving its declared position and identity.
         */
        PROPERTY_KEYS,

        /**
         * Environment variable names.
         * <p>
         * Class names: EnvVars
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the IAIConstantLocation.Location#ENV_VARS choice for semantic constant home: Environment variable names. <p> Class names: EnvVars The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIConstantLocation.Location, preserving its declared position and identity.
         */
        ENV_VARS,

        /**
         * Feature flags.
         * <p>
         * Class names: Features, FeatureFlags
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the IAIConstantLocation.Location#FEATURE_FLAGS choice for semantic constant home: Feature flags. <p> Class names: Features, FeatureFlags The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIConstantLocation.Location, preserving its declared position and identity.
         */
        FEATURE_FLAGS,

        // --- PROTOCOL & API ---

        /**
         * API constants.
         * <p>
         * Class names: *Constants, *ApiConstants
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the IAIConstantLocation.Location#API choice for semantic constant home: API constants. <p> Class names: *Constants, *ApiConstants The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIConstantLocation.Location, preserving its declared position and identity.
         */
        API,

        /**
         * Header names.
         * <p>
         * Class names: Headers
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the IAIConstantLocation.Location#HEADERS choice for semantic constant home: Header names. <p> Class names: Headers The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIConstantLocation.Location, preserving its declared position and identity.
         */
        HEADERS,

        /**
         * Query parameter names.
         * <p>
         * Class names: QueryParams
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the IAIConstantLocation.Location#QUERY_PARAMS choice for semantic constant home: Query parameter names. <p> Class names: QueryParams The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIConstantLocation.Location, preserving its declared position and identity.
         */
        QUERY_PARAMS,

        // --- DOMAIN SPECIFIC ---

        /**
         * Domain-specific constants.
         * <p>
         * Class names: *Constants (e.g., OrderConstants)
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the IAIConstantLocation.Location#DOMAIN choice for semantic constant home: Domain-specific constants. <p> Class names: *Constants (e.g., OrderConstants) The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIConstantLocation.Location, preserving its declared position and identity.
         */
        DOMAIN
    }
}
