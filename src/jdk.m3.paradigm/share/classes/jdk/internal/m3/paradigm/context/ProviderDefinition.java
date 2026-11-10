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
package jdk.internal.m3.paradigm.context;

import java.net.URI;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;

/**
 * Shared immutable provider metadata. One definition may serve arbitrarily many contexts.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/capability/ContextProviderDefinition.java}
 * blob {@code 4ee2055bf403755a49921fb69514b54e722bbe69} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public record ProviderDefinition(
        String key,
        URI endpoint,
        String model,
        int nativeCapabilities,
        int synthesizedCapabilities,
        int priority,
        int maxConcurrency,
        int maxRequestBytes,
        int maxResponseBytes,
        Duration timeout) {

    private static final int MAX_IO_BYTES = 64 * 1024 * 1024;

    public ProviderDefinition {
        key = requireToken(key, "key");
        model = requireText(model, "model");
        endpoint = Objects.requireNonNull(endpoint, "endpoint");
        String scheme = endpoint.getScheme();
        if (!endpoint.isAbsolute() || scheme == null
                || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
                || endpoint.getQuery() != null || endpoint.getFragment() != null) {
            throw new IllegalArgumentException(
                    "endpoint must be an absolute http(s) URI without query/fragment");
        }
        if (priority < 0 || maxConcurrency < 1 || maxConcurrency > 1024) {
            throw new IllegalArgumentException("invalid priority or maxConcurrency");
        }
        if (maxRequestBytes < 1 || maxRequestBytes > MAX_IO_BYTES
                || maxResponseBytes < 1 || maxResponseBytes > MAX_IO_BYTES) {
            throw new IllegalArgumentException("request/response limit outside supported range");
        }
        timeout = Objects.requireNonNull(timeout, "timeout");
        if (timeout.isZero() || timeout.isNegative()
                || timeout.compareTo(Duration.ofMinutes(10)) > 0) {
            throw new IllegalArgumentException("timeout must be in (0, 10 minutes]");
        }
    }

    public int effectiveCapabilities(boolean allowSynthesis) {
        return allowSynthesis
                ? nativeCapabilities | synthesizedCapabilities
                : nativeCapabilities;
    }

    public boolean supports(int requiredCapabilities, boolean allowSynthesis) {
        return Capability.containsAll(
                effectiveCapabilities(allowSynthesis), requiredCapabilities);
    }

    public boolean isLoopback() {
        String host = endpoint.getHost();
        return host != null && (host.equalsIgnoreCase("localhost")
                || host.equals("127.0.0.1")
                || host.equals("::1")
                || host.equals("0:0:0:0:0:0:0:1"));
    }

    private static String requireToken(String value, String name) {
        String text = requireText(value, name).toLowerCase(Locale.ROOT);
        if (!text.matches("[a-z0-9][a-z0-9._:-]{0,127}")) {
            throw new IllegalArgumentException(name + " contains unsupported characters");
        }
        return text;
    }

    private static String requireText(String value, String name) {
        String text = Objects.requireNonNull(value, name).strip();
        if (text.isEmpty() || text.length() > 512) {
            throw new IllegalArgumentException(name + " must contain 1..512 characters");
        }
        return text;
    }
}
