// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
/** Filesystem adapter of the existing manifest atom; NOT a substitute OpenRewrite scheduler. */
public final class CollectionFlowDriver {
    private CollectionFlowDriver() {}
    static void noLinks(Path root, Path target) {
        for (Path p=target; p!=null && p.startsWith(root); p=p.getParent())
            if (Files.isSymbolicLink(p)) throw new IllegalStateException("symlink: "+p);
    }
    public static int apply(Path resources, Path root) throws Exception {
        resources=resources.toAbsolutePath().normalize(); root=root.toAbsolutePath().normalize();
        final Path r=resources;
        var manifest=M3GpuToolJoinManifest.load("/crate/", name -> {
            Path file=r.resolve(name.substring("/crate/".length())); noLinks(r,file);
            return Files.newInputStream(file);
        }, M3GpuToolJoinManifest.NOOP);
        var scan=manifest.scan();
        for (var t:manifest.targets()) {
            if (t.removal()) throw new IllegalStateException("this task forbids removal");
            Path file=root.resolve(t.path()); noLinks(root,file);
            if (Files.exists(file,LinkOption.NOFOLLOW_LINKS)) {
                if (!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS)) throw new IllegalStateException("not a file");
                scan.observe(t.path(),M3GpuToolJoinManifest.sha256(Files.readString(file,StandardCharsets.UTF_8)));
            }
        }
        scan.requireAdmissible(); int changed=0;
        for (var t:manifest.targets()) {
            Path file=root.resolve(t.path()); noLinks(root,file); manifest.checkpoint();
            if (scan.missing(t.path())) {
                Files.createDirectories(file.getParent()); Files.writeString(file,t.text(),StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW); changed++;
            } else {
                String current=M3GpuToolJoinManifest.sha256(Files.readString(file,StandardCharsets.UTF_8));
                scan.editing(t.path(),current);
                if (!current.equals(t.after())) {
                    // Exact preimage already admitted; host provides exclusive worktree custody.
                    Files.writeString(file,t.text(),StandardCharsets.UTF_8,StandardOpenOption.TRUNCATE_EXISTING); changed++;
                }
            }
        }
        return changed;
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=2) throw new IllegalArgumentException("resource-directory target-root");
        System.out.println("SOURCE_SEAL_APPLY="+apply(Path.of(args[0]),Path.of(args[1])));
    }
}
