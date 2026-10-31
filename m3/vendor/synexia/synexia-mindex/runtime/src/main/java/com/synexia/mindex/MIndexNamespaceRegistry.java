// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Deterministically frozen hierarchy of lexicon namespaces.
 *
 * <p>Ids are assigned by canonical path order within a snapshot. Persistence
 * therefore records the snapshot/version together with packed atom refs.</p>
 */
public final class MIndexNamespaceRegistry {
    public record Definition(
            String path,
            MIndexLexiconKind kind,
            String parentPath,
            String languageTag,
            int priority) {

        public Definition {
            path = canonicalPath(path);
            kind = Objects.requireNonNull(kind, "kind");
            parentPath = parentPath == null || parentPath.isBlank() ? "" : canonicalPath(parentPath);
            languageTag = normalizeLanguageTag(languageTag);
        }

        public static Definition of(String path, MIndexLexiconKind kind, String parentPath, int priority) {
            return new Definition(path, kind, parentPath, "", priority);
        }

        public static Definition language(
                String path, MIndexLexiconKind kind, String parentPath, String languageTag, int priority) {
            return new Definition(path, kind, parentPath, languageTag, priority);
        }
    }

    private final List<MIndexNamespace> namespaces;
    private final Map<String, Integer> idsByPath;

    private MIndexNamespaceRegistry(List<MIndexNamespace> namespaces, Map<String, Integer> idsByPath) {
        this.namespaces = List.copyOf(namespaces);
        this.idsByPath = Map.copyOf(idsByPath);
    }

    public static MIndexNamespaceRegistry freeze(Collection<Definition> supplied) {
        Objects.requireNonNull(supplied, "supplied");
        Map<String, Definition> unique = new HashMap<>();
        for (Definition definition : supplied) {
            Objects.requireNonNull(definition, "definition");
            Definition previous = unique.putIfAbsent(definition.path(), definition);
            if (previous != null && !previous.equals(definition)) {
                throw new IllegalArgumentException("Conflicting namespace definition: " + definition.path());
            }
        }
        if (unique.size() > MIndexAtomRef.MAX_NAMESPACE_ID + 1L) {
            throw new IllegalArgumentException("Too many lexicon namespaces: " + unique.size());
        }

        List<Definition> ordered = new ArrayList<>(unique.values());
        ordered.sort(Comparator.comparing(Definition::path, MIndexDictionary::compareCodePoint));

        Map<String, Integer> ids = new HashMap<>();
        for (int id = 0; id < ordered.size(); id++) ids.put(ordered.get(id).path(), id);

        List<MIndexNamespace> namespaces = new ArrayList<>(ordered.size());
        for (int id = 0; id < ordered.size(); id++) {
            Definition definition = ordered.get(id);
            int parentId = -1;
            if (!definition.parentPath().isEmpty()) {
                Integer resolved = ids.get(definition.parentPath());
                if (resolved == null) {
                    throw new IllegalArgumentException(
                            "Unknown parent namespace " + definition.parentPath() + " for " + definition.path());
                }
                if (resolved == id) throw new IllegalArgumentException("Namespace cannot parent itself: " + definition.path());
                parentId = resolved;
            }
            namespaces.add(new MIndexNamespace(
                    id, definition.path(), definition.kind(), parentId,
                    definition.languageTag(), definition.priority()));
        }

        validateAcyclic(namespaces);
        return new MIndexNamespaceRegistry(namespaces, ids);
    }

    public int size() {
        return namespaces.size();
    }

    public List<MIndexNamespace> namespaces() {
        return namespaces;
    }

    public MIndexNamespace namespace(int id) {
        return namespaces.get(Objects.checkIndex(id, namespaces.size()));
    }

    public MIndexNamespace namespace(String path) {
        return find(path).orElseThrow(() -> new IllegalArgumentException("Unknown namespace: " + path));
    }

    public Optional<MIndexNamespace> find(String path) {
        Integer id = idsByPath.get(canonicalPath(path));
        return id == null ? Optional.empty() : Optional.of(namespaces.get(id));
    }

    public int id(String path) {
        return namespace(path).id();
    }

    public List<MIndexNamespace> children(int parentId) {
        namespace(parentId);
        List<MIndexNamespace> result = new ArrayList<>();
        for (MIndexNamespace namespace : namespaces) {
            if (namespace.parentId() == parentId) result.add(namespace);
        }
        return List.copyOf(result);
    }

    public List<MIndexNamespace> ancestors(int namespaceId) {
        MIndexNamespace current = namespace(namespaceId);
        List<MIndexNamespace> result = new ArrayList<>();
        while (current.hasParent()) {
            current = namespace(current.parentId());
            result.add(current);
        }
        return List.copyOf(result);
    }

    public boolean isDescendant(int namespaceId, int possibleAncestorId) {
        namespace(possibleAncestorId);
        MIndexNamespace current = namespace(namespaceId);
        while (current.hasParent()) {
            if (current.parentId() == possibleAncestorId) return true;
            current = namespace(current.parentId());
        }
        return false;
    }

    public static String canonicalPath(String input) {
        String value = Objects.requireNonNull(input, "path").strip().replace('\\', '/');
        while (value.contains("//")) value = value.replace("//", "/");
        while (value.startsWith("/")) value = value.substring(1);
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        if (value.isEmpty()) throw new IllegalArgumentException("namespace path must not be blank");
        return value.toLowerCase(Locale.ROOT);
    }

    private static String normalizeLanguageTag(String languageTag) {
        if (languageTag == null || languageTag.isBlank()) return "";
        return languageTag.strip().replace('_', '-').toLowerCase(Locale.ROOT);
    }

    private static void validateAcyclic(List<MIndexNamespace> namespaces) {
        for (MIndexNamespace namespace : namespaces) {
            Set<Integer> seen = new HashSet<>();
            int current = namespace.id();
            while (current >= 0) {
                if (!seen.add(current)) {
                    throw new IllegalArgumentException("Namespace parent cycle involving " + namespace.path());
                }
                current = namespaces.get(current).parentId();
            }
        }
    }
}
