/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
 * Recipe-generated SDK provider API name index. It does not implement external APIs. */
package com.m3.collections;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Canonical upstream FQNs and M3 backend dispositions for the collection SDK.
 *
 * <p>An M3 backend is NOT automatically a subtype, adapter, licensed fork, or
 * behavioral replacement for the named third-party API. All names are read
 * verbatim from the accompanying source-sealed, provider-reviewed catalogue.
 * No Guava, Eclipse, Spring, or Apache library is required to use this index.
 */
public final class M3CollectionSdkNames {
    public enum Provider { JDK, GUAVA, ECLIPSE, SPRING, APACHE }
    public enum Disposition { JDK_RETAINED, M3_BACKEND_ONLY, M3_VIEW_ONLY, PENDING }

    /** Correct upstream name with a distinct, never-aliased M3 target role. */
    public record Family(String upstreamFqcn, Provider provider, String existingM3Owner,
            String proposedM3Role, Disposition disposition, String semanticContract) {
        public Family {
            Objects.requireNonNull(upstreamFqcn);
            Objects.requireNonNull(provider);
            Objects.requireNonNull(existingM3Owner);
            Objects.requireNonNull(proposedM3Role);
            Objects.requireNonNull(disposition);
            Objects.requireNonNull(semanticContract);
            if (upstreamFqcn.isBlank() || !upstreamFqcn.contains(".")
                    || semanticContract.isBlank()) {
                throw new IllegalArgumentException("invalid SDK family record");
            }
        }
        /** M3 backend presence never implies upstream type/API substitutability. */
        public boolean vendorContractProven() { return false; }
        public boolean hasExistingM3Backend() { return !existingM3Owner.equals("-"); }
        public boolean isProposedOnly() { return disposition == Disposition.PENDING; }
    }

    private static final String RESOURCE = "/com/m3/collections/M3_SDK_FAMILY_CATALOG.tsv";
    private static final Map<String, Family> FAMILIES = load();

    private M3CollectionSdkNames() { }

    /** All reviewed name bindings, ordered by upstream fully qualified type name. */
    public static List<Family> all() { return List.copyOf(FAMILIES.values()); }
    public static int size() { return FAMILIES.size(); }
    public static Family get(String upstreamFqcn) { return FAMILIES.get(Objects.requireNonNull(upstreamFqcn)); }
    public static Family require(String upstreamFqcn) {
        Family found = get(upstreamFqcn);
        if (found == null) throw new IllegalArgumentException("unknown upstream collection: " + upstreamFqcn);
        return found;
    }
    public static List<Family> from(Provider provider) {
        Objects.requireNonNull(provider, "provider");
        ArrayList<Family> selected = new ArrayList<>();
        for (Family family : FAMILIES.values()) {
            if (family.provider() == provider) selected.add(family);
        }
        return List.copyOf(selected);
    }

    private static Map<String, Family> load() {
        InputStream stream = M3CollectionSdkNames.class.getResourceAsStream(RESOURCE);
        if (stream == null) throw new ExceptionInInitializerError("SDK catalogue resource missing: " + RESOURCE);
        LinkedHashMap<String, Family> entries = new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (!"upstream_fqcn\tvendor\texisting_m3_owner\tproposed_m3_role_NOT_IMPLEMENTED\tdisposition\tsemantic_contract".equals(header)) {
                throw new IOException("SDK catalogue schema drift");
            }
            String previous = "";
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] cols = line.split("\t", -1);
                if (cols.length != 6 || cols[0].compareTo(previous) <= 0) {
                    throw new IOException("duplicate, unsorted or malformed SDK family");
                }
                Provider provider = Provider.valueOf(cols[1]);
                String requiredPrefix = switch (provider) {
                    case JDK -> "java.util.";
                    case GUAVA -> "com.google.common.collect.";
                    case ECLIPSE -> "org.eclipse.collections.api.";
                    case SPRING -> "org.springframework.util.";
                    case APACHE -> "org.apache.commons.collections4.";
                };
                if (!cols[0].startsWith(requiredPrefix)
                        || (!cols[2].equals("-") && !cols[2].startsWith("com.m3.collections."))
                        || cols[3].isBlank()) {
                    throw new IOException("SDK API namespace mismatch: " + cols[0]);
                }
                Family family = new Family(cols[0], provider, cols[2], cols[3],
                        Disposition.valueOf(cols[4]), cols[5]);
                if (entries.putIfAbsent(cols[0], family) != null) {
                    throw new IOException("duplicate SDK family: " + cols[0]);
                }
                previous = cols[0];
            }
            if (entries.isEmpty()) throw new IOException("empty SDK family catalogue");
        } catch (IOException | IllegalArgumentException invalid) {
            throw new ExceptionInInitializerError(invalid);
        }
        return Collections.unmodifiableMap(entries);
    }
}
