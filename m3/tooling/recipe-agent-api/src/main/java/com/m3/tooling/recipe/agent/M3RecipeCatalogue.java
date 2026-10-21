// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.tooling.recipe.agent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Deterministic recipe discovery; being catalogued never implies being executable. */
public final class M3RecipeCatalogue {
    public record Candidate(String id, String category, String sourceKind,
                            M3RecipeAgent.Operation operation, M3RecipeAgent.Authority authority,
                            String providerId, List<String> requiredGates) {
        public Candidate {
            if (id == null || id.isBlank() || category == null || category.isBlank()
                    || sourceKind == null || sourceKind.isBlank()) {
                throw new IllegalArgumentException("incomplete recipe identity");
            }
            operation = Objects.requireNonNull(operation, "operation");
            authority = Objects.requireNonNull(authority, "authority");
            providerId = Objects.requireNonNullElse(providerId, "");
            requiredGates = List.copyOf(Objects.requireNonNull(requiredGates, "requiredGates"));
            if (requiredGates.isEmpty()) throw new IllegalArgumentException("recipe without gates");
            if (operation == M3RecipeAgent.Operation.SEMANTIC && providerId.isBlank()) {
                throw new IllegalArgumentException("semantic recipe without provider");
            }
        }

        public boolean available(Set<String> qualifiedProviders) {
            return operation != M3RecipeAgent.Operation.SEMANTIC || qualifiedProviders.contains(providerId);
        }
    }

    private final Map<String, Candidate> catalogue;

    public M3RecipeCatalogue(List<Candidate> candidates) {
        HashMap<String, Candidate> ids = new HashMap<>();
        for (Candidate candidate : List.copyOf(candidates)) {
            if (ids.putIfAbsent(candidate.id(), candidate) != null) {
                throw new IllegalArgumentException("duplicate recipe id: " + candidate.id());
            }
        }
        catalogue = Map.copyOf(ids);
    }

    /** Stable full catalogue for agent discovery (including unqualified providers). */
    public List<Candidate> listAll() {
        return catalogue.values().stream().sorted(Comparator.comparing(Candidate::id)).toList();
    }

    /** Include pending providers in discovery, but not in executable selection. */
    public List<Candidate> discover(String category, String sourceKind,
                                    M3RecipeAgent.Authority granted) {
        return catalogue.values().stream()
                .filter(c -> c.category().equals(category) &&
                        (c.sourceKind().equals(sourceKind) || c.sourceKind().equals("any")))
                .filter(c -> c.authority().ordinal() <= granted.ordinal())
                .sorted(Comparator.comparing(Candidate::id)).toList();
    }

    /** A coding agent must not silently choose between ambiguous applicable recipes. */
    public Candidate choose(String category, String sourceKind,
                            M3RecipeAgent.Authority granted, Set<String> qualifiedProviders) {
        List<Candidate> eligible = discover(category, sourceKind, granted).stream()
                .filter(c -> c.available(qualifiedProviders)).toList();
        if (eligible.isEmpty()) throw new IllegalArgumentException("no qualified recipe: " + category);
        int minAuthority = eligible.stream().mapToInt(c -> c.authority().ordinal()).min().orElseThrow();
        List<Candidate> minimal = eligible.stream()
                .filter(c -> c.authority().ordinal() == minAuthority).toList();
        if (minimal.size() != 1) throw new IllegalStateException("ambiguous recipes: "
                + minimal.stream().map(Candidate::id).toList());
        return minimal.getFirst();
    }

    public static M3RecipeCatalogue builtins() {
        List<Candidate> all = new ArrayList<>();
        all.add(new Candidate("m3.text.literal", "replace-literal", "any",
                M3RecipeAgent.Operation.LITERAL, M3RecipeAgent.Authority.FILE,
                "", List.of("preimage", "fixed-point", "diff")));
        all.add(new Candidate("m3.text.regex", "replace-regex", "any",
                M3RecipeAgent.Operation.REGEX, M3RecipeAgent.Authority.FILE,
                "", List.of("preimage", "fixed-point", "regex-corpus")));
        for (String kind : List.of("create", "delete", "move")) {
            M3RecipeAgent.Operation op = switch (kind) {
                case "create" -> M3RecipeAgent.Operation.CREATE_FILE;
                case "delete" -> M3RecipeAgent.Operation.DELETE_FILE;
                default -> M3RecipeAgent.Operation.MOVE_FILE;
            };
            all.add(new Candidate("m3.fs." + kind, "file-" + kind, "any", op,
                    M3RecipeAgent.Authority.FILE, "", List.of("path-fence", "preimage", "review")));
        }
        // Descriptors only: these need an installed, qualified semantic adapter.
        for (String[] semantic : new String[][] {
                {"java-ast", "java", "m3ast"},
                {"java-refactor", "java", "openrewrite"},
                {"java-imports", "java", "openrewrite"},
                {"java-abi", "java", "javac"},
                {"maven-model", "pom", "maven"},
                {"gradle-model", "gradle", "gradle"},
                {"xml-structural", "xml", "openrewrite"},
                {"yaml-structural", "yaml", "openrewrite"},
                {"json-structural", "json", "openrewrite"},
                {"native-ast", "c-cpp", "clang"},
                {"jni-contract", "java", "m3jni"},
                {"test-generation", "java", "m3tests"},
                {"dependency-upgrade", "pom", "openrewrite"},
                {"semantic-search", "java", "m3index"},
                {"code-atomization", "java", "m3atom"},
                {"java-change-type", "java", "openrewrite"},
                {"java-change-package", "java", "openrewrite"},
                {"java-rename-method", "java", "openrewrite"},
                {"java-change-signature", "java", "openrewrite"},
                {"java-extract-method", "java", "m3ast"},
                {"java-inline-method", "java", "m3ast"},
                {"java-extract-variable", "java", "m3ast"},
                {"java-change-visibility", "java", "m3ast"},
                {"java-dead-code", "java", "m3ast"},
                {"java-nullability", "java", "javac"},
                {"java-generics", "java", "javac"},
                {"java-control-flow", "java", "javac"},
                {"java-concurrency", "java", "m3ast"},
                {"java-stream-conversion", "java", "openrewrite"},
                {"java-record-conversion", "java", "openrewrite"},
                {"java-annotations", "java", "openrewrite"},
                {"java-logging", "java", "openrewrite"},
                {"java-security", "java", "openrewrite"},
                {"java-performance", "java", "m3ast"},
                {"java-dependency-graph", "java", "m3index"},
                {"java-diff-contract", "java", "javac"},
                {"jni-header", "c-cpp", "m3jni"},
                {"jni-ownership", "c-cpp", "m3jni"},
                {"jni-symbol-rename", "c-cpp", "clang"},
                {"native-format", "c-cpp", "clang"},
                {"maven-plugin", "pom", "maven"},
                {"maven-dependencies", "pom", "openrewrite"},
                {"maven-profiles", "pom", "openrewrite"},
                {"gradle-dependencies", "gradle", "openrewrite"},
                {"json-schema", "json", "m3schema"},
                {"xml-schema", "xml", "m3schema"},
                {"yaml-schema", "yaml", "m3schema"},
                {"test-refactor", "java", "openrewrite"},
                {"test-oracle", "java", "m3tests"},
                {"license-attribution", "any", "m3provenance"},
                {"git-superset-merge", "any", "m3git"},
                {"mechanical-backport", "any", "m3backport"},
                {"source-delta", "any", "m3file"},
                {"class-abi-delta", "java", "javac"}
        }) {
            all.add(new Candidate("m3.semantic." + semantic[0], semantic[0], semantic[1],
                    M3RecipeAgent.Operation.SEMANTIC, M3RecipeAgent.Authority.FILE,
                    semantic[2], List.of("parser", "contract", "tests", "fixed-point", "diff")));
        }
        return new M3RecipeCatalogue(all);
    }
}
