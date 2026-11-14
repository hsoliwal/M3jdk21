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
 * IApiSpec - REST/HTTP API specification for AI code generation.
 *
 * <h2>Inspired By Human IDLs</h2>
 *
 * <pre>
 * API specification languages humans invented:
 *   - OpenAPI/Swagger (REST)
 *   - RAML (RESTful API Modeling)
 *   - API Blueprint
 *   - WSDL (SOAP)
 *   - GraphQL Schema
 *   - gRPC/Protobuf
 *
 * &#64;IApiSpec unifies REST API specification into IOP.
 * </pre>
 *
 * <h2>Generated Code PatternKind</h2>
 *
 * <pre>{@code
 * // From: @IApiSpec(path = "/users/{id}", method = GET, produces = JSON)
 * // Generates:
 *
 * &#64;GetMapping(path = "/users/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
 * &#64;Operation(summary = "Get user by ID", tags = {"Users"})
 * &#64;ApiResponse(responseCode = "200", description = "User found")
 * &#64;ApiResponse(responseCode = "404", description = "User not found")
 * public ResponseEntity&lt;User> getUser(@PathVariable @Parameter(description = "User ID") String id) {
 * 	return userService.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
 * }
 * }
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * interface IUserApi {
 *
 *     &#64;IApiSpec(
 *         path = "/users",
 *         method = HttpMethod.POST,
 *         consumes = MediaType.JSON,
 *         produces = MediaType.JSON,
 *         summary = "Create a new user",
 *         requestBody = "User creation request",
 *         responses = {
 *             &#64;Response(code = 201, description = "User created"),
 *             &#64;Response(code = 400, description = "Invalid input"),
 * &#64;Response(code = 409, description = "User already exists")}, security = @Security(scheme = "bearer", scopes = {
 * "users:write"}))
 * User createUser(CreateUserRequest request);
 * }
 * }
 * </pre>
 *
 * @since IOP 1.0 - API Specification
 * @see IEventSpec
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares an HTTP endpoint schema, content types, response/security descriptions, throttling and validation preferences; it registers no route and performs no authentication or request validation.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.METHOD}; existing element and enum schemas are preserved.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IApiSpec.java}
 * blob {@code d2899328d1277fb8fb826be1afef1f2b7a6cc9dc} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IApiSpec {

    // --- ENDPOINT ---

    /**
     * URL path for the endpoint. Supports path variables: /users/{id}
     *
     * @return endpoint path
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IApiSpec#path intent: URL path for the endpoint. Supports path variables: /users/{id} Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; an explicit value is required.
     */
    String path();

    /**
     * HTTP method.
     *
     * @return HTTP method
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IApiSpec#method intent: HTTP method. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code HttpMethod}; default expression {@code HttpMethod.GET}.
     */
    HttpMethod method() default HttpMethod.GET;

    /** HTTP methods.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IApiSpec.HttpMethod choices described as HTTP methods. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 7 constants in the existing source order.
     */
    enum HttpMethod {
        /** HTTP GET - Read resource.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IApiSpec.HttpMethod#GET policy choice: HTTP GET - Read resource. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IApiSpec.HttpMethod; its declared order and identity are preserved.
         */
        GET,
        /** HTTP POST - Create resource.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IApiSpec.HttpMethod#POST policy choice: HTTP POST - Create resource. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IApiSpec.HttpMethod; its declared order and identity are preserved.
         */
        POST,
        /** HTTP PUT - Replace resource.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IApiSpec.HttpMethod#PUT policy choice: HTTP PUT - Replace resource. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IApiSpec.HttpMethod; its declared order and identity are preserved.
         */
        PUT,
        /** HTTP PATCH - Partial update.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IApiSpec.HttpMethod#PATCH policy choice: HTTP PATCH - Partial update. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IApiSpec.HttpMethod; its declared order and identity are preserved.
         */
        PATCH,
        /** HTTP DELETE - Remove resource.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IApiSpec.HttpMethod#DELETE policy choice: HTTP DELETE - Remove resource. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IApiSpec.HttpMethod; its declared order and identity are preserved.
         */
        DELETE,
        /** HTTP HEAD - Headers only.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IApiSpec.HttpMethod#HEAD policy choice: HTTP HEAD - Headers only. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IApiSpec.HttpMethod; its declared order and identity are preserved.
         */
        HEAD,
        /** HTTP OPTIONS - Allowed methods.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IApiSpec.HttpMethod#OPTIONS policy choice: HTTP OPTIONS - Allowed methods. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IApiSpec.HttpMethod; its declared order and identity are preserved.
         */
        OPTIONS
    }

    // --- CONTENT TYPES ---

    /**
     * Request content types accepted.
     *
     * @return media types
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IApiSpec#consumes intent: Request content types accepted. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code MediaType[]}; default expression {@code {MediaType.JSON}}.
     */
    MediaType[] consumes() default {MediaType.JSON};

    /**
     * Response content types produced.
     *
     * @return media types
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IApiSpec#produces intent: Response content types produced. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code MediaType[]}; default expression {@code {MediaType.JSON}}.
     */
    MediaType[] produces() default {MediaType.JSON};

    /** Media types for content negotiation.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IApiSpec.MediaType choices described as Media types for content negotiation. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 7 constants in the existing source order.
     */
    enum MediaType {
        /** application/json
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IApiSpec.MediaType#JSON policy choice: application/json This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IApiSpec.MediaType; its declared order and identity are preserved.
         */
        JSON,
        /** application/xml
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IApiSpec.MediaType#XML policy choice: application/xml This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IApiSpec.MediaType; its declared order and identity are preserved.
         */
        XML,
        /** text/plain
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IApiSpec.MediaType#TEXT policy choice: text/plain This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IApiSpec.MediaType; its declared order and identity are preserved.
         */
        TEXT,
        /** text/html
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IApiSpec.MediaType#HTML policy choice: text/html This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IApiSpec.MediaType; its declared order and identity are preserved.
         */
        HTML,
        /** application/octet-stream
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IApiSpec.MediaType#BINARY policy choice: application/octet-stream This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IApiSpec.MediaType; its declared order and identity are preserved.
         */
        BINARY,
        /** multipart/form-data
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IApiSpec.MediaType#MULTIPART policy choice: multipart/form-data This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IApiSpec.MediaType; its declared order and identity are preserved.
         */
        MULTIPART,
        /** application/x-www-form-urlencoded
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IApiSpec.MediaType#FORM policy choice: application/x-www-form-urlencoded This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IApiSpec.MediaType; its declared order and identity are preserved.
         */
        FORM
    }

    // --- DOCUMENTATION ---

    /**
     * Short summary of the operation.
     *
     * @return summary
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IApiSpec#summary intent: Short summary of the operation. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String summary() default "";

    /**
     * Detailed description of the operation.
     *
     * @return description
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IApiSpec#description intent: Detailed description of the operation. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String description() default "";

    /**
     * Operation tags for grouping.
     *
     * @return tags
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IApiSpec#tags intent: Operation tags for grouping. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] tags() default {};

    /**
     * Unique operation ID.
     *
     * @return operation ID
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IApiSpec#operationId intent: Unique operation ID. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String operationId() default "";

    // --- REQUEST ---

    /**
     * Request body description.
     *
     * @return request body description
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IApiSpec#requestBody intent: Request body description. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String requestBody() default "";

    /**
     * Whether request body is required.
     *
     * @return true if required
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IApiSpec#requestRequired intent: Whether request body is required. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
     */
    boolean requestRequired() default true;

    // --- RESPONSES ---

    /**
     * API responses.
     *
     * @return response definitions
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IApiSpec#responses intent: API responses. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Response[]}; default expression {@code {}}.
     */
    Response[] responses() default {};

    /** Response definition.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares response status, description, media token and schema class; it serializes no response and retains implicit CLASS retention without explicit Target restriction.
     * @m3.dataStructures CLASS (implicit default)-retained annotation targeting {@code all declaration contexts (no explicit Target)}; existing element and enum schemas are preserved.
     */
    @interface Response {

        /** HTTP status code.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IApiSpec.Response#code intent: HTTP status code. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code int}; an explicit value is required.
         */
        int code();

        /** Response description.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IApiSpec.Response#description intent: Response description. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; an explicit value is required.
         */
        String description();

        /** Response content type.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IApiSpec.Response#contentType intent: Response content type. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code MediaType}; default expression {@code MediaType.JSON}.
         */
        MediaType contentType() default MediaType.JSON;

        /** Response schema class.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IApiSpec.Response#schema intent: Response schema class. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code Class<?>}; default expression {@code Void.class}.
         */
        Class<?> schema() default Void.class;
    }

    // --- SECURITY ---

    /**
     * Security requirements.
     *
     * @return security definition
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IApiSpec#security intent: Security requirements. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Security}; default expression {@code @Security}.
     */
    Security security() default @Security;

    /** Security definition.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares a security scheme name, scope strings and type token; it authenticates no request and retains implicit CLASS retention without explicit Target restriction.
     * @m3.dataStructures CLASS (implicit default)-retained annotation targeting {@code all declaration contexts (no explicit Target)}; existing element and enum schemas are preserved.
     */
    @interface Security {

        /** Security scheme name.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IApiSpec.Security#scheme intent: Security scheme name. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
         */
        String scheme() default "";

        /** Required scopes/permissions.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IApiSpec.Security#scopes intent: Required scopes/permissions. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
         */
        String[] scopes() default {};

        /** Security type.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IApiSpec.Security#type intent: Security type. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code SecurityType}; default expression {@code SecurityType.NONE}.
         */
        SecurityType type() default SecurityType.NONE;

        /** Security types.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Enumerates the IApiSpec.Security.SecurityType choices described as Security types. Selection records an annotation value; execution remains the consumer responsibility.
         * @m3.dataStructures Nested enum containing 6 constants in the existing source order.
         */
        enum SecurityType {
            /** No security.
             * @m3.pattern synexia:aiop
             * @m3.algorithm Names the IApiSpec.Security.SecurityType#NONE policy choice: No security. This enum token does not implement the selected runtime behavior.
             * @m3.dataStructures Existing constant of IApiSpec.Security.SecurityType; its declared order and identity are preserved.
             */
            NONE,
            /** API key in header/query.
             * @m3.pattern synexia:aiop
             * @m3.algorithm Names the IApiSpec.Security.SecurityType#API_KEY policy choice: API key in header/query. This enum token does not implement the selected runtime behavior.
             * @m3.dataStructures Existing constant of IApiSpec.Security.SecurityType; its declared order and identity are preserved.
             */
            API_KEY,
            /** HTTP Basic auth.
             * @m3.pattern synexia:aiop
             * @m3.algorithm Names the IApiSpec.Security.SecurityType#BASIC policy choice: HTTP Basic auth. This enum token does not implement the selected runtime behavior.
             * @m3.dataStructures Existing constant of IApiSpec.Security.SecurityType; its declared order and identity are preserved.
             */
            BASIC,
            /** Bearer token (JWT).
             * @m3.pattern synexia:aiop
             * @m3.algorithm Names the IApiSpec.Security.SecurityType#BEARER policy choice: Bearer token (JWT). This enum token does not implement the selected runtime behavior.
             * @m3.dataStructures Existing constant of IApiSpec.Security.SecurityType; its declared order and identity are preserved.
             */
            BEARER,
            /** OAuth2.
             * @m3.pattern synexia:aiop
             * @m3.algorithm Names the IApiSpec.Security.SecurityType#OAUTH2 policy choice: OAuth2. This enum token does not implement the selected runtime behavior.
             * @m3.dataStructures Existing constant of IApiSpec.Security.SecurityType; its declared order and identity are preserved.
             */
            OAUTH2,
            /** OpenID Connect.
             * @m3.pattern synexia:aiop
             * @m3.algorithm Names the IApiSpec.Security.SecurityType#OIDC policy choice: OpenID Connect. This enum token does not implement the selected runtime behavior.
             * @m3.dataStructures Existing constant of IApiSpec.Security.SecurityType; its declared order and identity are preserved.
             */
            OIDC
        }
    }

    // --- RATE LIMITING ---

    /**
     * Maximum requests per time window. 0 = no rate limiting.
     *
     * @return rate limit
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IApiSpec#rateLimit intent: Maximum requests per time window. 0 = no rate limiting. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 0}.
     */
    int rateLimit() default 0;

    /**
     * Rate limit time window in seconds.
     *
     * @return window in seconds
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IApiSpec#rateLimitWindow intent: Rate limit time window in seconds. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 60}.
     */
    int rateLimitWindow() default 60;

    // --- CACHING ---

    /**
     * Cache-Control max-age in seconds. 0 = no caching.
     *
     * @return max age
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IApiSpec#cacheMaxAge intent: Cache-Control max-age in seconds. 0 = no caching. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 0}.
     */
    int cacheMaxAge() default 0;

    /**
     * Whether response can be cached publicly.
     *
     * @return true for public cache
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IApiSpec#cachePublic intent: Whether response can be cached publicly. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean cachePublic() default false;

    // --- VALIDATION ---

    /**
     * Whether to validate request body.
     *
     * @return true to validate
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IApiSpec#validateRequest intent: Whether to validate request body. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
     */
    boolean validateRequest() default true;

    /**
     * Whether to validate response body.
     *
     * @return true to validate
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IApiSpec#validateResponse intent: Whether to validate response body. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean validateResponse() default false;

    // --- MODEL ---

    /**
     * Preferred model for generation.
     *
     * @return model type
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IApiSpec#model intent: Preferred model for generation. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Model.ModelType}; default expression {@code Model.ModelType.SONNET}.
     */
    Model.ModelType model() default Model.ModelType.SONNET;
}
