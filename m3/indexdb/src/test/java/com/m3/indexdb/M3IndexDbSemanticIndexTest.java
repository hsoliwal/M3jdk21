// SPDX-License-Identifier: Apache-2.0
package com.m3.indexdb;

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

final class M3IndexDbSemanticIndexTest {
    @Test
    void leafFingerprintSeparatesExactStructureLogicAndSimHash() {
        var left = M3IndexDbSemanticFingerprint.leaf(
                "ATOM",
                List.of("Return", "Binary"),
                List.of("RETURN", "BINARY:Addition", "IDENT_TYPE:Int"),
                "return a + b;");
        var renamed = M3IndexDbSemanticFingerprint.leaf(
                "ATOM",
                List.of("Return", "Binary"),
                List.of("RETURN", "BINARY:Addition", "IDENT_TYPE:Int"),
                "return left + right;");
        var changedLogic = M3IndexDbSemanticFingerprint.leaf(
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
        var a = M3IndexDbSemanticFingerprint.leaf(
                "ATOM", List.of("Identifier"), List.of("IDENT_TYPE:Int"), "a");
        var b = M3IndexDbSemanticFingerprint.leaf(
                "ATOM", List.of("Literal"), List.of("LITERAL:Int:1"), "1");

        var first = M3IndexDbSemanticFingerprint.compose(
                "METHOD:calc",
                List.of(
                        new M3IndexDbSemanticFingerprint.Component("LEFT", a),
                        new M3IndexDbSemanticFingerprint.Component("RIGHT", b)));
        var same = M3IndexDbSemanticFingerprint.compose(
                "METHOD:calc",
                List.of(
                        new M3IndexDbSemanticFingerprint.Component("LEFT", a),
                        new M3IndexDbSemanticFingerprint.Component("RIGHT", b)));
        var reordered = M3IndexDbSemanticFingerprint.compose(
                "METHOD:calc",
                List.of(
                        new M3IndexDbSemanticFingerprint.Component("RIGHT", b),
                        new M3IndexDbSemanticFingerprint.Component("LEFT", a)));
        var rerole = M3IndexDbSemanticFingerprint.compose(
                "METHOD:calc",
                List.of(
                        new M3IndexDbSemanticFingerprint.Component("ARG", a),
                        new M3IndexDbSemanticFingerprint.Component("RIGHT", b)));

        assertEquals(first, same);
        assertNotEquals(first.structuralSha256(), reordered.structuralSha256());
        assertNotEquals(first.logicSha256(), reordered.logicSha256());
        assertNotEquals(first.logicSha256(), rerole.logicSha256());
        assertEquals(0, M3IndexDbSemanticFingerprint.hammingDistance(
                first.simHash64(), same.simHash64()));
        assertEquals(64, M3IndexDbSemanticFingerprint.hammingDistance(0L, -1L));
        assertTrue(first.normalizedComposition().contains("LEFT#0"));
        assertTrue(first.normalizedComposition().contains("RIGHT#1"));
    }

    @Test
    void hierarchyRecomposesFromAtomsToRepositoryAndQueriesHashes() {
        Fixture fixture = fixture("Addition");
        M3IndexDbSemanticIndex index =
                M3IndexDbSemanticIndex.of(fixture.nodes(), fixture.edges());

        M3IndexDbSemanticNode atom = index.require(fixture.atomId());
        M3IndexDbSemanticNode method = index.require(fixture.methodId());
        M3IndexDbSemanticNode repository = index.require(fixture.repositoryId());

        assertEquals(List.of(atom), index.children(method.nodeId()));
        assertEquals(List.of(method), index.parents(atom.nodeId()));
        assertEquals(1, index.nodesOfKind(M3IndexDbSemanticKind.ATOM).size());
        assertEquals(1, index.nodesOfKind(M3IndexDbSemanticKind.FIELD).size());
        assertEquals(1, index.nodesOfKind(M3IndexDbSemanticKind.METHOD).size());
        assertEquals(1, index.nodesOfKind(M3IndexDbSemanticKind.FILE).size());
        assertEquals(1, index.nodesOfKind(M3IndexDbSemanticKind.IMPLEMENTATION).size());
        assertEquals(1, index.nodesOfKind(M3IndexDbSemanticKind.DOCUMENTATION).size());
        assertEquals(1, index.nodesOfKind(M3IndexDbSemanticKind.PACKAGE).size());
        assertEquals(1, index.nodesOfKind(M3IndexDbSemanticKind.MODULE).size());
        assertEquals(1, index.nodesOfKind(M3IndexDbSemanticKind.LIBRARY).size());
        assertEquals(1, index.nodesOfKind(M3IndexDbSemanticKind.PROJECT).size());
        assertEquals(1, index.nodesOfKind(M3IndexDbSemanticKind.REPOSITORY).size());

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

        List<M3IndexDbSemanticIndex.Similarity> exact =
                index.nearSimHash(atom.fingerprint().simHash64(), 0);
        assertTrue(exact.stream().anyMatch(match -> match.node().equals(atom)));
        assertTrue(exact.stream().allMatch(match -> match.hammingDistance() == 0));
    }

    @Test
    void sameShapeDifferentAtomLogicChangesRepositoryLogicNotStructure() {
        M3IndexDbSemanticIndex addition =
                M3IndexDbSemanticIndex.of(fixture("Addition").nodes(), fixture("Addition").edges());
        M3IndexDbSemanticIndex subtraction =
                M3IndexDbSemanticIndex.of(fixture("Subtraction").nodes(), fixture("Subtraction").edges());

        M3IndexDbSemanticNode left =
                addition.nodesOfKind(M3IndexDbSemanticKind.REPOSITORY).getFirst();
        M3IndexDbSemanticNode right =
                subtraction.nodesOfKind(M3IndexDbSemanticKind.REPOSITORY).getFirst();

        assertEquals(left.fingerprint().structuralSha256(), right.fingerprint().structuralSha256());
        assertNotEquals(left.fingerprint().logicSha256(), right.fingerprint().logicSha256());
    }

    @Test
    void parallelProducerOrderAndDuplicateLocalOrdinalsDoNotChangeCanon() {
        Fixture fixture = twoFileFixture();
        List<M3IndexDbSemanticEdge> reversedEdges = new ArrayList<>(fixture.edges());
        java.util.Collections.reverse(reversedEdges);
        List<M3IndexDbSemanticNode> reversedNodes = new ArrayList<>(fixture.nodes());
        java.util.Collections.reverse(reversedNodes);

        M3IndexDbSemanticIndex first =
                M3IndexDbSemanticIndex.of(fixture.nodes(), fixture.edges());
        M3IndexDbSemanticIndex second =
                M3IndexDbSemanticIndex.of(reversedNodes, reversedEdges);

        assertArrayEquals(first.encode(), second.encode());
        assertEquals(
                first.nodesOfKind(M3IndexDbSemanticKind.PACKAGE).getFirst().fingerprint(),
                second.nodesOfKind(M3IndexDbSemanticKind.PACKAGE).getFirst().fingerprint());
        assertEquals(2, first.nodesOfKind(M3IndexDbSemanticKind.FILE).size());
    }

    @Test
    void unorderedMembershipReordersConvergeButOrderedAtomsDoNot() {
        M3IndexDbSemanticNode methodA = leaf(
                M3IndexDbSemanticKind.METHOD,
                "order/type#method/a",
                "src/A.java",
                "a()",
                "METHOD_A");
        M3IndexDbSemanticNode methodB = leaf(
                M3IndexDbSemanticKind.METHOD,
                "order/type#method/b",
                "src/A.java",
                "b()",
                "METHOD_B");
        M3IndexDbSemanticNode type = leaf(
                M3IndexDbSemanticKind.IMPLEMENTATION,
                "order/type",
                "src/A.java",
                "order.Type",
                "TYPE");

        M3IndexDbSemanticIndex methodsFirst = M3IndexDbSemanticIndex.of(
                List.of(type, methodA, methodB),
                List.of(
                        new M3IndexDbSemanticEdge(type.nodeId(), methodA.nodeId(), "METHOD", 0),
                        new M3IndexDbSemanticEdge(type.nodeId(), methodB.nodeId(), "METHOD", 1)));
        M3IndexDbSemanticIndex methodsReordered = M3IndexDbSemanticIndex.of(
                List.of(methodB, type, methodA),
                List.of(
                        new M3IndexDbSemanticEdge(type.nodeId(), methodB.nodeId(), "METHOD", 0),
                        new M3IndexDbSemanticEdge(type.nodeId(), methodA.nodeId(), "METHOD", 1)));

        assertArrayEquals(methodsFirst.encode(), methodsReordered.encode());
        assertEquals(
                methodsFirst.require(type.nodeId()).fingerprint(),
                methodsReordered.require(type.nodeId()).fingerprint());

        M3IndexDbSemanticNode atomA = leaf(
                M3IndexDbSemanticKind.ATOM,
                "order/method#atom/a",
                "src/A.java",
                "A",
                "ATOM_A");
        M3IndexDbSemanticNode atomB = leaf(
                M3IndexDbSemanticKind.ATOM,
                "order/method#atom/b",
                "src/A.java",
                "B",
                "ATOM_B");
        M3IndexDbSemanticNode method = leaf(
                M3IndexDbSemanticKind.METHOD,
                "order/method",
                "src/A.java",
                "ordered()",
                "METHOD");

        M3IndexDbSemanticIndex atomsFirst = M3IndexDbSemanticIndex.of(
                List.of(method, atomA, atomB),
                List.of(
                        new M3IndexDbSemanticEdge(method.nodeId(), atomA.nodeId(), "ATOM", 0),
                        new M3IndexDbSemanticEdge(method.nodeId(), atomB.nodeId(), "ATOM", 1)));
        M3IndexDbSemanticIndex atomsReordered = M3IndexDbSemanticIndex.of(
                List.of(method, atomA, atomB),
                List.of(
                        new M3IndexDbSemanticEdge(method.nodeId(), atomB.nodeId(), "ATOM", 0),
                        new M3IndexDbSemanticEdge(method.nodeId(), atomA.nodeId(), "ATOM", 1)));

        assertNotEquals(
                atomsFirst.require(method.nodeId()).fingerprint().logicSha256(),
                atomsReordered.require(method.nodeId()).fingerprint().logicSha256());
        assertFalse(java.util.Arrays.equals(atomsFirst.encode(), atomsReordered.encode()));
    }

    @Test
    void orderedRoleRejectsTwoDifferentChildrenAtTheSameOrdinal() {
        M3IndexDbSemanticNode fieldA = leaf(
                M3IndexDbSemanticKind.FIELD,
                "fields/A",
                "src/A.java",
                "A",
                "FIELD_A");
        M3IndexDbSemanticNode fieldB = leaf(
                M3IndexDbSemanticKind.FIELD,
                "fields/B",
                "src/A.java",
                "B",
                "FIELD_B");
        M3IndexDbSemanticNode type = leaf(
                M3IndexDbSemanticKind.IMPLEMENTATION,
                "fields/Type",
                "src/A.java",
                "Type",
                "TYPE");

        assertThrows(
                IllegalArgumentException.class,
                () -> M3IndexDbSemanticIndex.of(
                        List.of(type, fieldA, fieldB),
                        List.of(
                                new M3IndexDbSemanticEdge(
                                        type.nodeId(), fieldA.nodeId(), "FIELD", 0),
                                new M3IndexDbSemanticEdge(
                                        type.nodeId(), fieldB.nodeId(), "FIELD", 0))));
    }

    @Test
    void codecIsByteStableAndFailClosedOnCorruption() {
        Fixture fixture = fixture("Addition");
        M3IndexDbSemanticIndex index =
                M3IndexDbSemanticIndex.of(fixture.nodes(), fixture.edges());

        byte[] encoded = index.encode();
        M3IndexDbSemanticIndex decoded = M3IndexDbSemanticIndex.decode(encoded);

        assertArrayEquals(encoded, decoded.encode());
        assertEquals(index.nodes(), decoded.nodes());
        assertEquals(index.edges(), decoded.edges());

        byte[] corruptMagic = encoded.clone();
        corruptMagic[0] ^= 0x01;
        assertThrows(
                IllegalArgumentException.class,
                () -> M3IndexDbSemanticIndex.decode(corruptMagic));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3IndexDbSemanticIndex.decode(new byte[] {1, 2, 3}));
    }

    @Test
    void semanticIndexPersistsThroughExistingContentAddressedM3IndexDb() throws Exception {
        Fixture fixture = fixture("Addition");
        M3IndexDbSemanticIndex index =
                M3IndexDbSemanticIndex.of(fixture.nodes(), fixture.edges());
        Path root = Files.createTempDirectory("m3index-semantic-");

        String contentHash;
        try (M3IndexDB db = M3IndexDB.open(root)) {
            M3IndexDbArtifact artifact = index.store(db, "semantic-index");
            contentHash = artifact.contentSha256();
            assertEquals(M3IndexDbSemanticIndex.ARTIFACT_KIND, artifact.kind());
            assertEquals(M3IndexDbSemanticIndex.FORMAT_VERSION, artifact.formatVersion());
            assertEquals(index.encode().length, artifact.payloadLength());

            M3IndexDbSemanticIndex loaded =
                    M3IndexDbSemanticIndex.load(db, "semantic-index");
            assertEquals(index.nodes(), loaded.nodes());
            assertEquals(index.edges(), loaded.edges());

            M3IndexDbArtifact alias = index.store(db, "semantic-index-alias");
            assertEquals(contentHash, alias.contentSha256());
            assertEquals(index.encode().length, db.blobBytes());
        }

        try (M3IndexDB reopened = M3IndexDB.open(root)) {
            assertEquals(
                    contentHash,
                    reopened.requireArtifact("semantic-index").contentSha256());
            assertEquals(
                    index.nodes(),
                    M3IndexDbSemanticIndex.load(reopened, "semantic-index").nodes());
        }
    }

    @Test
    void conflictsDanglingCyclesInvalidQueriesAndWrongArtifactsAreRejected() throws Exception {
        Fixture fixture = fixture("Addition");
        M3IndexDbSemanticNode atom = fixture.nodes().getFirst();
        M3IndexDbSemanticNode conflict =
                new M3IndexDbSemanticNode(
                        atom.nodeId(),
                        atom.kind(),
                        atom.semanticKey(),
                        "different.java",
                        atom.symbol(),
                        atom.fingerprint());

        assertThrows(
                IllegalArgumentException.class,
                () -> M3IndexDbSemanticIndex.of(List.of(atom, conflict), List.of()));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3IndexDbSemanticIndex.of(
                        fixture.nodes(),
                        List.of(new M3IndexDbSemanticEdge(
                                fixture.methodId(),
                                "0".repeat(64),
                                "ATOM",
                                0))));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3IndexDbSemanticIndex.of(
                        List.of(atom),
                        List.of(new M3IndexDbSemanticEdge(
                                atom.nodeId(),
                                atom.nodeId(),
                                "SELF",
                                0))));

        M3IndexDbSemanticNode cycleA = leaf(
                M3IndexDbSemanticKind.FILE, "cycle/A", "A.java", "A", "A");
        M3IndexDbSemanticNode cycleB = leaf(
                M3IndexDbSemanticKind.FILE, "cycle/B", "B.java", "B", "B");
        assertThrows(
                IllegalArgumentException.class,
                () -> M3IndexDbSemanticIndex.of(
                        List.of(cycleA, cycleB),
                        List.of(
                                new M3IndexDbSemanticEdge(
                                        cycleA.nodeId(), cycleB.nodeId(), "NEXT", 0),
                                new M3IndexDbSemanticEdge(
                                        cycleB.nodeId(), cycleA.nodeId(), "NEXT", 0))));

        M3IndexDbSemanticIndex index =
                M3IndexDbSemanticIndex.of(fixture.nodes(), fixture.edges());
        assertThrows(IllegalArgumentException.class, () -> index.require("0".repeat(64)));
        assertThrows(IllegalArgumentException.class, () -> index.exactSha256("not-sha"));
        assertThrows(IllegalArgumentException.class, () -> index.nearSimHash(0L, -1));
        assertThrows(IllegalArgumentException.class, () -> index.nearSimHash(0L, 65));

        Path root = Files.createTempDirectory("m3index-wrong-");
        try (M3IndexDB db = M3IndexDB.open(root)) {
            db.putArtifact("wrong-kind", "other", 1, new byte[] {1});
            assertThrows(
                    IOException.class,
                    () -> M3IndexDbSemanticIndex.load(db, "wrong-kind"));

            db.putArtifact(
                    "wrong-version",
                    M3IndexDbSemanticIndex.ARTIFACT_KIND,
                    2,
                    new byte[] {1});
            assertThrows(
                    IOException.class,
                    () -> M3IndexDbSemanticIndex.load(db, "wrong-version"));
        }
    }

    @Test
    void constructorsRejectInvalidSemanticIdentityAndMetadata() {
        var fingerprint = M3IndexDbSemanticFingerprint.leaf(
                "ATOM", List.of("Return"), List.of("RETURN"), "return 1;");

        assertThrows(
                IllegalArgumentException.class,
                () -> new M3IndexDbSemanticNode(
                        "0".repeat(64),
                        M3IndexDbSemanticKind.ATOM,
                        "key",
                        "",
                        "symbol",
                        fingerprint));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3IndexDbSemanticEdge(
                        "0".repeat(64),
                        "1".repeat(64),
                        "",
                        0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3IndexDbSemanticEdge(
                        "0".repeat(64),
                        "1".repeat(64),
                        "ROLE",
                        -1));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3IndexDbSemanticFingerprint(
                        "bad",
                        "0".repeat(64),
                        "0".repeat(64),
                        0,
                        0,
                        0,
                        ""));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3IndexDbSemanticFingerprint.leaf(
                        " ",
                        List.of(),
                        List.of(),
                        "x"));
    }

    private static Fixture fixture(String operator) {
        M3IndexDbSemanticNode atom = leaf(
                M3IndexDbSemanticKind.ATOM,
                "repo/type/A#method/calc#atom/0",
                "src/A.java",
                "Return",
                operator);
        var placeholder = M3IndexDbSemanticFingerprint.leaf(
                "METHOD_PLACEHOLDER", List.of("Method"), List.of("calc"), "calc");
        M3IndexDbSemanticNode method = node(
                M3IndexDbSemanticKind.METHOD,
                "repo/type/A#method/calc",
                "src/A.java",
                "calc()",
                placeholder);
        M3IndexDbSemanticNode field = leaf(
                M3IndexDbSemanticKind.FIELD,
                "repo/type/A#field/SCALE",
                "src/A.java",
                "SCALE",
                "FIELD");
        M3IndexDbSemanticNode implementation = leaf(
                M3IndexDbSemanticKind.IMPLEMENTATION,
                "repo/type/A",
                "src/A.java",
                "A",
                "TYPE");
        M3IndexDbSemanticNode docs = leaf(
                M3IndexDbSemanticKind.DOCUMENTATION,
                "repo/file/src/A.java#docs",
                "src/A.java",
                "documentation",
                "DOCS");
        M3IndexDbSemanticNode file = leaf(
                M3IndexDbSemanticKind.FILE,
                "repo/file/src/A.java",
                "src/A.java",
                "src/A.java",
                "FILE");
        M3IndexDbSemanticNode pkg = leaf(
                M3IndexDbSemanticKind.PACKAGE,
                "repo/package/example",
                "",
                "example",
                "PACKAGE");
        M3IndexDbSemanticNode module = leaf(
                M3IndexDbSemanticKind.MODULE,
                "repo/module/java.base",
                "",
                "java.base",
                "MODULE");
        M3IndexDbSemanticNode library = leaf(
                M3IndexDbSemanticKind.LIBRARY,
                "repo/library/M3JDK21",
                "",
                "M3JDK21",
                "LIBRARY");
        M3IndexDbSemanticNode project = leaf(
                M3IndexDbSemanticKind.PROJECT,
                "repo/project/M3JDK21",
                "",
                "M3JDK21",
                "PROJECT");
        M3IndexDbSemanticNode repository = leaf(
                M3IndexDbSemanticKind.REPOSITORY,
                "repo/hsoliwal/M3jdk21",
                "",
                "hsoliwal/M3jdk21",
                "REPOSITORY");

        List<M3IndexDbSemanticNode> nodes = List.of(
                atom, method, field, implementation, docs, file,
                pkg, module, library, project, repository);
        List<M3IndexDbSemanticEdge> edges = List.of(
                new M3IndexDbSemanticEdge(method.nodeId(), atom.nodeId(), "ATOM", 0),
                new M3IndexDbSemanticEdge(implementation.nodeId(), field.nodeId(), "FIELD", 0),
                new M3IndexDbSemanticEdge(implementation.nodeId(), method.nodeId(), "METHOD", 1),
                new M3IndexDbSemanticEdge(file.nodeId(), docs.nodeId(), "DOCUMENTATION", 0),
                new M3IndexDbSemanticEdge(file.nodeId(), implementation.nodeId(), "IMPLEMENTATION", 1),
                new M3IndexDbSemanticEdge(pkg.nodeId(), file.nodeId(), "FILE", 0),
                new M3IndexDbSemanticEdge(module.nodeId(), pkg.nodeId(), "PACKAGE", 0),
                new M3IndexDbSemanticEdge(library.nodeId(), module.nodeId(), "MODULE", 0),
                new M3IndexDbSemanticEdge(project.nodeId(), library.nodeId(), "LIBRARY", 0),
                new M3IndexDbSemanticEdge(repository.nodeId(), project.nodeId(), "PROJECT", 0));

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
        M3IndexDbSemanticNode secondFile = leaf(
                M3IndexDbSemanticKind.FILE,
                "repo/file/src/B.java",
                "src/B.java",
                "src/B.java",
                "FILE_B");
        M3IndexDbSemanticNode secondType = leaf(
                M3IndexDbSemanticKind.INTERFACE,
                "repo/type/B",
                "src/B.java",
                "B",
                "INTERFACE");
        String packageId = base.nodes().stream()
                .filter(node -> node.kind() == M3IndexDbSemanticKind.PACKAGE)
                .findFirst()
                .orElseThrow()
                .nodeId();

        ArrayList<M3IndexDbSemanticNode> nodes = new ArrayList<>(base.nodes());
        nodes.add(secondFile);
        nodes.add(secondType);
        ArrayList<M3IndexDbSemanticEdge> edges = new ArrayList<>(base.edges());
        edges.add(new M3IndexDbSemanticEdge(secondFile.nodeId(), secondType.nodeId(), "INTERFACE", 0));
        edges.add(new M3IndexDbSemanticEdge(packageId, secondFile.nodeId(), "FILE", 0));
        return new Fixture(
                List.copyOf(nodes),
                List.copyOf(edges),
                base.atomId(),
                base.methodId(),
                base.repositoryId(),
                base.placeholderMethodFingerprint());
    }

    private static M3IndexDbSemanticNode leaf(
            M3IndexDbSemanticKind kind,
            String key,
            String sourcePath,
            String symbol,
            String logic) {
        return node(
                kind,
                key,
                sourcePath,
                symbol,
                M3IndexDbSemanticFingerprint.leaf(
                        kind.name(),
                        List.of("SHAPE:" + kind),
                        List.of("LOGIC:" + logic),
                        logic));
    }

    private static M3IndexDbSemanticNode node(
            M3IndexDbSemanticKind kind,
            String key,
            String sourcePath,
            String symbol,
            M3IndexDbSemanticFingerprint fingerprint) {
        return new M3IndexDbSemanticNode(
                M3IndexDbSemanticIndex.nodeId(kind, key),
                kind,
                key,
                sourcePath,
                symbol,
                fingerprint);
    }

    private record Fixture(
            List<M3IndexDbSemanticNode> nodes,
            List<M3IndexDbSemanticEdge> edges,
            String atomId,
            String methodId,
            String repositoryId,
            M3IndexDbSemanticFingerprint placeholderMethodFingerprint) {}
}
