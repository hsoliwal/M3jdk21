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

/**
 * TemplateEngines - Comparison of Java template engines for IOP code
 * generation.
 *
 * <h2>Template Engine Comparison (Code Generation Focus)</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  ENGINE         │ SPEED  │ SYNTAX  │ CODE GEN │ MAINTENANCE │ RECOMMEND  ║
 * ╠═══════════════════════════════════════════════════════════════════════════╣
 * ║  JavaPoet       │ ★★★★★ │ Java    │ ★★★★★   │ ★★★★★       │ ✓ BEST     ║
 * ║  JTE            │ ★★★★★ │ Simple  │ ★★★★☆   │ ★★★★★       │ ✓ FAST     ║
 * ║  Rocker         │ ★★★★★ │ Simple  │ ★★★★☆   │ ★★★★☆       │ ✓ FAST     ║
 * ║  FreeMarker     │ ★★★★☆ │ FTL     │ ★★★★☆   │ ★★★★★       │ ✓ MATURE   ║
 * ║  Velocity       │ ★★★☆☆ │ VTL     │ ★★★☆☆   │ ★★★☆☆       │ LEGACY     ║
 * ║  StringTemplate │ ★★★★☆ │ ST4     │ ★★★★☆   │ ★★★★☆       │ ✓ ANTLR    ║
 * ║  Thymeleaf      │ ★★☆☆☆ │ HTML    │ ★☆☆☆☆   │ ★★★★★       │ WEB ONLY   ║
 * ║  Mustache       │ ★★★★☆ │ {{}}    │ ★★☆☆☆   │ ★★★★★       │ SIMPLE     ║
 * ║  Pebble         │ ★★★★☆ │ Twig    │ ★★★☆☆   │ ★★★★☆       │ NICE       ║
 * ║  Handlebars     │ ★★★☆☆ │ {{}}    │ ★★☆☆☆   │ ★★★★☆       │ SIMPLE     ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>RECOMMENDATION FOR IOP CODE GENERATION</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                                                                           ║
 * ║  PRIMARY: JavaPoet (for Java source generation)                           ║
 * ║  ────────────────────────────────────────────────                         ║
 * ║  • Type-safe Java API for generating .java files                          ║
 * ║  • No string templates - pure Java code                                   ║
 * ║  • Handles imports, formatting, escaping automatically                    ║
 * ║  • Used by Dagger, AutoValue, Room, Retrofit                              ║
 * ║  • Square (company behind OkHttp, Retrofit)                               ║
 * ║                                                                           ║
 * ║  SECONDARY: JTE (Java Template Engine)                                    ║
 * ║  ─────────────────────────────────────                                    ║
 * ║  • Compiled templates (fast!)                                             ║
 * ║  • Type-safe parameters                                                   ║
 * ║  • Simple syntax                                                          ║
 * ║  • Hot reload in dev mode                                                 ║
 * ║                                                                           ║
 * ║  FALLBACK: FreeMarker (mature, well-documented)                           ║
 * ║  ────────────────────────────────────────────────                         ║
 * ║  • Apache project, mature                                                 ║
 * ║  • Good for complex templates                                             ║
 * ║  • Widely used, well-documented                                           ║
 * ║                                                                           ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Maven Dependencies</h2>
 *
 * <pre>{@code
 * <!-- JavaPoet - RECOMMENDED for Java code generation -->
 * <dependency>
 *   <groupId>com.squareup</groupId>
 *   <artifactId>javapoet</artifactId>
 *   <version>1.13.0</version>
 * </dependency>
 *
 * <!-- JTE - Fast compiled templates -->
 * <dependency>
 *   <groupId>gg.jte</groupId>
 *   <artifactId>jte</artifactId>
 *   <version>3.1.9</version>
 * </dependency>
 *
 * <!-- Rocker - Fast compiled templates -->
 * <dependency>
 *   <groupId>com.fizzed</groupId>
 *   <artifactId>rocker-runtime</artifactId>
 *   <version>1.4.0</version>
 * </dependency>
 *
 * <!-- FreeMarker - Mature, flexible -->
 * <dependency>
 *   <groupId>org.freemarker</groupId>
 *   <artifactId>freemarker</artifactId>
 *   <version>2.3.32</version>
 * </dependency>
 *
 * <!-- StringTemplate - ANTLR's template engine -->
 * <dependency>
 *   <groupId>org.antlr</groupId>
 *   <artifactId>ST4</artifactId>
 *   <version>4.3.4</version>
 * </dependency>
 * }</pre>
 *
 * <h2>JavaPoet Example (RECOMMENDED)</h2>
 *
 * <pre>{@code
 * TypeSpec annotation = TypeSpec.annotationBuilder("IAIHint").addModifiers(Modifier.PUBLIC)
 * 		.addAnnotation(AnnotationSpec.builder(Target.class).addMember("value", "$T.TYPE", ElementType.class).build())
 * 		.addAnnotation(AnnotationSpec.builder(Retention.class).addMember("value", "$T.SOURCE", RetentionPolicy.class)
 * 				.build())
 * 		.addMethod(MethodSpec.methodBuilder("algorithm").addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
 * 				.returns(String.class).defaultValue("$S", "").build())
 * 		.build();
 *
 * JavaFile javaFile = JavaFile.builder("jdk.internal.m3.iop", annotation).addFileComment("Generated by IOP - DO NOT EDIT")
 * 		.build();
 *
 * javaFile.writeTo(outputDir);
 * }</pre>
 *
 * <h2>JTE Example</h2>
 *
 * <pre>
 * {@code
 * // Template: annotation.jte
 * &#64;param String className
 * &#64;param List<Attribute> attributes
 *
 * package jdk.internal.m3.iop;
 *
 * public @interface ${className} {
 *     &#64;for(var attr : attributes)
 *     ${attr.type()} ${attr.name()}() default ${attr.defaultValue()};
 *     &#64;endfor
 * }
 * }
 * </pre>
 *
 * @since IOP 1.0 - Template Engine Selection
 * @m3.pattern synexia:iop
 * @m3.algorithm Expose the existing engine taxonomy and three static recommendation tokens; historical comparison prose is not a measured benchmark or runtime engine implementation.
 * @m3.dataStructures Final constant holder with private constructor.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/TemplateEngines.java}
 * blob {@code d9e05fdca6a366afade0f857d418557101e1c282} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class TemplateEngines {

    /**
     * Prevent ordinary caller instantiation with a private, empty constructor; no template engine is loaded.
     * @m3.pattern synexia:iop
     * @m3.algorithm Prevent ordinary caller instantiation with a private, empty constructor; no template engine is loaded.
     * @m3.dataStructures Private no-argument constructor with no instance fields or executable statements.
     */
    private TemplateEngines() {}

    /** Supported template engines.
     * @m3.pattern synexia:iop
     * @m3.algorithm Declare six engine identity tokens in fixed order, without loading, configuring or running their external libraries.
     * @m3.dataStructures Six Engine enum singletons in declaration order.
     */
    public enum Engine {
        /**
         * JavaPoet - Type-safe Java source generation (RECOMMENDED).
         * <ul>
         * <li>Pure Java API, no string templates</li>
         * <li>Automatic import management</li>
         * <li>Handles formatting and escaping</li>
         * <li>Used by Dagger, AutoValue, Room</li>
         * </ul>
         * @m3.pattern synexia:iop
         * @m3.algorithm Identify JavaPoet Java-source API by a fixed enum singleton; this token does not execute or install the engine.
         * @m3.dataStructures One Engine enum singleton with stable name and declaration-order ordinal.
         */
        JAVAPOET,

        /**
         * JTE - Java Template Engine (FAST).
         * <ul>
         * <li>Compiled templates</li>
         * <li>Type-safe parameters</li>
         * <li>Simple, clean syntax</li>
         * <li>Hot reload support</li>
         * </ul>
         * @m3.pattern synexia:iop
         * @m3.algorithm Identify Java Template Engine by a fixed enum singleton; this token does not execute or install the engine.
         * @m3.dataStructures One Engine enum singleton with stable name and declaration-order ordinal.
         */
        JTE,

        /**
         * Rocker - Fast compiled templates.
         * <ul>
         * <li>Statically typed</li>
         * <li>Compiled to Java</li>
         * <li>Very fast</li>
         * </ul>
         * @m3.pattern synexia:iop
         * @m3.algorithm Identify Rocker templates by a fixed enum singleton; this token does not execute or install the engine.
         * @m3.dataStructures One Engine enum singleton with stable name and declaration-order ordinal.
         */
        ROCKER,

        /**
         * FreeMarker - Mature Apache project.
         * <ul>
         * <li>Well-documented</li>
         * <li>Powerful macro system</li>
         * <li>Good IDE support</li>
         * </ul>
         * @m3.pattern synexia:iop
         * @m3.algorithm Identify FreeMarker templates by a fixed enum singleton; this token does not execute or install the engine.
         * @m3.dataStructures One Engine enum singleton with stable name and declaration-order ordinal.
         */
        FREEMARKER,

        /**
         * StringTemplate - ANTLR's template engine.
         * <ul>
         * <li>Strict separation of logic and presentation</li>
         * <li>Good for code generation</li>
         * <li>Used by ANTLR itself</li>
         * </ul>
         * @m3.pattern synexia:iop
         * @m3.algorithm Identify ANTLR StringTemplate library by a fixed enum singleton; this token does not execute or install the engine.
         * @m3.dataStructures One Engine enum singleton with stable name and declaration-order ordinal.
         */
        STRING_TEMPLATE,

        /**
         * Velocity - Legacy Apache project.
         * <ul>
         * <li>Still maintained but dated</li>
         * <li>VTL syntax</li>
         * </ul>
         * @m3.pattern synexia:iop
         * @m3.algorithm Identify Apache Velocity templates by a fixed enum singleton; this token does not execute or install the engine.
         * @m3.dataStructures One Engine enum singleton with stable name and declaration-order ordinal.
         */
        VELOCITY
    }

    /** IOP Recommendation.
     * @m3.pattern synexia:iop
     * @m3.algorithm Return the shared JAVAPOET enum identity through a final field as the authored Java-source recommendation.
     * @m3.dataStructures Public static final reference to an existing Engine enum singleton.
     */
    public static final Engine RECOMMENDED = Engine.JAVAPOET;

    /** For non-Java output (configs, scripts).
     * @m3.pattern synexia:iop
     * @m3.algorithm Return the shared JTE enum identity through a final field as the authored non-Java text recommendation.
     * @m3.dataStructures Public static final reference to an existing Engine enum singleton.
     */
    public static final Engine RECOMMENDED_TEXT = Engine.JTE;

    /** Fallback for complex templates.
     * @m3.pattern synexia:iop
     * @m3.algorithm Return the shared FREEMARKER enum identity through a final field as the authored complex-template fallback.
     * @m3.dataStructures Public static final reference to an existing Engine enum singleton.
     */
    public static final Engine FALLBACK = Engine.FREEMARKER;
}
