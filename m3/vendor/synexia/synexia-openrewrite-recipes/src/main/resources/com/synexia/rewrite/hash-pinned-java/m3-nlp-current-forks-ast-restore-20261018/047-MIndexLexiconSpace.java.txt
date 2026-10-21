// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Immutable standard lexicon graph plus one append-only dynamic-literal overlay.
 *
 * <p>Standard namespaces share a canonical MIndexSymbolTable. Resolution is
 * deterministic by namespace priority and path order; all classifications can
 * still be queried with resolveAll.</p>
 */
public final class MIndexLexiconSpace {
    private final int version;
    private final MIndexNamespaceRegistry registry;
    private final MIndexSymbolTable symbols;
    private final Map<Integer, MIndexSymbolLexicon> lexicons;
    private final int[] resolutionOrder;
    private final MIndexNamespace dynamicNamespace;
    private final MIndexDynamicLiteralPool dynamicLiterals;

    private MIndexLexiconSpace(
            int version,
            MIndexNamespaceRegistry registry,
            MIndexSymbolTable symbols,
            Map<Integer, MIndexSymbolLexicon> lexicons,
            int[] resolutionOrder,
            MIndexNamespace dynamicNamespace,
            MIndexDynamicLiteralPool dynamicLiterals) {
        this.version = version;
        this.registry = registry;
        this.symbols = symbols;
        this.lexicons = Map.copyOf(lexicons);
        this.resolutionOrder = resolutionOrder;
        this.dynamicNamespace = dynamicNamespace;
        this.dynamicLiterals = dynamicLiterals;
    }

    public static MIndexLexiconSpace freeze(
            int version,
            MIndexNamespaceRegistry registry,
            MIndexSymbolTable symbols,
            Collection<? extends MIndexSymbolLexicon> supplied,
            String dynamicNamespacePath,
            MIndexDynamicLiteralPool dynamicLiterals) {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(symbols, "symbols");
        Objects.requireNonNull(supplied, "supplied");
        Objects.requireNonNull(dynamicLiterals, "dynamicLiterals");
        MIndexAtomRef.of(version, 0, 0);

        MIndexNamespace dynamicNamespace = registry.namespace(dynamicNamespacePath);
        if (dynamicNamespace.kind() != MIndexLexiconKind.DYNAMIC_LITERAL) {
            throw new IllegalArgumentException("Dynamic namespace must have DYNAMIC_LITERAL kind: " + dynamicNamespace.path());
        }

        Map<Integer, MIndexSymbolLexicon> lexicons = new HashMap<>();
        for (MIndexSymbolLexicon lexicon : supplied) {
            Objects.requireNonNull(lexicon, "lexicon");
            if (lexicon.dictionaryVersion() != version) {
                throw new IllegalArgumentException("Lexicon version mismatch for " + lexicon.namespace().path());
            }
            if (lexicon.symbols() != symbols) {
                throw new IllegalArgumentException("Every standard lexicon must share the same canonical symbol table");
            }
            MIndexNamespace registered = registry.namespace(lexicon.namespace().id());
            if (!registered.equals(lexicon.namespace())) {
                throw new IllegalArgumentException("Lexicon namespace is not from this registry: " + lexicon.namespace());
            }
            if (registered.kind() == MIndexLexiconKind.DYNAMIC_LITERAL) {
                throw new IllegalArgumentException("Dynamic namespace cannot be supplied as a frozen symbol lexicon");
            }
            if (lexicons.putIfAbsent(registered.id(), lexicon) != null) {
                throw new IllegalArgumentException("Duplicate lexicon namespace: " + registered.path());
            }
        }

        List<MIndexSymbolLexicon> ordered = new ArrayList<>(lexicons.values());
        ordered.sort(Comparator
                .comparingInt((MIndexSymbolLexicon lexicon) -> lexicon.namespace().priority()).reversed()
                .thenComparing(lexicon -> lexicon.namespace().path(), MIndexDictionary::compareCodePoint));
        int[] resolutionOrder = new int[ordered.size()];
        for (int index = 0; index < ordered.size(); index++) {
            resolutionOrder[index] = ordered.get(index).namespace().id();
        }

        return new MIndexLexiconSpace(
                version, registry, symbols, lexicons, resolutionOrder, dynamicNamespace, dynamicLiterals);
    }

    public int version() { return version; }
    public MIndexNamespaceRegistry registry() { return registry; }
    public MIndexSymbolTable symbols() { return symbols; }
    public MIndexDynamicLiteralPool dynamicLiterals() { return dynamicLiterals; }
    public MIndexNamespace dynamicNamespace() { return dynamicNamespace; }

    public Optional<MIndexAtomRef> resolveKnown(String surface) {
        Objects.requireNonNull(surface, "surface");
        for (int namespaceId : resolutionOrder) {
            Optional<MIndexAtomRef> found = lexicons.get(namespaceId).lookup(surface);
            if (found.isPresent()) return found;
        }
        return Optional.empty();
    }

    public MIndexAtomRef resolve(String surface) {
        Objects.requireNonNull(surface, "surface");
        return resolveKnown(surface).orElseGet(() -> dynamicRef(surface));
    }

    public MIndexAtomRef resolve(String surface, int... preferredNamespaceIds) {
        Objects.requireNonNull(surface, "surface");
        Objects.requireNonNull(preferredNamespaceIds, "preferredNamespaceIds");
        for (int namespaceId : preferredNamespaceIds) {
            MIndexNamespace namespace = registry.namespace(namespaceId);
            if (namespace.kind() == MIndexLexiconKind.DYNAMIC_LITERAL) break;
            MIndexSymbolLexicon lexicon = lexicons.get(namespaceId);
            if (lexicon == null) continue;
            Optional<MIndexAtomRef> found = lexicon.lookup(surface);
            if (found.isPresent()) return found.get();
        }
        return dynamicRef(surface);
    }

    public List<MIndexAtomRef> resolveAll(String surface) {
        Objects.requireNonNull(surface, "surface");
        List<MIndexAtomRef> result = new ArrayList<>();
        for (int namespaceId : resolutionOrder) {
            lexicons.get(namespaceId).lookup(surface).ifPresent(result::add);
        }
        if (result.isEmpty()) result.add(dynamicRef(surface));
        return List.copyOf(result);
    }

    public String materialize(MIndexAtomRef ref) {
        MIndexNamespace namespace = namespace(ref);
        if (namespace.kind() == MIndexLexiconKind.DYNAMIC_LITERAL) {
            return dynamicLiterals.materialize(ref.entryId());
        }
        return lexicon(ref).materialize(ref);
    }

    public OptionalLong symbolId(MIndexAtomRef ref) {
        MIndexNamespace namespace = namespace(ref);
        if (namespace.kind() == MIndexLexiconKind.DYNAMIC_LITERAL) return OptionalLong.empty();
        return OptionalLong.of(lexicon(ref).symbolId(ref));
    }

    public long frequency(MIndexAtomRef ref) {
        MIndexNamespace namespace = namespace(ref);
        return namespace.kind() == MIndexLexiconKind.DYNAMIC_LITERAL ? 0L : lexicon(ref).frequency(ref);
    }

    public long classes(MIndexAtomRef ref) {
        MIndexNamespace namespace = namespace(ref);
        return namespace.kind() == MIndexLexiconKind.DYNAMIC_LITERAL
                ? MIndexLexicalClass.DYNAMIC.mask()
                : lexicon(ref).classes(ref);
    }

    public MIndexNamespace namespace(MIndexAtomRef ref) {
        requireVersion(ref);
        return registry.namespace(ref.namespaceId());
    }

    public boolean isDynamic(MIndexAtomRef ref) {
        return namespace(ref).kind() == MIndexLexiconKind.DYNAMIC_LITERAL;
    }

    public boolean sameCanonicalSymbol(MIndexAtomRef left, MIndexAtomRef right) {
        OptionalLong leftId = symbolId(left);
        OptionalLong rightId = symbolId(right);
        return leftId.isPresent() && rightId.isPresent() && leftId.getAsLong() == rightId.getAsLong();
    }

    public String decode(long[] atomBits) {
        Objects.requireNonNull(atomBits, "atomBits");
        StringBuilder result = new StringBuilder();
        for (long bits : atomBits) result.append(materialize(new MIndexAtomRef(bits)));
        return result.toString();
    }

    private MIndexAtomRef dynamicRef(String surface) {
        return MIndexAtomRef.of(version, dynamicNamespace.id(), dynamicLiterals.intern(surface));
    }

    private MIndexSymbolLexicon lexicon(MIndexAtomRef ref) {
        requireVersion(ref);
        MIndexSymbolLexicon lexicon = lexicons.get(ref.namespaceId());
        if (lexicon == null) {
            throw new IllegalArgumentException("No frozen lexicon for namespace " + ref.namespaceId());
        }
        lexicon.requireLocal(ref);
        return lexicon;
    }

    private void requireVersion(MIndexAtomRef ref) {
        if (ref == null || ref.version() != version) {
            throw new IllegalArgumentException("Atom ref does not belong to lexicon-space version " + version + ": " + ref);
        }
    }
}
