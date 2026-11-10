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

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Dependency-free Unicode normalization, tokenization and conservative English stemming.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextLexicalSemanticProvider.java}
 * blob {@code de7cfba9d421d525e595cf72dbdd417b8092f5a8} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class LexicalSemanticProvider implements SemanticProvider {
    public static final LexicalSemanticProvider INSTANCE =
            new LexicalSemanticProvider();

    private LexicalSemanticProvider() {
    }

    @Override
    public String name() {
        return "cop-lexical";
    }

    @Override
    public SemanticFeatures analyze(String text) {
        String normalized = normalize(text);
        List<String> tokens = tokenize(normalized);
        List<String> stems = tokens.stream()
                .map(LexicalSemanticProvider::stem)
                .toList();
        return new SemanticFeatures(
                normalized,
                tokens,
                stems,
                String.join(" ", stems),
                fingerprint(normalized));
    }

    public static String normalize(String text) {
        if (text == null || text.isBlank()) return "";
        return Normalizer.normalize(text, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .strip();
    }

    public static List<String> tokenize(String text) {
        ArrayList<String> tokens = new ArrayList<>();
        StringBuilder token = new StringBuilder();
        text.codePoints().forEach(codePoint -> {
            if (Character.isLetterOrDigit(codePoint)) {
                token.appendCodePoint(codePoint);
            } else if (token.length() > 0) {
                tokens.add(token.toString());
                token.setLength(0);
            }
        });
        if (token.length() > 0) tokens.add(token.toString());
        return List.copyOf(tokens);
    }

    public static String stem(String token) {
        if (!token.chars().allMatch(value -> value < 128)) return token;
        if (token.length() > 5 && token.endsWith("ing"))
            return token.substring(0, token.length() - 3);
        if (token.length() > 4 && token.endsWith("ed"))
            return token.substring(0, token.length() - 2);
        if (token.length() > 4 && token.endsWith("es"))
            return token.substring(0, token.length() - 2);
        if (token.length() > 3 && token.endsWith("s"))
            return token.substring(0, token.length() - 1);
        return token;
    }

    private static long fingerprint(String text) {
        long hash = 0xcbf29ce484222325L;
        for (int index = 0; index < text.length(); index++) {
            hash ^= text.charAt(index);
            hash *= 0x100000001b3L;
        }
        return hash;
    }
}
