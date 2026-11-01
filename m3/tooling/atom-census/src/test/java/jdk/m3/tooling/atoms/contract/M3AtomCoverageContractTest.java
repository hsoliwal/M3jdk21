/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.atoms.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Repository-resource and dependency-free self-test bindings for Maven/JUnit execution. */
class M3AtomCoverageContractTest {
    private static final Path CONTRACT = contractDirectory();

    private static Path contractDirectory() {
        String configured = System.getProperty("m3.atom.contract.dir");
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured);
        }
        Path direct = Path.of(System.getProperty("user.dir"), "contract");
        if (Files.isDirectory(direct)) {
            return direct;
        }
        return Path.of(System.getProperty("user.dir"), "postimages", "m3", "tooling", "atom-census",
                "contract");
    }


    @Test
    void dependencyFreeRefusalAndAdmissionSuitePasses() throws Exception {
        assertTrue(M3AtomCoverageVerifierSelfTest.runAll(CONTRACT) >= 21);
    }

    @Test
    void gateProfilesAreGeneratedFromTheClosedEnums() throws Exception {
        List<String> expected = new ArrayList<>();
        expected.add("kind\texecutable\tjava_atom\tnative_atom\trequired_gates");
        for (M3AtomKind kind : M3AtomKind.values()) {
            String gates = M3CoverageGate.requiredFor(kind).stream().map(Enum::name)
                    .collect(Collectors.joining(","));
            expected.add(String.join("\t", kind.name(), Boolean.toString(kind.executable()),
                    Boolean.toString(kind.javaAtom()), Boolean.toString(kind.nativeAtom()), gates));
        }
        assertEquals(expected, Files.readAllLines(CONTRACT.resolve("GATE_PROFILES.tsv"), StandardCharsets.UTF_8));
    }

    @Test
    void schemaNamesTheExactParserHeaders() throws Exception {
        List<String> schema = Files.readAllLines(CONTRACT.resolve("ATOM_COVERAGE_SCHEMA.tsv"), StandardCharsets.UTF_8);
        assertEquals("file\tposition\tcolumn\ttype\tconstraint", schema.get(0));
        assertEquals(List.of("atom_id", "source_path", "kind", "owner", "signature", "source_sha256",
                        "pattern_id", "contract_ref", "public_contract_locked"),
                schema.stream().skip(1).filter(line -> line.startsWith("ATOMS.tsv\t"))
                        .map(line -> line.split("\\t", -1)[2]).toList());
        assertEquals(List.of("atom_id", "gate", "status", "evidence_ref", "evidence_sha256", "note"),
                schema.stream().skip(1).filter(line -> line.startsWith("EVIDENCE.tsv\t"))
                        .map(line -> line.split("\\t", -1)[2]).toList());
        assertEquals("atom_id\tsource_path\tkind\towner\tsignature\tsource_sha256\tpattern_id"
                        + "\tcontract_ref\tpublic_contract_locked", M3AtomCoverageLedger.ATOMS_HEADER);
        assertEquals("atom_id\tgate\tstatus\tevidence_ref\tevidence_sha256\tnote",
                M3AtomCoverageLedger.EVIDENCE_HEADER);
    }
}
