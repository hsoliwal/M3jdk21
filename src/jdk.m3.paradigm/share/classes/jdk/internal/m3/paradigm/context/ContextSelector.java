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

import java.util.Locale;
import java.util.Objects;

/**
 * Detached inspection selector. The two existing grammars remain explicit:
 * AT_QUERY uses @property/raw-key; POINTER uses property/JSON-Pointer-escaped-key.
 * A selector is data, not permission. Its dialect must match the consuming API.
 * No ordinary context retains this value or any new field.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextSelector.java}
 * blob {@code e9a1542d083e310504206162b13cbf4bbe318c8e} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public record ContextSelector(Kind kind, String key, Dialect dialect)
        implements Comparable<ContextSelector> {
    public enum Dialect { AT_QUERY, POINTER }

    public enum Kind {
        CONTEXT("context", false, 1),
        SNAPSHOT("snapshot", false, 1),
        PROPERTIES("properties", false, 3),
        PROPERTY("property", true, 3),
        SCALARS("scalars", false, 3),
        SCALAR("scalar", true, 3),
        PREFERENCES("preferences", false, 3),
        PREFERENCE("preference", true, 3),
        PATTERNS("patterns", false, 3),
        PATTERN("pattern", true, 3),
        CONCEPTS("concepts", false, 3),
        ROLES("roles", false, 3),
        AUTHORITY("authority", false, 1),
        INVARIANTS("invariants", false, 3),
        CONTRACTS("contracts", false, 3),
        METHODS("methods", false, 3),
        METHOD("method", true, 1),
        RULES("rules", false, 1),
        COMPONENTS("components", false, 3),
        ID("id", false, 2),
        UID("uid", false, 2),
        TYPE("type", false, 2),
        REVISION("revision", false, 2),
        DURABLE_REVISION("durable-revision", false, 2),
        MODE("mode", false, 2),
        PHASE("phase", false, 2),
        BASE_ID("base-id", false, 2),
        OMITTED_PROPERTIES("omitted-properties", false, 2),
        FACETS("facets", false, 2);

        private final String token;
        private final boolean named;
        private final int dialects;
        Kind(String token, boolean named, int dialects) {
            this.token = token;
            this.named = named;
            this.dialects = dialects;
        }
        public String token() { return token; }
        public boolean named() { return named; }
        public boolean supports(Dialect dialect) {
            return (dialects & (Objects.requireNonNull(dialect) == Dialect.AT_QUERY ? 1 : 2)) != 0;
        }
        static Kind parse(String token) {
            for (Kind candidate : values()) {
                if (candidate.token.equals(token)) return candidate;
            }
            throw new IllegalArgumentException("Unknown context selector kind");
        }
    }

    public static final int MAX_NAME_LENGTH = 4096;
    public static final int MAX_TEXT_LENGTH = 2 * MAX_NAME_LENGTH + 32;
    private static final int MAX_AT_KEY_LENGTH = 1024;

    /** Retains the previously public two-argument @-selector constructor. */
    public ContextSelector(Kind kind, String key) {
        this(kind, key, Dialect.AT_QUERY);
    }

    public ContextSelector {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(dialect, "dialect");
        if (!kind.supports(dialect)) throw new IllegalArgumentException("Selector kind is unavailable in this dialect");
        if (!kind.named()) {
            if (key != null) throw new IllegalArgumentException("Selector does not accept a key");
        } else {
            Objects.requireNonNull(key, "key");
            // Bound the original input before stripping or encoding a new String.
            if (key.length() > MAX_TEXT_LENGTH) throw new IllegalArgumentException("Selector key exceeds input bound");
            if (dialect == Dialect.AT_QUERY) key = key.strip();
            int limit = dialect == Dialect.AT_QUERY ? MAX_AT_KEY_LENGTH : MAX_NAME_LENGTH;
            if (key.isEmpty() || key.length() > limit || key.indexOf('\0') >= 0
                    || dialect == Dialect.AT_QUERY && key.indexOf('#') >= 0) {
                throw new IllegalArgumentException("Invalid selector key");
            }
            validUnicode(key);
        }
    }

    /** Pointer factories retain exact key case and whitespace. */
    public static ContextSelector of(Kind kind) { return new ContextSelector(kind, null, Dialect.POINTER); }
    public static ContextSelector named(Kind kind, String name) {
        return new ContextSelector(kind, name, Dialect.POINTER);
    }
    public boolean named() { return kind.named(); }
    public String name() {
        if (!named()) throw new IllegalStateException("Selector has no name");
        return key;
    }
    public void requireDialect(Dialect expected) {
        if (dialect != Objects.requireNonNull(expected)) {
            throw new IllegalArgumentException("Selector dialect mismatch");
        }
    }

    /** Parse either existing spelling without translating one grammar into the other. */
    public static ContextSelector parse(String value) {
        Objects.requireNonNull(value, "selector");
        if (value.isEmpty() || value.length() > MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException("Selector text exceeds bound or is empty");
        }
        String trimmed = value.strip();
        boolean at = trimmed.startsWith("@");
        String text = at ? trimmed.substring(1) : value;
        int slash = text.indexOf('/');
        String token = slash < 0 ? text : text.substring(0, slash);
        Kind kind = Kind.parse(at ? token.toLowerCase(Locale.ROOT) : token);
        if (slash < 0) {
            if (kind.named()) throw new IllegalArgumentException("Selector requires a key");
            return new ContextSelector(kind, null, at ? Dialect.AT_QUERY : Dialect.POINTER);
        }
        String tail = text.substring(slash + 1);
        if (at) return new ContextSelector(kind, tail);
        if (tail.indexOf('/') >= 0) throw new IllegalArgumentException("Pointer selector must escape '/' as ~1");
        return named(kind, unescape(tail));
    }

    @Override public int compareTo(ContextSelector other) {
        return toString().compareTo(Objects.requireNonNull(other, "other").toString());
    }
    @Override public String toString() {
        String prefix = dialect == Dialect.AT_QUERY ? "@" : "";
        if (!named()) return prefix + kind.token();
        return prefix + kind.token() + "/" + (dialect == Dialect.AT_QUERY ? key : escape(key));
    }
    private static String escape(String value) { return value.replace("~", "~0").replace("/", "~1"); }
    private static String unescape(String value) {
        int firstEscape = value.indexOf('~');
        if (firstEscape < 0) return value;
        StringBuilder decoded = new StringBuilder(value.length());
        decoded.append(value, 0, firstEscape);
        for (int i = firstEscape; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '~') {
                if (++i == value.length()) throw new IllegalArgumentException("Truncated pointer escape");
                c = switch (value.charAt(i)) {
                    case '0' -> '~';
                    case '1' -> '/';
                    default -> throw new IllegalArgumentException("Invalid pointer escape");
                };
            }
            decoded.append(c);
        }
        return decoded.toString();
    }
    private static void validUnicode(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isHighSurrogate(c)) {
                if (++i == value.length() || !Character.isLowSurrogate(value.charAt(i))) {
                    throw new IllegalArgumentException("Unpaired selector surrogate");
                }
            } else if (Character.isLowSurrogate(c)) throw new IllegalArgumentException("Unpaired selector surrogate");
        }
    }
}
