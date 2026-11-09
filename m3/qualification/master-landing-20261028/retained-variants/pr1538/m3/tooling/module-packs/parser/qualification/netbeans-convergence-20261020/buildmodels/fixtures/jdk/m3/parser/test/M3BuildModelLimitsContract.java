// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.parser.test;

import java.net.URI;
import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Supplier;
import jdk.m3.parser.M3ASTAuthoritativeModels;
import jdk.m3.parser.M3ASTIndex;
import jdk.m3.parser.M3ASTIngress;
import jdk.m3.parser.M3File;
import org.apache.maven.model.building.ModelBuildingResult;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.gradle.tooling.model.GradleProject;
import org.jetbrains.kotlin.psi.KtFile;

/** Frozen finite/lazy API-shape oracle, separate from real-provider qualification. */
public final class M3BuildModelLimitsContract {
    private static int checks;
    private M3BuildModelLimitsContract() { }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void rejects(Class<? extends Throwable> expected, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable failure) {
            if (expected.isInstance(failure)) {
                for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
                    if (cause instanceof AssertionError) {
                        throw new AssertionError("provider probe failure wrapped before budget refusal", failure);
                    }
                }
                return;
            }
            throw new AssertionError("expected " + expected.getSimpleName()
                    + " but received " + failure, failure);
        }
        throw new AssertionError("expected " + expected.getSimpleName() + ", operation succeeded");
    }

    private static M3File source(String name) {
        return M3File.of(URI.create("memory:/" + name), "x");
    }

    private static M3ASTIngress.Limits limits(int nodes, int depth) {
        return new M3ASTIngress.Limits(16, nodes, depth);
    }

    private static M3ASTIndex maven(Model model, int nodes, int depth) {
        ModelBuildingResult original = () -> model;
        return M3ASTAuthoritativeModels.mavenEffective(source("pom.xml"), original, limits(nodes, depth));
    }

    private static void index(M3ASTIndex index, String[] kinds, int[] parents, boolean exact) {
        check(index.nodeCount() == kinds.length, "exact node count");
        check(kinds.length == parents.length, "oracle dimensions");
        for (int row = 0; row < kinds.length; row++) {
            check(Objects.equals(kinds[row], index.kind(row)), "kind at " + row);
            check(index.parent(row) == parents[row], "parent at " + row);
            check(index.hasExactRange(row) == exact, "range provenance at " + row);
            check(index.flags(row) == (exact ? M3ASTIndex.EXACT_RANGE | M3ASTIndex.SYNTAX_ONLY
                    : M3ASTIndex.SOURCE_UNVERIFIED), "flags at " + row);
            if (exact) check(index.sourceText(row).equals("x"), "source bytes at " + row);
            else rejects(IllegalStateException.class, () -> index.sourceText(0));
            int first = -1, count = 0, next = -1;
            for (int child = 0; child < parents.length; child++) {
                if (parents[child] == row) {
                    if (first < 0) first = child;
                    count++;
                }
                if (child > row && parents[child] == parents[row] && next < 0) next = child;
            }
            check(index.firstChild(row) == first, "first child at " + row);
            check(index.childCount(row) == count, "child count at " + row);
            check(index.nextSibling(row) == next, "next sibling at " + row);
        }
    }

    public static final class Model {
        public Object parent;
        public Object modules = List.of();
        public Object dependencies = List.of();
        public Object managed;
        public Object build;
        public Object profiles = List.of();
        public Object repositories = List.of();
        public Object pluginRepositories = List.of();
        public Object getParent() { return parent; }
        public Object getModules() { return modules; }
        public Object getDependencies() { return dependencies; }
        public Object getDependencyManagement() { return managed; }
        public Object getBuild() { return build; }
        public Object getProfiles() { return profiles; }
        public Object getRepositories() { return repositories; }
        public Object getPluginRepositories() { return pluginRepositories; }
    }

    public static final class Managed {
        public Object getDependencies() { return List.of("one", "two"); }
    }

    public static final class Build {
        public Object getPlugins() { return List.of(new Plugin(true), new Plugin(false)); }
        public Object getPluginManagement() { return new ManagedPlugins(); }
    }

    public static final class Plugin {
        private final boolean populated;
        public Plugin(boolean populated) { this.populated = populated; }
        public Object getExecutions() { return populated ? List.of("one", "two") : List.of(); }
        public Object getDependencies() { return populated ? List.of("one") : List.of(); }
    }

    public static final class ManagedPlugins {
        public Object getPlugins() { return List.of("one"); }
    }

    private static Model completeModel() {
        Model model = new Model();
        model.parent = new Object();
        model.modules = List.of("one", "two");
        model.dependencies = List.of("one", "two");
        model.managed = new Managed();
        model.build = new Build();
        model.profiles = List.of("one");
        model.repositories = List.of("one");
        model.pluginRepositories = List.of("one");
        return model;
    }

    public static final class Project implements GradleProject {
        private final String name;
        private final Object tasks;
        private final Object children;
        public Project(String name, Object tasks, Object children) {
            this.name = name; this.tasks = tasks; this.children = children;
        }
        @Override public String getName() { return name; }
        @Override public Object getTasks() { return tasks; }
        @Override public Object getChildren() { return children; }
    }

    private static Project leaf(String name) { return new Project(name, List.of(), List.of()); }

    public static final class LazyItems implements Iterable<Object> {
        private final int count;
        private final int permittedReads;
        private final Supplier<Object> item;
        public int reads;
        public LazyItems(int count, int permittedReads, Supplier<Object> item) {
            this.count = count; this.permittedReads = permittedReads; this.item = item;
        }
        @Override public Iterator<Object> iterator() {
            return new Iterator<>() {
                private int position;
                @Override public boolean hasNext() { return position < count; }
                @Override public Object next() {
                    if (!hasNext()) throw new NoSuchElementException();
                    if (++reads > permittedReads) throw new AssertionError("PRE_BUDGET_ENUMERATION");
                    position++;
                    return item.get();
                }
            };
        }
    }

    public static final class WideCollection extends AbstractCollection<Object> {
        @Override public int size() { return 100_000; }
        @Override public Iterator<Object> iterator() {
            throw new AssertionError("OVERSIZED_COLLECTION_MATERIALIZED");
        }
    }

    public static final class ReadLimitedList extends AbstractList<Project> {
        private final int count;
        private final int allowed;
        public int reads;
        public ReadLimitedList(int count, int allowed) { this.count = count; this.allowed = allowed; }
        @Override public int size() { return count; }
        @Override public Project get(int index) {
            Objects.checkIndex(index, count);
            if (++reads > allowed) throw new AssertionError("FRONTIER_EXCEEDED_BEFORE_REFUSAL");
            return leaf("queued");
        }
    }

    public static final class ClassNode extends ASTNode {
        private final Object fields;
        public ClassNode(Object fields) { this.fields = fields; }
        public Object getMethods() { return List.of(); }
        public Object getFields() { return fields; }
    }

    public static final class FieldNode extends ASTNode {
        public Object getInitialExpression() { return null; }
    }

    public static class KotlinElement {
        private final Object[] children;
        public KotlinElement(Object... children) { this.children = children; }
        public KtFile.Range getTextRange() { return new KtFile.Range(0, 1); }
        public Object[] getChildren() { return children; }
    }

    public static final class UnvisitedKotlinElement extends KotlinElement {
        @Override public KtFile.Range getTextRange() {
            throw new AssertionError("KOTLIN_FRONTIER_VISITED_AFTER_EXHAUSTION");
        }
    }

    private static void valid() {
        index(maven(new Model(), 1, 1), new String[] {"MAVEN_EFFECTIVE_MODEL"}, new int[] {-1}, false);
        index(maven(completeModel(), 20, 4), new String[] {
                "MAVEN_EFFECTIVE_MODEL", "MAVEN_INHERITED_PARENT", "MAVEN_MODULE", "MAVEN_MODULE",
                "MAVEN_DEPENDENCY", "MAVEN_DEPENDENCY", "MAVEN_DEPENDENCY_MANAGEMENT",
                "MAVEN_MANAGED_DEPENDENCY", "MAVEN_MANAGED_DEPENDENCY", "MAVEN_BUILD", "MAVEN_PLUGIN",
                "MAVEN_PLUGIN_EXECUTION", "MAVEN_PLUGIN_EXECUTION", "MAVEN_PLUGIN_DEPENDENCY",
                "MAVEN_PLUGIN", "MAVEN_PLUGIN_MANAGEMENT", "MAVEN_MANAGED_PLUGIN", "MAVEN_PROFILE",
                "MAVEN_REPOSITORY", "MAVEN_PLUGIN_REPOSITORY"},
                new int[] {-1, 0, 0, 0, 0, 0, 0, 6, 6, 0, 9, 10, 10, 10, 9, 9, 15, 0, 0, 0}, false);
        rejects(IllegalArgumentException.class, () -> maven(completeModel(), 19, 4));
        Model nullList = new Model();
        nullList.modules = Arrays.asList((Object) null);
        check(maven(nullList, 2, 2).nodeCount() == 2, "existing List null policy");
        Model nullCollection = new Model();
        nullCollection.modules = new LinkedHashSet<>(Arrays.asList((Object) null));
        rejects(NullPointerException.class, () -> maven(nullCollection, 2, 2));
        Model lazy = new Model();
        lazy.modules = new LazyItems(3, 3, () -> "module");
        check(maven(lazy, 4, 2).nodeCount() == 4, "exact finite lazy boundary");

        Project original = new Project("root", List.of("t1", "t2"), List.of(
                new Project("a", List.of("ta"), List.of(leaf("a1"))),
                new Project("b", List.of("tb"), List.of())));
        M3ASTIndex gradle = M3ASTAuthoritativeModels.gradleTooling(source("build.gradle"), original, limits(8, 3));
        check(gradle.domain() == M3ASTIndex.Domain.GRADLE_GROOVY, "Gradle Groovy domain");
        index(gradle, new String[] {"GRADLE_TOOLING_PROJECT", "GRADLE_TOOLING_TASK", "GRADLE_TOOLING_TASK",
                "GRADLE_TOOLING_PROJECT", "GRADLE_TOOLING_TASK", "GRADLE_TOOLING_PROJECT",
                "GRADLE_TOOLING_PROJECT", "GRADLE_TOOLING_TASK"}, new int[] {-1, 0, 0, 0, 3, 3, 0, 6}, false);
        check(M3ASTAuthoritativeModels.gradleTooling(source("build.gradle.kts"), original, limits(8, 3)).domain()
                == M3ASTIndex.Domain.GRADLE_KOTLIN, "Gradle inferred Kotlin domain");
        rejects(IllegalArgumentException.class, () -> M3ASTAuthoritativeModels.gradleTooling(
                source("build.gradle"), original, limits(7, 3)));

        FieldNode shared = new FieldNode();
        ModuleNode module = new ModuleNode(List.of(new ClassNode(List.of(shared, shared))));
        index(M3ASTAuthoritativeModels.gradleGroovyAst(source("build.gradle"), module, limits(4, 3)),
                new String[] {"GROOVY_ModuleNode", "GROOVY_ClassNode", "GROOVY_FieldNode", "GROOVY_FieldNode"},
                new int[] {-1, 0, 1, 1}, false);
        rejects(IllegalArgumentException.class, () -> M3ASTAuthoritativeModels.gradleGroovyAst(
                source("build.gradle"), module, limits(4, 2)));

        KotlinElement child = new KotlinElement();
        KtFile psi = new KtFile("x", child, child);
        index(M3ASTAuthoritativeModels.gradleKotlinPsi(source("build.gradle.kts"), psi, limits(3, 2)),
                new String[] {"KOTLIN_PSI_KtFile", "KOTLIN_PSI_KotlinElement", "KOTLIN_PSI_KotlinElement"},
                new int[] {-1, 0, 0}, true);
        rejects(IllegalArgumentException.class, () -> M3ASTAuthoritativeModels.gradleKotlinPsi(
                source("build.gradle.kts"), new KtFile("changed"), limits(3, 2)));
        rejects(IllegalArgumentException.class, () -> M3ASTAuthoritativeModels.gradleKotlinPsi(
                source("build.gradle.kts"), psi, limits(3, 1)));
    }

    private static void mavenDepth() {
        Model model = new Model(); model.dependencies = List.of("one");
        rejects(IllegalArgumentException.class, () -> maven(model, 2, 1));
    }

    private static void mavenNestedDepth() {
        rejects(IllegalArgumentException.class, () -> maven(completeModel(), 20, 3));
    }

    private static void mavenLazyBudget() {
        LazyItems items = new LazyItems(100_000, 6, () -> "module");
        Model model = new Model(); model.modules = items;
        rejects(IllegalArgumentException.class, () -> maven(model, 4, 2));
        check(items.reads <= 4, "lazy materialization remains within node budget");
    }

    private static void mavenCollectionBudget() {
        Model model = new Model(); model.dependencies = new WideCollection();
        rejects(IllegalArgumentException.class, () -> maven(model, 4, 2));
    }

    private static void gradleTaskDepth() {
        Project original = new Project("root", List.of("task"), List.of());
        rejects(IllegalArgumentException.class, () -> M3ASTAuthoritativeModels.gradleTooling(
                source("build.gradle"), original, limits(2, 1)));
    }

    private static void gradleLazyBudget() {
        LazyItems items = new LazyItems(100_000, 6, () -> leaf("child"));
        Project original = new Project("root", List.of(), items);
        rejects(IllegalArgumentException.class, () -> M3ASTAuthoritativeModels.gradleTooling(
                source("build.gradle"), original, limits(4, 2)));
        check(items.reads <= 4, "Gradle lazy materialization budget");
    }

    private static void gradleFrontier() {
        ReadLimitedList grandchildren = new ReadLimitedList(12, 5);
        Project[] children = new Project[12];
        Arrays.fill(children, leaf("sibling"));
        children[0] = new Project("first", List.of(), grandchildren);
        Project original = new Project("root", List.of(), Arrays.asList(children));
        rejects(IllegalArgumentException.class, () -> M3ASTAuthoritativeModels.gradleTooling(
                source("build.gradle"), original, limits(16, 3)));
        check(grandchildren.reads <= 5, "compound frontier bounded before reading next child");
    }

    private static void groovyLazyBudget() {
        LazyItems items = new LazyItems(100_000, 6, FieldNode::new);
        rejects(IllegalArgumentException.class, () -> M3ASTAuthoritativeModels.gradleGroovyAst(
                source("build.gradle"), new ModuleNode(items), limits(4, 2)));
        check(items.reads <= 5, "Groovy node buffer budget plus one boundary observation");
    }

    private static void kotlinFrontier() {
        Object[] grandchildren = new Object[12];
        Arrays.fill(grandchildren, new UnvisitedKotlinElement());
        Object[] children = new Object[12];
        Arrays.fill(children, new KotlinElement());
        children[0] = new KotlinElement(grandchildren);
        rejects(IllegalArgumentException.class, () -> M3ASTAuthoritativeModels.gradleKotlinPsi(
                source("build.gradle.kts"), new KtFile("x", children), limits(16, 3)));
    }

    public static void main(String[] args) {
        if (args.length != 1) throw new IllegalArgumentException("one frozen case name required");
        switch (args[0]) {
            case "valid" -> valid();
            case "maven-depth" -> mavenDepth();
            case "maven-nested-depth" -> mavenNestedDepth();
            case "maven-lazy-budget" -> mavenLazyBudget();
            case "maven-collection-budget" -> mavenCollectionBudget();
            case "gradle-task-depth" -> gradleTaskDepth();
            case "gradle-lazy-budget" -> gradleLazyBudget();
            case "gradle-frontier" -> gradleFrontier();
            case "groovy-lazy-budget" -> groovyLazyBudget();
            case "kotlin-frontier" -> kotlinFrontier();
            default -> throw new IllegalArgumentException("unknown frozen case: " + args[0]);
        }
        System.out.println("M3_BUILD_MODEL_LIMITS_PASS case=" + args[0] + " checks=" + checks);
    }
}
