// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.synexia.job.IProgressMonitor;
import com.synexia.m3.contract.JavaAtomExtractor;
import com.synexia.m3.contract.JavaFileComposition;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;

/**
 * Bounded in-memory convergence laboratory for source-changing OpenRewrite recipes.
 *
 * <p>The lab is deliberately mechanical. It does not infer correctness from an LLM, similarity
 * score, compiler success alone, or one preferred recipe order. It enumerates every non-empty
 * recipe subset and every permutation within each subset, applies fresh recipe instances in serial
 * passes, compiles after every recipe application, executes a behavioral probe, checks the
 * declared public/protected contract, losslessly atomizes/reconstructs every intermediate source
 * snapshot, detects source cycles, and requires all permutations of the same subset to converge
 * to one exact source, classfile and atom root.</p>
 */
public final class M3RecipeMasteryLab {
    private M3RecipeMasteryLab() {}

    /**
     * Host-supplied context setup for one fresh recipe application.
     *
     * <p>This exists for proof-gated recipes such as full M3 Atomize, whose trusted verifier must
     * be installed by the embedding test host after compiling and behavior-checking the exact
     * candidate. The source map is immutable and the hook itself grants no promotion authority.</p>
     */
    @FunctionalInterface
    public interface RecipeContextConfigurer {
        void configure(
                Fixture fixture,
                Map<String, String> sources,
                InMemoryExecutionContext context,
                IProgressMonitor monitor);
    }

    public record RecipeAtom(
            String id,
            Supplier<? extends Recipe> factory,
            RecipeContextConfigurer contextConfigurer) {
        public RecipeAtom {
            id = required(id, "id");
            factory = Objects.requireNonNull(factory, "factory");
            contextConfigurer = Objects.requireNonNull(contextConfigurer, "contextConfigurer");
        }

        public RecipeAtom(String id, Supplier<? extends Recipe> factory) {
            this(id, factory, (fixture, sources, context, monitor) -> {});
        }

        Recipe fresh() {
            return Objects.requireNonNull(factory.get(), "recipe");
        }

        void configure(
                Fixture fixture,
                Map<String, String> sources,
                InMemoryExecutionContext context,
                IProgressMonitor monitor) {
            contextConfigurer.configure(
                    Objects.requireNonNull(fixture, "fixture"),
                    Map.copyOf(Objects.requireNonNull(sources, "sources")),
                    Objects.requireNonNull(context, "context"),
                    monitor == null ? IProgressMonitor.noop() : monitor);
        }
    }

    /**
     * Read-only static evidence callback evaluated against every intermediate source generation.
     *
     * <p>The callback has no mutation surface: it receives an immutable source map and the existing
     * progress monitor, and must return one deterministic SHA-256 root. Observer output participates
     * in same-subset permutation convergence but is not required to equal the baseline because an
     * admitted semantics-preserving rewrite can legitimately change structural evidence.</p>
     */
    @FunctionalInterface
    public interface EvidenceFunction {
        String root(Map<String, String> sources, IProgressMonitor monitor);
    }

    public record EvidenceObserver(String id, EvidenceFunction function) {
        public EvidenceObserver {
            id = required(id, "id");
            function = Objects.requireNonNull(function, "function");
        }

        String observe(Map<String, String> sources, IProgressMonitor monitor) {
            String root = Objects.requireNonNull(function.root(sources, monitor), "evidence root");
            if (!root.matches("[0-9a-f]{64}")) {
                throw new IllegalStateException("evidence observer must return lowercase SHA-256: " + id);
            }
            return root;
        }
    }

    public record Fixture(
            String id,
            Map<String, String> sources,
            String probeClass,
            String probeMethod,
            List<String> contractClasses) {
        public Fixture {
            id = required(id, "id");
            Objects.requireNonNull(sources, "sources");
            if (sources.isEmpty()) throw new IllegalArgumentException("empty fixture");
            LinkedHashMap<String, String> copy = new LinkedHashMap<>();
            sources.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(
                            entry ->
                                    copy.put(
                                            portablePath(entry.getKey()),
                                            Objects.requireNonNull(entry.getValue(), "source")));
            sources = Map.copyOf(copy);
            probeClass = required(probeClass, "probeClass");
            probeMethod = required(probeMethod, "probeMethod");
            contractClasses = List.copyOf(Objects.requireNonNull(contractClasses, "contractClasses"));
            if (contractClasses.isEmpty()) {
                throw new IllegalArgumentException("empty contractClasses");
            }
        }
    }

    public record Limits(int maxPasses, int maxSchedules, int maxRecipes, int maxSources, long maxSourceBytes) {
        public Limits {
            if (maxPasses < 2 || maxPasses > 32) throw new IllegalArgumentException("maxPasses");
            if (maxSchedules < 1 || maxSchedules > 10_000) {
                throw new IllegalArgumentException("maxSchedules");
            }
            if (maxRecipes < 1 || maxRecipes > 8) throw new IllegalArgumentException("maxRecipes");
            if (maxSources < 1 || maxSources > 256) throw new IllegalArgumentException("maxSources");
            if (maxSourceBytes < 1 || maxSourceBytes > 64L * 1024L * 1024L) {
                throw new IllegalArgumentException("maxSourceBytes");
            }
        }

        public static Limits focused() {
            return new Limits(6, 64, 6, 32, 1_000_000L);
        }
    }

    public record Step(
            int pass,
            String recipe,
            boolean changed,
            String sourceRootSha256,
            String classRootSha256,
            String atomRootSha256,
            Map<String, String> evidenceRoots) {
        public Step {
            evidenceRoots = Map.copyOf(Objects.requireNonNull(evidenceRoots, "evidenceRoots"));
        }

        public Step(
                int pass,
                String recipe,
                boolean changed,
                String sourceRootSha256,
                String classRootSha256,
                String atomRootSha256) {
            this(
                    pass,
                    recipe,
                    changed,
                    sourceRootSha256,
                    classRootSha256,
                    atomRootSha256,
                    Map.of());
        }
    }

    public record Run(
            String fixture,
            String subset,
            List<String> schedule,
            int passes,
            String sourceRootSha256,
            String classRootSha256,
            String atomRootSha256,
            String contractRootSha256,
            String probeValue,
            Map<String, String> evidenceRoots,
            List<Step> steps) {
        public Run {
            schedule = List.copyOf(schedule);
            evidenceRoots = Map.copyOf(Objects.requireNonNull(evidenceRoots, "evidenceRoots"));
            steps = List.copyOf(steps);
        }

        public Run(
                String fixture,
                String subset,
                List<String> schedule,
                int passes,
                String sourceRootSha256,
                String classRootSha256,
                String atomRootSha256,
                String contractRootSha256,
                String probeValue,
                List<Step> steps) {
            this(
                    fixture,
                    subset,
                    schedule,
                    passes,
                    sourceRootSha256,
                    classRootSha256,
                    atomRootSha256,
                    contractRootSha256,
                    probeValue,
                    Map.of(),
                    steps);
        }
    }

    public record Report(List<Run> runs) {
        public Report {
            runs = List.copyOf(runs);
        }

        public int runCount() {
            return runs.size();
        }
    }

    public static Report verify(
            List<Fixture> fixtures,
            List<RecipeAtom> atoms,
            Limits limits) {
        return verify(fixtures, atoms, limits, IProgressMonitor.noop(), List.of());
    }

    public static Report verify(
            List<Fixture> fixtures,
            List<RecipeAtom> atoms,
            Limits limits,
            IProgressMonitor monitor) {
        return verify(fixtures, atoms, limits, monitor, List.of());
    }

    public static Report verify(
            List<Fixture> fixtures,
            List<RecipeAtom> atoms,
            Limits limits,
            List<EvidenceObserver> observers) {
        return verify(fixtures, atoms, limits, IProgressMonitor.noop(), observers);
    }

    public static Report verify(
            List<Fixture> fixtures,
            List<RecipeAtom> atoms,
            Limits limits,
            IProgressMonitor monitor,
            List<EvidenceObserver> observers) {
        IProgressMonitor progress = monitor == null ? IProgressMonitor.noop() : monitor;
        List<Fixture> checkedFixtures = List.copyOf(Objects.requireNonNull(fixtures, "fixtures"));
        List<RecipeAtom> checkedAtoms = List.copyOf(Objects.requireNonNull(atoms, "atoms"));
        List<EvidenceObserver> checkedObservers =
                List.copyOf(Objects.requireNonNull(observers, "observers"));
        Limits checkedLimits = Objects.requireNonNull(limits, "limits");
        if (checkedFixtures.isEmpty()) throw new IllegalArgumentException("empty fixtures");
        if (checkedAtoms.isEmpty() || checkedAtoms.size() > checkedLimits.maxRecipes()) {
            throw new IllegalArgumentException("recipe count outside convergence budget");
        }
        Set<String> ids = new LinkedHashSet<>();
        for (RecipeAtom atom : checkedAtoms) {
            if (!ids.add(atom.id())) throw new IllegalArgumentException("duplicate recipe id: " + atom.id());
        }
        Set<String> observerIds = new LinkedHashSet<>();
        for (EvidenceObserver observer : checkedObservers) {
            if (!observerIds.add(observer.id())) {
                throw new IllegalArgumentException("duplicate evidence observer id: " + observer.id());
            }
        }

        List<Schedule> schedules = schedules(checkedAtoms, checkedLimits.maxSchedules());
        long totalRuns = Math.multiplyExact((long) checkedFixtures.size(), schedules.size());
        progress.beginTask("M3 recipe convergence", totalRuns);
        List<Run> runs = new ArrayList<>();
        Map<String, Outcome> canonicalByFixtureSubset = new LinkedHashMap<>();
        try {
            for (Fixture fixture : checkedFixtures) {
                progress.checkCanceled();
                checkedFixture(fixture, checkedLimits);
                Observation baseline =
                        compileAndObserve(
                                fixture.sources(), fixture, progress, checkedObservers);
                for (Schedule schedule : schedules) {
                    progress.checkCanceled();
                    progress.subTask(fixture.id() + " :: " + schedule.ids());
                    Run run =
                            converge(
                                    fixture,
                                    schedule,
                                    baseline,
                                    checkedLimits,
                                    progress,
                                    checkedObservers);
                    String key = fixture.id() + "\n" + schedule.subset();
                    Outcome candidate =
                            new Outcome(
                                    run.sourceRootSha256(),
                                    run.classRootSha256(),
                                    run.atomRootSha256(),
                                    run.contractRootSha256(),
                                    run.probeValue(),
                                    run.evidenceRoots());
                    Outcome canonical = canonicalByFixtureSubset.putIfAbsent(key, candidate);
                    if (canonical != null && !canonical.equals(candidate)) {
                        throw new AssertionError(
                                "order-dependent convergence for "
                                        + fixture.id()
                                        + " subset "
                                        + schedule.subset()
                                        + "\nexpected="
                                        + canonical
                                        + "\nactual="
                                        + candidate
                                        + "\nschedule="
                                        + schedule.ids());
                    }
                    runs.add(run);
                    progress.worked(1);
                }
            }
            return new Report(runs);
        } finally {
            progress.done();
        }
    }

    private static Run converge(
            Fixture fixture,
            Schedule schedule,
            Observation baseline,
            Limits limits,
            IProgressMonitor progress,
            List<EvidenceObserver> observers) {
        Map<String, String> current = fixture.sources();
        // Compare exact source states only at the same complete-round phase.
        // A later recipe may legitimately revisit an earlier intermediate source.
        Rlc<String> rounds = new Rlc<>(current);
        List<Step> steps = new ArrayList<>();
        Observation lastObservation = baseline;

        for (int pass = 1; pass <= limits.maxPasses(); pass++) {
            progress.checkCanceled();
            for (RecipeAtom atom : schedule.atoms()) {
                progress.checkCanceled();
                ApplyResult applied = apply(fixture, current, atom, progress);
                checkedSources(applied.sources(), limits);
                current = applied.sources();

                Observation observation = compileAndObserve(current, fixture, progress, observers);
                requireBehavior(fixture, schedule, pass, atom.id(), baseline, observation);
                lastObservation = observation;
                String stepRoot = sourceRoot(current);
                steps.add(
                        new Step(
                                pass,
                                atom.id(),
                                applied.changed(),
                                stepRoot,
                                observation.classRootSha256(),
                                observation.atomRootSha256(),
                                observation.evidenceRoots()));
                rounds.observe(current, applied.changed() ? 1 : 0);
            }

            Rlc.Verdict verdict = rounds.finish();
            if (verdict == Rlc.Verdict.OSCILLATION) {
                throw new AssertionError(
                        "recipe cycle detected for "
                                + fixture.id()
                                + " subset "
                                + schedule.subset()
                                + " schedule "
                                + schedule.ids()
                                + " at complete pass "
                                + pass);
            }
            String root = sourceRoot(current);
            if (verdict == Rlc.Verdict.FIXED_POINT) {
                Observation finalObservation = compileAndObserve(current, fixture, progress, observers);
                requireBehavior(
                        fixture, schedule, pass, "final-observation", baseline, finalObservation);
                if (!lastObservation.equals(finalObservation)) {
                    throw new AssertionError(
                            "unstable final observation fixture="
                                    + fixture.id()
                                    + " subset="
                                    + schedule.subset()
                                    + " pass="
                                    + pass);
                }
                return new Run(
                        fixture.id(),
                        schedule.subset(),
                        schedule.ids(),
                        pass,
                        root,
                        finalObservation.classRootSha256(),
                        finalObservation.atomRootSha256(),
                        finalObservation.contractRootSha256(),
                        finalObservation.probeValue(),
                        finalObservation.evidenceRoots(),
                        steps);
            }
        }
        throw new AssertionError(
                "no fixed point within "
                        + limits.maxPasses()
                        + " passes for "
                        + fixture.id()
                        + " subset "
                        + schedule.subset()
                        + " schedule "
                        + schedule.ids());
    }

    private static ApplyResult apply(
            Fixture fixture,
            Map<String, String> input,
            RecipeAtom atom,
            IProgressMonitor monitor) {
        M3OpenRewriteTranspiler.ProjectExecution execution =
                M3OpenRewriteTranspiler.executeProjectCycle(
                        input,
                        List.of(),
                        new M3TranspilePass(atom.id(), atom.fresh()),
                        context ->
                                atom.configure(
                                        fixture,
                                        input,
                                        context,
                                        monitor));
        return new ApplyResult(execution.after(), execution.changed());
    }

    /**
     * Mechanical candidate proof used by trusted test-host verifiers.
     *
     * <p>The exact current and overlaid project must both compile, atomize losslessly, expose the
     * same public/protected contract and return the same behavioral probe value. Class bytes and
     * atom roots may change because private extraction intentionally restructures implementation.
     * This method grants no repository or canonical promotion authority.</p>
     */
    public static void requireCandidateBehavior(
            Fixture fixture,
            Map<String, String> currentSources,
            String sourcePath,
            String beforeSource,
            String afterSource,
            IProgressMonitor monitor) {
        Fixture checkedFixture = Objects.requireNonNull(fixture, "fixture");
        Map<String, String> current = Map.copyOf(Objects.requireNonNull(currentSources, "currentSources"));
        String path = portablePath(sourcePath);
        if (!Objects.equals(current.get(path), Objects.requireNonNull(beforeSource, "beforeSource"))) {
            throw new AssertionError("candidate before-source is not bound to current project: " + path);
        }
        LinkedHashMap<String, String> candidate = new LinkedHashMap<>(current);
        candidate.put(path, Objects.requireNonNull(afterSource, "afterSource"));
        IProgressMonitor progress = monitor == null ? IProgressMonitor.noop() : monitor;
        Observation before =
                compileAndObserve(current, checkedFixture, progress, List.of());
        Observation after =
                compileAndObserve(Map.copyOf(candidate), checkedFixture, progress, List.of());
        if (!before.contractRootSha256().equals(after.contractRootSha256())) {
            throw new AssertionError("candidate changed public/protected contract: " + path);
        }
        if (!sameProbe(before, after)) {
            throw new AssertionError("candidate changed behavioral probe: " + path);
        }
    }

    private static Observation compileAndObserve(
            Map<String, String> sources,
            Fixture fixture,
            IProgressMonitor progress,
            List<EvidenceObserver> observers) {
        progress.checkCanceled();
        CompiledProject compiled = compile(sources);
        progress.checkCanceled();
        requireFixtureClasses(compiled, fixture);
        String atoms = atomRoot(sources, progress);
        String contract = publicContract(compiled.loader(), fixture.contractClasses());
        Probe probe = probe(compiled.loader(), fixture.probeClass(), fixture.probeMethod());
        Map<String, String> evidence = evidenceRoots(sources, observers, progress);
        return new Observation(
                compiled.classRootSha256(), atoms, contract, probe.value(), probe.isNull(), evidence);
    }

    private static void requireFixtureClasses(CompiledProject compiled, Fixture fixture) {
        if (!compiled.classNames().contains(fixture.probeClass())) {
            throw new AssertionError(
                    "fixture probe class is not owned by compiled project: " + fixture.probeClass());
        }
        for (String className : fixture.contractClasses()) {
            if (!compiled.classNames().contains(className)) {
                throw new AssertionError(
                        "fixture contract class is not owned by compiled project: " + className);
            }
        }
    }

    private static Map<String, String> evidenceRoots(
            Map<String, String> sources,
            List<EvidenceObserver> observers,
            IProgressMonitor progress) {
        TreeMap<String, String> roots = new TreeMap<>();
        Map<String, String> immutableSources = Map.copyOf(sources);
        for (EvidenceObserver observer : observers) {
            progress.checkCanceled();
            String root = observer.observe(immutableSources, progress);
            if (roots.put(observer.id(), root) != null) {
                throw new IllegalArgumentException("duplicate evidence observer id: " + observer.id());
            }
        }
        return Map.copyOf(roots);
    }

    private static String atomRoot(
            Map<String, String> sources,
            IProgressMonitor progress) {
        MessageDigest digest = digest();
        JavaAtomExtractor extractor = new JavaAtomExtractor();
        for (Map.Entry<String, String> entry : sources.entrySet().stream()
                .sorted(Map.Entry.comparingByKey()).toList()) {
            progress.checkCanceled();
            if (M3SourceKind.classify(entry.getKey()) != M3SourceKind.JAVA) {
                continue;
            }
            try {
                JavaFileComposition composition =
                        extractor.compose(Path.of(entry.getKey()), entry.getValue(), progress);
                if (composition.status() != JavaFileComposition.Status.COMPLETE
                        || !composition.reconstruct().equals(entry.getValue())) {
                    throw new AssertionError(
                            "atomization/reconstruction failed: "
                                    + entry.getKey()
                                    + " status="
                                    + composition.status());
                }
                framed(digest, entry.getKey());
                framed(digest, composition.root());
            } catch (IOException failure) {
                throw new IllegalStateException(
                        "atomization failed: " + entry.getKey(), failure);
            }
        }
        return hex(digest.digest());
    }

    private static CompiledProject compile(Map<String, String> sources) {
        var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("JDK compiler is required");
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager standard =
                        compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8);
                MemoryFileManager manager = new MemoryFileManager(standard)) {
            // This fixture API admits no external dependency paths. Do not let the host
            // application's classpath or source tree silently supply part of the project.
            standard.setLocationFromPaths(StandardLocation.CLASS_PATH, List.of());
            standard.setLocationFromPaths(StandardLocation.SOURCE_PATH, List.of());
            standard.setLocationFromPaths(StandardLocation.ANNOTATION_PROCESSOR_PATH, List.of());
            standard.setLocationFromPaths(StandardLocation.MODULE_PATH, List.of());
            List<JavaFileObject> inputs =
                    sources.entrySet().stream()
                            .filter(
                                    entry ->
                                            M3SourceKind.classify(entry.getKey())
                                                    == M3SourceKind.JAVA)
                            .sorted(Map.Entry.comparingByKey())
                            .map(entry -> new SourceObject(entry.getKey(), entry.getValue()))
                            .map(JavaFileObject.class::cast)
                            .toList();
            if (inputs.isEmpty()) {
                throw new IllegalArgumentException(
                        "mastery fixture must contain at least one Java source");
            }
            List<String> options =
                    List.of(
                            "--release",
                            "21",
                            "-proc:none",
                            "-g:none",
                            "-Xlint:all",
                            "-Werror",
                            "-encoding",
                            "UTF-8");
            Boolean ok =
                    compiler.getTask(null, manager, diagnostics, options, null, inputs).call();
            if (!Boolean.TRUE.equals(ok)) {
                StringBuilder failure = new StringBuilder("fixture compilation failed");
                diagnostics.getDiagnostics()
                        .forEach(
                                diagnostic ->
                                        failure.append('\n')
                                                .append(diagnostic.getKind())
                                                .append(' ')
                                                .append(diagnostic.getSource())
                                                .append(':')
                                                .append(diagnostic.getLineNumber())
                                                .append(' ')
                                                .append(diagnostic.getMessage(Locale.ROOT)));
                throw new AssertionError(failure);
            }
            Map<String, byte[]> classes = manager.classes();
            return new CompiledProject(
                    new MemoryClassLoader(classes),
                    classRoot(classes),
                    classes.keySet());
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static Probe probe(ClassLoader loader, String className, String methodName) {
        try {
            Class<?> type = Class.forName(className, true, loader);
            Method method = type.getDeclaredMethod(methodName);
            if (!Modifier.isPublic(method.getModifiers())
                    || !Modifier.isStatic(method.getModifiers())
                    || method.getParameterCount() != 0) {
                throw new IllegalArgumentException("probe must be public static zero-arg");
            }
            Object result = method.invoke(null);
            return new Probe(String.valueOf(result), result == null);
        } catch (ClassNotFoundException
                | NoSuchMethodException
                | IllegalAccessException failure) {
            throw new AssertionError("probe failed: " + className + "#" + methodName, failure);
        } catch (InvocationTargetException failure) {
            throw new AssertionError(
                    "probe threw: " + className + "#" + methodName,
                    failure.getTargetException());
        }
    }

    private static String publicContract(ClassLoader loader, List<String> classNames) {
        List<String> lines = new ArrayList<>();
        for (String className : classNames.stream().sorted().toList()) {
            try {
                Class<?> type = Class.forName(className, false, loader);
                lines.add(
                        "C\t"
                                + type.getName()
                                + '\t'
                                + Modifier.toString(
                                        type.getModifiers()
                                                & (Modifier.PUBLIC
                                                        | Modifier.PROTECTED
                                                        | Modifier.ABSTRACT
                                                        | Modifier.FINAL
                                                        | Modifier.INTERFACE)));
                Class<?> superclass = type.getSuperclass();
                lines.add("S\t" + type.getName() + '\t' + (superclass == null ? "" : superclass.getTypeName()));
                String[] interfaces =
                        Arrays.stream(type.getInterfaces())
                                .map(Class::getTypeName)
                                .sorted()
                                .toArray(String[]::new);
                for (String value : interfaces) lines.add("I\t" + type.getName() + '\t' + value);

                for (Constructor<?> constructor : type.getDeclaredConstructors()) {
                    if (visible(constructor.getModifiers())) {
                        lines.add(
                                "K\t"
                                        + type.getName()
                                        + '\t'
                                        + Modifier.toString(constructor.getModifiers())
                                        + '\t'
                                        + parameterTypes(constructor.getParameterTypes()));
                    }
                }
                for (Field field : type.getDeclaredFields()) {
                    if (visible(field.getModifiers())) {
                        lines.add(
                                "F\t"
                                        + type.getName()
                                        + '\t'
                                        + field.getName()
                                        + '\t'
                                        + field.getType().getTypeName()
                                        + '\t'
                                        + Modifier.toString(field.getModifiers()));
                    }
                }
                for (Method method : type.getDeclaredMethods()) {
                    if (visible(method.getModifiers()) && !method.isSynthetic() && !method.isBridge()) {
                        lines.add(
                                "M\t"
                                        + type.getName()
                                        + '\t'
                                        + method.getName()
                                        + '\t'
                                        + parameterTypes(method.getParameterTypes())
                                        + '\t'
                                        + method.getReturnType().getTypeName()
                                        + '\t'
                                        + Modifier.toString(method.getModifiers()));
                    }
                }
            } catch (ClassNotFoundException failure) {
                throw new AssertionError("contract class missing: " + className, failure);
            }
        }
        lines.sort(String::compareTo);
        return sha256(String.join("\n", lines).getBytes(StandardCharsets.UTF_8));
    }

    private static boolean visible(int modifiers) {
        return Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers);
    }

    private static String parameterTypes(Class<?>[] parameters) {
        return Arrays.stream(parameters)
                .map(Class::getTypeName)
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }

    private static boolean sameProbe(Observation left, Observation right) {
        return left.probeNull() == right.probeNull()
                && left.probeValue().equals(right.probeValue());
    }

    private static void requireBehavior(
            Fixture fixture,
            Schedule schedule,
            int pass,
            String recipe,
            Observation baseline,
            Observation actual) {
        if (!sameProbe(baseline, actual)) {
            throw new AssertionError(
                    "behavior drift fixture="
                            + fixture.id()
                            + " subset="
                            + schedule.subset()
                            + " pass="
                            + pass
                            + " recipe="
                            + recipe);
        }
        if (!baseline.contractRootSha256().equals(actual.contractRootSha256())) {
            throw new AssertionError(
                    "public/protected contract drift fixture="
                            + fixture.id()
                            + " subset="
                            + schedule.subset()
                            + " pass="
                            + pass
                            + " recipe="
                            + recipe);
        }
    }

    private static List<Schedule> schedules(List<RecipeAtom> atoms, int maxSchedules) {
        LinkedHashMap<String, RecipeAtom> byId = new LinkedHashMap<>();
        for (RecipeAtom atom : atoms) {
            if (byId.putIfAbsent(atom.id(), atom) != null) {
                throw new IllegalArgumentException("duplicate recipe id: " + atom.id());
            }
        }
        M3RecipeMasterySchedulePlanner.Plan plan =
                M3RecipeMasterySchedulePlanner.plan(List.copyOf(byId.keySet()), maxSchedules);
        return plan.schedules().stream()
                .map(
                        schedule ->
                                new Schedule(
                                        schedule.subset(),
                                        schedule.order().stream().map(byId::get).toList()))
                .toList();
    }

    private static void checkedFixture(Fixture fixture, Limits limits) {
        checkedSources(fixture.sources(), limits);
    }

    private static void checkedSources(Map<String, String> sources, Limits limits) {
        if (sources.size() > limits.maxSources()) {
            throw new IllegalArgumentException("fixture source count exceeds budget");
        }
        long bytes = 0L;
        int javaSources = 0;
        for (Map.Entry<String, String> entry : sources.entrySet()) {
            M3SourceKind kind = M3SourceKind.classify(entry.getKey());
            String source = entry.getValue();
            if (source.indexOf('\0') >= 0) {
                throw new IllegalArgumentException(
                        "binary/NUL-bearing fixture source: " + entry.getKey());
            }
            if (kind == M3SourceKind.JAVA) javaSources++;
            bytes = Math.addExact(bytes, source.getBytes(StandardCharsets.UTF_8).length);
        }
        if (javaSources == 0) {
            throw new IllegalArgumentException(
                    "mastery fixture must contain at least one Java source");
        }
        if (bytes > limits.maxSourceBytes()) {
            throw new IllegalArgumentException("fixture source bytes exceed budget");
        }
    }

    private static String sourceRoot(Map<String, String> sources) {
        MessageDigest digest = digest();
        sources.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(
                        entry -> {
                            framed(digest, entry.getKey());
                            byte[] bytes = entry.getValue().getBytes(StandardCharsets.UTF_8);
                            framed(digest, bytes);
                        });
        return hex(digest.digest());
    }

    private static String classRoot(Map<String, byte[]> classes) {
        MessageDigest digest = digest();
        classes.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(
                        entry -> {
                            framed(digest, entry.getKey());
                            framed(digest, entry.getValue());
                        });
        return hex(digest.digest());
    }

    private static void framed(MessageDigest digest, String value) {
        framed(digest, value.getBytes(StandardCharsets.UTF_8));
    }

    private static void framed(MessageDigest digest, byte[] value) {
        digest.update((byte) (value.length >>> 24));
        digest.update((byte) (value.length >>> 16));
        digest.update((byte) (value.length >>> 8));
        digest.update((byte) value.length);
        digest.update(value);
    }

    private static MessageDigest digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }

    private static String sha256(byte[] value) {
        return hex(digest().digest(value));
    }

    private static String hex(byte[] value) {
        return java.util.HexFormat.of().formatHex(value);
    }

    private static String portablePath(String value) {
        String checked = required(value, "path").replace('\\', '/');
        if (checked.startsWith("/") || checked.contains("../") || checked.equals("..")) {
            throw new IllegalArgumentException("non-portable path: " + value);
        }
        return checked;
    }

    private static String required(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
        if (checked.isEmpty()) throw new IllegalArgumentException(field);
        return checked;
    }

    private record Schedule(String subset, List<RecipeAtom> atoms) {
        Schedule {
            atoms = List.copyOf(atoms);
        }

        List<String> ids() {
            return atoms.stream().map(RecipeAtom::id).toList();
        }
    }

    private record ApplyResult(Map<String, String> sources, boolean changed) {}

    private record Probe(String value, boolean isNull) {}

    private record Observation(
            String classRootSha256,
            String atomRootSha256,
            String contractRootSha256,
            String probeValue,
            boolean probeNull,
            Map<String, String> evidenceRoots) {
        Observation {
            evidenceRoots = Map.copyOf(Objects.requireNonNull(evidenceRoots, "evidenceRoots"));
        }
    }

    private record Outcome(
            String sourceRootSha256,
            String classRootSha256,
            String atomRootSha256,
            String contractRootSha256,
            String probeValue,
            Map<String, String> evidenceRoots) {
        Outcome {
            evidenceRoots = Map.copyOf(Objects.requireNonNull(evidenceRoots, "evidenceRoots"));
        }
    }

    private record CompiledProject(
            ClassLoader loader, String classRootSha256, Set<String> classNames) {
        CompiledProject {
            classNames = Set.copyOf(classNames);
        }
    }

    private static final class SourceObject extends SimpleJavaFileObject {
        private final String source;

        SourceObject(String path, String source) {
            super(URI.create("string:///" + portablePath(path)), Kind.SOURCE);
            this.source = source;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }

    private static final class ClassObject extends SimpleJavaFileObject {
        private final ByteArrayOutputStream output = new ByteArrayOutputStream();

        ClassObject(String binaryName) {
            super(URI.create("mem:///" + binaryName.replace('.', '/') + Kind.CLASS.extension), Kind.CLASS);
        }

        @Override
        public OutputStream openOutputStream() {
            return output;
        }

        byte[] bytes() {
            return output.toByteArray();
        }
    }

    private static final class MemoryFileManager
            extends ForwardingJavaFileManager<JavaFileManager> {
        private final Map<String, ClassObject> output = new LinkedHashMap<>();

        MemoryFileManager(JavaFileManager delegate) {
            super(delegate);
        }

        @Override
        public JavaFileObject getJavaFileForOutput(
                Location location,
                String className,
                JavaFileObject.Kind kind,
                FileObject sibling) {
            ClassObject object = new ClassObject(className);
            output.put(className, object);
            return object;
        }

        Map<String, byte[]> classes() {
            LinkedHashMap<String, byte[]> result = new LinkedHashMap<>();
            output.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> result.put(entry.getKey(), entry.getValue().bytes()));
            return Map.copyOf(result);
        }
    }

    private static final class MemoryClassLoader extends ClassLoader {
        private final Map<String, byte[]> classes;

        MemoryClassLoader(Map<String, byte[]> classes) {
            super(ClassLoader.getPlatformClassLoader());
            this.classes = classes;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            synchronized (getClassLoadingLock(name)) {
                Class<?> loaded = findLoadedClass(name);
                if (loaded == null) {
                    loaded = classes.containsKey(name)
                            ? findClass(name)
                            : super.loadClass(name, false);
                }
                if (resolve) resolveClass(loaded);
                return loaded;
            }
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            byte[] bytes = classes.get(name);
            if (bytes == null) return super.findClass(name);
            return defineClass(name, bytes, 0, bytes.length);
        }
    }
}
