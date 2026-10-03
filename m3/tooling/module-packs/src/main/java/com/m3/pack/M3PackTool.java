// SPDX-License-Identifier: Apache-2.0
package com.m3.pack;

import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Build Facade: validates an explicit content lock and stages immutable link inputs. */
public final class M3PackTool {
    private static final String HEADER = "path\tsha256\tmodule";
    private final Copier copier;

    /** Copy Strategy permits deterministic I/O failure and drift tests. */
    @FunctionalInterface
    interface Copier {
        void copy(Path source, Path target) throws IOException;
    }

    /** Uses non-overwriting filesystem copies. */
    public M3PackTool() {
        this((source, target) -> Files.copy(source, target));
    }

    M3PackTool(Copier copier) {
        this.copier = java.util.Objects.requireNonNull(copier, "copier");
    }

    /**
     * Validates every input before publication. Paths are relative to the lock, cannot escape its
     * directory through '..' or symlinks, and require both pinned bytes and the expected module name.
     */
    public List<M3ModuleArtifact> verify(Path lock) throws IOException {
        List<String> lines = Files.readAllLines(lock, StandardCharsets.UTF_8);
        if (lines.isEmpty() || !HEADER.equals(lines.getFirst())) {
            throw new IllegalArgumentException("INVALID_LOCK_HEADER");
        }
        Path base = lock.toAbsolutePath().getParent().toRealPath();
        List<M3ModuleArtifact> result = new ArrayList<>();
        Set<String> names = new HashSet<>();
        M3ModuleInspector inspector = new M3ModuleInspector();
        for (String line : lines.subList(1, lines.size())) {
            String[] fields = line.split("\t", -1);
            if (fields.length != 3 || !fields[1].matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("INVALID_LOCK_ROW: " + line);
            }
            Path relative = Path.of(fields[0]);
            if (relative.isAbsolute() || relative.normalize().startsWith("..")) {
                throw new IllegalArgumentException("PATH_OUTSIDE_LOCK: " + relative);
            }
            Path source = base.resolve(relative).toRealPath();
            if (!source.startsWith(base)) {
                throw new IllegalArgumentException("SYMLINK_OUTSIDE_LOCK: " + relative);
            }
            M3ModuleArtifact artifact = inspector.inspect(source);
            if (!artifact.sha256().equals(fields[1])
                    || !artifact.descriptor().name().equals(fields[2])) {
                throw new IllegalArgumentException("LOCK_IDENTITY_MISMATCH: " + relative);
            }
            if (!names.add(fields[2])) {
                throw new IllegalArgumentException("DUPLICATE_LOCK_MODULE: " + fields[2]);
            }
            result.add(artifact);
        }
        result.sort(java.util.Comparator.comparing(a -> a.descriptor().name()));
        return List.copyOf(result);
    }

    /**
     * Copies validated inputs to a new, caller-selected directory. Never overwrites an existing
     * directory or archive. Failures leave evidence for inspection; no recursive deletion occurs.
     */
    public Path stage(Path lock, Path destination) throws IOException {
        List<M3ModuleArtifact> artifacts = verify(lock);
        Files.createDirectory(destination);
        StringBuilder staged = new StringBuilder(HEADER).append('\n');
        for (M3ModuleArtifact artifact : artifacts) {
            String suffix = artifact.path().toString().endsWith(".jmod") ? ".jmod" : ".jar";
            String name = artifact.descriptor().name() + suffix;
            Path target = destination.resolve(name);
            copier.copy(artifact.path(), target);
            if (!artifact.sha256().equals(M3ModuleInspector.sha256(target))) {
                throw new IOException("STAGED_INPUT_DRIFT: " + artifact.path());
            }
            staged.append(name).append('\t').append(artifact.sha256()).append('\t')
                    .append(artifact.descriptor().name()).append('\n');
        }
        Path stagedLock = destination.resolve("LOCK.tsv");
        Files.writeString(stagedLock, staged, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        return stagedLock;
    }

    static void requireJava21(int feature) {
        if (feature != 21) {
            throw new IllegalStateException("JAVA_21_TOOLCHAIN_REQUIRED");
        }
    }

    /** Executes only explicit build commands; never downloads dependencies or alters JAVA_HOME. */
    public static void main(String[] args) throws IOException {
        requireJava21(Runtime.version().feature());
        if (args.length != 3) {
            throw new IllegalArgumentException("usage: verify LOCK ROOTS | stage LOCK NEW_DIRECTORY");
        }
        M3PackTool tool = new M3PackTool();
        switch (args[0]) {
            case "stage" -> System.out.println(tool.stage(Path.of(args[1]), Path.of(args[2])));
            case "verify" -> {
                List<M3ModuleArtifact> artifacts = tool.verify(Path.of(args[1]));
                Set<String> roots = Set.of(args[2].split(",", -1));
                var system = ModuleFinder.ofSystem().findAll().stream()
                        .map(java.lang.module.ModuleReference::descriptor).toList();
                Set<String> modules = new M3ModuleClosure().resolve(system, artifacts, roots);
                System.out.println("modules\t" + String.join(",", modules));
                System.out.println("module\tversion\tsha256\tmaximum_class_version\tnative_entries");
                for (M3ModuleArtifact artifact : artifacts) {
                    System.out.println(artifact.descriptor().name() + "\t"
                            + artifact.descriptor().rawVersion().orElse("UNVERSIONED") + "\t"
                            + artifact.sha256() + "\t" + artifact.maximumClassVersion() + "\t"
                            + artifact.nativeEntries().stream().collect(Collectors.joining(",")));
                }
            }
            default -> throw new IllegalArgumentException("UNKNOWN_COMMAND: " + args[0]);
        }
    }
}
