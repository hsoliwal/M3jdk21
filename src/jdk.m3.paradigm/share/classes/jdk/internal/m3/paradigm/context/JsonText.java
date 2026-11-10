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

/**
 * JSON scalar encoding only. SQLite and explicit application codecs own parsing/document structure.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/stream/ContextJsonText.java}
 * blob {@code 9c5c78f6ff33dfdf468d37783b1cd8751df1001d} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class JsonText {
    private JsonText() { }
    public static String quote(String text) {
        Objects.requireNonNull(text);
        StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) out.append(String.format(java.util.Locale.ROOT, "\\u%04x", (int) c));
                    else if (Character.isHighSurrogate(c)) {
                        if (i + 1 >= text.length() || !Character.isLowSurrogate(text.charAt(i + 1)))
                            throw new IllegalArgumentException("Unpaired UTF-16 surrogate");
                        out.append(c).append(text.charAt(++i));
                    } else if (Character.isLowSurrogate(c)) throw new IllegalArgumentException("Unpaired UTF-16 surrogate");
                    else out.append(c);
                }
            }
        }
        return out.append('"').toString();
    }
    public static String scalar(Object value) {
        if (value == null) return "null";
        if (value instanceof String text) return quote(text);
        if (value instanceof Boolean || value instanceof Byte || value instanceof Short
                || value instanceof Integer || value instanceof Long) return value.toString();
        if (value instanceof Double d && Double.isFinite(d)) return d.toString();
        if (value instanceof Float f && Float.isFinite(f)) return f.toString();
        throw new IllegalArgumentException("Unsupported query scalar");
    }
}
