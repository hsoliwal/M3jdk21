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
import java.util.Objects;

/**
 * Shared Chrome2api/OpenAI-compatible provider definition; no Chrome binary is embedded.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/chrome/Chrome2ApiProvider.java}
 * blob {@code 9caf8376cd934ef03d36c42b25691a1c318322d8} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class Chrome2ApiProvider {
    public static final URI DEFAULT_ENDPOINT =
            URI.create("http://127.0.0.1:11435/v1");
    public static final String DEFAULT_MODEL = "chrome-gemini-nano";

    private Chrome2ApiProvider() {
    }

    public static ProviderDefinition local() {
        return local(DEFAULT_ENDPOINT);
    }

    public static ProviderDefinition local(URI endpoint) {
        Objects.requireNonNull(endpoint, "endpoint");
        String host = endpoint.getHost() == null
                ? "local"
                : endpoint.getHost().toLowerCase(java.util.Locale.ROOT);
        int port = endpoint.getPort() < 0
                ? ("https".equalsIgnoreCase(endpoint.getScheme()) ? 443 : 80)
                : endpoint.getPort();
        return local("chrome2api." + host + ":" + port, endpoint);
    }

    public static ProviderDefinition local(String key, URI endpoint) {
        Objects.requireNonNull(endpoint, "endpoint");
        int nativeCapabilities = Capability.mask(
                Capability.PROMPT,
                Capability.TEXT_INPUT,
                Capability.IMAGE_INPUT,
                Capability.AUDIO_INPUT,
                Capability.STREAM,
                Capability.TEMPERATURE,
                Capability.TOP_K,
                Capability.CONTEXT_TOKENS,
                Capability.MAX_OUTPUT_TOKENS,
                Capability.STATELESS_MESSAGES,
                Capability.LOCAL_MEDIA_ONLY,
                Capability.ON_DEVICE,
                Capability.SSE,
                Capability.MODEL_DISCOVERY);
        int synthesizedCapabilities = Capability.mask(
                Capability.SUMMARIZE,
                Capability.WRITE,
                Capability.REWRITE,
                Capability.PROOFREAD,
                Capability.TRANSLATE,
                Capability.LANGUAGE_DETECT);
        ProviderDefinition definition = new ProviderDefinition(
                key,
                endpoint,
                DEFAULT_MODEL,
                nativeCapabilities,
                synthesizedCapabilities,
                100,
                1,
                10 * 1024 * 1024,
                16 * 1024 * 1024,
                Duration.ofSeconds(120));
        String numericHost = endpoint.getHost();
        if (!("127.0.0.1".equals(numericHost)
                || "::1".equals(numericHost)
                || "0:0:0:0:0:0:0:1".equals(numericHost))) {
            throw new IllegalArgumentException(
                    "Chrome2api local endpoint must use a numeric loopback address");
        }
        return definition;
    }
}
