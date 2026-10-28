/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Independent source-qualified row parity and packaged legacy resource regression. */
public final class M3CollectionSdkCatalogProjectionTest {
    private static final String HEADER = "upstream_fqcn\tvendor\texisting_m3_owner\t"
            + "proposed_m3_role_NOT_IMPLEMENTED\tdisposition\tsemantic_contract";

    @Test
    void standaloneCatalogueOracleSucceeds() throws Exception {
        assertTrue(M3CollectionSdkCatalogSelfTest.verify() > 0);
    }

    @Test
    void catalogueMatchesEveryQualifiedColumnWithoutLoadingProviders() throws Exception {
        var expected = rows("/expected-data/m3-sdk-families-20261017/FAMILY_CATALOG.tsv",
                "406d9809dab857a62b571423a51c3240a7969f682a4a37c577590d6d79f344f9");
        var actual = M3CollectionSdkCatalog.families();
        assertEquals(107, expected.size());
        assertEquals(expected.size(), actual.size());
        assertEquals(107, M3CollectionSdkCatalog.size());
        int optional = 0;
        String previous = "";
        for (int i = 0; i < expected.size(); i++) {
            String[] row = expected.get(i);
            var family = actual.get(i);
            assertTrue(row[0].compareTo(previous) > 0, row[0]);
            previous = row[0];
            assertArrayEquals(new String[] {row[0], row[1], row[2].equals("-") ? "" : row[2],
                    row[3], row[4], row[5]}, new String[] {family.upstreamName(), family.vendor().name(),
                    family.existingM3Owner(), family.candidateRole(), family.disposition().name(),
                    family.semanticContract()}, row[0]);
            assertEquals(family, M3CollectionSdkCatalog.require(row[0]));
            assertEquals(family, M3CollectionSdkCatalog.find(row[0]).orElseThrow());
            if (family.disposition() == M3CollectionSdkCatalog.Disposition.OPTIONAL_ADAPTER) optional++;
        }
        assertEquals(14, optional);
    }

    @Test
    void packagedLegacyIndexKeepsAll104SixColumnContracts() throws Exception {
        // This is the shipped resource, byte-pinned to the separate legacy authority.
        var expected = rows("/com/m3/collections/M3_SDK_FAMILY_CATALOG.tsv",
                "e986838120c2558a145c6b16942475c8f5ff885b166526d635ef9c10360d8313");
        var actual = M3CollectionSdkNames.all();
        assertEquals(104, expected.size());
        assertEquals(104, M3CollectionSdkNames.size());
        assertEquals(expected.size(), actual.size());
        assertArrayEquals(new String[] {"JDK_RETAINED", "M3_BACKEND_ONLY", "M3_VIEW_ONLY", "PENDING"},
                Arrays.stream(M3CollectionSdkNames.Disposition.values()).map(Enum::name).toArray(String[]::new));
        String previous = "";
        for (int i = 0; i < expected.size(); i++) {
            String[] row = expected.get(i);
            var family = actual.get(i);
            assertTrue(row[0].compareTo(previous) > 0, row[0]);
            previous = row[0];
            assertArrayEquals(row, new String[] {family.upstreamFqcn(), family.provider().name(),
                    family.existingM3Owner(), family.proposedM3Role(), family.disposition().name(),
                    family.semanticContract()}, row[0]);
            assertEquals(family, M3CollectionSdkNames.get(row[0]));
            assertEquals(family, M3CollectionSdkNames.require(row[0]));
            assertFalse(family.vendorContractProven(), row[0]);
            assertEquals(!row[2].equals("-"), family.hasExistingM3Backend(), row[0]);
            assertEquals(row[4].equals("PENDING"), family.isProposedOnly(), row[0]);
            assertFalse(family.proposedM3Role().isBlank(), row[0]);
            assertFalse(family.semanticContract().isBlank(), row[0]);
        }
        for (var provider : M3CollectionSdkNames.Provider.values()) {
            var selected = actual.stream().filter(family -> family.provider() == provider).toList();
            assertFalse(selected.isEmpty(), provider.name());
            assertEquals(selected, M3CollectionSdkNames.from(provider));
        }
        assertThrows(UnsupportedOperationException.class, actual::clear);
        assertNull(M3CollectionSdkNames.get("java.util.DoesNotExist"));
        assertThrows(IllegalArgumentException.class, () -> M3CollectionSdkNames.require("java.util.DoesNotExist"));
    }

    @Test
    void legacyGuavaAndSpringRemainDistinctProposedOnlyContracts() {
        var guava = M3CollectionSdkNames.require("com.google.common.collect.Multimap");
        var spring = M3CollectionSdkNames.require("org.springframework.util.MultiValueMap");
        assertEquals(M3CollectionSdkNames.Provider.GUAVA, guava.provider());
        assertEquals(M3CollectionSdkNames.Provider.SPRING, spring.provider());
        assertEquals("M3IndexedMultimap", guava.proposedM3Role());
        assertEquals("M3SpringMultiValueMapAdapter", spring.proposedM3Role());
        assertEquals("key-value-pairs", guava.semanticContract());
        assertEquals("spring-map-of-lists", spring.semanticContract());
        assertNotEquals(guava.semanticContract(), spring.semanticContract());
        for (var family : List.of(guava, spring)) {
            assertEquals(M3CollectionSdkNames.Disposition.PENDING, family.disposition());
            assertEquals("-", family.existingM3Owner());
            assertTrue(family.isProposedOnly());
            assertFalse(family.hasExistingM3Backend());
            assertFalse(family.vendorContractProven());
        }
    }

    private static List<String[]> rows(String resource, String sha256) throws Exception {
        byte[] bytes;
        try (InputStream input = M3CollectionSdkCatalogProjectionTest.class.getResourceAsStream(resource)) {
            assertNotNull(input, "packaged catalogue resource missing: " + resource);
            bytes = input.readAllBytes();
        }
        assertEquals(sha256, HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)),
                "source-qualified bytes: " + resource);
        List<String> lines = new String(bytes, StandardCharsets.UTF_8).lines().toList();
        assertEquals(HEADER, lines.getFirst());
        return lines.stream().skip(1).filter(line -> !line.isBlank()).map(line -> {
            String[] row = line.split("\t", -1);
            if (row.length != 6) throw new IllegalArgumentException("malformed catalogue row: " + line);
            return row;
        }).toList();
    }
}
