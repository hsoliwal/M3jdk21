// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.exact;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

/**
 * Exact current-master replay of the reviewed CDateTime/E4 owner closure.
 *
 * <p>Every existing target is accepted only at its recorded preimage or postimage.
 * The removed simple CSS provider is generated only when the exact BaseCSSThemingTest
 * anchor is present.  No fuzzy source matching is used.</p>
 */
public final class NebulaM3CurrentCDateTimeOwnerRecipe extends Recipe {
    private static final String ROOT =
            "/org/eclipse/nebula/m3/rewrite/exact/cdatetime-current-owner/";

    @Override public String getDisplayName() {
        return "Apply current-owner CDateTime and E4 compatibility closure";
    }

    @Override public String getDescription() {
        return "Replays the reviewed CDateTime CSS compatibility, E4 Jakarta migration, "
                + "test provider restoration and owned Batik repository wiring from exact "
                + "current Nebula master source hashes.";
    }

    @Override public Set<String> getTags() {
        return Set.of("nebula", "m3", "recipe-first", "cdatetime", "e4",
                "source-custody", "current-master");
    }

    @Override public List<Recipe> getRecipeList() {
        return List.of(
                new ParentPom(),
                new CDateTimeHandler(),
                new E4Manifest(),
                new E4Product(),
                new BigWidgets(),
                new SimpleWidgets(),
                new BaseCssThemingTest(),
                new SimpleProviderMaterializer());
    }

    private abstract static class JavaTarget extends Recipe {
        private final String repositoryPath;
        private final String modulePath;
        private final String beforeRawSha256;
        private final String afterRawSha256;
        private final String beforeResource;
        private final String afterResource;

        JavaTarget(
                String repositoryPath,
                String modulePath,
                String beforeRawSha256,
                String afterRawSha256,
                String beforeResource,
                String afterResource) {
            this.repositoryPath = repositoryPath;
            this.modulePath = modulePath;
            this.beforeRawSha256 = beforeRawSha256;
            this.afterRawSha256 = afterRawSha256;
            this.beforeResource = ROOT + beforeResource;
            this.afterResource = ROOT + afterResource;
        }

        @Override public int maxCycles() {
            return 1;
        }

        @Override public TreeVisitor<?, ExecutionContext> getVisitor() {
            String beforeRaw = resource(beforeResource);
            String afterRaw = resource(afterResource);
            if (!beforeRawSha256.equals(NebulaM3ExactJavaSnapshotRecipe.sha256(beforeRaw))) {
                throw new IllegalStateException(
                        "M3 current-owner Java preimage resource drift: " + repositoryPath);
            }
            if (!afterRawSha256.equals(NebulaM3ExactJavaSnapshotRecipe.sha256(afterRaw))) {
                throw new IllegalStateException(
                        "M3 current-owner Java postimage resource drift: " + repositoryPath);
            }

            return new TreeVisitor<Tree, ExecutionContext>() {
                @Override public Tree preVisit(Tree tree, ExecutionContext context) {
                    if (!(tree instanceof J.CompilationUnit unit) || !matches(unit.getSourcePath())) {
                        return tree;
                    }
                    stopAfterPreVisit();

                    J.CompilationUnit before = parse(beforeRaw, unit.getSourcePath(), context);
                    J.CompilationUnit after = parse(afterRaw, unit.getSourcePath(), context);
                    String current = unit.printAll();
                    if (after.printAll().equals(current)) {
                        return unit;
                    }
                    if (!before.printAll().equals(current)) {
                        context.getOnError().accept(
                                new IllegalStateException(
                                        "M3 current-owner Java preimage drift: "
                                                + normalized(unit.getSourcePath())));
                        return unit;
                    }

                    SourceFile replacement = after.withId(unit.getId());
                    replacement = replacement.withSourcePath(unit.getSourcePath());
                    replacement = replacement.withMarkers(unit.getMarkers());
                    replacement = replacement.withFileAttributes(unit.getFileAttributes());
                    replacement = replacement.withCharset(unit.getCharset());
                    replacement = replacement.withCharsetBomMarked(unit.isCharsetBomMarked());
                    return replacement.withChecksum(null);
                }
            };
        }

        private boolean matches(Path sourcePath) {
            String path = normalized(sourcePath);
            return path.equals(repositoryPath) || path.equals(modulePath);
        }

        private J.CompilationUnit parse(
                String body, Path sourcePath, ExecutionContext context) {
            List<SourceFile> parsed = JavaParser.fromJavaVersion()
                    .build()
                    .parseInputs(
                            List.of(Parser.Input.fromString(sourcePath, body)),
                            null,
                            context)
                    .toList();
            if (parsed.size() != 1 || !(parsed.getFirst() instanceof J.CompilationUnit unit)) {
                throw new IllegalStateException(
                        "M3 current-owner Java parse failed: " + repositoryPath);
            }
            return unit;
        }

        private static String resource(String name) {
            try (InputStream input =
                    NebulaM3CurrentCDateTimeOwnerRecipe.class.getResourceAsStream(name)) {
                if (input == null) {
                    throw new IllegalStateException(
                            "missing current-owner Java resource: " + name);
                }
                return new String(input.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException failure) {
                throw new IllegalStateException(
                        "cannot read current-owner Java resource", failure);
            }
        }

        private static String normalized(Path path) {
            return path.normalize().toString().replace('\\', '/');
        }
    }

    private abstract static class TextTarget extends NebulaM3ExactTextSnapshotRecipe {
        private final String repositoryPath;
        private final String modulePath;
        private final String before;
        private final String after;
        private final String resource;

        TextTarget(String repositoryPath, String modulePath, String before, String after, String resource) {
            this.repositoryPath = repositoryPath;
            this.modulePath = modulePath;
            this.before = before;
            this.after = after;
            this.resource = ROOT + resource;
        }

        @Override protected final String repositoryPath() { return repositoryPath; }
        @Override protected final String moduleRelativePath() { return modulePath; }
        @Override protected final String beforeSha256() { return before; }
        @Override protected final String afterSha256() { return after; }
        @Override protected final String afterResource() { return resource; }
    }

    private static final class ParentPom extends TextTarget {
        ParentPom() {
            super("releng/org.eclipse.nebula.nebula-parent/pom.xml",
                    "releng/org.eclipse.nebula.nebula-parent/pom.xml",
                    "1b30a4af10a3a3b5731e45ae36afa7c07faaf7d721c4b7bf15f88c3a481fc6f4",
                    "7598a2672ff8ccdf31589d1528daca3fe22ef5881fc98ea17daff23d5d444d00",
                    "00-nebula-parent-pom.xml.txt");
        }
        @Override public String getDisplayName() { return "Wire the owned Batik p2 repository"; }
        @Override public String getDescription() { return "Replays the reviewed parent POM repository/profile postimage."; }
    }

    private static final class CDateTimeHandler extends JavaTarget {
        CDateTimeHandler() {
            super("widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.css/src/org/eclipse/nebula/widgets/cdatetime/css/CDateTimePropertyHandler.java",
                    "src/org/eclipse/nebula/widgets/cdatetime/css/CDateTimePropertyHandler.java",
                    "04f07a9bc03ab54dd4635b7dd24548fa2d39035c51f5923181d5c5cd62785fa6",
                    "9b95b52df265443f996fc67e0316025bce167281d175478b0070edf56c54d87d",
                    "pre-01-CDateTimePropertyHandler.java.txt",
                    "01-CDateTimePropertyHandler.java.txt");
        }
        @Override public String getDisplayName() { return "Preserve legacy CDateTime font classification"; }
        @Override public String getDescription() { return "Replays the reviewed modern-switch form while preserving legacy CSS effects."; }
    }

    private static final class E4Manifest extends TextTarget {
        E4Manifest() {
            super("widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.example.e4/META-INF/MANIFEST.MF",
                    "widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.example.e4/META-INF/MANIFEST.MF",
                    "1bd3b08226fbc0ad9f0586c60cc71debe027992fc55f75fd0f4eff57664369cf",
                    "99ec161a425ef2a974b02a1986eb4185b44c515d26642f58a9c962a656841323",
                    "02-MANIFEST.MF.txt");
        }
        @Override public String getDisplayName() { return "Migrate CDateTime E4 manifest to Jakarta annotation"; }
        @Override public String getDescription() { return "Replays the reviewed E4 import package migration."; }
    }

    private static final class E4Product extends TextTarget {
        E4Product() {
            super("widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.example.e4/org.eclipse.nebula.widgets.cdatetime.example.e4.product",
                    "widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.example.e4/org.eclipse.nebula.widgets.cdatetime.example.e4.product",
                    "4ff3163f0a625e1adab50bf3e216026bd0fbb0353aa84012c6d8092cc61956c1",
                    "ef4669a264a851ad0e3eeb767f4846a08940621b205e959872e030f2811333fe",
                    "03-cdatetime-e4.product.txt");
        }
        @Override public String getDisplayName() { return "Migrate CDateTime E4 product annotation bundles"; }
        @Override public String getDescription() { return "Replays the reviewed Jakarta annotation/inject product bundle set."; }
    }

    private static final class BigWidgets extends JavaTarget {
        BigWidgets() {
            super("widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.example.e4/src/org/eclipse/nebula/widgets/cdatetime/example/e4/parts/BigWidgetsPart.java",
                    "src/org/eclipse/nebula/widgets/cdatetime/example/e4/parts/BigWidgetsPart.java",
                    "a0b890d25d9c7cdd433f00e49cecd87083ec12b1244a96f2ebc4903d826acc16",
                    "f7cf28894b872ed09dc009ae59632b3faa777b502a0fd73168ca85e46c62543c",
                    "pre-04-BigWidgetsPart.java.txt",
                    "04-BigWidgetsPart.java.txt");
        }
        @Override public String getDisplayName() { return "Migrate BigWidgets E4 annotations"; }
        @Override public String getDescription() { return "Replays the reviewed javax-to-jakarta annotation migration."; }
    }

    private static final class SimpleWidgets extends JavaTarget {
        SimpleWidgets() {
            super("widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.example.e4/src/org/eclipse/nebula/widgets/cdatetime/example/e4/parts/SimpleWidgetsPart.java",
                    "src/org/eclipse/nebula/widgets/cdatetime/example/e4/parts/SimpleWidgetsPart.java",
                    "88553ed64c0dfb7c08a5d7c64380886e65ef855b72774bcb3a8b5e52273a299b",
                    "08d0d45568b0d895607b8ebe74be1c9975dd1321f63f27e76b75170ae2404a99",
                    "pre-05-SimpleWidgetsPart.java.txt",
                    "05-SimpleWidgetsPart.java.txt");
        }
        @Override public String getDisplayName() { return "Migrate SimpleWidgets E4 annotations"; }
        @Override public String getDescription() { return "Replays the reviewed javax-to-jakarta annotation migration."; }
    }

    private static final class BaseCssThemingTest extends JavaTarget {
        BaseCssThemingTest() {
            super("widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.tests/src/org/eclipse/nebula/widgets/cdatetime/css/BaseCSSThemingTest.java",
                    "src/org/eclipse/nebula/widgets/cdatetime/css/BaseCSSThemingTest.java",
                    "79a0f8e1a6c46e87b6f49ece1389867da540a626c1d4fa43138df0e181158462",
                    "44dd7b7a3f606a5b72d1d2ba2c194d03fc4303795394cd59adc12b4cc87b275f",
                    "pre-06-BaseCSSThemingTest.java.txt",
                    "06-BaseCSSThemingTest.java.txt");
        }
        @Override public String getDisplayName() { return "Restore CDateTime CSS test provider registration"; }
        @Override public String getDescription() { return "Replays the reviewed constructor-driven 23-property provider registration."; }
    }

    private static final class SimpleProviderMaterializer
            extends ScanningRecipe<SimpleProviderMaterializer.State> {
        private static final String REPOSITORY_ANCHOR =
                "widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.tests/src/org/eclipse/nebula/widgets/cdatetime/css/BaseCSSThemingTest.java";
        private static final String MODULE_ANCHOR =
                "src/org/eclipse/nebula/widgets/cdatetime/css/BaseCSSThemingTest.java";
        private static final String REPOSITORY_TARGET =
                "widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.tests/src/org/eclipse/nebula/widgets/cdatetime/tests/css/CSSPropertyHandlerSimpleProviderImpl.java";
        private static final String MODULE_TARGET =
                "src/org/eclipse/nebula/widgets/cdatetime/tests/css/CSSPropertyHandlerSimpleProviderImpl.java";
        private static final String AFTER =
                "234eff7ce055796a8ea7f1d266dc69a63a280e0d4eb36ade24563e18b784fa48";

        static final class State {
            boolean anchorSeen;
            boolean repositoryCoordinates;
            boolean targetSeen;
            final List<String> conflicts = new ArrayList<>();
        }

        @Override public String getDisplayName() { return "Restore the removed simple CSS provider"; }
        @Override public String getDescription() {
            return "Generates the reviewed EPL-2.0 test provider only beside the exact CDateTime test anchor.";
        }
        @Override public Set<String> getTags() {
            return Set.of("nebula", "m3", "recipe-first", "cdatetime", "materializer");
        }
        @Override public int maxCycles() { return 1; }
        @Override public State getInitialValue(ExecutionContext context) {
            String body = resource(ROOT + "07-CSSPropertyHandlerSimpleProviderImpl.java.txt");
            if (!AFTER.equals(sha256(body))) throw new IllegalStateException("provider resource drift");
            return new State();
        }
        @Override public TreeVisitor<?, ExecutionContext> getScanner(State state) {
            return new TreeVisitor<Tree, ExecutionContext>() {
                @Override public Tree preVisit(Tree tree, ExecutionContext context) {
                    if (!(tree instanceof SourceFile source)) return tree;
                    stopAfterPreVisit();
                    String path = normalized(source.getSourcePath());
                    synchronized (state) {
                        if (REPOSITORY_ANCHOR.equals(path) || MODULE_ANCHOR.equals(path)) {
                            state.anchorSeen = true;
                            state.repositoryCoordinates = REPOSITORY_ANCHOR.equals(path);
                        }
                        if (REPOSITORY_TARGET.equals(path) || MODULE_TARGET.equals(path)) {
                            state.targetSeen = true;
                            if (!(source instanceof J.CompilationUnit)) {
                                state.conflicts.add("provider target is not Java");
                            } else {
                                String body = resource(ROOT + "07-CSSPropertyHandlerSimpleProviderImpl.java.txt");
                                J.CompilationUnit expected = parseProvider(
                                        body, source.getSourcePath(), context);
                                if (!expected.printAll().equals(source.printAll())) {
                                    state.conflicts.add("provider target drift");
                                }
                            }
                        }
                    }
                    return tree;
                }
            };
        }
        @Override public Collection<? extends SourceFile> generate(
                State state, Collection<SourceFile> generatedInThisCycle, ExecutionContext context) {
            synchronized (state) {
                if (!state.anchorSeen) return List.of();
                if (!state.conflicts.isEmpty()) throw new IllegalStateException(String.join("; ", state.conflicts));
                if (state.targetSeen) return List.of();
                String body = resource(ROOT + "07-CSSPropertyHandlerSimpleProviderImpl.java.txt");
                String path = state.repositoryCoordinates ? REPOSITORY_TARGET : MODULE_TARGET;
                J.CompilationUnit parsed = parseProvider(body, Path.of(path), context);
                return List.of(parsed.withSourcePath(Path.of(path)));
            }
        }
        @Override public TreeVisitor<?, ExecutionContext> getVisitor(State state) {
            return new TreeVisitor<Tree, ExecutionContext>() {};
        }

        private static J.CompilationUnit parseProvider(
                String body, Path sourcePath, ExecutionContext context) {
            List<SourceFile> parsed = JavaParser.fromJavaVersion().build()
                    .parseInputs(
                            List.of(Parser.Input.fromString(sourcePath, body)),
                            null,
                            context)
                    .toList();
            if (parsed.size() != 1 || !(parsed.getFirst() instanceof J.CompilationUnit unit)) {
                throw new IllegalStateException("provider Java parse failed");
            }
            return unit;
        }

        private static String normalized(Path path) {
            return path.normalize().toString().replace('\\', '/');
        }
        private static String resource(String name) {
            try (InputStream input = NebulaM3CurrentCDateTimeOwnerRecipe.class.getResourceAsStream(name)) {
                if (input == null) throw new IllegalStateException("missing current-owner resource: " + name);
                return new String(input.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException failure) {
                throw new IllegalStateException("cannot read current-owner resource", failure);
            }
        }
        private static String sha256(String value) {
            try {
                return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                        .digest(Objects.requireNonNull(value, "value").getBytes(StandardCharsets.UTF_8)));
            } catch (NoSuchAlgorithmException impossible) {
                throw new ExceptionInInitializerError(impossible);
            }
        }
    }
}
