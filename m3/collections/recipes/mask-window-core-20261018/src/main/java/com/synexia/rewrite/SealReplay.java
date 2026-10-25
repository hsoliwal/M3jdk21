/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package com.synexia.rewrite;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/** Thin task driver for the existing source-seal owner; no parser or matching engine of its own. */
public final class SealReplay {
    private SealReplay() { }
    private record Change(M3GpuToolJoinManifest.Target target, Path path, String observed) { }
    public static void main(String[] args) throws IOException {
        if (args.length != 3 || !(args[0].equals("apply") || args[0].equals("check"))) {
            throw new IllegalArgumentException("apply|check source-root recipe-resource-directory");
        }
        Path root = Path.of(args[1]).toAbsolutePath().normalize();
        Path resources = Path.of(args[2]).toAbsolutePath().normalize();
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(root)) {
            throw new IllegalArgumentException("source root must be a real directory");
        }
        var manifest = M3GpuToolJoinManifest.load("/task/", name ->
                Files.newInputStream(resources.resolve(name.substring("/task/".length()))), () -> { });
        var scan = manifest.scan();
        List<Change> changes = new ArrayList<>();
        for (var target : manifest.targets()) {
            Path path = safe(root, target.path());
            String observed = null;
            if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
                if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) throw new IOException("non-file target");
                observed = M3GpuToolJoinManifest.sha256(Files.readString(path));
                scan.observe(target.path(), observed);
            }
            changes.add(new Change(target, path, observed));
        }
        scan.requireAdmissible(); // entire source/template batch must pass before first output
        int count = 0;
        for (Change change : changes) {
            var target = change.target();
            if (target.after().equals(change.observed())) continue;
            count++;
            if (args[0].equals("check")) continue;
            Path path = safe(root, target.path());
            if (change.observed() == null) {
                if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) throw new IOException("addition raced");
            } else scan.editing(target.path(), M3GpuToolJoinManifest.sha256(Files.readString(path)));
            if (target.removal()) throw new IOException("task driver is additive only");
            Files.createDirectories(path.getParent());
            safe(root, target.path());
            Path temporary = Files.createTempFile(path.getParent(), ".m3-codec-", ".tmp");
            try {
                Files.writeString(temporary, target.text());
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temporary);
            }
        }
        System.out.println("M3_SEALED_REPLAY action=" + args[0] + " changes=" + count);
    }
    private static Path safe(Path root, String relative) throws IOException {
        Path path = root.resolve(relative).normalize();
        if (!path.startsWith(root)) throw new IOException("escaping target");
        Path cursor = root;
        for (Path part : root.relativize(path)) {
            cursor = cursor.resolve(part);
            if (Files.isSymbolicLink(cursor)) throw new IOException("symlink target ancestor");
        }
        return path;
    }
}
