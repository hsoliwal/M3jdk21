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

import jdk.internal.m3.collections.PlaceholderLogger;

/**
 * TEMPLATE_ENGINES_COMPARISON - Ranking by Power, Not Speed.
 *
 * <h2>THE RANKING (Most Powerful First)</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  RANK │ ENGINE              │ POWER  │ TYPE-SAFE │ USE CASE              ║
 * ╠═══════════════════════════════════════════════════════════════════════════╣
 * ║  #1   │ JavaPoet            │ ★★★★★  │ ✓ FULL    │ Java code generation  ║
 * ║  #2   │ StringTemplate (ST4)│ ★★★★★  │ ✗         │ Any code generation   ║
 * ║  #3   │ FreeMarker          │ ★★★★☆  │ ✗         │ Text/code templates   ║
 * ║  #4   │ Velocity            │ ★★★☆☆  │ ✗         │ Simple templates      ║
 * ║  #5   │ Mustache            │ ★★☆☆☆  │ ✗         │ Logic-less templates  ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>#1 - JavaPoet (RECOMMENDED FOR JAVA)</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  Maven: com.squareup:javapoet:1.13.0                                      ║
 * ║  GitHub: https://github.com/square/javapoet                               ║
 * ╠═══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                           ║
 * ║  WHY IT'S THE MOST POWERFUL:                                              ║
 * ║  • Type-safe API - compiler catches errors, not runtime                   ║
 * ║  • Automatic imports - never manually manage imports                      ║
 * ║  • Proper formatting - always syntactically correct Java                  ║
 * ║  • Composable - build complex types from simple pieces                    ║
 * ║  • No template syntax - pure Java code writes Java code                   ║
 * ║                                                                           ║
 * ║  EXAMPLE:                                                                 ║
 * ║  {@code                                                                   ║
 * ║  TypeSpec userService = TypeSpec.classBuilder("UserServiceImpl")          ║
 * ║      .addModifiers(Modifier.PUBLIC, Modifier.FINAL)                       ║
 * ║      .addSuperinterface(IUserService.class)                               ║
 * ║      .addField(UserRepository.class, "repository", Modifier.PRIVATE)      ║
 * ║      .addMethod(MethodSpec.methodBuilder("findById")                      ║
 * ║          .addModifiers(Modifier.PUBLIC)                                   ║
 * ║          .addAnnotation(Override.class)                                   ║
 * ║          .addParameter(String.class, "id")                                ║
 * ║          .returns(User.class)                                             ║
 * ║          .addStatement("return repository.findById(id).orElse(null)")     ║
 * ║          .build())                                                        ║
 * ║      .build();                                                            ║
 * ║  }                                                                        ║
 * ║                                                                           ║
 * ║  WHEN TO USE:                                                             ║
 * ║  • Java code generation (annotation processors, build plugins)            ║
 * ║  • Type-safety is critical                                                ║
 * ║  • Complex code structure with many variations                            ║
 * ║                                                                           ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>#2 - StringTemplate (ST4)</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  Maven: org.antlr:ST4:4.3.4                                               ║
 * ║  GitHub: https://github.com/antlr/stringtemplate4                         ║
 * ╠═══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                           ║
 * ║  WHY IT'S POWERFUL:                                                       ║
 * ║  • Designed by ANTLR creator (Terence Parr) specifically for code gen     ║
 * ║  • Strict separation of model and view (no code in templates)             ║
 * ║  • Template groups - organize related templates                           ║
 * ║  • Template inheritance - DRY templates                                   ║
 * ║  • Automatic null handling                                                ║
 * ║  • No side effects possible                                               ║
 * ║                                                                           ║
 * ║  PHILOSOPHY:                                                              ║
 * ║  "The key idea is that you should not be able to embed arbitrary          ║
 * ║   computations in a template. Templates are for view, not logic."         ║
 * ║                                                                           ║
 * ║  EXAMPLE:                                                                 ║
 * ║  class(name, fields, methods) ::= &lt;&lt;                                      ║
 * ║  public class &lt;name> {                                                    ║
 * ║      &lt;fields:field(); separator="\n">                                     ║
 * ║      &lt;methods:method(); separator="\n\n">                                 ║
 * ║  }                                                                        ║
 * ║  >>                                                                       ║
 * ║                                                                           ║
 * ║  field(f) ::= "private &lt;f.type> &lt;f.name>;"                                ║
 * ║                                                                           ║
 * ║  WHEN TO USE:                                                             ║
 * ║  • Multi-language code generation                                         ║
 * ║  • Strict model-view separation required                                  ║
 * ║  • Template reuse via inheritance                                         ║
 * ║                                                                           ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>#3 - FreeMarker (FTL)</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  Maven: org.freemarker:freemarker:2.3.32                                  ║
 * ║  GitHub: https://github.com/apache/freemarker                             ║
 * ╠═══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                           ║
 * ║  WHY IT'S POWERFUL:                                                       ║
 * ║  • Rich built-in functions (string, number, date, sequence, hash)         ║
 * ║  • Macros with named parameters and defaults                              ║
 * ║  • User-defined directives                                                ║
 * ║  • Namespace support for large templates                                  ║
 * ║  • Whitespace control (&lt; #t>, &lt; #lt>, &lt; #rt>)                             ║
 * ║  • Auto-escaping for XSS prevention                                       ║
 * ║                                                                           ║
 * ║  EXAMPLE:                                                                 ║
 * ║  &lt; #macro method m>                                                       ║
 * ║  public ${m.returnType} ${m.name}(                                        ║
 * ║      &lt; #list m.params as p>${p.type} ${p.name}&lt; #sep>, &lt;/ #sep>&lt;/ #list>  ║
 * ║  ) {                                                                      ║
 * ║      ${m.body!"// TODO"}                                                  ║
 * ║  }                                                                        ║
 * ║  &lt;/ #macro>                                                               ║
 * ║                                                                           ║
 * ║  WHEN TO USE:                                                             ║
 * ║  • Complex templates with many conditionals                               ║
 * ║  • Need built-in string/collection processing                             ║
 * ║  • HTML/XML generation with auto-escaping                                 ║
 * ║                                                                           ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>IOP RECOMMENDATION</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                                                                           ║
 * ║  FOR JAVA CODE GENERATION:                                                ║
 * ║  ─────────────────────────                                                ║
 * ║  Use JavaPoet (compile-time, type-safe, no template files)                ║
 * ║                                                                           ║
 * ║  FOR MULTI-LANGUAGE CODE GENERATION:                                      ║
 * ║  ───────────────────────────────────                                      ║
 * ║  Use StringTemplate (ST4) - designed for code gen                         ║
 * ║                                                                           ║
 * ║  FOR TEXT/CONFIG/SCRIPT TEMPLATES:                                        ║
 * ║  ──────────────────────────────────                                       ║
 * ║  Use FreeMarker (most feature-rich text templates)                        ║
 * ║                                                                           ║
 * ║  THE IOP HYBRID APPROACH:                                                 ║
 * ║  ────────────────────────                                                 ║
 * ║  • JavaPoet for Java code structure (100% CPU, 0% LLM)                    ║
 * ║  • StringTemplate for cross-language templates                            ║
 * ║  • LLM ONLY for method bodies (minimal surface area)                      ║
 * ║                                                                           ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * @since IOP 1.0
 * @m3.pattern synexia:iop
 * @m3.algorithm Groups five literal engine descriptors and five use-case labels. recommend selects a descriptor through an exhaustive enum switch; no engine library is loaded, benchmarked or executed.
 * @m3.dataStructures Two nested enums, four final descriptor fields per engine, an enum switch and a Lombok-generated static logger.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/TEMPLATE_ENGINES_COMPARISON.java}
 * blob {@code da5f6eb2447db0daff4888c3044e7cdc5075f724} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class TEMPLATE_ENGINES_COMPARISON {
    private static final PlaceholderLogger log = PlaceholderLogger.of(TEMPLATE_ENGINES_COMPARISON.class);


    /**
     * Private constructor for the static declaration owner.
     * @m3.pattern synexia:bop
     * @m3.algorithm Executes an empty private constructor body, preventing ordinary caller construction.
     * @m3.dataStructures No constructor parameters or declared instance state.
     */
    private TEMPLATE_ENGINES_COMPARISON() {}

    /** Template engine ranking by power.
     * @m3.pattern synexia:iop
     * @m3.algorithm Constructs five enum values from literal rating, safety flag, use-case and artifact text, then returns these stored fields through direct getters. Ratings are stored claims and no engine evaluation is performed.
     * @m3.dataStructures Five enum singleton values with final int powerRating, boolean typeSafe and String useCase/maven fields.
     */
    public enum TemplateEngine {

        /** Most powerful for Java - type-safe, programmatic
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the JAVAPOET singleton from rating 5, flag true, use-case text "Java code generation" and artifact text "com.squareup:javapoet:1.13.0" in its original ordinal position.
         * @m3.dataStructures One enum singleton with one int, one boolean and two final String fields.
         */
        JAVAPOET(5, true, "Java code generation", "com.squareup:javapoet:1.13.0"),

        /** ANTLR's engine - strict model-view separation
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the STRING_TEMPLATE singleton from rating 5, flag false, use-case text "Multi-language code gen" and artifact text "org.antlr:ST4:4.3.4" in its original ordinal position.
         * @m3.dataStructures One enum singleton with one int, one boolean and two final String fields.
         */
        STRING_TEMPLATE(5, false, "Multi-language code gen", "org.antlr:ST4:4.3.4"),

        /** Feature-rich text templates
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the FREEMARKER singleton from rating 4, flag false, use-case text "Text/code templates" and artifact text "org.freemarker:freemarker:2.3.32" in its original ordinal position.
         * @m3.dataStructures One enum singleton with one int, one boolean and two final String fields.
         */
        FREEMARKER(4, false, "Text/code templates", "org.freemarker:freemarker:2.3.32"),

        /** Simple, proven templates
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the VELOCITY singleton from rating 3, flag false, use-case text "Simple templates" and artifact text "org.apache.velocity:velocity-engine-core:2.3" in its original ordinal position.
         * @m3.dataStructures One enum singleton with one int, one boolean and two final String fields.
         */
        VELOCITY(3, false, "Simple templates", "org.apache.velocity:velocity-engine-core:2.3"),

        /** Logic-less templates
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the MUSTACHE singleton from rating 2, flag false, use-case text "Logic-less templates" and artifact text "com.github.spullara.mustache.java:compiler:0.9.10" in its original ordinal position.
         * @m3.dataStructures One enum singleton with one int, one boolean and two final String fields.
         */
        MUSTACHE(2, false, "Logic-less templates", "com.github.spullara.mustache.java:compiler:0.9.10");

        /**
         * Immutable powerRating value of a template-engine descriptor.
         * @m3.pattern synexia:iop
         * @m3.algorithm Retains the constructor-supplied powerRating int value in the corresponding final field without validation or normalization.
         * @m3.dataStructures One final int field per instance.
         */
        private final int powerRating;
        /**
         * Immutable typeSafe value of a template-engine descriptor.
         * @m3.pattern synexia:iop
         * @m3.algorithm Retains the constructor-supplied typeSafe boolean value in the corresponding final field without validation or normalization.
         * @m3.dataStructures One final boolean field per instance.
         */
        private final boolean typeSafe;
        /**
         * Immutable useCase value of a template-engine descriptor.
         * @m3.pattern synexia:iop
         * @m3.algorithm Retains the constructor-supplied useCase String value in the corresponding final field without validation or normalization.
         * @m3.dataStructures One final String field per instance.
         */
        private final String useCase;
        /**
         * Immutable maven value of a template-engine descriptor.
         * @m3.pattern synexia:iop
         * @m3.algorithm Retains the constructor-supplied maven String value in the corresponding final field without validation or normalization.
         * @m3.dataStructures One final String field per instance.
         */
        private final String maven;

        /**
         * Initializes one template-engine descriptor.
         * @m3.pattern synexia:bop
         * @m3.algorithm Assigns the supplied powerRating, typeSafe, useCase and maven values to corresponding final fields in source order without validation or dependency resolution.
         * @m3.dataStructures One int, one boolean and two String parameters plus four final instance fields.
         */
        TemplateEngine(int powerRating, boolean typeSafe, String useCase, String maven) {
            this.powerRating = powerRating;
            this.typeSafe = typeSafe;
            this.useCase = useCase;
            this.maven = maven;
        }

        /**
         * Accessor for the stored powerRating value.
         * @m3.pattern synexia:bop
         * @m3.algorithm Returns the existing powerRating field directly without conversion, validation or mutation.
         * @m3.dataStructures One stored int value and its returned value.
         */
        public int powerRating() {
            return powerRating;
        }

        /**
         * Accessor for the stored typeSafe value.
         * @m3.pattern synexia:bop
         * @m3.algorithm Returns the existing typeSafe field directly without conversion, validation or mutation.
         * @m3.dataStructures One stored boolean value and its returned value.
         */
        public boolean isTypeSafe() {
            return typeSafe;
        }

        /**
         * Accessor for the stored useCase value.
         * @m3.pattern synexia:bop
         * @m3.algorithm Returns the existing useCase field directly without conversion, validation or mutation.
         * @m3.dataStructures One stored String value and its returned value.
         */
        public String useCase() {
            return useCase;
        }

        /**
         * Accessor for the stored maven value.
         * @m3.pattern synexia:bop
         * @m3.algorithm Returns the existing maven field directly without conversion, validation or mutation.
         * @m3.dataStructures One stored String value and its returned value.
         */
        public String maven() {
            return maven;
        }
    }

    /** Recommendation based on use case.
     * @m3.pattern synexia:bop
     * @m3.algorithm Returns JAVAPOET for JAVA_CODE_GEN, STRING_TEMPLATE for MULTI_LANGUAGE_CODE, FREEMARKER for TEXT_TEMPLATES, VELOCITY for SIMPLE_SUBSTITUTION and MUSTACHE for LOGIC_LESS through an exhaustive switch. Null useCase throws NullPointerException.
     * @m3.dataStructures One UseCase reference, a switch expression and an existing TemplateEngine singleton reference.
     */
    public static TemplateEngine recommend(UseCase useCase) {
        return switch (useCase) {
            case JAVA_CODE_GEN -> TemplateEngine.JAVAPOET;
            case MULTI_LANGUAGE_CODE -> TemplateEngine.STRING_TEMPLATE;
            case TEXT_TEMPLATES -> TemplateEngine.FREEMARKER;
            case SIMPLE_SUBSTITUTION -> TemplateEngine.VELOCITY;
            case LOGIC_LESS -> TemplateEngine.MUSTACHE;
        };
    }

    /**
     * Ordered template-engine selection use cases.
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares 5 enum singleton values in source order. The values label intent and perform no independent validation or enforcement.
     * @m3.dataStructures Ordered enum singleton values and compiler-generated values/valueOf accessors.
     */
    public enum UseCase {
        /**
         * Java code gen selection label.
         * @m3.pattern synexia:iop
         * @m3.algorithm Declares the JAVA_CODE_GEN enum singleton in its original source position, retaining its name and ordinal without additional state.
         * @m3.dataStructures One enum singleton reference and its inherited name and ordinal.
         */
        JAVA_CODE_GEN,
        /**
         * Multi language code selection label.
         * @m3.pattern synexia:iop
         * @m3.algorithm Declares the MULTI_LANGUAGE_CODE enum singleton in its original source position, retaining its name and ordinal without additional state.
         * @m3.dataStructures One enum singleton reference and its inherited name and ordinal.
         */
        MULTI_LANGUAGE_CODE,
        /**
         * Text templates selection label.
         * @m3.pattern synexia:iop
         * @m3.algorithm Declares the TEXT_TEMPLATES enum singleton in its original source position, retaining its name and ordinal without additional state.
         * @m3.dataStructures One enum singleton reference and its inherited name and ordinal.
         */
        TEXT_TEMPLATES,
        /**
         * Simple substitution selection label.
         * @m3.pattern synexia:iop
         * @m3.algorithm Declares the SIMPLE_SUBSTITUTION enum singleton in its original source position, retaining its name and ordinal without additional state.
         * @m3.dataStructures One enum singleton reference and its inherited name and ordinal.
         */
        SIMPLE_SUBSTITUTION,
        /**
         * Logic less selection label.
         * @m3.pattern synexia:iop
         * @m3.algorithm Declares the LOGIC_LESS enum singleton in its original source position, retaining its name and ordinal without additional state.
         * @m3.dataStructures One enum singleton reference and its inherited name and ordinal.
         */
        LOGIC_LESS
    }
}
