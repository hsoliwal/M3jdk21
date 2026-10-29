// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
// Indexed evidence reuses the existing canonical spelling; views materialize only on access.
package com.m3.llm.chrome;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

/** Immutable, bounded projection of an existing COP snapshot; not another context owner.
 * No parent traversal, reflection, file access, OAuth, or Google-app discovery occurs here.
 */
public final class ChromeMlContextPacket {
    private static final Pattern FIELD_PATTERN =
            Pattern.compile("[A-Za-z_][A-Za-z0-9_.-]{0,127}");

    public enum Source { HOST, GMAIL, DRIVE, DOCS, CALENDAR, KEEP, TASKS }

    /** Identifies data already read by an authorized host connector. Not an OAuth grant. */
    public record EvidenceKey(Source source, String id) implements Comparable<EvidenceKey> {
        public EvidenceKey {
            Objects.requireNonNull(source, "source");
            id = checkedText(id, 512);
            if (id.isBlank()) throw new IllegalArgumentException("Empty evidence identity");
        }
        @Override public int compareTo(EvidenceKey other) {
            int order = source.compareTo(other.source);
            return order == 0 ? id.compareTo(other.id) : order;
        }
        @Override public String toString() { return "EvidenceKey[source=" + source + ", id=REDACTED]"; }
    }

    public record Evidence(EvidenceKey key, String excerpt) {
        public Evidence { Objects.requireNonNull(key, "key"); excerpt = checkedText(excerpt, 4096); }
        @Override public String toString() { return "Evidence[source=" + key.source() + ", excerpt=REDACTED]"; }
    }

    private final String contextId;
    private final long revision;
    private final Set<String> fields;
    private final Set<EvidenceKey> evidenceKeys;
    private final String encoded;
    private final String root;
    private final EvidenceKey[] evidenceIndex;
    private final int[] excerptBounds;
    private final int contextEnd;

    private ChromeMlContextPacket(String contextId, long revision, TreeSet<String> fields,
                                  Set<EvidenceKey> evidenceKeys, String encoded,
                                  EvidenceKey[] evidenceIndex, int[] excerptBounds, int contextEnd) {
        this.contextId = contextId;
        this.revision = revision;
        // fields is built entirely inside capture; no caller retains its mutable owner.
        this.fields = fields.isEmpty() ? Set.of() : Collections.unmodifiableSet(fields);
        this.evidenceKeys = evidenceKeys.isEmpty()
                ? Set.of() : Collections.unmodifiableSet(new TreeSet<>(evidenceKeys));
        this.encoded = encoded;
        this.root = hash(encoded);
        // These bounded arrays are constructed privately by capture and never escape.
        this.evidenceIndex = evidenceIndex;
        this.excerptBounds = excerptBounds;
        this.contextEnd = contextEnd;
    }

    /**
     * Only allowlisted keys are looked up. Unselected values are not visited or serialized.
     * Keys are exact/case-sensitive: use COP's canonical keys. Missing is distinct from null.
     * The source must be a host-captured stable snapshot; live concurrent maps are not snapshots.
     */
    public static ChromeMlContextPacket capture(String contextId, long revision, Map<String, ?> snapshot,
                                                 Set<String> readable, List<Evidence> evidence) {
        String id = checkedText(contextId, 512);
        if (id.isBlank() || revision < 0) throw new IllegalArgumentException("Invalid context identity/revision");
        Objects.requireNonNull(snapshot, "snapshot");
        if (Objects.requireNonNull(readable, "readable").size() > 128
                || Objects.requireNonNull(evidence, "evidence").size() > 16)
            throw new IllegalArgumentException("Projection count bound");
        TreeSet<String> fields = new TreeSet<>();
        // Do not hand a callback retaining fields to a caller-owned Set implementation.
        // The only mutable field-set owner can therefore be transferred to the private ctor.
        for (String key : readable) {
            if (key == null || !FIELD_PATTERN.matcher(key).matches())
                throw new IllegalArgumentException("Invalid projection key");
            fields.add(key);
        }
        Map<EvidenceKey, Evidence> documents = new TreeMap<>();
        List<Evidence> captured = List.copyOf(evidence);
        if (captured.size() > 16) throw new IllegalArgumentException("Projection count bound");
        captured.forEach(item -> {
            if (documents.putIfAbsent(item.key(), item) != null)
                throw new IllegalArgumentException("Duplicate evidence identity");
        });
        StringBuilder out = new StringBuilder();
        field(out, "cop.chromeml.projection.v1"); field(out, id); field(out, Long.toString(revision));
        field(out, Integer.toString(fields.size()));
        for (String key : fields) {
            field(out, key);
            if (!snapshot.containsKey(key)) field(out, "absent");
            else field(out, scalar(snapshot.get(key)));
        }
        int contextEnd = out.length();
        field(out, Integer.toString(documents.size()));
        EvidenceKey[] index = documents.keySet().toArray(EvidenceKey[]::new);
        int[] bounds = new int[index.length * 2];
        for (int ordinal = 0; ordinal < index.length; ordinal++) {
            EvidenceKey key = index[ordinal];
            String excerpt = documents.get(key).excerpt();
            field(out, key.source().name()); field(out, key.id());
            int before = out.length();
            field(out, excerpt);
            bounds[ordinal * 2] = before + Integer.toString(excerpt.length()).length() + 1;
            bounds[ordinal * 2 + 1] = out.length() - 1;
        }
        return new ChromeMlContextPacket(id, revision, fields, documents.keySet(), out.toString(),
                index, bounds, contextEnd);
    }

    public String contextId() { return contextId; }
    public long revision() { return revision; }
    public Set<String> fields() { return fields; }
    public Set<EvidenceKey> evidenceKeys() { return evidenceKeys; }
    /** Content checksum, not a signature, permission grant or safe-to-publish secret hash. */
    public String root() { return root; }
    String encoded() { return encoded; }
    @Override public String toString() {
        return "ChromeMlContextPacket[revision=" + revision + ", fields=" + fields.size()
                + ", evidence=" + evidenceKeys.size() + ", content=REDACTED]";
    }

    int evidenceCount() { return evidenceIndex.length; }

    Evidence evidenceAt(int ordinal) {
        Objects.checkIndex(ordinal, evidenceIndex.length);
        return new Evidence(evidenceIndex[ordinal],
                encoded.substring(excerptBounds[ordinal * 2], excerptBounds[ordinal * 2 + 1]));
    }

    int evidenceBytesAt(int ordinal) {
        Objects.checkIndex(ordinal, evidenceIndex.length);
        EvidenceKey key = evidenceIndex[ordinal];
        return utf8Bytes(key.source().name(), 0, key.source().name().length())
                + utf8Bytes(key.id(), 0, key.id().length())
                + utf8Bytes(encoded, excerptBounds[ordinal * 2], excerptBounds[ordinal * 2 + 1]);
    }

    String referenceEncoded() {
        StringBuilder out = new StringBuilder();
        field(out, "cop.chromeml.reference-data.v1");
        field(out, "original-context-prefix");
        fieldSlice(out, encoded, 0, contextEnd);
        field(out, "indexed-evidence");
        field(out, Integer.toString(evidenceIndex.length));
        for (int i = 0; i < evidenceIndex.length; i++) {
            field(out, Integer.toString(i));
            field(out, evidenceIndex[i].source().name());
            fieldSlice(out, encoded, excerptBounds[i * 2], excerptBounds[i * 2 + 1]);
        }
        return out.toString();
    }

    private static void fieldSlice(StringBuilder out, String text, int start, int end) {
        int length = end - start;
        if ((long) out.length() + length + 16 > 16_384)
            throw new IllegalArgumentException("Combined projection bound");
        out.append(length).append(':').append(text, start, end).append(';');
    }

    // Every input/range here was checked as complete, well-formed UTF-16 at capture.
    private static int utf8Bytes(String text, int start, int end) {
        int bytes = 0;
        for (int i = start; i < end; i++) {
            char value = text.charAt(i);
            if (value < 0x80) bytes++;
            else if (value < 0x800) bytes += 2;
            else if (Character.isHighSurrogate(value)) { bytes += 4; i++; }
            else bytes += 3;
        }
        return bytes;
    }

    private static String scalar(Object value) {
        if (value == null) return "null";
        if (value instanceof String text) return "text:" + checkedText(text, 4096);
        if (value instanceof Boolean) return "boolean:" + value;
        if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long)
            return value.getClass().getSimpleName() + ":" + value;
        if (value instanceof Float number && Float.isFinite(number)) return "float:" + number;
        if (value instanceof Double number && Double.isFinite(number)) return "double:" + number;
        if (value != null && value.getClass() == BigInteger.class) {
            BigInteger integer = (BigInteger) value;
            if (integer.bitLength() > 426) throw new IllegalArgumentException("Numeric projection bound");
            return "integer:" + integer;
        }
        if (value != null && value.getClass() == BigDecimal.class) {
            BigDecimal number = (BigDecimal) value;
            if (number.precision() > 128 || Math.abs((long) number.scale()) > 256)
                throw new IllegalArgumentException("Numeric projection bound");
            return "decimal:" + number;
        }
        throw new IllegalArgumentException("Projection requires immutable finite scalars");
    }

    static String checkedText(String text, int maximum) {
        Objects.requireNonNull(text, "text");
        if (text.length() > maximum || text.indexOf('\0') >= 0)
            throw new IllegalArgumentException("Text bound or NUL");
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isHighSurrogate(c)) {
                if (++i >= text.length() || !Character.isLowSurrogate(text.charAt(i)))
                    throw new IllegalArgumentException("Invalid Unicode");
            } else if (Character.isLowSurrogate(c)) throw new IllegalArgumentException("Invalid Unicode");
        }
        return text;
    }

    static String hash(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    static void field(StringBuilder out, String text) {
        // UTF-16 framing matches Java String lengths. Total bound is tested before each append.
        if ((long) out.length() + text.length() + 16 > 16_384)
            throw new IllegalArgumentException("Combined projection bound");
        out.append(text.length()).append(':').append(text).append(';');
    }
}
