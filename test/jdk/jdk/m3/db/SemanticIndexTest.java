// SPDX-License-Identifier: Apache-2.0
package jdk.m3.db;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class SemanticIndexTest {
    @Test
    void leafFingerprintSeparatesExactStructureLogicAndSimHash() {
        var left = SemanticFingerprint.leaf(
                "ATOM",
                List.of("Return", "Binary"),
                List.of("RETURN", "BINARY:Addition", "IDENT_TYPE:Int"),
                "return a + b;");
        var renamed = SemanticFingerprint.leaf(
                "ATOM",
                List.of("Return", "Binary"),
                List.of("RETURN", "BINARY:Addition", "IDENT_TYPE:Int"),
                "return left + right;");
        var changedLogic = SemanticFingerprint.leaf(
                "ATOM",
                List.of("Return", "Binary"),
                List.of("RETURN", "BINARY:Subtraction", "IDENT_TYPE:Int"),
                "return a - b;");

        assertNotEquals(left.exactSha256(), renamed.exactSha256());
        assertEquals(left.structuralSha256(), renamed.structuralSha256());
        assertEquals(left.logicSha256(), renamed.logicSha256());
        assertEquals(left.structuralHash64(), renamed.structuralHash64());
        assertEquals(left.logicHash64(), renamed.logicHash64());
        assertEquals(left.simHash64(), renamed.simHash64());

        assertEquals(left.structuralSha256(), changedLogic.structuralSha256());
        assertNotEquals(left.logicSha256(), changedLogic.logicSha256());
        assertNotEquals(left.logicHash64(), changedLogic.logicHash64());
        assertEquals(64, left.exactSha256().length());
        assertEquals(16, left.structuralHash64Hex().length());
        assertEquals(16, left.logicHash64Hex().length());
        assertEquals(16, left.simHash64Hex().length());
    }

    @Test
    void compositionIsRoleAndCanonicalOrderSensitive() {
        var a = SemanticFingerprint.leaf(
                "ATOM", List.of("Identifier"), List.of("IDENT_TYPE:Int"), "a");
        var b = SemanticFingerprint.leaf(
                "ATOM", List.of("Literal"), List.of("LITERAL:Int:1"), "1");

        var first = SemanticFingerprint.compose(
                "METHOD:calc",
                List.of(
                        new SemanticFingerprint.Component("LEFT", a),
                        new SemanticFingerprint.Component("RIGHT", b)));
        var same = SemanticFingerprint.compose(
                "METHOD:calc",
                List.of(
                        new SemanticFingerprint.Component("LEFT", a),
                        new SemanticFingerprint.Component("RIGHT", b)));
        var reordered = SemanticFingerprint.compose(
                "METHOD:calc",
                List.of(
                        new SemanticFingerprint.Component("RIGHT", b),
                        new SemanticFingerprint.Component("LEFT", a)));
        var rerole = SemanticFingerprint.compose(
                "METHOD:calc",
                List.of(
                        new SemanticFingerprint.Component("ARG", a),
                        new SemanticFingerprint.Component("RIGHT", b)));

        assertEquals(first, same);
        assertNotEquals(first.structuralSha256(), reordered.structuralSha256());
        assertNotEquals(first.logicSha256(), reordered.logicSha256());
        assertNotEquals(first.logicSha256(), rerole.logicSha256());
        assertEquals(0, SemanticFingerprint.hammingDistance(
                first.simHash64(), same.simHash64()));
        assertEquals(64, SemanticFingerprint.hammingDistance(0L, -1L));
        assertTrue(first.normalizedComposition().contains("LEFT#0"));
        assertTrue(first.normalizedComposition().contains("RIGHT#1"));
    }

    @Test
    void hierarchyRecomposesFromAtomsToRepositoryAndQueriesHashes() {
        Fixture fixture = fixture("Addition");
        SemanticIndex index =
                SemanticIndex.of(fixture.nodes(), fixture.edges());

        SemanticNode atom = index.require(fixture.atomId());
        SemanticNode method = index.require(fixture.methodId());
        SemanticNode repository = index.require(fixture.repositoryId());

        assertEquals(List.of(atom), index.children(method.nodeId()));
        assertEquals(List.of(method), index.parents(atom.nodeId()));
        assertEquals(1, index.nodesOfKind(SemanticKind.ATOM).size());
        assertEquals(1, index.nodesOfKind(SemanticKind.FIELD).size());
        assertEquals(1, index.nodesOfKind(SemanticKind.METHOD).size());
        assertEquals(1, index.nodesOfKind(SemanticKind.FILE).size());
        assertEquals(1, index.nodesOfKind(SemanticKind.IMPLEMENTATION).size());
        assertEquals(1, index.nodesOfKind(SemanticKind.DOCUMENTATION).size());
        assertEquals(1, index.nodesOfKind(SemanticKind.PACKAGE).size());
        assertEquals(1, index.nodesOfKind(SemanticKind.MODULE).size());
        assertEquals(1, index.nodesOfKind(SemanticKind.LIBRARY).size());
        assertEquals(1, index.nodesOfKind(SemanticKind.PROJECT).size());
        assertEquals(1, index.nodesOfKind(SemanticKind.REPOSITORY).size());

        assertNotEquals(
                fixture.placeholderMethodFingerprint().logicSha256(),
                method.fingerprint().logicSha256());
        assertTrue(index.exactSha256(atom.fingerprint().exactSha256()).contains(atom));
        assertTrue(index.structuralSha256(atom.fingerprint().structuralSha256()).contains(atom));
        assertTrue(index.logicSha256(atom.fingerprint().logicSha256()).contains(atom));
        assertTrue(index.structuralHash64(atom.fingerprint().structuralHash64()).contains(atom));
        assertTrue(index.logicHash64(atom.fingerprint().logicHash64()).contains(atom));
        assertEquals(repository, index.find(repository.nodeId()).orElseThrow());
        assertTrue(index.find("0".repeat(64)).isEmpty());
        assertEquals(List.of(), index.children(atom.nodeId()));
        assertEquals(List.of(), index.parents(repository.nodeId()));

        List<SemanticIndex.Similarity> exact =
                index.nearSimHash(atom.fingerprint().simHash64(), 0);
        assertTrue(exact.stream().anyMatch(match -> match.node().equals(atom)));
        assertTrue(exact.stream().allMatch(match -> match.hammingDistance() == 0));
    }

    @Test
    void sameShapeDifferentAtomLogicChangesRepositoryLogicNotStructure() {
        SemanticIndex addition =
                SemanticIndex.of(fixture("Addition").nodes(), fixture("Addition").edges());
        SemanticIndex subtraction =
                SemanticIndex.of(fixture("Subtraction").nodes(), fixture("Subtraction").edges());

        SemanticNode left =
                addition.nodesOfKind(SemanticKind.REPOSITORY).getFirst();
        SemanticNode right =
                subtraction.nodesOfKind(SemanticKind.REPOSITORY).getFirst();

        assertEquals(left.fingerprint().structuralSha256(), right.fingerprint().structuralSha256());
        assertNotEquals(left.fingerprint().logicSha256(), right.fingerprint().logicSha256());
    }

    @Test
    void parallelProducerOrderAndDuplicateLocalOrdinalsDoNotChangeCanon() {
        Fixture fixture = twoFileFixture();
        List<SemanticEdge> reversedEdges = new ArrayList<>(fixture.edges());
        java.util.Collections.reverse(reversedEdges);
        List<SemanticNode> reversedNodes = new ArrayList<>(fixture.nodes());
        java.util.Collections.reverse(reversedNodes);

        SemanticIndex first =
                SemanticIndex.of(fixture.nodes(), fixture.edges());
        SemanticIndex second =
                SemanticIndex.of(reversedNodes, reversedEdges);

        assertArrayEquals(first.encode(), second.encode());
        assertEquals(
                first.nodesOfKind(SemanticKind.PACKAGE).getFirst().fingerprint(),
                second.nodesOfKind(SemanticKind.PACKAGE).getFirst().fingerprint());
        assertEquals(2, first.nodesOfKind(SemanticKind.FILE).size());
    }

    @Test
    void unorderedMembershipReordersConvergeButOrderedAtomsDoNot() {
        SemanticNode methodA = leaf(
                SemanticKind.METHOD,
                "order/type#method/a",
                "src/A.java",
                "a()",
                "METHOD_A");
        SemanticNode methodB = leaf(
                SemanticKind.METHOD,
                "order/type#method/b",
                "src/A.java",
                "b()",
                "METHOD_B");
        SemanticNode type = leaf(
                SemanticKind.IMPLEMENTATION,
                "order/type",
                "src/A.java",
                "order.Type",
                "TYPE");

        SemanticIndex methodsFirst = SemanticIndex.of(
                List.of(type, methodA, methodB),
                List.of(
                        new SemanticEdge(type.nodeId(), methodA.nodeId(), "METHOD", 0),
                        new SemanticEdge(type.nodeId(), methodB.nodeId(), "METHOD", 1)));
        SemanticIndex methodsReordered = SemanticIndex.of(
                List.of(methodB, type, methodA),
                List.of(
                        new SemanticEdge(type.nodeId(), methodB.nodeId(), "METHOD", 0),
                        new SemanticEdge(type.nodeId(), methodA.nodeId(), "METHOD", 1)));

        assertArrayEquals(methodsFirst.encode(), methodsReordered.encode());
        assertEquals(
                methodsFirst.require(type.nodeId()).fingerprint(),
                methodsReordered.require(type.nodeId()).fingerprint());

        SemanticNode atomA = leaf(
                SemanticKind.ATOM,
                "order/method#atom/a",
                "src/A.java",
                "A",
                "ATOM_A");
        SemanticNode atomB = leaf(
                SemanticKind.ATOM,
                "order/method#atom/b",
                "src/A.java",
                "B",
                "ATOM_B");
        SemanticNode method = leaf(
                SemanticKind.METHOD,
                "order/method",
                "src/A.java",
                "ordered()",
                "METHOD");

        SemanticIndex atomsFirst = SemanticIndex.of(
                List.of(method, atomA, atomB),
                List.of(
                        new SemanticEdge(method.nodeId(), atomA.nodeId(), "ATOM", 0),
                        new SemanticEdge(method.nodeId(), atomB.nodeId(), "ATOM", 1)));
        SemanticIndex atomsReordered = SemanticIndex.of(
                List.of(method, atomA, atomB),
                List.of(
                        new SemanticEdge(method.nodeId(), atomB.nodeId(), "ATOM", 0),
                        new SemanticEdge(method.nodeId(), atomA.nodeId(), "ATOM", 1)));

        assertNotEquals(
                atomsFirst.require(method.nodeId()).fingerprint().logicSha256(),
                atomsReordered.require(method.nodeId()).fingerprint().logicSha256());
        assertFalse(java.util.Arrays.equals(atomsFirst.encode(), atomsReordered.encode()));
    }

    @Test
    void orderedRoleRejectsTwoDifferentChildrenAtTheSameOrdinal() {
        SemanticNode fieldA = leaf(
                SemanticKind.FIELD,
                "fields/A",
                "src/A.java",
                "A",
                "FIELD_A");
        SemanticNode fieldB = leaf(
                SemanticKind.FIELD,
                "fields/B",
                "src/A.java",
                "B",
                "FIELD_B");
        SemanticNode type = leaf(
                SemanticKind.IMPLEMENTATION,
                "fields/Type",
                "src/A.java",
                "Type",
                "TYPE");

        assertThrows(
                IllegalArgumentException.class,
                () -> SemanticIndex.of(
                        List.of(type, fieldA, fieldB),
                        List.of(
                                new SemanticEdge(
                                        type.nodeId(), fieldA.nodeId(), "FIELD", 0),
                                new SemanticEdge(
                                        type.nodeId(), fieldB.nodeId(), "FIELD", 0))));
    }

    @Test
    void codecIsByteStableAndFailClosedOnCorruption() {
        Fixture fixture = fixture("Addition");
        SemanticIndex index =
                SemanticIndex.of(fixture.nodes(), fixture.edges());

        byte[] encoded = index.encode();
        SemanticIndex decoded = SemanticIndex.decode(encoded);

        assertArrayEquals(encoded, decoded.encode());
        assertEquals(index.nodes(), decoded.nodes());
        assertEquals(index.edges(), decoded.edges());

        byte[] corruptMagic = encoded.clone();
        corruptMagic[0] ^= 0x01;
        assertThrows(
                IllegalArgumentException.class,
                () -> SemanticIndex.decode(corruptMagic));
        assertThrows(
                IllegalArgumentException.class,
                () -> SemanticIndex.decode(new byte[] {1, 2, 3}));
    }

    @Test
    void semanticIndexPersistsThroughExistingContentAddressedM3IndexDb() throws Exception {
        Fixture fixture = fixture("Addition");
        SemanticIndex index =
                SemanticIndex.of(fixture.nodes(), fixture.edges());
        Path root = Files.createTempDirectory("m3index-semantic-");

        String contentHash;
        try (M3DB db = M3DB.open(root)) {
            Artifact artifact = index.store(db, "semantic-index");
            contentHash = artifact.contentSha256();
            assertEquals(SemanticIndex.ARTIFACT_KIND, artifact.kind());
            assertEquals(SemanticIndex.FORMAT_VERSION, artifact.formatVersion());
            assertEquals(index.encode().length, artifact.payloadLength());

            SemanticIndex loaded =
                    SemanticIndex.load(db, "semantic-index");
            assertEquals(index.nodes(), loaded.nodes());
            assertEquals(index.edges(), loaded.edges());

            Artifact alias = index.store(db, "semantic-index-alias");
            assertEquals(contentHash, alias.contentSha256());
            assertEquals(index.encode().length, db.blobBytes());
        }

        try (M3DB reopened = M3DB.open(root)) {
            assertEquals(
                    contentHash,
                    reopened.requireArtifact("semantic-index").contentSha256());
            assertEquals(
                    index.nodes(),
                    SemanticIndex.load(reopened, "semantic-index").nodes());
        }
    }

    @Test
    void conflictsDanglingCyclesInvalidQueriesAndWrongArtifactsAreRejected() throws Exception {
        Fixture fixture = fixture("Addition");
        SemanticNode atom = fixture.nodes().getFirst();
        SemanticNode conflict =
                new SemanticNode(
                        atom.nodeId(),
                        atom.kind(),
                        atom.semanticKey(),
                        "different.java",
                        atom.symbol(),
                        atom.fingerprint());

        assertThrows(
                IllegalArgumentException.class,
                () -> SemanticIndex.of(List.of(atom, conflict), List.of()));
        assertThrows(
                IllegalArgumentException.class,
                () -> SemanticIndex.of(
                        fixture.nodes(),
                        List.of(new SemanticEdge(
                                fixture.methodId(),
                                "0".repeat(64),
                                "ATOM",
                                0))));
        assertThrows(
                IllegalArgumentException.class,
                () -> SemanticIndex.of(
                        List.of(atom),
                        List.of(new SemanticEdge(
                                atom.nodeId(),
                                atom.nodeId(),
                                "SELF",
                                0))));

        SemanticNode cycleA = leaf(
                SemanticKind.FILE, "cycle/A", "A.java", "A", "A");
        SemanticNode cycleB = leaf(
                SemanticKind.FILE, "cycle/B", "B.java", "B", "B");
        assertThrows(
                IllegalArgumentException.class,
                () -> SemanticIndex.of(
                        List.of(cycleA, cycleB),
                        List.of(
                                new SemanticEdge(
                                        cycleA.nodeId(), cycleB.nodeId(), "NEXT", 0),
                                new SemanticEdge(
                                        cycleB.nodeId(), cycleA.nodeId(), "NEXT", 0))));

        SemanticIndex index =
                SemanticIndex.of(fixture.nodes(), fixture.edges());
        assertThrows(IllegalArgumentException.class, () -> index.require("0".repeat(64)));
        assertThrows(IllegalArgumentException.class, () -> index.exactSha256("not-sha"));
        assertThrows(IllegalArgumentException.class, () -> index.nearSimHash(0L, -1));
        assertThrows(IllegalArgumentException.class, () -> index.nearSimHash(0L, 65));

        Path root = Files.createTempDirectory("m3index-wrong-");
        try (M3DB db = M3DB.open(root)) {
            db.putArtifact("wrong-kind", "other", 1, new byte[] {1});
            assertThrows(
                    IOException.class,
                    () -> SemanticIndex.load(db, "wrong-kind"));

            db.putArtifact(
                    "wrong-version",
                    SemanticIndex.ARTIFACT_KIND,
                    2,
                    new byte[] {1});
            assertThrows(
                    IOException.class,
                    () -> SemanticIndex.load(db, "wrong-version"));
        }
    }

    @Test
    void constructorsRejectInvalidSemanticIdentityAndMetadata() {
        var fingerprint = SemanticFingerprint.leaf(
                "ATOM", List.of("Return"), List.of("RETURN"), "return 1;");

        assertThrows(
                IllegalArgumentException.class,
                () -> new SemanticNode(
                        "0".repeat(64),
                        SemanticKind.ATOM,
                        "key",
                        "",
                        "symbol",
                        fingerprint));
        assertThrows(
                IllegalArgumentException.class,
                () -> new SemanticEdge(
                        "0".repeat(64),
                        "1".repeat(64),
                        "",
                        0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new SemanticEdge(
                        "0".repeat(64),
                        "1".repeat(64),
                        "ROLE",
                        -1));
        assertThrows(
                IllegalArgumentException.class,
                () -> new SemanticFingerprint(
                        "bad",
                        "0".repeat(64),
                        "0".repeat(64),
                        0,
                        0,
                        0,
                        ""));
        assertThrows(
                IllegalArgumentException.class,
                () -> SemanticFingerprint.leaf(
                        " ",
                        List.of(),
                        List.of(),
                        "x"));
    }

    private static Fixture fixture(String operator) {
        SemanticNode atom = leaf(
                SemanticKind.ATOM,
                "repo/type/A#method/calc#atom/0",
                "src/A.java",
                "Return",
                operator);
        var placeholder = SemanticFingerprint.leaf(
                "METHOD_PLACEHOLDER", List.of("Method"), List.of("calc"), "calc");
        SemanticNode method = node(
                SemanticKind.METHOD,
                "repo/type/A#method/calc",
                "src/A.java",
                "calc()",
                placeholder);
        SemanticNode field = leaf(
                SemanticKind.FIELD,
                "repo/type/A#field/SCALE",
                "src/A.java",
                "SCALE",
                "FIELD");
        SemanticNode implementation = leaf(
                SemanticKind.IMPLEMENTATION,
                "repo/type/A",
                "src/A.java",
                "A",
                "TYPE");
        SemanticNode docs = leaf(
                SemanticKind.DOCUMENTATION,
                "repo/file/src/A.java#docs",
                "src/A.java",
                "documentation",
                "DOCS");
        SemanticNode file = leaf(
                SemanticKind.FILE,
                "repo/file/src/A.java",
                "src/A.java",
                "src/A.java",
                "FILE");
        SemanticNode pkg = leaf(
                SemanticKind.PACKAGE,
                "repo/package/example",
                "",
                "example",
                "PACKAGE");
        SemanticNode module = leaf(
                SemanticKind.MODULE,
                "repo/module/java.base",
                "",
                "java.base",
                "MODULE");
        SemanticNode library = leaf(
                SemanticKind.LIBRARY,
                "repo/library/M3JDK21",
                "",
                "M3JDK21",
                "LIBRARY");
        SemanticNode project = leaf(
                SemanticKind.PROJECT,
                "repo/project/M3JDK21",
                "",
                "M3JDK21",
                "PROJECT");
        SemanticNode repository = leaf(
                SemanticKind.REPOSITORY,
                "repo/hsoliwal/M3jdk21",
                "",
                "hsoliwal/M3jdk21",
                "REPOSITORY");

        List<SemanticNode> nodes = List.of(
                atom, method, field, implementation, docs, file,
                pkg, module, library, project, repository);
        List<SemanticEdge> edges = List.of(
                new SemanticEdge(method.nodeId(), atom.nodeId(), "ATOM", 0),
                new SemanticEdge(implementation.nodeId(), field.nodeId(), "FIELD", 0),
                new SemanticEdge(implementation.nodeId(), method.nodeId(), "METHOD", 1),
                new SemanticEdge(file.nodeId(), docs.nodeId(), "DOCUMENTATION", 0),
                new SemanticEdge(file.nodeId(), implementation.nodeId(), "IMPLEMENTATION", 1),
                new SemanticEdge(pkg.nodeId(), file.nodeId(), "FILE", 0),
                new SemanticEdge(module.nodeId(), pkg.nodeId(), "PACKAGE", 0),
                new SemanticEdge(library.nodeId(), module.nodeId(), "MODULE", 0),
                new SemanticEdge(project.nodeId(), library.nodeId(), "LIBRARY", 0),
                new SemanticEdge(repository.nodeId(), project.nodeId(), "PROJECT", 0));

        return new Fixture(
                nodes,
                edges,
                atom.nodeId(),
                method.nodeId(),
                repository.nodeId(),
                placeholder);
    }

    private static Fixture twoFileFixture() {
        Fixture base = fixture("Addition");
        SemanticNode secondFile = leaf(
                SemanticKind.FILE,
                "repo/file/src/B.java",
                "src/B.java",
                "src/B.java",
                "FILE_B");
        SemanticNode secondType = leaf(
                SemanticKind.INTERFACE,
                "repo/type/B",
                "src/B.java",
                "B",
                "INTERFACE");
        String packageId = base.nodes().stream()
                .filter(node -> node.kind() == SemanticKind.PACKAGE)
                .findFirst()
                .orElseThrow()
                .nodeId();

        ArrayList<SemanticNode> nodes = new ArrayList<>(base.nodes());
        nodes.add(secondFile);
        nodes.add(secondType);
        ArrayList<SemanticEdge> edges = new ArrayList<>(base.edges());
        edges.add(new SemanticEdge(secondFile.nodeId(), secondType.nodeId(), "INTERFACE", 0));
        edges.add(new SemanticEdge(packageId, secondFile.nodeId(), "FILE", 0));
        return new Fixture(
                List.copyOf(nodes),
                List.copyOf(edges),
                base.atomId(),
                base.methodId(),
                base.repositoryId(),
                base.placeholderMethodFingerprint());
    }

    private static SemanticNode leaf(
            SemanticKind kind,
            String key,
            String sourcePath,
            String symbol,
            String logic) {
        return node(
                kind,
                key,
                sourcePath,
                symbol,
                SemanticFingerprint.leaf(
                        kind.name(),
                        List.of("SHAPE:" + kind),
                        List.of("LOGIC:" + logic),
                        logic));
    }

    private static SemanticNode node(
            SemanticKind kind,
            String key,
            String sourcePath,
            String symbol,
            SemanticFingerprint fingerprint) {
        return new SemanticNode(
                SemanticIndex.nodeId(kind, key),
                kind,
                key,
                sourcePath,
                symbol,
                fingerprint);
    }

    private record Fixture(
            List<SemanticNode> nodes,
            List<SemanticEdge> edges,
            String atomId,
            String methodId,
            String repositoryId,
            SemanticFingerprint placeholderMethodFingerprint) {}
}
