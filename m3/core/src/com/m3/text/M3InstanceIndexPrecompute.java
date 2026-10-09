/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.text;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Immutable M3JDK receiver for Synexia's concept-external instance index.
 *
 * <p>Proper names remain entity instances, not lexical token identities or
 * X-spine concepts. The operator supplies records and frozen concept
 * coordinates; this owner validates, copies, and exposes deterministic name
 * and reverse-concept views without bundling payload data.</p>
 */
public final class M3InstanceIndexPrecompute {
    private static final Comparator<String> UTF8_ORDER =
            M3InstanceIndexPrecompute::compareUtf8;

    private final String sourceId;
    private final String sourceRevision;
    private final String sourceFingerprint;
    private final String normalizationVersion;
    private final Set<Long> frozenConceptX;
    private final Map<String, InstanceRecord> recordsByName;
    private final Map<Long, Set<String>> namesByConceptX;

    public M3InstanceIndexPrecompute(
            String sourceId,
            String sourceRevision,
            String sourceFingerprint,
            String normalizationVersion,
            Set<Long> frozenConceptX,
            Map<String, InstanceRecord> recordsByName) {
        this.sourceId = text(sourceId, "sourceId");
        this.sourceRevision = text(sourceRevision, "sourceRevision");
        this.sourceFingerprint = text(sourceFingerprint, "sourceFingerprint");
        this.normalizationVersion = text(normalizationVersion, "normalizationVersion");
        this.frozenConceptX = immutableConcepts(frozenConceptX);

        TreeMap<String, InstanceRecord> byName = new TreeMap<>(UTF8_ORDER);
        TreeMap<Long, Set<String>> reverse = new TreeMap<>();
        Objects.requireNonNull(recordsByName, "recordsByName").forEach((key, record) -> {
            Objects.requireNonNull(key, "record name key");
            Objects.requireNonNull(record, "record");
            String normalizedKey = normalizedInstanceName(key);
            if (!normalizedKey.equals(normalizedInstanceName(record.nameUtf8()))) {
                throw new IllegalArgumentException("record key does not match normalized name");
            }
            if (!this.frozenConceptX.contains(record.instanceOfX())) {
                throw new IllegalArgumentException("instance_of_x is not frozen");
            }
            if (byName.putIfAbsent(normalizedKey, record) != null) {
                throw new IllegalArgumentException("duplicate normalized instance name");
            }
            reverse.computeIfAbsent(record.instanceOfX(), ignored -> new TreeSet<>(UTF8_ORDER))
                    .add(normalizedKey);
        });

        Map<Long, Set<String>> reverseCopy = new TreeMap<>();
        reverse.forEach((concept, names) ->
                reverseCopy.put(concept, Collections.unmodifiableSet(new TreeSet<>(names))));
        this.recordsByName = Collections.unmodifiableMap(byName);
        this.namesByConceptX = Collections.unmodifiableMap(reverseCopy);
    }

    public String sourceId() {
        return sourceId;
    }

    public String sourceRevision() {
        return sourceRevision;
    }

    public String sourceFingerprint() {
        return sourceFingerprint;
    }

    public String normalizationVersion() {
        return normalizationVersion;
    }

    public Set<Long> frozenConceptX() {
        return frozenConceptX;
    }

    public Map<String, InstanceRecord> recordsByName() {
        return recordsByName;
    }

    public Map<Long, Set<String>> namesByConceptX() {
        return namesByConceptX;
    }

    public int size() {
        return recordsByName.size();
    }

    public int referencedConceptCount() {
        return namesByConceptX.size();
    }

    public boolean appliesTo(
            String source,
            String revision,
            String fingerprint,
            String normalization) {
        return sourceId.equals(source)
                && sourceRevision.equals(revision)
                && sourceFingerprint.equals(fingerprint)
                && normalizationVersion.equals(normalization);
    }

    public InstanceRecord findByName(String name) {
        return recordsByName.get(normalizedInstanceName(name));
    }

    public List<InstanceRecord> instancesOf(long conceptX) {
        Set<String> names = namesByConceptX.get(conceptX);
        if (names == null) {
            return List.of();
        }
        List<InstanceRecord> result = new ArrayList<>(names.size());
        for (String name : names) {
            result.add(recordsByName.get(name));
        }
        return List.copyOf(result);
    }

    public static String normalizedInstanceName(String name) {
        String value = validateName(name, "name");
        StringBuilder result = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char item = value.charAt(index);
            result.append(item >= 'A' && item <= 'Z'
                    ? (char) (item - 'A' + 'a') : item);
        }
        return result.toString();
    }

    public enum InstanceType {
        BOOK_TITLE,
        AUTHOR,
        PERSON,
        PLACE,
        DATE,
        CHARACTER,
        ORGANIZATION,
        EVENT
    }

    public record InstanceRecord(
            String nameUtf8,
            InstanceType type,
            long instanceOfX,
            Map<String, String> metadata) {
        public InstanceRecord {
            nameUtf8 = validateName(nameUtf8, "name");
            type = Objects.requireNonNull(type, "type");
            if (instanceOfX <= 0L) {
                throw new IllegalArgumentException("instance_of_x must be positive");
            }
            metadata = immutableMetadata(metadata);
        }
    }

    private static Set<Long> immutableConcepts(Set<Long> input) {
        Objects.requireNonNull(input, "frozenConceptX");
        TreeSet<Long> copy = new TreeSet<>();
        for (Long value : input) {
            if (value == null || value <= 0L || !copy.add(value)) {
                throw new IllegalArgumentException("frozen concept X values must be unique positives");
            }
        }
        return Collections.unmodifiableSet(copy);
    }

    private static Map<String, String> immutableMetadata(Map<String, String> input) {
        Objects.requireNonNull(input, "metadata");
        TreeMap<String, String> copy = new TreeMap<>(UTF8_ORDER);
        input.forEach((key, value) -> {
            String validatedKey = validateName(key, "metadata key");
            if (value == null) {
                throw new IllegalArgumentException("metadata value is null");
            }
            validateUtf8(value, "metadata value");
            if (copy.putIfAbsent(validatedKey, value) != null) {
                throw new IllegalArgumentException("duplicate metadata key");
            }
        });
        return Collections.unmodifiableMap(copy);
    }

    private static String text(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is blank");
        }
        return value;
    }

    private static String validateName(String value, String field) {
        if (value == null || value.isEmpty()
                || value.chars().allMatch(M3InstanceIndexPrecompute::asciiWhitespace)) {
            throw new IllegalArgumentException(field + " must be nonblank UTF-8");
        }
        validateUtf8(value, field);
        for (int index = 0; index < value.length(); index++) {
            if (value.charAt(index) < 0x20) {
                throw new IllegalArgumentException(field + " contains an ASCII control");
            }
        }
        return value;
    }

    private static boolean asciiWhitespace(int value) {
        return value == ' ' || value == '\t' || value == '\r' || value == '\n';
    }

    private static void validateUtf8(String value, String field) {
        for (int index = 0; index < value.length(); index++) {
            char item = value.charAt(index);
            if (Character.isHighSurrogate(item)) {
                if (index + 1 >= value.length()
                        || !Character.isLowSurrogate(value.charAt(index + 1))) {
                    throw new IllegalArgumentException(field + " contains an unpaired surrogate");
                }
                index++;
            } else if (Character.isLowSurrogate(item)) {
                throw new IllegalArgumentException(field + " contains an unpaired surrogate");
            }
        }
    }

    private static int compareUtf8(String left, String right) {
        byte[] leftBytes = left.getBytes(StandardCharsets.UTF_8);
        byte[] rightBytes = right.getBytes(StandardCharsets.UTF_8);
        int limit = Math.min(leftBytes.length, rightBytes.length);
        for (int index = 0; index < limit; index++) {
            int leftValue = leftBytes[index] & 0xff;
            int rightValue = rightBytes[index] & 0xff;
            if (leftValue != rightValue) {
                return Integer.compare(leftValue, rightValue);
            }
        }
        return Integer.compare(leftBytes.length, rightBytes.length);
    }
}
