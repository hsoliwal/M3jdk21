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
 * IOP_TEMPLATE_LAW - The Immutable Rules for LLM-Bound Template Generation.
 *
 * <h2>LOCKED - CANNOT BE OVERRIDDEN</h2>
 *
 * <h2>THE ONE IOP SENTENCE (FINAL FORM)</h2>
 *
 * <pre>
 * ═══════════════════════════════════════════════════════════════════════════
 *
 *   Annotations choose libraries.
 *   Templates choose structure.
 *   Markers choose LLM scope.
 *   Anything else is illegal.
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * </pre>
 *
 * <h2>IOP TEMPLATE LAW (non-negotiable)</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                                                                           ║
 * ║   All templates MUST explicitly mark where the LLM is allowed to enhance. ║
 * ║   If a region is not marked, the LLM is forbidden to touch it.            ║
 * ║                                                                           ║
 * ║   No guessing.                                                            ║
 * ║   No "helpful" expansion.                                                 ║
 * ║   No silent edits.                                                        ║
 * ║   Silence = Prohibition.                                                  ║
 * ║                                                                           ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>PARADIGM STACK</h2>
 *
 * <pre>
 * COP → AOP → OOP → FOP → IOP → AnOP
 * (Context, Aspect, Object, Functional, Interface, Annotation-Oriented Programming)
 * </pre>
 *
 * <h2>THREE REGION TYPES</h2>
 *
 * <pre>
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │  REGION          │ OWNER           │ LLM ACCESS                        │
 * ├─────────────────────────────────────────────────────────────────────────┤
 * │  LOCKED          │ CPU/Template    │ FORBIDDEN                         │
 * │  LLM_ALLOWED     │ LLM             │ MAY GENERATE (bounded)            │
 * │  LLM_HINT        │ Guidance        │ READ-ONLY                         │
 * └─────────────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h2>CANONICAL MARKER SYNTAX</h2>
 *
 * <pre>{@code
 * // &lt;IOP-LLM-ALLOWED:method-body>
 * // LANG=Java
 * // ROLE=business-logic
 * // LIBRARY=apache-httpclient
 * // CONSTRAINTS:
 * // - must satisfy @IopInvariant
 * // - no new imports
 * // - no threading
 * // </IOP-LLM-ALLOWED>
 * }</pre>
 *
 * <h2>ANNOTATION → LIBRARY BINDING</h2>
 *
 * <pre>
 * {@code
 * &#64;AnOPTransport(
 *     type = Transport.HTTP,
 *     libraries = {
 *         &#64;AnOPLibrary(name = "apache-httpclient", version = "5.x", preferred = true),
 *         &#64;AnOPLibrary(name = "okhttp", version = "4.x")
 *     }
 * )
 * }
 * </pre>
 *
 * <h2>TEMPLATE DIRECTORY STRUCTURE (Library-Bound)</h2>
 *
 * <pre>
 * templates/
 *  └── transport/
 *      ├── http/
 *      │   ├── apache-httpclient/
 *      │   │   └── Client.java.ftl
 *      │   └── okhttp/
 *      │       └── Client.java.ftl
 *      └── rpc/
 *          └── apache-thrift/
 *              └── ServiceHandler.java.ftl
 * </pre>
 *
 * <h2>LLM WRITE PERMISSIONS MATRIX</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  ACTION                              │ ALLOWED                           ║
 * ╠═══════════════════════════════════════════════════════════════════════════╣
 * ║  Write method body (inside marker)   │ ✓                                 ║
 * ║  Add private helpers (if allowed)    │ ⚠ conditional                     ║
 * ║  Add comments (inside marker)        │ ✓                                 ║
 * ╠═══════════════════════════════════════════════════════════════════════════╣
 * ║  Add imports                         │ ✗ FORBIDDEN                       ║
 * ║  Change class signature              │ ✗ FORBIDDEN                       ║
 * ║  Add dependencies                    │ ✗ FORBIDDEN                       ║
 * ║  Choose different library            │ ✗ FORBIDDEN                       ║
 * ║  Change threading / IO model         │ ✗ FORBIDDEN                       ║
 * ║  Add fields                          │ ✗ FORBIDDEN                       ║
 * ║  Change constructors                 │ ✗ FORBIDDEN                       ║
 * ║  Add annotations                     │ ✗ FORBIDDEN                       ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>AI ENHANCEMENT MARKER FORMAT</h2>
 *
 * <pre>
 * &#64;AI-TEMPLATE: type=TYPE, version=VERSION, pattern=PATTERN
 * &#64;AI-ENHANCE: section=SECTION, priority=PRIORITY, scope=SCOPE
 * &#64;AI-LIB-HINT: category=package.name (description)
 * &#64;AI-LIB: specific.library.usage
 * &#64;AI-INFO: informational comment
 * </pre>
 *
 * <h2>LIBRARY FAMILIES</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  CATEGORY       │ LIBRARIES                                              ║
 * ╠═══════════════════════════════════════════════════════════════════════════╣
 * ║  HTTP           │ apache-httpclient-5, okhttp-4, java-http-client        ║
 * ║  RPC            │ apache-thrift, grpc, avro                              ║
 * ║  JSON           │ jackson, gson, jsonb                                   ║
 * ║  Validation     │ jakarta-validation, commons-validate                   ║
 * ║  DI             │ jakarta-inject, spring, guice                          ║
 * ║  Cache          │ caffeine, ehcache, guava-cache                         ║
 * ║  Pool           │ commons-pool2                                          ║
 * ║  Collections    │ eclipse-collections, guava, vavr                       ║
 * ║  NLP            │ apache-opennlp                                         ║
 * ║  Regex          │ google-re2j (safe, no ReDoS)                           ║
 * ║  IO             │ commons-io, java-nio                                   ║
 * ║  System         │ oshi-core                                              ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>END-TO-END FLOW</h2>
 *
 * <pre>
 * 1. Human writes annotations (IOP/AnOP)
 * 2. CPU selects template (library-bound)
 * 3. Template expands structure (LOCKED)
 * 4. Markers declare LLM scope (LLM_ALLOWED)
 * 5. LLM fills semantic gaps ONLY
 * 6. Diff validator enforces legality
 * 7. JDT/LSP reflects intent + constraints
 * </pre>
 *
 * <h2>DIFF VALIDATOR RULES (Build Gate)</h2>
 *
 * <pre>
 * 1. No edits outside IOP-LLM-ALLOWED
 * 2. No new imports unless explicitly marked
 * 3. No dependency drift
 * 4. No public API change
 * 5. Invariants preserved
 *
 * Failure = Build fails, agent output rejected
 * </pre>
 *
 * @since IOP 1.0 - Template Law
 * @see IOP_GENERATION_LAW
 * @see IOP_TYPE_HIERARCHY
 * @m3.pattern synexia:iop
 * @m3.algorithm Groups exact literal law text and ordered region, permission, prohibition, priority and library descriptors. The descriptor declarations and getters perform no template parsing or permission enforcement.
 * @m3.dataStructures Five static String constants, five nested enum types, and two final String fields per LibraryFamily value.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IOP_TEMPLATE_LAW.java}
 * blob {@code 9b225381df9e942f538f1a899dadec40892ed314} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class IOP_TEMPLATE_LAW {

    /**
     * Private constructor for the static declaration owner.
     * @m3.pattern synexia:bop
     * @m3.algorithm Executes an empty private constructor body, preventing ordinary caller construction.
     * @m3.dataStructures No constructor parameters or declared instance state.
     */
    private IOP_TEMPLATE_LAW() {}

    // --- THE IMMUTABLE RULES ---

    /** Rule 1: Markers define LLM scope.
     * @m3.pattern synexia:iop
     * @m3.algorithm Initializes one static final String from its exact source literal, retaining all text, punctuation and text-block newline or indentation semantics. The stored text does not enforce its stated law.
     * @m3.dataStructures One immutable String reference with the exact literal payload.
     */
    public static final String RULE_1 = """
			All templates MUST explicitly mark where LLM is allowed.
			If a region is not marked, the LLM is forbidden to touch it.
			Silence = Prohibition.
			""";

    /** Rule 2: Annotations choose libraries.
     * @m3.pattern synexia:iop
     * @m3.algorithm Initializes one static final String from its exact source literal, retaining all text, punctuation and text-block newline or indentation semantics. The stored text does not enforce its stated law.
     * @m3.dataStructures One immutable String reference with the exact literal payload.
     */
    public static final String RULE_2 = """
			Every annotation that implies behavior MUST declare allowed libraries.
			If a library is not declared, it is illegal everywhere.
			No guessing. No agent creativity.
			""";

    /** Rule 3: Templates are library-bound.
     * @m3.pattern synexia:iop
     * @m3.algorithm Initializes one static final String from its exact source literal, retaining all text, punctuation and text-block newline or indentation semantics. The stored text does not enforce its stated law.
     * @m3.dataStructures One immutable String reference with the exact literal payload.
     */
    public static final String RULE_3 = """
			Templates are library-specific, never generic.
			Annotation selects the template directory.
			CPU expands the template. LLM only touches marked regions.
			""";

    /** Rule 4: Marker inheritance.
     * @m3.pattern synexia:iop
     * @m3.algorithm Initializes one static final String from its exact source literal, retaining all text, punctuation and text-block newline or indentation semantics. The stored text does not enforce its stated law.
     * @m3.dataStructures One immutable String reference with the exact literal payload.
     */
    public static final String RULE_4 = """
			Markers inherit constraints from annotations:
			- @IopInvariant must be respected inside marker
			- @AnOPTransport constraints apply
			- @AnOPLibrary limits available libraries
			""";

    /** Rule 5: The One Sentence.
     * @m3.pattern synexia:iop
     * @m3.algorithm Initializes one static final String from its exact source literal, retaining all text, punctuation and text-block newline or indentation semantics. The stored text does not enforce its stated law.
     * @m3.dataStructures One immutable String reference with the exact literal payload.
     */
    public static final String THE_ONE_SENTENCE = """
			Annotations choose libraries.
			Templates choose structure.
			Markers choose LLM scope.
			Anything else is illegal.
			""";

    // --- REGION TYPES ---

    /** Template region ownership.
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares 3 enum singleton values in source order. The values label intent and perform no independent validation or enforcement.
     * @m3.dataStructures Ordered enum singleton values and compiler-generated values/valueOf accessors.
     */
    public enum RegionType {
        /** CPU/Template owns - LLM FORBIDDEN
         * @m3.pattern synexia:iop
         * @m3.algorithm Declares the LOCKED enum singleton in its original source position, retaining its name and ordinal without additional state.
         * @m3.dataStructures One enum singleton reference and its inherited name and ordinal.
         */
        LOCKED,

        /** LLM may generate - BOUNDED
         * @m3.pattern synexia:iop
         * @m3.algorithm Declares the LLM_ALLOWED enum singleton in its original source position, retaining its name and ordinal without additional state.
         * @m3.dataStructures One enum singleton reference and its inherited name and ordinal.
         */
        LLM_ALLOWED,

        /** Guidance only - READ-ONLY
         * @m3.pattern synexia:iop
         * @m3.algorithm Declares the LLM_HINT enum singleton in its original source position, retaining its name and ordinal without additional state.
         * @m3.dataStructures One enum singleton reference and its inherited name and ordinal.
         */
        LLM_HINT
    }

    // --- LLM PERMISSIONS ---

    /** What LLM is allowed to do inside markers.
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares 3 enum singleton values in source order. The values label intent and perform no independent validation or enforcement.
     * @m3.dataStructures Ordered enum singleton values and compiler-generated values/valueOf accessors.
     */
    public enum LLMPermission {
        /**
         * Allowed-in-marker WRITE_METHOD_BODY action label.
         * @m3.pattern synexia:iop
         * @m3.algorithm Declares the WRITE_METHOD_BODY enum singleton in its original source position, retaining its name and ordinal without additional state.
         * @m3.dataStructures One enum singleton reference and its inherited name and ordinal.
         */
        WRITE_METHOD_BODY,
        /**
         * Allowed-in-marker ADD_PRIVATE_HELPER action label.
         * @m3.pattern synexia:iop
         * @m3.algorithm Declares the ADD_PRIVATE_HELPER enum singleton in its original source position, retaining its name and ordinal without additional state.
         * @m3.dataStructures One enum singleton reference and its inherited name and ordinal.
         */
        ADD_PRIVATE_HELPER,
        /**
         * Allowed-in-marker ADD_COMMENT action label.
         * @m3.pattern synexia:iop
         * @m3.algorithm Declares the ADD_COMMENT enum singleton in its original source position, retaining its name and ordinal without additional state.
         * @m3.dataStructures One enum singleton reference and its inherited name and ordinal.
         */
        ADD_COMMENT
    }

    /** What LLM is FORBIDDEN from doing.
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares 8 enum singleton values in source order. The values label intent and perform no independent validation or enforcement.
     * @m3.dataStructures Ordered enum singleton values and compiler-generated values/valueOf accessors.
     */
    public enum LLMForbidden {
        /**
         * Forbidden ADD_IMPORTS action label.
         * @m3.pattern synexia:iop
         * @m3.algorithm Declares the ADD_IMPORTS enum singleton in its original source position, retaining its name and ordinal without additional state.
         * @m3.dataStructures One enum singleton reference and its inherited name and ordinal.
         */
        ADD_IMPORTS,
        /**
         * Forbidden CHANGE_CLASS_SIGNATURE action label.
         * @m3.pattern synexia:iop
         * @m3.algorithm Declares the CHANGE_CLASS_SIGNATURE enum singleton in its original source position, retaining its name and ordinal without additional state.
         * @m3.dataStructures One enum singleton reference and its inherited name and ordinal.
         */
        CHANGE_CLASS_SIGNATURE,
        /**
         * Forbidden ADD_DEPENDENCIES action label.
         * @m3.pattern synexia:iop
         * @m3.algorithm Declares the ADD_DEPENDENCIES enum singleton in its original source position, retaining its name and ordinal without additional state.
         * @m3.dataStructures One enum singleton reference and its inherited name and ordinal.
         */
        ADD_DEPENDENCIES,
        /**
         * Forbidden CHOOSE_DIFFERENT_LIBRARY action label.
         * @m3.pattern synexia:iop
         * @m3.algorithm Declares the CHOOSE_DIFFERENT_LIBRARY enum singleton in its original source position, retaining its name and ordinal without additional state.
         * @m3.dataStructures One enum singleton reference and its inherited name and ordinal.
         */
        CHOOSE_DIFFERENT_LIBRARY,
        /**
         * Forbidden CHANGE_THREADING_MODEL action label.
         * @m3.pattern synexia:iop
         * @m3.algorithm Declares the CHANGE_THREADING_MODEL enum singleton in its original source position, retaining its name and ordinal without additional state.
         * @m3.dataStructures One enum singleton reference and its inherited name and ordinal.
         */
        CHANGE_THREADING_MODEL,
        /**
         * Forbidden ADD_FIELDS action label.
         * @m3.pattern synexia:iop
         * @m3.algorithm Declares the ADD_FIELDS enum singleton in its original source position, retaining its name and ordinal without additional state.
         * @m3.dataStructures One enum singleton reference and its inherited name and ordinal.
         */
        ADD_FIELDS,
        /**
         * Forbidden CHANGE_CONSTRUCTORS action label.
         * @m3.pattern synexia:iop
         * @m3.algorithm Declares the CHANGE_CONSTRUCTORS enum singleton in its original source position, retaining its name and ordinal without additional state.
         * @m3.dataStructures One enum singleton reference and its inherited name and ordinal.
         */
        CHANGE_CONSTRUCTORS,
        /**
         * Forbidden ADD_ANNOTATIONS action label.
         * @m3.pattern synexia:iop
         * @m3.algorithm Declares the ADD_ANNOTATIONS enum singleton in its original source position, retaining its name and ordinal without additional state.
         * @m3.dataStructures One enum singleton reference and its inherited name and ordinal.
         */
        ADD_ANNOTATIONS
    }

    // --- PRIORITY LEVELS ---

    /** AI enhancement priority levels.
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares 4 enum singleton values in source order. The values label intent and perform no independent validation or enforcement.
     * @m3.dataStructures Ordered enum singleton values and compiler-generated values/valueOf accessors.
     */
    public enum Priority {
        /** Must be enhanced - HIGH token budget
         * @m3.pattern synexia:iop
         * @m3.algorithm Declares the CRITICAL enum singleton in its original source position, retaining its name and ordinal without additional state.
         * @m3.dataStructures One enum singleton reference and its inherited name and ordinal.
         */
        CRITICAL,

        /** Should be enhanced - MEDIUM token budget
         * @m3.pattern synexia:iop
         * @m3.algorithm Declares the HIGH enum singleton in its original source position, retaining its name and ordinal without additional state.
         * @m3.dataStructures One enum singleton reference and its inherited name and ordinal.
         */
        HIGH,

        /** May be enhanced - LOW token budget
         * @m3.pattern synexia:iop
         * @m3.algorithm Declares the MEDIUM enum singleton in its original source position, retaining its name and ordinal without additional state.
         * @m3.dataStructures One enum singleton reference and its inherited name and ordinal.
         */
        MEDIUM,

        /** Optional enhancement - MINIMAL token budget
         * @m3.pattern synexia:iop
         * @m3.algorithm Declares the LOW enum singleton in its original source position, retaining its name and ordinal without additional state.
         * @m3.dataStructures One enum singleton reference and its inherited name and ordinal.
         */
        LOW
    }

    // --- LIBRARY FAMILIES ---

    /** Pre-approved library families.
     * @m3.pattern synexia:iop
     * @m3.algorithm Defines twenty-five singleton values in source order, stores the two constructor-supplied String references and exposes them through direct accessors. It does not load libraries, resolve artifacts or decide whether imports are permitted.
     * @m3.dataStructures Twenty-five enum singleton values with final packagePrefix and description String fields.
     */
    public enum LibraryFamily {

        // HTTP
        /**
         * APACHE_HTTPCLIENT_5 library descriptor.
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the APACHE_HTTPCLIENT_5 singleton with exact packagePrefix text "org.apache.hc.client5" and description text "Apache HttpClient 5.x" in its original ordinal position.
         * @m3.dataStructures One enum singleton and two final String references.
         */
        APACHE_HTTPCLIENT_5("org.apache.hc.client5", "Apache HttpClient 5.x"),
        /**
         * OKHTTP_4 library descriptor.
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the OKHTTP_4 singleton with exact packagePrefix text "com.squareup.okhttp3" and description text "OkHttp 4.x" in its original ordinal position.
         * @m3.dataStructures One enum singleton and two final String references.
         */
        OKHTTP_4("com.squareup.okhttp3", "OkHttp 4.x"),
        /**
         * JAVA_HTTP_CLIENT library descriptor.
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the JAVA_HTTP_CLIENT singleton with exact packagePrefix text "java.net.http" and description text "Java 11+ HTTP Client" in its original ordinal position.
         * @m3.dataStructures One enum singleton and two final String references.
         */
        JAVA_HTTP_CLIENT("java.net.http", "Java 11+ HTTP Client"),

        // RPC
        /**
         * APACHE_THRIFT library descriptor.
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the APACHE_THRIFT singleton with exact packagePrefix text "org.apache.thrift" and description text "Apache Thrift" in its original ordinal position.
         * @m3.dataStructures One enum singleton and two final String references.
         */
        APACHE_THRIFT("org.apache.thrift", "Apache Thrift"),
        /**
         * GRPC library descriptor.
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the GRPC singleton with exact packagePrefix text "io.grpc" and description text "gRPC" in its original ordinal position.
         * @m3.dataStructures One enum singleton and two final String references.
         */
        GRPC("io.grpc", "gRPC"),
        /**
         * AVRO library descriptor.
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the AVRO singleton with exact packagePrefix text "org.apache.avro" and description text "Apache Avro" in its original ordinal position.
         * @m3.dataStructures One enum singleton and two final String references.
         */
        AVRO("org.apache.avro", "Apache Avro"),

        // JSON
        /**
         * JACKSON library descriptor.
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the JACKSON singleton with exact packagePrefix text "com.fasterxml.jackson" and description text "Jackson 2.x" in its original ordinal position.
         * @m3.dataStructures One enum singleton and two final String references.
         */
        JACKSON("com.fasterxml.jackson", "Jackson 2.x"),
        /**
         * GSON library descriptor.
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the GSON singleton with exact packagePrefix text "com.google.gson" and description text "Google Gson" in its original ordinal position.
         * @m3.dataStructures One enum singleton and two final String references.
         */
        GSON("com.google.gson", "Google Gson"),
        /**
         * JSONB library descriptor.
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the JSONB singleton with exact packagePrefix text "jakarta.json.bind" and description text "Jakarta JSON-B" in its original ordinal position.
         * @m3.dataStructures One enum singleton and two final String references.
         */
        JSONB("jakarta.json.bind", "Jakarta JSON-B"),

        // Validation
        /**
         * JAKARTA_VALIDATION library descriptor.
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the JAKARTA_VALIDATION singleton with exact packagePrefix text "jakarta.validation" and description text "Bean Validation 3.0" in its original ordinal position.
         * @m3.dataStructures One enum singleton and two final String references.
         */
        JAKARTA_VALIDATION("jakarta.validation", "Bean Validation 3.0"),
        /**
         * COMMONS_VALIDATE library descriptor.
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the COMMONS_VALIDATE singleton with exact packagePrefix text "org.apache.commons.lang3.Validate" and description text "Commons Lang3" in its original ordinal position.
         * @m3.dataStructures One enum singleton and two final String references.
         */
        COMMONS_VALIDATE("org.apache.commons.lang3.Validate", "Commons Lang3"),

        // DI
        /**
         * JAKARTA_INJECT library descriptor.
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the JAKARTA_INJECT singleton with exact packagePrefix text "jakarta.inject" and description text "JSR-330 DI" in its original ordinal position.
         * @m3.dataStructures One enum singleton and two final String references.
         */
        JAKARTA_INJECT("jakarta.inject", "JSR-330 DI"),
        /**
         * SPRING_CONTEXT library descriptor.
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the SPRING_CONTEXT singleton with exact packagePrefix text "org.springframework.context" and description text "Spring Framework" in its original ordinal position.
         * @m3.dataStructures One enum singleton and two final String references.
         */
        SPRING_CONTEXT("org.springframework.context", "Spring Framework"),
        /**
         * GUICE library descriptor.
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the GUICE singleton with exact packagePrefix text "com.google.inject" and description text "Google Guice" in its original ordinal position.
         * @m3.dataStructures One enum singleton and two final String references.
         */
        GUICE("com.google.inject", "Google Guice"),

        // Cache
        /**
         * CAFFEINE library descriptor.
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the CAFFEINE singleton with exact packagePrefix text "com.github.benmanes.caffeine" and description text "Caffeine Cache" in its original ordinal position.
         * @m3.dataStructures One enum singleton and two final String references.
         */
        CAFFEINE("com.github.benmanes.caffeine", "Caffeine Cache"),
        /**
         * EHCACHE library descriptor.
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the EHCACHE singleton with exact packagePrefix text "org.ehcache" and description text "Ehcache 3.x" in its original ordinal position.
         * @m3.dataStructures One enum singleton and two final String references.
         */
        EHCACHE("org.ehcache", "Ehcache 3.x"),
        /**
         * GUAVA_CACHE library descriptor.
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the GUAVA_CACHE singleton with exact packagePrefix text "com.google.common.cache" and description text "Guava Cache" in its original ordinal position.
         * @m3.dataStructures One enum singleton and two final String references.
         */
        GUAVA_CACHE("com.google.common.cache", "Guava Cache"),

        // Pool
        /**
         * COMMONS_POOL2 library descriptor.
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the COMMONS_POOL2 singleton with exact packagePrefix text "org.apache.commons.pool2" and description text "Commons Pool 2" in its original ordinal position.
         * @m3.dataStructures One enum singleton and two final String references.
         */
        COMMONS_POOL2("org.apache.commons.pool2", "Commons Pool 2"),

        // Collections
        /**
         * ECLIPSE_COLLECTIONS library descriptor.
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the ECLIPSE_COLLECTIONS singleton with exact packagePrefix text "org.eclipse.collections" and description text "Eclipse Collections" in its original ordinal position.
         * @m3.dataStructures One enum singleton and two final String references.
         */
        ECLIPSE_COLLECTIONS("org.eclipse.collections", "Eclipse Collections"),
        /**
         * GUAVA_COLLECT library descriptor.
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the GUAVA_COLLECT singleton with exact packagePrefix text "com.google.common.collect" and description text "Guava Collections" in its original ordinal position.
         * @m3.dataStructures One enum singleton and two final String references.
         */
        GUAVA_COLLECT("com.google.common.collect", "Guava Collections"),
        /**
         * VAVR library descriptor.
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the VAVR singleton with exact packagePrefix text "io.vavr" and description text "Vavr Functional" in its original ordinal position.
         * @m3.dataStructures One enum singleton and two final String references.
         */
        VAVR("io.vavr", "Vavr Functional"),

        // NLP
        /**
         * APACHE_OPENNLP library descriptor.
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the APACHE_OPENNLP singleton with exact packagePrefix text "opennlp.tools" and description text "Apache OpenNLP" in its original ordinal position.
         * @m3.dataStructures One enum singleton and two final String references.
         */
        APACHE_OPENNLP("opennlp.tools", "Apache OpenNLP"),

        // Regex
        /**
         * GOOGLE_RE2J library descriptor.
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the GOOGLE_RE2J singleton with exact packagePrefix text "com.google.re2j" and description text "Google RE2 (safe regex)" in its original ordinal position.
         * @m3.dataStructures One enum singleton and two final String references.
         */
        GOOGLE_RE2J("com.google.re2j", "Google RE2 (safe regex)"),

        // IO
        /**
         * COMMONS_IO library descriptor.
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the COMMONS_IO singleton with exact packagePrefix text "org.apache.commons.io" and description text "Commons IO" in its original ordinal position.
         * @m3.dataStructures One enum singleton and two final String references.
         */
        COMMONS_IO("org.apache.commons.io", "Commons IO"),
        /**
         * JAVA_NIO library descriptor.
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the JAVA_NIO singleton with exact packagePrefix text "java.nio" and description text "Java NIO" in its original ordinal position.
         * @m3.dataStructures One enum singleton and two final String references.
         */
        JAVA_NIO("java.nio", "Java NIO");

        /**
         * Immutable packagePrefix text for a library-family descriptor.
         * @m3.pattern synexia:iop
         * @m3.algorithm Retains the constructor-supplied packagePrefix String value in the corresponding final field without validation or normalization.
         * @m3.dataStructures One final String field per instance.
         */
        private final String packagePrefix;
        /**
         * Immutable description text for a library-family descriptor.
         * @m3.pattern synexia:iop
         * @m3.algorithm Retains the constructor-supplied description String value in the corresponding final field without validation or normalization.
         * @m3.dataStructures One final String field per instance.
         */
        private final String description;

        /**
         * Initializes a library-family text descriptor.
         * @m3.pattern synexia:bop
         * @m3.algorithm Assigns the supplied packagePrefix and description String references to corresponding final fields without validation, loading or normalization.
         * @m3.dataStructures Two String parameters and two final String instance fields.
         */
        LibraryFamily(String packagePrefix, String description) {
            this.packagePrefix = packagePrefix;
            this.description = description;
        }

        /**
         * Accessor for the stored packagePrefix value.
         * @m3.pattern synexia:bop
         * @m3.algorithm Returns the existing packagePrefix field directly without conversion, validation or mutation.
         * @m3.dataStructures One stored String value and its returned value.
         */
        public String packagePrefix() {
            return packagePrefix;
        }

        /**
         * Accessor for the stored description value.
         * @m3.pattern synexia:bop
         * @m3.algorithm Returns the existing description field directly without conversion, validation or mutation.
         * @m3.dataStructures One stored String value and its returned value.
         */
        public String description() {
            return description;
        }
    }
}
