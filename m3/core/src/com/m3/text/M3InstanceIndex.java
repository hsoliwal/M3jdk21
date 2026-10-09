/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Immutable receiver for first-party Synexia proper-name instances.
 *
 * <p>Instances are separate from lexical token identity and from the X-spine
 * concept catalog. Each record points at one concept through instanceOfX;
 * source identity remains the stable lookup key. No donor corpus or remote
 * query is loaded here.</p>
 */
public final class M3InstanceIndex {
    private static final Comparator<SharedLexiconCatalog.SourceIdentity> ID_ORDER =
            Comparator.comparing(SharedLexiconCatalog.SourceIdentity::sourceId)
                    .thenComparing(SharedLexiconCatalog.SourceIdentity::recordId);

    private final Map<SharedLexiconCatalog.SourceIdentity, InstanceRecord> byIdentity;
    private final Map<Integer, List<SharedLexiconCatalog.SourceIdentity>> byConcept;
    private final Map<NameKey, List<SharedLexiconCatalog.SourceIdentity>> byName;
    private final Map<SharedLexiconCatalog.SourceIdentity, List<TitleRecord>> titlesByInstance;

    private M3InstanceIndex(List<InstanceRecord> instances, List<TitleRecord> titles) {
        Objects.requireNonNull(instances, "instances");
        Objects.requireNonNull(titles, "titles");

        TreeMap<SharedLexiconCatalog.SourceIdentity, InstanceRecord> identityMap =
                new TreeMap<>(ID_ORDER);
        TreeMap<Integer, List<SharedLexiconCatalog.SourceIdentity>> conceptMap = new TreeMap<>();
        TreeMap<NameKey, List<SharedLexiconCatalog.SourceIdentity>> nameMap =
                new TreeMap<>(Comparator.comparing(NameKey::languageTag)
                        .thenComparing(NameKey::name));
        for (InstanceRecord instance : instances) {
            if (identityMap.put(instance.identity(), instance) != null) {
                throw new IllegalArgumentException("duplicate instance identity: " + instance.identity());
            }
            conceptMap.computeIfAbsent(instance.instanceOfX(), ignored -> new ArrayList<>())
                    .add(instance.identity());
            nameMap.computeIfAbsent(new NameKey(instance.languageTag(), instance.name()),
                            ignored -> new ArrayList<>()).add(instance.identity());
        }

        TreeMap<SharedLexiconCatalog.SourceIdentity, List<TitleRecord>> titleMap =
                new TreeMap<>(ID_ORDER);
        for (TitleRecord title : titles) {
            if (!identityMap.containsKey(title.instanceIdentity())) {
                throw new IllegalArgumentException("title points at missing instance: "
                        + title.instanceIdentity());
            }
            List<TitleRecord> values = titleMap.computeIfAbsent(title.instanceIdentity(),
                    ignored -> new ArrayList<>());
            if (values.stream().anyMatch(existing ->
                    existing.identity().equals(title.identity())
                            || existing.title().equals(title.title()))) {
                throw new IllegalArgumentException("duplicate title identity/text: " + title.identity());
            }
            values.add(title);
        }

        byIdentity = Collections.unmodifiableMap(identityMap);
        byConcept = immutableIdentityListsByConcept(conceptMap);
        byName = immutableIdentityListsByName(nameMap);
        titlesByInstance = immutableTitleLists(titleMap);
    }

    public static M3InstanceIndex of(List<InstanceRecord> instances, List<TitleRecord> titles) {
        return new M3InstanceIndex(List.copyOf(instances), List.copyOf(titles));
    }

    public static M3InstanceIndex empty() {
        return of(List.of(), List.of());
    }

    public int size() {
        return byIdentity.size();
    }

    public Optional<InstanceRecord> find(SharedLexiconCatalog.SourceIdentity identity) {
        return Optional.ofNullable(byIdentity.get(requireIdentity(identity)));
    }

    public List<InstanceRecord> instancesOf(int instanceOfX) {
        List<SharedLexiconCatalog.SourceIdentity> identities =
                byConcept.getOrDefault(instanceOfX, List.of());
        List<InstanceRecord> result = new ArrayList<>(identities.size());
        for (SharedLexiconCatalog.SourceIdentity identity : identities) {
            result.add(byIdentity.get(identity));
        }
        return List.copyOf(result);
    }

    /** Exact, language-scoped lookup; case folding is an ingestion policy. */
    public List<InstanceRecord> findByName(String languageTag, String name) {
        NameKey key = new NameKey(languageTag, name);
        List<SharedLexiconCatalog.SourceIdentity> identities =
                byName.getOrDefault(key, List.of());
        List<InstanceRecord> result = new ArrayList<>(identities.size());
        for (SharedLexiconCatalog.SourceIdentity identity : identities) {
            result.add(byIdentity.get(identity));
        }
        return List.copyOf(result);
    }

    public List<TitleRecord> titlesFor(SharedLexiconCatalog.SourceIdentity identity) {
        requireIdentity(identity);
        return titlesByInstance.getOrDefault(identity, List.of());
    }

    public record PrecomputeFacts(int utf16Units, int codePoints, int javaHash,
                                  boolean ascii, boolean latin1) {
        public PrecomputeFacts {
            if (utf16Units < 0 || codePoints < 0 || codePoints > utf16Units) {
                throw new IllegalArgumentException("invalid text dimensions");
            }
        }

        public static PrecomputeFacts forText(String text) {
            Objects.requireNonNull(text, "text");
            boolean ascii = true;
            boolean latin1 = true;
            for (int at = 0; at < text.length(); at++) {
                char value = text.charAt(at);
                ascii &= value <= 0x7f;
                latin1 &= value <= 0xff;
            }
            return new PrecomputeFacts(text.length(), text.codePointCount(0, text.length()),
                    text.hashCode(), ascii, latin1);
        }
    }

    public record InstanceRecord(SharedLexiconCatalog.SourceIdentity identity,
                                 String languageTag, String name, String instanceType,
                                 int instanceOfX, List<String> aliases,
                                 Map<String, String> metadata, String provenance,
                                 PrecomputeFacts precompute) {
        public InstanceRecord(SharedLexiconCatalog.SourceIdentity identity,
                              String languageTag, String name, String instanceType,
                              int instanceOfX, List<String> aliases,
                              Map<String, String> metadata, String provenance) {
            this(identity, languageTag, name, instanceType, instanceOfX, aliases, metadata,
                    provenance, PrecomputeFacts.forText(name));
        }

        public InstanceRecord {
            requireIdentity(identity);
            languageTag = text(languageTag, "languageTag");
            name = text(name, "name");
            instanceType = text(instanceType, "instanceType");
            if (instanceOfX < 0) throw new IllegalArgumentException("negative instanceOfX");
            aliases = canonicalAliases(aliases);
            metadata = canonicalMetadata(metadata);
            provenance = text(provenance, "provenance");
            Objects.requireNonNull(precompute, "precompute");
        }
    }

    public record TitleRecord(SharedLexiconCatalog.SourceIdentity identity,
                              SharedLexiconCatalog.SourceIdentity instanceIdentity,
                              String languageTag, String title, String titleType,
                              List<String> creators, String provenance,
                              PrecomputeFacts precompute) {
        public TitleRecord(SharedLexiconCatalog.SourceIdentity identity,
                           SharedLexiconCatalog.SourceIdentity instanceIdentity,
                           String languageTag, String title, String titleType,
                           List<String> creators, String provenance) {
            this(identity, instanceIdentity, languageTag, title, titleType, creators,
                    provenance, PrecomputeFacts.forText(title));
        }

        public TitleRecord {
            requireIdentity(identity);
            requireIdentity(instanceIdentity);
            languageTag = text(languageTag, "languageTag");
            title = text(title, "title");
            titleType = text(titleType, "titleType");
            creators = canonicalAliases(creators);
            provenance = text(provenance, "provenance");
            Objects.requireNonNull(precompute, "precompute");
        }
    }

    private record NameKey(String languageTag, String name) {
        private NameKey {
            languageTag = text(languageTag, "languageTag");
            name = text(name, "name");
        }
    }

    private static Map<Integer, List<SharedLexiconCatalog.SourceIdentity>>
    immutableIdentityListsByConcept(TreeMap<Integer, List<SharedLexiconCatalog.SourceIdentity>> source) {
        Map<Integer, List<SharedLexiconCatalog.SourceIdentity>> result = new TreeMap<>();
        source.forEach((key, values) -> {
            values.sort(ID_ORDER);
            result.put(key, List.copyOf(values));
        });
        return Collections.unmodifiableMap(result);
    }

    private static Map<NameKey, List<SharedLexiconCatalog.SourceIdentity>>
    immutableIdentityListsByName(TreeMap<NameKey, List<SharedLexiconCatalog.SourceIdentity>> source) {
        Map<NameKey, List<SharedLexiconCatalog.SourceIdentity>> result = new TreeMap<>(
                Comparator.comparing(NameKey::languageTag).thenComparing(NameKey::name));
        source.forEach((key, values) -> {
            values.sort(ID_ORDER);
            result.put(key, List.copyOf(values));
        });
        return Collections.unmodifiableMap(result);
    }

    private static Map<SharedLexiconCatalog.SourceIdentity, List<TitleRecord>>
    immutableTitleLists(TreeMap<SharedLexiconCatalog.SourceIdentity, List<TitleRecord>> source) {
        Map<SharedLexiconCatalog.SourceIdentity, List<TitleRecord>> result = new TreeMap<>(ID_ORDER);
        source.forEach((key, values) -> result.put(key, List.copyOf(values)));
        return Collections.unmodifiableMap(result);
    }

    private static List<String> canonicalAliases(List<String> values) {
        Objects.requireNonNull(values, "aliases");
        TreeSet<String> sorted = new TreeSet<>();
        for (String value : values) sorted.add(text(value, "alias"));
        return List.copyOf(sorted);
    }

    private static Map<String, String> canonicalMetadata(Map<String, String> values) {
        Objects.requireNonNull(values, "metadata");
        TreeMap<String, String> sorted = new TreeMap<>();
        values.forEach((key, value) -> {
            String canonicalKey = text(key, "metadata key");
            String canonicalValue = text(value, "metadata value");
            if (sorted.put(canonicalKey, canonicalValue) != null) {
                throw new IllegalArgumentException("duplicate metadata key: " + canonicalKey);
            }
        });
        return Collections.unmodifiableMap(sorted);
    }

    private static SharedLexiconCatalog.SourceIdentity requireIdentity(
            SharedLexiconCatalog.SourceIdentity identity) {
        Objects.requireNonNull(identity, "identity");
        text(identity.sourceId(), "sourceId");
        text(identity.recordId(), "recordId");
        return identity;
    }

    private static String text(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isEmpty()) throw new IllegalArgumentException(field + " is empty");
        return value;
    }
}
