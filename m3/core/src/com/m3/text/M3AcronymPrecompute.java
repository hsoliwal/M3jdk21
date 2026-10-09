/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.text;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Immutable M3JDK owner for Synexia's canonical acronym projection.
 *
 * <p>The acronym and expansion strings remain source identities. Lookup maps
 * and UTF-16/hash facts are derived, immutable views; this class does not
 * create a second interner or replace the M3 string runtime.</p>
 */
public final class M3AcronymPrecompute {
    private static final Pattern ACRONYM = Pattern.compile("[A-Za-z][A-Za-z0-9+.-]{0,31}");
    private static final Pattern DOMAIN = Pattern.compile("[a-z][a-z0-9._-]*");

    private final String sourceId;
    private final String sourceRevision;
    private final String sourceFingerprint;
    private final List<Entry> entries;
    private final Map<String, Entry> byAcronym;
    private final Map<String, Entry> byExpansion;
    private final Map<String, List<String>> acronymsByDomain;

    public M3AcronymPrecompute(
            String sourceId,
            String sourceRevision,
            String sourceFingerprint,
            List<Entry> entries) {
        this.sourceId = text(sourceId, "sourceId");
        this.sourceRevision = text(sourceRevision, "sourceRevision");
        this.sourceFingerprint = text(sourceFingerprint, "sourceFingerprint");
        Objects.requireNonNull(entries, "entries");
        if (entries.isEmpty()) throw new IllegalArgumentException("acronym entries are empty");

        List<Entry> entryCopy = new ArrayList<>(entries.size());
        LinkedHashMap<String, Entry> acronymMap = new LinkedHashMap<>();
        TreeMap<String, Entry> expansionMap = new TreeMap<>();
        TreeMap<String, List<String>> domainMap = new TreeMap<>();
        for (Entry entry : entries) {
            Objects.requireNonNull(entry, "entry");
            entryCopy.add(entry);
            if (acronymMap.putIfAbsent(entry.acronym(), entry) != null) {
                throw new IllegalArgumentException("duplicate acronym: " + entry.acronym());
            }
            // Synexia's donor uses the first entry for a case-folded expansion.
            expansionMap.putIfAbsent(entry.expansionKey(), entry);
            entry.domain().ifPresent(domain -> domainMap
                    .computeIfAbsent(domain, ignored -> new ArrayList<>())
                    .add(entry.acronym()));
        }
        domainMap.replaceAll((domain, names) -> {
            names.sort(String::compareTo);
            return List.copyOf(names);
        });
        this.entries = List.copyOf(entryCopy);
        this.byAcronym = Collections.unmodifiableMap(acronymMap);
        this.byExpansion = Collections.unmodifiableMap(expansionMap);
        this.acronymsByDomain = Collections.unmodifiableMap(domainMap);
    }

    public String sourceId() { return sourceId; }
    public String sourceRevision() { return sourceRevision; }
    public String sourceFingerprint() { return sourceFingerprint; }
    public List<Entry> entries() { return entries; }
    public int size() { return entries.size(); }
    public int domainCount() { return acronymsByDomain.size(); }

    /** Donor-compatible: acronym spelling is case-sensitive. */
    public Optional<Entry> findByAcronym(String acronym) {
        return Optional.ofNullable(byAcronym.get(Objects.requireNonNull(acronym, "acronym")));
    }

    /** Donor-compatible: expansion lookup folds with Locale.ROOT. */
    public Optional<Entry> findByExpansion(String expansion) {
        return Optional.ofNullable(byExpansion.get(
                Objects.requireNonNull(expansion, "expansion").toLowerCase(Locale.ROOT)));
    }

    public List<String> acronymsInDomain(String domain) {
        List<String> result = acronymsByDomain.get(
                Objects.requireNonNull(domain, "domain"));
        return result == null ? List.of() : result;
    }

    public boolean appliesTo(String source, String revision, String fingerprint) {
        return sourceId.equals(source) && sourceRevision.equals(revision)
                && sourceFingerprint.equals(fingerprint);
    }

    /**
     * Source identity plus derived immutable facts for one acronym row.
     * The domain is optional target metadata; an admitted source value is
     * retained exactly when present.
     */
    public record Entry(
            String acronym,
            String expansion,
            Optional<String> domain,
            int acronymUtf16Length,
            int expansionUtf16Length,
            int acronymJavaHash,
            int expansionJavaHash) {
        public Entry {
            acronym = validateAcronym(acronym);
            expansion = validateExpansion(expansion);
            domain = validateDomain(domain);
            acronymUtf16Length = acronym.length();
            expansionUtf16Length = expansion.length();
            acronymJavaHash = javaHash(acronym);
            expansionJavaHash = javaHash(expansion);
        }

        public Entry(String acronym, String expansion, String domain) {
            this(acronym, expansion,
                    domain == null || domain.isBlank() ? Optional.empty() : Optional.of(domain),
                    0, 0, 0, 0);
        }

        private String expansionKey() {
            return expansion.toLowerCase(Locale.ROOT);
        }

        private static String validateAcronym(String value) {
            if (value == null || !ACRONYM.matcher(value).matches()) {
                throw new IllegalArgumentException("invalid acronym: " + value);
            }
            return value;
        }

        private static String validateExpansion(String value) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("invalid acronym expansion");
            }
            validateUtf16(value, "expansion");
            return value;
        }

        private static Optional<String> validateDomain(Optional<String> value) {
            Objects.requireNonNull(value, "domain");
            if (value.isEmpty()) return value;
            String domain = value.orElseThrow();
            if (!DOMAIN.matcher(domain).matches()) {
                throw new IllegalArgumentException("invalid acronym domain: " + domain);
            }
            return value;
        }

        private static void validateUtf16(String value, String field) {
            for (int index = 0; index < value.length(); index++) {
                char unit = value.charAt(index);
                if (Character.isHighSurrogate(unit)) {
                    if (index + 1 >= value.length()
                            || !Character.isLowSurrogate(value.charAt(index + 1))) {
                        throw new IllegalArgumentException(field + " contains an unpaired surrogate");
                    }
                    index++;
                } else if (Character.isLowSurrogate(unit)) {
                    throw new IllegalArgumentException(field + " contains an unpaired surrogate");
                }
            }
        }

        private static int javaHash(String value) {
            int hash = 0;
            for (int index = 0; index < value.length(); index++) {
                hash = 31 * hash + value.charAt(index);
            }
            return hash;
        }
    }
}
