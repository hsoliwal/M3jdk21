/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.net.URI;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Trees;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.config.YamlResourceLoader;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

/** Actual source-only SDK scheduling plus preservation of the complete current owner. */
final class M3RegexRelationsCurrentOwnerRecipeTest {
    private static final String ROOT =
            "/com/synexia/rewrite/m3-regex-relations-current-owner-20261017/";
    private static final String RECEIVING = "src/java.base/share/classes/java/lang/M3StringRegexDp.java";
    private static final String HIERARCHICAL = "test/jdk/java/lang/String/M3StringHierarchicalRegexTest.java";
    private record Applied(Map<String, String> files, int changes) { }

    private static String contents(String name) throws IOException {
        try (var stream = M3RegexRelationsCurrentOwnerRecipeTest.class.getResourceAsStream(ROOT + name)) {
            assertNotNull(stream, name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    private static M3GpuToolJoinManifest manifest() {
        return M3GpuToolJoinManifest.load(ROOT,
                M3RegexRelationsCurrentOwnerRecipeTest.class::getResourceAsStream,
                M3GpuToolJoinManifest.NOOP);
    }
    private static Map<String, String> before() throws IOException {
        var input = new LinkedHashMap<String, String>();
        for (var target : manifest().targets()) {
            input.put(target.path(), contents("before/" + Path.of(target.path()).getFileName()));
        }
        input.put("opaque/Unrelated.txt", "fake workflow: make images and exit 0 stay opaque\n");
        return input;
    }
    private static List<SourceFile> sources(Map<String, String> input) {
        var result = new ArrayList<SourceFile>();
        input.forEach((path, text) -> result.add(
                PlainText.builder().sourcePath(Path.of(path)).text(text).build()));
        return result;
    }
    private static Recipe namedRecipe() {
        String descriptor = "/META-INF/rewrite/m3-regex-relations-current-owner-20261017.yml";
        Recipe recipe;
        try (var stream = M3RegexRelationsCurrentOwnerRecipeTest.class.getResourceAsStream(descriptor)) {
            assertNotNull(stream, descriptor);
            var loader = new YamlResourceLoader(stream, URI.create("classpath:" + descriptor),
                    new Properties(), M3RegexRelationsCurrentOwnerRecipe.class.getClassLoader());
            recipe = Environment.builder().load(loader).build()
                    .activateRecipes("com.synexia.m3.RegexRelationsCurrentOwner");
        } catch (IOException failure) {
            throw new java.io.UncheckedIOException("cannot read the exact named descriptor", failure);
        }
        assertEquals(1, recipe.getRecipeList().size());
        assertEquals("com.synexia.m3.RegexRelationsCurrentOwner",
                recipe.getRecipeList().get(0).getName());
        assertEquals(List.of(M3RegexRelationsCurrentOwnerRecipe.class),
                recipe.getRecipeList().get(0).getRecipeList().stream()
                        .map(Object::getClass).toList());
        return recipe;
    }
    private static Applied apply(Map<String, String> input) {
        var errors = new ArrayList<Throwable>();
        var run = namedRecipe().run(new InMemoryLargeSourceSet(sources(input)),
                new InMemoryExecutionContext(errors::add));
        assertTrue(errors.isEmpty(), errors.toString());
        var output = new LinkedHashMap<>(input);
        var changes = run.getChangeset().getAllResults();
        for (var change : changes) {
            assertNotNull(change.getAfter());
            output.put(change.getAfter().getSourcePath().toString().replace('\\', '/'),
                    change.getAfter().printAll());
        }
        return new Applied(output, changes.size());
    }


    @Test void allPartialStatesAndOrdersReachExactFixedPoint() throws Exception {
        var targets = manifest().targets().stream().toList();
        assertEquals(5, targets.size());
        var changed = targets.stream().filter(target -> !target.before().equals(target.after())).toList();
        assertEquals(2, changed.size());
        assertEquals(List.of(RECEIVING, HIERARCHICAL),
                changed.stream().map(M3GpuToolJoinManifest.Target::path).toList());
        for (var owned : targets) {
            assertTrue(!owned.addition() && !owned.removal());
            assertEquals(owned.before(), M3GpuToolJoinManifest.sha256(
                    contents("before/" + Path.of(owned.path()).getFileName())));
        }
        // All four before/after combinations of both independently owned outputs are admitted.
        for (int state = 0; state < 4; state++) {
            var input = before();
            for (int bit = 0; bit < changed.size(); bit++) {
                if ((state & (1 << bit)) != 0) {
                    var target = changed.get(bit);
                    input.put(target.path(), target.text());
                }
            }
            for (boolean reverse : new boolean[]{false, true}) {
                var ordered = new LinkedHashMap<String, String>();
                var paths = new ArrayList<>(input.keySet());
                if (reverse) java.util.Collections.reverse(paths);
                for (String target : paths) ordered.put(target, input.get(target));
                var result = apply(ordered);
                assertEquals(2 - Integer.bitCount(state), result.changes());
                for (var owned : targets) {
                    assertEquals(owned.after(),
                            M3GpuToolJoinManifest.sha256(result.files().get(owned.path())));
                }
                assertEquals(input.get("opaque/Unrelated.txt"), result.files().get("opaque/Unrelated.txt"));
                assertEquals(0, apply(result.files()).changes());
                if (state == 0 && !reverse) {
                    for (var target : changed) retain(target.path(), result.files().get(target.path()));
                }
            }
        }
    }

    @Test void eachOwnedInputRefusesDriftMissingAndDuplicateOwnership() throws Exception {
        for (var target : manifest().targets()) {
            var input = before();
            input.put(target.path(), "foreign workflow source\n");
            reject(sources(input), "source drift: " + target.path());
            input = before();
            input.remove(target.path());
            reject(sources(input), "required source missing: " + target.path());
            var duplicate = sources(before());
            duplicate.add(PlainText.builder().sourcePath(Path.of(target.path()))
                    .text(before().get(target.path())).build());
            reject(duplicate, "duplicate source target: " + target.path());
        }
    }

    private static void reject(List<SourceFile> input, String signature) {
        var errors = new ArrayList<Throwable>();
        try {
            var run = namedRecipe().run(new InMemoryLargeSourceSet(input),
                    new InMemoryExecutionContext(errors::add));
            assertTrue(run.getChangeset().getAllResults().isEmpty());
        } catch (IllegalStateException refusal) {
            errors.add(refusal);
        }
        StringBuilder observed = new StringBuilder();
        for (Throwable error : errors) {
            for (Throwable cause = error; cause != null; cause = cause.getCause()) {
                observed.append(cause).append('\n');
            }
        }
        assertTrue(observed.toString().contains(signature), observed.toString());
    }


    private record Parsed(Map<String, String> members, ClassTree owner) { }

    private static Parsed parseOwner(String source) throws Exception {
        var compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "source qualification requires the admitted Java 21 compiler");
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        var unitSource = new SimpleJavaFileObject(
                URI.create("string:///java/lang/M3StringRegexDp.java"), JavaFileObject.Kind.SOURCE) {
            @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return source;
            }
        };
        try (var manager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            var task = (JavacTask) compiler.getTask(null, manager, diagnostics,
                    List.of("-proc:none", "--release", "21"), null, List.of(unitSource));
            var units = new ArrayList<CompilationUnitTree>();
            task.parse().forEach(units::add); // Syntax only: never analyze, generate, or execute the JDK owner.
            assertEquals(1, units.size());
            assertTrue(diagnostics.getDiagnostics().stream()
                    .noneMatch(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR),
                    diagnostics.getDiagnostics().toString());
            var unit = units.get(0);
            var declarations = unit.getTypeDecls().stream()
                    .filter(tree -> tree instanceof ClassTree).toList();
            assertEquals(1, declarations.size());
            var owner = (ClassTree) declarations.get(0);
            assertEquals("M3StringRegexDp", owner.getSimpleName().toString());
            var positions = Trees.instance(task).getSourcePositions();
            var members = new LinkedHashMap<String, String>();
            for (Tree member : owner.getMembers()) {
                String key;
                if (member instanceof VariableTree variable) {
                    key = "field:" + variable.getName();
                } else if (member instanceof MethodTree method) {
                    key = "method:" + method.getName() + "/" + method.getParameters().size();
                } else if (member instanceof ClassTree nested) {
                    key = "type:" + nested.getSimpleName();
                } else {
                    throw new AssertionError("unexpected direct owner member " + member.getKind());
                }
                long start = positions.getStartPosition(unit, member);
                long end = positions.getEndPosition(unit, member);
                assertTrue(start >= 0 && end > start && end <= source.length(), key);
                assertTrue(members.put(key,
                        source.substring(Math.toIntExact(start), Math.toIntExact(end))) == null, key);
            }
            return new Parsed(members, owner);
        }
    }

    @Test void currentOwnerMembersAndCustodyRemainExact() throws Exception {
        var input = before();
        var output = apply(input).files();
        String original = input.get(RECEIVING);
        String candidate = output.get(RECEIVING);
        assertEquals("ca679153b93b33214e48c3d7cce4414cf9622ff380ee9aea3334c9a680f37779",
                M3GpuToolJoinManifest.sha256(original));
        assertEquals("471ffbf34b6046b5bc811783397e56bd57f94fadd1883680e39192ea08ce486e",
                M3GpuToolJoinManifest.sha256(candidate));
        assertEquals(12_888, original.getBytes(StandardCharsets.UTF_8).length);
        assertEquals(16_175, candidate.getBytes(StandardCharsets.UTF_8).length);
        // Independently replay the four refined insertions on the current immutable BEFORE.
        // A historical postimage cannot supply the newer carry closure or cursor traversal.
        List<String> anchors = List.of(
                "import java.lang.ref.Reference;",
                "    private static volatile boolean nativeUnavailable;\n",
                "            if (plan.closure == null) return rolling(source, plan);\n",
                "    private static boolean isAsciiAlphanumeric(char value) {\n");
        boolean[] insertAfter = {false, true, false, false};
        String replay = original;
        var additions = admittedInsertions();
        assertEquals(anchors.size(), additions.size());
        for (int index = 0; index < anchors.size(); index++) {
            String anchor = anchors.get(index);
            int offset = replay.indexOf(anchor);
            assertTrue(offset >= 0 && offset == replay.lastIndexOf(anchor),
                    "each current-owner insertion anchor must occur exactly once");
            replay = replay.replace(anchor, insertAfter[index]
                    ? anchor + additions.get(index) : additions.get(index) + anchor);
        }
        assertEquals(candidate, replay,
                "only the four refined insertions may alter the current owner");
        for (String region : preservedCurrentRegions()) {
            assertTrue(original.contains(region), "pinned current region missing");
            assertTrue(candidate.contains(region), "current carry/cursor region was replaced");
            assertEquals(original.indexOf(region), original.lastIndexOf(region));
            assertEquals(candidate.indexOf(region), candidate.lastIndexOf(region));
        }
        var prior = parseOwner(original);
        var after = parseOwner(candidate);
        String dispatch = after.members().get("method:tryMatch/2");
        int eligible = dispatch.indexOf(
                "if (plan.closure == null && m >= 256 && plan.atoms.length < Long.SIZE");
        int initialize = dispatch.indexOf("relationAccess()");
        assertTrue(eligible >= 0 && initialize > eligible,
                "Access initialization must follow the existing eligibility gate");
        assertEquals(initialize, dispatch.lastIndexOf("relationAccess()"));
        assertTrue(dispatch.contains("M3DotStarRelations.evaluate(source, relationAccess(),"));
        assertTrue(!dispatch.substring(0, eligible).contains("relationAccess()"),
                "unsupported, short, flat and cached plans must not initialize Access");
        var expectedMembers = new java.util.LinkedHashSet<>(prior.members().keySet());
        expectedMembers.add("field:RELATION_ACCESS");
        expectedMembers.add("method:relationAccess/0");
        assertEquals(expectedMembers, after.members().keySet());
        for (var entry : prior.members().entrySet()) {
            if (!entry.getKey().equals("method:tryMatch/2")) {
                assertEquals(entry.getValue(), after.members().get(entry.getKey()), entry.getKey());
            }
        }
        var accessFields = after.owner().getMembers().stream()
                .filter(tree -> tree instanceof VariableTree)
                .map(tree -> (VariableTree) tree)
                .filter(field -> field.getName().contentEquals("RELATION_ACCESS")).toList();
        assertEquals(1, accessFields.size());
        assertTrue(accessFields.get(0).getInitializer() == null,
                "optional adapter allocation must not enter class initialization");
        String inverse = candidate;
        for (String addition : admittedInsertions()) {
            int offset = inverse.indexOf(addition);
            assertTrue(offset >= 0 && offset == inverse.lastIndexOf(addition),
                    "each admitted source delta must occur exactly once");
            inverse = inverse.substring(0, offset) + inverse.substring(offset + addition.length());
        }
        assertEquals(original, inverse,
                "the exact current owner must survive removal of only the five admitted insertions");
        int custody = 0;
        for (var target : manifest().targets()) {
            if (target.path().equals(RECEIVING) || target.path().equals(HIERARCHICAL)) continue;
            custody++;
            assertEquals(target.before(), target.after());
            assertEquals(input.get(target.path()), output.get(target.path()), target.path());
            assertEquals(contents("before/" + Path.of(target.path()).getFileName()),
                    target.text(), target.path());
        }
        assertEquals(3, custody);
        String originalJtreg = input.get(HIERARCHICAL);
        String jtreg = output.get(HIERARCHICAL);
        assertEquals("1653c23098529c2fc9f7e66e1bfb5c5c2c97adc67c422b0e67cfbd7b5fb121fd",
                M3GpuToolJoinManifest.sha256(originalJtreg));
        assertEquals("69c725cb7cfe13b0a02bd6419cba2566f9c6b6ac30823d336765cbcebabbe601",
                M3GpuToolJoinManifest.sha256(jtreg));
        assertEquals(4_610, originalJtreg.getBytes(StandardCharsets.UTF_8).length);
        assertEquals(16_540, jtreg.getBytes(StandardCharsets.UTF_8).length);
        List<String> jtregAnchors = List.of(
                "import java.lang.reflect.Method;\n",
                "    public static void main(String[] args) throws Exception {\n",
                "        // Exercise the exact receiver Access adapter on a real shared M3String DAG, not a fixture.\n",
                "        System.out.println(\"M3_HIERARCHICAL_REGEX_TARGET checks=\" + checks + \" PASS\");\n");
        boolean[] jtregAfter = {true, false, false, false};
        String replayJtreg = originalJtreg;
        var jtregAdditions = admittedJtregInsertions();
        assertEquals(jtregAnchors.size(), jtregAdditions.size());
        for (int index = 0; index < jtregAnchors.size(); index++) {
            String anchor = jtregAnchors.get(index);
            int offset = replayJtreg.indexOf(anchor);
            assertTrue(offset >= 0 && offset == replayJtreg.lastIndexOf(anchor));
            replayJtreg = replayJtreg.replace(anchor, jtregAfter[index]
                    ? anchor + jtregAdditions.get(index) : jtregAdditions.get(index) + anchor);
        }
        assertEquals(jtreg, replayJtreg, "the entire original jtreg must survive additive replay");
        String inverseJtreg = jtreg;
        for (String addition : jtregAdditions) {
            int offset = inverseJtreg.indexOf(addition);
            assertTrue(offset >= 0 && offset == inverseJtreg.lastIndexOf(addition));
            inverseJtreg = inverseJtreg.substring(0, offset)
                    + inverseJtreg.substring(offset + addition.length());
        }
        assertEquals(originalJtreg, inverseJtreg,
                "remove only the four jtreg additions to restore every old case and both runs");
        assertTrue(jtreg.contains("String warmRegex = \"a*\".repeat(32) + \"b\";"));
        assertTrue(jtreg.contains("arguments[0] == trackedSource[0]"));
        assertTrue(jtreg.contains("access.set(null, original);"));
        assertTrue(jtreg.contains("getDeclaredField(\"RELATION_ACCESS\")"));
        assertTrue(jtreg.contains("-XX:+UseM3StringStorage"));
        assertTrue(jtreg.contains("-XX:-CompactStrings"));
        // Parsing source and preserving a jtreg fixture do not execute String/JNI/runtime semantics.
    }

    private static List<String> preservedCurrentRegions() {
        return List.of(
                "            state = close(destinations, plan.star);",
                "    private static long close(long bits, long star) {\n        if (bits == 0L) return 0L;\n        // A carry closes each starred run from its first seed through its exit.\n        // At most 32 atom bits keep the sum below 2^33.\n        return bits | (star ^ (star + (bits & star)));\n    }",
                "        // Reuse canonical leaf-local UTF-16 coordinates; no copied text or new owner.\n        M3String.CodeUnitCursor units = new M3String.CodeUnitCursor(source);\n        for (int i = 0; i < source.length(); i++) {\n            char value = units.at(i);"
        );
    }

    private static List<String> admittedInsertions() {
        return List.of(
                "import jdk.internal.mindex.M3DotStarRelations;\n\n",
                "    // Stateless access geometry only; optional allocation stays outside class initialization.\n    private static volatile M3DotStarRelations.Access<M3String> RELATION_ACCESS;\n",
                "            // Existing ordered relation algebra can reuse an entire immutable owner/range\n            // independently of its incoming frontier. Refusal retains every current lane below.\n            if (plan.closure == null && m >= 256 && plan.atoms.length < Long.SIZE\n                    && source.owner() instanceof M3StringTuple) {\n                try {\n                    int decision;\n                    try {\n                        decision = M3DotStarRelations.evaluate(source, relationAccess(), plan.atoms,\n                                plan.atoms.length, 128,\n                                Math.min(MAX_DP_CELLS, (long) m * (plan.atoms.length + 1)),\n                                null, null).decision();\n                    } finally {\n                        Reference.reachabilityFence(source);\n                    }\n                    if (decision != UNKNOWN) return decision;\n                } catch (OutOfMemoryError unavailable) {\n                    // Relation summaries are optional; keep the existing bit/rolling/native path.\n                }\n            }\n",
                "    /** Lazily publish stateless coordinates; never retain a String, owner, range or native handle. */\n    private static M3DotStarRelations.Access<M3String> relationAccess() {\n        M3DotStarRelations.Access<M3String> current = RELATION_ACCESS;\n        if (current != null) return current;\n        synchronized (M3StringRegexDp.class) {\n            current = RELATION_ACCESS;\n            if (current == null) {\n                current = new M3DotStarRelations.Access<>() {\n                    @Override public Object owner(M3String value) { return value.owner(); }\n                    @Override public long coordinate(M3String value) { return value.coordinate(); }\n                    @Override public int length(M3String value) { return value.length(); }\n                    @Override public int codeUnitAt(M3String value, int index) {\n                        return value.charAt(index);\n                    }\n                    @Override public M3String first(M3String value) {\n                        if (!(value.owner() instanceof M3StringTuple tuple)) return null;\n                        int start = value.start();\n                        int end = value.end();\n                        int split = tuple.left.length();\n                        return start >= split\n                                ? tuple.right.slice(start - split, end - split)\n                                : tuple.left.slice(start, Math.min(end, split));\n                    }\n                    @Override public M3String second(M3String value) {\n                        if (!(value.owner() instanceof M3StringTuple tuple)) return null;\n                        int split = tuple.left.length();\n                        return value.start() < split && value.end() > split\n                                ? tuple.right.slice(0, value.end() - split) : null;\n                    }\n                };\n                RELATION_ACCESS = current;\n            }\n        }\n        return current;\n    }\n\n"
        );
    }

    private static List<String> admittedJtregInsertions() {
        return List.of(
                "import java.lang.reflect.InvocationTargetException;\nimport java.lang.reflect.Proxy;\nimport java.util.ArrayList;\n",
                "    private record RouteCase(String value, String regex, boolean accessExpected,\n            boolean readsRequired, boolean unknownExpected, boolean interrupted, String label) { }\n\n    /**\n     * Proxy wraps the existing Access API; it never fabricates an M3 owner or a regex result.\n     * Access dispatch proves relation traversal entry, not that its result was returned.\n     * In particular, the 64-atom helper refuses before Access: zero dispatch at that boundary\n     * proves no owner work, while the source-sealed production selector supplies the call guard.\n     */\n    private static void verifyProductionRoutes(Field backing, Method decision,\n            Field access, Method evaluate, String required) throws Exception {\n        Class<?> m3 = decision.getParameterTypes()[0];\n        Method owner = m3.getDeclaredMethod(\"owner\");\n        Method start = m3.getDeclaredMethod(\"start\");\n        owner.setAccessible(true);\n        start.setAccessible(true);\n        Class<?> tuple = Class.forName(\"java.lang.M3StringTuple\");\n        Class<?> accessType = evaluate.getParameterTypes()[1];\n        Object original = access.get(null);\n        if (original == null) throw new AssertionError(\"deterministic Access initialization\");\n        // A backed two-unit view makes repeat construct a shared DAG even if a bootstrap\n        // literal had no M3 backing. Keep the old required source and its assertions unchanged.\n        String seed = required.substring(0, 2);\n        String terminal = required.substring(required.length() - 1);\n        if (backing.get(seed) == null || backing.get(terminal) == null) {\n            throw new AssertionError(\"known-backed route seed/range\");\n        }\n        required = String.join(\"\", seed.repeat(2048), terminal);\n        Object requiredStorage = backing.get(required);\n        if (requiredStorage == null || !tuple.isInstance(owner.invoke(requiredStorage))) {\n            throw new AssertionError(\"required production tuple not exercised\");\n        }\n\n        String mismatch = String.join(\"\", required.substring(0, required.length() - 1), \"c\");\n        String boundary255 = required.substring(required.length() - 255);\n        String boundary256 = required.substring(required.length() - 256);\n        String shifted = required.substring(1);\n        String trimmed = required.substring(1, required.length() - 1);\n        String right = required.substring(required.length() / 2);\n        for (String range : new String[]{boundary255, boundary256, shifted, trimmed, right}) {\n            Object storage = backing.get(range);\n            if (storage == null || owner.invoke(storage) != owner.invoke(requiredStorage)\n                    || (Integer) start.invoke(storage) <= 0) {\n                throw new AssertionError(\"required shared-owner nonzero range not exercised\");\n            }\n        }\n\n        var routes = new ArrayList<RouteCase>();\n        String high = String.valueOf('\\ud800');\n        String low = String.valueOf('\\udc00');\n        Object highStorage = backing.get(high);\n        Object lowStorage = backing.get(low);\n        if (highStorage == null || lowStorage == null\n                || owner.invoke(highStorage) == owner.invoke(lowStorage)) {\n            throw new AssertionError(\"required distinct surrogate unit owners\");\n        }\n        for (int count : new int[]{32, 33, 63, 64}) {\n            // A single star keeps the independent Pattern oracle free of overlapping-star blowup.\n            String regex = \"a\".repeat(count - 2) + \"a*b\";\n            boolean selected = count > 32 && count < Long.SIZE;\n            routes.add(new RouteCase(required, regex, selected, selected, false, false,\n                    \"atoms-\" + count + \"-match\"));\n            routes.add(new RouteCase(mismatch, regex, selected, selected, false, false,\n                    \"atoms-\" + count + \"-mismatch\"));\n        }\n        for (int count : new int[]{33, 63}) {\n            String regex = \"a\".repeat(count - 2) + \"a*b\";\n            routes.add(new RouteCase(boundary255, regex, false, false, false, false,\n                    \"length-255-atoms-\" + count));\n            // At 256, the relation work budget may refuse before a leaf read; rolling may decide.\n            routes.add(new RouteCase(boundary256, regex, true, false, false, false,\n                    \"length-256-atoms-\" + count));\n            for (String range : new String[]{shifted, trimmed, right}) {\n                routes.add(new RouteCase(range, regex, true, false, false, false,\n                        \"shared-range-atoms-\" + count));\n            }\n            String dotRegex = \"a\".repeat(count - 2) + \".*b\";\n            for (String unit : new String[]{\"\\n\", \"\\r\", \"\\u0085\", \"\\u2028\", \"\\u2029\", \"\\u0100\",\n                    \"\\ud800\", \"\\udc00\", \"\\ud800\\udc00\"}) {\n                String value = String.join(\"\", required.substring(0, required.length() - 1),\n                        unit, required.substring(required.length() - 1));\n                boolean surrogate = Character.isSurrogate(unit.charAt(0));\n                routes.add(new RouteCase(value, dotRegex, true, false, surrogate, false,\n                        \"UTF16-atoms-\" + count + \"-unit-\" + Integer.toHexString(unit.charAt(0))));\n            }\n            // The pair crosses distinct canonical unit owners and separately joined elements.\n            String separatedPair = String.join(\"\",\n                    required.substring(0, required.length() - 1), high, low,\n                    required.substring(required.length() - 1));\n            routes.add(new RouteCase(separatedPair, dotRegex, true, false, true, false,\n                    \"cross-owner-surrogate-pair-atoms-\" + count));\n        }\n        routes.add(new RouteCase(required, \"a\".repeat(31) + \"a*b\",\n                true, true, false, true, \"interrupted-33-atoms\"));\n\n        int[] calls = new int[2];\n        Object[] trackedSource = new Object[1];\n        Thread testThread = Thread.currentThread();\n        Object proxy = Proxy.newProxyInstance(accessType.getClassLoader(),\n                new Class<?>[]{accessType}, (ignored, method, arguments) -> {\n                    if (Thread.currentThread() == testThread\n                            && method.getDeclaringClass() == accessType) {\n                        if (arguments != null && arguments.length > 0\n                                && arguments[0] == trackedSource[0]) calls[0]++;\n                        if (method.getName().equals(\"codeUnitAt\")) calls[1]++;\n                    }\n                    try {\n                        return method.invoke(original, arguments);\n                    } catch (InvocationTargetException failure) {\n                        throw failure.getCause();\n                    }\n                });\n        int routeCases = 0;\n        int accessCases = 0;\n        int noAccessCases = 0;\n        int readCases = 0;\n        int unknownCases = 0;\n        int directCases = 0;\n        try {\n            access.set(null, proxy);\n            for (RouteCase route : routes) {\n                Object storage = backing.get(route.value());\n                if (storage == null || !tuple.isInstance(owner.invoke(storage))) {\n                    throw new AssertionError(\"required route tuple: \" + route.label());\n                }\n                boolean expected = Pattern.compile(route.regex())\n                        .matcher(new StringBuilder(route.value())).matches();\n                calls[0] = calls[1] = 0;\n                trackedSource[0] = storage;\n                if (route.interrupted()) Thread.currentThread().interrupt();\n                try {\n                    int actual = (Integer) decision.invoke(null, storage, route.regex());\n                    int dispatches = calls[0];\n                    int reads = calls[1];\n                    if (route.accessExpected() ? dispatches == 0 : dispatches != 0) {\n                        throw new AssertionError(\"relation Access entry: \" + route.label());\n                    }\n                    if (route.readsRequired() && reads == 0) {\n                        throw new AssertionError(\"required relation leaf read: \" + route.label());\n                    }\n                    if (route.unknownExpected() ? actual != -1 : actual != (expected ? 1 : 0)) {\n                        throw new AssertionError(\"production route decision: \" + route.label());\n                    }\n                    if (route.value().matches(route.regex()) != expected) {\n                        throw new AssertionError(\"public route parity: \" + route.label());\n                    }\n                    if (route.interrupted() && !Thread.currentThread().isInterrupted()) {\n                        throw new AssertionError(\"relation acquired cancellation semantics\");\n                    }\n                } finally {\n                    if (route.interrupted()) Thread.interrupted();\n                }\n                routeCases++;\n                if (route.accessExpected()) accessCases++; else noAccessCases++;\n                if (route.readsRequired()) readCases++;\n                if (route.unknownExpected()) unknownCases++;\n            }\n\n            // These separate Result assertions prove actual helper decisions and DAG reuse.\n            // Their budget is exactly the production caller's budget, not a larger test allowance.\n            for (int count : new int[]{33, 63}) {\n                int[] atoms = new int[count];\n                for (int index = 0; index < count - 1; index++) atoms[index] = 'a';\n                atoms[count - 2] |= 1 << 16;\n                atoms[count - 1] = 'b';\n                String regex = \"a\".repeat(count - 2) + \"a*b\";\n                for (String value : new String[]{required, mismatch}) {\n                    boolean expected = Pattern.compile(regex).matcher(new StringBuilder(value)).matches();\n                    Object summary = evaluate.invoke(null, backing.get(value), original, atoms,\n                            count, 128, Math.min(1_000_000L, (long) value.length() * (count + 1)),\n                            null, null);\n                    int actual = (Integer) summary.getClass().getMethod(\"decision\").invoke(summary);\n                    long reads = (Long) summary.getClass().getMethod(\"codeUnitReads\").invoke(summary);\n                    int reuse = (Integer) summary.getClass().getMethod(\"reusedSummaries\").invoke(summary);\n                    if (actual != (expected ? 1 : 0) || reuse == 0 || reads >= value.length()) {\n                        throw new AssertionError(\"bounded direct relation result/reuse: atoms=\" + count);\n                    }\n                    directCases++;\n                }\n            }\n        } finally {\n            access.set(null, original);\n        }\n        if (routeCases != 39 || accessCases != 33 || noAccessCases != 6 || readCases != 5\n                || unknownCases != 8 || directCases != 4) {\n            throw new AssertionError(\"route proof inventory drift\");\n        }\n        System.out.println(\"M3_HIERARCHICAL_REGEX_ROUTES cases=\" + routeCases\n                + \" accessCases=\" + accessCases + \" noAccessCases=\" + noAccessCases\n                + \" requiredReadCases=\" + readCases + \" surrogateUnknownCases=\" + unknownCases\n                + \" directHelperCases=\" + directCases\n                + \" scope=ENTRY_PLUS_SEPARATE_HELPER_RESULTS PASS\");\n    }\n\n",
                "        // Warm the lazy adapter through the actual eligible production path.\n        String warmRegex = \"a*\".repeat(32) + \"b\";\n        if ((Integer) decision.invoke(null, backing.get(required), warmRegex) != 1\n                || !Pattern.compile(warmRegex).matcher(new StringBuilder(required)).matches()) {\n            throw new AssertionError(\"required eligible 33-atom DAG warmup\");\n        }\n",
                "        verifyProductionRoutes(backing, decision, access, evaluate, required);\n"
        );
    }

    private static void retain(String path, String content) throws IOException {
        Path output = Path.of(System.getProperty("m3.fixture.root")).toAbsolutePath()
                .resolve("target/emitted/regex-relations-current-owner").resolve(path);
        Files.createDirectories(output.getParent());
        Files.writeString(output, content, StandardCharsets.UTF_8);
    }
}
