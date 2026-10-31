// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
/* Copyright 2026 Hitesh Soliwal and contributors. */
package com.synexia.rewrite;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;

/**
 * Bounded proof/emission fixture adapted from the existing #12386 kernel proof.
 * The unchanged M3GpuToolJoinManifest decides admission; this class never parses
 * or rewrites source and never claims OpenRewrite SDK execution.
 */
public final class M3CollectionRefinementRecipeProof {
    private static final String RESOURCE_PREFIX = "/collection-refinement/";
    private static long checks;

    private M3CollectionRefinementRecipeProof() { }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message + " [check " + checks + "]");
    }

    private static void refuse(Runnable operation, String message) {
        try {
            operation.run();
        } catch (IllegalStateException expected) {
            checks++;
            return;
        }
        throw new AssertionError("missing refusal: " + message);
    }

    private static Path directory(String argument) throws IOException {
        Path path = Path.of(argument).toAbsolutePath().normalize();
        if (!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalStateException("not a real directory: " + path);
        }
        return path.toRealPath();
    }

    /** Manifest paths are already canonical; reject filesystem symlink escapes too. */
    private static Path resolved(Path root, String relative) {
        Path result = root.resolve(relative).normalize();
        if (!result.startsWith(root)) throw new IllegalStateException("path escapes root");
        Path current = root;
        for (Path part : root.relativize(result)) {
            current = current.resolve(part);
            if (Files.isSymbolicLink(current)) {
                throw new IllegalStateException("symbolic link in source/output path: " + current);
            }
        }
        return result;
    }

    private static void rejectSymbolicAncestors(Path path) {
        Path current = path.getRoot();
        for (Path part : path) {
            current = current.resolve(part);
            if (Files.isSymbolicLink(current)) {
                throw new IllegalStateException("symbolic ancestor of output root: " + current);
            }
        }
    }

    private static M3GpuToolJoinManifest load(Path templates) {
        M3GpuToolJoinManifest manifest = M3GpuToolJoinManifest.load(RESOURCE_PREFIX,
                resource -> Files.newInputStream(resolved(templates,
                        resource.substring(RESOURCE_PREFIX.length()))),
                M3GpuToolJoinManifest.NOOP);
        if (manifest.targets().isEmpty()) throw new IllegalStateException("empty task manifest");
        for (var target : manifest.targets()) {
            if (target.removal()) throw new IllegalStateException("task excludes removals");
        }
        return manifest;
    }

    private static Map<String, String> readInputs(M3GpuToolJoinManifest manifest, Path root)
            throws IOException {
        Map<String, String> input = new LinkedHashMap<>();
        for (var target : manifest.targets()) {
            Path path = resolved(root, target.path());
            if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
                if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                    throw new IllegalStateException("non-file source: " + path);
                }
                input.put(target.path(), Files.readString(path, StandardCharsets.UTF_8));
            }
        }
        return input;
    }

    private static M3GpuToolJoinManifest.Scan scan(
            M3GpuToolJoinManifest manifest, Map<String, String> input) {
        var scan = manifest.scan();
        input.forEach((path, text) -> scan.observe(path, M3GpuToolJoinManifest.sha256(text)));
        scan.requireAdmissible();
        return scan;
    }

    /** Existing kernel replay projection; KEEP co-inputs retain their original text. */
    private static Map<String, String> replay(
            M3GpuToolJoinManifest manifest, Map<String, String> input) {
        var scan = scan(manifest, input);
        Map<String, String> result = new LinkedHashMap<>(input);
        for (var target : manifest.targets()) {
            if (scan.missing(target.path())) {
                result.put(target.path(), target.text());
            } else {
                String actual = M3GpuToolJoinManifest.sha256(input.get(target.path()));
                var admitted = scan.editing(target.path(), actual);
                if (!actual.equals(admitted.after())) result.put(target.path(), admitted.text());
            }
            check(M3GpuToolJoinManifest.sha256(result.get(target.path())).equals(target.after()),
                    "postimage mismatch: " + target.path());
        }
        return result;
    }

    private static void qualify(Path templates, Path inputRoot) throws IOException {
        var manifest = load(templates);
        Map<String, String> original = readInputs(manifest, inputRoot);
        Map<String, String> before = new LinkedHashMap<>(original);
        String decoy = "unrelated-recipe-proof-decoy.txt";
        if (manifest.target(decoy) != null) throw new IllegalStateException("decoy path occupied");
        before.put(decoy, "private long[] selected; // opaque source-shaped data\n");
        Map<String, String> after = replay(manifest, before);
        check(after.equals(replay(manifest, after)), "fixed point");
        check(after.get(decoy).equals(before.get(decoy)), "unrelated file changed");

        var paths = new ArrayList<>(before.keySet());
        Collections.reverse(paths);
        Map<String, String> reversed = new LinkedHashMap<>();
        paths.forEach(path -> reversed.put(path, before.get(path)));
        check(after.equals(replay(manifest, reversed)), "reverse input order");

        List<M3GpuToolJoinManifest.Target> targets = new ArrayList<>(manifest.targets());
        Map<String, String> partial = new LinkedHashMap<>(before);
        for (var target : targets) {
            Map<String, String> single = new LinkedHashMap<>(before);
            single.put(target.path(), after.get(target.path()));
            check(after.equals(replay(manifest, single)), "single-target resume");
            partial.put(target.path(), after.get(target.path()));
            check(after.equals(replay(manifest, partial)), "serial-prefix resume");

            if (!target.addition()) {
                Map<String, String> missing = new LinkedHashMap<>(before);
                missing.remove(target.path());
                refuse(() -> replay(manifest, missing), "required input missing");
            }
            Map<String, String> drift = new LinkedHashMap<>(before);
            drift.put(target.path(), "not an admitted source\n");
            refuse(() -> replay(manifest, drift), "source drift/occupied addition");
            var duplicate = manifest.scan();
            after.forEach((path, text) -> duplicate.observe(path, M3GpuToolJoinManifest.sha256(text)));
            duplicate.observe(target.path(), target.after());
            refuse(duplicate::requireAdmissible, "duplicate input");
            var late = scan(manifest, before);
            refuse(() -> late.editing(target.path(),
                    M3GpuToolJoinManifest.sha256("changed after scan\n")), "late edit");
        }

        long subsets = 0;
        if (targets.size() <= 12) {
            int combinations = 1 << targets.size();
            for (int bits = 0; bits < combinations; bits++) {
                Map<String, String> mixed = new LinkedHashMap<>(before);
                for (int index = 0; index < targets.size(); index++) {
                    if ((bits & (1 << index)) != 0) {
                        String path = targets.get(index).path();
                        mixed.put(path, after.get(path));
                    }
                }
                check(after.equals(replay(manifest, mixed)), "before/post subset");
                subsets++;
            }
        }

        String resourceText = Files.readString(resolved(templates, "manifest.tsv"),
                StandardCharsets.UTF_8);
        String templateName = resourceText.lines().map(line -> line.split("\t", -1))
                .filter(cells -> cells.length == 4 && !cells[3].equals("KEEP"))
                .map(cells -> cells[3]).findFirst().orElse(null);
        if (templateName != null) {
            refuse(() -> M3GpuToolJoinManifest.load(RESOURCE_PREFIX, resource -> {
                String relative = resource.substring(RESOURCE_PREFIX.length());
                byte[] bytes = Files.readAllBytes(resolved(templates, relative));
                if (relative.equals(templateName)) {
                    byte[] altered = java.util.Arrays.copyOf(bytes, bytes.length + 1);
                    altered[bytes.length] = '!';
                    return new ByteArrayInputStream(altered);
                }
                return new ByteArrayInputStream(bytes);
            }, M3GpuToolJoinManifest.NOOP), "template drift");
        }
        refuse(() -> M3GpuToolJoinManifest.load(RESOURCE_PREFIX,
                resource -> Files.newInputStream(resolved(templates,
                        resource.substring(RESOURCE_PREFIX.length()))),
                () -> { throw new CancellationException("cancelled proof fixture"); }),
                "cancellation");
        check(original.equals(readInputs(manifest, inputRoot)), "input tree changed");
        long changed = targets.stream().filter(target -> !target.after().equals(
                original.containsKey(target.path())
                        ? M3GpuToolJoinManifest.sha256(original.get(target.path()))
                        : M3GpuToolJoinManifest.ABSENT)).count();
        System.out.println("KERNEL_REPLAY_QUALIFY\tPASS\tchecks=" + checks
                + "\ttargets=" + targets.size() + "\tchanged=" + changed
                + "\texhaustiveSubsets=" + subsets + "\tSDK=NOT_RUN");
    }

    private static void emitFile(Path templates, Path inputRoot, Path outputRoot, String path)
            throws IOException {
        Path output = outputRoot.toAbsolutePath().normalize();
        rejectSymbolicAncestors(output);
        if (output.startsWith(inputRoot) || inputRoot.startsWith(output)) {
            throw new IllegalStateException("output and input roots must be disjoint");
        }
        var manifest = load(templates);
        var target = manifest.target(path);
        if (target == null) throw new IllegalStateException("target absent from task manifest");
        Map<String, String> input = readInputs(manifest, inputRoot);
        String candidate = replay(manifest, input).get(path);
        check(input.equals(readInputs(manifest, inputRoot)), "source changed before emission");
        Path destination = resolved(output, path);
        String outputBefore = M3GpuToolJoinManifest.ABSENT;
        String status = "CREATED";
        if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) {
            if (!Files.isRegularFile(destination, LinkOption.NOFOLLOW_LINKS)) {
                throw new IllegalStateException("non-file output");
            }
            outputBefore = M3GpuToolJoinManifest.sha256(Files.readString(destination,
                    StandardCharsets.UTF_8));
            if (!outputBefore.equals(target.after())) throw new IllegalStateException("output drift");
            status = "UNCHANGED";
        } else {
            Files.createDirectories(destination.getParent());
            Files.writeString(destination, candidate, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        }
        String actual = M3GpuToolJoinManifest.sha256(Files.readString(destination,
                StandardCharsets.UTF_8));
        check(actual.equals(target.after()), "emitted postimage mismatch");
        String sourceBefore = input.containsKey(path)
                ? M3GpuToolJoinManifest.sha256(input.get(path)) : M3GpuToolJoinManifest.ABSENT;
        System.out.println("KERNEL_REPLAY_EMIT\t" + status + "\tpath=" + path
                + "\tsourceBefore=" + sourceBefore + "\toutputBefore=" + outputBefore
                + "\tpostimage=" + actual + "\tSDK=NOT_RUN");
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 3 && args[0].equals("qualify")) {
            qualify(directory(args[1]), directory(args[2]));
        } else if (args.length == 5 && args[0].equals("emit-file")) {
            emitFile(directory(args[1]), directory(args[2]), Path.of(args[3]), args[4]);
        } else {
            throw new IllegalArgumentException("qualify TEMPLATE_ROOT INPUT_ROOT; "
                    + "emit-file TEMPLATE_ROOT INPUT_ROOT OUTPUT_ROOT TARGET_PATH");
        }
    }
}
