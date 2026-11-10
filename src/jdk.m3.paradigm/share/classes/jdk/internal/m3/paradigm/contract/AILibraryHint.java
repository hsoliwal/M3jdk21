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
 *     &#64;code
 *     &#64;AILibraryHint(category = Category.COLLECTIONS, preferred = {
 *             "com.google.guava:guava",
 *             "org.eclipse.collections:eclipse-collections"}, use = "ImmutableList, ImmutableMap for immutable collections")
 *     public interface IDataService {
 *     }
 *
 *     &#64;AILibraryHint(category = Category.JSON, preferred = {
 *             "com.fasterxml.jackson.core:jackson-databind"}, avoid = {
 *                     "org.json:json"}, reason = "Jackson is faster and has better streaming support")
 *     public interface ISerializationService {
 *     }
 *
 *     &#64;AILibraryHint(category = Category.HTTP_CLIENT, preferred = {
 *             "org.apache.httpcomponents.client5:httpclient5"}, minVersion = "5.0", features = {
 *                     "async", "http2"})
 *     public interface IHttpService {
 *     }
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Library Hints
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AILibraryHint.java}
 * blob {@code e3c1a81509a9cee4468fa122f673dd434a3b0fc0} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PACKAGE})
@Retention(RetentionPolicy.SOURCE)
@Documented
@Repeatable(AILibraryHints.class)
public @interface AILibraryHint {

    /**
     * Library category.
     *
     * @return category
     */
    Category category() default Category.GENERAL;

    /**
     * Preferred libraries (Maven coordinates: groupId:artifactId).
     *
     * @return preferred libraries
     */
    String[] preferred() default {};

    /**
     * Libraries to avoid.
     *
     * @return avoided libraries
     */
    String[] avoid() default {};

    /**
     * Specific classes/methods to use from the library.
     *
     * @return usage guidance
     */
    String use() default "";

    /**
     * Minimum version required.
     *
     * @return min version
     */
    String minVersion() default "";

    /**
     * Maximum version allowed.
     *
     * @return max version
     */
    String maxVersion() default "";

    /**
     * Required features from the library.
     *
     * @return required features
     */
    String[] features() default {};

    /**
     * Reason for library choice.
     *
     * @return reason
     */
    String reason() default "";

    /**
     * Alternative if preferred not available.
     *
     * @return fallback libraries
     */
    String[] fallback() default {};

    /**
     * License compatibility requirement.
     *
     * @return license
     */
    License license() default License.ANY_OPEN_SOURCE;

    // --- LIBRARY CATEGORIES ---

    /** Library categories covering common Java ecosystem needs. */
    enum Category {
        /** General purpose. */
        GENERAL,

        // --- CORE UTILITIES ---

        /**  */
        COLLECTIONS,

        /**  */
        STRING_UTILS,

        /**  */
        IO_UTILS,

        /**  */
        CONCURRENCY,

        /**  */
        DATE_TIME,

        /**  */
        MATH,

        /**  */
        REFLECTION,

        // --- SERIALIZATION ---

        /**  */
        JSON,

        /**  */
        XML,

        /**  */
        YAML,

        /**  */
        CSV,

        /**  */
        BINARY_SERIALIZATION,

        // --- NETWORKING ---

        /**  */
        HTTP_CLIENT,

        /**  */
        REST_CLIENT,

        /**  */
        WEBSOCKET,

        /**  */
        GRPC,

        /**  */
        GRAPHQL,

        // --- DATABASE ---

        /**  */
        JDBC,

        /**  */
        CONNECTION_POOL,

        /**  */
        ORM,

        /**  */
        SQL_BUILDER,

        /**  */
        NOSQL,

        // --- CACHING ---

        /**  */
        CACHE_LOCAL,

        /**  */
        CACHE_DISTRIBUTED,

        // --- MESSAGING ---

        /**  */
        MESSAGING,

        /**  */
        EVENT_BUS,

        // --- VALIDATION ---

        /**  */
        VALIDATION,

        /**  */
        SCHEMA_VALIDATION,

        // --- SECURITY ---

        /**  */
        CRYPTO,

        /**  */
        HASHING,

        /**  */
        JWT,

        /**  */
        OAUTH,

        // --- LOGGING & MONITORING ---

        /**  */
        LOGGING,

        /**  */
        METRICS,

        /**  */
        TRACING,

        // --- TESTING ---

        /**  */
        UNIT_TESTING,

        /**  */
        MOCKING,

        /**  */
        ASSERTIONS,

        /**  */
        TEST_CONTAINERS,

        /**  */
        PROPERTY_TESTING,

        // --- CODE GENERATION ---

        /**  */
        BOILERPLATE,

        /**  */
        MAPPING,

        /**  */
        ANNOTATION_PROCESSING,

        // --- FUNCTIONAL ---

        /**  */
        FUNCTIONAL,

        /**  */
        REACTIVE,

        // --- NLP / AI ---

        /**  */
        NLP,

        /**  */
        LLM,

        /**  */
        VECTOR_STORE,

        // --- FILE FORMATS ---

        /**  */
        EXCEL,

        /**  */
        PDF,

        /**  */
        IMAGE,

        /**  */
        ARCHIVE,

        // --- CONFIGURATION ---

        /**  */
        CONFIG,

        /**  */
        FEATURE_FLAGS,

        // --- SCHEDULING ---

        /**  */
        SCHEDULING,

        /**  */
        WORKFLOW,

        // --- CLI ---

        /**  */
        CLI,

        /**  */
        TERMINAL,

        // --- REGEX ---

        /**  */
        REGEX
    }

    // --- LICENSE ---

    /** License compatibility. */
    enum License {
        /** Any open source license. */
        ANY_OPEN_SOURCE,

        /** Apache 2.0 compatible. */
        APACHE_2,

        /** MIT compatible. */
        MIT,

        /** BSD compatible. */
        BSD,

        /** LGPL compatible. */
        LGPL,

        /** GPL compatible (copyleft). */
        GPL,

        /** Commercial license OK. */
        COMMERCIAL_OK,

        /** Public domain. */
        PUBLIC_DOMAIN
    }
}
