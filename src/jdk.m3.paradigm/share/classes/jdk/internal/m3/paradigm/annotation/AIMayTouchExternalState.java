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

package jdk.internal.m3.paradigm.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AIMayTouchExternalState - Declares that method may access external state.
 *
 * <h2>IOP v1 External State Contract</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  @AIMayTouchExternalState - EXTERNAL DEPENDENCY DECLARATION               ║
 * ╠═══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                           ║
 * ║  "This method/class may read from or write to external state."            ║
 * ║                                                                           ║
 * ║  External state includes:                                                 ║
 * ║    - File system                                                          ║
 * ║    - Network (HTTP, database, messaging)                                  ║
 * ║    - System properties / environment variables                            ║
 * ║    - System clock (current time)                                          ║
 * ║    - Random number generators                                             ║
 * ║    - Console I/O                                                          ║
 * ║                                                                           ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 * {@code
 *
 * // Method reads files
 * &#64;AIMayTouchExternalState(ExternalState.FILE_SYSTEM)
 * &#64;AIGenMethod("Load configuration from file")
 * Config loadConfig(Path path);
 *
 * // Method calls HTTP APIs
 * &#64;AIMayTouchExternalState(ExternalState.NETWORK)
 * &#64;AIGenMethod("Fetch user data from remote service")
 * User fetchUser(long id);
 *
 * // Method uses current time
 * &#64;AIMayTouchExternalState(ExternalState.SYSTEM_CLOCK)
 * &#64;AIGenMethod("Generate timestamped log entry")
 * LogEntry createLogEntry(String message);
 *
 * // Multiple external dependencies
 * @AIMayTouchExternalState({ExternalState.FILE_SYSTEM, ExternalState.NETWORK, ExternalState.SYSTEM_CLOCK})
 * &#64;AIGenClass("External data service")
 * interface IDataService {
 *
 * 	void syncData();
 * }
 * }
 * </pre>
 *
 * <h2>Why Declare External State?</h2>
 * <ul>
 * <li>Clear dependency documentation</li>
 * <li>Test planning (what to mock)</li>
 * <li>Deployment requirements</li>
 * <li>Security review focus</li>
 * <li>Performance profiling hints</li>
 * </ul>
 *
 * <h2>Interaction with Other Annotations</h2>
 *
 * <pre>
 * &#64;AISideEffectFree + @AIMayTouchExternalState
 *   → Read-only access to external state (no writes)
 *
 * {@code @AIDeterministic} + @AIMayTouchExternalState
 *   → External state must be mocked for determinism
 *
 * Neither annotation:
 *   → Method is assumed pure (no external state)
 * </pre>
 *
 * @since ContextOS 1.0 - IOP v1
 * @see AIDeterministic
 * @see AISideEffectFree
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/AIMayTouchExternalState.java}
 * blob {@code 4496af2c2694ddf0a3080ef5479b41d2cd31d572} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface AIMayTouchExternalState {

    /**
     * The types of external state this method may access.
     *
     * @return array of external state types
     */
    ExternalState[] value();

    /**
     * Whether access is read-only.
     *
     * <p>
     * If true, method reads external state but does not modify it.
     * </p>
     *
     * @return true if read-only (default: false)
     */
    boolean readOnly() default false;

    /**
     * Whether to require mocking in tests.
     *
     * <p>
     * If true, tests must mock external state access.
     * </p>
     *
     * @return true to require mocking (default: true)
     */
    boolean requireMocking() default true;

    /**
     * Whether to log all external state access.
     *
     * @return true to enable access logging (default: false)
     */
    boolean logAccess() default false;

    /**
     * Timeout in milliseconds for external state access.
     *
     * <p>
     * 0 means no timeout. Applies to network/database operations.
     * </p>
     *
     * @return timeout in ms (default: 30000)
     */
    int timeout() default 30000;

    /**
     * Whether to retry on failure.
     *
     * @return true to enable retries (default: true for NETWORK)
     */
    boolean retryOnFailure() default true;

    /**
     * Maximum retry attempts.
     *
     * @return max retries (default: 3)
     */
    int maxRetries() default 3;

    /** Types of external state that may be accessed. */
    enum ExternalState {
        /**
         * File system access (read/write files).
         *
         * <p>
         * Includes: Files.read, Files.write, FileInputStream, etc.
         * </p>
         */
        FILE_SYSTEM,

        /**
         * Network access (HTTP, TCP, UDP, etc.).
         *
         * <p>
         * Includes: HttpClient, Socket, RestTemplate, etc.
         * </p>
         */
        NETWORK,

        /**
         * Database access (SQL, NoSQL, etc.).
         *
         * <p>
         * Includes: JDBC, JPA, MongoDB, Redis, etc.
         * </p>
         */
        DATABASE,

        /**
         * Message queue access (Kafka, RabbitMQ, etc.).
         *
         * <p>
         * Includes: KafkaTemplate, JmsTemplate, etc.
         * </p>
         */
        MESSAGE_QUEUE,

        /**
         * System clock access (current time).
         *
         * <p>
         * Includes: System.currentTimeMillis(), Instant.now(), etc.
         * </p>
         */
        SYSTEM_CLOCK,

        /**
         * Random number generation.
         *
         * <p>
         * Includes: Random, SecureRandom, ThreadLocalRandom, etc.
         * </p>
         */
        RANDOM,

        /**
         * Environment variables / system properties.
         *
         * <p>
         * Includes: System.getenv(), System.getProperty(), etc.
         * </p>
         */
        ENVIRONMENT,

        /**
         * Console I/O (stdin, stdout, stderr).
         *
         * <p>
         * Includes: System.in, System.out, Console, etc.
         * </p>
         */
        CONSOLE,

        /**
         * Process execution (Runtime.exec, ProcessBuilder).
         *
         * <p>
         * External process invocation.
         * </p>
         */
        PROCESS,

        /**
         * Native code (JNI, JNA, Panama).
         *
         * <p>
         * Calls to native libraries.
         * </p>
         */
        NATIVE,

        /**
         * Thread/executor state.
         *
         * <p>
         * Includes: Thread.currentThread(), ExecutorService, etc.
         * </p>
         */
        THREADING,

        /**
         * Security/crypto state (KeyStore, SecureRandom).
         *
         * <p>
         * Cryptographic operations that may use hardware RNG.
         * </p>
         */
        SECURITY,

        /**
         * GUI/AWT state (clipboard, display, etc.).
         *
         * <p>
         * Desktop integration.
         * </p>
         */
        GUI,

        /**
         * Any external state (wildcard).
         *
         * <p>
         * Use when the exact types are unknown or too many to list.
         * </p>
         */
        ANY
    }
}
