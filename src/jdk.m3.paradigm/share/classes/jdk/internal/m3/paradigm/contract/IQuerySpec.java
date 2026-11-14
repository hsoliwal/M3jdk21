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

import jdk.internal.m3.paradigm.annotation.Model;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * IQuerySpec - Query specification for AI code generation.
 *
 * <h2>Inspired By Query Languages</h2>
 *
 * <pre>
 * Query languages humans created:
 *   - SQL (Structured Query Language)
 *   - JPQL/HQL (JPA/Hibernate queries)
 *   - GraphQL (Graph query language)
 *   - MongoDB Query Language
 *   - Elasticsearch Query DSL
 *   - SPARQL (RDF queries)
 *   - Cypher (Neo4j graph queries)
 *   - Lucene Query Syntax
 *
 * &#64;IQuerySpec specifies query behavior in IOP style.
 * </pre>
 *
 * <h2>Generated Code PatternKind</h2>
 *
 * <pre>{@code
 * // From: @IQuerySpec(select = "u", from = "User u", where = "u.age > :minAge")
 * // Generates:
 *
 * &#64;Query("SELECT u FROM User u WHERE u.age > :minAge")
 * List&lt;User> findByMinAge(@Param("minAge") int minAge);
 * }</pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * interface IUserRepository {
 *
 *     // Simple query
 *     &#64;IQuerySpec(
 *         select = "u",
 *         from = "User u",
 *         where = "u.email = :email"
 *     )
 *     Optional&lt;User> findByEmail(String email);
 *
 *     // Complex query with join
 *     &#64;IQuerySpec(
 *         select = "u",
 *         from = "User u JOIN u.orders o",
 *         where = "o.total > :minTotal",
 *         orderBy = "u.name ASC"
 *     )
 *     List&lt;User> findUsersWithLargeOrders(BigDecimal minTotal);
 *
 *     // Native SQL
 *     &#64;IQuerySpec(
 *         native = true,
 *         value = "SELECT * FROM users WHERE created_at > :since"
 *     )
 *     List&lt;User> findRecentUsers(Instant since);
 *
 *     // Projection
 *     &#64;IQuerySpec(
 *         select = "new UserSummary(u.id, u.name, COUNT(o))",
 *         from = "User u LEFT JOIN u.orders o",
 *         groupBy = "u.id, u.name"
 *     )
 *     List&lt;UserSummary> getUserSummaries();
 * }
 * }</pre>
 *
 * @since IOP 1.0 - Query Specification
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares complete or structured query text, joins, pagination, locking and persistence-context hints; it executes no query, validates no SQL and acquires no lock.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.METHOD}; existing element and enum schemas are preserved.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IQuerySpec.java}
 * blob {@code 00ba677040d3ed4969bddb042bb61152478e47b3} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IQuerySpec {

    // --- FULL QUERY ---

    /**
     * Complete query string (alternative to structured parts).
     *
     * @return full query
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IQuerySpec#value intent: Complete query string (alternative to structured parts). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String value() default "";

    /**
     * Whether this is a native SQL query.
     *
     * @return true for native SQL
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IQuerySpec#nativeQuery intent: Whether this is a native SQL query. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean nativeQuery() default false;

    // --- STRUCTURED QUERY PARTS ---

    /**
     * SELECT clause content.
     *
     * @return select expression
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IQuerySpec#select intent: SELECT clause content. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String select() default "";

    /**
     * FROM clause with entity and alias.
     *
     * @return from clause
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IQuerySpec#from intent: FROM clause with entity and alias. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String from() default "";

    /**
     * WHERE clause conditions.
     *
     * @return where conditions
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IQuerySpec#where intent: WHERE clause conditions. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String where() default "";

    /**
     * ORDER BY clause.
     *
     * @return order by expression
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IQuerySpec#orderBy intent: ORDER BY clause. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String orderBy() default "";

    /**
     * GROUP BY clause.
     *
     * @return group by expression
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IQuerySpec#groupBy intent: GROUP BY clause. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String groupBy() default "";

    /**
     * HAVING clause.
     *
     * @return having conditions
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IQuerySpec#having intent: HAVING clause. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String having() default "";

    // --- JOINS ---

    /**
     * JOIN clauses.
     *
     * @return join definitions
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IQuerySpec#joins intent: JOIN clauses. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Join[]}; default expression {@code {}}.
     */
    Join[] joins() default {};

    /** Join definition.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares a join target, category, alias and condition string; it executes no join and has implicit CLASS retention with no explicit Target restriction.
     * @m3.dataStructures CLASS (implicit default)-retained annotation targeting {@code all declaration contexts (no explicit Target)}; existing element and enum schemas are preserved.
     */
    @interface Join {

        /** Join type.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IQuerySpec.Join#type intent: Join type. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code JoinType}; default expression {@code JoinType.INNER}.
         */
        JoinType type() default JoinType.INNER;

        /** Join target (entity.field).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IQuerySpec.Join#target intent: Join target (entity.field). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; an explicit value is required.
         */
        String target();

        /** Alias for joined entity.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IQuerySpec.Join#alias intent: Alias for joined entity. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
         */
        String alias() default "";

        /** ON condition (for non-mapped joins).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IQuerySpec.Join#on intent: ON condition (for non-mapped joins). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
         */
        String on() default "";
    }

    /** Join types.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IQuerySpec.JoinType choices described as Join types. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 6 constants in the existing source order.
     */
    enum JoinType {
        /** INNER JOIN.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IQuerySpec.JoinType#INNER policy choice: INNER JOIN. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IQuerySpec.JoinType; its declared order and identity are preserved.
         */
        INNER,

        /** LEFT OUTER JOIN.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IQuerySpec.JoinType#LEFT policy choice: LEFT OUTER JOIN. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IQuerySpec.JoinType; its declared order and identity are preserved.
         */
        LEFT,

        /** RIGHT OUTER JOIN.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IQuerySpec.JoinType#RIGHT policy choice: RIGHT OUTER JOIN. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IQuerySpec.JoinType; its declared order and identity are preserved.
         */
        RIGHT,

        /** CROSS JOIN.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IQuerySpec.JoinType#CROSS policy choice: CROSS JOIN. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IQuerySpec.JoinType; its declared order and identity are preserved.
         */
        CROSS,

        /** FETCH JOIN (eager loading).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IQuerySpec.JoinType#FETCH policy choice: FETCH JOIN (eager loading). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IQuerySpec.JoinType; its declared order and identity are preserved.
         */
        FETCH,

        /** LEFT FETCH JOIN.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IQuerySpec.JoinType#LEFT_FETCH policy choice: LEFT FETCH JOIN. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IQuerySpec.JoinType; its declared order and identity are preserved.
         */
        LEFT_FETCH
    }

    // --- PAGINATION ---

    /**
     * Whether to support pagination.
     *
     * @return true for paginated
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IQuerySpec#pageable intent: Whether to support pagination. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean pageable() default false;

    /**
     * Default page size.
     *
     * @return page size
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IQuerySpec#pageSize intent: Default page size. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 20}.
     */
    int pageSize() default 20;

    /**
     * Maximum page size allowed.
     *
     * @return max page size
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IQuerySpec#maxPageSize intent: Maximum page size allowed. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 100}.
     */
    int maxPageSize() default 100;

    // --- CACHING ---

    /**
     * Whether query results should be cached.
     *
     * @return true for cached
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IQuerySpec#cacheable intent: Whether query results should be cached. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean cacheable() default false;

    /**
     * Cache region name.
     *
     * @return cache region
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IQuerySpec#cacheRegion intent: Cache region name. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String cacheRegion() default "";

    // --- MODIFICATION QUERIES ---

    /**
     * Whether this is a modifying query (UPDATE/DELETE).
     *
     * @return true for modification
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IQuerySpec#modifying intent: Whether this is a modifying query (UPDATE/DELETE). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean modifying() default false;

    /**
     * Whether to clear persistence context after modifying query.
     *
     * @return true to clear
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IQuerySpec#clearAutomatically intent: Whether to clear persistence context after modifying query. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean clearAutomatically() default false;

    /**
     * Whether to flush before executing modifying query.
     *
     * @return true to flush
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IQuerySpec#flushAutomatically intent: Whether to flush before executing modifying query. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean flushAutomatically() default false;

    // --- LOCKING ---

    /**
     * Lock mode for the query.
     *
     * @return lock mode
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IQuerySpec#lock intent: Lock mode for the query. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code LockMode}; default expression {@code LockMode.NONE}.
     */
    LockMode lock() default LockMode.NONE;

    /** Lock modes.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IQuerySpec.LockMode choices described as Lock modes. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 8 constants in the existing source order.
     */
    enum LockMode {
        /** No locking.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IQuerySpec.LockMode#NONE policy choice: No locking. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IQuerySpec.LockMode; its declared order and identity are preserved.
         */
        NONE,

        /** Read lock (shared).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IQuerySpec.LockMode#READ policy choice: Read lock (shared). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IQuerySpec.LockMode; its declared order and identity are preserved.
         */
        READ,

        /** Write lock (exclusive).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IQuerySpec.LockMode#WRITE policy choice: Write lock (exclusive). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IQuerySpec.LockMode; its declared order and identity are preserved.
         */
        WRITE,

        /** Optimistic read lock.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IQuerySpec.LockMode#OPTIMISTIC policy choice: Optimistic read lock. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IQuerySpec.LockMode; its declared order and identity are preserved.
         */
        OPTIMISTIC,

        /** Optimistic force increment.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IQuerySpec.LockMode#OPTIMISTIC_FORCE_INCREMENT policy choice: Optimistic force increment. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IQuerySpec.LockMode; its declared order and identity are preserved.
         */
        OPTIMISTIC_FORCE_INCREMENT,

        /** Pessimistic read lock.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IQuerySpec.LockMode#PESSIMISTIC_READ policy choice: Pessimistic read lock. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IQuerySpec.LockMode; its declared order and identity are preserved.
         */
        PESSIMISTIC_READ,

        /** Pessimistic write lock.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IQuerySpec.LockMode#PESSIMISTIC_WRITE policy choice: Pessimistic write lock. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IQuerySpec.LockMode; its declared order and identity are preserved.
         */
        PESSIMISTIC_WRITE,

        /** Pessimistic force increment.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IQuerySpec.LockMode#PESSIMISTIC_FORCE_INCREMENT policy choice: Pessimistic force increment. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IQuerySpec.LockMode; its declared order and identity are preserved.
         */
        PESSIMISTIC_FORCE_INCREMENT
    }

    // --- TIMEOUT ---

    /**
     * Query timeout in milliseconds. 0 = no timeout.
     *
     * @return timeout in ms
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IQuerySpec#timeoutMs intent: Query timeout in milliseconds. 0 = no timeout. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 0}.
     */
    int timeoutMs() default 0;

    // --- MODEL ---

    /**
     * Preferred model for generation.
     *
     * @return model type
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IQuerySpec#model intent: Preferred model for generation. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Model.ModelType}; default expression {@code Model.ModelType.HAIKU}.
     */
    Model.ModelType model() default Model.ModelType.HAIKU;
}
