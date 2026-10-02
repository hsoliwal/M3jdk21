// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.dag;

import com.m3.indexdb.M3IndexDbSemanticFingerprint;
import com.m3.rewrite.scope.M3EditScope;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Immutable fuzzy catalogue of possible M3 recipe DAGs.
 *
 * <p>Catalogue ranking is evidence only. A selected DAG has no source replacement authority; every
 * leaf recipe retains its own scope, provenance and verification gates.
 */
public final class M3RecipeDagCatalogue {
    private static final String RESOURCE = "/META-INF/m3/recipe-dags.tsv";
    private final List<Entry> entries;

    public M3RecipeDagCatalogue() {
        this.entries = load();
    }

    public List<Entry> entries() {
        return entries;
    }

    public List<Match> fuzzy(String query, int limit) {
        if (limit < 1 || limit > 1000) throw new IllegalArgumentException("limit");
        Set<String> queryTokens = tokens(query);
        long querySimHash = simHash(queryTokens);

        return entries.stream()
                .map(entry -> match(entry, queryTokens, querySimHash))
                .sorted(Comparator
                        .comparingDouble(Match::score).reversed()
                        .thenComparing(match -> match.entry().dagId()))
                .limit(limit)
                .toList();
    }

    private static Match match(Entry entry, Set<String> queryTokens, long querySimHash) {
        Set<String> intersection = new TreeSet<>(queryTokens);
        intersection.retainAll(entry.capabilityTokens());
        Set<String> union = new TreeSet<>(queryTokens);
        union.addAll(entry.capabilityTokens());
        double jaccard = union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();

        int distance = Long.bitCount(querySimHash ^ entry.simHash64());
        double sim = 1.0 - ((double) distance / Long.SIZE);
        double score = 0.65 * jaccard + 0.35 * sim;
        return new Match(entry, score, intersection.size(), distance);
    }

    private static List<Entry> load() {
        String text;
        try (var stream = M3RecipeDagCatalogue.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) throw new IllegalStateException("recipe DAG catalogue missing");
            text = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException("cannot read recipe DAG catalogue", failure);
        }

        List<Entry> result = new ArrayList<>();
        String prior = "";
        for (String line : text.lines().toList()) {
            if (line.isBlank() || line.startsWith("#") || line.startsWith("dag_id\t")) continue;
            String[] cells = line.split("\t", -1);
            if (cells.length != 5) throw new IllegalStateException("invalid recipe DAG row");
            Entry entry = new Entry(
                    cells[0],
                    tokens(cells[1].replace(',', ' ')),
                    M3EditScope.valueOf(cells[2]),
                    List.of(cells[3].split(">")),
                    Boolean.parseBoolean(cells[4]));
            if (prior.compareTo(entry.dagId()) >= 0) {
                throw new IllegalStateException("recipe DAG catalogue must be sorted and unique");
            }
            prior = entry.dagId();
            result.add(entry);
        }
        return List.copyOf(result);
    }

    private static Set<String> tokens(String value) {
        String checked = Objects.requireNonNull(value, "value")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .strip();
        if (checked.isEmpty()) throw new IllegalArgumentException("capability text required");
        return Set.copyOf(List.of(checked.split("\\s+")));
    }

    private static long simHash(Set<String> tokens) {
        int[] votes = new int[Long.SIZE];
        for (String token : tokens) {
            long value = Long.parseUnsignedLong(
                    M3IndexDbSemanticFingerprint.sha256Utf16("DAG_TOKEN|" + token).substring(0, 16), 16);
            for (int bit = 0; bit < Long.SIZE; bit++) {
                votes[bit] += ((value >>> bit) & 1L) == 0L ? -1 : 1;
            }
        }
        long result = 0L;
        for (int bit = 0; bit < votes.length; bit++) if (votes[bit] >= 0) result |= 1L << bit;
        return result;
    }

    public record Entry(
            String dagId,
            Set<String> capabilityTokens,
            M3EditScope maximumScope,
            List<String> recipeClasses,
            boolean mutationAuthority) {
        public Entry {
            dagId = required(dagId, "dagId");
            capabilityTokens = Set.copyOf(Objects.requireNonNull(capabilityTokens, "capabilityTokens"));
            if (capabilityTokens.isEmpty()) throw new IllegalArgumentException("capabilityTokens");
            maximumScope = Objects.requireNonNull(maximumScope, "maximumScope");
            recipeClasses = List.copyOf(Objects.requireNonNull(recipeClasses, "recipeClasses"));
            if (recipeClasses.isEmpty() || recipeClasses.stream().anyMatch(String::isBlank)) {
                throw new IllegalArgumentException("recipeClasses");
            }
            if (mutationAuthority) {
                throw new IllegalArgumentException("fuzzy DAG catalogue may not grant mutation authority");
            }
        }

        long simHash64() {
            return simHash(capabilityTokens);
        }
    }

    public record Match(Entry entry, double score, int exactTokenMatches, int simHashDistance) {}

    private static String required(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
        if (checked.isEmpty() || checked.indexOf('\0') >= 0) throw new IllegalArgumentException(field);
        return checked;
    }
}
