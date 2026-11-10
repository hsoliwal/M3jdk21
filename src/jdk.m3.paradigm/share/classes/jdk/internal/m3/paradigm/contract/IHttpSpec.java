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
 * IHttpSpec - HTTP client/integration specification for AI code generation.
 *
 * <h2>Inspired By HTTP Client Libraries</h2>
 *
 * <pre>
 * HTTP clients humans created:
 *   - Apache HttpClient (Java)
 *   - OkHttp (Android/Java)
 *   - Retrofit (declarative)
 *   - RestTemplate (Spring)
 *   - WebClient (Spring reactive)
 *   - Feign (declarative)
 *   - JAX-RS Client
 *   - requests (Python)
 *   - axios (JavaScript)
 *   - curl (CLI)
 *
 * &#64;IHttpSpec specifies HTTP operations in IOP style.
 * </pre>
 *
 * <h2>Generated Code PatternKind</h2>
 *
 * <pre>{@code
 * // From: @IHttpSpec(method = GET, path = "/users/{id}")
 * // Generates:
 *
 * public User getUser(String id) {
 * 	return httpClient.get("/users/" + id).header("Accept", "application/json").timeout(Duration.ofSeconds(30))
 * 			.retrieve().bodyAs(User.class);
 * }
 * }</pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * interface IUserClient {
 *
 *     // Simple GET
 * &#64;IHttpSpec(method = GET, path = "/users/{id}", responseType = User.class)
 * User getUser(String id);
 *
 * // POST with body
 * &#64;IHttpSpec(method = POST, path = "/users", requestType = CreateUserRequest.class, responseType = User.class, expectedStatus = 201)
 * User createUser(CreateUserRequest request);
 *
 * // With retry and circuit breaker
 * &#64;IHttpSpec(method = GET, path = "/external/data", retry = @IHttpSpec.Retry(maxAttempts = 3, backoffMs = 1000), circuitBreaker = @IHttpSpec.CircuitBreaker(failureThreshold = 5, resetTimeoutMs = 60000))
 * ExternalData fetchExternalData();
 * }
 * }
 * </pre>
 *
 * @since IOP 1.0 - HTTP Glue
 * @see IApiSpec
 * @see ISshSpec
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares an HTTP client request schema with serialization, status, retry, circuit, authentication, cache and logging preferences; it sends no request and resolves no credential.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.METHOD}; existing element and enum schemas are preserved.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IHttpSpec.java}
 * blob {@code 7ddd2237fe24e7d21cdf9224092cc7d6035178fa} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IHttpSpec {

    // --- SIMPLE OPTIONS ---

    /**
     * Predefined HTTP pattern.
     *
     * @return option pattern
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IHttpSpec#option intent: Predefined HTTP pattern. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Option}; default expression {@code Option.NONE}.
     */
    Option option() default Option.NONE;

    /** Common HTTP patterns.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IHttpSpec.Option choices described as Common HTTP patterns. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 9 constants in the existing source order.
     */
    enum Option {
        /** No preset - configure manually.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHttpSpec.Option#NONE policy choice: No preset - configure manually. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHttpSpec.Option; its declared order and identity are preserved.
         */
        NONE,

        /** Simple GET returning JSON.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHttpSpec.Option#GET_JSON policy choice: Simple GET returning JSON. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHttpSpec.Option; its declared order and identity are preserved.
         */
        GET_JSON,

        /** Simple POST with JSON body.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHttpSpec.Option#POST_JSON policy choice: Simple POST with JSON body. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHttpSpec.Option; its declared order and identity are preserved.
         */
        POST_JSON,

        /** PUT with JSON body.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHttpSpec.Option#PUT_JSON policy choice: PUT with JSON body. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHttpSpec.Option; its declared order and identity are preserved.
         */
        PUT_JSON,

        /** DELETE operation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHttpSpec.Option#DELETE_SIMPLE policy choice: DELETE operation. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHttpSpec.Option; its declared order and identity are preserved.
         */
        DELETE_SIMPLE,

        /** Download file/binary.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHttpSpec.Option#DOWNLOAD policy choice: Download file/binary. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHttpSpec.Option; its declared order and identity are preserved.
         */
        DOWNLOAD,

        /** Upload file/multipart.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHttpSpec.Option#UPLOAD policy choice: Upload file/multipart. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHttpSpec.Option; its declared order and identity are preserved.
         */
        UPLOAD,

        /** Health check endpoint.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHttpSpec.Option#HEALTH_CHECK policy choice: Health check endpoint. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHttpSpec.Option; its declared order and identity are preserved.
         */
        HEALTH_CHECK,

        /** Webhook callback.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHttpSpec.Option#WEBHOOK policy choice: Webhook callback. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHttpSpec.Option; its declared order and identity are preserved.
         */
        WEBHOOK
    }

    // --- HTTP METHOD ---

    /**
     * HTTP method.
     *
     * @return method
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IHttpSpec#method intent: HTTP method. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Method}; default expression {@code Method.GET}.
     */
    Method method() default Method.GET;

    /** HTTP methods.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IHttpSpec.Method choices described as HTTP methods. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 7 constants in the existing source order.
     */
    enum Method {
        /** GET request.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHttpSpec.Method#GET policy choice: GET request. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHttpSpec.Method; its declared order and identity are preserved.
         */
        GET,
        /** POST request.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHttpSpec.Method#POST policy choice: POST request. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHttpSpec.Method; its declared order and identity are preserved.
         */
        POST,
        /** PUT request.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHttpSpec.Method#PUT policy choice: PUT request. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHttpSpec.Method; its declared order and identity are preserved.
         */
        PUT,
        /** DELETE request.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHttpSpec.Method#DELETE policy choice: DELETE request. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHttpSpec.Method; its declared order and identity are preserved.
         */
        DELETE,
        /** PATCH request.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHttpSpec.Method#PATCH policy choice: PATCH request. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHttpSpec.Method; its declared order and identity are preserved.
         */
        PATCH,
        /** HEAD request.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHttpSpec.Method#HEAD policy choice: HEAD request. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHttpSpec.Method; its declared order and identity are preserved.
         */
        HEAD,
        /** OPTIONS request.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHttpSpec.Method#OPTIONS policy choice: OPTIONS request. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHttpSpec.Method; its declared order and identity are preserved.
         */
        OPTIONS
    }

    // --- URL ---

    /**
     * URL path (can include path variables like {id}).
     *
     * @return path
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IHttpSpec#path intent: URL path (can include path variables like {id}). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String path() default "";

    /**
     * Base URL (if different from default).
     *
     * @return base URL
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IHttpSpec#baseUrl intent: Base URL (if different from default). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String baseUrl() default "";

    /**
     * Query parameters as key=value pairs.
     *
     * @return query params
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IHttpSpec#queryParams intent: Query parameters as key=value pairs. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] queryParams() default {};

    // --- HEADERS ---

    /**
     * Request headers as "Name: Value" pairs.
     *
     * @return headers
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IHttpSpec#headers intent: Request headers as "Name: Value" pairs. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] headers() default {};

    /**
     * Content-Type header.
     *
     * @return content type
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IHttpSpec#contentType intent: Content-Type header. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code "application/json"}.
     */
    String contentType() default "application/json";

    /**
     * Accept header.
     *
     * @return accept type
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IHttpSpec#accept intent: Accept header. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code "application/json"}.
     */
    String accept() default "application/json";

    // --- BODY ---

    /**
     * Request body type.
     *
     * @return request type
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IHttpSpec#requestType intent: Request body type. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<?>}; default expression {@code Void.class}.
     */
    Class<?> requestType() default Void.class;

    /**
     * Response body type.
     *
     * @return response type
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IHttpSpec#responseType intent: Response body type. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<?>}; default expression {@code Void.class}.
     */
    Class<?> responseType() default Void.class;

    /**
     * Body serialization format.
     *
     * @return format
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IHttpSpec#bodyFormat intent: Body serialization format. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code BodyFormat}; default expression {@code BodyFormat.JSON}.
     */
    BodyFormat bodyFormat() default BodyFormat.JSON;

    /** Body formats.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IHttpSpec.BodyFormat choices described as Body formats. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 6 constants in the existing source order.
     */
    enum BodyFormat {
        /** JSON (Jackson).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHttpSpec.BodyFormat#JSON policy choice: JSON (Jackson). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHttpSpec.BodyFormat; its declared order and identity are preserved.
         */
        JSON,
        /** XML (JAXB).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHttpSpec.BodyFormat#XML policy choice: XML (JAXB). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHttpSpec.BodyFormat; its declared order and identity are preserved.
         */
        XML,
        /** Form URL encoded.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHttpSpec.BodyFormat#FORM policy choice: Form URL encoded. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHttpSpec.BodyFormat; its declared order and identity are preserved.
         */
        FORM,
        /** Multipart form data.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHttpSpec.BodyFormat#MULTIPART policy choice: Multipart form data. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHttpSpec.BodyFormat; its declared order and identity are preserved.
         */
        MULTIPART,
        /** Plain text.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHttpSpec.BodyFormat#TEXT policy choice: Plain text. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHttpSpec.BodyFormat; its declared order and identity are preserved.
         */
        TEXT,
        /** Binary/bytes.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHttpSpec.BodyFormat#BINARY policy choice: Binary/bytes. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHttpSpec.BodyFormat; its declared order and identity are preserved.
         */
        BINARY
    }

    // --- RESPONSE ---

    /**
     * Expected success status code.
     *
     * @return status code
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IHttpSpec#expectedStatus intent: Expected success status code. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 200}.
     */
    int expectedStatus() default 200;

    /**
     * Status codes to treat as success.
     *
     * @return success codes
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IHttpSpec#successCodes intent: Status codes to treat as success. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int[]}; default expression {@code {200, 201, 204}}.
     */
    int[] successCodes() default {200, 201, 204};

    // --- TIMEOUT ---

    /**
     * Connection timeout in milliseconds.
     *
     * @return connect timeout
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IHttpSpec#connectTimeoutMs intent: Connection timeout in milliseconds. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 10000}.
     */
    int connectTimeoutMs() default 10000;

    /**
     * Read timeout in milliseconds.
     *
     * @return read timeout
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IHttpSpec#readTimeoutMs intent: Read timeout in milliseconds. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 30000}.
     */
    int readTimeoutMs() default 30000;

    /**
     * Write timeout in milliseconds.
     *
     * @return write timeout
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IHttpSpec#writeTimeoutMs intent: Write timeout in milliseconds. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 30000}.
     */
    int writeTimeoutMs() default 30000;

    // --- RETRY ---

    /**
     * Retry configuration.
     *
     * @return retry config
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IHttpSpec#retry intent: Retry configuration. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Retry}; default expression {@code @Retry}.
     */
    Retry retry() default @Retry;

    /** Retry configuration.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares HTTP retry attempts, backoff, response codes and timeout preference; it schedules no retry and retains implicit CLASS retention without explicit Target restriction.
     * @m3.dataStructures CLASS (implicit default)-retained annotation targeting {@code all declaration contexts (no explicit Target)}; existing element and enum schemas are preserved.
     */
    @interface Retry {

        /** Maximum retry attempts.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IHttpSpec.Retry#maxAttempts intent: Maximum retry attempts. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 0}.
         */
        int maxAttempts() default 0;

        /** Initial backoff in milliseconds.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IHttpSpec.Retry#backoffMs intent: Initial backoff in milliseconds. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 1000}.
         */
        int backoffMs() default 1000;

        /** Backoff multiplier.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IHttpSpec.Retry#multiplier intent: Backoff multiplier. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code double}; default expression {@code 2.0}.
         */
        double multiplier() default 2.0;

        /** Maximum backoff in milliseconds.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IHttpSpec.Retry#maxBackoffMs intent: Maximum backoff in milliseconds. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 30000}.
         */
        int maxBackoffMs() default 30000;

        /** Status codes that trigger retry.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IHttpSpec.Retry#retryOn intent: Status codes that trigger retry. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code int[]}; default expression {@code {502, 503, 504}}.
         */
        int[] retryOn() default {502, 503, 504};

        /** Whether to retry on timeout.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IHttpSpec.Retry#retryOnTimeout intent: Whether to retry on timeout. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
         */
        boolean retryOnTimeout() default true;
    }

    // --- CIRCUIT BREAKER ---

    /**
     * Circuit breaker configuration.
     *
     * @return circuit breaker config
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IHttpSpec#circuitBreaker intent: Circuit breaker configuration. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code CircuitBreaker}; default expression {@code @CircuitBreaker}.
     */
    CircuitBreaker circuitBreaker() default @CircuitBreaker;

    /** Circuit breaker configuration.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares circuit thresholds, timing and half-open allowance; it maintains no circuit state and retains implicit CLASS retention without explicit Target restriction.
     * @m3.dataStructures CLASS (implicit default)-retained annotation targeting {@code all declaration contexts (no explicit Target)}; existing element and enum schemas are preserved.
     */
    @interface CircuitBreaker {

        /** Whether circuit breaker is enabled.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IHttpSpec.CircuitBreaker#enabled intent: Whether circuit breaker is enabled. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
         */
        boolean enabled() default false;

        /** Failure threshold to open circuit.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IHttpSpec.CircuitBreaker#failureThreshold intent: Failure threshold to open circuit. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 5}.
         */
        int failureThreshold() default 5;

        /** Time window for failure counting (ms).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IHttpSpec.CircuitBreaker#windowMs intent: Time window for failure counting (ms). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 60000}.
         */
        int windowMs() default 60000;

        /** Time before attempting reset (ms).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IHttpSpec.CircuitBreaker#resetTimeoutMs intent: Time before attempting reset (ms). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 30000}.
         */
        int resetTimeoutMs() default 30000;

        /** Percentage of calls to allow in half-open state.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IHttpSpec.CircuitBreaker#halfOpenCalls intent: Requested half-open allowance, preserved as an int defaulting to 3; the existing prose calls it a percentage, while the element name refers to calls, and this annotation implements neither interpretation. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 3}.
         */
        int halfOpenCalls() default 3;
    }

    // --- AUTHENTICATION ---

    /**
     * Authentication type.
     *
     * @return auth type
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IHttpSpec#auth intent: Authentication type. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code AuthType}; default expression {@code AuthType.NONE}.
     */
    AuthType auth() default AuthType.NONE;

    /** Authentication types.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IHttpSpec.AuthType choices described as Authentication types. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 7 constants in the existing source order.
     */
    enum AuthType {
        /** No authentication.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHttpSpec.AuthType#NONE policy choice: No authentication. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHttpSpec.AuthType; its declared order and identity are preserved.
         */
        NONE,
        /** HTTP Basic.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHttpSpec.AuthType#BASIC policy choice: HTTP Basic. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHttpSpec.AuthType; its declared order and identity are preserved.
         */
        BASIC,
        /** Bearer token (OAuth2).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHttpSpec.AuthType#BEARER policy choice: Bearer token (OAuth2). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHttpSpec.AuthType; its declared order and identity are preserved.
         */
        BEARER,
        /** API key in header.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHttpSpec.AuthType#API_KEY policy choice: API key in header. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHttpSpec.AuthType; its declared order and identity are preserved.
         */
        API_KEY,
        /** OAuth2 client credentials.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHttpSpec.AuthType#OAUTH2_CLIENT policy choice: OAuth2 client credentials. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHttpSpec.AuthType; its declared order and identity are preserved.
         */
        OAUTH2_CLIENT,
        /** OAuth2 authorization code.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHttpSpec.AuthType#OAUTH2_CODE policy choice: OAuth2 authorization code. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHttpSpec.AuthType; its declared order and identity are preserved.
         */
        OAUTH2_CODE,
        /** Custom authentication.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHttpSpec.AuthType#CUSTOM policy choice: Custom authentication. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHttpSpec.AuthType; its declared order and identity are preserved.
         */
        CUSTOM
    }

    /**
     * Header name for API key auth.
     *
     * @return header name
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IHttpSpec#apiKeyHeader intent: Header name for API key auth. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code "X-API-Key"}.
     */
    String apiKeyHeader() default "X-API-Key";

    // --- CACHING ---

    /**
     * Whether to cache responses.
     *
     * @return true to cache
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IHttpSpec#cacheable intent: Whether to cache responses. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean cacheable() default false;

    /**
     * Cache TTL in seconds.
     *
     * @return TTL
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IHttpSpec#cacheTtlSeconds intent: Cache TTL in seconds. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 300}.
     */
    int cacheTtlSeconds() default 300;

    /**
     * Cache key expression.
     *
     * @return cache key
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IHttpSpec#cacheKey intent: Cache key expression. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String cacheKey() default "";

    // --- LOGGING ---

    /**
     * Logging level for this request.
     *
     * @return log level
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IHttpSpec#logLevel intent: Logging level for this request. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code LogLevel}; default expression {@code LogLevel.BASIC}.
     */
    LogLevel logLevel() default LogLevel.BASIC;

    /** HTTP logging levels.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IHttpSpec.LogLevel choices described as HTTP logging levels. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 4 constants in the existing source order.
     */
    enum LogLevel {
        /** No logging.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHttpSpec.LogLevel#NONE policy choice: No logging. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHttpSpec.LogLevel; its declared order and identity are preserved.
         */
        NONE,
        /** Log method and URL only.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHttpSpec.LogLevel#BASIC policy choice: Log method and URL only. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHttpSpec.LogLevel; its declared order and identity are preserved.
         */
        BASIC,
        /** Log headers too.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHttpSpec.LogLevel#HEADERS policy choice: Log headers too. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHttpSpec.LogLevel; its declared order and identity are preserved.
         */
        HEADERS,
        /** Log full request/response body.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IHttpSpec.LogLevel#BODY policy choice: Log full request/response body. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IHttpSpec.LogLevel; its declared order and identity are preserved.
         */
        BODY
    }

    // --- MODEL ---

    /**
     * Preferred model for generation.
     *
     * @return model type
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IHttpSpec#model intent: Preferred model for generation. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Model.ModelType}; default expression {@code Model.ModelType.SONNET}.
     */
    Model.ModelType model() default Model.ModelType.SONNET;
}
