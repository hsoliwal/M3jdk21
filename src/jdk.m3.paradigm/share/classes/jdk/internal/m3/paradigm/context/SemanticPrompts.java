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

import java.util.List;
import java.util.Objects;

/**
 * Deterministic semantic-to-prompt compilation for providers that only expose PROMPT.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/capability/ContextSemanticPrompts.java}
 * blob {@code 694fccb0caf6c6390f40ffbb796ac825b6b03475} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class SemanticPrompts {
    private SemanticPrompts() {
    }

    public static PromptRequest compile(
            SemanticOperation operation,
            String input,
            String instruction,
            String targetLanguage) {
        Objects.requireNonNull(operation, "operation");
        input = Objects.requireNonNull(input, "input");
        String extra = instruction == null ? "" : instruction.strip();
        String prompt = switch (operation) {
            case PROMPT -> input;
            case SUMMARIZE ->
                    "Summarize the following text faithfully and concisely."
                            + suffix(extra) + "\n\nTEXT:\n" + input;
            case WRITE ->
                    "Write polished text from the following material. Preserve factual content."
                            + suffix(extra) + "\n\nMATERIAL:\n" + input;
            case REWRITE ->
                    "Rewrite the following text while preserving its meaning."
                            + suffix(extra) + "\n\nTEXT:\n" + input;
            case PROOFREAD ->
                    "Proofread the following text. Correct grammar, spelling and punctuation"
                            + " without changing meaning."
                            + suffix(extra) + "\n\nTEXT:\n" + input;
            case TRANSLATE ->
                    "Translate the following text to "
                            + required(targetLanguage, "targetLanguage")
                            + ". Return only the translation."
                            + suffix(extra) + "\n\nTEXT:\n" + input;
            case LANGUAGE_DETECT ->
                    "Identify the primary language of the following text."
                            + " Return only the language name."
                            + suffix(extra) + "\n\nTEXT:\n" + input;
        };
        return new PromptRequest(
                List.of(new PromptRequest.Message("user", prompt)),
                List.of(),
                null,
                null,
                null,
                null,
                null,
                false);
    }

    private static String suffix(String instruction) {
        return instruction.isEmpty()
                ? ""
                : " Additional instruction: " + instruction;
    }

    private static String required(String value, String name) {
        String text = Objects.requireNonNull(value, name).strip();
        if (text.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return text;
    }
}
