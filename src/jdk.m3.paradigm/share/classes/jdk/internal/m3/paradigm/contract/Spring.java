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
 * Spring Framework Patterns.
 *
 * <p>
 * Design patterns used in Spring Framework for building enterprise
 * applications. These patterns facilitate modular, scalable, and maintainable
 * Java applications.
 *
 * @see <a href=
 *      "https://www.geeksforgeeks.org/system-design/design-patterns-used-in-spring-framework/">Spring
 *      Design Patterns</a>
 * @m3.pattern synexia:iop
 * @m3.algorithm Declare Spring-related vocabulary with category and description labels; these tokens do not instantiate a container, authorize operations or register every token in PatternCatalog.
 * @m3.dataStructures Spring enum singleton array in declaration order with final label fields.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/Spring.java}
 * blob {@code 6b1ff6dfe28884b5a7760c5abccd106f85673858} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public enum Spring {

    // --- CORE SPRING PATTERNS ---
    /** Dependency Injection - receive dependencies from external source.
     * @m3.pattern synexia:iop
     * @m3.algorithm Identify Spring#DEPENDENCY_INJECTION with category equal to Core; description equal to Object receives dependencies from external source; preserve this authored label without executing or enforcing its claimed rule.
     * @m3.dataStructures One Spring enum singleton with its exact constructor literals.
     */
    DEPENDENCY_INJECTION("Core", "Object receives dependencies from external source"),
    /** Inversion of Control - framework controls object lifecycle.
     * @m3.pattern synexia:iop
     * @m3.algorithm Identify Spring#INVERSION_OF_CONTROL with category equal to Core; description equal to Framework controls object creation and lifecycle; preserve this authored label without executing or enforcing its claimed rule.
     * @m3.dataStructures One Spring enum singleton with its exact constructor literals.
     */
    INVERSION_OF_CONTROL("Core", "Framework controls object creation and lifecycle"),
    /** Bean Factory - creates and manages bean instances.
     * @m3.pattern synexia:iop
     * @m3.algorithm Identify Spring#BEAN_FACTORY with category equal to Core; description equal to Factory that creates and manages bean instances; preserve this authored label without executing or enforcing its claimed rule.
     * @m3.dataStructures One Spring enum singleton with its exact constructor literals.
     */
    BEAN_FACTORY("Core", "Factory that creates and manages bean instances"),
    /** Application Context - advanced container with enterprise features.
     * @m3.pattern synexia:iop
     * @m3.algorithm Identify Spring#APPLICATION_CONTEXT with category equal to Core; description equal to Advanced container with enterprise services; preserve this authored label without executing or enforcing its claimed rule.
     * @m3.dataStructures One Spring enum singleton with its exact constructor literals.
     */
    APPLICATION_CONTEXT("Core", "Advanced container with enterprise services"),

    // --- CREATIONAL PATTERNS ---
    /** Singleton Scope - one instance per application context.
     * @m3.pattern synexia:iop
     * @m3.algorithm Identify Spring#SINGLETON_SCOPE with category equal to Scope; description equal to Single bean instance per application context; preserve this authored label without executing or enforcing its claimed rule.
     * @m3.dataStructures One Spring enum singleton with its exact constructor literals.
     */
    SINGLETON_SCOPE("Scope", "Single bean instance per application context"),
    /** Prototype Scope - new instance on each request.
     * @m3.pattern synexia:iop
     * @m3.algorithm Identify Spring#PROTOTYPE_SCOPE with category equal to Scope; description equal to New bean instance on each injection; preserve this authored label without executing or enforcing its claimed rule.
     * @m3.dataStructures One Spring enum singleton with its exact constructor literals.
     */
    PROTOTYPE_SCOPE("Scope", "New bean instance on each injection"),
    /** Request Scope - one instance per HTTP request.
     * @m3.pattern synexia:iop
     * @m3.algorithm Identify Spring#REQUEST_SCOPE with category equal to Scope; description equal to Bean instance scoped to HTTP request; preserve this authored label without executing or enforcing its claimed rule.
     * @m3.dataStructures One Spring enum singleton with its exact constructor literals.
     */
    REQUEST_SCOPE("Scope", "Bean instance scoped to HTTP request"),
    /** Session Scope - one instance per HTTP session.
     * @m3.pattern synexia:iop
     * @m3.algorithm Identify Spring#SESSION_SCOPE with category equal to Scope; description equal to Bean instance scoped to HTTP session; preserve this authored label without executing or enforcing its claimed rule.
     * @m3.dataStructures One Spring enum singleton with its exact constructor literals.
     */
    SESSION_SCOPE("Scope", "Bean instance scoped to HTTP session"),
    /** Factory Bean - programmatic bean creation.
     * @m3.pattern synexia:iop
     * @m3.algorithm Identify Spring#FACTORY_BEAN with category equal to Creational; description equal to Custom factory for complex bean creation; preserve this authored label without executing or enforcing its claimed rule.
     * @m3.dataStructures One Spring enum singleton with its exact constructor literals.
     */
    FACTORY_BEAN("Creational", "Custom factory for complex bean creation"),

    // --- STRUCTURAL PATTERNS ---
    /** Proxy PatternKind - AOP proxies for method interception.
     * @m3.pattern synexia:iop
     * @m3.algorithm Identify Spring#PROXY with category equal to Structural; description equal to Surrogate object for AOP and transactions; preserve this authored label without executing or enforcing its claimed rule.
     * @m3.dataStructures One Spring enum singleton with its exact constructor literals.
     */
    PROXY("Structural", "Surrogate object for AOP and transactions"),
    /** Decorator - add behavior via proxies.
     * @m3.pattern synexia:iop
     * @m3.algorithm Identify Spring#DECORATOR with category equal to Structural; description equal to Add behavior dynamically via AOP; preserve this authored label without executing or enforcing its claimed rule.
     * @m3.dataStructures One Spring enum singleton with its exact constructor literals.
     */
    DECORATOR("Structural", "Add behavior dynamically via AOP"),
    /** Adapter - integrate different APIs.
     * @m3.pattern synexia:iop
     * @m3.algorithm Identify Spring#ADAPTER with category equal to Structural; description equal to Adapt interfaces for integration; preserve this authored label without executing or enforcing its claimed rule.
     * @m3.dataStructures One Spring enum singleton with its exact constructor literals.
     */
    ADAPTER("Structural", "Adapt interfaces for integration"),
    /** Facade - simplified interface to complex subsystems.
     * @m3.pattern synexia:iop
     * @m3.algorithm Identify Spring#FACADE with category equal to Structural; description equal to Simplified interface to Spring subsystems; preserve this authored label without executing or enforcing its claimed rule.
     * @m3.dataStructures One Spring enum singleton with its exact constructor literals.
     */
    FACADE("Structural", "Simplified interface to Spring subsystems"),
    /** Composite - tree structures of beans.
     * @m3.pattern synexia:iop
     * @m3.algorithm Identify Spring#COMPOSITE with category equal to Structural; description equal to Compose beans into tree structures; preserve this authored label without executing or enforcing its claimed rule.
     * @m3.dataStructures One Spring enum singleton with its exact constructor literals.
     */
    COMPOSITE("Structural", "Compose beans into tree structures"),

    // --- BEHAVIORAL PATTERNS ---
    /** Template Method - JdbcTemplate, RestTemplate, etc.
     * @m3.pattern synexia:iop
     * @m3.algorithm Identify Spring#TEMPLATE_METHOD with category equal to Behavioral; description equal to Algorithm skeleton with customizable steps; preserve this authored label without executing or enforcing its claimed rule.
     * @m3.dataStructures One Spring enum singleton with its exact constructor literals.
     */
    TEMPLATE_METHOD("Behavioral", "Algorithm skeleton with customizable steps"),
    /** Strategy - pluggable algorithms via interfaces.
     * @m3.pattern synexia:iop
     * @m3.algorithm Identify Spring#STRATEGY with category equal to Behavioral; description equal to Interchangeable algorithms via interfaces; preserve this authored label without executing or enforcing its claimed rule.
     * @m3.dataStructures One Spring enum singleton with its exact constructor literals.
     */
    STRATEGY("Behavioral", "Interchangeable algorithms via interfaces"),
    /** Observer - ApplicationEvent and listeners.
     * @m3.pattern synexia:iop
     * @m3.algorithm Identify Spring#OBSERVER with category equal to Behavioral; description equal to Event publishing and listening; preserve this authored label without executing or enforcing its claimed rule.
     * @m3.dataStructures One Spring enum singleton with its exact constructor literals.
     */
    OBSERVER("Behavioral", "Event publishing and listening"),
    /** Command - encapsulate requests as objects.
     * @m3.pattern synexia:iop
     * @m3.algorithm Identify Spring#COMMAND with category equal to Behavioral; description equal to Encapsulate operations as command objects; preserve this authored label without executing or enforcing its claimed rule.
     * @m3.dataStructures One Spring enum singleton with its exact constructor literals.
     */
    COMMAND("Behavioral", "Encapsulate operations as command objects"),

    // --- DATA ACCESS PATTERNS ---
    /** Repository - data access abstraction.
     * @m3.pattern synexia:iop
     * @m3.algorithm Identify Spring#REPOSITORY with category equal to Data; description equal to Abstraction for data access operations; preserve this authored label without executing or enforcing its claimed rule.
     * @m3.dataStructures One Spring enum singleton with its exact constructor literals.
     */
    REPOSITORY("Data", "Abstraction for data access operations"),
    /** DAO - Data Access Object pattern.
     * @m3.pattern synexia:iop
     * @m3.algorithm Identify Spring#DAO with category equal to Data; description equal to Separates persistence logic from business logic; preserve this authored label without executing or enforcing its claimed rule.
     * @m3.dataStructures One Spring enum singleton with its exact constructor literals.
     */
    DAO("Data", "Separates persistence logic from business logic"),
    /** Unit of Work - transaction management.
     * @m3.pattern synexia:iop
     * @m3.algorithm Identify Spring#UNIT_OF_WORK with category equal to Data; description equal to Manages transactional boundaries; preserve this authored label without executing or enforcing its claimed rule.
     * @m3.dataStructures One Spring enum singleton with its exact constructor literals.
     */
    UNIT_OF_WORK("Data", "Manages transactional boundaries"),
    /** Active Record - entities with persistence methods.
     * @m3.pattern synexia:iop
     * @m3.algorithm Identify Spring#ACTIVE_RECORD with category equal to Data; description equal to Domain objects with built-in persistence; preserve this authored label without executing or enforcing its claimed rule.
     * @m3.dataStructures One Spring enum singleton with its exact constructor literals.
     */
    ACTIVE_RECORD("Data", "Domain objects with built-in persistence"),

    // --- WEB PATTERNS ---
    /** Front Controller - DispatcherServlet.
     * @m3.pattern synexia:iop
     * @m3.algorithm Identify Spring#FRONT_CONTROLLER with category equal to Web; description equal to Central entry point for web requests; preserve this authored label without executing or enforcing its claimed rule.
     * @m3.dataStructures One Spring enum singleton with its exact constructor literals.
     */
    FRONT_CONTROLLER("Web", "Central entry point for web requests"),
    /** MVC - Model-View-Controller separation.
     * @m3.pattern synexia:iop
     * @m3.algorithm Identify Spring#MVC with category equal to Web; description equal to Separates concerns in web applications; preserve this authored label without executing or enforcing its claimed rule.
     * @m3.dataStructures One Spring enum singleton with its exact constructor literals.
     */
    MVC("Web", "Separates concerns in web applications"),
    /** View Resolver - view technology abstraction.
     * @m3.pattern synexia:iop
     * @m3.algorithm Identify Spring#VIEW_RESOLVER with category equal to Web; description equal to Resolves logical view names to actual views; preserve this authored label without executing or enforcing its claimed rule.
     * @m3.dataStructures One Spring enum singleton with its exact constructor literals.
     */
    VIEW_RESOLVER("Web", "Resolves logical view names to actual views"),
    /** Handler Mapping - URL to controller mapping.
     * @m3.pattern synexia:iop
     * @m3.algorithm Identify Spring#HANDLER_MAPPING with category equal to Web; description equal to Maps requests to appropriate handlers; preserve this authored label without executing or enforcing its claimed rule.
     * @m3.dataStructures One Spring enum singleton with its exact constructor literals.
     */
    HANDLER_MAPPING("Web", "Maps requests to appropriate handlers"),

    // --- ENTERPRISE PATTERNS ---
    /** Service Locator - lookup services (discouraged).
     * @m3.pattern synexia:iop
     * @m3.algorithm Identify Spring#SERVICE_LOCATOR with category equal to Enterprise; description equal to Lookup services from registry (use DI instead); preserve this authored label without executing or enforcing its claimed rule.
     * @m3.dataStructures One Spring enum singleton with its exact constructor literals.
     */
    SERVICE_LOCATOR("Enterprise", "Lookup services from registry (use DI instead)"),
    /** Business Delegate - decouple presentation from business.
     * @m3.pattern synexia:iop
     * @m3.algorithm Identify Spring#BUSINESS_DELEGATE with category equal to Enterprise; description equal to Decouples presentation from business tier; preserve this authored label without executing or enforcing its claimed rule.
     * @m3.dataStructures One Spring enum singleton with its exact constructor literals.
     */
    BUSINESS_DELEGATE("Enterprise", "Decouples presentation from business tier"),
    /** Transfer Object - DTO for data transfer.
     * @m3.pattern synexia:iop
     * @m3.algorithm Identify Spring#TRANSFER_OBJECT with category equal to Enterprise; description equal to Data Transfer Object for cross-layer data; preserve this authored label without executing or enforcing its claimed rule.
     * @m3.dataStructures One Spring enum singleton with its exact constructor literals.
     */
    TRANSFER_OBJECT("Enterprise", "Data Transfer Object for cross-layer data");

    /**
     * Store Spring.category once from the matching constructor argument without validation or transformation.
     * @m3.pattern synexia:iop
     * @m3.algorithm Store Spring.category once from the matching constructor argument without validation or transformation.
     * @m3.dataStructures Final String reference for the category label.
     */
    private final String category;
    /**
     * Store Spring.description once from the matching constructor argument without validation or transformation.
     * @m3.pattern synexia:iop
     * @m3.algorithm Store Spring.description once from the matching constructor argument without validation or transformation.
     * @m3.dataStructures Final String reference for the description label.
     */
    private final String description;

    /**
     * Initialize Spring by directly assigning each constructor argument to its matching final field, preserving label references and any explicit order integer.
     * @m3.pattern synexia:iop
     * @m3.algorithm Initialize Spring by directly assigning each constructor argument to its matching final field, preserving label references and any explicit order integer.
     * @m3.dataStructures Constructor arguments and final Spring instance fields.
     */
    Spring(String category, String description) {
        this.category = category;
        this.description = description;
    }

    /** PatternKind category.
     * @m3.pattern synexia:iop
     * @m3.algorithm Return this Spring singleton's stored category value unchanged.
     * @m3.dataStructures Existing final category String reference; no allocation or transformation.
     */
    public String category() {
        return category;
    }

    /** PatternKind description.
     * @m3.pattern synexia:iop
     * @m3.algorithm Return this Spring singleton's stored description value unchanged.
     * @m3.dataStructures Existing final description String reference; no allocation or transformation.
     */
    public String description() {
        return description;
    }
}
