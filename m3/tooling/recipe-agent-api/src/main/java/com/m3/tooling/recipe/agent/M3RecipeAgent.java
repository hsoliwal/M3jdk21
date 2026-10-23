// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.tooling.recipe.agent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Bounded, deterministic workspace mutation API for M3 recipe agents. */
public final class M3RecipeAgent {
    public enum Operation { LITERAL, REGEX, CREATE_FILE, DELETE_FILE, MOVE_FILE, SEMANTIC }
    public enum TargetKind { FILE, FOLDER }
    public enum Authority { FILE, FOLDER, PACKAGE, MODULE, REACTOR, PUBLIC_API }

    /** A folder selector's glob matches paths relative to the selected folder. */
    public record Target(TargetKind kind, String path, String glob) {
        public Target {
            Objects.requireNonNull(kind, "kind");
            path = safePath(path);
            glob = Objects.requireNonNullElse(glob, "*");
            if (glob.isBlank() || glob.length() > 256 || glob.indexOf('\\') >= 0 || glob.contains("..")) {
                throw new IllegalArgumentException("unsafe glob");
            }
            if (kind == TargetKind.FILE && !glob.equals("*")) {
                throw new IllegalArgumentException("file selector cannot have glob");
            }
        }
    }

    /** Exact SHA-256 preimages are mandatory for every selected existing file. */
    public record Action(String id, Operation operation, Target target, String pattern,
                         String replacement, String destination, Map<String, String> preimages,
                         int expectedMatches, List<String> dependsOn, Authority authority,
                         String providerId, boolean scopePromotionApproved) {
        /** Existing call sites default to refusing scope promotion. */
        public Action(String id, Operation operation, Target target, String pattern,
                      String replacement, String destination, Map<String, String> preimages,
                      int expectedMatches, List<String> dependsOn, Authority authority,
                      String providerId) {
            this(id, operation, target, pattern, replacement, destination,
                    preimages, expectedMatches, dependsOn, authority, providerId, false);
        }

        public Action {
            id = required(id, "id");
            operation = Objects.requireNonNull(operation, "operation");
            target = Objects.requireNonNull(target, "target");
            pattern = Objects.requireNonNullElse(pattern, "");
            replacement = Objects.requireNonNullElse(replacement, "");
            destination = Objects.requireNonNullElse(destination, "");
            preimages = Map.copyOf(Objects.requireNonNull(preimages, "preimages"));
            for (var preimage : preimages.entrySet()) {
                safePath(preimage.getKey());
                if (!preimage.getValue().matches("[0-9a-f]{64}")) {
                    throw new IllegalArgumentException("invalid preimage sha256");
                }
            }
            dependsOn = List.copyOf(Objects.requireNonNull(dependsOn, "dependsOn"));
            if (new HashSet<>(dependsOn).size() != dependsOn.size() || dependsOn.contains(id)) {
                throw new IllegalArgumentException("duplicate/self dependency");
            }
            dependsOn.forEach(d -> required(d, "dependency"));
            authority = Objects.requireNonNull(authority, "authority");
            providerId = Objects.requireNonNullElse(providerId, "");
            if (expectedMatches < 0 || expectedMatches > 100_000) {
                throw new IllegalArgumentException("unbounded match count");
            }
            if (target.kind() == TargetKind.FOLDER && authority.ordinal() < Authority.FOLDER.ordinal()) {
                throw new IllegalArgumentException("folder operation exceeds authority");
            }
            if (operation != Operation.LITERAL && operation != Operation.REGEX && operation != Operation.SEMANTIC
                    && target.kind() != TargetKind.FILE) {
                throw new IllegalArgumentException("path operation requires FILE target");
            }
            if ((operation == Operation.LITERAL || operation == Operation.REGEX)
                    && (pattern.isEmpty() || pattern.length() > 512)) {
                throw new IllegalArgumentException("empty/oversized search pattern");
            }
            if (operation == Operation.MOVE_FILE) {
                destination = safePath(destination);
                if (destination.equals(target.path())) throw new IllegalArgumentException("self move");
            } else if (!destination.isEmpty()) {
                throw new IllegalArgumentException("unexpected destination");
            }
            if (operation == Operation.SEMANTIC && providerId.isBlank()) {
                throw new IllegalArgumentException("semantic provider required");
            }
            if (operation == Operation.CREATE_FILE && !preimages.isEmpty()) {
                throw new IllegalArgumentException("CREATE_FILE requires absent path");
            }
            if (operation != Operation.LITERAL && operation != Operation.REGEX
                    && expectedMatches != 0) {
                throw new IllegalArgumentException("path action cannot declare matches");
            }
        }
    }

    /** Semantic providers must run their own parser, type and contract gates. */
    public interface SemanticProvider {
        String id();
        String rewrite(String relativePath, String source, Action action);
    }

    public record Change(String path, String before, String after) {
        public Change { path = safePath(path); }
        public String beforeSha256() { return before == null ? "ABSENT" : sha(before); }
        public String afterSha256() { return after == null ? "ABSENT" : sha(after); }
    }

    /** Preview is immutable and commits to every pre/post image and DAG edge. */
    public record Preview(String fingerprint, List<String> orderedActions, List<Change> changes) {
        public Preview {
            fingerprint = Objects.requireNonNull(fingerprint, "fingerprint");
            orderedActions = List.copyOf(orderedActions);
            changes = List.copyOf(changes);
        }
    }

    private static final int MAX_FILES = 20_000;
    private static final long MAX_FILE_BYTES = 2L * 1024L * 1024L;
    private final Map<String, SemanticProvider> providers;

    public M3RecipeAgent(List<SemanticProvider> providers) {
        HashMap<String, SemanticProvider> map = new HashMap<>();
        for (SemanticProvider provider : List.copyOf(providers)) {
            if (map.putIfAbsent(required(provider.id(), "provider"), provider) != null) {
                throw new IllegalArgumentException("duplicate provider");
            }
        }
        this.providers = Map.copyOf(map);
    }

    public M3RecipeAgent() { this(List.of()); }

    /** Discover installed Java provider plugins; registration does not imply qualification. */
    public static M3RecipeAgent fromServiceLoader() {
        return new M3RecipeAgent(ServiceLoader.load(SemanticProvider.class).stream()
                .map(ServiceLoader.Provider::get).toList());
    }

    public Set<String> providerIds() { return providers.keySet(); }

    /** Generate the exact, explicit preimage seals required for file or folder actions. */
    public Map<String, String> seal(Path workspace, Target target) throws IOException {
        Path root = root(workspace);
        TreeMap<String, String> result = new TreeMap<>();
        for (String path : selectedFiles(root, target, Map.of())) {
            String input = readOptional(resolve(root, path));
            if (input == null) throw new IllegalStateException("missing target: " + path);
            result.put(path, sha(input));
        }
        return Map.copyOf(result);
    }

    public Preview preview(Path workspace, List<Action> actions) throws IOException {
        Path root = root(workspace);
        List<Action> ordered = order(actions);
        Map<String, String> current = new TreeMap<>();
        Map<String, String> original = new TreeMap<>();
        Map<String, String> latestOwner = new HashMap<>();
        Map<String, Set<String>> ancestry = ancestry(ordered);
        for (Action action : ordered) {
            List<String> selected = select(root, action, current);
            if (!Set.copyOf(selected).equals(action.preimages().keySet())) {
                throw new IllegalStateException("scope/preimage set mismatch for " + action.id());
            }
            int total = 0;
            for (String path : selected) {
                String input = image(root, path, current, original);
                if (input == null || !sha(input).equals(action.preimages().get(path))) {
                    throw new IllegalStateException("stale input for " + action.id() + ": " + path);
                }
                checkCollision(latestOwner, ancestry, action.id(), path);
                switch (action.operation()) {
                    case LITERAL, REGEX, SEMANTIC -> {
                        String output;
                        if (action.operation() == Operation.SEMANTIC) {
                            SemanticProvider provider = providers.get(action.providerId());
                            if (provider == null) throw new IllegalStateException("unregistered provider: " + action.providerId());
                            output = Objects.requireNonNull(provider.rewrite(path, input, action), "provider output");
                            if (!output.equals(input) && !output.equals(
                                    provider.rewrite(path, output, action))) {
                                throw new IllegalStateException("non-fixed-point semantic recipe: " + action.id());
                            }
                        } else {
                            Replacement next = replace(input, action);
                            total += next.matches();
                            output = next.content();
                        }
                        current.put(path, output);
                    }
                    case DELETE_FILE -> current.put(path, null);
                    case MOVE_FILE -> {
                        String dest = action.destination();
                        checkCollision(latestOwner, ancestry, action.id(), dest);
                        if (image(root, dest, current, original) != null) {
                            throw new IllegalStateException("move target exists: " + dest);
                        }
                        current.put(path, null);
                        current.put(dest, input);
                    }
                    case CREATE_FILE -> throw new IllegalStateException("create selection must be empty");
                }
            }
            if (action.operation() == Operation.CREATE_FILE) {
                String path = action.target().path();
                checkCollision(latestOwner, ancestry, action.id(), path);
                if (image(root, path, current, original) != null) {
                    throw new IllegalStateException("create target exists: " + path);
                }
                current.put(path, action.replacement());
            }
            if ((action.operation() == Operation.LITERAL || action.operation() == Operation.REGEX)
                    && total != action.expectedMatches()) {
                throw new IllegalStateException("match count mismatch: " + action.id()
                        + " expected=" + action.expectedMatches() + " actual=" + total);
            }
        }
        List<Change> changes = new ArrayList<>();
        for (var entry : current.entrySet()) {
            String before = original.get(entry.getKey());
            if (!Objects.equals(before, entry.getValue())) {
                changes.add(new Change(entry.getKey(), before, entry.getValue()));
            }
        }
        StringBuilder material = new StringBuilder("m3-recipe-agent/v1\n");
        for (Action a : ordered) {
            material.append(a.id()).append('\t').append(a.operation()).append('\t')
                    .append(a.target()).append('\t').append(a.pattern()).append('\t')
                    .append(a.replacement()).append('\t').append(a.destination()).append('\t')
                    .append(new TreeMap<>(a.preimages())).append('\t').append(a.expectedMatches())
                    .append('\t').append(a.dependsOn().stream().sorted().toList()).append('\t')
                    .append(a.authority()).append('\t').append(a.providerId()).append('\t')
                    .append(a.scopePromotionApproved()).append('\n');
        }
        for (Change change : changes) {
            material.append(change.path()).append('\t').append(change.beforeSha256())
                    .append('\t').append(change.afterSha256()).append('\n');
        }
        return new Preview(sha(material.toString()), ordered.stream().map(Action::id).toList(), changes);
    }

    /** Apply only a previously reviewed fingerprint; destructive edits need explicit permission. */
    public Preview apply(Path workspace, List<Action> actions, String approvedFingerprint,
                         boolean allowDestructive) throws IOException {
        Path root = root(workspace);
        Preview plan = preview(root, actions);
        if (!plan.fingerprint().equals(approvedFingerprint)) {
            throw new IllegalStateException("plan fingerprint drift");
        }
        if (!allowDestructive && actions.stream().anyMatch(a ->
                a.operation() == Operation.DELETE_FILE || a.operation() == Operation.MOVE_FILE)) {
            throw new IllegalStateException("destructive operation requires explicit approval");
        }
        // Stage all bytes and verify every preimage before the first mutation.
        LinkedHashMap<String, Path> staged = new LinkedHashMap<>();
        List<Change> applied = new ArrayList<>();
        try {
            for (Change change : plan.changes()) {
                Path path = resolve(root, change.path());
                String now = readOptional(path);
                if (!Objects.equals(change.before(), now)) {
                    throw new IllegalStateException("concurrent change: " + change.path());
                }
                if (change.after() != null) {
                    Path scratch = Files.createTempFile(root, ".m3-recipe-", ".staged");
                    Files.writeString(scratch, change.after(), StandardCharsets.UTF_8);
                    staged.put(change.path(), scratch);
                }
            }
            for (Change change : plan.changes()) {
                Path path = resolve(root, change.path());
                if (!Objects.equals(change.before(), readOptional(path))) {
                    throw new IllegalStateException("concurrent change during apply: " + change.path());
                }
                if (change.after() == null) {
                    Files.delete(path);
                } else {
                    Files.createDirectories(path.getParent());
                    try {
                        Files.move(staged.get(change.path()), path,
                                StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                    } catch (AtomicMoveNotSupportedException unsupported) {
                        Files.move(staged.get(change.path()), path, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
                applied.add(change);
            }
            return plan;
        } catch (IOException | RuntimeException failure) {
            // Best effort rollback. No cross-file crash-atomicity is claimed.
            for (int i = applied.size() - 1; i >= 0; i--) {
                Change change = applied.get(i);
                Path path = resolve(root, change.path());
                try {
                    if (change.before() == null) Files.deleteIfExists(path);
                    else Files.writeString(path, change.before(), StandardCharsets.UTF_8);
                } catch (IOException rollbackFailure) {
                    failure.addSuppressed(rollbackFailure);
                }
            }
            throw failure;
        } finally {
            for (Path scratch : staged.values()) Files.deleteIfExists(scratch);
        }
    }

    private static Replacement replace(String input, Action action) {
        if (action.operation() == Operation.LITERAL) {
            String needle = action.pattern();
            int count = 0;
            for (int i = 0; (i = input.indexOf(needle, i)) >= 0; i += needle.length()) count++;
            String out = input.replace(needle, action.replacement());
            if (out.contains(needle) && !out.equals(input)) {
                throw new IllegalStateException("non-fixed-point literal recipe: " + action.id());
            }
            return new Replacement(out, count);
        }
        Pattern pattern = Pattern.compile(action.pattern());
        Matcher matcher = pattern.matcher(input);
        int matches = 0;
        while (matcher.find()) {
            if (++matches > 100_000) throw new IllegalStateException("regex match budget exceeded");
        }
        String out = pattern.matcher(input).replaceAll(action.replacement());
        if (!out.equals(input) && !pattern.matcher(out).replaceAll(action.replacement()).equals(out)) {
            throw new IllegalStateException("non-fixed-point regex recipe: " + action.id());
        }
        return new Replacement(out, matches);
    }

    private record Replacement(String content, int matches) {}

    private static void checkCollision(Map<String, String> owners, Map<String, Set<String>> ancestry,
                                       String actionId, String path) {
        String prior = owners.put(path, actionId);
        if (prior != null && !prior.equals(actionId) && !ancestry.get(actionId).contains(prior)) {
            throw new IllegalStateException("unordered writes to " + path + ": " + prior + " / " + actionId);
        }
    }

    private static Map<String, Set<String>> ancestry(List<Action> order) {
        HashMap<String, Set<String>> result = new HashMap<>();
        for (Action a : order) {
            HashSet<String> all = new HashSet<>(a.dependsOn());
            for (String parent : a.dependsOn()) all.addAll(result.get(parent));
            result.put(a.id(), Set.copyOf(all));
        }
        return result;
    }

    private static List<Action> order(List<Action> actions) {
        Map<String, Action> map = new TreeMap<>();
        for (Action action : List.copyOf(actions)) {
            if (map.putIfAbsent(action.id(), action) != null) throw new IllegalArgumentException("duplicate action id");
        }
        if (map.isEmpty()) throw new IllegalArgumentException("empty recipe plan");
        Map<String, Integer> indegree = new HashMap<>();
        Map<String, List<String>> dependents = new HashMap<>();
        for (Action action : map.values()) {
            indegree.put(action.id(), action.dependsOn().size());
            for (String parent : action.dependsOn()) {
                Action prior = map.get(parent);
                if (prior == null) throw new IllegalArgumentException("missing dependency: " + parent);
                if (action.authority().ordinal() > prior.authority().ordinal()
                        && !action.scopePromotionApproved()) {
                    throw new IllegalArgumentException("scope promotion requires reviewed explicit gate: " + action.id());
                }
                dependents.computeIfAbsent(parent, ignored -> new ArrayList<>()).add(action.id());
            }
        }
        PriorityQueue<String> ready = new PriorityQueue<>();
        indegree.forEach((k, v) -> { if (v == 0) ready.add(k); });
        List<Action> result = new ArrayList<>();
        while (!ready.isEmpty()) {
            String id = ready.remove();
            result.add(map.get(id));
            for (String next : dependents.getOrDefault(id, List.of())) {
                int remaining = indegree.compute(next, (k, v) -> v - 1);
                if (remaining == 0) ready.add(next);
            }
        }
        if (result.size() != actions.size()) throw new IllegalArgumentException("recipe DAG cycle");
        return List.copyOf(result);
    }

    private static List<String> select(Path root, Action action, Map<String, String> current)
            throws IOException {
        if (action.operation() == Operation.CREATE_FILE) {
            resolve(root, action.target().path());
            return List.of();
        }
        return selectedFiles(root, action.target(), current);
    }

    private static List<String> selectedFiles(Path root, Target target, Map<String, String> current)
            throws IOException {
        if (target.kind() == TargetKind.FILE) {
            String relative = target.path();
            if (image(root, relative, current, new HashMap<>()) == null) {
                throw new IllegalStateException("missing target: " + relative);
            }
            return List.of(relative);
        }
        Path folder = resolve(root, target.path());
        if (!Files.isDirectory(folder, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalStateException("missing folder: " + folder);
        }
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + target.glob());
        List<String> paths = new ArrayList<>();
        try (var walk = Files.walk(folder)) {
            int scanned = 0;
            for (Path path : (Iterable<Path>) walk::iterator) {
                if (++scanned > 200_000) throw new IllegalStateException("scan budget exceeded");
                if (Files.isSymbolicLink(path)) throw new IllegalStateException("symlink inside scope: " + path);
                if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) continue;
                if (!matcher.matches(folder.relativize(path))) continue;
                String relative = root.relativize(path).toString().replace('\\', '/');
                paths.add(relative);
                if (paths.size() > MAX_FILES) throw new IllegalStateException("file count budget exceeded");
            }
        }
        paths.sort(Comparator.naturalOrder());
        return List.copyOf(paths);
    }

    private static String image(Path root, String relative, Map<String, String> current,
                                Map<String, String> original) throws IOException {
        if (current.containsKey(relative)) return current.get(relative);
        String content = readOptional(resolve(root, relative));
        original.putIfAbsent(relative, content);
        return content;
    }

    private static String readOptional(Path path) throws IOException {
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return null;
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalStateException("target is not regular file: " + path);
        }
        if (Files.size(path) > MAX_FILE_BYTES) throw new IllegalStateException("file size budget exceeded: " + path);
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static Path root(Path workspace) throws IOException {
        Path value = Objects.requireNonNull(workspace, "workspace").toRealPath();
        if (!Files.isDirectory(value)) throw new IllegalArgumentException("workspace not directory");
        return value;
    }

    private static Path resolve(Path root, String relative) {
        Path value = root.resolve(safePath(relative)).normalize();
        if (!value.startsWith(root)) throw new IllegalArgumentException("out of workspace");
        Path cursor = value;
        while (!cursor.equals(root)) {
            if (Files.isSymbolicLink(cursor)) throw new IllegalStateException("symlink path: " + cursor);
            cursor = cursor.getParent();
        }
        return value;
    }

    private static String safePath(String path) {
        String value = required(path, "relative path").replace('\\', '/');
        if (value.startsWith("/") || value.indexOf(':') >= 0) throw new IllegalArgumentException("absolute path");
        for (String item : value.split("/", -1)) {
            if (item.isEmpty() || item.equals(".") || item.equals("..")
                    || item.equals(".git") || item.equals(".hg") || item.equals(".svn")) {
                throw new IllegalArgumentException("unsafe relative path");
            }
        }
        return value;
    }

    private static String required(String text, String name) {
        if (text == null || text.isBlank() || text.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(name);
        }
        return text;
    }

    public static String sha(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }
}
