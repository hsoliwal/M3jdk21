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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * IFieldSpec - Field-level specification for LLM code generation.
 *
 * <h2>Purpose</h2>
 *
 * <p>
 * Annotates fields to specify their behavior, constraints, and invariants for
 * LLM-based code generation. Similar to {@link ISpec} but at field granularity.
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	public class UserAccount {
 *
 * 		&#64;IFieldSpec(value = "User's email address - must be valid and unique", constraints = {
 * 				"Format: RFC 5322 compliant email", "Lowercase normalized", "Maximum 255 characters"}, invariants = {
 * 						"Must not be null or empty", "Must contain exactly one '@' character",
 * 						"Must end with valid TLD"}, examples = {"user@example.com → Valid",
 * 								"ADMIN@COMPANY.COM → admin@company.com (normalized)",
 * 								"invalid.email → ValidationException"})
 * 		private String email;
 *
 * 		&#64;IFieldSpec(value = "Account creation timestamp - immutable after initialization", constraints = {
 * 				"Must be in UTC",
 * 				"ISO 8601 format"}, invariants = {"Cannot be in the future", "Cannot be modified after creation"})
 * 		private final Instant createdAt = Instant.now();
 * 	}
 * }
 * </pre>
 *
 * <h2>Field Types</h2>
 *
 * <ul>
 * <li><strong>Primitive fields</strong> - Specify valid ranges, default values
 * <li><strong>Object fields</strong> - Specify null-safety, validation rules
 * <li><strong>Collection fields</strong> - Specify element constraints, size
 * limits
 * <li><strong>Static fields</strong> - Specify constants, immutability
 * <li><strong>Final fields</strong> - Specify initialization contract
 * </ul>
 *
 * <h2>Generation Process</h2>
 *
 * <pre>
 * 1. Scan fields annotated with @IFieldSpec
 * 2. Generate validation logic for constraints
 * 3. Generate getter/setter with validation
 * 4. Generate builder methods if needed
 * 5. Generate test cases for invariants
 * </pre>
 *
 * @see ISpec
 * @see IMethodSpec
 * @see IParamSpec
 * @see Stability
 * @since Phase 1
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/IFieldSpec.java}
 * blob {@code 77ea843b3f1b0cb99249dc7913f18708a13166be} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP LazyInit batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IFieldSpec {

    // --- SPECIFICATION ---

    /**
     * Natural language description of the field purpose and behavior.
     *
     * @return field description
     */
    String value();

    /**
     * Constraints that guide generation (soft requirements). These influence
     * validation logic.
     *
     * @return constraint descriptions
     */
    String[] constraints() default {};

    /**
     * Invariants that MUST hold (hard requirements). These become assertions in
     * getters/setters.
     *
     * @return invariant expressions
     */
    String[] invariants() default {};

    /**
     * Example values and their expected behavior. Format: "value ->
     * expected_result"
     *
     * @return example pairs
     */
    String[] examples() default {};

    // --- VALIDATION ---

    /**
     * Default value for the field if unspecified.
     *
     * @return default value expression (empty = no default)
     */
    String defaultValue() default "";

    /**
     * Whether null values are allowed for object fields.
     *
     * @return true if null is allowed
     */
    boolean nullable() default true;

    /**
     * Minimum value for numeric fields (inclusive).
     *
     * @return minimum value (empty = no minimum)
     */
    String min() default "";

    /**
     * Maximum value for numeric fields (inclusive).
     *
     * @return maximum value (empty = no maximum)
     */
    String max() default "";

    /**
     * Regular expression pattern for string fields.
     *
     * @return regex pattern (empty = no pattern)
     */
    String pattern() default "";

    // --- MODEL SELECTION ---

    /**
     * Preferred model for generation.
     *
     * @return model type (default: SONNET)
     */
    Model.ModelType model() default Model.ModelType.SONNET;

    /**
     * Fallback model if preferred is unavailable.
     *
     * @return fallback model type
     */
    Model.ModelType fallback() default Model.ModelType.OLLAMA;

    // --- STABILITY & QUALITY ---

    /**
     * Target stability level for generated validation code.
     *
     * @return stability level
     */
    Stability.Level stability() default Stability.Level.ALPHA;

    /**
     * Test class to verify generated validation logic.
     *
     * @return test class (Void.class = no test)
     */
    Class<?> test() default Void.class;

    /**
     * Maximum generation attempts before failing.
     *
     * @return max retries (default: 3)
     */
    int maxRetries() default 3;

    // MEMBER_UNION (user decision 2026-11-10, "Union into AnOP owners"): members of the IOP twin
    /**
     * BiFunction class for computed fields from two sources.
     * {@code BiFunction<A, B, R>} where R is the field type.
     *
     * <pre>{@code
     *
     * class FullNameComputer implements BiFunction&lt;String, String, String> {
     *
     * 	public String apply(String first, String last) {
     * 		return first + " " + last;
     * 	}
     * }
     *
     * &#64;IFieldSpec(biFunction = FullNameComputer.class, sources = {"firstName", "lastName"})
     * private String fullName;
     * }</pre>
     *
     * @return BiFunction class
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IFieldSpec#biFunction intent: BiFunction class for computed fields from two sources. {@code BiFunction<A, B, R>} where R is the field type. This element does not invoke the illustrated implementation. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<?>}; default expression {@code Void.class}.
     */
    Class<?> biFunction() default Void.class;

    /**
     * Method name in same class that returns default value. Simpler than creating a
     * separate Supplier class.
     *
     * <pre>{@code
     *
     * &#64;IFieldSpec(defaultMethod = "createDefaultCache")
     * private Cache&lt;String, User> cache;
     *
     * private Cache&lt;String, User> createDefaultCache() {
     * 	return CacheBuilder.newBuilder().build();
     * }
     * }</pre>
     *
     * @return method name
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IFieldSpec#defaultMethod intent: Method name in same class that returns default value. Simpler than creating a separate Supplier class. This element does not invoke the illustrated implementation. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String defaultMethod() default "";

    /**
     * Supplier class for default value. The class must implement
     * {@code Supplier<T>} where T is the field type.
     *
     * <pre>{@code
     *
     * // Define supplier as functional interface:
     * class CacheSupplier implements Supplier&lt;Cache&lt;String, User>> {
     *
     * 	public Cache&lt;String, User> get() {
     * 		return CacheBuilder.newBuilder().maximumSize(1000).build();
     * 	}
     * }
     *
     * // Use in field:
     * &#64;IFieldSpec(defaulter = CacheSupplier.class)
     * private Cache&lt;String, User> cache;
     * }</pre>
     *
     * @return supplier class
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IFieldSpec#defaulter intent: Supplier class for default value. The class must implement {@code Supplier<T>} where T is the field type. This element does not invoke the illustrated implementation. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<?>}; default expression {@code Void.class}.
     */
    Class<?> defaulter() default Void.class;

    /**
     * Whether to encrypt this field at rest (for persistence).
     *
     * @return true to encrypt
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IFieldSpec#encrypt intent: Whether to encrypt this field at rest (for persistence). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean encrypt() default false;

    /**
     * Environment variable name when source = ENVIRONMENT.
     *
     * @return env variable name
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IFieldSpec#envKey intent: Environment variable name when source = ENVIRONMENT. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String envKey() default "";

    /**
     * Whether this field should be included in equals/hashCode.
     *
     * @return true to include
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IFieldSpec#equalsHashCode intent: Whether this field should be included in equals/hashCode. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
     */
    boolean equalsHashCode() default true;

    /**
     * Whether to generate a getter.
     *
     * @return true to generate getter
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IFieldSpec#getter intent: Whether to generate a getter. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
     */
    boolean getter() default true;

    /**
     * JNDI name when source = JNDI.
     *
     * @return JNDI name
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IFieldSpec#jndiName intent: JNDI name when source = JNDI. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String jndiName() default "";

    /**
     * Lifecycle for lazy fields.
     *
     * @return lifecycle
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IFieldSpec#lifecycle intent: Lifecycle for lazy fields. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Lifecycle}; default expression {@code Lifecycle.SINGLETON}.
     */
    Lifecycle lifecycle() default Lifecycle.SINGLETON;

    /**
     * Consumer class implementing {@code Consumer<T>}. Called after value is set
     * (side effect).
     *
     * <pre>{@code
     *
     * class LoggingConsumer implements Consumer&lt;User> {
     *
     * 	public void accept(User user) {
     * 		log.info("User set: {}", user.getId());
     * 	}
     * }
     *
     * &#64;IFieldSpec(onSet = LoggingConsumer.class)
     * private User currentUser;
     * }</pre>
     *
     * @return Consumer class
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IFieldSpec#onSet intent: Consumer class implementing {@code Consumer<T>}. Called after value is set (side effect). This element does not invoke the illustrated implementation. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<?>}; default expression {@code Void.class}.
     */
    Class<?> onSet() default Void.class;

    /**
     * Predefined field pattern. Use this for common cases instead of configuring
     * individual parameters.
     *
     * <pre>
     * {@code
     * // Simple usage with options:
     * &#64;IFieldSpec(option = AUTOWIRED)           // Spring bean injection
     * &#64;IFieldSpec(option = CONFIG)              // Property file value
     * &#64;IFieldSpec(option = LAZY_CACHE)          // Lazy-init cache
     * &#64;IFieldSpec(option = SENSITIVE)           // Password/token
     * &#64;IFieldSpec(option = COMPUTED)            // Derived from others
     * }
     * </pre>
     *
     * @return field option pattern
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IFieldSpec#option intent: Predefined field pattern. Use this for common cases instead of configuring individual parameters. This element does not invoke the illustrated implementation. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Option}; default expression {@code Option.NONE}.
     */
    Option option() default Option.NONE;

    /**
     * Property key when source = PROPERTY.
     *
     * @return property key
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IFieldSpec#propertyKey intent: Property key when source = PROPERTY. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String propertyKey() default "";

    /**
     * Whether this field is required (null not allowed).
     *
     * @return true if required
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IFieldSpec#required intent: Whether this field is required (null not allowed). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean required() default false;

    /**
     * Whether this field contains sensitive data. If true: don't log, mask in
     * toString, encrypt at rest.
     *
     * @return true if sensitive
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IFieldSpec#sensitive intent: Whether this field contains sensitive data. If true: don't log, mask in toString, encrypt at rest. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean sensitive() default false;

    /**
     * Whether to generate a setter.
     *
     * @return true to generate setter
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IFieldSpec#setter intent: Whether to generate a setter. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean setter() default false;

    /**
     * Where the field value comes from. Use when option = NONE for detailed
     * configuration.
     *
     * @return value source
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IFieldSpec#source intent: Where the field value comes from. Use when option = NONE for detailed configuration. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Source}; default expression {@code Source.MANUAL}.
     */
    Source source() default Source.MANUAL;

    /**
     * Source field names for BiFunction/TriFunction computed fields.
     *
     * @return source field names
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IFieldSpec#sources intent: Source field names for BiFunction/TriFunction computed fields. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] sources() default {};

    /**
     * Whether this field needs thread-safe access.
     *
     * @return true for thread-safe
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IFieldSpec#threadSafe intent: Whether this field needs thread-safe access. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean threadSafe() default false;

    /**
     * Whether this field should be included in toString.
     *
     * @return true to include
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IFieldSpec#toStringInclude intent: Whether this field should be included in toString. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
     */
    boolean toStringInclude() default true;

    /**
     * Transformer class implementing {@code Function<T, T>}. Transforms value
     * before assignment.
     *
     * <pre>{@code
     *
     * class TrimTransformer implements Function&lt;String, String> {
     *
     * 	public String apply(String s) {
     * 		return s == null ? null : s.trim();
     * 	}
     * }
     *
     * &#64;IFieldSpec(transformer = TrimTransformer.class)
     * private String name;
     * }</pre>
     *
     * @return Function class
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IFieldSpec#transformer intent: Transformer class implementing {@code Function<T, T>}. Transforms value before assignment. This element does not invoke the illustrated implementation. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<?>}; default expression {@code Void.class}.
     */
    Class<?> transformer() default Void.class;

    /**
     * TriFunction class for computed fields from three sources.
     * {@code TriFunction<A, B, C, R>} where R is the field type.
     *
     * <pre>{@code
     *
     * class AddressComputer implements TriFunction&lt;String, String, String, String> {
     *
     * 	public String apply(String street, String city, String country) {
     * 		return street + ", " + city + ", " + country;
     * 	}
     * }
     *
     * &#64;IFieldSpec(triFunction = AddressComputer.class, sources = {"street", "city", "country"})
     * private String fullAddress;
     * }</pre>
     *
     * @return TriFunction class
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IFieldSpec#triFunction intent: TriFunction class for computed fields from three sources. {@code TriFunction<A, B, C, R>} where R is the field type. This element does not invoke the illustrated implementation. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<?>}; default expression {@code Void.class}.
     */
    Class<?> triFunction() default Void.class;

    /**
     * Tuple extractor for multi-value returns. Use when a single source returns
     * multiple values to extract.
     *
     * <pre>
     *
     * {
     * 	&#64;code
     * 	// Extract latitude and longitude from a Location
     * 	class LatLngExtractor implements Function&lt;Location, double[]> {
     *
     * 		public double[] apply(Location loc) {
     * 			return new double[]{loc.lat(), loc.lng()};
     * 		}
     * 	}
     *
     * 	&#64;IFieldSpec(tupleExtractor = LatLngExtractor.class, tupleIndex = 0)
     * 	private double latitude;
     *
     * 	&#64;IFieldSpec(tupleExtractor = LatLngExtractor.class, tupleIndex = 1)
     * 	private double longitude;
     * }
     * </pre>
     *
     * @return tuple extractor Function class
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IFieldSpec#tupleExtractor intent: Tuple extractor for multi-value returns. Use when a single source returns multiple values to extract. This element does not invoke the illustrated implementation. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<?>}; default expression {@code Void.class}.
     */
    Class<?> tupleExtractor() default Void.class;

    /**
     * Index in tuple/array to extract when using tupleExtractor.
     *
     * @return tuple index
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IFieldSpec#tupleIndex intent: Index in tuple/array to extract when using tupleExtractor. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 0}.
     */
    int tupleIndex() default 0;

    /**
     * Validator class implementing {@code Predicate<T>}. Returns true if value is
     * valid.
     *
     * <pre>{@code
     *
     * class AgeValidator implements Predicate&lt;Integer> {
     *
     * 	public boolean test(Integer age) {
     * 		return age >= 0 && age &lt;= 150;
     * 	}
     * }
     *
     * &#64;IFieldSpec(validator = AgeValidator.class)
     * private int age;
     * }</pre>
     *
     * @return Predicate class
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IFieldSpec#validator intent: Validator class implementing {@code Predicate<T>}. Returns true if value is valid. This element does not invoke the illustrated implementation. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<?>}; default expression {@code Void.class}.
     */
    Class<?> validator() default Void.class;

    /**
     * Whether this field is volatile.
     *
     * @return true for volatile
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IFieldSpec#volatileField intent: Whether this field is volatile. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean volatileField() default false;

    /** Field lifecycles.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IFieldSpec.Lifecycle choices described as Field lifecycles. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 5 constants in the existing source order.
     */
    enum Lifecycle {
        /** One instance for all accesses.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IFieldSpec.Lifecycle#SINGLETON policy choice: One instance for all accesses. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IFieldSpec.Lifecycle; its declared order and identity are preserved.
         */
        SINGLETON,
        /** New instance per access.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IFieldSpec.Lifecycle#PROTOTYPE policy choice: New instance per access. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IFieldSpec.Lifecycle; its declared order and identity are preserved.
         */
        PROTOTYPE,
        /** One instance per request.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IFieldSpec.Lifecycle#REQUEST policy choice: One instance per request. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IFieldSpec.Lifecycle; its declared order and identity are preserved.
         */
        REQUEST,
        /** One instance per session.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IFieldSpec.Lifecycle#SESSION policy choice: One instance per session. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IFieldSpec.Lifecycle; its declared order and identity are preserved.
         */
        SESSION,
        /** One instance per thread.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IFieldSpec.Lifecycle#THREAD_LOCAL policy choice: One instance per thread. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IFieldSpec.Lifecycle; its declared order and identity are preserved.
         */
        THREAD_LOCAL
    }

    /** Predefined field patterns for common cases.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IFieldSpec.Option choices described as Predefined field patterns for common cases. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 13 constants in the existing source order.
     */
    enum Option {
        /** No preset - configure manually.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IFieldSpec.Option#NONE policy choice: No preset - configure manually. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IFieldSpec.Option; its declared order and identity are preserved.
         */
        NONE,

        /** Autowired bean (required, singleton).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IFieldSpec.Option#AUTOWIRED policy choice: Autowired bean (required, singleton). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IFieldSpec.Option; its declared order and identity are preserved.
         */
        AUTOWIRED,

        /** Optional autowired bean (nullable).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IFieldSpec.Option#AUTOWIRED_OPTIONAL policy choice: Optional autowired bean (nullable). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IFieldSpec.Option; its declared order and identity are preserved.
         */
        AUTOWIRED_OPTIONAL,

        /** Configuration property (from application.properties).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IFieldSpec.Option#CONFIG policy choice: Configuration property (from application.properties). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IFieldSpec.Option; its declared order and identity are preserved.
         */
        CONFIG,

        /** Required configuration (fail if missing).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IFieldSpec.Option#CONFIG_REQUIRED policy choice: Required configuration (fail if missing). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IFieldSpec.Option; its declared order and identity are preserved.
         */
        CONFIG_REQUIRED,

        /** Environment variable.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IFieldSpec.Option#ENV policy choice: Environment variable. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IFieldSpec.Option; its declared order and identity are preserved.
         */
        ENV,

        /** Lazy initialization (singleton).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IFieldSpec.Option#LAZY policy choice: Lazy initialization (singleton). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IFieldSpec.Option; its declared order and identity are preserved.
         */
        LAZY,

        /** Lazy cache (thread-safe, memoized).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IFieldSpec.Option#LAZY_CACHE policy choice: Lazy cache (thread-safe, memoized). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IFieldSpec.Option; its declared order and identity are preserved.
         */
        LAZY_CACHE,

        /** Sensitive data (masked, encrypted).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IFieldSpec.Option#SENSITIVE policy choice: Sensitive data (masked, encrypted). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IFieldSpec.Option; its declared order and identity are preserved.
         */
        SENSITIVE,

        /** Computed from other fields.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IFieldSpec.Option#COMPUTED policy choice: Computed from other fields. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IFieldSpec.Option; its declared order and identity are preserved.
         */
        COMPUTED,

        /** Constant value.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IFieldSpec.Option#CONSTANT policy choice: Constant value. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IFieldSpec.Option; its declared order and identity are preserved.
         */
        CONSTANT,

        /** Thread-local storage.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IFieldSpec.Option#THREAD_LOCAL policy choice: Thread-local storage. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IFieldSpec.Option; its declared order and identity are preserved.
         */
        THREAD_LOCAL,

        /** Immutable (final, set in constructor).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IFieldSpec.Option#IMMUTABLE policy choice: Immutable (final, set in constructor). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IFieldSpec.Option; its declared order and identity are preserved.
         */
        IMMUTABLE
    }

    /** Value sources for fields.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IFieldSpec.Source choices described as Value sources for fields. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 9 constants in the existing source order.
     */
    enum Source {
        /** Manual assignment in constructor or setter.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IFieldSpec.Source#MANUAL policy choice: Manual assignment in constructor or setter. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IFieldSpec.Source; its declared order and identity are preserved.
         */
        MANUAL,
        /** Spring/CDI @Autowired injection.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IFieldSpec.Source#AUTOWIRED policy choice: Spring/CDI @Autowired injection. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IFieldSpec.Source; its declared order and identity are preserved.
         */
        AUTOWIRED,
        /** Property file (application.properties).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IFieldSpec.Source#PROPERTY policy choice: Property file (application.properties). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IFieldSpec.Source; its declared order and identity are preserved.
         */
        PROPERTY,
        /** Environment variable.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IFieldSpec.Source#ENVIRONMENT policy choice: Environment variable. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IFieldSpec.Source; its declared order and identity are preserved.
         */
        ENVIRONMENT,
        /** System property (-D).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IFieldSpec.Source#SYSTEM_PROPERTY policy choice: System property (-D). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IFieldSpec.Source; its declared order and identity are preserved.
         */
        SYSTEM_PROPERTY,
        /** JNDI lookup.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IFieldSpec.Source#JNDI policy choice: JNDI lookup. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IFieldSpec.Source; its declared order and identity are preserved.
         */
        JNDI,
        /** Lazy initialization on first access.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IFieldSpec.Source#LAZY policy choice: Lazy initialization on first access. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IFieldSpec.Source; its declared order and identity are preserved.
         */
        LAZY,
        /** Computed from other fields.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IFieldSpec.Source#COMPUTED policy choice: Computed from other fields. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IFieldSpec.Source; its declared order and identity are preserved.
         */
        COMPUTED,
        /** Constant value.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IFieldSpec.Source#CONSTANT policy choice: Constant value. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IFieldSpec.Source; its declared order and identity are preserved.
         */
        CONSTANT
    }
}
