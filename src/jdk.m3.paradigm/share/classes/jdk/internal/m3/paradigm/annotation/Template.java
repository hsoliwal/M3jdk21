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
 * Template - Marks a method/field/class as template-driven for TmOP.
 *
 * <h2>TmOP (Template-Oriented Programming) Integration</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  @Template - TEMPLATE-DRIVEN IMPLEMENTATION                               ║
 * ╠═══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                           ║
 * ║  "Define structure as a template, not as imperative code."                ║
 * ║                                                                           ║
 * ║  PARADIGM STACK:                                                          ║
 * ║    COP → AOP → OOP → FOP → IOP → AnOP → TmOP                              ║
 * ║                                             ↑                             ║
 * ║                                      Template-Oriented                    ║
 * ║                                                                           ║
 * ║  FreeMarker + JEXL + IOP enables:                                         ║
 * ║    • Compile-time template validation                                     ║
 * ║    • Generated implementations from templates                             ║
 * ║    • Separation of structure from logic                                   ║
 * ║    • Configurable code generation                                         ║
 * ║    • Model-driven development                                             ║
 * ║                                                                           ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Usage on Classes (Code Generation)</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	// Generate CRUD operations from template
 * 	&#64;Template(value = "crud-service.ftl", model = "entity")
 * 	public interface IUserService {
 *
 * 		User findById(Long id);
 *
 * 		List&lt;User> findAll();
 *
 * 		User save(User user);
 *
 * 		void delete(Long id);
 * 	}
 *
 * 	// Generate DTO from entity template
 * 	&#64;Template(value = "entity-to-dto.ftl", model = "User")
 * 	public record UserDTO(Long id, String name, String email) {
 * 	}
 * }
 * </pre>
 *
 * <h2>Usage on Methods (Output Generation)</h2>
 *
 * <pre>
 * {@code
 * // Generate email body from template
 * &#64;Template("email/welcome.ftl")
 * String generateWelcomeEmail(User user, String verificationLink);
 *
 * // Generate SQL query from template
 * &#64;Template(value = "sql/find-by-criteria.ftl", engine = Engine.FREEMARKER)
 * String buildQuery(SearchCriteria criteria);
 *
 * // Generate JSON response from template
 * &#64;Template(value = "json/api-response.ftl", contentType = "application/json")
 * String toJson(ApiResult result);
 * }
 * </pre>
 *
 * <h2>Usage on Fields (Computed Templates)</h2>
 *
 * <pre>{@code
 * &#64;Data
 * public class Report {
 *     private List&lt;DataPoint> points;
 *
 *     &#64;Template("report/summary.ftl")
 *     private String summary;  // Computed from template at access time
 *
 * &#64;Template(value = "report/chart.ftl", contentType = "image/svg+xml")
 * private String chartSvg; // SVG generated from template
 * }
 * }
 * </pre>
 *
 * <h2>Template Location Resolution</h2>
 *
 * <pre>
 * Template paths are resolved in order:
 * 1. Classpath: /templates/{value}
 * 2. Classpath: /ftl/{value}
 * 3. Classpath: /{value}
 * 4. File system (if absolute path)
 * </pre>
 *
 * <h2>Code Generation (Compile-Time)</h2>
 * <p>
 * At compile-time, the AnOP processor:
 * </p>
 *
 * <pre>{@code
 * // From: @Template("greeting.ftl")
 * //       String greet(String name);
 *
 * // Generated (using FreeMarker):
 * {@code @Override}
 * public String greet(String name) {
 *     Map&lt;String, Object> model = new HashMap&lt;>();
 *     model.put("name", name);
 *     return TemplateOps.process("greeting.ftl", model);
 * }
 * }</pre>
 *
 * <h2>Integration with @JexlExpr</h2>
 *
 * <pre>{@code
 * // Templates can contain JEXL expressions
 * &#64;Template(value = "invoice.ftl", jexlEnabled = true)
 * String generateInvoice(Order order);
 *
 * // invoice.ftl can use: ${order.total * 1.1} for JEXL evaluation
 * }</pre>
 *
 * <h2>Template Engines</h2>
 * <ul>
 * <li>{@link Engine#FREEMARKER} - Full FreeMarker support (default)</li>
 * <li>{@link Engine#JEXL} - Pure JEXL template strings</li>
 * <li>{@link Engine#MUSTACHE} - Logic-less templates</li>
 * <li>{@link Engine#STRING_TEMPLATE} - JDK StringTemplate (Java 21+)</li>
 * </ul>
 *
 * @since TmOP 1.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Template.java}
 * blob {@code 219d38936cfad74e4cddba22ffa2a9b6532c81af} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Template {

    /**
     * The template path or inline template content.
     *
     * <p>
     * If starts with "/" or contains ".", treated as file path. Otherwise, treated
     * as inline template content.
     * </p>
     *
     * <p>
     * Examples:
     * </p>
     *
     * <pre>{@code
     * &#64;Template("email/welcome.ftl")           // File path
     * &#64;Template("/templates/invoice.ftl")      // Absolute classpath
     * &#64;Template("Hello ${name}!")              // Inline template
     * }</pre>
     *
     * @return template path or content
     */
    String value();

    /**
     * Template engine to use.
     *
     * @return template engine
     */
    Engine engine() default Engine.FREEMARKER;

    /**
     * Model class name for class-level templates.
     *
     * <p>
     * Used when generating code from a model class.
     * </p>
     *
     * @return model class name
     */
    String model() default "";

    /**
     * Content type of the generated output.
     *
     * <p>
     * Used for HTTP response headers, file extensions, etc.
     * </p>
     *
     * @return MIME content type
     */
    String contentType() default "text/plain";

    /**
     * Character encoding for template processing.
     *
     * @return character encoding
     */
    String encoding() default "UTF-8";

    /**
     * Whether JEXL expressions are enabled within the template.
     *
     * <p>
     * If true, ${...} expressions are processed by JEXL before or after FreeMarker,
     * depending on the engine.
     * </p>
     *
     * @return true to enable JEXL processing
     */
    boolean jexlEnabled() default false;

    /**
     * Whether to cache the compiled template.
     *
     * <p>
     * Cached templates have faster repeated processing. Disable for development or
     * templates that change frequently.
     * </p>
     *
     * @return true to cache (default)
     */
    boolean cached() default true;

    /**
     * Whether to fail on missing variables.
     *
     * <p>
     * If true (strict mode), missing variables cause an error. If false (lenient
     * mode), missing variables render as empty.
     * </p>
     *
     * @return true for strict mode
     */
    boolean strict() default true;

    /**
     * Custom directives/functions to make available in the template.
     *
     * <p>
     * Format: "prefix=ClassName" pairs.
     * </p>
     *
     * <pre>{@code
     * &#64;Template(value = "report.ftl", directives = "fmt=DateFormatter,str=StringUtils")
     * }</pre>
     *
     * @return directive mappings
     */
    String[] directives() default {};

    /**
     * Output file path for code generation (class-level only).
     *
     * <p>
     * If specified, generated code is written to this path instead of being
     * returned as a string.
     * </p>
     *
     * @return output file path
     */
    String output() default "";

    /**
     * Processing strategy.
     *
     * @return the processing strategy
     */
    Strategy strategy() default Strategy.EAGER;

    /** Available template engines. */
    enum Engine {
        /**
         * Apache FreeMarker - Full-featured template engine.
         *
         * <p>
         * Supports:
         * </p>
         * <ul>
         * <li>Conditionals: {@code <#if>}</li>
         * <li>Loops: {@code <#list>}</li>
         * <li>Macros: {@code <#macro>}</li>
         * <li>Includes: {@code <#include>}</li>
         * <li>Built-in functions</li>
         * </ul>
         */
        FREEMARKER,

        /**
         * Apache Commons JEXL - Expression-based templates.
         *
         * <p>
         * Uses ${...} syntax for expressions.
         * </p>
         * <p>
         * Best for simple string interpolation.
         * </p>
         */
        JEXL,

        /**
         * Mustache - Logic-less templates.
         *
         * <p>
         * Uses {{...}} syntax.
         * </p>
         * <p>
         * Best for simple templates without logic.
         * </p>
         */
        MUSTACHE,

        /**
         * JDK StringTemplate - Java 21+ templates.
         *
         * <p>
         * Uses STR."..." syntax.
         * </p>
         * <p>
         * Native Java support, type-safe.
         * </p>
         */
        STRING_TEMPLATE,

        /**
         * Velocity - Apache Velocity templates.
         *
         * <p>
         * Uses $variable and #directives syntax.
         * </p>
         */
        VELOCITY,

        /**
         * Thymeleaf - HTML-focused template engine.
         *
         * <p>
         * Uses th: attributes in HTML.
         * </p>
         * <p>
         * Best for HTML generation.
         * </p>
         */
        THYMELEAF
    }

    /** Template processing strategy. */
    enum Strategy {
        /** Process template immediately when method is called. */
        EAGER,

        /** Process template lazily, cache result. */
        LAZY,

        /** Process template lazily, don't cache. */
        LAZY_UNCACHED,

        /** Process at compile-time only (code generation). */
        COMPILE_TIME
    }
}
