// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compiler.matsortable;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

/** Standalone genuine javac proof; has no runtime Lombok, JUnit or test-stub dependency. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/56cbdc6312594f06e21ef56f0ce33b605383270b/synexia-mat-value-processor/src/test/java/com/synexia/matvalue/processor/SortableProcessorContract.java blob f43741f44b5c; recipe m3/compiler/mat-sortable/recipe/mat-sortable-processor-20261107/relocate.py v1
public final class M3SortableProcessorContract {
    private static int checks;
    private M3SortableProcessorContract() {}

    public static void main(String[] args) throws Exception {
        positives();
        policyFingerprint();
        declaredDependencies();
        negatives();
        System.out.println("SORTABLE_PROCESSOR_ASSERTIONS=" + checks);
        System.out.println("RESULT=PASS");
    }

    private static void positives() throws Exception {
        String person = """
                package probe;
                import jdk.m3.collections.*;
                @M3Sortable public record Person(
                    @M3SortField(priority=0) String lastName,
                    @M3SortField(priority=1) String firstName,
                    int age, double score) {
                  @M3SortSignal public double relevance() { return score; }
                  @M3SortDistance public double distance(Person other) throws IllegalArgumentException {
                    return Math.abs(score-other.score);
                  }
                  String decoy() { return "@M3SortField(priority=99)"; }
                  String regex() { return "(class|@M3Sortable)"; }
                }
                """;
        String bean = """
                package probe;
                import jdk.m3.collections.*;
                @M3Sortable public class Bean {
                  @M3SortField(priority=0) final int age;
                  final String name;
                  public Bean(int age,String name) {this.age=age;this.name=name;}
                  @M3SortField(priority=1) String label() {return name;}
                  @M3SortSignal double score() throws IllegalStateException {return age*0.5;}
                }
                """;
        String descending = """
                package probe;
                import jdk.m3.collections.*;
                @M3Sortable public record Descending(
                    @M3SortField(priority=0, descending=true) long rank,
                    @M3SortField(priority=1) int tie) { }
                """;
        String scoreOnly = """
                package probe;
                import jdk.m3.collections.*;
                @M3Sortable public final class ScoreOnly {
                  private final double score;
                  public ScoreOnly(double score) { this.score = score; }
                  @M3SortSignal public double score() { return score; }
                }
                """;
        try (Result first = compile(true, List.of(
                    new Source("probe.Person", person),
                    new Source("probe.Bean", bean),
                    new Source("probe.Descending", descending),
                    new Source("probe.ScoreOnly", scoreOnly)));
             Result second = compile(true, List.of(
                    new Source("probe.ScoreOnly", scoreOnly),
                    new Source("probe.Descending", descending),
                    new Source("probe.Bean", bean),
                    new Source("probe.Person", person)))) {
            String a = Files.readString(first.generated.resolve("probe/PersonSortable.java"));
            String b = Files.readString(second.generated.resolve("probe/PersonSortable.java"));
            check(a.equals(b), "repeat and input submission order must yield exact generated source");
            String descendingFirst =
                    Files.readString(first.generated.resolve("probe/DescendingSortable.java"));
            String descendingSecond =
                    Files.readString(second.generated.resolve("probe/DescendingSortable.java"));
            check(descendingFirst.equals(descendingSecond),
                    "descending companion must be generated in both permutations");
            check(a.contains("dependency:probe.Person"), "type dependency recorded");
            check(a.contains("method:relevance()"), "method name recorded");
            check(a.contains("package:probe"), "package recorded");
            check(!a.contains("@M3SortField(priority=99)"), "code-looking string not parsed as field");
            Class<?> personType = first.loader.loadClass("probe.Person");
            Class<?> sortableType = first.loader.loadClass("probe.PersonSortable");
            List<?> structural = (List<?>) sortableType
                    .getMethod("structuralFeatures").invoke(null);
            check(structural.equals(structural.stream().sorted().toList()),
                    "source-time features ordered deterministically");
            check(structural.contains("package:probe"), "package dimension");
            check(structural.contains("class:probe.Person"), "class dimension");
            check(structural.stream().anyMatch(
                    feature -> feature.toString().startsWith("field:lastName:")),
                    "field-name/type dimension");
            check(structural.stream().anyMatch(
                    feature -> feature.toString().startsWith("method:relevance(")),
                    "method-signature dimension");
            check(structural.contains("dependency:java.lang.String"),
                    "declared dependency dimension");
            check(structural.stream().noneMatch(
                    feature -> feature.toString().contains("@M3SortField(priority=99)")),
                    "regex/code-like method body is not a declaration");
            String structureId = (String) sortableType
                    .getField("STRUCTURAL_FEATURES_SHA256").get(null);
            check(structureId.matches("[0-9a-f]{64}"), "feature seal");
            check(structureId.equals(second.loader.loadClass("probe.PersonSortable")
                    .getField("STRUCTURAL_FEATURES_SHA256").get(null)),
                    "source input permutations retain feature seal");
            check(Double.isFinite((Double) sortableType
                    .getMethod("structuralSignal").invoke(null)), "bounded numeric signal");
            boolean immutable = false;
            try {
                structural.clear();
            } catch (UnsupportedOperationException expected) {
                immutable = true;
            }
            check(immutable, "immutable structural facts");
            Constructor<?> ctor = personType.getConstructor(String.class, String.class, int.class, double.class);
            Object alan = ctor.newInstance("Brown", "Alan", 31, 0.3);
            Object mary = ctor.newInstance("Smith", "Mary", 31, 0.7);
            Object ann = ctor.newInstance("Brown", "Ann", 31, 0.3);
            Method comparator = sortableType.getMethod("compareExact", personType, personType);
            check(((Integer) comparator.invoke(null, alan, mary)) < 0, "lexical surname compare");
            check(((Integer) comparator.invoke(null, ann, alan)) > 0, "lexical firstname compare");
            check(((Integer) comparator.invoke(null, alan, alan)) == 0, "identity compare");
            check(((Integer) comparator.invoke(null, null, alan)) < 0, "nulls first");
            check(((Double) sortableType.getMethod("primary", personType).invoke(null, mary)) == 0.7,
                    "primary projection returns domain value");
            check(((Double) sortableType.getMethod("distance", personType, personType)
                    .invoke(null, alan, mary)) > 0, "explicit nonnegative distance");
            Object personEvidence = sortableType.getMethod("compareWithEvidence", personType, personType)
                    .invoke(null, alan, mary);
            Class<?> evidenceType = personEvidence.getClass();
            check(((Integer) evidenceType.getMethod("order").invoke(personEvidence)) < 0,
                    "exact field order retained in comparison evidence");
            check((Boolean) evidenceType.getMethod("exactOrderCertified").invoke(personEvidence),
                    "field comparator certifies its declared exact order");
            check((Boolean) evidenceType.getMethod("signalSeparationAvailable").invoke(personEvidence),
                    "finite primary signal separation is preserved");
            check(Math.abs((Double) evidenceType.getMethod("signalSeparation")
                    .invoke(personEvidence) - 0.4) < 1e-9,
                    "numeric signal gap is preserved, not reduced to comparator sign");
            check((Boolean) evidenceType.getMethod("domainDistanceAvailable").invoke(personEvidence),
                    "explicit @M3SortDistance evidence is preserved");
            check(Math.abs((Double) evidenceType.getMethod("domainDistance")
                    .invoke(personEvidence) - 0.4) < 1e-9,
                    "domain distance survives comparison");

            Class<?> beanType = first.loader.loadClass("probe.Bean");
            Class<?> beanSort = first.loader.loadClass("probe.BeanSortable");
            Constructor<?> bctor=beanType.getConstructor(int.class, String.class);
            Object one = bctor.newInstance(4, "A");
            Object two = bctor.newInstance(5, "B");
            check(((Integer) beanSort.getMethod("compareExact", beanType, beanType).invoke(null, one, two)) < 0,
                    "class field plus method order");
            Class<?> descendingType = first.loader.loadClass("probe.Descending");
            Class<?> descendingSort = first.loader.loadClass("probe.DescendingSortable");
            Constructor<?> descendingCtor = descendingType.getConstructor(long.class, int.class);
            Object low = descendingCtor.newInstance(Long.MIN_VALUE, 0);
            Object high = descendingCtor.newInstance(Long.MAX_VALUE, 0);
            Object tied1 = descendingCtor.newInstance(Long.MAX_VALUE, 1);
            Method descendingCompare = descendingSort.getMethod(
                    "compareExact", descendingType, descendingType);
            check(((Integer) descendingCompare.invoke(null, low, high)) > 0,
                    "descending long extrema reverse without overflow");
            check(((Integer) descendingCompare.invoke(null, high, low)) < 0,
                    "descending long extrema antisymmetry");
            check(((Integer) descendingCompare.invoke(null, high, tied1)) < 0,
                    "next ascending tie-break retained");
            check(Files.readString(first.generated.resolve("probe/DescendingSortable.java"))
                    .contains("Integer.compare(0, comparison)"),
                    "generated descending comparison is sign-safe");
            Class<?> scoreType = first.loader.loadClass("probe.ScoreOnly");
            Class<?> scoreSort = first.loader.loadClass("probe.ScoreOnlySortable");
            Constructor<?> scoreCtor = scoreType.getConstructor(double.class);
            Object positive = scoreCtor.newInstance(3.0);
            Object negativeZero = scoreCtor.newInstance(-0.0);
            Object positiveZero = scoreCtor.newInstance(0.0);
            Object anotherPositive = scoreCtor.newInstance(3.0);
            Object nan = scoreCtor.newInstance(Double.NaN);
            Method scoreComparator = scoreSort.getMethod("compareExact", scoreType, scoreType);
            check(((Integer) scoreComparator.invoke(null, negativeZero, positiveZero)) < 0,
                    "signal-only comparator respects Double.compare signed zero");
            check(((Integer) scoreComparator.invoke(null, positive, negativeZero)) > 0,
                    "signal-only comparator no longer returns zero for all distinct values");
            Object signedEvidence = scoreSort.getMethod("compareWithEvidence", scoreType, scoreType)
                    .invoke(null, negativeZero, positiveZero);
            check(!(Boolean) signedEvidence.getClass()
                    .getMethod("exactOrderCertified").invoke(signedEvidence),
                    "signal-only ordering is not falsely certified as exact domain order");
            check((Boolean) signedEvidence.getClass()
                    .getMethod("signalSeparationAvailable").invoke(signedEvidence),
                    "finite numeric signal separation is available even across signed zero");
            check((Double) signedEvidence.getClass()
                    .getMethod("signalSeparation").invoke(signedEvidence) == 0.0,
                    "signed zero can compare unequal despite zero numeric gap");
            check(!(Boolean) signedEvidence.getClass()
                    .getMethod("domainDistanceAvailable").invoke(signedEvidence),
                    "absence of explicit metric is recorded, not invented");
            Object nonfinite = scoreSort.getMethod("compareWithEvidence", scoreType, scoreType)
                    .invoke(null, positive, nan);
            check(!(Boolean) nonfinite.getClass()
                    .getMethod("signalSeparationAvailable").invoke(nonfinite),
                    "NaN separation must remain unavailable rather than invented");
            check(Files.readAllBytes(first.generated.resolve("probe/ScoreOnlySortable.java"))
                    .length > 0, "signal-only companion emitted");
            check(Files.readAllBytes(first.generated.resolve("probe/BeanSortable.java")).length > 0,
                    "class companion emitted");
        }
    }


    /** Same source schema can represent different exact sort policies. */
    private static void policyFingerprint() throws Exception {
        String ordinary = """
                package probe;
                import jdk.m3.collections.*;
                @M3Sortable public record Policy(
                    @M3SortField(priority=0) int first,
                    @M3SortField(priority=1) int second) {
                    String regex() { return "@M3SortField(priority=991)"; }
                }
                """;
        String swapped = ordinary
                .replace("@M3SortField(priority=0) int first",
                        "@M3SortField(priority=1) int first")
                .replace("@M3SortField(priority=1) int second",
                        "@M3SortField(priority=0) int second");
        String descending = ordinary.replace(
                "@M3SortField(priority=0) int first",
                "@M3SortField(priority=0,descending=true) int first");
        String decoy = ordinary.replace(
                "@M3SortField(priority=991)", "@M3SortField(priority=992)");
        try (Result normal = compile(true, List.of(new Source("probe.Policy", ordinary)));
             Result flipped = compile(true, List.of(new Source("probe.Policy", swapped)));
             Result reverse = compile(true, List.of(new Source("probe.Policy", descending)));
             Result opaque = compile(true, List.of(new Source("probe.Policy", decoy)));
             Result replay = compile(true, List.of(new Source("probe.Policy", ordinary)))) {
            String schema = (String) normal.loader.loadClass("probe.PolicySortable")
                    .getField("SCHEMA_SHA256").get(null);
            String policy = policyHash(normal);
            check(schema.equals(flipped.loader.loadClass("probe.PolicySortable")
                    .getField("SCHEMA_SHA256").get(null)),
                    "same structural schema after priority swap");
            check(schema.equals(reverse.loader.loadClass("probe.PolicySortable")
                    .getField("SCHEMA_SHA256").get(null)),
                    "same structural schema after direction change");
            check(!policy.equals(policyHash(flipped)),
                    "priority swap must invalidate policy evidence");
            check(!policy.equals(policyHash(reverse)),
                    "descending change must invalidate policy evidence");
            check(policy.equals(policyHash(opaque)),
                    "fake annotation inside string cannot change policy evidence");
            String structuralId = (String) normal.loader.loadClass("probe.PolicySortable")
                    .getField("STRUCTURAL_FEATURES_SHA256").get(null);
            check(structuralId.equals(flipped.loader.loadClass("probe.PolicySortable")
                    .getField("STRUCTURAL_FEATURES_SHA256").get(null)),
                    "priority change preserves schema-derived structural facts");
            check(structuralId.equals(reverse.loader.loadClass("probe.PolicySortable")
                    .getField("STRUCTURAL_FEATURES_SHA256").get(null)),
                    "direction change preserves schema-derived structural facts");
            check(structuralId.equals(opaque.loader.loadClass("probe.PolicySortable")
                    .getField("STRUCTURAL_FEATURES_SHA256").get(null)),
                    "code-like string change preserves structural facts");
            check(policy.equals(policyHash(replay)),
                    "repeat compiler invocation must preserve policy digest");
            check(policy.matches("[0-9a-f]{64}"), "SHA-256 policy digest");
            Class<?> kind = normal.loader.loadClass("probe.Policy");
            Object left = kind.getConstructor(int.class, int.class).newInstance(1, 10);
            Object right = kind.getConstructor(int.class, int.class).newInstance(2, 0);
            check(((Integer) normal.loader.loadClass("probe.PolicySortable")
                    .getMethod("compareExact", kind, kind)
                    .invoke(null, left, right)) < 0, "baseline exact order");
            Class<?> flippedKind = flipped.loader.loadClass("probe.Policy");
            Object flippedLeft = flippedKind.getConstructor(int.class, int.class)
                    .newInstance(1, 10);
            Object flippedRight = flippedKind.getConstructor(int.class, int.class)
                    .newInstance(2, 0);
            check(((Integer) flipped.loader.loadClass("probe.PolicySortable")
                    .getMethod("compareExact", flippedKind, flippedKind)
                    .invoke(null, flippedLeft, flippedRight)) > 0,
                    "swapped policy produces different exact order");
        }
    }

    private static String policyHash(Result compilation) throws Exception {
        return (String) compilation.loader.loadClass("probe.PolicySortable")
                .getField("ORDER_POLICY_SHA256").get(null);
    }

    /** Declared call-edge evidence remains separate from compiler-resolved invocation facts. */
    private static void declaredDependencies() throws Exception {
        String source = """
                package probe;
                import jdk.m3.collections.*;
                @M3Sortable
                @M3SortDependency({"probe.Repository#load()", "probe.Cache"})
                public class Declared {
                  @M3SortField(priority=0) public final int rank;
                  @M3SortDependency({"probe.FieldIndex#scan()"})
                  public final String name;
                  public Declared(int rank, String name) { this.rank=rank; this.name=name; }
                  @M3SortDependency({"probe.Service#fetch(int)"})
                  public int hint() { return rank; }
                  public String decoy() { return "probe.Decoy#never()"; }
                }
                """;
        String reordered = source.replace(
                "@M3SortDependency({\"probe.Repository#load()\", \"probe.Cache\"})",
                "@M3SortDependency({\"probe.Cache\", \"probe.Repository#load()\"})");
        String changed = source.replace("probe.Service#fetch(int)", "probe.Service#save(int)");
        try (Result first = compile(true, List.of(new Source("probe.Declared", source)));
             Result second = compile(true, List.of(new Source("probe.Declared", reordered)));
             Result third = compile(true, List.of(new Source("probe.Declared", changed)))) {
            String one = Files.readString(first.generated.resolve("probe/DeclaredSortable.java"));
            String two = Files.readString(second.generated.resolve("probe/DeclaredSortable.java"));
            String three = Files.readString(third.generated.resolve("probe/DeclaredSortable.java"));
            check(one.equals(two), "declared edges canonicalize annotation value order");
            check(!one.equals(three), "declared call change invalidates schema root");
            check(one.contains("declared-edge:type:probe.Declared->probe.Cache"),
                    "class source-time declared dependency");
            check(one.contains("declared-edge:field:name->probe.FieldIndex#scan()"),
                    "field source-time declared dependency");
            check(one.contains("declared-edge:method:hint()->probe.Service#fetch(int)"),
                    "method source-time declared call");
            check(!one.contains("declared-edge:method:decoy()"),
                    "code-looking literal cannot invent a call edge");
            Class<?> value = first.loader.loadClass("probe.Declared");
            Class<?> companion = first.loader.loadClass("probe.DeclaredSortable");
            List<?> structural = (List<?>) companion.getMethod("structuralFeatures")
                    .invoke(null);
            check(structural.contains("declared-edge:type:probe.Declared->probe.Cache"),
                    "declared type edge appears in immutable structural signal metadata");
            check(structural.contains("declared-edge:field:name->probe.FieldIndex#scan()"),
                    "declared field edge appears in structural signal metadata");
            check(structural.contains("declared-edge:method:hint()->probe.Service#fetch(int)"),
                    "declared method edge appears in structural signal metadata");
            check((Integer) companion.getField("STRUCTURAL_DECLARED_EDGE_COUNT").get(null) == 4,
                    "four explicitly declared edges, no dynamic-call inference");
            Object a = value.getConstructor(int.class, String.class).newInstance(1, "A");
            Object b = value.getConstructor(int.class, String.class).newInstance(2, "B");
            check((Integer) companion.getMethod("compareExact", value, value)
                    .invoke(null, a, b) < 0, "metadata does not change exact ordering");
        }
        String[] invalid = {
            "@M3SortDependency({})",
            "@M3SortDependency({\"  \"})",
            "@M3SortDependency({\"probe.A#go()\", \"probe.A#go()\"})"
        };
        for (String annotation : invalid) {
            String bad = "package negative; import jdk.m3.collections.*;"
                    + annotation
                    + "@M3Sortable public class X { @M3SortField(priority=0) int value; }";
            try (Result failure = compile(false, List.of(new Source("negative.X", bad)))) {
                check(!failure.success, "invalid declared dependency must fail compilation");
                check(failure.diagnostics.stream().anyMatch(
                        text -> text.contains("@M3SortDependency")),
                        "invalid declared dependency diagnostic required");
            }
        }
    }

    private static void negatives() throws Exception {
        String[] invalid = {
            "@M3Sortable public class X { @M3SortField(priority=0) private int age; }",
            "@M3Sortable public class X { @M3SortField(priority=0) int age; @M3SortField(priority=0) int score; }",
            "@M3Sortable public class X { @M3SortSignal int score() { return 1; } }",
            "@M3Sortable public class X { @M3SortField(priority=0) Object opaque; }",
            "@M3Sortable public class X { @M3SortDistance double dist(String s) { return 1; } "
                    + "@M3SortField(priority=0) int n; }",
            "@M3Sortable public class X<T> { @M3SortField(priority=0) int n; }"
        };
        for (String bad : invalid) {
            try (Result result=compile(false, List.of(new Source("negative.X",
                    "package negative; import jdk.m3.collections.*;" + bad)))) {
                check(!result.success, "invalid annotation must fail compilation");
            }
        }

        String[] checkedThrows = {
            "@M3Sortable public class X { @M3SortSignal double score() "
                    + "throws java.io.IOException { return 1.0; } }",
            "@M3Sortable public class X { @M3SortField(priority=0) double key() "
                    + "throws java.io.IOException { return 1.0; } }",
            "@M3Sortable public class X { @M3SortField(priority=0) int n; "
                    + "@M3SortDistance double distance(X other) "
                    + "throws java.io.IOException { return 1.0; } }"
        };
        for (String bad : checkedThrows) {
            try (Result result = compile(false, List.of(new Source("negative.X",
                    "package negative; import jdk.m3.collections.*;" + bad)))) {
                check(!result.success, "checked-throws annotation must fail");
                check(result.diagnostics.stream().anyMatch(
                        d -> d.contains("sortable method must not declare checked exceptions")),
                        "checked throws must produce an annotation diagnostic");
            }
        }

        StringBuilder oversize = new StringBuilder(
                "package negative; import jdk.m3.collections.*;"
                    + "@M3Sortable public class X { @M3SortSignal double score(){return 0.0;}");
        for (int index = 0; index < 129; index++) {
            oversize.append(" int field").append(index).append(";");
        }
        oversize.append(" }");
        try (Result result = compile(false, List.of(
                new Source("negative.X", oversize.toString())))) {
            check(!result.success, "oversize metadata must fail compilation");
            check(result.diagnostics.stream().anyMatch(
                    d -> d.contains("invalid @M3Sortable schema: @M3Sortable schema budget exceeded")),
                    "oversize schema must emit diagnostic rather than throwing");
        }
    }

    private static Result compile(boolean expected, List<Source> sources) throws Exception {
        Path root = Files.createTempDirectory("m3-sortable-proof-");
        Path classes = Files.createDirectories(root.resolve("classes"));
        Path generated = Files.createDirectories(root.resolve("generated"));
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("requires JDK 21 compiler");
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        var options = List.of("--release", "21", "-Xlint:all,-processing", "-Werror",
                "-classpath", System.getProperty("java.class.path"),
                "-d", classes.toString(), "-s", generated.toString());
        try (var fm=compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            var task=compiler.getTask(null, fm, diagnostics, options, null, sources);
            task.setProcessors(List.of(new M3MatSortableProcessor()));
            boolean success=Boolean.TRUE.equals(task.call());
            if (expected && !success) {
                throw new AssertionError("positive compile: " + diagnostics.getDiagnostics());
            }
            URLClassLoader loader=new URLClassLoader(new URL[]{classes.toUri().toURL()},
                    M3SortableProcessorContract.class.getClassLoader());
            return new Result(root,generated,loader,success,
                    diagnostics.getDiagnostics().stream()
                            .map(d -> d.getMessage(java.util.Locale.ROOT)).toList());
        }
    }

    private static final class Source extends SimpleJavaFileObject {
        private final String source;
        Source(String name, String source) {
            super(java.net.URI.create("string:///" + name.replace('.', '/') + ".java"), Kind.SOURCE);
            this.source = source;
        }
        @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }

    private static final class Result implements AutoCloseable {
        final Path root;
        final Path generated;
        final URLClassLoader loader;
        final boolean success;
        final List<String> diagnostics;
        Result(Path root, Path generated, URLClassLoader loader, boolean success,
                List<String> diagnostics) {
            this.root=root;this.generated=generated;this.loader=loader;this.success=success;
            this.diagnostics=List.copyOf(diagnostics);
        }
        @Override public void close() throws IOException {
            loader.close();
            try (var files = Files.walk(root)) {
                for (var path : files.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }
    private static void check(boolean pass, String message) {
        checks++;
        if (!pass) throw new AssertionError(message);
    }
}
