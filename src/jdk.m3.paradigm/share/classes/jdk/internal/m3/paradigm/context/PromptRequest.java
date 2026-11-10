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

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable prompt request; media are explicit local Paths, never remote URLs.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/capability/ContextPromptRequest.java}
 * blob {@code f72e00190f9659b750fbc8e36f35953d571d9d7e} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public record PromptRequest(
        List<Message> messages,
        List<Path> images,
        Path audio,
        Integer maxOutputTokens,
        Double temperature,
        Integer topK,
        Integer contextTokens,
        boolean stream) {

    private static final Set<String> ROLES =
            Set.of("system", "user", "assistant", "tool");

    public PromptRequest {
        messages = List.copyOf(Objects.requireNonNull(messages, "messages"));
        images = List.copyOf(Objects.requireNonNull(images, "images"));
        if (messages.isEmpty()) {
            throw new IllegalArgumentException("messages must not be empty");
        }
        images.forEach(path -> Objects.requireNonNull(path, "image"));
        positive(maxOutputTokens, "maxOutputTokens");
        positive(topK, "topK");
        positive(contextTokens, "contextTokens");
        if (temperature != null && !Double.isFinite(temperature)) {
            throw new IllegalArgumentException("temperature must be finite");
        }
    }

    public static PromptRequest user(String text) {
        return new PromptRequest(
                List.of(new Message("user", text)),
                List.of(),
                null,
                null,
                null,
                null,
                null,
                false);
    }

    public int requiredCapabilities() {
        int required = Capability.mask(
                Capability.PROMPT, Capability.TEXT_INPUT);
        if (!images.isEmpty()) {
            required |= Capability.IMAGE_INPUT.mask();
        }
        if (audio != null) {
            required |= Capability.AUDIO_INPUT.mask();
        }
        if (stream) {
            required |= Capability.mask(
                    Capability.STREAM, Capability.SSE);
        }
        if (maxOutputTokens != null) {
            required |= Capability.MAX_OUTPUT_TOKENS.mask();
        }
        if (temperature != null) {
            required |= Capability.TEMPERATURE.mask();
        }
        if (topK != null) {
            required |= Capability.TOP_K.mask();
        }
        if (contextTokens != null) {
            required |= Capability.CONTEXT_TOKENS.mask();
        }
        if (messages.size() > 1) {
            required |= Capability.STATELESS_MESSAGES.mask();
        }
        return required;
    }

    public record Message(String role, String text) {
        public Message {
            role = Objects.requireNonNull(role, "role")
                    .strip()
                    .toLowerCase(java.util.Locale.ROOT);
            text = Objects.requireNonNull(text, "text");
            if (!ROLES.contains(role)) {
                throw new IllegalArgumentException(
                        "unsupported message role: " + role);
            }
        }
    }

    private static void positive(Integer value, String name) {
        if (value != null && value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }
}
