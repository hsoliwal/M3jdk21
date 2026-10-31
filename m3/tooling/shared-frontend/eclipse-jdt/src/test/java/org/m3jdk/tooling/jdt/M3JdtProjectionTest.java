/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package org.m3jdk.tooling.jdt;

import com.m3.compiletime.M3ASTLeafIndex;
import com.synexia.cognix.eclipse.selective.api.AstProjection;
import com.synexia.cognix.eclipse.selective.api.ParseProfile;
import com.synexia.cognix.eclipse.selective.api.ParseRequest;
import com.synexia.cognix.eclipse.selective.api.ParseScope;
import com.synexia.cognix.eclipse.selective.api.Range;
import com.synexia.cognix.eclipse.selective.bridge.JdtContractTest;
import com.synexia.cognix.eclipse.selective.bridge.JdtRewriteNormalizationTest;
import com.synexia.cognix.eclipse.selective.bridge.JdtSelectiveParser;
import com.synexia.cognix.eclipse.selective.core.CoreContractTest;
import com.synexia.cognix.eclipse.selective.core.JdkGateContractTest;
import com.synexia.cognix.eclipse.selective.core.Utf8Snapshot;
import com.synexia.job.IProgressMonitor;
import java.util.Arrays;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Real JDT source owners feed the existing primitive M3AST evidence index. */
public class M3JdtProjectionTest {
    @Test void originalJdtContracts() { JdtContractTest.main(new String[0]); }
    @Test void originalRewriteContracts() throws Exception {
        JdtRewriteNormalizationTest.main(new String[0]);
    }
    @Test void originalSourceAndCacheContracts() throws Exception {
        CoreContractTest.main(new String[0]);
    }
    @Test void originalJdkGateContracts() throws Exception {
        JdkGateContractTest.main(new String[0]);
    }

    @Test void allSixScopesFeedDetachedPrimitiveFacts() {
        String source = "/*😀*/ class Example { int a(){ return 1 + 2; } int b(){ return 3; } }";
        for (ParseScope scope : ParseScope.values()) {
            String part = switch (scope) {
                case EXPRESSION -> "1 + 2";
                case STATEMENTS -> "return 1 + 2;";
                case CLASS_BODY -> "int a(){ return 1 + 2; }";
                case FOCAL -> "1 + 2";
                default -> source;
            };
            Utf8Snapshot snapshot = Utf8Snapshot.fromText("Example.java", source, true,
                    10000, IProgressMonitor.noop());
            ParseRequest request = new ParseRequest(snapshot.unitName(), source,
                    snapshot.sha256(), true, scope, new Range(source.indexOf(part), part.length()),
                    ParseProfile.java21());
            AstProjection projection = new JdtSelectiveParser().analyze(
                    request, snapshot, IProgressMonitor.noop());
            assertFalse(projection.unsafeSyntax(), scope.toString());
            assertTrue(projection.backend().startsWith("eclipse-jdt:"));
            int count = projection.nodes().size();
            assertTrue(count > 0);
            int[] handles = IntStream.range(0, count).toArray();
            long[] states = new long[count];
            Arrays.fill(states, M3ASTLeafIndex.LIVE);
            // A test-owned high state bit selects names; it is not a new API flag.
            long nameBit = Long.MIN_VALUE;
            for (int i = 0; i < count; i++) {
                if (projection.nodes().get(i).kind().equals("SimpleName")) states[i] |= nameBit;
            }
            int[] expected = IntStream.range(0, count).filter(i -> (states[i] & nameBit) != 0).toArray();
            M3ASTLeafIndex.FileFacts facts = new M3ASTLeafIndex.FileFacts("project", "module",
                    "package", "Example.java", snapshot.sha256(), handles, states);
            M3ASTLeafIndex index = M3ASTLeafIndex.build("repository", facts);
            Arrays.fill(handles, -1);
            Arrays.fill(states, 0);
            assertEquals(count, index.atomCount(index.repositoryRow()));
            assertArrayEquals(expected, index.selectOffsets(index.repositoryRow(), nameBit, 0));
            for (int offset : expected) assertEquals(offset, index.atomIdAtPhysical(offset));
            for (int level = M3ASTLeafIndex.FACTS; level <= M3ASTLeafIndex.REPOSITORY; level++) {
                assertEquals(1, index.levelCount(level));
                assertEquals(count, index.atomCount(index.rowAt(level, 0)));
            }
            // Returned lane copies cannot mutate retained evidence either.
            int[] detached = facts.atomIds();
            detached[0] = -1;
            assertEquals(0, index.atomIdAtPhysical(0));
        }
    }

    @Test void existingSnapshotAdmissionRejectsStaleSource() {
        Utf8Snapshot snapshot = Utf8Snapshot.fromText("Example.java", "class Example {}", false,
                10000, IProgressMonitor.noop());
        ParseRequest stale = new ParseRequest("Example.java", "class Changed {}", snapshot.sha256(),
                false, ParseScope.COMPILATION_UNIT, new Range(0, 0), ParseProfile.java21());
        assertThrows(IllegalArgumentException.class, () -> new JdtSelectiveParser().analyze(
                stale, snapshot, IProgressMonitor.noop()));
    }
}
