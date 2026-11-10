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

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Heuristic structural shape extracted from toString output.
 *
 * <p>This is never identity/equality authority. It exists only as an optional cold-path hint for
 * opaque third-party DTOs when explicit field/record metadata is insufficient.</p>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextToStringShape.java}
 * blob {@code e7fe65834ec0091fca595be0797b219b1a2419c1} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class ToStringShape {
    private static final Pattern JSON_KEY =
            Pattern.compile("\"(?:\\\\.|[^\"\\\\])*\"\\s*:");
    private static final Pattern QUOTED =
            Pattern.compile("\"(?:\\\\.|[^\"\\\\])*\"");
    private static final Pattern NUMBER =
            Pattern.compile("[-+]?\\d+(?:\\.\\d+)?(?:[eE][-+]?\\d+)?");
    private static final Pattern ASSIGNMENT_KEY =
            Pattern.compile("[\\p{L}_$][\\p{L}\\p{N}_$]*(?=\\s*=)");
    private static final Pattern WORD =
            Pattern.compile("[\\p{L}_$][\\p{L}\\p{N}_$]*");
    private static final Pattern SPACE =
            Pattern.compile("\\s+");

    private ToStringShape() { }

    public static String sample(Object value, int maxChars) {
        Objects.requireNonNull(value, "value");
        if (maxChars < 0) {
            throw new IllegalArgumentException("maxChars");
        }
        String rendered;
        try {
            rendered = String.valueOf(value);
        } catch (RuntimeException failure) {
            return "<toString-failed:" + failure.getClass().getName() + ">";
        }
        if (rendered.length() > maxChars) {
            rendered = rendered.substring(0, maxChars);
        }
        return shape(rendered);
    }

    public static String shape(CharSequence input) {
        String value = Objects.requireNonNull(input, "input").toString();
        value = JSON_KEY.matcher(value).replaceAll("\"k\":");
        value = QUOTED.matcher(value).replaceAll("\"*\"");
        value = NUMBER.matcher(value).replaceAll("#");
        value = ASSIGNMENT_KEY.matcher(value).replaceAll("k");
        value = WORD.matcher(value).replaceAll("@");
        return SPACE.matcher(value).replaceAll(" ").strip();
    }
}
