// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.eclipse.framework;

import static org.junit.jupiter.api.Assertions.*;

import com.m3.collections.M3IndexMaskCompactor;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.junit.jupiter.api.Test;

class M3EclipseResourceChangeSnapshotTest {
    private static final M3IndexMaskCompactor.Backend JAVA = M3IndexMaskCompactor.Backend.JAVA;
    private static final M3IndexMaskCompactor.Backend NATIVE = M3IndexMaskCompactor.Backend.NATIVE_REQUIRED;

    @Test void legacyCaptureRetainsOriginalCallbacksAndNeverReadsMetadata() {
        Node child = new Node("A.java", IResource.FILE, IResourceDelta.ADDED, IResourceDelta.CONTENT);
        Node root = new Node("project", IResource.PROJECT, IResourceDelta.CHANGED, 0, child);
        root.allowMetadata = child.allowMetadata = false;
        M3EclipseResourceDeltaSnapshot snapshot = M3EclipseResourceDeltaSnapshot.capture(root.delta, 2);
        assertEquals(2, snapshot.size());
        assertSame(child.delta, snapshot.at(1));
        assertEquals(0, snapshot.parentOrdinal(1));
        assertFalse(snapshot.hasCapturedChanges());
        for (Node node : List.of(root, child)) {
            assertEquals(1, node.kindReads);
            assertEquals(1, node.childrenReads);
            assertEquals(0, node.flagReads);
            assertEquals(0, node.resourceReads);
        }
        assertThrows(IllegalStateException.class, () -> snapshot.capturedFlags(0));
        assertThrows(IllegalStateException.class, () -> snapshot.capturedWorkState(0));
        assertThrows(IllegalStateException.class, () -> snapshot.selectByFlags(0, 0));
        assertThrows(IllegalStateException.class, () -> snapshot.selectWorkOrdinals(0, 0, JAVA));
    }

    @Test void fullFlagsIncludingSignBitAreFrozenAndSelectedWithoutNarrowing() {
        Node first = new Node("A.java", IResource.FILE, IResourceDelta.CHANGED,
                Integer.MIN_VALUE | IResourceDelta.CONTENT);
        Node second = new Node("B.class", IResource.FILE, IResourceDelta.CHANGED, IResourceDelta.ENCODING);
        Node root = new Node("project", IResource.PROJECT, IResourceDelta.CHANGED, 0, first, second);
        M3EclipseResourceDeltaSnapshot snapshot = M3EclipseResourceDeltaSnapshot.captureChanges(root.delta, 3);
        assertTrue(snapshot.hasCapturedChanges());
        int originalState = snapshot.capturedWorkState(1);
        first.flags = 0;
        first.name = "renamed.txt";
        first.kind = IResourceDelta.REMOVED;
        assertEquals(Integer.MIN_VALUE | IResourceDelta.CONTENT, snapshot.capturedFlags(1));
        assertEquals(originalState, snapshot.capturedWorkState(1));
        assertEquals(IResourceDelta.CHANGED, snapshot.capturedKind(1));
        List<IResourceDelta> highBit = snapshot.selectByFlags(Integer.MIN_VALUE, IResourceDelta.ENCODING);
        assertEquals(1, highBit.size());
        assertSame(first.delta, highBit.getFirst());
        assertEquals(0, snapshot.selectByFlags(Integer.MIN_VALUE, Integer.MIN_VALUE).size());
        List<IResourceDelta> all = snapshot.selectByFlags(0, 0);
        assertEquals(3, all.size());
        assertSame(root.delta, all.get(0));
        assertSame(first.delta, all.get(1));
        assertSame(second.delta, all.get(2));
        assertThrows(UnsupportedOperationException.class, () -> highBit.clear());
        assertThrows(IndexOutOfBoundsException.class, () -> snapshot.capturedFlags(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> snapshot.capturedWorkState(3));
        for (Node node : List.of(root, first, second)) {
            assertEquals(1, node.kindReads);
            assertEquals(1, node.childrenReads);
            assertEquals(1, node.flagReads);
            assertEquals(1, node.resourceReads);
            assertEquals(1, node.typeReads);
            assertEquals(node.type == IResource.FILE ? 1 : 0, node.nameReads);
        }
    }

    @Test void sourceClassAndDescriptorNominationsAreCaseSensitive() {
        String[] names = {"A.java", "A.class", "pom.xml", ".classpath", ".project", "module-info.java",
                "MANIFEST.MF", "build.xml", "build.gradle", "build.gradle.kts", "settings.gradle",
                "settings.gradle.kts", "A.JAVA", "POM.XML", "notes.txt"};
        Node[] children = Arrays.stream(names).map(name ->
                new Node(name, IResource.FILE, IResourceDelta.CHANGED, IResourceDelta.MARKERS))
                .toArray(Node[]::new);
        Node root = new Node("project", IResource.PROJECT, IResourceDelta.CHANGED, 0, children);
        M3EclipseResourceDeltaSnapshot snapshot = M3EclipseResourceDeltaSnapshot.captureChanges(root.delta, 16);
        assertEquals(M3EclipseResourceDeltaSnapshot.CONTAINER, snapshot.capturedWorkState(0));
        assertArrayEquals(new int[] {1, 6}, snapshot.selectWorkOrdinals(M3EclipseResourceDeltaSnapshot.JAVA_SOURCE, 0, JAVA));
        assertArrayEquals(new int[] {2}, snapshot.selectWorkOrdinals(M3EclipseResourceDeltaSnapshot.CLASS_FILE, 0, JAVA));
        assertArrayEquals(new int[] {3, 4, 5, 6, 7, 8, 9, 10, 11, 12},
                snapshot.selectWorkOrdinals(M3EclipseResourceDeltaSnapshot.BUILD_DESCRIPTOR, 0, JAVA));
        assertArrayEquals(new int[0], snapshot.selectWorkOrdinals(M3EclipseResourceDeltaSnapshot.CONTENT_DIRTY, 0, JAVA));
    }

    @Test void descriptorChangeNominatesParentAndSiblingsRegardlessOfChildOrder() {
        Node source = new Node("A.java", IResource.FILE, IResourceDelta.CHANGED, IResourceDelta.MARKERS);
        Node nested = new Node("src", IResource.FOLDER, IResourceDelta.CHANGED, 0, source);
        Node descriptor = new Node("pom.xml", IResource.FILE, IResourceDelta.REMOVED, 0);
        Node unrelated = new Node("B.java", IResource.FILE, IResourceDelta.CHANGED, IResourceDelta.MARKERS);
        Node project = new Node("project", IResource.PROJECT, IResourceDelta.CHANGED, 0, nested, descriptor);
        Node other = new Node("other", IResource.PROJECT, IResourceDelta.CHANGED, 0, unrelated);
        Node root = new Node("", IResource.ROOT, IResourceDelta.CHANGED, 0, project, other);
        M3EclipseResourceDeltaSnapshot snapshot = M3EclipseResourceDeltaSnapshot.captureChanges(root.delta, 7);
        int context = M3EclipseResourceDeltaSnapshot.CONTEXT_DIRTY;
        int descendants = M3EclipseResourceDeltaSnapshot.DESCENDANTS_DIRTY;
        assertEquals(0, snapshot.capturedWorkState(0) & context);
        assertEquals(context | descendants, snapshot.capturedWorkState(1) & (context | descendants));
        assertEquals(0, snapshot.capturedWorkState(2) & context);
        assertEquals(context | descendants, snapshot.capturedWorkState(3) & (context | descendants));
        assertEquals(context, snapshot.capturedWorkState(6) & context);
        assertEquals(0, snapshot.capturedWorkState(5) & context);
        assertArrayEquals(new int[] {4}, snapshot.selectWorkOrdinals(M3EclipseResourceDeltaSnapshot.REMOVED, 0, JAVA));
    }

    @Test void containerEncodingAndFormerFolderTypeNominateDescendants() {
        Node child = new Node("A.java", IResource.FILE, IResourceDelta.CHANGED, IResourceDelta.SYNC);
        Node folder = new Node("src", IResource.FOLDER, IResourceDelta.CHANGED, IResourceDelta.ENCODING, child);
        M3EclipseResourceDeltaSnapshot encoded = M3EclipseResourceDeltaSnapshot.captureChanges(folder.delta, 2);
        assertEquals(M3EclipseResourceDeltaSnapshot.CONTEXT_DIRTY,
                encoded.capturedWorkState(1) & M3EclipseResourceDeltaSnapshot.CONTEXT_DIRTY);
        assertEquals(0, encoded.capturedWorkState(1) & M3EclipseResourceDeltaSnapshot.CONTENT_DIRTY);
        Node formerlyFolder = new Node("replacement.txt", IResource.FILE, IResourceDelta.CHANGED, IResourceDelta.TYPE);
        M3EclipseResourceDeltaSnapshot typed = M3EclipseResourceDeltaSnapshot.captureChanges(formerlyFolder.delta, 1);
        assertEquals(M3EclipseResourceDeltaSnapshot.DESCENDANTS_DIRTY,
                typed.capturedWorkState(0) & M3EclipseResourceDeltaSnapshot.DESCENDANTS_DIRTY);
        assertEquals(0, typed.capturedWorkState(0) & M3EclipseResourceDeltaSnapshot.CONTAINER);
    }

    @Test void structuralAndContextFlagsAreConservativeButMarkersAloneAreClean() {
        int[] content = {IResourceDelta.CONTENT, IResourceDelta.ENCODING, IResourceDelta.REPLACED,
                IResourceDelta.TYPE, IResourceDelta.MOVED_FROM, IResourceDelta.MOVED_TO,
                IResourceDelta.COPIED_FROM, IResourceDelta.LOCAL_CHANGED};
        for (int flags : content) {
            int kind = flags == IResourceDelta.MOVED_FROM || flags == IResourceDelta.COPIED_FROM
                    ? IResourceDelta.ADDED : flags == IResourceDelta.MOVED_TO
                    ? IResourceDelta.REMOVED : IResourceDelta.CHANGED;
            Node node = new Node("A.java", IResource.FILE, kind, flags);
            int state = M3EclipseResourceDeltaSnapshot.captureChanges(node.delta, 1).capturedWorkState(0);
            assertEquals(M3EclipseResourceDeltaSnapshot.CONTENT_DIRTY, state & M3EclipseResourceDeltaSnapshot.CONTENT_DIRTY);
        }
        for (int kind : new int[] {IResourceDelta.ADDED, IResourceDelta.REMOVED,
                IResourceDelta.ADDED_PHANTOM, IResourceDelta.REMOVED_PHANTOM}) {
            Node node = new Node("A.java", IResource.FILE, kind, 0);
            int state = M3EclipseResourceDeltaSnapshot.captureChanges(node.delta, 1).capturedWorkState(0);
            assertEquals(M3EclipseResourceDeltaSnapshot.CONTENT_DIRTY, state & M3EclipseResourceDeltaSnapshot.CONTENT_DIRTY);
        }
        for (int flags : new int[] {IResourceDelta.DESCRIPTION, IResourceDelta.OPEN, IResourceDelta.DERIVED_CHANGED}) {
            Node node = new Node("project", IResource.PROJECT, IResourceDelta.CHANGED, flags);
            int state = M3EclipseResourceDeltaSnapshot.captureChanges(node.delta, 1).capturedWorkState(0);
            assertEquals(M3EclipseResourceDeltaSnapshot.CONTEXT_DIRTY, state & M3EclipseResourceDeltaSnapshot.CONTEXT_DIRTY);
        }
        Node marker = new Node("A.java", IResource.FILE, IResourceDelta.CHANGED, IResourceDelta.MARKERS | IResourceDelta.SYNC);
        assertEquals(M3EclipseResourceDeltaSnapshot.JAVA_SOURCE,
                M3EclipseResourceDeltaSnapshot.captureChanges(marker.delta, 1).capturedWorkState(0));
    }

    @Test void unchangedTraversalRefusesBudgetCyclesAndNullsBeforeMetadata() {
        Node child = new Node("A.java", IResource.FILE, IResourceDelta.CHANGED, 0);
        Node root = new Node("project", IResource.PROJECT, IResourceDelta.CHANGED, 0, child);
        assertThrows(IllegalStateException.class, () -> M3EclipseResourceDeltaSnapshot.captureChanges(root.delta, 1));
        assertEquals(0, root.flagReads);
        assertEquals(0, child.flagReads);
        root.children = new Node[] {root};
        assertThrows(IllegalStateException.class, () -> M3EclipseResourceDeltaSnapshot.captureChanges(root.delta, 3));
        assertEquals(0, root.flagReads);
        root.children = null;
        assertThrows(IllegalStateException.class, () -> M3EclipseResourceDeltaSnapshot.captureChanges(root.delta, 3));
        assertThrows(NullPointerException.class, () -> M3EclipseResourceDeltaSnapshot.captureChanges(null, 1));
        assertThrows(IllegalArgumentException.class, () -> M3EclipseResourceDeltaSnapshot.captureChanges(child.delta, 0));
    }

    @Test void metadataFailuresPropagateWithoutReturningPartialSnapshots() {
        Node root = new Node("A.java", IResource.FILE, IResourceDelta.CHANGED, 0);
        RuntimeException expected = new IllegalStateException("donor callback failure");
        root.flagFailure = expected;
        assertSame(expected, assertThrows(IllegalStateException.class,
                () -> M3EclipseResourceDeltaSnapshot.captureChanges(root.delta, 1)));
        assertEquals(0, root.resourceReads);
        root.flagFailure = null;
        root.nullResource = true;
        assertThrows(NullPointerException.class, () -> M3EclipseResourceDeltaSnapshot.captureChanges(root.delta, 1));
    }

    @Test void everyByteMaskPairMatchesAnIndependentOrdinalOracle() {
        Node[] nodes = {
            new Node("A.java", IResource.FILE, IResourceDelta.ADDED, 0),
            new Node("B.class", IResource.FILE, IResourceDelta.REMOVED, IResourceDelta.MOVED_TO),
            new Node("pom.xml", IResource.FILE, IResourceDelta.CHANGED, IResourceDelta.CONTENT),
            new Node("C.java", IResource.FILE, IResourceDelta.CHANGED, IResourceDelta.MARKERS),
            new Node("D.class", IResource.FILE, IResourceDelta.CHANGED, IResourceDelta.ENCODING),
            new Node("notes.txt", IResource.FILE, IResourceDelta.CHANGED, 0),
            new Node("removed", IResource.FOLDER, IResourceDelta.REMOVED, 0),
            new Node("former-folder", IResource.FILE, IResourceDelta.CHANGED, IResourceDelta.TYPE)
        };
        Node root = new Node("project", IResource.PROJECT, IResourceDelta.CHANGED, 0, nodes);
        M3EclipseResourceDeltaSnapshot snapshot = M3EclipseResourceDeltaSnapshot.captureChanges(root.delta, 9);
        boolean nativeAvailable = M3IndexMaskCompactor.nativeAvailable();
        if (Boolean.getBoolean("m3.resource.requireNative")) assertTrue(nativeAvailable, "required JNI unavailable");
        int[] state = new int[snapshot.size()];
        for (int i = 0; i < state.length; i++) state[i] = snapshot.capturedWorkState(i);
        for (int required = 0; required < 256; required++) {
            for (int excluded = 0; excluded < 256; excluded++) {
                int[] expected = new int[state.length];
                int count = 0;
                for (int ordinal = 0; ordinal < state.length; ordinal++) {
                    if ((state[ordinal] & required) == required && (state[ordinal] & excluded) == 0) {
                        expected[count++] = ordinal;
                    }
                }
                expected = Arrays.copyOf(expected, count);
                assertArrayEquals(expected, snapshot.selectWorkOrdinals(required, excluded, JAVA));
                if (nativeAvailable) assertArrayEquals(expected, snapshot.selectWorkOrdinals(required, excluded, NATIVE));
            }
        }
        if (!nativeAvailable) assertThrows(UnsatisfiedLinkError.class, () -> snapshot.selectWorkOrdinals(0, 0, NATIVE));
        int[] detached = snapshot.selectWorkOrdinals(0, 0, JAVA);
        detached[0] = 999;
        assertEquals(0, snapshot.selectWorkOrdinals(0, 0, JAVA)[0]);
        assertThrows(IllegalArgumentException.class, () -> snapshot.selectWorkOrdinals(256, 0, JAVA));
        assertThrows(IllegalArgumentException.class, () -> snapshot.selectWorkOrdinals(0, -1, JAVA));
        assertThrows(NullPointerException.class, () -> snapshot.selectWorkOrdinals(0, 0, null));
    }

    private static final class Node {
        String name;
        final int type;
        int kind;
        int flags;
        Node[] children;
        boolean allowMetadata = true;
        boolean nullResource;
        RuntimeException flagFailure;
        int kindReads, childrenReads, flagReads, resourceReads, typeReads, nameReads;
        final IResource resource;
        final IResourceDelta delta;

        Node(String name, int type, int kind, int flags, Node... children) {
            this.name = name;
            this.type = type;
            this.kind = kind;
            this.flags = flags;
            this.children = children;
            resource = (IResource) Proxy.newProxyInstance(IResource.class.getClassLoader(),
                    new Class<?>[] {IResource.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "getType" -> { typeReads++; yield this.type; }
                        case "getName" -> { nameReads++; yield this.name; }
                        case "toString" -> this.name;
                        default -> throw new AssertionError("unexpected resource callback: " + method.getName());
                    });
            delta = (IResourceDelta) Proxy.newProxyInstance(IResourceDelta.class.getClassLoader(),
                    new Class<?>[] {IResourceDelta.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "getKind" -> { kindReads++; yield this.kind; }
                        case "getAffectedChildren" -> {
                            childrenReads++;
                            yield this.children == null ? null : Arrays.stream(this.children)
                                    .map(node -> node.delta).toArray(IResourceDelta[]::new);
                        }
                        case "getFlags" -> {
                            if (!allowMetadata) throw new AssertionError("legacy flags callback");
                            flagReads++;
                            if (flagFailure != null) throw flagFailure;
                            yield this.flags;
                        }
                        case "getResource" -> {
                            if (!allowMetadata) throw new AssertionError("legacy resource callback");
                            resourceReads++;
                            yield nullResource ? null : resource;
                        }
                        case "toString" -> this.name;
                        default -> throw new AssertionError("unexpected delta callback: " + method.getName());
                    });
        }
    }
}
