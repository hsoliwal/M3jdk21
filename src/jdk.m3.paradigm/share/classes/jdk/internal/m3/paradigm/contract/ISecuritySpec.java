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
 * ISecuritySpec - Security specification for AI code generation.
 *
 * <h2>Security Patterns</h2>
 *
 * <pre>
 * Security patterns in Java:
 *   - Spring Security (@PreAuthorize, @Secured)
 *   - Jakarta Security (@RolesAllowed, @PermitAll)
 *   - OWASP recommendations
 *   - Input validation / output encoding
 *   - Encryption / hashing
 *   - Rate limiting / throttling
 *
 * &#64;ISecuritySpec specifies security constraints in IOP style.
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * interface IUserService {
 *
 *     // Role-based access
 * &#64;ISecuritySpec(roles = {"ADMIN", "USER_MANAGER"}, action = "delete user")
 * void deleteUser(String userId);
 *
 * // Input sanitization
 * &#64;ISecuritySpec(sanitize = {SCRIPT_INJECTION, SQL_INJECTION}, params = {"email", "name"})
 * User createUser(String email, String name);
 *
 * // Data encryption
 * &#64;ISecuritySpec(encrypt = true, algorithm = AES_256, sensitiveFields = {"ssn", "creditCard"})
 * User getUser(String id);
 * }
 * }
 * </pre>
 *
 * @since IOP 1.0 - Security Specification
 * @see IApiSpec
 * @see IAspectSpec
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares authentication, roles, permissions, sanitization, cryptographic, throttling and audit requirements; it authenticates no principal, encrypts no data and grants no permission.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code {ElementType.TYPE, ElementType.METHOD}}; existing element and enum schemas are preserved.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/ISecuritySpec.java}
 * blob {@code b8bdb082a0f437eaa0ac34b53d835634aa2f7496} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface ISecuritySpec {

    // --- SIMPLE OPTIONS ---

    /**
     * Predefined security pattern.
     *
     * @return option
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISecuritySpec#option intent: Predefined security pattern. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Option}; default expression {@code Option.NONE}.
     */
    Option option() default Option.NONE;

    /** Common security patterns.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the ISecuritySpec.Option choices described as Common security patterns. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 8 constants in the existing source order.
     */
    enum Option {
        /** No preset - configure manually.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISecuritySpec.Option#NONE policy choice: No preset - configure manually. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISecuritySpec.Option; its declared order and identity are preserved.
         */
        NONE,

        /** Public access (no auth).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISecuritySpec.Option#PUBLIC policy choice: Public access (no auth). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISecuritySpec.Option; its declared order and identity are preserved.
         */
        PUBLIC,

        /** Authenticated users only.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISecuritySpec.Option#AUTHENTICATED policy choice: Authenticated users only. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISecuritySpec.Option; its declared order and identity are preserved.
         */
        AUTHENTICATED,

        /** Admin role required.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISecuritySpec.Option#ADMIN_ONLY policy choice: Admin role required. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISecuritySpec.Option; its declared order and identity are preserved.
         */
        ADMIN_ONLY,

        /** Owner/self only.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISecuritySpec.Option#OWNER_ONLY policy choice: Owner/self only. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISecuritySpec.Option; its declared order and identity are preserved.
         */
        OWNER_ONLY,

        /** Read-only access.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISecuritySpec.Option#READ_ONLY policy choice: Read-only access. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISecuritySpec.Option; its declared order and identity are preserved.
         */
        READ_ONLY,

        /** Full sanitization.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISecuritySpec.Option#SANITIZED policy choice: Full sanitization. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISecuritySpec.Option; its declared order and identity are preserved.
         */
        SANITIZED,

        /** Encrypted at rest.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISecuritySpec.Option#ENCRYPTED policy choice: Encrypted at rest. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISecuritySpec.Option; its declared order and identity are preserved.
         */
        ENCRYPTED
    }

    // --- AUTHENTICATION ---

    /**
     * Required authentication type.
     *
     * @return auth type
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISecuritySpec#auth intent: Required authentication type. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code AuthType}; default expression {@code AuthType.REQUIRED}.
     */
    AuthType auth() default AuthType.REQUIRED;

    /** Authentication types.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the ISecuritySpec.AuthType choices described as Authentication types. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 7 constants in the existing source order.
     */
    enum AuthType {
        /** No authentication required.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISecuritySpec.AuthType#NONE policy choice: No authentication required. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISecuritySpec.AuthType; its declared order and identity are preserved.
         */
        NONE,

        /** Authentication required.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISecuritySpec.AuthType#REQUIRED policy choice: Authentication required. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISecuritySpec.AuthType; its declared order and identity are preserved.
         */
        REQUIRED,

        /** Authentication optional.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISecuritySpec.AuthType#OPTIONAL policy choice: Authentication optional. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISecuritySpec.AuthType; its declared order and identity are preserved.
         */
        OPTIONAL,

        /** API key required.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISecuritySpec.AuthType#API_KEY policy choice: API key required. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISecuritySpec.AuthType; its declared order and identity are preserved.
         */
        API_KEY,

        /** OAuth2 token required.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISecuritySpec.AuthType#OAUTH2 policy choice: OAuth2 token required. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISecuritySpec.AuthType; its declared order and identity are preserved.
         */
        OAUTH2,

        /** JWT token required.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISecuritySpec.AuthType#JWT policy choice: JWT token required. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISecuritySpec.AuthType; its declared order and identity are preserved.
         */
        JWT,

        /** Certificate required.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISecuritySpec.AuthType#CERTIFICATE policy choice: Certificate required. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISecuritySpec.AuthType; its declared order and identity are preserved.
         */
        CERTIFICATE
    }

    // --- AUTHORIZATION ---

    /**
     * Required roles (any of).
     *
     * @return roles
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISecuritySpec#roles intent: Required roles (any of). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] roles() default {};

    /**
     * Required permissions (any of).
     *
     * @return permissions
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISecuritySpec#permissions intent: Required permissions (any of). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] permissions() default {};

    /**
     * Required roles (all of).
     *
     * @return all roles required
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISecuritySpec#allRoles intent: Required roles (all of). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] allRoles() default {};

    /**
     * SpEL expression for authorization.
     *
     * @return authorization expression
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISecuritySpec#authorize intent: SpEL expression for authorization. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String authorize() default "";

    /**
     * Action description (for audit logging).
     *
     * @return action
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISecuritySpec#action intent: Action description (for audit logging). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String action() default "";

    // --- INPUT SANITIZATION ---

    /**
     * Sanitization types to apply.
     *
     * @return sanitization types
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISecuritySpec#sanitize intent: Sanitization types to apply. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Sanitize[]}; default expression {@code {}}.
     */
    Sanitize[] sanitize() default {};

    /** Sanitization types.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the ISecuritySpec.Sanitize choices described as Sanitization types. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 8 constants in the existing source order.
     */
    enum Sanitize {
        /** HTML/script injection.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISecuritySpec.Sanitize#SCRIPT_INJECTION policy choice: HTML/script injection. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISecuritySpec.Sanitize; its declared order and identity are preserved.
         */
        SCRIPT_INJECTION,

        /** SQL injection.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISecuritySpec.Sanitize#SQL_INJECTION policy choice: SQL injection. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISecuritySpec.Sanitize; its declared order and identity are preserved.
         */
        SQL_INJECTION,

        /** Command injection.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISecuritySpec.Sanitize#COMMAND_INJECTION policy choice: Command injection. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISecuritySpec.Sanitize; its declared order and identity are preserved.
         */
        COMMAND_INJECTION,

        /** Path traversal.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISecuritySpec.Sanitize#PATH_TRAVERSAL policy choice: Path traversal. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISecuritySpec.Sanitize; its declared order and identity are preserved.
         */
        PATH_TRAVERSAL,

        /** LDAP injection.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISecuritySpec.Sanitize#LDAP_INJECTION policy choice: LDAP injection. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISecuritySpec.Sanitize; its declared order and identity are preserved.
         */
        LDAP_INJECTION,

        /** XML injection.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISecuritySpec.Sanitize#XML_INJECTION policy choice: XML injection. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISecuritySpec.Sanitize; its declared order and identity are preserved.
         */
        XML_INJECTION,

        /** Header injection.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISecuritySpec.Sanitize#HEADER_INJECTION policy choice: Header injection. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISecuritySpec.Sanitize; its declared order and identity are preserved.
         */
        HEADER_INJECTION,

        /** All OWASP top 10.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISecuritySpec.Sanitize#OWASP_ALL policy choice: Requested broad security-review category named OWASP_ALL; no coverage of a particular OWASP list or sanitization guarantee is established. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISecuritySpec.Sanitize; its declared order and identity are preserved.
         */
        OWASP_ALL
    }

    /**
     * Parameters to sanitize.
     *
     * @return parameter names
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISecuritySpec#params intent: Parameters to sanitize. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] params() default {};

    // --- ENCRYPTION ---

    /**
     * Whether to encrypt data.
     *
     * @return true to encrypt
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISecuritySpec#encrypt intent: Whether to encrypt data. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean encrypt() default false;

    /**
     * Encryption algorithm.
     *
     * @return algorithm
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISecuritySpec#algorithm intent: Encryption algorithm. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Algorithm}; default expression {@code Algorithm.AES_256}.
     */
    Algorithm algorithm() default Algorithm.AES_256;

    /** Encryption algorithms.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the ISecuritySpec.Algorithm choices described as Encryption algorithms. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 4 constants in the existing source order.
     */
    enum Algorithm {
        /** AES 128-bit.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISecuritySpec.Algorithm#AES_128 policy choice: AES 128-bit. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISecuritySpec.Algorithm; its declared order and identity are preserved.
         */
        AES_128,

        /** AES 256-bit.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISecuritySpec.Algorithm#AES_256 policy choice: AES 256-bit. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISecuritySpec.Algorithm; its declared order and identity are preserved.
         */
        AES_256,

        /** RSA.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISecuritySpec.Algorithm#RSA policy choice: RSA. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISecuritySpec.Algorithm; its declared order and identity are preserved.
         */
        RSA,

        /** ChaCha20-Poly1305.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISecuritySpec.Algorithm#CHACHA20 policy choice: ChaCha20-Poly1305. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISecuritySpec.Algorithm; its declared order and identity are preserved.
         */
        CHACHA20
    }

    /**
     * Fields that contain sensitive data.
     *
     * @return sensitive field names
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISecuritySpec#sensitiveFields intent: Fields that contain sensitive data. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] sensitiveFields() default {};

    // --- HASHING ---

    /**
     * Fields to hash (one-way).
     *
     * @return fields to hash
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISecuritySpec#hashFields intent: Fields to hash (one-way). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] hashFields() default {};

    /**
     * Hashing algorithm.
     *
     * @return hash algorithm
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISecuritySpec#hashAlgorithm intent: Hashing algorithm. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code HashAlgorithm}; default expression {@code HashAlgorithm.BCRYPT}.
     */
    HashAlgorithm hashAlgorithm() default HashAlgorithm.BCRYPT;

    /** Hash algorithms.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the ISecuritySpec.HashAlgorithm choices described as Hash algorithms. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 4 constants in the existing source order.
     */
    enum HashAlgorithm {
        /** BCrypt (passwords).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISecuritySpec.HashAlgorithm#BCRYPT policy choice: BCrypt (passwords). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISecuritySpec.HashAlgorithm; its declared order and identity are preserved.
         */
        BCRYPT,

        /** Argon2 (passwords).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISecuritySpec.HashAlgorithm#ARGON2 policy choice: Argon2 (passwords). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISecuritySpec.HashAlgorithm; its declared order and identity are preserved.
         */
        ARGON2,

        /** SHA-256.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISecuritySpec.HashAlgorithm#SHA256 policy choice: SHA-256. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISecuritySpec.HashAlgorithm; its declared order and identity are preserved.
         */
        SHA256,

        /** SHA-512.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISecuritySpec.HashAlgorithm#SHA512 policy choice: SHA-512. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISecuritySpec.HashAlgorithm; its declared order and identity are preserved.
         */
        SHA512
    }

    // --- RATE LIMITING ---

    /**
     * Rate limit - requests per time window.
     *
     * @return requests allowed
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISecuritySpec#rateLimit intent: Rate limit - requests per time window. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 0}.
     */
    int rateLimit() default 0;

    /**
     * Rate limit time window in seconds.
     *
     * @return window seconds
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISecuritySpec#rateLimitWindow intent: Rate limit time window in seconds. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 60}.
     */
    int rateLimitWindow() default 60;

    /**
     * Rate limit key (IP, user, API key).
     *
     * @return rate limit key
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISecuritySpec#rateLimitKey intent: Rate limit key (IP, user, API key). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code RateLimitKey}; default expression {@code RateLimitKey.IP}.
     */
    RateLimitKey rateLimitKey() default RateLimitKey.IP;

    /** Rate limit key types.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the ISecuritySpec.RateLimitKey choices described as Rate limit key types. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 4 constants in the existing source order.
     */
    enum RateLimitKey {
        /** Per IP address.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISecuritySpec.RateLimitKey#IP policy choice: Per IP address. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISecuritySpec.RateLimitKey; its declared order and identity are preserved.
         */
        IP,
        /** Per authenticated user.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISecuritySpec.RateLimitKey#USER policy choice: Per authenticated user. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISecuritySpec.RateLimitKey; its declared order and identity are preserved.
         */
        USER,
        /** Per API key.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISecuritySpec.RateLimitKey#API_KEY policy choice: Per API key. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISecuritySpec.RateLimitKey; its declared order and identity are preserved.
         */
        API_KEY,
        /** Global.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISecuritySpec.RateLimitKey#GLOBAL policy choice: Global. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISecuritySpec.RateLimitKey; its declared order and identity are preserved.
         */
        GLOBAL
    }

    // --- AUDIT ---

    /**
     * Whether to audit this operation.
     *
     * @return true to audit
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISecuritySpec#audit intent: Whether to audit this operation. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean audit() default false;

    /**
     * Audit level.
     *
     * @return audit level
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISecuritySpec#auditLevel intent: Audit level. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code AuditLevel}; default expression {@code AuditLevel.BASIC}.
     */
    AuditLevel auditLevel() default AuditLevel.BASIC;

    /** Audit levels.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the ISecuritySpec.AuditLevel choices described as Audit levels. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 3 constants in the existing source order.
     */
    enum AuditLevel {
        /** Log action only.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISecuritySpec.AuditLevel#BASIC policy choice: Log action only. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISecuritySpec.AuditLevel; its declared order and identity are preserved.
         */
        BASIC,
        /** Log action + parameters.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISecuritySpec.AuditLevel#DETAILED policy choice: Log action + parameters. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISecuritySpec.AuditLevel; its declared order and identity are preserved.
         */
        DETAILED,
        /** Log action + params + result.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISecuritySpec.AuditLevel#FULL policy choice: Log action + params + result. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISecuritySpec.AuditLevel; its declared order and identity are preserved.
         */
        FULL
    }

    // --- DESCRIPTION ---

    /**
     * Security requirement description.
     *
     * @return description
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISecuritySpec#value intent: Security requirement description. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String value() default "";

    // --- MODEL ---

    /**
     * Preferred model for generation. Security code should use OPUS.
     *
     * @return model type
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISecuritySpec#model intent: Preferred generation-model token for the security specification; no security capability is established by this choice. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Model.ModelType}; default expression {@code Model.ModelType.OPUS}.
     */
    Model.ModelType model() default Model.ModelType.OPUS;
}
