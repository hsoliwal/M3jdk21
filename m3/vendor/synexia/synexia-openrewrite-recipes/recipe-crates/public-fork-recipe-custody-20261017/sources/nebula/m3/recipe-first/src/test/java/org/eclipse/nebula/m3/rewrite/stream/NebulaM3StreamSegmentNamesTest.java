/* SPDX-License-Identifier: EPL-2.0 */
package org.eclipse.nebula.m3.rewrite.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.openrewrite.Cursor;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

final class NebulaM3StreamSegmentNamesTest {
    private static final String HINT = "findFirstEnabledItemContainingEvent";
    private static final String RESOURCE =
            "/org/eclipse/nebula/m3/rewrite/exact/rounded-toolbar-stream/RoundedToolbar.";
    private static final String TOOLBAR_DIRECTORY =
            "widgets/opal/roundedtoolbar/org.eclipse.nebula.widgets.opal.roundedtoolbar/src/"
            + "org/eclipse/nebula/widgets/opal/roundedtoolbar/";
    private static final String PREDICATE =
            "element -> element.getBounds().contains(event.x, event.y) && element.isEnabled()";

    @Test
    void actualBeforeSourceHasThreeIdenticalHintsAndOnePointUnknown() throws Exception {
        String source = resource("before");
        J.CompilationUnit unit = parse(source, false);
        List<J.MethodInvocation> terminals = terminals(unit);
        assertEquals(4, terminals.size());
        StringBuilder evidence = new StringBuilder(
                "physical_line\tsource_sha256\tbody_sha256\tstatus\thuman_hint\tordered_callees"
                + "\tpredicate_order\titem_parameter\tevent_capture"
                + "\treplacement_authority\tequivalence_authority\tbody\n");
        int sourceOffset = 0;
        for (int index = 0; index < terminals.size(); index++) {
            J.MethodInvocation terminal = terminals.get(index);
            var candidate = NebulaM3StreamSegmentNames.inspect(terminal);
            assertEquals(index < 3 ? NebulaM3StreamSegmentNames.Status.MATCH
                    : NebulaM3StreamSegmentNames.Status.UNKNOWN, candidate.status());
            assertEquals(index < 3 ? Optional.of(HINT) : Optional.empty(), candidate.suggestedName());
            assertEquals(List.of("stream", "filter", "findFirst"), candidate.segments().stream()
                    .map(segment -> segment.invocation().getSimpleName()).toList());
            assertSame(terminal, candidate.segments().getLast().invocation());
            for (var segment : candidate.segments()) {
                assertSame(segment.invocation().getMethodType(), segment.callee());
                assertSame(segment.invocation().getSelect(), segment.receiver());
                assertEquals(segment.invocation().getArguments(), segment.arguments());
                assertNotNull(segment.callee());
            }
            assertFalse(candidate.replacementAuthority());
            assertFalse(candidate.equivalenceAuthority());
            if (index < 3) {
                JavaType.Variable item = candidate.itemParameter().orElseThrow();
                J.Lambda lambda = (J.Lambda) candidate.segments().get(1).arguments().getFirst();
                J.VariableDeclarations parameter = (J.VariableDeclarations) lambda.getParameters()
                        .getParameters().getFirst();
                assertSame(item, parameter.getVariables().getFirst().getVariableType());
                J.Binary predicate = (J.Binary) lambda.getBody();
                J.MethodInvocation contains = (J.MethodInvocation) predicate.getLeft();
                J.MethodInvocation bounds = (J.MethodInvocation) contains.getSelect();
                J.MethodInvocation enabled = (J.MethodInvocation) predicate.getRight();
                assertSame(item, ((J.Identifier) bounds.getSelect()).getFieldType());
                assertSame(item, ((J.Identifier) enabled.getSelect()).getFieldType());
                J.Identifier eventX = (J.Identifier) ((J.FieldAccess) contains.getArguments().getFirst()).getTarget();
                J.Identifier eventY = (J.Identifier) ((J.FieldAccess) contains.getArguments().getLast()).getTarget();
                assertSame(eventX, candidate.eventCapture().orElseThrow());
                assertSame(eventX.getFieldType(), eventY.getFieldType());
                assertEquals("event", candidate.eventCapture().orElseThrow().getSimpleName());
                assertEquals(Optional.of(HINT), NebulaM3StreamSegmentNames.selectName(candidate,
                        unit.getClasses().getFirst()));
            }
            String body = terminal.print(new Cursor(new Cursor(null, unit), terminal)).stripLeading();
            int start = source.indexOf("items.stream()", sourceOffset);
            assertTrue(start >= 0);
            assertEquals(body, source.substring(start, start + body.length()));
            sourceOffset = start + body.length();
            long line = 1 + source.substring(0, start).chars().filter(ch -> ch == '\n').count();
            String callees = String.join(" -> ", candidate.segments().stream()
                    .map(segment -> segment.callee().toString()).toList());
            evidence.append(line).append('\t').append(sha256(source)).append('\t').append(sha256(body))
                    .append('\t').append(candidate.status()).append('\t')
                    .append(candidate.suggestedName().orElse(""))
                    .append('\t').append(escape(callees)).append('\t')
                    .append(index < 3 ? "bounds.contains(event.x,event.y) && sameItem.isEnabled()"
                            : "bounds.contains(Point); UNKNOWN")
                    .append('\t').append(escape(candidate.itemParameter().map(Object::toString).orElse("")))
                    .append('\t').append(escape(candidate.eventCapture()
                            .map(event -> event.getFieldType().toString()).orElse("")))
                    .append("\tfalse\tfalse\t").append(escape(body)).append('\n');
        }
        assertEquals(source, unit.printAll());
        Path output = Path.of("target/segment-name-evidence.tsv");
        Files.createDirectories(output.getParent());
        Files.writeString(output, evidence.toString(), StandardCharsets.UTF_8);
    }

    @Test
    void changedStagesPredicateOrderAndBindingsRemainUnknown() throws Exception {
        Map<String, String> chains = new TreeMap<>();
        chains.put("canonical", "items.stream().filter(" + PREDICATE + ").findFirst()");
        chains.put("parallel", "items.parallelStream().filter(" + PREDICATE + ").findFirst()");
        chains.put("extraStage", "items.stream().filter(" + PREDICATE + ").limit(1).findFirst()");
        chains.put("mapping", "items.stream().map(element -> element).filter(" + PREDICATE + ").findFirst()");
        chains.put("reversedPredicate", "items.stream().filter(element -> element.isEnabled()"
                + " && element.getBounds().contains(event.x, event.y)).findFirst()");
        chains.put("bitwise", "items.stream().filter(" + PREDICATE.replace("&&", "&") + ").findFirst()");
        chains.put("differentItem", "items.stream().filter(" + PREDICATE.replace("element.isEnabled()",
                "other.isEnabled()") + ").findFirst()");
        chains.put("differentEvent", "items.stream().filter(" + PREDICATE.replace("event.y", "second.y")
                + ").findFirst()");
        chains.put("swappedCoordinates", "items.stream().filter(" + PREDICATE.replace("event.x, event.y",
                "event.y, event.x") + ").findFirst()");
        chains.put("capturedField", "items.stream().filter(" + PREDICATE.replace("event.x, event.y",
                "captured.x, captured.y") + ").findFirst()");
        chains.put("extraCapture", "items.stream().filter(" + PREDICATE + " && flag).findFirst()");
        chains.put("blockLambda", "items.stream().filter(element -> { return "
                + PREDICATE.substring(PREDICATE.indexOf(" -> ") + 4) + "; }).findFirst()");
        chains.put("typedLambda", "items.stream().filter((RoundedToolItem element) -> "
                + PREDICATE.substring(PREDICATE.indexOf(" -> ") + 4) + ").findFirst()");
        chains.put("parenthesizedLambda", "items.stream().filter(" + PREDICATE.replace("element ->",
                "(element) ->") + ").findFirst()");
        chains.put("memberReference", "items.stream().filter(RoundedToolItem::isEnabled).findFirst()");
        chains.put("rawCollection", "raw.stream().filter(element -> ((RoundedToolItem) element)"
                + ".getBounds().contains(event.x, event.y) && ((RoundedToolItem) element).isEnabled()).findFirst()");
        StringBuilder fixture = new StringBuilder("""
                import java.util.List;
                import java.util.Optional;
                import org.eclipse.swt.widgets.Event;
                import org.eclipse.nebula.widgets.opal.roundedtoolbar.RoundedToolItem;
                class Probe {
                    List<RoundedToolItem> items;
                    @SuppressWarnings("rawtypes") List raw;
                    Event captured;
                """);
        for (var chain : chains.entrySet()) {
            fixture.append("Object ").append(chain.getKey())
                    .append("(Event event, Event second, RoundedToolItem other, boolean flag) { return ")
                    .append(chain.getValue()).append("; }\n");
        }
        fixture.append("Object changedTerminal(Event event) { return items.stream().filter(")
                .append(PREDICATE).append(").findAny(); }\n}");
        J.CompilationUnit unit = parse(fixture.toString(), true);
        Map<String, NebulaM3StreamSegmentNames.Candidate> candidates = candidatesByMethod(unit);
        assertEquals(chains.size() + 1, candidates.size());
        for (var entry : candidates.entrySet()) {
            assertEquals(entry.getKey().equals("canonical") ? NebulaM3StreamSegmentNames.Status.MATCH
                    : NebulaM3StreamSegmentNames.Status.UNKNOWN, entry.getValue().status(), entry.getKey());
        }
        var canonical = candidates.get("canonical");
        J.MethodInvocation terminal = canonical.segments().getLast().invocation();
        assertEquals(NebulaM3StreamSegmentNames.Status.UNKNOWN,
                NebulaM3StreamSegmentNames.inspect(terminal.withMethodType(null)).status());
        assertThrows(UnsupportedOperationException.class, () -> canonical.segments().clear());
        assertEquals(fixture.toString(), unit.printAll());
    }

    @Test
    void sameCallNamesWithDifferentAttributedOwnersRemainUnknown() throws Exception {
        J.CompilationUnit unit = parse("""
                import java.util.function.Predicate;
                import org.eclipse.swt.widgets.Event;
                import org.eclipse.nebula.widgets.opal.roundedtoolbar.RoundedToolItem;
                class Spoof {
                    Spoof stream() { return this; }
                    Spoof filter(Predicate<RoundedToolItem> predicate) { return this; }
                    Object findFirst() { return null; }
                    Object call(Event event) {
                        return this.stream().filter(element -> element.getBounds().contains(event.x, event.y)
                                && element.isEnabled()).findFirst();
                    }
                }
                """, true);
        var candidate = NebulaM3StreamSegmentNames.inspect(terminals(unit).getFirst());
        assertEquals(3, candidate.segments().size());
        assertEquals("Spoof", candidate.segments().getFirst().callee().getDeclaringType().getFullyQualifiedName());
        assertEquals(NebulaM3StreamSegmentNames.Status.UNKNOWN, candidate.status());
        assertTrue(candidate.suggestedName().isEmpty());
    }

    @Test
    void namingSelectionDeclinesActualHelperAndDeclaredOrInheritedCollisions() throws Exception {
        J.CompilationUnit before = parse(resource("before"), false);
        var candidate = NebulaM3StreamSegmentNames.inspect(terminals(before).getFirst());
        J.CompilationUnit after = parse(resource("after"), false);
        assertEquals(Optional.of(HINT), NebulaM3StreamSegmentNames.selectName(candidate,
                before.getClasses().getFirst()));
        assertTrue(NebulaM3StreamSegmentNames.selectName(candidate, after.getClasses().getFirst()).isEmpty());
        J.CompilationUnit collisions = parse("""
                class Parent { protected void findFirstEnabledItemContainingEvent() {} }
                class Inherited extends Parent {}
                class FieldCollision { int findFirstEnabledItemContainingEvent; }
                class MethodCollision { private void findFirstEnabledItemContainingEvent(int value) {} }
                """, false);
        for (J.ClassDeclaration scope : collisions.getClasses()) {
            assertTrue(NebulaM3StreamSegmentNames.selectName(candidate, scope).isEmpty(), scope.getSimpleName());
        }
        assertTrue(NebulaM3StreamSegmentNames.selectName(candidate,
                before.getClasses().getFirst().withType((JavaType.FullyQualified) null)).isEmpty());
    }

    private static J.CompilationUnit parse(String source, boolean includeToolbar) throws IOException {
        Path root = Path.of(System.getProperty("m3.stream.root", "../..")).toAbsolutePath().normalize();
        List<String> dependencies = new ArrayList<>();
        dependencies.add(Files.readString(root.resolve(TOOLBAR_DIRECTORY + "RoundedToolItem.java")));
        dependencies.add(Files.readString(root.resolve(TOOLBAR_DIRECTORY + "GradientColor.java")));
        dependencies.add(Files.readString(root.resolve("widgets/opal/commons/org.eclipse.nebula.widgets.opal.commons/"
                + "src/org/eclipse/nebula/widgets/opal/commons/AdvancedPath.java")));
        if (includeToolbar) {
            dependencies.add(resource("before"));
        }
        List<Path> classpath = realSdkClasspath();
        assertFalse(classpath.isEmpty(), "Real SWT and JFace classpath is required");
        var errors = new ArrayList<Throwable>();
        var parser = JavaParser.fromJavaVersion().classpath(classpath)
                .dependsOn(dependencies.toArray(String[]::new)).build();
        var parsed = parser.parse(new InMemoryExecutionContext(errors::add), source).findFirst().orElseThrow();
        assertTrue(parsed instanceof J.CompilationUnit, parsed.getClass().getName());
        assertTrue(errors.isEmpty(), errors.toString());
        return (J.CompilationUnit) parsed;
    }

    private static List<Path> realSdkClasspath() {
        String configured = System.getProperty("m3.stream.classpath", "").strip();
        if (!configured.isEmpty()) {
            return List.of(configured.split(Pattern.quote(File.pathSeparator))).stream()
                    .map(Path::of)
                    .toList();
        }
        return List.of(
                        "org.eclipse.swt.widgets.Event",
                        "org.eclipse.jface.viewers.Viewer")
                .stream()
                .map(NebulaM3StreamSegmentNamesTest::codeSource)
                .distinct()
                .toList();
    }

    private static Path codeSource(String className) {
        try {
            Class<?> type = Class.forName(className, false,
                    NebulaM3StreamSegmentNamesTest.class.getClassLoader());
            return Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (ClassNotFoundException | URISyntaxException failure) {
            throw new IllegalStateException(
                    "required reviewed SWT/JFace test artifact unavailable: " + className,
                    failure);
        }
    }

    private static List<J.MethodInvocation> terminals(J.CompilationUnit unit) {
        List<J.MethodInvocation> result = new ArrayList<>();
        new JavaIsoVisitor<List<J.MethodInvocation>>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation invocation,
                    List<J.MethodInvocation> found) {
                if (invocation.getSimpleName().equals("findFirst")) {
                    found.add(invocation);
                }
                return super.visitMethodInvocation(invocation, found);
            }
        }.visit(unit, result);
        return result;
    }

    private static Map<String, NebulaM3StreamSegmentNames.Candidate> candidatesByMethod(J.CompilationUnit unit) {
        Map<String, NebulaM3StreamSegmentNames.Candidate> result = new TreeMap<>();
        new JavaIsoVisitor<Map<String, NebulaM3StreamSegmentNames.Candidate>>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation invocation,
                    Map<String, NebulaM3StreamSegmentNames.Candidate> found) {
                if (invocation.getSimpleName().equals("findFirst") || invocation.getSimpleName().equals("findAny")) {
                    J.MethodDeclaration owner = getCursor().firstEnclosing(J.MethodDeclaration.class);
                    assertNotNull(owner);
                    found.put(owner.getSimpleName(), NebulaM3StreamSegmentNames.inspect(invocation));
                }
                return super.visitMethodInvocation(invocation, found);
            }
        }.visit(unit, result);
        return result;
    }

    private static String resource(String lane) throws IOException {
        try (var input = NebulaM3StreamSegmentNamesTest.class.getResourceAsStream(RESOURCE + lane + ".java.txt")) {
            assertNotNull(input);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String sha256(String source) throws NoSuchAlgorithmException {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(source.getBytes(StandardCharsets.UTF_8)));
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\t", "\\t").replace("\r", "\\r").replace("\n", "\\n");
    }
}
