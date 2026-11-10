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

import jdk.internal.m3.paradigm.annotation.LLM;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AILibraryHint - AnOP annotation for library/framework recommendations.
 *
 * <h2>AnOP Advisory: Library Selection</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  Guide LLM to use specific libraries instead of reinventing the wheel.   ║
 * ║  "If FOSS solved it, don't reinvent it."                                 ║
 * ║                                                                           ║
 * ║  PRIORITY: FOSS Library > Project Utility > Custom Implementation        ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	&#64;AILibraryHint(category = Category.COLLECTIONS, preferred = {"com.google.guava:guava",
 * 			"org.eclipse.collections:eclipse-collections"}, use = "ImmutableList, ImmutableMap for immutable collections")
 * 	public interface IDataService&lt;Self extends IDataService&lt;Self&gt;&gt; extends IContext&lt;Self&gt; {
 * 	}
 *
 * 	&#64;AILibraryHint(category = Category.JSON, avoid = {"org.json:json"}, preferred = {
 * 			"com.fasterxml.jackson.core:jackson-databind"}, reason = "Jackson is faster and has better streaming support")
 * 	public interface ISerializationService&lt;Self extends ISerializationService&lt;Self>> extends IContext&lt;Self> {
 * 	}
 *
 * 	&#64;AILibraryHint(category = Category.HTTP_CLIENT, preferred = {
 * 			"org.apache.httpcomponents.client5:httpclient5"}, minVersion = "5.0", features = {"async", "http2"})
 * 	public interface IHttpService&lt;Self extends IHttpService&lt;Self>> extends IContext&lt;Self> {
 * 	}
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Library Hints
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares library and framework selection preferences, alternatives, versions and integration guidance; this annotation records guidance rather than implementing or verifying the requested behavior.
 * @m3.dataStructures SOURCE-retained annotation targeting {ElementType.TYPE, ElementType.METHOD, ElementType.PACKAGE}; preserves its declared element and nested-token schema.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAILibraryHint.java}
 * blob {@code eb58b00d60bde3e10efffaa1bafd58d817edec49} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PACKAGE})
@Retention(RetentionPolicy.SOURCE)
@Documented
@Repeatable(IAILibraryHints.class)
public @interface IAILibraryHint {

    /**
     * Library category.
     *
     * @return category
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAILibraryHint#category advisory metadata for Library category. The element itself neither executes nor enforces that request.
     * @m3.dataStructures Category annotation element; exact default expression Category.GENERAL.
     */
    Category category() default Category.GENERAL;

    /**
     * Preferred libraries (Maven coordinates: groupId:artifactId).
     *
     * @return preferred libraries
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAILibraryHint#preferred advisory metadata for Preferred libraries (Maven coordinates: groupId:artifactId). The element itself neither executes nor enforces that request.
     * @m3.dataStructures String[] annotation element; exact default expression {}.
     */
    String[] preferred() default {};

    /**
     * Libraries to avoid.
     *
     * @return avoided libraries
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAILibraryHint#avoid advisory metadata for Libraries to avoid. The element itself neither executes nor enforces that request.
     * @m3.dataStructures String[] annotation element; exact default expression {}.
     */
    String[] avoid() default {};

    /**
     * Specific classes/methods to use from the library.
     *
     * @return usage guidance
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAILibraryHint#use advisory metadata for Specific classes/methods to use from the library. The element itself neither executes nor enforces that request.
     * @m3.dataStructures String annotation element; exact default expression "".
     */
    String use() default "";

    /**
     * Minimum version required.
     *
     * @return min version
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAILibraryHint#minVersion advisory metadata for Minimum version required. The element itself neither executes nor enforces that request.
     * @m3.dataStructures String annotation element; exact default expression "".
     */
    String minVersion() default "";

    /**
     * Maximum version allowed.
     *
     * @return max version
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAILibraryHint#maxVersion advisory metadata for Maximum version allowed. The element itself neither executes nor enforces that request.
     * @m3.dataStructures String annotation element; exact default expression "".
     */
    String maxVersion() default "";

    /**
     * Required features from the library.
     *
     * @return required features
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAILibraryHint#features advisory metadata for Required features from the library. The element itself neither executes nor enforces that request.
     * @m3.dataStructures String[] annotation element; exact default expression {}.
     */
    String[] features() default {};

    /**
     * Reason for library choice.
     *
     * @return reason
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAILibraryHint#reason advisory metadata for Reason for library choice. The element itself neither executes nor enforces that request.
     * @m3.dataStructures String annotation element; exact default expression "".
     */
    String reason() default "";

    /**
     * Alternative if preferred not available.
     *
     * @return fallback libraries
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAILibraryHint#fallback advisory metadata for Alternative if preferred not available. The element itself neither executes nor enforces that request.
     * @m3.dataStructures String[] annotation element; exact default expression {}.
     */
    String[] fallback() default {};

    /**
     * License compatibility requirement.
     *
     * @return license
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAILibraryHint#license advisory metadata for License compatibility requirement. The element itself neither executes nor enforces that request.
     * @m3.dataStructures License annotation element; exact default expression License.ANY_OPEN_SOURCE.
     */
    License license() default License.ANY_OPEN_SOURCE;

    // --- LIBRARY CATEGORIES ---

    /** Library categories covering common Java ecosystem needs. */
    /**
     * Category.
     * @author Synexia
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates 60 symbolic choices for Category. @author Synexia within IAILibraryHint advisory metadata; no selected strategy executes in this enum.
     * @m3.dataStructures Nested IAILibraryHint.Category enum with 60 source-declared constants in existing order.
     */
    enum Category {
        /** General purpose.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#GENERAL token within Category. @author Synexia: General purpose. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        GENERAL,

        // --- CORE UTILITIES ---

        /** Collections (Guava, Eclipse Collections, Apache Commons Collections).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#COLLECTIONS token within Category. @author Synexia: Collections (Guava, Eclipse Collections, Apache Commons Collections). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        COLLECTIONS,

        /** String utilities (Apache Commons Lang, Guava).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#STRING_UTILS token within Category. @author Synexia: String utilities (Apache Commons Lang, Guava). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        STRING_UTILS,

        /** I/O utilities (Apache Commons IO, Guava).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#IO_UTILS token within Category. @author Synexia: I/O utilities (Apache Commons IO, Guava). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        IO_UTILS,

        /** Concurrency utilities (JCTools, Disruptor).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#CONCURRENCY token within Category. @author Synexia: Concurrency utilities (JCTools, Disruptor). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        CONCURRENCY,

        /** Date/Time (java.time, Joda-Time).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#DATE_TIME token within Category. @author Synexia: Date/Time (java.time, Joda-Time). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        DATE_TIME,

        /** Math/Numeric (Apache Commons Math, JScience).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#MATH token within Category. @author Synexia: Math/Numeric (Apache Commons Math, JScience). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        MATH,

        /** Reflection (Reflections, ClassGraph).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#REFLECTION token within Category. @author Synexia: Reflection (Reflections, ClassGraph). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        REFLECTION,

        // --- SERIALIZATION ---

        /** JSON (Jackson, Gson, JSON-B).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#JSON token within Category. @author Synexia: JSON (Jackson, Gson, JSON-B). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        JSON,

        /** XML (JAXB, Jackson XML, XStream).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#XML token within Category. @author Synexia: XML (JAXB, Jackson XML, XStream). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        XML,

        /** YAML (SnakeYAML, Jackson YAML).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#YAML token within Category. @author Synexia: YAML (SnakeYAML, Jackson YAML). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        YAML,

        /** CSV (Apache Commons CSV, OpenCSV).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#CSV token within Category. @author Synexia: CSV (Apache Commons CSV, OpenCSV). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        CSV,

        /** Binary serialization (Protobuf, Avro, Kryo).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#BINARY_SERIALIZATION token within Category. @author Synexia: Binary serialization (Protobuf, Avro, Kryo). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        BINARY_SERIALIZATION,

        // --- NETWORKING ---

        /** HTTP Client (Apache HttpClient 5, OkHttp, Java HttpClient).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#HTTP_CLIENT token within Category. @author Synexia: HTTP Client (Apache HttpClient 5, OkHttp, Java HttpClient). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        HTTP_CLIENT,

        /** REST Client (RestTemplate, WebClient, Feign).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#REST_CLIENT token within Category. @author Synexia: REST Client (RestTemplate, WebClient, Feign). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        REST_CLIENT,

        /** WebSocket (Tyrus, Jetty WebSocket).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#WEBSOCKET token within Category. @author Synexia: WebSocket (Tyrus, Jetty WebSocket). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        WEBSOCKET,

        /** gRPC.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#GRPC token within Category. @author Synexia: gRPC. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        GRPC,

        /** GraphQL (graphql-java, Netflix DGS).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#GRAPHQL token within Category. @author Synexia: GraphQL (graphql-java, Netflix DGS). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        GRAPHQL,

        // --- DATABASE ---

        /** JDBC utilities (Apache Commons DBCP, HikariCP).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#JDBC token within Category. @author Synexia: JDBC utilities (Apache Commons DBCP, HikariCP). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        JDBC,

        /** Connection pooling (HikariCP, c3p0, Apache DBCP).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#CONNECTION_POOL token within Category. @author Synexia: Connection pooling (HikariCP, c3p0, Apache DBCP). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        CONNECTION_POOL,

        /** ORM (Hibernate, EclipseLink, MyBatis).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#ORM token within Category. @author Synexia: ORM (Hibernate, EclipseLink, MyBatis). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        ORM,

        /** SQL Builder (jOOQ, QueryDSL).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#SQL_BUILDER token within Category. @author Synexia: SQL Builder (jOOQ, QueryDSL). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        SQL_BUILDER,

        /** NoSQL clients (MongoDB, Redis, Cassandra).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#NOSQL token within Category. @author Synexia: NoSQL clients (MongoDB, Redis, Cassandra). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        NOSQL,

        // --- CACHING ---

        /** Local cache (Caffeine, Guava Cache, Ehcache).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#CACHE_LOCAL token within Category. @author Synexia: Local cache (Caffeine, Guava Cache, Ehcache). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        CACHE_LOCAL,

        /** Distributed cache (Redis, Hazelcast, Infinispan).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#CACHE_DISTRIBUTED token within Category. @author Synexia: Distributed cache (Redis, Hazelcast, Infinispan). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        CACHE_DISTRIBUTED,

        // --- MESSAGING ---

        /** Message queue clients (Kafka, RabbitMQ, ActiveMQ).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#MESSAGING token within Category. @author Synexia: Message queue clients (Kafka, RabbitMQ, ActiveMQ). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        MESSAGING,

        /** Event bus (Guava EventBus, MBassador).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#EVENT_BUS token within Category. @author Synexia: Event bus (Guava EventBus, MBassador). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        EVENT_BUS,

        // --- VALIDATION ---

        /** Bean Validation (Hibernate Validator, Apache BVal).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#VALIDATION token within Category. @author Synexia: Bean Validation (Hibernate Validator, Apache BVal). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        VALIDATION,

        /** Schema validation (JSON Schema, XML Schema).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#SCHEMA_VALIDATION token within Category. @author Synexia: Schema validation (JSON Schema, XML Schema). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        SCHEMA_VALIDATION,

        // --- SECURITY ---

        /** Cryptography (Bouncy Castle, Google Tink).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#CRYPTO token within Category. @author Synexia: Cryptography (Bouncy Castle, Google Tink). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        CRYPTO,

        /** Hashing (Guava Hashing, Apache Commons Codec).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#HASHING token within Category. @author Synexia: Hashing (Guava Hashing, Apache Commons Codec). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        HASHING,

        /** JWT (jjwt, Nimbus JOSE+JWT, Auth0 java-jwt).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#JWT token within Category. @author Synexia: JWT (jjwt, Nimbus JOSE+JWT, Auth0 java-jwt). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        JWT,

        /** OAuth/OIDC (Spring Security OAuth, Keycloak).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#OAUTH token within Category. @author Synexia: OAuth/OIDC (Spring Security OAuth, Keycloak). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        OAUTH,

        // --- LOGGING & MONITORING ---

        /** Logging (SLF4J, Logback, Log4j2).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#LOGGING token within Category. @author Synexia: Logging (SLF4J, Logback, Log4j2). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        LOGGING,

        /** Metrics (Micrometer, Dropwizard Metrics).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#METRICS token within Category. @author Synexia: Metrics (Micrometer, Dropwizard Metrics). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        METRICS,

        /** Tracing (OpenTelemetry, Jaeger, Zipkin).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#TRACING token within Category. @author Synexia: Tracing (OpenTelemetry, Jaeger, Zipkin). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        TRACING,

        // --- TESTING ---

        /** Unit testing (JUnit 5, TestNG).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#UNIT_TESTING token within Category. @author Synexia: Unit testing (JUnit 5, TestNG). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        UNIT_TESTING,

        /** Mocking (Mockito, EasyMock, PowerMock).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#MOCKING token within Category. @author Synexia: Mocking (Mockito, EasyMock, PowerMock). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        MOCKING,

        /** Assertions (AssertJ, Hamcrest, Truth).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#ASSERTIONS token within Category. @author Synexia: Assertions (AssertJ, Hamcrest, Truth). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        ASSERTIONS,

        /** Test containers (Testcontainers).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#TEST_CONTAINERS token within Category. @author Synexia: Test containers (Testcontainers). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        TEST_CONTAINERS,

        /** Property-based testing (jqwik, QuickCheck).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#PROPERTY_TESTING token within Category. @author Synexia: Property-based testing (jqwik, QuickCheck). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        PROPERTY_TESTING,

        // --- CODE GENERATION ---

        /** Boilerplate reduction (Lombok, AutoValue, Immutables).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#BOILERPLATE token within Category. @author Synexia: Boilerplate reduction (Lombok, AutoValue, Immutables). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        BOILERPLATE,

        /** Mapping (MapStruct, ModelMapper, Dozer).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#MAPPING token within Category. @author Synexia: Mapping (MapStruct, ModelMapper, Dozer). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        MAPPING,

        /** Annotation processing (JavaPoet, compile-testing).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#ANNOTATION_PROCESSING token within Category. @author Synexia: Annotation processing (JavaPoet, compile-testing). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        ANNOTATION_PROCESSING,

        // --- FUNCTIONAL ---

        /** Functional programming (Vavr, Cyclops, jOOL).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#FUNCTIONAL token within Category. @author Synexia: Functional programming (Vavr, Cyclops, jOOL). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        FUNCTIONAL,

        /** Reactive (Project Reactor, RxJava).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#REACTIVE token within Category. @author Synexia: Reactive (Project Reactor, RxJava). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        REACTIVE,

        // --- NLP / AI ---

        /** NLP (Apache OpenNLP, Stanford NLP).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#NLP token within Category. @author Synexia: NLP (Apache OpenNLP, Stanford NLP). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        NLP,

        /** LLM clients (Langchain4j, Spring AI).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#LLM token within Category. @author Synexia: LLM clients (Langchain4j, Spring AI). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        LLM,

        /** Vector stores (Milvus, Pinecone, Weaviate).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#VECTOR_STORE token within Category. @author Synexia: Vector stores (Milvus, Pinecone, Weaviate). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        VECTOR_STORE,

        // --- FILE FORMATS ---

        /** Excel (Apache POI, EasyExcel).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#EXCEL token within Category. @author Synexia: Excel (Apache POI, EasyExcel). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        EXCEL,

        /** PDF (Apache PDFBox, iText).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#PDF token within Category. @author Synexia: PDF (Apache PDFBox, iText). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        PDF,

        /** Image processing (ImageIO, Thumbnailator).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#IMAGE token within Category. @author Synexia: Image processing (ImageIO, Thumbnailator). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        IMAGE,

        /** Archive (Apache Commons Compress).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#ARCHIVE token within Category. @author Synexia: Archive (Apache Commons Compress). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        ARCHIVE,

        // --- CONFIGURATION ---

        /** Configuration (Typesafe Config, Apache Commons Configuration).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#CONFIG token within Category. @author Synexia: Configuration (Typesafe Config, Apache Commons Configuration). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        CONFIG,

        /** Feature flags (Unleash, FF4j, LaunchDarkly).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#FEATURE_FLAGS token within Category. @author Synexia: Feature flags (Unleash, FF4j, LaunchDarkly). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        FEATURE_FLAGS,

        // --- SCHEDULING ---

        /** Job scheduling (Quartz, db-scheduler).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#SCHEDULING token within Category. @author Synexia: Job scheduling (Quartz, db-scheduler). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        SCHEDULING,

        /** Workflow (Temporal, Camunda, Flowable).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#WORKFLOW token within Category. @author Synexia: Workflow (Temporal, Camunda, Flowable). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        WORKFLOW,

        // --- CLI ---

        /** CLI parsing (picocli, JCommander, Apache Commons CLI).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#CLI token within Category. @author Synexia: CLI parsing (picocli, JCommander, Apache Commons CLI). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        CLI,

        /** Terminal UI (JLine, Lanterna).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#TERMINAL token within Category. @author Synexia: Terminal UI (JLine, Lanterna). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        TERMINAL,

        // --- REGEX ---

        /** Safe Regex (Google RE2/J - no ReDoS).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.Category#REGEX token within Category. @author Synexia: Safe Regex (Google RE2/J - no ReDoS). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        REGEX
    }

    // --- LICENSE ---

    /** License compatibility.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates 8 symbolic choices for License compatibility within IAILibraryHint advisory metadata; no selected strategy executes in this enum.
     * @m3.dataStructures Nested IAILibraryHint.License enum with 8 source-declared constants in existing order.
     */
    enum License {
        /** Any open source license.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.License#ANY_OPEN_SOURCE token within License compatibility: Any open source license. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.License enum constant, retaining declared order and identity; it stores no implementation object.
         */
        ANY_OPEN_SOURCE,

        /** Apache 2.0 compatible.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.License#APACHE_2 token within License compatibility: Apache 2.0 compatible. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.License enum constant, retaining declared order and identity; it stores no implementation object.
         */
        APACHE_2,

        /** MIT compatible.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.License#MIT token within License compatibility: MIT compatible. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.License enum constant, retaining declared order and identity; it stores no implementation object.
         */
        MIT,

        /** BSD compatible.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.License#BSD token within License compatibility: BSD compatible. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.License enum constant, retaining declared order and identity; it stores no implementation object.
         */
        BSD,

        /** LGPL compatible.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.License#LGPL token within License compatibility: LGPL compatible. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.License enum constant, retaining declared order and identity; it stores no implementation object.
         */
        LGPL,

        /** GPL compatible (copyleft).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.License#GPL token within License compatibility: GPL compatible (copyleft). This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.License enum constant, retaining declared order and identity; it stores no implementation object.
         */
        GPL,

        /** Commercial license OK.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.License#COMMERCIAL_OK token within License compatibility: Commercial license OK. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.License enum constant, retaining declared order and identity; it stores no implementation object.
         */
        COMMERCIAL_OK,

        /** Public domain.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAILibraryHint.License#PUBLIC_DOMAIN token within License compatibility: Public domain. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAILibraryHint.License enum constant, retaining declared order and identity; it stores no implementation object.
         */
        PUBLIC_DOMAIN
    }
}
