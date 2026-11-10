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
 * ISshSpec - SSH connection/command specification for AI code generation.
 *
 * <h2>Inspired By SSH Libraries</h2>
 *
 * <pre>
 * SSH tools humans created:
 *   - JSch (Java)
 *   - Apache MINA SSHD (Java)
 *   - sshj (Java)
 *   - Paramiko (Python)
 *   - ssh2 (Node.js)
 *   - OpenSSH (CLI)
 *   - Ansible (automation)
 *   - Fabric (Python deployment)
 *
 * &#64;ISshSpec specifies SSH operations in IOP style.
 * </pre>
 *
 * <h2>Generated Code PatternKind</h2>
 *
 * <pre>{@code
 * // From: @ISshSpec(host = "${ssh.host}", command = "ls -la")
 * // Generates:
 *
 * public String listFiles() {
 * 	try (var session = sshClient.connect(host, port)) {
 * 		session.authPublicKey(user, keyPath);
 * 		return session.exec("ls -la").getOutput();
 * 	}
 * }
 * }</pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * interface IServerOps {
 *
 *     // Simple command execution
 * &#64;ISshSpec(host = "${server.host}", command = "df -h")
 * String getDiskUsage();
 *
 * // With sudo
 * &#64;ISshSpec(host = "${server.host}", command = "systemctl restart nginx", sudo = true)
 * void restartNginx();
 *
 * // File transfer
 * &#64;ISshSpec(host = "${server.host}", operation = UPLOAD, localPath = "/tmp/config.yml", remotePath = "/etc/app/config.yml")
 * void uploadConfig();
 *
 * // With jump host (bastion)
 * &#64;ISshSpec(host = "${internal.host}", jumpHost = "${bastion.host}", command = "cat /var/log/app.log | tail -100")
 * String getTailLogs();
 * }
 * }
 * </pre>
 *
 * @since IOP 1.0 - SSH Glue
 * @see IHttpSpec
 * @see IExecSpec
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares SSH connection, authentication, command, transfer, tunnel, timeout and output preferences; it reads no key, connects to no host and executes no command.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.METHOD}; existing element and enum schemas are preserved.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/ISshSpec.java}
 * blob {@code 06a6367c06327e1593ff487a16d8935edbad2220} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface ISshSpec {

    // --- SIMPLE OPTIONS ---

    /**
     * Predefined SSH pattern.
     *
     * @return option pattern
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISshSpec#option intent: Predefined SSH pattern. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Option}; default expression {@code Option.NONE}.
     */
    Option option() default Option.NONE;

    /** Common SSH patterns.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the ISshSpec.Option choices described as Common SSH patterns. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 8 constants in the existing source order.
     */
    enum Option {
        /** No preset - configure manually.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISshSpec.Option#NONE policy choice: No preset - configure manually. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISshSpec.Option; its declared order and identity are preserved.
         */
        NONE,

        /** Simple command execution.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISshSpec.Option#EXEC policy choice: Simple command execution. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISshSpec.Option; its declared order and identity are preserved.
         */
        EXEC,

        /** Execute with sudo.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISshSpec.Option#EXEC_SUDO policy choice: Execute with sudo. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISshSpec.Option; its declared order and identity are preserved.
         */
        EXEC_SUDO,

        /** Upload file (SCP).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISshSpec.Option#UPLOAD policy choice: Upload file (SCP). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISshSpec.Option; its declared order and identity are preserved.
         */
        UPLOAD,

        /** Download file (SCP).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISshSpec.Option#DOWNLOAD policy choice: Download file (SCP). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISshSpec.Option; its declared order and identity are preserved.
         */
        DOWNLOAD,

        /** SFTP operations.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISshSpec.Option#SFTP policy choice: SFTP operations. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISshSpec.Option; its declared order and identity are preserved.
         */
        SFTP,

        /** Port forwarding (tunnel).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISshSpec.Option#TUNNEL policy choice: Port forwarding (tunnel). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISshSpec.Option; its declared order and identity are preserved.
         */
        TUNNEL,

        /** Interactive shell.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISshSpec.Option#SHELL policy choice: Interactive shell. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISshSpec.Option; its declared order and identity are preserved.
         */
        SHELL
    }

    // --- CONNECTION ---

    /**
     * SSH host (can be property reference like ${ssh.host}).
     *
     * @return host
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISshSpec#host intent: SSH host (can be property reference like ${ssh.host}). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String host() default "";

    /**
     * SSH port.
     *
     * @return port
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISshSpec#port intent: SSH port. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 22}.
     */
    int port() default 22;

    /**
     * SSH username (can be property reference).
     *
     * @return username
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISshSpec#user intent: SSH username (can be property reference). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code "${ssh.user}"}.
     */
    String user() default "${ssh.user}";

    // --- AUTHENTICATION ---

    /**
     * Authentication method.
     *
     * @return auth method
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISshSpec#auth intent: Authentication method. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code AuthMethod}; default expression {@code AuthMethod.PUBLIC_KEY}.
     */
    AuthMethod auth() default AuthMethod.PUBLIC_KEY;

    /** SSH authentication methods.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the ISshSpec.AuthMethod choices described as SSH authentication methods. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 5 constants in the existing source order.
     */
    enum AuthMethod {
        /** Public key authentication.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISshSpec.AuthMethod#PUBLIC_KEY policy choice: Public key authentication. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISshSpec.AuthMethod; its declared order and identity are preserved.
         */
        PUBLIC_KEY,
        /** Password authentication.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISshSpec.AuthMethod#PASSWORD policy choice: Password authentication. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISshSpec.AuthMethod; its declared order and identity are preserved.
         */
        PASSWORD,
        /** Keyboard-interactive.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISshSpec.AuthMethod#KEYBOARD_INTERACTIVE policy choice: Keyboard-interactive. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISshSpec.AuthMethod; its declared order and identity are preserved.
         */
        KEYBOARD_INTERACTIVE,
        /** Agent forwarding.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISshSpec.AuthMethod#AGENT policy choice: Agent forwarding. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISshSpec.AuthMethod; its declared order and identity are preserved.
         */
        AGENT,
        /** Certificate-based.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISshSpec.AuthMethod#CERTIFICATE policy choice: Certificate-based. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISshSpec.AuthMethod; its declared order and identity are preserved.
         */
        CERTIFICATE
    }

    /**
     * Path to private key file (for PUBLIC_KEY auth).
     *
     * @return key path
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISshSpec#keyPath intent: Path to private key file (for PUBLIC_KEY auth). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code "${ssh.key.path}"}.
     */
    String keyPath() default "${ssh.key.path}";

    /**
     * Key passphrase (property reference recommended).
     *
     * @return passphrase property
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISshSpec#passphrase intent: Key passphrase (property reference recommended). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code "${ssh.key.passphrase}"}.
     */
    String passphrase() default "${ssh.key.passphrase}";

    /**
     * Known hosts file path.
     *
     * @return known hosts path
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISshSpec#knownHosts intent: Known hosts file path. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code "${user.home}/.ssh/known_hosts"}.
     */
    String knownHosts() default "${user.home}/.ssh/known_hosts";

    /**
     * Whether to verify host key.
     *
     * @return true to verify
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISshSpec#verifyHostKey intent: Whether to verify host key. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
     */
    boolean verifyHostKey() default true;

    // --- OPERATION TYPE ---

    /**
     * SSH operation type.
     *
     * @return operation
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISshSpec#operation intent: SSH operation type. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Operation}; default expression {@code Operation.EXEC}.
     */
    Operation operation() default Operation.EXEC;

    /** SSH operations.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the ISshSpec.Operation choices described as SSH operations. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 6 constants in the existing source order.
     */
    enum Operation {
        /** Execute command.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISshSpec.Operation#EXEC policy choice: Execute command. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISshSpec.Operation; its declared order and identity are preserved.
         */
        EXEC,
        /** Upload file (SCP put).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISshSpec.Operation#UPLOAD policy choice: Upload file (SCP put). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISshSpec.Operation; its declared order and identity are preserved.
         */
        UPLOAD,
        /** Download file (SCP get).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISshSpec.Operation#DOWNLOAD policy choice: Download file (SCP get). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISshSpec.Operation; its declared order and identity are preserved.
         */
        DOWNLOAD,
        /** SFTP operations.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISshSpec.Operation#SFTP policy choice: SFTP operations. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISshSpec.Operation; its declared order and identity are preserved.
         */
        SFTP,
        /** Port forwarding.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISshSpec.Operation#TUNNEL policy choice: Port forwarding. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISshSpec.Operation; its declared order and identity are preserved.
         */
        TUNNEL,
        /** Interactive shell.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISshSpec.Operation#SHELL policy choice: Interactive shell. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISshSpec.Operation; its declared order and identity are preserved.
         */
        SHELL
    }

    // --- COMMAND EXECUTION ---

    /**
     * Command to execute.
     *
     * @return command
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISshSpec#command intent: Command to execute. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String command() default "";

    /**
     * Commands to execute in sequence.
     *
     * @return commands
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISshSpec#commands intent: Commands to execute in sequence. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] commands() default {};

    /**
     * Whether to run with sudo.
     *
     * @return true for sudo
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISshSpec#sudo intent: Whether to run with sudo. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean sudo() default false;

    /**
     * Working directory for command.
     *
     * @return directory
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISshSpec#workingDir intent: Working directory for command. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String workingDir() default "";

    /**
     * Environment variables as KEY=VALUE.
     *
     * @return env vars
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISshSpec#env intent: Environment variables as KEY=VALUE. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] env() default {};

    // --- FILE TRANSFER ---

    /**
     * Local file path (for UPLOAD/DOWNLOAD).
     *
     * @return local path
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISshSpec#localPath intent: Local file path (for UPLOAD/DOWNLOAD). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String localPath() default "";

    /**
     * Remote file path (for UPLOAD/DOWNLOAD).
     *
     * @return remote path
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISshSpec#remotePath intent: Remote file path (for UPLOAD/DOWNLOAD). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String remotePath() default "";

    /**
     * File permissions (octal, e.g., "644").
     *
     * @return permissions
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISshSpec#permissions intent: File permissions (octal, e.g., "644"). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String permissions() default "";

    /**
     * Whether to preserve timestamps.
     *
     * @return true to preserve
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISshSpec#preserveTimestamp intent: Whether to preserve timestamps. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean preserveTimestamp() default false;

    // --- TUNNEL / PORT FORWARDING ---

    /**
     * Local port for tunnel.
     *
     * @return local port
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISshSpec#localPort intent: Local port for tunnel. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 0}.
     */
    int localPort() default 0;

    /**
     * Remote host for tunnel.
     *
     * @return remote host
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISshSpec#remoteHost intent: Remote host for tunnel. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code "localhost"}.
     */
    String remoteHost() default "localhost";

    /**
     * Remote port for tunnel.
     *
     * @return remote port
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISshSpec#remotePort intent: Remote port for tunnel. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 0}.
     */
    int remotePort() default 0;

    // --- JUMP HOST (BASTION) ---

    /**
     * Jump host (bastion) for ProxyJump.
     *
     * @return jump host
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISshSpec#jumpHost intent: Jump host (bastion) for ProxyJump. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String jumpHost() default "";

    /**
     * Jump host port.
     *
     * @return jump port
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISshSpec#jumpPort intent: Jump host port. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 22}.
     */
    int jumpPort() default 22;

    /**
     * Jump host user.
     *
     * @return jump user
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISshSpec#jumpUser intent: Jump host user. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code "${ssh.user}"}.
     */
    String jumpUser() default "${ssh.user}";

    // --- TIMEOUT ---

    /**
     * Connection timeout in milliseconds.
     *
     * @return connect timeout
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISshSpec#connectTimeoutMs intent: Connection timeout in milliseconds. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 30000}.
     */
    int connectTimeoutMs() default 30000;

    /**
     * Command execution timeout in milliseconds. 0 = no timeout.
     *
     * @return exec timeout
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISshSpec#execTimeoutMs intent: Command execution timeout in milliseconds. 0 = no timeout. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 60000}.
     */
    int execTimeoutMs() default 60000;

    // --- RETRY ---

    /**
     * Maximum connection retry attempts.
     *
     * @return max retries
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISshSpec#maxRetries intent: Maximum connection retry attempts. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 3}.
     */
    int maxRetries() default 3;

    /**
     * Retry delay in milliseconds.
     *
     * @return retry delay
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISshSpec#retryDelayMs intent: Retry delay in milliseconds. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 5000}.
     */
    int retryDelayMs() default 5000;

    // --- OUTPUT ---

    /**
     * Output handling.
     *
     * @return output type
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISshSpec#output intent: Output handling. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code OutputType}; default expression {@code OutputType.STRING}.
     */
    OutputType output() default OutputType.STRING;

    /** Output types.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the ISshSpec.OutputType choices described as Output types. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 6 constants in the existing source order.
     */
    enum OutputType {
        /** Return stdout as String.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISshSpec.OutputType#STRING policy choice: Return stdout as String. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISshSpec.OutputType; its declared order and identity are preserved.
         */
        STRING,
        /** Return stdout as lines (List&lt;String>).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISshSpec.OutputType#LINES policy choice: Return stdout as lines (List&lt;String>). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISshSpec.OutputType; its declared order and identity are preserved.
         */
        LINES,
        /** Return stdout as bytes.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISshSpec.OutputType#BYTES policy choice: Return stdout as bytes. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISshSpec.OutputType; its declared order and identity are preserved.
         */
        BYTES,
        /** Stream output (InputStream).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISshSpec.OutputType#STREAM policy choice: Stream output (InputStream). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISshSpec.OutputType; its declared order and identity are preserved.
         */
        STREAM,
        /** Return exit code only.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISshSpec.OutputType#EXIT_CODE policy choice: Return exit code only. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISshSpec.OutputType; its declared order and identity are preserved.
         */
        EXIT_CODE,
        /** Return full result object.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ISshSpec.OutputType#FULL policy choice: Return full result object. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ISshSpec.OutputType; its declared order and identity are preserved.
         */
        FULL
    }

    /**
     * Whether to capture stderr.
     *
     * @return true to capture
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISshSpec#captureStderr intent: Whether to capture stderr. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
     */
    boolean captureStderr() default true;

    /**
     * Expected exit code (0 = success).
     *
     * @return expected exit code
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISshSpec#expectedExitCode intent: Expected exit code (0 = success). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 0}.
     */
    int expectedExitCode() default 0;

    // --- LOGGING ---

    /**
     * Whether to log commands (security consideration).
     *
     * @return true to log
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISshSpec#logCommands intent: Whether to log commands (security consideration). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
     */
    boolean logCommands() default true;

    /**
     * Whether command output is sensitive.
     *
     * @return true if sensitive
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISshSpec#sensitiveOutput intent: Whether command output is sensitive. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean sensitiveOutput() default false;

    // --- MODEL ---

    /**
     * Preferred model for generation.
     *
     * @return model type
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ISshSpec#model intent: Preferred model for generation. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Model.ModelType}; default expression {@code Model.ModelType.SONNET}.
     */
    Model.ModelType model() default Model.ModelType.SONNET;
}
