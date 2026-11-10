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

import jdk.internal.m3.paradigm.annotation.IFieldSpec;
import jdk.internal.m3.paradigm.annotation.Model;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * IConfigSpec - Configuration specification for AI code generation.
 *
 * <h2>Configuration Patterns</h2>
 *
 * <pre>
 * Configuration patterns in Java:
 *   - Spring @Value, @ConfigurationProperties
 *   - MicroProfile Config
 *   - Apache Commons Configuration
 *   - Environment variables
 *   - YAML/Properties files
 *   - Typesafe Config (HOCON)
 *
 * &#64;IConfigSpec specifies configuration binding in IOP style.
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * // Configuration class
 * &#64;IConfigSpec(
 *     prefix = "app.database",
 *     source = YAML,
 *     validate = true
 * )
 * public class DatabaseConfig {
 *
 *     &#64;IConfigSpec.Property(key = "url", required = true)
 *     private String url;
 *
 *     &#64;IConfigSpec.Property(key = "username", defaultValue = "admin")
 *     private String username;
 *
 * &#64;IConfigSpec.Property(key = "pool.size", min = 1, max = 100)
 * private int poolSize = 10;
 * }
 *
 * // Field-level config
 * &#64;IConfigSpec(key = "app.feature.enabled", defaultValue = "false", refresh = true)
 * private boolean featureEnabled;
 * }
 * </pre>
 *
 * @since IOP 1.0 - Configuration Specification
 * @see IFieldSpec
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares configuration keys, sources, defaults, validation bounds, refresh and sensitive-value hints; it reads no file or environment variable and performs no decryption or refresh.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code {ElementType.TYPE, ElementType.FIELD}}; existing element and enum schemas are preserved.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IConfigSpec.java}
 * blob {@code b3a1e48858304f70d6263c1251ad756773efc14b} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IConfigSpec {

    // --- SIMPLE OPTIONS ---

    /**
     * Predefined config pattern.
     *
     * @return option
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IConfigSpec#option intent: Predefined config pattern. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Option}; default expression {@code Option.NONE}.
     */
    Option option() default Option.NONE;

    /** Common config patterns.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IConfigSpec.Option choices described as Common config patterns. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 7 constants in the existing source order.
     */
    enum Option {
        /** No preset - configure manually.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IConfigSpec.Option#NONE policy choice: No preset - configure manually. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IConfigSpec.Option; its declared order and identity are preserved.
         */
        NONE,

        /** Properties file binding.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IConfigSpec.Option#PROPERTIES policy choice: Properties file binding. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IConfigSpec.Option; its declared order and identity are preserved.
         */
        PROPERTIES,

        /** YAML file binding.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IConfigSpec.Option#YAML policy choice: YAML file binding. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IConfigSpec.Option; its declared order and identity are preserved.
         */
        YAML,

        /** Environment variables.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IConfigSpec.Option#ENV policy choice: Environment variables. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IConfigSpec.Option; its declared order and identity are preserved.
         */
        ENV,

        /** System properties.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IConfigSpec.Option#SYSTEM policy choice: System properties. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IConfigSpec.Option; its declared order and identity are preserved.
         */
        SYSTEM,

        /** JSON file.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IConfigSpec.Option#JSON policy choice: JSON file. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IConfigSpec.Option; its declared order and identity are preserved.
         */
        JSON,

        /** Vault/secrets.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IConfigSpec.Option#VAULT policy choice: Vault/secrets. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IConfigSpec.Option; its declared order and identity are preserved.
         */
        VAULT
    }

    // --- KEY/PREFIX ---

    /**
     * Configuration key (for field).
     *
     * @return key
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IConfigSpec#key intent: Configuration key (for field). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String key() default "";

    /**
     * Configuration prefix (for class).
     *
     * @return prefix
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IConfigSpec#prefix intent: Configuration prefix (for class). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String prefix() default "";

    // --- SOURCE ---

    /**
     * Configuration source.
     *
     * @return source
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IConfigSpec#source intent: Configuration source. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Source}; default expression {@code Source.AUTO}.
     */
    Source source() default Source.AUTO;

    /** Configuration sources.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IConfigSpec.Source choices described as Configuration sources. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 8 constants in the existing source order.
     */
    enum Source {
        /** Auto-detect.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IConfigSpec.Source#AUTO policy choice: Auto-detect. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IConfigSpec.Source; its declared order and identity are preserved.
         */
        AUTO,

        /** Properties file.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IConfigSpec.Source#PROPERTIES policy choice: Properties file. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IConfigSpec.Source; its declared order and identity are preserved.
         */
        PROPERTIES,

        /** YAML file.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IConfigSpec.Source#YAML policy choice: YAML file. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IConfigSpec.Source; its declared order and identity are preserved.
         */
        YAML,

        /** JSON file.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IConfigSpec.Source#JSON policy choice: JSON file. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IConfigSpec.Source; its declared order and identity are preserved.
         */
        JSON,

        /** Environment variable.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IConfigSpec.Source#ENVIRONMENT policy choice: Environment variable. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IConfigSpec.Source; its declared order and identity are preserved.
         */
        ENVIRONMENT,

        /** System property.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IConfigSpec.Source#SYSTEM policy choice: System property. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IConfigSpec.Source; its declared order and identity are preserved.
         */
        SYSTEM,

        /** Vault/secrets manager.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IConfigSpec.Source#VAULT policy choice: Vault/secrets manager. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IConfigSpec.Source; its declared order and identity are preserved.
         */
        VAULT,

        /** Remote config server.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IConfigSpec.Source#REMOTE policy choice: Remote config server. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IConfigSpec.Source; its declared order and identity are preserved.
         */
        REMOTE
    }

    /**
     * Source file path.
     *
     * @return path
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IConfigSpec#path intent: Source file path. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String path() default "";

    // --- DEFAULT VALUE ---

    /**
     * Default value if not configured.
     *
     * @return default value
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IConfigSpec#defaultValue intent: Default value if not configured. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String defaultValue() default "";

    /**
     * Whether property is required.
     *
     * @return true if required
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IConfigSpec#required intent: Whether property is required. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean required() default false;

    // --- VALIDATION ---

    /**
     * Whether to validate configuration.
     *
     * @return true to validate
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IConfigSpec#validate intent: Whether to validate configuration. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
     */
    boolean validate() default true;

    /**
     * Minimum value (for numbers).
     *
     * @return min
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IConfigSpec#min intent: Minimum value (for numbers). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code long}; default expression {@code Long.MIN_VALUE}.
     */
    long min() default Long.MIN_VALUE;

    /**
     * Maximum value (for numbers).
     *
     * @return max
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IConfigSpec#max intent: Maximum value (for numbers). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code long}; default expression {@code Long.MAX_VALUE}.
     */
    long max() default Long.MAX_VALUE;

    /**
     * Regex pattern (for strings).
     *
     * @return pattern
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IConfigSpec#pattern intent: Regex pattern (for strings). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String pattern() default "";

    /**
     * Allowed values (enum-like).
     *
     * @return allowed values
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IConfigSpec#allowedValues intent: Allowed values (enum-like). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] allowedValues() default {};

    // --- REFRESH ---

    /**
     * Whether config can be refreshed at runtime.
     *
     * @return true for refreshable
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IConfigSpec#refresh intent: Whether config can be refreshed at runtime. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean refresh() default false;

    /**
     * Refresh interval in seconds.
     *
     * @return interval
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IConfigSpec#refreshInterval intent: Refresh interval in seconds. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 0}.
     */
    int refreshInterval() default 0;

    // --- SECURITY ---

    /**
     * Whether this is a sensitive config (don't log).
     *
     * @return true if sensitive
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IConfigSpec#sensitive intent: Whether this is a sensitive config (don't log). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean sensitive() default false;

    /**
     * Whether to encrypt value.
     *
     * @return true to encrypt
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IConfigSpec#encrypted intent: Whether to encrypt value. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean encrypted() default false;

    // --- PROPERTY DEFINITION (nested) ---

    /** Property definition for fields.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares a field configuration key, default, requiredness, bounds and sensitivity hint; it reads or validates no property.
     * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.FIELD}; existing element and enum schemas are preserved.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.SOURCE)
    @interface Property {

        /** Property key.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IConfigSpec.Property#key intent: Property key. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
         */
        String key() default "";

        /** Default value.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IConfigSpec.Property#defaultValue intent: Default value. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
         */
        String defaultValue() default "";

        /** Required.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IConfigSpec.Property#required intent: Required. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
         */
        boolean required() default false;

        /** Min value.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IConfigSpec.Property#min intent: Min value. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code long}; default expression {@code Long.MIN_VALUE}.
         */
        long min() default Long.MIN_VALUE;

        /** Max value.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IConfigSpec.Property#max intent: Max value. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code long}; default expression {@code Long.MAX_VALUE}.
         */
        long max() default Long.MAX_VALUE;

        /** Sensitive.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IConfigSpec.Property#sensitive intent: Sensitive. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
         */
        boolean sensitive() default false;
    }

    // --- DESCRIPTION ---

    /**
     * Configuration description.
     *
     * @return description
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IConfigSpec#value intent: Configuration description. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String value() default "";

    // --- MODEL ---

    /**
     * Preferred model for generation.
     *
     * @return model type
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IConfigSpec#model intent: Preferred model for generation. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Model.ModelType}; default expression {@code Model.ModelType.HAIKU}.
     */
    Model.ModelType model() default Model.ModelType.HAIKU;
}
