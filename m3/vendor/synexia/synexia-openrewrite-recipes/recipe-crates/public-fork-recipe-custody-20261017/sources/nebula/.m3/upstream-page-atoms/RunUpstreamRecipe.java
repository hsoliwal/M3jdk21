// SPDX-License-Identifier: Apache-2.0
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import org.openrewrite.*;
import org.openrewrite.config.Environment;
import org.openrewrite.config.YamlResourceLoader;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

/** Executes the upstream catalogue recipe, verifies complete classes, then applies its real results. */
public final class RunUpstreamRecipe {
    static final String BASE = "887cf8256ab471425bc631334d9e23d24726d34e";
    static final String PREFIX = "widgets/pagination/org.eclipse.nebula.widgets.pagination/src/org/eclipse/nebula/widgets/pagination/";
    static final String RECIPE = "com.synexia.NebulaPageBoundaryAtoms";
    static final String YAML_HASH = "f0d6565526e08e275e646a173536e8bf727a294808b47fb69a9dfa0b9d6e9fda";
    static final Map<String, List<String>> SEALS = Map.of(
        PREFIX + "collections/PageListHelper.java", List.of("4d57e51cc1050b8cbf4f24221297cd521196d90b6239bffb073fe04320383688", "d8c8311c8e54355fd0fccaf439608b3d7142c4938f4d118c8dfdd14f205637d6"),
        PREFIX + "collections/BeanComparator.java", List.of("19564a6f6d4612788356333c306db4d2a80de46cb24bb4e05bbb90782cf11b8a", "40b75e386fec078f139b74ee3af6d1cd857a28aa312813b3008d716eaa526eca"));
    static final List<String> CLOSURE = List.of("PageableController.java", "IPageChangedListener.java", "collections/PageListHelper.java", "collections/PageResult.java", "collections/SortProcessor.java", "collections/DefaultSortProcessor.java", "collections/BeanComparator.java", "collections/BeanUtils.java");
    private RunUpstreamRecipe() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 3 || !Set.of("--apply", "--check").contains(args[2])) throw new IllegalArgumentException("<nebula-root> <recipe-crate> <--apply|--check>");
        Path root = Path.of(args[0]).toRealPath(), lane = Path.of(args[1]).toRealPath();
        Path evidence = lane.resolve("target/evidence"); Files.createDirectories(evidence);
        String yaml = Files.readString(lane.resolve("recipe.yml"));
        require(hash(yaml).equals(YAML_HASH), "RECIPE_PAYLOAD_DRIFT");
        Map<String, String> baseline = new TreeMap<>(), current = new TreeMap<>();
        for (String relative : CLOSURE) {
            checkpoint(); String path = PREFIX + relative;
            String original = command(root, "git", "show", BASE + ":" + path);
            String actual = readOwned(root, path);
            if (SEALS.containsKey(path)) {
                require(hash(original).equals(SEALS.get(path).get(0)), "PINNED_BASELINE_DRIFT:" + path);
                require(SEALS.get(path).contains(hash(actual)), "CURRENT_SOURCE_DRIFT:" + path);
            } else require(original.equals(actual), "DEPENDENCY_SOURCE_DRIFT:" + path);
            baseline.put(path, original); current.put(path, actual);
        }
        Recipe recipe;
        try (var in = Files.newInputStream(lane.resolve("recipe.yml"))) {
            recipe = Environment.builder().load(new YamlResourceLoader(in, lane.resolve("recipe.yml").toUri(), new Properties())).build().activateRecipes(RECIPE);
        }
        require(recipe.validateAll().stream().allMatch(v -> v.isValid()), "INVALID_RECIPE");
        Map<String, String> before = new TreeMap<>(); SEALS.keySet().forEach(p -> before.put(p, baseline.get(p)));
        before.put("README.md", "```java\n" + baseline.get(PREFIX + "collections/BeanComparator.java") + "\n```\n");
        before.put("unrelated/BeanComparator.java", baseline.get(PREFIX + "collections/BeanComparator.java"));
        Map<String, String> after = execute(recipe, before, 2);
        for (String path : SEALS.keySet()) require(hash(after.get(path)).equals(SEALS.get(path).get(1)), "UNEXPECTED_RECIPE_OUTPUT:" + path);
        require(after.get("README.md").equals(before.get("README.md")), "DOCUMENT_CHANGED");
        require(after.get("unrelated/BeanComparator.java").equals(before.get("unrelated/BeanComparator.java")), "SOURCE_FENCE_BROKEN");
        require(execute(recipe, after, 0).equals(after), "REPLAY_NOT_FIXED");
        System.out.println("PASS upstream=org.openrewrite.text.FindAndReplace version=8.17.1 changedFiles=2 fixedPoint=true markdownUnchanged=true unrelatedJavaUnchanged=true");
        Map<String, String> candidate = new TreeMap<>(baseline); SEALS.keySet().forEach(p -> candidate.put(p, after.get(p)));
        PageContract.verify(baseline, candidate, evidence);
        StringBuilder receipts = new StringBuilder("path\tbeforeSha256\tafterSha256\n");
        for (String path : new TreeSet<>(SEALS.keySet())) {
            checkpoint();
            require(readOwned(root, path).equals(current.get(path)), "SOURCE_CHANGED_DURING_VERIFICATION");
            if (args[2].equals("--check")) require(current.get(path).equals(after.get(path)), "RECIPE_NOT_APPLIED:" + path);
            receipts.append(path).append('\t').append(hash(baseline.get(path))).append('\t').append(hash(after.get(path))).append('\n');
            Path output = evidence.resolve(Path.of(path).getFileName()); Files.writeString(output, after.get(path));
        }
        // Only after the complete transformation and contract suite passes. Atomic per file, not per batch.
        int writes = 0;
        if (args[2].equals("--apply")) for (String path : new TreeSet<>(SEALS.keySet())) {
            checkpoint(); require(readOwned(root, path).equals(current.get(path)), "SOURCE_RACE");
            if (current.get(path).equals(after.get(path))) continue;
            Path target = root.resolve(path), temp = Files.createTempFile(target.getParent(), ".m3-upstream-", ".tmp");
            try {
                Files.writeString(temp, after.get(path));
                if (Files.getFileStore(target).supportsFileAttributeView("posix")) Files.setPosixFilePermissions(temp, Files.getPosixFilePermissions(target));
                checkpoint(); require(readOwned(root, path).equals(current.get(path)), "SOURCE_RACE_BEFORE_COMMIT");
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); writes++;
            } finally { Files.deleteIfExists(temp); }
        }
        Files.writeString(evidence.resolve("source-seals.tsv"), receipts);
        System.out.print(receipts);
        System.out.println("PASS sourceWrites=" + writes + " publicApiUnchanged=true; fullTychoAndUiNotRun=true");
    }

    static Map<String, String> execute(Recipe recipe, Map<String, String> inputs, int expectedChanges) {
        List<SourceFile> files = new ArrayList<>();
        inputs.forEach((path, text) -> files.add(PlainText.builder().sourcePath(Path.of(path)).text(text).build()));
        List<Throwable> errors = new ArrayList<>();
        RecipeRun run = recipe.run(new InMemoryLargeSourceSet(files), new InMemoryExecutionContext(errors::add), 3);
        require(errors.isEmpty(), "UPSTREAM_ERRORS:" + errors);
        require(run.getDataTableRows("org.openrewrite.table.SourcesFileErrors").isEmpty(), "UPSTREAM_SOURCE_ERRORS");
        var changes = run.getChangeset().getAllResults(); require(changes.size() == expectedChanges, "CHANGE_COUNT:" + changes.size());
        Map<String, String> result = new TreeMap<>(inputs);
        for (Result change : changes) {
            require(change.getBefore() != null && change.getAfter() != null, "CREATE_DELETE_NOT_ALLOWED");
            String path = change.getBefore().getSourcePath().toString();
            require(change.getAfter().getSourcePath().equals(change.getBefore().getSourcePath()) && SEALS.containsKey(path), "OUT_OF_SCOPE_RESULT");
            result.put(path, change.getAfter().printAll());
        }
        return result;
    }
    static String readOwned(Path root, String relative) throws Exception {
        Path file = root.resolve(relative); require(Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) && file.equals(file.toRealPath()), "LINK_OR_NON_REGULAR");
        require(Files.size(file) < 1_048_576L, "FILE_BUDGET"); return Files.readString(file);
    }
    static String command(Path root, String... command) throws Exception {
        Process process = new ProcessBuilder(command).directory(root.toFile()).redirectError(ProcessBuilder.Redirect.INHERIT).start();
        byte[] output = process.getInputStream().readNBytes(1_048_577);
        if (output.length > 1_048_576 || !process.waitFor(45, java.util.concurrent.TimeUnit.SECONDS)) { process.destroyForcibly(); throw new IOException("PROCESS_BUDGET"); }
        require(process.exitValue() == 0, "PROCESS_FAILED:" + Arrays.toString(command)); return new String(output, StandardCharsets.UTF_8);
    }
    static String hash(String text) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))); }
    static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
    static void checkpoint() { if (Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException(); }
}
